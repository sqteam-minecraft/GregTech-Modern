package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import com.gregtechceu.gtceu.client.model.ReactorHeatVentModel;
import com.gregtechceu.gtceu.client.renderer.block.ReactorHeatVentRenderer;
import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ReactorHeatVent extends AppearanceBlock implements SimpleWaterloggedBlock, IBlockRendererProvider,
        IReactorElement {

    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class);

    private final ReactorHeatVentRenderer renderer;

    public ReactorHeatVent(Properties properties) {
        super(properties);

        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(AXIS, Direction.Axis.Y));
        renderer = new ReactorHeatVentRenderer(new ReactorHeatVentModel("nuclear/"));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.WATERLOGGED, AXIS));
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
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        if (!context.canPlace() || context.getPlayer() == null) return null;

        var state = defaultBlockState();
        if (context.getPlayer().isShiftKeyDown())
            return state.setValue(AXIS, context.getNearestLookingDirection().getAxis());

        return state.setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(AXIS, rotation.rotate(Direction
                .get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))).getAxis());
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(Direction
                .get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))));
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

    @Nullable
    @Override
    public IRenderer getRenderer(BlockState state) {
        return renderer;
    }

    @Override
    public int calculateEdgeCapacity(BlockState state, @Nullable IReactorElement to) {
        if (to == null) return 0;
        return Math.min(getHeatThroughput(), to.getHeatThroughput());
    }

    @Override
    public int getHeatThroughput() {
        return 30;
    }

    @Override
    @NotNull
    public List<BlockPos> getNetworkNeighbors(Map<BlockPos, IReactorElement> structure, BlockPos position, Level level) {
        List<BlockPos> neighbors = new ArrayList<>();
        Direction.Axis placementAxis = level.getBlockState(position).getValue(AXIS);

        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();

        // Check all possible adjacent directions.
        for (Direction dir : Direction.values()) {
            Vec3i axis = dir.getNormal();

            if (dir.getAxis() == placementAxis) continue;

            BlockPos neighborPos = new BlockPos(x + axis.getX(), y + axis.getY(), z + axis.getZ());
            if (structure.containsKey(neighborPos)) {
                neighbors.add(neighborPos);
            }
        }

        return neighbors;
    }
}
