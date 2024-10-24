package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelConnector;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;
import com.gregtechceu.gtceu.api.capability.nuclear.ReactorFuel;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelRod extends AppearanceBlock implements IReactorFuelRod,
                     SimpleWaterloggedBlock, VerticalModelLinkBlock {

    @Persisted
    @Getter
    public double heat;
    @Nullable
    private IFissionReactor reactor;

    public static final EnumProperty<ReactorFuel> FUEL_TYPE = EnumProperty.create("fuel", ReactorFuel.class);
    public static final IntegerProperty RODS = IntegerProperty.create("rods", 0, 0xFF);

    public FuelRod(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(RODS, 0)
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(FUEL_TYPE, ReactorFuel.URANIUM)
                .setValue(V_LINK, VLinkTypes.NONE));
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintColor() {
        return (state, reader, pos, tintIndex) -> {
            if (state.getBlock() instanceof FuelRod) {
                if (tintIndex == 0) return state.getValue(FUEL_TYPE).getFuel().getLayerARGB(0);
            }
            return -1;
        };
    }

    @Override
    public int getUses() {
        return 0;
    }

    @Override
    public void addHeat(double heat) {
        this.heat += heat;
    }

    @Override
    public void removeHeat(double heat) {
        this.heat -= heat;
    }

    @Nullable
    @Override
    public IFissionReactor getAssignedReactor() {
        return reactor;
    }

    @Override
    public void assignToReactor(@Nullable IFissionReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(RODS, FUEL_TYPE, BlockStateProperties.WATERLOGGED, V_LINK));
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
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state.setValue(RODS, level.random.nextInt(0, 0xFF)).setValue(FUEL_TYPE,
                ReactorFuel.values()[level.random.nextInt(0, ReactorFuel.values().length)])
                .setValue(V_LINK, retrieveVLinkType(state, level, pos)), level, pos, oldState, movedByPiston);
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
    public VLinkTypes retrieveVLinkType(BlockState state, Level level, BlockPos pos) {
        int hasBlockAbove = (level.getBlockState(pos.above()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;
        int hasBlockBelow = (level.getBlockState(pos.below()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;

        return switch (hasBlockAbove << 1 | hasBlockBelow) {
            case 0b01 -> VLinkTypes.DOWN;
            case 0b10 -> VLinkTypes.UP;
            case 0b11 -> VLinkTypes.BOTH;
            default -> VLinkTypes.NONE;
        };
    }
}
