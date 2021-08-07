package com.pixxel.ai;

import com.badlogic.gdx.math.MathUtils;
import com.pixxel.objects.WorldValues;

public class Connection implements com.badlogic.gdx.ai.pfa.Connection<com.pixxel.ai.Node> {

    public final com.pixxel.ai.Node fromNode;
    public final com.pixxel.ai.Node toNode;
    private float cost = 0;

    public Connection(com.pixxel.ai.Node fromNode, com.pixxel.ai.Node toNode, float cost) {
        this.cost = cost;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.fromNode.connections.add(this);
        this.toNode.connections.add(this);
    }

    /**To save this connection as JSON*/
    public WorldValues.ConnectionData generateConnectionData() {
        WorldValues.ConnectionData data = new WorldValues.ConnectionData();
        data.cost = cost;
        data.i1 = fromNode.data.index;
        data.i2 = toNode.data.index;
        return data;
    }

    @Override
    public float getCost() {
        return MathUtils.random(cost, cost+3);
    }

    @Override
    public com.pixxel.ai.Node getFromNode() {
        return fromNode;
    }

    @Override
    public Node getToNode() {
        return toNode;
    }
}
