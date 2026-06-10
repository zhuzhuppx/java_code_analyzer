package com.projectassistant.model;

/**
 * 外部依赖信息
 */
public class DependencyInfo {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope;  // compile / runtime / test / provided / system
    private boolean optional;
    private String type;   // maven / gradle / jar

    public DependencyInfo() {}

    public DependencyInfo(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = "compile";
        this.type = "maven";
    }

    // === Getters & Setters ===

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return String.format("%s:%s:%s [%s]", groupId, artifactId, version, scope);
    }

    public String toGav() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
