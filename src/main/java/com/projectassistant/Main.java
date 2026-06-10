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
import com.projectassistant.knowledge.SkillGenerator;
import com.projectassistant.reporter.ReportGenerator;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * ProjectAssistant - 主入口
 *
 * 升级版：彻底理解 Java 项目，像老狗一样熟悉代码
 *
 * 用法:
 *   java com.projectassistant.Main <项目路径> [markdown|html|knowledge]
 *
 * 输出格式:
 *   markdown  - Markdown 报告 (默认)
 *   html      - HTML 报告
 *   knowledge - 大模型知识库 (专为 LLM 优化的项目知识文档)
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("用法: java com.projectassistant.Main <项目路径> [markdown|html|knowledge]");
            System.exit(1);
        }

        String projectPath = args[0];
        String format = args.length >= 2 ? args[1] : "markdown";

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

            switch (format) {
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
            System.out.println("  ✅ " + (format.equals("knowledge") ? "知识库" : "报告") + "已保存: " + outputFile);

            // 同时也生成知识库（如果选了其他模式）
            if (!format.equals("knowledge")) {
                KnowledgeBaseGenerator kb = new KnowledgeBaseGenerator(project);
                String kbContent = kb.generate();
                Path kbFile = reportsDir.resolve(projectName + "_knowledge.md");
                Files.writeString(kbFile, kbContent);
                System.out.println("  ✅ 知识库已保存: " + kbFile);
            }

            // 始终生成可安装的 Agent Skill
            System.out.println();
            System.out.println("生成项目 Skill...");
            SkillGenerator skillGen = new SkillGenerator(project);
            Path skillDir = reportsDir.resolve("skills");
            skillGen.save(skillDir.toString());

        } catch (IOException e) {
            System.err.println("错误: 写入报告失败 - " + e.getMessage());
            System.exit(1);
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        System.out.println("\n============================================");
        System.out.println("  扫描完成! 耗时: " + (totalElapsed / 1000.0) + " 秒");
        System.out.println("  报告: reports/");
        System.out.println("============================================");
    }
}
