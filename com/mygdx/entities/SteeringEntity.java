package com.mygdx.entities;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.ai.Node;
import com.mygdx.darkdawn.Logger;
import com.mygdx.objects.WorldObject;
import com.mygdx.objects.WorldValues;

import java.util.ArrayList;

/**This class uses the com.mygdx.ai package for pathfinding*/
public class SteeringEntity {

    private WorldObject entity;
    private boolean moving = false;
    public Vector2 targetOffset = new Vector2(); //A range
    private Vector2 randomOffset = new Vector2();

    public float speed;
    ArrayList<Node> path = new ArrayList<>();
    private boolean finished = false;

    public SteeringEntity(WorldObject entity) {
        this.entity = entity;
    }

    public void update() {
        if(moving && path.size() > 0) {
            finished = false;
            entity.setFixed(false);
            entity.moveTo(path.get(0).data.x + randomOffset.x, path.get(0).data.y + randomOffset.y, speed);

            if(entity.getPosition().dst(path.get(0).data.x + randomOffset.x, path.get(0).data.y + randomOffset.y) <= speed) {
                finished = true;
                entity.setPosition(path.get(0).data.x + randomOffset.x, path.get(0).data.y + randomOffset.y);
                path.remove(0);
                randomOffset.set(MathUtils.random(targetOffset.x, targetOffset.y), MathUtils.random(targetOffset.x, targetOffset.y));
                if(path.isEmpty()) moving = false;
            }
        }
    }

    /**May return null*/
    public Node currentTarget() {
        if(!moving) return null;
        else if(path.size() > 0) {
            return path.get(0);
        }
        return null;
    }

    /**Sets a path, the entity should follow.*/
    public void setPath(GraphPath<Node> path, float speed, boolean overwrite) {
        if(moving || !overwrite || path == null) return;
        else cancelMovement();

        this.speed = speed;
        moving = true;
        entity.setFixed(false);
        for(Node n : path) this.path.add(n);
    }

    public void setPath(float speed, boolean overwrite, Vector2... nodes) {
        if(moving && !overwrite) return;
        else cancelMovement();

        this.speed = speed;
        moving = true;
        entity.setFixed(false);
        for(Vector2 v : nodes) {
            WorldValues.NodeData data = new WorldValues.NodeData();
            data.x = v.x;
            data.y = v.y;
            this.path.add(new Node(data));
        }
    }

    /**Cancels the target*/
    public void cancelMovement() {
        finished = true;
        moving = false;
        path.clear();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isMoving() {
        return moving;
    }
}
