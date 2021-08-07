package com.pixxel.objects;

public class GroundData {
	
	private RootObject[][] tiles;
	public final int sizeX;
	public final int sizeY;
	
	/**sizeX and sizeY represents the actual grid size <br>
	 * (The array size in x and y direction is sizeX+1 and sizeY+1)*/
	public GroundData(int sizeX, int sizeY) {
		if(sizeX < 0) sizeX = 0;
		if(sizeY < 0) sizeY = 0;
		
		this.sizeX = sizeX+1;
		this.sizeY = sizeY+1;
		tiles = new RootObject[sizeX+1][sizeY+1];
	}
	
	public void fillGround(RootObject object) {
		for(int i = 0; i < sizeX; i++) {
			for(int j = 0; j < sizeY; j++) {
				tiles[i][j] = object.copy();
			}
		}
	}
	
	public void setTile(RootObject object, int x, int y) {
		if(x >= 0 && y >= 0 && x < sizeX && y < sizeY)
			tiles[x][y] = object;
	}
	
	public RootObject[][] getGroundData() {
		return tiles.clone();
	}
	
	public RootObject getTile(int x, int y) {
		return tiles[x][y];
	}
}
