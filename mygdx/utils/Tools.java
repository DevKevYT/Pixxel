package com.mygdx.utils;

import java.util.ArrayList;

public interface Tools {
	public final class Hitbox {
		private Hitbox(){};
		
		public static final boolean hitbox(float x, float y, float boxX, float boxY, float boxWidth, float boxHeight) {
			float edgeX = boxX + boxWidth;
			float edgeY = boxY + boxHeight;
			return x > boxX && x < edgeX && y > boxY && y < edgeY;
		}
	}

	public final class Map {
		public static final float map(float value, float istart, float istop, float ostart, float ostop) {
			return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
		}
	}
	
	public final class Convert  {

		public static final short convertToMask(ArrayList<Integer> maskBits) {
			short converted = 0;
			for(int i = 0; i < maskBits.size(); i++) {
				converted = (short) (converted | maskBits.get(i));
			}
			return converted;
		}

		public static final boolean testForFloat(String string) {
			try {
				Float.valueOf(string);
				return true;
			} catch(Exception e) {
				return false;
			}
		}
		
		public static final boolean testForInteger(String string) {
			try {
				Integer.valueOf(string);
				return true;
			} catch(Exception e) {
				return false;
			}
		}

		public static final boolean testForBoolean(String string) {
			try {
				Boolean.valueOf(string);
				return true;
			} catch(Exception e) {
				return false;
			}
		}
		
		public static final Float toFloat(String string) {
			try {
				Float f = Float.valueOf(string);
				return f;
			} catch(Exception e) {
				return null;
			}
		}
		
		public static final Integer toInteger(String string) {
			try {
				Integer f = Integer.valueOf(string);
				return f;
			} catch(Exception e) {
				return null;
			}
		}
		
		public static final boolean testForNumbers(String... string) {
			for(String s : string) {
				if(!testForFloat(s)) return false;
			}
			
			return true;
		}
	}
}
