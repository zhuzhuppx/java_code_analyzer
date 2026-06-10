package com.projectassistant.model;

/**
 * 模块信息 — Maven 子模块或项目子目录
 */
public class ModuleInfo {
    private String name;
    private String path;
    private String groupId;
    private String artifactId;
    private String version;
    private int classCount;
    private long totalLines;

    public ModuleInfo() {}

    public ModuleInfo(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getClassCount() { return classCount; }
    public void setClassCount(int classCount) { this.classCount = classCount; }

    public long getTotalLines() { return totalLines; }
    public void setTotalLines(long totalLines) { this.totalLines = totalLines; }
}
