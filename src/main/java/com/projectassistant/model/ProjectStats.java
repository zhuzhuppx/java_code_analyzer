package com.projectassistant.model;

/**
 * 项目统计 — 项目健康度指标
 */
public class ProjectStats {

    private int totalFiles;
    private int totalLines;
    private int totalCodeLines;
    private int totalCommentLines;
    private int totalClasses;
    private int totalInterfaces;
    private int totalEnums;
    private int totalRecords;
    private int totalAnnotations;
    private int totalMethods;
    private int totalFields;
    private int totalDependencies;
    private double commentRatio;        // 注释率
    private double averageMethodLines;  // 方法平均行数
    private int maxComplexity;          // 最大圈复杂度

    // 代码异味指标
    private int godClassCount;          // 上帝类（方法 > 20 或行数 > 500）
    private int longMethodCount;        // 长方法（> 50 行）
    private int highComplexityCount;    // 高复杂度方法（> 10）

    // === Getters & Setters ===

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

    public int getTotalCodeLines() { return totalCodeLines; }
    public void setTotalCodeLines(int totalCodeLines) { this.totalCodeLines = totalCodeLines; }

    public int getTotalCommentLines() { return totalCommentLines; }
    public void setTotalCommentLines(int totalCommentLines) { this.totalCommentLines = totalCommentLines; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public int getTotalInterfaces() { return totalInterfaces; }
    public void setTotalInterfaces(int totalInterfaces) { this.totalInterfaces = totalInterfaces; }

    public int getTotalEnums() { return totalEnums; }
    public void setTotalEnums(int totalEnums) { this.totalEnums = totalEnums; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getTotalAnnotations() { return totalAnnotations; }
    public void setTotalAnnotations(int totalAnnotations) { this.totalAnnotations = totalAnnotations; }

    public int getTotalMethods() { return totalMethods; }
    public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }

    public int getTotalFields() { return totalFields; }
    public void setTotalFields(int totalFields) { this.totalFields = totalFields; }

    public int getTotalDependencies() { return totalDependencies; }
    public void setTotalDependencies(int totalDependencies) { this.totalDependencies = totalDependencies; }

    public double getCommentRatio() { return commentRatio; }
    public void setCommentRatio(double commentRatio) { this.commentRatio = commentRatio; }

    public double getAverageMethodLines() { return averageMethodLines; }
    public void setAverageMethodLines(double averageMethodLines) { this.averageMethodLines = averageMethodLines; }

    public int getMaxComplexity() { return maxComplexity; }
    public void setMaxComplexity(int maxComplexity) { this.maxComplexity = maxComplexity; }

    public int getGodClassCount() { return godClassCount; }
    public void setGodClassCount(int godClassCount) { this.godClassCount = godClassCount; }

    public int getLongMethodCount() { return longMethodCount; }
    public void setLongMethodCount(int longMethodCount) { this.longMethodCount = longMethodCount; }

    public int getHighComplexityCount() { return highComplexityCount; }
    public void setHighComplexityCount(int highComplexityCount) { this.highComplexityCount = highComplexityCount; }

    public String getHealthLevel() {
        int score = 100;
        if (commentRatio < 0.1) score -= 15;
        if (godClassCount > 3) score -= 15;
        if (longMethodCount > 10) score -= 15;
        if (highComplexityCount > 10) score -= 15;
        if (averageMethodLines > 30) score -= 10;

        if (score >= 85) return "A - 优秀";
        if (score >= 70) return "B - 良好";
        if (score >= 55) return "C - 需要改进";
        return "D - 危险";
    }
}
