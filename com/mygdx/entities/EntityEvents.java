package com.mygdx.entities;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.objects.WorldObject;

/**To generalize the entity behavior and to make sure*/
public interface EntityEvents {
    public void onHit(WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY);
}
