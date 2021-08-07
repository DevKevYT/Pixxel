package com.pixxel.objects;

import java.util.ArrayList;

public interface GameValues {

    public class CustomData {
        public String currentWorld = "";
        public ArrayList<Object_> custom = new ArrayList<>();
    }

    public class Object_ {
        public String name = "";
        public String classPath = "";
        public Object object;
    }

    public class GameData {
        public CustomData custom = new CustomData();
        public ArrayList<String> worlds = new ArrayList<>();
        public ArrayList<String> assets = new ArrayList<>();
        public ArrayList<RootValues.Variable> hashData = new ArrayList<>();

        public GameData(){}
    }
}
