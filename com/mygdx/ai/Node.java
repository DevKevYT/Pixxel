package com.mygdx.ai;

import com.badlogic.gdx.utils.Array;
import com.mygdx.objects.WorldValues;

import java.util.ArrayList;

public class Node {
    public WorldValues.NodeData data;
    public Array<com.badlogic.gdx.ai.pfa.Connection<Node>> connections = new Array<>();

    public Node(WorldValues.NodeData data) {
        this.data = data;
    }
}
