package com.projectassistant.web;

import com.projectassistant.model.ProjectModel;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.knowledge.KnowledgeBaseGenerator;
import com.projectassistant.reporter.ReportGenerator;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import com.google.gson.*;

public class WebServer {

    private static final int DEFAULT_PORT = 8653;
    private static final Path REPORTS_DIR = Paths.get("reports").toAbsolutePath().normalize();
    private static final Path API_KEY_FILE = REPORTS_DIR.resolve(".apikey");
    private static volatile ScanTask currentTask;
    private static String cachedHtml;
    private static volatile String chatApiKey;
    private static final Gson gson = new Gson();

    public static void start(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length)
                port = Integer.parseInt(args[i + 1]);
        }
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            chatApiKey = envKey;
        } else if (Files.exists(API_KEY_FILE)) {
            String fileKey = Files.readString(API_KEY_FILE, StandardCharsets.UTF_8).trim();
            if (!fileKey.isEmpty()) chatApiKey = fileKey;
        }
        Files.createDirectories(REPORTS_DIR);
        cachedHtml = loadHtml();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebServer::handleRoot);
        server.createContext("/scan", WebServer::handleScan);
        server.createContext("/status", WebServer::handleStatus);
        server.createContext("/reports", WebServer::handleReports);
        server.createContext("/chat", WebServer::handleChat);
        server.createContext("/apikey", WebServer::handleApiKey);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("  ProjectAssistant Web started: http://localhost:" + port);
    }

    private static void handleRoot(HttpExchange ex) throws IOException {
        if (ex.getRequestURI().getPath().equals("/")) {
            sendResponse(ex, 200, "text/html; charset=utf-8", cachedHtml);
        } else {
            send404(ex);
        }
    }

    private static String loadHtml() throws IOException {
        String dp = Paths.get("").toAbsolutePath().normalize().toString();
        String tmpl;
        try (InputStream is = WebServer.class.getResourceAsStream("/webui.html")) {
            if (is == null) throw new IOException("webui.html not found in classpath");
            tmpl = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        return tmpl.replace("{{DEFAULT_PATH}}", dp);
    }

    private static void handleScan(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send405(ex); return; }
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody()))
            .lines().collect(Collectors.joining());
        Map<String, String> params = parseQuery(body);
        String projectPath = params.get("path");
        if (projectPath == null || projectPath.isEmpty()) {
            sendJson(ex, 400, gson.toJson(Map.of("error", "missing path")));
            return;
        }
        if (currentTask != null && currentTask.isRunning()) {
            sendJson(ex, 409, gson.toJson(Map.of("error", "scan already running")));
            return;
        }
        currentTask = new ScanTask(Paths.get(projectPath));
        Thread t = new Thread(currentTask::run);
        t.setDaemon(true);
        t.start();
        sendJson(ex, 200, gson.toJson(Map.of("status", "started")));
    }

    private static void handleStatus(HttpExchange ex) throws IOException {
        sendJson(ex, 200, currentTask == null
            ? gson.toJson(Map.of("status", "idle"))
            : currentTask.toJson());
    }

    private static void handleReports(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(q != null ? q : "");
        String filename = qm.get("file");
        if (filename == null || filename.isEmpty()) {
            List<String> names;
            try (Stream<Path> files = Files.list(REPORTS_DIR)) {
                names = files.filter(p -> p.toString().endsWith(".md"))
                    .map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
            }
            sendJson(ex, 200, gson.toJson(names));
            return;
        }
        Path fp = REPORTS_DIR.resolve(filename).normalize();
        if (!fp.startsWith(REPORTS_DIR) || !Files.exists(fp)) { send404(ex); return; }
        sendResponse(ex, 200, "text/markdown; charset=utf-8", Files.readString(fp));
    }

    private static void handleChat(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send405(ex); return; }
        if (chatApiKey == null || chatApiKey.isEmpty()) {
            sendJson(ex, 200, gson.toJson(Map.of(
                "error", "API Key \u672A\u914D\u7F6E\uFF0C\u8BF7\u70B9\u51FB\u53F3\u4E0A\u89D2\u914D\u7F6E"
            )));
            return;
        }
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody()))
            .lines().collect(Collectors.joining());
        Map<String, String> params = parseQuery(body);
        String question = params.get("question");
        if (question == null || question.isEmpty()) {
            sendJson(ex, 200, gson.toJson(Map.of("error", "empty question")));
            return;
        }

        // Load knowledge base for context
        String kbContext = "";
        Path kbPath = REPORTS_DIR.resolve("knowledge_base.md");
        if (Files.exists(kbPath)) {
            try { kbContext = Files.readString(kbPath); }
            catch (Exception ignored) {}
        }
        if (kbContext.length() > 30000)
            kbContext = kbContext.substring(0, 30000) + "\n... (truncated)";

        try {
            HttpClient client = HttpClient.newHttpClient();
            Map<String, Object> msg1 = new HashMap<>();
            msg1.put("role", "system");
            msg1.put("content", "You are a Java expert. Project context:\n" + kbContext);
            Map<String, Object> msg2 = new HashMap<>();
            msg2.put("role", "user");
            msg2.put("content", question);
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("model", "deepseek-v4-flash");
            reqBody.put("messages", Arrays.asList(msg1, msg2));
            reqBody.put("stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + chatApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(reqBody)))
                .build();

            HttpResponse<String> rp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (rp.statusCode() != 200) {
                String err = "API request failed: " + rp.statusCode();
                sendJson(ex, 200, gson.toJson(Map.of("error", err)));
                return;
            }
            JsonObject json = JsonParser.parseString(rp.body()).getAsJsonObject();
            String reply = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
            sendJson(ex, 200, gson.toJson(Map.of("reply", reply)));
        } catch (Exception e) {
            String m = e.getMessage() != null ? e.getMessage() : "unknown error";
            sendJson(ex, 200, gson.toJson(Map.of("error", m)));
        }
    }

    private static void handleApiKey(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            String body = new BufferedReader(new InputStreamReader(ex.getRequestBody()))
                .lines().collect(Collectors.joining());
            Map<String, String> params = parseQuery(body);
            String key = params.get("key");
            if (key != null && !key.isEmpty()) {
                chatApiKey = key;
                Files.writeString(API_KEY_FILE, key, StandardCharsets.UTF_8);
                sendJson(ex, 200, gson.toJson(Map.of("status", "ok")));
            } else {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing key")));
            }
        } else if ("GET".equals(ex.getRequestMethod())) {
            boolean hasKey = chatApiKey != null && !chatApiKey.isEmpty();
            String src = hasKey
                ? (System.getenv("DEEPSEEK_API_KEY") != null ? "env" : "manual")
                : "none";
            Map<String, Object> resp = new HashMap<>();
            resp.put("configured", hasKey);
            resp.put("source", src);
            sendJson(ex, 200, gson.toJson(resp));
        } else { send405(ex); }
    }

    // ========== Helpers ==========

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        sendResponse(ex, code, "application/json; charset=utf-8", json);
    }

    private static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void send404(HttpExchange ex) throws IOException {
        sendJson(ex, 404, gson.toJson(Map.of("error", "not found")));
    }

    private static void send405(HttpExchange ex) throws IOException {
        sendJson(ex, 405, gson.toJson(Map.of("error", "method not allowed")));
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            try {
                if (eq > 0)
                    map.put(URLDecoder.decode(pair.substring(0, eq), "UTF-8"),
                            URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
                else if (eq < 0)
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
            } catch (Exception ignored) {}
        }
        return map;
    }

    // ========== ScanTask ==========

    static class ScanTask {
        final Path projectPath;
        volatile boolean running;
        volatile String error;
        volatile long startTime;
        volatile int classes, methods, lines, apis, chains, findings, vulns, health;
        volatile String phase;

        ScanTask(Path projectPath) { this.projectPath = projectPath; }

        boolean isRunning() { return running; }

        void run() {
            running = true;
            startTime = System.currentTimeMillis();
            error = null;
            try {
                phase = "Scanning files...";
                ProjectModel model = new ProjectScanner(projectPath.toString()).scan();

                phase = "Analyzing...";
                ProjectAnalyzer analyzer = new ProjectAnalyzer(model);
                List<AnalysisResult> results = analyzer.analyze();

                // Collect stats
                classes = model.getStats().getTotalClasses();
                methods = model.getStats().getTotalMethods();
                lines = model.getStats().getTotalLines();
                apis = model.getApiEndpoints().size();
                chains = model.getCallChains() != null ? model.getCallChains().size() : 0;
                findings = results.size();
                vulns = model.getVulnFindings() != null ? model.getVulnFindings().size() : 0;
                health = calcHealth(model, results);

                phase = "Generating report...";
                String reportFile = REPORTS_DIR.resolve("report.md").toString();
                new ReportGenerator(model, results).saveReport(reportFile, "markdown");
                String kbFile = REPORTS_DIR.resolve("knowledge_base.md").toString();
                new KnowledgeBaseGenerator(model).save(kbFile);

                phase = "Done";
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": "
                    + (e.getMessage() != null ? e.getMessage() : "");
                phase = "Error";
            } finally {
                running = false;
            }
        }

        int elapsed() { return (int)((System.currentTimeMillis() - startTime) / 1000); }

        String toJson() {
            Map<String, Object> m = new HashMap<>();
            m.put("status", running ? "running" : (error != null ? "error" : "done"));
            m.put("elapsed", elapsed());
            if (running || error == null) {
                m.put("classes", classes);
                m.put("methods", methods);
                m.put("lines", lines);
                m.put("apis", apis);
                m.put("chains", chains);
                m.put("findings", findings);
                m.put("vulns", vulns);
                m.put("health", health);
            }
            if (running) m.put("phase", phase);
            if (error != null) m.put("message", error);
            return gson.toJson(m);
        }

        // Simplified health score calculation
        private int calcHealth(ProjectModel model, List<AnalysisResult> results) {
            int score = 80;
            if (results != null) {
                long crit = results.stream().filter(r -> r.getSeverity() >= 70).count();
                score -= crit * 5;
                long warn = results.stream().filter(r -> r.getSeverity() >= 50 && r.getSeverity() < 70).count();
                score -= warn * 2;
            }
            if (model.getVulnFindings() != null && !model.getVulnFindings().isEmpty())
                score -= 15;
            if (score < 0) score = 0;
            return score;
        }
    }
}