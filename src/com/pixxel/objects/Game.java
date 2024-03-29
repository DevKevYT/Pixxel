package com.pixxel.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.pixxel.Main.Logger;
import com.pixxel.Main.FileHandler;
import com.pixxel.ui.ChatBar;
import com.pixxel.ui.Dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**Main instance of the running engine.
 * This class keeps track of all assets, worlds, world loading, etc ...<br><br>
 *
 * It also provides a function to save custom global data which needs to be available across world saves.<br>
 * For example Player Inventories, Current World, Health ...<br><br>
 * Look here for more info on custom object saving:<br>
 * {@link Game#getCutomData(String, Class)}<br>
 * {@link Game#setCustomData(String, Object, Class)}<br><br>
 *
 * This Object is automatically attached to the so called game file, where also the data like assets, worlds and custom data is saved.
 * It needs to be specified in the constructor.<br><br>
 * You could technically create a {@link Game} instance without a file<br>
 * But you would not be able to save data.<br>I do not recommend this*/
public class Game implements Disposable {

    private GameValues.GameData game;
    private FileHandle file;
    private HashMap<String, FileHandle> maps = new HashMap<>();
    public HashData hashData;

    private ArrayList<RootObject> library = new ArrayList<>();
    public final World stage;

    public Skin resource;
    public final Stage gui;
    public com.pixxel.ui.Dialog dialog;
    public com.pixxel.ui.ChatBar chat;
    private boolean hasGui = false;

    public static final int dpiX = 800;
    public static final String VERSION = "1.0.0";

    public Game(GameValues.GameData game) {
        stage = new World(this);
        this.game = game;
        this.gui = new Stage();
    }

    public Game(FileHandle file) {
        stage = new World(this);
        this.game = FileHandler.readJSON(file, GameValues.GameData.class);
        this.file = file;
        this.gui = new Stage();
    }

    /**Builds the game instance from the given file at initializing*/
    public void build() {
        Logger.logInfo("Game", "Initializing game instance...");

        buildHashData();
        verifyFiles();
        loadLibrary();

        Logger.logInfo("Game", "Game initialized");
    }

    public void buildHashData() {
        Logger.logInfo("Game", "Loading hash data...");
        hashData = new HashData(game.hashData);
    }

    /**Builds a gui<br>Res describes the 'design'<br>
     * Use null to disable gui and the gui will also get disabled, if the function failed to load the resources
     * <br>Check with hasGui()*/
    public void setGui(Skin resource) {
        if(resource != null) hasGui = true;
        else {
            hasGui = false;
            return;
        }
        resource.getFont("default-font").getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Logger.logInfo("game", "Setting up the gui...");
        this.resource = resource;
        dialog = new Dialog(gui, resource);
        dialog.dialog.setVisible(false);
        gui.addActor(dialog.dialog);
        chat = new ChatBar(this, resource);
        chat.hideCmd();
        chat.setBounds(Gdx.graphics.getWidth()-310, 10, 300, 20);
        gui.addActor(chat);
    }


    public BitmapFont generateFont(int resolution, FileHandle font) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(font);
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = resolution;
        BitmapFont f = generator.generateFont(param);
        f.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        generator.dispose();
        return f;
    }

    /**Draws text into the currently loaded world x and y and size as Normalized units.<br>
     * But is needs to load the font each frame.<br>
     * Use<br> {@link Game#drawWorldText(Batch, BitmapFont, float, float, float, String)}<br> instead.
     * */
    public GlyphLayout drawWorldText(Batch batch, String fontName, float x, float y, float size, String text) {
        if(stage == null) return null;
        BitmapFont font = resource.getFont(fontName);
        if(font == null) return null;
        return drawWorldText(batch, font, x, y, size, text);
    }

    private static Vector2 translate = new Vector2();
    /**Draws text into the currently loaded world x and y and size as Normalized units.
     * @param batch The batch for drawing the font
     * @param font Previously generated bitmap font
     * @param x Normalized x coordinate
     * @param y Normalized y coordinate
     * @param text The text you would like to render
     * @see com.pixxel.behavior.Tag#drawOver(WorldObject, SpriteBatch) */
    public GlyphLayout drawWorldText(Batch batch, BitmapFont font, float x, float y, float size, String text) {
        if(stage == null) return null;
        final Matrix4 prev = batch.getProjectionMatrix().cpy();
        final float prevScale = font.getData().scaleX;
        batch.setProjectionMatrix(gui.getCamera().combined.cpy());
        translate.set(stage.translateTo(x * stage.getScale(), y * stage.getScale(), true, gui.getCamera()));

        font.getData().setScale(stage.invertedZoom * stage.getScale() * size);
        GlyphLayout layout = font.draw(batch, text, translate.x, translate.y);
        batch.setProjectionMatrix(prev);
        font.getData().setScale(prevScale);
        return layout;
    }

    /**Loads and checks every world file (From local path by default. If the file path starts with an *
     * it will look for a file related to this path on the device),
     * if its not broken and puts it into a Hashmap*/
    public void verifyFiles()  {
        Logger.logInfo("Game", "Verifying map files...");
        maps.clear();
        FileHandle file = null;
        for(String path : game.worlds) {
            if(!path.startsWith(FileHandler.ABSOLUTE)) file = Gdx.files.local(path);
            else file = Gdx.files.external(path.substring(1)); //Remove the * at beginning
            if(file.exists() && path.endsWith(".json")) {
                maps.put(file.name(), file);
                Logger.logInfo("Game", "Map added: " + file.file().getAbsolutePath());
            } else Logger.logError("Game", "Map file not found! " + path);

        }
    }

    /**Loads all assets specified in the game file*/
    private void loadLibrary() {
         Logger.logInfo("Game", "Loading object files...");
         FileHandle file = null;
         for (int i = 0; i < game.assets.size(); i++) {
             String path = game.assets.get(i);
                 if(path.startsWith(FileHandler.ABSOLUTE)) file = Gdx.files.external(path.substring(1));
                 else file = Gdx.files.local(path);
                 if (!file.isDirectory() && file.exists() && file.path().endsWith(".json")) {
                     try {
                         Logger.logInfo("Game", "Loading: " + file.path());
                         RootObject obj = new RootObject(Objects.requireNonNull(FileHandler.readJSON(file, com.pixxel.objects.RootValues.RootObjectValues.class)));
                         obj.file = file;
                         obj.loadTexture();
                         library.add(obj);
                         library.addAll(obj.tileset);
                     } catch(Exception e) {
                         Logger.logError("Game", "Failed to load root object from file: " + path + " [" + e.toString().replace('\n', ' ') + "]");
                     }
                 } else if (file.isDirectory() && file.exists()){
                     Logger.logInfo("Game", "loading directory: " + file.path());
                     for(FileHandle f : file.list()) {
                         if (!f.isDirectory() && f.path().endsWith(".json")) {
                             try {
                                 com.pixxel.objects.RootValues.RootObjectValues values = FileHandler.readJSON(f, RootValues.RootObjectValues.class);
                                 RootObject obj = new RootObject(values);
                                 obj.file = f;
                                 obj.loadTexture();
                                 library.add(obj);
                                 library.addAll(obj.tileset);
                             } catch(Exception e) {
                                 Logger.logError("Game", "Failed to load root object from directory " + f.path() + ": " + e.toString().replace('\n', ' '));
                             }
                         }
                     }
                 } else if(!file.exists()) Logger.logError("Game", "Unable to load root object from directory " + file.path() + ": File not found");
         }
    }

    /**Saves the game file this includes Custom data and hashes*/
    public boolean saveGamefile() {
        Logger.logInfo("Game", "Saving game file to: " + file.path());
        if(file == null) {
            Logger.logError("Game", "Failed to save: Game was not loaded from a file!");
            return false;
        }
        try {
            game.custom.currentWorld  = getCurrentWorld();
            FileHandler.writeJSON(file, game, true);
            return true;
        } catch(IOException e) {
            Logger.logError("Game", "Failed to save game: " + e.toString().replace('\n', ' '));
            return false;
        }
    }

    /**Saves the current loaded world*/
    public boolean saveStage() {
        if(stage != null) return stage.save();
        return false;
    }

    /**Returns the loaded stage or null, if the loading failed*/
    public World loadWorld(String fileName) {
        Logger.logInfo("Game", "PREPARING TO LOAD  " + fileName);
        FileHandle file = maps.get(fileName);

        if(file == null) {
            Logger.logError("Game", "Map file not found in gamedata worldlist: " + fileName);
            return null;
        }

        Logger.logInfo("Game", "GENERATING...");
        stage.generate(file, library);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Logger.logInfo("Game", "DONE");
        return stage;
    }

    public HashMap<String, FileHandle> getMaps() {
        return maps;
    }

    public static String getFilename(String path) {
        if(path.contains("/") && !path.endsWith("/")) {
            return path.substring(path.lastIndexOf("/")+1);
        } else return path;
    }

    public GameValues.GameData getProperties() {
        return game;
    }

    /**May be null*/
    public FileHandle getFile() {
        return file;
    }

    public void addLibrary(ArrayList<RootObject> root) {
        library.addAll(root);
    }

    public ArrayList<RootObject> getLibrary() {
        return library;
    }

    public String getCurrentWorld() {
        return game.custom.currentWorld;
    }

    public boolean hasGui() { return hasGui; }

    /**Saves every variable from the given object.
     * It is not recommended to use this function with objects,
     * you don't exactly know what they're made of.
     * Also  nested objects could result in a really unnecessary bit game file and wrong loading<br><br>
     * Objects you want to save here should also have a constructor without params.<br>
     * Otherwise an exception will be thrown upon loading the object later.
     *
     * @param name  The Variable name inside the game file. Should be unique
     * @param object  The object instance you want to save
     * @param clazz  The class. For example if you want to save Object, use Object.class as type parameter
     * @see Game#getCutomData(String, Class) */
    public void setCustomData(String name, Object object, Class<?> clazz) {
        for(GameValues.Object_ s : game.custom.custom) {
            if(s.name.equals(name)) {
                s.classPath = clazz.getName();
                s.object = object;
                return;
            }
        }

        GameValues.Object_ obj = new GameValues.Object_();
        obj.name = name;
        obj.classPath = clazz.getName();
        obj.object = object;
        game.custom.custom.add(obj);
    }

    /**Loads an object from the game file.<br>
     * The object is then castet to the given class.
     * @param name Variable name. Should exist, otherwise null is returned
     * @param clazz The class type the object should get casted to
     * @see Game#setCustomData(String, Object, Class) */
   public <T> T getCutomData(String name, Class<T> clazz) {
        for(GameValues.Object_ obj : game.custom.custom) {
            if(obj.name.equals(name) && obj.classPath.equals(clazz.getName())) {
                return (T) ((GameValues.Object_) (obj)).object;
            }
        }
        return null;
   }

    /**Removes a variable from the custom space
     * @param name The variable name
     * @see Game#setCustomData(String, Object, Class) */
    public void removeCustomData(String name) {
        for(GameValues.Object_ s : game.custom.custom) {
            if(s.name.equals(name)) {
                game.custom.custom.remove(s);
                return;
            }
        }
    }

    @Override
    public void dispose() {
        if(stage != null) stage.dispose();
    }

    public void resize(int width, int height) {
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                gui.getViewport().update(width, height);
                gui.getViewport().apply();
                float ratio = (float) height / (float) width;
                stage.getViewport().viewportWidth = dpiX * stage.getScale();//  * 1f/(width*stage.getScale()*0.01f) * stage.getScale();
                stage.getViewport().viewportHeight = dpiX * ratio * stage.getScale();// * 1f/(width*stage.getScale()*0.01f) * stage.getScale();
                stage.getViewport().update();
            }
        });
    }

    @Override
    public String toString() {
        if(file != null) return file.path();
        return "Game";
    }
}
