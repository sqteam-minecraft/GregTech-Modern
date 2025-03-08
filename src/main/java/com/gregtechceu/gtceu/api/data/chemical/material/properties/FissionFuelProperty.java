package com.gregtechceu.gtceu.api.data.chemical.material.properties;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class FissionFuelProperty implements IMaterialProperty<FissionFuelProperty>, StringRepresentable,
        Comparable<FissionFuelProperty> {

    private static final Map<String, FissionFuelProperty> INSTANCES = new HashMap<>();

    private final Supplier<Material> fuel;
    private final int heat;
    private final String name;

    public FissionFuelProperty(String name, Supplier<Material> fuel, int heat) {
        this.fuel = fuel;
        this.heat = heat;
        this.name = name;
        INSTANCES.put(this.getSerializedName(), this);
    }

    public static Collection<FissionFuelProperty> getInstances() {
        return Set.copyOf(INSTANCES.values());
    }

    public static FissionFuelProperty getDefaultValue(){
        return INSTANCES.values().stream().findFirst().orElseThrow(() -> new IllegalStateException("No FissionFuelProperty instances available"));
    }

    @Override
    public @NotNull String getSerializedName() {
        return getName().toLowerCase();
    }

    public String getName() {
        return this.name + "_" + this.heat;
    }

    public int getHeatProduces() {
        return heat;
    }

    @Override
    public void verifyProperty(MaterialProperties properties) {}

    @Override
    public int compareTo(@NotNull FissionFuelProperty o) {
        return this.getName().compareTo(o.getName());
    }

    public static FissionFuelProperty getByName(String name) {
        return INSTANCES.get(name);
    }

    public Material getFuel(){
        return fuel.get();
    }
}
