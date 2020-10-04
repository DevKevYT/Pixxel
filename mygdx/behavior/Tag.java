package com.mygdx.behavior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.RootObject;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;
import com.mygdx.utils.Tools;

import java.lang.reflect.Type;
import java.util.HashMap;

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
    public void onCreate(WorldObject object) {
        font = object.getWorld().game.generateFont(24, Gdx.files.internal(fontPath));
        layout = new GlyphLayout(font, text);
    }

    @Override
    public void postCreate() {

    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {}

    @Override
    public void onRemove() {}

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {
        object.getWorld().game.drawWorldText(batch, font, object.getPosition().x - x, object.getPosition().y - y, size, text);
    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {}
}
