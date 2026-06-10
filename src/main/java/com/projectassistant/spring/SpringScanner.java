package com.projectassistant.spring;

import com.projectassistant.model.*;
import com.projectassistant.spring.BeanInfo.InjectionPoint;
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
    private final Map<String, String> beanTypeMap = new HashMap<>(); // bean 类名/接口名 -> 角色 (controller/service/repo/config)
    private final Map<String, List<String>> interfaceImplMap = new HashMap<>(); // 接口 -> 实现类列表
    private final Map<String, BeanInfo> beanInfoMap = new LinkedHashMap<>();   // className -> BeanInfo
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
                + beanInfoMap.size() + " 个 Bean, " + countInjections() + " 条注入关系");
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
        // 第1步：识别所有 Bean（含实现接口映射）
        for (ClassInfo ci : classes) {
            String role = detectBeanRole(ci);
            if (role == null) continue;

            String className = ci.getFullyQualifiedName();
            registerBean(ci, className, role);

            // 识别 bean 的显式名称（如 @Service("myService")）
            String explicitName = extractBeanName(ci);
            if (explicitName != null && !explicitName.isEmpty()) {
                beanTypeMap.put(explicitName, role);
            }

            // 扫描实现的接口，加入接口名 -> 实现类映射
            for (String iface : ci.getInterfaces()) {
                interfaceImplMap.computeIfAbsent(iface, k -> new ArrayList<>()).add(className);
                beanTypeMap.put(iface, role);
            }
        }

        // 第2步：解析每个 Bean 的注入点
        for (ClassInfo ci : classes) {
            String className = ci.getFullyQualifiedName();
            if (!beanInfoMap.containsKey(className)) continue;
            BeanInfo bi = beanInfoMap.get(className);

            // 字段注入 @Autowired / @Resource / @Inject
            for (FieldInfo fi : ci.getFields()) {
                InjectionPoint ip = resolveFieldInjection(ci, fi);
                if (ip != null) {
                    bi.addInjection(ip);
                    if (ip.getTargetBeanName() != null && beanInfoMap.containsKey(ip.getTargetBeanName())) {
                        beanInfoMap.get(ip.getTargetBeanName()).addInjectedBy(className);
                    }
                    beanDependencies.computeIfAbsent(className, k -> new ArrayList<>())
                            .add(ip.getTargetType());
                }
            }

            // 构造器注入（检测构造方法的参数类型）
            for (MethodInfo mi : ci.getMethods()) {
                if (!mi.getName().equals(ci.getSimpleName())) continue;
                List<String> params = mi.getParameters();
                if (params == null || params.isEmpty()) continue;
                for (String param : params) {
                    String paramType = param.replaceAll("\\s+final\\s+", "").replaceAll("\\bfinal\\s+", "").trim();
                    String resolved = resolveBeanForType(paramType);
                    if (resolved != null) {
                        InjectionPoint ip = new InjectionPoint("constructor-arg", paramType, "constructor", "Autowired");
                        ip.setTargetBeanName(resolved);
                        bi.addInjection(ip);
                        beanDependencies.computeIfAbsent(className, k -> new ArrayList<>()).add(paramType);
                        if (beanInfoMap.containsKey(resolved)) {
                            beanInfoMap.get(resolved).addInjectedBy(className);
                        }
                    }
                }
            }
        }

        // 第3步：检测循环依赖
        detectCircularDependencies();
    }

    /** 检查类是否是 Spring Bean，返回角色 */
    private String detectBeanRole(ClassInfo ci) {
        for (String ann : ci.getAnnotations()) {
            String clean = ann.replace("@", "");
            if (clean.startsWith("Service")) return "service";
            if (clean.startsWith("Repository")) return "repository";
            if (clean.startsWith("Component")) return "component";
            if (clean.startsWith("Controller") || clean.startsWith("RestController")) return "controller";
            if (clean.startsWith("Configuration")) return "configuration";
        }
        return null;
    }

    /** 注册一个 Bean */
    private void registerBean(ClassInfo ci, String className, String role) {
        if (beanInfoMap.containsKey(className)) return;
        BeanInfo bi = new BeanInfo(className, ci.getSimpleName(), role);
        for (String ann : ci.getAnnotations()) {
            if (ann.startsWith("Scope")) {
                Matcher m = Pattern.compile("value\\s*=\\s*\"([^\"]+)\"").matcher(ann);
                if (m.find()) bi.setScope(m.group(1));
                else bi.setScope("singleton");
            }
            if (ann.startsWith("Primary") || ann.equals("Primary")) bi.setPrimary(true);
            if (ann.startsWith("Lazy")) bi.setLazy(true);
        }
        beanInfoMap.put(className, bi);
        beanTypeMap.put(className, role);
    }

    /** 提取 @Service("xx") 中的显式 bean 名称 */
    private String extractBeanName(ClassInfo ci) {
        for (String ann : ci.getAnnotations()) {
            Matcher m = Pattern.compile("@(Service|Component|Repository|Controller|RestController)\\s*\\(\"([^\"]+)\"\\)").matcher(ann);
            if (m.find()) return m.group(2);
            m = Pattern.compile("@(Service|Component|Repository|Controller|RestController)\\s*\\(\\s*value\\s*=\\s*\"([^\"]+)\"\\s*\\)").matcher(ann);
            if (m.find()) return m.group(2);
        }
        return null;
    }

    /** 解析字段注入 */
    private InjectionPoint resolveFieldInjection(ClassInfo ci, FieldInfo fi) {
        String injectionAnn = null;
        String qualifier = null;
        for (String ann : fi.getAnnotations()) {
            String clean = ann.replace("@", "");
            if (clean.equals("Autowired") || clean.equals("Resource") || clean.equals("Inject")) {
                injectionAnn = clean;
            }
            if (clean.startsWith("Qualifier")) {
                Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(ann);
                if (m.find()) qualifier = m.group(1);
            }
        }
        if (injectionAnn == null) return null;

        String targetType = fi.getType();
        if (targetType != null && targetType.contains("<")) {
            Matcher m = Pattern.compile("<([^>]+)>").matcher(targetType);
            if (m.find()) targetType = m.group(1);
        }

        InjectionPoint ip = new InjectionPoint(fi.getName(), targetType, "field", injectionAnn);
        if (qualifier != null) ip.setQualifier(qualifier);

        String resolved = resolveBeanForType(targetType);
        if (resolved != null) ip.setTargetBeanName(resolved);
        return ip;
    }

    /** 根据类型查找匹配的 Bean（支持接口 -> 实现类映射） */
    private String resolveBeanForType(String type) {
        if (type == null) return null;
        String cleanType = type.replaceAll("[\\[\\]]", "").split("<")[0].trim();
        String simpleName = cleanType.contains(".") ? cleanType.substring(cleanType.lastIndexOf('.') + 1) : cleanType;

        if (beanInfoMap.containsKey(cleanType)) return cleanType;
        if (beanTypeMap.containsKey(cleanType)) return cleanType;

        if (interfaceImplMap.containsKey(cleanType)) {
            List<String> impls = interfaceImplMap.get(cleanType);
            if (impls.size() == 1) return impls.get(0);
            for (String impl : impls) {
                BeanInfo bi = beanInfoMap.get(impl);
                if (bi != null && bi.isPrimary()) return impl;
            }
            return impls.get(0) + " (注意: 有" + impls.size() + "个实现)";
        }

        for (String beanClass : beanInfoMap.keySet()) {
            String beanSimple = beanClass.contains(".") ? beanClass.substring(beanClass.lastIndexOf('.') + 1) : beanClass;
            if (beanSimple.equals(simpleName)) return beanClass;
        }
        return cleanType;
    }

    /** 检测循环依赖 */
    private void detectCircularDependencies() {
        for (String beanName : beanDependencies.keySet()) {
            Set<String> path = new LinkedHashSet<>();
            if (hasCycle(beanName, new HashSet<>(), path)) {
                System.out.println("  [Spring] ⚠️ 循环依赖: " + String.join(" -> ", path));
            }
        }
    }

    private boolean hasCycle(String current, Set<String> visited, Set<String> path) {
        if (path.contains(current)) return true;
        if (visited.contains(current)) return false;
        visited.add(current);
        path.add(current);
        List<String> deps = beanDependencies.get(current);
        if (deps != null) {
            for (String dep : deps) {
                for (String bean : beanDependencies.keySet()) {
                    if (bean.endsWith("." + dep) || bean.equals(dep)) {
                        if (hasCycle(bean, visited, path)) return true;
                    }
                }
            }
        }
        path.remove(current);
        return false;
    }

    private int countInjections() {
        return (int) beanInfoMap.values().stream()
                .flatMap(bi -> bi.getInjections().stream())
                .count();
    }

    /** 扫描配置属性 */
    private void scanConfigProperties() {
        for (ClassInfo ci : classes) {
            for (String ann : ci.getAnnotations()) {
                if (ann.startsWith("ConfigurationProperties")) {
                    Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(ann);
                    if (m.find()) configProperties.put("config.prefix", m.group(1));
                }
            }
            for (MethodInfo mi : ci.getMethods()) {
                for (String ann : mi.getAnnotations()) {
                    if (ann.startsWith("Value")) {
                        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(ann);
                        if (m.find()) configProperties.put(m.group(1), mi.getReturnType());
                    }
                }
            }
        }
    }

    // ============ 新增公开访问 ============

    public Map<String, BeanInfo> getBeanInfoMap() { return beanInfoMap; }
    public Map<String, String> getBeanTypeMap() { return beanTypeMap; }

    // ==================== 对外输出 ====================

    public List<ApiEndpoint> getEndpoints() { return endpoints; }
    public Map<String, List<String>> getBeanDependencies() { return beanDependencies; }
    public Map<String, String> getConfigProperties() { return configProperties; }
    public String getProjectPattern() { return projectPattern; }
    public boolean isSpringBoot() { return hasSpringBoot; }
    public String getServerPort() { return serverPort; }
    public String getContextPath() { return contextPath; }
}
