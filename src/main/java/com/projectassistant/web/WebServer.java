package com.projectassistant.web;

import com.projectassistant.model.*;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.knowledge.KnowledgeBaseGenerator;
import com.projectassistant.reporter.ReportGenerator;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * 嵌入式 Web 服务器 — 浏览器里点点点就能扫项目
 *
 * 启动: java -jar xxx.jar --web
 * 默认: http://localhost:8653
 */
public class WebServer {

    private static final int DEFAULT_PORT = 8653;
    private static final Path REPORTS_DIR = Paths.get("reports").toAbsolutePath().normalize();

    // 当前扫描任务状态
    private static volatile ScanTask currentTask = null;

    public static void start(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            }
        }

        Files.createDirectories(REPORTS_DIR);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebServer::handleRoot);
        server.createContext("/scan", WebServer::handleScan);
        server.createContext("/status", WebServer::handleStatus);
        server.createContext("/reports", WebServer::handleReports);
        server.createContext("/api/report", WebServer::handleApiReport);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   ProjectAssistant Web 版已启动!    ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║   http://localhost:" + port + "              ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }

    // ====== 路由处理 ======

    /** 首页：返回 HTML */
    private static void handleRoot(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            send404(ex);
            return;
        }
        String html = buildHtml();
        sendResponse(ex, 200, "text/html; charset=utf-8", html);
    }

    /** POST /scan 启动扫描 */
    private static void handleScan(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendResponse(ex, 405, "text/plain", "Method Not Allowed");
            return;
        }

        // 读取请求体
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "utf-8"))
                .lines().collect(Collectors.joining());
        Map<String, String> params = parseQuery(body);
        String projectPath = params.get("path");
        if (projectPath == null || projectPath.isEmpty()) {
            sendResponse(ex, 400, "application/json", "{\"error\":\"缺少 path 参数\"}");
            return;
        }

        // 如果已有任务在跑，拒绝
        if (currentTask != null && currentTask.isRunning()) {
            sendResponse(ex, 409, "application/json", "{\"error\":\"已有扫描任务进行中\"}");
            return;
        }

        // 启动异步扫描
        Path targetPath = Paths.get(projectPath);
        currentTask = new ScanTask(targetPath);
        Thread scanThread = new Thread(currentTask::run);
        scanThread.setDaemon(true);
        scanThread.start();

        sendResponse(ex, 200, "application/json", "{\"status\":\"started\"}");
    }

    /** GET /status 查询扫描状态 */
    private static void handleStatus(HttpExchange ex) throws IOException {
        if (currentTask == null) {
            sendResponse(ex, 200, "application/json", "{\"status\":\"idle\"}");
            return;
        }
        sendResponse(ex, 200, "application/json", currentTask.toJson());
    }

    /** GET /reports?file=xxx 查看 / 下载报告 */
    private static void handleReports(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(query != null ? query : "");
        String filename = qm.get("file");

        if (filename == null || filename.isEmpty()) {
            // 列出报告
            StringBuilder sb = new StringBuilder("[");
            try (Stream<Path> files = Files.list(REPORTS_DIR)) {
                List<String> names = files
                        .filter(p -> p.toString().endsWith(".md"))
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(names.get(i)).append("\"");
                }
            }
            sb.append("]");
            sendResponse(ex, 200, "application/json", sb.toString());
            return;
        }

        // 安全检查：防止目录穿越
        Path filePath = REPORTS_DIR.resolve(filename).normalize();
        if (!filePath.startsWith(REPORTS_DIR) || !Files.exists(filePath)) {
            send404(ex);
            return;
        }

        String content = Files.readString(filePath);
        sendResponse(ex, 200, "text/markdown; charset=utf-8", content);
    }

    /** GET /api/report?file=xxx 返回报告 JSON 概览 */
    private static void handleApiReport(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(query != null ? query : "");
        String filename = qm.get("file");

        if (filename == null || filename.isEmpty()) {
            sendResponse(ex, 400, "application/json", "{\"error\":\"缺少 file 参数\"}");
            return;
        }

        Path filePath = REPORTS_DIR.resolve(filename).normalize();
        if (!filePath.startsWith(REPORTS_DIR) || !Files.exists(filePath)) {
            sendResponse(ex, 404, "application/json", "{\"error\":\"报告不存在\"}");
            return;
        }

        // 读取 Markdown 并提取前几个章节作为预览
        String content = Files.readString(filePath);
        String preview = extractPreview(content, 3000);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("filename", filename);
        info.put("size", content.length());
        info.put("preview", preview);
        info.put("fullUrl", "/reports?file=" + filename);

        sendJson(ex, 200, info);
    }

    // ====== 工具方法 ======

    private static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes("utf-8");
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private static void sendJson(HttpExchange ex, int code, Object obj) throws IOException {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) obj).entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) {
                json.append("\"").append(escapeJson((String) v)).append("\"");
            } else if (v instanceof Number) {
                json.append(v);
            } else {
                json.append("\"").append(v).append("\"");
            }
        }
        json.append("}");
        sendResponse(ex, code, "application/json; charset=utf-8", json.toString());
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void send404(HttpExchange ex) throws IOException {
        sendResponse(ex, 404, "text/plain", "404 Not Found");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                try {
                    params.put(URLDecoder.decode(pair.substring(0, eq), "utf-8"),
                               URLDecoder.decode(pair.substring(eq + 1), "utf-8"));
                } catch (Exception e) { /* skip */ }
            }
        }
        return params;
    }

    /** 从 Markdown 中提取预览（前 N 字符，保持章节结构） */
    private static String extractPreview(String content, int maxLen) {
        if (content.length() <= maxLen) return content;
        // 截断到最近的章节标题
        String truncated = content.substring(0, maxLen);
        int lastSection = truncated.lastIndexOf("\n## ");
        if (lastSection > maxLen / 2) {
            truncated = truncated.substring(0, lastSection);
        }
        return truncated + "\n\n... (报告过长，已截断)";
    }

    // ====== HTML 页面 ======

    private static String buildHtml() {
        return "<!DOCTYPE html>\n" +
        "<html lang=\"zh-CN\">\n" +
        "<head>\n" +
        "<meta charset=\"utf-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
        "<title>ProjectAssistant — Java 老狗级项目理解</title>\n" +
        "<style>\n" +
        "* { margin:0; padding:0; box-sizing:border-box; }\n" +
        "body { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif; background:#f5f5f5; color:#333; }\n" +
        ".container { max-width:960px; margin:0 auto; padding:20px; }\n" +
        "header { background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; padding:30px 20px; border-radius:12px; margin-bottom:24px; }\n" +
        "header h1 { font-size:24px; margin-bottom:4px; }\n" +
        "header p { opacity:.85; font-size:14px; }\n" +
        ".card { background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,.08); padding:20px; margin-bottom:16px; }\n" +
        "label { display:block; font-weight:600; margin-bottom:8px; font-size:14px; }\n" +
        ".input-row { display:flex; gap:8px; }\n" +
        "input[type=text] { flex:1; padding:10px 14px; border:1px solid #ddd; border-radius:8px; font-size:14px; outline:none; transition:border .2s; }\n" +
        "input[type=text]:focus { border-color:#667eea; }\n" +
        "button { padding:10px 24px; border:none; border-radius:8px; font-size:14px; cursor:pointer; transition:all .2s; font-weight:600; }\n" +
        "button.primary { background:#667eea; color:#fff; }\n" +
        "button.primary:hover { background:#5a6fd6; }\n" +
        "button.primary:disabled { opacity:.5; cursor:not-allowed; }\n" +
        "button.secondary { background:#e8e8e8; color:#555; }\n" +
        "button.secondary:hover { background:#ddd; }\n" +
        ".status-bar { display:flex; align-items:center; gap:12px; padding:12px 16px; border-radius:8px; margin-bottom:16px; font-size:14px; }\n" +
        ".status-idle { background:#f0f0f0; color:#666; }\n" +
        ".status-running { background:#fff3cd; color:#856404; }\n" +
        ".status-done { background:#d4edda; color:#155724; }\n" +
        ".status-error { background:#f8d7da; color:#721c24; }\n" +
        ".spinner { display:inline-block; width:16px; height:16px; border:2px solid #856404; border-top-color:transparent; border-radius:50%; animation:spin .8s linear infinite; }\n" +
        "@keyframes spin { to { transform:rotate(360deg); } }\n" +
        ".reports-list { list-style:none; }\n" +
        ".reports-list li { padding:12px 16px; border-bottom:1px solid #eee; display:flex; justify-content:space-between; align-items:center; }\n" +
        ".reports-list li:last-child { border-bottom:none; }\n" +
        ".reports-list a { color:#667eea; text-decoration:none; font-weight:500; }\n" +
        ".reports-list a:hover { text-decoration:underline; }\n" +
        ".reports-list .badge { font-size:12px; background:#e8e8e8; padding:2px 8px; border-radius:4px; color:#666; }\n" +
        ".report-content { background:#fafafa; border:1px solid #eee; border-radius:8px; padding:16px; max-height:600px; overflow-y:auto; font-size:13px; line-height:1.6; white-space:pre-wrap; }\n" +
        ".report-content h2 { color:#667eea; margin:16px 0 8px; }\n" +
        ".report-content table { border-collapse:collapse; width:100%; margin:8px 0; font-size:12px; }\n" +
        ".report-content th,.report-content td { border:1px solid #ddd; padding:6px 8px; text-align:left; }\n" +
        ".report-content th { background:#f0f0f0; }\n" +
        ".stats-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(120px,1fr)); gap:12px; }\n" +
        ".stat-card { background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; border-radius:10px; padding:16px; text-align:center; }\n" +
        ".stat-card .num { font-size:28px; font-weight:700; }\n" +
        ".stat-card .label { font-size:12px; opacity:.85; margin-top:4px; }\n" +
        ".hidden { display:none; }\n" +
        ".footer { text-align:center; color:#999; font-size:12px; padding:20px; }\n" +
        "</style>\n" +
        "</head>\n" +
        "<body>\n" +
        "<div class=\"container\">\n" +
        "<header><h1>🔍 ProjectAssistant</h1><p>Java 老狗级项目理解工具 · 内嵌 Web 版</p></header>\n" +

        // 状态区
        "<div class=\"card\">\n" +
        "<label>📂 项目路径</label>\n" +
        "<div class=\"input-row\">\n" +
        "<input type=\"text\" id=\"pathInput\" placeholder=\"例如: /home/user/my-project\" value=\"" + getDefaultPath() + "\">\n" +
        "<button class=\"primary\" id=\"scanBtn\" onclick=\"startScan()\">🚀 开始扫描</button>\n" +
        "</div>\n" +
        "</div>\n" +

        "<div id=\"statusArea\" class=\"status-bar status-idle\">💤 空闲，等待扫描</div>\n" +
        "<div id=\"statsArea\" class=\"hidden\"><div class=\"stats-grid\" id=\"statsGrid\"></div></div>\n" +

        // 报告列表
        "<div class=\"card\">\n" +
        "<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;\">\n" +
        "<label style=\"margin:0;\">📄 扫描报告</label>\n" +
        "<button class=\"secondary\" onclick=\"loadReports()\">🔄 刷新</button>\n" +
        "</div>\n" +
        "<ul class=\"reports-list\" id=\"reportsList\"><li style=\"color:#999;\">点击刷新加载报告列表</li></ul>\n" +
        "</div>\n" +

        // 报告预览
        "<div class=\"card hidden\" id=\"previewCard\">\n" +
        "<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;\">\n" +
        "<label style=\"margin:0;\" id=\"previewTitle\">📖 报告预览</label>\n" +
        "<button class=\"secondary\" onclick=\"closePreview()\">✕ 关闭</button>\n" +
        "</div>\n" +
        "<div class=\"report-content\" id=\"previewContent\"></div>\n" +
        "</div>\n" +

        "<div class=\"footer\">ProjectAssistant · 已扫描 <span id=\"scanCount\">-</span> 项目</div>\n" +
        "</div>\n" +

        "<script>\n" +
        "let pollTimer = null;\n" +

        "function startScan() {\n" +
        "  const path = document.getElementById('pathInput').value.trim();\n" +
        "  if (!path) { alert('请输入项目路径'); return; }\n" +
        "  document.getElementById('scanBtn').disabled = true;\n" +
        "  setStatus('running', '⏳ 扫描中...');\n" +
        "  document.getElementById('statsArea').classList.add('hidden');\n" +
        "  fetch('/scan', { method:'POST', body:'path='+encodeURIComponent(path),\n" +
        "    headers:{'Content-Type':'application/x-www-form-urlencoded'} })\n" +
        "    .then(r=>r.json()).then(d=>{\n" +
        "      if (d.status==='started') { pollStatus(); }\n" +
        "      else { setStatus('error', '❌ 启动失败: '+JSON.stringify(d)); pollTimer=null; }\n" +
        "    }).catch(e=>{ setStatus('error','❌ 网络错误'); pollTimer=null; });\n" +
        "}\n" +

        "function pollStatus() {\n" +
        "  fetch('/status').then(r=>r.json()).then(d=>{\n" +
        "    if (d.status==='running') {\n" +
        "      setStatus('running', '⏳ '+d.phase+' ('+d.elapsed+'s) — 已发现 '+d.classes+' 个类');\n" +
        "      if (d.classes > 0) showMiniStats(d);\n" +
        "      pollTimer = setTimeout(pollStatus, 1000);\n" +
        "    } else if (d.status==='done') {\n" +
        "      setStatus('done', '✅ 扫描完成! 耗时 '+d.elapsed+'s');\n" +
        "      document.getElementById('scanBtn').disabled = false;\n" +
        "      showStats(d);\n" +
        "      loadReports();\n" +
        "      pollTimer = null;\n" +
        "    } else if (d.status==='error') {\n" +
        "      setStatus('error', '❌ '+d.message);\n" +
        "      document.getElementById('scanBtn').disabled = false;\n" +
        "      pollTimer = null;\n" +
        "    } else {\n" +
        "      pollTimer = setTimeout(pollStatus, 1000);\n" +
        "    }\n" +
        "  }).catch(e=>{ setStatus('error','❌ 状态查询失败'); pollTimer=null; });\n" +
        "}\n" +

        "function setStatus(type, msg) {\n" +
        "  const el = document.getElementById('statusArea');\n" +
        "  el.className = 'status-bar status-'+type;\n" +
        "  el.innerHTML = msg;\n" +
        "}\n" +

        "function showMiniStats(d) {\n" +
        "  const grid = document.getElementById('statsGrid');\n" +
        "  grid.innerHTML = '';\n" +
        "  addStatCard(grid, '📁 类', d.classes);\n" +
        "  if (d.methods) addStatCard(grid, '📝 方法', d.methods);\n" +
        "  if (d.apis) addStatCard(grid, '🔗 API', d.apis);\n" +
        "  if (d.lines) addStatCard(grid, '📏 行数', d.lines);\n" +
        "  document.getElementById('statsArea').classList.remove('hidden');\n" +
        "}\n" +

        "function showStats(d) {\n" +
        "  const grid = document.getElementById('statsGrid');\n" +
        "  grid.innerHTML = '';\n" +
        "  addStatCard(grid, '📁 Java 类', d.classes||'-');\n" +
        "  addStatCard(grid, '📏 总行数', d.lines||'-');\n" +
        "  addStatCard(grid, '📝 方法数', d.methods||'-');\n" +
        "  addStatCard(grid, '🔗 API 端点', d.apis||'-');\n" +
        "  addStatCard(grid, '📊 数据库表', d.tables||'-');\n" +
        "  addStatCard(grid, '🧩 Bean 数', d.beans||'-');\n" +
        "  addStatCard(grid, '⚠️ 漏洞', d.vulns||'0');\n" +
        "  addStatCard(grid, '❤️ 健康分', d.health||'-');\n" +
        "  document.getElementById('statsArea').classList.remove('hidden');\n" +
        "}\n" +

        "function addStatCard(parent, label, value) {\n" +
        "  const div = document.createElement('div');\n" +
        "  div.className = 'stat-card';\n" +
        "  div.innerHTML = '<div class=\"num\">'+value+'</div><div class=\"label\">'+label+'</div>';\n" +
        "  parent.appendChild(div);\n" +
        "}\n" +

        "function loadReports() {\n" +
        "  fetch('/reports').then(r=>r.json()).then(list=>{\n" +
        "    const ul = document.getElementById('reportsList');\n" +
        "    if (list.length===0) { ul.innerHTML='<li style=\"color:#999;\">暂无报告</li>'; return; }\n" +
        "    ul.innerHTML = list.map(f => {\n" +
        "      const isKb = f.includes('_knowledge');\n" +
        "      const badge = isKb ? '🧠 知识库' : '📄 报告';\n" +
        "      return '<li><a href=\"#\" onclick=\"showReport(\\''+f+'\\');return false;\">'+f+'</a> <span class=\"badge\">'+badge+'</span></li>';\n" +
        "    }).join('');\n" +
        "    document.getElementById('scanCount').textContent = list.length;\n" +
        "  }).catch(e=>{});\n" +
        "}\n" +

        "function showReport(filename) {\n" +
        "  document.getElementById('previewTitle').textContent = '📖 '+filename;\n" +
        "  document.getElementById('previewCard').classList.remove('hidden');\n" +
        "  document.getElementById('previewContent').textContent = '加载中...';\n" +
        "  fetch('/reports?file='+encodeURIComponent(filename))\n" +
        "    .then(r=>r.text()).then(content=>{\n" +
        "      document.getElementById('previewContent').textContent = content;\n" +
        "    }).catch(e=>{});\n" +
        "}\n" +

        "function closePreview() {\n" +
        "  document.getElementById('previewCard').classList.add('hidden');\n" +
        "}\n" +

        "// 加载报告列表\n" +
        "loadReports();\n" +
        "</script>\n" +
        "</body></html>";
    }

    private static String getDefaultPath() {
        // 尝试取当前目录或常见路径
        return Paths.get("").toAbsolutePath().normalize().toString();
    }

    // ====== 扫描任务 ======

    static class ScanTask {
        final Path targetPath;
        volatile String status = "running";
        volatile String phase = "初始化";
        volatile long startTime;
        volatile int classes;
        volatile int methods;
        volatile int lines;
        volatile int apis;
        volatile int tables;
        volatile int beans;
        volatile int vulns;
        volatile int health;
        volatile String errorMessage;
        volatile long elapsed;

        ScanTask(Path targetPath) {
            this.targetPath = targetPath;
        }

        boolean isRunning() { return "running".equals(status); }

        void run() {
            startTime = System.currentTimeMillis();
            try {
                // 1. 扫描源码
                phase = "扫描 Java 源码";
                ProjectScanner scanner = new ProjectScanner(targetPath.toString());
                ProjectModel project = scanner.scan();
                classes = project.getClasses().size();
                methods = project.getStats().getTotalMethods();
                lines = project.getStats().getTotalLines();

                // 2. 分析
                phase = "分析依赖与调用链";
                ProjectAnalyzer analyzer = new ProjectAnalyzer(project);
                List<AnalysisResult> results = analyzer.analyze();

                apis = project.getApiEndpoints().size();
                tables = project.getDatabaseTables().size();
                beans = project.getBeanInfos().size();
                vulns = project.getVulnFindings() != null ? project.getVulnFindings().size() : 0;

                // 3. 输出
                phase = "生成报告";
                String projectName = project.getProjectName();
                Path outputBase = REPORTS_DIR;
                Files.createDirectories(outputBase);

                // 知识库
                KnowledgeBaseGenerator kb = new KnowledgeBaseGenerator(project);
                Files.writeString(outputBase.resolve(projectName + "_knowledge.md"), kb.generate());

                // Markdown 报告
                ReportGenerator mdReporter = new ReportGenerator(project, results);
                Files.writeString(outputBase.resolve(projectName + ".md"), mdReporter.generateMarkdown());

                // Skill
                Path toolDir = Paths.get("").toAbsolutePath().normalize();
                Path skillDir = toolDir.resolve("skills");
                kb.saveSkill(skillDir.toString());

                // 健康分
                health = extractHealthScore(kb.generate());

                elapsed = (System.currentTimeMillis() - startTime) / 1000;
                status = "done";

            } catch (Exception e) {
                status = "error";
                errorMessage = e.getMessage();
                elapsed = (System.currentTimeMillis() - startTime) / 1000;
            }
        }

        String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"status\":\"").append(status).append("\"");
            sb.append(",\"phase\":\"").append(escapeJson(phase)).append("\"");
            sb.append(",\"elapsed\":").append(elapsed > 0 ? elapsed : (System.currentTimeMillis()-startTime)/1000);
            sb.append(",\"classes\":").append(classes);
            sb.append(",\"methods\":").append(methods);
            sb.append(",\"lines\":").append(lines);
            sb.append(",\"apis\":").append(apis);
            sb.append(",\"tables\":").append(tables);
            sb.append(",\"beans\":").append(beans);
            sb.append(",\"vulns\":").append(vulns);
            sb.append(",\"health\":").append(health);
            if (errorMessage != null) {
                sb.append(",\"message\":\"").append(escapeJson(errorMessage)).append("\"");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private static int extractHealthScore(String kbContent) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("健康评分[：: ]*\\*\\*(\\d+)/100\\*\\*")
                .matcher(kbContent);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
