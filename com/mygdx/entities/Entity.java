package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mygdx.animation.Movie;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

/**There are a few rules to create an entity in the Pixxel engine:
 * 1. Implementing this class to your object will have the ID: "data", this behavior will handle general entity behaviors like health, healthbar and shadow.
 * 2. The rendering and the actual specific entity behavior (Pathfinding, animations etc...) is another behavior class wich
 *    additionally implements the EntityEvents interface. This behavior should have the ID: "entity"*/
public class Entity extends Behavior {

    public int health = 1000;
    public int maxHealth = 1000;
    public boolean barVisible = true;
    public int barType = 0;  //1 = player, 0 = enemy, 2 = friendly creatures
    public float sScale = 1;
    public float sOffset = 0;
    public String name = "";

    private ProgressBar healthbar;
    private Label nameLabel;
    public static Sprite entityShadow;

    public static Movie entityDespawn;
    static {
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures//FX//entity-despawn.atlas"));
        entityDespawn = new Movie(9, atlas, "despawn1", "despawn2", "despawn3", "despawn4", "despawn5");
    }

    public Entity(WorldValues.BehaviorValues values) {
        super(values, Entity.class);
    }

    @Override
    public void onSave() {

    }

    public void hit(Integer addhealth) {
        health += addhealth;
    }

    @Override
    public void onCreate(WorldObject object) {
        if(entityShadow == null) {
            entityShadow = new Sprite(super.world.game.resource.getSprite("entity-shadow"));
        }

        if(barType == 1) healthbar = new ProgressBar(0, 1, 0.01f, false, super.world.game.resource, "healthbar-player");
        else healthbar = new ProgressBar(0, 1, 0.01f, false, super.world.game.resource, "healthbar-enemy");

        healthbar.getStyle().background.setMinHeight(7);
        healthbar.getStyle().knobBefore.setMinHeight(5);
        healthbar.setSize(world.getTileSizeNORM() * 0.7f, 10);
        healthbar.setAnimateDuration(0.1f);
        super.world.game.gui.addActor(healthbar);

        nameLabel = new Label("", world.game.resource, "itemInfo");
        nameLabel.setFontScale(0.3f);

        world.game.gui.addActor(nameLabel);
        setID("data");

        if(name.isEmpty()) name = parent.getBehavior("entity") != null ? parent.getBehavior("entity").getClassPath() : "???";
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(health <= 0) {
            healthbar.setVisible(false);
            nameLabel.setVisible(false);
        }
    }

    @Override
    public void onRemove() {
        nameLabel.remove();
        healthbar.remove();
    }

    Vector2 translated = new Vector2();
    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {
        if(barVisible && health > 0) {
            healthbar.setVisible(true);
            if(maxHealth == 0) maxHealth = 1;
            float percentage = (float) (health) / (float) (maxHealth);
            healthbar.setValue(percentage);
            healthbar.setPosition(translated.x, translated.y);
            healthbar.setSize(world.getTileSizeNORM()/world.getViewport().zoom, 20f/world.getViewport().zoom);
            translated.set(world.translateTo(parent.getPosition().x*world.getScale() - world.getTileSizePX()*.5f, parent.getPosition().y*world.getScale() + parent.getSize().y*world.getScale()*.5f, false, world.game.gui.getCamera()));

            nameLabel.setText(name);
            nameLabel.setPosition(healthbar.getX(), healthbar.getY() + healthbar.getPrefHeight() + 5);
            nameLabel.setSize(healthbar.getWidth(), healthbar.getHeight());
        } else healthbar.setVisible(false);
    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {
        if(entityShadow == null) return;
        float x = parent.getPosition().x * world.getScale() - entityShadow.getWidth() * world.getScale() * 0.5f;
        float y = parent.getPosition().y * world.getScale() - parent.getSize().y*parent.getScale()*0.5f - parent.getYOff()*world.getScale() - sOffset*world.getScale();

        batch.draw(entityShadow, x, y, entityShadow.getWidth() * world.getScale() * sScale, entityShadow.getHeight() * world.getScale() * sScale);
    }
}
