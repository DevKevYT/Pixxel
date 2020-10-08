package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.animation.Movie;
import com.mygdx.animation.SpriteAnimation;
import com.mygdx.behavior.Torch;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;
import com.mygdx.particles.HitParticle;
import com.mygdx.particles.HitmarkerParticle;
import com.mygdx.particles.HitmarkerParticlePool;

import java.util.ArrayList;

public class Player extends Behavior implements EntityEvents {

    SpriteAnimation animation;
    SpriteAnimation itemAnimations;
    private float speed = 1.5f;
    public float maxSpeed = 6;
    private byte side = 0; //0 = front 1 = left 2 = up 3 = right
    private boolean attacking = false;
    private boolean dashing = false;
    private float dashTime = 0;
    private Vector2 attackDirection = new Vector2();

    private ArrayList<WorldObject> attackList = new ArrayList<>(1);
    public PlayerInventory inventory;
    public Entity entity;

    boolean blocking = false;
    boolean bowTensing = false;
    private TextureAtlas itemAtlas;
    private boolean isDead = false;

    private ArrayList<Vector3> dashShadow = new ArrayList<>();

    public Player(WorldValues.BehaviorValues values) {
        super(values, Player.class);
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures/player/player.atlas"));
        itemAtlas = new TextureAtlas(Gdx.files.internal("textures/player/items21.atlas"));
        animation = new SpriteAnimation();
        animation.addMovie("idle-front", new Movie(1, atlas, "idle-front"));
        animation.addMovie("idle-left", new Movie(1, atlas,"idle-left"));
        animation.addMovie("idle-up", new Movie(1, atlas, "idle-up"));
        animation.addMovie("idle-right", new Movie(1, atlas, "idle-right"));

        animation.addMovie("walk-down", new Movie(8, atlas, "down1", "down2", "down3", "down4"));
        animation.addMovie("handheld-walk-down", new Movie(8, atlas, "handheld-front1", "handheld-front2", "handheld-front3", "handheld-front4"));

        animation.addMovie("walk-up", new Movie(8, atlas, "up1", "up2", "up3", "up4"));
        animation.addMovie("handheld-walk-up", new Movie(8, atlas, "handheld-up1", "handheld-up2", "handheld-up3", "handheld-up4"));

        animation.addMovie("walk-left", new Movie(8, atlas, "left1", "left2", "left3", "left4"));
        animation.addMovie("handheld-walk-left", new Movie(8, atlas, "handheld-left1", "handheld-left2", "handheld-left3", "handheld-left4"));

        animation.addMovie("walk-right", new Movie(8, atlas, "right2", "right3", "right4", "right1"));
        animation.addMovie("handheld-walk-right", new Movie(8, atlas, "handheld-right1", "handheld-right2", "handheld-right3", "handheld-right4"));

        animation.addMovie("melee-left", new Movie(15, atlas,"melee-left1", "melee-left2", "melee-left3", "melee-left4", "melee-left4"));
        animation.addMovie("melee-right", new Movie(15, atlas,"melee-right1", "melee-right2", "melee-right3", "melee-right4", "melee-right4"));
        animation.addMovie("hit-left", new Movie(10, atlas, "hit-left-1", "hit-left-0", "hit-left-1"));
        animation.addMovie("hit-right", new Movie(10, atlas, "hit-right-1", "hit-right-0", "hit-right-1"));

        animation.addMovie("block-right", new Movie(1, atlas, "block-right"));
        animation.addMovie("block-left", new Movie(1, atlas, "block-left"));

        animation.addMovie("bow-left", new Movie(3, atlas, "bow-left1", "bow-left2", "bow-left3"));
        animation.addMovie("bow-right", new Movie(3, atlas, "bow-right1", "bow-right2", "bow-right3"));
        animation.play("idle-front");

        itemAnimations = new SpriteAnimation();
    }

    @Override
    public void onSave() {
        inventory.saveInventory(world.game);
    }

    @Override
    public void onCreate(WorldObject object) {

        object.getRootValues().values.fixed = false;
        if(object.getHashWrapper().get("respawnX") != null) object.getHashWrapper().setString("respawnX", "20");
        if(object.getHashWrapper().get("respawnY") != null) object.getHashWrapper().setString("respawnY", "20"); //now, we know for sure they exist


        if(inventory == null) {
            inventory = new PlayerInventory(world.game, 30, 20);
            world.game.chat.inputLine.setY(110);

            inventory.setEquipListener((cell, targetSlot) -> {
                if (cell != null) {
                   if (targetSlot == 3) {
                       bowTensing = false;
                       blocking = false;
                       itemAnimations.removeMovie("handheld-down");
                       itemAnimations.removeMovie("handheld-left");
                       itemAnimations.removeMovie("handheld-up");
                       itemAnimations.removeMovie("handheld-right");
                       itemAnimations.removeMovie("sword-left");
                       itemAnimations.removeMovie("sword-right");
                       itemAnimations.stop();

                        if (cell.itemRoot.data.animation != null) {//Load the Item animation here...
                            if (cell.itemRoot.data.useType == ItemValues.UseType.SWORD) { //Like in /textures/player/README.txt, index 0 is left and index 1 is right
                                if (cell.itemRoot.data.animation.regions.length > 0) {
                                    String[] frames = cell.itemRoot.data.animation.regions[0];
                                    if (frames.length >= 4) {
                                        Movie m = new Movie(animation.getMovie("melee-left").frameRate);
                                        for (String frame : frames) m.addRegion(frame, itemAtlas);
                                        itemAnimations.addMovie("sword-left", m);
                                    }
                                }
                                if (cell.itemRoot.data.animation.regions.length >= 1) {
                                    String[] frames = cell.itemRoot.data.animation.regions[1];
                                    if (frames.length >= 4) {
                                        Movie m = new Movie(animation.getMovie("melee-right").frameRate);
                                        for (String frame : frames) m.addRegion(frame, itemAtlas);
                                        itemAnimations.addMovie("sword-right", m);
                                    }
                                }
                            } else if (cell.itemRoot.data.useType == ItemValues.UseType.HANDHELD) {
                                if (cell.itemRoot.data.animation.regions.length >= 4) {
                                    String[] names = new String[]{"handheld-down", "handheld-left", "handheld-up", "handheld-right"};
                                    for (int i = 0; i < 4; i++) {
                                        Movie movie = new Movie(cell.itemRoot.data.animation.frameRate);
                                        for (int j = 0; j < cell.itemRoot.data.animation.regions[i].length; j++) {
                                            movie.addRegion(cell.itemRoot.data.animation.regions[i][j], itemAtlas);
                                        }
                                        itemAnimations.addMovie(names[i], movie);
                                    }
                                }
                            }
                            if (cell.itemRoot.data.useType == ItemValues.UseType.BOW) {
                                if (cell.itemRoot.data.animation.regions.length >= 2) {
                                    String[] frames = cell.itemRoot.data.animation.regions[1];
                                    int frameRate = (int) (1 / cell.itemRoot.data.levelStats.get(cell.cellData.level).arrow_tensing_time * 3);
                                    if (cell.itemRoot.data.animation.regions[0].length >= 3)
                                        itemAnimations.addMovie("bow-tension-left", new Movie(frameRate, itemAtlas, cell.itemRoot.data.animation.regions[0][0], cell.itemRoot.data.animation.regions[0][1], cell.itemRoot.data.animation.regions[0][2]));
                                    if (frames.length >= 3)
                                        itemAnimations.addMovie("bow-tension-right", new Movie(frameRate, itemAtlas, frames[0], frames[1], frames[2]));
                                    animation.getMovie("bow-left").frameRate = frameRate;
                                    animation.getMovie("bow-right").frameRate = frameRate;
                                }
                            }
                        }
                        if (cell.itemRoot.data.id.equals("torch") && parent.getBehavior("torch") == null) {
                            WorldValues.BehaviorValues values = new WorldValues.BehaviorValues();
                            values.id = "torch";
                            values.classPath = "com.mygdx.behavior.Torch";
                            Torch torch = new Torch(values);
                            parent.addBehavior(torch);
                            torch.dist = 1;
                            torch.yOffset = -8;
                        } else if(!cell.itemRoot.data.id.equals("torch")) parent.removeBehavior("torch");
                    }
                }
                if (cell == null) parent.removeBehavior("torch");
                inventory.itemInfo.setDisplay(cell.itemRoot, cell);
                inventory.itemInfo.setVisible(false);
            });
            inventory.loadInventory(object.getWorld().game);
        }
        entity = (Entity) parent.getBehavior("data");
        if(entity == null) {
            WorldValues.BehaviorValues values = new WorldValues.BehaviorValues();
            values.id = "data";
            values.classPath = "com.mygdx.entities.Entity";
            entity = new Entity(values);
            entity.maxHealth = 1000;
            entity.health = 1000;
            parent.addBehavior(entity);
        }
        entity.barVisible = false;
        entity.name = "";
        inventory.healthbar.setValue((float) entity.health / (float) entity.maxHealth);
        if(entity.health < 0) isDead = true;
    }

    @Override
    public void postCreate() {

    }

    boolean attackPressed = false;
    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(isDead) {
            if(camRot <= 45) {
                camRot += deltaTime * 45;
                world.getViewport().rotate(deltaTime);
                world.getViewport().zoom -= 0.01f * deltaTime;
            }
            return;
        }

        boolean gotHit = animation.getCurrentId().equals("hit-left") ||  animation.getCurrentId().equals("hit-right");
        inventory.setVisible(!world.game.dialog.dialog.isVisible());


        if(animation.finished("melee-right") || animation.finished("melee-left") || gotHit) {
            attacking = false;
            attackPressed = false;
            itemAnimations.stop();
        }

        if (!world.game.dialog.dialog.isVisible() && !Main.debug.debugMode && !object.getWorld().game.chat.isShowed() && !inventory.isInventoryOpened()) {
            blocking = false;
            if(!(Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || attacking || attackPressed) && bowTensing) {
               if(itemAnimations.getFrameIndex() > 0) {
                   WorldValues.WorldObjectValues values = new WorldValues.WorldObjectValues();
                   values.x = parent.getX();
                   values.y = parent.getY() - 5;
                   values.id = "arrow";
                   WorldObject arrow = world.addObject(values);
                   WorldValues.BehaviorValues behaviorValues = new WorldValues.BehaviorValues();
                   behaviorValues.classPath = "com.mygdx.entities.Arrow";
                   behaviorValues.id = "arrow";
                   Arrow arr = new Arrow(behaviorValues);
                   arr.direction = new Vector2(world.getMousePos().scl(world.getPPM()).sub(parent.getPosition()));
                   arr.speed = inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).arrow_speed_min;
                   arr.source = parent;
                   arr.damage = inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).base_damage;
                   if (itemAnimations.getFrameIndex() == 2) {
                       arr.speed = inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).arrow_speed_max;
                       arr.damage = (int) (inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).base_damage * 1.2f);
                   }
                   arrow.addBehavior(arr);
                   arrow.setFixed(false);
                   arrow.setCategoryBit(1);
                   arrow.setGroupIndex(0);
                   ArrayList<Integer> maskBits = new ArrayList<>();
                   maskBits.add(1);
                   arrow.setMaskBits(maskBits);
                   arrow.getHitboxBody().setBullet(true);
               }
                bowTensing = false;
                itemAnimations.stop();
            } else if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !gotHit && !attacking && !attackPressed) {
                if(inventory.getEquippedItem().data.useType == ItemValues.UseType.SWORD && inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).shield_protection > 0) {
                    if (world.getMousePos().x <= parent.getX() * world.getScale())
                        animation.play("block-left");
                    else animation.play("block-right");
                    side = 3;
                    blocking = true;
                } else if(inventory.getEquippedItem().data.useType == ItemValues.UseType.BOW) {
                    if (world.getMousePos().x >= parent.getX() * world.getScale()) {
                        itemAnimations.playAt("bow-tension-right", itemAnimations.getCurrentId().equals("bow-tension-left") ? itemAnimations.getFrameCount() : 0);
                        animation.playAt("bow-right", animation.getCurrentId().equals("bow-left") ? animation.getFrameCount() : 0);
                    } else {
                        itemAnimations.playAt("bow-tension-left", itemAnimations.getCurrentId().equals("bow-tension-right") ? itemAnimations.getFrameCount() : 0);
                        animation.playAt("bow-left", animation.getCurrentId().equals("bow-right") ? animation.getFrameCount() : 0);
                    }
                    side = 3;
                    if (bowTensing && itemAnimations.getFrameIndex() == 2) {
                        animation.freeze();
                        itemAnimations.freeze();
                    }

                    bowTensing = true;
                }
            }

            boolean upPressed = false;
            boolean downPressed = false;
            boolean noKeyPressed = true;
            ItemValues.UseType useType = inventory.getEquippedItem().data.useType;
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !gotHit && !blocking && !bowTensing && (useType == ItemValues.UseType.SWORD ||useType == ItemValues.UseType.BOW)) {
                if (dashTime == 0) {
                    dashing = true;
                    cancelSwordAttack();
                } else cancelDash();
            }
            if (Gdx.input.isKeyPressed(Input.Keys.W) && !attackPressed && !attacking && !blocking) {
                side = 2;
                upPressed = true;
                noKeyPressed = false;
                object.move(0, 1, bowTensing ? speed*.4f : speed*.8f);
                if(!blocking && !bowTensing && useType != ItemValues.UseType.HANDHELD) animation.play("walk-up");
                else if(!bowTensing) {
                    animation.play("handheld-walk-up");
                    itemAnimations.play("handheld-up");
                }
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) && !attackPressed && !attacking && !blocking) {
                side = 0;
                downPressed = true;
                noKeyPressed = false;
                object.move(0, -1, bowTensing ? speed*.4f : speed*.8f);
                if(!blocking && !bowTensing && useType != ItemValues.UseType.HANDHELD) animation.play("walk-down");
                else if(!bowTensing) {
                    animation.play("handheld-walk-down");
                    itemAnimations.play("handheld-down");
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A) && !attackPressed && !attacking && !blocking) {
                side = 1;
                noKeyPressed = false;
                object.move(-1, upPressed ? 1 : (downPressed ? -1 : 0), bowTensing ? speed*.5f : speed);
                if(!upPressed && !downPressed && !blocking && !bowTensing) {
                    if(useType != ItemValues.UseType.HANDHELD && !bowTensing) animation.play("walk-left");
                    else if (!bowTensing) {
                        animation.play("handheld-walk-left");
                        itemAnimations.play("handheld-left");
                    }
                }
            } else if (Gdx.input.isKeyPressed(Input.Keys.D) && !attackPressed && !attacking && !blocking) {
                side = 3;
                noKeyPressed = false;
                object.move(1, upPressed ? 1 : (downPressed ? -1 : 0), bowTensing ? speed*.5f : speed);
                if(!upPressed && !downPressed && !blocking && !bowTensing) {
                    if(useType != ItemValues.UseType.HANDHELD && !bowTensing) animation.play("walk-right");
                    else if (!bowTensing) {
                        animation.play("handheld-walk-right");
                        itemAnimations.play("handheld-right");
                    }
                }
            }
            if(!attackPressed && !attacking && noKeyPressed && !blocking && !bowTensing) {
                if (side == 0) {
                    animation.play("idle-front");
                    if(inventory.getEquippedItem() != null) {
                        if (inventory.getEquippedItem().data.useType == ItemValues.UseType.HANDHELD || useType == ItemValues.UseType.BOW)
                            itemAnimations.play("handheld-down");
                    }
                } else if (side == 1) {
                    animation.play("idle-left");
                    if(inventory.getEquippedItem() != null) {
                        if (inventory.getEquippedItem().data.useType == ItemValues.UseType.HANDHELD || useType == ItemValues.UseType.BOW)
                            itemAnimations.play("handheld-left");
                    }
                } else if (side == 2) {
                    animation.play("idle-up");
                    if(inventory.getEquippedItem() != null) {
                        if (inventory.getEquippedItem().data.useType == ItemValues.UseType.HANDHELD)
                            itemAnimations.play("handheld-up");
                    }
                } else if (side == 3) {
                    animation.play("idle-right");
                    if(inventory.getEquippedItem() != null) {
                        if (inventory.getEquippedItem().data.useType == ItemValues.UseType.HANDHELD)
                            itemAnimations.play("handheld-right");
                    }
                }
            }

            if(dashing) {
                if(Gdx.graphics.getFrameId() % (int) ((Gdx.graphics.getFramesPerSecond()*.05f)+1) == 0) {
                    dashShadow.add(new Vector3(parent.getPosition(), 1));
                }
                parent.moveTo(parent.getWorld().getMousePos().x*parent.getWorld().getPPM(), parent.getWorld().getMousePos().y*parent.getWorld().getPPM(), speed*3);
                if(parent.getPosition().scl(parent.getWorld().getScale()).dst(parent.getWorld().getMousePos()) < 5*parent.getWorld().getScale()) cancelDash();
                dashTime += deltaTime;
            }
            if(dashTime > 0.2f) cancelDash();

            if(attackPressed && animation.getFrameIndex() <= 2 && !gotHit) {
                parent.move(attackDirection.x, attackDirection.y, speed*.5f);
            }

            if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !attacking && !attackPressed && !gotHit && !inventory.touches(Gdx.input.getX(), Gdx.input.getY()) && !blocking &&
                inventory.getEquippedItem().data.useType == ItemValues.UseType.SWORD && !bowTensing) {
                attackPressed = true;
                Vector2 mouse = world.getMousePos();
                attackDirection.set(world.getMousePos().x - parent.getPosition().x * world.getScale(), world.getMousePos().y - parent.getPosition().y * world.getScale()).nor();

                if (mouse.x > parent.getX() * world.getScale() ){//&& mouse.y > bottomHeight && mouse.y < topHeight) {
                    animation.play("melee-right");
                    itemAnimations.playMajor("sword-right", 1);
                    side = 3;
                } else if(mouse.x < parent.getX()*world.getScale() ){//&& mouse.y > bottomHeight && mouse.y < topHeight) {
                    animation.play("melee-left");
                    itemAnimations.playMajor("sword-left", 1);
                    side = 1;
                }
            }
        }

        if((animation.getCurrentId().equals("melee-right") || animation.getCurrentId().equals("melee-left") && !gotHit)) {
            if(animation.getFrameIndex() >= 1 && !attacking) {
                attacking = true;
                attackList.clear();
                attackList = world.getObjectsInRadius(object.getPosition().x, object.getPosition().y, 1, false, attackList);

                world.shake.shake(0.6f, 0.1f, true);
                for(WorldObject w : attackList) {
                    if(!w.equals(object) && w.dist(object) < 40) {
                        float perfectAngle = w.getPosition().sub(parent.getPosition()).nor().angle();
                        float actualAngle = attackDirection.angle();
                        float angleDiff = (actualAngle - perfectAngle + 180 + 360) % 360 - 180;

                        if(angleDiff <= 90 && angleDiff >= -90) {
                            w.applyForce(attackDirection.x * 15, attackDirection.y * 15);

                            for(Behavior b : w.getBehavior()) {
                                if (b instanceof EntityEvents) {
                                    final boolean crit = isCrit(inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).crit_chance);
                                    parent.getWorld().shake.resetTimer(0);
                                    if (parent.getWorld().shake.getDuration() >= 1)
                                        parent.getWorld().shake.resetDuration(1);

                                    try {
                                        w.invokeMethod(b.getID(), "onHit", parent, crit ? -inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).crit_damage : -inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).base_damage, attackDirection.x * 8, attackDirection.y * 8);
                                        world.particleSystem.addParticle(new HitParticle(parent.getX() + attackDirection.x * 34, parent.getY() + attackDirection.y * 34, 100, null));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    if (crit) {
                                        parent.getWorld().shake.shake(1, 0.5f, false);
                                        world.particleSystem.addParticle(new com.mygdx.particles.HitmarkerParticle(parent.getX(), parent.getY(), new com.mygdx.particles.HitmarkerParticlePool("Crit!", true)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(entity != null) inventory.healthbar.setValue((float) entity.health / (float) entity.maxHealth);

        if(speed > maxSpeed) speed = maxSpeed;

        if(object.moved() && speed < maxSpeed) speed += maxSpeed*deltaTime*15;
        else if(!object.moved() && speed > 0) speed -= .5f;
        else if(!object.moved()) speed = 0;

        object.setTexture(animation.getCurrentFrame());
        animation.update(Gdx.graphics.getFramesPerSecond());
        itemAnimations.update(Gdx.graphics.getFramesPerSecond());
        inventory.update();
    }

    private boolean isCrit(float chance) {
        chance = chance < 0? 0 : (chance > 1 ? 1 : chance);
        return (int) (MathUtils.random(0, 100)) < (100 * chance);
    }

    private void cancelSwordAttack() {
        itemAnimations.stop();
        attackPressed = false;
        attacking = false;
    }

    private void cancelDash() {
        dashTime = 0;
        dashing = false;
    }

    @Override
    public void onRemove() {
        Logger.logInfo("", "Disposing Player Inventory...");
        inventory.dispose();
        inventory = null;
    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {
        if(itemAnimations.getCurrentFrame() != null) {
            float sizeX = parent.getSize().x * world.getScale() * parent.getScale() * world.getPPM();
            float sizeY = parent.getSize().y * world.getScale() * parent.getScale() * world.getPPM();
            float rotation = parent.getRotation();
//            if(inventory.getEquippedItem().data.useType == ItemValues.UseType.BOW) {
//                float dx = world.getMousePos().sub(parent.getPosition().scl(world.getScale())).x;
//                float dy = world.getMousePos().sub(parent.getPosition().scl(world.getScale())).y;
//                rotation = (float) Math.toDegrees(Math.atan2(dy, dx));
//                if(rotation < 0)rotation += 360;
//            }
            batch.draw(itemAnimations.getCurrentFrame(),   //Dude this was pain...
                    parent.getX() * world.getScale() - sizeX * .5f - parent.getRootValues().values.textureOffset.x,
                    parent.getY() * world.getScale() - sizeY * .5f - parent.getRootValues().values.textureOffset.y,
                    sizeX * .5f + parent.getRootValues().values.textureOffset.x,// + (inventory.getEquippedItem().data.useType == ItemValues.UseType.BOW ? 1*world.getScale() : 0),
                    sizeY * .5f + parent.getRootValues().values.textureOffset.y,// + (inventory.getEquippedItem().data.useType == ItemValues.UseType.BOW ? -5*world.getScale() : 0),
                    sizeX, sizeY, 1.85f, 1.85f, rotation);
        }
    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {
        for(Vector3 shadow : dashShadow) {
            batch.setColor(0, 0, 0, shadow.z);
            float sizeX = parent.getSize().x * world.getScale() * parent.getScale() * world.getPPM() * shadow.z;
            float sizeY = parent.getSize().y * world.getScale() * parent.getScale() * world.getPPM() * shadow.z;
            object.drawObjectTexture(batch, parent.getRootValues().texture, shadow.x + sizeX, shadow.y + sizeY, sizeX, sizeY);
            shadow.z -= Gdx.graphics.getDeltaTime()*2;
        }
        batch.setColor(Color.WHITE);
        for(int i = 0; i < dashShadow.size(); i++) {
            if(dashShadow.get(i).z <= 0) {
                dashShadow.remove(i);
            }
        }
    }

    float camRot = 0;
    public void die() {
        isDead = true;
    }

    @Override
    public void onHit(WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY) {
        if(source != null || isDead) {
            blocking = false;
            if(animation.getCurrentId().equals("block-left") || animation.getCurrentId().equals("block-right")) {
                world.particleSystem.addParticle(new com.mygdx.particles.HitmarkerParticle(parent.getX(), parent.getY(), new com.mygdx.particles.HitmarkerParticlePool("Blocked", true)));
                parent.applyForce(knockbackX*.5f, knockbackY*.5f);
                addHealth -= (int) (addHealth * (inventory.getEquippedItem().data.levelStats.get(inventory.getEquippedCell().cellData.level).shield_protection / 100f));
                blocking = true;
            }

            if(!blocking) {
                parent.applyForce(knockbackX, knockbackY);
                parent.invokeMethod(Entity.class, "hit", addHealth);
                if(source.getX() < parent.getX()) animation.playMajor("hit-left", 1);
                else animation.playMajor("hit-right", 1);
            }
            world.particleSystem.addParticle(new HitmarkerParticle(parent.getX(), parent.getY(), new HitmarkerParticlePool(String.valueOf(addHealth), false)));

            if((int) Behavior.getVariable(parent.getBehavior("data"), "health") <= 0) {
                parent.getWorld().game.chat.post("Player", "died", true);
               die();
            }
        }
    }
}
