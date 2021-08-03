package com.pixxel.objects;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.pixxel.utils.Tools;

public class Tile {
	protected int x, y; //x and y are the indices of the tile
	public WorldObject groundTile;
	public Chunk chunk;
	protected ArrayList<WorldObject> objects = new ArrayList<WorldObject>();

	public Tile(WorldObject groundTile, int x, int y, Chunk chunk) {
		this.groundTile = groundTile;
		this.x = x;
		this.y = y;
		this.chunk = chunk;
	}

	public void attachObject(WorldObject object) {
		releaseObject(object);
		object.bindingTile = this;
		objects.add(object);
	}

	public void releaseObject(WorldObject object) {
		if(object.bindingTile != null) object.bindingTile.objects.remove(object);
		object.bindingTile = null;
	}
	
	/**Checks, if a point is over a tile (Scaled positions)*/
	public boolean onTile(float x, float y, float scale) {
		Vector2 pos = groundTile.getPosition().scl(scale);
		Vector2 size = groundTile.getSize().scl(scale);

		return Tools.Hitbox.hitbox(x, y, pos.x - size.x*.5f,
				pos.y-size.y*.5f, size.x, size.y);
	}

	public WorldObject getObject() {
		return groundTile;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}

	public String toString() {
		return x + " " + y;
	}
}
