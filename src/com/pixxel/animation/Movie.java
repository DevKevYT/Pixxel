package com.pixxel.animation;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

public class Movie {
	
	public int frameRate = 1;
	protected final ArrayList<Sprite> frames = new ArrayList<Sprite>();
	protected String id;
	
	public Movie(int frameRate, final Sprite... frames){
		//if(frames.length == 0) throw new IllegalArgumentException("Every movie should have at least one frame!");
		if(frameRate == 0) throw new IllegalArgumentException("The framerate can't be zero");
		
		for(Sprite s : frames) this.frames.add(s);
		this.frameRate = frameRate;
	}

	public Movie(int frameRate) {
		if(frameRate == 0) throw new IllegalArgumentException("The framerate can't be zero");
		this.frameRate = frameRate;
	}
	
	public Movie(int frameRate, final Array<AtlasRegion> regions){
		if(regions.size == 0) throw new IllegalArgumentException("Every movie should have at least one frame!");
		if(frameRate == 0) throw new IllegalArgumentException("The framerate can't be zero");
		
		for(AtlasRegion a : regions) 
			this.frames.add(new Sprite(a));
		
		this.frameRate = frameRate;
	}

	/**Loads all regions from a spritesheet into a movie*/
	public Movie(int frameRate, FileHandle atlasFile) {
		if (frameRate == 0) throw new IllegalArgumentException("Framerate can`t be zero");
		TextureAtlas regions = new TextureAtlas(atlasFile);
		ObjectSet<Texture> textures = regions.getTextures();
		if (textures.size == 0) throw new IllegalArgumentException("The movie needs to have at least one frame");

		for(Texture t : textures) this.frames.add(new Sprite(t));
		this.frameRate = frameRate;
	}

	/**Loads specified regions of an atlas file*/
	public Movie(int frameRate, FileHandle atlasFile, String... regions) {
		if (frameRate == 0) throw new IllegalArgumentException("Framerate can`t be zero");
		TextureAtlas r = new TextureAtlas(atlasFile);
		ObjectSet<Texture> textures = r.getTextures();
		if (textures.size == 0) throw new IllegalArgumentException("The movie needs to have at least one frame");

		for(String s : regions) this.frames.add(new Sprite(r.findRegion(s)));
		this.frameRate = frameRate;
	}

	public Movie(int frameRate, TextureAtlas atlas, String... regions) {
		if (frameRate == 0) throw new IllegalArgumentException("Frame rate can`t be zero");
		if (regions.length == 0) throw new IllegalArgumentException("The movie needs to have at least one frame");

		for(String s : regions) {
			this.frames.add(new Sprite(atlas.findRegion(s)));
		}
		this.frameRate = frameRate;
	}

	public static final Sprite load(String internal) {
		return new Sprite(new Texture(Gdx.files.local(internal)));
	}
	
	public Sprite[] getFrames() {
		return frames.toArray(new Sprite[frames.size()]);
	}
	
	public void addFrame(Sprite... frames) {
		for(Sprite s : frames) 
			this.frames.add(s);
	}

	public void addRegion(String region, TextureAtlas atlas) {
		this.frames.add(new Sprite(atlas.findRegion(region)));
	}
	
	public String getId() {
		return id;
	}
}