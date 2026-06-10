package com.projectassistant;

import com.projectassistant.model.*;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.reporter.ReportGenerator;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * ProjectAssistant - 主入口
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
            System.out.println("============================================");
            System.out.println("  ProjectAssistant - Java 项目智能扫描器");
            System.out.println("============================================");
            System.out.println();
            System.out.println("扫描项目: " + path.toAbsolutePath());

            // Step 1: 扫描
            System.out.println();
            System.out.println("[1/3] 扫描 Java 源码...");
            ProjectScanner scanner = new ProjectScanner(projectPath);
            ProjectModel project = scanner.scan();

            // 输出摘要
            ProjectStats stats = project.getStats();
            System.out.println("  + 类/接口: " + project.getClasses().size());
            System.out.println("  + 方法数: " + stats.getTotalMethods());
            System.out.println("  + 总行数: " + stats.getTotalLines());
            System.out.println("  + 依赖: " + project.getDependencies().size());

            // Step 2: 分析
            System.out.println();
            System.out.println("[2/3] 深度代码分析...");
            ProjectAnalyzer analyzer = new ProjectAnalyzer(project);
            List<AnalysisResult> results = analyzer.analyze();

            long critical = results.stream().filter(r -> r.getSeverity() >= 70).count();
            long important = results.stream().filter(r -> r.getSeverity() >= 50 && r.getSeverity() < 70).count();
            long suggestions = results.stream().filter(r -> r.getSeverity() >= 30 && r.getSeverity() < 50).count();
            System.out.println("  + 严重问题: " + critical);
            System.out.println("  + 重要问题: " + important);
            System.out.println("  + 改进建议: " + suggestions);

            // Step 3: 生成报告
            System.out.println();
            System.out.println("[3/3] 生成报告...");

            String reportDir = "reports";
            String reportFile;
            if ("html".equalsIgnoreCase(format)) {
                reportFile = reportDir + "/" + project.getProjectName() + "_report.html";
            } else {
                reportFile = reportDir + "/" + project.getProjectName() + "_report.md";
            }

            ReportGenerator reporter = new ReportGenerator(project, results);

            // 同时生成两种格式
            reporter.saveReport(reportFile, format);
            reporter.saveReport(reportDir + "/" + project.getProjectName() + "_report.html", "html");

            System.out.println();
            System.out.println("============================================");
            System.out.println("  扫描完成!");
            System.out.println("  报告: " + reportDir + "/");
            System.out.println("============================================");

        } catch (IOException e) {
            System.err.println("IO 错误: " + e.getMessage());
            System.exit(1);
        }
    }
}
