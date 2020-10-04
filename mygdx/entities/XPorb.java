package com.mygdx.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.ContactListener;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class XPorb extends Behavior {

    WorldObject player;
    public String target = "player";
    private float speed = 0;
    private Vector2 direction;
    private boolean spreaded = false;

    public XPorb(WorldValues.BehaviorValues values) {
        super(values, XPorb.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        WorldObject[] objects = world.getObjectsByAddress(target);
        if(objects.length > 0) {
            player = objects[0];
            world.addContactListener(new ContactListener(parent) {
                @Override
                public void beginContact(WorldObject object1, WorldObject object2, Contact contact) {
                    if(object1.equals(parent) ||object2.equals(parent)) {
                        WorldObject collide = object1.equals(parent) ? object2 : object1;
                        if(collide.getAddress().equals(target)) {
                            world.removeObject(parent);
                        }
                    }
                }

                @Override
                public void endContact(WorldObject object1, WorldObject object2, Contact contact) {

                }

                @Override
                public void preSolve(WorldObject object1, WorldObject object2, Contact contact, Manifold oldManifold) {

                }

                @Override
                public void postSolve(WorldObject object1, WorldObject object2, Contact contact, ContactImpulse impulse) {

                }
            });
        }
        object.setFixed(false);
        direction = new Vector2();
        direction.setToRandomDirection();
        speed = MathUtils.random(4, 6);
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        parent.setRotation(parent.getRotation() + 1);
        if(player != null) {
            if(speed > 0) {
                object.move(direction.x, direction.y, speed);
                if(!spreaded) {
                    speed *= 0.9f;
                    if (speed < 0.5 * world.getScale()) {
                        speed = 0;
                        spreaded = true;
                    }
                }
            }

            if(spreaded) {
                if (player.dist(parent) < 4 * world.getTileSizeNORM()) {
                    if(speed < 5) speed += 0.2f;
                    direction.set(player.getPosition().sub(parent.getPosition()).nor());
                } else speed = 0;
            }
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
