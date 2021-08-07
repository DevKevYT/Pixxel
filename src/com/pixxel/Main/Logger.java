package com.pixxel.Main;

import com.badlogic.gdx.Gdx;

/**Basc logger wrapper for LibGDX*/
public final class Logger {

    public static boolean LOG_INFO = true;
    public static boolean LOG_ERROR = true;

    public static void logInfo(String tag, String message) {
        if(LOG_INFO) Gdx.app.log(tag, ": [INFO] " + message);
    }

    public static void logError(String tag, String message) {
        if(LOG_ERROR) Gdx.app.error(tag, ": [ERROR] " + message);
    }

    public static void logSuccess(String tag, String message) {
        Gdx.app.debug(tag, ": [SUCCESS] " + message);
    }
}
