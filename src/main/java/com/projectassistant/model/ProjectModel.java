package com.projectassistant.model;

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

    /**
     * 包依赖图: 包名 -> 它依赖的包名集合
     */
    public Map<String, Set<String>> getPackageDependencies() { return packageDependencies; }

    /**
     * 调用图: 调用者全限定签名 -> 被调用者列表
     */
    public Map<String, List<String>> getCallGraph() { return callGraph; }

    public ProjectStats getStats() { return stats; }

    @Override
    public String toString() {
        return "ProjectModel{" +
                "name='" + projectName + '\'' +
                ", buildType='" + buildType + '\'' +
                ", modules=" + modules.size() +
                ", classes=" + classes.size() +
                ", dependencies=" + dependencies.size() +
                '}';
    }
}
