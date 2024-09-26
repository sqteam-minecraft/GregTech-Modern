package com.gregtechceu.gtceu.data.recipe.misc.nuclear;

import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.FISSION_REACTOR_RECIPES;

public class FissionLoader {
    public static void init(Consumer<FinishedRecipe> provider) {
        FISSION_REACTOR_RECIPES.recipeBuilder("fission_distilled_water_to_superheated_steam")
                .inputFluids(GTMaterials.DistilledWater.getFluid(5))
                .outputFluids(GTMaterials.SuperheatedSteam.getFluid(5))
                .duration(20)
                .fissionStartHeat(200)
                .save(provider);
    }
}
