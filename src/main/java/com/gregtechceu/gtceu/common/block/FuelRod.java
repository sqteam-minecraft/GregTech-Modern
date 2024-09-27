package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelConnector;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;
import com.gregtechceu.gtceu.api.capability.nuclear.ReactorFuel;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelRod extends AppearanceBlock implements IReactorFuelRod {
    @Persisted
    @Getter
    public double heat;
    @Nullable
    private IFissionReactor reactor;

    public static final EnumProperty<VLinkTypes> V_LINK = EnumProperty.create("vlink", VLinkTypes.class);

    public static final IntegerProperty RODS = IntegerProperty.create("rods", 0, 0xFF);

    public FuelRod(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(V_LINK, VLinkTypes.NONE)
                .setValue(RODS, 0b11_10_01_00));
    }


    @Override
    public int getUses() {
        return 0;
    }

    @Override
    public void addHeat(double heat)
    {
        this.heat += heat;
    }

    @Override
    public void removeHeat(double heat)
    {
        this.heat -= heat;
    }

    @Nullable
    @Override
    public IFissionReactor getFissionReactor()
    {
        return reactor;
    }

    @Override
    public void setReactor(IFissionReactor reactor)
    {
        this.reactor = reactor;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(V_LINK, RODS));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            if (state.getBlock() != oldState.getBlock())
                updateVLinkState(state.setValue(RODS, level.random.nextInt(0, 0xFF)), level, pos);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateVLinkState(state, level, pos);
        }
    }

    private void updateVLinkState(BlockState state, Level level, BlockPos pos) {
        int hasBlockAbove = (level.getBlockState(pos.above()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;
        int hasBlockBelow = (level.getBlockState(pos.below()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;
        VLinkTypes type =  switch (hasBlockAbove<<1 | hasBlockBelow) {
            case 0b01 -> VLinkTypes.DOWN;
            case 0b10 -> VLinkTypes.UP;
            case 0b11 -> VLinkTypes.BOTH;
            default -> VLinkTypes.NONE;
        };
        //if(state.getValue(RODS)==0b11_10_01_00){
        //    level.setBlock(pos, state.setValue(V_LINK, type).setValue(RODS, level.random.nextInt(0, 0xFF)), 3);
        //}
        level.setBlock(pos, state.setValue(V_LINK, type), 3);
    }

    public enum VLinkTypes implements StringRepresentable {
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
