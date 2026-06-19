package com.projectassistant.simulation;

import java.util.*;
import java.util.stream.*;

/**
 * 社会推演引擎 — 核心
 *
 * 给定场景和人物，跑出每个人的决策 + 推演叙事
 */
public class SimulationEngine {

    /**
     * 跑一次完整推演
     */
    public SimulationResult simulate(Scenario scenario, List<Person> persons) {
        SimulationResult result = new SimulationResult();
        result.scenarioName = scenario.title;
        result.scenarioDesc = scenario.description;

        // 第一轮：各自独立决策
        Map<Person, Option> firstChoices = new LinkedHashMap<>();
        for (Person p : persons) {
            Option chosen = p.choose(scenario);
            firstChoices.put(p, chosen);
        }

        // 第二轮：知道别人选择后，是否改变主意
        Map<Person, Option> finalChoices = new LinkedHashMap<>(firstChoices);
        for (Person p : persons) {
            Option current = finalChoices.get(p);
            for (Person other : persons) {
                if (p == other) continue;
                Option otherChoice = firstChoices.get(other);
                if (otherChoice == null) continue;

                // 评估别人的选择对"我"的影响
                double socialInfluence = evaluateSocialInfluence(p, other, otherChoice);
                if (Math.abs(socialInfluence) > 0.5) {
                    // 重新评估
                    Option newChoice = p.choose(scenario);
                    finalChoices.put(p, newChoice);
                }
            }
        }

        // 记录决策和评分
        for (Person p : persons) {
            Option chosen = finalChoices.get(p);
            SimulationResult.PersonDecision pd = new SimulationResult.PersonDecision();
            pd.personName = p.name;
            pd.personalityLabel = p.personality.label;
            pd.chosenOption = chosen != null ? chosen.label : "（未选择）";
            pd.utilityScore = chosen != null ? p.evaluateUtility(chosen, scenario) : 0;

            for (Option opt : scenario.options) {
                SimulationResult.OptionScore os = new SimulationResult.OptionScore();
                os.optionLabel = opt.label;
                os.utility = p.evaluateUtility(opt, scenario);

                // 分解效用
                os.breakdown.put("利益", opt.benefit * p.personality.getValue("利益"));
                os.breakdown.put("风险回避", -opt.risk *
                    ((p.personality.conscientiousness / 10.0 + p.personality.getValue("安全")) / 2));
                os.breakdown.put("新鲜感", opt.novelty * (p.personality.openness / 10.0));
                os.breakdown.put("关系和谐", opt.socialHarmony *
                    (p.personality.agreeableness / 10.0 + p.personality.getValue("关系")) / 2);
                os.breakdown.put("原则", opt.principled * p.personality.getValue("原则"));
                os.breakdown.put("公平", opt.fairness * p.personality.getValue("公平"));
                os.breakdown.put("权力", opt.power * p.personality.getValue("权力"));

                pd.allScores.add(os);
            }
            result.decisions.add(pd);
        }

        // 生成推演叙事
        result.narrative = generateNarrative(persons, finalChoices, scenario);

        // 生成总结
        result.summary = generateSummary(persons, finalChoices, scenario);

        return result;
    }

    /** 社会影响评估：别人的选择对我有什么影响 */
    private double evaluateSocialInfluence(Person me, Person other, Option otherChoice) {
        // 我对他/她的态度
        Person.Relationship rel = me.relationships.get(other.name);
        if (rel == null) return 0;

        double influence = 0;
        // 信任的人会影响我
        influence += rel.trust * 0.3;
        // 好感度高的人会影响我
        influence += rel.attitude * 0.2;

        // 对方的选择如果指向我（affectsPerson）
        if (otherChoice.affectsPerson != null) {
            if (otherChoice.affectsPerson.equals(me.name)) {
                influence += otherChoice.socialHarmony * 0.5;
                influence -= otherChoice.socialConflict * 0.5;
            }
        }

        return influence;
    }

    /** 生成叙事文本 */
    private String generateNarrative(List<Person> persons,
                                     Map<Person, Option> choices,
                                     Scenario scenario) {
        StringBuilder n = new StringBuilder();

        n.append("📌 **场景**：").append(scenario.description).append("\n\n");

        for (Person p : persons) {
            Option chosen = choices.get(p);
            n.append("**").append(p.name).append("**（").append(p.personality.label).append("）");
            n.append(" → ").append(chosen != null ? chosen.label : "未选择");

            if (chosen != null) {
                n.append("：").append(chosen.description);
            }
            n.append("\n\n");

            // 对外向/内向的人做额外描述
            if (p.personality.extraversion >= 7) {
                n.append(p.name).append("性格外向，行动果断，迅速做出了判断。\n\n");
            } else if (p.personality.extraversion <= 3) {
                n.append(p.name).append("性格内向，思虑再三后才做出决定。\n\n");
            }
        }

        // 描述互动
        if (persons.size() >= 2) {
            Person p1 = persons.get(0);
            Person p2 = persons.get(1);
            Option c1 = choices.get(p1);
            Option c2 = choices.get(p2);

            // 根据性格和选择生成关系变化
            boolean sameChoice = c1 != null && c2 != null && c1.label.equals(c2.label);
            if (sameChoice) {
                n.append(p1.name).append("和").append(p2.name).append("做出了相同的选择，");
                if (p1.personality.agreeableness >= 6 || p2.personality.agreeableness >= 6) {
                    n.append("这让他们对彼此多了几分认同。");
                } else {
                    n.append("虽然选了同一条路，但各自的心思截然不同。");
                }
            } else {
                n.append(p1.name).append("和").append(p2.name).append("选择了不同的方向，");
                if (p1.personality.neuroticism >= 6 || p2.personality.neuroticism >= 6) {
                    n.append("这为日后的摩擦埋下了伏笔。");
                } else {
                    n.append("分歧在所难免。");
                }
            }
            n.append("\n\n");

            // 三人的情况
            if (persons.size() >= 3) {
                long uniqueChoices = choices.values().stream()
                    .filter(Objects::nonNull).map(o -> o.label).distinct().count();
                if (uniqueChoices == 3) {
                    n.append("三个人走向了三个不同的方向，这个团队的裂痕正在扩大。");
                } else if (uniqueChoices == 2) {
                    n.append("两派意见已经形成，中间地带的人将面临站队的压力。");
                } else {
                    n.append("整个团队出奇一致，但这究竟是共识还是盲从？");
                }
                n.append("\n\n");
            }
        }

        return n.toString();
    }

    /** 生成总结 */
    private String generateSummary(List<Person> persons,
                                   Map<Person, Option> choices,
                                   Scenario scenario) {
        StringBuilder s = new StringBuilder();

        // 找出最对立的两个人
        if (persons.size() >= 2) {
            Person p1 = persons.get(0);
            Person p2 = persons.get(1);
            Option c1 = choices.get(p1);
            Option c2 = choices.get(p2);

            boolean conflicting = c1 != null && c2 != null
                && ((c1.socialConflict > 5 && c2.socialHarmony > 5)
                    || (c1.benefit > 6 && c2.fairness > 6));

            if (conflicting) {
                s.append("🔴 **冲突预警**：").append(p1.name).append("和").append(p2.name);
                s.append("在本次决策中展现了根本性的价值观冲突。");
                s.append(p1.name).append("更看重").append(c1.benefit > 5 ? "利益" : "进取");
                s.append("，而").append(p2.name).append("更看重").append(c2.fairness > 5 ? "公平" : "稳定");
                s.append("。如果不加协调，未来可能爆发更大的矛盾。\n\n");
            }
        }

        // 人格总结
        s.append("🧠 **人格影响力**：本次推演中，");
        for (Person p : persons) {
            Option chosen = choices.get(p);
            if (chosen == null) continue;
            String drivingFactor = "";
            if (chosen.benefit > 6) drivingFactor = "利益驱动";
            else if (chosen.principled > 6) drivingFactor = "原则驱动";
            else if (chosen.socialHarmony > 6) drivingFactor = "关系驱动";
            else if (chosen.risk < 3) drivingFactor = "风险规避";
            else drivingFactor = "综合考量";

            s.append(p.name).append("的决策由**").append(drivingFactor).append("**主导");
            s.append("（").append(p.personality.label).append("）").append("；");
        }

        return s.toString();
    }

    // ======= 交互推演（两人面对面） =======

    /**
     * 模拟两人的直接互动
     */
    public InteractionResult interact(Person a, Person b, String context) {
        InteractionResult r = new InteractionResult();
        r.personA = a.name; r.personB = b.name;
        r.context = context;

        // 计算第一印象
        double impression = calculateFirstImpression(a, b);
        r.initialImpression = impression;

        // 对话效果
        double conversationFlow = calculateConversationFlow(a, b);
        r.conversationQuality = conversationFlow;

        // 关系变化
        double attitudeChange = (impression - 0.5) * 0.4 + (conversationFlow - 0.5) * 0.3;
        double trustChange = (conversationFlow - 0.5) * 0.3 + (a.personality.agreeableness / 20.0);

        // 更新关系
        Person.Relationship relA = a.relationships.computeIfAbsent(b.name, k -> new Person.Relationship());
        Person.Relationship relB = b.relationships.computeIfAbsent(a.name, k -> new Person.Relationship());
        relA.update(attitudeChange, trustChange);
        relB.update(attitudeChange * 0.7, trustChange * 0.7);

        // 更新记忆
        a.remember("与" + b.name + "互动", context, attitudeChange);
        b.remember("与" + a.name + "互动", context, -attitudeChange);

        r.attitudeChange = attitudeChange;
        r.trustChange = trustChange;
        r.aMoodChange = attitudeChange * 0.5;
        r.bMoodChange = -attitudeChange * 0.3;
        r.narrative = generateInteractionNarrative(a, b, impression, conversationFlow, attitudeChange);

        return r;
    }

    private double calculateFirstImpression(Person a, Person b) {
        double score = 0.5;
        // 相似性吸引
        score += 0.2 * (1 - Math.abs(a.personality.extraversion - b.personality.extraversion) / 10);
        score += 0.1 * (1 - Math.abs(a.personality.openness - b.personality.openness) / 10);
        // 宜人性的人给人好印象
        score += 0.15 * (b.personality.agreeableness / 10.0);
        // 已有关系
        Person.Relationship rel = a.relationships.get(b.name);
        if (rel != null) score += rel.attitude * 0.2;
        return Math.max(0, Math.min(1, score));
    }

    private double calculateConversationFlow(Person a, Person b) {
        double score = 0.5;
        // 两个外向的人聊得来
        if (a.personality.extraversion >= 6 && b.personality.extraversion >= 6)
            score += 0.2;
        // 一个外向一个内向 — 可能尴尬
        if (Math.abs(a.personality.extraversion - b.personality.extraversion) > 5)
            score -= 0.1;
        // 宜人性高的人让对话更顺畅
        score += 0.1 * (a.personality.agreeableness / 10.0 + b.personality.agreeableness / 10.0) / 2;
        // 神经质高的人容易把天聊死
        score -= 0.1 * (a.personality.neuroticism / 10.0 + b.personality.neuroticism / 10.0) / 2;
        return Math.max(0, Math.min(1, score));
    }

    private String generateInteractionNarrative(Person a, Person b, double impression,
                                                 double conversation, double attitudeChange) {
        StringBuilder n = new StringBuilder();
        n.append("**").append(a.name).append("** (").append(a.personality.label).append(")");
        n.append(" 与 **").append(b.name).append("** (").append(b.personality.label).append(") 相遇了。\n\n");

        n.append("**第一印象**：");
        if (impression >= 0.7) n.append("彼此印象很好，感觉投缘。");
        else if (impression >= 0.5) n.append("还算不错，没有特别的感觉。");
        else n.append("有些微妙，气氛略显尴尬。");
        n.append("\n\n");

        n.append("**对话质量**：");
        if (conversation >= 0.7) n.append("聊得很投机，话题源源不断。");
        else if (conversation >= 0.5) n.append("客套地聊了几句，不温不火。");
        else n.append("话不投机半句多，很快就沉默了下来。");
        n.append("\n\n");

        n.append("**关系变化**：");
        if (attitudeChange >= 0.2) n.append("关系升温，此后对彼此多了几分好感。");
        else if (attitudeChange >= 0) n.append("没什么太大变化，各自继续各自的路。");
        else if (attitudeChange >= -0.2) n.append("有些小摩擦，但还不至于影响大局。");
        else n.append("这次相遇让两人对彼此产生了负面印象。");

        return n.toString();
    }

    // ======= 结果 =======
    public static class InteractionResult {
        public String personA, personB;
        public String context;
        public double initialImpression;
        public double conversationQuality;
        public double attitudeChange;
        public double trustChange;
        public double aMoodChange, bMoodChange;
        public String narrative;

        public String toMarkdown() {
            return "# 🤝 人际互动：" + personA + " × " + personB + "\n\n"
                + "**情境**：" + context + "\n\n"
                + "## 📖 叙事\n\n" + narrative + "\n\n"
                + "## 📊 量化结果\n\n"
                + "| 指标 | 数值 |\n|---|---|\n"
                + "| 第一印象 | " + String.format("%.2f", initialImpression) + " |\n"
                + "| 对话质量 | " + String.format("%.2f", conversationQuality) + " |\n"
                + "| 好感变化 | " + String.format("%+.2f", attitudeChange) + " |\n"
                + "| 信任变化 | " + String.format("%+.2f", trustChange) + " |\n"
                + "| " + personA + "心情变化 | " + String.format("%+.2f", aMoodChange) + " |\n"
                + "| " + personB + "心情变化 | " + String.format("%+.2f", bMoodChange) + " |\n";
        }
    }
}
