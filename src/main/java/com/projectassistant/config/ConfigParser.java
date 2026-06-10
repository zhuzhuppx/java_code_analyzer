package com.projectassistant.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * 项目配置文件解析器
 * 读取 application.yml / application.properties / bootstrap.yml 等配置
 * 提取：数据库连接、中间件、端口、日志级别等关键信息
 */
public class ConfigParser {

    private final Path rootPath;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final List<ConfigSource> sources = new ArrayList<>();

    // 关键配置键
    private static final Set<String> KEY_KEYS = new HashSet<>(Arrays.asList(
        "server.port", "server.servlet.context-path",
        "spring.datasource.url", "spring.datasource.username",
        "spring.datasource.driver-class-name",
        "spring.redis.host", "spring.redis.port", "spring.redis.password",
        "spring.rabbitmq.host", "spring.rabbitmq.port",
        "spring.kafka.bootstrap-servers",
        "spring.elasticsearch.uris",
        "spring.mongodb.uri",
        "spring.cloud.nacos.server-addr",
        "spring.application.name",
        "spring.profiles.active",
        "spring.config.import",
        "logging.level",
        "mybatis.mapper-locations", "mybatis-plus.mapper-locations",
        "pagehelper", "feign", "dubbo", "grpc"
    ));

    /** 配置来源记录 */
    public static class ConfigSource {
        private final String filePath;
        private final String type;
        private final int keyCount;

        public ConfigSource(String filePath, String type, int keyCount) {
            this.filePath = filePath;
            this.type = type;
            this.keyCount = keyCount;
        }

        public String getFilePath() { return filePath; }
        public String getType() { return type; }
        public int getKeyCount() { return keyCount; }
    }

    public ConfigParser(String rootPath) {
        this.rootPath = Paths.get(rootPath);
    }

    /** 执行解析 */
    public Map<String, String> parse() {
        properties.clear();
        sources.clear();
        findAndParseConfigFiles();
        System.out.println("  [Config] 发现 " + sources.size() + " 个配置文件, "
                + properties.size() + " 个配置项");
        return properties;
    }

    /** 递归查找配置文件 */
    private void findAndParseConfigFiles() {
        if (!Files.exists(rootPath)) return;

        List<String> patterns = Arrays.asList(
            "application.yml", "application.yaml",
            "application.properties",
            "bootstrap.yml", "bootstrap.yaml",
            "bootstrap.properties",
            "application-dev.yml", "application-prod.yml",
            "application-test.yml",
            "application-dev.properties", "application-prod.properties",
            "application-local.yml", "application-local.properties",
            "logback-spring.xml", "logback.xml"
        );

        try {
            Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return patterns.stream().anyMatch(name::equals);
                })
                .filter(p -> !p.toString().contains("target" + File.separator))
                .filter(p -> !p.toString().contains(".mvn" + File.separator))
                .forEach(this::parseFile);
        } catch (IOException e) {
            // skip
        }
    }

    /** 根据文件类型解析 */
    private void parseFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        try {
            String content = Files.readString(file);
            if (name.endsWith(".properties")) {
                parseProperties(content, file);
            } else if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                parseYaml(content, file);
            } else if (name.endsWith(".xml")) {
                parseLogbackXml(content, file);
            }
        } catch (Exception e) {
            // skip bad files
        }
    }

    /** 解析 .properties 文件 */
    private void parseProperties(String content, Path file) {
        int count = 0;
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\""))
                value = value.substring(1, value.length() - 1);
            properties.put(key, value);
            count++;
        }
        sources.add(new ConfigSource(file.toString(), "properties", count));
    }

    /** 简易 YAML 解析（扁平化） */
    private void parseYaml(String content, Path file) {
        int count = 0;
        String[] lines = content.split("\n");
        List<String> path = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            int indent = 0;
            for (char c : line.toCharArray()) { if (c == ' ') indent++; else break; }
            int depth = indent / 2;

            while (path.size() > depth) path.remove(path.size() - 1);

            String trimmed = line.trim();
            if (trimmed.endsWith(":")) {
                String key = trimmed.substring(0, trimmed.length() - 1).trim();
                if (depth < path.size()) path.set(depth, key);
                else path.add(key);
            } else if (trimmed.contains(":")) {
                int colon = trimmed.indexOf(':');
                String key = trimmed.substring(0, colon).trim();
                String value = trimmed.substring(colon + 1).trim();
                if (value.isEmpty() || value.equals("|") || value.equals(">")) continue;
                if (value.startsWith("\"") && value.endsWith("\""))
                    value = value.substring(1, value.length() - 1);
                if (value.startsWith("'") && value.endsWith("'"))
                    value = value.substring(1, value.length() - 1);

                StringBuilder fullKey = new StringBuilder();
                for (int i = 0; i < depth && i < path.size(); i++) {
                    if (fullKey.length() > 0) fullKey.append('.');
                    fullKey.append(path.get(i));
                }
                if (fullKey.length() > 0) fullKey.append('.');
                fullKey.append(key);
                String finalKey = fullKey.toString();

                if (isKeyConfig(finalKey)) {
                    properties.put(finalKey, value);
                    count++;
                }
            }
        }
        sources.add(new ConfigSource(file.toString(), "yaml", count));
    }

    /** 解析 logback 中的日志级别 */
    private void parseLogbackXml(String content, Path file) {
        int count = 0;
        Pattern levelPattern = Pattern.compile(
            "<(?:logger|root)\\s+[^>]*level\\s*=\\s*\"([^\"]+)\"");
        Matcher m = levelPattern.matcher(content);
        while (m.find()) {
            properties.put("logging.level." + (count++), m.group(1));
        }
        if (count > 0) sources.add(new ConfigSource(file.toString(), "xml", count));
    }

    private boolean isKeyConfig(String key) {
        if (KEY_KEYS.contains(key)) return true;
        for (String kk : KEY_KEYS) {
            if (key.startsWith(kk)) return true;
        }
        return key.contains("datasource") || key.contains("redis")
            || key.contains("rabbit") || key.contains("kafka")
            || key.contains("nacos") || key.contains("eureka")
            || key.contains("feign") || key.contains("dubbo")
            || key.contains("server.") || key.contains("spring.");
    }

    /** 获取关键配置摘要 */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (properties.containsKey("spring.application.name"))
            sb.append("应用名: ").append(properties.get("spring.application.name")).append("\n");
        if (properties.containsKey("server.port"))
            sb.append("端口: ").append(properties.get("server.port")).append("\n");
        if (properties.containsKey("spring.datasource.url"))
            sb.append("数据库: ").append(maskPassword(properties.get("spring.datasource.url"))).append("\n");
        if (properties.containsKey("spring.redis.host"))
            sb.append("Redis: ").append(properties.get("spring.redis.host"))
              .append(":").append(properties.getOrDefault("spring.redis.port", "6379")).append("\n");
        if (properties.containsKey("spring.profiles.active"))
            sb.append("环境: ").append(properties.get("spring.profiles.active")).append("\n");
        if (properties.containsKey("spring.cloud.nacos.server-addr"))
            sb.append("Nacos: ").append(properties.get("spring.cloud.nacos.server-addr")).append("\n");
        return sb.toString();
    }

    private String maskPassword(String url) {
        if (url == null) return null;
        return url.replaceAll("password=[^&]+", "password=****");
    }

    public List<ConfigSource> getSources() { return sources; }
    public Map<String, String> getProperties() { return properties; }
}
