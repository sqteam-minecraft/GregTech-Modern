package com.gregtechceu.gtceu.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@Getter
public class LargeHeatExchanger extends WorkableMultiblockMachine implements IExplosionMachine, IDisplayUIMachine
{
    private FluidHatchPartMachine hotInput;
    private FluidHatchPartMachine coldInput;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LargeHeatExchanger.class,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);


    public LargeHeatExchanger(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IGuiTexture getScreenTexture() {
        return GuiTextures.DISPLAY_STEAM.get(true);
    }

    @Override
    public void onStructureFormed() {
        getDefinition().setPartSorter(Comparator.comparing(it -> multiblockPartSorter().apply(it.self().getPos())));
        super.onStructureFormed();

        Queue<FluidHatchPartMachine> io = new LinkedList<>();
        for (IMultiPart part : getParts()) {
            if (part instanceof FluidHatchPartMachine hatch) {
                io.add(hatch);
            }
        }

        hotInput = io.poll();
        io.poll();
        coldInput = io.poll();
    }

    private Function<BlockPos, Integer> multiblockPartSorter() {
        return RelativeDirection.RIGHT.getSorter(getFrontFacing(), getUpwardsFacing(), isFlipped());
    }

    @Nullable
    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params,
                                          @NotNull OCResult result) {
        if (!(machine instanceof LargeHeatExchanger heatExchanger))
            return null;

        var hotInputHatch = heatExchanger.getHotInput();
        var coldInputHatch = heatExchanger.getColdInput();

        var inputFluids = RecipeHelper.getInputFluids(recipe);

        var hotInputFluid = inputFluids.get(0);
        var coldInputFluid = inputFluids.get(1);
        if (hotInputFluid.getFluid().getFluidType().getTemperature() < coldInputFluid.getFluid().getFluidType().getTemperature()) {
            hotInputFluid = inputFluids.get(1);
            coldInputFluid = inputFluids.get(0);
        }

        var hotInputFluidType = hotInputHatch.tank.getFluidInTank(0).getFluid().getFluidType();
        var coldInputFluidType = coldInputHatch.tank.getFluidInTank(0).getFluid().getFluidType();

        if (hotInputFluidType == hotInputFluid.getFluid().getFluidType() &&
                coldInputFluidType == coldInputFluid.getFluid().getFluidType()) {
            result.init(0, recipe.duration, 1, params.getOcAmount());
            return recipe;
        }

        return null;
    }
}
