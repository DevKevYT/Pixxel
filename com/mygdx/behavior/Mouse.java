package com.mygdx.behavior;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class Mouse extends Behavior {

    public String target = "player";
    private WorldObject obj;

    public Mouse(WorldValues.BehaviorValues values) {
        super(values, Mouse.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        obj = object.getWorld().getObjectsByAddress(target)[0];
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        object.setPosition(object.getWorld().getMousePos().x * object.getWorld().getPPM(), object.getWorld().getMousePos().y * object.getWorld().getPPM());
        object.setRotation((int) obj.getPosition().angle(object.getPosition()));
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
