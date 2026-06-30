package com.projectassistant.sql;

import com.projectassistant.model.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * 数据库 Schema 逆向工程
 * 从 @Entity / @Table / @Column 注解 + MyBatis XML resultMap 推断表结构
 */
public class SchemaParser {

    private final List<ClassInfo> classes;
    private final List<TableInfo> tables = new ArrayList<>();
    private final List<java.nio.file.Path> xmlFiles;

    // JPA 注解
    private static final Pattern ENTITY_TABLE = Pattern.compile(
            "@Table\\s*\\([^)]*name\\s*=\\s*\"([^\"]+)\"[^)]*\\)");
    private static final Pattern COLUMN_NAME = Pattern.compile(
            "@Column\\s*\\([^)]*name\\s*=\\s*\"([^\"]+)\"[^)]*\\)");
    private static final Pattern COLUMN_PROPS = Pattern.compile(
            "@Column\\s*\\(([^)]+)\\)");
    private static final Pattern ID_GENERATED = Pattern.compile(
            "@GeneratedValue\\s*\\([^)]*strategy\\s*=\\s*(?:GenerationType\\.)?(IDENTITY|AUTO|SEQUENCE|TABLE)[^)]*\\)");

    // MyBatis resultMap
    private static final Pattern RESULT_MAP = Pattern.compile(
            "<resultMap\\s+[^>]*id\\s*=\\s*\"([^\"]+)\"\\s+[^>]*type\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern RESULT_COLUMN = Pattern.compile(
            "<(?:id|result)\\s+[^>]*column\\s*=\\s*\"([^\"]+)\"[^>]*property\\s*=\\s*\"([^\"]+)\"[^>]*jdbcType\\s*=\\s*\"([^\"]+)\"[^>]*/?>");
    private static final Pattern RESULT_COLUMN_SIMPLE = Pattern.compile(
            "<(?:id|result)\\s+[^>]*column\\s*=\\s*\"([^\"]+)\"[^>]*property\\s*=\\s*\"([^\"]+)\"[^>]*/?>");
    private static final Pattern ASSOCIATION = Pattern.compile(
            "<association\\s+[^>]*property\\s*=\\s*\"([^\"]+)\"[^>]*/?>");
    private static final Pattern COLLECTION = Pattern.compile(
            "<collection\\s+[^>]*property\\s*=\\s*\"([^\"]+)\"[^>]*/?>");

    // MyBatis-Plus 注解
    private static final Pattern TABLENAME_ANN = Pattern.compile(
            "@TableName\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern TABLEFIELD_ANN = Pattern.compile(
            "@TableField\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern TABLEID_ANN = Pattern.compile(
            "@TableId\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");

    // Java → SQL 类型映射
    private static final Map<String, String> JAVA_TO_SQL = new LinkedHashMap<>();
    static {
        JAVA_TO_SQL.put("String", "VARCHAR");
        JAVA_TO_SQL.put("int", "INT");
        JAVA_TO_SQL.put("Integer", "INT");
        JAVA_TO_SQL.put("long", "BIGINT");
        JAVA_TO_SQL.put("Long", "BIGINT");
        JAVA_TO_SQL.put("double", "DOUBLE");
        JAVA_TO_SQL.put("Double", "DOUBLE");
        JAVA_TO_SQL.put("float", "FLOAT");
        JAVA_TO_SQL.put("Float", "FLOAT");
        JAVA_TO_SQL.put("boolean", "TINYINT");
        JAVA_TO_SQL.put("Boolean", "TINYINT");
        JAVA_TO_SQL.put("Date", "DATETIME");
        JAVA_TO_SQL.put("LocalDate", "DATE");
        JAVA_TO_SQL.put("LocalDateTime", "DATETIME");
        JAVA_TO_SQL.put("LocalTime", "TIME");
        JAVA_TO_SQL.put("BigDecimal", "DECIMAL");
        JAVA_TO_SQL.put("byte[]", "BLOB");
        JAVA_TO_SQL.put("Byte[]", "BLOB");
    }

    public SchemaParser(List<ClassInfo> classes, List<java.nio.file.Path> xmlFiles) {
        this.classes = classes;
        this.xmlFiles = xmlFiles;
    }

    /** 执行解析 */
    public List<TableInfo> parse() {
        tables.clear();
        parseEntityClasses();
        parseXmlResultMaps();
        mergeTables();
        System.out.println("  [Schema] 发现 " + tables.size() + " 张表, "
                + tables.stream().mapToInt(t -> t.getColumns().size()).sum() + " 个字段");
        return tables;
    }

    /** 从 JPA @Entity 或 MyBatis-Plus @TableName 解析表结构 */
    private void parseEntityClasses() {
        for (ClassInfo ci : classes) {
            boolean isEntity = false;
            boolean hasTableOrTableName = false;
            for (String ann : ci.getAnnotations()) {
                String clean = ann.replace("@", "");
                if (clean.equals("Entity") || clean.startsWith("Entity(")) {
                    isEntity = true;
                }
                if (TABLENAME_ANN.matcher(ann).find() || ENTITY_TABLE.matcher(ann).find()) {
                    hasTableOrTableName = true;
                }
            }
            if (!isEntity && !hasTableOrTableName) continue;

            String tableName = inferTableName(ci);
            TableInfo ti = new TableInfo();
            ti.setTableName(tableName);
            ti.setEntityClass(ci.getFullyQualifiedName());

            // 解析每个字段
            for (FieldInfo fi : ci.getFields()) {
                TableInfo.Column col = parseColumn(fi, ci);
                if (col != null) ti.getColumns().add(col);
            }

            tables.add(ti);
        }
    }

    /** 从 MyBatis resultMap 推断表结构 */
    private void parseXmlResultMaps() {
        if (xmlFiles == null) return;

        for (java.nio.file.Path xmlFile : xmlFiles) {
            try {
                String content = java.nio.file.Files.readString(xmlFile);
                Matcher rm = RESULT_MAP.matcher(content);
                while (rm.find()) {
                    String id = rm.group(1);
                    String type = rm.group(2);
                    // 用 resultMap id 做表名
                    String tableName = inferTableFromResultMap(id);
                    TableInfo ti = new TableInfo();
                    ti.setTableName(tableName);
                    ti.setEntityClass(type);
                    ti.setComment("MyBatis resultMap: " + id);

                    // 从该 resultMap 内解析列
                    int start = rm.end();
                    // 找到对应的 </resultMap>
                    int end = findClosingTag(content, start, "resultMap");
                    if (end < 0) continue;
                    String mapBody = content.substring(start, end);

                    // 解析 <id> 和 <result> 列
                    Matcher rc = RESULT_COLUMN.matcher(mapBody);
                    while (rc.find()) {
                        TableInfo.Column col = new TableInfo.Column();
                        col.setColumnName(rc.group(1));
                        col.setFieldName(rc.group(2));
                        col.setSqlType(rc.group(3));
                        col.setPrimaryKey(mapBody.substring(rc.start(), rc.end()).startsWith("<id"));
                        ti.getColumns().add(col);
                    }
                    // 无 jdbcType 版本的
                    Matcher rs = RESULT_COLUMN_SIMPLE.matcher(mapBody);
                    while (rs.find()) {
                        String colName = rs.group(1);
                        boolean alreadyExists = ti.getColumns().stream()
                                .anyMatch(c -> c.getColumnName().equals(colName));
                        if (!alreadyExists) {
                            TableInfo.Column col = new TableInfo.Column();
                            col.setColumnName(colName);
                            col.setFieldName(rs.group(2));
                            col.setSqlType("VARCHAR");
                            col.setPrimaryKey(mapBody.substring(rs.start(), rs.end()).startsWith("<id"));
                            ti.getColumns().add(col);
                        }
                    }

                    if (!ti.getColumns().isEmpty()) {
                        tables.add(ti);
                    }
                }
            } catch (Exception e) {
                // skip bad XML files
            }
        }
    }

    /** 根据 resultMap id 推断表名 */
    private String inferTableFromResultMap(String id) {
        // 尝试从常见命名提取：BaseResultMap → base, UserResultMap → user
        String lower = id.toLowerCase();
        if (lower.endsWith("resultmap")) {
            String base = lower.substring(0, lower.length() - 9);
            return toSnakeCase(base);
        }
        if (lower.endsWith("map")) {
            String base = lower.substring(0, lower.length() - 3);
            return toSnakeCase(base);
        }
        return toSnakeCase(id);
    }

    /** 从 @Entity 类名/注解推断表名 */
    private String inferTableName(ClassInfo ci) {
        // 先找 @Table(name = "xxx")
        for (String ann : ci.getAnnotations()) {
            Matcher m = ENTITY_TABLE.matcher(ann);
            if (m.find()) return m.group(1);
        }
        // 再找 MyBatis-Plus @TableName("xxx")
        for (String ann : ci.getAnnotations()) {
            Matcher m = TABLENAME_ANN.matcher(ann);
            if (m.find()) return m.group(1);
        }
        // 默认：类名转蛇形
        return toSnakeCase(ci.getSimpleName());
    }

    /** 解析字段上的 @Column 注解 */
    private TableInfo.Column parseColumn(FieldInfo fi, ClassInfo ci) {
        String fieldName = fi.getName();
        if (fieldName == null || fieldName.equals("serialVersionUID")) return null;

        TableInfo.Column col = new TableInfo.Column();
        col.setFieldName(fieldName);
        col.setJavaType(fi.getType());

        // 默认列名：字段名转蛇形
        col.setColumnName(toSnakeCase(fieldName));

        // 默认 SQL 类型：Java 类型映射
        col.setSqlType(mapJavaToSql(fi.getType()));

        for (String ann : fi.getAnnotations()) {
            String clean = ann.replace("@", "");

            // @Column(name = "xxx")
            Matcher cm = COLUMN_NAME.matcher(ann);
            if (cm.find()) col.setColumnName(cm.group(1));

            // MyBatis-Plus @TableField("xxx")
            Matcher tf = TABLEFIELD_ANN.matcher(ann);
            if (tf.find()) col.setColumnName(tf.group(1));

            // @Column(length = xxx, nullable = false, unique = true)
            Matcher cp = COLUMN_PROPS.matcher(ann);
            if (cp.find()) {
                String props = cp.group(1);
                Matcher len = Pattern.compile("length\\s*=\\s*(\\d+)").matcher(props);
                if (len.find()) col.setLength(Integer.parseInt(len.group(1)));
                if (props.contains("nullable = false") || props.contains("nullable=false"))
                    col.setNullable(false);
                if (props.contains("unique = true") || props.contains("unique=true")) {
                    // 唯一索引标记
                }
            }

            // @Id
            if (clean.equals("Id")) {
                col.setPrimaryKey(true);
            }

            // MyBatis-Plus @TableId
            if (clean.startsWith("TableId")) {
                col.setPrimaryKey(true);
            }

            // @GeneratedValue(strategy = IDENTITY)
            Matcher gm = ID_GENERATED.matcher(ann);
            if (gm.find()) {
                col.setAutoIncrement("IDENTITY".equals(gm.group(1)));
            }

            // @TableLogic - MyBatis-Plus 逻辑删除
            if (clean.startsWith("TableLogic")) {
                col.setComment("逻辑删除");
            }

            // @Version
            if (clean.startsWith("Version")) {
                col.setComment("乐观锁");
            }
        }

        return col;
    }

    /** Java 类型 → SQL 类型 */
    private String mapJavaToSql(String javaType) {
        if (javaType == null) return "VARCHAR";
        String simple = javaType.contains(".") ? javaType.substring(javaType.lastIndexOf('.') + 1) : javaType;
        // 去掉泛型
        simple = simple.split("<")[0].trim();
        return JAVA_TO_SQL.getOrDefault(simple, "VARCHAR");
    }

    /** 驼峰 → 蛇形 */
    private String toSnakeCase(String camel) {
        if (camel == null || camel.isEmpty()) return camel;
        // 去掉尾部的 Entity / DO / VO / DTO / PO
        String base = camel.replaceAll("(Entity|DO|VO|DTO|PO|BO|POJO)$", "");
        if (base.isEmpty()) base = camel;
        // 驼峰 → 下划线
        String result = base.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
                .toLowerCase();
        return result;
    }

    /** 查找闭合标签 */
    private int findClosingTag(String content, int start, String tagName) {
        int depth = 1;
        String openTag = "<" + tagName;
        String closeTag = "</" + tagName + ">";
        int pos = start;
        while (depth > 0 && pos < content.length()) {
            int nextOpen = content.indexOf(openTag, pos);
            int nextClose = content.indexOf(closeTag, pos);
            if (nextClose < 0) return -1;
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                pos = nextOpen + openTag.length();
            } else {
                depth--;
                pos = nextClose + closeTag.length();
            }
        }
        return depth == 0 ? pos - closeTag.length() : -1;
    }

    /** 合并同名的表（Entity + resultMap 的信息合并） */
    private void mergeTables() {
        Map<String, TableInfo> merged = new LinkedHashMap<>();
        for (TableInfo ti : tables) {
            String key = ti.getTableName();
            if (merged.containsKey(key)) {
                TableInfo existing = merged.get(key);
                // 合并字段（去重）
                Set<String> existingCols = existing.getColumns().stream()
                        .map(TableInfo.Column::getColumnName)
                        .collect(Collectors.toSet());
                for (TableInfo.Column col : ti.getColumns()) {
                    if (!existingCols.contains(col.getColumnName())) {
                        existing.getColumns().add(col);
                    }
                }
                if (ti.getEntityClass() != null) existing.setEntityClass(ti.getEntityClass());
            } else {
                merged.put(key, ti);
            }
        }
        tables.clear();
        tables.addAll(merged.values());
    }

    public List<TableInfo> getTables() { return tables; }
}
