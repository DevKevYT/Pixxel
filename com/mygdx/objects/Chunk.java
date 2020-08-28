package com.mygdx.objects;

import com.badlogic.gdx.Gdx;
import com.mygdx.entities.Player;
import com.mygdx.objects.WorldValues.ChunkData;

import java.util.ArrayList;
import java.util.HashMap;

public class Chunk {

    private ChunkData data;
    private RootObject rootTile; //The loaded root tile of the chunk. Gets a single time loaded, when the chunk gets loaded, never null
    public Tile[][] ground;  //Never null also loaded in Memory, when the chunk is not loaded, but null, if the chunnk was never loaded
    public RootObject[][] changed;  //Never null, gets handled and managed by the world in World#changeTile

    //Speichert alle objekte, die an diesem chunk gebunden sind
    //Alle Objekte in dieser Liste werden automatisch gerendert, wenn der chunk geladen ist.
    public ArrayList<WorldObject> objects = new ArrayList<>();

    //Queue contains only worldobject values and is most likely to be empty, after the chunk got loaded for the first time
    //Theese objects get added when the chunk gets loaded.
    private ArrayList<WorldValues.WorldObjectValues> queue = new ArrayList<>(0);
    private ArrayList<WorldValues.TilePos> tilequeue = new ArrayList<>(0);

    public final int x, y;  //Index of the chunk INDEX!!!
    public final float xpos, ypos, width, height;  //Absolute x and y position of the chunk depending on its x and y index (Normalized!!!!)
    private boolean loaded = false;
    private boolean everLoaded = false;
    private World world;

    public Chunk(World world, ChunkData data, int x, int y) {
        this.x = x;
        this.y = y;
        this.data = data;
        this.world = world;

        xpos = x *      world.getTileSizeNORM() * WorldValues.GroundValues.SQUARE;
        ypos = y *      world.getTileSizeNORM() * WorldValues.GroundValues.SQUARE;
        width =         world.getTileSizeNORM() * WorldValues.GroundValues.SQUARE;
        height =        world.getTileSizeNORM() * WorldValues.GroundValues.SQUARE;

        changed = new RootObject[WorldValues.GroundValues.SQUARE][WorldValues.GroundValues.SQUARE];

        queue.addAll(data.o);
        tilequeue.addAll(data.g.c);
    }

    /**Attaches an object to this Chunk*/
    public void attachObject(WorldObject obj) {  //If an object has wrong coordinates, it will be instantly removed in the next frame
        releaseObject(obj);

        objects.add(obj);
        obj.binding = this;

        Tile tile = world.estimateTile(obj.getPosition().x, obj.getPosition().y, false);
        if(tile != null) tile.attachObject(obj);
    }

    public void releaseObject(WorldObject object) {
        if(object.binding != null) {
            object.binding.objects.remove(object);
            object.binding = null;
        }

        if(object.bindingTile != null) {
            object.bindingTile.objects.remove(object);
            object.bindingTile = null;
        }
    }

    public void fillGround(RootObject object) {
        if(ground == null) {
            ground = new Tile[WorldValues.GroundValues.SQUARE][WorldValues.GroundValues.SQUARE];
            for (int i = 0; i < WorldValues.GroundValues.SQUARE; i++) {
                for (int j = 0; j < WorldValues.GroundValues.SQUARE; j++) {
                    ground[i][j] = new Tile(null, i, j, this);

                    if (changed[i][j] == null) world.changeTile(object, this, i, j); //Set the tile
                    else world.changeTile(changed[i][j], this, i, j);
                }
            }
        }
    }

    public void load() {  //Loads the chunk and adds worldobjects to the world
        if(loaded) return;

        loaded = true;
        everLoaded = true;

        if(rootTile == null) {  //Load the root tile from the world library (Happens only once)
            for(RootObject root : world.library) {
                if(root.getID().equals(data.g.r)) rootTile = root.copy();
            }
        }

        if(rootTile == null) rootTile = world.MISSING.copy();
        fillGround(rootTile);

        while(!tilequeue.isEmpty()) {
            world.changeTile(world.getByID(tilequeue.get(0).id), this, xFromIndex(tilequeue.get(0).i), yFromIndex(tilequeue.get(0).i));
            tilequeue.remove(0);
        }

        while(!queue.isEmpty()) {
            WorldObject o = world.addObject(queue.get(0));
            if(queue.get(0).addr == world.getWorldData().viewport.focus) world.cameraFocus = o;
            queue.remove(0);
        }

        world.loadedChunks.add(this);
    }

    public void wipe() {
        release();
        fillGround(world.MISSING);
        tilequeue.clear();
        queue.clear();
        objects.clear();
    }

    /**Copies chunkdata to another chunk*/
    public static void copyChunk(Chunk from, Chunk to) {
        to.release();
        to.data = from.data.copy();
        if(from.loaded()) to.load();
    }

    public void addQueue(WorldValues.WorldObjectValues values) {
        queue.add(values);
    }

    /**If eraseAll is true, also the world objects get removed (Only address-less ones), saving
     * the maximum amount of data, but it will also slow down the {@link Chunk#load()} process*/
    public void release() {  //Clears the tiles and objects from this chunk and calls the garbage collector
        //ground = null;       //TODO Save objects state to chunk data.
        world.loadedChunks.remove(this);
        loaded = false;
    }

    /**Heavy operation. I recomment to call this function only, when nessesaey<br>
     * For example world saving etc.*/
    public ChunkData getData() {
        if(!everLoaded)  return data;  //The chunk was never loaded during the whole time the world was loaded -> No changes and nothing to save

        ChunkData data = new ChunkData();
        for(WorldObject obj : objects) data.o.add(obj.worldObjectValues);  //Saving objects

        if(rootTile != null) data.g.r = rootTile.getID();
        else data.g.r = this.data.g.r;

        String currentRoot = rootTile.getID();   //Get existing tiles
        int score = 0; //Score of the current root tile

        HashMap<String, Integer> hash = new HashMap<>();  //Dont know what this whole does, but i estimate it calculates the majority of a tile type to reduce world file size
        main: for(int i = 0; i < WorldValues.GroundValues.SQUARE; i++) {
            for(int j = 0; j < WorldValues.GroundValues.SQUARE; j++) {
                if(!ground[i][j].groundTile.getID().equals(currentRoot)) {
                    boolean exist = false;
                    a: for(String s : hash.keySet()) {
                        if(s.equals(ground[i][j].groundTile.getID())) {
                            int newscore = hash.get(ground[i][j].groundTile.getID()) + 1;
                            hash.put(ground[i][j].groundTile.getID(), newscore);
                            if(newscore > (WorldValues.GroundValues.SQUARE * WorldValues.GroundValues.SQUARE) / 2) break main;
                            exist = true;
                            break a;
                        }
                    }
                    if(!exist) hash.put(ground[i][j].groundTile.getID(), 1);
                }
                else score++;
            }
        }

        int half = (WorldValues.GroundValues.SQUARE * WorldValues.GroundValues.SQUARE) / 2; //Evaluate the tiles
        if(score <= half) {
            for(String s : hash.keySet()) {
                if(hash.get(currentRoot) != null) {
                    if(hash.get(s) > hash.get(currentRoot))  currentRoot = s;
                } else currentRoot = s;
                if(hash.get(s) > half) break;
            }
        }
        data.g.r = currentRoot;

        for(int i = 0; i < WorldValues.GroundValues.SQUARE; i++) {  //Estimate the changed chunks and swap root tile, if needed
            for(int j = 0; j < WorldValues.GroundValues.SQUARE; j++) {
                if(changed[i][j] != null) {
                    if(!changed[i][j].getID().equals(currentRoot)) {
                        WorldValues.TilePos t = new WorldValues.TilePos();
                        t.i = indexFromTilepos(i, j);
                        t.id = changed[i][j].getID();
                        data.g.c.add(t);
                    }
                }
            }
        }
        return data;
    }

    public int xFromIndex(int index) {
        return index % WorldValues.GroundValues.SQUARE;
    }

    public int yFromIndex(int index) {
        return index / WorldValues.GroundValues.SQUARE;
    }

    public int indexFromTilepos(int x, int y) {
        return x + (y * WorldValues.GroundValues.SQUARE);
    }

    /**Checks if a position is on the chunk*/
    public boolean onChunk(float x, float y) {
        return x > xpos && y > ypos && x < xpos+width && y < ypos+height;
    }

    public boolean loaded() {
        return loaded;
    }
    public boolean everLoaded() { return everLoaded; }

    public String toString() {
        return x + " " + y;
    }
}
