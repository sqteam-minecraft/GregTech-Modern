package com.gregtechceu.gtceu.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Implement this interface in order to make a Block have model rendering with vertical links
 */
public interface VerticalModelLinkBlock {

    /**
     * The vertical link property
     * 
     * @see Block#registerDefaultState(BlockState)
     * @see Block#createBlockStateDefinition(StateDefinition.Builder)
     */
    EnumProperty<VLinkTypes> V_LINK = EnumProperty.create("vlink", VLinkTypes.class);

    /**
     * Call this method when the block is placed
     * 
     * @param state         the block state of the block
     * @param level         the level the block is in
     * @param pos           the position of the block
     * @param oldState      the old block state
     * @param movedByPiston whether the block was moved by a piston
     * @see Block#onPlace(BlockState, Level, BlockPos, BlockState, boolean)
     */
    @SuppressWarnings("deprecation")
    default void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            if (state.getBlock() != oldState.getBlock())
                level.setBlock(pos, state.setValue(V_LINK, retrieveVLinkType(state, level, pos)), 3);
        }
    }

    /**
     * Call this method when a neighbor block changes
     * 
     * @param state         the block state of the block
     * @param level         the level the block is in
     * @param pos           the position of the block
     * @param neighborBlock the block that changed
     * @param neighborPos   the position of the neighbor block
     * @param movedByPiston whether the block was moved by a piston
     * @see Block#neighborChanged(BlockState, Level, BlockPos, Block, BlockPos, boolean)
     */
    @SuppressWarnings("deprecation")
    default void neighborChanged(BlockState state, @NotNull Level level, BlockPos pos, Block neighborBlock,
                                 BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) level.setBlock(pos, state.setValue(V_LINK, retrieveVLinkType(state, level, pos)), 3);
    }

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
