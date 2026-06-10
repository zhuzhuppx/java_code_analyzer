package com.projectassistant.reporter;

import com.projectassistant.model.*;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

/**
 * Markdown / HTML 报告生成器
 */
public class ReportGenerator {

    private final ProjectModel project;
    private final List<AnalysisResult> analysisResults;
    private final StringBuilder md = new StringBuilder();

    public ReportGenerator(ProjectModel project, List<AnalysisResult> results) {
        this.project = project;
        this.analysisResults = results;
    }

    /**
     * 生成 Markdown 报告
     */
    public String generateMarkdown() {
        md.setLength(0);
        writeHeader();
        writeSummary();
        writeProjectStructure();
        writePackageDeps();
        writeClassDetails();
        writeAnalysisResults();
        writeFooter();
        return md.toString();
    }

    /**
     * 生成 HTML 报告
     */
    /**
     * 生成 HTML 报告 — 自动将 Markdown 转为基本 HTML
     */
    public String generateHtml() {
        String mdContent = generateMarkdown();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>项目扫描报告 - ").append(project.getProjectName()).append("</title>\n");
        html.append("<style>\n");
        html.append("  body { font-family: -apple-system, 'Segoe UI', sans-serif; max-width: 960px; margin: 0 auto; padding: 20px; background: #f5f5f5; }\n");
        html.append("  .container { background: #fff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n");
        html.append("  h1 { color: #1a1a2e; border-bottom: 3px solid #e94560; padding-bottom: 10px; }\n");
        html.append("  h2 { color: #16213e; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px; }\n");
        html.append("  h3 { color: #0f3460; }\n");
        html.append("  table { border-collapse: collapse; width: 100%; margin: 15px 0; }\n");
        html.append("  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }\n");
        html.append("  th { background: #1a1a2e; color: #fff; }\n");
        html.append("  tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append("  code { background: #f0f0f0; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }\n");
        html.append("  pre { background: #f5f5f5; padding: 15px; border-radius: 5px; overflow-x: auto; }\n");
        html.append("  .stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px,1fr)); gap: 15px; margin: 20px 0; }\n");
        html.append("  .stat-card { background: #f8f9fa; border-radius: 6px; padding: 15px; text-align: center; border: 1px solid #e9ecef; }\n");
        html.append("  .stat-value { font-size: 2em; font-weight: bold; color: #0f3460; }\n");
        html.append("  .stat-label { font-size: 0.9em; color: #666; margin-top: 5px; }\n");
        html.append("</style>\n</head>\n<body>\n<div class=\"container\">\n");
        html.append(mdToHtml(mdContent));
        html.append("\n</div>\n</body>\n</html>");
        return html.toString();
    }

    /**
     * 简易 Markdown 转 HTML 转换器（支持标题/表格/列表/代码块/粗体）
     */
    private String mdToHtml(String md) {
        String[] lines = md.split("\n");
        StringBuilder out = new StringBuilder();
        boolean inTable = false, inCode = false;
        for (String line : lines) {
            String t = line.trim();
            if (t.equals("```")) {
                if (inCode) { out.append("</pre>\n"); inCode = false; }
                else { out.append("<pre>"); inCode = true; }
                continue;
            }
            if (inCode) { out.append(line).append("\n"); continue; }

            // 放过已有 HTML 标签（如 <div class="stats-grid">）
            if (t.startsWith("<")) {
                if (inTable) { out.append("</table>\n"); inTable = false; }
                out.append(line).append("\n");
                continue;
            }

            if (t.isEmpty()) { if (inTable) { out.append("</table>\n"); inTable = false; } continue; }

            if (t.startsWith("###### ")) { out.append("<h6>").append(inline(t.substring(7))).append("</h6>\n"); continue; }
            if (t.startsWith("##### "))  { out.append("<h5>").append(inline(t.substring(6))).append("</h5>\n"); continue; }
            if (t.startsWith("#### "))   { out.append("<h4>").append(inline(t.substring(5))).append("</h4>\n"); continue; }
            if (t.startsWith("### "))    { out.append("<h3>").append(inline(t.substring(4))).append("</h3>\n"); continue; }
            if (t.startsWith("## "))     { out.append("<h2>").append(inline(t.substring(3))).append("</h2>\n"); continue; }
            if (t.startsWith("# "))      { out.append("<h1>").append(inline(t.substring(2))).append("</h1>\n"); continue; }

            if (t.equals("---") || t.equals("***") || t.equals("___")) { continue; }
            if (t.startsWith("|")) {
                if (!inTable) { inTable = true; out.append("<table>\n"); }
                if (t.matches("\\|[\\s:\\-|]+\\|")) continue;
                out.append("<tr>");
                String[] cells = line.split("\\|");
                for (int i = 1; i < cells.length; i++)
                    out.append("<td>").append(inline(escHtml(cells[i].trim()))).append("</td>");
                out.append("</tr>\n");
                continue;
            }
            if (inTable) { out.append("</table>\n"); inTable = false; }

            if (t.startsWith("- ") || t.startsWith("* "))
                { out.append("<p>\\u2022 ").append(inline(escHtml(t.substring(2)))).append("</p>\n"); continue; }

            out.append("<p>").append(inline(escHtml(line))).append("</p>\n");
        }
        if (inTable) out.append("</table>\n");
        if (inCode) out.append("</pre>\n");
        return out.toString();
    }

    private String inline(String s) {
        return s.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
                .replaceAll("`([^`]+)`", "<code>$1</code>");
    }

    private String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 保存报告到文件
     */
    public void saveReport(String outputPath, String format) throws IOException {
        String content = "markdown".equalsIgnoreCase(format) ? generateMarkdown() : generateHtml();
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        System.out.println("  ✅ 报告已保存: " + path.toAbsolutePath());
    }

    // ==================== 内部方法 ====================

    private void writeHeader() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        md.append("# 项目扫描报告\n\n");
        md.append("| 项目 | 值 |\n|---|---|\n");
        md.append("| **项目名称** | ").append(project.getProjectName()).append(" |\n");
        md.append("| **项目路径** | ").append(project.getProjectPath()).append(" |\n");
        md.append("| **Java 版本** | ").append(project.getJavaVersion()).append(" |\n");
        md.append("| **生成时间** | ").append(now).append(" |\n");
        md.append("| **总类数** | ").append(project.getClasses().size()).append(" |\n");
        md.append("| **总行数** | ").append(project.getStats().getTotalLines()).append(" |\n\n");
    }

    private void writeSummary() {
        ProjectStats s = project.getStats();
        md.append("## 统计概要\n\n");

        md.append("<div class=\"stats-grid\">\n");
        addStatCard("Java 类", String.valueOf(project.getClasses().size()));
        addStatCard("总行数", String.valueOf(s.getTotalLines()));
        addStatCard("方法数", String.valueOf(s.getTotalMethods()));
        addStatCard("字段数", String.valueOf(s.getTotalFields()));
        addStatCard("平均方法长度", String.format("%.1f", s.getAverageMethodLines()) + " 行");
        addStatCard("平均复杂度", String.valueOf(s.getMaxComplexity()));
        addStatCard("上帝类", String.valueOf(s.getGodClassCount()));
        addStatCard("长方法", String.valueOf(s.getLongMethodCount()));
        addStatCard("高复杂度", String.valueOf(s.getHighComplexityCount()));
        if (!project.getDependencies().isEmpty()) {
            addStatCard("依赖数", String.valueOf(project.getDependencies().size()));
        }
        md.append("</div>\n\n");
    }

    private void addStatCard(String label, String value) {
        md.append("  <div class=\"stat-card\"><div class=\"stat-value\">")
          .append(value)
          .append("</div><div class=\"stat-label\">")
          .append(label)
          .append("</div></div>\n");
    }

    private void writeProjectStructure() {
        md.append("## 项目结构\n\n");
        md.append("```\n");
        md.append(project.getProjectName()).append("/\n");

        // 按包分组展示
        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            String pkg = entry.getKey();
            md.append("  +-- ").append(pkg.replace(".", "/")).append("/\n");
            for (ClassInfo ci : entry.getValue()) {
                String icon = "interface".equals(ci.getType()) ? "[I]" :
                              "enum".equals(ci.getType()) ? "[E]" : "[C]";
                md.append("  |   +-- ").append(icon).append(" ")
                  .append(ci.getSimpleName()).append(".java\n");
            }
        }
        md.append("```\n\n");
    }

    private void writePackageDeps() {
        Map<String, Set<String>> deps = project.getPackageDependencies();
        if (deps.isEmpty()) return;

        md.append("## 包依赖关系\n\n");
        md.append("| 包 | 依赖项 |\n|---|---|\n");
        for (Map.Entry<String, Set<String>> entry : deps.entrySet()) {
            String depList = String.join(", ", entry.getValue());
            md.append("| `").append(entry.getKey())
              .append("` | ").append(depList.isEmpty() ? "-" : "`" + depList + "`")
              .append(" |\n");
        }
        md.append("\n");
    }

    private void writeClassDetails() {
        md.append("## 类详情\n\n");

        for (ClassInfo ci : project.getClasses()) {
            String icon = "interface".equals(ci.getType()) ? "[I]" :
                          "enum".equals(ci.getType()) ? "[E]" :
                          "abstract".equals(ci.getType()) ? "[A]" : "[C]";
            md.append("### ").append(icon).append(" ").append(ci.getSimpleName()).append("\n\n");
            md.append("- **全称**: `").append(ci.getFullyQualifiedName()).append("`\n");
            md.append("- **类型**: ").append(ci.getType()).append("\n");
            md.append("- **行数**: ").append(ci.getLineCount()).append("\n");
            md.append("- **包**: `").append(ci.getPackageName()).append("`\n");
            md.append("- **可见性**: ").append(ci.getVisibility()).append("\n");

            if (!ci.getAnnotations().isEmpty()) {
                md.append("- **注解**: `").append(String.join("`, `", ci.getAnnotations())).append("`\n");
            }
            if (!ci.getInterfaces().isEmpty()) {
                md.append("- **实现**: `").append(String.join("`, `", ci.getInterfaces())).append("`\n");
            }
            if (ci.getSuperClassName() != null && !ci.getSuperClassName().equals("Object")) {
                md.append("- **继承**: `").append(ci.getSuperClassName()).append("`\n");
            }

            // 字段
            if (!ci.getFields().isEmpty()) {
                md.append("\n#### 字段 (").append(ci.getFields().size()).append(")\n\n");
                md.append("| 可见性 | 静态 | 名称 | 类型 |\n");
                md.append("|---|---|---|---|\n");
                for (FieldInfo fi : ci.getFields()) {
                    md.append("| ").append(fi.getVisibility())
                      .append(" | ").append(fi.isStatic() ? "Y" : "N")
                      .append(" | `").append(fi.getName()).append("`")
                      .append(" | `").append(fi.getType()).append("`")
                      .append(" |\n");
                }
            }

            // 方法
            if (!ci.getMethods().isEmpty()) {
                md.append("\n#### 方法 (").append(ci.getMethods().size()).append(")\n\n");
                md.append("| 可见性 | 名称 | 返回 | 参数 | 行数 | 复杂度 |\n");
                md.append("|---|---|---|---|---|---|\n");
                for (MethodInfo mi : ci.getMethods()) {
                    md.append("| ").append(mi.getVisibility())
                      .append(" | `").append(mi.getName()).append("()`")
                      .append(" | `").append(mi.getReturnType()).append("`")
                      .append(" | ").append(mi.getParameters().isEmpty() ? "-" : "`" + String.join(", ", mi.getParameters()) + "`")
                      .append(" | ").append(mi.getLineCount())
                      .append(" | ").append(mi.getCyclomaticComplexity())
                      .append(" |\n");
                }
            }
            md.append("\n---\n\n");
        }
    }

    private void writeAnalysisResults() {
        if (analysisResults.isEmpty()) {
            md.append("## 代码健康\n\n项目未发现明显问题。\n\n");
            return;
        }

        md.append("## 分析发现\n\n");

        // 按严重程度分组
        Map<String, List<AnalysisResult>> bySeverity = analysisResults.stream()
                .collect(Collectors.groupingBy(r -> {
                    if (r.getSeverity() >= 70) return "critical";
                    if (r.getSeverity() >= 50) return "important";
                    if (r.getSeverity() >= 30) return "suggestion";
                    return "info";
                }, LinkedHashMap::new, Collectors.toList()));

        String[] order = {"critical", "important", "suggestion", "info"};
        String[] labels = {"严重问题", "重要问题", "改进建议", "信息提示"};

        for (int i = 0; i < order.length; i++) {
            List<AnalysisResult> group = bySeverity.get(order[i]);
            if (group == null || group.isEmpty()) continue;

            md.append("### ").append(labels[i]).append(" (").append(group.size()).append(")\n\n");
            md.append("| # | 分类 | 描述 | 建议 |\n");
            md.append("|---|---|---|---|\n");

            int idx = 1;
            for (AnalysisResult r : group) {
                String desc = r.getDescription().replace("\n", "<br>");
                md.append("| ").append(idx++)
                  .append(" | ").append(r.getTitle())
                  .append(" | ").append(desc)
                  .append(" | ").append(r.getSuggestion())
                  .append(" |\n");
            }
            md.append("\n");
        }
    }

    private void writeFooter() {
        md.append("\n---\n");
        md.append("> *由 ProjectAssistant 扫描器自动生成 *\n");
    }
}
