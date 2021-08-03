package com.pixxel.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.pixxel.animation.Movie;
import com.pixxel.animation.SpriteAnimation;
import com.pixxel.objects.Particle;

public class HitParticle extends Particle<Object> {

    public final com.pixxel.animation.SpriteAnimation animation;
    public final com.pixxel.animation.Movie particle;

  //  static {
  //      TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures//FX//entity-hit.atlas"));
  //      particle = new Movie(2, atlas, "hit1", "hit2", "hit3", "hit4");
  //  }

    public HitParticle(float startX, float startY, float maxAliveTime, Object pool) {
        super(startX, startY, maxAliveTime, pool);
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures//FX//entity-hit.atlas"));
        particle = new Movie(15, atlas, "hit1", "hit2", "hit3", "hit4");
        animation = new SpriteAnimation();
        animation.addMovie("particle", particle);
        animation.play("particle");
    }

    @Override
    public void step(float delta) {
       animation.update(Gdx.graphics.getFramesPerSecond());
       if(animation.finished("particle")) destroy();
    }

    @Override
    public void construct() {

    }

    @Override
    public void draw(Batch batch) {
        if(isDead()) return;
        if(animation.getCurrentFrame() != null) {
            batch.setColor(1, 1, 1, 0.8f);
            batch.draw(animation.getCurrentFrame(), getX() * system.getWorld().getScale() - super.system.getWorld().getTileSizePX() / 2f,
                    getY() * system.getWorld().getScale() - super.system.getWorld().getTileSizePX() / 2f,
                    super.system.getWorld().getTileSizePX(),
                    super.system.getWorld().getTileSizePX());
            batch.setColor(1, 1, 1, 1);
        }
    }
}
