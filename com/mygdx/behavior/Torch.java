package com.mygdx.behavior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.Behavior;
import com.mygdx.objects.RootValues;
import com.mygdx.objects.World;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

public class Torch extends Behavior {

    private float angle = 0;
    private float speed = 0;
    public float dist = 1;
    public float xOffset = 0;
    public float yOffset = 0;
    public float r = 1, g = 1, b = 1, a = 1;
    RootValues.LightSource light;
    public Torch(WorldValues.BehaviorValues values) {
        super(values, Torch.class);
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onCreate(WorldObject object) {
        r = 0.5f;
        g = 0.8f;
        b = 0.9f;
        a = 0.8f;
        light = new RootValues.LightSource();
        light.rays = 80;
        light.r = r;
        light.g = g;
        light.b = b;
        light.a = a;
        object.createLight(light);
    }

    @Override
    public void postCreate() {

    }

    public boolean lightChanged(){
        return light.r != r || light.g != g || light.b != b || light.a != a;
    }

    @Override
    public void onUpdate(World world, WorldObject object, float deltaTime) {
        if(lightChanged()){
            light.r = r;
            light.g = g;
            light.b = b;
            light.a = a;
            object.createLight(light);
        }
        parent.getLight().setDistance((MathUtils.sin(angle)*0.1f+1.2f)*dist*100*world.getScale());
        parent.getRootValues().values.light.x = xOffset;
        parent.getRootValues().values.light.y = yOffset;

        if(angle > MathUtils.PI2)  angle = 0;
        else angle += speed;

        if(Gdx.graphics.getFrameId() % (int) (Gdx.graphics.getFramesPerSecond() * 0.5f + 1) == 0) {
            speed = MathUtils.random(0.1f, 0.3f);
        }
    }

    @Override
    public void onRemove() {
        if(parent != null) parent.createLight(null);
    }

    @Override
    public void drawOver(WorldObject object, SpriteBatch batch) {

    }

    @Override
    public void drawBehind(WorldObject object, SpriteBatch batch) {

    }
}
