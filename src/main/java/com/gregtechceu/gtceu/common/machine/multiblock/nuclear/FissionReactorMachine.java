package com.gregtechceu.gtceu.common.machine.multiblock.nuclear;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import com.gregtechceu.gtceu.api.capability.nuclear.ReactorFuel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import com.gregtechceu.gtceu.api.machine.multiblock.FissionReactorType;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.multiblock.part.nuclear.ReactorRedstoneControlHatch;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.states;

public class FissionReactorMachine extends WorkableMultiblockMachine
        implements IFissionReactor, IDisplayUIMachine, IDataInfoProvider
{
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FissionReactorMachine.class,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final int MAX_HEAT = 1000;
    public static final int MIN_HEAT = 0;

    public static final int MIN_RADIUS = 2;
    public static final int MIN_DEPTH = 2;

    @Persisted
    private int lDist = 0, rDist = 0, bDist = 0, fDist = 0, hDist = 0;
    @Nullable
    private FissionReactorType reactorType = null;
    @NotNull
    private Set<ReactorFuel> fuels;
    @Persisted
    @Getter
    @Setter
    private int heat = 0;
    @Getter
    @Setter
    private int excessHeat = 0;
    private int throttle = 15;
    // runtime
    @Getter
    @Nullable
    private Collection<IReactorElement> reactorElements;
    @Persisted
    private int tick = 0;
    private TickableSubscription subscription;
    private ReactorRedstoneControlHatch redstoneControl;

    public FissionReactorMachine(IMachineBlockEntity holder, FissionReactorType reactorType, Set<ReactorFuel> fuels) {
        super(holder);
        this.reactorType = reactorType;
        this.fuels = fuels;
    }

    //////////////////////////////////////
    // ****** Initialization ******//
    //////////////////////////////////////

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateHeatSubscription));
        }
    }

    protected void updateHeatSubscription() {
        if (heat > 0) {
            subscription = subscribeServerTick(subscription, this::updateHeat);
        } else if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    protected void updateHeat() {
        tick++;
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null) return;
        BlockPos pos = getPos();
        EnvironmentalHazardSavedData environmentalHazards = EnvironmentalHazardSavedData
                .getOrCreate(level);
//            environmentalHazards.addZone(level.getChunk(pos).getPos(),
//                    new EnvironmentalHazardSavedData.HazardZone(pos, 1, true,
//                            HazardProperty.HazardTrigger.ANY, GTMedicalConditions.CARCINOGEN));
        if (redstoneControl != null) {
            int signal = redstoneControl.getRedstoneSignalStrength();
            this.throttle = 15 - signal;
        }

        if (throttle != 15 && tick % (throttle + 1) == 0)
            heat = Mth.clamp(heat + 1, MIN_HEAT, MAX_HEAT);

        if (heat >= MAX_HEAT) {
            // todo meltdown
        }
    }

    @Nullable
    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params,
                                          @NotNull OCResult result) {
        if (!(machine instanceof FissionReactorMachine reactorMachine))
            return null;

        var heatToStart = recipe.data.contains("heat_to_start") ? recipe.data.getInt("heat_to_start") : 0;

        // get the excess heat over the required heat to start the recipe
        reactorMachine.excessHeat = reactorMachine.heat - heatToStart;

        // if not yet hot enough, don't run the recipe
        if (reactorMachine.excessHeat <= 0)
            return null;

        int limitMaxParallel = 10000;
        int singleHeatConsumption = 1;

        // get the amount of parallel required to match the speed of heat generation
        int maxParallel = Mth.clamp(reactorMachine.excessHeat * singleHeatConsumption, 1, limitMaxParallel);

        var parallelResult = GTRecipeModifiers.fastParallel(reactorMachine, recipe, maxParallel, false);

        recipe = new GTRecipe(recipe.recipeType, recipe.id,
                recipe.copyContents(recipe.inputs, ContentModifier.multiplier(parallelResult.getSecond())),
                recipe.copyContents(recipe.outputs, ContentModifier.multiplier(parallelResult.getSecond())),
                recipe.tickInputs, recipe.tickOutputs, recipe.inputChanceLogics, recipe.outputChanceLogics,
                recipe.tickInputChanceLogics, recipe.tickOutputChanceLogics, recipe.conditions,
                recipe.ingredientActions,
                recipe.data, recipe.duration, false);

        reactorMachine.heat = Math.max(reactorMachine.heat - singleHeatConsumption * parallelResult.getSecond(), MIN_HEAT);

        result.init(0, recipe.duration, parallelResult.getSecond(), params.getOcAmount());

        return recipe;
    }

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////

    protected void initializeAbilities() {
        for (IMultiPart part : getParts()) {
            if (part instanceof ReactorRedstoneControlHatch hatch) {
                this.redstoneControl = hatch;
            }
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        initializeAbilities();

        if (reactorElements != null) {
            this.reactorElements.forEach(element -> element.setReactor(null));
            this.reactorElements = null;
        }
        Set<IReactorElement> elements = getMultiblockState().getMatchContext().getOrCreate("reactorElement",
                Sets::newHashSet);
        this.reactorElements = ImmutableSet.copyOf(elements);
        this.reactorElements.forEach(element -> element.setReactor(this));
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.heat = MIN_HEAT;
        if (reactorElements != null) {
            this.reactorElements.forEach(element -> element.setReactor(null));
            this.reactorElements = null;
        }
    }

    @Override
    public boolean shouldAddPartToController(IMultiPart part) {
        var cache = getMultiblockState().getCache();
        for (Direction side : GTUtil.DIRECTIONS) {
            if (!cache.contains(part.self().getPos().relative(side))) {
                return true;
            }
        }
        return false;
    }

    public void updateStructureDimensions() {
        Level world = getLevel();
        if (world == null) return;

        Direction front = getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = left.getOpposite();

        BlockPos.MutableBlockPos lPos = getPos().mutable();
        BlockPos.MutableBlockPos rPos = getPos().mutable();
        BlockPos.MutableBlockPos fPos = getPos().mutable();
        BlockPos.MutableBlockPos bPos = getPos().mutable();
        BlockPos.MutableBlockPos hPos = getPos().mutable();

        // find the distances from the controller to the plascrete blocks on one horizontal axis and the Y axis
        // repeatable aisles take care of the second horizontal axis
        int lDist = 0;
        int rDist = 0;
        int bDist = 0;
        int fDist = 0;
        int hDist = 0;

        // find the left, right, back, and front distances for the structure pattern
        // maximum size is 15x15x15 including walls, so check 7 block radius around the controller for blocks
        for (int i = 1; i < 8; i++) {
            if (lDist == 0 && isBlockEdge(world, lPos, left)) lDist = i;
            if (rDist == 0 && isBlockEdge(world, rPos, right)) rDist = i;
            if (bDist == 0 && isBlockEdge(world, bPos, back)) bDist = i;
            if (fDist == 0 && isBlockEdge(world, fPos, front)) fDist = i;
            if (lDist != 0 && rDist != 0 && bDist != 0 && fDist != 0) break;
        }

        // height is diameter instead of radius, so it needs to be done separately
        for (int i = 1; i < 15; i++) {
            if (isBlockFloor(world, hPos, Direction.DOWN)) hDist = i;
            if (hDist != 0) break;
        }

        if (lDist < MIN_RADIUS || rDist < MIN_RADIUS || bDist < MIN_RADIUS || fDist < MIN_RADIUS || hDist < MIN_DEPTH) {
            this.isFormed = false;
            return;
        }

        this.lDist = lDist;
        this.rDist = rDist;
        this.bDist = bDist;
        this.fDist = fDist;
        this.hDist = hDist;
    }

    /**
     * @param world     the world to check
     * @param pos       the pos to check and move
     * @param direction the direction to move
     * @return if a block is a valid wall block at pos moved in direction
     */
    public boolean isBlockEdge(@NotNull Level world, @NotNull BlockPos.MutableBlockPos pos,
                               @NotNull Direction direction) {
        assert reactorType != null; // todo remove nulls
        return world.getBlockState(pos.move(direction)) == reactorType.getCasing().getDefaultState();
    }

    /**
     * @param world     the world to check
     * @param pos       the pos to check and move
     * @param direction the direction to move
     * @return if a block is a valid floor block at pos moved in direction
     */
    public boolean isBlockFloor(@NotNull Level world, @NotNull BlockPos.MutableBlockPos pos,
                                @NotNull Direction direction) {
        if (isBlockEdge(world, pos, direction)) return true;
        assert reactorType != null; // todo remove nulls
        return world.getBlockState(pos) == reactorType.getWall().getDefaultState();
    }

    @NotNull
    @Override
    public BlockPattern getPattern() {
        // return the default structure, even if there is no valid size found
        // this means auto-build will still work, and prevents terminal crashes.
        if (getLevel() != null) updateStructureDimensions();

        // these can sometimes get set to 0 when loading the game, breaking JEI
        if (lDist < MIN_RADIUS) lDist = MIN_RADIUS;
        if (rDist < MIN_RADIUS) rDist = MIN_RADIUS;
        if (bDist < MIN_RADIUS) bDist = MIN_RADIUS;
        if (fDist < MIN_RADIUS) fDist = MIN_RADIUS;
        if (hDist < MIN_DEPTH) hDist = MIN_DEPTH;

        if (this.getFrontFacing() == Direction.EAST || this.getFrontFacing() == Direction.WEST) {
            int tmp = lDist;
            lDist = rDist;
            rDist = tmp;
        }

        // build each row of the structure
        StringBuilder borderBuilder = new StringBuilder();     // BBBBB
        StringBuilder wallBuilder = new StringBuilder();       // BWWWB
        StringBuilder insideBuilder = new StringBuilder();     // W   W
        StringBuilder controllerBuilder = new StringBuilder(); // BWCWB
        StringBuilder centerBuilder = new StringBuilder();     // BWKWB

        // everything to the left of the controller
        for (int i = 0; i < lDist; i++) {
            borderBuilder.append("B");
            if (i == 0) {
                wallBuilder.append("B");
                insideBuilder.append("W");
                controllerBuilder.append("B");
                centerBuilder.append("B");
            } else {
                insideBuilder.append(" ");
                wallBuilder.append("W");
                controllerBuilder.append("W");
                centerBuilder.append("W");
            }
        }

        // everything in-line with the controller
        borderBuilder.append("B");
        wallBuilder.append("W");
        insideBuilder.append(" ");
        controllerBuilder.append("C");
        centerBuilder.append("K");

        // everything to the right of the controller
        for (int i = 0; i < rDist; i++) {
            borderBuilder.append("B");
            if (i == rDist - 1) {
                wallBuilder.append("B");
                insideBuilder.append("W");
                controllerBuilder.append("B");
                centerBuilder.append("B");
            } else {
                insideBuilder.append(" ");
                wallBuilder.append("W");
                controllerBuilder.append("W");
                centerBuilder.append("W");
            }
        }

        // build each slice of the structure
        String[] wall = new String[hDist + 1]; // "BBBBB", "BWWWB", "BWWWB", "BWWWB", "BBBBB"
        Arrays.fill(wall, wallBuilder.toString());
        wall[0] = borderBuilder.toString();
        wall[wall.length - 1] = borderBuilder.toString();

        String[] slice = new String[hDist + 1]; // "BWWWB", "W   W", "W   W", "W   W", "BWWWB"
        Arrays.fill(slice, insideBuilder.toString());
        slice[0] = wallBuilder.toString();
        slice[slice.length - 1] = wallBuilder.toString();

        String[] center = Arrays.copyOf(slice, slice.length); // "BWKWB", "W   W", "W   W", "W   W", "BWCWB"
        if (this.getFrontFacing() == Direction.NORTH || this.getFrontFacing() == Direction.SOUTH) {
            center[0] = centerBuilder.reverse().toString();
            center[center.length - 1] = controllerBuilder.reverse().toString();
        } else {
            center[0] = centerBuilder.toString();
            center[center.length - 1] = controllerBuilder.toString();
        }

        TraceabilityPredicate wallPredicate = states(getWallState());
        TraceabilityPredicate basePredicate =
                Predicates.autoAbilities(true, false, false)
                        .or(abilities(PartAbility.REACTOR_REDSTONE_CONTROL).setMaxGlobalLimited(1));

        // layer the slices one behind the next
        return FactoryBlockPattern.start()
                .aisle(wall)
                .aisle(slice).setRepeatable(bDist - 1)
                .aisle(center)
                .aisle(slice).setRepeatable(fDist - 1)
                .aisle(wall)
                .where('C', Predicates.controller(Predicates.blocks(this.getDefinition().get())))
                .where('B', states(getCasingState()).or(basePredicate))
                .where('W', wallPredicate
                        .or(abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setMaxGlobalLimited(8))
                        .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setMaxGlobalLimited(8)))
                .where('K', wallPredicate) // the block beneath the controller must only be a casing for structure
                // dimension checks
                .where(' ', innerPredicate().or(states(getCasingState())))
                .build();
    }

    @NotNull
    protected BlockState getCasingState() { return GTBlocks.CASING_STEEL_SOLID.getDefaultState(); }

    @NotNull
    protected BlockState getWallState() {
        return GTBlocks.CASING_REINFORCED_BOROSILICATE_GLASS.getDefaultState();
    }

    @NotNull
    protected TraceabilityPredicate innerPredicate() {
        return new TraceabilityPredicate(blockWorldState -> {
            Set<IReactorElement> elements = blockWorldState.getMatchContext().getOrCreate("reactorElement",
                    Sets::newHashSet);
            // all non-GTMachines are allowed inside by default
            BlockState block = blockWorldState.getBlockState();
//            if (blockEntity != null) {
//                var element = GTCapabilityHelper.getReactorElement(blockWorldState.getWorld(),
//                        blockWorldState.getPos(), null);
//                if (element != null) {
//                    elements.add(element);
//                }
//            }
            return true;
        }, null) {

            @Override
            public boolean isAny() {
                return true;
            }

            @Override
            public boolean addCache() {
                return true;
            }
        };
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (isFormed()) {
            if (!isWorkingEnabled()) {
                textList.add(Component.translatable("gtceu.multiblock.work_paused"));

            } else if (isActive()) {
                textList.add(Component.translatable("gtceu.multiblock.running"));
                int currentProgress = (int) (recipeLogic.getProgressPercent() * 100);
                textList.add(Component.translatable("gtceu.multiblock.progress", currentProgress));
            } else {
                textList.add(Component.translatable("gtceu.multiblock.idling"));
            }

            if (recipeLogic.isWaiting()) {
                textList.add(Component.translatable("gtceu.multiblock.waiting")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }
            textList.add(Component.translatable("gtceu.multiblock.fission_reactor.heat", this.heat));
        } else {
            Component tooltip = Component.translatable("gtceu.multiblock.invalid_structure.tooltip")
                    .withStyle(ChatFormatting.GRAY);
            textList.add(Component.translatable("gtceu.multiblock.invalid_structure")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
    }

    @NotNull
    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode == PortableScannerBehavior.DisplayMode.SHOW_ALL ||
                mode == PortableScannerBehavior.DisplayMode.SHOW_MACHINE_INFO) {
            return Collections.singletonList(Component.translatable(
                    "gtceu.multiblock.fission_reactor.throttle", 100 - (int) Math.floor(this.throttle / 0.15)));
        }
        return new ArrayList<>();
    }

    @Override
    public Set<FissionReactorType> getTypes() {
        return this.reactorType == null ? Set.of() : Set.of(this.reactorType);
    }
}
