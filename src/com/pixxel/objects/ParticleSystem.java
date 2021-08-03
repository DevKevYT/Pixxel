package com.pixxel.objects;


import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;

/**A particle system to handle all particles
 * @see Particle*/
public class ParticleSystem implements Disposable {

    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<Particle> removeQueue = new ArrayList<>();
    private World world;
    private int maxParticles = Integer.MAX_VALUE;

    public ParticleSystem(World world) {
        this.world = world;
    }

    public void addParticle(Particle particle) {
        if(particle.isDead() || particles.size()+1 > maxParticles || particle.expired()) return;
        particle.system = this;
        particle.construct();
        particles.add(particle);
    }

    /**This function needs to be called between the worlds spritebaches begin() and end() methods*/
    public void updateAndDraw(float delta) {
        for(Particle p : particles) {
            if(!p.isDead()) {
                p.aliveTime += delta;
                if(p.expired()) {
                    removeQueue.add(p);
                    continue;
                }

                p.step(delta);
                p.draw(world.batch);
            }
        }

        while(!removeQueue.isEmpty()) {
            removeQueue.remove(0);
        }
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void dispose() {
        particles.clear();
        removeQueue.clear();
    }
}
