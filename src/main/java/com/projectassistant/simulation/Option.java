package com.projectassistant.simulation;

/**
 * 一个可选项
 */
public class Option {
    public String id;
    public String label;        // 选项名称
    public String description;  // 选项描述

    // 效用维度（数值越大越偏向该方向）
    public double benefit;          // 利益收益
    public double risk;             // 风险程度
    public double novelty;          // 新鲜感
    public double socialHarmony;    // 对社会和谐的贡献
    public double socialConflict;   // 引发的社会冲突
    public double principled;       // 符合原则的程度
    public double fairness;         // 公平程度
    public double power;            // 获得权力的程度

    /** 此选项影响的人（可选） */
    public String affectsPerson;

    public Option() {}

    public Option(String label, String desc) {
        this.id = label.toLowerCase().replaceAll("\\s+", "_");
        this.label = label;
        this.description = desc;
    }

    /** 链式设置收益 */
    public Option with(double benefit, double risk) {
        this.benefit = benefit; this.risk = risk;
        return this;
    }

    public Option novelty(double v) { this.novelty = v; return this; }
    public Option socialHarmony(double v) { this.socialHarmony = v; return this; }
    public Option socialConflict(double v) { this.socialConflict = v; return this; }
    public Option principled(double v) { this.principled = v; return this; }
    public Option fairness(double v) { this.fairness = v; return this; }
    public Option power(double v) { this.power = v; return this; }
    public Option affects(String person) { this.affectsPerson = person; return this; }
}
