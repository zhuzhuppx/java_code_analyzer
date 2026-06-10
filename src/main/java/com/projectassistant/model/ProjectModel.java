package com.projectassistant.model;

import com.projectassistant.spring.*;
import com.projectassistant.sql.*;
import com.projectassistant.chain.*;
import java.util.*;

/**
 * 项目模型 — 持有项目扫描和分析的全部数据
 */
public class ProjectModel {

    private String projectName;
    private String rootPath;
    private String buildType;
    private String javaVersion = "unknown";

    private final List<ModuleInfo> modules = new ArrayList<>();
    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<DependencyInfo> dependencies = new ArrayList<>();
    private final Map<String, Set<String>> packageDependencies = new HashMap<>();
    private final Map<String, List<String>> callGraph = new HashMap<>();
    private final ProjectStats stats = new ProjectStats();

    // ============ 升级：老狗级能力 ============
    private List<ApiEndpoint> apiEndpoints = new ArrayList<>();
    private Map<String, List<String>> beanDependencies = new HashMap<>();
    private String projectPattern = "unknown";
    private boolean springBoot = false;
    private List<TableInfo> databaseTables = new ArrayList<>();
    private Map<String, String> mapperSql = new HashMap<>();
    private List<CallChain> callChains = new ArrayList<>();
    private List<String> criticalChains = new ArrayList<>();
    private Map<String, String> configProperties = new HashMap<>();
    private List<BeanInfo> beanInfos = new ArrayList<>();
    private List<VulnFinding> vulnFindings = new ArrayList<>();

    public static class VulnFinding {
        public final String groupId;
        public final String artifactId;
        public final String currentVersion;
        public final String cve;
        public final String severity;
        public final String description;

        public VulnFinding(String g, String a, String v, String c, String s, String d) {
            this.groupId = g; this.artifactId = a; this.currentVersion = v;
            this.cve = c; this.severity = s; this.description = d;
        }
    }

    // ==================== 基本属性 ====================

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectPath() { return rootPath; }
    public void setRootPath(String rootPath) { this.rootPath = rootPath; }

    public String getBuildType() { return buildType; }
    public void setBuildType(String buildType) { this.buildType = buildType; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    // ==================== 数据结构 ====================

    public List<ModuleInfo> getModules() { return modules; }
    public List<ClassInfo> getClasses() { return classes; }
    public List<DependencyInfo> getDependencies() { return dependencies; }
    public Map<String, Set<String>> getPackageDependencies() { return packageDependencies; }
    public Map<String, List<String>> getCallGraph() { return callGraph; }
    public ProjectStats getStats() { return stats; }

    // ============ 升级方法 ============

    public List<ApiEndpoint> getApiEndpoints() { return apiEndpoints; }
    public void setApiEndpoints(List<ApiEndpoint> eps) { this.apiEndpoints = eps; }

    public Map<String, List<String>> getBeanDependencies() { return beanDependencies; }
    public void setBeanDependencies(Map<String, List<String>> deps) { this.beanDependencies = deps; }

    public String getProjectPattern() { return projectPattern; }
    public void setProjectPattern(String p) { this.projectPattern = p; }

    public boolean isSpringBoot() { return springBoot; }
    public void setSpringBoot(boolean sb) { this.springBoot = sb; }

    public List<TableInfo> getDatabaseTables() { return databaseTables; }
    public void setDatabaseTables(List<TableInfo> tables) { this.databaseTables = tables; }

    public Map<String, String> getMapperSql() { return mapperSql; }
    public void setMapperSql(Map<String, String> sql) { this.mapperSql = sql; }

    public List<CallChain> getCallChains() { return callChains; }
    public void setCallChains(List<CallChain> chains) { this.callChains = chains; }

    public List<String> getCriticalChains() { return criticalChains; }
    public void setCriticalChains(List<String> chains) { this.criticalChains = chains; }

    public Map<String, String> getConfigProperties() { return configProperties; }
    public void setConfigProperties(Map<String, String> props) { this.configProperties = props; }

    public List<BeanInfo> getBeanInfos() { return beanInfos; }
    public void setBeanInfos(List<BeanInfo> infos) { this.beanInfos = infos; }

    public List<VulnFinding> getVulnFindings() { return vulnFindings; }
    public void setVulnFindings(List<VulnFinding> v) { this.vulnFindings = v; }

    @Override
    public String toString() {
        return "ProjectModel{name='" + projectName + "', buildType='" + buildType
                + "', classes=" + classes.size() + ", endpoints=" + apiEndpoints.size()
                + ", tables=" + databaseTables.size() + ", chains=" + callChains.size() + "}";
    }
}
