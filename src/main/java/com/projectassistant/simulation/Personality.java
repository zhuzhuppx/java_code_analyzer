package com.projectassistant.simulation;

import java.util.*;

/**
 * 人格模型 — 基于大五人格（OCEAN）+ 价值观权重
 */
public class Personality {
    // 大五人格 0~10（方便加减）
    public double openness;             // 开放性：好奇≈10 保守≈0
    public double conscientiousness;    // 尽责性：自律≈10 随意≈0
    public double extraversion;         // 外向性：外向≈10 内向≈0
    public double agreeableness;        // 宜人性：友善≈10 竞争≈0
    public double neuroticism;          // 神经质：敏感≈10 稳定≈0

    /** 价值观权重（一个人在乎什么） */
    public Map<String, Double> values = new LinkedHashMap<>();

    /** 中文标签 */
    public String label;

    public Personality() {
        // 默认中庸
        openness = 5;
        conscientiousness = 5;
        extraversion = 5;
        agreeableness = 5;
        neuroticism = 5;
        label = "中庸型";
    }

    /** 用一句话描述人格 */
    public String describe() {
        List<String> tags = new ArrayList<>();
        if (openness >= 7) tags.add("好奇");
        else if (openness <= 3) tags.add("保守");

        if (conscientiousness >= 7) tags.add("自律");
        else if (conscientiousness <= 3) tags.add("随性");

        if (extraversion >= 7) tags.add("外向");
        else if (extraversion <= 3) tags.add("内向");

        if (agreeableness >= 7) tags.add("友善");
        else if (agreeableness <= 3) tags.add("强势");

        if (neuroticism >= 7) tags.add("敏感");
        else if (neuroticism <= 3) tags.add("稳定");

        return String.join("、", tags) + " (" + label + ")";
    }

    /** 获取某项价值观，默认0.5 */
    public double getValue(String key) {
        return values.getOrDefault(key, 0.5);
    }

    // ======= 预设人格 =======
    public static Personality CONSERVATIVE() {
        Personality p = new Personality();
        p.openness = 2; p.conscientiousness = 8;
        p.extraversion = 3; p.agreeableness = 6; p.neuroticism = 4;
        p.values.put("利益", 0.6); p.values.put("名声", 0.3);
        p.values.put("安全", 0.9); p.values.put("和谐", 0.7);
        p.label = "保守谨慎型";
        return p;
    }

    public static Personality AMBITIOUS() {
        Personality p = new Personality();
        p.openness = 7; p.conscientiousness = 7;
        p.extraversion = 8; p.agreeableness = 3; p.neuroticism = 3;
        p.values.put("利益", 0.9); p.values.put("名声", 0.8);
        p.values.put("安全", 0.3); p.values.put("权力", 0.9);
        p.label = "进取野心型";
        return p;
    }

    public static Personality RIGHTEOUS() {
        Personality p = new Personality();
        p.openness = 4; p.conscientiousness = 9;
        p.extraversion = 4; p.agreeableness = 2; p.neuroticism = 6;
        p.values.put("利益", 0.2); p.values.put("名声", 0.7);
        p.values.put("原则", 1.0); p.values.put("公平", 1.0);
        p.label = "正直固执型";
        return p;
    }

    public static Personality SOCIABLE() {
        Personality p = new Personality();
        p.openness = 7; p.conscientiousness = 4;
        p.extraversion = 9; p.agreeableness = 8; p.neuroticism = 3;
        p.values.put("利益", 0.4); p.values.put("名声", 0.5);
        p.values.put("关系", 0.9); p.values.put("快乐", 0.8);
        p.label = "社交活跃型";
        return p;
    }

    public static Personality ANALYTICAL() {
        Personality p = new Personality();
        p.openness = 6; p.conscientiousness = 9;
        p.extraversion = 2; p.agreeableness = 5; p.neuroticism = 5;
        p.values.put("利益", 0.6); p.values.put("安全", 0.7);
        p.values.put("理性", 1.0); p.values.put("效率", 0.8);
        p.label = "理性分析型";
        return p;
    }

    public static Personality fromPreset(String name) {
        return switch (name) {
            case "conservative" -> CONSERVATIVE();
            case "ambitious" -> AMBITIOUS();
            case "righteous" -> RIGHTEOUS();
            case "sociable" -> SOCIABLE();
            case "analytical" -> ANALYTICAL();
            default -> new Personality();
        };
    }
}
