package ru.floerka.yandexgpt.api.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@ToString
public class Answer {

    private final String modelVersion;
    private final int inputTokens;
    private final int answerTokens;
    private final int totalTokens;
    private final List<OutputMessage> messages;
}
