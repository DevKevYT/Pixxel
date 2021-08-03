package com.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.pixxel.Main.GenerateScriptFunctions.GenerateCommands;
import com.pixxel.engineUtils.DebugWindow;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.Game;

/**Designed for Desktop and Android only.
 * @author DevKevYT*/
public class Main extends ApplicationAdapter {

	public static GenerateCommands commands;

	public static DebugWindow debug;

	public static InputMultiplexer inputs;
	public static Game game;
	public static Cursor default_cursor;
	@Override
	public void create () {

		Behavior.findBehaviors();

		Skin skin = new Skin();
		skin.addRegions(new TextureAtlas(Gdx.files.internal("UI//uiskin.atlas")));
		skin.addRegions(new TextureAtlas(Gdx.files.internal("UI//gameui.atlas")));
		skin.load(Gdx.files.internal("UI//config.json"));

		game = new Game(Gdx.files.local("local/game.json"));
		game.build();
		game.setGui(skin);
		game.loadWorld(game.getCurrentWorld());

		debug = new DebugWindow(game, game.getLibrary(), skin);

		game.chat.inputLine.setBounds(10, 10, Gdx.graphics.getWidth() - 10, 25);
		Gdx.input.setInputProcessor(game.gui);

		Pixmap p = new Pixmap(Gdx.files.internal("UI//default-cursor.png"));
		default_cursor = Gdx.graphics.newCursor(p, 5, 6);
		Gdx.graphics.setCursor(default_cursor);
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
			//game.chat.hideCmd();
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
