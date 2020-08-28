package com.mygdx.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.darkdawn.Logger;

import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

/**Makes custom object updating of objects possible (Only possible within source code with abstract methods)<br>
 * Abstract functions are only called, when the object is/was added to the specified world.
 * You can also overwrite the drawOver() and drawBehind() function<br>
 * It is also possible to change variables of a {@link Behavior} object with {@link Behavior#setVariable(Object, String, Object)}
 * and {@link Behavior#getBehaviorVariables(Object)}, but variables in the class need to be public and primitive except
 * of String. These functions also only support int, float, double, String and boolean.*/
public abstract class Behavior {

    public static final ArrayList<Class<?>> BEHAVIOR_CLASSES = new ArrayList<>();
    public volatile boolean doPostCreate = true;
    public HashData data;
    final WorldValues.BehaviorValues values;

    public WorldObject parent; //Null, until behavior gets added to the world
    public World world;
    protected boolean added;
    Class<?> subClass;

    static {
        //Find all behaviors

    }

    //Variables you don't want to save should be declared private, protected, or none, be non primitive (Integer i = 0) or start with a underscore e.g. public int _noSave = 0;
    public Behavior(WorldValues.BehaviorValues values, Class<?> subClass) {
        added = false;
        parent = null;
        this.values = values;
        data = new HashData(values.persistent);
        this.subClass = subClass;

        for(Class<?> c : BEHAVIOR_CLASSES) {
            if(c.equals(subClass)) return;
        }
        BEHAVIOR_CLASSES.add(subClass);
    }

    protected final void addToWorld(World world, WorldObject parent) {
        if(added) return;

        this.parent = parent;
        this.world = world;
        loadVariables(true);
        onCreate(this.parent);
        if(!doPostCreate) added = true;
    }

    /**Call this funtion in Gdx.app.postRunnable*/
    protected final void doPostCreate() {
        postCreate();
        added = true;
    }

    /**Converts the public and valid variables into hashdata to save declared variables.<br>
     * <br>To get a variable saved, it needs to be <b>public</b> and native (except String): String, Integer, Float, Boolean.
     * <br>This function gets automatically called when: saveVars is true and behavior {@link Behavior#isEnabled()}
     * Of course you can call is whenever you want.<br>It is recomendet, that you use this function manually, if the behavior is disabled and call it in {@link Behavior#onSave()}*/
    public void saveBehaviorVariables() {
        try {  //This saves all public variables
            HashMap vars = getBehaviorVariables(this, false);
            for(Object s : vars.keySet()) data.set(s.toString(), vars.get(s));
        } catch (IllegalAccessException e) {
            Gdx.app.log("Tag", "Failed to save Variables " + e.getLocalizedMessage());
        }
    }

    public void loadVariables(boolean eraseNonExisting) {
        HashMap current = null;
        try {
            current = getBehaviorVariables(this, true);
        } catch (IllegalAccessException e) {
            data.clear(); //Dont panic but PANIC!!! Self destroy everything to avoid memory leaks
            saveBehaviorVariables();
            Gdx.app.log("Behavior", "Unable to load Variables! " + e.getLocalizedMessage());
            return;
        }
        if(current == null) return;

        for(int i = 0; i < data.getFields().size(); i++) {
            RootValues.Variable v = data.getFields().get(i);

            boolean found = false;
            for (Object name : current.keySet()) {
                if (name.toString().equals(v.N)) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                data.remove(v.N);
                i--;
                Gdx.app.log("Behavior " + parent.getID() + "/" + getID(), "Removing unused variable " + v.N);
                continue;
            }

            try {
                if(v.T == RootValues.Type.STR) setVariable(this, v.N, v.S);
                else if(v.T == RootValues.Type.INT) setVariable(this, v.N, v.I);
                else if(v.T == RootValues.Type.FLOAT) setVariable(this, v.N, v.F);
                else if(v.T == RootValues.Type.BOOL) setVariable(this, v.N, v.B); //Typo... embarassing hah :D
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**Returns a hashmap with all public (!) variables of the given
     * Object that extends {@link Behavior}*/
    public static final <T> HashMap getBehaviorVariables(Object object, boolean getDefault) throws IllegalAccessException {
        if(!(object instanceof Behavior)) return null;
        HashMap vars = new HashMap();
        for(Field f : object.getClass().getDeclaredFields()) {
            Class type = f.getType();
            if(!type.isPrimitive() && !type.toString().equals("class java.lang.String")) continue;
            if(!Modifier.isPublic(f.getModifiers())) continue;
            if(f.getName().startsWith("_")) continue;

            if(type.toString().equals("float")) {
                vars.put(f.getName(), f.getFloat(object));
            } else if(type.toString().equals("int")) {
                vars.put(f.getName(), f.getInt(object));
            } else if(type.toString().equals("boolean")) {
                vars.put(f.getName(), f.getBoolean(object));
            } else vars.put(f.getName(), f.get(object));
        }
        return vars;
    }

    public static Object getVariable(Object fromObject, String name) {
        if(fromObject == null || name.isEmpty()) return null;
        try {
            return fromObject.getClass().getField(name).get(fromObject);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**Returns all valid fields of the behavior class (@see {@link Behavior#getBehaviorVariables(Object)}*/
    public static Field[] getFields(Object object) {
        ArrayList<Field> fieldArray = new ArrayList<>(1);

        for(Field f : object.getClass().getFields()) {
            Class type = f.getType();
            if(!type.isPrimitive() && !type.toString().equals("class java.lang.String")) continue;
            if(!Modifier.isPublic(f.getModifiers())) continue;
            if(f.getName().startsWith("_")) continue;

            fieldArray.add(f);
        }
        return fieldArray.toArray(new Field[fieldArray.size()]);
    }

    /***/
    public static final void setVariable(Object object,  String varName, Object value) throws IllegalAccessException, NoSuchFieldException {
        HashMap valid = getBehaviorVariables(object, false);
        if (valid == null) return;
        if (valid.isEmpty()) return;

        boolean found = false;
        for (Object name : valid.keySet()) {
            if (name.toString().equals(varName)) {
                found = true;
                break;
            }
        }

        if (!found) throw new IllegalAccessException("Variable not accessible or does not exist: " + varName);
        Field field = object.getClass().getField(varName);
        if (field.getType().getTypeName().equals("float")) {
            field.setFloat(object, (Float) value);
        } else if(field.getType().getTypeName().equals("int")) {
            field.setInt(object, (Integer) value);
        } else if(field.getType().getTypeName().equals("double")) {
            field.setDouble(object, (Double) value);
        } else if(field.getType().getTypeName().equals("boolean")) {
            field.setBoolean(object, (Boolean) value);
        } else field.set(object, value);
    }

    /**Returns all */
    public Method[] getSubclassMethods() {
        if(subClass != null) {
            for(Method m : subClass.asSubclass(subClass).getMethods()) {
                for(Annotation t : m.getAnnotations()) System.out.println(t.annotationType().getSimpleName());
            }
            return subClass.getMethods();
        }
        else return new Method[]{};
    }

    private static Field getFieldByName(String name, Class clazz) throws NoSuchFieldException {
        return clazz.getField(name);
    }

    public String getClassPath() {
        return values.classPath;
    }

    public String getID() {
        return values.id;
    }

    public void setID(String id) {
        values.id = id;
    }

    public boolean isEnabled() {
        return values.enabled;
    }

    public void setEnabled(boolean enabled) {
        values.enabled = enabled;
    }

    public abstract void onSave();

    /**Called, when the behavior object gets successfully added to an object. (Called only once)
     * Called, event if the behavior is disabled*/
    public abstract  void onCreate(WorldObject object);

    /**This function is always called in the Gdx.postRunnable, to ensure this function
     * can safely be used to load textures etc.
     * Update functions etc. are only called, when this function is done.
     * You can disable this function by setting "doPostCreate" to false in the constructor*/
    public abstract void postCreate();

    /**Put here, whatever you like / the enity should do/move.
     * <br>Called every frame, when the object gets updated*/
    public abstract void onUpdate(World world, WorldObject object, float deltaTime); //Do whatever the behavior should do

    /**Called, when the object gets removed*/
    public abstract void onRemove();

    /**Used to get overwritten. Called before object drawing Feel free to draw other textures or modify the Spritebatch!*/
    public abstract void drawOver(WorldObject object, SpriteBatch batch);

    /**Used to get overwritten. Called after object got rendered*/
    public abstract void drawBehind(WorldObject object, SpriteBatch batch);
}
