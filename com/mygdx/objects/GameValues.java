package com.mygdx.objects;

import java.util.ArrayList;

public interface GameValues {

    public class CustomData {  //Data can be inventory storage data across worlds etc...
        public String currentWorld = "";
        public ArrayList<Object_> custom = new ArrayList<>();
    }

    public class Object_ {
        public String name = ""; //Imaginable as the "Variable name"
        public String classPath = ""; //The full class name e.g. com.mygdx.items.ItemData
        public Object object; //The actual Java object
    }

    public class GameData {
        public CustomData custom = new CustomData();
        public ArrayList<String> worlds = new ArrayList<>();
        public ArrayList<String> root = new ArrayList<>();
        public ArrayList<RootValues.Variable> hashData = new ArrayList<>();

        public GameData(){}
    }
}
