package com.mygdx.darkdawn;

import java.util.ArrayList;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.mygdx.ai.GraphData;
import com.mygdx.darkdawn.GenerateScriptFunctions.GenerateCommands;
import com.mygdx.engineUtils.DebugWindow;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.Game;
import com.mygdx.objects.GameValues;
import com.mygdx.objects.RootObject;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.WorldValues;

/**Designed for Desktop and Android only.
 * @author Philipp Gersch*/
public class Main extends ApplicationAdapter {
	//public static OrthographicCamera camera;
	public static GenerateCommands commands;

	public static DebugWindow debug;
	//public static CommandLine cmd;

	public static InputMultiplexer inputs;
	public static Game game;
	public static Cursor default_cursor;
	@Override
	public void create () {
		Skin skin = new Skin();
		skin.addRegions(new TextureAtlas(Gdx.files.internal("UI//uiskin.atlas")));
		skin.addRegions(new TextureAtlas(Gdx.files.internal("UI//gameui.atlas")));
		skin.load(Gdx.files.internal("UI//config.json"));

		game = new Game(Gdx.files.local("local/game.json"));
		game.build();
		game.setGui(skin);
		game.loadWorld(game.getCurrentWorld());

		debug = new DebugWindow(game, game.getLibrary(), skin);

		inputs = new InputMultiplexer();
		inputs.addProcessor(debug.stage);
		inputs.addProcessor(game.gui);
		Gdx.input.setInputProcessor(inputs);

		Pixmap p = new Pixmap(Gdx.files.internal("UI//default-cursor.png"));
		default_cursor = Gdx.graphics.newCursor(p, 5, 6);
		Gdx.graphics.setCursor(default_cursor);

		Item.loadItemLibrary(Gdx.files.internal("local/items/items.json"));
	}

	@Override
	public void render () {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glClearColor(0, 0, 0, 1);

		game.stage.updateViewport();
		game.stage.update(Gdx.graphics.getDeltaTime());
		game.stage.draw(Gdx.graphics.getDeltaTime());

		game.gui.act(Gdx.graphics.getDeltaTime());
		game.gui.draw();

		debug.update();

		if(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.F3)) {
			game.chat.hideCmd();
			debug.setDebug(!debug.debugMode);
		}
		if(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.T)) {
			game.chat.setCmd(true);
		}
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE)) game.chat.hideCmd();

		if(Gdx.input.isKeyJustPressed(Keys.F11) && !Gdx.graphics.isFullscreen()) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		else if(Gdx.input.isKeyJustPressed(Keys.F11) && Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(800, 480);
	}
	
	static public float map(float value, float istart, float istop, float ostart, float ostop) {
	    return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
	}
	
	@Override
	public void resize(int width, int height) {
		debug.updateViewport(width, height, game.gui.getCamera());
		game.resize(width, height);
	}
	
	@Override
	public void dispose () {
		game.dispose();
	}
}
