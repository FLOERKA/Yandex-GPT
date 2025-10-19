package ru.floerka.yandexgpt;

import lombok.AllArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.floerka.yandexgpt.api.GPTApi;
import ru.floerka.yandexgpt.api.models.Answer;
import ru.floerka.yandexgpt.api.models.InputMessage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class YandexGPT {

    private final String folderID;
    private final String token;
    private final OkHttpClient client;
    private final JSONInfo jsonInfo;

    public YandexGPT(String folderID, String token, int maxTokens, double temperature, List<InputMessage> history, @Nullable String serverPrompt) {
        this.folderID = folderID;
        this.token = token;
        this.client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .build();
        if (history.isEmpty()) {
            history.add(new InputMessage("system", serverPrompt));
        }
        this.jsonInfo = new JSONInfo(maxTokens, temperature, history);
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("modelUri", generateModelURI());
        object.put("completionOptions", new JSONObject().put("maxTokens", jsonInfo.maxTokens).put("temperature", jsonInfo.temperature));
        object.put("messages", generateMessagesHistory(jsonInfo.history));
        return object;
    }

    public Optional<Answer> tryMessage(String prompt) {
        Optional<Answer> answerOptional = Optional.empty();
        jsonInfo.history.add(new InputMessage("user", prompt));
        Request request = GPTApi.generateRequest(folderID, token, generateJSON());
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
