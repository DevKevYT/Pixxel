package com.pixxel.ai;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.pixxel.Main.Logger;
import com.pixxel.objects.WorldValues;
import com.pixxel.objects.World;

import java.util.ArrayList;

public class GraphData implements IndexedGraph<com.pixxel.ai.Node>, Disposable {

    Heuristic h = new Heuristic();
    ArrayList<com.pixxel.ai.Node> nodes = new ArrayList<>();
    ArrayList<Connection> generatedConnection = new ArrayList<>(); //Generated in generateGraph()
    private int lastIndex = 0;
    private boolean needReindex = false;
    private com.pixxel.objects.World world;

    /**The node positions are normalized.*/
    public GraphData(World world, ArrayList<WorldValues.NodeData> nodes) {
        int countIndex = 0;
        for(WorldValues.NodeData n : nodes) {
            if(!needReindex && lastIndex != countIndex) needReindex = true; //Nodes getting reindexed, after graph generation, because there are gaps e.g. 1, 2, 4, ...
            if(n.index > lastIndex) lastIndex = n.index;
            countIndex++;

            this.nodes.add(new com.pixxel.ai.Node(n));
        }
    }

    /**Generates connections between the nodes using their corresponding indexes
     * If any connectionData set has an invalid index, the connection wont be established and the connectionData list will be
     * altered*/
    public void generateGraph(ArrayList<WorldValues.ConnectionData> connectionData) {
        for(int i = 0; i < connectionData.size(); i++) {
            com.pixxel.ai.Node fromNode = findNode(connectionData.get(i).i1);
            if(fromNode == null) {
                Logger.logError("GenerateGraph", "Couldn't find node with index " + connectionData.get(i).i1);
                continue;
            }
            com.pixxel.ai.Node toNode = findNode(connectionData.get(i).i2);
            if(toNode == null) {
                Logger.logError("GenerateGraph", "Couldn't find node with index " + connectionData.get(i).i2);
                continue;
            }
            generatedConnection.add(new Connection(fromNode, toNode, connectionData.get(i).cost));
        }
        //Finally, fix any index issues by "reindexing"
        if(needReindex) reindex();
    }

    public com.pixxel.ai.Node findNode(int index) {
        if(index < 0 || index > lastIndex) return null;
        for(com.pixxel.ai.Node n : nodes) {
            if(n.data.index == index) return n;
        }
        return null;
    }

    public ArrayList<com.pixxel.ai.Node> getNodes() {
        return nodes;
    }

    public ArrayList<Connection> getNodeConnections() {
        return generatedConnection;
    }

    /**In the connection data the fromNode (i1) will be ignored. Imortant is the index of the "toNode" (i2) and the cost
     * Notice: The node positions should be normalized (as world scale would be 1)*/
    public void addNode(WorldValues.NodeData data, WorldValues.ConnectionData... connections) {
        com.pixxel.ai.Node node = new com.pixxel.ai.Node(data);
        node.data.index = lastIndex;
        nodes.add(node);

        for(WorldValues.ConnectionData c : connections) {
            if(c.i2 < 0 || c.i2 > lastIndex) continue;
            com.pixxel.ai.Node found = findNode(c.i2);
            if(found == null) {
                Logger.logError("addNode", "Couldn't find node with index " + c.i2);
                continue;
            }
            Connection connection = new Connection(node, found, c.cost);
            generatedConnection.add(connection);
        }
        lastIndex++;
    }

    public void addConnection(WorldValues.ConnectionData data) {
        com.pixxel.ai.Node found1 = findNode(data.i1);
        if(found1 == null) {
            Logger.logError("addConnection", "Couldn't find node with index " + data.i1);
            return;
        }
        com.pixxel.ai.Node found2 = findNode(data.i2);
        if(found2 == null) {
            Logger.logError("addConnection", "Couldn't find node with index " + data.i2);
            return;
        }
        for(Connection c : generatedConnection) {
            if(c.fromNode.equals(found1) && c.toNode.equals(found2) || c.fromNode.equals(found2) && c.toNode.equals(found1)) return;
        }
        generatedConnection.add(new Connection(found1, found2, data.cost));
    }

    public void removeNode(com.pixxel.ai.Node node) {
        for(int i = 0; i < generatedConnection.size(); i++) {
            if(generatedConnection.get(i).toNode.equals(node) || generatedConnection.get(i).fromNode.equals(node)) {
                generatedConnection.remove(i);
                i--;
            }
        }
        nodes.remove(node);
        reindex();
    }

    public void removeConnection(Connection connection) {
        generatedConnection.remove(connection);
    }

    /**Position should be normalized*/
    public GraphPath<com.pixxel.ai.Node> findPath(Vector2 startPos, Vector2 endPos) {
        com.pixxel.ai.Node start = null, end = null;
        float recordStart = -1;
        float recordEnd = -1;
        for(com.pixxel.ai.Node n : nodes) {
            float startDist = startPos.dst(n.data.x, n.data.y);
            if(startDist < recordStart || recordStart == -1) {
                recordStart = startDist;
                start = n;
            }
            float endDist = endPos.dst(n.data.x, n.data.y);
            if(endDist < recordEnd || recordEnd == -1) {
                recordEnd = endDist;
                end = n;
            }
        }
        if(start != null && end != null) {
            return findPath(start, end);
        } else return null;
    }

    public GraphPath<com.pixxel.ai.Node> findPath(com.pixxel.ai.Node start, com.pixxel.ai.Node end) {
        GraphPath<com.pixxel.ai.Node> generated = new DefaultGraphPath<com.pixxel.ai.Node>();
        new IndexedAStarPathFinder<>(this).searchNodePath(start, end, h, generated);
        return generated;
    }

    /**Don't call this function every frame. This function is intendet to use for world saving*/
    public ArrayList<WorldValues.NodeData> getNodeData() {
        ArrayList<WorldValues.NodeData> data = new ArrayList<>();
        for(com.pixxel.ai.Node n : nodes) data.add(n.data);
        return data;
    }

    /**Don't call this function every frame. This function is intendet to use for world saving*/
    public ArrayList<WorldValues.ConnectionData> getConnectionData() {
        ArrayList<WorldValues.ConnectionData> data = new ArrayList<>();
        for(Connection c : generatedConnection) {
            WorldValues.ConnectionData gen = new WorldValues.ConnectionData();
            gen.i2 = c.toNode.data.index;
            gen.i1 = c.fromNode.data.index;
            gen.cost = c.getCost();
            data.add(gen);
        }
        return data;
    }

    public void reindex() {
        for(int i = 0; i < nodes.size(); i++) nodes.get(i).data.index = i; //Connections wont get affected since the node objects are referenced anyways after generating from connectiondata
        lastIndex = nodes.size();
    }

    @Override
    public int getIndex(com.pixxel.ai.Node node) {
        return node.data.index;
    }

    @Override
    public int getNodeCount() {
        return nodes.size();
    }

    @Override
    public Array<com.badlogic.gdx.ai.pfa.Connection<com.pixxel.ai.Node>> getConnections(com.pixxel.ai.Node fromNode) {
        Array<com.badlogic.gdx.ai.pfa.Connection<Node>> connections = new Array<>();
        for(Connection c : generatedConnection) {
            if(c.toNode.equals(fromNode) ||c.fromNode.equals(fromNode)) {
               Connection con = new Connection(fromNode, c.toNode.equals(fromNode) ? c.fromNode : c.toNode, c.getCost());
               connections.add(con);
            }
        }
        return connections;
    }

    @Override
    public void dispose() {
        nodes.clear();
        generatedConnection.clear();
    }
}
