package com.mygdx.behavior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.utils.Align;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.Trigger;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;
import com.mygdx.ui.DialogValues.*;

public class KeyTrigger extends Behavior {

    Group keyUI;
    Label keyInfo;
    Label keys;
    ProgressBar pressProgress;
    Trigger targetTrigger;

    private boolean keyPressed = false;
    private boolean released = false;
    private boolean done = false;
    private int targetKey;
    public KeyTrigger(WorldValues.BehaviorValues values) {
        super(values, KeyTrigger.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        Logger.logInfo("", "Creating UI...");
        keyUI = new Group();
        keyUI.setBounds(0, 20, world.game.gui.getWidth(), 40);
        keyInfo = new Label("", world.game.resource);
        keyInfo.setAlignment(Align.center);
        keyInfo.setBounds(0, 20, keyUI.getWidth(), 20);
        keyUI.addActor(keyInfo);

        keys = new Label("", world.game.resource);
        keys.setAlignment(Align.right);
        keys.setBounds(0, 0, keyUI.getWidth() / 2 - 30,20);
        keyUI.addActor(keys);

        pressProgress = new ProgressBar(0, 1, 0.05f, false, world.game.resource, "key-progress");
        pressProgress.setBounds(keyUI.getWidth() / 2 - 25, 0, 50, 20);
        keyUI.addActor(pressProgress);

        world.game.gui.addActor(keyUI);
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        keyUI.setVisible(false);
        targetKey = -1;
        targetTrigger = null;
        if(object.getWorld().game.dialog.dialog.isVisible()) return;
        for (int i = 0; i < world.getObjectsWithTrigger().size(); i++) {
            WorldObject trigger = world.getObjectsWithTrigger().get(i);
            if (trigger.getTrigger() != null) {
                if(!trigger.getTrigger().values.keys.isEmpty() && trigger.touched(object.getPosition()) && !trigger.getTrigger().isDisabled()) {
                    keyInfo.setText(trigger.getTrigger().values.messageText);
                    keys.setText(Input.Keys.toString(trigger.getTrigger().values.keys.get(0)));
                    keyUI.setVisible(true);
                    targetKey = trigger.getTrigger().values.keys.get(0);
                    targetTrigger = trigger.getTrigger();
                    break;
                }
            } else trigger.removeTrigger();
        }

        if(targetKey != -1) {
            if (Gdx.input.isKeyPressed(targetKey) && !done) {
                released = false;
                pressProgress.setValue(pressProgress.getValue() + deltaTime*2); //Half a second
                if(pressProgress.getValue() >= pressProgress.getMaxValue()) {
                    targetTrigger.keyTrigger();
                    pressProgress.setValue(0);
                    done = true;
                }
            } else if(!Gdx.input.isKeyPressed(targetKey)) {
                pressProgress.setValue(0);
                released = true;
                done = false;
            }
        } else pressProgress.setValue(0);
    }

    @Override
    public void onRemove() {
        keyUI.remove();
    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {
    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {
    }
}
