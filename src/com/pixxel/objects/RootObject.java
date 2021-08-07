package com.pixxel.objects;

import java.io.File;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.pixxel.animation.Movie;
import com.pixxel.animation.SpriteAnimation;
import com.pixxel.Main.Logger;
import com.pixxel.objects.RootValues.RootObjectValues;

/**
 * Root objects are like shells.
 * A root object can have all the meta informations like textures, color, lights, fixtures etc.
 * When a world object is created and added to the world it also has a root object attached for all the basic information.
 */
public class RootObject {

    public final RootObjectValues values;
    public FileHandle file;  //Not very important, but a useful tool to detect, where the object is loaded from. Set in Game#loadLibrary()

    ArrayList<FixtureDef> fixtures = new ArrayList<FixtureDef>();
    BodyDef bodyDef = new BodyDef();

    public Sprite texture;
    public SpriteAnimation animation = null;
    private boolean textureLoaded = false;
    public ArrayList<RootObject> tileset = new ArrayList<>();

    /**
     * Copies the values, so you can modify this root object independent of the instance<br>
     * of the given values object. Box2D fixtures and body def is created,<br>
     * when the custom values are confirmed by creating a world object.<b><br>
     * NOTE: Changing values, after a world objects has been created, wont have affect!
     * <br>Changing root object values in world objects is only possible with the provided functions!</b>
     */
    public RootObject(RootObjectValues values) {
        this.values = values.copy();
    }

    public RootObject(String name) {
        values = new RootObjectValues();
        values.id = name;
    }

    /**
     * Constructs a new {@link RootObject} in the default state.
     */
    public RootObject() {
        values = new RootObjectValues();
    }

    /**
     * Just creates a clone of the given root object
     */
    public RootObject(RootObject object) {
        values = object.values.copy();
    }

    public void setScale(float scale) {
        values.scale = scale;
    }

    public float getScale() {
        return values.scale;
    }

    public void setTextureOffset(Vector2 offset) {
        this.values.textureOffset.set(offset);
    }

    public void setSize(Vector2 size) {
        this.values.size = new Vector2(size);
    }

    /**Applies size rules (unscaled).<br>
     * For every value that is size relates is:
     * <br>0 (only object with texture) = x or y with fitting scale to y or x. E.g. x = 0, y = 20, x would be set to be fitting to y and image size
     * <br>-0.1 to -Infinity (All size related values): Scale to tile size. -1 Would be 1 * tileSize, -0.5 would be half the tilesize etc.
     * <br><br>TO have affect to hitboxes, this function needs to be called, BEFORE adding the object to a world and AFTER {@link RootObject#loadTexture()}*/
    public void checkSize(World world) {
        if(values.size.x < 0 && world != null) values.size.x = values.size.x * world.getTileSizeNORM() * -1;
        if(values.size.y < 0 && world != null) values.size.y = values.size.y * world.getTileSizeNORM() * -1;

        if(values.fixtures != null) {
            for (com.pixxel.objects.RootValues.Fixture f : values.fixtures) { //Apply rules to hitboxes
                if (f.width < 0 && world != null) f.width = world.getTileSizeNORM() * -1 * f.width;
                if (!f.isCircle && f.height < 0 && world != null)
                    f.height = world.getTileSizeNORM() * -1 * f.height;
            }
        }

        if(textureLoaded()) {
            if(values.size.x == 0 && values.size.y != 0) {
                if(texture.getTexture().getWidth() == 0) values.size.x = 0;
                else values.size.x = values.size.y * ((float) texture.getTexture().getWidth() / (float) texture.getTexture().getHeight());
            }
            if(values.size.y == 0 && values.size.x != 0) {
                if(texture.getTexture().getHeight() == 0) values.size.y = 0;
                else values.size.y = values.size.x * ((float) texture.getTexture().getHeight() / (float) texture.getTexture().getWidth());
            }
        } else {
            if(values.size.x == 0) Logger.logError("Warning RootObject " + getID(), "Width is set to fit image height, but no image loaded, so width = 0");
            if(values.size.y == 0) Logger.logError("Warning RootObject " + getID(), "Height is set to fit image width, but no image loaded, so height = 0");
        }
    }

    /**
     * Loads the internal texture or the animation, if the RootObject was created via {@link RootObjectValues}
     * <br>Overwrites the texture if previously loaded with {@link RootObject#setTexture(Sprite)}
     */
    public void loadTexture() {
        if (values.texturePath.isEmpty() || values.texturePath == null || textureLoaded) return;
        if(!values.loadType.equals(RootObjectValues.atlas)) {
            FileHandle image = null;

            if(values.texturePath.startsWith("*") && file != null) {
                image = new FileHandle(new File(file.parent().path() +  values.texturePath.substring(1)));
            } else if(values.texturePath.startsWith("#")) {
                image = new FileHandle(new File(values.texturePath.substring(1)));
            } else image = Gdx.files.local(values.texturePath);

            if (image == null) {
                Gdx.app.log("RootObject", "Failed to load texture for root object " + getID() + ": File not found (" + values.texturePath + ")");
                return;
            } else if (!image.exists()) {
                Gdx.app.log("RootObject", "Failed to load texture for root object " + getID() + ": File not found (" + values.texturePath + ")");
                return;
            }

            final Texture texture = new Texture(image); //Here texture can't be null!

            if (values.animation != null) {
                loadAnimation(texture);
            } else loadTexture(texture);

            if (values.loadType.equals(RootObjectValues.tileset)) { //Image can't be null (line 87)
                if (values.tX < 0 || values.tY < 0 || values.tW < 0 && values.tH < 0) {
                    Gdx.app.log("RootObject", "Failed to load tileset for object " + values.id);
                    return;
                }

                int startY = values.tY + values.tH;
                for (int x = values.tX, index = 0; x < texture.getWidth(); x += values.tW, index++) {  //Start at tx + tw, because the first tile is already loaded inside this function
                    for (int y = startY; y <= texture.getHeight() - values.tH; y += values.tH, index++) {
                        RootObjectValues childValues = values.copy();
                        childValues.loadType = RootObjectValues.none;
                        childValues.tX = x;
                        childValues.tY = y;
                        childValues.id += String.valueOf(index);
                        RootObject child = new RootObject(childValues);
                        child.file = RootObject.this.file;
                        child.loadTexture();
                        tileset.add(child);
                    }
                    startY = values.tY;
                }
                for(RootObject childs : tileset) {
                    if(!childs.equals(this)) {
                        childs.tileset.addAll(tileset);
                        childs.tileset.add(this);
                    }
                }
            }

            Logger.logInfo("Game", "Texture for " + getID() + " loaded!");
        } else {
            if(values.texturePath.isEmpty()) {
                Logger.logError("RootObject","Failed to load atlas tileset for " + getID() + ": No atlasPath specified");
                return;
            }
            Logger.logInfo("", "loading atlas");
            FileHandle atlasFile = Gdx.files.local(values.texturePath);
            TextureAtlas atlas = new TextureAtlas(atlasFile);
            for(TextureAtlas.AtlasRegion r : atlas.getRegions()) {
                RootObjectValues childValues = values.copy();
                childValues.loadType = RootObjectValues.none;
                childValues.id = r.name;
                childValues.tX = r.getRegionX(); childValues.tY = r.getRegionY(); childValues.tW = r.getRegionWidth(); childValues.tH = r.getRegionHeight();
                RootObject child = new RootObject(childValues);
                child.file = RootObject.this.file;
                child.loadTexture();
                tileset.add(child);
            }
        }
    }

    public void loadAnimation(Texture region) {
        if (values.animation == null) return;

        Movie m = new Movie(values.animation.framerate);
        for (RootValues.Frame f : values.animation.frames)
            m.addFrame(new Sprite(new TextureRegion(region, f.x, f.y, f.w, f.h)));
        this.animation = new SpriteAnimation();
        animation.addMovie("root", m);
        animation.play("root");
        textureLoaded = true;
    }

    /**
     * @deprecated - First, remove the texture {@link RootObject#setTexture(Sprite)} with null, and then load it again<br>
     * Loads the texture from the file system, ignoring, if it was already loaded.
     * <br>I recommend using this function very carefully and only if you are absolutely sure, changes where made.<br>
     * Otherwise the performance could get really worse.
     */
    public void updateTexture() {
        FileHandle fileHandle = Gdx.files.internal(values.texturePath);
        if (!fileHandle.exists()) return;

        loadTexture(new Texture(fileHandle));
    }

    /**
     * Loads the texture independent of the texture path. If a region is set, the region of this texture will be loaded.
     * <br>Useful for setting the textures for a lot of objects from a sprite sheet you only want to load once.
     */
    public void loadTexture(Texture texture) {
        if (texture == null) return;

        if (values.tX >= 0 && values.tY >= 0 && values.tW > 0 && values.tH > 0) {
            setTexture(new Sprite(new TextureRegion(texture, values.tX, values.tY, values.tW, values.tH)));
        } else setTexture(new Sprite(texture));
    }

    public void setTexture(Sprite texture) {
        textureLoaded = texture == null ? false : true;
        this.texture = texture;
    }

    public void addFixture(FixtureDef fixture) {
        this.fixtures.add(fixture);
    }

    /**
     * Size in x direction divided by 2
     */
    public float getRadius() {
        return values.size.x * .5f;
    }

    public Vector2 getTextureOffset() {
        return new Vector2(values.textureOffset);
    }

    public Vector2 getSize() {
        return values.size.cpy();
    }

    /**
     * Adds a shape to default {@link FixtureDef} settings.
     */
    public void addShape(Shape shape) {
        FixtureDef def = new FixtureDef();
        def.shape = shape;
        this.fixtures.add(def);
    }

    public FixtureDef[] getFixtures() {
        return fixtures.toArray(new FixtureDef[fixtures.size()]);
    }

    public boolean hasHitbox() {
        if(values.fixtures == null) return false;
        else return values.fixtures.size() > 0;
    }

    public boolean textureLoaded() {
        return textureLoaded;
    }

    public final String toString() {
        return values.id;
    }

    public final String getID() {
        return values.id;
    }

    public final RootObject copy() {
        RootObject copy = new RootObject(values.copy());  //Fixtures and bodydef copy is done in the constr.
        copy.file = file;
        copy.tileset.addAll(tileset);
        if (texture != null) {
            copy.texture = texture;
            copy.textureLoaded = textureLoaded;
        } else copy.texture = null;

        return copy;
    }
}
