package com.projectassistant.sql;

import java.util.*;

/**
 * 数据库表信息
 */
public class TableInfo {
    private String tableName;
    private String entityClass;
    private String comment;
    private List<Column> columns = new ArrayList<>();

    public String getTableName() { return tableName; }
    public void setTableName(String t) { this.tableName = t; }
    public String getEntityClass() { return entityClass; }
    public void setEntityClass(String e) { this.entityClass = e; }
    public String getComment() { return comment; }
    public void setComment(String c) { this.comment = c; }
    public List<Column> getColumns() { return columns; }

    /** 获取主键列 */
    public List<Column> getPrimaryKeys() {
        return columns.stream().filter(Column::isPrimaryKey).toList();
    }

    @Override
    public String toString() {
        return tableName + " (" + columns.size() + " cols)";
    }

    public static class Column {
        private String columnName;
        private String fieldName;
        private String javaType;
        private String sqlType = "VARCHAR";
        private int length = 255;
        private boolean primaryKey;
        private boolean autoIncrement;
        private boolean nullable = true;
        private String defaultValue;
        private String comment;

        public String getColumnName() { return columnName; }
        public void setColumnName(String c) { this.columnName = c; }
        public String getFieldName() { return fieldName; }
        public void setFieldName(String f) { this.fieldName = f; }
        public String getJavaType() { return javaType; }
        public void setJavaType(String j) { this.javaType = j; }
        public String getSqlType() { return sqlType; }
        public void setSqlType(String s) { this.sqlType = s; }
        public int getLength() { return length; }
        public void setLength(int l) { this.length = l; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean p) { this.primaryKey = p; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean a) { this.autoIncrement = a; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean n) { this.nullable = n; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String d) { this.defaultValue = d; }
        public String getComment() { return comment; }
        public void setComment(String c) { this.comment = c; }
    }
}
