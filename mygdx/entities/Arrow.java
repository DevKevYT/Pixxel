package com.mygdx.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.ContactListener;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

import java.lang.reflect.InvocationTargetException;

public class Arrow extends Behavior {

    public Vector2 direction = new Vector2();
    public float speed = 0;
    private boolean hit = false;
    private float hitTimer = 0;
    private float maxTime = 10;
    public WorldObject source;

    private WorldObject stuckOn;
    private Entity entity;
    private Vector2 impactPos = new Vector2();
    public int damage = 0;
    public Arrow(WorldValues.BehaviorValues values) {
        super(values, Arrow.class);
    }

    @Override
    public void onSave() {
        world.removeObject(parent);
    }

    @Override
    public void onCreate(WorldObject object) {
        object.setFixed(false);
        object.setYOff(-99);
        direction.nor();
        object.setPosition(object.getPosition().add(direction.scl(10)));
        direction.nor();
        world.addContactListener(new ContactListener(parent) {
           @Override
           public void beginContact(WorldObject object1, WorldObject object2, Contact contact) {
               if(hit) return;
               if(object1.equals(source) || object2.equals(source)) return;
               if(object1.equals(parent)) {
                   if(object2.getBehavior("arrow") != null) return;
               }
               if(object2.equals(parent)) {
                   if(object1.getBehavior("arrow") != null) return;
               }

               if((object1.equals(parent) || object2.equals(parent))) {
                   speed = 0;
                   hit = true;
                   hitTimer = 0;
                   maxTime = 10;
                   parent.getHitboxBody().setLinearVelocity(0, 0);
                   stuckOn = object1.equals(parent) ? object2 : object1;
                   try {
                       stuckOn.invokeMethod("entity", "onHit", source, -damage, direction.x * 6, direction.y * 6);
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
                   impactPos.set(parent.getPosition().sub(stuckOn.getPosition()));
                   world.shake.shake(0.5f, 0.5f, false);
                   entity = (Entity) stuckOn.getBehavior("data");
                   world.removeObject(parent);
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

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        hitTimer += deltaTime;
        if(hitTimer > maxTime) {
            world.removeObject(parent);
            return;
        }
        object.move(direction.x, direction.y, speed);
        object.setRotation((int) direction.angle() - 90);
        if(object.getX() < world.getOrigin().x || object.getX() > world.getWorldWidth() || object.getY() < world.getOrigin().y || object.getY() > world.getWorldHeight()) {
            world.removeObject(parent);
        }

        if(hit) {
            parent.clearFixtures();
            parent.setPosition(stuckOn.getX() + impactPos.x, stuckOn.getY() + impactPos.y);
            if(entity.health <= 0) world.removeObject(parent);
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
