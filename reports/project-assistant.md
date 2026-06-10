# 项目扫描报告

| 项目 | 值 |
|---|---|
| **项目名称** | project-assistant |
| **项目路径** | /app/working/workspaces/D2GPcF/project-assistant |
| **Java 版本** | unknown |
| **生成时间** | 2026-06-10 18:13:01 |
| **总类数** | 19 |
| **总行数** | 3764 |

## 统计概要

<div class="stats-grid">
  <div class="stat-card"><div class="stat-value">19</div><div class="stat-label">Java 类</div></div>
  <div class="stat-card"><div class="stat-value">3764</div><div class="stat-label">总行数</div></div>
  <div class="stat-card"><div class="stat-value">249</div><div class="stat-label">方法数</div></div>
  <div class="stat-card"><div class="stat-value">113</div><div class="stat-label">字段数</div></div>
  <div class="stat-card"><div class="stat-value">11.7 行</div><div class="stat-label">平均方法长度</div></div>
  <div class="stat-card"><div class="stat-value">7</div><div class="stat-label">平均复杂度</div></div>
  <div class="stat-card"><div class="stat-value">3</div><div class="stat-label">上帝类</div></div>
  <div class="stat-card"><div class="stat-value">0</div><div class="stat-label">长方法</div></div>
  <div class="stat-card"><div class="stat-value">0</div><div class="stat-label">高复杂度</div></div>
  <div class="stat-card"><div class="stat-value">3</div><div class="stat-label">依赖数</div></div>
</div>

## 项目架构模式

| 维度 | 值 |
|---|---|
| **架构模式** | unknown |
| **Spring Boot** | 否 |
| **Java 版本** | unknown |
| **构建工具** | maven |

## API 路由

未检测到 API 端点（非 Spring Web 项目或无法解析）。

## Bean 依赖

未检测到依赖注入关系。

## 数据库结构

未检测到数据库映射（无 JPA Entity 或无 MyBatis Mapper）。

## 调用链分析

未追踪到调用链。

## 项目结构

```
project-assistant/
  +-- com/projectassistant/
  |   +-- [C] Main.java
  +-- com/projectassistant/analyzer/
  |   +-- [C] ProjectAnalyzer.java
  +-- com/projectassistant/chain/
  |   +-- [C] CallChain.java
  |   +-- [C] CallChainAnalyzer.java
  +-- com/projectassistant/knowledge/
  |   +-- [C] KnowledgeBaseGenerator.java
  |   +-- [C] SkillGenerator.java
  +-- com/projectassistant/model/
  |   +-- [C] ClassInfo.java
  |   +-- [C] DependencyInfo.java
  |   +-- [C] FieldInfo.java
  |   +-- [C] MethodInfo.java
  |   +-- [C] ModuleInfo.java
  |   +-- [C] ProjectModel.java
  |   +-- [C] ProjectStats.java
  +-- com/projectassistant/reporter/
  |   +-- [C] ReportGenerator.java
  +-- com/projectassistant/scanner/
  |   +-- [C] ProjectScanner.java
  +-- com/projectassistant/spring/
  |   +-- [C] ApiEndpoint.java
  |   +-- [C] SpringScanner.java
  +-- com/projectassistant/sql/
  |   +-- [C] SqlParser.java
  |   +-- [C] TableInfo.java
```

## 包依赖关系

| 包 | 依赖项 |
|---|---|
| `com.projectassistant.analyzer` | - |
| `com.projectassistant.sql` | `com.projectassistant.sql` |
| `com.projectassistant.chain` | - |
| `com.projectassistant` | - |
| `com.projectassistant.knowledge` | - |
| `com.projectassistant.reporter` | - |
| `com.projectassistant.model` | - |
| `com.projectassistant.spring` | - |
| `com.projectassistant.scanner` | - |

## 类详情

### [C] ProjectAnalyzer

- **全称**: `com.projectassistant.analyzer.ProjectAnalyzer`
- **类型**: class
- **行数**: 541
- **包**: `com.projectassistant.analyzer`
- **可见性**: public

#### 字段 (9)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `project` | `ProjectModel` |
| private | N | `findings` | `return` |
| private | N | `true` | `return` |
| private | N | `false` | `return` |
| private | N | `type` | `String` |
| private | N | `title` | `String` |
| private | N | `description` | `String` |
| private | N | `severity` | `int` |
| private | N | `suggestion` | `String` |

#### 方法 (26)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `analyze()` | `List` | - | 1 | 1 |
| private | `analyzeDependencyCycles()` | `void` | - | 1 | 3 |
| private | `hasCycle()` | `boolean` | - | 1 | 4 |
| private | `analyzeArchitectureLayers()` | `void` | - | 1 | 7 |
| package-private | `if()` | `else` | `pkg.contains("") ||` | 1 | 5 |
| package-private | `if()` | `else` | `pkg.contains("") || pkg.contains("") ||` | 1 | 4 |
| package-private | `if()` | `else` | `pkg.contains("") || pkg.contains("") ||` | 1 | 3 |
| package-private | `if()` | `else` | `pkg.contains("") || pkg.contains("") ||` | 1 | 2 |
| private | `analyzeDesignPatterns()` | `void` | - | 1 | 4 |
| private | `analyzeCodeSmells()` | `void` | - | 1 | 4 |
| package-private | `if()` | `else` | - | 1 | 3 |
| package-private | `if()` | `else` | `mi.getName().startsWith("") ||` | 1 | 3 |
| package-private | `if()` | `else` | `mi.getName().startsWith("") ||` | 1 | 2 |
| private | `analyzeUnusedCode()` | `void` | - | 1 | 4 |
| private | `analyzeTODOs()` | `void` | - | 1 | 3 |
| private | `analyzeSpringStructure()` | `void` | - | 1 | 3 |
| private | `analyzeNamingConventions()` | `void` | - | 1 | 3 |
| private | `analyzeCohesion()` | `void` | - | 1 | 4 |
| private | `analyzePublicApi()` | `void` | - | 1 | 2 |
| private | `addFinding()` | `void` | - | 1 | 1 |
| public | `getType()` | `String` | - | 1 | 1 |
| public | `getTitle()` | `String` | - | 1 | 1 |
| public | `getDescription()` | `String` | - | 1 | 1 |
| public | `getSeverity()` | `int` | - | 1 | 1 |
| public | `getSuggestion()` | `String` | - | 1 | 1 |
| public | `getSeverityLabel()` | `String` | - | 1 | 1 |

---

### [C] CallChain

- **全称**: `com.projectassistant.chain.CallChain`
- **类型**: class
- **行数**: 35
- **包**: `com.projectassistant.chain`
- **可见性**: public
- **注解**: `Override`

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `entryPoint` | `String` |
| private | N | `entryRole` | `String` |

#### 方法 (6)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getEntryPoint()` | `String` | - | 1 | 1 |
| public | `getEntryRole()` | `String` | - | 1 | 1 |
| public | `getCallPaths()` | `List>` | - | 1 | 1 |
| public | `getMaxDepth()` | `int` | - | 1 | 1 |
| public | `getLongestPath()` | `List` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] CallChainAnalyzer

- **全称**: `com.projectassistant.chain.CallChainAnalyzer`
- **类型**: class
- **行数**: 126
- **包**: `com.projectassistant.chain`
- **可见性**: public

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `classRoleMap` | `Map<String, String>` |
| private | N | `result` | `return` |

#### 方法 (5)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `analyze()` | `void` | - | 1 | 2 |
| private | `traceChain()` | `void` | - | 1 | 2 |
| public | `getCriticalChains()` | `List` | - | 1 | 5 |
| private | `inferRole()` | `String` | `String` | 1 | 1 |
| public | `getChains()` | `List` | - | 1 | 1 |

---

### [C] KnowledgeBaseGenerator

- **全称**: `com.projectassistant.knowledge.KnowledgeBaseGenerator`
- **类型**: class
- **行数**: 259
- **包**: `com.projectassistant.knowledge`
- **可见性**: public

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `project` | `ProjectModel` |
| private | N | `m` | `return` |

#### 方法 (15)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `generate()` | `String` | - | 1 | 1 |
| private | `preamble()` | `void` | - | 1 | 1 |
| private | `overview()` | `void` | - | 1 | 3 |
| private | `architecture()` | `void` | - | 1 | 3 |
| private | `apiCatalog()` | `void` | - | 1 | 3 |
| private | `databaseSchema()` | `void` | - | 1 | 4 |
| private | `beanGraph()` | `void` | - | 1 | 3 |
| private | `callChains()` | `void` | - | 1 | 2 |
| private | `keyClasses()` | `void` | - | 1 | 3 |
| private | `businessFlow()` | `void` | - | 1 | 4 |
| private | `inferAction()` | `String` | `ApiEndpoint` | 1 | 1 |
| private | `configurations()` | `void` | - | 1 | 2 |
| private | `devGuide()` | `void` | - | 1 | 1 |
| private | `shorten()` | `String` | `String` | 1 | 1 |
| public | `save()` | `void` | `String` | 1 | 1 |

---

### [C] SkillGenerator

- **全称**: `com.projectassistant.knowledge.SkillGenerator`
- **类型**: class
- **行数**: 256
- **包**: `com.projectassistant.knowledge`
- **可见性**: public

#### 字段 (1)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `project` | `ProjectModel` |

#### 方法 (4)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `save()` | `void` | `String` | 1 | 1 |
| private | `sanitize()` | `String` | `String` | 1 | 1 |
| private | `buildSkillMd()` | `String` | - | 1 | 3 |
| private | `shorten()` | `String` | `String` | 1 | 1 |

---

### [C] Main

- **全称**: `com.projectassistant.Main`
- **类型**: class
- **行数**: 179
- **包**: `com.projectassistant`
- **可见性**: public

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `extension` | `String` |
| private | N | `content` | `String` |

#### 方法 (1)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `main()` | `void` | `String[]` | 1 | 2 |

---

### [C] ClassInfo

- **全称**: `com.projectassistant.model.ClassInfo`
- **类型**: class
- **行数**: 138
- **包**: `com.projectassistant.model`
- **可见性**: public
- **注解**: `Override`

#### 字段 (14)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `fullyQualifiedName` | `String` |
| private | N | `packageName` | `String` |
| private | N | `simpleName` | `String` |
| private | N | `type` | `String` |
| private | N | `visibility` | `String` |
| private | N | `sourceFilePath` | `String` |
| private | N | `moduleName` | `String` |
| private | N | `isAbstract` | `boolean` |
| private | N | `isFinal` | `boolean` |
| private | N | `isStatic` | `boolean` |
| private | N | `superClassName` | `String` |
| private | N | `lineCount` | `int` |
| private | N | `codeLineCount` | `int` |
| private | N | `totalComplexity` | `int` |

#### 方法 (21)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getFullyQualifiedName()` | `String` | - | 1 | 1 |
| public | `getPackageName()` | `String` | - | 1 | 1 |
| public | `getSimpleName()` | `String` | - | 1 | 1 |
| public | `getType()` | `String` | - | 1 | 1 |
| public | `getVisibility()` | `String` | - | 1 | 1 |
| public | `getSourceFilePath()` | `String` | - | 1 | 1 |
| public | `getModuleName()` | `String` | - | 1 | 1 |
| public | `isAbstract()` | `boolean` | - | 1 | 1 |
| public | `isFinal()` | `boolean` | - | 1 | 1 |
| public | `isStatic()` | `boolean` | - | 1 | 1 |
| public | `getSuperClassName()` | `String` | - | 1 | 1 |
| public | `getInterfaces()` | `List` | - | 1 | 1 |
| public | `getTypeParameters()` | `List` | - | 1 | 1 |
| public | `getFields()` | `List` | - | 1 | 1 |
| public | `getMethods()` | `List` | - | 1 | 1 |
| public | `getAnnotations()` | `List` | - | 1 | 1 |
| public | `getInnerClasses()` | `List` | - | 1 | 1 |
| public | `getLineCount()` | `int` | - | 1 | 1 |
| public | `getCodeLineCount()` | `int` | - | 1 | 1 |
| public | `getTotalComplexity()` | `int` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] DependencyInfo

- **全称**: `com.projectassistant.model.DependencyInfo`
- **类型**: class
- **行数**: 53
- **包**: `com.projectassistant.model`
- **可见性**: public
- **注解**: `Override`

#### 字段 (6)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `groupId` | `String` |
| private | N | `artifactId` | `String` |
| private | N | `version` | `String` |
| private | N | `scope` | `String` |
| private | N | `optional` | `boolean` |
| private | N | `type` | `String` |

#### 方法 (8)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getGroupId()` | `String` | - | 1 | 1 |
| public | `getArtifactId()` | `String` | - | 1 | 1 |
| public | `getVersion()` | `String` | - | 1 | 1 |
| public | `getScope()` | `String` | - | 1 | 1 |
| public | `isOptional()` | `boolean` | - | 1 | 1 |
| public | `getType()` | `String` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |
| public | `toGav()` | `String` | - | 1 | 1 |

---

### [C] FieldInfo

- **全称**: `com.projectassistant.model.FieldInfo`
- **类型**: class
- **行数**: 49
- **包**: `com.projectassistant.model`
- **可见性**: public
- **注解**: `Override`

#### 字段 (7)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `name` | `String` |
| private | N | `type` | `String` |
| private | N | `visibility` | `String` |
| private | N | `isStatic` | `boolean` |
| private | N | `isFinal` | `boolean` |
| private | N | `hasGetter` | `boolean` |
| private | N | `hasSetter` | `boolean` |

#### 方法 (9)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getName()` | `String` | - | 1 | 1 |
| public | `getType()` | `String` | - | 1 | 1 |
| public | `getVisibility()` | `String` | - | 1 | 1 |
| public | `isStatic()` | `boolean` | - | 1 | 1 |
| public | `isFinal()` | `boolean` | - | 1 | 1 |
| public | `getAnnotations()` | `List` | - | 1 | 1 |
| public | `isHasGetter()` | `boolean` | - | 1 | 1 |
| public | `isHasSetter()` | `boolean` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] MethodInfo

- **全称**: `com.projectassistant.model.MethodInfo`
- **类型**: class
- **行数**: 93
- **包**: `com.projectassistant.model`
- **可见性**: public
- **注解**: `Override`

#### 字段 (12)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `name` | `String` |
| private | N | `returnType` | `String` |
| private | N | `visibility` | `String` |
| private | N | `isAbstract` | `boolean` |
| private | N | `isStatic` | `boolean` |
| private | N | `isFinal` | `boolean` |
| private | N | `isSynchronized` | `boolean` |
| private | N | `isConstructor` | `boolean` |
| private | N | `isOverride` | `boolean` |
| private | N | `lineCount` | `int` |
| private | N | `cyclomaticComplexity` | `int` |
| private | N | `signature` | `String` |

#### 方法 (19)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getName()` | `String` | - | 1 | 1 |
| public | `getReturnType()` | `String` | - | 1 | 1 |
| public | `getVisibility()` | `String` | - | 1 | 1 |
| public | `isAbstract()` | `boolean` | - | 1 | 1 |
| public | `isStatic()` | `boolean` | - | 1 | 1 |
| public | `isFinal()` | `boolean` | - | 1 | 1 |
| public | `isSynchronized()` | `boolean` | - | 1 | 1 |
| public | `isConstructor()` | `boolean` | - | 1 | 1 |
| public | `isOverride()` | `boolean` | - | 1 | 1 |
| public | `getParameters()` | `List` | - | 1 | 1 |
| public | `getParameterNames()` | `List` | - | 1 | 1 |
| public | `getExceptions()` | `List` | - | 1 | 1 |
| public | `getAnnotations()` | `List` | - | 1 | 1 |
| public | `getLineCount()` | `int` | - | 1 | 1 |
| public | `getCyclomaticComplexity()` | `int` | - | 1 | 1 |
| public | `getSignature()` | `String` | - | 1 | 1 |
| public | `getCalledMethods()` | `List` | - | 1 | 1 |
| public | `getAccessedFields()` | `List` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] ModuleInfo

- **全称**: `com.projectassistant.model.ModuleInfo`
- **类型**: class
- **行数**: 42
- **包**: `com.projectassistant.model`
- **可见性**: public

#### 字段 (7)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `name` | `String` |
| private | N | `path` | `String` |
| private | N | `groupId` | `String` |
| private | N | `artifactId` | `String` |
| private | N | `version` | `String` |
| private | N | `classCount` | `int` |
| private | N | `totalLines` | `long` |

#### 方法 (7)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getName()` | `String` | - | 1 | 1 |
| public | `getPath()` | `String` | - | 1 | 1 |
| public | `getGroupId()` | `String` | - | 1 | 1 |
| public | `getArtifactId()` | `String` | - | 1 | 1 |
| public | `getVersion()` | `String` | - | 1 | 1 |
| public | `getClassCount()` | `int` | - | 1 | 1 |
| public | `getTotalLines()` | `long` | - | 1 | 1 |

---

### [C] ProjectModel

- **全称**: `com.projectassistant.model.ProjectModel`
- **类型**: class
- **行数**: 94
- **包**: `com.projectassistant.model`
- **可见性**: public
- **注解**: `Override`

#### 字段 (3)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `projectName` | `String` |
| private | N | `rootPath` | `String` |
| private | N | `buildType` | `String` |

#### 方法 (20)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getProjectName()` | `String` | - | 1 | 1 |
| public | `getProjectPath()` | `String` | - | 1 | 1 |
| public | `getBuildType()` | `String` | - | 1 | 1 |
| public | `getJavaVersion()` | `String` | - | 1 | 1 |
| public | `getModules()` | `List` | - | 1 | 1 |
| public | `getClasses()` | `List` | - | 1 | 1 |
| public | `getDependencies()` | `List` | - | 1 | 1 |
| public | `getPackageDependencies()` | `Map>` | - | 1 | 1 |
| public | `getCallGraph()` | `Map>` | - | 1 | 1 |
| public | `getStats()` | `ProjectStats` | - | 1 | 1 |
| public | `getApiEndpoints()` | `List` | - | 1 | 1 |
| public | `getBeanDependencies()` | `Map>` | - | 1 | 1 |
| public | `getProjectPattern()` | `String` | - | 1 | 1 |
| public | `isSpringBoot()` | `boolean` | - | 1 | 1 |
| public | `getDatabaseTables()` | `List` | - | 1 | 1 |
| public | `getMapperSql()` | `Map` | - | 1 | 1 |
| public | `getCallChains()` | `List` | - | 1 | 1 |
| public | `getCriticalChains()` | `List` | - | 1 | 1 |
| public | `getConfigProperties()` | `Map` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] ProjectStats

- **全称**: `com.projectassistant.model.ProjectStats`
- **类型**: class
- **行数**: 98
- **包**: `com.projectassistant.model`
- **可见性**: public

#### 字段 (18)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `totalFiles` | `int` |
| private | N | `totalLines` | `int` |
| private | N | `totalCodeLines` | `int` |
| private | N | `totalCommentLines` | `int` |
| private | N | `totalClasses` | `int` |
| private | N | `totalInterfaces` | `int` |
| private | N | `totalEnums` | `int` |
| private | N | `totalRecords` | `int` |
| private | N | `totalAnnotations` | `int` |
| private | N | `totalMethods` | `int` |
| private | N | `totalFields` | `int` |
| private | N | `totalDependencies` | `int` |
| private | N | `commentRatio` | `double` |
| private | N | `averageMethodLines` | `double` |
| private | N | `maxComplexity` | `int` |
| private | N | `godClassCount` | `int` |
| private | N | `longMethodCount` | `int` |
| private | N | `highComplexityCount` | `int` |

#### 方法 (19)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getTotalFiles()` | `int` | - | 1 | 1 |
| public | `getTotalLines()` | `int` | - | 1 | 1 |
| public | `getTotalCodeLines()` | `int` | - | 1 | 1 |
| public | `getTotalCommentLines()` | `int` | - | 1 | 1 |
| public | `getTotalClasses()` | `int` | - | 1 | 1 |
| public | `getTotalInterfaces()` | `int` | - | 1 | 1 |
| public | `getTotalEnums()` | `int` | - | 1 | 1 |
| public | `getTotalRecords()` | `int` | - | 1 | 1 |
| public | `getTotalAnnotations()` | `int` | - | 1 | 1 |
| public | `getTotalMethods()` | `int` | - | 1 | 1 |
| public | `getTotalFields()` | `int` | - | 1 | 1 |
| public | `getTotalDependencies()` | `int` | - | 1 | 1 |
| public | `getCommentRatio()` | `double` | - | 1 | 1 |
| public | `getAverageMethodLines()` | `double` | - | 1 | 1 |
| public | `getMaxComplexity()` | `int` | - | 1 | 1 |
| public | `getGodClassCount()` | `int` | - | 1 | 1 |
| public | `getLongMethodCount()` | `int` | - | 1 | 1 |
| public | `getHighComplexityCount()` | `int` | - | 1 | 1 |
| public | `getHealthLevel()` | `String` | - | 1 | 1 |

---

### [C] ReportGenerator

- **全称**: `com.projectassistant.reporter.ReportGenerator`
- **类型**: class
- **行数**: 446
- **包**: `com.projectassistant.reporter`
- **可见性**: public

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `project` | `ProjectModel` |
| private | N | `analysisResults` | `List<AnalysisResult>` |

#### 方法 (19)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `generateMarkdown()` | `String` | - | 1 | 1 |
| public | `generateHtml()` | `String` | - | 1 | 1 |
| private | `mdToHtml()` | `String` | `String` | 1 | 4 |
| private | `inline()` | `String` | `String` | 1 | 1 |
| private | `escHtml()` | `String` | `String` | 1 | 1 |
| public | `saveReport()` | `void` | `String, String` | 1 | 1 |
| private | `writeHeader()` | `void` | - | 1 | 1 |
| private | `writeSummary()` | `void` | - | 1 | 2 |
| private | `addStatCard()` | `void` | `String, String` | 1 | 1 |
| private | `writeProjectPattern()` | `void` | - | 1 | 1 |
| private | `writeApiEndpoints()` | `void` | - | 1 | 2 |
| private | `writeBeanDependencies()` | `void` | - | 1 | 2 |
| private | `writeDatabaseTables()` | `void` | - | 1 | 2 |
| private | `writeCallChains()` | `void` | - | 1 | 2 |
| private | `writeProjectStructure()` | `void` | - | 1 | 3 |
| private | `writePackageDeps()` | `void` | - | 1 | 2 |
| private | `writeClassDetails()` | `void` | - | 1 | 3 |
| private | `writeAnalysisResults()` | `void` | - | 1 | 2 |
| private | `writeFooter()` | `void` | - | 1 | 1 |

---

### [C] ProjectScanner

- **全称**: `com.projectassistant.scanner.ProjectScanner`
- **类型**: class
- **行数**: 781
- **包**: `com.projectassistant.scanner`
- **可见性**: public

#### 字段 (6)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `rootPath` | `Path` |
| private | N | `project` | `ProjectModel` |
| private | N | `project` | `return` |
| private | N | `null` | `return` |
| private | N | `javaFiles` | `List<Path>` |
| private | N | `source` | `return` |

#### 方法 (13)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `scan()` | `ProjectModel` | - | 1 | 1 |
| private | `detectBuildType()` | `void` | - | 1 | 2 |
| package-private | `if()` | `else` | - | 1 | 1 |
| private | `scanModules()` | `void` | - | 1 | 3 |
| private | `parseMavenPom()` | `void` | `Path, List<ModuleInfo>` | 1 | 2 |
| private | `extractXmlTag()` | `String` | `String, String` | 1 | 2 |
| private | `scanJavaFiles()` | `void` | - | 1 | 1 |
| private | `parseJavaFile()` | `ClassInfo` | `Path` | 1 | 1 |
| private | `findBodyStart()` | `int` | `String, int` | 1 | 1 |
| private | `findBodyEnd()` | `int` | `String, int` | 1 | 4 |
| private | `findMatchingParen()` | `int` | `String, int` | 1 | 3 |
| private | `isJavaKeyword()` | `boolean` | `String` | 1 | 1 |
| package-private | `switch()` | `return` | `var` | 1 | 1 |

---

### [C] ApiEndpoint

- **全称**: `com.projectassistant.spring.ApiEndpoint`
- **类型**: class
- **行数**: 40
- **包**: `com.projectassistant.spring`
- **可见性**: public
- **注解**: `Override`

#### 字段 (7)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `httpMethod` | `String` |
| private | N | `path` | `String` |
| private | N | `controllerClass` | `String` |
| private | N | `methodName` | `String` |
| private | N | `returnType` | `String` |
| private | N | `description` | `String` |
| private | N | `secured` | `boolean` |

#### 方法 (10)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getHttpMethod()` | `String` | - | 1 | 1 |
| public | `getPath()` | `String` | - | 1 | 1 |
| public | `getControllerClass()` | `String` | - | 1 | 1 |
| public | `getMethodName()` | `String` | - | 1 | 1 |
| public | `getReturnType()` | `String` | - | 1 | 1 |
| public | `getParameters()` | `List` | - | 1 | 1 |
| public | `getAnnotations()` | `List` | - | 1 | 1 |
| public | `getDescription()` | `String` | - | 1 | 1 |
| public | `isSecured()` | `boolean` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |

---

### [C] SpringScanner

- **全称**: `com.projectassistant.spring.SpringScanner`
- **类型**: class
- **行数**: 272
- **包**: `com.projectassistant.spring`
- **可见性**: public

#### 字段 (1)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `classes` | `List<ClassInfo>` |

#### 方法 (19)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `scan()` | `void` | - | 1 | 1 |
| private | `scanProjectPattern()` | `void` | - | 1 | 3 |
| private | `scanControllers()` | `void` | - | 1 | 3 |
| private | `extractClassLevelPath()` | `String` | `ClassInfo` | 1 | 3 |
| private | `scanMethodEndpoint()` | `void` | - | 1 | 2 |
| private | `inferHttpMethod()` | `String` | `String, String` | 1 | 1 |
| private | `extractMethodPath()` | `String` | `String` | 1 | 2 |
| private | `normalizePath()` | `String` | `String` | 1 | 1 |
| private | `scanBeanDependencies()` | `void` | - | 1 | 4 |
| private | `isSpringBean()` | `boolean` | `String` | 1 | 1 |
| private | `scanConfigProperties()` | `void` | - | 1 | 5 |
| public | `getEndpoints()` | `List` | - | 1 | 1 |
| public | `getBeanDependencies()` | `Map>` | - | 1 | 1 |
| public | `getBeanTypeMap()` | `Map` | - | 1 | 1 |
| public | `getConfigProperties()` | `Map` | - | 1 | 1 |
| public | `getProjectPattern()` | `String` | - | 1 | 1 |
| public | `isSpringBoot()` | `boolean` | - | 1 | 1 |
| public | `getServerPort()` | `String` | - | 1 | 1 |
| public | `getContextPath()` | `String` | - | 1 | 1 |

---

### [C] SqlParser

- **全称**: `com.projectassistant.sql.SqlParser`
- **类型**: class
- **行数**: 197
- **包**: `com.projectassistant.sql`
- **可见性**: public

#### 字段 (2)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `classes` | `List<ClassInfo>` |
| private | N | `null` | `return` |

#### 方法 (12)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `scan()` | `void` | - | 1 | 1 |
| private | `detectORM()` | `void` | - | 1 | 3 |
| private | `scanJPAEntities()` | `void` | - | 1 | 4 |
| private | `scanMyBatisMappers()` | `void` | - | 1 | 4 |
| private | `detectSqlType()` | `String` | `String` | 1 | 1 |
| private | `scanInlineSQL()` | `void` | - | 1 | 1 |
| private | `toSnakeCase()` | `String` | `String` | 1 | 1 |
| public | `getTables()` | `List` | - | 1 | 1 |
| public | `getMapperSql()` | `Map` | - | 1 | 1 |
| public | `getEntityTableMap()` | `Map` | - | 1 | 1 |
| public | `hasMyBatis()` | `boolean` | - | 1 | 1 |
| public | `hasJPA()` | `boolean` | - | 1 | 1 |

---

### [C] TableInfo

- **全称**: `com.projectassistant.sql.TableInfo`
- **类型**: class
- **行数**: 65
- **包**: `com.projectassistant.sql`
- **可见性**: public
- **注解**: `Override`

#### 字段 (10)

| 可见性 | 静态 | 名称 | 类型 |
|---|---|---|---|
| private | N | `tableName` | `String` |
| private | N | `entityClass` | `String` |
| private | N | `comment` | `String` |
| private | N | `columnName` | `String` |
| private | N | `fieldName` | `String` |
| private | N | `javaType` | `String` |
| private | N | `primaryKey` | `boolean` |
| private | N | `autoIncrement` | `boolean` |
| private | N | `defaultValue` | `String` |
| private | N | `comment` | `String` |

#### 方法 (16)

| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |
|---|---|---|---|---|---|
| public | `getTableName()` | `String` | - | 1 | 1 |
| public | `getEntityClass()` | `String` | - | 1 | 1 |
| public | `getComment()` | `String` | - | 1 | 1 |
| public | `getColumns()` | `List` | - | 1 | 1 |
| public | `getPrimaryKeys()` | `List` | - | 1 | 1 |
| public | `toString()` | `String` | - | 1 | 1 |
| public | `getColumnName()` | `String` | - | 1 | 1 |
| public | `getFieldName()` | `String` | - | 1 | 1 |
| public | `getJavaType()` | `String` | - | 1 | 1 |
| public | `getSqlType()` | `String` | - | 1 | 1 |
| public | `getLength()` | `int` | - | 1 | 1 |
| public | `isPrimaryKey()` | `boolean` | - | 1 | 1 |
| public | `isAutoIncrement()` | `boolean` | - | 1 | 1 |
| public | `isNullable()` | `boolean` | - | 1 | 1 |
| public | `getDefaultValue()` | `String` | - | 1 | 1 |
| public | `getComment()` | `String` | - | 1 | 1 |

---

## 分析发现

### 严重问题 (2)

| # | 分类 | 描述 | 建议 |
|---|---|---|---|
| 1 | 💩 上帝类 | com.projectassistant.analyzer.ProjectAnalyzer 有 26 个方法 | 建议拆分为多个职责单一的类 |
| 2 | 💩 上帝类 | com.projectassistant.model.ClassInfo 有 21 个方法 | 建议拆分为多个职责单一的类 |

### 重要问题 (21)

| # | 分类 | 描述 | 建议 |
|---|---|---|---|
| 1 | 💩 大类别 | com.projectassistant.analyzer.ProjectAnalyzer 有 541 行 | 建议拆分为多个小类 |
| 2 | 💩 大类别 | com.projectassistant.scanner.ProjectScanner 有 781 行 | 建议拆分为多个小类 |
| 3 | 低内聚 | ProjectAnalyzer 内聚度=0.00（仅 0/26 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 4 | 低内聚 | CallChain 内聚度=0.00（仅 0/6 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 5 | 低内聚 | CallChainAnalyzer 内聚度=0.00（仅 0/5 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 6 | 低内聚 | KnowledgeBaseGenerator 内聚度=0.00（仅 0/15 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 7 | 低内聚 | SkillGenerator 内聚度=0.00（仅 0/4 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 8 | 低内聚 | Main 内聚度=0.00（仅 0/1 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 9 | 低内聚 | ClassInfo 内聚度=0.00（仅 0/21 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 10 | 低内聚 | DependencyInfo 内聚度=0.00（仅 0/8 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 11 | 低内聚 | FieldInfo 内聚度=0.00（仅 0/9 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 12 | 低内聚 | MethodInfo 内聚度=0.00（仅 0/19 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 13 | 低内聚 | ModuleInfo 内聚度=0.00（仅 0/7 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 14 | 低内聚 | ProjectModel 内聚度=0.00（仅 0/20 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 15 | 低内聚 | ProjectStats 内聚度=0.00（仅 0/19 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 16 | 低内聚 | ReportGenerator 内聚度=0.00（仅 0/19 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 17 | 低内聚 | ProjectScanner 内聚度=0.00（仅 0/13 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 18 | 低内聚 | ApiEndpoint 内聚度=0.00（仅 0/10 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 19 | 低内聚 | SpringScanner 内聚度=0.00（仅 0/19 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 20 | 低内聚 | SqlParser 内聚度=0.00（仅 0/12 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |
| 21 | 低内聚 | TableInfo 内聚度=0.00（仅 0/16 方法访问字段） | 方法不操作类字段，考虑是否应该放在其它类中 |

### 改进建议 (16)

| # | 分类 | 描述 | 建议 |
|---|---|---|---|
| 1 | 架构分层 | 架构分层概览:<br>  - Entity/Domain 层: 1 个包<br>  - 其他: 8 个包<br> | 清晰的层次结构有助于维护和扩展 |
| 2 | 可能的未使用方法 | ProjectAnalyzer.analyze() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 3 | 可能的未使用方法 | CallChainAnalyzer.analyze() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 4 | 可能的未使用方法 | KnowledgeBaseGenerator.generate() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 5 | 可能的未使用方法 | KnowledgeBaseGenerator.save() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 6 | 可能的未使用方法 | SkillGenerator.save() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 7 | 可能的未使用方法 | Main.main() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 8 | 可能的未使用方法 | ReportGenerator.generateMarkdown() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 9 | 可能的未使用方法 | ReportGenerator.generateHtml() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 10 | 可能的未使用方法 | ReportGenerator.saveReport() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 11 | 可能的未使用方法 | ProjectScanner.scan() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 12 | 可能的未使用方法 | SpringScanner.scan() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 13 | 可能的未使用方法 | SqlParser.scan() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 14 | 可能的未使用方法 | SqlParser.hasMyBatis() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 15 | 可能的未使用方法 | SqlParser.hasJPA() 在项目内部未被调用 | 考虑删除或标记为 @Deprecated |
| 16 | 待办事项 | 项目中共有 19 处 TODO/FIXME | 定期清理待办事项，避免技术债务累积 |

### 信息提示 (1)

| # | 分类 | 描述 | 建议 |
|---|---|---|---|
| 1 | Public API 规模 | 项目共有 168 个 public 方法 | public API 是项目的对外契约，变更需谨慎 |


---
> *由 ProjectAssistant 扫描器自动生成 *
