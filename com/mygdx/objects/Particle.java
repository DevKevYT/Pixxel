package com.mygdx.objects;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

/**A particle that can get mounted to a ParticleSystem*/
public abstract class Particle <T> {

    protected float aliveTime = 0;
    protected final float maxAliveTime; //Time in seconds
    private boolean dead = false;
    public ParticleSystem system;

    T pool;
    private Vector2 position = new Vector2();

    public Particle(float startX, float startY, float maxAliveTime, T pool) {
        position.x = startX;
        position.y = startY;
        this.maxAliveTime = maxAliveTime;
        this.pool = pool;
    }

    public void destroy() {
        system.removeQueue.add(this);
        dead = true;
    }

    public T getPool() {
        return pool;
    }

    public abstract void step(float delta);

    public abstract void construct();

    /**Batch is usually the world camera projection*/
    public abstract void draw(Batch batch);

    public boolean isDead() {
        return dead;
    }

    /**True, when aliveTime > maxAliveTime*/
    public boolean expired() {
        return aliveTime > maxAliveTime;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public void setX(float x) {
        this.position.x = x;
    }

    public void setY(float y) {
        this.position.y = y;
    }
}
