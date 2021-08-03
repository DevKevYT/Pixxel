This branch only contains the source code as well as the gradle script files and assets with some readme's<br>
to help you getting an overview before starting.<br>
To start a project using the existing code refer to the release tags on the right.

# Pixxel
LibGDX engine for 2d games.<br>
This engine provides some basic game handling features to get you startet on your project.<br>
Please not that this is not a full AAA type of Engine like Unity to create games within minutes.<br>

## Features
This engine features
 - [x] World rendering
 - [x] Real time level editor
 - [x] Built in script to bring your worlds to life
 - [x] Asset handling
 - [x] Box2d and Box2d lights physics
 - [x] Basic pathfinding AI

## Getting started
To get started with your project, please refer to my video tutorial [here](coming soon)

- Download the latest release
- Open the folder in Android Studio
- Sync the project

With this method, you still have full control over the whole code.<br>
It is <i>your</i> project, right?

This project requires the following versions:
- Java source and target compatibillity: 1.8 (build.gradle)
- gdxVersion 1.9.9 (build.gradle)

# File structure and how to get along
Every file that is needed for your game is located in */android/assets/<br>

## Game File
The game.json file is the most important file of your, well, <i>game</i>.<br>
It holds information where your assets and objects are located in your file system.
It also manages some saved data of your player like inventory etc.<br><br>
Take a look [here](../pixxel-doc/assets/local/README.md) for more information about the game file.

## World Files
Most likely your game won't just consist of one single world, right?<br>
You can create world files wherever you like (The game.json file will keet track of your worlds),<br>
but using the default path /assets/local/worlds/ is a good choice.

## Object Files
Every object in your world consists of a simple json file.
Again, you can store your objects wherever you like, but remember to put the relative path in your game.json file.<br>

Another program to create and edit objects without creating a file manually is in progress but not finished.<br>
So you need to create objects manually.<br>
Explanations and examples can be found [here](../pixxel-doc/assets/local/Demo/)

## Script syntax and usage
As mentioned above, this engine also features a script.<br>
This script is very useful for some basic level behaviors like
- Opening doors ...
- Lighting torches ...
- Entering houses ...
- Teleporting the player ...
- Etc ...

So this script is meant to do some basic level behavior,<br>
so you dont have to write Java behavior classes for everything.

You can watch a tutorial on how to use this script [here](coming soon ...)<br>
or read the syntax and download a test program [here](https://github.com/DevKevYT/devscript)
