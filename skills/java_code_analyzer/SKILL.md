---
name: java_code_analyzer
description: "java_code_analyzer 项目知识库 — 25 个类, 0 个 API, 0 张表"
metadata:
  copaw:
    emoji: "📦"
    requires: {}
---

# java_code_analyzer 项目知识库

> 由 Java老狗 自动生成 | 2026-06-12 11:17

当用户询问本项目相关的任何问题时，优先使用以下信息回答。

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目 | java_code_analyzer |
| 构建 | maven |
| Java | unknown |
| Spring Boot | 否 |
| 行数 | 6343 |
| 类数 | 25 |
| 方法数 | 348 |
| API | 0 |
| 数据库表 | 0 |
| 依赖 | 4 |

### 依赖

```
com.google.code.gson:gson:2.10.1
org.yaml:snakeyaml:2.0
com.h2database:h2:2.2.224
org.junit.jupiter:junit-jupiter:5.10.0 [test]
```

## 2. 架构

**unknown**

```
  projectassistant/
    [C] Main
  analyzer/
    [C] ProjectAnalyzer
  chain/
    [C] CallChain
    [C] CallChainAnalyzer
  chat/
    [C] DeepSeekChat
  config/
    [C] ConfigParser
  db/
    [C] DatabaseManager
  knowledge/
    [C] KnowledgeBaseGenerator
  model/
    [C] ClassInfo
    [C] DependencyInfo
    [C] FieldInfo
    [C] MethodInfo
    [C] ModuleInfo
    [C] ProjectModel
    [C] ProjectStats
  reporter/
    [C] ReportGenerator
  scanner/
    [C] ProjectScanner
    [C] VulnScanner
  spring/
    [C] ApiEndpoint
    [C] BeanInfo
    [C] SpringScanner
  sql/
    [C] SchemaParser
    [C] SqlParser
    [C] TableInfo
  web/
    [C] WebServer
```

## 3. API

（无）

## 4. 数据库

（无）

## 5. Bean 依赖图

（无）

## 6. 调用链

（无）

## 7. 关键类

### com.projectassistant

- **Main** (class) — 2 方法, 2 字段

### com.projectassistant.analyzer

- **ProjectAnalyzer** (class) — 26 方法, 9 字段

### com.projectassistant.chain

- **CallChain** (class) — 6 方法, 2 字段
- **CallChainAnalyzer** (class) — 5 方法, 2 字段

### com.projectassistant.chat

- **DeepSeekChat** (class) — 0 方法, 0 字段

### com.projectassistant.config

- **ConfigParser** (class) — 17 方法, 5 字段

### com.projectassistant.db

- **DatabaseManager** (class) — 9 方法, 4 字段

### com.projectassistant.knowledge

- **KnowledgeBaseGenerator** (class) — 19 方法, 2 字段

### com.projectassistant.model

- **ClassInfo** (class) — 21 方法, 14 字段
- **DependencyInfo** (class) — 8 方法, 6 字段
- **FieldInfo** (class) — 9 方法, 7 字段
- **MethodInfo** (class) — 19 方法, 12 字段
- **ModuleInfo** (class) — 7 方法, 7 字段
- **ProjectModel** (class) — 22 方法, 9 字段
- **ProjectStats** (class) — 19 方法, 18 字段

### com.projectassistant.reporter

- **ReportGenerator** (class) — 19 方法, 2 字段

### com.projectassistant.scanner

- **ProjectScanner** (class) — 13 方法, 6 字段
- **VulnScanner** (class) — 6 方法, 15 字段

### com.projectassistant.spring

- **ApiEndpoint** (class) — 25 方法, 17 字段
- **BeanInfo** (class) — 17 方法, 13 字段
- **SpringScanner** (class) — 35 方法, 8 字段

### com.projectassistant.sql

- **SchemaParser** (class) — 15 方法, 5 字段
- **SqlParser** (class) — 12 方法, 3 字段
- **TableInfo** (class) — 16 方法, 10 字段

### com.projectassistant.web

- **WebServer** (class) — 1 方法, 4 字段

## 8. 类全量信息

> 以下为每个类的详细字段和注解信息，大模型可据此自行推断数据库结构、业务逻辑。

### com.projectassistant

```
class Main {
    String extension;
    String content;

    // --- 2 个方法 ---
    void main(String[]);
    else if(var);
}
```

### com.projectassistant.analyzer

```
class ProjectAnalyzer {
    ProjectModel project;
    return findings;
    return true;
    return false;
    String type;
    String title;
    String description;
    int severity;
    String suggestion;

    // --- 26 个方法 ---
    List analyze();
    void analyzeDependencyCycles();
    boolean hasCycle();
    void analyzeArchitectureLayers();
    else if(pkg.contains("") ||);
    else if(pkg.contains("") || pkg.contains("") ||);
    else if(pkg.contains("") || pkg.contains("") ||);
    else if(pkg.contains("") || pkg.contains("") ||);
    void analyzeDesignPatterns();
    void analyzeCodeSmells();
    else if();
    else if(mi.getName().startsWith("") ||);
    else if(mi.getName().startsWith("") ||);
    void analyzeUnusedCode();
    void analyzeTODOs();
    void analyzeSpringStructure();
    void analyzeNamingConventions();
    void analyzeCohesion();
    void analyzePublicApi();
    void addFinding();
    String getType();
    String getTitle();
    String getDescription();
    int getSeverity();
    String getSuggestion();
    String getSeverityLabel();
}
```

### com.projectassistant.chain

```
class CallChain {
    String entryPoint;
    String entryRole;

    // --- 6 个方法 ---
    String getEntryPoint();
    String getEntryRole();
    List> getCallPaths();
    int getMaxDepth();
    List getLongestPath();
    String toString();
}
```

```
class CallChainAnalyzer {
    Map<String, String> classRoleMap;
    return result;

    // --- 5 个方法 ---
    void analyze();
    void traceChain();
    List getCriticalChains();
    String inferRole(String);
    List getChains();
}
```

### com.projectassistant.chat

```
class DeepSeekChat {
}
```

### com.projectassistant.config

```
class ConfigParser {
    Path rootPath;
    String filePath;
    String type;
    int keyCount;
    return properties;

    // --- 17 个方法 ---
    String getFilePath();
    String getType();
    int getKeyCount();
    Map parse();
    void findAndParseConfigFiles();
    void parseFile(Path);
    else if(name.endsWith("") ||);
    else if(var);
    void parseProperties(String, Path);
    void parseYaml(String, Path);
    else if(var);
    void parseLogbackXml(String, Path);
    boolean isKeyConfig(String);
    String getSummary();
    String maskPassword(String);
    List getSources();
    Map getProperties();
}
```

### com.projectassistant.db

```
class DatabaseManager {
    volatile Connection conn;
    return id;
    return list;
    return result;

    // --- 9 个方法 ---
    void init();
    long saveProject(String, String);
    void saveKnowledgeBase(long, String);
    void saveReport(long, String, String);
    void saveChatMessage(long, String, String);
    List> listProjects();
    Map getProject(long);
    long getProjectIdForPath(String);
    void close();
}
```

### com.projectassistant.knowledge

```
class KnowledgeBaseGenerator {
    ProjectModel project;
    return m;

    // --- 19 个方法 ---
    String generate();
    void preamble();
    void overview();
    int calculateHealthScore();
    void architecture();
    void apiCatalog();
    void databaseSchema();
    void beanGraph();
    String capitalize(String);
    void callChains();
    void keyClasses();
    void rawClassDetails();
    void businessFlow();
    String inferAction(ApiEndpoint);
    void configurations();
    void devGuide();
    String shorten(String);
    void save(String);
    void saveSkill(String);
}
```

### com.projectassistant.model

```
class ClassInfo {
    String fullyQualifiedName;
    String packageName;
    String simpleName;
    String type;
    String visibility;
    String sourceFilePath;
    String moduleName;
    boolean isAbstract;
    boolean isFinal;
    boolean isStatic;
    String superClassName;
    int lineCount;
    int codeLineCount;
    int totalComplexity;

    // --- 21 个方法 ---
    String getFullyQualifiedName();
    String getPackageName();
    String getSimpleName();
    String getType();
    String getVisibility();
    String getSourceFilePath();
    String getModuleName();
    boolean isAbstract();
    boolean isFinal();
    boolean isStatic();
    String getSuperClassName();
    List getInterfaces();
    List getTypeParameters();
    List getFields();
    List getMethods();
    List getAnnotations();
    List getInnerClasses();
    int getLineCount();
    int getCodeLineCount();
    int getTotalComplexity();
    String toString();
}
```

```
class DependencyInfo {
    String groupId;
    String artifactId;
    String version;
    String scope;
    boolean optional;
    String type;

    // --- 8 个方法 ---
    String getGroupId();
    String getArtifactId();
    String getVersion();
    String getScope();
    boolean isOptional();
    String getType();
    String toString();
    String toGav();
}
```

```
class FieldInfo {
    String name;
    String type;
    String visibility;
    boolean isStatic;
    boolean isFinal;
    boolean hasGetter;
    boolean hasSetter;

    // --- 9 个方法 ---
    String getName();
    String getType();
    String getVisibility();
    boolean isStatic();
    boolean isFinal();
    List getAnnotations();
    boolean isHasGetter();
    boolean isHasSetter();
    String toString();
}
```

```
class MethodInfo {
    String name;
    String returnType;
    String visibility;
    boolean isAbstract;
    boolean isStatic;
    boolean isFinal;
    boolean isSynchronized;
    boolean isConstructor;
    boolean isOverride;
    int lineCount;
    int cyclomaticComplexity;
    String signature;

    // --- 19 个方法 ---
    String getName();
    String getReturnType();
    String getVisibility();
    boolean isAbstract();
    boolean isStatic();
    boolean isFinal();
    boolean isSynchronized();
    boolean isConstructor();
    boolean isOverride();
    List getParameters();
    List getParameterNames();
    List getExceptions();
    List getAnnotations();
    int getLineCount();
    int getCyclomaticComplexity();
    String getSignature();
    List getCalledMethods();
    List getAccessedFields();
    String toString();
}
```

```
class ModuleInfo {
    String name;
    String path;
    String groupId;
    String artifactId;
    String version;
    int classCount;
    long totalLines;

    // --- 7 个方法 ---
    String getName();
    String getPath();
    String getGroupId();
    String getArtifactId();
    String getVersion();
    int getClassCount();
    long getTotalLines();
}
```

```
class ProjectModel {
    String projectName;
    String rootPath;
    String buildType;
    String groupId;
    String artifactId;
    String currentVersion;
    String cve;
    String severity;
    String description;

    // --- 22 个方法 ---
    String getProjectName();
    String getProjectPath();
    String getBuildType();
    String getJavaVersion();
    List getModules();
    List getClasses();
    List getDependencies();
    Map> getPackageDependencies();
    Map> getCallGraph();
    ProjectStats getStats();
    List getApiEndpoints();
    Map> getBeanDependencies();
    String getProjectPattern();
    boolean isSpringBoot();
    List getDatabaseTables();
    Map getMapperSql();
    List getCallChains();
    List getCriticalChains();
    Map getConfigProperties();
    List getBeanInfos();
    List getVulnFindings();
    String toString();
}
```

```
class ProjectStats {
    int totalFiles;
    int totalLines;
    int totalCodeLines;
    int totalCommentLines;
    int totalClasses;
    int totalInterfaces;
    int totalEnums;
    int totalRecords;
    int totalAnnotations;
    int totalMethods;
    int totalFields;
    int totalDependencies;
    double commentRatio;
    double averageMethodLines;
    int maxComplexity;
    int godClassCount;
    int longMethodCount;
    int highComplexityCount;

    // --- 19 个方法 ---
    int getTotalFiles();
    int getTotalLines();
    int getTotalCodeLines();
    int getTotalCommentLines();
    int getTotalClasses();
    int getTotalInterfaces();
    int getTotalEnums();
    int getTotalRecords();
    int getTotalAnnotations();
    int getTotalMethods();
    int getTotalFields();
    int getTotalDependencies();
    double getCommentRatio();
    double getAverageMethodLines();
    int getMaxComplexity();
    int getGodClassCount();
    int getLongMethodCount();
    int getHighComplexityCount();
    String getHealthLevel();
}
```

### com.projectassistant.reporter

```
class ReportGenerator {
    ProjectModel project;
    List<AnalysisResult> analysisResults;

    // --- 19 个方法 ---
    String generateMarkdown();
    String generateHtml();
    String mdToHtml(String);
    String inline(String);
    String escHtml(String);
    void saveReport(String, String);
    void writeHeader();
    void writeSummary();
    void addStatCard(String, String);
    void writeProjectPattern();
    void writeApiEndpoints();
    void writeBeanDependencies();
    void writeDatabaseTables();
    void writeCallChains();
    void writeProjectStructure();
    void writePackageDeps();
    void writeClassDetails();
    void writeAnalysisResults();
    void writeFooter();
}
```

### com.projectassistant.scanner

```
class ProjectScanner {
    Path rootPath;
    ProjectModel project;
    return project;
    return null;
    List<Path> javaFiles;
    return source;

    // --- 13 个方法 ---
    ProjectModel scan();
    void detectBuildType();
    else if();
    void scanModules();
    void parseMavenPom(Path, List<ModuleInfo>);
    String extractXmlTag(String, String);
    void scanJavaFiles();
    ClassInfo parseJavaFile(Path);
    int findBodyStart(String, int);
    int findBodyEnd(String, int);
    int findMatchingParen(String, int);
    boolean isJavaKeyword(String);
    return switch(var);
}
```

```
class VulnScanner {
    List<DependencyInfo> dependencies;
    return findings;
    return false;
    String groupId;
    String artifactId;
    String affectedRange;
    String cve;
    String severity;
    String description;
    String groupId;
    String artifactId;
    String currentVersion;
    String cve;
    String severity;
    String description;

    // --- 6 个方法 ---
    List scan();
    boolean isAffected(String, String);
    int compareVersions(String, String);
    int parseIntSafe(String);
    VulnRule vuln(String, String, String, String, String, String);
    List getFindings();
}
```

### com.projectassistant.spring

```
class ApiEndpoint {
    String httpMethod;
    String path;
    String controllerClass;
    String methodName;
    String returnType;
    String description;
    boolean secured;
    String requestBodyType;
    String consumes;
    String produces;
    String summary;
    boolean deprecated;
    String name;
    String type;
    boolean required;
    String defaultValue;
    String description;

    // --- 25 个方法 ---
    String getHttpMethod();
    String getPath();
    String getControllerClass();
    String getMethodName();
    String getReturnType();
    List getParameters();
    List getAnnotations();
    String getDescription();
    boolean isSecured();
    List getRequestParams();
    List getPathVariables();
    String getRequestBodyType();
    List getRequestHeaders();
    String getConsumes();
    String getProduces();
    String getSummary();
    boolean isDeprecated();
    String getSignature();
    String toString();
    String getName();
    String getType();
    boolean isRequired();
    String getDefaultValue();
    String getDescription();
    String toString();
}
```

```
class BeanInfo {
    String className;
    String simpleName;
    String beanName;
    String role;
    String scope;
    boolean isPrimary;
    boolean isLazy;
    String fieldName;
    String targetType;
    String targetBeanName;
    String injectionType;
    String annotation;
    String qualifier;

    // --- 17 个方法 ---
    String getFieldName();
    String getTargetType();
    String getTargetBeanName();
    String getInjectionType();
    String getAnnotation();
    String getQualifier();
    String getClassName();
    String getSimpleName();
    String getBeanName();
    String getRole();
    String getScope();
    boolean isPrimary();
    boolean isLazy();
    List getInjections();
    List getInjectedBy();
    void addInjection(InjectionPoint);
    void addInjectedBy(String);
}
```

```
class SpringScanner {
    List<ClassInfo> classes;
    return true;
    return false;
    return null;
    return null;
    return ip;
    return cleanType;
    return false;

    // --- 35 个方法 ---
    void scan();
    void scanProjectPattern();
    boolean hasAnnotation(ClassInfo, String);
    void scanControllers();
    String extractClassLevelPath(ClassInfo);
    void scanMethodEndpoint();
    pn, cleanType();
    pn, cleanType();
    String cleanType(String);
    boolean isSimpleType(String);
    String extractApiSummary(MethodInfo);
    return decamelize(var);
    String decamelize(String);
    String inferHttpMethod(String, String);
    String extractMethodPath(String);
    String normalizePath(String);
    void scanBeanDependencies();
    String detectBeanRole(ClassInfo);
    void registerBean(ClassInfo, String, String);
    String extractBeanName(ClassInfo);
    InjectionPoint resolveFieldInjection(ClassInfo, FieldInfo);
    String resolveBeanForType(String);
    void detectCircularDependencies();
    boolean hasCycle(String, Set<String>, Set<String>);
    int countInjections();
    void scanConfigProperties();
    Map getBeanInfoMap();
    Map getBeanTypeMap();
    List getEndpoints();
    Map> getBeanDependencies();
    Map getConfigProperties();
    String getProjectPattern();
    boolean isSpringBoot();
    String getServerPort();
    String getContextPath();
}
```

### com.projectassistant.sql

```
class SchemaParser {
    List<ClassInfo> classes;
    List<java.nio.file.Path> xmlFiles;
    return tables;
    return col;
    return result;

    // --- 15 个方法 ---
    List parse();
    void parseEntityClasses();
    void parseXmlResultMaps();
    String inferTableFromResultMap(String);
    return toSnakeCase(var);
    return toSnakeCase(var);
    return toSnakeCase(var);
    String inferTableName(ClassInfo);
    return toSnakeCase(var);
    TableInfo.Column parseColumn(FieldInfo, ClassInfo);
    String mapJavaToSql(String);
    String toSnakeCase(String);
    int findClosingTag(String, int, String);
    void mergeTables();
    List getTables();
}
```

```
class SqlParser {
    List<ClassInfo> classes;
    List<Path> xmlFiles;
    return null;

    // --- 12 个方法 ---
    void scan();
    void detectORM();
    void scanJPAEntities();
    void scanMyBatisMappers();
    String detectSqlType(String);
    void scanInlineSQL();
    String toSnakeCase(String);
    List getTables();
    Map getMapperSql();
    Map getEntityTableMap();
    boolean hasMyBatis();
    boolean hasJPA();
}
```

```
class TableInfo {
    String tableName;
    String entityClass;
    String comment;
    String columnName;
    String fieldName;
    String javaType;
    boolean primaryKey;
    boolean autoIncrement;
    String defaultValue;
    String comment;

    // --- 16 个方法 ---
    String getTableName();
    String getEntityClass();
    String getComment();
    List getColumns();
    List getPrimaryKeys();
    String toString();
    String getColumnName();
    String getFieldName();
    String getJavaType();
    String getSqlType();
    int getLength();
    boolean isPrimaryKey();
    boolean isAutoIncrement();
    boolean isNullable();
    String getDefaultValue();
    String getComment();
}
```

### com.projectassistant.web

```
class WebServer {
    volatile ScanTask currentTask;
    String cachedHtml;
    volatile String chatApiKey;
    String line;

    // --- 1 个方法 ---
    void start(String[]);
}
```

## 9. 业务流

**projectassistant**: 1 个类

**analyzer**: 1 个类

**chain**: 2 个类

**chat**: 1 个类

**config**: 1 个类

**db**: 1 个类

**knowledge**: 1 个类

**model**: 7 个类

**reporter**: 1 个类

**scanner**: 2 个类

**spring**: 3 个类

**sql**: 3 个类

**web**: 1 个类

## 10. 配置

（无）

## 11. 对 Agent 的提示

| 用户想问 | 看哪节 |
|---|---|
| 这是什么项目？ | 1. 概览 / 2. 架构 |
| 有哪些接口？ | 3. API |
| 数据库怎么设计的？ | 4. 数据库 |
| 改这个字段影响哪？ | 5. Bean / 6. 调用链 |
| 帮我加个接口 | 8. 业务流 / 7. 关键类 |
| 这个配置是什么意思？ | 9. 配置 |

---
> 由 Java老狗 生成 | 安装命令: `copaw skill install <this-dir>`
