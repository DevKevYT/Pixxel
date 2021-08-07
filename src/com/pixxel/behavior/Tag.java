package com.pixxel.behavior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.WorldValues;
import com.pixxel.objects.World;
import com.pixxel.objects.WorldObject;

public class Tag extends Behavior {

    public float x = 0, y = 0;
    public String text = "";
    public int _dontSave = 0;
    public String fontPath = "UI//fonts//RetroGaming.ttf";
    public float size = 10;

    public BitmapFont font;
    private GlyphLayout layout;
    private float invZoom = 0;

    public Tag(WorldValues.BehaviorValues values) {
        super(values, Tag.class);

    }

    @Override
    public void onSave() {}

    @Override
    public void onCreate(com.pixxel.objects.WorldObject object) {
        font = object.getWorld().game.generateFont(24, Gdx.files.internal(fontPath));
        layout = new GlyphLayout(font, text);
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, com.pixxel.objects.WorldObject object, float deltaTime) {}

    @Override
    public void onRemove() {}

    @Override
    public void drawOver(com.pixxel.objects.WorldObject object, SpriteBatch batch) {
        object.getWorld().game.drawWorldText(batch, font, object.getPosition().x - x, object.getPosition().y - y, size, text);
    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {}
}
