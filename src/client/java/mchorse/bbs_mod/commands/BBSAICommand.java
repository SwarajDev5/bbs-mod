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

    private static final String SERVER_URL = "http://127.0.0.1:8000";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bbs")
            .then(CommandManager.literal("ai")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                    .executes(context -> {
                        String prompt = StringArgumentType.getString(context, "prompt");
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("§6[AI] Sending prompt request to backend..."), false);

                        CompletableFuture.runAsync(() -> {
                            try {
                                JsonObject json = new JsonObject();
                                json.addProperty("prompt", prompt);

                                HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(SERVER_URL + "/generate"))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                                    .build();

                                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                                if (response.statusCode() == 200) {
                                    JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                                    String result = responseJson.get("scene").getAsString();
                                    source.sendFeedback(() -> Text.literal("§a[AI Response]: §f" + result), false);
                                } else {
                                    JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
                                    String detail = errorJson.has("detail") ? errorJson.get("detail").getAsString() : "Server Error";
                                    source.sendError(Text.literal("§c[AI Error]: " + detail));
                                }
                            } catch (Exception e) {
                                source.sendError(Text.literal("§c[AI Error]: Failed to reach server. Is your Python app running?"));
                            }
                        });
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("aistatus")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    CompletableFuture.runAsync(() -> {
                        try {
                            HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(SERVER_URL + "/status"))
                                .timeout(Duration.ofSeconds(3))
                                .GET()
                                .build();

                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            if (response.statusCode() == 200) {
                                JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                                String model = responseJson.get("model").getAsString();
                                source.sendFeedback(() -> Text.literal("§a[AI Connection Status]: §2ONLINE §7| Active Model: §e" + model), false);
                            } else {
                                source.sendError(Text.literal("§c[AI Connection Status]: Error response code " + response.statusCode()));
                            }
                        } catch (Exception e) {
                            source.sendError(Text.literal("§c[AI Connection Status]: §4OFFLINE §7(Check your console endpoint server)"));
                        }
                    });
                    return 1;
                })
            )
            .then(CommandManager.literal("aimodel")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    CompletableFuture.runAsync(() -> {
                        try {
                            HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(SERVER_URL + "/model"))
                                .timeout(Duration.ofSeconds(3))
                                .GET()
                                .build();

                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            if (response.statusCode() == 200) {
                                JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                                String model = responseJson.get("model").getAsString();
                                source.sendFeedback(() -> Text.literal("§a[AI Running Model]: §e" + model), false);
                            } else {
                                source.sendError(Text.literal("§c[AI Error]: Could not read backend configuration properties."));
                            }
                        } catch (Exception e) {
                            source.sendError(Text.literal("§c[AI Error]: Core Python framework is offline."));
                        }
                    });
                    return 1;
                })
            )
        );
    }
}
