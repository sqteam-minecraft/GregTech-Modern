package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelConnector;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FissionFuelProperty;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import com.gregtechceu.gtceu.api.nuclear.HeatSource;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * A fuel rod block that acts as a reactor element and a fuel connector while also serving as a heat source.
 * It calculates its heat production based on a 2x2 cell grid where each cell stores a rod count,
 * with bonus multipliers based on interactions between adjacent cells.
 */
public class FuelRod extends ReactorElement implements IReactorFuelConnector, HeatSource {

    @Nullable
    private IFissionReactor reactor;

    // Constants for encoding rod counts in a 2x2 grid.
    private static final int NUM_CELLS = 4;
    private static final int BITS_PER_CELL = 2;
    private static final int CELL_MASK = (1 << BITS_PER_CELL) - 1;  // 0b11 equals 3
    private static final int MAX_RODS_STATE = (1 << (BITS_PER_CELL * NUM_CELLS)) - 1;  // 255 (0xFF)

    public static final StaticObjectProperty<FissionFuelProperty> FUEL_TYPE =
            StaticObjectProperty.create("fuel", FissionFuelProperty.class, FissionFuelProperty.getInstances());
    public static final IntegerProperty RODS = IntegerProperty.create("rods", 0, MAX_RODS_STATE);

    // Base heat multipliers per rod count (0, 1, 2, or 3 rods in a cell).
    private static final double[] HEAT_MULTIPLIERS = {0.0, 1.0, 2.5, 6.0};

    // Bonus interaction matrix: bonus = INTERACTION_MATRIX[currentCellRodCount][neighborCellRodCount].
    private static final double[][] INTERACTION_MATRIX = {
            {0.0,  0.0,   0.0,   0.0},
            {0.0,  0.05,  0.10,  0.25},
            {0.0,  0.10,  0.15,  0.30},
            {0.0,  0.25,  0.30,  0.45}
    };

    // Predefined neighbor indices for each cell in a 2x2 grid.
    private static final int[][] NEIGHBOR_INDICES = {
            {1, 2},  // Neighbors for cell 0.
            {0, 3},  // Neighbors for cell 1.
            {0, 3},  // Neighbors for cell 2.
            {1, 2}   // Neighbors for cell 3.
    };

    public FuelRod(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(RODS, 0)
                .setValue(FUEL_TYPE, FissionFuelProperty.getDefaultValue()));
    }

    /**
     * Provides the tint color for the fuel rod block on the client.
     *
     * @return A {@code BlockColor} that applies the fuel type's color.
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintColor() {
        return (state, reader, pos, tintIndex) -> {
            if (state.getBlock() instanceof FuelRod && tintIndex == 0) {
                return state.getValue(FUEL_TYPE).getFuel().getLayerARGB(0);
            }
            return -1;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RODS, FUEL_TYPE);
    }

    /**
     * Determines the vertical link type for fuel connectors by checking adjacent blocks.
     * Combines the presence of connectors above and below into a connection code.
     *
     * @param state The current block state.
     * @param level The level containing the block.
     * @param pos   The block position.
     * @return The vertical link type.
     */
    @Override
    public VLinkTypes retrieveVLinkType(BlockState state, Level level, BlockPos pos) {
        int hasAbove = (level.getBlockState(pos.above()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;
        int hasBelow = (level.getBlockState(pos.below()).getBlock() instanceof IReactorFuelConnector) ? 1 : 0;
        int connectionCode = (hasAbove << 1) | hasBelow;  // Bit 1: above, Bit 0: below

        return switch (connectionCode) {
            case 0b01 -> VLinkTypes.DOWN;
            case 0b10 -> VLinkTypes.UP;
            case 0b11 -> VLinkTypes.BOTH;
            default -> VLinkTypes.NONE;
        };
    }

    /**
     * Calculates the edge capacity between this reactor element and another.
     * The capacity is the minimum of this element's heat production and the other's throughput.
     *
     * @param state The current block state.
     * @param to    The target reactor element.
     * @return The edge capacity.
     */
    @Override
    public int calculateEdgeCapacity(BlockState state, IReactorElement to) {
        if (to == null) return 0;
        return Math.min(getHeatProduction(state), to.getHeatThroughput());
    }

    @Override
    public int getHeatThroughput() {
        return 0;
    }

    /**
     * Calculates the total heat production based on the current fuel rod state.
     * <p>
     * The fuel rod is divided into a 2x2 grid of cells. Each cell's rod count is stored in 2 bits.
     * For each cell, base heat is computed by multiplying the fuel's base production with a multiplier
     * corresponding to the rod count. A bonus is then applied based on the interaction with neighboring cells.
     * </p>
     *
     * @param state The block state.
     * @return The total heat production as an integer.
     */
    @Override
    public int getHeatProduction(BlockState state) {
        int rodsState = state.getValue(RODS);
        int baseProduction = state.getValue(FUEL_TYPE).getHeatProduces();

        double[] baseHeat = new double[NUM_CELLS];
        int[] rodsInCells = new int[NUM_CELLS];

        // Decode rod counts from the state (each cell uses 2 bits).
        for (int cell = 0; cell < NUM_CELLS; cell++) {
            int rodCount = (rodsState >> (cell * BITS_PER_CELL)) & CELL_MASK;
            rodsInCells[cell] = rodCount;
            baseHeat[cell] = baseProduction * HEAT_MULTIPLIERS[rodCount];
        }

        // Calculate total heat with bonus from neighboring interactions.
        double totalHeat = 0.0;
        for (int cell = 0; cell < NUM_CELLS; cell++) {
            double cellHeat = baseHeat[cell];
            double bonus = calculateBonus(rodsInCells[cell], NEIGHBOR_INDICES[cell], rodsInCells);
            totalHeat += cellHeat * (1.0 + bonus);
        }

        return (int) totalHeat;
    }

    /**
     * Calculates the bonus multiplier for a cell based on its rod count and that of its neighbors.
     *
     * @param rodsInCell      The rod count in the current cell.
     * @param neighborIndices The indices of neighboring cells.
     * @param rodsInCells     The array containing rod counts for all cells.
     * @return The bonus multiplier.
     */
    private double calculateBonus(int rodsInCell, int[] neighborIndices, int[] rodsInCells) {
        double bonus = 0.0;
        for (int neighborIndex : neighborIndices) {
            int neighborRods = rodsInCells[neighborIndex];
            bonus += INTERACTION_MATRIX[rodsInCell][neighborRods];
        }
        return bonus;
    }
}
