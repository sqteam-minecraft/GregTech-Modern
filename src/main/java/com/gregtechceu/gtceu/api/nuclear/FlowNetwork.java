package com.gregtechceu.gtceu.api.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FlowNetwork {
    Map<Node, List<Edge>> adjacents = new HashMap<>();

    public void addNode(Node node) {
        adjacents.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        // Ensure both nodes are in the network
        if (!adjacents.containsKey(edge.getFrom())) {
            addNode(edge.getFrom());
        }
        if (!adjacents.containsKey(edge.getTo())) {
            addNode(edge.getTo());
        }

        adjacents.get(edge.getFrom()).add(edge);
        // Add reverse edge for residual graph
        Edge reverseEdge = new Edge(edge.getTo(), edge.getFrom(), 0);
        reverseEdge.setReverse(true);
        adjacents.get(edge.getTo()).add(reverseEdge);
    }

    public List<Edge> getEdges(Node node) {
        return adjacents.get(node);
    }

    // Modified printFlows function
    public String printFlows(Node source, Node sink) {
        Set<Node> visited = new HashSet<>();
        visited.add(source);
        return printFlowPaths(source, sink, visited, 0);
    }

    private String printFlowPaths(Node current, Node sink, Set<Node> visited, int indent) {
        if (current.equals(sink)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        List<Edge> edges = getEdges(current);
        for (Edge edge : edges) {
            if (edge.getCapacity() > 0 && edge.getFlow() > 0 && !edge.isReverse()) {
                Node toNode = edge.getTo();
                String line = String.format("%s%s -> %s : %d/%d\n",
                        "    ".repeat(indent),
                        edge.getFrom().getId(),
                        toNode.getId(),
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

    public static FlowNetworkResult buildFlowNetwork(Map<BlockPos, IReactorElement> structure, Level level) {
        // Initialize the flow network
        FlowNetwork network = new FlowNetwork();

        // Create source and sink nodes
        Node source = new Node("Source", null);
        Node sink = new Node("Sink", null);
        network.addNode(source);
        network.addNode(sink);

        // Maps to store positions and nodes
        Map<BlockPos, Node> positionNodeMap = new HashMap<>();
        Map<BlockPos, PositionedComponent> positionComponentMap = new HashMap<>();

        // Map to store connections between positions
        Map<BlockPos, List<BlockPos>> connections = new HashMap<>();

        // Collect components and build connections
        for (Map.Entry<BlockPos, IReactorElement> entry : structure.entrySet()) {
            BlockPos position = entry.getKey();
            IReactorElement component = entry.getValue();

            PositionedComponent positionedComponent = new PositionedComponent(component, position);
            positionComponentMap.put(position, positionedComponent);

            Node node = new Node(positionedComponent.getId(), component);
            positionNodeMap.put(position, node);
            network.addNode(node);

            List<BlockPos> neighbors = new ArrayList<>();

            // Check neighboring positions (assuming BlockPos has getX(), getY(), getZ())
            int x = position.getX();
            int y = position.getY();
            int z = position.getZ();

            for (Direction dir : Direction.values()) {
                Vec3i axis = dir.getNormal();
                int nx = x + axis.getX();
                int ny = y + axis.getY();
                int nz = z + axis.getZ();
                BlockPos neighborPos = new BlockPos(nx, ny, nz);
                if (structure.containsKey(neighborPos)) {
                    neighbors.add(neighborPos);
                }
            }

            connections.put(position, neighbors);
        }

        // Add edges based on connections
        for (Map.Entry<BlockPos, List<BlockPos>> entry : connections.entrySet()) {
            BlockPos fromPosition = entry.getKey();
            Node fromNode = positionNodeMap.get(fromPosition);
            PositionedComponent fromComponent = positionComponentMap.get(fromPosition);

            // If the component is a FuelRod, connect from source
            if (fromComponent.getComponent() instanceof HeatSource heatSource) {
                network.addEdge(new Edge(source, fromNode, heatSource.getHeatProduction(level.getBlockState(fromPosition))));
            }

            // Add edges to neighboring components
            for (BlockPos toPosition : entry.getValue()) {
                Node toNode = positionNodeMap.get(toPosition);
                PositionedComponent toComponent = positionComponentMap.get(toPosition);

                int capacity = fromComponent.getComponent().calculateEdgeCapacity(fromComponent.getBlockState(level), toComponent.getComponent());
                if (capacity > 0) {
                    network.addEdge(new Edge(fromNode, toNode, capacity));
                }
            }

            // Add edge to sink if applicable
            int capacityToSink = fromComponent.getComponent().calculateEdgeCapacity(fromComponent.getBlockState(level), null);
            if (capacityToSink > 0) {
                network.addEdge(new Edge(fromNode, sink, capacityToSink));
            }
        }

        // Return the network along with source and sink nodes
        return new FlowNetworkResult(network, source, sink, positionNodeMap);
    }
}