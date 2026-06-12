package com.projectassistant.analyzer;

import com.projectassistant.model.*;
import com.projectassistant.spring.ApiEndpoint;
import com.projectassistant.sql.LiveDatabaseReader;
import com.projectassistant.sql.LiveDatabaseReader.*;
import com.projectassistant.sql.TableInfo;
import java.util.*;
import java.util.stream.*;

/**
 * 业务逻辑分析器
 *
 * 结合代码扫描 + 实时数据库结构，推导完整业务逻辑：
 *   API → Controller → Service → Repository → Entity → Table
 */
public class BusinessLogicAnalyzer {

    private final ProjectModel project;
    private LiveDatabaseReader.DatabaseSchema dbSchema;

    /** Entity 类名 → 匹配的数据库表名 */
    private final Map<String, String> entityToTable = new LinkedHashMap<>();
    /** 数据库表名 → 涉及的 API 端点 */
    private final Map<String, List<ApiEndpoint>> tableToApis = new LinkedHashMap<>();
    /** 数据库表名 → 涉及的 Service 类 */
    private final Map<String, List<String>> tableToServices = new LinkedHashMap<>();
    /** API 端点 → 涉及的数据库表 */
    private final Map<String, List<String>> apiToTables = new LinkedHashMap<>();

    public BusinessLogicAnalyzer(ProjectModel project) {
        this.project = project;
    }

    /**
     * 设置实时数据库结构（可选，不设置则使用代码推断的表结构）
     */
    public void setDatabaseSchema(LiveDatabaseReader.DatabaseSchema schema) {
        this.dbSchema = schema;
    }

    /**
     * 执行业务逻辑分析
     */
    public BusinessReport analyze() {
        // 1. 匹配 Entity → Table
        matchEntityToTable();

        // 2. 分析 Service 操作了哪些 Entity
        analyzeServiceTableRelations();

        // 3. 分析 API 操作了哪些 Table
        analyzeApiTableRelations();

        // 4. 构建完整报告
        return buildReport();
    }

    // ───────── 步骤 1: Entity → Table 匹配 ─────────

    private void matchEntityToTable() {
        for (ClassInfo ci : project.getClasses()) {
            String tableName = findTableForEntity(ci);
            if (tableName != null) {
                entityToTable.put(ci.getSimpleName(), tableName);
            }
        }
    }

    private String findTableForEntity(ClassInfo ci) {
        // 优先从 @Table(name="xxx") 注解提取
        for (String ann : ci.getAnnotations()) {
            String t = parseTableName(ann);
            if (t != null) return t;
        }
        // 其次从 @Entity 注解推断：User → user / sys_user
        boolean hasEntity = ci.getAnnotations().stream()
                .anyMatch(a -> a.startsWith("Entity"));
        if (hasEntity) {
            String inferred = camelToSnake(ci.getSimpleName());
            // 检查代码推断的表
            for (TableInfo ti : project.getDatabaseTables()) {
                if (ti.getTableName().equalsIgnoreCase(inferred)
                        || ti.getTableName().endsWith("_" + inferred)
                        || ti.getTableName().equalsIgnoreCase(ci.getSimpleName())) {
                    return ti.getTableName();
                }
            }
            // 检查实时数据库
            if (dbSchema != null) {
                for (TableSchema ts : dbSchema.tables) {
                    if (ts.name.equalsIgnoreCase(inferred)
                            || ts.name.endsWith("_" + inferred)
                            || ts.name.equalsIgnoreCase(ci.getSimpleName())) {
                        return ts.name;
                    }
                }
            }
            return inferred;  // 推断名，可能不精确
        }
        return null;
    }

    /** 从 @Table(name="xxx") 或 @TableName("xxx") 提取表名 */
    private String parseTableName(String annotation) {
        if (!annotation.startsWith("Table") && !annotation.startsWith("TableName")) return null;
        int idx = annotation.indexOf("name=");
        if (idx < 0) {
            // 位置参数: @Table("xxx")
            int start = annotation.indexOf('"');
            if (start >= 0) {
                int end = annotation.indexOf('"', start + 1);
                if (end > start) return annotation.substring(start + 1, end);
            }
            return null;
        }
        int start = annotation.indexOf('"', idx);
        if (start < 0) return null;
        int end = annotation.indexOf('"', start + 1);
        return (end > start) ? annotation.substring(start + 1, end) : null;
    }

    // ───────── 步骤 2: Service → Table 关联 ─────────

    private void analyzeServiceTableRelations() {
        for (ClassInfo ci : project.getClasses()) {
            String role = inferRole(ci);
            if (!"service".equals(role) && !"repository".equals(role)) continue;

            List<String> relatedTables = new ArrayList<>();
            // Service 中注入的 Repository → Entity → Table
            for (FieldInfo f : ci.getFields()) {
                String entityName = inferEntityFromField(f);
                if (entityName != null) {
                    String tbl = entityToTable.get(entityName);
                    if (tbl != null && !relatedTables.contains(tbl)) relatedTables.add(tbl);
                }
                // 直接从字段类型找表名
                String tblByType = findTableByTypeName(f.getType());
                if (tblByType != null && !relatedTables.contains(tblByType)) relatedTables.add(tblByType);
            }
            // 方法参数/返回值中涉及的 Entity
            for (MethodInfo m : ci.getMethods()) {
                String retEntity = inferEntityFromType(m.getReturnType());
                if (retEntity != null) {
                    String tbl = entityToTable.get(retEntity);
                    if (tbl != null && !relatedTables.contains(tbl)) relatedTables.add(tbl);
                }
                if (m.getParameters() != null) {
                    for (String param : m.getParameters()) {
                        String pEntity = inferEntityFromType(cleanParamType(param));
                        if (pEntity != null) {
                            String tbl = entityToTable.get(pEntity);
                            if (tbl != null && !relatedTables.contains(tbl)) relatedTables.add(tbl);
                        }
                    }
                }
            }

            String svcName = ci.getSimpleName();
            tableToServices.computeIfAbsent(svcName, k -> relatedTables);
            for (String tbl : relatedTables) {
                tableToServices.computeIfAbsent(tbl, k -> new ArrayList<>());
                if (!tableToServices.get(tbl).contains(svcName))
                    tableToServices.get(tbl).add(svcName);
            }
        }
    }

    // ───────── 步骤 3: API → Table 关联 ─────────

    private void analyzeApiTableRelations() {
        for (ApiEndpoint ep : project.getApiEndpoints()) {
            String ctrlClass = ep.getControllerClass();
            String ctrlSimple = ctrlClass.substring(ctrlClass.lastIndexOf('.') + 1);
            String methodName = ep.getMethodName() != null ? ep.getMethodName() : "";
            String httpMethod = ep.getHttpMethod();

            List<String> tables = new ArrayList<>();

            // 3a. Controller 类名推断: XxxController → XxxService → Xxx → table
            String baseName = stripSuffix(ctrlSimple, "Controller");
            if (baseName != null) {
                String svcName = baseName + "Service";
                List<String> svcTables = tableToServices.get(svcName);
                if (svcTables != null) tables.addAll(svcTables);

                // 直接 Entity 名匹配
                String tbl = entityToTable.get(baseName);
                if (tbl != null && !tables.contains(tbl)) tables.add(tbl);
            }

            // 3b. 方法名推断: findByXxx / saveXxx / deleteXxx → Xxx → table
            String entityFromMethod = inferEntityFromMethodName(methodName);
            if (entityFromMethod != null) {
                String tbl = entityToTable.get(entityFromMethod);
                if (tbl != null && !tables.contains(tbl)) tables.add(tbl);
            }

            // 3c. 请求体/返回类型推断
            if (ep.getRequestBodyType() != null) {
                String reqEntity = inferEntityFromType(ep.getRequestBodyType());
                if (reqEntity != null) {
                    String tbl = entityToTable.get(reqEntity);
                    if (tbl != null && !tables.contains(tbl)) tables.add(tbl);
                }
            }
            if (ep.getReturnType() != null && !ep.getReturnType().equals("void")) {
                String retEntity = inferEntityFromType(ep.getReturnType());
                if (retEntity != null) {
                    String tbl = entityToTable.get(retEntity);
                    if (tbl != null && !tables.contains(tbl)) tables.add(tbl);
                }
            }

            // 去重
            tables = tables.stream().distinct().collect(Collectors.toList());

            String apiKey = ep.getHttpMethod() + " " + ep.getPath();
            apiToTables.put(apiKey, tables);
            for (String tbl : tables) {
                tableToApis.computeIfAbsent(tbl, k -> new ArrayList<>());
                if (tableToApis.get(tbl).stream().noneMatch(a ->
                        a.getHttpMethod().equals(ep.getHttpMethod()) && a.getPath().equals(ep.getPath())))
                    tableToApis.get(tbl).add(ep);
            }
        }
    }

    // ───────── 步骤 4: 构建报告 ─────────

    private BusinessReport buildReport() {
        BusinessReport report = new BusinessReport();

        // 统计
        report.totalEntities = entityToTable.size();
        report.totalTables = dbSchema != null ? dbSchema.tables.size() : project.getDatabaseTables().size();
        report.totalApis = project.getApiEndpoints().size();
        report.entityToTable = new LinkedHashMap<>(entityToTable);

        // 每张表的业务分析
        Set<String> allTables = new LinkedHashSet<>();
        if (dbSchema != null) {
            for (TableSchema ts : dbSchema.tables) allTables.add(ts.name);
        }
        for (TableInfo ti : project.getDatabaseTables()) allTables.add(ti.getTableName());

        for (String tbl : allTables) {
            TableBusinessFlow flow = new TableBusinessFlow();
            flow.tableName = tbl;

            // 找匹配的 Entity
            for (Map.Entry<String, String> e : entityToTable.entrySet()) {
                if (e.getValue().equals(tbl)) flow.entityName = e.getKey();
            }

            // 找匹配的实时 Schema
            if (dbSchema != null) {
                for (TableSchema ts : dbSchema.tables) {
                    if (ts.name.equals(tbl)) {
                        flow.columns = ts.columns.size();
                        flow.comment = ts.comment;
                        break;
                    }
                }
            }

            // 涉及的 API
            List<ApiEndpoint> apis = tableToApis.get(tbl);
            if (apis != null) {
                for (ApiEndpoint ep : apis) {
                    flow.apiEndpoints.add(ep.getHttpMethod() + " " + ep.getPath());
                    // 推断 CRUD
                    String crud = inferCrud(ep.getHttpMethod(), ep.getPath(), ep.getMethodName());
                    if (!flow.crudOperations.contains(crud)) flow.crudOperations.add(crud);
                }
            }

            // 涉及的 Service
            List<String> svcs = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : tableToServices.entrySet()) {
                if (e.getValue().contains(tbl) && !e.getKey().equals(tbl)) svcs.add(e.getKey());
            }
            flow.services = svcs;

            report.tableFlows.add(flow);
        }

        return report;
    }

    // ───────── 工具方法 ─────────

    private String inferRole(ClassInfo ci) {
        String name = ci.getSimpleName().toLowerCase();
        if (name.contains("service") || ci.getAnnotations().stream().anyMatch(a -> a.equals("Service")))
            return "service";
        if (name.contains("repository") || name.contains("mapper")
                || ci.getAnnotations().stream().anyMatch(a -> a.equals("Repository") || a.equals("Mapper")))
            return "repository";
        if (name.contains("controller") || ci.getAnnotations().stream().anyMatch(a -> a.equals("Controller") || a.equals("RestController")))
            return "controller";
        return "other";
    }

    private String inferEntityFromField(FieldInfo f) {
        String type = f.getType();
        // Repository<User> → User
        if (type.contains("<") && type.contains(">")) {
            int s = type.indexOf('<') + 1;
            int e = type.indexOf('>');
            if (s > 0 && e > s) return type.substring(s, e).trim();
        }
        return null;
    }

    private String findTableByTypeName(String typeName) {
        // JPA Repository<Xxx> or 字段类型 XxxService/XxxMapper
        String base = stripSuffix(typeName, "Repository");
        if (base == null) base = stripSuffix(typeName, "Mapper");
        if (base == null) base = stripSuffix(typeName, "Service");
        if (base == null && typeName.contains(".")) {
            base = typeName.substring(typeName.lastIndexOf('.') + 1);
        }
        if (base != null && entityToTable.containsKey(base))
            return entityToTable.get(base);
        return null;
    }

    private String inferEntityFromType(String type) {
        if (type == null || type.isEmpty()) return null;
        // 处理泛型 List<Xxx>, Page<Xxx>
        if (type.contains("<") && type.contains(">")) {
            int s = type.indexOf('<') + 1;
            int e = type.indexOf('>');
            if (s > 0 && e > s) type = type.substring(s, e).trim();
        }
        if (type.contains(".")) type = type.substring(type.lastIndexOf('.') + 1);
        if (entityToTable.containsKey(type)) return type;
        return null;
    }

    private String inferEntityFromMethodName(String method) {
        if (method == null || method.isEmpty()) return null;
        // findByXxx / saveXxx / deleteXxx / updateXxx / getXxx
        String[] prefixes = {"findBy", "findAll", "find", "save", "delete", "update", "get", "insert", "remove"};
        for (String p : prefixes) {
            if (method.startsWith(p) && method.length() > p.length()) {
                return method.substring(p.length());  // 可能含多个词
            }
        }
        return null;
    }

    private String inferCrud(String httpMethod, String path, String methodName) {
        String m = methodName != null ? methodName.toLowerCase() : "";
        String p = path.toLowerCase();

        if (httpMethod.equals("POST") || m.contains("save") || m.contains("insert") || m.contains("create")
                || p.contains("/add") || p.contains("/create") || p.contains("/save"))
            return "CREATE";
        if (httpMethod.equals("DELETE") || m.contains("delete") || m.contains("remove")
                || p.contains("/delete") || p.contains("/remove"))
            return "DELETE";
        if (httpMethod.equals("PUT") || httpMethod.equals("PATCH") || m.contains("update") || m.contains("edit")
                || p.contains("/update") || p.contains("/edit"))
            return "UPDATE";
        if (httpMethod.equals("GET") || m.contains("find") || m.contains("get") || m.contains("list")
                || m.contains("page") || m.contains("query"))
            return "READ";
        return "READ";
    }

    private String stripSuffix(String name, String suffix) {
        if (name.endsWith(suffix) && name.length() > suffix.length())
            return name.substring(0, name.length() - suffix.length());
        return null;
    }

    private String cleanParamType(String param) {
        // "String name" → "String"
        int space = param.indexOf(' ');
        return space > 0 ? param.substring(0, space).trim() : param.trim();
    }

    static String camelToSnake(String camel) {
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

    /** 生成 Markdown 格式的业务分析报告 */
    public String toMarkdown(BusinessReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# 业务逻辑分析报告\n\n");
        md.append("> 由 Java老狗 综合分析代码 + 数据库生成\n\n");

        md.append("## 总览\n\n");
        md.append("| 指标 | 值 |\n|---|---|\n");
        md.append("| 识别 Entity | ").append(report.totalEntities).append(" |\n");
        md.append("| 数据库表 | ").append(report.totalTables).append(" |\n");
        md.append("| API 端点 | ").append(report.totalApis).append(" |\n\n");

        // Entity → Table 映射
        if (!report.entityToTable.isEmpty()) {
            md.append("## Entity ↔ Table 映射\n\n");
            md.append("| Entity | 数据库表 |\n|---|---|\n");
            for (Map.Entry<String, String> e : report.entityToTable.entrySet()) {
                md.append("| `").append(e.getKey()).append("` | `").append(e.getValue()).append("` |\n");
            }
            md.append("\n");
        }

        // 每张表的详细业务流
        md.append("## 表级业务流\n\n");
        for (TableBusinessFlow flow : report.tableFlows) {
            md.append("### ").append(flow.tableName).append("\n\n");
            if (flow.entityName != null) {
                md.append("**Entity**: `").append(flow.entityName).append("`  \n");
            }
            if (flow.comment != null && !flow.comment.isEmpty()) {
                md.append("**说明**: ").append(flow.comment).append("  \n");
            }
            md.append("**列数**: ").append(flow.columns).append("  \n");
            if (!flow.crudOperations.isEmpty()) {
                md.append("**CRUD**: ").append(String.join(" / ", flow.crudOperations)).append("  \n");
            }
            if (flow.services != null && !flow.services.isEmpty()) {
                md.append("**涉及 Service**: ").append(flow.services.stream()
                        .map(s -> "`" + s + "`").collect(Collectors.joining(", "))).append("  \n");
            }
            if (!flow.apiEndpoints.isEmpty()) {
                md.append("**API 端点**:\n");
                for (String api : flow.apiEndpoints) {
                    md.append("- ").append(api).append("\n");
                }
            }
            md.append("\n");
        }

        // API 视角
        md.append("## API → 数据表 映射\n\n");
        md.append("| API | 操作的表 | 数据流 |\n|---|---|---|\n");
        for (Map.Entry<String, List<String>> e : apiToTables.entrySet()) {
            String tables = e.getValue().isEmpty() ? "（未识别）" :
                    e.getValue().stream().map(t -> "`" + t + "`").collect(Collectors.joining(", "));
            String flowIcon = e.getValue().isEmpty() ? "" : "🔀";
            md.append("| `").append(e.getKey()).append("` | ").append(tables).append(" | ").append(flowIcon).append(" |\n");
        }
        md.append("\n");

        // API 数据流详情
        md.append("## API 数据流详情\n\n");
        md.append("> 每个接口的完整调用链和操作的数据表\n\n");

        List<ApiDataFlow> flows = analyzeApiDataFlows();
        if (flows.isEmpty()) {
            md.append("（无可用的调用链数据）\n\n");
        } else {
            for (ApiDataFlow flow : flows) {
                md.append("### ").append(flow.apiMethod).append(" `").append(flow.apiPath).append("`\n\n");
                md.append("**CRUD**: ").append(flow.crud).append("  \n");
                if (!flow.tables.isEmpty()) {
                    md.append("**涉及表**: ").append(flow.tables.stream()
                            .map(t -> "`" + t + "`").collect(Collectors.joining(", "))).append("\n\n");
                }
                md.append("**调用链**:\n\n");
                for (int i = 0; i < flow.chainSteps.size(); i++) {
                    ApiFlowStep step = flow.chainSteps.get(i);
                    String indent = "  ".repeat(i);
                    md.append(indent).append("└─ **").append(step.method).append("**");
                    if (!step.tables.isEmpty()) {
                        md.append(" → ").append(step.tables.stream()
                                .map(t -> "`" + t + "`").collect(Collectors.joining(", ")));
                    }
                    if (!step.operation.isEmpty()) {
                        md.append(" [").append(step.operation).append("]");
                    }
                    md.append("\n");
                }
                md.append("\n");
            }
        }

        return md.toString();
    }

    // ───────── API 数据流分析 ─────────

    /**
     * 分析每个 API 的完整数据流（调用链 + 数据表）
     */
    public List<ApiDataFlow> analyzeApiDataFlows() {
        List<ApiDataFlow> results = new ArrayList<>();
        List<com.projectassistant.chain.CallChain> callChains = project.getCallChains();
        if (callChains == null || callChains.isEmpty()) return results;

        for (com.projectassistant.chain.CallChain chain : callChains) {
            if (!"API".equals(chain.getEntryRole())) continue;

            // 匹配 API 端点
            String entry = chain.getEntryPoint();
            String ctrlMethod = entry.contains("(") ? entry.substring(0, entry.indexOf('(')) : entry;

            // 找匹配的 ApiEndpoint
            for (ApiEndpoint ep : project.getApiEndpoints()) {
                String controller = ep.getControllerClass();
                String ctrlSimple = controller.substring(controller.lastIndexOf('.') + 1);
                String epMethod = ep.getMethodName();
                // 判断是否匹配
                if (!matchEntryToEndpoint(ctrlSimple, epMethod, ctrlMethod)) continue;

                ApiDataFlow flow = new ApiDataFlow();
                flow.apiMethod = ep.getHttpMethod();
                flow.apiPath = ep.getPath();
                flow.crud = inferCrud(ep.getHttpMethod(), ep.getPath(), ep.getMethodName());
                flow.entryPoint = entry;

                // 遍历所有调用路径，取最长路径
                List<String> longestPath = chain.getLongestPath();
                Set<String> allTables = new LinkedHashSet<>();

                for (String stepMethod : longestPath) {
                    ApiFlowStep step = new ApiFlowStep();
                    step.method = stepMethod;
                    step.tables = findTablesForMethod(stepMethod);
                    step.operation = inferStepOperation(stepMethod, step.tables);
                    allTables.addAll(step.tables);
                    flow.chainSteps.add(step);
                }

                flow.tables = new ArrayList<>(allTables);
                results.add(flow);
                break;
            }
        }

        // 对没有调用链的 API 也生成简单数据流
        for (ApiEndpoint ep : project.getApiEndpoints()) {
            String apiKey = ep.getHttpMethod() + " " + ep.getPath();
            boolean found = results.stream().anyMatch(f ->
                    f.apiMethod.equals(ep.getHttpMethod()) && f.apiPath.equals(ep.getPath()));
            if (!found) {
                ApiDataFlow flow = new ApiDataFlow();
                flow.apiMethod = ep.getHttpMethod();
                flow.apiPath = ep.getPath();
                flow.crud = inferCrud(ep.getHttpMethod(), ep.getPath(), ep.getMethodName());
                flow.tables = apiToTables.getOrDefault(apiKey, List.of());
                // 生成简化步骤
                String controller = ep.getControllerClass();
                String ctrlSimple = controller.substring(controller.lastIndexOf('.') + 1);
                String ctrlStep = ctrlSimple + "." + (ep.getMethodName() != null ? ep.getMethodName() : "handle");
                ApiFlowStep step = new ApiFlowStep();
                step.method = ctrlStep;
                step.tables = flow.tables;
                step.operation = flow.crud;
                flow.chainSteps.add(step);
                results.add(flow);
            }
        }

        return results;
    }

    private boolean matchEntryToEndpoint(String ctrlSimple, String epMethod, String entryMethod) {
        // 格式: UserController.createUser  匹配  UserController + createUser
        String ctrlPart = entryMethod.contains(".") ? entryMethod.substring(0, entryMethod.indexOf('.')) : "";
        String methodPart = entryMethod.contains(".") ? entryMethod.substring(entryMethod.indexOf('.') + 1) : entryMethod;
        return ctrlPart.equals(ctrlSimple) && (epMethod == null || methodPart.isEmpty() || methodPart.equals(epMethod));
    }

    private List<String> findTablesForMethod(String method) {
        // 从方法名推断涉及的表
        List<String> tables = new ArrayList<>();
        String className = method.contains(".") ? method.substring(0, method.indexOf('.')) : method;

        // 检查是否是 Service/Repository
        String svcKey = className;
        List<String> svcTables = tableToServices.get(svcKey);
        if (svcTables != null) tables.addAll(svcTables);

        // 检查类名中是否含 Entity 名
        for (Map.Entry<String, String> e : entityToTable.entrySet()) {
            if (className.contains(e.getKey())) {
                if (!tables.contains(e.getValue())) tables.add(e.getValue());
            }
            // 方法名中是否包含 Entity 名
            if (method.contains(e.getKey())) {
                if (!tables.contains(e.getValue())) tables.add(e.getValue());
            }
        }

        // 检查数据库操作关键词
        String lower = method.toLowerCase();
        for (Map.Entry<String, String> e : entityToTable.entrySet()) {
            String entityLower = e.getKey().toLowerCase();
            if (lower.contains(entityLower)) {
                if (!tables.contains(e.getValue())) tables.add(e.getValue());
            }
        }

        return tables;
    }

    private String inferStepOperation(String method, List<String> tables) {
        String lower = method.toLowerCase();
        if (lower.contains("insert") || lower.contains("save") || lower.contains("add") || lower.contains("create"))
            return "INSERT";
        if (lower.contains("delete") || lower.contains("remove") || lower.contains("del"))
            return "DELETE";
        if (lower.contains("update") || lower.contains("edit") || lower.contains("modify") || lower.contains("set"))
            return "UPDATE";
        if (lower.contains("select") || lower.contains("find") || lower.contains("get") || lower.contains("query")
                || lower.contains("list") || lower.contains("page") || lower.contains("search"))
            return "SELECT";
        if (lower.contains("mapper") || lower.contains("repository") || lower.contains("dao"))
            return "SQL";
        return "";
    }

    // ────── 报告模型 ──────

    public static class BusinessReport {
        public int totalEntities;
        public int totalTables;
        public int totalApis;
        public Map<String, String> entityToTable = new LinkedHashMap<>();
        public List<TableBusinessFlow> tableFlows = new ArrayList<>();
    }

    public static class TableBusinessFlow {
        public String tableName;
        public String entityName;
        public String comment;
        public int columns;
        public List<String> apiEndpoints = new ArrayList<>();
        public List<String> crudOperations = new ArrayList<>();
        public List<String> services = new ArrayList<>();
    }

    /** API 数据流 */
    public static class ApiDataFlow {
        public String apiMethod;
        public String apiPath;
        public String crud;
        public String entryPoint;
        public List<String> tables = new ArrayList<>();
        public List<ApiFlowStep> chainSteps = new ArrayList<>();
    }

    /** 数据流中的一步 */
    public static class ApiFlowStep {
        public String method;
        public List<String> tables = new ArrayList<>();
        public String operation = "";
    }
}
