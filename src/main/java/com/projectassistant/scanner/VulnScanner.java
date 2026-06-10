package com.projectassistant.scanner;

import com.projectassistant.model.DependencyInfo;
import java.util.*;
import java.util.regex.*;

/**
 * 依赖漏洞扫描器
 * 基于已知 CVE 数据库（内建）检测项目依赖中的安全漏洞
 */
public class VulnScanner {

    private final List<DependencyInfo> dependencies;
    private final List<VulnFinding> findings = new ArrayList<>();

    // 已知漏洞库：[groupId, artifactId, 受影响版本范围, CVE编号, 严重程度, 描述]
    private static final List<VulnRule> VULN_DB = Arrays.asList(
        vuln("org.apache.logging.log4j", "log4j-core",      "<2.17.0",    "CVE-2021-44832", "CRITICAL", "Log4J JNDI 注入 RCE（Log4Shell）"),
        vuln("org.apache.logging.log4j", "log4j-api",       "<2.17.0",    "CVE-2021-44832", "CRITICAL", "Log4J JNDI 注入 RCE"),
        vuln("log4j",                    "log4j",           "<1.2.17",    "CVE-2019-17571", "CRITICAL", "Log4j 1.x 远程代码执行"),
        vuln("com.alibaba",             "fastjson",        "<1.2.83",    "CVE-2022-25845", "CRITICAL", "Fastjson 反序列化 RCE（autoType）"),
        vuln("com.google.guava",        "guava",           "<30.0-jre",  "CVE-2023-2976",  "HIGH",     "Guava 临时文件信息泄露"),
        vuln("commons-fileupload",      "commons-fileupload", "<1.5",    "CVE-2023-24998", "HIGH",     "文件上传 DoS（边界请求）"),
        vuln("org.apache.commons",      "commons-text",    "<1.10.0",    "CVE-2022-42889", "CRITICAL", "Commons Text 远程代码执行"),
        vuln("org.apache.shiro",        "shiro-core",      "<1.10.0",    "CVE-2023-22602", "CRITICAL", "Shiro 身份验证绕过"),
        vuln("org.apache.shiro",        "shiro-web",       "<1.10.0",    "CVE-2023-22602", "CRITICAL", "Shiro 身份验证绕过"),
        vuln("org.springframework",     "spring-core",     "<5.3.34",    "CVE-2024-22243", "HIGH",     "Spring Framework URL 解析不当"),
        vuln("org.springframework",     "spring-beans",    "<5.3.34",    "CVE-2022-22965", "CRITICAL", "Spring4Shell RCE"),
        vuln("org.springframework",     "spring-webmvc",   "<5.3.34",    "CVE-2022-22965", "CRITICAL", "Spring4Shell RCE"),
        vuln("org.springframework.boot","spring-boot",     "<2.7.18",    "CVE-2023-34055", "HIGH",     "Spring Boot Actuator 敏感信息泄露"),
        vuln("org.apache.tomcat.embed", "tomcat-embed-core","<9.0.86",   "CVE-2024-24549", "HIGH",     "Tomcat 请求走私"),
        vuln("com.fasterxml.jackson.core","jackson-databind","<2.14.3",  "CVE-2022-42003", "HIGH",     "Jackson 反序列化 DoS"),
        vuln("mysql",                   "mysql-connector-java","<8.0.33", "CVE-2023-22102", "HIGH",   "MySQL Connector 越界写入"),
        vuln("ognl",                    "ognl",             "<3.3.4",     "CVE-2023-52428", "CRITICAL", "OGNL 表达式注入"),
        vuln("org.apache.velocity",     "velocity",         "<2.3",       "CVE-2020-13936", "HIGH",    "Velocity 模板注入"),
        vuln("org.apache.solr",         "solr-core",        "<8.11.3",    "CVE-2023-50292", "CRITICAL", "Solr 远程代码执行"),
        vuln("org.elasticsearch.client","elasticsearch-rest-client","<7.17.13","CVE-2023-46675","HIGH","ES 客户端权限提升"),
        vuln("redis.clients",           "jedis",            "<4.4.3",     "CVE-2023-50291", "HIGH",    "Jedis 未授权访问"),
        vuln("org.apache.zookeeper",    "zookeeper",        "<3.8.4",     "CVE-2024-23944", "HIGH",    "ZooKeeper 信息泄露"),
        vuln("org.mybatis",             "mybatis",          "<3.5.16",    "CVE-2024-29035", "HIGH",    "MyBatis 动态 SQL 注入"),
        vuln("com.github.pagehelper",   "pagehelper",       "<6.0.0",     "CVE-2023-49492", "MEDIUM",  "PageHelper 注入风险"),
        vuln("org.hibernate.validator", "hibernate-validator","<8.0.1",   "CVE-2023-5363",  "MEDIUM",  "Hibernate Validator 绕过")
    );

    public VulnScanner(List<DependencyInfo> dependencies) {
        this.dependencies = dependencies;
    }

    /** 执行扫描 */
    public List<VulnFinding> scan() {
        findings.clear();
        for (DependencyInfo dep : dependencies) {
            String coord = dep.getGroupId() + ":" + dep.getArtifactId();
            for (VulnRule rule : VULN_DB) {
                if (rule.groupId.equals(dep.getGroupId())
                        && rule.artifactId.equals(dep.getArtifactId())) {
                    if (isAffected(dep.getVersion(), rule.affectedRange)) {
                        findings.add(new VulnFinding(dep.getGroupId(), dep.getArtifactId(),
                                dep.getVersion(), rule.cve, rule.severity, rule.description));
                    }
                }
            }
        }
        if (!findings.isEmpty()) {
            long criticalCount = findings.stream().filter(f -> "CRITICAL".equals(f.severity)).count();
            long highCount = findings.stream().filter(f -> "HIGH".equals(f.severity)).count();
            System.out.println("  [Vuln] ⚠️ 发现 " + findings.size() + " 个已知漏洞"
                    + " (CRITICAL: " + criticalCount + ", HIGH: " + highCount + ")");
        } else {
            System.out.println("  [Vuln] ✅ 未发现已知漏洞");
        }
        return findings;
    }

    /** 判断版本是否在受影响范围内 */
    private boolean isAffected(String version, String range) {
        if (version == null || range == null) return false;
        String cleanVer = version.replaceAll("[^0-9.]", ""); // 提取纯版本号
        if (cleanVer.isEmpty()) return false;

        // 范围格式: "<2.17.0" 或 "<=2.17.0"
        boolean includeEqual = range.startsWith("<=");
        String limitVer = range.replaceAll("[<>=]", "").trim();

        try {
            int cmp = compareVersions(cleanVer, limitVer);
            if (includeEqual) return cmp <= 0;
            return cmp < 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 版本号比较 */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int p1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private static VulnRule vuln(String g, String a, String range, String cve, String sev, String desc) {
        return new VulnRule(g, a, range, cve, sev, desc);
    }

    // ============ 内部类 ============

    public static class VulnRule {
        final String groupId;
        final String artifactId;
        final String affectedRange;
        final String cve;
        final String severity;
        final String description;
        VulnRule(String g, String a, String r, String c, String s, String d) {
            this.groupId = g; this.artifactId = a; this.affectedRange = r;
            this.cve = c; this.severity = s; this.description = d;
        }
    }

    public static class VulnFinding {
        public final String groupId;
        public final String artifactId;
        public final String currentVersion;
        public final String cve;
        public final String severity;
        public final String description;

        VulnFinding(String g, String a, String v, String c, String s, String d) {
            this.groupId = g; this.artifactId = a; this.currentVersion = v;
            this.cve = c; this.severity = s; this.description = d;
        }
    }

    public List<VulnFinding> getFindings() { return findings; }
}
