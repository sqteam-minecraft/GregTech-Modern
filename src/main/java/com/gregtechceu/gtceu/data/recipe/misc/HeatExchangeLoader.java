package com.gregtechceu.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.HEAT_EXCHANGE_RECIPES;

public class HeatExchangeLoader
{
    public static void init(Consumer<FinishedRecipe> provider) {
        HEAT_EXCHANGE_RECIPES.recipeBuilder("superheated_steam_and_water_heat_exchange")
                .inputFluids(GTMaterials.DistilledWater.getFluid(100))
                .inputFluids(GTMaterials.SuperheatedSteam.getFluid(25))
                .outputFluids(GTMaterials.Steam.getFluid(16000))
                .outputFluids(GTMaterials.DistilledWater.getFluid(25))
                .duration(2)
                .save(provider);
    }
}
