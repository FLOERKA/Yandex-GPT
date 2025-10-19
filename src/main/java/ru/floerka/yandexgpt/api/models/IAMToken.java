package ru.floerka.yandexgpt.api.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class IAMToken {

    private final String token;
    private final Instant expireAt;
}
