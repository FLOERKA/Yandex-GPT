package ru.floerka.yandexgpt.api;

import ru.floerka.yandexgpt.YandexGPT;
import ru.floerka.yandexgpt.api.models.InputMessage;
import ru.floerka.yandexgpt.utils.Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    public static void tryToSave(YandexGPT yandexGPT) throws NoSuchFieldException, IllegalAccessException {
        Field confGet = yandexGPT.getClass().getDeclaredField("save");
        confGet.setAccessible(true);
        Configuration configuration = (Configuration) confGet.get(yandexGPT);
        if(configuration == null) return;

        Field jsonInfoGet = yandexGPT.getClass().getDeclaredField("jsonInfo");
        jsonInfoGet.setAccessible(true);
        YandexGPT.JSONInfo jsonInfo = (YandexGPT.JSONInfo) jsonInfoGet.get(yandexGPT);
        List<String> messages = new ArrayList<>();
        jsonInfo.history.forEach( msg -> messages.add(format(msg)));

        configuration.set("history", messages);
        configuration.cleanSave();

    }

    public static String format(InputMessage message) {
        String text = message.getMessage().replace("\n", "");
        return "[role:"+message.getRole()+"][message:"+text+"]";
    }
}
