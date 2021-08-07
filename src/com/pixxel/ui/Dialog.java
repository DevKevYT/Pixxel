package com.pixxel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.pixxel.Main.Logger;
import com.pixxel.ui.DialogValues.DialogStyle;
import com.pixxel.ui.DialogValues.Frame;
import com.pixxel.ui.DialogValues.Aligments;

import java.util.ArrayList;

public class Dialog {
    private Skin skin;
    private InputProcessor prev = null;

    private Stage stage;
    public Group dialog;
    private Image bg;
    private Image arrow;
    private Image titleBg;
    private Label text;
    private Label title;
    private Label infoRow;
    private Label.LabelStyle labelStyle;

    private BitmapFont font;
    private ArrayList<CheckBox> choiceGroup;
    private Table choiceRow;

    //private ArrayList<Label> choice = new ArrayList<>();
    private int choiceIndex = 0;

    private int framesPassed = 0;
    private int index = 0;
    private volatile boolean writing = false;

    private int continueDesktop = Input.Keys.SPACE;
    private float x, y, w, h;

    private DialogStyle style;
    private int queueIndex = 0;
    private Vector3 mousePos = new Vector3();

    private boolean created = false;
    /***/
    public Dialog(Stage stage, Skin skin) {
        this.style = new DialogStyle();
        style.drawableTitleBackground = "";
        style.drawableChoiceArrow = "";
        style.drawableBackground = "";
        style.font = "";
        dialog = new Group();
        labelStyle = new Label.LabelStyle();
        labelStyle.fontColor = Color.WHITE;

        this.skin = skin;
        this.stage = stage;
        this.x = stage.getViewport().getScreenX();
        this.y = stage.getViewport().getScreenY();
        this.w = stage.getViewport().getScreenWidth();
        this.h = stage.getViewport().getScreenHeight();
        choiceGroup = new ArrayList<>();
        //choiceGroup.setMaxCheckCount(1);
       // choiceGroup.setMinCheckCount(1);
    }

    /**If background is null, the default- black background is loaded from a pixmap*/
    private void loadBackground(String drawable) {
        if(bg== null) {
            bg = new Image();
            dialog.addActor(bg);
        }
        bg.setAlign(Align.bottomLeft);
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                try {
                    Drawable loaded = skin.getDrawable(drawable);
                    bg.setDrawable(loaded);
                } catch(Exception e) {
                    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
                    pixmap.setColor(Color.BLACK);
                    pixmap.fill();
                    bg.setDrawable(new TextureRegionDrawable(new Texture(pixmap)));
                    pixmap.dispose();
                }
            }
        });
    }

    private void loadTitleBackground(String drawable) {
        if(titleBg == null) {
            titleBg = new Image();
            dialog.addActor(titleBg);
        }
        titleBg.setAlign(Align.bottomLeft);
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                try {
                    Drawable loaded = skin.getDrawable(drawable);
                    titleBg.setDrawable(loaded);
                } catch(Exception e) {
                    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
                    pixmap.setColor(Color.BLACK);
                    pixmap.fill();
                    titleBg.setDrawable(new TextureRegionDrawable(new Texture(pixmap)));
                    pixmap.dispose();
                }
            }
        });
    }

    private void loadChoiceArrow(String drawable) {
        if(arrow == null) {
            arrow = new Image();
            dialog.addActor(arrow);
        }
        arrow.setAlign(Align.bottomLeft);
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                try {
                    Drawable loaded = skin.getDrawable(drawable);
                    arrow.setDrawable(loaded);
                    arrow.setSize(20, 20);
                } catch(Exception e) {
                    Pixmap pixmap = new Pixmap(10, 10, Pixmap.Format.RGBA4444);
                    pixmap.setColor(1, 1, 1, 1);
                    pixmap.fillTriangle(0, 0, 0, 10, 10, 5);
                    arrow = new Image(new TextureRegionDrawable(new Texture(pixmap)));
                    arrow.setSize(20, 20);
                    pixmap.dispose();
                }
            }
        });
    }

    /**
     * Sets the dialog with the specified values. And realigns compoments, if nessesary
     */
    public void setDialog(DialogStyle style) {
        if(!style.drawableBackground.equals(this.style.drawableBackground)) loadBackground(style.drawableBackground);
        if(!style.drawableTitleBackground.equals(this.style.drawableTitleBackground)) loadTitleBackground(style.drawableTitleBackground);
        if(!style.drawableChoiceArrow.equals(this.style.drawableChoiceArrow)) loadChoiceArrow(style.drawableChoiceArrow);
        if(!style.font.equals(this.style.font)) {
            labelStyle.font = skin.getFont(style.font);
        }

        arrow.setVisible(false);

        if(text == null) {
            text = new Label("", labelStyle);
            text.setAlignment(Align.topLeft);
            text.setWrap(true);
            dialog.addActor(text);
        }
        text.setFontScale(style.fontScale);

        if(title == null) {
            title = new Label("", labelStyle);
            title.setAlignment(Align.topLeft);
            dialog.addActor(title);
        }
        title.setFontScale(style.fontScale);

        if(infoRow == null) {
            infoRow = new Label("", labelStyle);
            infoRow.setFontScale(style.fontScale);
            dialog.addActor(infoRow);
        }
        infoRow.setAlignment(Align.bottomRight);

        if(choiceRow == null) {
            choiceRow = new Table();
            dialog.addActor(choiceRow);
        }

        dialog.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        this.style = style;
        created = true;
    }

    public void realign(Frame frame) {
        if(!created) return;
        float lineHeight = text.getStyle().font.getLineHeight() * text.getFontScaleY() + 5;
        float dialogHeight = lineHeight * style.rows;

        title.setAlignment(toGdxAlign(frame.title.textAlign));
        text.setAlignment(toGdxAlign(frame.text.textAlign));

        //TODO keep height, when you have no choice components lul
        if (frame.align == Aligments.BOTTOM || frame.align == Aligments.BOTTOMRIGHT || frame.align == Aligments.BOTTOMLEFT) {
            bg.setBounds(x + style.padX, y + style.padY, w - style.padX * 2, dialogHeight + lineHeight + frame.text.padY + frame.choice.padY);
            float titleWidth = bg.getWidth() / frame.titleArea;
            titleBg.setBounds(bg.getX(), bg.getY() + bg.getHeight(), titleWidth, lineHeight);
            if (frame.align == Aligments.LEFT || frame.align == Aligments.BOTTOMLEFT)
                titleBg.setX(bg.getX());
            else if (frame.align == Aligments.RIGHT || frame.align == Aligments.BOTTOMRIGHT)
                titleBg.setX(bg.getX() + bg.getWidth() - titleWidth);
            else if (frame.align == Aligments.CENTER) titleBg.setX(bg.getX() + titleWidth);
        } else if (frame.align == Aligments.TOP || frame.align == Aligments.TOPLEFT || frame.align == Aligments.TOPRIGHT) {
            bg.setBounds(x + style.padX, y + h - style.padY - dialogHeight - lineHeight - frame.text.padY - frame.choice.padY, w - style.padX * 2, dialogHeight + lineHeight + frame.text.padY + frame.choice.padY);
            float titleWidth = bg.getWidth() / frame.titleArea;
            titleBg.setBounds(bg.getX(), bg.getY() - lineHeight, titleWidth, lineHeight);
            if (frame.align == Aligments.LEFT || frame.align == Aligments.BOTTOMLEFT)
                titleBg.setX(bg.getX());
            else if (frame.align == Aligments.RIGHT || frame.align == Aligments.BOTTOMRIGHT)
                titleBg.setX(bg.getX() + bg.getWidth() - titleWidth);
            else if (frame.align == Aligments.CENTER) titleBg.setX(bg.getX() + titleWidth);
        } else if (frame.align == Aligments.LEFT) {
            bg.setBounds(x + style.padX, y + style.padY, dialogHeight + lineHeight, h - style.padY * 2 - lineHeight);
            titleBg.setBounds(bg.getX(), bg.getY() + bg.getHeight(), bg.getWidth(), lineHeight);
        } else if (frame.align == Aligments.RIGHT) {
            bg.setBounds(x + w - style.padX - dialogHeight - lineHeight, y + style.padY, dialogHeight + lineHeight, h - style.padY * 2 - lineHeight);
            titleBg.setBounds(bg.getX(), bg.getY() + bg.getHeight(), bg.getWidth(), lineHeight);
        }
        infoRow.setBounds(bg.getX() + frame.choice.padX, bg.getY() + frame.choice.padY, bg.getWidth() - frame.choice.padX * 2, lineHeight - +frame.choice.padY * 2);
        text.setBounds(bg.getX() + frame.text.padX, bg.getY() + frame.text.padY, bg.getWidth() - frame.text.padX * 2, bg.getHeight() - frame.text.padY * 2);
        title.setBounds(titleBg.getX() + frame.title.padX, titleBg.getY(), titleBg.getWidth() - frame.title.padX * 2, titleBg.getHeight()-3f);

        choiceRow.setBounds(bg.getX() + style.padX / 2, bg.getY() + style.padY / 2, bg.getWidth() - style.padX, 30);
    }

    /**
     * Triggers the dialog, witch is set by the dialog values at
     * {@link Dialog#setDialog(DialogValues)}<br>
     * Visibillity is automatically set to true
     */
    public void display() {
        if(!created) return;

        prev = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(this.stage);

        choiceRow.clearChildren();
        choiceRow.clear();
        dialog.setVisible(true);
        if (!this.style.frames.isEmpty()) {
            queueIndex = 0;
            realign(style.frames.get(queueIndex));
            title.setText(style.frames.get(queueIndex).title.text);
        } else return;
        choiceIndex = 0;
        infoRow.setText("");
        writing = true;
        text.setText("");
        choiceGroup.clear();
        arrow.setVisible(false);
        dialog.clearActions();
        dialog.addAction(new Action() {
            public boolean act(float delta) {
            update(delta);
            return false;
            }
        });
    }

    public void hide() {
        choiceRow.clearChildren();
        choiceRow.clear();
        dialog.setVisible(false);
        dialog.clearActions();
        Gdx.input.setInputProcessor(prev);
        prev = null;
    }

    /**End the curren frame, display the whole text and show options*/
    private void endFrame() {
        if(!created) return;
        arrow.setVisible(false);

        if (style.frames.get(queueIndex).options != null) {
            //Adding choice components
            choiceRow.clearChildren();
            choiceRow.clear();
            CheckBox.CheckBoxStyle cstyle = new CheckBox.CheckBoxStyle();
            cstyle.checkboxOff = null;
            cstyle.checkboxOn = arrow.getDrawable();
            cstyle.font = labelStyle.font;
            for (int i = 0; i < style.frames.get(queueIndex).options.choice.length; i++) {
                CheckBox choice = new CheckBox(style.frames.get(queueIndex).options.choice[i].text, cstyle);
                choice.getImageCell().padRight(10);
                Cell cell = choiceRow.add(choice).width(choiceRow.getWidth() / style.frames.get(queueIndex).options.choice.length).align(Align.center).fillY().align(Align.center);
                choiceGroup.add(choice);

                final int index = i;
                choice.setName("fire");
                choice.addListener(new ChangeListener() {
                    final int choiceIndex = index;
                    public void changed(ChangeEvent event, Actor actor) {
                        if(!choice.getName().equals("fire")) return;
                        if(choice.isChecked()) {
                            for(CheckBox c : choiceGroup) {
                                if(!c.equals(choice)) {
                                    c.setName("");
                                    c.setChecked(false);
                                    c.setName("fire");
                                }
                            }
                        }
                        if(!choice.isChecked()) {
                            Logger.logInfo("", "closing");
                            if(style.frames.get(queueIndex).options.choice[choiceIndex].dialog != null) {
                                setDialog(style.frames.get(queueIndex).options.choice[choiceIndex].dialog);
                                display();
                                return;
                            } else hide();
                        }
                    }});
                if(i == 0) choice.setChecked(true);
            }
        } else {
            if (queueIndex + 1 < style.frames.size()) infoRow.setText("next");
            else infoRow.setText("close");
        }

        text.setText(style.frames.get(queueIndex).text.text);
        framesPassed = 0;
        index = 0;
        writing = false;
        realign(style.frames.get(queueIndex));
    }

    public void update(float delta) {
        if(!created) return;
        if (queueIndex < style.frames.size() && writing) {
            if (style.frames.get(queueIndex).speed > 0) {
                float passedTime = (1f / (float) Gdx.graphics.getFramesPerSecond()) * framesPassed * 1000;
                framesPassed++;
                if (passedTime > style.frames.get(queueIndex).speed) {
                    int charsToAdd = (int) (passedTime / style.frames.get(queueIndex).speed);
                    if (index + charsToAdd < style.frames.get(queueIndex).text.text.length()) {
                        while (style.frames.get(queueIndex).text.text.substring(index, index + charsToAdd).equals(" "))
                            charsToAdd++;
                        text.setText(text.getText() + style.frames.get(queueIndex).text.text.substring(index, index + charsToAdd));
                        index += charsToAdd;
                        framesPassed = 0;
                    } else endFrame();
                }
            } else endFrame();
        }

        //Choice handling
        if (queueIndex < style.frames.size()) {
            if ((Gdx.input.isKeyJustPressed(style.frames.get(queueIndex).escapeKey) || Gdx.input.justTouched()) && choiceGroup.isEmpty()) {
                if (!writing) {
                    queueIndex++;
                    if (queueIndex < style.frames.size()) {
                        realign(style.frames.get(queueIndex));
                        writing = true;
                        infoRow.setText("");
                        text.setText("");
                        title.setText(style.frames.get(queueIndex).title.text);
                    } else hide();
                } else endFrame();
            }
        }
    }

    public boolean writing() {
        return writing;
    }

    public int toGdxAlign(Aligments align) {
        if (align == Aligments.BOTTOM) return Align.bottom;
        if (align == Aligments.BOTTOMRIGHT) return Align.bottomRight;
        if (align == Aligments.BOTTOMLEFT) return Align.bottomLeft;
        if (align == Aligments.TOP) return Align.top;
        if (align == Aligments.TOPRIGHT) return Align.topRight;
        if (align == Aligments.TOPLEFT) return Align.topLeft;
        if (align == Aligments.LEFT) return Align.left;
        if (align == Aligments.RIGHT) return Align.right;
        if (align == Aligments.CENTER) return Align.center;
        return Align.topLeft;
    }
}
