package com.pixxel.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/***
 * Use this class to create custom behaving objects like player, enemies, projectiles etc.
 * which would not be possible with the script.
 *
 * This Class also provides the ability to save variables across saves and restarts.
 * Every variable declared as either:
 * <ul>
 *     <li>Public</li>
 *     <li>Integer</li>
 *     <li>Float</li>
 *     <li>String</li>
 *     <li>Double</li>
 *     <li>Boolean</li>
 * </ul>
 * will get saved in the proper world file the object is added in and loaded, if the world gets loaded.
 * If you don't want to get a variable saved, use a complex datatype, make the variable private
 * or let the variable name start with a "_". Example:<br><br>
 * <code>public int test = 0;</code> -> Will get saved and loaded if modified<br>
 * <code>public int _test = 0;</code> -> Won't get saved. Starts with a "_"<br>
 * <code>private int test = 0;</code> -> Won't get saved. Variable private<br>
 * <code>public StringBuilder string = new StringBuilder("test");</code> -> Won't get saved. Complex datatype<br><br>

 * Overwritten Methods:<br>
 * <ul>
 *<li>{@link Behavior#onCreate(WorldObject)}</li>
 *<li>{@link Behavior#onSave()}</li>
 *<li>{@link Behavior#postCreate()}</li>
 *<li>{@link Behavior#onUpdate(World, WorldObject, float)}</li>
 *<li>{@link Behavior#onRemove()}</li>
 *</ul>
 * Optional Methods to overwrite<br>
 * <ul>
 *<li>{@link Behavior#drawBehind(WorldObject, SpriteBatch)}</li>
 *<li>{@link Behavior#drawOver(WorldObject, SpriteBatch)}</li>
 * </ul>
 */
public abstract class Behavior {

    public static final ArrayList<Class<?>> BEHAVIOR_CLASSES = new ArrayList<>();
    public volatile boolean doPostCreate = true;
    public HashData data;
    final com.pixxel.objects.WorldValues.BehaviorValues values;

    public WorldObject parent; //Null, until behavior gets added to the world
    public World world;
    protected boolean added;
    Class<?> subClass;

    public Behavior(WorldValues.BehaviorValues values, Class<?> subClass) {
        added = false;
        parent = null;
        this.values = values;
        data = new HashData(values.persistent);
        this.subClass = subClass;
    }

    /**Fired when the object is added to the world*/
    protected final void addToWorld(World world, WorldObject parent) {
        if(added) return;

        this.parent = parent;
        this.world = world;
        loadVariables(true);
        onCreate(this.parent);
        if(!doPostCreate) added = true;
    }

    /**Needs to be called in a Gdx.postRunnable thread*/
    protected final void doPostCreate() {
        postCreate();
        added = true;
    }

    public static void findBehaviors() {
        Reflections reflections = new Reflections("com.pixxel");
        Set<Class<? extends Behavior>> classes = reflections.getSubTypesOf(Behavior.class);
        BEHAVIOR_CLASSES.addAll(classes);
    }

    /**Converts the public and valid variables into hashdata to save declared variables.<br>
     * <br>To get a variable saved, it needs to be <b>public</b> and native (except String): String, Integer, Float, Boolean.
     * <br>This function gets automatically called when: saveVars is true and behavior {@link Behavior#isEnabled()}
     * Of course you can call is whenever you want.<br>It is recomendet, that you use this function manually, if the behavior is disabled and call it in {@link Behavior#onSave()}*/
    public void saveBehaviorVariables() {
        try {
            HashMap vars = getBehaviorVariables(this);
            for(Object s : vars.keySet()) data.set(s.toString(), vars.get(s));
        } catch (IllegalAccessException e) {
            Gdx.app.log("Tag", "Failed to save Variables " + e.getLocalizedMessage());
        }
    }

    /**Loads the variables from the world.*/
    public void loadVariables(boolean eraseNonExisting) {
        HashMap current = null;
        try {
            current = getBehaviorVariables(this);
        } catch (IllegalAccessException e) {
            data.clear(); //Dont panic but PANIC!!! Self destroy everything to avoid memory leaks
            saveBehaviorVariables();
            Gdx.app.log("Behavior", "Unable to load Variables! " + e.getLocalizedMessage());
            return;
        }
        if(current == null) return;

        for(int i = 0; i < data.getFields().size(); i++) {
            com.pixxel.objects.RootValues.Variable v = data.getFields().get(i);

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
                if(v.T == com.pixxel.objects.RootValues.Type.STR) setVariable(this, v.N, v.S);
                else if(v.T == com.pixxel.objects.RootValues.Type.INT) setVariable(this, v.N, v.I);
                else if(v.T == com.pixxel.objects.RootValues.Type.FLOAT) setVariable(this, v.N, v.F);
                else if(v.T == RootValues.Type.BOOL) setVariable(this, v.N, v.B);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**@return A hashmap of all behavior variables which are able to get saved from the specific object.
     * @see Behavior
     * @throws IllegalAccessException If something somehow went wromg ...*/
    public static final <T> HashMap getBehaviorVariables(Object object) throws IllegalAccessException {
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

    /**@return A specific variable with the given name.*/
    public static Object getVariable(Object fromObject, String name) {
        if(fromObject == null || name.isEmpty()) return null;
        try {
            return fromObject.getClass().getField(name).get(fromObject);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**@return All valid fields of the behavior class. Not as hashmap like in {@link Behavior#getBehaviorVariables(Object)} instead as Fields.
     * @see Behavior#getBehaviorVariables(Object) */
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

    /**Sets a variable in you behavior class, if accessible*/
    public static final void setVariable(Object object,  String varName, Object value) throws IllegalAccessException, NoSuchFieldException {
        HashMap valid = getBehaviorVariables(object);
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

    /**Returns all methods the super class has*/
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

    /**@return The classpath of the behavior*/
    public String getClassPath() {
        return values.classPath;
    }

    /**The Id of the behavior. An object may have multiple behaviors, so all behaviors need to have
     * a unique id.
     * @return The ID of this specific behavior*/
    public final String getID() {
        return values.id;
    }

    /**Sets the ID. Keep in mind that all behavior ID's of a single object need to be unique*/
    public final void setID(String id) {
        values.id = id;
    }

    /**If disabled, the onUpdate function is not called after each frame
     *@return If the behavior is enabled or not*/
    public boolean isEnabled() {
        return values.enabled;
    }

    /**Enables or disables an object
     * @see Behavior#isEnabled() */
    public void setEnabled(boolean enabled) {
        values.enabled = enabled;
    }

    /**Called, when a world gets saved. Useful for saving some custom data on your object
     * (The class variables are being saved automatically, so you don't have to worry
     * @see Behavior*/
    public abstract void onSave();

    /**Called, when the behavior object gets successfully added to an object and being added to the world. (Called only once)*/
    public abstract  void onCreate(WorldObject object);

    /**This function is always called in the Gdx.postRunnable, to ensure this function
     * can safely be used to load textures etc.
     * Update functions etc. are only called, when this function is done.
     * You can disable this function by setting {@link Behavior#doPostCreate} to false in the constructor*/
    public abstract void postCreate();

    /**Put here, whatever you like / the entity should do/move.
     * <br>Called every frame, when the object gets updated*/
    public abstract void onUpdate(World world, WorldObject object, float deltaTime); //Do whatever the behavior should do

    /**Called, when the object gets removed / destroyed from a world*/
    public abstract void onRemove();

    /**Called before object drawing. Feel free to draw other textures or modify the Spritebatch!*/
    public abstract void drawOver(WorldObject object, SpriteBatch batch);

    /**Called after object got rendered*/
    public abstract void drawBehind(WorldObject object, SpriteBatch batch);
}
