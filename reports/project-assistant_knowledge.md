# 项目知识库

> 由 ProjectAssistant 自动生成，专为大模型理解优化
> 生成时间: 2026-06-10 18:47:45

---

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目名称 | project-assistant |
| 构建工具 | maven |
| Java 版本 | unknown |
| Spring Boot | 否 |
| 代码总行数 | 3982 |
| 类/接口数 | 19 |
| 方法数 | 248 |
| API 端点 | 0 |
| 数据库表 | 0 |
| 外部依赖 | 3 |
| 注释率 | 22.6% |
| 平均方法行数 | 12.4 |
| 最大圈复杂度 | 7 |

### 外部依赖

```
com.google.code.gson:gson:2.10.1
org.yaml:snakeyaml:2.0
org.junit.jupiter:junit-jupiter:5.10.0 [test]
```

## 2. 架构模式

**架构类型**: unknown

### 模块结构

```
  projectassistant/
    +-- [C] Main.java
    analyzer/
      +-- [C] ProjectAnalyzer.java
    chain/
      +-- [C] CallChain.java
      +-- [C] CallChainAnalyzer.java
    chat/
      +-- [C] DeepSeekChat.java
    knowledge/
      +-- [C] KnowledgeBaseGenerator.java
    model/
      +-- [C] ClassInfo.java
      +-- [C] DependencyInfo.java
      +-- [C] FieldInfo.java
      +-- [C] MethodInfo.java
      +-- [C] ModuleInfo.java
      +-- [C] ProjectModel.java
      +-- [C] ProjectStats.java
    reporter/
      +-- [C] ReportGenerator.java
    scanner/
      +-- [C] ProjectScanner.java
    spring/
      +-- [C] ApiEndpoint.java
      +-- [C] SpringScanner.java
    sql/
      +-- [C] SqlParser.java
      +-- [C] TableInfo.java
```

## 3. API 路由

无 API 端点。

## 4. 数据库

无数据库映射。

## 5. Bean 依赖

无。

## 6. 调用链

无。

## 7. 关键类

### com.projectassistant

- **Main** (class) — 2 方法, 2 字段

### com.projectassistant.analyzer

- **ProjectAnalyzer** (class) — 26 方法, 9 字段

### com.projectassistant.chain

- **CallChain** (class) — 6 方法, 2 字段, @Override
- **CallChainAnalyzer** (class) — 5 方法, 2 字段

### com.projectassistant.chat

- **DeepSeekChat** (class) — 0 方法, 0 字段

### com.projectassistant.knowledge

- **KnowledgeBaseGenerator** (class) — 17 方法, 2 字段

### com.projectassistant.model

- **ClassInfo** (class) — 21 方法, 14 字段, @Override
- **DependencyInfo** (class) — 8 方法, 6 字段, @Override
- **FieldInfo** (class) — 9 方法, 7 字段, @Override
- **MethodInfo** (class) — 19 方法, 12 字段, @Override
- **ModuleInfo** (class) — 7 方法, 7 字段
- **ProjectModel** (class) — 20 方法, 3 字段, @Override
- **ProjectStats** (class) — 19 方法, 18 字段

### com.projectassistant.reporter

- **ReportGenerator** (class) — 19 方法, 2 字段

### com.projectassistant.scanner

- **ProjectScanner** (class) — 13 方法, 6 字段

### com.projectassistant.spring

- **ApiEndpoint** (class) — 10 方法, 7 字段, @Override
- **SpringScanner** (class) — 19 方法, 1 字段

### com.projectassistant.sql

- **SqlParser** (class) — 12 方法, 2 字段
- **TableInfo** (class) — 16 方法, 10 字段, @Override

## 8. 类全量信息

> 以下为每个类的详细字段和注解信息，大模型可据此自行推断数据库结构、业务逻辑等。

### com.projectassistant.model

```
@Override
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
@Override
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
@Override
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
@Override
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
@Override
class ProjectModel {
    String projectName;
    String rootPath;
    String buildType;

    // --- 20 个方法 ---
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
@Override
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

### com.projectassistant.knowledge

```
class KnowledgeBaseGenerator {
    ProjectModel project;
    return m;

    // --- 17 个方法 ---
    String generate();
    void preamble();
    void overview();
    void architecture();
    void apiCatalog();
    void databaseSchema();
    void beanGraph();
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

### com.projectassistant.spring

```
@Override
class ApiEndpoint {
    String httpMethod;
    String path;
    String controllerClass;
    String methodName;
    String returnType;
    String description;
    boolean secured;

    // --- 10 个方法 ---
    String getHttpMethod();
    String getPath();
    String getControllerClass();
    String getMethodName();
    String getReturnType();
    List getParameters();
    List getAnnotations();
    String getDescription();
    boolean isSecured();
    String toString();
}
```

```
class SpringScanner {
    List<ClassInfo> classes;

    // --- 19 个方法 ---
    void scan();
    void scanProjectPattern();
    void scanControllers();
    String extractClassLevelPath(ClassInfo);
    void scanMethodEndpoint();
    String inferHttpMethod(String, String);
    String extractMethodPath(String);
    String normalizePath(String);
    void scanBeanDependencies();
    boolean isSpringBean(String);
    void scanConfigProperties();
    List getEndpoints();
    Map> getBeanDependencies();
    Map getBeanTypeMap();
    Map getConfigProperties();
    String getProjectPattern();
    boolean isSpringBoot();
    String getServerPort();
    String getContextPath();
}
```

### com.projectassistant.sql

```
class SqlParser {
    List<ClassInfo> classes;
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
@Override
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

## 9. 业务流

无法推断。

## 10. 配置

无。

## 11. 开发指南

**启动类**: 未检测到
**架构**: unknown
| 你想问 | 看哪节 |
|---|---|
| 这是什么项目？ | 1. 概览 / 2. 架构 |
| 有哪些接口？ | 3. API 路由 |
| 数据库怎么设计的？ | 4. 数据库 |
| 改这个字段影响哪？ | 5. Bean依赖 / 6. 调用链 |
| 帮我加个接口 | 9. 业务流 / 7. 关键类 |
| 这个类的字段和注解？ | 8. 类全量信息 |
| 这个配置是什么意思？ | 10. 配置 |

---
> 知识库由 ProjectAssistant 生成 | 配合大模型使用效果更佳
