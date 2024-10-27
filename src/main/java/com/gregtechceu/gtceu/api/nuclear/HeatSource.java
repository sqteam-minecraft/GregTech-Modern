package com.gregtechceu.gtceu.api.nuclear;

import net.minecraft.world.level.block.state.BlockState;

public interface HeatSource {
    int getHeatProduction(BlockState state);
}
