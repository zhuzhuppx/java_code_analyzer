# 项目知识库

> 由 ProjectAssistant 自动生成，专为大模型理解优化
> 生成时间: 2026-06-10 17:59:42

---

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目名称 | project-assistant |
| 构建工具 | maven |
| Java 版本 | unknown |
| Spring Boot | 否 |
| 代码总行数 | 3481 |
| 类/接口数 | 18 |
| 方法数 | 245 |
| API 端点 | 0 |
| 数据库表 | 0 |
| 外部依赖 | 3 |

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

- **Main** (class) — 1 方法, 0 字段

### com.projectassistant.analyzer

- **ProjectAnalyzer** (class) — 26 方法, 9 字段

### com.projectassistant.chain

- **CallChain** (class) — 6 方法, 2 字段, @Override
- **CallChainAnalyzer** (class) — 5 方法, 2 字段

### com.projectassistant.knowledge

- **KnowledgeBaseGenerator** (class) — 15 方法, 2 字段

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

## 8. 业务流

无法推断。

## 9. 配置

无。

## 10. 开发指南

**启动类**: 未检测到
**架构**: unknown
**ORM**: 无

### 开发场景

当被问到以下问题时，根据本项目知识库回答：

1. **这个项目是干什么的？** -> 看 API 路由 + 业务流章节
2. **改一个字段影响哪些地方？** -> 看数据库 + 调用链
3. **帮我加个接口** -> 参考现有 API 风格
4. **这个接口的业务逻辑？** -> 看调用链 + 关键类

---
> 知识库由 ProjectAssistant 生成 | 配合大模型使用效果更佳
