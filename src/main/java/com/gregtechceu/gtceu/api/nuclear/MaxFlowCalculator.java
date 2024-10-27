package com.gregtechceu.gtceu.api.nuclear;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MaxFlowCalculator {
    public static int edmondsKarp(FlowNetwork network, Node source, Node sink) {
        int maxFlow = 0;
        Map<Edge, Edge> edgeMap = createEdgeMap(network);

        while (true) {
            Map<Node, Edge> parentMap = new HashMap<>();
            Queue<Node> queue = new LinkedList<>();
            queue.add(source);

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

            if (!parentMap.containsKey(sink)) break;

            int pathFlow = Integer.MAX_VALUE;
            for (Node node = sink; node != source; ) {
                Edge edge = parentMap.get(node);
                pathFlow = Math.min(pathFlow, edge.getResidualCapacity());
                node = edge.getFrom();
            }

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

    private static Map<Edge, Edge> createEdgeMap(FlowNetwork network) {
        Map<Edge, Edge> edgeMap = new HashMap<>();
        for (Node node : network.adjacents.keySet()) {
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
