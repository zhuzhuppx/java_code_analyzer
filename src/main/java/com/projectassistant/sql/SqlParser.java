package com.projectassistant.sql;

import com.projectassistant.model.*;
import java.util.*;
import java.util.regex.*;

/**
 * SQL 理解引擎
 * 解析 MyBatis XML Mapper、JPA Entity、原生 SQL
 */
public class SqlParser {

    private final List<ClassInfo> classes;
    private final List<TableInfo> tables = new ArrayList<>();
    private final Map<String, String> mapperSql = new HashMap<>();  // mapper方法 -> SQL
    private final Map<String, String> entityTableMap = new HashMap<>(); // entity类 -> 表名
    private boolean hasMyBatis = false;
    private boolean hasJPA = false;

    // SQL 正则
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "(SELECT\\s+.+?\\s+FROM\\s+[^\\s]+(?:\\s+WHERE\\s+[^;]*)?)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TABLE_NAME = Pattern.compile(
            "(?:FROM|JOIN|INTO|UPDATE|TABLE)\\s+([`\"']?)(\\w+)\\1",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTITY_TABLE = Pattern.compile(
            "@Table\\s*\\(\\s*(?:name\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern COLUMN_ANN = Pattern.compile(
            "@Column\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern MYBATIS_SELECT = Pattern.compile(
            "<select\\s+[^>]*id\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*(.*?)\\s*</select>",
            Pattern.DOTALL);
    private static final Pattern MYBATIS_INSERT = Pattern.compile(
            "<insert\\s+[^>]*id\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*(.*?)\\s*</insert>",
            Pattern.DOTALL);
    private static final Pattern MYBATIS_UPDATE = Pattern.compile(
            "<update\\s+[^>]*id\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*(.*?)\\s*</update>",
            Pattern.DOTALL);
    private static final Pattern MYBATIS_DELETE = Pattern.compile(
            "<delete\\s+[^>]*id\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*(.*?)\\s*</delete>",
            Pattern.DOTALL);

    public SqlParser(List<ClassInfo> classes) {
        this.classes = classes;
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
     * 解析 MyBatis XML Mapper（从源码原始内容中提取）
     */
    private void scanMyBatisMappers() {
        for (ClassInfo ci : classes) {
            boolean isMapper = ci.getAnnotations().stream()
                    .anyMatch(a -> a.equals("Mapper") || a.startsWith("Mapper("));
            if (!isMapper && !ci.getSimpleName().endsWith("Mapper")) continue;

            // 从类的接口方法名 + 可能的 XML 内嵌 SQL 提取
            for (MethodInfo mi : ci.getMethods()) {
                String sqlType = detectSqlType(mi.getName());
                if (sqlType != null) {
                    mapperSql.put(ci.getSimpleName() + "." + mi.getName(),
                            sqlType + " " + mi.getReturnType() + "(" +
                            String.join(", ", mi.getParameters()) + ")");
                }
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
