package com.projectassistant.simulation;

import java.util.*;

/**
 * 场景 — 一个需要决策的情境
 */
public class Scenario {
    public String id;
    public String title;            // "新领导空降"
    public String description;      // 背景描述
    public List<Option> options = new ArrayList<>();
    public String context;          // 场景上下文（影响所有人的因素）

    public Scenario() {}

    public Scenario(String title, String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.title = title;
        this.description = description;
    }

    /** 添加一个选项 */
    public Scenario addOption(Option option) {
        options.add(option);
        return this;
    }

    // ======= 预设场景 =======

    /** 新领导空降 — 经典场景 */
    public static Scenario NEW_BOSS() {
        Scenario s = new Scenario("新领导空降", "部门空降了一位新领导，风格不明。作为中层管理者，你会怎么做？");

        Option a = new Option("主动靠拢", "主动接近新领导，展示能力，争取信任")
            .with(7, 4).power(6).novelty(3)
            .socialHarmony(2).socialConflict(3).principled(3);
        Option b = new Option("观望", "先不急着表态，观察新领导的行事风格再做打算")
            .with(3, 1).power(1).novelty(2)
            .socialHarmony(5).socialConflict(1).principled(6).fairness(5);
        Option c = new Option("对立", "对新领导的到来表示不满，联合其他同事抵抗")
            .with(2, 8).power(3).novelty(5)
            .socialHarmony(1).socialConflict(9).principled(5).fairness(4);

        return s.addOption(a).addOption(b).addOption(c);
    }

    /** 利益冲突 — 项目奖金分配 */
    public static Scenario BONUS_CONFLICT() {
        Scenario s = new Scenario("奖金分配", "团队完成了一个大项目，有一笔奖金需要你分配。你会怎么分？");

        Option a = new Option("按贡献分", "严格按每个人贡献比例分配，公平但可能有人不满")
            .with(4, 3).fairness(9).principled(8)
            .socialHarmony(3).socialConflict(4);
        Option b = new Option("平均分", "大家平分，维护团队和谐")
            .with(3, 1).fairness(4).principled(3)
            .socialHarmony(8).socialConflict(1);
        Option c = new Option("多给自己", "利用分配权给自己多分一些")
            .with(9, 7).power(3)
            .socialHarmony(0).socialConflict(8).principled(1).fairness(0);

        return s.addOption(a).addOption(b).addOption(c);
    }

    /** 信任考验 — 同事泄露机密 */
    public static Scenario TRUST_TEST() {
        Scenario s = new Scenario("信任考验", "你发现关系要好的同事泄露了公司机密，但他请求你帮他保密。你怎么选？");

        Option a = new Option("举报", "向公司举报，维护公司利益")
            .with(3, 6).principled(9).fairness(8)
            .socialHarmony(1).socialConflict(7).affects("同事");
        Option b = new Option("沉默", "假装不知道，帮同事保密")
            .with(5, 7).principled(1).fairness(2)
            .socialHarmony(6).socialConflict(3).affects("同事");
        Option c = new Option("劝说自首", "劝同事自己主动承认，你陪他一起面对")
            .with(4, 4).principled(6).fairness(5)
            .socialHarmony(4).socialConflict(3).novelty(4).affects("同事");

        return s.addOption(a).addOption(b).addOption(c);
    }

    public static Scenario getPreset(String name) {
        return switch (name) {
            case "new_boss" -> NEW_BOSS();
            case "bonus" -> BONUS_CONFLICT();
            case "trust" -> TRUST_TEST();
            default -> NEW_BOSS();
        };
    }
}
