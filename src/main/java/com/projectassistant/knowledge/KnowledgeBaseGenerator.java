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
 * 大模型读取后能像 Java 老狗一样回答问题：
 *   - "这个接口的调用链是什么？"
 *   - "改这个字段会影响哪些地方？"
 *   - "帮我加个分页查询接口"
 *   - "这个项目的核心业务流程是什么？"
 */
public class KnowledgeBaseGenerator {

    private final ProjectModel project;
    private final StringBuilder kb = new StringBuilder();

    public KnowledgeBaseGenerator(ProjectModel project) {
        this.project = project;
    }

    public String generate() {
        kb.setLength(0);
        writePreamble();
        writeOverview();
        writeArchitecture();
        writeApiCatalog();
        writeDatabaseSchema();
        writeBeanGraph();
        writeCallChains();
        writeKeyClasses();
        writeBusinessFlow();
        writeConfiguration();
        writeDevelopmentGuide();
        return kb.toString();
    }

    private void writePreamble() {
        kb.append("# 项目知识库\n\n");
        kb.append("> 由 ProjectAssistant 自动生成，专为大模型理解优化\n");
        kb.append("> 生成时间: ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .append("\n\n---\n\n");
    }

    private void writeOverview() {
        kb.append("## 1. 项目概览\n\n");
        ProjectStats stats = project.getStats();
        kb.append("| 属性 | 值 |\n|---|---|\n");
        kb.append("| 项目名称 | ").append(project.getProjectName()).append(" |\n");
        kb.append("| 构建工具 | ").append(project.getBuildType()).append(" |\n");
        kb.append("| Java 版本 | ").append(project.getJavaVersion()).append(" |\n");
        kb.append("| Spring Boot | ").append(project.isSpringBoot() ? "是" : "否").append(" |\n");
        kb.append("| 代码总行数 | ").append(stats.getTotalLines()).append(" |\n");
        kb.append("| 类/接口数 | ").append(project.getClasses().size()).append(" |\n");
        kb.append("| 方法数 | ").append(stats.getTotalMethods()).append(" |\n");
        kb.append("| API 端点 | ").append(project.getApiEndpoints().size()).append(" |\n");
        kb.append("| 数据库表 | ").append(project.getDatabaseTables().size()).append(" |\n");
        kb.append("| 外部依赖 | ").append(project.getDependencies().size()).append(" |\n\n");

        if (!project.getDependencies().isEmpty()) {
            kb.append("### 外部依赖\n\n```\n");
            for (DependencyInfo dep : project.getDependencies()) {
                kb.append(dep.getGroupId()).append(":").append(dep.getArtifactId())
                  .append(":").append(dep.getVersion());
                if (dep.getScope() != null && !dep.getScope().equals("compile")) {
                    kb.append(" [").append(dep.getScope()).append("]");
                }
                kb.append("\n");
            }
            kb.append("```\n\n");
        }
    }

    private void writeArchitecture() {
        kb.append("## 2. 架构模式\n\n");
        kb.append("**架构类型**: ").append(project.getProjectPattern()).append("\n\n");

        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        kb.append("### 模块结构\n\n```\n");
        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            String pkg = entry.getKey();
            String indent = "  ";
            String[] parts = pkg.split("\\.");
            if (parts.length > 2) indent = "  " + "  ".repeat(Math.min(parts.length - 2, 4));
            kb.append(indent).append(parts[parts.length - 1]).append("/\n");
            for (ClassInfo ci : entry.getValue()) {
                String icon = "interface".equals(ci.getType()) ? "[I]" :
                              "enum".equals(ci.getType()) ? "[E]" : "[C]";
                kb.append(indent).append("  +-- ").append(icon).append(" ")
                  .append(ci.getSimpleName()).append(".java\n");
            }
        }
        kb.append("```\n\n");
    }

    private void writeApiCatalog() {
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        if (endpoints.isEmpty()) { kb.append("## 3. API 路由\n\n无 API 端点。\n\n"); return; }
        kb.append("## 3. API 路由大全 (").append(endpoints.size()).append(" 个)\n\n");
        kb.append("| # | 方法 | 路径 | Controller | 方法 |\n|---|---|---|---|---|\n");
        int idx = 1;
        for (ApiEndpoint ep : endpoints) {
            kb.append("| ").append(idx++).append(" | ").append(ep.getHttpMethod())
              .append(" | `").append(ep.getPath()).append("`")
              .append(" | ").append(ep.getControllerClass())
              .append(" | `").append(ep.getMethodName()).append("()` |\n");
        }
        kb.append("\n");
    }

    private void writeDatabaseSchema() {
        List<TableInfo> tables = project.getDatabaseTables();
        if (tables.isEmpty()) { kb.append("## 4. 数据库\n\n无数据库映射。\n\n"); return; }
        kb.append("## 4. 数据库结构 (").append(tables.size()).append(" 张表)\n\n");
        for (TableInfo table : tables) {
            kb.append("### ").append(table.getTableName()).append("\n\n");
            if (table.getEntityClass() != null) kb.append("Entity: ").append(table.getEntityClass()).append("\n\n");
            kb.append("| 字段 | 列名 | 类型 | 主键 |\n|---|---|---|---|\n");
            for (TableInfo.Column col : table.getColumns()) {
                kb.append("| `").append(col.getFieldName()).append("`")
                  .append(" | `").append(col.getColumnName()).append("`")
                  .append(" | ").append(col.getJavaType())
                  .append(" | ").append(col.isPrimaryKey() ? "PK" : "")
                  .append(" |\n");
            }
            kb.append("\n");
        }
    }

    private void writeBeanGraph() {
        Map<String, List<String>> deps = project.getBeanDependencies();
        if (deps.isEmpty()) { kb.append("## 5. Bean 依赖\n\n无。\n\n"); return; }
        kb.append("## 5. Bean 依赖\n\n```\n");
        for (Map.Entry<String, List<String>> entry : deps.entrySet()) {
            kb.append(shorten(entry.getKey())).append(" 依赖: ");
            kb.append(entry.getValue().stream().map(this::shorten).collect(Collectors.joining(", ")));
            kb.append("\n");
        }
        kb.append("```\n\n");
    }

    private void writeCallChains() {
        List<String> chains = project.getCriticalChains();
        if (chains.isEmpty()) { kb.append("## 6. 调用链\n\n无。\n\n"); return; }
        kb.append("## 6. 调用链\n\n");
        for (String chain : chains) kb.append("- ").append(chain).append("\n");
        kb.append("\n");
    }

    private void writeKeyClasses() {
        kb.append("## 7. 关键类\n\n");
        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            kb.append("### ").append(entry.getKey()).append("\n\n");
            for (ClassInfo ci : entry.getValue()) {
                kb.append("- **").append(ci.getSimpleName()).append("**")
                  .append(" (").append(ci.getType()).append(")")
                  .append(" — ").append(ci.getMethods().size()).append(" 方法, ")
                  .append(ci.getFields().size()).append(" 字段");
                if (!ci.getAnnotations().isEmpty())
                    kb.append(", @").append(String.join(" @", ci.getAnnotations()));
                kb.append("\n");
            }
            kb.append("\n");
        }
    }

    private void writeBusinessFlow() {
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        kb.append("## 8. 业务流\n\n");
        if (endpoints.isEmpty()) { kb.append("无法推断。\n\n"); return; }

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
            kb.append("### ").append(module).append("\n\n");
            for (ApiEndpoint ep : entry.getValue()) {
                kb.append("- ").append(ep.getHttpMethod()).append(" `").append(ep.getPath()).append("`")
                  .append(" → ").append(inferAction(ep)).append("\n");
            }
            kb.append("\n");
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

    private void writeConfiguration() {
        Map<String, String> config = project.getConfigProperties();
        kb.append("## 9. 配置\n\n");
        if (config.isEmpty()) { kb.append("无。\n\n"); return; }
        for (Map.Entry<String, String> e : config.entrySet())
            kb.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n");
        kb.append("\n");
    }

    private void writeDevelopmentGuide() {
        kb.append("## 10. 开发指南\n\n");
        kb.append("**启动类**: ");
        project.getClasses().stream()
                .filter(ci -> ci.getAnnotations().contains("SpringBootApplication"))
                .findFirst()
                .ifPresentOrElse(ci -> kb.append("`").append(ci.getFullyQualifiedName()).append("`\n"),
                                 () -> kb.append("未检测到\n"));
        kb.append("**架构**: ").append(project.getProjectPattern()).append("\n");
        kb.append("**ORM**: ").append(project.getDatabaseTables().isEmpty() ? "无" : "JPA/MyBatis").append("\n\n");

        kb.append("### 开发场景\n\n");
        kb.append("当被问到以下问题时，根据本项目知识库回答：\n\n");
        kb.append("1. **这个项目是干什么的？** -> 看 API 路由 + 业务流章节\n");
        kb.append("2. **改一个字段影响哪些地方？** -> 看数据库 + 调用链\n");
        kb.append("3. **帮我加个接口** -> 参考现有 API 风格\n");
        kb.append("4. **这个接口的业务逻辑？** -> 看调用链 + 关键类\n\n");
        kb.append("---\n> 知识库由 ProjectAssistant 生成 | 配合大模型使用效果更佳\n");
    }

    private String shorten(String s) {
        if (s == null) return "?";
        int i = s.lastIndexOf('.');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    public void save(String outputPath) throws IOException {
        String content = generate();
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        System.out.println("  ✅ 知识库已保存: " + path.toAbsolutePath());
    }
}
