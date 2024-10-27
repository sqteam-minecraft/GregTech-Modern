package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class ReactorHeatVent extends AppearanceBlock implements SimpleWaterloggedBlock {
    public ReactorHeatVent(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.WATERLOGGED));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) :
                super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction,
                                           @NotNull BlockState neighborState,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos,
                                           @NotNull BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @OnlyIn(Dist.CLIENT)
    public static Supplier<BlockColor> tintColor(int pipeColor, int plateColor) {
        return () -> (state, reader, pos, tintIndex) -> {
            if (state.getBlock() instanceof ReactorHeatVent) {
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
}
