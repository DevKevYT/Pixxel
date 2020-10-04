package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.animation.Movie;
import com.mygdx.animation.SpriteAnimation;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;
import com.mygdx.particles.HitmarkerParticle;
import com.mygdx.particles.HitmarkerParticlePool;

import java.util.ArrayList;

public class Eater extends Behavior implements EntityEvents {

    public boolean dead = false;

    SpriteAnimation animation;

    private SteeringEntity steering;
    public String target = "";
    private WorldObject tobj;
    private boolean attacking = false;
    private float meleeCooldown = 0; //Seconds
    private ArrayList<WorldObject> tracked = new ArrayList<>();

    private float hitStrech = 0;
    private float prevWidth = 0;

    public Eater(WorldValues.BehaviorValues values) {
        super(values, Eater.class);
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures/entities/eater/eater.atlas"));

                animation = new SpriteAnimation();
                animation.addMovie("idle-left", new Movie(1, atlas, "idle-left"));
                animation.addMovie("walk-left", new Movie(9, atlas, "walk-left1", "walk-left2", "walk-left3", "walk-left4", "walk-left5"));
                animation.addMovie("walk-right", new Movie(9, atlas, "walk-right1", "walk-right2", "walk-right3", "walk-right4", "walk-right5"));
                animation.addMovie("attack-right", new Movie(9, atlas, "attack-right1", "attack-right2", "attack-right3", "attack-right4"));
                animation.addMovie("hit-left", new Movie(10, atlas, "hit-left1", "hit-left2", "hit-left1"));
                animation.addMovie("hit-right", new Movie(10, atlas, "hit-right1", "hit-right2", "hit-right1"));
                animation.addMovie("death", Entity.entityDespawn);
                animation.play("idle-left");
            }
        });
    }

    @Override
    public void onSave() {
        parent.getRootValues().values.fixtures = null;
        parent.worldObjectValues.change.fixtures = null;
    }

    @Override
    public void onCreate(WorldObject object) {
        object.setFixed(false);
        if(object.worldObjectValues.change.fixtures != null) object.worldObjectValues.change.fixtures.clear();
        RootValues.Fixture head = new RootValues.Fixture();
        head.height = 9;
        head.width = 20;
        head.yOff = -8;
        RootValues.Fixture body = new RootValues.Fixture();
        body.isCircle = true;
        body.width = 18;
        body.yOff = -4;
        body.xOff = 4;
        parent.setFixture(body, head);
        steering = new SteeringEntity(super.parent);

        if(parent.getBehavior("data") == null) {
            WorldValues.BehaviorValues values = new WorldValues.BehaviorValues();
            values.id = "data";
            values.classPath = "com.mygdx.entities.Entity";
            Entity entity = new Entity(values);
            entity.maxHealth = 300;
            entity.health = 300;
            parent.addBehavior(entity);
            entity.name = "SPIDER";
        } else ((Entity) parent.getBehavior("data")).name = "SPIDER";
        parent.setFixedrotation(true);
        parent.setSize(new Vector2(world.getTileSizeNORM(), world.getTileSizeNORM()));
        prevWidth = parent.getSize().x;


    }

    @Override
    public void postCreate() {

    }

    boolean knocked = false;
    float timeOffset = 0.5f;
    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(animation == null) return;
        if(Gdx.graphics.getFramesPerSecond() > 0 && !attacking && !dead && !target.isEmpty()) {
            WorldObject[] t = world.getObjectsByAddress(target);
            if (t.length > 0) tobj = t[0];

            if (Gdx.graphics.getFrameId() % (int) (Gdx.graphics.getFramesPerSecond()*timeOffset) == 0 && !target.isEmpty() && !attacking) {
                timeOffset = MathUtils.random(0.2f, 1f);
                if(tobj != null) {
                    steering.setPath(world.getGraph().findPath(parent.getPosition(), tobj.getPosition()), 2.5f, true);
                }
            }
        }

        meleeCooldown += 1;

        if(animation.getCurrentId().equals("attack-right") && attacking && !knocked && !dead) {
            if(animation.getFrameIndex() == 3) {
                tracked.clear();
                parent.getWorld().getObjectsInRadius(parent.getX(), parent.getY(), 1, true, tracked);
                for(WorldObject obj : tracked) {
                    if(obj.dist(parent) < 30) {
                        if (!obj.isFixed() && obj.getBehavior("entity") instanceof EntityEvents) {
                            final Vector2 direction = obj.getPosition().sub(parent.getPosition()).nor().scl(20);
                            obj.invokeMethod(Player.class, "onHit", parent, -170, direction.x, direction.y);
                        }
                    }
                }
                knocked = true;
            }
        }

        if(animation.finished("attack-right") && !dead) {
            knocked = false;
            attacking = false;
            steering.setPath(world.getGraph().findPath(parent.getPosition(), tobj.getPosition()), 1.5f, true);
            meleeCooldown = 0;
        }

        if(tobj != null && !attacking && meleeCooldown > Gdx.graphics.getFramesPerSecond() && !dead) {
            if(tobj.dist(parent) < world.getTileSizeNORM()) {
                attacking = true;
                meleeCooldown = 0;
                steering.cancelMovement();
                animation.play("attack-right");
            }
        }

        if(steering.isFinished() && tobj != null && attacking) {
            parent.moveTo(tobj.getX(), tobj.getY(), attacking ? 3 : 1.5f);
        }

        if((steering.isMoving() || parent.moved()) && tobj != null && !attacking) {
            if(steering.currentTarget() == null ? tobj.getX() < parent.getX() : steering.currentTarget().data.x < parent.getX()) animation.play("walk-left");
            if(steering.currentTarget() == null ? tobj.getX() > parent.getX() : steering.currentTarget().data.x > parent.getX()) animation.play("walk-right");
        } else {
            if(!attacking && !dead) animation.play("idle-left");
        }

        if(dead && animation.finished("death")) parent.getWorld().removeObject(parent);

        if(hitStrech > 0) hitStrech -= deltaTime*5;
        if(!dead) steering.update();
        animation.update(Gdx.graphics.getFramesPerSecond());
        object.getRootValues().values.size.x = prevWidth - hitStrech*15;

        object.setTexture(animation.getCurrentFrame());
    }

    @Override
    public void onRemove() {
        ItemValues.ItemCellData data = new ItemValues.ItemCellData();
        data.quantity = 1;
        data.id = "iron_nugget";
        data.level = 0;
        Item.dropItem(Item.getItem("iron_nugget"), data, world, parent.getX(), parent.getY());
    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void onHit(WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY) {
        if(source != null) {
            world.particleSystem.addParticle(new HitmarkerParticle(parent.getX(), parent.getY(), new HitmarkerParticlePool(String.valueOf(addHealth*-1), false)));
            parent.applyForce(knockbackX, knockbackY);
            parent.invokeMethod(Entity.class, "hit", addHealth);
            attacking = false;
            knocked = false;
            meleeCooldown = 0;
            hitStrech = 1;
            prevWidth = world.getTileSizeNORM();
            if((int) Behavior.getVariable(parent.getBehavior("data"), "health") <= 0) {
                tracked.clear();
                dead = true;
                steering.cancelMovement();
                parent.setFixed(true);
                hitStrech = 0;
                //parent.clearFixtures();
                animation.removeMovie("death");
                animation.addMovie("death", Entity.entityDespawn);
                animation.playMajor("death", 1);
            } else  {
                if(source.getX() < parent.getX()) animation.playMajor("hit-left", 1);
                else animation.playMajor("hit-right", 1);
            }
        }
    }
}
