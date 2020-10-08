package com.mygdx.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.objects.Particle;

public class HitmarkerParticle extends Particle<HitmarkerParticlePool> {

    private static BitmapFont hitmarkerFont;
    private static BitmapFont crithitmarkerFont;
    float xSpeed;
    float ySpeed;
    float gravity;
    float fontSize = 0;
    static {
       // FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("UI//fonts//RetroGaming.ttf"));
        //FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        //param.borderColor = Color.DARK_GRAY;
       // param.borderWidth = 2;
       // param.color = Color.WHITE;
       // param.size = 24;
      hitmarkerFont = new BitmapFont(Gdx.files.internal("UI//fonts//pixxel.fnt"));
      crithitmarkerFont = new BitmapFont(Gdx.files.internal("UI//fonts//pixxel.fnt"));
      crithitmarkerFont.setColor(Color.GOLDENROD);
    }

    public HitmarkerParticle(float startX, float startY, HitmarkerParticlePool pool) {
        super(startX, startY, 0.5f, pool);
        xSpeed = MathUtils.random(-40, 40);
        xSpeed += (xSpeed < 0 ? -10 : 10);
        ySpeed = MathUtils.random(150) + 80;
        gravity = ySpeed; //1 second to overcome yspeed
    }

    @Override
    public void step(float delta) {
        setX(getX() + xSpeed*delta);
        ySpeed -= gravity*delta*2; //should do a whole arc nice
        setY(getY() + ySpeed*delta);
        if(fontSize < 20) fontSize += delta*61*2;
    }

    @Override
    public void construct() {
    }

    @Override
    public void draw(Batch batch) {
        float size = fontSize;
        if(getPool().crit) {
            super.system.getWorld().game.drawWorldText(batch, crithitmarkerFont, getX()-size*.5f, getY()-size*.5f, size, getPool().value);
        } else {
            super.system.getWorld().game.drawWorldText(batch, hitmarkerFont, getX()-size*.5f, getY()-size*.5f, size, getPool().value);
        }
    }
}
