package com.mygdx.objects;

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
import com.mygdx.darkdawn.Logger;
import com.mygdx.ui.ChatBar;
import com.mygdx.ui.Dialog;
import com.mygdx.darkdawn.FileHandler;
import com.mygdx.objects.GameValues.GameData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Game implements Disposable {

    private GameData game;
    private FileHandle file;
    private HashMap<String, FileHandle> maps = new HashMap<>();
    public HashData hashData;

    private ArrayList<RootObject> library = new ArrayList<>();
    public final World stage;

    public Skin resource;
    public final Stage gui;
    public Dialog dialog;
    public ChatBar chat;
    private boolean hasGui = false;

    //public final OrthographicCamera guiViewport; //Viewport for the GUI
    public static final int dpiX = 800;

    public Game(GameData game) {
        stage = new World(this);
        this.game = game;
        this.gui = new Stage();
    }

    public Game(FileHandle file) {
        stage = new World(this);
        this.game = FileHandler.readJSON(file, GameData.class);
        this.file = file;
        this.gui = new Stage();
    }

    /**Builds the game instance from the given file at initializing*/
    public void build() {
        Logger.logInfo("Game", "###Initializing game instance...###");

        buildHashData();
        verifyFiles();
        loadLibrary();

        Logger.logInfo("Game", "###Game initialized###");
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
        chat.setBounds(10, 40, Gdx.graphics.getWidth()*.75f, 20);
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

    /**Draws text into the currently loaded world
     * x and y and size as Normalized units. Size also depends on the {@link Game#stage#getViewport()}*/
    public GlyphLayout drawWorldText(Batch batch, String fontName, float x, float y, float size, String text) {
        if(stage == null) return null;
        BitmapFont font = resource.getFont(fontName);
        if(font == null) return null;
        return drawWorldText(batch, font, x, y, size, text);
    }

    private static Vector2 translate = new Vector2();
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
            } else {
                Logger.logError("Game", "Map file not found! " + path);
                continue;
            }
        }
    }

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
                         RootObject obj = new RootObject(Objects.requireNonNull(FileHandler.readJSON(file, RootValues.RootObjectValues.class)));
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
                                 RootValues.RootObjectValues values = FileHandler.readJSON(f, RootValues.RootObjectValues.class);
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

    public GameData getProperties() {
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

   public <T> T getCutomData(String name, Class<T> clazz) {
        for(GameValues.Object_ obj : game.custom.custom) {
            if(obj.name.equals(name) && obj.classPath.equals(clazz.getName())) {
                return (T) ((GameValues.Object_) (obj)).object;
            }
        }
        return null;
   }

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
