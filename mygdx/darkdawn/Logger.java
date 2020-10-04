package com.mygdx.darkdawn;

import com.badlogic.gdx.Gdx;

public final class Logger {

    public static void logInfo(String tag, String message) {
        Gdx.app.log(tag, ": [INFO] " + message);
    }

    public static void logError(String tag, String message) {
        Gdx.app.error(tag, ": [ERROR] " + message);
    }

    public static void logSuccess(String tag, String message) {
        Gdx.app.debug(tag, ": [SUCCESS] " + message);
    }
}
