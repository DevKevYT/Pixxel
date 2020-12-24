package com.mygdx.objects;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.objects.RootValues.Fixture;
import com.mygdx.objects.RootValues.RootObjectValues;
import com.mygdx.objects.WorldValues.TriggerValues;
import com.mygdx.objects.WorldValues.WorldObjectValues;
import com.mygdx.utils.Tools;

import box2dLight.PointLight;

/**A class for an Object that is actually drawn on the World.*/
public class WorldObject implements Disposable {

	public float minVel = .01f;
	public float minAngVel = .01f;
	
	private World world;
	private final RootObject rootObject;  //For live use, accepts changes, but thei're not persistent. 
	public WorldObjectValues worldObjectValues;  //For persisted use accepts all changes and is saveable
	
	private Body hitboxBody;  //null until Object added in a b2dworld
	
	private boolean added = false; //True, when object is present in b2dworld
	private boolean hitboxCreated = false;

	protected Tile bindingTile;
	protected Chunk binding;

	/**This address have objects 'without' an address*/
	public static final String NO_ADDR = "";
	/**Tiles have this address. Gets set to {@link WorldObject#NO_ADDR}, when a normal object has this address.*/
	public static final String TILE_ADDR = "tile";
	/**Forces the world to set a random, unique address.*
	public static final long FORCE_ADDR = 2;*/
	/**This address is reserved for the player*/

	private Trigger trigger;
	private boolean moved = false;

	protected ArrayList<Behavior> behaviors = new ArrayList<>();
	private PointLight pointlight = null;

	protected boolean getsRemoved = false;

	HashData hashWrapper;

	/**Position is relative to the world's bottom left corner (origin), where the object gets added in.
	 * If this object is added as tile in the world, the position is ignored.
	 * @param position - The normalized position. You can use {@link World#toScreen(float, float)}*/
	WorldObject(RootObject rootObject, Vector2 position) {
		this.worldObjectValues = new WorldObjectValues(); 
		this.rootObject = rootObject.copy();
		
		worldObjectValues.id = this.rootObject.getID();
		worldObjectValues.x = position.x;
		worldObjectValues.y = position.y;
		worldObjectValues.id = rootObject.values.id;
		worldObjectValues.applyChanges(this.rootObject);
	}
	
	/**Loads {@link WorldObjectValues} into the world object, witch defines a position etc.
	 * Overwrites the {@link RootObjectValues#rotation}*/
	WorldObject(WorldObjectValues data, RootObject object) { //Rotation, position and id are already in 'data'
		this.rootObject = object.copy();
		this.worldObjectValues = data.copy();
		this.worldObjectValues.id = object.getID();
		this.worldObjectValues.applyChanges(rootObject);

		if(!object.values.texturePath.equals(rootObject.values.texturePath) || object.values.tX != rootObject.values.tX ||
				object.values.tY != rootObject.values.tY  || object.values.tW != rootObject.values.tW || object.values.tH != rootObject.values.tH) {
			Gdx.app.postRunnable(new Runnable() {
				public void run() {
					rootObject.setTexture(null);
					rootObject.loadTexture();
				}
			});
		}
	}

	/**Adds the object to a box2d world <b>(Only if it has a hit box!)</b> Auto scale
	 * @param world - The world the object should get added into.
	 * @param addr - The address of the object. Dosen't need to be unique*/
	public final Body addToWorld(World world, String addr) { //Uses worldObject values for rotation etc, overwrites position
		if(added) return hitboxBody;
		if(!rootObject.textureLoaded()) {
			Gdx.app.postRunnable(new Runnable() {
				public void run() {
					rootObject.loadTexture();
				}
			});
		}
		this.world = world;
		rootObject.checkSize(world);
		//Vector2 worldPos = new Vector2(worldObjectValues.x, worldObjectValues.y).scl(world.getScale());
		setScale(rootObject.values.scale * world.getWorldData().scl); //"Adds" the world scale to maintain previous, custom scale
		rootObject.values.textureOffset.scl(rootObject.values.scale);
		createFixtures();

		//apply trigger, if exist
		if(worldObjectValues.change.trigger != null) setTrigger(worldObjectValues.change.trigger);
		//Apply point lights, if exist
		createLight(rootObject.values.light);

		worldObjectValues.addr = addr;
		if(addr.equals(world.getWorldData().viewport.focus) && !addr.isEmpty()) world.cameraFocus = this;
		added = true;
		rootObject.checkSize(world);
		hashWrapper = new HashData(worldObjectValues.hash);

		//Try to apply behavior classes and fire the onCreate event
		for(WorldValues.BehaviorValues s : worldObjectValues.behaviors) {
			try {
				for(Behavior b : this.behaviors) {
					if(b.getID().equals(s.id)) {
						Logger.logError("WorldObject", "Object behaviors need to have an unique id! (Behavior with id " + s.id + " already added!");
						continue;
					}
				}
				Behavior b = (Behavior) Class.forName(s.classPath).getConstructor(WorldValues.BehaviorValues.class).newInstance(s);
				behaviors.add(b);
			} catch (Exception e) {
				Logger.logError("WorldObject " + getID(), "Unable to load behavior class for " + toString() + ":");
				e.printStackTrace();
			}
		}
		for(int i = 0; i < behaviors.size(); i++) {
			try {
				behaviors.get(i).addToWorld(world, this);
				Behavior b = behaviors.get(i);
				if(behaviors.get(i).doPostCreate) {
					Gdx.app.postRunnable(new Runnable() {
						public void run() {
							if (b.values.enabled) b.doPostCreate();
						}
					});
				}
			} catch (Exception e) {
				Logger.logError("WorldObject " + getID(), "Unable to onCreate behavior class for " + toString() + ":");
				e.printStackTrace();
			}
		}

		world.objectMoved = true;
		return hitboxBody;
	}

	public void createFixtures() {
		hitboxCreated =  false;
		if(hitboxBody != null) world.b2dWorld.destroyBody(hitboxBody);
		if(rootObject.values.fixtures == null) return;

		BodyDef bdef = new BodyDef();
		bdef.fixedRotation = rootObject.values.fixedRotation;

		if(rootObject.values.type == RootObjectValues.STATIC) bdef.type = BodyDef.BodyType.StaticBody;
		else if(rootObject.values.type == RootObjectValues.DYNAMIC) bdef.type = BodyDef.BodyType.DynamicBody;
		else bdef.type = BodyDef.BodyType.KinematicBody;

		rootObject.bodyDef = bdef;
		hitboxBody = world.b2dWorld.createBody(rootObject.bodyDef);
		for(Fixture f : rootObject.values.fixtures) {
			FixtureDef def = new FixtureDef();
			def.density = f.density;

			if((f.width == 0 || f.height == 0) && !f.isCircle) {
				Gdx.app.log("Warning " + getID(), "Width/Height = 0");
				continue;
			} else if(f.isCircle && f.width == 0) {
				Gdx.app.log("Warning " + getID(), "Radius (width) = 0");
				continue;
			}

			if(f.isCircle) {
				CircleShape shape = new CircleShape();
				shape.setPosition(new Vector2(f.xOff * rootObject.values.scale, f.yOff * rootObject.values.scale));
				shape.setRadius(f.width * rootObject.values.scale * .5f);
				def.shape = shape;
			} else {
				PolygonShape shape = new PolygonShape();
				shape.setAsBox(f.width*.5f*rootObject.values.scale, f.height*.5f*rootObject.values.scale, new Vector2(f.xOff * rootObject.values.scale, f.yOff * rootObject.values.scale), 0);
				def.shape = shape;
			}

			this.rootObject.fixtures.add(def);
			hitboxBody.createFixture(def);
		}
		hitboxBody.setUserData(this);
		setFilterData(worldObjectValues.categoryBits, worldObjectValues.maskBits, worldObjectValues.groupIndex);
		hitboxCreated = true;
		hitboxBody.setTransform(new Vector2(worldObjectValues.x, worldObjectValues.y).scl(world.getScale()), worldObjectValues.rotation * MathUtils.degreesToRadians);
	}

	/**Creates a light. use createLight(null) to remove any existing lightsource*/
	public void createLight(RootValues.LightSource values) {
		if(values != null) {
			worldObjectValues.change.light = values.copy();
			worldObjectValues.applyChanges(rootObject);
		} else {
			rootObject.values.light = null;
			worldObjectValues.change.light = null;
		}

		if(pointlight != null) {
			pointlight.remove(true);
			pointlight = null;
		}

		if(values != null) {
			PointLight light = new PointLight(world.b2dLightWorld, rootObject.values.light.rays, new Color(rootObject.values.light.r, rootObject.values.light.g, rootObject.values.light.b, rootObject.values.light.a),
					rootObject.values.light.dist * world.getScale(), getPosition().x*world.getScale() + rootObject.values.light.x * world.getScale(), getPosition().y*world.getScale() + rootObject.values.light.y * world.getScale());
			light.setSoftnessLength(rootObject.values.light.softDist * world.getScale());
			light.setXray(rootObject.values.light.xray);
			pointlight = light;
			setFilterData(worldObjectValues.categoryBits, worldObjectValues.maskBits, worldObjectValues.groupIndex);
		}
	}

	/**Checks the change class for changes and applies them on the fixture*/
	public void updateFixtures() {
		worldObjectValues.applyChanges(rootObject); //Check for changes
		if(hitboxBody != null) world.b2dWorld.destroyBody(hitboxBody);
		createFixtures();
	}

	/**@param generateNewLight - If true, a new PointLight instance is created with the set value.*/
	public void updateLight(boolean generateNewLight) {
		worldObjectValues.applyChanges(rootObject); //Check for changes

		if(pointlight != null && !generateNewLight && rootObject.values.light != null) {
			setFilterData(worldObjectValues.categoryBits, worldObjectValues.maskBits, worldObjectValues.groupIndex);
			pointlight.setDistance(rootObject.values.light.dist);
			pointlight.setColor(rootObject.values.light.r, rootObject.values.light.g, rootObject.values.light.b, rootObject.values.light.a);
			pointlight.setXray(rootObject.values.light.xray);
		}
		if(generateNewLight) createLight(rootObject.values.light);
	}

	public void setFilterData(int categoryBits, int[] maskBits, int groupIndex) {
		ArrayList<Integer> list = new ArrayList<>();
		for(int i : maskBits) list.add(i);
		setFilterData(categoryBits, list, groupIndex);
	}

	public void setFilterData(int categoryBits, ArrayList<Integer> maskBits, int groupIndex) {
		worldObjectValues.groupIndex = groupIndex;
		worldObjectValues.categoryBits = categoryBits;
		if(!maskBits.equals(worldObjectValues.maskBits)) {
			worldObjectValues.maskBits.clear();
			for (int mask : maskBits) worldObjectValues.maskBits.add(mask);
		}
		Filter f = new Filter();
		f.categoryBits = (short) categoryBits;
		f.groupIndex = (short) groupIndex;
		f.maskBits = Tools.Convert.convertToMask(maskBits);

		if(hitboxBody != null){
			for(com.badlogic.gdx.physics.box2d.Fixture fixture : hitboxBody.getFixtureList()) fixture.setFilterData(f);
		}
		if(pointlight != null) pointlight.setContactFilter(f);
	}

	public void setMaskBits(ArrayList<Integer> maskBits) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(worldObjectValues.categoryBits, maskBits, worldObjectValues.groupIndex);
		} else if(hitboxBody != null) {
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(hitboxBody.getFixtureList().first().getFilterData().categoryBits, maskBits, hitboxBody.getFixtureList().first().getFilterData().groupIndex);
		}
	}

	//I know this name is just wrong but... why not
	public void setCategoryBit(int bit) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(bit, worldObjectValues.maskBits, worldObjectValues.groupIndex);
		} else if(hitboxBody != null) { //Update all values from actual hitbox body
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(bit, worldObjectValues.maskBits, hitboxBody.getFixtureList().first().getFilterData().groupIndex);
		}
	}

	public void setGroupIndex(int index) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(worldObjectValues.categoryBits, worldObjectValues.maskBits, index);
		} else if(hitboxBody != null) { //Update all values from actual hitbox body
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(hitboxBody.getFixtureList().first().getFilterData().categoryBits, worldObjectValues.maskBits, index);
		}
	}

	/**{@link WorldObject#addToWorld(World, boolean, long)}*/
	public final Body addToWorld(World world) {
		return addToWorld(world, NO_ADDR);
	}

	/**Normalized size.*/
	public final void setSize(Vector2 size) {
		rootObject.values.size.set(size);
		worldObjectValues.change.size.set(size);
	}
	
	/**Normalized position.*/
	public final void setPosition(float x, float y) {
		worldObjectValues.x = x;
		worldObjectValues.y = y;
		if(!added) return;
		moved = true;

		if(hasHitbox()) hitboxBody.setTransform(x*world.getScale(), y*world.getScale(), hitboxBody.getAngle());
	}
	
	public final void setPosition(Vector2 position) {
		setPosition(position.x, position.y);
	}
	
	/**x and y only specifying the direction. Speed in tiles/second*/
	public final void move(float x, float y, float speed) {
		if(!added || rootObject.values.fixed) return;

		moved = true;
		world.objectMoved = true;
		Vector2 dir = new Vector2(x, y);
		dir.nor();
		dir.scl(speed);

		if(hasHitbox() && rootObject.values.type != RootObjectValues.STATIC) hitboxBody.setLinearVelocity(dir.scl(world.getTileSizeNORM()));
		else {
			worldObjectValues.x += dir.x * Gdx.graphics.getDeltaTime() * world.getTileSizeNORM();
			worldObjectValues.y += dir.y * Gdx.graphics.getDeltaTime() * world.getTileSizeNORM();
			if(rootObject.values.type == RootObjectValues.STATIC
				&& hasHitbox()) hitboxBody.setTransform(worldObjectValues.x * world.getScale(), worldObjectValues.y * getScale(), hitboxBody.getAngle());
		}
	}

	/**Applies force to the hitbox body. Don't think the physical way.
	 * If forceX would be 1: The speed of the object would be 1, but grip brakes it*/
	public final void applyForce(float forceX, float forceY) {
		if(rootObject.values.fixed) return;
		worldObjectValues.force.add(forceX * world.getTileSizeNORM() * Gdx.graphics.getDeltaTime(), forceY * world.getTileSizeNORM() * Gdx.graphics.getDeltaTime()); //Add forces to previous force
		moved = true;
	}

	public final void cancelForce() {
		worldObjectValues.force.set(0, 0);
		moved = false;
	}

	public float getForceX() {
		return worldObjectValues.force.x;
	}

	public float getForceY() {
		return worldObjectValues.force.y;
	}
	
	/**Moves the object towards the x and y position with the given speed. Speed im tiles/second
	 * @return The calculated normal vector of the direction.*/
	public final Vector2 moveTo(float x, float y, float speed) {
		if(rootObject.values.fixed) return new Vector2(x, y);
		Vector2 dir = new Vector2(x, y).sub(worldObjectValues.x, worldObjectValues.y).nor();
		move(dir.x, dir.y, speed);
		return dir;
	}

	/**Only applied, when the object is added to a b2dwold via {@link WorldObject#addToWorld(World, boolean)}*/
	public final void update(float delta) {
		if(!added) return;

		if(behaviors.size() != worldObjectValues.behaviors.size()) {
			updateBehaviors();
		}

		for(Behavior b : behaviors) {
			if(b.isEnabled() && b.added) {
				try {
					b.onUpdate(world, this, delta);
				} catch(Exception e) {
					Logger.logError("Trigger " + getID(), "Error occurred while updating " + b.getID() + " behavior: " + e.toString());
					e.printStackTrace();
				}
			}
		}

		if(!worldObjectValues.force.isZero()) {
			move(worldObjectValues.force.x, worldObjectValues.force.y, worldObjectValues.force.len());
			worldObjectValues.force.x -= worldObjectValues.force.x * rootObject.values.grip * (delta*60);
			worldObjectValues.force.y -= worldObjectValues.force.y * rootObject.values.grip * (delta*60);

			if(worldObjectValues.force.len() < 0.1f * world.getScale()) {
				worldObjectValues.force.setZero();
				moved = false;
			}
		}

		if(hitboxCreated && rootObject.values.type != RootObjectValues.STATIC) {
			if (hitboxBody.getAngularVelocity() != 0) {
				if (hitboxBody.getAngularVelocity() <= minAngVel) hitboxBody.setAngularVelocity(0);
				else hitboxBody.setAngularVelocity(hitboxBody.getAngularVelocity() * rootObject.values.grip * (delta*60));
			}

			Vector2 normPos = world.toScreen(hitboxBody.getPosition());
			worldObjectValues.x = normPos.x;
			worldObjectValues.y = normPos.y;
			setRotation((int) (hitboxBody.getAngle() * MathUtils.radiansToDegrees));
		}

		if(hitboxCreated && !moved && hitboxBody.getLinearVelocity().len() > 0 && worldObjectValues.force.isZero()) {
			hitboxBody.setLinearVelocity(0, 0);
		}

		if(trigger != null) trigger.update();
		else if(trigger == null && worldObjectValues.change.trigger != null) setTrigger(worldObjectValues.change.trigger);

		if(rootObject.values.light != null && pointlight != null) {
			pointlight.setPosition(getPosition().x * world.getScale() + rootObject.values.light.x * world.getScale(),
					getPosition().y *world.getScale() + rootObject.values.light.y * world.getScale());
		}

		if(moved) {
			if (binding != null) {  //Check for tile change, and leave, if the object is in the void, the last visited chunk
				if (!binding.onChunk(getPosition().x, getPosition().y)) {
					Chunk newChunk = world.estimateChunk(getPosition().x, getPosition().y);
					if (newChunk != null) newChunk.attachObject(this);
				}
			}
			if(bindingTile != null) {
				if(!bindingTile.onTile(getPosition().x*world.getScale(), getPosition().y*world.getScale(), world.getScale())) {
					Tile newTile = world.estimateTile(getPosition().x, getPosition().y, true);
					if(newTile != null) newTile.attachObject(this);
				}
			}
		}

		if(rootObject.animation != null) rootObject.setTexture(rootObject.animation.update(Gdx.graphics.getFramesPerSecond()));
		moved = false;
	}

	public void enableBehavior(String id, boolean enable) {
		for(int i = 0; i < this.behaviors.size(); i++) {
			if(this.behaviors.get(i).getID().equals(id)) {
				this.getBehavior().get(i).setEnabled(enable);
				break;
			}
		}
	}

	//public void clearBehaviors() {
	//	behaviors.clear();
	//	worldObjectValues.behaviors.clear();
	//}

	public final void removeBehavior(String id) {
		for (int i = 0; i < this.behaviors.size(); i++) {
			if (this.behaviors.get(i).getID().equals(id)) {
				this.behaviors.get(i).onRemove();
				this.getBehavior().remove(i);
				break;
			}
		}

		for (int i = 0; i < worldObjectValues.behaviors.size(); i++) {
			if (worldObjectValues.behaviors.get(i).id.equals(id)) {
				worldObjectValues.behaviors.remove(i);
				break;
			}
		}
	}

	/**Snychronizes the loaded behaviors with the worldValuesBehaviors*/
	public void updateBehaviors(){
		for(int i = 0; i < worldObjectValues.behaviors.size(); i++) {
			boolean found = false;
			for(Behavior b : behaviors) {
				if(worldObjectValues.behaviors.get(i).id.equals(b.getID())) {
					found = true;
					break;
				}
			}
			if(!found) {
				try {
					loadBehavior(worldObjectValues.behaviors.get(i));
				} catch (Exception e) {
					Logger.logError(getID(), "Error while updating behavior: " + e.toString());
					e.printStackTrace();
					worldObjectValues.behaviors.remove(i);
					i--;
				}
			}
		}
		if(worldObjectValues.behaviors.size() != behaviors.size()) {
			for(int i = 0; i < behaviors.size(); i++) {
				boolean found = false;
				for(WorldValues.BehaviorValues v : worldObjectValues.behaviors) {
					if(v.id.equals(behaviors.get(i).getID())) {
						found = true;
						break;
					}
				}
				if(!found) {
					behaviors.remove(i);
					i--;
				}
			}
		}
	}


	public final void addBehavior(Behavior behavior) {
		behavior.addToWorld(world, this);
		worldObjectValues.behaviors.add(behavior.values);
		this.behaviors.add(behavior);

		if(behavior.doPostCreate) {
			Gdx.app.postRunnable(new Runnable() {
				public void run() {
					if (behavior.values.enabled) behavior.doPostCreate();
				}
			});
		}
	}

	/**Called, when a behavior is loaded from worldobjectvalues (The setbehavior will add the behavior to worldobjectvalues and this would cause duplicates)*/
	public final Behavior loadBehavior(WorldValues.BehaviorValues values) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		for(Behavior b : this.behaviors) {
			if(b.getID().equals(values.id)) throw new IllegalArgumentException("Object behaviors need to have an unique id! (Behavior with id " + values.id + " already added!");
		}

		Behavior b = (Behavior) Class.forName(values.classPath).getConstructor(WorldValues.BehaviorValues.class).newInstance(values);
		addBehavior(b);
		return b;
	}

	/**Calls any method with the given name
	 * Returned exception is null, if any method call went successfull*/
	public void invokeMethod(Class<?> behavior, String functionName, Object... args) {
		if(behavior == null) return;
		for(Behavior b : behaviors) {
			if(b.getClass() == behavior) {
				ArrayList<Class<?>> classes = new ArrayList<>(0);
				for(Object o : args) classes.add(o.getClass());
				try {
					b.getClass().getMethod(functionName, classes.toArray(new Class[classes.size()])).invoke(b, args);
				} catch(Exception e) {
					Logger.logError("WorldObject", "Failed to invoke method " + functionName + " for behavior: " + behavior.getName() + " on worldobject: " + getAddress() + " (" + getID() + ") Caused by:");
					e.printStackTrace();
				}
			}
		}
	}

	/**Invokes a Method from a given behavior with the specified id and tries to call a function with the function name and passes the arguments*/
	public void invokeMethod(String id, String functionName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		for(Behavior b : behaviors) {
			if(b.getID().equals(id)) {
				ArrayList<Class<?>> classes = new ArrayList<>(0);
				for(Object o : args) classes.add(o.getClass());

				b.getClass().getMethod(functionName, classes.toArray(new Class[classes.size()])).invoke(b, args);
			}
		}
	}

	/**May be null*/
	public final ArrayList<Behavior> getBehavior() {
		return behaviors;
	}

	public final Behavior getBehavior(String id) {
		for(Behavior b : behaviors) {
			if(b.getID().equals(id)) return b;
		}
		return null;
	}

	public final void removeTrigger() {
		if(world != null) world.objectsWithTrigger.remove(this);
		trigger = null;
		worldObjectValues.change.trigger = null;
	}

	/**@return The new trigger instance*/
	public final void setTrigger(TriggerValues trigger) {
		if(trigger == null) {
			removeTrigger();
			return;
		}

		worldObjectValues.change.trigger = trigger.copy();
		if(this.trigger != null) this.trigger.values = worldObjectValues.change.trigger.copy();
		else {
			//First time adding trigger
			if(world != null) world.objectsWithTrigger.add(this);
			this.trigger = new Trigger(this, worldObjectValues.change.trigger);
		}
	}

	public void setBlendingColor(Color color) {
		setBlendingColor(color.r, color.g, color.b, color.a);
	}

	public void setBlendingColor(float r, float g, float b, float a) {
		worldObjectValues.change.r = r;
		worldObjectValues.change.g = g;
		worldObjectValues.change.b = b;
		worldObjectValues.change.a = a;
		rootObject.values.r = r;
		rootObject.values.g = g;
		rootObject.values.b = b;
		rootObject.values.a = a;
	}

	public float getR() {
		return rootObject.values.r;
	}

	public float getG() {
		return rootObject.values.g;
	}

	public float getB() {
		return rootObject.values.b;
	}

	public float getA() {
		return rootObject.values.a;
	}

	/**Assumes the batch.begin() function is already called.<br>
	 * If the object is not added in a world, call {@link WorldObject#draw(SpriteBatch, World)}*/
	public final void draw(SpriteBatch batch) {
		if(!added) return;
		draw(batch, world);
	}

	public final void drawCenter(SpriteBatch batch, Sprite texture, float x, float y, float width, float height) {
		batch.draw(texture, x-width*.5f, y-height*.5f, width, height);
	}

	/**Draws any texture with the object bounds with custom size*/
	public void drawObjectTexture(SpriteBatch batch, Sprite texture, float x, float y, float width, float height) {
		batch.draw(texture,   //Dude this was pain...
				x*world.getScale()-rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.x,
				y*world.getScale()-rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.y,
				rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.x,
				rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.y,
				width, height,
				1, 1,
				getRotation());
	}

	/**Makes rendering an object possible, event when is was not added to a world*/
	public final void draw(SpriteBatch batch, World world) {
		batch.setColor(rootObject.values.r, rootObject.values.g, rootObject.values.b, rootObject.values.a);

		for(Behavior b : behaviors) {
			if(b.isEnabled()) b.drawBehind(this, batch);
		}

		if(rootObject.texture != null && world != null && worldObjectValues.visible) {  //TODO Scale position
			batch.draw(rootObject.texture,   //Dude this was pain...
					worldObjectValues.x*world.getScale()-rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.x,
					worldObjectValues.y*world.getScale()-rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.y,
					rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.x,
					rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.y,
					rootObject.values.size.x*world.getScale()*rootObject.values.scale*world.getPPM(), rootObject.values.size.y*world.getScale()*rootObject.values.scale*world.getPPM(),
					1, 1, 
					getRotation());
		}

		for(Behavior b : behaviors) {
			if(b.isEnabled()) b.drawOver(this, batch);
		}
	}

	public void setVisible(boolean visible) {
		worldObjectValues.visible = visible;
	}
	
	/**Changes the texture only temporary. Wont have affect, after loading a world*/
	public final void setTexture(Sprite texture) {
		rootObject.texture = texture;
	}
	
	/**Rotation is counter-clock wise in degrees*/
	public final void setRotation(int angle) {
		worldObjectValues.rotation = angle;
		
		if(added && hasHitbox()) hitboxBody.setTransform(hitboxBody.getPosition(), angle * MathUtils.degreesToRadians);
	}
	
	public final int getRotation() {
		return worldObjectValues.rotation;
	}
	
	/**Wont affect the hit box.*/
	public final void setScale(float scale) {
		rootObject.setScale(scale);
		
		if(!added) return;
		if(scale != world.getScale()) worldObjectValues.change.scale = scale;
	}

	/**Calls {@link Behavior#saveBehaviorVariables()} on every behavior attached to the object.
	 * Useful, if you want to copy this world object.*/
	public void updateBehaviorVariables() {
		for(Behavior b : behaviors) {
			b.saveBehaviorVariables();
		}
	}

	public void triggerBehaviorSave() {
		for(Behavior b : behaviors) {
			if(b.isEnabled()) {
				try {
					b.onSave();
				} catch(Exception e) {
					Logger.logError("WorldObject " + getID(), " Caught exception while calling onSave() from behavior " + b.getID() + " (" + b.getClassPath() + "): " + e.toString());
				}
				b.saveBehaviorVariables();
			}
		}
	}
	
	/**May return null, if the object is not added to a world!*/
	public final Body getB2DBody() {
		return hitboxBody;
	}

	private final Vector2 tempPos = new Vector2();
	/**Returns the normal position*/
	public final Vector2 getPosition() {
		tempPos.set(worldObjectValues.x, worldObjectValues.y);
		return tempPos;
	}

	public final PointLight getLight() {
		return pointlight;
	}

	//public final void setLight(RootValues.LightSource values) {
	//	removeLight();
	//	if(values != null) {
	//		PointLight light = new PointLight(world.b2dLightWorld, values.rays, new Color(values.r, values.g, values.b, values.a),
	//				values.dist * world.getScale(), getPosition().x*world.getScale() + values.x * world.getScale(), getPosition().y*world.getScale() + values.y * world.getScale());
	//		light.setSoftnessLength(values.softDist * world.getScale());
	//		light.setXray(values.xray);
	//		Filter filter = new Filter();
	//		filter.groupIndex = (short) values.groupIndex;
	//		filter.maskBits = 0;
	//		light.setContactFilter(filter);
	//		pointlight = light;
	//		if(rootObject.values.pointLight != values) worldObjectValues.change.pointLight = values.copy();
	//		rootObject.values.pointLight = values;
	//	}
	//}

	//public void removeLight() {
	//	if(pointlight != null) pointlight.remove(true);
	//}

	/**The fixture may not get added instantly or the next frame,
	 * because this object sends a request to the world to add a fixture,
	 * to prevent crashed and */
	public void setFixture(Fixture... fixture) {
		worldObjectValues.change.fixtures = null;
		getRootValues().values.fixtures = null;
		worldObjectValues.change.fixtures = new ArrayList<>();
		getRootValues().values.fixtures = new ArrayList<>();
		for (Fixture f : fixture) {
			worldObjectValues.change.fixtures.add(f);
			getRootValues().values.fixtures.add(f);
		}
		world.requestFixture(this);
	}

	public void clearFixtures() {
		getRootValues().values.fixtures = null;
		worldObjectValues.change.fixtures = null;
		hitboxCreated = false;
		if(getHitboxBody() != null) {
			for(com.badlogic.gdx.physics.box2d.Fixture f : getHitboxBody().getFixtureList()) {
				getHitboxBody().destroyFixture(f);
			}
			//getWorld().b2dWorld.destroyBody(getHitboxBody());
		}
	}

	public final float getX() {
		return worldObjectValues.x;
	}
	
	public final float getY() {
		return worldObjectValues.y;
	}

	public final float getScaledX(World world) {
		return worldObjectValues.x * world.getScale();
	}

	public final float getScaledY(World world) {
		return worldObjectValues.y * world.getScale();
	}

	/**Returns the coordinates of the corner in the bottom left. You may need to scale the vector with the world scale*/
	public final Vector2 cornerBottomLeft(float worldscale) {
		return new Vector2(
				worldObjectValues.x*worldscale-getSize().x*.5f*getScale()-getTextureOffset().x,
				worldObjectValues.y*worldscale-getSize().y*.5f*getScale()-getTextureOffset().y
		);
	}

	public final Vector2 cornerBottomRight(float worldscale) {
		return new Vector2(
				worldObjectValues.x*worldscale+getSize().x*.5f*getScale()-getTextureOffset().x,
				worldObjectValues.y*worldscale-getSize().y*.5f*getScale()-getTextureOffset().y
		);
	}

	public final Vector2 cornerTopRight(float worldscale) {
		return new Vector2(
				worldObjectValues.x*worldscale+getSize().x*.5f*getScale()-getTextureOffset().x,
				worldObjectValues.y*worldscale+getSize().y*.5f*getScale()-getTextureOffset().y
		);
	}


	public final Vector2 cornerTopLeft(float worldscale) {
		return new Vector2(
				worldObjectValues.x*worldscale-getSize().x*.5f*getScale()-getTextureOffset().x,
				worldObjectValues.y*worldscale+getSize().y*.5f*getScale()-getTextureOffset().y
		);
	}


	public final float getScaledWidth(World world) {
		return rootObject.getSize().x * world.getScale();
	}

	public final float getScaledHeight(World world) {
		return rootObject.getSize().y * world.getScale();
	}

	/**Returns the size normal of the object in pixels*/
	public final Vector2 getSize() {
		return rootObject.getSize();
	}
	
	/**Returns a copy of the root object.Don't call this function too often.*/
	public final RootObject getRootValues() {
		return rootObject;
	}
	
	public final WorldObjectValues getWorldValues() {
		return worldObjectValues.copy();
	}
	
	/**May be null*/
	public final World getWorld() {
		return world;
	}
	
	public final String getID() {
		return rootObject.getID();
	}
	
	public final String getAddress() {
		return worldObjectValues.addr;
	}

	public final void setAddress(String address) {
		worldObjectValues.addr = address;

		if (getAddress().equals(WorldObject.NO_ADDR) || getAddress().equals(WorldObject.TILE_ADDR)) {
			world.getObjectWithAddress().remove(this);
		} else if(!getAddress().equals(WorldObject.NO_ADDR)) world.getObjectWithAddress().add(this);

		for(WorldObject obj : world.objectsWithTrigger) {
			obj.getTrigger().refreshJoinTrigger();
		}
	}

	public final boolean added() {
		return added;
	}
	
	public final boolean hasHitbox() {
		if(getHitboxBody() != null) {
			return !getHitboxBody().getFixtureList().isEmpty();
		} else return false;
	}
	
	public final float getScale() {
		return rootObject.values.scale;
	}
	
	public final Vector2 getTextureOffset() {
		return rootObject.values.textureOffset;
	}

	/**May be null*/
	public final Chunk getBinding() {
		return binding;
	}

	/**May be null*/
	public final Tile getTileBinding() {
		return bindingTile;
	}

	public final HashData getHashWrapper() {
		return hashWrapper;
	}

	/**Normalized position!*/
	public final boolean touched(Vector2 position) {
		return position.x >= worldObjectValues.x-rootObject.values.size.x*.5f &&
				position.y >= worldObjectValues.y-rootObject.values.size.y*.5f &&
				 position.x <= worldObjectValues.x+rootObject.values.size.x*.5f &&
				  position.y <= worldObjectValues.y+rootObject.values.size.y*.5f;
	}

	/**Returns the normalizes distance*/
	public final float dist(WorldObject other) {
		return getPosition().dst(other.getPosition());
	}

	public boolean getsDestroyed() {
		return getsRemoved;
	}
	
	/**Might be null, when the object dosent have a trigger!*/
	public final Trigger getTrigger() {
		return trigger;
	}
	
	public String toString() {
		return getID();
	}

	public final float getYOff() {
		return rootObject.values.yOff;
	}

	public final void setYOff(float yOff) {
		rootObject.values.yOff = yOff;
		worldObjectValues.change.yOff = yOff;
	}

	/**May be null*/
	public Body getHitboxBody() {
		return hitboxBody;
	}

	public final boolean moved() {
		return moved;
	}

	public void setFixed(boolean fixed) {
		rootObject.values.fixed = fixed;
		worldObjectValues.change.fixed = fixed;
		moved = false;
		if(hasHitbox()) getHitboxBody().setLinearVelocity(0, 0);
	}

	public void setFixedrotation(boolean fixed) {
		worldObjectValues.change.fixedRotation = fixed;
		rootObject.values.fixedRotation = fixed;
		if(hasHitbox()) hitboxBody.setFixedRotation(fixed);
	}

	public boolean isFixed() {
		return rootObject.values.fixed;
	}

	@Override
	public void dispose() {
		if(!added) return;

			for (Behavior b : behaviors) {
				try {
					b.onRemove();
				} catch (Exception e) {
					Logger.logError("WorldObject", "Behavior " + b.getID() + " threw an exception, while disposing: " + e.toString().replace('\n', ' '));
				}
			}

		if(hasHitbox()) world.b2dWorld.destroyBody(hitboxBody);
		createLight(null);
		if(!world.disposing) {
			if (getBinding() != null) getBinding().releaseObject(this);
			if (getTileBinding() != null) getTileBinding().releaseObject(this);

			added = false;
			System.gc();
			behaviors.clear();
		}
	}
}
