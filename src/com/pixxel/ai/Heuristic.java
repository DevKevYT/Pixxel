package com.pixxel.ai;

public class Heuristic implements com.badlogic.gdx.ai.pfa.Heuristic<com.pixxel.ai.Node> {

    @Override
    public float estimate(com.pixxel.ai.Node node, Node endNode) {
        final float dx = node.data.x - endNode.data.x;
        final float dy = node.data.y - endNode.data.y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }
}
