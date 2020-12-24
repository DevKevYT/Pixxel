package com.mygdx.items;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class EffectData {
    public final String id;
    public final boolean canStack;
    public Drawable icon;

    public EffectData(String id, boolean canStack) {
        this.id = id;
        this.canStack = canStack;
    }
}
