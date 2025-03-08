package com.gregtechceu.gtceu.api.nuclear;

import net.minecraft.core.BlockPos;
import java.util.Map;

/**
 * Encapsulates the result of building a flow network including the network itself,
 * the source and sink nodes, and a mapping from block positions to nodes.
 */
public record FlowNetworkResult(FlowNetwork network, Node source, Node sink, Map<BlockPos, Node> positionNodeMap) {

    /**
     * Convenience method to print the flow paths from the source to the sink.
     *
     * @return A formatted string of flow paths.
     */
    public String printFlows() {
        return network.printFlows(source, sink);
    }
}
