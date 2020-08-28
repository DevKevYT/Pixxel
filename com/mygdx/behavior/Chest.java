package com.mygdx.behavior;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class Chest extends Behavior {

    public String command = "";
    private String prevCommand = null;
    public boolean opened = false; //To be able to reset the chest later (Even it seems a bit stupid...)
    private boolean isOpen = false;
    private WorldValues.TriggerValues trigger;

    public Chest(WorldValues.BehaviorValues values) {
        super(values, Chest.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        if(!parent.getRootValues().getID().equals("chest") && !parent.getRootValues().getID().equals("chest1")) {
            parent.removeBehavior(getID());
            return;
        }

        trigger = new WorldValues.TriggerValues();
        trigger.keys.add(Input.Keys.F);
        trigger.messageText = "Open";
        trigger.tileCheck = false;
        trigger.scr = command;
        parent.setTrigger(trigger);

        prevCommand = command;
        parent.setTexture(parent.getRootValues().tileset.get(0).texture);
        if(opened) isOpen = true;
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(parent.getTrigger() != null) {
            if (parent.getTrigger().isRunning() && !opened) { //Open the chest, once the trigger is triggered
                opened = true;
                isOpen = true;
                parent.setTexture(parent.getRootValues().tileset.get(1).texture);
                parent.setTrigger(null);
            } else {
                if (!prevCommand.equals(command)) {
                    parent.getTrigger().setScript(command);
                    trigger.scr = command;
                }
                prevCommand = command;
            }
        }
        if(isOpen && !opened) {
            isOpen = false;
            parent.setTexture(parent.getRootValues().tileset.get(0).texture);
            parent.setTrigger(null);
            parent.setTrigger(this.trigger);
        }
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {

    }
}
