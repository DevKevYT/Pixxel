package com.pixxel.objects;

import com.pixxel.objects.WorldValues.ChunkData;

import java.util.ArrayList;
import java.util.HashMap;

public class Chunk {

    /**Raw data as represented in the world file*/
    private ChunkData data;
    /**The loaded root tile of the chunk. Gets a single time loaded, when the chunk gets loaded, never null*/
    private com.pixxel.objects.RootObject rootTile;
    /**Never null also loaded in Memory, when the chunk is not loaded, but null, if the chunk was never loaded*/
    public com.pixxel.objects.Tile[][] ground;
    /**Never null, gets handled and managed by the world in {@link World#changeTile(RootObject, int, int)}*/
    public com.pixxel.objects.RootObject[][] changed;

    /**Contains all objects being located on the chunk at the current frame.*/
    public ArrayList<WorldObject> objects = new ArrayList<>();

    /**Queue contains only {@link com.pixxel.objects.WorldValues.WorldObjectValues} values and is most likely empty, after the chunk got loaded for the first time
    These objects get added when the chunk gets loaded.*/
    private ArrayList<com.pixxel.objects.WorldValues.WorldObjectValues> queue = new ArrayList<>(0);
    private ArrayList<com.pixxel.objects.WorldValues.TilePos> tilequeue = new ArrayList<>(0);

    /**Index of the chunk, not the coordinates*/
    public final int x, y;
    /**Absolute, normalized positions of the chunk*/
    public final float xpos, ypos, width, height;
    private boolean loaded = false;
    private boolean everLoaded = false;
    private com.pixxel.objects.World world;

    public Chunk(com.pixxel.objects.World world, ChunkData data, int x, int y) {
        this.x = x;
        this.y = y;
        this.data = data;
        this.world = world;

        xpos = x *      world.getTileSizeNORM() * com.pixxel.objects.WorldValues.GroundValues.SQUARE;
        ypos = y *      world.getTileSizeNORM() * com.pixxel.objects.WorldValues.GroundValues.SQUARE;
        width =         world.getTileSizeNORM() * com.pixxel.objects.WorldValues.GroundValues.SQUARE;
        height =        world.getTileSizeNORM() * com.pixxel.objects.WorldValues.GroundValues.SQUARE;

        changed = new com.pixxel.objects.RootObject[com.pixxel.objects.WorldValues.GroundValues.SQUARE][com.pixxel.objects.WorldValues.GroundValues.SQUARE];

        //queue.addAll(data.o);
        tilequeue.addAll(data.g.c);
        //Already load objects with a trigger
        for(com.pixxel.objects.WorldValues.WorldObjectValues check : data.o) {
            if(!check.addr.equals(WorldObject.NO_ADDR) && !check.addr.equals(WorldObject.TILE_ADDR)) {
                //Instantly load this object without a "queue"
              world.addObject(check);
            } else queue.add(check);
        }
    }

    /**Attaches an object to this Chunk*/
    public void attachObject(WorldObject obj) {  //If an object has wrong coordinates, it will be instantly removed in the next frame
        releaseObject(obj);

        objects.add(obj);
        obj.binding = this;

        com.pixxel.objects.Tile tile = world.estimateTile(obj.getPosition().x, obj.getPosition().y, false);
        if(tile != null) tile.attachObject(obj);
    }

    /**Releases an object from the chunk*/
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

    public void fillGround(com.pixxel.objects.RootObject object) {
        if(ground == null) {
            ground = new com.pixxel.objects.Tile[com.pixxel.objects.WorldValues.GroundValues.SQUARE][com.pixxel.objects.WorldValues.GroundValues.SQUARE];
            for (int i = 0; i < com.pixxel.objects.WorldValues.GroundValues.SQUARE; i++) {
                for (int j = 0; j < com.pixxel.objects.WorldValues.GroundValues.SQUARE; j++) {
                    ground[i][j] = new Tile(null, i, j, this);

                    if (changed[i][j] == null) world.changeTile(object, this, i, j); //Set the tile
                    else world.changeTile(changed[i][j], this, i, j);
                }
            }
        }
    }

    /**Loads a chunk*/
    public void load() {  //Loads the chunk and adds worldobjects to the world
        if(loaded) return;

        loaded = true;
        everLoaded = true;

        if(rootTile == null) {  //Load the root tile from the world library (Happens only once)
            for(RootObject root : world.library) {
                if(root.getID().equals(data.g.r)) rootTile = root.copy();
            }
        }

        if(rootTile == null) rootTile = com.pixxel.objects.World.MISSING.copy();
        fillGround(rootTile);

        while(!tilequeue.isEmpty()) {
            world.changeTile(world.getByID(tilequeue.get(0).id), this, xFromIndex(tilequeue.get(0).i), yFromIndex(tilequeue.get(0).i));
            tilequeue.remove(0);
        }

        while(!queue.isEmpty()) {
            WorldObject o = world.addObject(queue.get(0));
            if(queue.get(0).addr.equals(world.getWorldData().viewport.focus)) world.cameraFocus = o;
            queue.remove(0);
        }

        for(WorldObject addr : world.getObjectWithAddress()) {
            Chunk chunk = world.estimateChunk(addr.getX(), addr.getY());
            if(chunk != null) {
                if(chunk.equals(this)) attachObject(addr);
            }
        }
        world.loadedChunks.add(this);
    }

    /**Wipes the chunk. Usually called upon closing the world*/
    public void wipe() {
        release();
        fillGround(World.MISSING);
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

    public void addQueue(com.pixxel.objects.WorldValues.WorldObjectValues values) {
        queue.add(values);
    }

    /**If eraseAll is true, also the world objects get removed (Only address-less ones), saving
     * the maximum amount of data, but it will also slow down the {@link Chunk#load()} process*/
    public void release() {  //Clears the tiles and objects from this chunk and calls the garbage collector
        world.loadedChunks.remove(this);
        loaded = false;
    }

    /**Heavy operation. I recommend to call this function only, when really necessary<br>
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
        main: for(int i = 0; i < com.pixxel.objects.WorldValues.GroundValues.SQUARE; i++) {
            for(int j = 0; j < com.pixxel.objects.WorldValues.GroundValues.SQUARE; j++) {
                if(!ground[i][j].groundTile.getID().equals(currentRoot)) {
                    boolean exist = false;
                    a: for(String s : hash.keySet()) {
                        if(s.equals(ground[i][j].groundTile.getID())) {
                            int newscore = hash.get(ground[i][j].groundTile.getID()) + 1;
                            hash.put(ground[i][j].groundTile.getID(), newscore);
                            if(newscore > (com.pixxel.objects.WorldValues.GroundValues.SQUARE * com.pixxel.objects.WorldValues.GroundValues.SQUARE) / 2) break main;
                            exist = true;
                            break a;
                        }
                    }
                    if(!exist) hash.put(ground[i][j].groundTile.getID(), 1);
                }
                else score++;
            }
        }

        int half = (com.pixxel.objects.WorldValues.GroundValues.SQUARE * com.pixxel.objects.WorldValues.GroundValues.SQUARE) / 2; //Evaluate the tiles
        if(score <= half) {
            for(String s : hash.keySet()) {
                if(hash.get(currentRoot) != null) {
                    if(hash.get(s) > hash.get(currentRoot))  currentRoot = s;
                } else currentRoot = s;
                if(hash.get(s) > half) break;
            }
        }
        data.g.r = currentRoot;

        for(int i = 0; i < com.pixxel.objects.WorldValues.GroundValues.SQUARE; i++) {  //Estimate the changed chunks and swap root tile, if needed
            for(int j = 0; j < com.pixxel.objects.WorldValues.GroundValues.SQUARE; j++) {
                if(changed[i][j] != null) {
                    if(!changed[i][j].getID().equals(currentRoot)) {
                        com.pixxel.objects.WorldValues.TilePos t = new com.pixxel.objects.WorldValues.TilePos();
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
        return index % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
    }

    public int yFromIndex(int index) {
        return index / com.pixxel.objects.WorldValues.GroundValues.SQUARE;
    }

    public int indexFromTilepos(int x, int y) {
        return x + (y * WorldValues.GroundValues.SQUARE);
    }

    /**Checks if a normalized position is on the chunk*/
    public boolean onChunk(float x, float y) {
        return x > xpos && y > ypos && x < xpos+width && y < ypos+height;
    }

    /**@return true If the chunk is currently loaded*/
    public boolean loaded() {
        return loaded;
    }

    /**@return true If the chunk was ever loaded in a session*/
    public boolean everLoaded() { return everLoaded; }

    public String toString() {
        return x + " " + y;
    }
}
