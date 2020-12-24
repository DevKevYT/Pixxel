package com.mygdx.items;

import java.util.ArrayList;

public interface ItemValues {

    public class ItemLibrary {
        public ArrayList<ItemData> list = new ArrayList<>();
    }

    public class Animation {
        public int frameRate = 10;
        public String[][] regions; //Explained in local/textures/player/README.txt

        public Animation copy() {
            Animation cpy = new Animation();
            cpy.frameRate = frameRate;
            cpy.regions = new String[regions.length][];

            for(int i = 0; i < regions.length; i++) {
                cpy.regions[i] = new String[regions[i].length];
                for(int j = 0; j  < regions[i].length; j++) {
                    cpy.regions[i][j] = regions[i][j];
                }
            }
            return cpy;
        }
    }

    public class LevelStats {
        public float shield_protection = 0; //Percent (100% would block 100% from the enemy)
        public int base_damage = 0; //For weapons
        public int crit_damage = 0;
        public float crit_chance = 0; //0 = 0% 1 = 100%
        public float arrow_speed_min = 3;
        public float arrow_speed_max = 3;
        public float arrow_tensing_time = 1; //1 Second

        public LevelStats copy() {
            LevelStats cpy = new LevelStats();
            cpy.shield_protection = shield_protection;
            cpy.base_damage = base_damage;
            cpy.crit_damage = crit_damage;
            cpy.crit_chance = crit_chance;
            cpy.arrow_speed_min = arrow_speed_min;
            cpy.arrow_speed_max = arrow_speed_max;
            cpy.arrow_tensing_time = arrow_tensing_time;
            return cpy;
        }
    }

    public class ItemEffect {
        public String id = "";
        public float attribute = 0;
        public float duration = 0;

        public ItemEffect copy() {
            ItemEffect cpy = new ItemEffect();
            cpy.id = id;
            cpy.attribute = attribute;
            cpy.duration = duration;
            return cpy;
        }
    }

    public class ItemData {  //Base information about an item
        public String name = "missing"; //The name to display
        public String id = "missing"; //The ID of the item, should be unique
        public String icon = ""; //Name of the icon file in the same directory
        public Rarity rarity = Rarity.COMMON;
        public int maxStack = 999;
        public float destroyTime = 3; //Seconds
        public int targetSlot = 0; //0, 1, 2, 3=main weapon If the target slot is 3, a use type needs to get implemented: SWORD or ITEM

        public UseType useType = UseType.HANDHELD;
        public Animation animation; //Explained in local/textures/player/README.txt
        public ArrayList<LevelStats> levelStats = new ArrayList<>(1);

        public String description = "";
        public ArrayList<ItemEffect> effects = new ArrayList<ItemEffect>();

        public ItemData() {
            //levelStats.add(new LevelStats());
        }

        public static ItemData copy(ItemData other) {
            ItemData cpy = new ItemData();
            cpy.name = other.name;
            cpy.id = other.id;
            cpy.icon = other.icon;
            cpy.rarity = other.rarity;
            cpy.maxStack = other.maxStack;
            cpy.useType = other.useType;
            cpy.targetSlot = other.targetSlot;
            cpy.animation = other.animation != null ? other.animation.copy() : null;
            cpy.levelStats.clear();
            for(LevelStats l : other.levelStats) cpy.levelStats.add(l.copy());
            cpy.description = other.description;
            for(ItemEffect e : other.effects) cpy.effects.add(e.copy());
            return cpy;
        }
    }

    enum UseType {
        SWORD,
        HANDHELD,
        BOW
    }

    enum Rarity {
        COMMON,
        RARE,
        LEGENDARY,
        MYSTIC
    }

    enum Category {
        SWORD, BOW, ITEM, CONSUMABLE
    }

    public class Recipe {
        public String targetID = "missing";
        public int xp = 0;
        public Category category = Category.SWORD;
        public ArrayList<Requirements> requirements = new ArrayList<>();
    }

    public class Requirements {
        public String itemID = "missing";
        public int amount = -1;
    }

    public class BlueprintValues {
        public ArrayList<Recipe> list = new ArrayList<>();
    }

    public class ItemCellData {  //What the player has in his inventory
        public int quantity = 1;
        public int level = 0;
        public String id = "missing";

        public static ItemCellData copy(ItemCellData other) {
            ItemCellData cpy = new ItemCellData();
            cpy.quantity = other.quantity;
            cpy.id = other.id;
            cpy.level = other.level;
            return cpy;
        }
    }
}
