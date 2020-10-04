package com.mygdx.items;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.mygdx.darkdawn.FileHandler;
import com.mygdx.darkdawn.Logger;

import java.util.ArrayList;

public class Blueprint {

    public static ArrayList<Blueprint> blueprintLibrary = new ArrayList<>();
    public ItemValues.Recipe recipe;

    public Blueprint(ItemValues.Recipe recipe) {
        this.recipe = recipe;
    }

    public static void loadBlueprints(FileHandle file) {
        Json reader = new Json();
        try {
            ItemValues.BlueprintValues blueprints = reader.fromJson(ItemValues.BlueprintValues.class, file);
            for(ItemValues.Recipe r : blueprints.list) {
                Blueprint b = new Blueprint(r);
                blueprintLibrary.add(b);
            }
        } catch(Exception e) {
            Logger.logError("ItemLoader", "Unable to load blueprint list: " + e.toString());
            e.printStackTrace();
        }
        Logger.logInfo("ItemLoader", "Blueprints loaded!");
    }
}
