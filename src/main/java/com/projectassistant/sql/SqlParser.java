package com.projectassistant.sql;

import com.projectassistant.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * SQL 理解引擎
 * 解析 MyBatis XML Mapper、JPA Entity、注解 SQL
 */
public class SqlParser {

    private final List<ClassInfo> classes;
    private final List<Path> xmlFiles;
    private final List<TableInfo> tables = new ArrayList<>();
    private final Map<String, String> mapperSql = new HashMap<>();
    private final Map<String, String> entityTableMap = new HashMap<>();
    private boolean hasMyBatis = false;
    private boolean hasJPA = false;

    // XML Mapper SQL 标签
    private static final Pattern XML_SQL_TAG = Pattern.compile(
            "<(select|insert|update|delete)\\s+[^>]*?id\\s*=\\s*\"([^\"]+)\"[^>]*?>\\s*" +
            "(.*?)\\s*</\\1>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // XML <sql> 片段
    private static final Pattern XML_SQL_FRAGMENT = Pattern.compile(
            "<sql\\s+[^>]*?id\\s*=\\s*\"([^\"]+)\"[^>]*?>\\s*(.*?)\\s*</sql>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // MyBatis 注解 SQL: @Select("sql"), @Insert("sql"), @Update("sql"), @Delete("sql")
    private static final Pattern ANNOTATION_SQL = Pattern.compile(
            "(Select|Insert|Update|Delete)\\(\\s*\"([^\"]*)\"\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ENTITY_TABLE = Pattern.compile(
            "@Table\\s*\\(\\s*(?:name\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern COLUMN_ANN = Pattern.compile(
            "@Column\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    public SqlParser(List<ClassInfo> classes) {
        this(classes, new ArrayList<>());
    }

    public SqlParser(List<ClassInfo> classes, List<Path> xmlFiles) {
        this.classes = classes;
        this.xmlFiles = xmlFiles != null ? xmlFiles : new ArrayList<>();
    }

    /**
     * 执行 SQL 扫描
     */
    public void scan() {
        System.out.println("  [SQL] 扫描数据库映射...");
        detectORM();
        scanJPAEntities();
        scanMyBatisMappers();
        scanInlineSQL();
        System.out.println("  [SQL] 发现 " + tables.size() + " 张表, "
                + mapperSql.size() + " 个 Mapper SQL");
    }

    private void detectORM() {
        for (ClassInfo ci : classes) {
            if (ci.getAnnotations().contains("Mapper") ||
                ci.getAnnotations().contains("Repository")) {
                for (String ann : ci.getAnnotations()) {
                    if (ann.startsWith("Mapper")) hasMyBatis = true;
                }
            }
            for (String ann : ci.getAnnotations()) {
                if (ann.startsWith("Entity") || ann.startsWith("Table")) hasJPA = true;
            }
        }
    }

    /**
     * 解析 JPA Entity 类 → 表结构
     */
    private void scanJPAEntities() {
        for (ClassInfo ci : classes) {
            String tableName = null;
            for (String ann : ci.getAnnotations()) {
                Matcher m = ENTITY_TABLE.matcher(ann);
                if (m.find()) {
                    tableName = m.group(1);
                    break;
                }
            }
            if (tableName == null && ci.getAnnotations().stream()
                    .anyMatch(a -> a.equals("Entity") || a.startsWith("Entity("))) {
                // 默认表名 = 类名小写下划线
                tableName = toSnakeCase(ci.getSimpleName());
            }
            if (tableName == null) continue;

            entityTableMap.put(ci.getFullyQualifiedName(), tableName);
            TableInfo table = new TableInfo();
            table.setTableName(tableName);
            table.setEntityClass(ci.getFullyQualifiedName());

            // 解析字段
            for (FieldInfo fi : ci.getFields()) {
                TableInfo.Column col = new TableInfo.Column();
                col.setFieldName(fi.getName());
                col.setJavaType(fi.getType());

                // @Column name
                String colName = null;
                for (String ann : fi.getAnnotations()) {
                    Matcher m = COLUMN_ANN.matcher(ann);
                    if (m.find()) { colName = m.group(1); break; }
                }
                col.setColumnName(colName != null ? colName : toSnakeCase(fi.getName()));

                // @Id
                boolean isId = fi.getAnnotations().stream()
                        .anyMatch(a -> a.equals("Id") || a.startsWith("Id("));
                col.setPrimaryKey(isId);

                // @GeneratedValue
                boolean autoGen = fi.getAnnotations().stream()
                        .anyMatch(a -> a.startsWith("GeneratedValue"));
                col.setAutoIncrement(autoGen);

                table.getColumns().add(col);
            }
            tables.add(table);
        }
    }

    /**
     * 解析 MyBatis Mapper：从注解 SQL + XML 文件中提取真实 SQL
     */
    private void scanMyBatisMappers() {
        // 1. 从注解中提取 SQL（@Select, @Insert, @Update, @Delete）
        for (ClassInfo ci : classes) {
            for (MethodInfo mi : ci.getMethods()) {
                for (String ann : mi.getAnnotations()) {
                    Matcher m = ANNOTATION_SQL.matcher(ann);
                    if (m.find()) {
                        String sql = m.group(2).trim();
                        String key = ci.getSimpleName() + "." + mi.getName();
                        sql = sql.replaceAll("\\s+", " ").trim();
                        mapperSql.put(key, sql);
                        hasMyBatis = true;
                    }
                }
            }
        }

        // 2. 从 XML 文件中提取 SQL
        Map<String, String> xmlFragments = new HashMap<>();
        for (Path xmlFile : xmlFiles) {
            try {
                String content = Files.readString(xmlFile);
                // 提取 <sql> 片段
                Matcher fm = XML_SQL_FRAGMENT.matcher(content);
                while (fm.find()) {
                    xmlFragments.put(fm.group(1), fm.group(2).trim());
                }
                // 提取 <select/insert/update/delete>
                Matcher m = XML_SQL_TAG.matcher(content);
                while (m.find()) {
                    String sqlType = m.group(1).toUpperCase();
                    String id = m.group(2);
                    String sql = m.group(3).trim();

                    // 递归展开 <include refid="..."/>（最多5层）
                    for (int depth = 0; depth < 5; depth++) {
                        String prev = sql;
                        for (Map.Entry<String, String> frag : xmlFragments.entrySet()) {
                            sql = sql.replaceAll(
                                    "<include\\s+[^>]*?refid\\s*=\\s*\"?" + Pattern.quote(frag.getKey()) + "\"?[^>]*?/>",
                                    Matcher.quoteReplacement(frag.getValue()));
                        }
                        if (sql.equals(prev)) break;
                    }
                    // 清理 XML 标签和多余空白
                    sql = sql.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                    // SQL 本身已含关键字（SELECT/INSERT/UPDATE/DELETE），不再重复加前缀
                    if (!sql.isEmpty()) {
                        mapperSql.put(id, sql);
                        hasMyBatis = true;
                    }
                }
            } catch (IOException e) {
                // 跳过无法读取的 XML
            }
        }
    }

    /**
     * 从方法名推断 SQL 类型
     */
    private String detectSqlType(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("select") || lower.startsWith("find") ||
            lower.startsWith("get") || lower.startsWith("query") ||
            lower.startsWith("list") || lower.startsWith("search") ||
            lower.startsWith("count") || lower.startsWith("exist"))
            return "SELECT";
        if (lower.startsWith("insert") || lower.startsWith("save") ||
            lower.startsWith("add") || lower.startsWith("create") ||
            lower.startsWith("persist"))
            return "INSERT";
        if (lower.startsWith("update") || lower.startsWith("modify") ||
            lower.startsWith("set") || lower.startsWith("change"))
            return "UPDATE";
        if (lower.startsWith("delete") || lower.startsWith("remove") ||
            lower.startsWith("drop") || lower.startsWith("clear"))
            return "DELETE";
        return null;
    }

    /**
     * 扫描内嵌 SQL 字符串
     */
    private void scanInlineSQL() {
        // 这里可以从源码行中抽取类似 .sql 字符串或 StringBuilder SQL
        // 目前简单处理，从类信息中提取
    }

    // ==================== 工具 ====================

    private String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2")
                    .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                    .toLowerCase();
    }

    // ==================== 对外输出 ====================

    public List<TableInfo> getTables() { return tables; }
    public Map<String, String> getMapperSql() { return mapperSql; }
    public Map<String, String> getEntityTableMap() { return entityTableMap; }
    public boolean hasMyBatis() { return hasMyBatis; }
    public boolean hasJPA() { return hasJPA; }
}
