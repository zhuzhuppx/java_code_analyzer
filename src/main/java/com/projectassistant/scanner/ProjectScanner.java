package com.projectassistant.scanner;

import com.projectassistant.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * 项目扫描器 — 深入挖掘 Java 项目的每一个角落
 *
 * 功能:
 * 1. 递归扫描所有 .java 文件
 * 2. 解析 pom.xml / build.gradle
 * 3. 提取每个类的完整结构信息
 * 4. 分析调用关系
 * 5. 计算圈复杂度
 * 6. 统计代码指标
 */
public class ProjectScanner {

    private final Path rootPath;
    private final ProjectModel project;
    private final Set<Path> processedFiles = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    // 正则表达式常量
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z_][\\w.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([a-zA-Z_][\\w.*]*)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:(public|private|protected)\\s+)?(?:abstract\\s+)?(?:final\\s+)?(?:static\\s+)?(?:strictfp\\s+)?" +
            "(class|interface|enum|@interface|record)\\s+" +
            "([a-zA-Z_][\\w]*)");
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("extends\\s+([a-zA-Z_][\\w.]*(?:<[^>]*>)?)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("implements\\s+([a-zA-Z_][\\w.<>, ]*(?:\\{[^}]*\\})?)");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@([a-zA-Z_][\\w.]+)");
    private static final Pattern FULL_ANNOTATION = Pattern.compile(
            "@([a-zA-Z_][\\w.]+)(\\s*\\([^)]*\\))?\\s*");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:(public|private|protected)\\s+)?" +
            "(?:abstract\\s+)?(?:static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?" +
            "(?:<[^>]+>\\s*)?" +  // 泛型
            "([a-zA-Z_][\\w<>\\[\\],.? ]*)" +  // 返回类型
            "\\s+" +
            "([a-zA-Z_][\\w]*)\\s*\\(");  // 方法名
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?:(public|private|protected)\\s+)?" +
            "(?:static\\s+)?(?:final\\s+)?(?:transient\\s+)?" +
            "([a-zA-Z_][\\w<>\\[\\],.? ]*)" +
            "\\s+" +
            "([a-zA-Z_][\\w]*)\\s*(?:;|=|@)");
    private static final Pattern CALL_PATTERN = Pattern.compile(
            "([a-zA-Z_][\\w]*)\\s*\\.\\s*([a-zA-Z_][\\w]*)\\s*\\(");
    private static final Pattern IF_PATTERN = Pattern.compile("\\bif\\s*\\(");
    private static final Pattern FOR_PATTERN = Pattern.compile("\\b(for|while|do)\\s*\\(");
    private static final Pattern CASE_PATTERN = Pattern.compile("\\bcase\\s+");
    private static final Pattern CATCH_PATTERN = Pattern.compile("\\bcatch\\s*\\(");
    private static final Pattern AND_OR_PATTERN = Pattern.compile("\\b(&&|\\|\\|)\\b");

    public ProjectScanner(String rootPath) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        this.project = new ProjectModel();
        this.project.setRootPath(this.rootPath.toString());
        this.project.setProjectName(this.rootPath.getFileName().toString());
    }

    /**
     * 执行完整扫描
     */
    public ProjectModel scan() throws IOException {
        System.out.println("🔍 开始扫描项目: " + rootPath);
        long start = System.currentTimeMillis();

        // 1. 探测构建类型
        detectBuildType();

        // 2. 扫描模块
        scanModules();

        // 3. 扫描所有 Java 文件
        scanJavaFiles();

        // 4. 分析依赖
        scanDependencies();

        // 5. 计算统计指标
        calculateStats();

        // 6. 分析架构
        analyzeArchitecture();

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("✅ 扫描完成！耗时: %.2f 秒\n", elapsed / 1000.0);
        System.out.println("  📊 共 " + project.getClasses().size() + " 个类, "
                + project.getStats().getTotalMethods() + " 个方法, "
                + project.getStats().getTotalLines() + " 行代码");

        executor.shutdown();
        return project;
    }

    // ==================== 内部方法 ====================

    private void detectBuildType() {
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            project.setBuildType("maven");
            System.out.println("  📦 构建类型: Maven");
        } else if (Files.exists(rootPath.resolve("build.gradle")) ||
                   Files.exists(rootPath.resolve("build.gradle.kts"))) {
            project.setBuildType("gradle");
            System.out.println("  📦 构建类型: Gradle");
        } else {
            project.setBuildType("unknown");
            System.out.println("  📦 构建类型: 未知");
        }
    }

    private void scanModules() throws IOException {
        // Maven 多模块
        if ("maven".equals(project.getBuildType())) {
            Path pomFile = rootPath.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                parseMavenPom(pomFile, project.getModules());
            }
            // 扫描子模块
            try (Stream<Path> paths = Files.find(rootPath, 3,
                    (p, attr) -> {
                        Path parent = p.getParent();
                        return p.getFileName().toString().equals("pom.xml") &&
                               parent != null && !parent.equals(rootPath);
                    })) {
                paths.forEach(p -> {
                    Path parent = p.getParent();
                    if (parent == null) return;
                    ModuleInfo module = new ModuleInfo();
                    module.setName(parent.getFileName().toString());
                    module.setPath(rootPath.relativize(parent).toString());
                    parseMavenPom(p, Collections.singletonList(module));
                    project.getModules().add(module);
                });
            }
        }
        // 如果没有子模块，把根目录作为一个模块
        if (project.getModules().isEmpty()) {
            project.getModules().add(new ModuleInfo("root", "."));
        }
        System.out.println("  📁 发现 " + project.getModules().size() + " 个模块");
    }

    private void parseMavenPom(Path pomFile, List<ModuleInfo> modules) {
        try {
            List<String> lines = Files.readAllLines(pomFile);
            String content = String.join("\n", lines);

            // 提取 groupId, artifactId, version
            String groupId = extractXmlTag(content, "groupId");
            String artifactId = extractXmlTag(content, "artifactId");
            String version = extractXmlTag(content, "version");

            for (ModuleInfo module : modules) {
                if (groupId != null) module.setGroupId(groupId);
                if (artifactId != null) module.setArtifactId(artifactId);
                if (version != null) module.setVersion(version);
            }

            // 提取子模块
            Matcher m = Pattern.compile("<module>([^<]+)</module>").matcher(content);
            while (m.find()) {
                Path modulePath = pomFile.getParent().resolve(m.group(1));
                if (Files.exists(modulePath)) {
                    boolean exists = project.getModules().stream()
                            .anyMatch(mod -> mod.getPath().equals(m.group(1)));
                    if (!exists) {
                        ModuleInfo mi = new ModuleInfo(m.group(1), m.group(1));
                        Path mpom = modulePath.resolve("pom.xml");
                        if (Files.exists(mpom)) {
                            List<String> mlines = Files.readAllLines(mpom);
                            String mcontent = String.join("\n", mlines);
                            mi.setGroupId(extractXmlTag(mcontent, "groupId"));
                            mi.setArtifactId(extractXmlTag(mcontent, "artifactId"));
                            mi.setVersion(extractXmlTag(mcontent, "version"));
                        }
                        project.getModules().add(mi);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("  ⚠️ 解析 pom.xml 失败: " + e.getMessage());
        }
    }

    private String extractXmlTag(String xml, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">(?:<!\\[CDATA\\[)?([^<]+)(?:\\]\\]>)?</" + tag + ">").matcher(xml);
        if (m.find()) {
            String val = m.group(1).trim();
            return val.isEmpty() || val.contains("${") ? null : val;
        }
        return null;
    }

    private void scanJavaFiles() throws IOException {
        System.out.println("  🔎 扫描 Java 源文件...");
        List<Path> javaFiles;

        try (Stream<Path> paths = Files.walk(rootPath)) {
            javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("target" + File.separator))
                    .filter(p -> !p.toString().contains("build" + File.separator))
                    .filter(p -> !p.toString().contains("node_modules"))
                    .collect(Collectors.toList());
        }

        System.out.println("  📄 发现 " + javaFiles.size() + " 个 Java 文件");

        project.getStats().setTotalFiles(javaFiles.size());

        // 并行解析
        List<Future<ClassInfo>> futures = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            futures.add(executor.submit(() -> parseJavaFile(javaFile)));
        }

        for (Future<ClassInfo> future : futures) {
            try {
                ClassInfo ci = future.get();
                if (ci != null) {
                    project.getClasses().add(ci);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        // 关联模块
        for (ClassInfo ci : project.getClasses()) {
            for (ModuleInfo mi : project.getModules()) {
                if (ci.getSourceFilePath() != null &&
                    ci.getSourceFilePath().contains(mi.getPath())) {
                    ci.setModuleName(mi.getName());
                    mi.setClassCount(mi.getClassCount() + 1);
                    break;
                }
            }
        }

        System.out.println("  📚 解析完成: " + project.getClasses().size() + " 个顶级类型");
    }

    private ClassInfo parseJavaFile(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            String content = String.join("\n", lines);
            int totalLines = lines.size();

            // 计算代码行数（去掉注释和空行）
            int codeLines = (int) lines.stream()
                    .filter(l -> !l.trim().isEmpty())
                    .filter(l -> !l.trim().startsWith("//"))
                    .filter(l -> !l.trim().startsWith("*"))
                    .filter(l -> !l.trim().startsWith("/*"))
                    .count();

            // 提取包名
            String packageName = "";
            Matcher pm = PACKAGE_PATTERN.matcher(content);
            if (pm.find()) {
                packageName = pm.group(1);
            }

            // 提取所有 import
            List<String> imports = new ArrayList<>();
            Matcher im = IMPORT_PATTERN.matcher(content);
            while (im.find()) {
                imports.add(im.group(1));
            }

            // 解析类声明
            ClassInfo classInfo = parseClassDeclaration(content, filePath, packageName, imports);
            if (classInfo == null) return null;

            classInfo.setLineCount(totalLines);
            classInfo.setCodeLineCount(codeLines);

            // 计算父模块的行数
            String relPath = rootPath.relativize(filePath).toString();
            classInfo.setSourceFilePath(relPath);

            return classInfo;
        } catch (IOException e) {
            return null;
        }
    }

    private ClassInfo parseClassDeclaration(String content, Path filePath,
                                            String packageName, List<String> imports) {
        // 去掉字符串和注释以简化匹配
        String clean = removeStringsAndComments(content);

        // 找到第一个类/接口/枚举声明
        Matcher cm = CLASS_PATTERN.matcher(clean);
        if (!cm.find()) return null;

        String visibility = cm.group(1);
        String type = cm.group(2);
        String className = cm.group(3);

        // 映射类型
        String mappedType;
        switch (type) {
            case "@interface": mappedType = "annotation"; break;
            case "interface": mappedType = "interface"; break;
            case "enum": mappedType = "enum"; break;
            case "record": mappedType = "record"; break;
            default: mappedType = "class";
        }

        ClassInfo ci = new ClassInfo();
        ci.setPackageName(packageName);
        ci.setSimpleName(className);
        ci.setFullyQualifiedName(packageName.isEmpty() ? className : packageName + "." + className);
        ci.setType(mappedType);
        ci.setVisibility(visibility != null ? visibility : "package-private");
        ci.setAbstract(clean.substring(0, cm.end()).contains("abstract"));
        ci.setFinal(clean.substring(0, cm.end()).contains("final"));
        ci.setStatic(clean.substring(0, cm.end()).contains("static"));

        // 类体范围
        int bodyStart = findBodyStart(clean, cm.end());
        int bodyEnd = findBodyEnd(clean, bodyStart);
        String classBody = bodyStart > 0 && bodyEnd > bodyStart ?
                clean.substring(bodyStart, bodyEnd) : clean.substring(cm.end());

        // 提取父类
        Matcher em = EXTENDS_PATTERN.matcher(clean.substring(0, bodyStart));
        if (em.find()) {
            ci.setSuperClassName(em.group(1).replaceAll("<[^>]*>", "").trim());
        }

        // 提取接口
        Matcher iplm = IMPLEMENTS_PATTERN.matcher(clean.substring(0, bodyStart));
        if (iplm.find()) {
            String[] ifaces = iplm.group(1).split(",");
            for (String iface : ifaces) {
                ci.getInterfaces().add(iface.trim().replaceAll("<[^>]*>", ""));
            }
        }

        // 提取注解
        Matcher am = ANNOTATION_PATTERN.matcher(clean.substring(0, cm.start()));
        while (am.find()) {
            ci.getAnnotations().add(am.group(1));
        }

        // 提取方法
        parseMethods(classBody, ci, packageName, imports, content);

        // 提取字段
        parseFields(classBody, ci);

        // 提取调用关系
        parseMethodCalls(classBody, ci, packageName);

        return ci;
    }

    private void parseMethods(String body, ClassInfo classInfo,
                              String packageName, List<String> imports,
                              String originalContent) {
        // 按行处理
        String[] lines = body.split("\n");
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean inMethod = false;
        int methodStartLine = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            current.append(line).append("\n");

            if (!inMethod) {
                // 检测方法声明
                Matcher mm = METHOD_PATTERN.matcher(line);
                if (mm.find() && !line.trim().startsWith(".") && !line.contains("=")) {
                    String potentialReturn = mm.group(2);
                    String methodName = mm.group(3);

                    // 过滤掉可能的变量声明
                    if (methodName != null && !methodName.equals("new") &&
                        Character.isLowerCase(methodName.charAt(0))) {

                        MethodInfo mi = new MethodInfo();
                        mi.setName(methodName);
                        mi.setVisibility(mm.group(1) != null ? mm.group(1) : "package-private");

                        String returnType = potentialReturn != null ?
                                potentialReturn.replaceAll("<[^>]+>", "").trim() : "void";
                        mi.setReturnType(returnType);

                        mi.setConstructor(classInfo.getSimpleName().equals(methodName));
                        mi.setAbstract(line.contains("abstract") || body.contains("abstract "));

                        mi.setStatic(line.substring(0, Math.min(mm.start(), line.length())).contains("static"));
                        mi.setFinal(line.substring(0, Math.min(mm.start(), line.length())).contains("final"));

                        // 提取注解（含参数，如 @RequestMapping("/bizCustomer")）
                        for (int j = methodStartLine; j < i; j++) {
                            Matcher an = FULL_ANNOTATION.matcher(lines[j]);
                            while (an.find()) {
                                String full = an.group(0).trim();
                                mi.getAnnotations().add(full);
                                if (full.startsWith("Override")) mi.setOverride(true);
                            }
                        }

                        // 提取参数
                        extractParameters(line, mi);

                        // 计算复杂度
                        mi.setCyclomaticComplexity(computeCyclomaticComplexity(body, i));

                        // 行数
                        mi.setLineCount(1);

                        classInfo.getMethods().add(mi);
                    }
                }
            }
        }
    }

    private void extractParameters(String methodLine, MethodInfo mi) {
        int parenStart = methodLine.indexOf('(');
        int parenEnd = findMatchingParen(methodLine, parenStart);
        if (parenStart < 0 || parenEnd < 0) return;

        String paramsStr = methodLine.substring(parenStart + 1, parenEnd);
        if (paramsStr.trim().isEmpty()) return;

        String[] params = paramsStr.split(",");
        for (String param : params) {
            param = param.trim();
            if (param.isEmpty()) continue;

            // 解析 final Type name 模式
            String cleanParam = param.replaceAll("^final\\s+", "");
            String[] parts = cleanParam.split("\\s+");
            if (parts.length >= 2) {
                // 类型可能是泛型如 List<String>, Map<K,V> 等
                int typeEnd = parts.length - 1;
                StringBuilder typeBuilder = new StringBuilder();
                for (int j = 0; j < typeEnd; j++) {
                    if (j > 0) typeBuilder.append(" ");
                    typeBuilder.append(parts[j]);
                }
                mi.getParameters().add(typeBuilder.toString());
                mi.getParameterNames().add(parts[parts.length - 1]);
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                mi.getParameters().add("var");
                mi.getParameterNames().add(parts[0]);
            }

            // 构建签名
            mi.setSignature(mi.getReturnType() + " " + mi.getName() +
                    "(" + String.join(", ", mi.getParameters()) + ")");
        }
    }

    private void parseFields(String body, ClassInfo classInfo) {
        List<String> pendingAnnotations = new ArrayList<>();

        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) continue;

            // 跳过方法体内部
            // 检测字段声明
            Matcher fm = FIELD_PATTERN.matcher(line);
            if (fm.find() && !line.contains("(") && !line.contains("=")) {
                String vis = fm.group(1);
                String type = fm.group(2);
                String name = fm.group(3);

                if (name != null && Character.isLowerCase(name.charAt(0)) &&
                    !isJavaKeyword(name) && !type.startsWith("@")) {

                    FieldInfo fi = new FieldInfo();
                    fi.setName(name);
                    fi.setType(type != null ? type.trim() : "unknown");
                    fi.setVisibility(vis != null ? vis : "private");

                    // 检查修饰符
                    String before = line.substring(0, Math.min(fm.start(), line.length()));
                    fi.setStatic(before.contains("static") || line.contains("static"));
                    fi.setFinal(before.contains("final") || line.contains("final"));

                    // 附加挂起的注解
                    if (!pendingAnnotations.isEmpty()) {
                        fi.setAnnotations(new ArrayList<>(pendingAnnotations));
                        pendingAnnotations.clear();
                    }

                    classInfo.getFields().add(fi);
                    continue;
                }
            }

            // 提取注解（含参数），用于字段前的注解
            Matcher am = FULL_ANNOTATION.matcher(line);
            boolean foundAnnot = false;
            while (am.find()) {
                String full = am.group(0).trim();
                // 检查是否是 Spring 相关字段注解
                String bare = full.startsWith("@") ? full.substring(1) : full;
                String name = bare.replaceAll("\\(.*", "");
                if (name.equals("Autowired") || name.equals("Resource") || name.equals("Inject")
                        || name.equals("Value") || name.equals("Qualifier")
                        || name.equals("Lazy") || name.startsWith("Json")
                        || name.equals("NotNull") || name.equals("NotBlank") || name.equals("NotEmpty")
                        || name.equals("Size") || name.equals("Pattern") || name.equals("Valid")
                        || name.equals("DateTimeFormat") || name.equals("NumberFormat")) {
                    pendingAnnotations.add(full);
                    foundAnnot = true;
                }
            }
            if (!foundAnnot) {
                // 非注解行，清空待定注解（避免错误的跨行关联）
                // 但保留如果行看起来像字段声明的一部分（如多行声明）
                if (!line.endsWith(",") && !line.endsWith("(") && !line.endsWith("{")) {
                    pendingAnnotations.clear();
                }
            }
        }
    }

    private void parseMethodCalls(String body, ClassInfo classInfo, String packageName) {
        for (MethodInfo method : classInfo.getMethods()) {
            // 在方法体中查找调用
            int methodIdx = body.indexOf(method.getName() + "(");
            if (methodIdx < 0) continue;

            int bodyStart = body.indexOf('{', methodIdx);
            int bodyEnd = body.indexOf('}', bodyStart);
            if (bodyStart < 0 || bodyEnd < 0) continue;

            String methodBody = body.substring(bodyStart, bodyEnd);

            Matcher cm = CALL_PATTERN.matcher(methodBody);
            while (cm.find()) {
                String target = cm.group(1);
                String call = cm.group(2);
                if (!target.equals("this") && !target.equals("super") &&
                    !target.equals("System") && !target.equals("Math") &&
                    !method.getAccessedFields().contains(target)) {
                    method.getCalledMethods().add(target + "." + call);
                }
            }

            // 跟踪字段访问（this.xxx）
            Matcher fm = Pattern.compile("this\\.([a-zA-Z_][\\w]*)").matcher(methodBody);
            while (fm.find()) {
                if (!method.getAccessedFields().contains(fm.group(1))) {
                    method.getAccessedFields().add(fm.group(1));
                }
            }
        }
    }

    private int computeCyclomaticComplexity(String body, int startLine) {
        String[] lines = body.split("\n");
        int complexity = 1;  // 基础复杂度

        for (int i = startLine; i < Math.min(startLine + 100, lines.length); i++) {
            String line = lines[i];
            if (line.contains("{")) {
                complexity += countMatches(IF_PATTERN, line);
                complexity += countMatches(FOR_PATTERN, line);
                complexity += countMatches(CASE_PATTERN, line);
                complexity += countMatches(CATCH_PATTERN, line);
                complexity += countMatches(AND_OR_PATTERN, line);
            }
            if (line.contains("}") && line.trim().length() < 3) break;
        }
        return complexity;
    }

    private int countMatches(Pattern pattern, String text) {
        int count = 0;
        Matcher m = pattern.matcher(text);
        while (m.find()) count++;
        return count;
    }

    private void scanDependencies() throws IOException {
        if ("maven".equals(project.getBuildType())) {
            scanMavenDependencies(rootPath.resolve("pom.xml"));
        }
        System.out.println("  📦 发现 " + project.getDependencies().size() + " 个外部依赖");
    }

    private void scanMavenDependencies(Path pomFile) throws IOException {
        if (!Files.exists(pomFile)) return;

        String content = Files.readString(pomFile);
        // 提取 <dependency> 块
        Pattern depPattern = Pattern.compile(
                "<dependency>\\s*(.*?)\\s*</dependency>", Pattern.DOTALL);
        Matcher dm = depPattern.matcher(content);

        while (dm.find()) {
            String depBlock = dm.group(1);
            DependencyInfo dep = new DependencyInfo();
            dep.setGroupId(extractXmlTagSimple(depBlock, "groupId"));
            dep.setArtifactId(extractXmlTagSimple(depBlock, "artifactId"));
            dep.setVersion(extractXmlTagSimple(depBlock, "version"));
            dep.setScope(extractXmlTagSimple(depBlock, "scope"));
            if (dep.getScope() == null) dep.setScope("compile");
            if ("true".equals(extractXmlTagSimple(depBlock, "optional"))) {
                dep.setOptional(true);
            }

            if (dep.getGroupId() != null && dep.getArtifactId() != null) {
                project.getDependencies().add(dep);
            }
        }
    }

    private String extractXmlTagSimple(String xml, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">").matcher(xml);
        if (m.find()) {
            String val = m.group(1).trim();
            return val.isEmpty() || val.contains("${") ? null : val;
        }
        return null;
    }

    private void calculateStats() {
        ProjectStats stats = project.getStats();
        int totalMethods = 0;
        int totalFields = 0;
        long totalLines = 0;
        long totalCodeLines = 0;

        for (ClassInfo ci : project.getClasses()) {
            totalLines += ci.getLineCount();
            totalCodeLines += ci.getCodeLineCount();
            totalMethods += ci.getMethods().size();
            totalFields += ci.getFields().size();

            switch (ci.getType()) {
                case "class": stats.setTotalClasses(stats.getTotalClasses() + 1); break;
                case "interface": stats.setTotalInterfaces(stats.getTotalInterfaces() + 1); break;
                case "enum": stats.setTotalEnums(stats.getTotalEnums() + 1); break;
                case "record": stats.setTotalRecords(stats.getTotalRecords() + 1); break;
                case "annotation": stats.setTotalAnnotations(stats.getTotalAnnotations() + 1); break;
            }

            // 检测上帝类
            if (ci.getMethods().size() > 20 || ci.getLineCount() > 500) {
                stats.setGodClassCount(stats.getGodClassCount() + 1);
            }

            // 统计长方法
            for (MethodInfo mi : ci.getMethods()) {
                if (mi.getLineCount() > 50) {
                    stats.setLongMethodCount(stats.getLongMethodCount() + 1);
                }
                if (mi.getCyclomaticComplexity() > 10) {
                    stats.setHighComplexityCount(stats.getHighComplexityCount() + 1);
                }
                stats.setMaxComplexity(Math.max(stats.getMaxComplexity(),
                        mi.getCyclomaticComplexity()));
            }
        }

        stats.setTotalMethods(totalMethods);
        stats.setTotalFields(totalFields);
        stats.setTotalLines((int) totalLines);
        stats.setTotalCodeLines((int) totalCodeLines);
        stats.setTotalCommentLines((int) (totalLines - totalCodeLines));
        stats.setTotalDependencies(project.getDependencies().size());
        stats.setCommentRatio(totalLines > 0 ?
                (double) (totalLines - totalCodeLines) / totalLines : 0);
        stats.setAverageMethodLines(totalMethods > 0 ?
                (double) totalCodeLines / totalMethods : 0);

        // 构建字段到类的映射（用于检查 getter/setter）
        for (ClassInfo ci : project.getClasses()) {
            for (FieldInfo fi : ci.getFields()) {
                for (MethodInfo mi : ci.getMethods()) {
                    String capName = Character.toUpperCase(fi.getName().charAt(0)) +
                            fi.getName().substring(1);
                    if (mi.getName().equals("get" + capName) ||
                        (fi.getType().equals("boolean") && mi.getName().equals("is" + capName))) {
                        fi.setHasGetter(true);
                    }
                    if (mi.getName().equals("set" + capName)) {
                        fi.setHasSetter(true);
                    }
                }
            }
        }
    }

    private void analyzeArchitecture() {
        // 构建包依赖图
        for (ClassInfo ci : project.getClasses()) {
            String pkg = ci.getPackageName();
            project.getPackageDependencies().putIfAbsent(pkg, new HashSet<>());
        }

        // 构建调用图
        for (ClassInfo ci : project.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                String caller = ci.getFullyQualifiedName() + "#" + mi.getName();
                project.getCallGraph().putIfAbsent(caller, new ArrayList<>());

                for (String called : mi.getCalledMethods()) {
                    project.getCallGraph().get(caller).add(called);
                }
            }
        }

        // 构建包间依赖
        for (ClassInfo ci : project.getClasses()) {
            String pkg = ci.getPackageName();
            for (MethodInfo mi : ci.getMethods()) {
                for (String called : mi.getCalledMethods()) {
                    // 尝试推断目标包
                    for (ClassInfo target : project.getClasses()) {
                        if (called.startsWith(target.getSimpleName() + ".") ||
                            called.endsWith("." + target.getSimpleName())) {
                            String targetPkg = target.getPackageName();
                            project.getPackageDependencies()
                                    .computeIfAbsent(pkg, k -> new HashSet<>())
                                    .add(targetPkg);
                        }
                    }
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private String removeStringsAndComments(String source) {
        // 删除块注释
        source = source.replaceAll("/\\*[^*]*(?:\\*[^/*][^*]*)*\\*/", "");
        // 删除单行注释（保留行号参考）
        source = source.replaceAll("//[^\n]*", "");
        // 删除字符串字面量
        source = source.replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\"");
        return source;
    }

    private int findBodyStart(String source, int fromIndex) {
        int brace = source.indexOf('{', fromIndex);
        if (brace < 0) return -1;
        return brace + 1;
    }

    private int findBodyEnd(String source, int fromIndex) {
        if (fromIndex < 0 || fromIndex >= source.length()) return -1;
        int depth = 1;
        for (int i = fromIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int findMatchingParen(String s, int openPos) {
        if (openPos < 0 || openPos >= s.length()) return -1;
        int depth = 1;
        for (int i = openPos + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private boolean isJavaKeyword(String word) {
        return switch (word) {
            case "abstract", "assert", "boolean", "break", "byte", "case",
                 "catch", "char", "class", "const", "continue", "default",
                 "do", "double", "else", "enum", "extends", "final",
                 "finally", "float", "for", "goto", "if", "implements",
                 "import", "instanceof", "int", "interface", "long",
                 "native", "new", "package", "private", "protected",
                 "public", "return", "short", "static", "strictfp",
                 "super", "switch", "synchronized", "this", "throw",
                 "throws", "transient", "try", "void", "volatile",
                 "while", "var", "record", "sealed", "permits", "yield" -> true;
            default -> false;
        };
    }
}
