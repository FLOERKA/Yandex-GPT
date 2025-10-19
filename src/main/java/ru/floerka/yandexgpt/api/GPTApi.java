package ru.floerka.yandexgpt.api;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.floerka.yandexgpt.api.models.Answer;
import ru.floerka.yandexgpt.api.models.InputMessage;
import ru.floerka.yandexgpt.api.models.OutputMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GPTApi {


    private static final String url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";


    public static Request generateRequest(String folder, String token, JSONObject payLoad) {
        return new Request.Builder()
                .post(generateBody(payLoad))
                .url(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("x-folder-id", folder)
                .build();
    }
    public static RequestBody generateBody(JSONObject object) {
        return RequestBody.create(object.toString(), MediaType.parse("application/json"));
    }
    public static Response execute(Request request, OkHttpClient client) throws IOException {
        return client.newCall(request).execute();
    }
    public static Answer parseAnswer(JSONObject json) {
        assert json.has("result") && json.has("usage");

        JSONObject result = json.getJSONObject("result");
        JSONArray resultsArray = result.getJSONArray("alternatives");
        List<OutputMessage> messages = new ArrayList<>();
        for(int i = 0; i < resultsArray.length(); i++) {
            JSONObject object = resultsArray.getJSONObject(i);
            JSONObject message = object.getJSONObject("message");
            String status = object.getString("status");
            String role = message.getString("role");
            String text = message.getString("text");
            messages.add(new OutputMessage(role,text,status));
        }

        JSONObject usage = result.getJSONObject("usage");
        int inputTokens = Integer.parseInt(usage.getString("inputTextTokens"));
        int ansTokens = Integer.parseInt(usage.getString("completionTokens"));
        int totalTokens = Integer.parseInt(usage.getString("totalTokens"));
        String modelVersion = result.getString("modelVersion");
        return new Answer(modelVersion, inputTokens, ansTokens, totalTokens, messages);
    }
}
