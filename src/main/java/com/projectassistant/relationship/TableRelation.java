package com.projectassistant.relationship;

/**
 * 表间关联关系（替代外键）
 */
public class TableRelation {
    public String sourceTable;          // 源表（如 sys_user）
    public String targetTable;          // 目标表（如 sys_role）
    public String sourceColumn;         // 源表列（如 role_id）
    public String targetColumn;         // 目标表列（如 id）
    public String relationType;         // MANY_TO_ONE / ONE_TO_MANY / MANY_TO_MANY / ONE_TO_ONE
    public String joinTable;            // 中间表（MANY_TO_MANY 时）
    public String sourceJoinColumn;     // 中间表指向源表的列
    public String targetJoinColumn;     // 中间表指向目标表的列
    public String discoveredFrom;       // JPA / MYBATIS_XML / CODE_INFERENCE
    public int confidence;              // 置信度 1-5

    /** 输出可读的关联描述 */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceTable).append(".").append(sourceColumn);
        sb.append(" → ").append(targetTable).append(".").append(targetColumn);
        if (joinTable != null) {
            sb.append(" (via ").append(joinTable).append(")");
        }
        sb.append(" [").append(relationType).append("]");
        return sb.toString();
    }
}
