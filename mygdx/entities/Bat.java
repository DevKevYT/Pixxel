package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.animation.Movie;
import com.mygdx.animation.SpriteAnimation;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;
import com.mygdx.particles.HitmarkerParticle;
import com.mygdx.particles.HitmarkerParticlePool;

public class Bat extends Behavior implements EntityEvents {

    private SpriteAnimation animation;
    private Entity entity;
    private SteeringEntity steering;
    private boolean idle = true;
    private float idleMovement = 0;

    public String target = "";
    private WorldObject targetObject = null;
    byte side = 0;
    private float hitStrech = 0;
    private float prevWidth = 0;

    public Bat(WorldValues.BehaviorValues values) {
        super(values, Bat.class);
    }

    @Override
    public void postCreate() {
        animation = new SpriteAnimation();
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures//entities//Bat//bat.atlas"));
        animation.addMovie("fly-right", new Movie(10, atlas, "bat-right1", "bat-right2", "bat-right3", "bat-right4"));
        animation.addMovie("fly-left", new Movie(10, atlas, "bat-left1", "bat-left2", "bat-left3", "bat-left4"));
        animation.addMovie("hit-left", new Movie(10, atlas, "bat-left1", "bat-hit-left", "bat-left3"));
        animation.addMovie("hit-right", new Movie(10, atlas, "bat-right1", "bat-hit-right", "bat-right3"));
        animation.addMovie("despawn", Entity.entityDespawn);
        animation.play("fly-right");
    }

    @Override
    public void onSave() {
        parent.clearFixtures();
    }

    @Override
    public void onCreate(WorldObject object) {
        setID("entity");
        Behavior n = parent.getBehavior("data");
        if(!(n instanceof Entity)) {
            parent.removeBehavior("data");
            WorldValues.BehaviorValues values = new WorldValues.BehaviorValues();
            values.classPath = Entity.class.getName();
            values.id = "data";
            Entity e = new Entity(values);
            e.health = 100;
            e.maxHealth = 100;
            e.barType = 0;
            parent.addBehavior(e);
            entity = e;
        } else entity = (Entity) n;

        if(!parent.hasHitbox()) {
            RootValues.Fixture body = new RootValues.Fixture();
            body.isCircle = true;
            body.width = 20;
            parent.setFixture(body);
        }
        parent.setFixedrotation(true);
        parent.setFixed(false);
        parent.setSize(new Vector2(parent.getWorld().getTileSizeNORM(), parent.getWorld().getTileSizeNORM()));
        prevWidth = parent.getSize().x;

        steering = new SteeringEntity(parent);
        findTarget();
    }

    private void findTarget() {
        if(!target.isEmpty()) {
            WorldObject[] targets = world.getObjectsByAddress(target);
            if(targets.length > 0) targetObject = targets[0];
            else target = "";
        }
    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        animation.update(Gdx.graphics.getFramesPerSecond());

        idleMovement += deltaTime;

        if(targetObject == null && !target.isEmpty()) {
            findTarget();
        }

        if(targetObject != null) {
            if (parent.dist(targetObject) < world.getTileSizeNORM() * 3 && idleMovement > 1) {
                steering.cancelMovement();
                steering.setPath(world.getGraph().findPath(parent.getPosition(), targetObject.getPosition()), 2, true);
                idle = false;
                idleMovement = 0;
            } else if(parent.dist(targetObject) > world.getTileSizeNORM()*4 && !idle) {
                idle = true;
                steering.cancelMovement();
            }
        } else {
            target = "";
            idle = true;
        }

        if(idle) {
                if(idleMovement > 5) {
                    steering.cancelMovement();
                    steering.targetOffset.set(-20, 20);
                    Vector2 randomPos = new Vector2(object.getPosition());
                    randomPos.add(new Vector2().setToRandomDirection().scl(world.getTileSizeNORM()*2));
                    steering.setPath(world.getGraph().findPath(object.getPosition(), randomPos), 1, true);
                    idleMovement = 0;
                }
        }

        if(animation.finished("despawn")) {
            parent.getWorld().removeObject(parent);
            return;
        }

        if(steering.isMoving()) {
            if (steering.currentTarget().data.x < parent.getX()) side = 0;
            else side = 1;
        }

        if(side == 0) animation.play("fly-left");
        else if(side == 1) animation.play("fly-right");

        if(hitStrech > 0) hitStrech -= deltaTime*5;
        object.getRootValues().values.size.x = prevWidth - hitStrech*15;

        parent.setTexture(animation.getCurrentFrame());
        steering.update();
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

    @Override
    public void onHit(WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY) {
        if(source != null || entity.health <= 0) {
            prevWidth = parent.getSize().x;
            hitStrech = 1;

            parent.applyForce(knockbackX, knockbackY);
            entity.health += addHealth;
            world.particleSystem.addParticle(new HitmarkerParticle(parent.getX(), parent.getY(), new HitmarkerParticlePool(-addHealth + "", false)));

            if(entity.health <= 0) {
                animation.removeMovie("despawn");
                animation.addMovie("despawn", Entity.entityDespawn);
                animation.playMajor("despawn", 1);
            } else {
                if(source.getX() < parent.getX()) {
                    animation.playMajor("hit-left", 1);
                    side = 0;
                } else {
                    animation.playMajor("hit-right", 1);
                    side = 1;
                }
            }
        }
    }
}
