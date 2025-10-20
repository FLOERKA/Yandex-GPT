package ru.floerka.yandexgpt.utils;

import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;

public class Configuration extends YamlFile {

    public Configuration(File file) {
        super(file);
        try {this.createOrLoadWithComments();} catch (IOException e) {throw new RuntimeException(e);}
    }
    public Configuration(String path) {
        super(path);
        try {this.createOrLoadWithComments();} catch (IOException e) {throw new RuntimeException(e);}
    }
    public void cleanSave() {
        try {save();} catch (IOException e) {throw new RuntimeException(e);}
    }
    public void cleanLoad() {
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
