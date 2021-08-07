package com.pixxel.entities;

import com.pixxel.objects.WorldObject;

/**To generalize the entity behavior and to make sure*/
public interface EntityEvents {
    public void onHit(WorldObject source, Integer addHealth, Float knockbackX, Float knockbackY);
}
