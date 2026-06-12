package com.projectassistant.relationship;

import com.projectassistant.model.ClassInfo;
import com.projectassistant.model.FieldInfo;
import com.projectassistant.model.MethodInfo;
import com.projectassistant.model.ProjectModel;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * 表关系分析器
 *
 * 从代码中自动推断表之间的关联关系（替代外键）：
 * - JPA 注解 (@OneToMany, @ManyToOne, @JoinColumn, @JoinTable)
 * - MyBatis XML (resultMap association/collection, SQL JOIN)
 * - Service 层逻辑推断
 */
public class RelationAnalyzer {

    private final ProjectModel project;
    private final String projectPath;
    /** Entity 类名（simpleName）→ 表名 */
    private final Map<String, String> entityToTable;
    /** 发现的所有表关系 */
    private final List<TableRelation> relations = new ArrayList<>();

    public RelationAnalyzer(ProjectModel project, Map<String, String> entityToTable) {
        this.project = project;
        this.projectPath = project.getProjectPath() != null ? project.getProjectPath() : "";
        this.entityToTable = entityToTable;
    }

    public List<TableRelation> getRelations() { return relations; }

    /** 执行所有关系分析 */
    public void analyze() {
        System.out.println("  [Relation] 分析表关联关系...");

        analyzeJpaEntities();
        analyzeMyBatisXml();
        inferFromCode();

        System.out.println("  [Relation] 发现 " + relations.size() + " 个表间关系");
    }

    // ───────── JPA 实体分析 ─────────

    private void analyzeJpaEntities() {
        for (ClassInfo cls : project.getClasses()) {
            String simpleName = cls.getSimpleName();
            if (!entityToTable.containsKey(simpleName)) continue;

            List<String> classAnns = cls.getAnnotations();
            if (classAnns == null || classAnns.isEmpty()) continue;

            // 检查或收集 @Table(name="xxx")
            String tableName = entityToTable.get(simpleName);

            for (FieldInfo field : cls.getFields()) {
                List<String> fieldAnns = field.getAnnotations();
                if (fieldAnns == null || fieldAnns.isEmpty()) continue;

                String annStr = String.join(" ", fieldAnns);
                TableRelation rel = parseJpaRelation(simpleName, tableName, field, annStr);
                if (rel != null) {
                    rel.discoveredFrom = "JPA";
                    rel.confidence = 5;
                    deduplicateAdd(rel);
                }
            }
        }
    }

    private TableRelation parseJpaRelation(String entitySimple, String entityTable,
                                            FieldInfo field, String annotations) {
        String lower = annotations.toLowerCase();
        String fieldName = field.getName();
        String fieldType = field.getType();

        boolean oneToMany = lower.contains("@onetomany");
        boolean manyToOne = lower.contains("@manytoone");
        boolean manyToMany = lower.contains("@manytomany");
        boolean oneToOne  = lower.contains("@onetoone");
        if (!oneToMany && !manyToOne && !manyToMany && !oneToOne) return null;

        TableRelation rel = new TableRelation();
        rel.sourceTable = entityTable;
        rel.confidence = 4;

        String joinColumnName = extractAnnotValue(annotations, "name", "JoinColumn");
        String referencedColumn = extractAnnotValue(annotations, "referencedColumnName", "JoinColumn");
        String joinTableName = extractAnnotValue(annotations, "name", "JoinTable");

        if (oneToMany) {
            rel.relationType = "ONE_TO_MANY";
            String targetTable = resolveTargetTable(annotations, fieldName, fieldType);
            if (targetTable == null || targetTable.equals(entityTable)) return null;
            rel.targetTable = targetTable;
            rel.sourceColumn = "id";
            rel.targetColumn = inferFkColumn(entityTable);
        } else if (manyToMany) {
            rel.relationType = "MANY_TO_MANY";
            String targetTable = resolveTargetTable(annotations, fieldName, fieldType);
            if (targetTable == null || targetTable.equals(entityTable)) return null;
            rel.targetTable = targetTable;
            // 解析中间表
            if (joinTableName != null && !joinTableName.isEmpty()) {
                rel.joinTable = joinTableName;
                String jc = extractAnnotValue(annotations, "name", "joinColumns");
                String ijc = extractAnnotValue(annotations, "inverseJoinColumns", "joinColumns");
                rel.sourceJoinColumn = jc != null ? jc : entitySimple.toLowerCase() + "_id";
                rel.targetJoinColumn = ijc != null ? ijc : findSimpleName(targetTable).toLowerCase() + "_id";
            } else {
                // 按约定：A_B
                rel.joinTable = toSnakeCase(entitySimple) + "_" + toSnakeCase(findSimpleName(targetTable));
                rel.sourceJoinColumn = entitySimple.toLowerCase() + "_id";
                rel.targetJoinColumn = findSimpleName(targetTable).toLowerCase() + "_id";
            }
            rel.sourceColumn = "id";
            rel.targetColumn = "id";
        } else if (manyToOne) {
            rel.relationType = "MANY_TO_ONE";
            String targetTable = resolveTargetTable(annotations, fieldName, fieldType);
            if (targetTable == null || targetTable.equals(entityTable)) return null;
            rel.targetTable = targetTable;
            rel.sourceColumn = (joinColumnName != null) ? joinColumnName : fieldName + "_id";
            rel.targetColumn = (referencedColumn != null) ? referencedColumn : "id";
        } else if (oneToOne) {
            rel.relationType = "ONE_TO_ONE";
            String targetTable = resolveTargetTable(annotations, fieldName, fieldType);
            if (targetTable == null || targetTable.equals(entityTable)) return null;
            rel.targetTable = targetTable;
            rel.sourceColumn = (joinColumnName != null) ? joinColumnName : fieldName + "_id";
            rel.targetColumn = (referencedColumn != null) ? referencedColumn : "id";
        }

        return rel;
    }

    private String resolveTargetTable(String annotations, String fieldName, String fieldType) {
        // targetEntity 属性
        String te = extractAnnotValue(annotations, "targetEntity");
        if (te != null) {
            String s = findSimpleName(te);
            if (entityToTable.containsKey(s)) return entityToTable.get(s);
        }
        // 字段类型
        if (fieldType != null) {
            String generic = extractGenericType(fieldType);
            if (generic != null) {
                if (entityToTable.containsKey(generic)) return entityToTable.get(generic);
            }
            if (entityToTable.containsKey(fieldType)) return entityToTable.get(fieldType);
        }
        // 字段名约定
        String base = fieldName.endsWith("List") ? fieldName.substring(0, fieldName.length()-4) : fieldName;
        String snakeBase = toSnakeCase(base);
        for (Map.Entry<String, String> e : entityToTable.entrySet()) {
            if (e.getKey().equalsIgnoreCase(base) || e.getKey().equalsIgnoreCase(snakeBase)) return e.getValue();
        }
        return null;
    }

    // ───────── MyBatis XML 分析 ─────────

    private void analyzeMyBatisXml() {
        if (projectPath == null || projectPath.isEmpty()) return;
        try {
            Files.walk(Paths.get(projectPath))
                .filter(p -> p.toString().endsWith(".xml"))
                .forEach(this::analyzeXmlFile);
        } catch (IOException e) {
            System.err.println("  ⚠️ MyBatis XML 扫描失败: " + e.getMessage());
        }
    }

    private void analyzeXmlFile(Path xmlPath) {
        try {
            String content = Files.readString(xmlPath);
            if (!content.contains("mapper") && !content.contains("resultMap")) return;

            // resultMap association / collection
            Pattern rmPattern = Pattern.compile(
                "<resultMap[^>]*id=\"([^\"]+)\"[^>]*type=\"([^\"]+)\"[^>]*>(.*?)</resultMap>",
                Pattern.DOTALL);
            Matcher rmMatcher = rmPattern.matcher(content);
            while (rmMatcher.find()) {
                String resultType = rmMatcher.group(2);
                String mapBody = rmMatcher.group(3);
                String sourceTable = xmlTypeToTable(resultType);
                if (sourceTable == null) continue;

                // 提取 association 和 collection
                // 尝试1: 有 column 属性的
                Pattern ac1 = Pattern.compile(
                    "<(association|collection)[^>]*property=\"([^\"]+)\"[^>]*column=\"([^\"]+)\"[^>]*select=\"([^\"]+)\"",
                    Pattern.DOTALL);
                Matcher m1 = ac1.matcher(mapBody);
                while (m1.find()) {
                    String property = m1.group(2);
                    String column = m1.group(3);
                    // select 指向另一个 mapper 方法，不容易反向找表
                    // 用 property 名推断目标表
                    String targetTable = guessTableFromField(property);
                    if (targetTable != null && !targetTable.equals(sourceTable)) {
                        addRel(sourceTable, column, targetTable, "id", "MYBATIS_XML", 4);
                    }
                }

                // 尝试2: 有 javaType 的
                Pattern ac2 = Pattern.compile(
                    "<(association|collection)[^>]*property=\"([^\"]+)\"[^>]*javaType=\"([^\"]+)\"",
                    Pattern.DOTALL);
                Matcher m2 = ac2.matcher(mapBody);
                while (m2.find()) {
                    String assocType = m2.group(1);
                    String property = m2.group(2);
                    String javaType = m2.group(3);
                    String targetTable = xmlTypeToTable(javaType);
                    if (targetTable != null && !targetTable.equals(sourceTable)) {
                        String rtype = "collection".equals(assocType) ? "ONE_TO_MANY" : "MANY_TO_ONE";
                        addRel(targetTable, inferFkColumn(sourceTable), sourceTable, "id", "MYBATIS_XML", 5)
                           .relationType = rtype;
                    }
                }
            }

            // SQL JOIN 推断
            analyzeSqlJoins(content);

        } catch (IOException e) {
            // skip
        }
    }

    private void analyzeSqlJoins(String content) {
        // JOIN tableA a ON a.col = b.col 或 JOIN tableA a ON a.col = tableB.col
        Pattern joinPattern = Pattern.compile(
            "join\\s+[`'\"]?([^\\s`'\"]+)[`'\"]?\\s+(?:as\\s+)?[`'\"]?(\\w+)[`'\"]?\\s+on\\s+" +
            "[`'\"]?(\\w+)[`'\"]?\\.([`'\"]?\\w+[`'\"]?)\\s*=\\s*" +
            "[`'\"]?(\\w+)[`'\"]?\\.([`'\"]?\\w+[`'\"]?)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = joinPattern.matcher(content);
        while (m.find()) {
            String table1 = m.group(1).replaceAll("[`'\"]", "");
            String alias1 = m.group(2).replaceAll("[`'\"]", "");
            String aliasOrTableA = m.group(3).replaceAll("[`'\"]", "");
            String col1 = m.group(4).replaceAll("[`'\"]", "");
            String aliasOrTableB = m.group(5).replaceAll("[`'\"]", "");
            String col2 = m.group(6).replaceAll("[`'\"]", "");

            // 确定实际表名
            String actualTable1 = alias1.equalsIgnoreCase(table1) ? table1 : resolveAlias(alias1, content);
            String actualTable2 = table1; // 默认与 JOIN 的表相同，但需要从 alias 推断
            if (aliasOrTableB.equals(alias1)) {
                // ON a.col = a.otherCol — 同一表
                continue;
            }
            actualTable2 = resolveAlias(aliasOrTableB, content);
            if (actualTable2 == null) actualTable2 = aliasOrTableB;

            if (actualTable1 != null && actualTable2 != null && !actualTable1.equals(actualTable2)) {
                addRel(actualTable1, col1, actualTable2, col2, "MYBATIS_XML", 4);
            }
        }
    }

    // ───────── 代码推理 ─────────

    private void inferFromCode() {
        for (ClassInfo cls : project.getClasses()) {
            String simple = cls.getSimpleName();
            if (!simple.endsWith("Service") && !simple.endsWith("ServiceImpl")) continue;

            for (MethodInfo method : cls.getMethods()) {
                List<String> called = method.getCalledMethods();
                if (called == null || called.size() < 2) continue;

                // 提取 mapper 调用
                Set<String> mappers = new HashSet<>();
                for (String call : called) {
                    Matcher matcher = Pattern.compile("(\\w+Mapper)\\.").matcher(call);
                    if (matcher.find()) mappers.add(matcher.group(1));
                }
                if (mappers.size() < 2) continue;

                // 同时调用的 mapper 之间可能存在关联
                List<String> ml = new ArrayList<>(mappers);
                for (int i = 0; i < ml.size(); i++) {
                    for (int j = i+1; j < ml.size(); j++) {
                        String t1 = mapperToTable(ml.get(i));
                        String t2 = mapperToTable(ml.get(j));
                        if (t1 != null && t2 != null && !t1.equals(t2)) {
                            addRel(t1, inferFkColumn(t2), t2, "id", "CODE_INFERENCE", 2);
                        }
                    }
                }
            }
        }
    }

    // ───────── 工具方法 ─────────

    private TableRelation addRel(String srcTable, String srcCol, String tgtTable, String tgtCol,
                                  String from, int confidence) {
        TableRelation rel = new TableRelation();
        rel.sourceTable = srcTable;
        rel.sourceColumn = srcCol;
        rel.targetTable = tgtTable;
        rel.targetColumn = tgtCol;
        rel.relationType = "INFERRED";
        rel.discoveredFrom = from;
        rel.confidence = confidence;
        deduplicateAdd(rel);
        return rel;
    }

    private void deduplicateAdd(TableRelation rel) {
        boolean exists = relations.stream().anyMatch(r ->
            r.sourceTable.equals(rel.sourceTable) && r.targetTable.equals(rel.targetTable)
            && r.sourceColumn.equals(rel.sourceColumn) && r.targetColumn.equals(rel.targetColumn));
        if (!exists) {
            relations.add(rel);
        } else {
            relations.stream()
                .filter(r -> r.sourceTable.equals(rel.sourceTable) && r.targetTable.equals(rel.targetTable))
                .forEach(r -> { if (r.confidence < rel.confidence) r.confidence = rel.confidence; });
        }
    }

    private String extractAnnotValue(String allAnns, String attr) {
        return extractAnnotValue(allAnns, attr, null);
    }

    private String extractAnnotValue(String allAnns, String attr, String annotHint) {
        if (allAnns == null || attr == null) return null;
        // 匹配: name="xxx" 或 name = "xxx"
        String pattern = (annotHint != null) ?
            "@" + annotHint + ".*?" + attr + "\\s*=\\s*\"([^\"]+)\"" :
            attr + "\\s*=\\s*\"([^\"]+)\"";
        Matcher m = Pattern.compile(pattern).matcher(allAnns);
        if (m.find()) return m.group(1);
        return null;
    }

    private String extractGenericType(String fieldType) {
        if (fieldType == null) return null;
        Matcher m = Pattern.compile("<\\s*(\\w+)\\s*>").matcher(fieldType);
        return m.find() ? m.group(1) : null;
    }

    private String findSimpleName(String qualified) {
        return qualified.contains(".") ? qualified.substring(qualified.lastIndexOf('.') + 1) : qualified;
    }

    private String guessTableFromField(String fieldName) {
        String base = fieldName.endsWith("List") ? fieldName.substring(0, fieldName.length()-4) : fieldName;
        String snake = toSnakeCase(base);
        for (Map.Entry<String, String> e : entityToTable.entrySet()) {
            String ek = e.getKey().toLowerCase().replace("_", "");
            if (snake.replace("_","").equals(ek) || base.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return "sys_" + snake;
    }

    private String xmlTypeToTable(String typeName) {
        if (typeName == null) return null;
        String simple = findSimpleName(typeName);
        if (entityToTable.containsKey(simple)) return entityToTable.get(simple);
        // 约定
        return "sys_" + toSnakeCase(simple);
    }

    private String resolveAlias(String alias, String sqlContent) {
        Pattern p = Pattern.compile("(?:from|join)\\s+(\\w+)\\s+" + Pattern.quote(alias) + "\\b",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sqlContent);
        if (m.find()) return m.group(1).replaceAll("[`'\"]", "");
        return alias;
    }

    private String mapperToTable(String mapperName) {
        String entity = mapperName.endsWith("Mapper") ?
            mapperName.substring(0, mapperName.length()-6) : mapperName;
        if (entityToTable.containsKey(entity)) return entityToTable.get(entity);
        return "sys_" + toSnakeCase(entity);
    }

    private String inferFkColumn(String table) {
        String[] parts = table.split("_");
        return parts.length >= 2 ? parts[parts.length-1] + "_id" : table + "_id";
    }

    public static String toSnakeCase(String camel) {
        if (camel == null || camel.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 生成关系 Markdown 报告 */
    public String toMarkdown() {
        if (relations.isEmpty()) return "（未发现表间关系）\n";
        StringBuilder md = new StringBuilder();
        md.append("## 表间关联关系\n\n");
        md.append("| 源表.源列 | 关系 | 目标表.目标列 | 发现方式 |\n|---|---|---|---|\n");
        for (TableRelation r : relations) {
            String typeIcon = switch (r.relationType) {
                case "ONE_TO_MANY" -> "1→N";
                case "MANY_TO_ONE" -> "N→1";
                case "MANY_TO_MANY" -> "M→N";
                case "ONE_TO_ONE"  -> "1→1";
                default -> r.joinTable != null ? "M→N(via)" : "关联";
            };
            String src = r.joinTable != null
                ? ("`" + r.sourceTable + "` → `" + r.joinTable + "`")
                : ("`" + r.sourceTable + "." + r.sourceColumn + "`");
            String tgt = r.joinTable != null
                ? ("`" + r.joinTable + "` → `" + r.targetTable + "`")
                : ("`" + r.targetTable + "." + r.targetColumn + "`");
            md.append("| ").append(src)
              .append(" | ").append(typeIcon)
              .append(" | ").append(tgt)
              .append(" | ").append(r.discoveredFrom)
              .append(" |\n");
        }
        md.append("\n");
        return md.toString();
    }

    /** 生成 ER 文本图 */
    public String toErText() {
        if (relations.isEmpty()) return "（无关系）\n";
        StringBuilder sb = new StringBuilder("```\n");
        Set<String> tables = new LinkedHashSet<>();
        for (TableRelation r : relations) {
            tables.add(r.sourceTable);
            tables.add(r.targetTable);
            if (r.joinTable != null) tables.add(r.joinTable);
        }
        for (String t : tables) sb.append("  [").append(t).append("]\n");
        sb.append("\n");
        for (TableRelation r : relations) {
            String arrow = switch (r.relationType) {
                case "ONE_TO_MANY" -> "──1→N──";
                case "MANY_TO_ONE" -> "──N→1──";
                case "MANY_TO_MANY" -> "──M→N──";
                default -> "──────";
            };
            sb.append("  ").append(r.sourceTable).append(" ").append(arrow).append(" ").append(r.targetTable);
            sb.append("  (").append(r.sourceColumn).append(" → ").append(r.targetColumn).append(")\n");
        }
        sb.append("```\n");
        return sb.toString();
    }
}
