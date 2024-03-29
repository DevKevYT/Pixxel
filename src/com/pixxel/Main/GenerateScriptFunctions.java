package com.pixxel.Main;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.StringBuilder;
import com.devkev.devscript.raw.ApplicationBuilder;
import com.devkev.devscript.raw.Array;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.DataType;
import com.devkev.devscript.raw.Dictionary;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Process;
import com.devkev.devscript.raw.Block;
import com.game.Main;
import com.pixxel.objects.Behavior;
import com.pixxel.objects.Game;
import com.pixxel.objects.RootValues;
import com.pixxel.objects.World;
import com.pixxel.objects.WorldObject;
import com.pixxel.objects.WorldValues;
import com.pixxel.ui.Dialog;
import com.pixxel.ui.DialogValues;

import static com.devkev.devscript.raw.ApplicationBuilder.testForFloat;
import static com.devkev.devscript.raw.ApplicationBuilder.testForWholeNumber;

public interface GenerateScriptFunctions {

	public static class GenerateCommands extends Library {

		public GenerateCommands() {
			super("ScriptCommands");
		}

		@Override
		public Command[] createLib() {
			return new Command[]{
					new Command("getobject", "string", "getobject [id]") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.World world = com.game.Main.game.stage;
							com.pixxel.objects.WorldObject[] obj = world.getObjectsByAddress(args[0].toString());

							if (obj == null) {
								process.kill(block, "Sorry, object not found");
								return null;
							}
							if (obj.length == 0) {
								process.kill(block, "Sorry, object not found");
								return null;
							}
							return obj[0];
						}
					},

					new Command("getobjects", "string", "getobjects <id>") {
						@Override
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.World world = com.game.Main.game.stage;
							com.pixxel.objects.WorldObject[] obj = world.getObjectsByAddress(args[0].toString());

							if (obj == null) {
								process.kill(block, "Sorry, object not found");
								return null;
							}

							Array arr = new Array();
							for(com.pixxel.objects.WorldObject o : obj) arr.getIndexes().add(o);
							return arr;
						}
					},

					new Command("camera", "string ? ...", "camera <focus <obj>|zoom <float>|getzoom|getfocus|instantfocus <obj>|renderdist <distance> (Use values <0 to set the render distance to infinity>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length == 0) {
								process.kill(block, "Options are: focus,zoom,getzoom,getfocus,instantfocus");
								return null;
							}
							String arg0 = args[0].toString();
							if (arg0.equals("focus") && args.length >= 2) {
								com.pixxel.objects.WorldObject obj = (com.pixxel.objects.WorldObject) args[1];
								com.pixxel.objects.World w = obj.getWorld();
								w.cameraFocus = obj;
								process.log("Focus set to: " + obj, true);
								return null;

							} else if (arg0.equals("zoom") && args.length >= 1) {
								if (testForFloat(args[1].toString())) com.game.Main.game.stage.getViewport().zoom = Float.valueOf(args[1].toString());
								return null;
							} else if (arg0.equals("getzoom")) {
								return com.game.Main.game.stage.getViewport().zoom;
							} else if (arg0.equals("instantfocus") && args.length >= 1) {
								com.pixxel.objects.WorldObject obj = (com.pixxel.objects.WorldObject) args[1];
								obj.getWorld().cameraFocus = obj;
								Vector2 pos = obj.getPosition().scl(obj.getWorld().getScale());
								obj.getWorld().getViewport().position.set(pos.x, pos.y, 0);
							} else if (arg0.equals("getfocus")) {
								return com.game.Main.game.stage.cameraFocus;
							} else if(arg0.equals("renderdist")) {
								if(args.length > 1) {
									if(testForWholeNumber(args[1].toString())) {
										com.game.Main.game.stage.setRenderDistance(Integer.valueOf(args[1].toString()));
									}
								}
							} else process.kill(block, "Unknown option: " + arg0 + " Valid options are: focus,zoom,getzoom,getfocus,instantfocus");
							return null;
						}
					},

					new Command("filedialog", "string", "dialog <path-to-json-file>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.ui.Dialog dialog = com.game.Main.game.dialog;
							DialogValues.DialogStyle values = FileHandler.readJSON(Gdx.files.internal(args[0].toString()), DialogValues.DialogStyle.class);
							if (values == null) {
								process.kill(block, "Dialog values not found in " + args[0].toString() + "!");
								return null;
							}
							dialog.setDialog(values);
							dialog.display();
							return null;
						}
					},

					new Command("dialog", "string", "dialog [dialog config (JSON)]") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							Dialog dialog = com.game.Main.game.dialog;
							Json json = new Json();
							DialogValues.DialogStyle values = json.fromJson(DialogValues.DialogStyle.class, args[0].toString());
							dialog.setDialog(values);
							dialog.display();
							return null;
						}
					},

					new Command("loadworld", "string", "loadworld <worldname(!)> The world file needs to be registered in the game file") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							String fileName = args[0].toString();
							if (!fileName.endsWith(".json")) fileName += ".json";

							com.pixxel.objects.World w = com.game.Main.game.loadWorld(fileName);
							if (w == null)
								process.kill(block, "World: " + fileName + " not found in game file!");
							return null;
						}
					},

					new Command("createworld", "string string string", "<path+name (internal)> <size x> <size y> (Size in chunks)") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							String name = args[0].toString();
							if (!testForWholeNumber(args[1].toString())) {
								process.kill(block, "Argument 1 needs to be an integer");
								return null;
							}
							if (!testForWholeNumber(args[2].toString())) {
								process.kill(block, "Argument 2 needs to be an integer");
								return null;
							}
							int sizeX = Integer.valueOf(args[1].toString());
							int sizeY = Integer.valueOf(args[2].toString());

							if (sizeX <= 0 || sizeY <= 0) {
								process.kill(block, "Size x and y need to be greater than 0");
								return null;
							}

							if (!name.endsWith(".json")) name += ".json";

							for (String s : com.game.Main.game.getProperties().worlds) {
								if (com.pixxel.objects.Game.getFilename(s).equals(com.pixxel.objects.Game.getFilename(name))) {
									process.kill(block, "World already exists with this filename. Delete it first with deleteworld");
									return null;
								}
							}
							File worldFile = new File(Gdx.files.getLocalStoragePath() + name);
							worldFile.createNewFile();

							com.pixxel.objects.WorldValues.WorldData data = new com.pixxel.objects.WorldValues.WorldData();
							data.scl = 0.02f;
							data.sizeX = sizeX;
							data.sizeY = sizeY;
							data.viewport.rd = 2;
							FileHandler.writeJSON(Gdx.files.local(name), data, true);

							com.game.Main.game.getProperties().worlds.add(name);
							com.game.Main.game.saveGamefile();
							com.game.Main.game.verifyFiles();
							process.log("World added to Game! Load world with loadworld $world " + com.pixxel.objects.Game.getFilename(name), true);
							return null;
						}
					},

					new Command("deleteworld", "string", "deleteworld <worldname>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							String name = args[0].toString();
							if (!name.endsWith(".json")) name += ".json";

							for (int i = 0; i < com.game.Main.game.getProperties().worlds.size(); i++) {
								if (Game.getFilename(com.game.Main.game.getProperties().worlds.get(i)).equals(name)) {
									process.log("Found non existing world " + name + " in game file. Removing...", true);
									com.game.Main.game.getProperties().worlds.remove(i);
									com.game.Main.game.verifyFiles();
									break;
								}
							}
							for (String s : com.game.Main.game.getMaps().keySet()) {
								if (s.equals(name)) {
									com.game.Main.game.getMaps().get(s).delete();
									process.log("Map deleted!", true);
									return null;
								}
							}
							com.game.Main.game.saveGamefile();
							return null;
						}
					},

					new Command("listworlds", "", "Lists all veryfied world files") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							for (String s : com.game.Main.game.getMaps().keySet()) process.log(s, true);
							return null;
						}
					},

					new Command("save", "", "Saves the current loaded world") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							boolean success = com.game.Main.game.stage.save();
							com.game.Main.game.saveGamefile();
							return null;
						}
					},

					new Command("spawn", "string string string", "spawn <id> <x> <y>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldValues.WorldObjectValues values = new com.pixxel.objects.WorldValues.WorldObjectValues();
							values.id = args[0].toString();

							if(!testForFloat(args[1].toString()) || !testForFloat(args[2].toString())) {
								process.kill(block, "Arguments 2 and 3 need to be numbers!");
								return null;
							}
							values.x = Float.valueOf(args[1].toString());
							values.y = Float.valueOf(args[2].toString());
							com.pixxel.objects.WorldObject w = com.game.Main.game.stage.addObject(values);

							//process.log("Object " + w.getID() + " spawned", true);
							return w;
						}
					},

					new Command("spawn", "string string string string", "spawn <id> <x> <y> <meta>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldValues.WorldObjectValues values = new com.pixxel.objects.WorldValues.WorldObjectValues();

							if(!testForFloat(args[1].toString()) || !testForFloat(args[2].toString())) {
								process.kill(block, "Arguments 2 and 3 need to be numbers!");
								return null;
							}

							Json json = new Json();
							try {
								values = json.fromJson(com.pixxel.objects.WorldValues.WorldObjectValues.class, args[3].toString());
							} catch(Exception e) {
								values = new com.pixxel.objects.WorldValues.WorldObjectValues();
								Logger.logError("Spawn", "Mistype in JSON data: " + e.getMessage());
							}

							values.id = args[0].toString();
							values.x = Float.valueOf(args[1].toString());
							values.y = Float.valueOf(args[2].toString());
							com.pixxel.objects.WorldObject w = com.game.Main.game.stage.addObject(values);

							//process.log("Object " + w.getID() + " spawned", true);
							return w;
						}
					},

					new Command("spawnat", "obj string string string", "spawnat <obj> <offsetx> <offsety>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject relative = (com.pixxel.objects.WorldObject) args[0];
							com.pixxel.objects.WorldValues.WorldObjectValues values = new com.pixxel.objects.WorldValues.WorldObjectValues();
							values.id = args[1].toString();
							if(!testForFloat(args[2].toString()) || !testForFloat(args[3].toString())) {
								process.kill(block, "Arguments 3 and 4 need to be numbers!");
								return null;
							}
							com.pixxel.objects.WorldObject w = com.game.Main.game.stage.addObject(values);
							w.setPosition(Float.valueOf(args[2].toString()) + relative.getPosition().x, Float.valueOf(args[3].toString()) + relative.getPosition().y);
							//process.log("Object " + w.getID() + " spawned", true);
							return w;
						}
					},

					new Command("spawnat", "obj string string string string", "spawnat <obj> <offsetx> <offsety> <jsondata>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject relative = (com.pixxel.objects.WorldObject) args[0];
							com.pixxel.objects.WorldValues.WorldObjectValues values = new com.pixxel.objects.WorldValues.WorldObjectValues();
							if(!testForFloat(args[2].toString()) || !testForFloat(args[3].toString())) {
								process.kill(block, "Arguments 3 and 4 need to be numbers!");
								return null;
							}

							Json json = new Json();
							try {
								values = json.fromJson(com.pixxel.objects.WorldValues.WorldObjectValues.class, args[4].toString());
							} catch(Exception e) {
								process.kill(block, e.toString());
								return null;
							}

							values.id = args[1].toString();
							values.x = Float.valueOf(args[2].toString()) + relative.getPosition().x;
							values.y = Float.valueOf(args[3].toString()) + relative.getPosition().y;
							com.pixxel.objects.WorldObject w = com.game.Main.game.stage.addObject(values);
							//process.log("Object " + w.getID() + " spawned", true);
							return w;
						}
					},

					new Command("remove", "obj", "remove <obj>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.game.Main.game.stage.removeObject((com.pixxel.objects.WorldObject) args[0]);
							return null;
						}
					},

					new Command("behavior", "obj ? ...", "behavior <obj> <add <id> <classpath>|<obj> remove <id>|<obj> clear|<obj> list|<obj> listvars <id>|<obj>getvar <id> <name>|setvar <id> <name> <value>|listfunc <id>>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length < 2) {
								process.kill(block, "\nValid options are:\n<obj> <add <id> <classpath>\n<obj> remove <id>\n<obj> clear\n<obj> list\n<obj> listvars <id>\n<obj> getvar <id> <name>\n<obj> setvar <id> <name> <value>");
								return null;
							}
							com.pixxel.objects.WorldObject object = (com.pixxel.objects.WorldObject) args[0];
							String option = args[1].toString();

							if (option.equals("add") && args.length >= 4) {
								com.pixxel.objects.WorldValues.BehaviorValues values = new com.pixxel.objects.WorldValues.BehaviorValues();
								values.classPath = args[3].toString();
								values.id = args[2].toString();
								values.enabled = true;
								try {
									com.pixxel.objects.Behavior b = object.loadBehavior(values);
								} catch(Exception e) {
									process.kill(block, e.toString());
									return null;
								}
							} else if (option.equals("remove") && args.length >= 3) {
								object.removeBehavior(args[2].toString());
							} else if (option.equals("clear")) {
								object.getBehavior().clear();
							} else if (option.equals("list")) {
								if (object.getBehavior().isEmpty()) process.log("Object has no behavior classes!", true);
								else {
									for (com.pixxel.objects.Behavior b : object.getBehavior()) {
										if (b.isEnabled()) process.log("[enabled] " + b.getID() + ": " + b.getClassPath(), true);
										else process.log("[DISABLED] " + b.getID() + ": " + b.getClassPath(), true);
									}
								}
							} else if (option.equals("listvars") && args.length >= 3) {
								com.pixxel.objects.Behavior b = object.getBehavior(args[2].toString());
								if (b == null) process.log("No behavior with id found", true);
								else {
									HashMap variables = com.pixxel.objects.Behavior.getBehaviorVariables(b);
									for (Object s : variables.keySet()) process.log(s.toString() + " = " + variables.get(s), true);
								}
							} else if (option.equals("getvar") && args.length >= 4) {
								com.pixxel.objects.Behavior b = object.getBehavior(args[2].toString());
								if (b == null) process.log("Behavior not found!", true);
								else return com.pixxel.objects.Behavior.getBehaviorVariables(b).get(args[3].toString()).toString();
							} else if (option.equals("setvar") && args.length >= 5) {
								com.pixxel.objects.Behavior b = object.getBehavior(args[2].toString());
								if (b == null) { process.log("Behavior with id not found!", true);
								} else com.pixxel.objects.Behavior.setVariable(b, args[3].toString(), args[4]);
							} else if(option.equals("listfunc") && args.length >= 3) {
								com.pixxel.objects.Behavior b = object.getBehavior(args[2].toString());
								if(b != null) {
									for(Method m : b.getSubclassMethods()) {
										process.log(m.getName(), true);
									}
								}
							} else {
								process.kill(block, "\nValid options are:\n<obj> <add <id> <classpath>\n<obj> remove <id>\n<obj> clear\n<obj> list\n<obj> listvars <id>\n<obj> getvar <id> <name>\n<obj> setvar <id> <name> <value>");
								return null;
							}
							return null;
						}
					},

					new Command("settrigger", "obj string", "") {
						@Override
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							String all = new String();
							for(int i = 1; i < args.length; i++) {
								all += " " + args[i].toString();
							}
							com.pixxel.objects.WorldValues.TriggerValues values = new com.pixxel.objects.WorldValues.TriggerValues();
							values.scr = all;
							((com.pixxel.objects.WorldObject) args[0]).setTrigger(values);
							return null;
						}
					},

					new Command("settrigger", "obj string string @string boolean", "<obj> <script> <messageText> <keys> <joinTrigger>") {
						@Override
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldValues.TriggerValues values = new com.pixxel.objects.WorldValues.TriggerValues();
							values.scr = args[1].toString();
							values.joinFilter.clear();
							values.joinFilter.addAll(Arrays.asList(args[4].toString().split(",")));
							values.messageText = args[2].toString();
							for(Object indices : ((Array) args[3]).getIndexes()) {
								if(testForWholeNumber(indices.toString())) {
									values.keys.add(Integer.valueOf(indices.toString()));
								}
							}
							((com.pixxel.objects.WorldObject) args[0]).setTrigger(values);
							return null;
						}
					},

					new Command("reload", "obj", "Reloads an object with the default rootobject values from the file") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject obj = (com.pixxel.objects.WorldObject) args[0];
							WorldValues.WorldObjectValues values = obj.worldObjectValues.copy();
							World world = obj.getWorld();
							obj.getWorld().removeObject(obj);
							world.addObject(values);
							return null;
						}
					},

					new Command("objecthash", "obj string ...", "objecthash <object> <set <name> <value>|list|delete <name>|get <name>>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length < 1) {
								process.kill(block, "\nValid options are: " + description);
								return null;
							}
							com.pixxel.objects.WorldObject object = (com.pixxel.objects.WorldObject) args[0];
							String option = args[1].toString();
							if(option.equals("set") && args.length >= 4) {
								object.getHashWrapper().setString(args[2].toString(), args[3].toString());
							} else if(option.equals("get") && args.length >= 3) {
								com.pixxel.objects.RootValues.Variable v = object.getHashWrapper().getVar(args[2].toString());
								if(v != null) return v.S;
								else process.error("Hash with name: " + args[2].toString() + " on object " + object.getAddress() + " not found");
							} else if(option.equals("list")) {
								for(com.pixxel.objects.RootValues.Variable v : object.getHashWrapper().getFields()) process.log(v.N + " " + v.S, true);
							} else if(option.equals("delete") && args.length >= 3) {
								return object.getHashWrapper().remove(args[2].toString());
							} else process.kill(block, "Valid options are: " + description);
							return null;
						}
					},

					new Command("hash", "string ...", "hash <set <name> <value>|list|delete <name>|get <name>>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length < 1) {
								process.kill(block, "\nValid options are:\nset <name> <value>\nlist\ndelete <name>\nget <name>");
								return null;
							}
							String option = args[0].toString();
							if (option.equals("set") && args.length >= 3) com.game.Main.game.hashData.setString(args[1].toString(), args[2].toString());
							else if (option.equals("get")) return com.game.Main.game.hashData.getString(args[1].toString());
							else if (option.equals("list")) {
								for (com.pixxel.objects.RootValues.Variable v : com.game.Main.game.hashData.getFields()) {
									process.log(v.N + " = " + v.S, true);
								}
							} else if (option.equals("delete") || option.equals("del") && args.length >= 2) com.game.Main.game.hashData.remove(args[1].toString());
							else process.kill(block, "\nValid options are:\nset <name> <value>\nlist\ndelete <name>\nget <name>");
							return null;
						}
					},

					new Command("post", "string string", "post <poster> <message>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.game.Main.game.chat.post(args[0].toString(), args[1].toString(), true);
							return null;
						}
					},

					new Command("function", "obj string string @?", "function <obj> <id> <function-name> <arguments> (Arguments as array e.g. [1 2 3])") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject object = (com.pixxel.objects.WorldObject) args[0];

							ArrayList<Object> arrayValues = ((Array) args[3]).getIndexes();
							Object[] arguments = new Object[arrayValues.size()];
							for (int i = 0; i < arrayValues.size(); i++) arguments[i] = arrayValues.get(i);
							Behavior b = object.getBehavior(args[1].toString());
							if(b != null) object.invokeMethod(b.getClass(), args[2].toString(), arguments);
							else process.error("Behavior with name: " + args[1].toString() + " not found!");
							return null;
						}
					},

					new Command("interrupt", "obj", "interrupt <obj>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject obj = (com.pixxel.objects.WorldObject) args[0];
							if (obj.getTrigger() != null) obj.getTrigger().process.kill(obj.getTrigger().process.getMain(), "Interrupted by command");
							return null;
						}
					},

					new Command("isrunning", "obj", "") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(((com.pixxel.objects.WorldObject) args[0]).getTrigger() != null) {
								return ((com.pixxel.objects.WorldObject) args[0]).getTrigger().process.isRunning();
							} else return false;
						}
					},

					new Command("trigger", "obj", "trigger <obj>") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject obj = (com.pixxel.objects.WorldObject) args[0];
							if (obj.getTrigger() != null) {
								process.log("Executing script from " + obj, true);
								obj.getTrigger().invokeTrigger();
							}
							return null;
						}
					},

					new Command("setscript", "obj string", "setscript <obj> <script>") {
						@Override
						public Object execute(Object[] objects, Process process, Block block) throws Exception {
							return null;
						}
					},

					new Command("command", "string ...", "") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							int page = 0;
							int pageLength = 10; //5 lines
							Command[] commands = process.getLibrary("ScriptCommands").commands;
							if(args.length > 0) {
								if(testForWholeNumber(args[0].toString())) page = Integer.valueOf(args[0].toString());
								else {
									for(Command c : commands) {
										if(c.name.equals(args[0].toString())) {
											process.log("Printing details for: " + args[0].toString() + ":", true);
											StringBuilder arguments = new StringBuilder();
											for (DataType argument : c.arguments) arguments.append("<" + argument.type.typeName + (argument.isArray ? "(array)> " : "> "));
											process.log("  " + c.name + " " + arguments.toString(), true);
											process.log("  USAGE: " + c.description, true);
											return null;
										}
									}
									process.log("Command with name " + args[0].toString() + " not found :/", true);
									return null;
								}
							}
							//10 commands per page
							process.log("", true);
							process.log("---Printing commands, page " + page + " of " + (commands.length / pageLength) + "---", true);
							for(int i = page * pageLength; i < commands.length && i-(page*pageLength) < pageLength; i++) {
								StringBuilder arguments = new StringBuilder();
								for(DataType argument : commands[i].arguments) arguments.append("<" + argument.type.typeName + (argument.isArray ? "(array)> " : "> "));
								process.log("  " + commands[i].name + " " + arguments.toString(), true);
								//process.log("  USAGE: " + commands[i].description, true);
							}
							process.log("---Use command <page> or command <command> for usage--- ", true);

							return null;
						}
					},

					new Command("getmeta", "obj ???", "NOTICE: phrases wrapped in () are keys from the returned dictionary.\n" +
							"getmeta <object> <rotation|pos (x,y)|addr|visibility|group|force (x,y)|light (x,y,r,g,b,a,dist,softdist,rays,xray)|" +
							"") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length < 2) {
								process.log("Valid options are: " + description, true);
								return null;
							}
							com.pixxel.objects.WorldObject object = (com.pixxel.objects.WorldObject) args[0];
							String option = args[1].toString();
							if(option.equals("rotation")) return String.valueOf(object.getRotation());
							else if(option.equals("pos")) {
								Dictionary d = new Dictionary();
								d.addEntry("x", String.valueOf(object.getPosition().x));
								d.addEntry("y", String.valueOf(object.getPosition().y));
								return d;
							} else if(option.equals("addr")) return String.valueOf(object.getAddress());
							else if(option.equals("visibility")) String.valueOf(object.worldObjectValues.visible);
							else if(option.equals("group")) return String.valueOf(object.worldObjectValues.groupIndex);
							else if(option.equals("force")) {
								Dictionary d = new Dictionary();
								d.addEntry("x", String.valueOf(object.getForceX()));
								d.addEntry("y", String.valueOf(object.getForceY()));
								return d;
							} else if(option.equals("light")) {
								if(object.getLight() != null) {
									Dictionary d = new Dictionary();
									d.addEntry("x", String.valueOf(object.getRootValues().values.light.x));
									d.addEntry("y", String.valueOf(object.getRootValues().values.light.y));
									d.addEntry("r", String.valueOf(object.getRootValues().values.light.r));
									d.addEntry("g", String.valueOf(object.getRootValues().values.light.g));
									d.addEntry("b", String.valueOf(object.getRootValues().values.light.b));
									d.addEntry("a", String.valueOf(object.getRootValues().values.light.a));
									d.addEntry("dist", String.valueOf(object.getRootValues().values.light.dist));
									d.addEntry("softdist", String.valueOf(object.getRootValues().values.light.softDist));
									d.addEntry("rays", String.valueOf(object.getRootValues().values.light.rays));
									d.addEntry("xray", String.valueOf(object.getRootValues().values.light.xray));
									return d;
								} else new Dictionary();
							}
							return null;
						}
					},

					new Command("setmeta", "obj ??? ...", "setmeta <object> <rotation <deg>|rotate <deg>|pos <x> <y>|addr <addr>|removeaddr|visible <true|false>|" +
							"group <index>|force <dx> <dy>|light <x> <y> <r> <g> <b> <a> <dist> <softDist> <rays> <xray?>|" +
							"removelight|addfixture/setfixture <rect <width> <height> <x> <y>|circle <radius> <x> <y>> |clearfixtures") {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if (args.length < 2) {
								process.log("Valid options are: " + description, true);
								return null;
							}
							com.pixxel.objects.WorldObject object = (com.pixxel.objects.WorldObject) args[0];
							String option = args[1].toString();

							if (option.equals("rotation") && args.length > 2) {
								if (testForWholeNumber(args[2].toString())) object.setRotation(Integer.valueOf(args[2].toString()));
							} else if (option.equals("rotate") && args.length > 2) {
								if (testForWholeNumber(args[2].toString())) object.setRotation(object.getRotation() + Integer.valueOf(args[2].toString()));
							} else if (option.equals("pos") && args.length > 3) {
								Float newX = getPosition(object.getPosition().x, args[2].toString(), process, block);
								Float newY = getPosition(object.getPosition().y, args[3].toString(), process, block);
								if (newX != null && newY != null) object.setPosition(newX, newY);
							} else if (option.equals("addr") && args.length > 2) {
								object.setAddress(args[2].toString());
							} else if (option.equals("removeaddr")) {
								object.setAddress(com.pixxel.objects.WorldObject.NO_ADDR);
							} else if(option.equals("visible") && args.length > 2) {
								if(args[2].toString().equals("1") || args[2].toString().equals("0")) object.setVisible(args[2].toString().equals("1"));
								else if(args[2].toString().equals("true") || args[2].toString().equals("false")) object.setVisible(args[2].toString().equals("true"));
							} else if(option.equals("group") && args.length > 2) {
								if(testForWholeNumber(args[2].toString())) object.setGroupIndex(Integer.valueOf(args[2].toString()));
							} else if(option.equals("force") && args.length > 3) {
								Float x = Float.valueOf(args[2].toString());
								Float y = Float.valueOf(args[3].toString());
								if(x != null && y != null) object.applyForce(x, y);
							} else if(option.equals("light") && args.length == 12) {
								Float x = Float.valueOf(args[2].toString());
								Float y = Float.valueOf(args[3].toString());
								Float r = Float.valueOf(args[4].toString());
								Float g = Float.valueOf(args[5].toString());
								Float b = Float.valueOf(args[6].toString());
								Float a = Float.valueOf(args[7].toString());
								Float dist = Float.valueOf(args[8].toString());
								Float softDist = Float.valueOf(args[9].toString());
								Integer rays = Integer.valueOf(args[10].toString());
								String xray = args[11].toString();
								if (x != null && y != null && r != null && g != null && b != null && a != null && dist != null && softDist != null && rays != null
										&& (xray.equals("true") || xray.equals("false"))) {
									com.pixxel.objects.RootValues.LightSource newLight = new com.pixxel.objects.RootValues.LightSource();
									newLight.x = x;
									newLight.y = y;
									newLight.r = r;
									newLight.g = g;
									newLight.b = b;
									newLight.a = a;
									newLight.dist = dist;
									newLight.softDist = softDist;
									newLight.rays = rays;
									newLight.xray = xray.equals("true");

									object.getRootValues().values.light = newLight.copy();
									object.updateLight(true);
								}
							} else if(option.equals("light.x")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.x = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.y")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.y = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.r")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.r = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.g")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.g = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.b")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.b = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.a")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.a = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.dist")) {
								if(object.getRootValues().values.light != null && testForFloat(args[2].toString())) {
									object.worldObjectValues.change.light.dist = Float.valueOf(args[2].toString()); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("light.xray")) {
								if(object.getRootValues().values.light != null) {
									object.worldObjectValues.change.light.xray = args[2].toString().equals("true"); //Already live updated
									object.updateLight(false);
								}
							} else if(option.equals("removelight")) {
								object.createLight(null);
							} else if((option.equals("addfixture") || option.equals("setfixture")) && args.length > 3) {
								int bodyType = args[2].toString().equals("static") ? 1 : 0;
								//if(option.equals("setfixture") || object.getRootValues().values.fixtures == null) object.getRootValues().values.fixtures = new ArrayList<>(1);
								if(args[3].toString().equals("circle")) {
									Float radius = Float.valueOf(args[4].toString());
									if(radius != null) {
										com.pixxel.objects.RootValues.Fixture circle = new com.pixxel.objects.RootValues.Fixture();
										circle.isCircle = true;
										circle.width = radius;

										if(args.length >= 7) {
											Float xoff = Float.valueOf(args[5].toString());
											Float yOff = Float.valueOf(args[6].toString());
											if(xoff != null && yOff != null) {
												circle.xOff = xoff;
												circle.yOff = yOff;
											}
										}

										if(object.getWorldValues().change.fixtures == null) object.worldObjectValues.change.fixtures = new ArrayList<>();
										else if(option.equals("setfixture") && object.getRootValues().values.fixtures != null) object.getRootValues().values.fixtures.clear();
										object.worldObjectValues.change.fixtures.add(circle);
										object.worldObjectValues.change.type = bodyType;
										object.updateFixtures();
									}
								} else if(args[3].toString().equals("rect")) {
									Float width = Float.valueOf(args[4].toString());
									Float height = Float.valueOf(args[5].toString());
									if(width != null && height != null) {
										com.pixxel.objects.RootValues.Fixture rect = new RootValues.Fixture();
										rect.width = width;
										rect.height = height;

										if(args.length >= 7) {
											Float xoff = Float.valueOf(args[6].toString());
											Float yOff = Float.valueOf(args[7].toString());
											if(xoff != null && yOff != null) {
												rect.xOff = xoff;
												rect.yOff = yOff;
											}
										}
										if(object.getWorldValues().change.fixtures == null) object.worldObjectValues.change.fixtures = new ArrayList<>();
										else if(option.equals("setfixture") && object.getRootValues().values.fixtures != null) object.getRootValues().values.fixtures.clear();
										object.worldObjectValues.change.fixtures.add(rect);
										object.updateFixtures();
									}
								}
							} else if(option.equals("clearfixtures")) {
								object.clearFixtures();
								if(object.getHitboxBody() != null) object.getWorld().b2dWorld.destroyBody(object.getHitboxBody());
								process.log("Fixtures cleared", true);
							}
							return null;
						}
					},

					new Command("touches", "obj obj", "Returns true, if the first object is inside the hitbox of the second. Useful for scripts", 1) {
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.pixxel.objects.WorldObject arg0 = (com.pixxel.objects.WorldObject) args[0];
							com.pixxel.objects.WorldObject arg1 = (WorldObject) args[1];
							return arg1.touched(arg0.getPosition());
						}
					},

					new Command("shake", "string string boolean", "shake [strength] [duration] [overwriteOld]") {
						@Override
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							com.game.Main.game.stage.shake.shake(Float.valueOf(args[0].toString()), Float.valueOf(args[1].toString()), (Boolean) args[2]);
							return null;
						}
					},

					new Command("mapdata", "string string ...", "<set|get> <scale (float)> <ambient (float)> <loadscript (string)>\n" +
							"Note: you don't need to specify the values after the second argument when the first argument is get") { //you can customize the "viewport" values in the camera command
						@Override
						public Object execute(Object[] args, Process process, Block block) throws Exception {
							if(args.length > 0) {
								if (args[0].toString().equals("get")) {
									if (args[1].toString().equals("scale"))
										return String.valueOf(com.game.Main.game.stage.getWorldData().scl);
									else if (args[1].toString().equals("ambient"))
										return String.valueOf(com.game.Main.game.stage.getWorldData().ambient);
									else if (args[1].toString().equals("loadscript"))
										return com.game.Main.game.stage.getWorldData().loadScript;
								} else if (args[0].toString().equals("set") && args.length >= 3) {
									if (args[1].toString().equals("scale") && testForFloat(args[2].toString()))
										com.game.Main.game.stage.getWorldData().scl = Float.valueOf(args[2].toString());
									else if (args[1].toString().equals("ambient") && testForFloat(args[2].toString()))
										com.game.Main.game.stage.setAmbientLight(Float.valueOf(args[2].toString()));
									else if (args[1].toString().equals("loadscript"))
										Main.game.stage.getWorldData().loadScript = args[2].toString();
								} else
									process.kill(block, "Error valid syntax is:" + this.description);
							} else process.kill(block, "Error valid syntax is:" + this.description);
							return null;
						}
					}
			};
		}

		public Float getPosition(float relativePos, String pos, Process process, Block block) {
			float xpos = 0;
			if(pos.startsWith("~")) {
				if(pos.length() > 1) {
					pos = pos.substring(1);
					if(ApplicationBuilder.testForFloat(pos)) xpos = relativePos + Float.valueOf(pos);
					else {
						process.kill(block, "Syntax error at " + pos);
						return null;
					}
				} else xpos = relativePos;
			} else {
				if(ApplicationBuilder.testForFloat(pos)) xpos = Float.valueOf(pos);
				else {
					process.kill(block, "Syntax error at " + pos);
					return null;
				}
			}
			return xpos;
		}

		public static Library getLibrary() {
			Library lib = new GenerateCommands();
			return lib;
		}
	}
}
