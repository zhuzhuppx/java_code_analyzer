package com.projectassistant.spring;

import com.projectassistant.model.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Spring 深度理解引擎
 * 扫描 Spring 注解，构建：
 * 1. API 路由全景表
 * 2. Bean 依赖注入图
 * 3. 配置属性一览
 * 4. 项目架构模式识别
 */
public class SpringScanner {

    private final List<ClassInfo> classes;
    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private final Map<String, List<String>> beanDependencies = new HashMap<>(); // bean -> 它依赖的 beans
    private final Map<String, String> beanTypeMap = new HashMap<>(); // bean 类名 -> 角色 (controller/service/repo/config)
    private final Map<String, String> configProperties = new HashMap<>();
    private String projectPattern = "unknown"; // MVC / DDD / Hexagonal / 传统分层
    private boolean hasSpringBoot = false;
    private String serverPort = "8080";
    private String contextPath = "";

    // Spring 注解正则
    private static final Pattern REQUEST_MAPPING = Pattern.compile(
            "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)" +
            "\\s*(?:\\([^)]*\\))?");
    private static final Pattern MAPPING_PATH = Pattern.compile(
            "(?:value|path)\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern METHOD_ATTR = Pattern.compile(
            "method\\s*=\\s*(?:RequestMethod\\.)?(GET|POST|PUT|DELETE|PATCH)");
    private static final Pattern AUTOWIRED = Pattern.compile(
            "@(Autowired|Resource|Inject)\\s*");
    private static final Pattern QUALIFIER = Pattern.compile(
            "@Qualifier\\s*\\(\"([^\"]+)\"\\)");
    private static final Pattern PREAUTHORIZE = Pattern.compile(
            "@PreAuthorize\\s*\\([^)]*\\)");
    private static final Pattern SERVER_PORT = Pattern.compile(
            "server\\.port\\s*[=:]\\s*(\\d+)");
    private static final Pattern CONTEXT_PATH = Pattern.compile(
            "server\\.servlet\\.context-path\\s*[=:]\\s*\"?([^\"]+)\"?");

    public SpringScanner(List<ClassInfo> classes) {
        this.classes = classes;
    }

    /**
     * 执行 Spring 扫描
     */
    public void scan() {
        System.out.println("  [Spring] 扫描 Spring 框架注解...");
        scanProjectPattern();
        scanControllers();
        scanBeanDependencies();
        scanConfigProperties();
        System.out.println("  [Spring] 发现 " + endpoints.size() + " 个 API 端点, "
                + beanDependencies.size() + " 个 Bean");
    }

    /** 识别项目架构模式 */
    private void scanProjectPattern() {
        boolean hasController = false, hasService = false, hasRepo = false;
        boolean hasEntity = false, hasDomain = false, hasPort = false, hasAdapter = false;

        for (ClassInfo ci : classes) {
            String pkg = ci.getPackageName();
            if (hasAnnotation(ci, "RestController") || hasAnnotation(ci, "Controller")) {
                hasController = true;
                beanTypeMap.put(ci.getFullyQualifiedName(), "controller");
            }
            if (hasAnnotation(ci, "Service")) {
                hasService = true;
                beanTypeMap.put(ci.getFullyQualifiedName(), "service");
            }
            if (hasAnnotation(ci, "Repository")) {
                hasRepo = true;
                beanTypeMap.put(ci.getFullyQualifiedName(), "repository");
            }
            if (pkg.contains("domain") || pkg.contains("entity")) hasEntity = true;
            if (pkg.contains("port")) hasPort = true;
            if (pkg.contains("adapter")) hasAdapter = true;
        }

        if (hasPort && hasAdapter) projectPattern = "Hexagonal/Ports-Adapters";
        else if (hasDomain && hasController && hasService && hasRepo) projectPattern = "DDD 分层";
        else if (hasController && hasService && hasRepo) projectPattern = "经典三层 (Controller-Service-Repository)";
        else if (hasController && hasService) projectPattern = "Controller-Service 两层";
        else if (hasController) projectPattern = "Controller 直出";

        // Spring Boot 检测
        for (ClassInfo ci : classes) {
            if (hasAnnotation(ci, "SpringBootApplication") ||
                hasAnnotation(ci, "EnableAutoConfiguration")) {
                hasSpringBoot = true;
                break;
            }
        }
    }

    /** 辅助：检查类注解（兼容 @Name 和 Name 两种格式） */
    private boolean hasAnnotation(ClassInfo ci, String name) {
        return ci.getAnnotations().stream()
                .anyMatch(a -> a.equals(name) || a.equals("@" + name) || a.startsWith("@" + name + "("));
    }

    /** 扫描所有 @RestController/@Controller 提取 API 路由 */
    private void scanControllers() {
        for (ClassInfo ci : classes) {
            boolean isController = hasAnnotation(ci, "RestController") || hasAnnotation(ci, "Controller");
            if (!isController) continue;

            String classPath = extractClassLevelPath(ci);
            boolean classSecured = ci.getAnnotations().stream()
                    .anyMatch(a -> a.startsWith("PreAuthorize") || a.startsWith("Secured") ||
                                   a.startsWith("RolesAllowed"));

            for (MethodInfo mi : ci.getMethods()) {
                scanMethodEndpoint(ci, mi, classPath, classSecured);
            }
        }
    }

    private String extractClassLevelPath(ClassInfo ci) {
        for (String ann : ci.getAnnotations()) {
            if (ann.startsWith("RequestMapping")) {
                Matcher m = MAPPING_PATH.matcher(ann);
                if (m.find()) return m.group(1);
            }
        }
        return "";
    }

    private void scanMethodEndpoint(ClassInfo ci, MethodInfo mi,
                                     String classPath, boolean classSecured) {
        for (String ann : mi.getAnnotations()) {
            Matcher rm = REQUEST_MAPPING.matcher(ann);
            if (!rm.find()) continue;

            String annName = rm.group(1);
            // 从注解名推断 HTTP 方法
            String httpMethod = inferHttpMethod(annName, ann);

            // 提取路径
            String methodPath = extractMethodPath(ann);
            String fullPath = normalizePath(classPath + "/" + methodPath);

            // 安全检测
            boolean secured = classSecured || ann.contains("PreAuthorize") ||
                              ann.contains("Secured") || ann.contains("RolesAllowed");

            ApiEndpoint ep = new ApiEndpoint();
            ep.setHttpMethod(httpMethod);
            ep.setPath(fullPath);
            ep.setControllerClass(ci.getFullyQualifiedName());
            ep.setMethodName(mi.getName());
            ep.setReturnType(mi.getReturnType());
            ep.getParameters().addAll(mi.getParameterNames());
            ep.getAnnotations().add(ann);
            ep.setSecured(secured);
            endpoints.add(ep);
        }
    }

    private String inferHttpMethod(String annName, String annExpr) {
        switch (annName) {
            case "GetMapping": return "GET";
            case "PostMapping": return "POST";
            case "PutMapping": return "PUT";
            case "DeleteMapping": return "DELETE";
            case "PatchMapping": return "PATCH";
            case "RequestMapping":
                Matcher m = METHOD_ATTR.matcher(annExpr);
                return m.find() ? m.group(1) : "ANY";
            default: return "ANY";
        }
    }

    private String extractMethodPath(String ann) {
        Matcher m = MAPPING_PATH.matcher(ann);
        if (m.find()) {
            String path = m.group(1);
            // 移除占位符 {xxx} -> :xxx（兼容 OpenAPI 格式）
            return path.replaceAll("\\{([^}]+)\\}", ":$1");
        }
        return "";
    }

    private String normalizePath(String path) {
        return "/" + Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }

    /** 扫描 Bean 依赖注入关系 */
    private void scanBeanDependencies() {
        for (ClassInfo ci : classes) {
            List<String> deps = new ArrayList<>();
            for (FieldInfo fi : ci.getFields()) {
                // @Autowired / @Resource / @Inject
                boolean hasInject = fi.getAnnotations().stream()
                        .anyMatch(a -> a.equals("Autowired") || a.equals("Resource") || a.equals("Inject"));
                if (hasInject) {
                    deps.add(fi.getType());
                }
            }
            // 构造器注入检测
            for (MethodInfo mi : ci.getMethods()) {
                if (mi.isConstructor()) {
                    for (String param : mi.getParameters()) {
                        // 构造器参数如果没有 @Autowired 但类本身是 Spring Bean，也视为注入
                        String cleanParam = param.replaceAll("\\[.*\\]", "").trim();
                        if (isSpringBean(cleanParam)) {
                            deps.add(cleanParam);
                        }
                    }
                }
            }
            if (!deps.isEmpty()) {
                beanDependencies.put(ci.getFullyQualifiedName(), deps);
            }
        }
    }

    private boolean isSpringBean(String className) {
        return beanTypeMap.containsKey(className) ||
               className.contains("Service") ||
               className.contains("Repository") ||
               className.contains("Mapper") ||
               className.contains("Component") ||
               className.contains("Dao") ||
               className.contains("Client") ||
               className.contains("Util") ||
               className.contains("Helper") ||
               className.contains("Manager");
    }

    /** 扫描配置属性 */
    private void scanConfigProperties() {
        // 从 application.yml / application.properties 解析
        // 这里从类注解中的 @Value 和 @ConfigurationProperties 提取
        for (ClassInfo ci : classes) {
            for (String ann : ci.getAnnotations()) {
                if (ann.startsWith("ConfigurationProperties")) {
                    Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(ann);
                    if (m.find()) {
                        configProperties.put("config.prefix", m.group(1));
                    }
                }
            }
            for (MethodInfo mi : ci.getMethods()) {
                for (String ann : mi.getAnnotations()) {
                    if (ann.startsWith("Value")) {
                        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(ann);
                        if (m.find()) {
                            configProperties.put(m.group(1), mi.getReturnType());
                        }
                    }
                }
            }
        }
    }

    // ==================== 对外输出 ====================

    public List<ApiEndpoint> getEndpoints() { return endpoints; }
    public Map<String, List<String>> getBeanDependencies() { return beanDependencies; }
    public Map<String, String> getBeanTypeMap() { return beanTypeMap; }
    public Map<String, String> getConfigProperties() { return configProperties; }
    public String getProjectPattern() { return projectPattern; }
    public boolean isSpringBoot() { return hasSpringBoot; }
    public String getServerPort() { return serverPort; }
    public String getContextPath() { return contextPath; }
}
