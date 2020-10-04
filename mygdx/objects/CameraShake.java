package com.mygdx.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraShake {

    private float currentTime = 0;  //Time and current time in seconds
    private float duration = 0;
    private float strength = 0;
    private World world;
    public Vector3 camPos = new Vector3();

    public CameraShake(World world) {
        this.world = world;
    }

    /**Shaking effect can get stacked by calling it multiple times with cancelOld being false*/
    public void shake(float strength, float duration, boolean cancelOld) {
        if(cancelOld) cancelShaking();
        this.strength += strength*world.getScale();
        this.duration += duration;
    }

    public void tick(float delta) {
        if(currentTime >= duration) {
            cancelShaking();
            return;
        }

        float currentStrength = strength * ((duration - currentTime) / duration);

        if(Gdx.graphics.getFrameId() % ((int) (Gdx.graphics.getFramesPerSecond() / 30f)+1) == 0) {
            camPos.x = MathUtils.random(-1f, 1f) * currentStrength;
            camPos.y = MathUtils.random(-1f, 1f) * currentStrength;
        }
        world.getViewport().translate(camPos);

        currentTime += delta;
    }

    public void resetDuration(float duration) {
        this.duration = duration;
        if(duration >= currentTime) cancelShaking();
    }

    public float getDuration() {
        return duration;
    }

    public void resetTimer(float currentTime) {
        this.currentTime = currentTime;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void cancelShaking() {
        currentTime = 0;
        duration = 0;
        strength = 0;
    }
}
