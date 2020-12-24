package com.mygdx.behavior;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

import java.util.ArrayList;

public class Chest extends Behavior {

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

        if(parent.getTrigger() == null) {
            trigger = new WorldValues.TriggerValues();
            if (!opened) trigger.keys.add(Input.Keys.F);
            trigger.messageText = "Open Chest";
            parent.setTrigger(trigger);
        } else {
            if(!opened) {
                parent.getTrigger().values.keys.clear();
                parent.getTrigger().values.keys.add(Input.Keys.F);
            }
            parent.getTrigger().values.messageText = "Open Chest";
        }

        if(!opened) parent.setTexture(parent.getRootValues().tileset.get(0).texture);
        else {
            parent.setTexture(parent.getRootValues().tileset.get(1).texture);
            parent.getTrigger().disable(true);
        }

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
                parent.getTrigger().disable(true);
            }
        }
        if(isOpen && !opened) {
            isOpen = false;
            parent.setTexture(parent.getRootValues().tileset.get(0).texture);
            parent.getTrigger().disable(false);
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
