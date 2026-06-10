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
 *   java com.projectassistant.Main <项目路径> [报告格式]
 *
 * 报告格式: markdown (默认) 或 html
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java com.projectassistant.Main <项目路径> [markdown|html]");
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

        try {
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  ProjectAssistant - Java 老狗级项目理解  ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.println();
            System.out.println("扫描项目: " + path.toAbsolutePath());

            // Step 1: 基础扫描
            System.out.println();
            System.out.println("[1/4] 扫描 Java 源码...");
            ProjectScanner scanner = new ProjectScanner(projectPath);
            ProjectModel project = scanner.scan();

            // 输出摘要
            ProjectStats stats = project.getStats();
            System.out.println("  + 类/接口: " + project.getClasses().size());
            System.out.println("  + 方法数: " + stats.getTotalMethods());
            System.out.println("  + 总行数: " + stats.getTotalLines());
            System.out.println("  + 依赖: " + project.getDependencies().size());

            // Step 2: Spring 深度扫描
            System.out.println();
            System.out.println("[2/4] Spring 框架理解...");
            SpringScanner springScanner = new SpringScanner(project.getClasses());
            springScanner.scan();

            project.setApiEndpoints(springScanner.getEndpoints());
            project.setBeanDependencies(springScanner.getBeanDependencies());
            project.setProjectPattern(springScanner.getProjectPattern());
            project.setSpringBoot(springScanner.isSpringBoot());
            project.setConfigProperties(springScanner.getConfigProperties());

            System.out.println("  + 项目模式: " + project.getProjectPattern());
            System.out.println("  + API 端点: " + project.getApiEndpoints().size());
            System.out.println("  + Bean 依赖: " + project.getBeanDependencies().size());

            // Step 3: SQL 与数据库理解
            System.out.println();
            System.out.println("[3/4] 数据库与 SQL 理解...");
            SqlParser sqlParser = new SqlParser(project.getClasses());
            sqlParser.scan();

            project.setDatabaseTables(sqlParser.getTables());
            project.setMapperSql(sqlParser.getMapperSql());

            System.out.println("  + 数据库表: " + project.getDatabaseTables().size());
            System.out.println("  + Mapper: " + project.getMapperSql().size());
            System.out.println("  + ORM: "
                    + (sqlParser.hasJPA() ? "JPA " : "")
                    + (sqlParser.hasMyBatis() ? "MyBatis" : "无"));

            // Step 4: 调用链追踪
            System.out.println();
            System.out.println("[4/4] 调用链追踪...");
            CallChainAnalyzer chainAnalyzer = new CallChainAnalyzer(springScanner.getBeanTypeMap());

            // 把 API 端点转为调用链入口
            List<String> endpoints = project.getApiEndpoints().stream()
                    .map(ep -> ep.getControllerClass() + "." + ep.getMethodName())
                    .distinct()
                    .toList();
            chainAnalyzer.analyze(project.getCallGraph(), endpoints);
            project.setCallChains(chainAnalyzer.getChains());
            project.setCriticalChains(chainAnalyzer.getCriticalChains());

            System.out.println("  + 调用链: " + project.getCriticalChains().size() + " 条关键路径");

            // Step 5: 知识库生成（专为大模型优化）
            System.out.println();
            System.out.println("生成知识库...");
            com.projectassistant.knowledge.KnowledgeBaseGenerator kbGen =
                    new com.projectassistant.knowledge.KnowledgeBaseGenerator(project);
            kbGen.save("reports/" + project.getProjectName() + "_knowledge.md");

            // Step 6: 深度分析 + 报告
            System.out.println();
            System.out.println("生成分析报告...");
            ProjectAnalyzer analyzer = new ProjectAnalyzer(project);
            List<AnalysisResult> results = analyzer.analyze();

            ReportGenerator reporter = new ReportGenerator(project, results);
            String reportDir = "reports";
            reporter.saveReport(reportDir + "/" + project.getProjectName() + "_report.md", "markdown");
            reporter.saveReport(reportDir + "/" + project.getProjectName() + "_report.html", "html");

            System.out.println();
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  扫描完成！项目已经摸透了！             ║");
            System.out.println("║  报告: " + reportDir + "/");
            System.out.println("║  类数: " + project.getClasses().size()
                    + "  端点: " + project.getApiEndpoints().size()
                    + "  表: " + project.getDatabaseTables().size());
            System.out.println("╚══════════════════════════════════════════╝");

        } catch (IOException e) {
            System.err.println("IO 错误: " + e.getMessage());
            System.exit(1);
        }
    }
}
