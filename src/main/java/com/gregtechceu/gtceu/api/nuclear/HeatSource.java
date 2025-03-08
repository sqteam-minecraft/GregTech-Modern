package com.gregtechceu.gtceu.api.nuclear;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for reactor components that serve as heat sources.
 */
public interface HeatSource {

    /**
     * Returns the heat production value based on the block state.
     *
     * @param state The block state.
     * @return The heat production value.
     */
    int getHeatProduction(BlockState state);
}
