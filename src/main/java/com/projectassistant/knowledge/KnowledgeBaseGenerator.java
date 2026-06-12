package com.projectassistant.knowledge;

import com.projectassistant.model.*;
import com.projectassistant.spring.ApiEndpoint;
import com.projectassistant.spring.BeanInfo;
import com.projectassistant.spring.BeanInfo.InjectionPoint;
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
        rawClassDetails();
        businessFlow();
        configurations();
        devGuide();
        return sb.toString();
    }

    private void preamble() {
        sb.append("# 项目知识库\n\n");
        sb.append("> 由 Java老狗 自动生成，专为大模型理解优化\n");
        sb.append("> 生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .append("\n\n---\n\n");
        sb.append("## ⚠️ 使用规则\n\n");
        sb.append("**你在回答本项目的问题时，必须严格遵守以下规则：**\n\n");
        sb.append("1. **只使用知识库中明确列出的信息。** 不要编造、猜测或补全任何未列出的内容。\n");
        sb.append("2. **API 文档必须严格依据「3. API 路由大全」生成。** 每个接口的路径、方法、参数、返回值都以此为唯一来源。\n");
        sb.append("3. **不要自行推断未出现的 API、字段或表。** 如果某个 API 未在「3. API 路由大全」中出现，则它不存在。\n");
        sb.append("4. **接口描述以知识库中的 summary 为准。** 如果 summary 为空，直接使用方法名作为描述。\n");
        sb.append("5. **参数信息以知识库为准。** 路径参数、查询参数、请求体类型都来自扫描结果，不要额外添加。\n\n");
        sb.append("**违反上述规则的后果：** 用户会得到错误的 API 文档，导致开发故障。请务必遵守。\n\n---\n\n");
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

        // 依赖漏洞摘要
        List<ProjectModel.VulnFinding> vulns = project.getVulnFindings();
        if (!vulns.isEmpty()) {
            sb.append("### ⚠️ 已知安全漏洞 (").append(vulns.size()).append(")\n\n");
            long crit = vulns.stream().filter(v -> "CRITICAL".equals(v.severity)).count();
            long high = vulns.stream().filter(v -> "HIGH".equals(v.severity)).count();
            sb.append("> CRITICAL: ").append(crit).append(" | HIGH: ").append(high)
              .append(" | 其他: ").append(vulns.size() - crit - high).append("\n\n");
            sb.append("| 组件 | 当前版本 | CVE | 严重程度 | 说明 |\n|---|---|---|---|---|\n");
            for (ProjectModel.VulnFinding v : vulns) {
                sb.append("| ").append(v.artifactId).append(" | ").append(v.currentVersion)
                  .append(" | ").append(v.cve).append(" | **").append(v.severity).append("**")
                  .append(" | ").append(v.description).append(" |\n");
            }
            sb.append("\n");
        }

        // 健康评分
        sb.append("### 健康评分: **").append(calculateHealthScore()).append("/100**\n\n");
    }

    /** 计算项目健康评分 */
    private int calculateHealthScore() {
        int score = 80;
        List<ProjectModel.VulnFinding> vulns = project.getVulnFindings();
        if (!vulns.isEmpty()) {
            long crit = vulns.stream().filter(v -> "CRITICAL".equals(v.severity)).count();
            long high = vulns.stream().filter(v -> "HIGH".equals(v.severity)).count();
            score -= crit * 8 + high * 4;
        }
        if (project.getDatabaseTables().isEmpty() && !project.getMapperSql().isEmpty()) score -= 5;
        int totalMethods = project.getStats().getTotalMethods();
        if (totalMethods > 0) {
            double avgComplexity = project.getClasses().stream()
                    .flatMap(c -> c.getMethods().stream())
                    .filter(m -> m.getCyclomaticComplexity() > 0)
                    .mapToInt(m -> m.getCyclomaticComplexity())
                    .average().orElse(0);
            if (avgComplexity > 10) score -= 10;
            else if (avgComplexity > 5) score -= 5;
        }
        if (!project.getDatabaseTables().isEmpty()) score += 5;
        if (!project.getBeanInfos().isEmpty()) score += 5;
        if (!project.getCallChains().isEmpty()) score += 5;
        return Math.max(0, Math.min(100, score));
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

        // 按 Controller 分组
        Map<String, List<ApiEndpoint>> byController = endpoints.stream()
                .collect(Collectors.groupingBy(ApiEndpoint::getControllerClass, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ApiEndpoint>> entry : byController.entrySet()) {
            String controller = entry.getKey();
            List<ApiEndpoint> eps = entry.getValue();
            String shortName = controller.substring(controller.lastIndexOf('.') + 1);
            sb.append("### ").append(shortName).append("\n\n");
            sb.append("> `").append(controller).append("` — ").append(eps.size()).append(" 个接口\n\n");

            for (ApiEndpoint ep : eps) {
                // 摘要行
                sb.append("- **").append(ep.getHttpMethod()).append("** `").append(ep.getPath()).append("`");
                if (ep.getSummary() != null && !ep.getSummary().isEmpty()) {
                    sb.append(" — ").append(ep.getSummary());
                }
                if (ep.isDeprecated()) sb.append(" ⚠️**已废弃**");
                sb.append("\n");
                sb.append("  - 方法: `").append(ep.getMethodName()).append("()`");
                if (ep.getReturnType() != null && !ep.getReturnType().equals("void")) {
                    sb.append(" → ").append(ep.getReturnType());
                }
                sb.append("\n");

                // 路径变量
                if (!ep.getPathVariables().isEmpty()) {
                    sb.append("  - 路径参数: ");
                    sb.append(ep.getPathVariables().stream()
                            .map(p -> "`" + p.getName() + ": " + p.getType() + "`")
                            .collect(Collectors.joining(", ")));
                    sb.append("\n");
                }

                // 查询参数
                if (!ep.getRequestParams().isEmpty()) {
                    sb.append("  - 查询参数: ");
                    sb.append(ep.getRequestParams().stream()
                            .map(p -> "`" + p.getName() + ": " + p.getType() + "`"
                                    + (p.isRequired() ? "" : " (可选)")
                                    + (p.getDefaultValue() != null ? " = " + p.getDefaultValue() : ""))
                            .collect(Collectors.joining(", ")));
                    sb.append("\n");
                }

                // 请求体
                if (ep.getRequestBodyType() != null) {
                    sb.append("  - 请求体: `").append(ep.getRequestBodyType()).append("`\n");
                }

                // 请求头
                if (!ep.getRequestHeaders().isEmpty()) {
                    sb.append("  - 请求头: ");
                    sb.append(ep.getRequestHeaders().stream()
                            .map(p -> "`" + p.getName() + ": " + p.getType() + "`")
                            .collect(Collectors.joining(", ")));
                    sb.append("\n");
                }

                // consumes/produces
                if (ep.getConsumes() != null || ep.getProduces() != null) {
                    sb.append("  - 媒体类型: ");
                    if (ep.getConsumes() != null) sb.append("请求: `").append(ep.getConsumes()).append("`");
                    if (ep.getConsumes() != null && ep.getProduces() != null) sb.append(" ");
                    if (ep.getProduces() != null) sb.append("响应: `").append(ep.getProduces()).append("`");
                    sb.append("\n");
                }

                // 安全
                if (ep.isSecured()) sb.append("  - 🔒 需认证\n");
            }
            sb.append("\n");
        }
    }

    private void databaseSchema() {
        List<TableInfo> tables = project.getDatabaseTables();
        sb.append("## 4. 数据库\n\n");

        // 表结构
        if (tables.isEmpty()) {
            sb.append("### 表结构\n\n（无结构化表信息）\n\n");
        } else {
            sb.append("### 表结构 (").append(tables.size()).append(" 张表)\n\n");
            for (TableInfo table : tables) {
                sb.append("#### ").append(table.getTableName()).append("\n\n");
                if (table.getEntityClass() != null) sb.append("Entity: ").append(table.getEntityClass()).append("\n\n");
                sb.append("| 字段 | 列名 | 类型 | 主键 | 自增 | 可空 | 默认值 |\n");
                sb.append("|---|---|---|---|---|---|---|\n");
                for (TableInfo.Column col : table.getColumns()) {
                    sb.append("| `").append(col.getFieldName()).append("`")
                      .append(" | `").append(col.getColumnName()).append("`")
                      .append(" | ").append(col.getJavaType())
                      .append(" | ").append(col.isPrimaryKey() ? "PK" : "")
                      .append(" | ").append(col.isAutoIncrement() ? "自增" : "")
                      .append(" | ").append(!col.isNullable() ? "NOT NULL" : "")
                      .append(" | ").append(col.getDefaultValue() != null ? col.getDefaultValue() : "")
                      .append(" |\n");
                }
                sb.append("\n");
            }
        }

        // Mapper SQL 原文
        Map<String, String> mapperSql = project.getMapperSql();
        if (!mapperSql.isEmpty()) {
            sb.append("### Mapper SQL (").append(mapperSql.size()).append(" 条)\n\n");
            sb.append("> 以下为 Mapper XML 或注解中的原始 SQL，大模型可直接分析 SQL 正确性、性能、注入风险。\n\n");
            for (Map.Entry<String, String> entry : mapperSql.entrySet()) {
                sb.append("**").append(entry.getKey()).append("**\n\n");
                sb.append("```sql\n").append(entry.getValue()).append("\n```\n\n");
            }
        } else {
            sb.append("### Mapper SQL\n\n（未检测到 Mapper SQL）\n\n");
        }
    }

    private void beanGraph() {
        List<BeanInfo> infos = project.getBeanInfos();
        if (infos.isEmpty()) { sb.append("## 5. Bean 依赖图\n\n无（未检测到 Spring Bean）。\n\n"); return; }

        sb.append("## 5. Bean 依赖图 (").append(infos.size()).append(" 个 Bean)\n\n");

        // 按角色分组展示
        Map<String, List<BeanInfo>> byRole = infos.stream()
                .collect(Collectors.groupingBy(BeanInfo::getRole, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<BeanInfo>> roleEntry : byRole.entrySet()) {
            sb.append("### ").append(capitalize(roleEntry.getKey())).append(" (").append(roleEntry.getValue().size()).append(")\n\n");
            for (BeanInfo bi : roleEntry.getValue()) {
                sb.append("- **").append(shorten(bi.getClassName())).append("**");
                if (!bi.getBeanName().equals(shorten(bi.getSimpleName())))
                    sb.append(" [@").append(capitalize(bi.getRole())).append("(\"").append(bi.getBeanName()).append("\")]");
                if (bi.isPrimary()) sb.append(" 🏆 @Primary");
                if (bi.getScope() != null && !"singleton".equals(bi.getScope()))
                    sb.append(" [@Scope(\"").append(bi.getScope()).append("\")]");

                // 注入的依赖
                if (!bi.getInjections().isEmpty()) {
                    sb.append(" 注入:");
                    for (InjectionPoint ip : bi.getInjections()) {
                        sb.append(" ").append(ip.getAnnotation()).append(" ");
                        if (ip.getInjectionType().equals("constructor")) sb.append("构造器");
                        else sb.append(ip.getFieldName());
                        sb.append(" → ").append(shorten(ip.getTargetType()));
                        if (ip.getTargetBeanName() != null && !ip.getTargetBeanName().equals(ip.getTargetType())) {
                            sb.append(" [").append(shorten(ip.getTargetBeanName())).append("]");
                        }
                        if (ip.getQualifier() != null) sb.append(" @Qualifier(\"").append(ip.getQualifier()).append("\")");
                    }
                }
                // 被谁注入
                if (!bi.getInjectedBy().isEmpty()) {
                    sb.append(" | 被: ");
                    sb.append(bi.getInjectedBy().stream().map(this::shorten).collect(Collectors.joining(", ")));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 依赖关系汇总图
        Map<String, List<String>> deps = project.getBeanDependencies();
        if (!deps.isEmpty()) {
            sb.append("### 依赖关系（简图）\n\n```\n");
            for (Map.Entry<String, List<String>> entry : deps.entrySet()) {
                sb.append(shorten(entry.getKey())).append(" 依赖: ");
                sb.append(entry.getValue().stream().map(this::shorten).collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("```\n\n");
        }

        // 依赖漏洞摘要
        List<ProjectModel.VulnFinding> vulns = project.getVulnFindings();
        if (!vulns.isEmpty()) {
            sb.append("### ⚠️ 已知安全漏洞 (").append(vulns.size()).append(")\n\n");
            long crit = vulns.stream().filter(v -> "CRITICAL".equals(v.severity)).count();
            long high = vulns.stream().filter(v -> "HIGH".equals(v.severity)).count();
            sb.append("> CRITICAL: ").append(crit).append(" | HIGH: ").append(high)
              .append(" | 其他: ").append(vulns.size() - crit - high).append("\n\n");
            sb.append("| 组件 | 当前版本 | CVE | 严重程度 | 说明 |\n|---|---|---|---|---|\n");
            for (ProjectModel.VulnFinding v : vulns) {
                sb.append("| ").append(v.artifactId).append(" | ").append(v.currentVersion)
                  .append(" | ").append(v.cve).append(" | **").append(v.severity).append("**")
                  .append(" | ").append(v.description).append(" |\n");
            }
            sb.append("\n");
        }

        // 健康评分
        sb.append("### 健康评分: **").append(calculateHealthScore()).append("/100**\n\n");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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

    /**
     * 类全量信息 — 让大模型自己推理出实体、表结构、业务逻辑
     */
    private void rawClassDetails() {
        sb.append("## 8. 类全量信息\n\n");
        sb.append("> 以下为每个类的详细字段和注解信息，大模型可据此自行推断数据库结构、业务逻辑等。\n\n");

        // 按包分组，优先展示可能包含实体的包
        Map<String, List<ClassInfo>> pkgMap = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassInfo::getPackageName, TreeMap::new, Collectors.toList()));

        // 先把可能包含 entity/domain/model/pojo 的包排在前面
        List<String> priorityPkgs = new ArrayList<>();
        List<String> otherPkgs = new ArrayList<>();
        for (String pkg : pkgMap.keySet()) {
            String lower = pkg.toLowerCase();
            if (lower.contains("entity") || lower.contains("domain") || lower.contains("model")
                    || lower.contains("pojo") || lower.contains("dto") || lower.contains("vo")) {
                priorityPkgs.add(pkg);
            } else {
                otherPkgs.add(pkg);
            }
        }

        List<String> orderedPkgs = new ArrayList<>(priorityPkgs);
        orderedPkgs.addAll(otherPkgs);

        for (String pkg : orderedPkgs) {
            List<ClassInfo> classes = pkgMap.get(pkg);
            sb.append("### ").append(pkg).append("\n\n");

            for (ClassInfo ci : classes) {
                // 类头：注解 + 类型 + 类名
                if (!ci.getAnnotations().isEmpty()) {
                    sb.append("```\n");
                    for (String ann : ci.getAnnotations()) {
                        sb.append("@").append(ann).append("\n");
                    }
                    sb.append(ci.getType()).append(" ").append(ci.getSimpleName());
                    if (ci.getSuperClassName() != null && !ci.getSuperClassName().equals("java.lang.Object")) {
                        sb.append(" extends ").append(shorten(ci.getSuperClassName()));
                    }
                    sb.append(" {\n");
                } else {
                    sb.append("```\n");
                    sb.append(ci.getType()).append(" ").append(ci.getSimpleName());
                    if (ci.getSuperClassName() != null && !ci.getSuperClassName().equals("java.lang.Object")) {
                        sb.append(" extends ").append(shorten(ci.getSuperClassName()));
                    }
                    sb.append(" {\n");
                }

                // 字段
                for (FieldInfo f : ci.getFields()) {
                    String indent = ci.getMethods().size() > 5 ? "    // " : "    ";
                    if (!f.getAnnotations().isEmpty()) {
                        for (String ann : f.getAnnotations()) {
                            sb.append("    @").append(ann).append("\n");
                        }
                    }
                    sb.append("    ").append(f.getType()).append(" ").append(f.getName());
                    if ("" != null && !"".isEmpty()) {
                        sb.append(" = ").append("");
                    }
                    sb.append(";\n");
                }

                // 方法名摘要（不展开方法体）
                if (!ci.getMethods().isEmpty()) {
                    sb.append("\n    // --- ").append(ci.getMethods().size()).append(" 个方法 ---\n");
                    for (MethodInfo m : ci.getMethods()) {
                        sb.append("    ").append(m.getReturnType()).append(" ")
                          .append(m.getName()).append("(");
                        if (m.getParameters() != null && !m.getParameters().isEmpty()) {
                            sb.append(String.join(", ", m.getParameters()));
                        }
                        sb.append(");\n");
                    }
                }

                sb.append("}\n");
                sb.append("```\n\n");
            }
        }
    }

    private void businessFlow() {
        List<ApiEndpoint> endpoints = project.getApiEndpoints();
        sb.append("## 9. 业务流\n\n");
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
        sb.append("## 10. 配置\n\n");
        if (config.isEmpty()) { sb.append("无。\n\n"); return; }
        for (Map.Entry<String, String> e : config.entrySet())
            sb.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n");
        sb.append("\n");
    }

    private void devGuide() {
        sb.append("## 11. 开发指南\n\n");
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
        sb.append("| 帮我加个接口 | 9. 业务流 / 7. 关键类 |\n");
        sb.append("| 这个类的字段和注解？ | 8. 类全量信息 |\n");
        sb.append("| 这个配置是什么意思？ | 10. 配置 |\n\n");
        sb.append("---\n> 知识库由 Java老狗 生成 | 配合大模型使用效果更佳\n");
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
        md.append("> 由 Java老狗 自动生成 | ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
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
            // 按 Controller 分组
            Map<String, List<ApiEndpoint>> byCtrl = eps.stream()
                    .collect(Collectors.groupingBy(ApiEndpoint::getControllerClass, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<ApiEndpoint>> e : byCtrl.entrySet()) {
                String ctrl = e.getKey();
                String shortName = ctrl.substring(ctrl.lastIndexOf('.') + 1);
                md.append("### ").append(shortName).append("\n\n");
                md.append("> `").append(ctrl).append("` — ").append(e.getValue().size()).append(" 个接口\n\n");
                for (ApiEndpoint ep : e.getValue()) {
                    md.append("- **").append(ep.getHttpMethod()).append("** `").append(ep.getPath()).append("`");
                    if (ep.getSummary() != null && !ep.getSummary().isEmpty()) {
                        md.append(" — ").append(ep.getSummary());
                    }
                    if (ep.isDeprecated()) md.append(" ⚠️已废弃");
                    md.append("\n");
                    if (ep.getReturnType() != null && !ep.getReturnType().equals("void")) {
                        md.append("  - 返回: ").append(ep.getReturnType()).append("\n");
                    }
                    if (!ep.getPathVariables().isEmpty()) {
                        md.append("  - 路径参数: ").append(ep.getPathVariables().stream()
                                .map(p -> "`" + p.getName() + ": " + p.getType() + "`")
                                .collect(Collectors.joining(", "))).append("\n");
                    }
                    if (!ep.getRequestParams().isEmpty()) {
                        md.append("  - 查询参数: ").append(ep.getRequestParams().stream()
                                .map(p -> "`" + p.getName() + ": " + p.getType() + "`"
                                        + (p.isRequired() ? "" : " (可选)")
                                        + (p.getDefaultValue() != null ? " = " + p.getDefaultValue() : ""))
                                .collect(Collectors.joining(", "))).append("\n");
                    }
                    if (ep.getRequestBodyType() != null) {
                        md.append("  - 请求体: `").append(ep.getRequestBodyType()).append("`\n");
                    }
                    if (ep.isSecured()) md.append("  - 🔒 需认证\n");
                }
                md.append("\n");
            }
        }

        // === 4. 数据库 ===
        List<TableInfo> tables = project.getDatabaseTables();
        Map<String, String> mapperSql = project.getMapperSql();
        md.append("## 4. 数据库\n\n");

        if (!tables.isEmpty()) {
            md.append("### 表结构 (").append(tables.size()).append(" 张表)\n\n");
            for (TableInfo t : tables) {
                md.append("#### ").append(t.getTableName()).append("\n\n");
                if (t.getEntityClass() != null) md.append("Entity: ").append(t.getEntityClass()).append("\n\n");
                md.append("| 字段 | 列 | 类型 | PK | 自增 | 可空 |\n|---|---|---|---|---|---|\n");
                for (TableInfo.Column col : t.getColumns())
                    md.append("| `").append(col.getFieldName()).append("` | `").append(col.getColumnName()).append("` | ").append(col.getJavaType()).append(" | ").append(col.isPrimaryKey() ? "PK" : "").append(" | ").append(col.isAutoIncrement() ? "自增" : "").append(" | ").append(!col.isNullable() ? "NOT NULL" : "").append(" |\n");
                md.append("\n");
            }
        }

        if (!mapperSql.isEmpty()) {
            md.append("### Mapper SQL (").append(mapperSql.size()).append(" 条)\n\n");
            for (Map.Entry<String, String> entry : mapperSql.entrySet()) {
                md.append("**").append(entry.getKey()).append("**\n\n```sql\n").append(entry.getValue()).append("\n```\n\n");
            }
        }

        if (tables.isEmpty() && mapperSql.isEmpty()) {
            md.append("（无）\n\n");
        }

        // === 5. Bean ===
        List<BeanInfo> binfs = project.getBeanInfos();
        md.append("## 5. Bean 依赖图\n\n");
        if (binfs.isEmpty()) {
            md.append("（无）\n\n");
        } else {
            Map<String, List<BeanInfo>> byRole = binfs.stream()
                    .collect(Collectors.groupingBy(BeanInfo::getRole, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<BeanInfo>> roleEntry : byRole.entrySet()) {
                md.append("### ").append(capitalize(roleEntry.getKey())).append(" (").append(roleEntry.getValue().size()).append(")\n\n");
                for (BeanInfo bi : roleEntry.getValue()) {
                    md.append("- **").append(shorten(bi.getClassName())).append("**");
                    if (bi.isPrimary()) md.append(" 🏆");
                    if (!bi.getInjections().isEmpty()) {
                        md.append(" 注入:");
                        for (InjectionPoint ip : bi.getInjections()) {
                            md.append(" ").append(ip.getAnnotation()).append(" ");
                            if (ip.getInjectionType().equals("constructor")) md.append("构造器");
                            else md.append(ip.getFieldName());
                            md.append(" → ").append(shorten(ip.getTargetType()));
                            if (ip.getTargetBeanName() != null && !ip.getTargetBeanName().equals(ip.getTargetType()))
                                md.append(" [").append(shorten(ip.getTargetBeanName())).append("]");
                        }
                    }
                    md.append("\n");
                }
                md.append("\n");
            }
            Map<String, List<String>> deps = project.getBeanDependencies();
            if (!deps.isEmpty()) {
                md.append("依赖关系:\n```\n");
                for (Map.Entry<String, List<String>> e : deps.entrySet())
                    md.append(shorten(e.getKey())).append(" → ").append(e.getValue().stream().map(this::shorten).collect(Collectors.joining(", "))).append("\n");
                md.append("```\n\n");
            }
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

        // === 8. 类全量信息 ===
        md.append("## 8. 类全量信息\n\n");
        md.append("> 以下为每个类的详细字段和注解信息，大模型可据此自行推断数据库结构、业务逻辑。\n\n");
        for (String pkg : pkgMap.keySet()) {
            List<ClassInfo> classes = pkgMap.get(pkg);
            md.append("### ").append(pkg).append("\n\n");
            for (ClassInfo ci : classes) {
                if (!ci.getAnnotations().isEmpty()) {
                    md.append("```\n");
                    for (String ann : ci.getAnnotations()) md.append("@").append(ann).append("\n");
                    md.append(ci.getType()).append(" ").append(ci.getSimpleName());
                    if (ci.getSuperClassName() != null && !ci.getSuperClassName().equals("java.lang.Object"))
                        md.append(" extends ").append(shorten(ci.getSuperClassName()));
                    md.append(" {\n");
                } else {
                    md.append("```\n").append(ci.getType()).append(" ").append(ci.getSimpleName());
                    if (ci.getSuperClassName() != null && !ci.getSuperClassName().equals("java.lang.Object"))
                        md.append(" extends ").append(shorten(ci.getSuperClassName()));
                    md.append(" {\n");
                }
                for (FieldInfo f : ci.getFields()) {
                    if (!f.getAnnotations().isEmpty()) {
                        for (String ann : f.getAnnotations()) md.append("    @").append(ann).append("\n");
                    }
                    md.append("    ").append(f.getType()).append(" ").append(f.getName()).append(";\n");
                }
                if (!ci.getMethods().isEmpty()) {
                    md.append("\n    // --- ").append(ci.getMethods().size()).append(" 个方法 ---\n");
                    for (MethodInfo m : ci.getMethods()) {
                        md.append("    ").append(m.getReturnType()).append(" ").append(m.getName()).append("(");
                        if (m.getParameters() != null && !m.getParameters().isEmpty())
                            md.append(String.join(", ", m.getParameters()));
                        md.append(");\n");
                    }
                }
                md.append("}\n").append("```\n\n");
            }
        }

        // === 9. 业务流 ===
        md.append("## 9. 业务流\n\n");
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

        // === 10. 配置 ===
        Map<String, String> config = project.getConfigProperties();
        md.append("## 10. 配置\n\n");
        if (config.isEmpty()) { md.append("（无）\n\n"); }
        else { for (Map.Entry<String, String> e : config.entrySet()) md.append("- `").append(e.getKey()).append("`: ").append(e.getValue()).append("\n"); md.append("\n"); }

        // === 11. Agent 提示 ===
        md.append("## 11. 对 Agent 的提示\n\n");
        md.append("| 用户想问 | 看哪节 |\n|---|---|\n");
        md.append("| 这是什么项目？ | 1. 概览 / 2. 架构 |\n");
        md.append("| 有哪些接口？ | 3. API |\n");
        md.append("| 数据库怎么设计的？ | 4. 数据库 |\n");
        md.append("| 改这个字段影响哪？ | 5. Bean / 6. 调用链 |\n");
        md.append("| 帮我加个接口 | 8. 业务流 / 7. 关键类 |\n");
        md.append("| 这个配置是什么意思？ | 9. 配置 |\n\n");
        md.append("---\n> 由 Java老狗 生成 | 安装命令: `copaw skill install <this-dir>`\n");

        Files.writeString(skillDir.resolve("SKILL.md"), md.toString());
        System.out.println("  ✅ 项目 Skill 已生成: " + skillDir.toAbsolutePath());
        System.out.println("  📦 安装命令: copaw skill install " + skillDir.toAbsolutePath());
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
