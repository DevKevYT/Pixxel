package com.mygdx.objects;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.mygdx.darkdawn.GenerateScriptFunctions;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.objects.WorldValues.TriggerValues;

public class Trigger {

	public Process process = new Process(true);
	private WorldObject parent;
	private String errors;
	private ArrayList<Tile> hitboxTiles = new ArrayList<>(2);
	private ArrayList<WorldObject> touched = new ArrayList<>(2);
	private float prevSizeX = 0;
	private float prevSizeY = 0;
	private boolean executing = false;

	/**Key to activate trigger. > 0 = Mouse Mouse-button-codes <0 = Key-codes 0 = touch*/
	public TriggerValues values = new TriggerValues();

	public Trigger(WorldObject parent, TriggerValues values) {
		this.parent = parent;
		this.values = values.copy();
		setScript(this.values.scr);
		process.includeLibrary(new GenerateScriptFunctions.GenerateCommands());
	}
	
	public Trigger(WorldObject parent) {
		this.parent = parent;
		process.includeLibrary(new GenerateScriptFunctions.GenerateCommands());
	}
	
	private void checkTouchedTiles() {
		prevSizeX = parent.getSize().x;
		prevSizeY = parent.getSize().y;
		hitboxTiles.clear();
		float bottomLeftX = parent.getPosition().x - parent.getSize().x * parent.getScale() * parent.getWorld().getPPM() * 0.5f;
		float bottomLeftY = parent.getPosition().y - parent.getSize().y * parent.getScale() * parent.getWorld().getPPM() * 0.5f;
		Tile touched = parent.getWorld().estimateTile(bottomLeftX, bottomLeftY, true);
		float touchedBottomLeftX = touched.groundTile.getPosition().x - parent.getWorld().getTileSizeNORM() * 0.5f;
		float touchedBottomLeftY = touched.groundTile.getPosition().y - parent.getWorld().getTileSizeNORM() * 0.5f;

		if (touched != null) {
			int tileX = (int) ((bottomLeftX - touchedBottomLeftX + parent.getSize().x) / parent.getWorld().getTileSizeNORM());
			int tileY = (int) ((bottomLeftY - touchedBottomLeftY + parent.getSize().y) / parent.getWorld().getTileSizeNORM());
			hitboxTiles = parent.getWorld ().getTilesInRadius(bottomLeftX, bottomLeftY, tileX, tileY, true, hitboxTiles);
		}

		refreshTouchedObjects();
	}

	private void refreshTouchedObjects() {
		this.touched.clear();
		for(Tile t : hitboxTiles) {
			for(WorldObject w : t.objects) {
				if(parent.touched(w.getPosition())) this.touched.add(w);
			}
		}
	}
	
	public Trigger() {
		process.includeLibrary(new GenerateScriptFunctions.GenerateCommands());
	}
	
	public void setKeyTrigger(int... keyCode) {
		addKeyTrigger(keyCode);
	}
	
	/**How should the trigger get activated? More info at {@link Trigger#keys}
	 * 0 For Button event*/
	public void addKeyTrigger(int... keyCode) {
		for(int code : keyCode) {
			if(!keyPresent(code)) values.keys.add(code);
		}
	}
	
	public void addTouchTrigger() {
		if(!keyPresent(0)) values.keys.add(0);
	}
	
	/**Command must already be compiled without errors,
	 * warnings are allowed, otherwise the script is empty*/
	public void setScript(String command) {
		process.getOutput().clear();
		process.addOutput(new Output() {
			@Override
			public void log(String s, boolean b) {
				Logger.logInfo("Trigger " + parent.getID(), s);
			}

			@Override
			public void error(String s) {
				errors += s + "\n";
				Logger.logError("Trigger " + parent.getID(), s);
			}

			@Override
			public void warning(String s) {
				Logger.logInfo("Trigger " + parent.getID() + " warning: ", s);
			}
		});

		values.scr = command;
		Logger.logInfo("Trigger " + parent.getID(), "Trigger set!");
	}

	/**The keycode that gets passed as argument for onclick. Mousebutton keys are negative*/
	int key = 0;
	WorldObject trigger = null;
	boolean alreadyExecuted = false;

	public void update() {
		if(!process.isRunning()) executing = false;
		if(parent == null) return;

		if((parent.moved() || prevSizeX != parent.getRootValues().values.size.x || prevSizeY != parent.getRootValues().values.size.y) && values.tileCheck) checkTouchedTiles();
		if(!values.tileCheck) {
			prevSizeX = -1;
			prevSizeY = -1;
		}

		if(Main.debug.debugMode) return;

		if(values.tileCheck) {
			if ((int) (Gdx.graphics.getFramesPerSecond() * .25f) > 0) {
				if (Gdx.graphics.getFrameId() % (int) (Gdx.graphics.getFramesPerSecond() * .25f) == 0) {

					main:
					for (Tile t : hitboxTiles) {
						for (WorldObject actual : t.objects) {
							if (parent.touched(actual.getPosition())) {
								boolean found = false;
								this.trigger = actual;
								WorldObject invoker = null;
								for (WorldObject known : touched) {
									if (known.equals(actual)) {
										found = true;
										break;
									} else invoker = actual;
								}
								if (!found) {
									joinTrigger(invoker);
									break main;
								}
							}
						}
					}
					refreshTouchedObjects();
				}
			}
		}
	}
	
	public void keyTrigger() {
		process.setVariable("event", "onkey", true, true);
		process.setVariable("key", key != 0 ? String.valueOf(key) : null, true, true);
		execute();
	}

	public void joinTrigger(WorldObject invoker) {
		process.removeVariable("invoker");
		process.setVariable("invoker", invoker, true, true);
		process.removeVariable("event");
		process.setVariable("event", "onjoin", true, true);
		process.setVariable("invoker", trigger, true, true);
   		execute();
	}

	public void removeTrigger() {
		process.removeVariable("event");
		process.setVariable("event", "onremove", true, true);
		execute();
	}

	public void invokeTrigger() {
		process.removeVariable("event");
		process.setVariable("event", "invoke", true, true);
		execute();
	}

	/**Fires an event with the custom event variable name $event == eventName*/
	public void customEvent(String eventName) {
		process.removeVariable("event");
		process.setVariable("event", eventName, true, true);
		execute();
	}

	public void execute() {
		if(process.isRunning()) return;
		try {
			executing = true;
			errors = "";
			process.setVariable("self", parent, true, true);
			process.setVariable("world", parent.getWorld(), true, true);
			process.execute(values.scr, true);
		} catch(Exception e) {
			Logger.logError("Trigger", "A script error occurred, while executing object trigger! " + e.toString());
		}
		alreadyExecuted = true;
	}

	public boolean isRunning() {
		return executing;
	}

	public String getErrors() {
		return errors;
	}
	
	private boolean keyPresent(int keyCode) {
		for(int code : values.keys) {
			if(code == keyCode) return true;
		}
		return false;
	}
}
