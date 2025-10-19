package ru.floerka.yandexgpt;

import lombok.AllArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.simpleyaml.configuration.file.YamlFile;
import ru.floerka.yandexgpt.api.GPTApi;
import ru.floerka.yandexgpt.api.TokensAPI;
import ru.floerka.yandexgpt.api.models.Answer;
import ru.floerka.yandexgpt.api.models.IAMToken;
import ru.floerka.yandexgpt.api.models.InputMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexGPT {

    private final String folderID;
    private String iAmToken;
    private final String oAuth;
    private final OkHttpClient client;
    private final JSONInfo jsonInfo;

    public YandexGPT(String folderID, String token, String oAuth, int maxTokens, double temperature, List<InputMessage> history, @Nullable String serverPrompt) {
        this.folderID = folderID;
        this.iAmToken = token;
        this.oAuth = oAuth;
        this.client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .build();
        if (history.isEmpty()) {
            history.add(new InputMessage("system", serverPrompt));
        }
        this.jsonInfo = new JSONInfo(maxTokens, temperature, history);
    }

    public YandexGPT(YamlFile configuration) {
        try {
            configuration.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.client = new OkHttpClient.Builder().connectTimeout(configuration.getInt("client.connect-timeout", 10), TimeUnit.SECONDS).readTimeout(configuration.getInt("client.read-timeout", 10), TimeUnit.SECONDS)
                .build();
        this.folderID = configuration.getString("folder-id", "");
        this.iAmToken = configuration.getString("i-am-token", "");
        this.oAuth = configuration.getString("oauth-token", "");

        if(iAmToken.isEmpty() && !oAuth.isEmpty()) {
            try {
                Optional<IAMToken> tokenOptional = TokensAPI.generateToken(oAuth,client);
                tokenOptional.ifPresent(iamToken -> this.iAmToken = iamToken.getToken());
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        } else if(oAuth.isEmpty() && iAmToken.isEmpty()) {
            throw new RuntimeException("У вас не подключен токен IAM или OAuth. Работа не может быть продолжена");
        }

        List<InputMessage> history = new ArrayList<>();
        Pattern patternRole = Pattern.compile("\\[role:(system|user)]");
        Pattern patternMessage = Pattern.compile("\\[message:([^]]+)]");
        configuration.getStringList("history").forEach( message -> {
            Matcher matcherRole = patternRole.matcher(message);
            Matcher matcherMessage = patternMessage.matcher(message);

            if (matcherRole.find() && matcherMessage.find()) {
                String role = matcherRole.group(1);
                String messageText = matcherMessage.group(1);
                history.add(new InputMessage(role,messageText));
            }
        });
        if(history.isEmpty() || !history.get(0).getRole().equals("system")) {
            history.add(new InputMessage("system", configuration.getString("system-prompt", "")));
        }
        this.jsonInfo = new JSONInfo(configuration.getInt("max-tokens", 300),
                configuration.getDouble("temperature", 0.3d), history);
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("modelUri", generateModelURI());
        object.put("completionOptions", new JSONObject().put("maxTokens", jsonInfo.maxTokens).put("temperature", jsonInfo.temperature));
        object.put("messages", generateMessagesHistory(jsonInfo.history));
        return object;
    }

    public Optional<Answer> sendMessage(String prompt) {
        Optional<Answer> answerOptional = Optional.empty();
        jsonInfo.history.add(new InputMessage("user", prompt));
        Request request = GPTApi.generateRequest(folderID, iAmToken, generateJSON());
        try {
            Response response = GPTApi.execute(request, client);
            if (response.isSuccessful()) {
                String message = response.body().string();
                JSONObject json = new JSONObject(message);
                answerOptional = Optional.of(GPTApi.parseAnswer(json));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return answerOptional;
    }

    public CompletableFuture<Answer> sendMessageAsync(String prompt) {
        CompletableFuture<Answer> future = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            Optional<Answer> optionalAnswer = sendMessage(prompt);
            if(optionalAnswer.isPresent()) {
                future.complete(optionalAnswer.get());
            } else future.complete(null);
        });
        return future;
    }

    private JSONArray generateMessagesHistory(List<InputMessage> list) {
        JSONArray messages = new JSONArray();
        for (InputMessage m : list) {
            JSONObject jm = new JSONObject()
                    .put("role", m.getRole())
                    .put("text", m.getMessage());
            messages.put(jm);
        }
        return messages;
    }

    private String generateModelURI() {
        return "gpt://" + folderID + "/yandexgpt/rc";
    }

    @AllArgsConstructor
    public static class JSONInfo {
        private int maxTokens;
        private double temperature;
        private List<InputMessage> history;
    }


}
