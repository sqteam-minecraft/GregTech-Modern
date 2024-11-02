package com.gregtechceu.gtceu.client.renderer.block;

import com.gregtechceu.gtceu.client.model.ReactorHeatVentModel;
import com.gregtechceu.gtceu.common.block.ReactorElement;
import com.gregtechceu.gtceu.common.block.ReactorHeatVent;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public record ReactorHeatVentRenderer(ReactorHeatVentModel model) implements IRenderer {
    public ReactorHeatVentRenderer(ReactorHeatVentModel model) {
        this.model = model;
        if (LDLib.isClient()) registerEvent();
    }

    @Override
    public void registerEvent() {}

    @Override
    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction side,
                                       RandomSource rand) {
        if (level == null || !(state.getBlock() instanceof ReactorHeatVent)) return model.bakeQuads(side);

        Direction.Axis axis = state.getValue(ReactorHeatVent.AXIS);

        BakedModel bakedModelCore = this.model.getCore(level,
                new Vec3i(axis == Direction.Axis.Z ? 90 : 0, 0, axis == Direction.Axis.X ? 90 : 0));

        if (bakedModelCore == null) return List.of();

        List<BakedModel> bakedModelLink = new ArrayList<>();
        List<BakedModel> bakedModelPlate = new ArrayList<>();
        List<BakedModel> bakedModelPlug = new ArrayList<>();

        List<Direction> plane = Arrays.stream(Direction.values())
                .filter(d -> d.getAxis() != axis)
                .toList();

        plane.forEach(d -> {
                //Vec3i rotation = new Vec3i(axis == Direction.Axis.Z ? 90 : 0,
                //        getModelYRot(d), axis == Direction.Axis.X ? 90 : 0);
            Vec3i rotation = new Vec3i(axis == Direction.Axis.Z ? 90 : 0, getModelYRot(d), axis == Direction.Axis.X ? 90 : 0);
                if (level.getBlockState(pos.relative(d)).getBlock() instanceof ReactorElement) {

                    bakedModelLink.add(this.model.getLink(level, rotation));
                    if (level.getBlockState(pos.relative(d.getCounterClockWise(axis))).getBlock()
                            instanceof ReactorElement)
                        bakedModelPlate.add(this.model.getPlates(level, rotation));
                }
                else bakedModelPlug.add(this.model.getPlug(level, rotation));
        });

        List<BakedQuad> quads = new ArrayList<>();

        quads.addAll(bakedModelCore.getQuads(state, side, rand));
//        quads.addAll(bakedModelPlate.stream()
//                .map(e -> e.getQuads(state, side, rand))
//                .flatMap(Collection::stream)
//                .toList());
        quads.addAll(bakedModelLink.stream()
                .map(e -> e.getQuads(state, side, rand))
                .flatMap(Collection::stream)
                .toList());
//        quads.addAll(bakedModelPlug.stream()
//                .map(e -> e.getQuads(state, side, rand))
//                .flatMap(Collection::stream)
//                .toList());
        return quads;
    }

    private int getModelYRot(Direction direction) {
        int angle = (int) (direction.toYRot());
        return angle % 180 == 0 ? Mth.abs(angle - 180) : angle;
    }
}