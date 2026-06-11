package com.projectassistant.web;

import com.projectassistant.db.DatabaseManager;
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
    private static volatile ScanTask currentTask;
    private static String cachedHtml;
    private static volatile String chatApiKey;
    private static final Gson gson = new Gson();

    public static void start(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length)
                port = Integer.parseInt(args[i + 1]);
        }
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            chatApiKey = envKey;
        }
        Files.createDirectories(REPORTS_DIR);
        DatabaseManager.init();
        cachedHtml = loadHtml();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebServer::handleRoot);
        server.createContext("/scan", WebServer::handleScan);
        server.createContext("/status", WebServer::handleStatus);
        server.createContext("/history", WebServer::handleHistory);
        server.createContext("/project", WebServer::handleProject);
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

    private static void handleHistory(HttpExchange ex) throws IOException {
        List<Map<String, Object>> list = DatabaseManager.listProjects();
        sendJson(ex, 200, gson.toJson(list));
    }

    private static void handleProject(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(q != null ? q : "");
        String idStr = qm.get("id");
        if (idStr == null) { sendJson(ex, 400, gson.toJson(Map.of("error", "missing id"))); return; }
        try {
            long id = Long.parseLong(idStr);
            Map<String, Object> data = DatabaseManager.getProject(id);
            sendJson(ex, 200, gson.toJson(data));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(Map.of("error", "invalid id")));
        }
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
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody()))
            .lines().collect(Collectors.joining());
        Map<String, String> params = parseQuery(body);
        String question = params.get("question");
        String apiKey = params.get("apiKey");
        String pidStr = params.get("projectId");
        if (apiKey == null || apiKey.isEmpty()) apiKey = chatApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            sendJson(ex, 200, gson.toJson(Map.of(
                "error", "API Key \u672A\u914D\u7F6E\uFF0C\u8BF7\u70B9\u51FB\u53F3\u4E0A\u89D2\u914D\u7F6E"
            )));
            return;
        }
        long projectId = -1;
        if (pidStr != null) { try { projectId = Long.parseLong(pidStr); } catch (NumberFormatException ignored) {} }
        if (projectId > 0) DatabaseManager.saveChatMessage(projectId, "user", question);
        String kbContext = "";
        if (projectId > 0) {
            Map<String, Object> pdata = DatabaseManager.getProject(projectId);
            if (pdata.containsKey("kb")) kbContext = (String) pdata.get("kb");
        }
        if (kbContext.isEmpty()) {
            Path kbPath = REPORTS_DIR.resolve("knowledge_base.md");
            if (Files.exists(kbPath)) {
                try { kbContext = Files.readString(kbPath); } catch (Exception ignored) {}
            }
        }
        if (kbContext.length() > 30000)
            kbContext = kbContext.substring(0, 30000) + "\n... (truncated)";
        ex.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, 0);
        OutputStream os = ex.getResponseBody();
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
            reqBody.put("stream", true);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(reqBody)))
                .build();
            HttpResponse<InputStream> rp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (rp.statusCode() != 200) {
                String errBody = new String(rp.body().readAllBytes(), StandardCharsets.UTF_8);
                sendSseEvent(os, "error", "API error: " + rp.statusCode() + " - " + errBody);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(rp.body(), StandardCharsets.UTF_8));
            StringBuilder fullReply = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) break;
                try {
                    JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                    if (delta == null || delta.get("content") == null) continue;
                    String chunk = delta.get("content").getAsString();
                    fullReply.append(chunk);
                    sendSseEvent(os, "content", chunk);
                } catch (Exception ignored) {}
            }
            if (projectId > 0 && fullReply.length() > 0) {
                DatabaseManager.saveChatMessage(projectId, "assistant", fullReply.toString());
            }
            sendSseEvent(os, "done", "true");
        } catch (Exception e) {
            String m = e.getMessage() != null ? e.getMessage() : "unknown error";
            try { sendSseEvent(os, "error", m); } catch (Exception ignored) {}
        } finally {
            try { os.close(); } catch (Exception ignored) {}
        }
    }
    private static void sendSseEvent(OutputStream os, String type, String data) throws IOException {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("type", type);
        ev.put("data", data);
        os.write(("data: " + gson.toJson(ev) + "\n\n").getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private static void handleApiKey(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            String body = new BufferedReader(new InputStreamReader(ex.getRequestBody()))
                .lines().collect(Collectors.joining());
            Map<String, String> params = parseQuery(body);
            String key = params.get("key");
            if (key != null && !key.isEmpty()) {
                chatApiKey = key;
                sendJson(ex, 200, gson.toJson(Map.of("status", "ok")));
            } else {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing key")));
            }
        } else if ("GET".equals(ex.getRequestMethod())) {
            boolean hasKey = chatApiKey != null && !chatApiKey.isEmpty();
            sendJson(ex, 200, gson.toJson(Map.of(
                "configured", hasKey,
                "source", hasKey ? "env" : "none"
            )));
        } else {
            send405(ex);
        }
    }

    // ========== Utility ==========

    private static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.getResponseHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        sendResponse(ex, code, "application/json; charset=utf-8", json);
    }

    private static void send404(HttpExchange ex) throws IOException {
        sendResponse(ex, 404, "text/plain", "404 Not Found");
    }

    private static void send405(HttpExchange ex) throws IOException {
        sendResponse(ex, 405, "text/plain", "405 Method Not Allowed");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
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
        volatile long projectId = -1;

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
                String reportMd = new ReportGenerator(model, results).generateMarkdown();
                String kbFile = REPORTS_DIR.resolve("knowledge_base.md").toString();
                new KnowledgeBaseGenerator(model).save(kbFile);
                String kbContent = Files.readString(Paths.get(kbFile));

                // Save to DB
                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("classes", classes); stats.put("methods", methods);
                stats.put("lines", lines); stats.put("apis", apis);
                stats.put("chains", chains); stats.put("findings", findings);
                stats.put("vulns", vulns); stats.put("health", health);
                String statsJson = gson.toJson(stats);
                projectId = DatabaseManager.saveProject(projectPath.toString(), statsJson);
                if (projectId > 0) {
                    DatabaseManager.saveKnowledgeBase(projectId, kbContent);
                    DatabaseManager.saveReport(projectId, "report.md", reportMd);
                    // Also save other reports
                    Path knowledgeBasePath = Paths.get(kbFile);
                    if (Files.exists(knowledgeBasePath)) {
                        try {
                            DatabaseManager.saveReport(projectId, "knowledge_base.md",
                                    Files.readString(knowledgeBasePath));
                        } catch (Exception ignored) {}
                    }
                }

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
            if (projectId > 0) m.put("projectId", projectId);
            if (running) m.put("phase", phase);
            if (error != null) m.put("message", error);
            return gson.toJson(m);
        }

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
