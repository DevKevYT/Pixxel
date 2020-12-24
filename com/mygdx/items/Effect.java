package com.mygdx.items;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class Effect {

    private EffectData root;
    private float time = 0;
    private float duration = 0;
    public float attr;
    public int stacked = 1;

    public Effect(EffectData root, float duration, float attribute) {
        this.root = root;
        this.attr = attribute;
        this.duration = duration;
        time = duration;
    }

    /**Add another effect on top, if canStack is true, otherwise do nothing*/
    public boolean stack(Effect effect, boolean stackTime, boolean stackAttribute) {
        if(!getData().canStack) return false;
        else {
            stacked++;
            if(stackTime) {
                duration += effect.duration;
                time += effect.time;
            }
            if(stackAttribute) attr += effect.attr;
        }
        return true;
    }

    /**For example the effect effectivity in percent*/
    public void putAttribute(float attr) {
        this.attr = attr;
    }

    public void update(float delta) {
        time -= delta;
    }

    public void addTime(float time) {
        this.time += time;
    }

    public float getTime() {
        return time;
    }

    public void reset() {
        time = 0;
    }

    public float getDuration() {
        return duration;
    }

    public EffectData getData() {
        return root;
    }
}
