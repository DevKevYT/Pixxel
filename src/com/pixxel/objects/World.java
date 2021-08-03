package com.pixxel.objects;

import java.io.IOException;
import java.util.ArrayList;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Disposable;
import com.pixxel.ai.GraphData;
import com.pixxel.Main.FileHandler;
import com.pixxel.Main.Logger;
import com.pixxel.objects.RootValues.RootObjectValues;
import com.pixxel.objects.WorldValues.WorldData;
import com.pixxel.objects.WorldValues.WorldObjectValues;

import box2dLight.RayHandler;

public final class World implements Disposable {

	public final SpriteBatch batch;
	public final com.badlogic.gdx.physics.box2d.World b2dWorld;
	public final RayHandler b2dLightWorld;
	private final Vector2 mousePos = new Vector2();

	protected final ArrayList<com.pixxel.objects.RootObject> library = new ArrayList<>();
	protected final ArrayList<WorldObject> totalObjects = new ArrayList<>();
	private final ArrayList<WorldObject> renderedObjects = new ArrayList<>();
	private final ArrayList<WorldObject> objectsWithAddress = new ArrayList<>();
	private final ArrayList<WorldObject> removeQueue = new ArrayList<>(0);
	protected final ArrayList<WorldObject> objectsWithTrigger = new ArrayList<>();
	private final ArrayList<WorldObject> requestFixture = new ArrayList<>(1);

	//Chunk and render handling
	protected Chunk[][] chunks;
	public ArrayList<Chunk> loadedChunks = new ArrayList<>();
	private int startX=0, startY=0, endX=0, endY=0;
	private int centerX=-1, centerY=-1;

	private WorldData worldData;
	private float width = 0;
	private float height = 0;
	private float pixelsPerMeter;
	private float tilesizePX;
	private float tilesizeNORM;
	public float objectHitbox = 25;
	
	private int mouseX = -1;
	private int mouseY = -1;
	private Chunk touchedChunk;

	public WorldObject selected; //Little tool, when the mouse is over, null when not
	
	private OrthographicCamera viewport;
	protected float invertedZoom = 0; //Used for drawing world fonts
	public WorldObject cameraFocus = null;
	
	public static final com.pixxel.objects.RootObject MISSING;
	private FileHandle file = null;
	public final com.pixxel.objects.Game game;

	private final Vector3 target = new Vector3();
	private final Vector3 campos = new Vector3();

	private boolean firstUpdateCall = false;
	private GraphData worldGraph;
	public com.pixxel.objects.ParticleSystem particleSystem;
	public com.pixxel.objects.CameraShake shake;

	private ArrayList<ContactListener> contactListeners = new ArrayList<>();

	static {
		RootObjectValues MISSINGVAL = new RootObjectValues();
		MISSINGVAL.id = "missing";
		MISSINGVAL.size.set(25, 25);
		MISSINGVAL.texturePath = "textures//missingtexture.png";
		MISSING = new com.pixxel.objects.RootObject(MISSINGVAL);
	}

	public World(com.pixxel.objects.Game game, SpriteBatch batch) {
		this.batch = batch;
		b2dWorld = new com.badlogic.gdx.physics.box2d.World(new Vector2(), false);
		b2dLightWorld = new RayHandler(b2dWorld);
		viewport = new OrthographicCamera();
		worldData = new WorldData();
		this.game = game;
		particleSystem = new ParticleSystem(this);
		shake = new CameraShake(this);
	}
	
	public World(com.pixxel.objects.Game game) {
		this(game, new SpriteBatch());
	}

	public void generate(Vector2 origin, float tileSize, float scale) {
		disposeTemp(false);
		WorldData temp = new WorldData();
		temp.scl = scale;
		temp.tilesizeMETERS = tileSize;
		temp.origin.set(origin);
		generate(temp);
	}
	
	public void generate(FileHandle file, ArrayList<com.pixxel.objects.RootObject> objectlibrary) {
		if(file == null) throw new IllegalArgumentException("File not found!");
		if(!file.exists()) throw new IllegalArgumentException("File not found! " + file.path());
		library.clear();
		library.addAll(objectlibrary);
		this.file = file;
		generate(FileHandler.readJSON(file, WorldData.class));
	}

	public void generate(Game game, FileHandle file, ArrayList<com.pixxel.objects.RootObject> objectlibrary) {
		if(file == null) throw new IllegalArgumentException("File not found!");
		if(!file.exists()) throw new IllegalArgumentException("File not found! " + file.path());
		library.clear();
		library.addAll(objectlibrary);
		this.file = file;
		generate(FileHandler.readJSON(file, WorldData.class));
	}
	
	/**Generates a world with the given data. <b><br>The old world state gets overwritten!</b>
	 * Its recommendet to put this function in post-runnable after loading a world in runtime.*/
	private void generate(WorldData data) {
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				if(data.scl == 0) {
					Logger.logError("World", "World scale can't be zero! Continuing with scale = 1");
					data.scl = 1;
				}
				firstUpdateCall = false;
				worldData = data;
				b2dLightWorld.setAmbientLight(worldData.ambient);
				if(worldData.viewport.smooth > 1) worldData.viewport.smooth = 1;
				else if(worldData.viewport.smooth < 0) worldData.viewport.smooth = 0;

				disposeTemp(false);
				tilesizePX = worldData.tilesizeMETERS * (WorldData.tilesizePX_default * worldData.scl);
				tilesizeNORM = worldData.tilesizeMETERS * WorldData.tilesizePX_default;
				pixelsPerMeter = 1 / worldData.scl;
				MISSING.loadTexture();

				width = worldData.sizeX * tilesizeNORM * com.pixxel.objects.WorldValues.GroundValues.SQUARE;
				height = worldData.sizeY * tilesizeNORM * com.pixxel.objects.WorldValues.GroundValues.SQUARE;

				for(com.pixxel.objects.RootObject obj : library) obj.checkSize(World.this);  //Check library

				if(worldData.sizeX > 0 && worldData.sizeY > 0) {
					chunks = new Chunk[worldData.sizeX][worldData.sizeY];
					for (int i = 0; i < worldData.sizeX; i++) {
						for (int j = 0; j < worldData.sizeY; j++) {

							if (i < worldData.chunks.length) {
								if (j < worldData.chunks[i].length) {
									chunks[i][j] = new Chunk(World.this, worldData.chunks[i][j], i, j);
									if (worldData.viewport.rd <= 0) chunks[i][j].load();
									continue;
								}
							}
							Logger.logError("World", "Loading corrupted chunk " + i + " " + j);
							chunks[i][j] = new Chunk(World.this, new com.pixxel.objects.WorldValues.ChunkData(), i, j);
							if (worldData.viewport.rd <= 0) chunks[i][j].load();
						}
					}
				}

				game.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				if(Gdx.app.getType() == Application.ApplicationType.Android) worldData.viewport.rd = com.pixxel.objects.WorldValues.ViewportData.androidRD;
				viewport.position.set(new Vector3(data.viewport.x * getScale(), data.viewport.y * getScale(), 0));
				viewport.zoom = data.viewport.zoom;

				Logger.logInfo("World", "Generating Nodes...");
				worldGraph = new GraphData(World.this, data.grapth);
				worldGraph.generateGraph(data.connections);

				Logger.logInfo("World", "Setting up contact listeners...");
				b2dWorld.setContactListener(new com.badlogic.gdx.physics.box2d.ContactListener() {
					@Override
					public void beginContact(Contact contact) {
						for(int i = 0; i < contactListeners.size(); i++) {
							if(contact.getFixtureA().getBody().getUserData() instanceof WorldObject && contact.getFixtureB().getBody().getUserData() instanceof WorldObject) {
								if(contactListeners.get(i) != null)
									contactListeners.get(i).beginContact((WorldObject) contact.getFixtureA().getBody().getUserData(), (WorldObject) contact.getFixtureB().getBody().getUserData(), contact);
							}
						}
					}

					@Override
					public void endContact(Contact contact) {
						for(int i = 0; i < contactListeners.size(); i++) {
							if(contact.getFixtureA().getBody().getUserData() instanceof WorldObject && contact.getFixtureB().getBody().getUserData() instanceof WorldObject) {
								if(contactListeners.get(i) != null)
									contactListeners.get(i).endContact((WorldObject) contact.getFixtureA().getBody().getUserData(), (WorldObject) contact.getFixtureB().getBody().getUserData(), contact);
							}
						}
					}

					@Override
					public void preSolve(Contact contact, Manifold oldManifold) {
						for(int i = 0; i < contactListeners.size(); i++) {
							if(contact.getFixtureA().getBody().getUserData() instanceof WorldObject && contact.getFixtureB().getBody().getUserData() instanceof WorldObject) {
								if(contactListeners.get(i) != null)
									contactListeners.get(i).preSolve((WorldObject) contact.getFixtureA().getBody().getUserData(), (WorldObject) contact.getFixtureB().getBody().getUserData(), contact, oldManifold);
							}
						}
					}

					@Override
					public void postSolve(Contact contact, ContactImpulse impulse) {
						for(int i = 0; i < contactListeners.size(); i++) {
							if(contact.getFixtureA().getBody().getUserData() instanceof WorldObject && contact.getFixtureB().getBody().getUserData() instanceof WorldObject) {
								if(contactListeners.get(i) != null)
									contactListeners.get(i).postSolve((WorldObject) contact.getFixtureA().getBody().getUserData(), (WorldObject) contact.getFixtureB().getBody().getUserData(), contact, impulse);
							}
						}
					}
				});

				Logger.logInfo("World", "World generated");
			}
		});
	}
	
	public com.pixxel.objects.RootObject getByID(String id) {
		for(com.pixxel.objects.RootObject obj : library) {
			if(obj.getID().equals(id)) return obj;
		}
		return MISSING;
	}

	public WorldObject[] getObjectsByAddress(String addr) {
		if(addr.equals(WorldObject.NO_ADDR) || addr.equals(WorldObject.TILE_ADDR)) return new WorldObject[]{};
		ArrayList<WorldObject> objects = new ArrayList<>(0);

		for(WorldObject obj : objectsWithAddress) {
			if(obj.getAddress().equals(addr)) objects.add(obj);
		}

		if(!objects.isEmpty()) return objects.toArray(new WorldObject[objects.size()]);
		else return new WorldObject[]{};
	}

	/**Returns the first occurrence of an object with the given address or null*/
	public WorldObject getObjectByAddress(String addr) {
		for(WorldObject obj : objectsWithAddress) {
			if(obj.getAddress().equals(addr)) return obj;
		}
		return null;
	}

	/**If the onCreate new is set to true, it will onCreate a new world file in the local folder, if the file
	 * does not exist*/
	public boolean save() {
		Logger.logInfo("World", "Saving world");
		try {
			if(file != null) FileHandler.writeJSON(file, getFullData(), false);
			else return false;
		} catch (IOException e) {
			Logger.logError("World", "World failed to save: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**Returns the file the world is loaded from<br>
	 * May be null*/
	public FileHandle getFile() {
		return file;
	}
	
	/**Sets the render distance. use 0 for unlimited distance. Default is 0*/
	public void setRenderDistance(int renderDistance) {
		worldData.viewport.rd = renderDistance;
		if(worldData.viewport.rd < 0) worldData.viewport.rd = 0;
	}

	public void resizeViewport(int width, int height) {
		viewport.setToOrtho(false, width * getScale(), height * getScale());

		viewport.position.set(worldData.viewport.x * getScale(), worldData.viewport.y * getScale(), 0);
		viewport.zoom = worldData.viewport.zoom;
		viewport.update();
	}
	
	public void setViewport(OrthographicCamera viewport, int width, int height) {
		this.viewport = viewport;
		resizeViewport(width, height);
	}

	public OrthographicCamera getViewport() {
		return viewport;
	}

	public void updateViewport() {
		if(cameraFocus != null && worldData.viewport.center) {
			target.set(cameraFocus.getPosition().scl(getScale()), 0);
			campos.set(viewport.position);
			campos.scl(worldData.viewport.smooth);
			target.scl(1 - worldData.viewport.smooth);
			campos.add(target);
			if(worldData.viewport.edge) {
				if(viewport.position.x - viewport.viewportWidth*viewport.zoom*.5f <= getOrigin().x && campos.x-viewport.viewportWidth*viewport.zoom*.5f <= getOrigin().x) campos.x = viewport.position.x;
				if(viewport.position.x - viewport.viewportWidth*viewport.zoom*.5f < getOrigin().x) campos.x = getOrigin().x + viewport.viewportWidth*viewport.zoom*.5f;

				if(viewport.position.x + viewport.viewportWidth*viewport.zoom*.5f >= getWorldWidth()*getScale() && campos.x + viewport.viewportWidth*viewport.zoom*.5f >= getWorldWidth()*getScale()) campos.x = viewport.position.x;
				if(viewport.position.x + viewport.viewportWidth*viewport.zoom*.5f > getWorldWidth()*getScale()) campos.x = getWorldWidth()*getScale() - viewport.viewportWidth*viewport.zoom*.5f;

				if(viewport.position.y + viewport.viewportHeight*viewport.zoom*.5f >= getWorldHeight()*getScale() && campos.y + viewport.viewportHeight*viewport.zoom*.5f >= getWorldHeight()*getScale()) campos.y = viewport.position.y;
				if(viewport.position.y + viewport.viewportHeight*viewport.zoom*.5f > getWorldHeight()*getScale()) campos.y = getWorldHeight()*getScale() - viewport.viewportHeight*viewport.zoom*.5f;

				if(viewport.position.y - viewport.viewportHeight*viewport.zoom*.5f <= getOrigin().y && campos.y-viewport.viewportHeight*viewport.zoom*.5f <= getOrigin().y) campos.y = viewport.position.y;
				if(viewport.position.y - viewport.viewportHeight*viewport.zoom*.5f < getOrigin().y) campos.y = getOrigin().y + viewport.viewportHeight*viewport.zoom*.5f;
			}
			viewport.position.set(campos);
			worldData.viewport.x = viewport.position.x * getPPM();
			worldData.viewport.y = viewport.position.y * getPPM();
		} else campos.set(viewport.position);

		invertedZoom = 1f/viewport.zoom;
		viewport.update();
	}
	
	public void updateRendering() {
		if(worldData.viewport.rd <= 0) {
			startX = 0;
			startY = 0;
			endX = getSizeX()-1;
			endY = getSizeY()-1;
			return;
		}

		Chunk load = estimateChunk(viewport.position.x*getPPM(), viewport.position.y*getPPM());
		startX = load.x - worldData.viewport.rd;
		startY = load.y - worldData.viewport.rd;
		endX = load.x + worldData.viewport.rd+1;
		endY = load.y + worldData.viewport.rd+1;
		if(startX < 0) startX = 0;
		if(startY < 0) startY = 0;
		if(endX > getSizeX()) endX = getSizeX();
		if(endY > getSizeY()) endY = getSizeY();

		//Check for chunk center change
		if(centerX != load.x || centerY != load.y) {
			for(int i = 0; i < loadedChunks.size(); i++) { //Remove chunk from loaded chunk list, if out of render distance
				if(loadedChunks.get(i).x < startX || loadedChunks.get(i).x > endX || loadedChunks.get(i).y < startY || loadedChunks.get(i).y > endY) {
					objectMoved = true;
					loadedChunks.get(i).release();
					i--;
				}
			}

			for(int x = startX; x < endX; x++) {
				for(int y = startY; y < endY; y++) {
					chunks[x][y].load();
				}
			}
		}
		centerX = load.x;
		centerY = load.y;
	}
	
	public void update(float delta) {
		if(b2dWorld.getBodyCount() > 0 && !b2dWorld.isLocked())
			b2dWorld.step(delta * getScale(), 6, 2);
		b2dLightWorld.update();

		objectHitbox = 8*viewport.zoom;
		updateRendering();
		mousePos.set(unproject(Gdx.input.getX(), Gdx.input.getY()));
		mouseX = -1;
		mouseY = -1;

		shake.tick(delta);

		boolean destroyedHitbox = false;
		while(!removeQueue.isEmpty() && !b2dWorld.isLocked() && !destroyedHitbox) {
			if(removeQueue.get(0).hasHitbox()) destroyedHitbox = true;
			renderedObjects.remove(removeQueue.get(0));
			requestFixture.remove(removeQueue.get(0));
			destroyObject(removeQueue.get(0));
			removeQueue.remove(0);
		}

		if(!requestFixture.isEmpty()) {
			requestFixture.get(0).createFixtures();
			requestFixture.remove(0);
		}

		if(!firstUpdateCall) {
			game.chat.executeScript(getWorldData().loadScript);
			firstUpdateCall = true;
		}
	}

	public boolean objectMoved = false;
	public void draw(float delta) {

		if(objectMoved) renderedObjects.clear();
		batch.setProjectionMatrix(viewport.combined);
		batch.begin();

		for(Chunk chunk : loadedChunks) {
			for(int x = 0; x < com.pixxel.objects.WorldValues.GroundValues.SQUARE; x++) {  //A single chunk
				for(int y = 0; y < com.pixxel.objects.WorldValues.GroundValues.SQUARE; y++) {
					batch.setColor(Color.WHITE);
					if(chunk.ground[x][y].onTile(mousePos.x, mousePos.y, getScale())) {
						touchedChunk = chunk;
						mouseX = x;
						mouseY = y;
					}
					chunk.ground[x][y].groundTile.draw(batch, this);
				}
			}
			if(objectMoved) sort(chunk);  //Sort and add objects to get rendered
		}

		selected = null;
		objectMoved = false;
		for(WorldObject obj : renderedObjects) {
			if(selected == null) {
				if(mousePos.dst(toWorld(obj.getPosition())) <= objectHitbox*worldData.scl) selected = obj;
			}
			obj.update(delta);  //Moved flag gets resetted.
			obj.draw(batch);
		}
		particleSystem.updateAndDraw(delta);
		batch.end();

		b2dLightWorld.setCombinedMatrix(viewport.combined);
		b2dLightWorld.render();
	}
	
	private void sort(Chunk newobjects) {
		for(int i = 0; i < newobjects.objects.size(); i++) {
			WorldObject obj = newobjects.objects.get(i);
			if(obj == null) continue;
			if(obj.getsDestroyed()) continue;
			float y = obj.getPosition().y-obj.getTextureOffset().y-obj.getYOff();
			int index = 0;
			for(WorldObject rend : renderedObjects) {
				if(y <= rend.getPosition().y-obj.getTextureOffset().y-rend.getYOff()) index++;
			}
			renderedObjects.add(index, obj);
		}
	}

	public void requestFixture(WorldObject object) {
		requestFixture.add(object);
	}

	/**Dumps the object safely at the end of the next {@link World#update(float)} function.
	 * If the object has a trigger, the object is removed, when the script finished*/
	public void removeObject(WorldObject object) {
		if(object.getsRemoved) return;
		if(object.getTrigger() != null) {
			object.getTrigger().removeTrigger();
			object.removeTrigger();
		}

		object.setGroupIndex(999);
		object.getsRemoved = true;
		removeQueue.add(object);
	}

	boolean disposing = false;
	/**Instantly destroys an object. <br><b>This may lead to exceptions!</b><br>
	 * It is recommendet to use the World {@link World#removeObject()} function.
	 * This function adds the object to a queue and is removed at the end of the update function from the world {@link #update(float)}
	 * and dumps the object safely!*/
	public void destroyObject(WorldObject object) {
		if(object.binding != null) object.binding.objects.remove(object);
		object.dispose();

		if(!object.getAddress().equals(WorldObject.NO_ADDR) && !disposing) {
			objectsWithAddress.remove(object);
			if(cameraFocus != null) {
				if (cameraFocus.equals(object)) {
					//Search for the next best object to focus
					cameraFocus = getObjectByAddress(object.getAddress());
				}
			}
		}
		if(object.getTrigger() != null && !disposing) {
			object.getTrigger().removeTrigger();
			object.removeTrigger();
		}
		for(int i = 0; i < contactListeners.size(); i++) {
			if(contactListeners.get(i).owner != null) {
				if(contactListeners.get(i).owner.equals(object)) {
					contactListeners.remove(i);
				}
			}
		}
		totalObjects.remove(object);
		renderedObjects.remove(object);
	}

	public void changeTile(com.pixxel.objects.RootObject root, int tileX, int tileY) {
		int chunkTileX = tileX / com.pixxel.objects.WorldValues.GroundValues.SQUARE;
		int chunkTileY = tileY / com.pixxel.objects.WorldValues.GroundValues.SQUARE;
		if(chunkTileX < 0 || chunkTileX >= worldData.sizeX || chunkTileY < 0 || chunkTileY >= worldData.sizeY) return;
		changeTile(root, chunks[chunkTileX][chunkTileY], tileX % com.pixxel.objects.WorldValues.GroundValues.SQUARE, tileY % com.pixxel.objects.WorldValues.GroundValues.SQUARE);
	}
	
	public void changeTile(com.pixxel.objects.RootObject object, Chunk chunk, int tileX, int tileY) {
		if(tileX >= 0 && tileY >= 0 && tileX < com.pixxel.objects.WorldValues.GroundValues.SQUARE && tileY < com.pixxel.objects.WorldValues.GroundValues.SQUARE) {
			boolean wasLoaded = chunk.loaded();
			chunk.load();  //Theres no way around...

			com.pixxel.objects.RootObject newtileRoot = object.copy();  //Root configuration as tile
			newtileRoot.values.fixedRotation = true;
			newtileRoot.values.type = 1;
			newtileRoot.loadTexture();

			Tile oldtile = chunk.ground[tileX][tileY];
			if(oldtile.groundTile != null) oldtile.groundTile.dispose();

			WorldObject wobj = new WorldObject(newtileRoot, new Vector2(chunk.xpos + tileX * tilesizeNORM + tilesizeNORM*.5f,
					chunk.ypos + tileY * tilesizeNORM + tilesizeNORM*.5f));
			wobj.setSize(new Vector2(tilesizeNORM, tilesizeNORM));
			wobj.setScale(getScale());

			if(wobj.hasHitbox()) wobj.addToWorld(this, "");
			chunk.ground[tileX][tileY] = new Tile(wobj, tileX, tileY, chunk);
			chunk.ground[tileX][tileY].objects.addAll(oldtile.objects);
			chunk.changed[tileX][tileY] = newtileRoot;
			if(!wasLoaded) chunk.release();
		}
	}

	public void expandWidth(int amount) {
		if(worldData.sizeX+amount <= 0) return;
		save();
		worldData.sizeX+=amount;
		generate(worldData);
	}

	public void expandHeight(int amount) {
		if(worldData.sizeY+amount <= 0) return;
		save();
		worldData.sizeY+=amount;
		generate(worldData);
	}

	public void expandX(int amount) {
		//Step 1 expand width
		expandWidth(amount);
		//Step 2: Move tiles
		for(int x = worldData.sizeX-amount-1; x >= 0; x--) {
			for(int y = 0; y < worldData.sizeY; y++) {
				Gdx.app.log("Debug", "Copying chunk " + x + " " +  " to " + (x+amount) + " " + y);
				Chunk.copyChunk(chunks[x][y], chunks[x+amount][y]);
			}
		}

		for(int x = 0; x < amount; x++) {
			for(int y = 0; y < worldData.sizeY; y++) {
				chunks[x][y].wipe();
			}
		}
	}

	public void expandY(int amount) {
		expandHeight(amount);
	}

	/**Searches the library {@link World#library} for the root object. May be slow!<br>
	 * I recomment to use the {@link World#addObject(WorldObjectValues, com.pixxel.objects.RootObject)} function, where the
	 * root object is already set.*/
	public WorldObject addObject(WorldObjectValues values) {
		com.pixxel.objects.RootObject root = getByID(values.id);
		if(root != null) return addObject(values, root);
		else return addObject(values, MISSING);
	}
	
	//Core function for adding objects!
	public WorldObject addObject(WorldObjectValues values, com.pixxel.objects.RootObject root) {
		//if(!root.textureLoaded()) root.loadTexture();
		WorldObject worldObject = new WorldObject(values, root);  //Prepare to add object

		String address = values.addr;  //Setting up addresses
		if(address.equals(WorldObject.TILE_ADDR)) address = WorldObject.NO_ADDR;

		worldObject.addToWorld(this, address);

		if(!address.equals(WorldObject.NO_ADDR)) objectsWithAddress.add(worldObject);

		Chunk c = estimateChunk(values.x, values.y);
		if(c != null) c.attachObject(worldObject);

		totalObjects.add(worldObject);  //Already add object to totalObjects, so non added objects later remain registered
		return worldObject;
	}
	
	public WorldObject addObject(RootObject root, Vector2 position) {
		if(!root.textureLoaded()) root.loadTexture();
		WorldObjectValues v = new WorldObjectValues();
		v.x = position.x;
		v.y = position.y;
		v.rotation = root.values.initRotation;
		return addObject(v, root);
	}


	public ArrayList<Tile> getTilesInRadius(float x, float y, int sizeX, int sizeY, boolean wake, ArrayList<Tile> list) {
		sizeX = Math.abs(sizeX);  //Radius is always positive
		sizeY = Math.abs(sizeY);

		Tile currentTile = estimateTile(x, y, wake);
		if(currentTile == null) return new ArrayList<>(0);
		Chunk currentChunk = currentTile.chunk;
		if(currentChunk == null) return new ArrayList<>(0);

		int shiftX = (currentTile.getX() - sizeX + sizeX * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;  //Dude this was pain. Sat a long time on this...
		int chunkStepX = (sizeX + ((-sizeX + currentTile.getX() + sizeX * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE)) / com.pixxel.objects.WorldValues.GroundValues.SQUARE;

		int shiftY = (currentTile.getY() - sizeY + sizeY * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
		int chunkStepY = (sizeY + ((-sizeY + currentTile.getY() + sizeY * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE)) / com.pixxel.objects.WorldValues.GroundValues.SQUARE;

		for(int i = /*-sizeX*/0, xIndex=0; i<=sizeX; i++, xIndex++) {
			int indexX = (i + currentTile.getX() + sizeX* com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
			int chunkIndexX = ((xIndex + shiftX) / com.pixxel.objects.WorldValues.GroundValues.SQUARE) - chunkStepX + currentChunk.x;

			for(int j = /*-sizeY*/0, yIndex=0; j <= sizeY; j++, yIndex++) {

				int indexY = (j + currentTile.getY() + sizeY * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
				int chunkIndexY = ((yIndex + shiftY) / com.pixxel.objects.WorldValues.GroundValues.SQUARE) - chunkStepY + currentChunk.y;

				Chunk c = getChunk(chunkIndexX, chunkIndexY);
				if(c != null) {
					if(wake) c.load();
					if(c.loaded()) list.add(c.ground[indexX][indexY]);
				}
			}
		}
		return list;
	}

	/**Returns all objects in a specified radius (Square).<br>Radius in tiles (5 Tiles = 1 Chunk)
	 *<br>Returns an empty list, if the position has no objects inside the given radius, or the position os outside of the map, never null)
	 * <br>x and y as normalized (unscaled to world) position!
	 * @param x X position (Normalized)
	 * @param y Y position (Normalized)
	 * @param radius Radius in tiles (You can use World{@link #getTileSizePX()} to calculate precise distances
	 * @param wake If the check should wake unloaded chunks while checking for neighbors*/
	public ArrayList<WorldObject> getObjectsInRadius(float x, float y, int radius, boolean wake, ArrayList<WorldObject> list) {
		radius = Math.abs(radius);  //Radius is always positive

		Tile currentTile = estimateTile(x, y, wake);
		if(currentTile == null) return new ArrayList<>(0);
		Chunk currentChunk = currentTile.chunk;
		if(currentChunk == null) return new ArrayList<>(0);

		int shiftX = (currentTile.getX() - radius + radius* com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;  //Dude this was pain. Sat a long time on this...
		int chunkStepX = (radius + ((-radius + currentTile.getX() + radius * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE)) / com.pixxel.objects.WorldValues.GroundValues.SQUARE;

		int shiftY = (currentTile.getY() - radius + radius * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
		int chunkStepY = (radius + ((-radius + currentTile.getY() + radius * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE)) / com.pixxel.objects.WorldValues.GroundValues.SQUARE;

		for(int i = -radius, xIndex=0; i<=radius; i++, xIndex++) {
			int indexX = (i + currentTile.getX() + radius* com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
			int chunkIndexX = ((xIndex + shiftX) / com.pixxel.objects.WorldValues.GroundValues.SQUARE) - chunkStepX + currentChunk.x;

			for(int j = -radius, yIndex=0; j <= radius; j++, yIndex++) {

				int indexY = (j + currentTile.getY() + radius * com.pixxel.objects.WorldValues.GroundValues.SQUARE) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
				int chunkIndexY = ((yIndex + shiftY) / com.pixxel.objects.WorldValues.GroundValues.SQUARE) - chunkStepY + currentChunk.y;

				Chunk c = getChunk(chunkIndexX, chunkIndexY);
				if(c != null) {
					if(wake) c.load();
					if(c.loaded()) list.addAll(c.ground[indexX][indexY].objects);
				}
			}
		}
		return list;
	}

	public float getScale() {
		return worldData.scl;
	}
	
	public int touchedTileX() {
		return mouseX;
	}
	
	public int touchedTileY() {
		return mouseY;
	}
	
	public Vector2 getOrigin() {
		return new Vector2(worldData.origin);
	}

	public float getWorldWidth() {
		return width;
	}

	public float getWorldHeight() {
		return height;
	}

	/**Translates a point with world coordinates to a point projected in the given camera.
	 * Remember to scale the normalized worldobject position with the worlds scale {@link World#getScale()}*/
	public Vector2 translateTo(float x, float y, boolean flip, Camera other) {
		Vector3 point = viewport.project(new Vector3(x, y, 0));
		Vector3 translate = other.unproject(point);
		translate.y = -translate.y + other.viewportHeight;
		return new Vector2(translate.x, translate.y);
	}

	/**Translates a point with world coordinates to a point projected in the given camera.
	 * Remember to scale the normalized worldobject position with the worlds scale {@link World#getScale()}*/
	public Vector2 translateTo(Vector2 position, boolean flip, Camera other) {
		Vector3 point = viewport.project(new Vector3(position.x, position.y, 0));
		Vector3 translate = other.unproject(point);
		translate.y = -translate.y + other.viewportHeight;
		return new Vector2(translate.x, translate.y);
	}

	private final Vector3 unprojectTemp = new Vector3();
	private final Vector3 positionTemp = new Vector3();
	/**Unprojects the coordinates by the camera. (The position is automatically scaled to world position)*/
	public Vector2 unproject(float x, float y) {
		positionTemp.set(x, y,0);
		unprojectTemp.set(viewport.unproject(positionTemp));
		return new Vector2(unprojectTemp.x, unprojectTemp.y);
	}
	
	public Vector2 unproject(Vector2 pos) {
		return unproject(pos.x, pos.y);
	}

	/**Translates a given point to the default window viewport, unlike in {@link World#translateTo(float, float, OrthographicCamera)},
	 * wich translates coordinates to another viewport.*/
	public Vector2 translateToWindow(float x, float y) {
		float x1 = x - viewport.position.x - viewport.viewportWidth;
		float y1 = y - viewport.position.y - viewport.viewportHeight;
		return new Vector2(x1 * viewport.zoom, y1 * viewport.zoom);
	}

	/**@see World#translateToWindow(float x, float y) */
	public Vector2 translateToWindow(Vector2 pos) {
		return translateToWindow(pos.x, pos.y);
	}
	
	public Vector2 toScreen(float x, float y) {
		return new Vector2((x - worldData.origin.x) * pixelsPerMeter, (y - worldData.origin.y) * pixelsPerMeter);
	}
	
	public Vector2 toScreen(Vector2 worldPos) {
		return toScreen(worldPos.x, worldPos.y);
	}
	
	/**Converts the given screen coordinate to this world coordinates*/
	public Vector2 toWorld(float x, float y) {
		return new Vector2((x + worldData.origin.x) * worldData.scl, (y + worldData.origin.y) * worldData.scl);
	}
	
	public Vector2 toWorld(Vector2 v) {
		return toWorld(v.x, v.y);
	}
	
	/**Don't use this function too aggressive. Returns a copy of this data*/
	public WorldData getWorldData() {
		return worldData;
	}

	//@Deprecated
	public Tile getTouchedTile() {
		if(touchedChunk != null && mouseX != -1 && mouseY != -1) {
			return touchedChunk.ground[mouseX][mouseY];
		} else return null;
	}

	public Chunk getTouchedChunk() {
		return touchedChunk;
	}
	
	public Vector2 getMousePos() {
		return mousePos.cpy();
	}
	
	public float getPPM() {
		return pixelsPerMeter;
	}
	
	public float getTileSizePX() {
		return tilesizePX;
	}
	
	/**Returns the normaized tile size in pixels. (Normalized means a world with scale 1)*/
	public float getTileSizeNORM() {
		return tilesizeNORM;
	}
	
	public ArrayList<WorldObject> getWorldObjects() {
		return totalObjects;
	}
	
	/**Returns a world object that match the given address. (0 returns instantly null!)*/
	public WorldObject getWorldObject(String address) {
		if(address.equals(WorldObject.NO_ADDR)) return null;
		
		for(WorldObject obj : totalObjects) {
			if(obj.getAddress().equals(address)) return obj;
		}
		return null;
	}
	
	public ArrayList<WorldObject> getRenderedObjects() {
		return renderedObjects;
	}

	public ArrayList<WorldObject> getObjectWithAddress() { return objectsWithAddress; }

	public ArrayList<WorldObject> getObjectsWithTrigger() {return objectsWithTrigger;}

	public WorldData getFullData() {
		Logger.logInfo("World", "Preparing full data...");
		WorldData data = worldData.copy();

		Logger.logInfo("World", "Copying chunks...");
		data.chunks = new com.pixxel.objects.WorldValues.ChunkData[data.sizeX][data.sizeY];
		for(int i = 0; i < data.sizeX; i++) {
			for(int j = 0; j < data.sizeY; j++) {
				for(WorldObject obj : chunks[i][j].objects) {
					if(!obj.getBehavior().isEmpty()) {
						Logger.logInfo("World", "Saving behavior variables... " + obj.getID());
						obj.triggerBehaviorSave();
					}
				}

				data.chunks[i][j] = new com.pixxel.objects.WorldValues.ChunkData();
				data.chunks[i][j] = chunks[i][j].getData();
			}
		}
		update(Gdx.graphics.getDeltaTime());
		for(int i = 0; i < data.sizeX; i++) {
			for(int j = 0; j < data.sizeY; j++) {
				data.chunks[i][j] = new com.pixxel.objects.WorldValues.ChunkData();
				data.chunks[i][j] = chunks[i][j].getData();
			}
		}

		Logger.logInfo("World", "Copying viewport data...");
		//Persist viewport
		Vector2 norm = toScreen(campos.x, campos.y);
		data.viewport.x = norm.x;
		data.viewport.y = norm.y;
		data.viewport.zoom = viewport.zoom;

		if(cameraFocus != null) {
			if(!cameraFocus.getAddress().equals(WorldObject.TILE_ADDR)) data.viewport.focus = cameraFocus.getAddress();
		}
		Logger.logInfo("World", "Saving Node data...");
		data.grapth = worldGraph.getNodeData();
		data.connections = worldGraph.getConnectionData();
		Logger.logInfo("World", "Done");
		return data;
	}

	public void addContactListener(ContactListener contactListener) {
		this.contactListeners.add(contactListener);
	}

	public ArrayList<ContactListener> getContactListeners() {
		return contactListeners;
	}

	public void clearContactListeners() {
		this.contactListeners.clear();
	}

	public void setAmbientLight(float ambient) {
		b2dLightWorld.setAmbientLight(ambient);
		worldData.ambient = ambient;
	}

	/**Tries to find the chunk, the position is on (Normalized!) from the world data*/
	public Chunk estimateChunk(float x, float y) {
		if(chunks == null) return null;

		int chunkX = (int) (x / tilesizeNORM / com.pixxel.objects.WorldValues.GroundValues.SQUARE);
		int chunkY = (int) (y / tilesizeNORM / com.pixxel.objects.WorldValues.GroundValues.SQUARE);
		if(chunkX < 0) chunkX = 0;
		else if(chunkX > worldData.sizeX-1) chunkX = worldData.sizeX-1;
		if(chunkY < 0) chunkY = 0;
		else if(chunkY > worldData.sizeY-1) chunkY = worldData.sizeY-1;
		return chunks[chunkX][chunkY];
	}

	/**Calculates the tile index of the specified loaded chunk.
	 * Returns null, when the chunk is not loaded
	 * <br>If wake is true and the x and y coordinates lay on a chunk, that has never been loaded (Ground = null), it will load the chunk (May cause lag),
	 * otherwise it would return null*/
	public Tile estimateTile(float x, float y, boolean wake) {
		Chunk c = estimateChunk(x, y);
		int tileX = (int) (x / tilesizeNORM) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;
		int tileY = (int) (y / tilesizeNORM) % com.pixxel.objects.WorldValues.GroundValues.SQUARE;

		if(tileX < 0) tileX = 0;
		else if(tileX > com.pixxel.objects.WorldValues.GroundValues.SQUARE) tileX = com.pixxel.objects.WorldValues.GroundValues.SQUARE-1;

		if(tileY < 0) tileY = 0;
		else if(tileY >= com.pixxel.objects.WorldValues.GroundValues.SQUARE) tileY = WorldValues.GroundValues.SQUARE-1;

		if(!c.everLoaded() && wake) c.load();
		else if(!c.everLoaded() && !wake) return null;

		return c.ground[tileX][tileY];
	}

	public void disposeTemp(boolean clearLibrary) {
		Gdx.app.log("world", "Disposing world with " + totalObjects.size() + " objects!");
		mouseX = -1;
		mouseY = -1;
		disposing = true;
		while(totalObjects.size() > 0) destroyObject(totalObjects.get(0));
		disposing = false;
		renderedObjects.clear();
		objectsWithAddress.clear();
		objectsWithTrigger.clear();
		loadedChunks.clear();
		startX = 0;
		startY = 0;
		endX = 0;
		endY = 0;
		cameraFocus = null;
		selected = null;
		centerX = -1;
		centerY = -1;
		chunks = null;
		campos.x = 0;
		campos.y = 0;
		firstUpdateCall = false;
		if(worldGraph != null) worldGraph.dispose();
		if(clearLibrary) library.clear();
		b2dWorld.setContactListener(null);
		contactListeners.clear();
		Gdx.app.log("world", "World resetted!");
	}

	public Chunk[][] getChunks() {
		return chunks;
	}

	public Chunk getChunk(int x, int y) {
		if(x >= 0 && y >= 0 && x < getSizeX() && y < getSizeY()) {
			return chunks[x][y];
		} else return null;
	}

	/**This function is for read only, please do not modify it*/
	public ArrayList<WorldObject> getRemoveQueue() {
		return removeQueue;
	}

	public GraphData getGraph() {
		return worldGraph;
	}

	@Override
	public void dispose() {
		b2dWorld.dispose();
		b2dLightWorld.dispose();
	}

	public int getSizeX() {
		return worldData.sizeX;
	}

	public int getSizeY() {
		return worldData.sizeY;
	}

	@Override
	public String toString() {
		if(file != null) return "World (" + file.file().getAbsolutePath() + ")";
		else return "World (no location)";
	}
}
