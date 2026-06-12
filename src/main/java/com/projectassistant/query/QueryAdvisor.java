package com.projectassistant.query;

import com.projectassistant.relationship.TableRelation;
import com.projectassistant.sql.LiveDatabaseReader.*;
import java.util.*;
import java.util.regex.*;

/**
 * 自然语言 → SQL 查询建议器
 *
 * 利用已发现的表关系 + 数据库列信息，将用户问题转为可执行的 SQL
 */
public class QueryAdvisor {

    private final List<TableRelation> relations;
    private final DatabaseSchema dbSchema;
    private final Map<String, List<ColumnSchema>> tableColumns = new LinkedHashMap<>();
    private final Map<String, String> tableComments = new LinkedHashMap<>();
    private static final Map<Pattern, String> AGG_PATTERNS = new LinkedHashMap<>();

    static {
        AGG_PATTERNS.put(Pattern.compile("(count|数量|总数|多少个|条数|次数)"), "COUNT");
        AGG_PATTERNS.put(Pattern.compile("(sum|合计|总和|总金额|总额|总收入)"), "SUM");
        AGG_PATTERNS.put(Pattern.compile("(avg|平均|均值|平均数)"), "AVG");
        AGG_PATTERNS.put(Pattern.compile("(max|最高|最大|最多)"), "MAX");
        AGG_PATTERNS.put(Pattern.compile("(min|最低|最小|最少)"), "MIN");
    }

    public QueryAdvisor(List<TableRelation> relations, DatabaseSchema dbSchema) {
        this.relations = relations != null ? relations : List.of();
        this.dbSchema = dbSchema;
        if (dbSchema != null) {
            for (TableSchema ts : dbSchema.tables) {
                tableColumns.put(ts.name, ts.columns);
                tableComments.put(ts.name, ts.comment != null ? ts.comment : "");
            }
        }
    }

    public QuerySuggestion suggest(String question) {
        if (dbSchema == null) {
            return new QuerySuggestion("-- 未连接数据库，无法生成查询\n-- 请先在左侧连接数据库", List.of(), 0);
        }
        String q = question.trim();
        if (q.isEmpty()) return new QuerySuggestion("-- 请输入问题", List.of(), 0);

        String mainTable = identifyMainTable(q);
        List<String> joinTables = identifyJoinTables(q, mainTable);
        List<SelectField> selectFields = identifySelectFields(q, mainTable, joinTables);
        List<WhereClause> whereClauses = identifyWhereClauses(q, mainTable);
        String groupBy = identifyGroupBy(q, selectFields);
        String orderBy = identifyOrderBy(q, selectFields);
        String limit = identifyLimit(q);

        String sql = buildSql(mainTable, joinTables, selectFields, whereClauses, groupBy, orderBy, limit);
        String explanation = buildExplanation(q, mainTable, joinTables, whereClauses);

        return new QuerySuggestion(sql, List.of(explanation), 1);
    }

    private String identifyMainTable(String q) {
        String lower = q.toLowerCase();

        // 业务词映射
        Map<String, String> common = Map.ofEntries(
            Map.entry("订单", "sys_order"), Map.entry("用户", "sys_user"),
            Map.entry("角色", "sys_role"), Map.entry("权限", "sys_permission"),
            Map.entry("商品", "sys_goods"), Map.entry("产品", "sys_product"),
            Map.entry("分类", "sys_category"), Map.entry("日志", "sys_log"),
            Map.entry("文章", "sys_article"), Map.entry("评论", "sys_comment"),
            Map.entry("配置", "sys_config"), Map.entry("部门", "sys_dept"),
            Map.entry("菜单", "sys_menu"), Map.entry("字典", "sys_dict")
        );
        for (var e : common.entrySet()) {
            if (lower.contains(e.getKey()) && tableColumns.containsKey(e.getValue())) return e.getValue();
        }

        // 表名匹配
        for (String tn : tableColumns.keySet()) {
            String readable = tn.replace("sys_", "").replace("_", " ");
            if (lower.contains(readable) || lower.contains(tn.toLowerCase())) return tn;
        }

        return tableColumns.isEmpty() ? "unknown" : tableColumns.keySet().iterator().next();
    }

    private List<String> identifyJoinTables(String q, String mainTable) {
        Set<String> joined = new LinkedHashSet<>();
        String lower = q.toLowerCase();

        for (TableRelation rel : relations) {
            String other = null;
            if (rel.sourceTable.equals(mainTable)) other = rel.targetTable;
            else if (rel.targetTable.equals(mainTable)) other = rel.sourceTable;
            if (other == null) continue;

            String readable = other.replace("sys_", "").replace("_", " ");
            if (lower.contains(readable) || lower.contains(other.replace("sys_", "").replace("_", ""))) {
                joined.add(other);
            }
        }

        // 聚合查询自动关联
        boolean needAgg = q.matches(".*(每个|各|按.*分组|数量|总数|统计|count|sum|avg).*");
        if (needAgg && joined.isEmpty()) {
            for (TableRelation rel : relations) {
                if (rel.sourceTable.equals(mainTable)) joined.add(rel.targetTable);
                if (rel.targetTable.equals(mainTable)) joined.add(rel.sourceTable);
            }
        }

        return new ArrayList<>(joined);
    }

    private List<SelectField> identifySelectFields(String q, String mainTable, List<String> joinTables) {
        List<SelectField> fields = new ArrayList<>();
        String lower = q.toLowerCase();
        boolean hasAgg = false;

        for (var entry : AGG_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(lower).find()) {
                hasAgg = true;
                String target = findAggColumn(lower, mainTable, joinTables);
                if (target != null) {
                    fields.add(new SelectField(entry.getValue() + "(" + target + ")", entry.getValue() + "(" + target + ")", true));
                    break;
                }
            }
        }

        // 标识列
        if (!hasAgg) {
            for (String table : concat(mainTable, joinTables)) {
                List<ColumnSchema> cols = tableColumns.get(table);
                if (cols == null) continue;
                for (ColumnSchema c : cols) {
                    String n = c.name.toLowerCase();
                    if (n.contains("name") || n.contains("title") || n.contains("nick") || n.contains("username")) {
                        fields.add(new SelectField(table + "." + c.name, c.name, false));
                        break;
                    }
                }
            }
        }

        if (fields.isEmpty()) {
            fields.add(new SelectField(mainTable + ".*", "*", false));
        }

        return fields;
    }

    private String findAggColumn(String lower, String mainTable, List<String> joinTables) {
        Map<String, String> hints = Map.ofEntries(
            Map.entry("金额", "amount"), Map.entry("余额", "balance"),
            Map.entry("价格", "price"), Map.entry("数量", "quantity"),
            Map.entry("积分", "points"), Map.entry("年龄", "age"),
            Map.entry("分数", "score"), Map.entry("收入", "income"),
            Map.entry("销售额", "amount"), Map.entry("销量", "quantity")
        );
        for (var h : hints.entrySet()) {
            if (lower.contains(h.getKey())) {
                for (String table : concat(mainTable, joinTables)) {
                    String col = findColumn(table, h.getValue());
                    if (col != null) return col;
                }
            }
        }
        // 默认主键 COUNT
        String pk = findPrimaryKey(mainTable);
        return pk != null ? pk : mainTable + ".id";
    }

    private List<WhereClause> identifyWhereClauses(String q, String mainTable) {
        List<WhereClause> wheres = new ArrayList<>();
        String lower = q.toLowerCase();

        if (lower.contains("今天")) wheres.add(new WhereClause(findDateColumn(mainTable) + " >= CURDATE()", "今天"));
        if (lower.contains("昨天")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND "
            + findDateColumn(mainTable) + " < CURDATE()", "昨天"));
        if (lower.contains("本月") || lower.contains("这个月")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_FORMAT(CURDATE(),'%Y-%m-01')", "本月"));
        if (lower.contains("上月") || lower.contains("上个月")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 1 MONTH) AND "
            + findDateColumn(mainTable) + " < DATE_FORMAT(CURDATE(),'%Y-%m-01')", "上月"));
        if (lower.contains("今年")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_FORMAT(CURDATE(),'%Y-01-01')", "今年"));
        if (lower.contains("最近7天") || lower.contains("最近一周")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)", "最近7天"));
        if (lower.contains("最近30天") || lower.contains("最近一个月")) wheres.add(new WhereClause(
            findDateColumn(mainTable) + " >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)", "最近30天"));

        return wheres;
    }

    private String identifyGroupBy(String q, List<SelectField> fields) {
        if (q.matches(".*(每个|各|按|分组|分别).*")) {
            for (SelectField f : fields) {
                if (!f.isAgg) return f.expression;
            }
        }
        return null;
    }

    private String identifyOrderBy(String q, List<SelectField> fields) {
        boolean desc = q.matches(".*(最多|最高|最大|排行|排名|top|desc|倒序).*");
        boolean asc = q.matches(".*(最少|最低|最小|asc|升序).*");
        for (SelectField f : fields) {
            if (f.isAgg) return f.alias + (desc ? " DESC" : asc ? " ASC" : " DESC");
        }
        return null;
    }

    private String identifyLimit(String q) {
        if (q.contains("前10") || q.contains("前十")) return "10";
        if (q.contains("前5") || q.contains("前五")) return "5";
        Matcher m = Pattern.compile("前(\\d+)").matcher(q);
        return m.find() ? m.group(1) : null;
    }

    private String buildSql(String main, List<String> joins, List<SelectField> fields,
                            List<WhereClause> wheres, String gb, String ob, String lim) {
        StringBuilder sql = new StringBuilder("SELECT\n");
        for (int i = 0; i < fields.size(); i++) {
            sql.append("    ").append(fields.get(i).expression);
            if (i < fields.size()-1) sql.append(",");
            sql.append("\n");
        }
        sql.append("FROM ").append(main).append("\n");

        for (String jt : joins) {
            String jc = buildJoin(main, jt);
            if (jc != null) sql.append("    ").append(jc).append("\n");
        }

        if (!wheres.isEmpty()) {
            sql.append("WHERE\n");
            for (int i = 0; i < wheres.size(); i++) {
                sql.append("    ").append(wheres.get(i).condition);
                if (i < wheres.size()-1) sql.append(" AND\n");
                sql.append("\n");
            }
        }
        if (gb != null) sql.append("GROUP BY ").append(gb).append("\n");
        if (ob != null) sql.append("ORDER BY ").append(ob).append("\n");
        if (lim != null) sql.append("LIMIT ").append(lim).append("\n");
        sql.append(";\n");
        return sql.toString();
    }

    private String buildJoin(String main, String join) {
        for (TableRelation r : relations) {
            if (r.sourceTable.equals(main) && r.targetTable.equals(join))
                return "LEFT JOIN " + join + " ON " + main + "." + r.sourceColumn + " = " + join + "." + r.targetColumn;
            if (r.targetTable.equals(main) && r.sourceTable.equals(join))
                return "LEFT JOIN " + join + " ON " + main + "." + r.targetColumn + " = " + join + "." + r.sourceColumn;
        }
        String fk = join.replace("sys_", "").replace("_", "") + "_id";
        return "LEFT JOIN " + join + " ON " + main + "." + fk + " = " + join + ".id";
    }

    private String findPrimaryKey(String t) {
        List<ColumnSchema> cols = tableColumns.get(t);
        if (cols != null) for (ColumnSchema c : cols) if (c.primaryKey) return t + "." + c.name;
        return t + ".id";
    }

    private String findDateColumn(String t) {
        List<ColumnSchema> cols = tableColumns.get(t);
        if (cols != null) for (ColumnSchema c : cols) {
            String n = c.name.toLowerCase();
            if (n.contains("create") || n.contains("gmt") || n.contains("time") || n.contains("date"))
                return t + "." + c.name;
        }
        return t + ".create_time";
    }

    private String findColumn(String t, String... names) {
        List<ColumnSchema> cols = tableColumns.get(t);
        if (cols == null) return null;
        for (String n : names) for (ColumnSchema c : cols) if (c.name.equalsIgnoreCase(n)) return t + "." + c.name;
        return null;
    }

    private List<String> concat(String a, List<String> b) {
        List<String> r = new ArrayList<>();
        r.add(a); r.addAll(b);
        return r;
    }

    private String buildExplanation(String q, String main, List<String> joins, List<WhereClause> wheres) {
        StringBuilder sb = new StringBuilder("📊 查询说明：");
        sb.append("\n• 主表：`").append(main).append("`");
        if (tableComments.containsKey(main) && !tableComments.get(main).isEmpty())
            sb.append(" (").append(tableComments.get(main)).append(")");
        if (!joins.isEmpty()) {
            sb.append("\n• 关联表：");
            for (String j : joins) {
                sb.append("`").append(j).append("` ");
            }
        }
        if (!wheres.isEmpty()) {
            sb.append("\n• 过滤：");
            for (WhereClause w : wheres) sb.append(w.label).append(" ");
        }
        sb.append("\n• ⚠️ 此为代码推断生成的参考查询，执行前请审查");
        return sb.toString();
    }

    // ───────── 模型 ─────────
    public static class QuerySuggestion {
        public String sql;
        public List<String> explanations;
        public int confidence;
        public QuerySuggestion(String sql, List<String> ex, int c) {
            this.sql = sql; this.explanations = ex; this.confidence = c;
        }
    }

    static class SelectField {
        String expression, alias;
        boolean isAgg;
        SelectField(String e, String a, boolean ag) { expression=e; alias=a; isAgg=ag; }
    }

    static class WhereClause {
        String condition, label;
        WhereClause(String c, String l) { condition=c; label=l; }
    }
}
