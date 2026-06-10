package com.projectassistant;

import com.projectassistant.model.*;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.spring.SpringScanner;
import com.projectassistant.spring.ApiEndpoint;
import com.projectassistant.sql.SqlParser;
import com.projectassistant.sql.TableInfo;
import com.projectassistant.chain.CallChainAnalyzer;
import com.projectassistant.chain.CallChain;
import com.projectassistant.knowledge.KnowledgeBaseGenerator;
import com.projectassistant.chat.DeepSeekChat;
import com.projectassistant.reporter.ReportGenerator;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;

/**
 * ProjectAssistant - 主入口
 *
 * 彻底理解 Java 项目，像老狗一样熟悉代码
 *
 * 用法:
 *   java com.projectassistant.Main <项目路径> [格式|--ask "问题"|--chat]
 *
 * 格式:
 *   markdown          - Markdown 报告 (默认)
 *   html              - HTML 报告
 *   knowledge         - 知识库 (专为大模型优化)
 *   --ask "问题"      - 扫描后用 DeepSeek 回答
 *   --chat            - 扫描后进入交互式问答
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("用法: java com.projectassistant.Main <项目路径> [格式|--ask \"问题\"|--chat]");
            System.exit(1);
        }

        String projectPath = args[0];
        String mode = args.length >= 2 ? args[1] : "markdown";
        String askQuestion = null;
        boolean chatMode = false;

        // 解析模式
        if (mode.equals("--ask")) {
            if (args.length < 3) {
                System.err.println("错误: --ask 需要提供问题内容");
                System.exit(1);
            }
            askQuestion = args[2];
            mode = "knowledge";  // 先扫，生成知识库，再问
        } else if (mode.equals("--chat")) {
            chatMode = true;
            mode = "knowledge";
        }

        // 验证路径
        Path path = Paths.get(projectPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            System.err.println("错误: 路径不存在或不是目录: " + projectPath);
            System.exit(1);
        }

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  ProjectAssistant - Java 老狗级项目理解  ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("\n扫描项目: " + Paths.get(projectPath).toAbsolutePath().normalize());

        long totalStart = System.currentTimeMillis();

        // ==================== 1. 扫描 ====================
        System.out.println("\n[1/4] 扫描 Java 源码...");
        ProjectScanner scanner = new ProjectScanner(projectPath);
        ProjectModel project = scanner.scan();

        // ==================== 2. Spring 理解 ====================
        System.out.println("\n[2/4] Spring 框架理解...");
        SpringScanner springScanner = null;
        try {
            springScanner = new SpringScanner(project.getClasses());
            springScanner.scan();
            project.setApiEndpoints(springScanner.getEndpoints());
            project.setBeanDependencies(springScanner.getBeanDependencies());
            project.setProjectPattern(springScanner.getProjectPattern());
            project.setSpringBoot(springScanner.isSpringBoot());
            project.setConfigProperties(springScanner.getConfigProperties());
            List<ApiEndpoint> endpoints = project.getApiEndpoints();
            System.out.println("  + 项目模式: " + project.getProjectPattern());
            System.out.println("  + API 端点: " + endpoints.size());
            System.out.println("  + Bean 依赖: " + project.getBeanDependencies().size());
        } catch (Exception e) {
            System.out.println("  ⚠️ Spring 扫描跳过: " + e.getMessage());
        }

        // ==================== 3. 数据库理解 ====================
        System.out.println("\n[3/4] 数据库与 SQL 理解...");
        try {
            SqlParser sqlParser = new SqlParser(project.getClasses());
            sqlParser.scan();
            List<TableInfo> tables = sqlParser.getTables();
            project.setDatabaseTables(tables);
            project.setMapperSql(sqlParser.getMapperSql());
            System.out.println("  + 数据库表: " + tables.size());
            System.out.println("  + Mapper: " + sqlParser.getMapperSql().size());
            System.out.println("  + ORM: " + (sqlParser.hasJPA() ? "JPA" : sqlParser.hasMyBatis() ? "MyBatis" : "无"));
        } catch (Exception e) {
            System.out.println("  ⚠️ SQL 扫描跳过: " + e.getMessage());
        }

        // ==================== 4. 调用链追踪 ====================
        System.out.println("\n[4/4] 调用链追踪...");
        try {
            List<String> apiEntries = project.getApiEndpoints().stream()
                    .map(e -> e.getControllerClass() + "." + e.getMethodName())
                    .distinct()
                    .toList();
            java.util.Map<String, String> roleMap = springScanner != null ?
                    springScanner.getBeanTypeMap() : new java.util.HashMap<>();
            CallChainAnalyzer chainAnalyzer = new CallChainAnalyzer(roleMap);
            chainAnalyzer.analyze(project.getCallGraph(), apiEntries);
            List<CallChain> chains = chainAnalyzer.getChains();
            project.setCallChains(chains);
            project.setCriticalChains(chainAnalyzer.getCriticalChains());
            System.out.println("  + 调用链: " + chains.size() + " 条关键路径");
        } catch (Exception e) {
            System.out.println("  ⚠️ 调用链分析跳过: " + e.getMessage());
        }

        // ==================== 5. 分析 ====================
        System.out.println("\n生成报告...");
        ProjectAnalyzer analyzer = new ProjectAnalyzer(project);
        List<AnalysisResult> results = analyzer.analyze();

        // ==================== 6. 输出 ====================
        try {
            Path reportsDir = Paths.get(projectPath).toAbsolutePath().normalize().resolve("reports");
            Files.createDirectories(reportsDir);

            String projectName = project.getProjectName();
            String extension;
            String content;

            switch (mode) {
                case "html":
                    ReportGenerator reporter = new ReportGenerator(project, results);
                    content = reporter.generateHtml();
                    extension = ".html";
                    break;
                case "knowledge":
                    KnowledgeBaseGenerator kb = new KnowledgeBaseGenerator(project);
                    content = kb.generate();
                    extension = "_knowledge.md";
                    break;
                default: // markdown
                    ReportGenerator mdReporter = new ReportGenerator(project, results);
                    content = mdReporter.generateMarkdown();
                    extension = ".md";
                    break;
            }

            Path outputFile = reportsDir.resolve(projectName + extension);
            Files.writeString(outputFile, content);
            System.out.println("  ✅ " + (mode.equals("knowledge") ? "知识库" : "报告") + "已保存: " + outputFile);

            // 同时也生成知识库（如果选了其他模式）
            if (!mode.equals("knowledge")) {
                KnowledgeBaseGenerator kb = new KnowledgeBaseGenerator(project);
                String kbContent = kb.generate();
                Path kbFile = reportsDir.resolve(projectName + "_knowledge.md");
                Files.writeString(kbFile, kbContent);
                System.out.println("  ✅ 知识库已保存: " + kbFile);
            }

            // 始终生成可安装的 Agent Skill
            System.out.println();
            System.out.println("生成项目 Skill...");
            KnowledgeBaseGenerator kb2 = new KnowledgeBaseGenerator(project);
            // 输出到工具自身目录下的 skills/（方便提交到仓库）
            Path toolDir = Paths.get("").toAbsolutePath().normalize();
            Path skillDir = toolDir.resolve("skills");
            kb2.saveSkill(skillDir.toString());

        } catch (IOException e) {
            System.err.println("错误: 写入报告失败 - " + e.getMessage());
            System.exit(1);
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        System.out.println("\n============================================");
        System.out.println("  扫描完成! 耗时: " + (totalElapsed / 1000.0) + " 秒");
        System.out.println("  报告: reports/");
        System.out.println("============================================");

        // ==================== 7. DeepSeek 问答 ====================
        if (chatMode || askQuestion != null) {
            System.out.println("\n[5/4] 启动 DeepSeek 问答...");
            try {
                // 用知识库内容作为 system prompt
                KnowledgeBaseGenerator kbForChat = new KnowledgeBaseGenerator(project);
                String knowledge = kbForChat.generate();
                String systemPrompt = "你是一个 Java 老狗，对以下项目了如指掌。\n" +
                    "用项目知识回答问题，如果不知道就说不知道。\n\n" +
                    "=== 项目知识 ===\n" + knowledge;

                DeepSeekChat chat = new DeepSeekChat(systemPrompt, new Scanner(System.in));

                if (askQuestion != null) {
                    System.out.println("你: " + askQuestion);
                    System.out.println("🐋 DeepSeek 思考中...\n");
                    String reply = chat.ask(askQuestion);
                    System.out.println(reply);
                } else {
                    chat.interactiveChat();
                }
            } catch (Exception e) {
                System.err.println("⚠️ DeepSeek 问答失败: " + e.getMessage());
            }
        }
    }
}
