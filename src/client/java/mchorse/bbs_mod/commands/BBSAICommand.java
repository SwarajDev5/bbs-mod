package mchorse.bbs_mod.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BBSAICommand {

    private static final String SERVER_URL = "http://node1.bicore.host:2004";
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bbs")
            .then(CommandManager.literal("ai")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                    .executes(c -> {
                        String prompt = StringArgumentType.getString(c, "prompt");
                        sendRequest(c.getSource(), "/generate", "POST", "{\"prompt\":\"" + prompt + "\"}", "scene");
                        return 1;
                    })))
            .then(CommandManager.literal("aistatus").executes(c -> { sendRequest(c.getSource(), "/status", "GET", null, "status"); return 1; }))
            .then(CommandManager.literal("aimodel").executes(c -> { sendRequest(c.getSource(), "/model", "GET", null, "model"); return 1; })));
    }

    private static void sendRequest(ServerCommandSource source, String endpoint, String method, String body, String key) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + endpoint));
                if (method.equals("POST")) builder.POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
                else builder.GET();

                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String result = JsonParser.parseString(response.body()).getAsJsonObject().get(key).getAsString();
                    source.sendFeedback(() -> Text.literal("§a[BBS AI]: §f" + result), false);
                } else {
                    source.sendError(Text.literal("§c[AI Error]: Server returned " + response.statusCode()));
                }
            } catch (Exception e) {
                source.sendError(Text.literal("§c[AI Error]: Python server unreachable."));
            }
        });
    }
}
