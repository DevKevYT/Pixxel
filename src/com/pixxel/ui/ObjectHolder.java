package com.pixxel.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;

/**Simple actor class just to hold an object*/
public class ObjectHolder<T> extends Actor {

    private T type;

    public ObjectHolder(T type) {
        this.type = type;
    }

    public ObjectHolder() {}

    public T getObject() {
        return type;
    }

    public void setObject(T type) {
        this.type = type;
    }
}
