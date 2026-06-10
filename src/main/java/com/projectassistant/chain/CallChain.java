package com.projectassistant.chain;

import java.util.*;

/**
 * 调用链 — 从入口到末端的一整条调用路径
 */
public class CallChain {
    private String entryPoint;       // 入口：如 UserController.listUsers()
    private String entryRole;        // 入口角色：API / MQ / SCHEDULED
    private List<List<String>> callPaths = new ArrayList<>(); // 多条路径

    public String getEntryPoint() { return entryPoint; }
    public void setEntryPoint(String e) { this.entryPoint = e; }
    public String getEntryRole() { return entryRole; }
    public void setEntryRole(String r) { this.entryRole = r; }
    public List<List<String>> getCallPaths() { return callPaths; }

    /** 获取最大深度 */
    public int getMaxDepth() {
        return callPaths.stream().mapToInt(List::size).max().orElse(0);
    }

    /** 获取最长路径 */
    public List<String> getLongestPath() {
        return callPaths.stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(List.of());
    }

    @Override
    public String toString() {
        return entryRole + ": " + entryPoint + " (" + callPaths.size() + " paths, max depth " + getMaxDepth() + ")";
    }
}
