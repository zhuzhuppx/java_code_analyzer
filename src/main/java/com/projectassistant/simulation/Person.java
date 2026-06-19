package com.projectassistant.simulation;

import java.util.*;

/**
 * 模拟世界中的人
 */
public class Person {
    public String id;
    public String name;
    public Personality personality;
    public String description;

    // 状态
    public double energy = 10;
    public double mood = 0;
    public double wealth = 50;

    // 关系网
    public Map<String, Relationship> relationships = new LinkedHashMap<>();

    // 记忆
    public List<Memory> memories = new ArrayList<>();

    public Person() {}

    public Person(String name, Personality p) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.personality = p;
        this.description = p.describe();
    }

    /** 评估一个选项的效用值 — 核心决策函数 */
    public double evaluateUtility(Option option, Scenario scenario) {
        double utility = 0;

        // 利益：看重利益的人更在意收益
        utility += option.benefit * personality.getValue("利益");

        // 风险：尽责性高/看重安全的人回避风险
        double riskAversion = (personality.conscientiousness / 10.0
                             + personality.getValue("安全")) / 2;
        utility -= option.risk * riskAversion;

        // 新事物：开放的人喜欢新鲜
        utility += option.novelty * (personality.openness / 10.0);

        // 社会关系影响：宜人性高的人在乎关系和谐
        double socialWeight = (personality.agreeableness / 10.0
                             + personality.getValue("关系") + personality.getValue("和谐")) / 3;
        utility += option.socialHarmony * socialWeight;
        utility -= option.socialConflict * socialWeight;

        // 原则性
        if (personality.getValue("原则") > 0.5) {
            utility += option.principled * personality.getValue("原则");
        }

        // 公平性
        if (personality.getValue("公平") > 0.5) {
            utility += option.fairness * personality.getValue("公平");
        }

        // 权力
        if (personality.getValue("权力") > 0.5) {
            utility += option.power * personality.getValue("权力");
        }

        // 已有的关系影响
        for (Map.Entry<String, Relationship> rel : relationships.entrySet()) {
            if (option.affectsPerson != null && option.affectsPerson.equals(rel.getKey())) {
                utility += rel.getValue().attitude * 0.3;
            }
        }

        // 心情影响（心情好的时候更激进）
        utility += mood * 0.1;

        return utility;
    }

    /** 做出选择 */
    public Option choose(Scenario scenario) {
        if (scenario.options.isEmpty()) return null;
        return scenario.options.stream()
            .max(Comparator.comparingDouble(o -> evaluateUtility(o, scenario)))
            .orElse(scenario.options.get(0));
    }

    /** 在某事后更新记忆 */
    public void remember(String event, String detail, double emotionalImpact) {
        Memory m = new Memory();
        m.tick = System.currentTimeMillis();
        m.event = event;
        m.detail = detail;
        m.emotionalImpact = emotionalImpact;
        memories.add(m);
        // 记忆影响心情
        mood = Math.max(-1, Math.min(1, mood + emotionalImpact * 0.1));
        // 只保留最近20条记忆
        if (memories.size() > 20) memories.remove(0);
    }

    // ======= 关系 =======
    public static class Relationship {
        public double attitude;      // -1 ~ 1 好感度
        public double trust;         // 0 ~ 1 信任度
        public int interactions;

        public Relationship() { this(0, 0.5); }

        public Relationship(double attitude, double trust) {
            this.attitude = attitude;
            this.trust = trust;
            this.interactions = 1;
        }

        public void update(double attitudeChange, double trustChange) {
            attitude = Math.max(-1, Math.min(1, attitude + attitudeChange));
            trust = Math.max(0, Math.min(1, trust + trustChange));
            interactions++;
        }
    }

    // ======= 记忆 =======
    public static class Memory {
        public long tick;
        public String event;
        public String detail;
        public double emotionalImpact;
    }
}
