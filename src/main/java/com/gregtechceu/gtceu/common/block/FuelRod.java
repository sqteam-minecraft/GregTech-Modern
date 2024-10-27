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

public class FuelRod extends ReactorElement implements IReactorFuelConnector, HeatSource {

    @Nullable
    private IFissionReactor reactor;

    //public static final EnumProperty<ReactorFuel> FUEL_TYPE = EnumProperty.create("fuel", ReactorFuel.class);
    public static final StaticObjectProperty<FissionFuelProperty> FUEL_TYPE = StaticObjectProperty.create("fuel", FissionFuelProperty.class, FissionFuelProperty.getInstances());
    public static final IntegerProperty RODS = IntegerProperty.create("rods", 0, 0xFF);

    public FuelRod(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(RODS, 0)
                .setValue(FUEL_TYPE, FissionFuelProperty.getDefaultValue()));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RODS, FUEL_TYPE);
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

    @Override
    public int calculateEdgeCapacity(BlockState state, IReactorElement to) {
        if (to == null) return 0;
        return Math.min(getHeatProduction(state), to.getHeatThroughput());
    }

    @Override
    public int getHeatThroughput() {
        return 0;
    }

    // Constants for heat multipliers based on rod count
    private static final double[] HEAT_MULTIPLIERS = {0.0, 1.0, 2.5, 6.0}; // Index corresponds to rod count

    // Constant interaction matrix for bonuses between rod counts
    private static final double[][] INTERACTION_MATRIX = {
            // Neighbor rod counts: 0,    1,      2,      3
            /* rodsInCell = 0 */ {0.0,  0.0,    0.0,    0.0},    // 0 rods in cell
            /* rodsInCell = 1 */ {0.0,  0.05,   0.10,   0.25},   // 1 rod in cell
            /* rodsInCell = 2 */ {0.0,  0.10,   0.15,   0.30},   // 2 rods in cell
            /* rodsInCell = 3 */ {0.0,  0.25,   0.30,   0.45}    // 3 (4 rods) in cell
    };


    @Override
    public int getHeatProduction(BlockState state) {
        int rodsState = state.getValue(RODS);
        int fuelTypeBaseProduction = state.getValue(FUEL_TYPE).getHeatProduces();

        // Arrays to hold base heat and rod counts for each cell
        double[] baseHeat = new double[4];
        int[] rodsInCells = new int[4];

        // Calculate base heat and construct the rod count vector
        for (int i = 0; i < 4; i++) {
            int rodsInCell = (rodsState >> (i * 2)) & 0b11;
            rodsInCells[i] = rodsInCell;

            double cellMultiplier = HEAT_MULTIPLIERS[rodsInCell];
            baseHeat[i] = fuelTypeBaseProduction * cellMultiplier;
        }

        // Calculate bonuses using matrix multiplication
        double totalHeat = 0.0;
        for (int i = 0; i < 4; i++) {
            double cellHeat = baseHeat[i];

            // Get the indices of the neighboring cells
            int[] neighbors = getNeighbors(i);

            // Construct the rod count vector for the neighbors
            int[] neighborRodCounts = new int[neighbors.length];
            for (int j = 0; j < neighbors.length; j++) {
                neighborRodCounts[j] = rodsInCells[neighbors[j]];
            }

            // Calculate the bonus for this cell
            double bonus = calculateBonus(rodsInCells[i], neighborRodCounts);

            // Adjust the cell heat with the bonus
            cellHeat += cellHeat * bonus;

            // Add to total heat
            totalHeat += cellHeat;
        }

        return (int) totalHeat;
    }

    /**
     * Returns the indices of neighboring cells for a given cell index.
     */
    private int[] getNeighbors(int cellIndex) {
        // Define neighbor pairs for each cell (cells are in a 2x2 grid)
        return switch (cellIndex) {
            case 0 -> new int[]{1, 2}; // Cell 0 neighbors: Cell 1 and Cell 2
            case 1 -> new int[]{0, 3}; // Cell 1 neighbors: Cell 0 and Cell 3
            case 2 -> new int[]{0, 3}; // Cell 2 neighbors: Cell 0 and Cell 3
            case 3 -> new int[]{1, 2}; // Cell 3 neighbors: Cell 1 and Cell 2
            default -> new int[]{};
        };
    }

    /**
     * Calculates the bonus for a cell based on its rod count and the rod counts of its neighbors.
     */
    private double calculateBonus(int rodsInCell, int[] neighborRodCounts) {
        double bonus = 0.0;

        for (int rodsInNeighbor : neighborRodCounts) {
            // Use the interaction matrix to get the bonus coefficient
            double bonusCoefficient = INTERACTION_MATRIX[rodsInCell][rodsInNeighbor];
            bonus += bonusCoefficient; // Sum bonuses from all neighbors
        }

        return bonus;
    }
}
