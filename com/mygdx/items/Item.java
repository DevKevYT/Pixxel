package com.mygdx.items;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.mygdx.behavior.WorldItem;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class Item {

    public static ArrayList<Item> itemLibrary = new ArrayList<>();
    public ItemValues.ItemData data; //Root data
    protected FileHandle file;
    private Sprite icon;

    public Item(ItemValues.ItemData data) {
        this.data = data;
        if(data.levelStats.isEmpty()) data.levelStats.add(new ItemValues.LevelStats()); //Add default levelstat, because level is always 0 at the beginning
    }

    public Item copy() {
        Item copy = new Item(ItemValues.ItemData.copy(data));
        copy.icon = this.icon;
        copy.file = file;
        return copy;
    }

    /**Checks, if the path is valid*/
    public FileHandle canLoadIcon() {
        if(data.icon.isEmpty()) return null;
        FileHandle checkFile = new FileHandle(file.parent().path() + "/" + data.icon);
        if(checkFile.exists()) return checkFile;
        else return null;
    }

    public Sprite loadIcon() {
        if(icon != null) return icon;
        FileHandle test = canLoadIcon();
        if(test == null) return null;
        else {
            icon =  new Sprite(new Texture(test));
            return icon;
        }
    }

    /**Loads a set of items from a JSON file in the format:
     * {list: [ {item1...},{item2...}]}*/
    public static void loadItemLibrary(FileHandle fileHandle) {
        Json reader = new Json();
        try {
            ItemValues.ItemLibrary lib = reader.fromJson(ItemValues.ItemLibrary.class, fileHandle);
            for(ItemValues.ItemData d : lib.list) {
                Item item = new Item(d);
                item.file = fileHandle;
                itemLibrary.add(item);
            }
        } catch(Exception e) {
            Logger.logError("ItemLoader", "Unable to load Item list: " + e.toString());
        }
        Logger.logInfo("ItemLoader", "Items loaded!");
    }

    public static WorldObject dropItem(Item item, ItemValues.ItemCellData cellData, World world, float x, float y) {
        WorldValues.WorldObjectValues values = new WorldValues.WorldObjectValues();
        values.x = x;
        values.y = y;
        values.id = "missing";
        WorldObject worlditem = world.addObject(values);
        WorldValues.BehaviorValues bvalues = new WorldValues.BehaviorValues();
        bvalues.id = "item";
        bvalues.classPath = WorldItem.class.getName();
        WorldItem behavior = new WorldItem(bvalues);
        behavior.level = cellData.level;
        behavior.quantity = cellData.quantity;
        behavior.itemID = item.data.id;
        worlditem.addBehavior(behavior);
        return worlditem;
    }

    public static Item getItem(String id) {
        for(Item i : itemLibrary) {
            if(i.data.id.equals(id)) return i;
        }
        return new Item(new ItemValues.ItemData());
    }
}
