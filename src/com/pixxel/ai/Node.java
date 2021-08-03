package com.pixxel.ai;

import com.badlogic.gdx.utils.Array;
import com.pixxel.objects.WorldValues;

public class Node {
    public WorldValues.NodeData data;
    public Array<com.badlogic.gdx.ai.pfa.Connection<Node>> connections = new Array<>();

    public Node(WorldValues.NodeData data) {
        this.data = data;
    }
}
