package ru.floerka.yandexgpt.api.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class InputMessage {

    private final String role;
    private final String message;
}
