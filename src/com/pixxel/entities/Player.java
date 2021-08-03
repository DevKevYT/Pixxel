package com.pixxel.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pixxel.objects.WorldValues;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.World;
import com.pixxel.objects.WorldObject;

class Player extends Behavior implements EntityEvents {

    public Player(WorldValues.BehaviorValues values) {
        super(values, Player.class);
    }

    @Override
    public void onHit(com.pixxel.objects.WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY) {

    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(com.pixxel.objects.WorldObject object) {

    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, com.pixxel.objects.WorldObject object, float deltaTime) {

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
