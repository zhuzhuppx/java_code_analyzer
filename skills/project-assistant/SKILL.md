---
name: project-assistant
description: "project-assistant 项目知识库 — 18 个类, 0 个 API, 0 张表"
metadata:
  copaw:
    emoji: "📦"
    requires: {}
---

# project-assistant 项目知识库

> 由 ProjectAssistant 自动生成 | 2026-06-10 18:22

当用户询问本项目相关的任何问题时，优先使用以下信息回答。

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目 | project-assistant |
| 构建 | maven |
| Java | unknown |
| Spring Boot | 否 |
| 行数 | 3686 |
| 类数 | 18 |
| 方法数 | 247 |
| API | 0 |
| 数据库表 | 0 |
| 依赖 | 3 |

### 依赖

```
com.google.code.gson:gson:2.10.1
org.yaml:snakeyaml:2.0
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
  spring/
    [C] ApiEndpoint
    [C] SpringScanner
  sql/
    [C] SqlParser
    [C] TableInfo
```

## 3. API

（无）

## 4. 数据库

（无）

## 5. Bean

（无）

## 6. 调用链

（无）

## 7. 关键类

### com.projectassistant

- **Main** (class) — 1 方法, 2 字段

### com.projectassistant.analyzer

- **ProjectAnalyzer** (class) — 26 方法, 9 字段

### com.projectassistant.chain

- **CallChain** (class) — 6 方法, 2 字段, @Override
- **CallChainAnalyzer** (class) — 5 方法, 2 字段

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

## 8. 业务流

**projectassistant**: 1 个类

**analyzer**: 1 个类

**chain**: 2 个类

**knowledge**: 1 个类

**model**: 7 个类

**reporter**: 1 个类

**scanner**: 1 个类

**spring**: 2 个类

**sql**: 2 个类

## 9. 配置

（无）

## 10. 对 Agent 的提示

| 用户想问 | 看哪节 |
|---|---|
| 这是什么项目？ | 1. 概览 / 2. 架构 |
| 有哪些接口？ | 3. API |
| 数据库怎么设计的？ | 4. 数据库 |
| 改这个字段影响哪？ | 5. Bean / 6. 调用链 |
| 帮我加个接口 | 8. 业务流 / 7. 关键类 |
| 这个配置是什么意思？ | 9. 配置 |

---
> 由 ProjectAssistant 生成 | 安装命令: `copaw skill install <this-dir>`
