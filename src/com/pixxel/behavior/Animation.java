package com.pixxel.behavior;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.WorldValues;
import com.pixxel.objects.World;
import com.pixxel.objects.WorldObject;

public class Animation extends Behavior {

    public float tX = 0;
    public float tY = 0;
    public float speed = 0;
    public boolean moving = false;
    public boolean rotating = false;
    private com.pixxel.objects.WorldObject currentInvoker = null;

    public Animation(WorldValues.BehaviorValues values) {
        super(values, Animation.class);
    }

    /**Function to call, if an object should move to a specified position*/
    public void moveto(Float x, Float y, Float speed, com.pixxel.objects.WorldObject invoker, Boolean pauseTillDone) {
        if(moving || speed == 0) return;
        tX = x;
        tY = y;
        this.speed = speed;
        moving = true;
        currentInvoker = invoker;
        if(invoker != null && pauseTillDone) {
            if (invoker.getTrigger() != null) {
                invoker.getTrigger().process.pause(invoker.getTrigger().process.getMain());
            }
        }
    }

    /**Moves the object relative to its current position*/
    public void move(Float x, Float y, Float speed, com.pixxel.objects.WorldObject invoker, Boolean pauseTillDone) {
        if(speed == 0 || moving) return;
        tX = x + parent.getPosition().x;
        tY = y + parent.getPosition().y;
        this.speed = speed;
        moving = true;
        currentInvoker = invoker;
        if(invoker != null && pauseTillDone) {
            if (invoker.getTrigger() != null) {
                invoker.getTrigger().process.pause(invoker.getTrigger().process.getMain());
            }
        }
    }

    public void cancel() {
        moving = false;
        speed = 0;
        tX = 0;
        tY = 0;
        if(currentInvoker != null) {
            if (currentInvoker.getTrigger() != null)
                currentInvoker.getTrigger().process.wake(currentInvoker.getTrigger().process.getMain());
        }
    }

    @Override
    public void onSave() {
       if(moving) parent.setPosition(tX, tY);
       cancel();
    }

    @Override
    public void onCreate(com.pixxel.objects.WorldObject object) {
       // if(moving) object.setPosition(tX, tY);
       // moving = false;
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, com.pixxel.objects.WorldObject object, float deltaTime) {
        if(moving) {

            parent.setFixed(false);
            object.moveTo(tX, tY, speed);
            parent.setFixed(true);

            if(object.getPosition().dst(tX, tY) < speed) {
                object.setPosition(tX, tY);
                moving = false;
                speed = 0;
                tX = 0;
                tY = 0;
                if(currentInvoker != null) {
                    if (currentInvoker.getTrigger() != null)
                        currentInvoker.getTrigger().process.wake(currentInvoker.getTrigger().process.getMain());
                }
            }
        }
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void drawOver(com.pixxel.objects.WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {

    }
}
