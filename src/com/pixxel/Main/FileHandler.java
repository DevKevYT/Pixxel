package com.pixxel.Main;

import java.io.IOException;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class FileHandler {
	public final static Json jsonHandler = new Json();
	public static final String ABSOLUTE = "*";

	public FileHandler() {
	}
	
	/**Writes an object as json to the specified file. <br>Does nothing, when the object is null.*/
	public static void writeJSON(FileHandle file, Object objectToSave, boolean prettyPrint) throws IOException {
		if(!file.exists()) {
			try {
				file.file().createNewFile();
			} catch (IOException e) {
				throw e;
			}
		}
		if(objectToSave == null) return;
		
		if(prettyPrint) file.writeString(jsonHandler.prettyPrint(objectToSave), false);
		else file.writeString(jsonHandler.toJson(objectToSave), false);
	}
	
	public static <T> T readJSON(FileHandle file, Class<T> type) {
		if(!file.exists()) return null;
		return jsonHandler.fromJson(type, file.read());
	}
}
