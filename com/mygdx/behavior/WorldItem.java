package com.mygdx.behavior;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.mygdx.darkdawn.Logger;
import com.mygdx.entities.Entity;
import com.mygdx.entities.Player;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.ContactListener;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class WorldItem extends Behavior {

    float floatOffset = 0;
    float angle = 0;
    public String pickupTarget = "player";
    private WorldObject target;
    private float speed = 0;
    private Vector2 direction = new Vector2();
    private boolean spreaded = false;
    private Vector2 textureOffset = new Vector2();

    public int quantity = 0;
    public String itemID = "";
    public int level = 0;
    private Item item;

    public WorldItem(WorldValues.BehaviorValues values) {
        super(values, WorldItem.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        if(!itemID.isEmpty()) {
            item = Item.getItem(itemID);
        } else {
            object.getWorld().removeObject(object);
            return;
        }

        target = world.getObjectByAddress(pickupTarget);
        parent.setFilterData(1, new int[]{1, 2}, 0);
        RootValues.Fixture circle = new RootValues.Fixture();
        circle.isCircle = true;
        circle.width = 10;
        parent.setFixture(circle);
        parent.setFixed(false);
        world.addContactListener(new ContactListener(parent) {
            @Override
            public void beginContact(WorldObject object1, WorldObject object2, Contact contact) {
                if((object1.equals(parent) || object2.equals(parent)) && spreaded) {
                    WorldObject pickup = object1.equals(parent) ? object2 : object1;
                    Behavior b = pickup.getBehavior("entity");
                    if(b instanceof Player) {
                        if(item != null) {
                            Player p = (Player) b;
                            if(item.data.id.equals("xp")) {
                                p.inventory.giveXP(12);
                            } else {
                                ItemValues.ItemCellData data = new ItemValues.ItemCellData();
                                data.id = item.data.id;
                                data.level = level;
                                data.quantity = quantity;

                                if (p.inventory.give(data)) {
                                    world.removeObject(parent);
                                    parent.getHitboxBody().setLinearVelocity(0, 0);
                                } else {
                                    speed = 0;
                                    spreaded = false;
                                    return;
                                }
                            }
                        }
                        world.removeObject(parent);
                    }
                }
            }
            public void endContact(WorldObject object1, WorldObject object2, Contact contact) {}
            public void preSolve(WorldObject object1, WorldObject object2, Contact contact, Manifold oldManifold) {}
            public void postSolve(WorldObject object1, WorldObject object2, Contact contact, ContactImpulse impulse) {}
        });
        direction.setToRandomDirection();
        speed = MathUtils.random(4, 6);

        parent.setFixedrotation(true);
        parent.setTexture(null);
    }

    @Override
    public void postCreate() {
        RootValues.LightSource light = new RootValues.LightSource();
        light.a = 0.5f;
        if(item.data.rarity == ItemValues.Rarity.RARE) {
            light.r = 0;
            light.g = 0;
            light.b = 1;
        } else if(item.data.rarity == ItemValues.Rarity.LEGENDARY) {
            light.r = Color.PURPLE.r;
            light.g = Color.PURPLE.g;
            light.b = Color.PURPLE.b;
        } else if(item.data.rarity == ItemValues.Rarity.MYSTIC) {
            light.a = 0.6f;
            light.r = Color.GOLD.r;
            light.g = Color.GOLD.g;
            light.b = Color.GOLD.b;
        }

        if(item.data.id.equals("xp")) {
            light.r = 0;
            light.g = 1;
            light.b = 0;
            light.a = 0.7f;
        }

        light.dist = 30;
        light.rays = 20;
        light.xray = true;
        parent.createLight(light);
    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(parent.getLight() == null) return;
        floatOffset = MathUtils.sin(angle)*world.getScale()*world.getTileSizePX()*.5f;

        if(parent.getLight() != null) {
            parent.getLight().setPosition(parent.getX() * world.getScale(), parent.getY() * world.getScale() + floatOffset);
            parent.getLight().setDistance(MathUtils.sin(angle * 1.2f) * 5 * world.getScale() + 30 * world.getScale());
        }
        angle += deltaTime*MathUtils.PI2;
        if(angle > MathUtils.PI2) angle = 0;

        textureOffset.set(0, floatOffset);
        if(target != null) {
            if(speed > 0) {
                object.move(direction.x, direction.y, speed);
                if(!spreaded) {
                    speed *= 0.9f/(deltaTime*60);
                    if (speed < 0.5 * world.getScale()) {
                        speed = 0;
                        spreaded = true;
                    }
                }
            }

            if(spreaded) {
                if (target.dist(parent) < 1 * world.getTileSizeNORM()) {
                    if(speed < 10) speed += 0.2f;
                    direction.set(target.getPosition().sub(parent.getPosition()).nor());
                } else speed = 0;
            }
        }

        parent.getRootValues().setTextureOffset(textureOffset);
        parent.setTexture(item.loadIcon());
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {
        parent.drawCenter(batch, Entity.entityShadow, parent.getScaledX(world), parent.getScaledY(world) - parent.getSize().y*world.getScale()*.5f,
                parent.getSize().x*world.getScale()*.5f, Entity.entityShadow.getHeight()*world.getScale()*.5f);
    }
}
