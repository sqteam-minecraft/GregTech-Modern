package com.gregtechceu.gtceu.api.nuclear;

import lombok.Getter;

/**
 * Represents an edge in a flow network between two nodes with a specified capacity.
 * It supports flow adjustments and can represent reverse edges in the residual graph.
 */
public class Edge {

    @Getter
    private final Node from;
    @Getter
    private final Node to;
    @Getter
    private final int capacity;
    @Getter
    private int flow;
    private boolean isReverse;

    public Edge(Node from, Node to, int capacity) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.flow = 0;
    }

    /**
     * Returns the available capacity on this edge.
     *
     * @return The residual capacity (capacity - flow).
     */
    public int getResidualCapacity() {
        return capacity - flow;
    }

    /**
     * Adjusts the current flow along this edge by the specified delta.
     *
     * @param delta The amount by which to adjust the flow.
     */
    public void addFlow(int delta) {
        flow += delta;
    }

    /**
     * Marks this edge as a reverse edge in the residual graph.
     *
     * @param isReverse True if this edge is a reverse edge.
     */
    public void setReverse(boolean isReverse) {
        this.isReverse = isReverse;
    }

    /**
     * Indicates if this edge is a reverse edge.
     *
     * @return True if reverse, false otherwise.
     */
    public boolean isReverse() {
        return isReverse;
    }
}
