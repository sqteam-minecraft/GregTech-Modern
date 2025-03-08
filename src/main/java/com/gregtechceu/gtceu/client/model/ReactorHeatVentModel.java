package com.gregtechceu.gtceu.client.model;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReactorHeatVentModel {

    private static final Logger log = LoggerFactory.getLogger(ReactorHeatVentModel.class);

    private final String path;

    private final HashMap<Matrix4f, BakedModel> core = new HashMap<>();
    private final HashMap<Matrix4f, BakedModel> link = new HashMap<>();
    private final HashMap<Matrix4f, BakedModel> plates = new HashMap<>();
    private final HashMap<Matrix4f, BakedModel> plug = new HashMap<>();

    private final Map<String, ResourceLocation> override = new HashMap<>();

    public ReactorHeatVentModel(String path) {
        this.path = path;
    }

    @SuppressWarnings("deprecation")
    public BakedModel bakeAndTransform(BlockModel blockModel, Matrix4f matrix, ResourceLocation location) {
        Transformation transformation = new Transformation(matrix);
        blockModel.customData.setRootTransform(transformation);

        log.debug("Baking model {}", location.withSuffix("_" + matrix.hashCode()));
        return blockModel.bake(
                ModelFactory.getModeBaker(),
                new SpriteOverrider(override),
                BlockModelRotation.X0_Y0,
                location.withSuffix("_" + matrix.hashCode()));
    }

    private BakedModel getReactorHeatVentBakedModel(BlockAndTintGetter level, Matrix4f rotation, String suffix,
                                                    HashMap<Matrix4f, BakedModel> map) {
        if (level != null && !map.containsKey(rotation)) {
            UnbakedModel unbakedModel = ModelFactory.getModeBakery().getModel(ResourceLocation.of(
                    String.format("gtceu:block/%sreactor_heat_vent%s", this.path, suffix), ':'));
            map.put(rotation, bakeAndTransform((BlockModel) unbakedModel, rotation,
                    ResourceLocation.of(
                            String.format("gtceu:block/%sreactor_heat_vent%s", this.path, suffix), ':')));
        }

        return map.get(rotation);
    }

    public synchronized BakedModel getCore(BlockAndTintGetter level, Matrix4f rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "", core);
    }

    public synchronized BakedModel getLink(BlockAndTintGetter level, Matrix4f rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_link", link);
    }

    public synchronized BakedModel getPlates(BlockAndTintGetter level, Matrix4f rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_plate", plates);
    }

    public synchronized BakedModel getPlug(BlockAndTintGetter level, Matrix4f rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_plug", plug);
    }
}