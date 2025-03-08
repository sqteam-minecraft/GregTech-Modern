package com.gregtechceu.gtceu.api.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wraps a reactor element with its associated block position.
 * Provides utility methods to retrieve block state and generate a unique identifier.
 */
public record PositionedComponent(IReactorElement component, BlockPos position) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionedComponent that = (PositionedComponent) o;
        return position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    /**
     * Retrieves the block state at the component's position in the given level.
     *
     * @param level The level containing the block.
     * @return The block state.
     */
    public BlockState getBlockState(Level level) {
        return level.getBlockState(this.position);
    }

    /**
     * Generates a unique identifier for the component based on its type and position.
     *
     * @return A string identifier.
     */
    public String getId() {
        return component.getClass()
                .getSimpleName() + "@" + position.getX() + "," + position.getY() + "," + position.getZ();
    }
}
