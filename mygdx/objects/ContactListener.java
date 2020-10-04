package com.mygdx.objects;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;

public abstract class ContactListener {
    WorldObject owner; //May be null

    /**@param owner This is used to remove this listener when the object gets removed
     * since it is not longer in use anymore. Owner also helps to keep track of witch
     * listener belongs to witch. Use can use null to ignore tracking and to keep
     * the listener after object removal*/
    public ContactListener(WorldObject owner) {
        this.owner = owner;
    }

    public abstract void beginContact(WorldObject object1, WorldObject object2, Contact contact);
    public abstract void endContact(WorldObject object1, WorldObject object2, Contact contact);
    public abstract void preSolve(WorldObject object1, WorldObject object2, Contact contact, Manifold oldManifold);
    public abstract void postSolve(WorldObject object1, WorldObject object2, Contact contact, ContactImpulse impulse);
}
