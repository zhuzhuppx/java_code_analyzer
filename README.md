# 🔍 ProjectAssistant

**比程序员更懂项目** — 智能 Java 项目工程扫描器

ProjectAssistant 是一款深度理解 Java 项目的静态分析工具。它不仅能扫描类结构、Spring 配置、API 端点、SQL 映射、调用链，还能将扫描结果转化为**大模型可直接理解的知识库**，支持 DeepSeek 对话问答，并自带 Web UI，让你像聊代码一样跟项目对话。

---

## ✨ 特性一览

### 🔬 深度扫描引擎

| 维度 | 扫描内容 |
|------|---------|
| **类结构** | 类、方法、字段、继承层次、接口实现、泛型参数 |
| **Spring** | Controller/Service/Repository 识别、`@RequestMapping` 路由、`@Autowired` 依赖注入、配置属性（`application.yml/properties`） |
| **SQL** | MyBatis Mapper XML 解析、表中继、`@Entity` 逆向推断表结构、SQL 语句提取 |
| **调用链** | 跨类方法调用链分析，生成完整链路图 |
| **安全** | 常见漏洞模式扫描（SQL 注入、XSS、敏感信息泄露等） |
| **依赖** | Maven/Gradle 依赖树、横向依赖（多模块）、版本冲突检测 |

### 🖥️ Web UI

无需安装任何 IDE 插件，浏览器打开即可用：

- **一键扫描** — 输入项目路径，秒级获取完整分析
- **项目历史** — 自动保存扫描结果，支持回溯浏览
- **智能对话** — 基于 DeepSeek，用自然语言询问项目细节
- **实时流式** — SSE 流式响应，打字机效果
- **状态看板** — 类、方法、行数、API 数据、健康评分一目了然

### 📤 多种输出格式

- **Markdown 报告** — 结构化输出，适合文档归档
- **HTML 报告** — 格式化展示，可直接浏览器打开
- **知识库（Knowledge Base）** — 专为大模型优化的纯文本知识结构，可直接作为 LLM 上下文
- **Agent Skill** — 自动生成可安装的 AI Agent Skill 文件

### 🤖 AI 集成

- 深度集成 **DeepSeek API**
- 扫描后直接提问：「系统有哪些 REST 接口？」「用户模块的表结构是什么？」「这个项目的架构是什么？」
- 知识库模式可配合任何 LLM（ChatGPT、Claude、DeepSeek 等）进行 RAG

---

## 🚀 快速开始

### 前置要求

- **Java 17+**
- **Maven 3.8+**
- （可选）DeepSeek API Key（用于对话功能）

### 编译打包

```bash
git clone [你的仓库地址]
cd java_code_analyzer
mvn package -DskipTests
```

### 运行

#### Web UI（推荐）

```bash
# 启动 Web 服务，默认端口 8653
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar

# 自定义端口
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar --port 8080

# 带 DeepSeek API Key 启动（也可在页面中设置）
DEEPSEEK_API_KEY=sk-your-key java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar
```

打开浏览器访问 **http://localhost:8653**，输入项目路径即可扫描。

> 💡 默认路径为 JAR 所在目录，可在输入框中修改。

#### 命令行模式

```bash
# 生成 Markdown 报告（默认）
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar /path/to/project

# 生成 HTML 报告
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar /path/to/project html

# 生成知识库（适合喂给大模型）
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar /path/to/project knowledge

# 扫描并立即提问
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar /path/to/project --ask "这个项目使用了哪些设计模式？"

# 扫描后进入交互式对话
java -jar target/java_code_analyzer-1.0.0-jar-with-dependencies.jar /path/to/project --chat
```

---

## 🏗️ 架构

```
java_code_analyzer/
├── src/main/java/com/projectassistant/
│   ├── Main.java                          # 入口
│   ├── scanner/
│   │   ├── ProjectScanner.java            # 项目类扫描器（808 行）
│   │   └── VulnScanner.java               # 漏洞模式扫描器
│   ├── analyzer/
│   │   └── ProjectAnalyzer.java           # 综合分析引擎
│   ├── spring/
│   │   ├── SpringScanner.java             # Spring 框架扫描
│   │   ├── ApiEndpoint.java               # API 端点模型
│   │   └── BeanInfo.java                  # Bean 信息模型
│   ├── sql/
│   │   ├── SqlParser.java                 # SQL/MyBatis 解析
│   │   ├── SchemaParser.java              # 表结构逆向
│   │   └── TableInfo.java                 # 表信息模型
│   ├── chain/
│   │   ├── CallChainAnalyzer.java         # 调用链分析
│   │   └── CallChain.java                 # 调用链模型
│   ├── knowledge/
│   │   └── KnowledgeBaseGenerator.java    # 知识库生成（841 行）
│   ├── chat/
│   │   └── DeepSeekChat.java              # DeepSeek 对话集成
│   ├── reporter/
│   │   └── ReportGenerator.java           # Markdown/HTML 报告生成
│   ├── config/
│   │   └── ConfigParser.java              # 项目配置解析
│   ├── model/
│   │   ├── ProjectModel.java              # 项目数据模型
│   │   ├── ClassInfo.java                 # 类信息
│   │   ├── MethodInfo.java                # 方法信息
│   │   ├── FieldInfo.java                 # 字段信息
│   │   ├── ModuleInfo.java                # 模块信息
│   │   ├── DependencyInfo.java            # 依赖信息
│   │   └── ProjectStats.java              # 统计数据模型
│   ├── db/
│   │   └── DatabaseManager.java           # H2 数据库持久化
│   └── web/
│       └── WebServer.java                 # HTTP Web 服务器
└── src/main/resources/
    └── webui.html                         # Web 前端页面
```

**核心数据流：**

```
项目路径 → ProjectScanner（扫描类结构）
         → SpringScanner（Spring 分析）
         → SqlParser（SQL 解析）
         → CallChainAnalyzer（调用链）
         → VulnScanner（安全扫描）
         → ProjectAnalyzer（综合分析）
         → ReportGenerator / KnowledgeBaseGenerator（输出）
```

---

## 🎯 典型使用场景

### 接手遗留项目

新接手一个不熟悉的 Java 项目？ProjectAssistant 能在一分钟内告诉你：
- 项目用了什么框架（Spring Boot、MyBatis、JPA...）
- 有多少个 Controller、Service、配置类
- 数据库表结构是什么样的
- API 端点列表和参数
- 最复杂的类和方法在哪里

### 代码审查

- 自动发现 SQL 注入、XSS 等常见安全漏洞
- 识别配置硬编码问题
- 检测循环依赖与方法调用风险

### LLM / Agent 集成

- 生成的知识库可直接作为大模型的 RAG 上下文
- Agent Skill 模块让 AI 助手能理解项目结构
- 配合 DeepSeek/ChatGPT/Claude，实现「聊代码」体验

---

## ⚙️ 技术栈

| 技术 | 用途 |
|------|------|
| Java 17 | 核心语言 |
| Maven | 构建管理 |
| Gson | JSON 序列化 |
| SnakeYAML | YAML 配置解析 |
| H2 Database | 扫描结果持久化 |
| JUnit 5 | 单元测试 |
| DeepSeek API | AI 对话集成 |
| SSE | 流式响应传输 |
| Zero 外部依赖 | 仅依赖 3 个第三方库 |

---

## 🤝 贡献

欢迎贡献代码、提 Issue 或建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feat/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某个特性'`)
4. 推送到远程 (`git push origin feat/amazing-feature`)
5. 提交 Pull Request

---

## 📄 License

MIT License — 详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- [DeepSeek](https://deepseek.com/) — 提供强大的 AI 对话能力
- [H2 Database](https://h2database.com/) — 轻量级嵌入式数据库
- 所有给这个项目提过 Issue 和建议的朋友

---

> **ProjectAssistant** — 不是代码搜索，是代码理解。
