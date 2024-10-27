package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ReactorHeatExchanger extends ReactorElement {

    public ReactorHeatExchanger(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    public static Supplier<BlockColor> tintColor(int pipeColor, int plateColor) {
        return () -> (state, reader, pos, tintIndex) -> {
            if (state.getBlock() instanceof ReactorHeatExchanger) {
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
    public int calculateEdgeCapacity(BlockState state, IReactorElement to) {
        if (to == null) return getMaxHeatToConsume();
        return 0;
    }

    private int getMaxHeatToConsume() {
        return 5;
    }

    @Override
    public int getHeatThroughput() {
        return 10;
    }
}
