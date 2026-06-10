package com.projectassistant.chain;

import java.util.*;
import java.util.stream.*;

/**
 * 调用链分析器
 * 追踪 Controller → Service → DAO 全链路
 */
public class CallChainAnalyzer {

    private final List<CallChain> chains = new ArrayList<>();
    private final Map<String, String> classRoleMap; // 类名 → 角色

    public CallChainAnalyzer(Map<String, String> classRoleMap) {
        this.classRoleMap = classRoleMap;
    }

    /**
     * 从调用关系图中提取完整调用链
     * @param callGraph 调用者 -> 被调用者列表
     * @param endpoints API 端点列表 (格式: "Controller.method")
     */
    public void analyze(Map<String, List<String>> callGraph,
                        List<String> endpoints) {
        System.out.println("  [Chain] 追踪调用链...");

        for (String entry : endpoints) {
            CallChain chain = new CallChain();
            chain.setEntryPoint(entry);
            chain.setEntryRole("API");

            Set<String> visited = new HashSet<>();
            List<String> path = new ArrayList<>();
            path.add(entry);

            traceChain(entry, callGraph, visited, path, chain);
            chains.add(chain);
        }

        // 去重合并
        System.out.println("  [Chain] 发现 " + chains.size() + " 条调用链");
    }

    private void traceChain(String current, Map<String, List<String>> callGraph,
                            Set<String> visited, List<String> path,
                            CallChain chain) {
        if (!callGraph.containsKey(current) || visited.contains(current)) {
            // 记录这条路径
            chain.getCallPaths().add(new ArrayList<>(path));
            return;
        }

        visited.add(current);
        List<String> callees = callGraph.get(current);

        if (callees == null || callees.isEmpty()) {
            chain.getCallPaths().add(new ArrayList<>(path));
            visited.remove(current);
            return;
        }

        boolean hasCallee = false;
        for (String callee : callees) {
            String cleanCallee = callee.replaceAll("\\(.*\\)", "");
            if (visited.contains(cleanCallee)) continue;

            path.add(cleanCallee);
            traceChain(cleanCallee, callGraph, visited, path, chain);
            path.remove(path.size() - 1);
            hasCallee = true;
        }

        if (!hasCallee) {
            chain.getCallPaths().add(new ArrayList<>(path));
        }

        visited.remove(current);
    }

    /**
     * 获取关键链路摘要（展示给开发者的最有用链路）
     */
    public List<String> getCriticalChains() {
        List<String> result = new ArrayList<>();
        for (CallChain chain : chains) {
            for (List<String> path : chain.getCallPaths()) {
                if (path.size() >= 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < path.size(); i++) {
                        if (i > 0) sb.append("  ->  ");
                        String step = path.get(i);
                        String role = inferRole(step);
                        sb.append("[").append(role).append("] ").append(step);
                    }
                    result.add(sb.toString());
                }
            }
        }
        return result;
    }

    private String inferRole(String className) {
        if (className == null) return "?";
        String lower = className.toLowerCase();
        if (lower.contains("controller") || lower.contains("endpoint")) return "API";
        if (lower.contains("service") || lower.contains("business")) return "SERVICE";
        if (lower.contains("repository") || lower.contains("dao") ||
            lower.contains("mapper")) return "DAO";
        if (lower.contains("entity") || lower.contains("model") ||
            lower.contains("domain")) return "ENTITY";
        if (lower.contains("config") || lower.contains("properties")) return "CONFIG";
        if (lower.contains("util") || lower.contains("helper") ||
            lower.contains("tool")) return "UTIL";
        if (lower.contains("client") || lower.contains("remote") ||
            lower.contains("feign") || lower.contains("rpc")) return "RPC";
        if (lower.contains("mq") || lower.contains("producer") ||
            lower.contains("consumer") || lower.contains("queue")) return "MQ";
        if (lower.contains("cache") || lower.contains("redis")) return "CACHE";
        if (lower.contains("filter") || lower.contains("interceptor") ||
            lower.contains("aspect")) return "AOP";
        return "OTHER";
    }

    public List<CallChain> getChains() { return chains; }
}
