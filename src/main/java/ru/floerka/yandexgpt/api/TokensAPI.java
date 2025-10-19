package ru.floerka.yandexgpt.api;

import okhttp3.*;
import org.json.JSONObject;
import ru.floerka.yandexgpt.api.models.IAMToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public class TokensAPI {

    private static final String url = "https://iam.api.cloud.yandex.net/iam/v1/tokens";

    private static Request createIAMTokenRequest(String oAuth) {
        JSONObject bodyObject = new JSONObject().put("yandexPassportOauthToken", oAuth);
        RequestBody body = GPTApi.generateBody(bodyObject);
        return new Request.Builder().post(body).url(url).header("Content-Type", "application/json").build();
    }

    public static Optional<IAMToken> generateToken(Request request, OkHttpClient client) throws IOException {
        Optional<IAMToken> tokenOptional = Optional.empty();
        try {
            Response response = GPTApi.execute(request,client);
            if(response.isSuccessful()) {
                JSONObject object = new JSONObject(response.body().string());
                if(object.has("iamToken")) {
                    tokenOptional = Optional.of(new IAMToken(object.getString("iamToken"), Instant.parse(object.getString("expiresAt"))));
                } else throw new IOException("Вероятно, OAuth токен невалидный. Проверьте его. Пожалуйста");
            }
        } catch (IOException e) {
            throw new IOException();
        }
        return tokenOptional;
    }

    public static Optional<IAMToken> generateToken(String oauth, OkHttpClient client) throws IOException {
        return generateToken(createIAMTokenRequest(oauth), client);
    }

}
