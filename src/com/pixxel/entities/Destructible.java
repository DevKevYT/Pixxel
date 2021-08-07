package com.pixxel.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.RootValues;
import com.pixxel.objects.WorldValues;
import com.pixxel.particles.HitParticle;
import com.pixxel.particles.HitmarkerParticle;
import com.pixxel.particles.HitmarkerParticlePool;
import com.pixxel.objects.World;
import com.pixxel.objects.WorldObject;

public class Destructible extends Behavior implements EntityEvents {

    Sprite[] snippets;
    public int health = 0;
    public boolean immune = false;

    public Destructible(WorldValues.BehaviorValues values) {
        super(values, Destructible.class);
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
    public void onUpdate(com.pixxel.objects.World world, com.pixxel.objects.WorldObject object, float deltaTime) {

    }

    @Override
    public void onRemove() {
        parent.clearFixtures();
    }

    @Override
    public void drawOver(com.pixxel.objects.WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(com.pixxel.objects.WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void onHit(com.pixxel.objects.WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY) {
        if(immune) {
            world.particleSystem.addParticle(new HitmarkerParticle(parent.getX(), parent.getY(), new HitmarkerParticlePool("0", true)));
            return;
        }
        world.particleSystem.addParticle(new HitmarkerParticle(parent.getX(), parent.getY(), new HitmarkerParticlePool(String.valueOf(addHealth*-1), false)));
        health += addHealth;
        if(health > 0) return;
        snippets = new Sprite[3];
        Sprite original = parent.getRootValues().texture;
        int snippetBounds = original.getRegionWidth() / 4;
        for(int i = 0; i < snippets.length; i++) {
            TextureRegion region = new TextureRegion(original);
            region.setRegion(MathUtils.random(original.getRegionX()+1, original.getRegionX() + original.getRegionWidth()-snippetBounds), MathUtils.random(original.getRegionY()+1, original.getRegionY() + original.getRegionHeight()-snippetBounds), snippetBounds, snippetBounds);
            snippets[i] = new Sprite(region);
            WorldValues.WorldObjectValues values = new WorldValues.WorldObjectValues();
            values.id = "missing";
            values.x = parent.getX() + MathUtils.random(-parent.getSize().x *.5f, parent.getSize().x *.5f);
            values.y = parent.getY() + MathUtils.random(-parent.getSize().y *.5f, parent.getSize().y *.5f);
            com.pixxel.objects.WorldObject w = world.addObject(values);
            w.getRootValues().texture = snippets[i];
            w.setSize(new Vector2(parent.getSize().x / 6, parent.getSize().x / 6));
            w.setFixed(false);
            w.setYOff(-999);
            RootValues.Fixture f = new RootValues.Fixture();
            f.width = w.getSize().x;
            f.height = w.getSize().y;
            w.setFixture(f);
            w.setFilterData(parent.worldObjectValues.categoryBits, parent.worldObjectValues.maskBits, parent.worldObjectValues.groupIndex);
            w.addBehavior(new Behavior(new WorldValues.BehaviorValues(), null) {
                private float time = MathUtils.random(3);
                private float a = 1;
                public void onSave() {
                    world.destroyObject(w);
                }
                public void onCreate(com.pixxel.objects.WorldObject object) { }
                public void postCreate() { }
                public void onUpdate(World world, com.pixxel.objects.WorldObject object, float deltaTime) {
                    time += deltaTime;
                    if(time > 4) {
                        w.setBlendingColor(1, 1, 1, a);
                        a -= deltaTime*2;
                        if(a <= 0) world.removeObject(w);
                    }
                }
                public void onRemove() { }
                public void drawOver(com.pixxel.objects.WorldObject object, SpriteBatch batch) { }
                public void drawBehind(WorldObject object, SpriteBatch batch) {}
            });
        }
        world.particleSystem.addParticle(new HitParticle(parent.getX()-5, parent.getY()-5, 3f, null));

        if(parent.getTrigger() != null) {
            parent.getTrigger().customEvent("destroy");
        }
        world.removeObject(parent);
    }
}
