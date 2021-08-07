package com.pixxel.objects;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.pixxel.objects.WorldValues.TriggerValues;
import com.pixxel.files.JsonHandler;

public interface RootValues {

	public class Fixture { //Newer Version of FixtureDefinition, to reduce RootObject filesize and improve AI support
		public boolean isCircle = false; //If true, WIDTH is used as radius
		public float density = 1f;
		public float xOff = 0f;
		public float yOff = 0f;

		public float width = 0;
		public float height = 0;
		public float rotation = 0;  //Rotation in degrees

		public Fixture copy() {
			Fixture cpy = new Fixture();
			cpy.isCircle = isCircle;
			cpy.density = density;
			cpy.xOff = xOff;
			cpy.yOff = yOff;
			cpy.width = width;
			cpy.height = height;
			cpy.rotation = rotation;
			return cpy;
		}
	}

	public class Animation {
		public int framerate = 0;
		public ArrayList<Frame> frames = new ArrayList<Frame>();

		public Animation copy() {
			Animation cpy = new Animation();
			cpy.framerate = framerate;
			for(Frame f : frames) {
				Frame c = new Frame();
				c.x = f.x;
				c.y = f.y;
				c.w = f.w;
				c.h = f.h;
				cpy.frames.add(c);
			}
			return cpy;
		}
	}

	//Region from a spritesheet
	public class Frame {
		public int x = 0;
		public int y = 0;
		public int w = 0;
		public int h = 0;
	}

	public class LightSource {
		public float x = 0;
		public float y = 0; //Position relative to the objects origin
		public float r = 1;  //rgb colors (White as default)
		public float g = 1;
		public float b = 1;
		public float a = 1;
		public float dist = 0;
		public float softDist = 0;
		public int rays = 25;
		public boolean xray = false;

		public LightSource copy() {
			LightSource cpy = new LightSource();
			cpy.x = x;
			cpy.y = y;
			cpy.r = r;
			cpy.g = g;
			cpy.b = b;
			cpy.a = a;
			cpy.dist = dist;
			cpy.softDist = softDist;
			cpy.rays = rays;
			cpy.xray = xray;
			return cpy;
		}

		public boolean equals(LightSource other) {
			return other.x == x && other.y == y && other.r == r && other.g == g && other.b == b && other.a == a && dist == other.dist
					&& other.softDist == softDist && other.rays == rays && other.xray == xray;
		}
	}

	public enum Type {
		INT, STR, FLOAT, BOOL;
	}

	public class Variable {
		public Type T = Type.STR;
		public String N = "";

		public int I = 0;
		public float F = 0f;
		public String S = "";
		public boolean B = false;

		public Variable() {}

		public Variable copy() {
			Variable cpy = new Variable();
			cpy.N = N;
			cpy.I = I;
			cpy.F = F;
			cpy.S = S;
			cpy.B = B;
			cpy.T = T;
			return cpy;
		}

		public String toString() {
			if(T == Type.INT) return String.valueOf(I);
			if(T == Type.STR) return S;
			if(T == Type.FLOAT) return String.valueOf(F);
			if(T == Type.BOOL) return String.valueOf(B);
			return S;
		}
	}

	public class RootObjectValues {

		public static final String tileset = "TILESET";
		public static final String atlas = "ATLAS"; //The atlas file needs to have the same path and name as the texturePath (but with .atlas extension)
		public static final String none = "DEFAULT";

		public static final int DYNAMIC = 0;
		public static final int STATIC = 1;

		public String loadType = none; //TILESET
		public String id = "";  //The 'name' of the object. Should be unique in a world
		public String texturePath = "";  //The internal assets path to a picture, where the texture gets loaded from (# at beginning: local path, * at beginning: texture relative for object file
		public Animation animation = null;  //Manages an animation (beta)
		//public ArrayList<BehaviorValues> behavior = new ArrayList<>();
		//MOVED TO WORLDOBJECT AGAIN
		public int tX = -1;  //Manages the texture region of a picture. The region is only
		public int tY = -1;  //recognized, when all region bounds are not -1
		public int tW = -1;
		public int tH = -1;
		public float yOff = 0;  //Manages the 'layers' Je kleiner der wert ist desto weiter 'unten' wird dieses Object gerendert
		public int initRotation = 0;  //Should the object have a inititlal rotation?
		public TriggerValues trigger = null;  //Trigger
		public LightSource light = null;
		//public ArrayList<FixtureDefinition> fixtures = new ArrayList<>(); //Fixtures for hitboxes (Managed by box2d) OBSOLETE, Managed now by Fixture class
		public ArrayList<Fixture> fixtures = null;
		
		/**Changeable 1 = max grip 0 = min grip other = going crazy*/
		public float grip = 0.3f; //Grip of the object, can be changed at runtime to simulate ice etc.
		public boolean fixed = true; //Fixed objects are NOT able to move. You need to set this value to false, if you create behaviors, where objects need to move
		public float scale = 1;  //Scale of the object (usually 1)
		public Vector2 size = new Vector2();  //Size of the object. Manipulated by the scale. (-1 = tile size -0.5 = Half of tile size -2 = Twice the tilesize. (-1 for both is just 0) (Done in checkSize() in RootObject class)
		                                      //0 at y or y = scale image | (x or y) < 0 = scale of tile size
		public Vector2 textureOffset = new Vector2(); //Offset
		public float r = 1, g = 1, b = 1, a = 1;
		//Body definition
		public boolean fixedRotation = false;
		public int type = DYNAMIC; //0 = Dynamic 1 = Static

		public RootObjectValues() {
		}
		
		/**Returns a new instance of {@link RootObjectValues} with the same values.*/
		public final RootObjectValues copy() {
			RootObjectValues copy = new RootObjectValues();
			copy.id = id;
			copy.texturePath = texturePath;
			copy.tX = tX;
			copy.tY = tY;
			copy.tW = tW;
			copy.tH = tH;
			copy.scale = scale;
			copy.initRotation = initRotation;
			copy.size.set(new Vector2(size.x, size.y));
			copy.textureOffset.set(new Vector2(textureOffset.x, textureOffset.y));
			copy.fixedRotation = fixedRotation;
			copy.type = type;
			copy.fixed = fixed;
			if(trigger != null) copy.trigger = trigger.copy();
			if(fixtures != null) {
				copy.fixtures = new ArrayList<>(fixtures.size());
				for(Fixture f : fixtures) copy.fixtures.add(f.copy());
			}
			if(animation != null) {
				copy.animation = new Animation();
				copy.animation.framerate = animation.framerate;
				for(Frame f : animation.frames) {
					Frame framecopy = new Frame();
					framecopy.x = f.x;
					framecopy.y = f.y;
					framecopy.w = f.w;
					framecopy.h = f.h;
					copy.animation.frames.add(framecopy);
				}
			}
			copy.yOff = yOff;
			if(light != null) copy.light = light.copy();
			copy.loadType = loadType;
			copy.r = r;
			copy.g = g;
			copy.b = b;
			copy.a = a;
			return copy;
		}

		/**Overwrites all values, that are non-default in value object
		 * If, for example scale in values is not 1 (default value) this value will override the scale field in this
		 * object.<br>Ignores the following fields (final): <ul><li>id</li><li>loadType</li></ul>*/
		public void applyChanges(RootObjectValues values, boolean addArray) {
			if(!values.texturePath.isEmpty()) texturePath = values.texturePath;
			if(values.tX != -1) tX = values.tX;
			if(values.tY != -1) tY = values.tY;
			if(values.tW != -1) tX = values.tW;
			if(values.tH != -1) tX = values.tH;
			if(!values.size.isZero()) size.set(values.size);
			if(values.scale != 1) scale = values.scale;
			if(!values.textureOffset.isZero()) textureOffset.set(values.textureOffset);
			fixedRotation = values.fixedRotation;
			if(values.animation != null) animation = values.animation.copy();
			if(values.yOff != 0) yOff = values.yOff;
			fixed = values.fixed;
			if(values.grip != 0.5f) grip = values.grip;
			if(values.type != RootObjectValues.DYNAMIC) type = values.type;
			if(values.fixtures != null) {
				fixtures = new ArrayList<>(values.fixtures.size());
				for (Fixture f : values.fixtures) fixtures.add(f.copy());
			}
			if(values.light != null) light = values.light.copy();
			if(values.trigger != null) trigger = values.trigger;
			if(values.initRotation != 0) initRotation = values.initRotation;
			if(values.r != 1) r = values.r;
			if(values.g != 1) g = values.g;
			if(values.b != 1) b = values.b;
			if(values.a != 1) a = values.a;
		}
		
		/**Loads values from a JSON file.
		 * @return - The loaded values. Null, when an exception occured*/
		public static RootObjectValues loadFromJSON(String path) {
			RootObjectValues values = new RootObjectValues();
			com.pixxel.files.JsonHandler handler = new JsonHandler();
			values = handler.readJSON(path, RootObjectValues.class);
			return values;
		}
	}
}
