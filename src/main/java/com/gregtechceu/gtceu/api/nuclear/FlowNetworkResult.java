package com.gregtechceu.gtceu.api.nuclear;

import net.minecraft.core.BlockPos;

import java.util.Map;

public class FlowNetworkResult {
    private final FlowNetwork network;
    private final Node source;
    private final Node sink;
    private final Map<BlockPos, Node> positionNodeMap;

    public FlowNetworkResult(FlowNetwork network, Node source, Node sink, Map<BlockPos, Node> positionNodeMap) {
        this.network = network;
        this.source = source;
        this.sink = sink;
        this.positionNodeMap = positionNodeMap;
    }

    public FlowNetwork getNetwork() {
        return network;
    }

    public Node getSource() {
        return source;
    }

    public Node getSink() {
        return sink;
    }

    public String printFlows() {
        return network.printFlows(source, sink);
    }

    public Map<BlockPos, Node> getPositionNodeMap() {
        return positionNodeMap;
    }
}
