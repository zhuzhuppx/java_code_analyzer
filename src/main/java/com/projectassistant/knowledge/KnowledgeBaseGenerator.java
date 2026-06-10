package com.projectassistant.knowledge;

import com.projectassistant.model.*;
import com.projectassistant.spring.ApiEndpoint;
import com.projectassistant.sql.TableInfo;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

/**
 * 项目知识库生成器 - 专为大模型优化
 *
 * 产出一份结构化、高密度的项目知识文档，
 * 大模型读取后能像 Java 老狗一样回答问题。
 */
public class KnowledgeBaseGenerator {

    private final ProjectModel project;
    private final StringBuilder sb = new StringBuilder();

    public KnowledgeBaseGenerator(ProjectModel project) {
        this.project = project;
    }

    public String generate() {
        sb.setLength(0);
        preamble();
        overview();
        architecture();
        apiCatalog();
        databaseSchema();
        beanGraph();
        callChains();
        keyClasses();
        businessFlow();
        configurations();
        devGuide();
        return sb.toString();
    }

    private void preamble() {
        sb.append("# 项目知识库\n\n");
        sb.append("> 由 ProjectAssistant 自动生成，专为大模型理解优化\n");
        sb.append("> 生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .append("\n\n---\n\n");
    }

    private void overview() {
        sb.append("## 1. 项目概览\n\n");
        ProjectStats s = project.getStats();
        sb.append("| 属性 | 值 |\n|---|---|\n");
        sb.append("| 项目名称 | ").append(project.getProjectName()).append(" |\n");
        sb.append("| 构建工具 | ").append(project.getBuildType()).append(" |\n");
        sb.append("| Java 版本 | ").append(project.getJavaVersion()).append(" |\n");
        sb.append("| Spring Boot | ").append(project.isSpringBoot() ? "是" : "否").append(" |\n");
        sb.append("| 代码总行数 | ").append(s.getTotalLines()).append(" |\n");
        sb.append("| 类/接口数 | ").append(project.getClasses().size()).append(" |\n");
        sb.append("| 方法数 | ").append(s.getTotalMethods()).append(" |\n");
        sb.append("| API 端点 | ").append(project.getApiEndpoints().size()).append(" |\n");
        sb.append("| 数据库表 | ").append(project.getDatabaseTables().size()).append(" |\n");
        sb.append("| 外部依赖 | ").append(project.getDependencies().size()).append(" |\n");
        sb.append("| 注释率 | ").append(String.format("%.1f%%", s.getCommentRatio() * 100)).append(" |\n");
        sb.append("| 平均方法行数 | ").append(String.format("%.1f", s.getAverageMethodLines())).append(" |\n");
        sb.append("| 最大圈复杂度 | ").append(s.getMaxComplexity()).append(" |\n\n");

        if (!project.getDependencies().isEmpty()) {
            sb.append("### 外部依赖\n\n```\n");
            for (DependencyInfo dep : project.getDependencies()) {
                sb.append(dep.getGroupId()).append(":").append(dep.getArtifactId()).append(":").append(dep.getVersion());
                if (dep.getScope() != null && !dep.getScope().equals("compile"))
                    sb.append(" [").append(dep.getScope()).append("]");
                sb.append("\n");
            }
            sb.append("```\n\n");
        }
    }

    private void architecture() {
        sb.append("## 2. 架构模式\n\n");
        sb.append("**架构类型**: ").append(project.getProjectPattern()).append("\n\n");

        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        sb.append("### 模块结构\n\n```\n");
        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            String pkg = entry.getKey();
            String[] parts = pkg.split("\\.");
            String indent = parts.length > 2 ? "  " + "  ".repeat(Math.min(parts.length - 2, 4)) : "  ";
            sb.append(indent).append(parts[parts.length - 1]).append("/\n");
            for (ClassInfo ci : entry.getValue()) {
                String icon = "interface".equals(ci.getType()) ? "[I]" :
                              "enum".equals(ci.getType()) ? "[E]" : "[C]";
                sb.append(indent).append("  +-- ").append(icon).append(" ")
                  .append(ci.getSimpleName()).append(".java\n");
            }
        }
        sb.append("```\n\n");
    }

    private void apiCatalog() {
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        if (endpoints.isEmpty()) { sb.append("## 3. API 路由\n\n无 API 端点。\n\n"); return; }
        sb.append("## 3. API 路由大全 (").append(endpoints.size()).append(" 个)\n\n");
        sb.append("| # | 方法 | 路径 | Controller | 方法 |\n|---|---|---|---|---|\n");
        int idx = 1;
        for (ApiEndpoint ep : endpoints) {
            sb.append("| ").append(idx++).append(" | ").append(ep.getHttpMethod())
              .append(" | `").append(ep.getPath()).append("`")
              .append(" | ").append(ep.getControllerClass())
              .append(" | `").append(ep.getMethodName()).append("()` |\n");
        }
        sb.append("\n");
    }

    private void databaseSchema() {
        List<TableInfo> tables = project.getDatabaseTables();
        if (tables.isEmpty()) { sb.append("## 4. 数据库\n\n无数据库映射。\n\n"); return; }
        sb.append("## 4. 数据库结构 (").append(tables.size()).append(" 张表)\n\n");
        for (TableInfo table : tables) {
            sb.append("### ").append(table.getTableName()).append("\n\n");
            if (table.getEntityClass() != null) sb.append("Entity: ").append(table.getEntityClass()).append("\n\n");
            sb.append("| 字段 | 列名 | 类型 | 主键 |\n|---|---|---|---|\n");
            for (TableInfo.Column col : table.getColumns()) {
                sb.append("| `").append(col.getFieldName()).append("`")
                  .append(" | `").append(col.getColumnName()).append("`")
                  .append(" | ").append(col.getJavaType())
                  .append(" | ").append(col.isPrimaryKey() ? "PK" : "")
                  .append(" |\n");
            }
            sb.append("\n");
        }
    }

    private void beanGraph() {
        Map<String, List<String>> deps = project.getBeanDependencies();
        if (deps.isEmpty()) { sb.append("## 5. Bean 依赖\n\n无。\n\n"); return; }
        sb.append("## 5. Bean 依赖\n\n```\n");
        for (Map.Entry<String, List<String>> entry : deps.entrySet()) {
            sb.append(shorten(entry.getKey())).append(" 依赖: ");
            sb.append(entry.getValue().stream().map(this::shorten).collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        sb.append("```\n\n");
    }

    private void callChains() {
        List<String> chains = project.getCriticalChains();
        if (chains.isEmpty()) { sb.append("## 6. 调用链\n\n无。\n\n"); return; }
        sb.append("## 6. 调用链\n\n");
        for (String chain : chains) sb.append("- ").append(chain).append("\n");
        sb.append("\n");
    }

    private void keyClasses() {
        sb.append("## 7. 关键类\n\n");
        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n\n");
            for (ClassInfo ci : entry.getValue()) {
                sb.append("- **").append(ci.getSimpleName()).append("**")
                  .append(" (").append(ci.getType()).append(") — ")
                  .append(ci.getMethods().size()).append(" 方法, ")
                  .append(ci.getFields().size()).append(" 字段");
                if (!ci.getAnnotations().isEmpty())
                    sb.append(", @").append(String.join(" @", ci.getAnnotations()));
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private void businessFlow() {
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        sb.append("## 8. 业务流\n\n");
        if (endpoints.isEmpty()) { sb.append("无法推断。\n\n"); return; }
        Map<String, List<ApiEndpoint>> byPrefix = new TreeMap<>();
        for (ApiEndpoint ep : endpoints) {
            String prefix = "/";
            if (ep.getPath().length() > 1) {
                String[] parts = ep.getPath().split("/");
                prefix = parts.length > 1 ? "/" + parts[1] : "/";
            }
            byPrefix.computeIfAbsent(prefix, k -> new ArrayList<>()).add(ep);
        }
        for (Map.Entry<String, List<ApiEndpoint>> entry : byPrefix.entrySet()) {
            String module = entry.getKey().replace("/", "");
            if (module.isEmpty()) module = "ROOT";
            sb.append("### ").append(module).append("\n\n");
            for (ApiEndpoint ep : entry.getValue()) {
                sb.append("- ").append(ep.getHttpMethod()).append(" `").append(ep.getPath()).append("`")
                  .append(" → ").append(inferAction(ep)).append("\n");
            }
            sb.append("\n");
        }
    }

    private String inferAction(ApiEndpoint ep) {
        String m = ep.getMethodName().toLowerCase();
        if (m.startsWith("list")||m.startsWith("find")||m.startsWith("search")||m.startsWith("query")||ep.getHttpMethod().equals("GET"))
            return "查询";
        if (m.startsWith("create")||m.startsWith("add")||m.startsWith("save")||ep.getHttpMethod().equals("POST"))
            return "新增";
        if (m.startsWith("update")||m.startsWith("edit")||m.startsWith("modify")||ep.getHttpMethod().equals("PUT"))
            return "更新";
        if (m.startsWith("delete")||m.startsWith("remove")||ep.getHttpMethod().equals("DELETE"))
            return "删除";
        if (m.startsWith("login")||m.startsWith("logout")||m.startsWith("auth")||m.startsWith("register"))
            return "认证";
        if (m.startsWith("export")||m.startsWith("import")||m.startsWith("upload")||m.startsWith("download"))
            return "导入导出";
        return m;
    }

    private void configurations() {
        Map<String, String> config = project.getConfigProperties();
        sb.append("## 9. 配置\n\n");
        if (config.isEmpty()) { sb.append("无。\n\n"); return; }
        for (Map.Entry<String, String> e : config.entrySet())
            sb.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n");
        sb.append("\n");
    }

    private void devGuide() {
        sb.append("## 10. 开发指南\n\n");
        sb.append("**启动类**: ");
        project.getClasses().stream()
                .filter(ci -> ci.getAnnotations().contains("SpringBootApplication"))
                .findFirst()
                .ifPresentOrElse(ci -> sb.append("`").append(ci.getFullyQualifiedName()).append("`\n"),
                                 () -> sb.append("未检测到\n"));
        sb.append("**架构**: ").append(project.getProjectPattern()).append("\n");
        sb.append("| 你想问 | 看哪节 |\n|---|---|\n");
        sb.append("| 这是什么项目？ | 1. 概览 / 2. 架构 |\n");
        sb.append("| 有哪些接口？ | 3. API 路由 |\n");
        sb.append("| 数据库怎么设计的？ | 4. 数据库 |\n");
        sb.append("| 改这个字段影响哪？ | 5. Bean依赖 / 6. 调用链 |\n");
        sb.append("| 帮我加个接口 | 8. 业务流 / 7. 关键类 |\n");
        sb.append("| 这个配置是什么意思？ | 9. 配置 |\n\n");
        sb.append("---\n> 知识库由 ProjectAssistant 生成 | 配合大模型使用效果更佳\n");
    }

    private String shorten(String s) {
        if (s == null) return "?";
        int i = s.lastIndexOf('.');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    /**
     * 保存为普通知识库 .md 文件
     */
    public void save(String outputPath) throws IOException {
        String content = generate();
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        System.out.println("  ✅ 知识库已保存: " + path.toAbsolutePath());
    }

    /**
     * 新模式：保存为可安装的 Agent Skill 目录
     *
     * 输出结构:
     *   <outputDir>/<project-name>/
     *     └── SKILL.md    ← YAML front matter + 项目知识
     *
     * 安装: copaw skill install <outputDir>/<project-name>
     */
    public void saveSkill(String outputDir) throws IOException {
        Path skillDir = Paths.get(outputDir, sanitize(project.getProjectName()));
        Files.createDirectories(skillDir);

        StringBuilder md = new StringBuilder();
        md.append("---\n");
        md.append("name: ").append(sanitize(project.getProjectName())).append("\n");
        md.append("description: \"");
        md.append(project.getProjectName()).append(" 项目知识库");
        md.append(" — ").append(project.getClasses().size()).append(" 个类, ");
        md.append(project.getApiEndpoints().size()).append(" 个 API, ");
        md.append(project.getDatabaseTables().size()).append(" 张表\"\n");
        md.append("metadata:\n");
        md.append("  copaw:\n");
        md.append("    emoji: \"📦\"\n");
        md.append("    requires: {}\n");
        md.append("---\n\n");

        // 项目知识正文：内容和 generate() 一致，但多了 agent 使用提示
        md.append("# ").append(project.getProjectName()).append(" 项目知识库\n\n");
        md.append("> 由 ProjectAssistant 自动生成 | ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        md.append("当用户询问本项目相关的任何问题时，优先使用以下信息回答。\n\n");

        // === 1. 概览 ===
        ProjectStats s = project.getStats();
        md.append("## 1. 项目概览\n\n");
        md.append("| 属性 | 值 |\n|---|---|\n");
        md.append("| 项目 | ").append(project.getProjectName()).append(" |\n");
        md.append("| 构建 | ").append(project.getBuildType()).append(" |\n");
        md.append("| Java | ").append(project.getJavaVersion()).append(" |\n");
        md.append("| Spring Boot | ").append(project.isSpringBoot() ? "是" : "否").append(" |\n");
        md.append("| 行数 | ").append(s.getTotalLines()).append(" |\n");
        md.append("| 类数 | ").append(project.getClasses().size()).append(" |\n");
        md.append("| 方法数 | ").append(s.getTotalMethods()).append(" |\n");
        md.append("| API | ").append(project.getApiEndpoints().size()).append(" |\n");
        md.append("| 数据库表 | ").append(project.getDatabaseTables().size()).append(" |\n");
        md.append("| 依赖 | ").append(project.getDependencies().size()).append(" |\n\n");

        if (!project.getDependencies().isEmpty()) {
            md.append("### 依赖\n\n```\n");
            for (DependencyInfo dep : project.getDependencies()) {
                md.append(dep.getGroupId()).append(":").append(dep.getArtifactId()).append(":").append(dep.getVersion());
                if (dep.getScope() != null && !"compile".equals(dep.getScope())) md.append(" [").append(dep.getScope()).append("]");
                md.append("\n");
            }
            md.append("```\n\n");
        }

        // === 2. 架构 ===
        md.append("## 2. 架构\n\n**").append(project.getProjectPattern()).append("**\n\n```\n");
        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ClassInfo>> e : pkgMap.entrySet()) {
            String[] parts = e.getKey().split("\\.");
            md.append("  ").append(parts[parts.length-1]).append("/\n");
            for (ClassInfo ci : e.getValue()) {
                String icon = "interface".equals(ci.getType()) ? "[I]" : "enum".equals(ci.getType()) ? "[E]" : "[C]";
                md.append("    ").append(icon).append(" ").append(ci.getSimpleName()).append("\n");
            }
        }
        md.append("```\n\n");

        // === 3. API ===
        List<ApiEndpoint> eps = project.getApiEndpoints();
        md.append("## 3. API\n\n");
        if (eps.isEmpty()) { md.append("（无）\n\n"); }
        else {
            md.append("| 方法 | 路径 | Controller | 方法 |\n|---|---|---|---|\n");
            for (ApiEndpoint ep : eps) md.append("| ").append(ep.getHttpMethod()).append(" | `").append(ep.getPath()).append("` | ").append(ep.getControllerClass()).append(" | `").append(ep.getMethodName()).append("` |\n");
            md.append("\n");
        }

        // === 4. 数据库 ===
        List<TableInfo> tables = project.getDatabaseTables();
        md.append("## 4. 数据库\n\n");
        if (tables.isEmpty()) { md.append("（无）\n\n"); }
        else {
            for (TableInfo t : tables) {
                md.append("### ").append(t.getTableName()).append("\n\n");
                if (t.getEntityClass() != null) md.append("Entity: ").append(t.getEntityClass()).append("\n\n");
                md.append("| 字段 | 列 | 类型 | PK |\n|---|---|---|---|\n");
                for (TableInfo.Column col : t.getColumns())
                    md.append("| `").append(col.getFieldName()).append("` | `").append(col.getColumnName()).append("` | ").append(col.getJavaType()).append(" | ").append(col.isPrimaryKey() ? "PK" : "").append(" |\n");
                md.append("\n");
            }
        }

        // === 5. Bean ===
        Map<String, List<String>> deps = project.getBeanDependencies();
        md.append("## 5. Bean\n\n");
        if (deps.isEmpty()) { md.append("（无）\n\n"); }
        else {
            md.append("```\n");
            for (Map.Entry<String, List<String>> e : deps.entrySet())
                md.append(shorten(e.getKey())).append(" → ").append(e.getValue().stream().map(this::shorten).collect(Collectors.joining(", "))).append("\n");
            md.append("```\n\n");
        }

        // === 6. 调用链 ===
        List<String> chains = project.getCriticalChains();
        md.append("## 6. 调用链\n\n");
        if (chains.isEmpty()) { md.append("（无）\n\n"); }
        else { for (String c : chains) md.append("- ").append(c).append("\n"); md.append("\n"); }

        // === 7. 关键类 ===
        md.append("## 7. 关键类\n\n");
        for (Map.Entry<String, List<ClassInfo>> e : pkgMap.entrySet()) {
            md.append("### ").append(e.getKey()).append("\n\n");
            for (ClassInfo ci : e.getValue()) {
                md.append("- **").append(ci.getSimpleName()).append("** (").append(ci.getType()).append(") — ").append(ci.getMethods().size()).append(" 方法, ").append(ci.getFields().size()).append(" 字段");
                if (!ci.getAnnotations().isEmpty()) md.append(", @").append(String.join(" @", ci.getAnnotations()));
                md.append("\n");
            }
            md.append("\n");
        }

        // === 8. 业务流 ===
        md.append("## 8. 业务流\n\n");
        if (!eps.isEmpty()) {
            Map<String, List<ApiEndpoint>> byPrefix = new TreeMap<>();
            for (ApiEndpoint ep : eps) {
                String p = "/";
                if (ep.getPath().length() > 1) { String[] parts = ep.getPath().split("/"); p = parts.length > 1 ? "/" + parts[1] : "/"; }
                byPrefix.computeIfAbsent(p, k -> new ArrayList<>()).add(ep);
            }
            for (Map.Entry<String, List<ApiEndpoint>> e : byPrefix.entrySet()) {
                String mod = e.getKey().replace("/", "");
                if (mod.isEmpty()) mod = "ROOT";
                md.append("**").append(mod).append("**: ");
                md.append(e.getValue().stream().map(ep -> ep.getHttpMethod() + " `" + ep.getPath() + "`").collect(Collectors.joining(", ")));
                md.append("\n\n");
            }
        } else {
            for (String pkg : pkgMap.keySet()) {
                String[] parts = pkg.split("\\.");
                md.append("**").append(parts[parts.length-1]).append("**: ").append(pkgMap.get(pkg).size()).append(" 个类\n\n");
            }
        }

        // === 9. 配置 ===
        Map<String, String> config = project.getConfigProperties();
        md.append("## 9. 配置\n\n");
        if (config.isEmpty()) { md.append("（无）\n\n"); }
        else { for (Map.Entry<String, String> e : config.entrySet()) md.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n"); md.append("\n"); }

        // === 10. Agent 提示 ===
        md.append("## 10. 对 Agent 的提示\n\n");
        md.append("| 用户想问 | 看哪节 |\n|---|---|\n");
        md.append("| 这是什么项目？ | 1. 概览 / 2. 架构 |\n");
        md.append("| 有哪些接口？ | 3. API |\n");
        md.append("| 数据库怎么设计的？ | 4. 数据库 |\n");
        md.append("| 改这个字段影响哪？ | 5. Bean / 6. 调用链 |\n");
        md.append("| 帮我加个接口 | 8. 业务流 / 7. 关键类 |\n");
        md.append("| 这个配置是什么意思？ | 9. 配置 |\n\n");
        md.append("---\n> 由 ProjectAssistant 生成 | 安装命令: `copaw skill install <this-dir>`\n");

        Files.writeString(skillDir.resolve("SKILL.md"), md.toString());
        System.out.println("  ✅ 项目 Skill 已生成: " + skillDir.toAbsolutePath());
        System.out.println("  📦 安装命令: copaw skill install " + skillDir.toAbsolutePath());
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
