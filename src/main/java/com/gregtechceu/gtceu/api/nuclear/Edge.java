package com.gregtechceu.gtceu.api.nuclear;

public class Edge {
    private Node from;
    private Node to;
    private int capacity;
    private int flow;
    private boolean isReverse;

    public Edge(Node from, Node to, int capacity) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.flow = 0;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getResidualCapacity() {
        return capacity - flow;
    }

    public void addFlow(int delta) {
        flow += delta;
    }

    public int getFlow() {
        return flow;
    }

    public void setReverse(boolean isReverse) {
        this.isReverse = isReverse;
    }

    public boolean isReverse() {
        return isReverse;
    }
}