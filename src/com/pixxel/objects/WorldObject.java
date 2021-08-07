package com.pixxel.objects;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Disposable;
import com.pixxel.Main.Logger;
import com.pixxel.objects.RootValues.Fixture;
import com.pixxel.objects.RootValues.RootObjectValues;
import com.pixxel.objects.WorldValues.TriggerValues;
import com.pixxel.objects.WorldValues.WorldObjectValues;
import com.pixxel.utils.Tools;

import box2dLight.PointLight;

/**A class for an Object that is actually drawn on the World.
 * @see World*/
public class WorldObject implements Disposable {

	public float minVel = .01f;
	public float minAngVel = .01f;
	
	private com.pixxel.objects.World world;
	private final com.pixxel.objects.RootObject rootObject;  //For live use, accepts changes, but thei're not persistent.
	public WorldObjectValues worldObjectValues;  //For persisted use accepts all changes and is saveable
	
	private Body hitboxBody;  //null until Object added in a b2dworld
	
	private boolean added = false; //True, when object is present in b2dworld
	private boolean hitboxCreated = false;

	protected com.pixxel.objects.Tile bindingTile;
	protected com.pixxel.objects.Chunk binding;

	/**This address have objects 'without' an address*/
	public static final String NO_ADDR = "";
	/**Tiles have this address. Gets set to {@link WorldObject#NO_ADDR}, when a normal object has this address.*/
	public static final String TILE_ADDR = "tile";
	/**Forces the world to set a random, unique address.*
	public static final long FORCE_ADDR = 2;*/
	/**This address is reserved for the player*/

	private com.pixxel.objects.Trigger trigger;
	private boolean moved = false;

	protected ArrayList<com.pixxel.objects.Behavior> behaviors = new ArrayList<>();
	private PointLight pointlight = null;

	protected boolean getsRemoved = false;

	com.pixxel.objects.HashData hashWrapper;

	/**Position is relative to the world's bottom left corner (origin), where the object gets added in.
	 * If this object is added as tile in the world, the position is ignored.
	 * @param position - The normalized position. You can use {@link com.pixxel.objects.World#toScreen(float, float)}*/
	WorldObject(com.pixxel.objects.RootObject rootObject, Vector2 position) {
		this.worldObjectValues = new WorldObjectValues(); 
		this.rootObject = rootObject.copy();
		
		worldObjectValues.id = this.rootObject.getID();
		worldObjectValues.x = position.x;
		worldObjectValues.y = position.y;
		worldObjectValues.id = rootObject.values.id;
		worldObjectValues.applyChanges(this.rootObject);
	}
	
	/**Loads {@link WorldObjectValues} into the world object, witch defines a position etc.
	 * Overwrites the {@link RootObjectValues#initRotation}*/
	WorldObject(WorldObjectValues data, com.pixxel.objects.RootObject object) { //Rotation, position and id are already in 'data'
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
	public final Body addToWorld(com.pixxel.objects.World world, String addr) { //Uses worldObject values for rotation etc, overwrites position
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
		hashWrapper = new com.pixxel.objects.HashData(worldObjectValues.hash);

		//Try to apply behavior classes and fire the onCreate event
		for(com.pixxel.objects.WorldValues.BehaviorValues s : worldObjectValues.behaviors) {
			try {
				for(com.pixxel.objects.Behavior b : this.behaviors) {
					if(b.getID().equals(s.id)) {
						Logger.logError("WorldObject", "Object behaviors need to have an unique id! (Behavior with id " + s.id + " already added!");
						continue;
					}
				}
				com.pixxel.objects.Behavior b = (com.pixxel.objects.Behavior) Class.forName(s.classPath).getConstructor(com.pixxel.objects.WorldValues.BehaviorValues.class).newInstance(s);
				behaviors.add(b);
			} catch (Exception e) {
				Logger.logError("WorldObject " + getID(), "Unable to load behavior class for " + toString() + ":");
				e.printStackTrace();
			}
		}
		for(int i = 0; i < behaviors.size(); i++) {
			try {
				behaviors.get(i).addToWorld(world, this);
				com.pixxel.objects.Behavior b = behaviors.get(i);
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

	/**Creates a light. use createLight(null) to remove any existing lightsource
	 * @param values The light parameters*/
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

	/**Sets its box2d filter data. If this object has a hitbox.
	 * @see https://www.iforce2d.net/b2dtut/collision-filtering*/
	public void setFilterData(int categoryBits, int[] maskBits, int groupIndex) {
		ArrayList<Integer> list = new ArrayList<>();
		for(int i : maskBits) list.add(i);
		setFilterData(categoryBits, list, groupIndex);
	}

	/**Sets its box2d filter data. If this object has a hitbox.
	 * @see https://www.iforce2d.net/b2dtut/collision-filtering*/
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

	/**Sets its box2d filter data. If this object has a hitbox.
	 * @see https://www.iforce2d.net/b2dtut/collision-filtering*/
	public void setMaskBits(ArrayList<Integer> maskBits) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(worldObjectValues.categoryBits, maskBits, worldObjectValues.groupIndex);
		} else if(hitboxBody != null) {
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(hitboxBody.getFixtureList().first().getFilterData().categoryBits, maskBits, hitboxBody.getFixtureList().first().getFilterData().groupIndex);
		}
	}

	/**Sets its box2d filter data. If this object has a hitbox.
	 * @see https://www.iforce2d.net/b2dtut/collision-filtering*/
	public void setCategoryBit(int bit) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(bit, worldObjectValues.maskBits, worldObjectValues.groupIndex);
		} else if(hitboxBody != null) { //Update all values from actual hitbox body
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(bit, worldObjectValues.maskBits, hitboxBody.getFixtureList().first().getFilterData().groupIndex);
		}
	}

	/**Sets its box2d filter data. If this object has a hitbox.
	 * @see https://www.iforce2d.net/b2dtut/collision-filtering*/
	public void setGroupIndex(int index) {
		if((hitboxBody == null && pointlight == null) || (hitboxBody == null && pointlight != null)) {
			setFilterData(worldObjectValues.categoryBits, worldObjectValues.maskBits, index);
		} else if(hitboxBody != null) { //Update all values from actual hitbox body
			//Use the values from the first fixture, since all need to have the same filter data anyways
			setFilterData(hitboxBody.getFixtureList().first().getFilterData().categoryBits, worldObjectValues.maskBits, index);
		}
	}

	/**{@link WorldObject#addToWorld(com.pixxel.objects.World, boolean, long)}*/
	public final Body addToWorld(com.pixxel.objects.World world) {
		return addToWorld(world, NO_ADDR);
	}

	/**Normalized size.
	 * <br>See {@link World} for more information about normalizing*/
	public final void setSize(Vector2 size) {
		rootObject.values.size.set(size);
		worldObjectValues.change.size.set(size);
	}
	
	/**Normalized position.
	 * <br>See {@link World} for more information about normalizing*/
	public final void setPosition(float x, float y) {
		worldObjectValues.x = x;
		worldObjectValues.y = y;
		if(!added) return;
		moved = true;

		if(hasHitbox()) hitboxBody.setTransform(x*world.getScale(), y*world.getScale(), hitboxBody.getAngle());
	}

	/**Normalized position
	 * <br>See {@link World} for more information about normalizing*/
	public final void setPosition(Vector2 position) {
		setPosition(position.x, position.y);
	}
	
	/**@param x Target for the x direction the object should move to
	 * @param y Target for the y direction the object should move to
	 * @param speed Speed in tiles/second*/
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
	 * If forceX = 1, the initial speed would be 1 tile/s.
	 * The object grip breaks it*/
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

	/**Only applied, when the object is added to a b2dwold via {@link WorldObject#addToWorld(com.pixxel.objects.World, boolean)}*/
	public final void update(float delta) {
		if(!added) return;

		if(behaviors.size() != worldObjectValues.behaviors.size()) {
			updateBehaviors();
		}

		for(com.pixxel.objects.Behavior b : behaviors) {
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
					com.pixxel.objects.Chunk newChunk = world.estimateChunk(getPosition().x, getPosition().y);
					if (newChunk != null) newChunk.attachObject(this);
				}
			}
			if(bindingTile != null) {
				if(!bindingTile.onTile(getPosition().x*world.getScale(), getPosition().y*world.getScale(), world.getScale())) {
					com.pixxel.objects.Tile newTile = world.estimateTile(getPosition().x, getPosition().y, true);
					if(newTile != null) newTile.attachObject(this);
				}
			}
		}

		if(rootObject.animation != null) rootObject.setTexture(rootObject.animation.update(Gdx.graphics.getFramesPerSecond()));
		moved = false;
	}

	/**Enables or disables a behavior with the given id
	 * @param id Behavior Id (Needs to already be part of the object)
	 * @param enable If false, the behavior gets disabled*/
	public void enableBehavior(String id, boolean enable) {
		for(int i = 0; i < this.behaviors.size(); i++) {
			if(this.behaviors.get(i).getID().equals(id)) {
				this.getBehavior().get(i).setEnabled(enable);
				break;
			}
		}
	}

	/**Removes a behavior with the given id
	 * @param id The Id of the added behavior*/
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

	/**Synchronizes the loaded behaviors with the worldValuesBehaviors*/
	public void updateBehaviors(){
		for(int i = 0; i < worldObjectValues.behaviors.size(); i++) {
			boolean found = false;
			for(com.pixxel.objects.Behavior b : behaviors) {
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
				for(com.pixxel.objects.WorldValues.BehaviorValues v : worldObjectValues.behaviors) {
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

	/**Adds a specific behavior to this object.
	 * This instance should already have a unique id set in
	 * {@link Behavior#setID(String)}*/
	public final void addBehavior(com.pixxel.objects.Behavior behavior) {
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

	/**Adds and loads a behavior using the values
	 * @param values The behaviour values
	 * @see com.pixxel.objects.WorldValues.BehaviorValues
	 * @throws IllegalArgumentException If a behavior with the given id is already added to this object*/
	public final com.pixxel.objects.Behavior loadBehavior(com.pixxel.objects.WorldValues.BehaviorValues values) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		for(com.pixxel.objects.Behavior b : this.behaviors) {
			if(b.getID().equals(values.id)) throw new IllegalArgumentException("Object behaviors need to have an unique id! (Behavior with id " + values.id + " already added!");
		}

		com.pixxel.objects.Behavior b = (com.pixxel.objects.Behavior) Class.forName(values.classPath).getConstructor(WorldValues.BehaviorValues.class).newInstance(values);
		addBehavior(b);
		return b;
	}

	/**Calls any java method from the given behavior object using its class type.<br>
	 * Useful if you want to communicate between multiple behaviors of an object.<br><br>
	 * If you have - for whatever reason - multiple behaviors with the same class type attached to this object,
	 * use {@link WorldObject#invokeMethod(String, String, Object...)}<br>
	 * This will only call the method from the very specific behavior instead of calling the method from all behaviors with this class type.
	 * @param behavior The ClassType of the behavior. Example: <code>Tag.class</code>
	 * @param functionName The name of the method
	 * @param args Any arguments the method expects.
	 * */
	public void invokeMethod(Class<?> behavior, String functionName, Object... args) {
		if(behavior == null) return;
		for(com.pixxel.objects.Behavior b : behaviors) {
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

	/**Like {@link WorldObject#invokeMethod(Class, String, Object...)} but instead call a function from
	 * a very specific behavior with the given id.
	 * @param id The Id of the behavior.
	 * @param functionName The name of the method.
	 * @param args Any arguments the method expects. */
	public void invokeMethod(String id, String functionName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		for(com.pixxel.objects.Behavior b : behaviors) {
			if(b.getID().equals(id)) {
				ArrayList<Class<?>> classes = new ArrayList<>(0);
				for(Object o : args) classes.add(o.getClass());

				b.getClass().getMethod(functionName, classes.toArray(new Class[classes.size()])).invoke(b, args);
			}
		}
	}

	/**May be null if behavior not found
	 * @return All behaviors of this object*/
	public final ArrayList<com.pixxel.objects.Behavior> getBehavior() {
		return behaviors;
	}

	/**May be null if behavior not found
	 * @return Any added behavior that matches the given class type.
	 * If multiple behaviors are added with the same class type,the first occurrence will be returned*/
	public final com.pixxel.objects.Behavior getBehavior(Class<?> clazz) {
		for(com.pixxel.objects.Behavior b : behaviors) {
			if(b.subClass == clazz) return b;
		}
		return null;
	}

	/**May be null if behavior not found
	 * @return Any added behavior that matches the specific id*/
	public final com.pixxel.objects.Behavior getBehavior(String id) {
		for(com.pixxel.objects.Behavior b : behaviors) {
			if(b.getID().equals(id)) return b;
		}
		return null;
	}

	/**Removes the trigger script from this object*/
	public final void removeTrigger() {
		if(world != null) world.objectsWithTrigger.remove(this);
		trigger = null;
		worldObjectValues.change.trigger = null;
	}

	/**Adds a new trigger to this object.
	 * @param trigger The values for the trigger*/
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
			this.trigger = new com.pixxel.objects.Trigger(this, worldObjectValues.change.trigger);
		}
	}

	/**Sets the blending color.*/
	public void setBlendingColor(Color color) {
		setBlendingColor(color.r, color.g, color.b, color.a);
	}

	/**Sets the blending color.*/
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

	/**Returns the red RGB blending color.*/
	public float getR() {
		return rootObject.values.r;
	}
	/**Returns the green RGB blending color.*/
	public float getG() {
		return rootObject.values.g;
	}
	/**Returns the blue RGB blending color.*/
	public float getB() {
		return rootObject.values.b;
	}
	/**Returns the alpha / transparency blending.*/
	public float getA() {
		return rootObject.values.a;
	}

	/**Assumes the batch.begin() function is already called.<br>
	 * If the object is not added in a world, call {@link WorldObject#draw(SpriteBatch, com.pixxel.objects.World)}*/
	public final void draw(SpriteBatch batch) {
		if(!added) return;
		draw(batch, world);
	}

	public final void drawCenter(SpriteBatch batch, Sprite texture, float x, float y, float width, float height) {
		batch.draw(texture, x-width*.5f, y-height*.5f, width, height);
	}

	/**Draws any texture with the object bounds with custom size*/
	public void drawObjectTexture(SpriteBatch batch, Sprite texture, float x, float y, float width, float height) {
		batch.draw(texture,
				x*world.getScale()-rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.x,
				y*world.getScale()-rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())-rootObject.values.textureOffset.y,
				rootObject.values.size.x*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.x,
				rootObject.values.size.y*.5f*world.getScale()*(rootObject.values.scale*world.getPPM())+rootObject.values.textureOffset.y,
				width, height,
				1, 1,
				getRotation());
	}

	/**Makes rendering an object possible, event when is was not added to a world.
	 * Useful to render duplicates of this object without adding more something.
	 * Useful for using in behaviors.*/
	public final void draw(SpriteBatch batch, com.pixxel.objects.World world) {
		batch.setColor(rootObject.values.r, rootObject.values.g, rootObject.values.b, rootObject.values.a);

		for(com.pixxel.objects.Behavior b : behaviors) {
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

		for(com.pixxel.objects.Behavior b : behaviors) {
			if(b.isEnabled()) b.drawOver(this, batch);
		}
	}

	/**@param visible If false,the object wont get rendered to the world.*/
	public void setVisible(boolean visible) {
		worldObjectValues.visible = visible;
	}
	
	/**Changes the texture only temporary. Wont have affect, after loading a world*/
	public final void setTexture(Sprite texture) {
		rootObject.texture = texture;
	}
	
	/**@param angle The angle. Rotation is counter-clock wise in degrees*/
	public final void setRotation(int angle) {
		worldObjectValues.rotation = angle;
		
		if(added && hasHitbox()) hitboxBody.setTransform(hitboxBody.getPosition(), angle * MathUtils.degreesToRadians);
	}

	/**@return The rotation in degrees
	 * @see WorldObject#setRotation(int) */
	public final int getRotation() {
		return worldObjectValues.rotation;
	}
	
	/**Wont affect the hit box.
	 * Scales the object
	 * @param scale The scale*/
	public final void setScale(float scale) {
		rootObject.setScale(scale);
		
		if(!added) return;
		if(scale != world.getScale()) worldObjectValues.change.scale = scale;
	}

	/**Calls {@link com.pixxel.objects.Behavior#saveBehaviorVariables()} on every behavior attached to the object.
	 * Useful, if you want to copy this world object.*/
	public void updateBehaviorVariables() {
		for(com.pixxel.objects.Behavior b : behaviors) {
			b.saveBehaviorVariables();
		}
	}

	public void triggerBehaviorSave() {
		for(com.pixxel.objects.Behavior b : behaviors) {
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

	/**Returns the normalized position
	 * {@link World} for more information about normalized positions*/
	public final Vector2 getPosition() {
		tempPos.set(worldObjectValues.x, worldObjectValues.y);
		return tempPos;
	}

	/**Returns the light if this object has one*/
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

	/**@deprecated Use {@link WorldObject#createLight(RootValues.LightSource)} with param being null*/
	public void removeLight() {
		if(pointlight != null) pointlight.remove(true);
	}

	/**The fixture may not get added instantly or the next frame,
	 * because this object sends a request to the world to add a fixture,
	 * to prevent crashes*/
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

	/**Removes all fixtures from this body*/
	public void clearFixtures() {
		getRootValues().values.fixtures = null;
		worldObjectValues.change.fixtures = null;
		hitboxCreated = false;
		if(getHitboxBody() != null) {
			for(com.badlogic.gdx.physics.box2d.Fixture f : getHitboxBody().getFixtureList()) {
				getHitboxBody().destroyFixture(f);
			}
		}
	}

	public final float getX() {
		return worldObjectValues.x;
	}
	
	public final float getY() {
		return worldObjectValues.y;
	}

	public final float getScaledX(com.pixxel.objects.World world) {
		return worldObjectValues.x * world.getScale();
	}

	public final float getScaledY(com.pixxel.objects.World world) {
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


	public final float getScaledWidth(com.pixxel.objects.World world) {
		return rootObject.getSize().x * world.getScale();
	}

	public final float getScaledHeight(com.pixxel.objects.World world) {
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
