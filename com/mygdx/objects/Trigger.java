package com.mygdx.objects;

import java.util.ArrayList;

import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.mygdx.darkdawn.GenerateScriptFunctions;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.WorldValues.TriggerValues;

public class Trigger {

	public Process process = new Process(true);
	private WorldObject parent;
	private ArrayList<WorldObject> joinTracker = new ArrayList<>(1);
	private ArrayList<WorldObject> touching = new ArrayList<>(1);
	private ArrayList<WorldObject> previous = new ArrayList<>(1);
	private String errors;
	//private ArrayList<Tile> hitboxTiles = new ArrayList<>(2);
	//private ArrayList<WorldObject> touched = new ArrayList<>(2);
//	private float prevSizeX = 0;
//	private float prevSizeY = 0;
	private boolean executing = false;
	private boolean refresh = true;
	private boolean disabled = false;

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

	public ArrayList<WorldObject> getJoinFilter() {
		return joinTracker;
	}

	/**Sets the join trigger data. You need to update the list with refreshJoinTrigger after*/
	public void setJoinFilter(String... addresses) {
		this.joinTracker.clear();
		a: for(String addr : addresses) {
			for(String e : values.joinFilter) {
				if(e.equals(addr)) continue a;
			}
			values.joinFilter.add(addr);
		}
		refresh = true;
	}

	/**Refreshes the tracked objects. Call this function if you modiy the joinTrigger list during runtime*/
	public void refreshJoinTrigger() {
		//You have the string list. Now fetch the WorldObjects
		joinTracker.clear();
		for(String s : values.joinFilter) {
			WorldObject[] obj = parent.getWorld().getObjectsByAddress(s);
			for(WorldObject o : obj) {
				joinTracker.add(o);
			}
		}
		refresh = true;
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

	/**Temporary disable trigger*/
	public void disable(boolean disable) {
		this.disabled = disable;
	}

	public boolean isDisabled() {
		return disabled;
	}

	/**The keycode that gets passed as argument for onclick. Mousebutton keys are negative*/
	int key = 0;
	WorldObject trigger = null;
	boolean alreadyExecuted = false;

	public void update() {
		if(!process.isRunning()) executing = false;
		if(parent == null || disabled) return;

		if(refresh) {
			if(!values.joinFilter.isEmpty()) {
				refreshJoinTrigger();
				Logger.logInfo("Trigger" + parent.getID(), "Join Filter refreshed!");
			}
			refresh = false;
		}

		if(!joinTracker.isEmpty() && !executing) {
			touching.clear();
			for (WorldObject w : joinTracker) {
				if (parent.touched(w.getPosition())) {
					boolean found = false;
					WorldObject invoker = null;
					for (WorldObject p : previous) {
						if (p.equals(w)) {
							found = true;
							break;
						}
					}
					if (!found) {
						invoker = w;
						joinTrigger(invoker);
					}
					touching.add(w);
				}
			}
			previous.clear();
			previous.addAll(touching);
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
   		execute();
	}

	public void leaveTrigger(WorldObject leaver) {
		process.removeVariable("leaver");
		process.setVariable("leaver", leaver, true, true);
		process.removeVariable("event");
		process.setVariable("event", "onleave", true, true);
		//process.setVariable("invoker", trigger, true, true);
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
			//process.setVariable("world", parent.getWorld(), true, true);
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
