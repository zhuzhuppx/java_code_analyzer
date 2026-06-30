package com.projectassistant.web;

import com.projectassistant.db.DatabaseManager;
import com.projectassistant.model.ProjectModel;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.spring.SpringScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.analyzer.BusinessLogicAnalyzer;
import com.projectassistant.knowledge.KnowledgeBaseGenerator;
import com.projectassistant.query.QueryAdvisor;
import com.projectassistant.relationship.TableRelation;
import com.projectassistant.reporter.ReportGenerator;
import com.projectassistant.sql.LiveDatabaseReader;
import com.projectassistant.sql.SqlParser;
import com.projectassistant.sql.SchemaParser;
import com.projectassistant.sql.TableInfo;
import com.sun.net.httpserver.*;
import java.io.File;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
        server.createContext("/skill", WebServer::handleSkill);
        server.createContext("/dbconnect", WebServer::handleDbConnect);
        server.createContext("/business", WebServer::handleBusiness);
        server.createContext("/api-flow", WebServer::handleApiFlow);
        server.createContext("/nl-query", WebServer::handleNaturalQuery);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("  Java老狗 Web started: http://localhost:" + port);
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
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params;
        String projectPath;
        String dbUrl = null, dbUser = null, dbPass = null;
        // 支持 JSON 和 form-urlencoded 两种格式
        if (body.startsWith("{")) {
            Map<String, Object> json = gson.fromJson(body, Map.class);
            projectPath = (String) json.get("path");
            if (json.containsKey("dbUrl")) dbUrl = (String) json.get("dbUrl");
            if (json.containsKey("dbUser")) dbUser = (String) json.get("dbUser");
            if (json.containsKey("dbPass")) dbPass = (String) json.get("dbPass");
        } else {
            params = parseQuery(body);
            projectPath = params.get("path");
            dbUrl = params.get("dbUrl");
            dbUser = params.get("dbUser");
            dbPass = params.get("dbPass");
        }
        if (projectPath == null || projectPath.isEmpty()) {
            sendJson(ex, 400, gson.toJson(Map.of("error", "missing path")));
            return;
        }
        if (currentTask != null && currentTask.isRunning()) {
            sendJson(ex, 409, gson.toJson(Map.of("error", "scan already running")));
            return;
        }
        currentTask = new ScanTask(Paths.get(projectPath), dbUrl, dbUser, dbPass);
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
        if (kbContext.length() > 90000)
            kbContext = kbContext.substring(0, 90000) + "\n... (truncated)";
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

    private static void handleSkill(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(q != null ? q : "");
        String idStr = qm.get("id");
        if (idStr == null) { sendJson(ex, 400, gson.toJson(Map.of("error", "missing id"))); return; }
        try {
            long id = Long.parseLong(idStr);
            Map<String, Object> data = DatabaseManager.getProject(id);
            String path = data != null && data.containsKey("path") ? (String) data.get("path") : "unknown";
            String projectName = Paths.get(path).getFileName().toString();
            if (projectName == null || projectName.isEmpty()) projectName = "project";

            // Look for pre-generated skill sub-documents
            Path skillDir = REPORTS_DIR.resolve("skills").resolve(projectName.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase());
            Path skillIndex = skillDir.resolve("SKILL.md");

            if (!Files.exists(skillIndex)) {
                // Fallback: generate from KB
                String kb = data != null && data.containsKey("kb") ? (String) data.get("kb") : null;
                if (kb == null || kb.isEmpty()) {
                    sendJson(ex, 404, gson.toJson(Map.of("error", "no skill found for this project")));
                    return;
                }
                String body = kb.replaceFirst("^# 项目知识库\\s*", "");
                body = body.replaceFirst("^> 由 Java老狗 自动生成.*?(\\n|$)", "");
                String skill = "---\n"
                    + "name: " + projectName + "\n"
                    + "description: \"" + projectName + " 项目知识库\"\n"
                    + "metadata:\n"
                    + "  copaw:\n"
                    + "    emoji: \"\\uD83D\\uDCE6\"\n"
                    + "    requires: {}\n"
                    + "---\n\n"
                    + "当用户询问本项目相关的任何问题时，优先使用以下信息回答。\n\n"
                    + body;
                String zipName = projectName + "_" + LocalDate.now().toString() + ".zip";
                byte[] skillBytes = skill.getBytes(StandardCharsets.UTF_8);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                    zos.putNextEntry(new ZipEntry("SKILL.md"));
                    zos.write(skillBytes);
                    zos.closeEntry();
                }
                byte[] zipBytes = baos.toByteArray();
                ex.getResponseHeaders().add("Content-Type", "application/zip");
                ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + zipName + "\"");
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.sendResponseHeaders(200, zipBytes.length);
                ex.getResponseBody().write(zipBytes);
                ex.getResponseBody().close();
                return;
            }

            // Zip all .md files in the skill directory
            String zipName = projectName + "_" + LocalDate.now().toString() + ".zip";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos);
                 DirectoryStream<Path> stream = Files.newDirectoryStream(skillDir, "*.md")) {
                for (Path mdFile : stream) {
                    zos.putNextEntry(new ZipEntry(mdFile.getFileName().toString()));
                    zos.write(Files.readAllBytes(mdFile));
                    zos.closeEntry();
                }
            }
            byte[] zipBytes = baos.toByteArray();
            ex.getResponseHeaders().add("Content-Type", "application/zip");
            ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + zipName + "\"");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, zipBytes.length);
            ex.getResponseBody().write(zipBytes);
            ex.getResponseBody().close();
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(Map.of("error", "invalid id")));
        }
    }

    /** 将实时数据库 Schema 转为 TableInfo 列表 */
    private static List<TableInfo> convertDbSchemaToTableInfo(LiveDatabaseReader.DatabaseSchema schema) {
        List<TableInfo> tables = new ArrayList<>();
        for (LiveDatabaseReader.TableSchema ts : schema.tables) {
            TableInfo ti = new TableInfo();
            ti.setTableName(ts.name);
            ti.setComment(ts.comment != null ? ts.comment : "");
            for (LiveDatabaseReader.ColumnSchema cs : ts.columns) {
                TableInfo.Column col = new TableInfo.Column();
                col.setFieldName(cs.name);          // 没有 Java 字段名，用列名代替
                col.setColumnName(cs.name);
                col.setPrimaryKey(cs.primaryKey);
                col.setNullable(cs.nullable);
                col.setAutoIncrement(cs.autoIncrement);
                col.setLength(cs.size);
                col.setComment(cs.comment != null ? cs.comment : "");
                col.setJavaType(mapJdbcType(cs.jdbcType));  // JDBC→Java 类型映射
                col.setSqlType(mapJdbcType(cs.jdbcType));
                ti.getColumns().add(col);
            }
            tables.add(ti);
        }
        return tables;
    }

    /** JDBC 类型名 → 常见 SQL / Java 类型名 */
    private static String mapJdbcType(String jdbcType) {
        if (jdbcType == null) return "VARCHAR";
        return switch (jdbcType.toUpperCase()) {
            case "INTEGER", "INT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "SMALLINT" -> "SMALLINT";
            case "TINYINT" -> "TINYINT";
            case "FLOAT" -> "FLOAT";
            case "DOUBLE" -> "DOUBLE";
            case "REAL" -> "REAL";
            case "DECIMAL", "NUMERIC" -> "DECIMAL";
            case "VARCHAR", "CHAR", "NVARCHAR", "NCHAR" -> "VARCHAR";
            case "TEXT", "LONGVARCHAR", "CLOB" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "TIMESTAMP", "DATETIME" -> "DATETIME";
            case "BLOB", "LONGVARBINARY", "BINARY" -> "BLOB";
            case "BOOLEAN", "BIT" -> "BOOLEAN";
            default -> "VARCHAR";
        };
    }

    private static void handleDbConnect(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, gson.toJson(Map.of("error", "method not allowed")));
            return;
        }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> params = gson.fromJson(body, Map.class);
            String url = (String) params.getOrDefault("url", "");
            String user = (String) params.getOrDefault("user", "");
            String password = (String) params.getOrDefault("password", "");
            long projectId = params.containsKey("projectId") ? ((Number) params.get("projectId")).longValue() : -1;
            if (url.isEmpty()) {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing url")));
                return;
            }
            LiveDatabaseReader reader = new LiveDatabaseReader(url, user, password);
            LiveDatabaseReader.DatabaseSchema schema = reader.readSchema();
            String md = reader.toMarkdown(schema);

            // 构造返回结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("dbProduct", schema.dbProduct);
            result.put("dbVersion", schema.dbVersion);
            result.put("tableCount", schema.tables.size());
            result.put("markdown", md);

            // 表列表（概览）
            List<Map<String, Object>> tableList = new ArrayList<>();
            for (LiveDatabaseReader.TableSchema t : schema.tables) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("name", t.name);
                tm.put("type", t.type);
                tm.put("columns", t.columns.size());
                tm.put("comment", t.comment != null ? t.comment : "");
                tableList.add(tm);
            }
            result.put("tables", tableList);

            // 若指定了 projectId，将数据库连接信息保存到项目记录中
            if (projectId >= 0) {
                DatabaseManager.saveDbConfig(projectId, url, user, password);
                result.put("saved", true);
            }

            sendJson(ex, 200, gson.toJson(result));
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(Map.of(
                "error", "连接失败: " + e.getClass().getSimpleName() + ": " +
                    (e.getMessage() != null ? e.getMessage() : "未知错误")
            )));
        }
    }

    private static void handleBusiness(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, gson.toJson(Map.of("error", "method not allowed")));
            return;
        }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> params = gson.fromJson(body, Map.class);
            long projectId = params.containsKey("projectId") ? ((Number) params.get("projectId")).longValue() : -1;

            if (projectId < 0) {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing projectId")));
                return;
            }

            // 从 DB 获取项目信息
            Map<String, Object> data = DatabaseManager.getProject(projectId);
            if (data == null || !data.containsKey("path")) {
                sendJson(ex, 404, gson.toJson(Map.of("error", "project not found")));
                return;
            }
            String projectPath = (String) data.get("path");

            // 重新扫描项目（需要完整模型）
            ProjectModel model = buildModel(projectPath);

            BusinessLogicAnalyzer biz = new BusinessLogicAnalyzer(model);
            if (params.containsKey("dbUrl")) {
                try {
                    String dbUrl = (String) params.get("dbUrl");
                    String dbUser = (String) params.getOrDefault("dbUser", "");
                    String dbPass = (String) params.getOrDefault("dbPass", "");
                    LiveDatabaseReader dbReader = new LiveDatabaseReader(dbUrl, dbUser, dbPass);
                    biz.setDatabaseSchema(dbReader.readSchema());
                } catch (Exception e) {
                    System.err.println("  ⚠️ DB schema fetch failed: " + e.getMessage());
                }
            }

            BusinessLogicAnalyzer.BusinessReport report = biz.analyze();
            String markdown = biz.toMarkdown(report);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("markdown", markdown);
            result.put("totalEntities", report.totalEntities);
            result.put("totalTables", report.totalTables);
            result.put("totalApis", report.totalApis);
            result.put("projectName", model.getProjectName());
            sendJson(ex, 200, gson.toJson(result));

        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(Map.of(
                "error", "分析失败: " + e.getClass().getSimpleName() + ": " +
                    (e.getMessage() != null ? e.getMessage() : "未知错误")
            )));
        }
    }

    /** API 数据流分析 */
    private static void handleApiFlow(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, gson.toJson(Map.of("error", "method not allowed")));
            return;
        }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> params = gson.fromJson(body, Map.class);
            long projectId = params.containsKey("projectId") ? ((Number) params.get("projectId")).longValue() : -1;

            if (projectId < 0) {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing projectId")));
                return;
            }

            Map<String, Object> data = DatabaseManager.getProject(projectId);
            if (data == null || !data.containsKey("path")) {
                sendJson(ex, 404, gson.toJson(Map.of("error", "project not found")));
                return;
            }

            ProjectModel model = buildModel((String) data.get("path"));

            BusinessLogicAnalyzer biz = new BusinessLogicAnalyzer(model);
            if (params.containsKey("dbUrl")) {
                try {
                    LiveDatabaseReader dbReader = new LiveDatabaseReader(
                        (String) params.get("dbUrl"),
                        (String) params.getOrDefault("dbUser", ""),
                        (String) params.getOrDefault("dbPass", "")
                    );
                    biz.setDatabaseSchema(dbReader.readSchema());
                } catch (Exception e) {
                    System.err.println("  ⚠️ DB schema fetch failed: " + e.getMessage());
                }
            }

            biz.analyze();
            List<BusinessLogicAnalyzer.ApiDataFlow> flows = biz.analyzeApiDataFlows();

            // 可选：只返回指定 API 的数据流
            String filterApi = (String) params.getOrDefault("api", "");
            if (!filterApi.isEmpty()) {
                String finalFilter = filterApi;
                flows = flows.stream()
                    .filter(f -> (f.apiMethod + " " + f.apiPath).equals(finalFilter)
                              || f.apiPath.equals(finalFilter))
                    .collect(Collectors.toList());
            }

            // 转为 JSON 安全的数据
            List<Map<String, Object>> flowList = new ArrayList<>();
            for (BusinessLogicAnalyzer.ApiDataFlow flow : flows) {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("apiMethod", flow.apiMethod);
                fm.put("apiPath", flow.apiPath);
                fm.put("tables", flow.tables);
                fm.put("crud", flow.crud);

                List<Map<String, Object>> steps = new ArrayList<>();
                for (BusinessLogicAnalyzer.ApiFlowStep step : flow.chainSteps) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("method", step.method);
                    sm.put("tables", step.tables);
                    sm.put("operation", step.operation);
                    steps.add(sm);
                }
                fm.put("chainSteps", steps);
                flowList.add(fm);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("flows", flowList);
            result.put("total", flowList.size());
            sendJson(ex, 200, gson.toJson(result));

        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(Map.of(
                "error", "数据流分析失败: " + e.getClass().getSimpleName() + ": " +
                    (e.getMessage() != null ? e.getMessage() : "未知错误")
            )));
        }
    }

    /** 提取公共的 model 构建逻辑 */
    private static ProjectModel buildModel(String projectPath) throws IOException {
        ProjectModel model = new ProjectScanner(projectPath).scan();
        SpringScanner springScanner = new SpringScanner(model.getClasses());
        springScanner.scan();
        model.setApiEndpoints(springScanner.getEndpoints());
        model.setBeanDependencies(springScanner.getBeanDependencies());
        model.setProjectPattern(springScanner.getProjectPattern());
        model.setSpringBoot(springScanner.isSpringBoot());
        model.setConfigProperties(springScanner.getConfigProperties());
        model.setBeanInfos(new ArrayList<>(springScanner.getBeanInfoMap().values()));

        ProjectAnalyzer analyzer = new ProjectAnalyzer(model);
        analyzer.analyze();
        return model;
    }

    /** 自然语言查询：根据用户问题生成 SQL */
    private static void handleNaturalQuery(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, gson.toJson(Map.of("error", "method not allowed")));
            return;
        }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> params = gson.fromJson(body, Map.class);
            String question = (String) params.getOrDefault("question", "");
            if (question.isEmpty()) {
                sendJson(ex, 400, gson.toJson(Map.of("error", "missing question")));
                return;
            }

            long projectId = params.containsKey("projectId") ? ((Number) params.get("projectId")).longValue() : -1;
            List<TableRelation> relations = List.of();
            LiveDatabaseReader.DatabaseSchema dbSchema = null;

            // 尝试从缓存获取业务分析结果中的关系和数据库信息
            // 如果有 projectId，重新扫描并分析关系
            if (projectId >= 0) {
                try {
                    Map<String, Object> data = DatabaseManager.getProject(projectId);
                    if (data != null && data.containsKey("path")) {
                        ProjectModel model = buildModel((String) data.get("path"));

                        // 先尝试用已有数据库连接获取 schema
                        // 如果没有连数据库，扫描也能用代码推断的关系
                        BusinessLogicAnalyzer biz = new BusinessLogicAnalyzer(model);
                        if (params.containsKey("dbUrl")) {
                            try {
                                LiveDatabaseReader dbReader = new LiveDatabaseReader(
                                    (String) params.get("dbUrl"),
                                    (String) params.getOrDefault("dbUser", ""),
                                    (String) params.getOrDefault("dbPass", "")
                                );
                                dbSchema = dbReader.readSchema();
                                biz.setDatabaseSchema(dbSchema);
                            } catch (Exception e) {
                                System.err.println("  ⚠️ DB fetch failed: " + e.getMessage());
                            }
                        }
                        biz.analyze();
                        relations = biz.getRelations();
                        if (dbSchema == null) dbSchema = getDbSchemaFromParams(params);
                    }
                } catch (Exception e) {
                    System.err.println("  ⚠️ 扫描失败: " + e.getMessage());
                }
            }

            // 如果没有项目扫描结果，尝试只用数据库 schema
            if (relations.isEmpty() && params.containsKey("dbUrl")) {
                try {
                    LiveDatabaseReader dbReader = new LiveDatabaseReader(
                        (String) params.get("dbUrl"),
                        (String) params.getOrDefault("dbUser", ""),
                        (String) params.getOrDefault("dbPass", "")
                    );
                    dbSchema = dbReader.readSchema();
                } catch (Exception e) {
                    sendJson(ex, 400, gson.toJson(Map.of("error", "数据库连接失败: " + e.getMessage())));
                    return;
                }
            }

            QueryAdvisor advisor = new QueryAdvisor(relations, dbSchema);
            QueryAdvisor.QuerySuggestion suggestion = advisor.suggest(question);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("sql", suggestion.sql);
            result.put("explanations", suggestion.explanations);
            result.put("confidence", suggestion.confidence);
            result.put("hasSchema", dbSchema != null);
            sendJson(ex, 200, gson.toJson(result));

        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(Map.of(
                "error", "查询失败: " + e.getClass().getSimpleName() + ": " +
                    (e.getMessage() != null ? e.getMessage() : "未知错误")
            )));
        }
    }

    private static LiveDatabaseReader.DatabaseSchema getDbSchemaFromParams(Map<String, Object> params) {
        if (!params.containsKey("dbUrl")) return null;
        try {
            LiveDatabaseReader dbReader = new LiveDatabaseReader(
                (String) params.get("dbUrl"),
                (String) params.getOrDefault("dbUser", ""),
                (String) params.getOrDefault("dbPass", "")
            );
            return dbReader.readSchema();
        } catch (Exception e) {
            return null;
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
        final String dbUrl;
        final String dbUser;
        final String dbPass;
        volatile boolean running;
        volatile String error;
        volatile long startTime;
        volatile int classes, methods, lines, apis, chains, findings, vulns, health;
        volatile String phase;
        volatile long projectId = -1;
        volatile boolean usedLiveDb;

        ScanTask(Path projectPath, String dbUrl, String dbUser, String dbPass) {
            this.projectPath = projectPath;
            this.dbUrl = dbUrl;
            this.dbUser = dbUser;
            this.dbPass = dbPass;
        }

        boolean isRunning() { return running; }

        void run() {
            running = true;
            startTime = System.currentTimeMillis();
            error = null;
            try {
                phase = "Scanning files...";
                ProjectModel model = new ProjectScanner(projectPath.toString()).scan();

                phase = "Analyzing Spring...";
                SpringScanner springScanner = new SpringScanner(model.getClasses());
                springScanner.scan();
                model.setApiEndpoints(springScanner.getEndpoints());
                model.setBeanDependencies(springScanner.getBeanDependencies());
                model.setProjectPattern(springScanner.getProjectPattern());
                model.setSpringBoot(springScanner.isSpringBoot());
                model.setConfigProperties(springScanner.getConfigProperties());
                model.setBeanInfos(new ArrayList<>(springScanner.getBeanInfoMap().values()));

                // 扫描 Mapper XML → 收集数据库表结构和 SQL
                List<Path> xmlFiles = new ArrayList<>();
                try (var paths = java.nio.file.Files.walk(projectPath)) {
                    paths.filter(p -> p.toString().endsWith(".xml"))
                         .filter(p -> !p.toString().contains("target" + File.separator))
                         .filter(p -> p.toString().contains("mapper") || p.toString().contains("Mapper") ||
                                      p.toString().contains("mybatis") || p.toString().contains("sqlmap") ||
                                      p.toString().contains("resources") || p.toString().contains("/dao/") ||
                                      p.toString().contains("/Dao/"))
                         .forEach(xmlFiles::add);
                }
                SqlParser sqlParser = new SqlParser(model.getClasses(), xmlFiles);
                sqlParser.scan();
                List<TableInfo> tables = sqlParser.getTables();
                if (!tables.isEmpty()) model.setDatabaseTables(tables);
                if (!sqlParser.getMapperSql().isEmpty()) model.setMapperSql(sqlParser.getMapperSql());

                SchemaParser schemaParser = new SchemaParser(model.getClasses(), xmlFiles);
                List<TableInfo> schemaTables = schemaParser.parse();
                if (!schemaTables.isEmpty()) model.setDatabaseTables(schemaTables);

                // 如果提供了实时数据库连接，优先用真实表结构覆盖代码解析结果
                boolean usedLiveDb = false;
                if (dbUrl != null && !dbUrl.isEmpty()) {
                    phase = "Connecting to live database...";
                    try {
                        LiveDatabaseReader dbReader = new LiveDatabaseReader(dbUrl, dbUser != null ? dbUser : "", dbPass != null ? dbPass : "");
                        LiveDatabaseReader.DatabaseSchema dbSchema = dbReader.readSchema();
                        List<TableInfo> realTables = convertDbSchemaToTableInfo(dbSchema);
                        model.setDatabaseTables(realTables);
                        usedLiveDb = true;
                        System.out.println("  [Scan] 使用实时数据库 (" + dbSchema.dbProduct + " " + dbSchema.dbVersion + ")，共 " + dbSchema.tables.size() + " 张表");
                    } catch (Exception e) {
                        System.err.println("  ⚠️ 实时数据库连接失败: " + e.getMessage() + "，使用 Mapper 解析的表结构");
                    }
                }

                phase = "Deep analyzing...";
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

                // Generate Skill sub-documents
                try {
                    Path skillOut = REPORTS_DIR.resolve("skills");
                    Files.createDirectories(skillOut);
                    new KnowledgeBaseGenerator(model).saveSkill(skillOut.toString());
                } catch (Exception e) {
                    System.err.println("  ⚠️ Skill generation failed: " + e.getMessage());
                }

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
            // 扫描完成后标记是否使用了实时数据库
            if (!running && error == null) {
                m.put("usedLiveDb", usedLiveDb);
            }
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
