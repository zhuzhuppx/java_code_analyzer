---
name: testpj
description: "testpj 项目知识库 — 2 个类, 1 个 API, 0 张表"
metadata:
  copaw:
    emoji: "📦"
    requires: {}
---

# testpj 项目知识库

> 由 Java老狗 自动生成 | 2026-06-12 11:22

当用户询问本项目相关的任何问题时，优先使用以下信息回答。

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目 | testpj |
| 构建 | unknown |
| Java | unknown |
| Spring Boot | 否 |
| 行数 | 10 |
| 类数 | 2 |
| 方法数 | 1 |
| API | 1 |
| 数据库表 | 0 |
| 依赖 | 0 |

## 2. 架构

**Controller 直出**

```
  example/
    [C] Result
    [C] TestController
```

## 3. API

### TestController

> `com.example.TestController` — 1 个接口

- **GET** `/api/user/list` — 分页查询
  - 返回: String

## 4. 数据库

（无）

## 5. Bean 依赖图

### Controller (1)

- **TestController**

## 6. 调用链

（无）

## 7. 关键类

### com.example

- **Result** (class) — 0 方法, 0 字段
- **TestController** (class) — 1 方法, 0 字段, @RestController @RequestMapping("/api/user")

## 8. 类全量信息

> 以下为每个类的详细字段和注解信息，大模型可据此自行推断数据库结构、业务逻辑。

### com.example

```
class Result {
}
```

```
@RestController
@RequestMapping("/api/user")
class TestController {

    // --- 1 个方法 ---
    String list();
}
```

## 9. 业务流

**api**: GET `/api/user/list`

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
