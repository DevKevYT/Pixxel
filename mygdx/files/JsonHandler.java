package com.mygdx.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

public class JsonHandler {
	private Json jsonHandler = new Json();
	
	public JsonHandler() {
		jsonHandler.setOutputType(OutputType.javascript);
	}
	
	public Json getJsonhandler() {
		return jsonHandler;
	}
	
	public void writeJSON(String path, Object objectToSave, boolean prettyPrint) {
		File f = new File(path);
		try {
			if(!f.exists()) f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			
			if(prettyPrint) writer.write(jsonHandler.prettyPrint(objectToSave));
			else writer.write(jsonHandler.toJson(objectToSave));
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeJSON(FileHandle file, Object objectToSave, boolean prettyPrint) {
		try {
			if(!file.exists()) file.file().createNewFile();
			if(prettyPrint) file.writeString(jsonHandler.prettyPrint(objectToSave), false);
			else file.writeString(jsonHandler.toJson(objectToSave), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public <T> T readJSON(String path, Class<T> classs) {
		File f = new File(path);
			try {
				if(!f.exists()) throw new FileNotFoundException("File to read JSON from not found: " + path);
				BufferedReader reader = new BufferedReader(new FileReader(f));
				T o = jsonHandler.fromJson(classs, reader);
				if(o == null) throw new Exception("Failed to read JSON file!");
				return o;
			} catch (Exception e) {
				e.printStackTrace();
			} 
			return null;
	}
	
	public <T> T readJSON(FileHandle fileHandle, Class<T> classs) {
			try {
				if(!fileHandle.exists()) throw new FileNotFoundException("File to read JSON from not found: " + fileHandle.path());
				T o = jsonHandler.fromJson(classs, fileHandle.read());
				if(o == null) throw new Exception("Failed to read JSON file!");
				return o;
			} catch (Exception e) {
				e.printStackTrace();
			} 
			return null;
	}
}
