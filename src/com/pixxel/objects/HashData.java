package com.pixxel.objects;

import java.util.ArrayList;

/**Class for storing behavior variables etc.
 * Handled by the engine*/
public class HashData {

    private ArrayList<com.pixxel.objects.RootValues.Variable> fields;

    public HashData(ArrayList<com.pixxel.objects.RootValues.Variable> variables) {
        this.fields = variables;
    }

    public HashData() {
        fields = new ArrayList<>();
    }

    public ArrayList<com.pixxel.objects.RootValues.Variable> getFields() {
        return fields;
    }

    public final com.pixxel.objects.RootValues.Variable getVar(String key) {
        for(int i = 0; i < fields.size(); i++) {
            if(fields.get(i).N.equals(key)) return fields.get(i);
        }
        return null;
    }

    public boolean testForString(Object obj) {
        return obj.getClass().getTypeName().equals(String.class.getTypeName());
    }

    public boolean testForInt(Object obj) {
        return obj.getClass().getTypeName().equals(Integer.class.getTypeName());
    }

    public boolean testForFloat(Object obj) {
        return obj.getClass().getTypeName().equals(Float.class.getTypeName());
    }

    public boolean testFoBoolean(Object obj) {
        return obj.getClass().getTypeName().equals(Boolean.class.getTypeName());
    }

    public void clear() {
        fields.clear();
    }

    /**@return true, if a field with the name was found*/
    public boolean remove(String name) {
        for(com.pixxel.objects.RootValues.Variable v : fields) {
            if(v.N.equals(name)) {
                fields.remove(v);
                return true;
            }
        }
        return false;
    }

    /**Valid objecs are: Integer, Float, String and Boolean*/
    public final void set(String name, Object object) {
        if(testForString(object)) setString(name, object.toString());
        else if(testForInt(object)) setInt(name, (Integer) object);
        else if(testForFloat(object)) setFloat(name, (Float) object);
        else if(testFoBoolean(object)) setBoolean(name, (Boolean) object);
    }

    public final void setString(String name, String value) {
        com.pixxel.objects.RootValues.Variable c = getVar(name);
        if(c == null) {
            com.pixxel.objects.RootValues.Variable var = new com.pixxel.objects.RootValues.Variable();
            var.S = value;
            var.T = com.pixxel.objects.RootValues.Type.STR;
            var.N = name;
            fields.add(var);
        } else if(c.T == com.pixxel.objects.RootValues.Type.STR) c.S = value;
    }

    public final void setInt(String name, int value) {
        com.pixxel.objects.RootValues.Variable c = getVar(name);
        if(c == null) {
            com.pixxel.objects.RootValues.Variable var = new com.pixxel.objects.RootValues.Variable();
            var.I = value;
            var.T = com.pixxel.objects.RootValues.Type.INT;
            var.N = name;
            fields.add(var);
        } else if(c.T == com.pixxel.objects.RootValues.Type.INT) c.I = value;
    }

    public final void setFloat(String name, float value) {
        com.pixxel.objects.RootValues.Variable c = getVar(name);
        if(c == null) {
            com.pixxel.objects.RootValues.Variable var = new com.pixxel.objects.RootValues.Variable();
            var.F = value;
            var.T = com.pixxel.objects.RootValues.Type.FLOAT;
            var.N = name;
            fields.add(var);
        } else if(c.T == com.pixxel.objects.RootValues.Type.FLOAT) c.F = value;
    }

    public final void setBoolean(String name, boolean value) {
        com.pixxel.objects.RootValues.Variable c = getVar(name);
        if(c == null) {
            com.pixxel.objects.RootValues.Variable var = new com.pixxel.objects.RootValues.Variable();
            var.B = value;
            var.T = com.pixxel.objects.RootValues.Type.BOOL;
            var.N = name;
            fields.add(var);
        } else if(c.T == com.pixxel.objects.RootValues.Type.BOOL) c.B = value;
    }

    public final String getString(String name) {
        com.pixxel.objects.RootValues.Variable v = getVar(name);
        if(v != null) return v.S;
        return null;
    }

    public final Integer getInt(String name) {
        com.pixxel.objects.RootValues.Variable v = getVar(name);
        if(v != null) return v.I;
        return null;
    }

    public final Float getFloat(String name) {
        com.pixxel.objects.RootValues.Variable v = getVar(name);
        if(v != null) return v.F;
        return null;
    }

    public final Boolean getBoolean(String name) {
        com.pixxel.objects.RootValues.Variable v = getVar(name);
        if(v != null) return v.B;
        return null;
    }

    /**Returns the value as a String*/
    public final String get(String name) {
        com.pixxel.objects.RootValues.Variable v = getVar(name);
        if(v == null) return null;
        else if(v.T == com.pixxel.objects.RootValues.Type.STR) return v.S;
        else if(v.T == com.pixxel.objects.RootValues.Type.INT) return String.valueOf(v.I);
        else if(v.T == com.pixxel.objects.RootValues.Type.FLOAT) return String.valueOf(v.F);
        else if(v.T == RootValues.Type.BOOL) return String.valueOf(v.B);

        return null;
    }
}
