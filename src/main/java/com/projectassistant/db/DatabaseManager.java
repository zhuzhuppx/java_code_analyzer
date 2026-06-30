package com.projectassistant.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private static final Path DB_DIR = Paths.get("reports").toAbsolutePath().normalize();
    private static final String DB_URL = "jdbc:h2:" + DB_DIR.resolve("projectassistant").toString()
            + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static volatile Connection conn;
    private static final Map<String, Long> pathCache = new ConcurrentHashMap<>();

    public static synchronized void init() throws SQLException, IOException {
        Files.createDirectories(DB_DIR);
        try { Class.forName("org.h2.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("H2 driver not found in classpath", e); }
        conn = DriverManager.getConnection(DB_URL, "sa", "");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS projects (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "path VARCHAR(1024)," +
                    "scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "stats_json CLOB)");
            st.execute("CREATE TABLE IF NOT EXISTS knowledge_bases (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "project_id BIGINT," +
                    "content CLOB," +
                    "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE IF NOT EXISTS reports (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "project_id BIGINT," +
                    "filename VARCHAR(255)," +
                    "content CLOB," +
                    "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE IF NOT EXISTS chat_messages (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "project_id BIGINT," +
                    "role VARCHAR(16)," +
                    "content CLOB," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_chat_project ON chat_messages(project_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_kb_project ON knowledge_bases(project_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_report_project ON reports(project_id)");
        }
        // warm path cache
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, path FROM projects")) {
            while (rs.next()) pathCache.put(rs.getString("path"), rs.getLong("id"));
        }
    }

    public static synchronized long saveProject(String path, String statsJson) {
        try {
            if (pathCache.containsKey(path)) {
                long id = pathCache.get(path);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE projects SET stats_json=?, scanned_at=CURRENT_TIMESTAMP WHERE id=?")) {
                    ps.setString(1, statsJson);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                return id;
            }
            String name = Path.of(path).getFileName().toString();
            if (name.isEmpty()) name = path;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO projects (name, path, stats_json) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, path);
                ps.setString(3, statsJson);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) { long id = rs.getLong(1); pathCache.put(path, id); return id; }
                }
            }
        } catch (SQLException e) { System.err.println("DB saveProject error: " + e.getMessage()); }
        return -1;
    }

    public static synchronized void saveKnowledgeBase(long projectId, String content) {
        try {
            // Delete old KB then insert
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM knowledge_bases WHERE project_id=?")) {
                del.setLong(1, projectId);
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO knowledge_bases (project_id, content) VALUES (?,?)")) {
                ps.setLong(1, projectId);
                ps.setString(2, content);
                ps.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("DB saveKB error: " + e.getMessage()); }
    }

    public static synchronized void saveReport(long projectId, String filename, String content) {
        try {
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM reports WHERE project_id=? AND filename=?")) {
                del.setLong(1, projectId);
                del.setString(2, filename);
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reports (project_id, filename, content) VALUES (?,?,?)")) {
                ps.setLong(1, projectId);
                ps.setString(2, filename);
                ps.setString(3, content);
                ps.executeUpdate();
            }
        } catch (SQLException e) { System.err.println("DB saveReport error: " + e.getMessage()); }
    }

    public static synchronized void saveChatMessage(long projectId, String role, String content) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO chat_messages (project_id, role, content) VALUES (?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("DB saveChat error: " + e.getMessage()); }
    }

    public static synchronized List<Map<String, Object>> listProjects() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT p.id, p.name, p.path, p.scanned_at, p.stats_json, " +
                     "(SELECT COUNT(*) FROM chat_messages WHERE project_id=p.id) AS chat_count " +
                     "FROM projects p ORDER BY p.scanned_at DESC")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("name", rs.getString("name"));
                m.put("path", rs.getString("path"));
                m.put("scannedAt", rs.getString("scanned_at"));
                String statsJson = rs.getString("stats_json");
                if (statsJson != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = new com.google.gson.Gson().fromJson(statsJson, Map.class);
                    m.putAll(stats);
                }
                m.put("chatCount", rs.getInt("chat_count"));
                list.add(m);
            }
        } catch (SQLException e) { System.err.println("DB listProjects error: " + e.getMessage()); }
        return list;
    }

    public static synchronized Map<String, Object> getProject(long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, path, scanned_at, stats_json FROM projects WHERE id=?")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("id", rs.getLong("id"));
                    result.put("name", rs.getString("name"));
                    result.put("path", rs.getString("path"));
                    result.put("scannedAt", rs.getString("scanned_at"));
                    String statsJson = rs.getString("stats_json");
                    if (statsJson != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stats = new com.google.gson.Gson().fromJson(statsJson, Map.class);
                        result.putAll(stats);
                    }
                }
            }
        } catch (SQLException e) { System.err.println("DB getProject error: " + e.getMessage()); }

        // Load KB
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT content FROM knowledge_bases WHERE project_id=? ORDER BY generated_at DESC LIMIT 1")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) result.put("kb", rs.getString("content"));
            }
        } catch (SQLException e) { System.err.println("DB getKB error: " + e.getMessage()); }

        // Load reports
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT filename, content FROM reports WHERE project_id=? ORDER BY filename")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, String>> reports = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> r = new LinkedHashMap<>();
                    r.put("filename", rs.getString("filename"));
                    r.put("content", rs.getString("content"));
                    reports.add(r);
                }
                result.put("reports", reports);
            }
        } catch (SQLException e) { System.err.println("DB getReports error: " + e.getMessage()); }

        // Load chat messages
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT role, content FROM chat_messages WHERE project_id=? ORDER BY created_at")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, String>> chats = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> c = new LinkedHashMap<>();
                    c.put("role", rs.getString("role"));
                    c.put("content", rs.getString("content"));
                    chats.add(c);
                }
                result.put("chats", chats);
            }
        } catch (SQLException e) { System.err.println("DB getChats error: " + e.getMessage()); }

        return result;
    }

    public static synchronized void saveDbConfig(long projectId, String dbUrl, String dbUser, String dbPass) {
        try {
            // 读当前 stats_json
            String statsJson = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT stats_json FROM projects WHERE id=?")) {
                ps.setLong(1, projectId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) statsJson = rs.getString("stats_json");
                }
            }
            if (statsJson == null) statsJson = "{}";
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = new com.google.gson.Gson().fromJson(statsJson, Map.class);
            stats.put("dbUrl", dbUrl);
            stats.put("dbUser", dbUser != null ? dbUser : "");
            stats.put("dbPass", dbPass != null ? dbPass : "");
            String updatedJson = new com.google.gson.Gson().toJson(stats);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE projects SET stats_json=? WHERE id=?")) {
                ps.setString(1, updatedJson);
                ps.setLong(2, projectId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("DB saveDbConfig error: " + e.getMessage());
        }
    }

    public static synchronized long getProjectIdForPath(String path) {
        return pathCache.getOrDefault(path, -1L);
    }

    public static synchronized void close() {
        try { if (conn != null) conn.close(); }
        catch (SQLException ignored) {}
    }
}
