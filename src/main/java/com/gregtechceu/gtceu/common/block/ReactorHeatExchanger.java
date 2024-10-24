package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ReactorHeatExchanger extends AppearanceBlock implements SimpleWaterloggedBlock, VerticalModelLinkBlock {

    public ReactorHeatExchanger(Properties properties) {
        super(properties);

        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(V_LINK, VLinkTypes.NONE));
    }

    @OnlyIn(Dist.CLIENT)
    public static Supplier<BlockColor> tintColor(int pipeColor, int plateColor) {
        return () -> (state, reader, pos, tintIndex) -> {
            if (state.getBlock() instanceof ReactorHeatExchanger rhe) {
                if (tintIndex == 0) return pipeColor;
                if (tintIndex == 1) return plateColor;
            }
            return -1;
        };
    }

    @OnlyIn(Dist.CLIENT)
    public static Supplier<ItemColor> tintItemColor(int pipeColor, int plateColor) {
        return () -> (stack, tintIndex) -> {
            if (tintIndex == 0) return pipeColor;
            if (tintIndex == 1) return plateColor;
            return -1;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                boolean movedByPiston) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(V_LINK, retrieveVLinkType(state, level, pos)), 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.WATERLOGGED, V_LINK));
    }

    @Override
    public VLinkTypes retrieveVLinkType(BlockState state, Level level, BlockPos pos) {
        int hasBlockAbove = (level.getBlockState(pos.above()).getBlock() instanceof ReactorHeatExchanger) ? 1 : 0;
        int hasBlockBelow = (level.getBlockState(pos.below()).getBlock() instanceof ReactorHeatExchanger) ? 1 : 0;

        return switch (hasBlockAbove << 1 | hasBlockBelow) {
            case 0b01 -> VLinkTypes.DOWN;
            case 0b10 -> VLinkTypes.UP;
            case 0b11 -> VLinkTypes.BOTH;
            default -> VLinkTypes.NONE;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) :
                super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                           LevelAccessor level,
                                           BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState,
                        boolean movedByPiston) {
        if (!level.isClientSide()) {
            if (state.getBlock() != oldState.getBlock())
                level.setBlock(pos, state.setValue(V_LINK, retrieveVLinkType(state, level, pos)), 3);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }
}
