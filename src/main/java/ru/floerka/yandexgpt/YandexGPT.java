package ru.floerka.yandexgpt;

import lombok.AllArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.floerka.yandexgpt.api.ConfigManager;
import ru.floerka.yandexgpt.api.GPTApi;
import ru.floerka.yandexgpt.api.TokensAPI;
import ru.floerka.yandexgpt.api.exception.BadRequestException;
import ru.floerka.yandexgpt.api.models.Answer;
import ru.floerka.yandexgpt.api.models.IAMToken;
import ru.floerka.yandexgpt.api.models.InputMessage;
import ru.floerka.yandexgpt.utils.Configuration;

import java.io.File;
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
    private final Configuration save;

    public YandexGPT(String folderID, String iamToken, String oAuth, int maxTokens, double temperature, List<InputMessage> history, @Nullable String serverPrompt, Configuration configuration) {
        this.folderID = folderID;
        this.iAmToken = iamToken;
        this.oAuth = oAuth;
        this.client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .build();

        if (history.isEmpty()) {
            if(serverPrompt == null || serverPrompt.isEmpty()) {
                boolean th = false;
                if(configuration != null) {
                    serverPrompt = configuration.getString("system-prompt");
                    if(serverPrompt == null || serverPrompt.isEmpty()) th = true;
                } else th = true;
                if(th) {
                    throw new RuntimeException("Системный промпт обязательно должен быть прописан либо в history, либо отдельно.");
                }
            }
            history.add(new InputMessage("system", serverPrompt));
        }

        this.jsonInfo = new JSONInfo(maxTokens, temperature, history);
        this.save = configuration;

        checkBeans();
    }

    public YandexGPT(Configuration configuration) {

        configuration.cleanLoad();

        this.client = new OkHttpClient.Builder().connectTimeout(configuration.getInt("client.connect-timeout", 10), TimeUnit.SECONDS).readTimeout(configuration.getInt("client.read-timeout", 10), TimeUnit.SECONDS)
                .build();
        this.folderID = configuration.getString("folder-id", "");
        this.iAmToken = configuration.getString("i-am-token", "");
        this.oAuth = configuration.getString("oauth-token", "");

        checkBeans();

        List<InputMessage> history = new ArrayList<>();
        Pattern patternRole = Pattern.compile("\\[role:(system|user|assistant)]");
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
            if(configuration.getString("system-prompt").isEmpty())
                throw new RuntimeException("Системный промпт обязательно должен быть прописан либо в history, либо отдельно.");
            history.add(new InputMessage("system", configuration.getString("system-prompt", "")));
        }

        this.jsonInfo = new JSONInfo(configuration.getInt("max-tokens", 300),
                configuration.getDouble("temperature", 0.3d), history);
        this.save = configuration;
    }



    public YandexGPT(File file) {
        this(new Configuration(file));
    }

    public void checkBeans() {
        if(folderID == null || folderID.isEmpty())
            throw new RuntimeException("Не был введён folderID");
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
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("modelUri", generateModelURI());
        object.put("completionOptions", new JSONObject().put("maxTokens", jsonInfo.maxTokens).put("temperature", jsonInfo.temperature));
        object.put("messages", generateMessagesHistory(jsonInfo.history));
        return object;
    }

    public Optional<Answer> sendMessage(String prompt) {
        Optional<Answer> answerOptional;
        jsonInfo.history.add(new InputMessage("user", prompt));
        Request request = GPTApi.generateRequest(folderID, iAmToken, generateJSON());
        try {
            Response response = GPTApi.execute(request, client);
            if (response.isSuccessful()) {
                String message = response.body().string();
                JSONObject json = new JSONObject(message);
                Answer answer = GPTApi.parseAnswer(json);
                synchronized (jsonInfo.history) {
                    answer.getMessages().forEach( output -> jsonInfo.history.add(new InputMessage(output.getRole(), output.getMessage())));
                }
                answerOptional = Optional.of(GPTApi.parseAnswer(json));
            } else {
                throw new BadRequestException(response.body().string());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(save != null) {
            synchronized (save) {
                try {
                    ConfigManager.tryToSave(this);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
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


    public static class Builder {
        private String folderID = "";
        private String iAmToken = "";
        private String oAuth = "";
        private int maxTokens = 300;
        private double temperature = 0.3d;
        private List<InputMessage> history = new ArrayList<>();
        private String serverPrompt = "";
        private Configuration configuration = null;


        public Builder folderID(String folderID) {
            this.folderID = folderID;
            return this;
        }
        public Builder iAmToken(String iAmToken) {
            this.iAmToken = iAmToken;
            return this;
        }
        public Builder oAuth(String oAuth) {
            this.oAuth = oAuth;
            return this;
        }
        public Builder maxTokens(int amount) {
            this.maxTokens = amount;
            return this;
        }
        public Builder temperature(double amount) {
            this.temperature = amount;
            return this;
        }
        public Builder history(List<?> history) {
            if(history == null) return this;

            Pattern patternRole = Pattern.compile("\\[role:(system|user|assistant)]");
            Pattern patternMessage = Pattern.compile("\\[message:([^]]+)]");

            List<InputMessage> messages = new ArrayList<>();
            history.forEach(s -> {
                if(s instanceof String check) {
                    Matcher matcherRole = patternRole.matcher(check);
                    Matcher matcherMessage = patternMessage.matcher(check);

                    if (matcherRole.find() && matcherMessage.find()) {
                        String role = matcherRole.group(1);
                        String messageText = matcherMessage.group(1);
                        messages.add(new InputMessage(role,messageText));
                    }
                } else if(s instanceof InputMessage) {
                    messages.add((InputMessage) s);
                }
            });
            this.history = messages;
            return this;
        }

        public Builder serverPrompt(String message) {
            this.serverPrompt = message;
            return this;
        }
        public Builder configuration(File file) {
            this.configuration = new Configuration(file);
            return this;
        }
        public Builder configuration(String path) {
            this.configuration = new Configuration(path);
            return this;
        }
        public YandexGPT build() {
            return new YandexGPT(folderID,iAmToken,oAuth,maxTokens,temperature,history,serverPrompt,configuration);
        }
    }

    @AllArgsConstructor
    public static class JSONInfo {
        public final int maxTokens;
        public final double temperature;
        public final List<InputMessage> history;
    }


}
