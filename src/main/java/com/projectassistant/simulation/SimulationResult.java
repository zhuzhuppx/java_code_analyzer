package com.projectassistant.simulation;

import java.util.*;

/**
 * 一次推演的结果
 */
public class SimulationResult {
    public String scenarioName;
    public String scenarioDesc;
    public long timestamp = System.currentTimeMillis();

    /** 每个人的选择和评分 */
    public List<PersonDecision> decisions = new ArrayList<>();

    /** 推演文本（叙事） */
    public String narrative;

    /** 总结 */
    public String summary;

    /** 每个人的选择 */
    public static class PersonDecision {
        public String personName;
        public String personalityLabel;
        public String chosenOption;
        public double utilityScore;
        public List<OptionScore> allScores = new ArrayList<>();
    }

    public static class OptionScore {
        public String optionLabel;
        public double utility;
        public Map<String, Double> breakdown = new LinkedHashMap<>();
    }

    /** 生成可读报告 */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# 🎭 社会推演：").append(scenarioName).append("\n\n");
        md.append("> ").append(scenarioDesc).append("\n\n");

        md.append("## 参与者\n\n");
        for (PersonDecision d : decisions) {
            md.append("### 👤 ").append(d.personName).append(" (").append(d.personalityLabel).append(")\n\n");

            md.append("| 选项 | 效用值 |\n|---|---|\n");
            // 按效用值从高到低排序
            d.allScores.stream()
                .sorted((a, b) -> Double.compare(b.utility, a.utility))
                .forEach(s -> {
                    String chosen = s.optionLabel.equals(d.chosenOption) ? " ✅ **选中**" : "";
                    md.append("| ").append(s.optionLabel).append(chosen)
                      .append(" | ").append(String.format("%.2f", s.utility)).append(" |\n");
                });

            // 显示选中选项的效用分解
            OptionScore chosen = d.allScores.stream()
                .filter(s -> s.optionLabel.equals(d.chosenOption))
                .findFirst().orElse(null);
            if (chosen != null && !chosen.breakdown.isEmpty()) {
                md.append("\n**决策因素分解**（").append(d.chosenOption).append("）：\n\n");
                md.append("| 因素 | 权重 |\n|---|---|\n");
                for (var e : chosen.breakdown.entrySet()) {
                    md.append("| ").append(e.getKey()).append(" | ")
                      .append(String.format("%.2f", e.getValue())).append(" |\n");
                }
            }
            md.append("\n");
        }

        md.append("## 📖 推演叙事\n\n").append(narrative).append("\n\n");
        md.append("## 💡 总结\n\n").append(summary).append("\n");

        return md.toString();
    }
}
