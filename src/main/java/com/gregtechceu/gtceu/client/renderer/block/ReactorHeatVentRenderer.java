package com.gregtechceu.gtceu.client.renderer.block;

import com.gregtechceu.gtceu.client.model.ReactorHeatVentModel;
import com.gregtechceu.gtceu.common.block.ReactorElement;
import com.gregtechceu.gtceu.common.block.ReactorHeatVent;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public record ReactorHeatVentRenderer(ReactorHeatVentModel model) implements IRenderer {

    public ReactorHeatVentRenderer(ReactorHeatVentModel model) {
        this.model = model;
        if (LDLib.isClient()) {
            registerEvent();
        }
    }

    @Override
    public void registerEvent() {}

    @Override
    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction side,
                                       RandomSource rand) throws IllegalStateException {
        if (level == null || !(state.getBlock() instanceof ReactorHeatVent)) {
            return Collections.emptyList();
        }

        // Get the primary axis from the block state
        Direction.Axis axis = state.getValue(ReactorHeatVent.AXIS);

        // Build the transformation for the core model
        Matrix4f coreTransform = new Matrix4f()
                .translate(0.5f, 0.5f, 0.5f)
                .mul(getRotationMatrix(new Vec3i(axis == Direction.Axis.Z ? 90 : 0, 0, axis == Direction.Axis.X ? 90 : 0)))
                .translate(-0.5f, -0.5f, -0.5f);
        BakedModel bakedModelCore = this.model.getCore(level, coreTransform);
        if (bakedModelCore == null) {
            return List.of();
        }

        List<BakedModel> bakedModelLink = new ArrayList<>();
        List<BakedModel> bakedModelPlate = new ArrayList<>();
        List<BakedModel> bakedModelPlug = new ArrayList<>();

        // Process each direction perpendicular to the primary axis
        List<Direction> plane = Arrays.stream(Direction.values())
                .filter(d -> d.getAxis() != axis)
                .toList();

        plane.forEach(direction -> {
            Matrix4f rotation = getRotationForAxisAndDirection(axis, direction);
            BlockPos relativePos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(relativePos);

            if (neighborState.getBlock() instanceof ReactorElement) {
                bakedModelLink.add(this.model.getLink(level, rotation));

                // Check for adjacent ReactorElement for the plate model
                BlockPos platePos = pos.relative(direction.getCounterClockWise(axis));
                if (level.getBlockState(platePos).getBlock() instanceof ReactorElement) {
                    bakedModelPlate.add(this.model.getPlates(level, rotation));
                }
            } else {
                bakedModelPlug.add(this.model.getPlug(level, rotation));
            }
        });

        List<BakedQuad> quads = new ArrayList<>(bakedModelCore.getQuads(state, side, rand));
        bakedModelPlate.forEach(model -> quads.addAll(model.getQuads(state, side, rand)));
        bakedModelLink.forEach(model -> quads.addAll(model.getQuads(state, side, rand)));
        bakedModelPlug.forEach(model -> quads.addAll(model.getQuads(state, side, rand)));

        return quads;
    }

    /**
     * Calculates the adjusted Y rotation for the given direction.
     *
     * @param direction The direction for which to calculate the rotation.
     * @return The adjusted rotation angle.
     */
    private int getModelYRot(Direction direction) {
        int angle = (int) direction.toYRot();
        return angle % 180 == 0 ? Mth.abs(angle - 180) : angle;
    }

    /**
     * Returns a rotation matrix based on a vector of rotation angles (in degrees) along x, y, and z.
     *
     * @param rotation A Vec3i representing rotation angles in degrees.
     * @return The rotation matrix.
     */
    private Matrix4f getRotationMatrix(Vec3i rotation) {
        Vector3f angleRadians = new Vector3f(
                rotation.getX() * Mth.DEG_TO_RAD,
                rotation.getY() * Mth.DEG_TO_RAD,
                rotation.getZ() * Mth.DEG_TO_RAD
        );
        Quaternionf quaternionXZ = new Quaternionf().rotateXYZ(angleRadians.x(), 0, angleRadians.z());
        Quaternionf quaternionY = new Quaternionf().rotateY(angleRadians.y());
        return new Matrix4f().rotate(quaternionY).rotate(quaternionXZ);
    }

    /**
     * Builds the transformation matrix for a given primary axis and a perpendicular direction.
     *
     * @param axis      The primary axis of the heat vent.
     * @param direction The perpendicular direction for which the rotation is applied.
     * @return The transformation matrix.
     */
    private Matrix4f getRotationForAxisAndDirection(Direction.Axis axis, Direction direction) {
        Matrix4f matrix = new Matrix4f().identity();
        matrix.translate(0.5f, 0.5f, 0.5f);

        switch (axis) {
            case Y -> {
                int angle = getModelYRot(direction);
                matrix.mul(getRotationMatrix(new Vec3i(0, angle, 0)));
            }
            case Z -> {
                switch (direction) {
                    case UP -> matrix.rotateAffineXYZ(0.5f * Mth.PI, 0, 0);
                    case WEST -> matrix.rotateAffineXYZ(0.5f * Mth.PI, 0.5f * Mth.PI, 0);
                    case DOWN -> matrix.rotateAffineXYZ(1.5f * Mth.PI, 0, Mth.PI);
                    case EAST -> matrix.rotateAffineYXZ(1.5f * Mth.PI, 0, 1.5f * Mth.PI);
                    default -> throw new IllegalStateException("Unexpected value: " + direction);
                }
            }
            case X -> {
                switch (direction) {
                    case UP -> matrix.rotateAffineXYZ(0.5f * Mth.PI, 0, 0.5f * Mth.PI);
                    case SOUTH -> matrix.rotateAffineXYZ(0, Mth.PI, 1.5f * Mth.PI);
                    case DOWN -> matrix.rotateAffineYXZ(0.5f * Mth.PI, 1.5f * Mth.PI, 0);
                    case NORTH -> matrix.rotateAffineZYX(0.5f * Mth.PI, 0, 0);
                    default -> throw new IllegalStateException("Unexpected value: " + direction);
                }
            }
        }

        matrix.translate(-0.5f, -0.5f, -0.5f);
        return matrix;
    }
}
