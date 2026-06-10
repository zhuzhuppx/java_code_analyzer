package com.projectassistant.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

/**
 * DeepSeek 对话客户端
 *
 * 加载项目知识后，直接用 DeepSeek 聊项目相关的问题。
 * API 兼容 OpenAI 格式，不需要额外依赖。
 *
 * 用法:
 *   java ... --ask "这个项目有什么接口？"
 *   java ... --chat        (交互模式)
 *
 * API Key 会在启动时从命令行输入读取。
 */
public class DeepSeekChat {

    private static final String BASE_URL = "https://api.deepseek.com";
    private static final String MODEL = "deepseek-v4-flash";
    private static final Gson gson = new Gson();
    private final HttpClient http;
    private final String apiKey;
    private final JsonArray messages;
    private final Scanner scanner;

    public DeepSeekChat(String systemPrompt, Scanner scanner) {
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key == null || key.isEmpty()) {
            System.out.print("请输入 DeepSeek API Key: ");
            key = scanner.nextLine().trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("API Key 不能为空");
            }
        }
        this.apiKey = key;
        this.scanner = scanner;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);
    }

    /**
     * 单次问答
     */
    public String ask(String question) throws IOException, InterruptedException {
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", question);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("stream", false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            return "⚠️ API 请求失败 (" + resp.statusCode() + "): " + resp.body();
        }

        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);
        String reply = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .get("message").getAsJsonObject()
                .get("content").getAsString();

        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.addProperty("content", reply);
        messages.add(assistantMsg);

        return reply;
    }

    /**
     * 交互式聊天模式
     */
    public void interactiveChat() throws IOException, InterruptedException {
        System.out.println("\n💬 进入对话模式（输入 /bye 退出）\n");

        while (true) {
            System.out.print("你 > ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("/bye") || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("👋 再见！");
                break;
            }
            if (input.isEmpty()) continue;

            System.out.println("🧠 DeepSeek 思考中...");
            long start = System.currentTimeMillis();
            String reply = ask(input);
            long elapsed = (System.currentTimeMillis() - start) / 1000;
            System.out.println("🐋 DeepSeek (" + elapsed + "s) >");
            System.out.println(reply);
            System.out.println();
        }
    }
}
