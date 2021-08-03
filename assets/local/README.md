# The Game File
This file is a simple json.<br>
Example:
```json
{
  "custom": {
	  "currentWorld": "demo.json"
  },
  "worlds": [
	  "/local/worlds/demo.json"
  ],
  "assets": [
        "local/Demo/Animated/torch-lit.json",
  	"local/Demo/Atlas/",
  	"local/Demo/Simple/",
  	"local/Demo/Tileset/"
  ]
}

```
## Default Properties
### custom 
This object is used to hold custom data of your game like player inventory etc.
The following java functions can be used to add and remove custom objects.<br>
Those can be found int the Game.java class.
```java
game.setCustomData("name", object, Object.class);
game.getCustomData("name", Object.class);
game.removeCustomData("name");
```
Additionally, this property holds a fixed value: currentWorld.
This value holds the file name of the world the game should load at startup.
___
### worlds
Array of world file locations.<br>
Optionally, you can define a directory to load all files inside it.
___
### assets
Array of object file locations. (Props inside your game world not java objects)<br>
Optionally, you can define a directory to load all files inside it.<br>
Not handled by the engine. You need to add paths automatically.
___
### hashes
Added automatically.<br>
You can imagine those as global variables for your script to use variables across world loading.<br>
To set, use and modify hashes, use the hash command.<br>
For more info use
```command hash```
