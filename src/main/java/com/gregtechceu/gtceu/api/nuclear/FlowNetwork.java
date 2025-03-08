package com.gregtechceu.gtceu.api.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Represents a flow network composed of nodes and edges.
 * The network is built from a structure map, connecting reactor elements,
 * and supports printing flow paths from source to sink.
 */
public class FlowNetwork {

    // Accessible for use by other components (e.g., the max flow calculator)
    Map<Node, List<Edge>> adjacentNodes = new HashMap<>();

    public void addNode(Node node) {
        adjacentNodes.putIfAbsent(node, new ArrayList<>());
    }

    /**
     * Adds an edge to the network and automatically creates a reverse edge with zero capacity.
     *
     * @param edge The edge to add.
     */
    public void addEdge(Edge edge) {
        // Ensure both nodes exist in the network.
        if (!adjacentNodes.containsKey(edge.getFrom())) addNode(edge.getFrom());
        if (!adjacentNodes.containsKey(edge.getTo())) addNode(edge.getTo());

        adjacentNodes.get(edge.getFrom()).add(edge);

        // Create and add the reverse edge for residual capacity calculations.
        Edge reverseEdge = new Edge(edge.getTo(), edge.getFrom(), 0);
        reverseEdge.setReverse(true);
        adjacentNodes.get(edge.getTo()).add(reverseEdge);
    }

    public List<Edge> getEdges(Node node) {
        return adjacentNodes.get(node);
    }

    /**
     * Recursively prints all flow paths from the source to the sink.
     *
     * @param source The source node.
     * @param sink   The sink node.
     * @return A formatted string representing the flow paths.
     */
    public String printFlows(Node source, Node sink) {
        Set<Node> visited = new HashSet<>();
        visited.add(source);
        return printFlowPaths(source, sink, visited, 0);
    }

    /**
     * Helper method to recursively build the flow paths string.
     *
     * @param current The current node.
     * @param sink    The target sink node.
     * @param visited A set of visited nodes to avoid cycles.
     * @param indent  The current indentation level.
     * @return A formatted string for the current branch.
     */
    private String printFlowPaths(Node current, Node sink, Set<Node> visited, int indent) {
        if (current.equals(sink)) return "";
        StringBuilder builder = new StringBuilder();
        List<Edge> edges = getEdges(current);
        for (Edge edge : edges) {
            // Only consider forward edges with flow.
            if (edge.getCapacity() > 0 && edge.getFlow() > 0 && !edge.isReverse()) {
                Node toNode = edge.getTo();
                String line = String.format("%s%s -> %s : %d/%d\n",
                        "    ".repeat(indent),
                        edge.getFrom().id(),
                        toNode.id(),
                        edge.getFlow(),
                        edge.getCapacity());
                builder.append(line);

                if (!visited.contains(toNode)) {
                    visited.add(toNode);
                    builder.append(printFlowPaths(toNode, sink, visited, indent + 1));
                    visited.remove(toNode);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Builds a flow network based on the provided reactor structure.
     *
     * @param structure A mapping of block positions to reactor elements.
     * @param level     The level containing the reactor.
     * @return A {@code FlowNetworkResult} containing the built network, source, sink, and node mapping.
     */
    public static FlowNetworkResult buildFlowNetwork(Map<BlockPos, IReactorElement> structure, Level level) {
        FlowNetwork network = new FlowNetwork();

        Node source = new Node("Source", null);
        Node sink = new Node("Sink", null);
        network.addNode(source);
        network.addNode(sink);

        Map<BlockPos, Node> positionNodeMap = new HashMap<>();
        Map<BlockPos, PositionedComponent> positionComponentMap = new HashMap<>();
        Map<BlockPos, List<BlockPos>> connections = new HashMap<>();

        // Process each reactor element in the structure.
        for (Map.Entry<BlockPos, IReactorElement> entry : structure.entrySet()) {
            BlockPos position = entry.getKey();
            IReactorElement component = entry.getValue();

            PositionedComponent positionedComponent = new PositionedComponent(component, position);
            positionComponentMap.put(position, positionedComponent);

            Node node = new Node(positionedComponent.getId(), component);
            positionNodeMap.put(position, node);
            network.addNode(node);

            List<BlockPos> neighbors = component.getNetworkNeighbors(structure, position, level);
            connections.put(position, neighbors);
        }

        // Create network edges based on component connections.
        for (Map.Entry<BlockPos, List<BlockPos>> entry : connections.entrySet()) {
            BlockPos fromPosition = entry.getKey();
            Node fromNode = positionNodeMap.get(fromPosition);
            PositionedComponent fromComponent = positionComponentMap.get(fromPosition);

            // If the component is a heat source, add an edge from the source.
            if (fromComponent.component() instanceof HeatSource heatSource) {
                network.addEdge(new Edge(source, fromNode, heatSource
                        .getHeatProduction(level.getBlockState(fromPosition))));
            }

            // Add edges to each neighboring component.
            for (BlockPos toPosition : entry.getValue()) {
                Node toNode = positionNodeMap.get(toPosition);
                PositionedComponent toComponent = positionComponentMap.get(toPosition);

                int capacity = fromComponent.component()
                        .calculateEdgeCapacity(fromComponent.getBlockState(level), toComponent.component());
                if (capacity > 0) {
                    network.addEdge(new Edge(fromNode, toNode, capacity));
                }
            }

            // Connect the component to the sink if applicable.
            int capacityToSink = fromComponent.component()
                    .calculateEdgeCapacity(fromComponent.getBlockState(level), null);
            if (capacityToSink > 0) {
                network.addEdge(new Edge(fromNode, sink, capacityToSink));
            }
        }

        return new FlowNetworkResult(network, source, sink, positionNodeMap);
    }
}
