package com.pixxel.ui;

import com.badlogic.gdx.Input;

import java.util.ArrayList;

import javax.swing.GroupLayout;

public interface DialogValues {

    public class DialogStyle {
        public String drawableBackground = "default-dialog-background"; //Regions of the skin in the construcktor
        public String drawableTitleBackground = "default-dialog-title-background";
        public String drawableChoiceArrow = "default-choice-arrow";
        public String font = "default-font"; //Font from the config file
        public String id = "default";
        public int rows = 2;
        public float fontScale = 1;
        public float padX = 10;  //Same
        public float padY = 10;  //Pad from the corner of the screen/stage viewport
        public ArrayList<Frame> frames = new ArrayList<>();
    }

    public class Frame {
        public Aligments align = Aligments.BOTTOM;
        public Label text = new Label();
        public Label title = new Label();  //PadY gets ignored
        public Label choice = new Label();  //This is, where the options get displayed, text gets ignored
        public float titleArea = 3;  //Means the title background has 1/3 of the width of the whole frame
        public float speed = 25; //How fast the characters appear (im ms)
        public int escapeKey = Input.Keys.SPACE;  //Null
        public Option options;  //Null
    }

    public class Label {
        public float fontScale = 0;
        public float r = 1, g = 1, b = 1;
        public Aligments align = Aligments.LEFT;
        public Aligments textAlign = Aligments.TOPLEFT;
        public String text = "";
        public float padX = 10;
        public float padY = 10;
    }

    public class Option {
        public Aligments align = Aligments.CENTER;
        public Choice[] choice;
    }

    public class Choice {
        public String command = "";
        public DialogStyle dialog = null; //:)
        public String text;  //Pad wont hav any effect
    }

    public enum Aligments {
        TOP,
        TOPLEFT,
        TOPRIGHT,
        BOTTOM,
        BOTTOMLEFT,
        BOTTOMRIGHT,
        LEFT,
        RIGHT,
        CENTER;
    }
}
