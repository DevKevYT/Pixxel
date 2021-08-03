package com.pixxel.objects;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

public interface WorldValues {

	public final class TilePos {
		String id = "";
		/*int x = -1;
		int y = -1;*/
		int i = -1;
	}

	public final class GroundValues {
		public static final int SQUARE = 5; //Width and height of the chunk in tiles
		public String r = "missing";
		public ArrayList<TilePos> c = new ArrayList<TilePos>(); //x any y indexes are relative to the chunk!!

		public GroundValues copy() {
			GroundValues cpy = new GroundValues();
			cpy.r = r;
			for(TilePos c : c) {
				TilePos tilePos = new TilePos();
				tilePos.i = c.i;
				cpy.c.add(tilePos);
			}
			return cpy;
		}
	}

	public final class ChunkData {
		public ArrayList<WorldObjectValues> o = new ArrayList<>();
		public GroundValues g = new GroundValues();

		public ChunkData copy() {
			ChunkData cpy = new ChunkData();
			for(WorldObjectValues values : o) cpy.o.add(values.copy());
			cpy.g = g.copy();
			return cpy;
		}
	}
	
	public final class ViewportData {
		public static final int androidRD = 5;
		public int rd = 0;
		public float x = 0;
		public float y = 0;
		public float smooth = 0.9f;  //Number between 0 and 1 0: Instant focus 1: No camera movement
		public float zoom = .7f;
		public String focus = WorldObject.NO_ADDR;  //Player address (3) as default
		public boolean edge = true;  //If the camera can move into the void (true means no)
		public boolean center = true; //true: The camera moves to the focus object,false not
	}
	
	public final class TriggerValues {
		public String scr = "";
		public String messageText = "";
		public ArrayList<String> joinFilter = new ArrayList<>(); //Object addresses, separated by a ','
		public ArrayList<Integer> keys = new ArrayList<Integer>(1);

		public TriggerValues() {
		}
		
		public TriggerValues copy() {
			TriggerValues cpy = new TriggerValues();
			cpy = new TriggerValues();
			cpy.keys.addAll(keys);
			cpy.scr = scr;
			cpy.messageText = messageText;
			cpy.joinFilter.addAll(joinFilter);
			return cpy;
		}
	}

	public class BehaviorValues {
		public String classPath = "";
		public String id = "default";
		public boolean enabled = true;
		public ArrayList<RootValues.Variable> persistent = new ArrayList<>(3);

		public BehaviorValues copy() {
			BehaviorValues cpy = new BehaviorValues();
			cpy.classPath = classPath;
			cpy.id = id;
			cpy.enabled = enabled;
			for(RootValues.Variable v : persistent) cpy.persistent.add(v.copy());
			return cpy;
		}
	}

	public class NodeData {
		public float x = 0;
		public float y = 0;
		public int index = -1;

		public NodeData copy() {
			NodeData cpy = new NodeData();
			cpy.x = x;
			cpy.y = y;
			cpy.index = index;
			return cpy;
		}
	}

	public class ConnectionData {
		public int i1 = -1;
		public int i2 = -1;
		public float cost = 0;

		public ConnectionData copy() {
			ConnectionData cpy = new ConnectionData();
			cpy.i1 = i1;
			cpy.i2 = i2;
			cpy.cost = cost;
			return cpy;
		}
	}
	
	public final class WorldObjectValues {
		public String id = "missing";  //The name. The Root object must be extra set
		public String addr = ""; //Addresses can only contain upper and lowercase letters and numbers. No special chars
		public float x = 0;
		public float y = 0;
		//public float yOff = 0;  //Y offset for layers -> Moved to root objects
		public int rotation = 0;
		public boolean visible = true;
		//public long relative = WorldObject.NO_ADDR;  //This is an object that moves relative to this object
		public ArrayList<BehaviorValues> behaviors = new ArrayList<>(0);
		//MOVED TO ROOT OBJECT public String behavior = null; //Contains the full class path to a class that extends Behavior (example: com.mygdx.entites.Player -> Player behavior behavior)
		//MOVED TO WORLDOBJECT AGAIN
		public RootValues.RootObjectValues change = new RootValues.RootObjectValues();  //Contains changes from the original every other value is default

		public int groupIndex = 1; //Collision filtering
		public ArrayList<Integer> maskBits = new ArrayList<>();
		public int categoryBits = 1;

		public Vector2 force = new Vector2();
		public ArrayList<RootValues.Variable> hash = new ArrayList<>(0); //Will only have String values


		public WorldObjectValues() {
		}
		
		public final WorldObjectValues copy() {
			return copy(this);
		}
		
		public final static WorldObjectValues copy(WorldObjectValues values) {
			WorldObjectValues cpy = new WorldObjectValues();
			cpy.id = values.id;
			cpy.x = values.x;
			cpy.y = values.y;
			cpy.rotation = values.rotation;
			cpy.change = values.change.copy();
			cpy.addr = values.addr;
			cpy.visible = values.visible;
			//cpy.relative = values.relative;
			cpy.force = values.force.cpy();
			for(BehaviorValues b : values.behaviors) cpy.behaviors.add(b.copy());
			for(RootValues.Variable v : values.hash) cpy.hash.add(v.copy());
			cpy.groupIndex = values.groupIndex;
			cpy.categoryBits = values.categoryBits;
			for(int i : values.maskBits) cpy.maskBits.add(i);
			return cpy;
		}

		/**Same as {@link WorldObjectValues#applyChanges(RootObject)}. Ignores change field and id field
		 * @param addArrays If true, array values are addet to the old values. Otherwise all indexes will get overwritten*/
		public void applyChanges(WorldObjectValues values, boolean addArrays) {
			if(values.x != 0) x = values.x;
			if(values.y != 0) y = values.y;
			if(values.rotation != 0) rotation = values.rotation;
			visible = values.visible;
			if(!addArrays) behaviors.clear();
			if(!values.behaviors.isEmpty()) {
				for(BehaviorValues v : values.behaviors) behaviors.add(v.copy());
			}
			if(values.groupIndex != 1) groupIndex = values.groupIndex;
			if(!addArrays) maskBits.clear();
			if(!values.maskBits.isEmpty()) {
				for (int i : values.maskBits) maskBits.add(i);
			}
			if(values.categoryBits != 0x0001) categoryBits = values.categoryBits;
			if(!values.force.isZero()) force.set(values.force);
		}
		
		/**Takes the {@link WorldObjectValues#change} object and applies them to the object, when made.
		 * Ignores various variables like: rotation or shape*/
		public void applyChanges(RootObject object) {
			if(!change.texturePath.isEmpty()) object.values.texturePath = change.texturePath;
			if(change.tX != -1) object.values.tX = change.tX;
			if(change.tY != -1) object.values.tX = change.tY;
			if(change.tW != -1) object.values.tX = change.tW;
			if(change.tH != -1) object.values.tX = change.tH;
			if(!change.size.isZero()) object.values.size.set(change.size);
			if(change.scale != 1) object.values.scale = change.scale;
			if(!change.textureOffset.isZero()) object.values.textureOffset.set(change.textureOffset);  //TODO More possible changes
			if(change.fixedRotation) object.values.fixedRotation = true;
			if(change.animation != null) object.values.animation = change.animation.copy();
			if(change.yOff != 0) object.values.yOff = change.yOff;
			if(!change.fixed) object.values.fixed = change.fixed;
			if(change.grip != 0.5f) object.values.grip = change.grip;
			if(change.type != 0) object.values.type = change.type;
			if(change.fixtures != null) {
				object.values.fixtures = new ArrayList<>(change.fixtures.size());
				for (RootValues.Fixture f : change.fixtures) object.values.fixtures.add(f.copy());
			}
			if(change.light != null) object.values.light = change.light.copy();
			if(!change.fixed) object.values.fixed = change.fixed; //TODO filtering data
			if(change.trigger != null) object.values.trigger = change.trigger;
			object.values.fixedRotation = change.fixedRotation;
			if(change.r != 1) object.values.r = change.r;
			if(change.g != 1) object.values.g = change.g;
			if(change.b != 1) object.values.b = change.b;
			if(change.a != 1) object.values.a = change.a;
		}
		
		/**Compares two {@link RootObject}'s and returns a new one with just the changes set.
		 * <b>If a difference was found, the value of obj1 is taken.</b>*/
		public static RootValues.RootObjectValues compare(RootObject obj1, RootObject obj2) {
			RootValues.RootObjectValues change = new RootValues.RootObjectValues();
			if(!obj1.values.id.equals(obj2.values.id)) change.id = obj1.values.id;
			if(!obj1.values.texturePath.equals(obj2.values.texturePath)) change.texturePath = obj1.values.texturePath;
			if(obj1.values.tX != obj2.values.tX) change.tX = obj1.values.tX;
			if(obj1.values.tY != obj2.values.tY) change.tX = obj1.values.tY;
			if(obj1.values.tW != obj2.values.tW) change.tX = obj1.values.tW;
			if(obj1.values.tH != obj2.values.tH) change.tX = obj1.values.tH;
			if(obj1.values.initRotation != obj2.values.initRotation) change.initRotation = obj1.values.initRotation;
			if(!obj1.values.size.sub(obj2.values.size).isZero()) change.size.set(obj1.values.size);
			if(obj1.values.scale != obj2.values.scale) change.scale = obj1.values.scale;
			if(!obj1.values.textureOffset.sub(obj2.values.textureOffset).isZero()) change.textureOffset.set(obj1.values.textureOffset);
			if(obj1.values.fixedRotation != obj2.values.fixedRotation) change.fixedRotation = obj1.values.fixedRotation;
			if(obj1.values.type != obj2.values.type) change.type = obj1.values.type;
			if(obj1.values.trigger != null) change.trigger = obj1.values.trigger;
			if(obj1.values.yOff != obj2.values.yOff) change.yOff = obj1.values.yOff;
			if(obj1.values.animation != null) change.animation = obj1.values.animation.copy();
			if(obj1.values.light != null) change.light = obj1.values.light.copy();
			if(obj1.values.r != 1) change.r = obj1.values.r;
			if(obj1.values.g != 1) change.g = obj1.values.r;
			if(obj1.values.b != 1) change.b = obj1.values.r;
			if(obj1.values.a != 1) change.a = obj1.values.r;
			return change;
		}
	}
	
	public final class WorldData {
		public String displayWorldName = "";
		public String displayWorldDescription = "";
		public static final float tilesizePX_default = 40;

		public float scl = 1; 				//Default values
		public float tilesizeMETERS = 1;
		public int sizeX = -1;
		public int sizeY = -1;
		public float ambient = 1;
		public String loadScript = ""; //Scripts executed like the name is explaining
		public Vector2 origin = new Vector2();
		public ViewportData viewport = new ViewportData();
		public ArrayList<NodeData> grapth = new ArrayList<>();
		public ArrayList<ConnectionData> connections = new ArrayList<>();
		public ChunkData[][] chunks = new ChunkData[0][0];

		public WorldData() { //No-arg constructor
		}
		
		/**Returns a copy of this root data object. 
		 * <br><b>NOTE: World objects are not copied!</b>*/
		public final WorldData copy() {
			return copy(this);
		}
		
		/**Returns a copy of the given RootData*/
		public static WorldData copy(WorldData data) {
			WorldData cpy = new WorldData();
			cpy.scl = data.scl;
			cpy.tilesizeMETERS = data.tilesizeMETERS;
			cpy.origin =  new Vector2(data.origin.x, data.origin.y);
			cpy.viewport.rd = data.viewport.rd;
			cpy.viewport.x = data.viewport.x;
			cpy.viewport.y = data.viewport.y;
			cpy.viewport.zoom = data.viewport.zoom;
			cpy.viewport.focus = data.viewport.focus;
			cpy.viewport.edge = data.viewport.edge;
			cpy.sizeX = data.sizeX;
			cpy.sizeY = data.sizeY;

			cpy.loadScript = data.loadScript;

			cpy.chunks = new ChunkData[data.chunks.length][];
			for(int i = 0; i < data.chunks.length; i++) {
				cpy.chunks[i] = new ChunkData[data.chunks[i].length];
				for(int j = 0; j < data.chunks[i].length; j++) {
					cpy.chunks[i][j] = data.chunks[i][j].copy();
				}
			}
			cpy.ambient = data.ambient;
			cpy.grapth = new ArrayList<>();
			for(NodeData n : data.grapth) cpy.grapth.add(n.copy());
			cpy.connections = new ArrayList<>();
			for(ConnectionData c : data.connections) cpy.connections.add(c.copy());
			return cpy;
		}
	}
}
