package com.mygdx.behavior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.mygdx.animation.Movie;
import com.mygdx.animation.SpriteAnimation;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class EntitySpawn extends Behavior {

    public EntitySpawn(WorldValues.BehaviorValues values) {
        super(values, EntitySpawn.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        parent.setVisible(false);
    }

    @Override
    public void postCreate() {
        parent.setTexture(null);
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures//FX//entity-spawn.atlas"));
        parent.getRootValues().animation = new SpriteAnimation();
        parent.getRootValues().animation.addMovie("root", new Movie(10, atlas, "s1", "s2", "s3", "s4", "s5", "s6", "s7"));
        parent.setVisible(true);
    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {

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
