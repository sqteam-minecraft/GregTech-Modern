package com.gregtechceu.gtceu.api.nuclear;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Implements the Edmonds-Karp algorithm to compute the maximum flow in a flow network.
 */
public class MaxFlowCalculator {

    /**
     * Computes the maximum flow from the source to the sink using the Edmonds-Karp algorithm.
     *
     * <p>This implementation uses breadth-first search (BFS) to find augmenting paths in the residual graph.
     * For each found path, it updates the flows along the edges and their corresponding reverse edges.</p>
     *
     * @param network The flow network.
     * @param source  The source node.
     * @param sink    The sink node.
     * @return The maximum flow value.
     */
    public static int edmondsKarp(FlowNetwork network, Node source, Node sink) {
        int maxFlow = 0;
        Map<Edge, Edge> edgeMap = createEdgeMap(network);

        // Iteratively search for augmenting paths.
        while (true) {
            Map<Node, Edge> parentMap = new HashMap<>();
            Queue<Node> queue = new LinkedList<>();
            queue.add(source);

            // BFS to find an augmenting path.
            while (!queue.isEmpty()) {
                Node current = queue.poll();
                for (Edge edge : network.getEdges(current)) {
                    Node to = edge.getTo();
                    if (edge.getResidualCapacity() > 0 && !parentMap.containsKey(to) && to != source) {
                        parentMap.put(to, edge);
                        if (to == sink) break;
                        queue.add(to);
                    }
                }
            }

            // If no augmenting path is found, terminate.
            if (!parentMap.containsKey(sink)) break;

            // Determine the bottleneck (minimum residual capacity) along the path.
            int pathFlow = Integer.MAX_VALUE;
            for (Node node = sink; node != source; ) {
                Edge edge = parentMap.get(node);
                pathFlow = Math.min(pathFlow, edge.getResidualCapacity());
                node = edge.getFrom();
            }

            // Update flows along the path.
            for (Node node = sink; node != source; ) {
                Edge edge = parentMap.get(node);
                edge.addFlow(pathFlow);
                Edge reverseEdge = edgeMap.get(edge);
                reverseEdge.addFlow(-pathFlow);
                node = edge.getFrom();
            }

            maxFlow += pathFlow;
        }
        return maxFlow;
    }

    /**
     * Constructs a mapping between each forward edge and its corresponding reverse edge.
     * This is used for quick look-up during flow adjustments.
     *
     * @param network The flow network.
     * @return A map linking each forward edge to its reverse edge.
     */
    private static Map<Edge, Edge> createEdgeMap(FlowNetwork network) {
        Map<Edge, Edge> edgeMap = new HashMap<>();
        for (Node node : network.adjacentNodes.keySet()) {
            for (Edge edge : network.getEdges(node)) {
                for (Edge reverseEdge : network.getEdges(edge.getTo())) {
                    if (reverseEdge.getTo() == edge.getFrom()) {
                        edgeMap.put(edge, reverseEdge);
                        break;
                    }
                }
            }
        }
        return edgeMap;
    }
}
