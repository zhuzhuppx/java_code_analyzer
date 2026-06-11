package com.projectassistant.web;

import com.projectassistant.model.ProjectModel;
import com.projectassistant.scanner.ProjectScanner;
import com.projectassistant.analyzer.ProjectAnalyzer;
import com.projectassistant.analyzer.ProjectAnalyzer.AnalysisResult;
import com.projectassistant.knowledge.KnowledgeBaseGenerator;
import com.projectassistant.reporter.ReportGenerator;
import com.projectassistant.chain.CallChainAnalyzer;
import com.projectassistant.chain.CallChain;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Embedded Web Server
 * Start: java -jar xxx.jar --web  |  Default port: 8653
 */
public class WebServer {

    private static final int DEFAULT_PORT = 8653;
    private static final Path REPORTS_DIR = Paths.get("reports").toAbsolutePath().normalize();
    private static volatile ScanTask currentTask = null;
    private static String cachedHtml = null;

    public static void start(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) port = Integer.parseInt(args[i + 1]);
        }
        Files.createDirectories(REPORTS_DIR);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebServer::handleRoot);
        server.createContext("/scan", WebServer::handleScan);
        server.createContext("/status", WebServer::handleStatus);
        server.createContext("/reports", WebServer::handleReports);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510");
        System.out.println("\u2502   ProjectAssistant Web \u5df2\u542f\u52a8!     \u2502");
        System.out.println("\u2502   http://localhost:" + port + "              \u2502");
        System.out.println("\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518\n");
    }

    private static void handleRoot(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) { send404(ex); return; }
        if (cachedHtml == null) cachedHtml = buildHtml();
        sendResponse(ex, 200, "text/html; charset=utf-8", cachedHtml);
    }

    private static void handleScan(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { sendResponse(ex, 405, "text/plain", "Method Not Allowed"); return; }
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "utf-8")).lines().collect(Collectors.joining());
        Map<String, String> params = parseQuery(body);
        String projectPath = params.get("path");
        if (projectPath == null || projectPath.isEmpty()) { sendJson(ex, 400, "{\"error\":\"missing path\"}"); return; }
        if (currentTask != null && currentTask.isRunning()) { sendJson(ex, 409, "{\"error\":\"scan already running\"}"); return; }
        currentTask = new ScanTask(Paths.get(projectPath));
        Thread t = new Thread(currentTask::run); t.setDaemon(true); t.start();
        sendJson(ex, 200, "{\"status\":\"started\"}");
    }

    private static void handleStatus(HttpExchange ex) throws IOException {
        sendJson(ex, 200, currentTask == null ? "{\"status\":\"idle\"}" : currentTask.toJson());
    }

    private static void handleReports(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getRawQuery();
        Map<String, String> qm = parseQuery(q != null ? q : "");
        String filename = qm.get("file");
        if (filename == null || filename.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            try (Stream<Path> files = Files.list(REPORTS_DIR)) {
                List<String> names = files.filter(p -> p.toString().endsWith(".md")).map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
                for (int i = 0; i < names.size(); i++) { if (i > 0) sb.append(","); sb.append('"').append(names.get(i)).append('"'); }
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString()); return;
        }
        Path fp = REPORTS_DIR.resolve(filename).normalize();
        if (!fp.startsWith(REPORTS_DIR) || !Files.exists(fp)) { send404(ex); return; }
        sendResponse(ex, 200, "text/markdown; charset=utf-8", Files.readString(fp));
    }

    private static void sendResponse(HttpExchange ex, int code, String ct, String body) throws IOException {
        byte[] b = body.getBytes("utf-8");
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b); ex.getResponseBody().close();
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        sendResponse(ex, code, "application/json; charset=utf-8", json);
    }

    private static void send404(HttpExchange ex) throws IOException { sendResponse(ex, 404, "text/plain", "404"); }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> p = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return p;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) { try { p.put(URLDecoder.decode(pair.substring(0, eq), "utf-8"), URLDecoder.decode(pair.substring(eq + 1), "utf-8")); } catch (Exception ignored) {} }
        }
        return p;
    }

    // ====== Build HTML ======
    private static String buildHtml() {
        String dp = Paths.get("").toAbsolutePath().normalize().toString().replace("\"", "&quot;");
        return "<!DOCTYPE html>\n<html lang='zh-CN'>\n<head>\n<meta charset='utf-8'>\n<meta name='viewport' content='width=device-width, initial-scale=1'>\n<title>ProjectAssistant</title>\n"
            + "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'><" + "/script>\n"
            + "<style>\n"
            + "*{margin:0;padding:0;box-sizing:border-box}\n"
            + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Noto Sans SC',sans-serif;background:#f0f2f5;color:#1e293b}\n"
            + ".app{display:flex;flex-direction:column;min-height:100vh}\n"
            + ".hdr{background:#fff;border-bottom:1px solid #e2e8f0;padding:0 24px;height:58px;display:flex;align-items:center;justify-content:space-between;position:sticky;top:0;z-index:100}\n"
            + ".hdr h1{font-size:17px;display:flex;align-items:center;gap:8px}\n"
            + ".hdr h1 em{font-style:normal;background:linear-gradient(135deg,#6366f1,#a855f7);-webkit-background-clip:text;-webkit-text-fill-color:transparent}\n"
            + ".hdr .m{font-size:12px;color:#64748b}\n"
            + ".mn{display:flex;flex:1;padding:20px 24px;gap:20px;max-width:1400px;width:100%;margin:0 auto}\n"
            + ".sb{width:340px;flex-shrink:0;display:flex;flex-direction:column;gap:16px}\n"
            + ".ct{flex:1;min-width:0;display:flex;flex-direction:column;gap:16px}\n"
            + ".cd{background:#fff;border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.06);padding:20px}\n"
            + ".ctitle{font-size:12px;font-weight:600;color:#64748b;text-transform:uppercase;letter-spacing:.5px;margin-bottom:12px;display:flex;align-items:center;gap:6px}\n"
            + ".ig{display:flex;gap:8px}\n"
            + ".ig input{flex:1;padding:10px 14px;border:1.5px solid #e2e8f0;border-radius:8px;font-size:14px;outline:none;transition:border .2s;font-family:inherit}\n"
            + ".ig input:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}\n"
            + ".btn{padding:10px 20px;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;transition:all .2s;display:inline-flex;align-items:center;gap:6px;white-space:nowrap;font-family:inherit}\n"
            + ".btn-p{background:#6366f1;color:#fff}\n"
            + ".btn-p:hover{background:#818cf8;transform:translateY(-1px);box-shadow:0 4px 12px rgba(99,102,241,.3)}\n"
            + ".btn-p:disabled{opacity:.5;cursor:not-allowed}\n"
            + ".btn-g{background:transparent;color:#64748b;padding:8px 12px}\n"
            + ".btn-g:hover{background:rgba(0,0,0,.05)}\n"
            + ".bs{padding:6px 12px;font-size:12px}\n"
            + ".st{display:flex;align-items:center;gap:10px;padding:12px 16px;border-radius:8px;font-size:13px;transition:all .3s}\n"
            + ".st-i{background:#f1f5f9;color:#64748b}\n"
            + ".st-r{background:#fef9c3;color:#854d0e}\n"
            + ".st-d{background:#dcfce7;color:#166534}\n"
            + ".st-e{background:#fee2e2;color:#991b1b}\n"
            + ".sp{width:16px;height:16px;border:2.5px solid currentColor;border-top-color:transparent;border-radius:50%;animation:sp .7s linear infinite;display:inline-block}\n"
            + "@keyframes sp{to{transform:rotate(360deg)}}\n"
            + ".pb{margin-top:8px;height:4px;background:#e2e8f0;border-radius:2px;overflow:hidden}\n"
            + ".pb .fl{height:100%;background:linear-gradient(90deg,#6366f1,#a855f7);border-radius:2px;transition:width .5s ease;width:0%}\n"
            + ".sg{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}\n"
            + ".si{background:linear-gradient(135deg,#6366f1,#7c3aed);color:#fff;border-radius:10px;padding:14px 12px;text-align:center;transition:transform .2s}\n"
            + ".si:hover{transform:translateY(-2px)}\n"
            + ".si .n{font-size:22px;font-weight:700}\n"
            + ".si .l{font-size:11px;opacity:.8;margin-top:2px}\n"
            + ".si.w{background:linear-gradient(135deg,#f59e0b,#d97706)}\n"
            + ".si.d{background:linear-gradient(135deg,#ef4444,#dc2626)}\n"
            + ".si.s{background:linear-gradient(135deg,#10b981,#059669)}\n"
            + ".ri{display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #e2e8f0}\n"
            + ".ri:last-child{border-bottom:none}\n"
            + ".ri .nm{font-size:13px;color:#6366f1;cursor:pointer;font-weight:500;flex:1}\n"
            + ".ri .nm:hover{text-decoration:underline}\n"
            + ".ri .bg{font-size:11px;padding:2px 8px;border-radius:4px;font-weight:500;margin-left:8px}\n"
            + ".bg-r{background:#e0e7ff;color:#6366f1}\n"
            + ".bg-k{background:#fae8ff;color:#a21caf}\n"
            + ".es{text-align:center;padding:30px 0;color:#64748b;font-size:13px}\n"
            + ".pt{display:flex;gap:0;border-bottom:1px solid #e2e8f0;margin-bottom:16px}\n"
            + ".pt button{padding:8px 16px;border:none;background:none;font-size:13px;font-weight:500;cursor:pointer;color:#64748b;border-bottom:2px solid transparent;transition:all .2s;font-family:inherit}\n"
            + ".pt button.act{color:#6366f1;border-bottom-color:#6366f1}\n"
            + ".rb{font-size:13px;line-height:1.7;overflow-wrap:break-word}\n"
            + ".rb h2{font-size:16px;color:#6366f1;margin:20px 0 8px;padding-bottom:4px;border-bottom:1px solid #e2e8f0}\n"
            + ".rb h3{font-size:14px;margin:14px 0 6px}\n"
            + ".rb table{border-collapse:collapse;width:100%;margin:8px 0;font-size:12px}\n"
            + ".rb th,.rb td{border:1px solid #e2e8f0;padding:5px 8px;text-align:left}\n"
            + ".rb th{background:#f8fafc;font-weight:600}\n"
            + ".rb code{background:#f1f5f9;padding:1px 5px;border-radius:3px;font-size:12px}\n"
            + ".rb pre{background:#1e293b;color:#e2e8f0;padding:12px 16px;border-radius:8px;overflow-x:auto;font-size:12px;line-height:1.5;margin:8px 0}\n"
            + ".rb blockquote{border-left:3px solid #6366f1;padding-left:12px;margin:8px 0;color:#64748b}\n"
            + ".rb ul{padding-left:20px;margin:6px 0}\n"
            + ".h{display:none!important}\n"
            + ".fi{animation:fi .3s ease}\n"
            + "@keyframes fi{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}\n"
            + ".wl{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:60px 40px;text-align:center}\n"
            + ".wl .ico{font-size:48px;margin-bottom:16px}\n"
            + ".wl h2{font-size:20px;margin-bottom:8px}\n"
            + ".wl p{color:#64748b;max-width:400px;font-size:14px}\n"
            + ".wl .tags{display:flex;gap:12px;font-size:12px;color:#64748b;margin-top:20px;flex-wrap:wrap;justify-content:center}\n"
            + "@media(max-width:900px){.mn{flex-direction:column}.sb{width:100%}.sg{grid-template-columns:repeat(2,1fr)}}\n"
            + "</style>\n</head>\n<body>\n<div class='app'>\n"
            + "<div class='hdr'><h1>\uD83D\uDD0D <em>ProjectAssistant</em></h1><div class='m'>Java \u8001\u72d7\u7ea7\u9879\u76ee\u7406\u89e3 \u00b7 <span id='sc'>0</span> \u6b21\u626b\u63cf</div></div>\n"
            + "<div class='mn'>\n"
            + "<div class='sb'>\n"
            + "<div class='cd'><div class='ctitle'>\uD83D\uDCC2 \u9879\u76ee\u8def\u5f84</div>\n"
            + "<div class='ig'><input type='text' id='pi' placeholder='/path/to/java-project' value='" + dp + "' onkeydown='if(event.key===\"Enter\")scan()'>\n"
            + "<button class='btn btn-p' id='sb' onclick='scan()'>\uD83D\uDE80 \u626B\u63CF</button></div></div>\n"
            + "<div class='st st-i' id='sta'>\u23F3 \u5C31\u7EEA\uFF0C\u8F93\u5165\u8DEF\u5F84\u5F00\u59CB\u626B\u63CF</div>\n"
            + "<div class='cd h fi' id='scd'><div class='ctitle'>\uD83D\uDCCA \u626B\u63CF\u7ED3\u679C</div>\n"
            + "<div class='sg' id='sg'></div><div class='pb'><div class='fl' id='pf'></div></div></div>\n"
            + "<div class='cd' style='flex:1'><div class='ctitle' style='display:flex;justify-content:space-between;align-items:center'>\n"
            + "<span>\uD83D\uDCC4 \u62A5\u544A</span><button class='btn btn-g bs' onclick='lr()'>\uD83D\uDD04 \u5237\u65B0</button></div>\n"
            + "<div id='rc'><div class='es'>\u6682\u65E0\u62A5\u544A</div></div></div>\n"
            + "</div>\n"
            + "<div class='ct'>\n"
            + "<div class='cd h fi' id='pc'>\n"
            + "<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:4px'>\n"
            + "<div class='ctitle' style='margin:0' id='pt'>\uD83D\uDCD6 \u62A5\u544A</div>\n"
            + "<div style='display:flex;gap:4px'><button class='btn btn-g bs' onclick='rp()'>\uD83D\uDD04</button><button class='btn btn-g bs' onclick='cp()'>\u2715</button></div></div>\n"
            + "<div class='pt' id='ptabs'></div><div class='rb' id='pct'><p style='color:#64748b'>\u9009\u62E9\u62A5\u544A\u67E5\u770B</p></div></div>\n"
            + "<div class='cd wl' id='wc'><div class='ico'>\uD83D\uDD0D</div>\n"
            + "<h2>Java \u9879\u76EE\u7406\u89E3\u5DE5\u5177</h2>\n"
            + "<p>\u8F93\u5165\u9879\u76EE\u8DEF\u5F84\u5F00\u59CB\u626B\u63CF\uFF0C\u81EA\u52A8\u5206\u6790\u6E90\u7801\u3001Spring \u6846\u67B6\u3001\u6570\u636E\u5E93\u3001\u8C03\u7528\u94FE\u3001\u4F9D\u8D56\u6F0F\u6D1E\uFF0C\u751F\u6210\u5927\u6A21\u578B\u53CB\u597D\u7684\u77E5\u8BC6\u5E93\u3002</p>\n"
            + "<div class='tags'><span>\uD83D\uDCC1 \u6E90\u7801\u626B\u63CF</span><span>\uD83D\uDD17 API \u8DEF\u7531</span><span>\uD83E\uDDE9 Bean \u4F9D\u8D56</span><span>\uD83D\uDDC4\uFE0F Schema \u9006\u5411</span><span>\u26A0\uFE0F \u6F0F\u6D1E\u626B\u63CF</span></div></div>\n"
            + "</div>\n</div>\n</div>\n"

            // JavaScript
            + "<script>\n"
            + "var pt_=null,cr=null;\n"
            + "function md(t){if(typeof marked!=='undefined')try{return marked.parse(t)}catch(e){}return '<pre style=\"white-space:pre-wrap\">'+t.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</pre>'}\n"
            + "function scan(){var p=document.getElementById('pi').value.trim();if(!p){st('i','请输入路径');return}"
            + "document.getElementById('sb').disabled=true;document.getElementById('scd').classList.add('h');"
            + "st('r','扫描中...');"
            + "fetch('/scan',{method:'POST',body:'path='+encodeURIComponent(p),headers:{'Content-Type':'application/x-www-form-urlencoded'}})"
            + ".then(function(r){return r.json()}).then(function(d){if(d.status==='started'){pp();st('r','初始化...')}else{st('e','失败: '+JSON.stringify(d));document.getElementById('sb').disabled=false}})"
            + ".catch(function(e){st('e','网络错误');document.getElementById('sb').disabled=false})}\n"
            + "function pp(){"
            + "fetch('/status').then(function(r){return r.json()}).then(function(d){"
            + "if(d.status==='running'){"
            + "var pct=Math.min(95,Math.round(d.elapsed/60*100));"
            + "document.getElementById('pf').style.width=pct+'%';"
            + "if(d.classes>0){document.getElementById('scd').classList.remove('h');ms(d)}"
            + "st('r',(d.phase||'')+(d.classes?' | '+d.classes+' 类; '+d.methods+' 方法; ':'')+'('+d.elapsed+'s)');"
            + "pt_=setTimeout(pp,1500)"
            + "}else if(d.status==='done'){"
            + "document.getElementById('pf').style.width='100%';document.getElementById('sb').disabled=false;"
            + "st('d','完成! 耗时 '+d.elapsed+'s');fs(d);lr();pt_=null;"
            + "setTimeout(function(){document.getElementById('pf').style.width='0%'},2000)"
            + "}else if(d.status==='error'){st('e','错误: '+d.message);document.getElementById('sb').disabled=false;pt_=null}"
            + "else{pt_=setTimeout(pp,1500)}"
            + "}).catch(function(e){st('e','状态查询失败');pt_=null})}\n"
            + "function st(t,m){var e=document.getElementById('sta');e.className='st st-'+t;e.innerHTML=m}\n"
            + "function ms(d){document.getElementById('sg').innerHTML='';"
            + "ac(d.classes||0,'类','');ac(d.methods||0,'方法','');ac(d.lines||0,'行','');ac(d.apis||0,'API','')}\n"
            + "function fs(d){var g=document.getElementById('sg');g.innerHTML='';"
            + "ac(d.classes||'-','Java 类','');ac(d.lines||'-','总行数','');"
            + "ac(d.methods||'-','方法数','');ac(d.apis||'-','API 端点','');"
            + "ac(d.findings||'-','分析结论','');ac(d.chains||'-','调用链','');"
            + "ac(d.vulns||'0','漏洞',(parseInt(d.vulns)||0)>0?'d':'');"
            + "ac(d.health||'-','健康分',d.health<60?'d':d.health<80?'w':'s');"
            + "document.getElementById('scd').classList.remove('h')}\n"
            + "function ac(v,l,c){var g=document.getElementById('sg');var d=document.createElement('div');d.className='si'+(c?' '+c:'');d.innerHTML='<div class=\"n\">'+v+'</div><div class=\"l\">'+l+'</div>';g.appendChild(d)}\n"
            + "function lr(){"
            + "fetch('/reports').then(function(r){return r.json()}).then(function(list){"
            + "document.getElementById('sc').textContent=list.length;"
            + "if(list.length===0){document.getElementById('rc').innerHTML='<div class=\"es\">暂无报告</div>';return}"
            + "document.getElementById('rc').innerHTML=list.map(function(f){"
            + "var kb=f.includes('_knowledge');"
            + "return '<div class=\"ri\"><span class=\"nm\" onclick=\"sr(\\''+f+'\\')\">'+f+'</span><span class=\"bg '+(kb?'bg-k':'bg-r')+'\">'+(kb?'知识库':'报告')+'</span></div>'"
            + "}).join('')})}\n"
            + "var crFiles={};\n"
            + "function sr(f){"
            + "document.getElementById('wc').classList.add('h');document.getElementById('pc').classList.remove('h');"
            + "cr=f;document.getElementById('pt').textContent=' '+f;"
            + "document.getElementById('pct').innerHTML='<p>加载中...</p>';rp2(f,'report')}\n"
            + "function rp2(f,t){"
            + "fetch('/reports?file='+encodeURIComponent(f)).then(function(r){return r.text()}).then(function(c){"
            + "crFiles[f]=c;var kb=f.includes('_knowledge');var tabs=document.getElementById('ptabs');"
            + "if(kb){tabs.innerHTML='<button class=\"'+(t==='report'?'act':'')+'\" onclick=\"rp2(\\''+f+'\\',\\'report\\')\">报告</button><button class=\"'+(t==='knowledge'?'act':'')+'\" onclick=\"rp2(\\''+f+'\\',\\'knowledge\\')\">知识库</button>'}"
            + "else tabs.innerHTML='<button class=\"act\">报告</button>';"
            + "if(t==='knowledge'){document.getElementById('pct').innerHTML=md(crFiles[f].replace(/.*?<!-- KNOWLEDGE_START -->/s,''))}else{document.getElementById('pct').innerHTML=md(c)}})}}\n"
            + "function rp(){if(cr)rp2(cr,'report')}\n"
            + "function cp(){document.getElementById('pc').classList.add('h');document.getElementById('wc').classList.remove('h');cr=null}\n"
            + "lr();\n"
            + "<" + "/script>\n"
            + "</body>\n</html>\n";
    }

    // ====== Scan Task ======
    static class ScanTask {
        final Path projectPath;
        final long startTime = System.currentTimeMillis() / 1000;
        volatile boolean running = true;
        volatile String errorMsg = null;
        volatile String phase = "";
        volatile int classes = 0, methods = 0, lines = 0, apis = 0;
        volatile int findings = 0, chains = 0, vulns = 0;
        volatile int health = 0;

        ScanTask(Path p) { this.projectPath = p; }

        boolean isRunning() { return running; }

        void run() {
            try {
                long start = System.currentTimeMillis();

                // Phase 1: scan
                phase = "解析项目结构...";
                ProjectModel project = new ProjectScanner(projectPath.toString()).scan();
                classes = (int)project.getClasses().size();
                methods = (int)project.getClasses().stream().mapToLong(c -> c.getMethods().size()).sum();
                lines = (int)project.getClasses().stream().mapToLong(c -> c.getLineCount()).sum();
                apis = project.getApiEndpoints().size();

                // Phase 2: analyze
                phase = "分析代码质量...";
                ProjectAnalyzer analyzer = new ProjectAnalyzer(project);
                java.util.List<AnalysisResult> results = analyzer.analyze();
                findings = results.size();

                // Phase 3: vuln scan
                phase = "扫描漏洞...";
                try {
                    com.projectassistant.scanner.VulnScanner vs = new com.projectassistant.scanner.VulnScanner(project.getDependencies());
                    java.util.List<com.projectassistant.scanner.VulnScanner.VulnFinding> vlist = vs.scan();
                    java.util.List<ProjectModel.VulnFinding> vf = new java.util.ArrayList<>();
                    for (var v : vlist) {
                        vf.add(new ProjectModel.VulnFinding(v.groupId, v.artifactId, v.currentVersion, v.cve, v.severity, v.description));
                    }
                    project.setVulnFindings(vf);
                    vulns = vf.size();
                } catch (Exception ignored) {}

                // Phase 4: call chain
                phase = "追踪调用链...";
                try {
                    java.util.List<String> apiEntries = project.getApiEndpoints().stream()
                        .map(e -> e.getControllerClass() + "." + e.getMethodName())
                        .distinct().collect(java.util.stream.Collectors.toList());
                    CallChainAnalyzer chainAnalyzer = new CallChainAnalyzer(new java.util.HashMap<>());
                    chainAnalyzer.analyze(project.getCallGraph(), apiEntries);
                    project.setCallChains(chainAnalyzer.getChains());
                    chains = chainAnalyzer.getChains().size();
                } catch (Exception ignored) {}

                // Phase 5: generate reports
                phase = "生成报告...";
                String reportMd = new ReportGenerator(project, results).generateMarkdown();
                String projectName = project.getProjectName();
                Path reportFile = REPORTS_DIR.resolve(projectName + ".md");
                Files.writeString(reportFile, reportMd);

                phase = "生成知识库...";
                KnowledgeBaseGenerator kbGen = new KnowledgeBaseGenerator(project);
                String kbContent = kbGen.generate();
                Path kbFile = REPORTS_DIR.resolve(projectName + "_knowledge.md");
                try {
                    Files.writeString(kbFile, kbContent);
                } catch (Exception ignored) {}
                try {
                    kbGen.saveSkill(REPORTS_DIR.toString());
                } catch (Exception ignored) {}

                phase = "完成!";
                long elapsed = (System.currentTimeMillis() - start) / 1000;
                running = false;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                if (errorMsg == null) errorMsg = e.getClass().getSimpleName();
                running = false;
            }
        }

        String toJson() {
            if (errorMsg != null) return "{\"status\":\"error\",\"message\":\"" + errorMsg.replace("\"", "\\\"") + "\"}";
            if (running) {
                long now = System.currentTimeMillis() / 1000;
                return "{\"status\":\"running\",\"elapsed\":" + (now - startTime) + ",\"phase\":\"" + phase + "\""
                    + ",\"classes\":" + classes + ",\"methods\":" + methods + ",\"lines\":" + lines + ",\"apis\":" + apis + "}";
            }
            return "{\"status\":\"done\",\"elapsed\":" + (System.currentTimeMillis()/1000 - startTime)
                + ",\"classes\":" + classes + ",\"methods\":" + methods + ",\"lines\":" + lines + ",\"apis\":" + apis
                + ",\"findings\":" + findings + ",\"chains\":" + chains + ",\"vulns\":" + vulns + ",\"health\":" + health + "}";
        }
    }
}
