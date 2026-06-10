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
 * 项目 Skill 生成器
 *
 * 把项目理解结果打包成一个可安装的 Agent Skill，
 * 输出目录结构:
 *
 * skills/<project-name>/
 *   └── SKILL.md     ← YAML front matter + 项目知识（给 agent 看）
 *
 * 安装方式:
 *   copaw skill install <path-to-skill-dir>
 *
 * 装好后 agent 就永久记住这个项目了，
 * 你问项目相关问题，agent 自动加载 SKILL.md 来回答。
 */
public class SkillGenerator {

    private final ProjectModel project;

    public SkillGenerator(ProjectModel project) {
        this.project = project;
    }

    public void save(String outputDir) throws IOException {
        Path skillDir = Paths.get(outputDir, sanitize(project.getProjectName()));
        Files.createDirectories(skillDir);

        String skillContent = buildSkillMd();
        Files.writeString(skillDir.resolve("SKILL.md"), skillContent);

        System.out.println("  ✅ 项目 Skill 已生成: " + skillDir.toAbsolutePath());
        System.out.println("  📦 安装命令: copaw skill install " + skillDir.toAbsolutePath());
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private String buildSkillMd() {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(sanitize(project.getProjectName())).append("\n");
        sb.append("description: \"");
        sb.append(project.getProjectName()).append(" 项目知识库");
        sb.append(" — ").append(project.getClasses().size()).append(" 个类, ");
        sb.append(project.getApiEndpoints().size()).append(" 个 API, ");
        sb.append(project.getDatabaseTables().size()).append(" 张表\"\n");
        sb.append("metadata:\n");
        sb.append("  copaw:\n");
        sb.append("    emoji: \"📦\"\n");
        sb.append("    requires: {}\n");
        sb.append("---\n\n");

        // 标题
        sb.append("# ").append(project.getProjectName()).append(" 项目知识库\n\n");
        sb.append("> 由 ProjectAssistant 自动生成 | ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        sb.append("当用户询问本项目相关的任何问题时，优先使用以下信息回答。\n\n");

        // 1. 概览
        ProjectStats stats = project.getStats();
        sb.append("## 1. 项目概览\n\n");
        sb.append("| 属性 | 值 |\n|---|---|\n");
        sb.append("| 项目 | ").append(project.getProjectName()).append(" |\n");
        sb.append("| 构建 | ").append(project.getBuildType()).append(" |\n");
        sb.append("| Java | ").append(project.getJavaVersion()).append(" |\n");
        sb.append("| Spring Boot | ").append(project.isSpringBoot() ? "是" : "否").append(" |\n");
        sb.append("| 行数 | ").append(stats.getTotalLines()).append(" |\n");
        sb.append("| 类数 | ").append(project.getClasses().size()).append(" |\n");
        sb.append("| 方法数 | ").append(stats.getTotalMethods()).append(" |\n");
        sb.append("| API | ").append(project.getApiEndpoints().size()).append(" |\n");
        sb.append("| 数据库表 | ").append(project.getDatabaseTables().size()).append(" |\n");
        sb.append("| 依赖 | ").append(project.getDependencies().size()).append(" |\n\n");

        if (!project.getDependencies().isEmpty()) {
            sb.append("### 外部依赖\n\n```\n");
            for (DependencyInfo dep : project.getDependencies()) {
                sb.append(dep.getGroupId()).append(":").append(dep.getArtifactId()).append(":").append(dep.getVersion());
                if (dep.getScope() != null && !"compile".equals(dep.getScope())) sb.append(" [").append(dep.getScope()).append("]");
                sb.append("\n");
            }
            sb.append("```\n\n");
        }

        // 2. 架构
        sb.append("## 2. 架构模式\n\n");
        sb.append("**").append(project.getProjectPattern()).append("**\n\n");

        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));
        sb.append("```\n");
        for (Map.Entry<String, List<ClassInfo>> entry : pkgMap.entrySet()) {
            String pkg = entry.getKey();
            String[] parts = pkg.split("\\.");
            String name = parts.length > 0 ? parts[parts.length-1] : pkg;
            sb.append("  ").append(name).append("/\n");
            for (ClassInfo ci : entry.getValue()) {
                String icon = "interface".equals(ci.getType()) ? "[I]" :
                              "enum".equals(ci.getType()) ? "[E]" : "[C]";
                sb.append("    ").append(icon).append(" ").append(ci.getSimpleName()).append("\n");
            }
        }
        sb.append("```\n\n");

        // 3. API
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        sb.append("## 3. API 路由\n\n");
        if (endpoints.isEmpty()) {
            sb.append("（无 Spring MVC 端点）\n\n");
        } else {
            sb.append("| 方法 | 路径 | Controller | 方法 |\n|---|---|---|---|\n");
            for (ApiEndpoint ep : endpoints) {
                sb.append("| ").append(ep.getHttpMethod()).append(" | `").append(ep.getPath()).append("`")
                  .append(" | ").append(ep.getControllerClass())
                  .append(" | `").append(ep.getMethodName()).append("` |\n");
            }
            sb.append("\n");
        }

        // 4. 数据库
        List<TableInfo> tables = project.getDatabaseTables();
        sb.append("## 4. 数据库\n\n");
        if (tables.isEmpty()) {
            sb.append("（未检测到 JPA/MyBatis 映射）\n\n");
        } else {
            for (TableInfo table : tables) {
                sb.append("### ").append(table.getTableName()).append("\n\n");
                if (table.getEntityClass() != null) sb.append("Entity: ").append(table.getEntityClass()).append("\n\n");
                sb.append("| 字段 | 列 | 类型 | PK |\n|---|---|---|---|\n");
                for (TableInfo.Column col : table.getColumns()) {
                    sb.append("| `").append(col.getFieldName()).append("`")
                      .append(" | `").append(col.getColumnName()).append("`")
                      .append(" | ").append(col.getJavaType())
                      .append(" | ").append(col.isPrimaryKey() ? "PK" : "").append(" |\n");
                }
                sb.append("\n");
            }
        }

        // 5. Bean 依赖
        Map<String, List<String>> deps = project.getBeanDependencies();
        sb.append("## 5. Bean 依赖\n\n");
        if (deps.isEmpty()) {
            sb.append("（未检测到 Spring Bean）\n\n");
        } else {
            sb.append("```\n");
            for (Map.Entry<String, List<String>> entry : deps.entrySet()) {
                sb.append(shorten(entry.getKey())).append(" → ");
                sb.append(entry.getValue().stream().map(this::shorten).collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("```\n\n");
        }

        // 6. 调用链
        List<String> chains = project.getCriticalChains();
        sb.append("## 6. 调用链\n\n");
        if (chains.isEmpty()) {
            sb.append("（未检测到）\n\n");
        } else {
            for (String chain : chains) sb.append("- ").append(chain).append("\n");
            sb.append("\n");
        }

        // 7. 关键类
        sb.append("## 7. 关键类\n\n");
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

        // 8. 业务流
        sb.append("## 8. 业务流\n\n");
        if (!endpoints.isEmpty()) {
            // 按前缀分组推断业务模块
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
                sb.append("**").append(module).append("**: ");
                sb.append(entry.getValue().stream()
                    .map(ep -> ep.getHttpMethod() + " `" + ep.getPath() + "`")
                    .collect(Collectors.joining(", ")));
                sb.append("\n\n");
            }
        } else {
            // 无 API 时按包名推断
            for (String pkg : pkgMap.keySet()) {
                String[] parts = pkg.split("\\.");
                String name = parts.length > 0 ? parts[parts.length-1] : pkg;
                sb.append("**").append(name).append("**: ").append(pkgMap.get(pkg).size()).append(" 个类\n\n");
            }
        }

        // 9. 配置
        Map<String, String> config = project.getConfigProperties();
        sb.append("## 9. 配置\n\n");
        if (config.isEmpty()) {
            sb.append("（未检测到配置文件）\n\n");
        } else {
            for (Map.Entry<String, String> e : config.entrySet())
                sb.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n");
            sb.append("\n");
        }

        // 10. 用法提示
        sb.append("## 10. 对 Agent 的提示\n\n");
        sb.append("当用户询问以下类型问题时，参考对应章节回答：\n\n");
        sb.append("| 用户想问 | 看哪节 |\n|---|---|\n");
        sb.append("| 这是什么项目？ | 1. 概览 / 2. 架构 |\n");
        sb.append("| 有哪些接口？ | 3. API 路由 |\n");
        sb.append("| 数据库怎么设计的？ | 4. 数据库 |\n");
        sb.append("| 改这个字段影响哪？ | 5. Bean依赖 / 6. 调用链 |\n");
        sb.append("| 帮我加个接口 / 改个业务 | 8. 业务流 / 7. 关键类 |\n");
        sb.append("| 这个配置是什么意思？ | 9. 配置 |\n\n");

        sb.append("---\n");
        sb.append("> 由 ProjectAssistant 生成 | 安装命令: `copaw skill install <this-dir>`\n");

        return sb.toString();
    }

    private String shorten(String s) {
        if (s == null) return "?";
        int i = s.lastIndexOf('.');
        return i >= 0 ? s.substring(i + 1) : s;
    }
}
