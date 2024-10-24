package com.gregtechceu.gtceu.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import lombok.NonNull;

/**
 * Implement this interface in order to make a BlockEntity into a block that can be part of a fission reactor
 */
public interface VerticalModelLinkBlock {

    EnumProperty<VLinkTypes> V_LINK = EnumProperty.create("vlink", VLinkTypes.class);

    /**
     * Retrieves the type of vertical link the block has
     * 
     * @param state the block state of the block
     * @param level the level the block is in
     * @param pos   the position of the block
     * @return the type of vertical link the block has
     */
    VLinkTypes retrieveVLinkType(BlockState state, Level level, BlockPos pos);

    /**
     * The types of vertical link a block can have
     */
    enum VLinkTypes implements StringRepresentable {

        UP,
        DOWN,
        BOTH,
        NONE;

        @Override
        @NonNull
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
