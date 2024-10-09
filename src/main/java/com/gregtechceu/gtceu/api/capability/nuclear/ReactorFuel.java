package com.gregtechceu.gtceu.api.capability.nuclear;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ReactorFuel implements StringRepresentable {
    THORIUM(GTMaterials.ThoriumFissionFuel),
    URANIUM(GTMaterials.UraniumFissionFuel),
    PLUTONIUM(GTMaterials.PlutoniumFissionFuel);

    private final Material fuel;

    ReactorFuel(Material fuel) { this.fuel = fuel; }

    @Override
    public @NotNull String getSerializedName() { return this.name().toLowerCase(); }
}
