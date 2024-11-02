package com.gregtechceu.gtceu.client.model;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReactorHeatVentModel {

    private static final Logger log = LoggerFactory.getLogger(ReactorHeatVentModel.class);

    private final String path;

    private final HashMap<Vec3i, BakedModel> core = new HashMap<>();
    private final HashMap<Vec3i, BakedModel> link = new HashMap<>();
    private final HashMap<Vec3i, BakedModel> plates = new HashMap<>();
    private final HashMap<Vec3i, BakedModel> plug = new HashMap<>();

    private final Map<String, ResourceLocation> override = new HashMap<>();

    public ReactorHeatVentModel(String path) {
        this.path = path;
    }

    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> bakeQuads(@Nullable Direction side) {
        List<BakedQuad> quads = new ArrayList<>();
        //quads.add(FaceQuad.builder(side, sprite).cube(core).tintIndex(0).bake())
        return Collections.emptyList();
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

    private BakedModel getReactorHeatVentBakedModel(BlockAndTintGetter level, Vec3i rotation, String suffix,
                                                    HashMap<Vec3i, BakedModel> map) {
        if (level != null && !map.containsKey(rotation)) {
            UnbakedModel unbakedModel = ModelFactory.getModeBakery().getModel(ResourceLocation.of(
                    String.format("gtceu:block/%sreactor_heat_vent%s", this.path, suffix), ':'));
            map.put(rotation, bakeAndTransform((BlockModel) unbakedModel,
                    new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .mul(getRotationMatrix(rotation))
                            .mul(new Matrix4f().translate(-0.5f, -0.5f, -0.5f)),
                    ResourceLocation.of(
                            String.format("gtceu:block/%sreactor_heat_vent%s", this.path, suffix), ':')));
        }

        return map.get(rotation);
    }

    public synchronized BakedModel getCore(BlockAndTintGetter level, Vec3i rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "", core);
    }

    public synchronized BakedModel getLink(BlockAndTintGetter level, Vec3i rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_link", link);
    }

    public synchronized BakedModel getPlates(BlockAndTintGetter level, Vec3i rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_plate", plates);
    }

    public synchronized BakedModel getPlug(BlockAndTintGetter level, Vec3i rotation) {
        return getReactorHeatVentBakedModel(level, rotation, "_plug", plug);
    }

    /**
     * Get the rotation matrix for the given rotation
     * @param axis the axis of rotation
     * @param rotation the rotation in degrees
     * @return the rotation matrix
     */
    private Matrix4f getRotationMatrix(Vec3i rotation) {
        var angleRadians = new Vector3f(
                rotation.getX() * Mth.DEG_TO_RAD,
                rotation.getY() * Mth.DEG_TO_RAD,
                rotation.getZ() * Mth.DEG_TO_RAD
        );
        //var normal = Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal();
        Quaternionf q4f = new Quaternionf().rotateXYZ(angleRadians.x(), angleRadians.y(), angleRadians.z());
        return new Matrix4f().rotate(q4f);
    }
}