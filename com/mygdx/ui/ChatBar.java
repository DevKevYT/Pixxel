package com.mygdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.devkev.devscript.nativecommands.NativeLibrary;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.mygdx.darkdawn.GenerateScriptFunctions;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.objects.Game;

import java.util.ArrayList;

public class ChatBar extends Actor {

    private Game game;
    private Skin skin;
    public Stage stage;
    private boolean showed = false;

    public TextField inputLine;
    private ArrayList<String> history = new ArrayList<>();
    private int searchIndex = 0;
    public int maxLines = 15;
    private Label.LabelStyle lineStyle;
    private ArrayList<Line> lines = new ArrayList<>(0);
    public boolean autoWrap = true;
    private InputProcessor prev;
    Process executer = new Process(true);

    private class Line extends Label {
        private int timer = 0;
        int line = 0;
        private boolean timerDone = false;

        public Line(CharSequence text, LabelStyle style, float lineHeight) {
            super("", style);
            super.setHeight(lineHeight);
            setLineText(text.toString());
        }

        public void setLineText(String text) {
            setText(text);
            if (getPrefWidth() <= inputLine.getWidth()) setWidth(inputLine.getWidth());
            else setWidth(getPrefWidth());
        }

        public void setHeight(float height) {
            return;
        }

        public void updateTimer(float delta) {
            if (timer > Gdx.graphics.getFramesPerSecond() * 5 && !timerDone) {
                timerDone = true;
            } else timer++;
        }
    }

    public ChatBar(Game game, Skin skin) {
        this.game = game;
        this.skin = skin;
        stage = new Stage();

        Pixmap bg = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
        bg.setColor(0, 0, 0, .6f);
        bg.fill();

        TextField.TextFieldStyle style = new TextField.TextFieldStyle(skin.get("blanc", TextField.TextFieldStyle.class));
        style.background = new TextureRegionDrawable(new Texture(bg));
        inputLine = new TextField("", style);
        stage.addActor(inputLine);

        lineStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        lineStyle.background = new TextureRegionDrawable(new Texture(bg));

        bg.dispose();

        stage.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    if (inputLine.getText().startsWith("/")) {
                        executeScript(inputLine.getText().substring(1));
                    } else if (!inputLine.getText().isEmpty())
                        post("player", inputLine.getText(), true);
                    history.add(0, inputLine.getText());
                    if (history.size() > 9) history.remove(9);
                    inputLine.setText("");
                    searchIndex = -1;
                }

                if (keycode == Input.Keys.UP && !history.isEmpty()) {
                    if (searchIndex < history.size() - 1) searchIndex++;
                    inputLine.setText(history.get(searchIndex));
                    inputLine.setCursorPosition(inputLine.getText().length());
                }
                if (keycode == Input.Keys.DOWN) {
                    if (searchIndex > 1) {
                        searchIndex--;
                        inputLine.setText(history.get(searchIndex));
                        inputLine.setCursorPosition(inputLine.getText().length());
                    } else {
                        inputLine.setText("");
                        searchIndex = -1;
                    }
                }
                return super.keyDown(event, keycode);
            }
        });

        executer.addOutput(new Output() {
            @Override
            public void log(String s, boolean b) {
                post("", s, b);
            }

            @Override
            public void error(String s) {
                post("", s, true);
                Logger.logInfo("Script", s);
            }

            @Override
            public void warning(String s) {

            }
        });
        executer.includeLibrary(GenerateScriptFunctions.GenerateCommands.getLibrary());
    }

    public void executeScript(String script) {
        executer.maxRuntime = 1000;
        executer.removeVariable("world");
        executer.setVariable("world", game.stage, true, true);
        executer.removeVariable("selected");
        if(Main.debug.selectedObjects.size() == 1) executer.setVariable("selected", Main.debug.selectedObjects.get(0).object, false, true);
        executer.execute(script, false);
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        inputLine.setBounds(x, y, width, height);
    }

    public void post(String poster, String message, boolean newline) {
        if (!poster.isEmpty()) message = "<" + poster + "> " + message;

        if (!newline && !lines.isEmpty()) message = lines.get(lines.size() - 1).getText() + message;

        if (autoWrap) {
            GlyphLayout testLayout = new GlyphLayout();
            testLayout.setText(lineStyle.font, message);
            char[] chars = message.toCharArray();
            int startIndex = 0;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == '\n') {
                    startIndex = i;
                    continue;
                }

                testLayout.setText(lineStyle.font, message.substring(startIndex, i));
                if (testLayout.width >= inputLine.getWidth()) {
                    String start = message.substring(0, i);
                    String end = message.substring(i);
                    message = start + "\n" + end;
                    startIndex = i;
                }
                testLayout.reset();
            }
        }
        int linesInMessage = count(message, '\n') + 1;

        if(linesInMessage == 1 && !newline && !lines.isEmpty()) {
            lines.get(lines.size()-1).setLineText(message);
            return;
        }

        Line[] newlinestoadd = new Line[linesInMessage];

        String[] split = message.split("\n");
        for (int i = 0; i < newlinestoadd.length; i++) {
            newlinestoadd[i] = new Line(split[i], lineStyle, lineStyle.font.getData().lineHeight);
            newlinestoadd[i].line -= i;
            stage.addActor(newlinestoadd[i]);

            lines.add(newlinestoadd[i]);
            if (lines.size() > maxLines) {
                lines.get(0).remove();
                lines.remove(0);
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).line += linesInMessage;
            lines.get(i).setPosition(inputLine.getX(), inputLine.getY() + lines.get(i).line * lines.get(i).getHeight());
        }
    }

    private int count(String string, char c) {
        int count = 0;
        for (char s : string.toCharArray()) {
            if (s == c) count++;
        }
        return count;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stage.act(delta);
        stage.draw();

        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).updateTimer(delta);
            if (!showed && lines.get(i).timerDone) lines.get(i).setVisible(false);
        }
    }

    public boolean isAutoWrap() {
        return autoWrap;
    }

    public void setAutoWrap(boolean wrap) {
        wrap = autoWrap;
    }

    public void showCmd() {
        inputLine.setVisible(true);
        prev = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(stage);
        stage.setKeyboardFocus(inputLine);
        showed = true;

        for (Line l : lines) l.setVisible(true);
    }

    public void hideCmd() {
        inputLine.setVisible(false);
        if(prev != null) Gdx.input.setInputProcessor(prev);
        showed = false;
        for (Line l : lines) {
            if (l.timerDone) l.setVisible(false);
        }
    }

    public void setCmd(boolean flag) {
        if (flag) showCmd();
        else hideCmd();
    }

    public boolean isShowed() {
        return showed;
    }
}
