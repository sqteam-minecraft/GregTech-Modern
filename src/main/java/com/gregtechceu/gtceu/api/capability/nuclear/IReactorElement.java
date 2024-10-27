package com.gregtechceu.gtceu.api.capability.nuclear;

import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Implement this interface in order to make a BlockEntity into a block that can be part of a fission reactor
 */
public interface IReactorElement {
    int calculateEdgeCapacity(BlockState state, @Nullable IReactorElement to);

    int getHeatThroughput();
}
