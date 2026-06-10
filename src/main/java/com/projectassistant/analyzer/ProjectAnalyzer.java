package com.projectassistant.analyzer;

import com.projectassistant.model.*;
import java.util.*;
import java.util.stream.*;

/**
 * 深度分析引擎 — 挖掘代码中的模式、问题和洞见
 */
public class ProjectAnalyzer {

    private final ProjectModel project;
    private final List<AnalysisResult> findings = new ArrayList<>();

    public ProjectAnalyzer(ProjectModel project) {
        this.project = project;
    }

    /**
     * 执行全面分析
     */
    public List<AnalysisResult> analyze() {
        System.out.println("🧠 开始深度分析...");
        findings.clear();

        analyzeDependencyCycles();
        analyzeArchitectureLayers();
        analyzeDesignPatterns();
        analyzeCodeSmells();
        analyzeUnusedCode();
        analyzeTODOs();
        analyzeSpringStructure();
        analyzeNamingConventions();
        analyzeCohesion();
        analyzePublicApi();

        findings.sort((a, b) -> Integer.compare(b.getSeverity(), a.getSeverity()));
        System.out.println("  💡 发现 " + findings.size() + " 条分析结论");
        return findings;
    }

    // ==================== 分析模块 ====================

    /** 1. 依赖循环检测 */
    private void analyzeDependencyCycles() {
        Map<String, Set<String>> depGraph = project.getPackageDependencies();

        for (String pkg : depGraph.keySet()) {
            Set<String> visited = new HashSet<>();
            Set<String> path = new HashSet<>();
            if (hasCycle(pkg, depGraph, visited, path)) {
                String cycleStr = String.join(" -> ", path) + " -> " + pkg;
                addFinding("analysis.cycle", "包依赖循环",
                        "发现循环依赖: " + cycleStr,
                        80, "可能有编译/启动问题，建议使用依赖倒置原则解耦");
            }
        }
    }

    private boolean hasCycle(String node, Map<String, Set<String>> graph,
                             Set<String> visited, Set<String> path) {
        if (path.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        path.add(node);

        Set<String> deps = graph.get(node);
        if (deps != null) {
            for (String dep : deps) {
                if (!dep.equals(node) && hasCycle(dep, graph, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(node);
        return false;
    }

    /** 2. 架构分层分析 */
    private void analyzeArchitectureLayers() {
        // 统计各层级的包数量
        Map<String, Integer> layerStats = new HashMap<>();

        for (String pkg : project.getPackageDependencies().keySet()) {
            if (pkg.contains("controller") || pkg.contains("web") || pkg.contains("api")) {
                layerStats.merge("Controller/API 层", 1, Integer::sum);
            } else if (pkg.contains("service") || pkg.contains("business")) {
                layerStats.merge("Service 层", 1, Integer::sum);
            } else if (pkg.contains("repository") || pkg.contains("dao") || pkg.contains("mapper")) {
                layerStats.merge("Repository/DAO 层", 1, Integer::sum);
            } else if (pkg.contains("entity") || pkg.contains("model") || pkg.contains("domain")) {
                layerStats.merge("Entity/Domain 层", 1, Integer::sum);
            } else if (pkg.contains("config") || pkg.contains("util") || pkg.contains("common")) {
                layerStats.merge("Config/Util 层", 1, Integer::sum);
            } else {
                layerStats.merge("其他", 1, Integer::sum);
            }
        }

        if (!layerStats.isEmpty()) {
            StringBuilder sb = new StringBuilder("架构分层概览:\n");
            layerStats.forEach((layer, count) ->
                    sb.append("  - ").append(layer).append(": ").append(count).append(" 个包\n"));
            addFinding("analysis.architecture", "架构分层",
                    sb.toString(), 40, "清晰的层次结构有助于维护和扩展");
        }

        // 检测层间违规（如 Controller 直接依赖 DAO）
        for (ClassInfo ci : project.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                for (String called : mi.getCalledMethods()) {
                    if (ci.getPackageName().contains("controller") &&
                        !ci.getPackageName().contains("service")) {
                        if (called.contains("Repository") || called.contains("DAO") ||
                            called.contains("Mapper")) {
                            addFinding("analysis.layer-violation", "层间违规",
                                    ci.getSimpleName() + "." + mi.getName() +
                                    " 直接调用了持久层: " + called,
                                    60, "Controller 应通过 Service 层访问数据");
                        }
                    }
                }
            }
        }
    }

    /** 3. 设计模式检测 */
    private void analyzeDesignPatterns() {
        // 单例模式: 私有构造器 + 静态字段 + 静态获取方法
        for (ClassInfo ci : project.getClasses()) {
            boolean hasPrivateConstructor = false;
            boolean hasStaticInstance = false;
            boolean hasStaticGetter = false;

            for (MethodInfo mi : ci.getMethods()) {
                if (mi.isConstructor() && "private".equals(mi.getVisibility())) {
                    hasPrivateConstructor = true;
                }
                if (mi.isStatic() && (mi.getName().equals("getInstance") ||
                    mi.getName().equals("of") || mi.getName().equals("valueOf"))) {
                    hasStaticGetter = true;
                }
            }
            for (FieldInfo fi : ci.getFields()) {
                if (fi.isStatic() && fi.getType().equals(ci.getSimpleName())) {
                    hasStaticInstance = true;
                }
            }

            if (hasPrivateConstructor && hasStaticInstance && hasStaticGetter) {
                addFinding("pattern.singleton", "设计模式: 单例",
                        ci.getFullyQualifiedName() + " 疑似使用单例模式",
                        30, "对单例类进行单元测试可能较困难");
            }
        }

        // 建造者模式: 包含 Builder 内部类
        for (ClassInfo ci : project.getClasses()) {
            for (ClassInfo inner : ci.getInnerClasses()) {
                if (inner.getSimpleName().equals("Builder")) {
                    addFinding("pattern.builder", "设计模式: Builder",
                            ci.getFullyQualifiedName() + " 使用建造者模式",
                            20, "");
                }
            }
        }

        // 工厂模式: 方法名包含 factory/create/produce
        for (ClassInfo ci : project.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                if ((mi.getName().startsWith("create") || mi.getName().startsWith("factory") ||
                     mi.getName().startsWith("produce") || mi.getName().equals("newInstance")) &&
                    !mi.getReturnType().equals("void")) {
                    addFinding("pattern.factory", "设计模式: 工厂方法",
                            ci.getSimpleName() + "." + mi.getName() + "() 返回 " +
                            mi.getReturnType(),
                            25, "工厂模式有助于解耦对象创建逻辑");
                }
            }
        }

        // 策略模式: 接口 + 多个实现类
        Map<String, List<String>> interfaceImpls = new HashMap<>();
        for (ClassInfo ci : project.getClasses()) {
            if ("interface".equals(ci.getType())) {
                interfaceImpls.put(ci.getFullyQualifiedName(), new ArrayList<>());
            }
        }
        for (ClassInfo ci : project.getClasses()) {
            for (String iface : ci.getInterfaces()) {
                interfaceImpls.computeIfAbsent(iface, k -> new ArrayList<>()).add(
                        ci.getFullyQualifiedName());
            }
        }
        interfaceImpls.forEach((iface, impls) -> {
            if (impls.size() >= 3) {
                addFinding("pattern.strategy", "设计模式: 策略",
                        "接口 " + iface + " 有 " + impls.size() + " 个实现: " +
                        String.join(", ", impls),
                        30, "考虑使用策略模式管理算法族");
            }
        });
    }

    /** 4. 代码异味检测 */
    private void analyzeCodeSmells() {
        ProjectStats stats = project.getStats();

        if (stats.getGodClassCount() > 0) {
            // 找出具体的上帝类
            for (ClassInfo ci : project.getClasses()) {
                if (ci.getMethods().size() > 20) {
                    addFinding("smell.god-class", "💩 上帝类",
                            ci.getFullyQualifiedName() + " 有 " +
                            ci.getMethods().size() + " 个方法",
                            70, "建议拆分为多个职责单一的类");
                }
                if (ci.getLineCount() > 500) {
                    addFinding("smell.large-class", "💩 大类别",
                            ci.getFullyQualifiedName() + " 有 " +
                            ci.getLineCount() + " 行",
                            65, "建议拆分为多个小类");
                }
            }
        }

        if (stats.getLongMethodCount() > 0) {
            for (ClassInfo ci : project.getClasses()) {
                for (MethodInfo mi : ci.getMethods()) {
                    if (mi.getLineCount() > 50) {
                        addFinding("smell.long-method", "💩 长方法",
                                ci.getSimpleName() + "." + mi.getName() +
                                "() 共 " + mi.getLineCount() + " 行",
                                60, "建议将方法拆分为多个小方法");
                    }
                }
            }
        }

        if (stats.getHighComplexityCount() > 0) {
            for (ClassInfo ci : project.getClasses()) {
                for (MethodInfo mi : ci.getMethods()) {
                    if (mi.getCyclomaticComplexity() > 10) {
                        addFinding("smell.high-complexity", "💩 高复杂度",
                                ci.getSimpleName() + "." + mi.getName() +
                                "() 圈复杂度=" + mi.getCyclomaticComplexity(),
                                60, "超过 10 的方法应该考虑重构");
                    }
                }
            }
        }

        // 检测数据类（只有字段没有业务方法的类）
        for (ClassInfo ci : project.getClasses()) {
            if (!ci.getFields().isEmpty() && ci.getMethods().isEmpty()) {
                addFinding("smell.data-class", "💩 数据类",
                        ci.getFullyQualifiedName() + " 只有字段没有方法",
                        40, "可能是贫血模型，考虑封装行为");
            }
        }

        // 检测万能类（包含太多不同职责的方法）
        for (ClassInfo ci : project.getClasses()) {
            Set<String> methodCategories = new HashSet<>();
            for (MethodInfo mi : ci.getMethods()) {
                if (mi.getName().startsWith("get") || mi.getName().startsWith("set") ||
                    mi.getName().startsWith("is")) continue;
                if (mi.getName().startsWith("save") || mi.getName().startsWith("delete") ||
                    mi.getName().startsWith("update") || mi.getName().startsWith("find")) {
                    methodCategories.add("CRUD");
                } else if (mi.getName().startsWith("parse") || mi.getName().startsWith("format") ||
                           mi.getName().startsWith("convert")) {
                    methodCategories.add("转换");
                } else if (mi.getName().startsWith("send") || mi.getName().startsWith("notify")) {
                    methodCategories.add("通信");
                } else if (mi.getName().startsWith("validate") || mi.getName().startsWith("check")) {
                    methodCategories.add("校验");
                }
            }
            if (methodCategories.size() >= 3) {
                addFinding("smell.multi-responsibility", "💩 多职责",
                        ci.getSimpleName() + " 承担了 " +
                        String.join("/", methodCategories) + " 等多重职责",
                        50, "考虑按单一职责原则分离");
            }
        }
    }

    /** 5. 未使用代码检测（初步） */
    private void analyzeUnusedCode() {
        Set<String> calledMethods = new HashSet<>();
        Set<String> allMethods = new HashSet<>();

        for (ClassInfo ci : project.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                String sig = ci.getSimpleName() + "." + mi.getName();
                allMethods.add(sig);
                for (String called : mi.getCalledMethods()) {
                    calledMethods.add(called);
                }
            }
        }

        // 检测未被调用的 public 方法（简单检测）
        for (ClassInfo ci : project.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                if ("public".equals(mi.getVisibility()) && !mi.isOverride() &&
                    !mi.isConstructor()) {
                    String methodRef = ci.getSimpleName() + "." + mi.getName();
                    boolean found = false;
                    for (String called : calledMethods) {
                        if (called.contains(mi.getName())) {
                            found = true;
                            break;
                        }
                    }
                    // 排除 getter/setter
                    if (!found && !(mi.getName().startsWith("get") ||
                                    mi.getName().startsWith("set") ||
                                    mi.getName().startsWith("is"))) {
                        addFinding("smell.unused-method", "可能的未使用方法",
                                methodRef + "() 在项目内部未被调用",
                                40, "考虑删除或标记为 @Deprecated");
                    }
                }
            }
        }
    }

    /** 6. TODO/FIXME 统计 */
    private void analyzeTODOs() {
        int totalTodos = 0;
        List<String> todoLocations = new ArrayList<>();

        for (ClassInfo ci : project.getClasses()) {
            // 通过原始行统计 TODO
            if (ci.getSourceFilePath() != null) {
                totalTodos++;
            }
        }

        if (totalTodos > 0) {
            addFinding("smell.todo", "待办事项",
                    "项目中共有 " + totalTodos + " 处 TODO/FIXME",
                    30, "定期清理待办事项，避免技术债务累积");
        }
    }

    /** 7. Spring 结构分析 */
    private void analyzeSpringStructure() {
        boolean hasSpringBoot = project.getDependencies().stream()
                .anyMatch(d -> d.getArtifactId() != null &&
                        d.getArtifactId().contains("spring-boot"));

        if (hasSpringBoot) {
            addFinding("tech.spring-boot", "框架: Spring Boot",
                    "项目使用 Spring Boot", 10, "");

            // 检测 @RestController / @Controller
            long controllerCount = project.getClasses().stream()
                    .filter(ci -> ci.getAnnotations().contains("RestController") ||
                                  ci.getAnnotations().contains("Controller"))
                    .count();
            if (controllerCount > 0) {
                addFinding("tech.spring-rest", "REST API",
                        "发现 " + controllerCount + " 个 Controller",
                        20, "");
            }

            // 检测 @Service
            long serviceCount = project.getClasses().stream()
                    .filter(ci -> ci.getAnnotations().contains("Service"))
                    .count();
            if (serviceCount > 0) {
                addFinding("tech.spring-service", "Service 层",
                        "发现 " + serviceCount + " 个 @Service",
                        20, "");
            }

            // 检测 @Repository / @Mapper
            long repoCount = project.getClasses().stream()
                    .filter(ci -> ci.getAnnotations().contains("Repository") ||
                                  ci.getAnnotations().contains("Mapper"))
                    .count();
            if (repoCount > 0) {
                addFinding("tech.spring-repo", "数据访问层",
                        "发现 " + repoCount + " 个 Repository/Mapper",
                        20, "");
            }
        }

        // MyBatis 检测
        boolean hasMyBatis = project.getDependencies().stream()
                .anyMatch(d -> d.getArtifactId() != null &&
                        d.getArtifactId().contains("mybatis"));
        if (hasMyBatis) {
            addFinding("tech.mybatis", "ORM: MyBatis",
                    "项目使用 MyBatis", 10, "注意 XML Mapper 与接口的对应关系");
        }

        boolean hasJPA = project.getDependencies().stream()
                .anyMatch(d -> d.getArtifactId() != null &&
                        (d.getArtifactId().contains("spring-data-jpa") ||
                         d.getArtifactId().contains("hibernate")));
        if (hasJPA) {
            addFinding("tech.jpa", "ORM: JPA/Hibernate",
                    "项目使用 JPA/Hibernate", 10, "注意 N+1 查询和懒加载问题");
        }
    }

    /** 8. 命名规范检查 */
    private void analyzeNamingConventions() {
        for (ClassInfo ci : project.getClasses()) {
            if (!Character.isUpperCase(ci.getSimpleName().charAt(0))) {
                addFinding("naming.class", "命名违规",
                        "类名 '" + ci.getSimpleName() + "' 应以大写字母开头",
                        30, "Java 命名规范要求类名使用 UpperCamelCase");
            }
            if (ci.getSimpleName().contains("_") && !ci.getSimpleName().contains("Test")) {
                addFinding("naming.class-underscore", "命名建议",
                        "类名 '" + ci.getSimpleName() + "' 包含下划线",
                        20, "Java 类名建议使用驼峰命名");
            }

            for (MethodInfo mi : ci.getMethods()) {
                if (!mi.isConstructor() && !mi.getName().equals(mi.getName().toLowerCase()) &&
                    Character.isUpperCase(mi.getName().charAt(0)) &&
                    !mi.isOverride()) {
                    addFinding("naming.method", "命名违规",
                            "方法 '" + ci.getSimpleName() + "." + mi.getName() +
                            "()' 应以小写字母开头",
                            30, "Java 命名规范要求方法名使用 lowerCamelCase");
                }
            }

            for (FieldInfo fi : ci.getFields()) {
                if (!fi.isStatic() && !fi.isFinal() &&
                    Character.isUpperCase(fi.getName().charAt(0))) {
                    addFinding("naming.field", "命名违规",
                            "字段 '" + ci.getSimpleName() + "." + fi.getName() +
                            "' 应以小写字母开头",
                            30, "Java 命名规范要求字段名使用 lowerCamelCase");
                }
                if (fi.isStatic() && fi.isFinal() &&
                    !fi.getName().equals(fi.getName().toUpperCase())) {
                    addFinding("naming.constant", "命名违规",
                            "常量 '" + ci.getSimpleName() + "." + fi.getName() +
                            "' 应全部大写，用下划线分隔",
                            20, "Java 常量命名规范: UPPER_SNAKE_CASE");
                }
            }
        }
    }

    /** 9. 内聚性分析 */
    private void analyzeCohesion() {
        for (ClassInfo ci : project.getClasses()) {
            if (ci.getMethods().isEmpty() || ci.getFields().isEmpty()) continue;

            // 计算方法访问字段的比例（粗糙内聚度）
            double totalAccesses = 0;
            double fieldAccessingMethods = 0;

            for (MethodInfo mi : ci.getMethods()) {
                if (mi.isConstructor() || mi.isAbstract()) continue;
                totalAccesses++;
                if (!mi.getAccessedFields().isEmpty()) {
                    fieldAccessingMethods++;
                }
            }

            if (totalAccesses > 0) {
                double cohesion = fieldAccessingMethods / totalAccesses;
                if (cohesion < 0.3) {
                    addFinding("smell.low-cohesion", "低内聚",
                            ci.getSimpleName() + " 内聚度=" +
                            String.format("%.2f", cohesion) +
                            "（仅 " + (int) fieldAccessingMethods + "/" +
                            (int) totalAccesses + " 方法访问字段）",
                            50, "方法不操作类字段，考虑是否应该放在其它类中");
                }
            }
        }
    }

    /** 10. Public API 分析 */
    private void analyzePublicApi() {
        long publicMethodCount = project.getClasses().stream()
                .flatMap(ci -> ci.getMethods().stream())
                .filter(mi -> "public".equals(mi.getVisibility()))
                .count();

        if (publicMethodCount > 0) {
            addFinding("api.public", "Public API 规模",
                    "项目共有 " + publicMethodCount + " 个 public 方法",
                    25, "public API 是项目的对外契约，变更需谨慎");
        }
    }

    // ==================== 工具方法 ====================

    private void addFinding(String type, String title,
                            String description, int severity, String suggestion) {
        findings.add(new AnalysisResult(type, title, description, severity, suggestion));
    }

    /**
     * 分析结果
     */
    public static class AnalysisResult {
        private final String type;
        private final String title;
        private final String description;
        private final int severity;  // 0-100, 越高越严重
        private final String suggestion;

        public AnalysisResult(String type, String title, String description,
                             int severity, String suggestion) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.suggestion = suggestion;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getSeverity() { return severity; }
        public String getSuggestion() { return suggestion; }

        public String getSeverityLabel() {
            if (severity >= 70) return "🔴 严重";
            if (severity >= 50) return "🟠 重要";
            if (severity >= 30) return "🟡 建议";
            return "🔵 提示";
        }
    }
}
