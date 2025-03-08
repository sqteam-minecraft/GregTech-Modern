package com.gregtechceu.gtceu.api.capability.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement this interface in order to make a BlockEntity into a block that can be part of a fission reactor
 */
public interface IReactorElement {
    /**
     * @param state - the <code>BlockState</code> of this <code>IReactorElement</code>
     * @param to - optional <code>IReactorElement</code> to which the edge is connected
     *
     * @return the edge capacity of this element
     */
    int calculateEdgeCapacity(BlockState state, @Nullable IReactorElement to);

    /**
     * Get the heat throughput of this element
     */
    int getHeatThroughput();

    /**
     * Retrieves neighboring positions for a given block position in the structure.
     *
     * @param structure A mapping of block positions to reactor elements.
     * @param position  The current block position.
     * @return A list of neighboring block positions present in the structure.
     */
    default @NotNull List<BlockPos> getNetworkNeighbors(Map<BlockPos, IReactorElement> structure, BlockPos position,
                                                        Level level) {
        List<BlockPos> neighbors = new ArrayList<>();

        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();

        // Check all possible adjacent directions.
        for (Direction dir : Direction.values()) {
            Vec3i axis = dir.getNormal();
            BlockPos neighborPos = new BlockPos(x + axis.getX(), y + axis.getY(), z + axis.getZ());
            if (structure.containsKey(neighborPos)) {
                neighbors.add(neighborPos);
            }
        }
        return neighbors;
    }
}
