package ru.floerka.yandexgpt;

import ru.floerka.yandexgpt.api.models.Answer;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        YandexGPT yandexGPT = new YandexGPT("b1gvq2emg6lmb4n597tu",
                "t1.9euelZqey86LnciJzJPGmcuXyZPHyO3rnpWaypmQjI6WmcqXzsqal5yRmYrl9PcZaCs4-e8WGmuv3fT3WRYpOPnvFhprr83n9euelZqcnM7NmIqLmpqQkZLMlZiZlu_8xeuelZqcnM7NmIqLmpqQkZLMlZiZlg.N--oUv3AdX-lNFHWfBp2KklJuBFthDwsCXbaoatAm9fTKp3XnPcdrjWI06ZIOtnyMkRhCdkmldKX74VnkURQBQ",
                500, 0.3, new ArrayList<>(), "Отвечай одним лаконичным словом");
        Answer answer = yandexGPT.tryMessage("Ты меня любишь?").get();
        System.out.println(answer.toString());

        System.out.println();

    }
}
