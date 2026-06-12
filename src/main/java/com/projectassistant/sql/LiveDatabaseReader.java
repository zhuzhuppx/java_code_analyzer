package com.projectassistant.sql;

import java.sql.*;
import java.util.*;

/**
 * 实时数据库连接读取器
 * 通过 JDBC 连接数据库，读取表结构、字段、主键、索引、外键等信息
 */
public class LiveDatabaseReader {

    // 显式注册常见 JDBC 驱动（解决 fat JAR 中 SPI 文件被覆盖问题）
    static {
        registerDriver("com.mysql.cj.jdbc.Driver", "MySQL");
        registerDriver("org.postgresql.Driver", "PostgreSQL");
        registerDriver("org.h2.Driver", "H2");
    }

    private static void registerDriver(String className, String label) {
        try {
            java.sql.Driver driver = (java.sql.Driver) Class.forName(className)
                    .getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
            System.err.println("  [DB] Registered: " + label);
        } catch (Exception e) { /* not available */ }
    }

    private final String url;
    private final String user;
    private final String password;

    /** 数据库类型识别 */
    private String dbType;

    public LiveDatabaseReader(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * 读取数据库所有表的结构
     */
    public DatabaseSchema readSchema() throws SQLException {
        DatabaseSchema schema = new DatabaseSchema();
        schema.url = url;

        // 自动补充 MySQL 常用参数（SSL、时区）
        String connUrl = autoFixUrl(url);

        try (Connection conn = DriverManager.getConnection(connUrl, user, password)) {
            // ⚠️ 强制只读，禁止任何 INSERT/UPDATE/DELETE/DDL
            conn.setReadOnly(true);

            DatabaseMetaData meta = conn.getMetaData();
            schema.dbProduct = meta.getDatabaseProductName();
            schema.dbVersion = meta.getDatabaseProductVersion();
            dbType = schema.dbProduct.toLowerCase();

            // 获取所有表
            String[] types = {"TABLE", "VIEW"};
            try (ResultSet tables = meta.getTables(null, null, "%", types)) {
                while (tables.next()) {
                    String catalog = tables.getString("TABLE_CAT");
                    String schemaName = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    String tableType = tables.getString("TABLE_TYPE");

                    TableSchema ts = new TableSchema();
                    ts.catalog = catalog;
                    ts.schema = schemaName;
                    ts.name = tableName;
                    ts.type = tableType;
                    ts.comment = getTableComment(meta, catalog, schemaName, tableName);

                    // 读取列
                    readColumns(meta, catalog, schemaName, tableName, ts);

                    // 读取主键
                    readPrimaryKeys(meta, catalog, schemaName, tableName, ts);

                    // 读取索引
                    readIndexes(meta, catalog, schemaName, tableName, ts);

                    // 读取外键
                    readForeignKeys(meta, catalog, schemaName, tableName, ts);

                    schema.tables.add(ts);
                }
            }
        }

        return schema;
    }

    private String getTableComment(DatabaseMetaData meta, String catalog, String schema, String table) {
        // MySQL 特有的表注释读取
        if (dbType.contains("mysql")) {
            String sql = "SELECT table_comment FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_name = ?";
            try (PreparedStatement ps = meta.getConnection().prepareStatement(sql)) {
                ps.setString(1, catalog != null ? catalog : schema);
                ps.setString(2, table);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String c = rs.getString("table_comment");
                        if (c != null && !c.isEmpty()) return c;
                    }
                }
            } catch (SQLException ignored) {}
        }
        return "";
    }

    private void readColumns(DatabaseMetaData meta, String catalog, String schema, String table,
                             TableSchema ts) throws SQLException {
        try (ResultSet cols = meta.getColumns(catalog, schema, table, "%")) {
            while (cols.next()) {
                ColumnSchema c = new ColumnSchema();
                c.name = cols.getString("COLUMN_NAME");
                c.jdbcType = cols.getString("TYPE_NAME");
                c.sqlType = cols.getInt("DATA_TYPE");
                c.size = cols.getInt("COLUMN_SIZE");
                c.decimalDigits = cols.getInt("DECIMAL_DIGITS");
                c.nullable = cols.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                c.defaultValue = cols.getString("COLUMN_DEF");
                c.autoIncrement = "YES".equalsIgnoreCase(cols.getString("IS_AUTOINCREMENT"));
                c.comment = cols.getString("REMARKS");
                c.position = cols.getInt("ORDINAL_POSITION");
                ts.columns.add(c);
            }
        }
        // 排序
        ts.columns.sort(Comparator.comparingInt(c -> c.position));
    }

    private void readPrimaryKeys(DatabaseMetaData meta, String catalog, String schema, String table,
                                 TableSchema ts) throws SQLException {
        try (ResultSet pk = meta.getPrimaryKeys(catalog, schema, table)) {
            while (pk.next()) {
                String colName = pk.getString("COLUMN_NAME");
                ts.primaryKeys.add(colName);
                // 标记对应列为主键
                for (ColumnSchema c : ts.columns) {
                    if (c.name.equals(colName)) {
                        c.primaryKey = true;
                        break;
                    }
                }
            }
        }
    }

    private void readIndexes(DatabaseMetaData meta, String catalog, String schema, String table,
                             TableSchema ts) throws SQLException {
        Map<String, IndexSchema> idxMap = new LinkedHashMap<>();
        try (ResultSet idx = meta.getIndexInfo(catalog, schema, table, false, false)) {
            while (idx.next()) {
                String idxName = idx.getString("INDEX_NAME");
                if (idxName == null) continue;
                String colName = idx.getString("COLUMN_NAME");
                boolean unique = !idx.getBoolean("NON_UNIQUE");
                short type = idx.getShort("TYPE");

                IndexSchema is = idxMap.computeIfAbsent(idxName, k -> {
                    IndexSchema i = new IndexSchema();
                    i.name = idxName;
                    i.unique = unique;
                    i.type = type;
                    return i;
                });
                if (colName != null) is.columns.add(colName);
            }
        }
        ts.indexes.addAll(idxMap.values());
    }

    private void readForeignKeys(DatabaseMetaData meta, String catalog, String schema, String table,
                                 TableSchema ts) throws SQLException {
        try (ResultSet fk = meta.getImportedKeys(catalog, schema, table)) {
            while (fk.next()) {
                ForeignKeySchema fks = new ForeignKeySchema();
                fks.fkColumn = fk.getString("FKCOLUMN_NAME");
                fks.pkTableCatalog = fk.getString("PKTABLE_CAT");
                fks.pkTableSchema = fk.getString("PKTABLE_SCHEM");
                fks.pkTable = fk.getString("PKTABLE_NAME");
                fks.pkColumn = fk.getString("PKCOLUMN_NAME");
                fks.updateRule = fk.getShort("UPDATE_RULE");
                fks.deleteRule = fk.getShort("DELETE_RULE");
                fks.fkName = fk.getString("FK_NAME");
                ts.foreignKeys.add(fks);
            }
        }
    }

    /** 自动补全 JDBC URL 的常用参数 */
    private static String autoFixUrl(String raw) {
        if (raw.startsWith("jdbc:mysql://")) {
            // 确保有 useSSL / serverTimezone 等常用参数
            String lower = raw.toLowerCase();
            if (!lower.contains("usessl=") && !lower.contains("sslmode=")) {
                raw += raw.contains("?") ? "&" : "?";
                raw += "useSSL=false&allowPublicKeyRetrieval=true";
                lower = raw.toLowerCase();
            }
            if (!lower.contains("servertimezone=") && !lower.contains("connectiontimezone=")) {
                raw += "&serverTimezone=Asia/Shanghai";
            }
            // 连接超时 5 秒，避免卡住
            if (!lower.contains("connecttimeout=")) {
                raw += "&connectTimeout=5000";
            }
            // 读取超时 10 秒
            if (!lower.contains("sockettimeout=")) {
                raw += "&socketTimeout=10000";
            }
        }
        return raw;
    }

    /** 将数据库结构输出为 Markdown 知识库格式 */
    public String toMarkdown(DatabaseSchema schema) {
        StringBuilder md = new StringBuilder();
        md.append("# 数据库结构\n\n");
        md.append("> 由 Java老狗 实时读取 | 数据库: ").append(schema.dbProduct)
          .append(" ").append(schema.dbVersion).append("\n\n");

        md.append("| 属性 | 值 |\n|---|---|\n");
        md.append("| 数据库 | ").append(schema.dbProduct).append(" |\n");
        md.append("| 版本 | ").append(schema.dbVersion).append(" |\n");
        md.append("| 表/视图数 | ").append(schema.tables.size()).append(" |\n\n");

        for (TableSchema t : schema.tables) {
            md.append("## ").append(t.name).append("\n\n");
            md.append("> 类型: ").append(t.type);
            if (t.comment != null && !t.comment.isEmpty()) md.append(" | 说明: ").append(t.comment);
            md.append("\n\n");

            // 字段表
            md.append("| # | 列名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 自增 | 说明 |\n");
            md.append("|---|---|---|---|---|---|---|---|---|\n");
            for (ColumnSchema c : t.columns) {
                String sizeStr = c.size > 0 ? String.valueOf(c.size) : "";
                if (c.decimalDigits > 0) sizeStr += "," + c.decimalDigits;
                md.append("| ").append(c.position)
                  .append(" | `").append(c.name).append("`")
                  .append(" | ").append(c.jdbcType)
                  .append(" | ").append(sizeStr)
                  .append(" | ").append(c.nullable ? "YES" : "NO")
                  .append(" | ").append(c.defaultValue != null ? "`" + c.defaultValue + "`" : "")
                  .append(" | ").append(c.primaryKey ? "PK" : "")
                  .append(" | ").append(c.autoIncrement ? "自增" : "")
                  .append(" | ").append(c.comment != null ? c.comment : "")
                  .append(" |\n");
            }
            md.append("\n");

            // 索引
            if (!t.indexes.isEmpty()) {
                md.append("### 索引\n\n");
                md.append("| 索引名 | 唯一 | 列 |\n|---|---|---|\n");
                for (IndexSchema idx : t.indexes) {
                    md.append("| `").append(idx.name).append("`")
                      .append(" | ").append(idx.unique ? "UNIQUE" : "")
                      .append(" | ").append(String.join(", ", idx.columns))
                      .append(" |\n");
                }
                md.append("\n");
            }

            // 外键
            if (!t.foreignKeys.isEmpty()) {
                md.append("### 外键\n\n");
                md.append("| 外键名 | 列 | 引用表 | 引用列 | 更新规则 | 删除规则 |\n|---|---|---|---|---|---|\n");
                for (ForeignKeySchema fk : t.foreignKeys) {
                    String upd = ruleName(fk.updateRule);
                    String del = ruleName(fk.deleteRule);
                    md.append("| `").append(fk.fkName != null ? fk.fkName : "").append("`")
                      .append(" | `").append(fk.fkColumn).append("`")
                      .append(" | ").append(fk.pkTable)
                      .append(" | `").append(fk.pkColumn).append("`")
                      .append(" | ").append(upd)
                      .append(" | ").append(del)
                      .append(" |\n");
                }
                md.append("\n");
            }
        }

        return md.toString();
    }

    private String ruleName(short rule) {
        return switch (rule) {
            case DatabaseMetaData.importedKeyNoAction -> "NO ACTION";
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            case DatabaseMetaData.importedKeySetNull -> "SET NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
            default -> String.valueOf(rule);
        };
    }

    // ────── 内部数据模型 ──────

    public static class DatabaseSchema {
        public String url;
        public String dbProduct;
        public String dbVersion;
        public List<TableSchema> tables = new ArrayList<>();
    }

    public static class TableSchema {
        public String catalog;
        public String schema;
        public String name;
        public String type;
        public String comment;
        public List<ColumnSchema> columns = new ArrayList<>();
        public List<String> primaryKeys = new ArrayList<>();
        public List<IndexSchema> indexes = new ArrayList<>();
        public List<ForeignKeySchema> foreignKeys = new ArrayList<>();
    }

    public static class ColumnSchema {
        public String name;
        public String jdbcType;
        public int sqlType;
        public int size;
        public int decimalDigits;
        public boolean nullable;
        public String defaultValue;
        public boolean autoIncrement;
        public String comment;
        public int position;
        public boolean primaryKey;
    }

    public static class IndexSchema {
        public String name;
        public boolean unique;
        public short type;
        public List<String> columns = new ArrayList<>();
    }

    public static class ForeignKeySchema {
        public String fkColumn;
        public String pkTableCatalog;
        public String pkTableSchema;
        public String pkTable;
        public String pkColumn;
        public short updateRule;
        public short deleteRule;
        public String fkName;
    }
}
