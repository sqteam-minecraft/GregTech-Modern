package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.world.level.block.Block;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.util.entry.BlockEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record FissionReactorType(String name, String translationKey, @Nullable BlockEntry<Block> casing,
                                 @Nullable BlockEntry<Block> wall) {

    private static final Map<String, FissionReactorType> FISSION_CONTROLLER_TYPES = new Object2ObjectOpenHashMap<>();

    public static final FissionReactorType TIER_1 = new FissionReactorType("fisson_controller_1",
            "gtceu.recipe.fisson_controller_1.display_name",
            GTBlocks.CASING_STEEL_SOLID,
            GTBlocks.CASING_REINFORCED_BOROSILICATE_GLASS);
    public static final FissionReactorType TIER_2 = new FissionReactorType("fisson_controller_2",
            "gtceu.recipe.fisson_controller_2.display_name",
            null,
            null);
    public static final FissionReactorType TIER_3 = new FissionReactorType("fisson_controller_3",
            "gtceu.recipe.fisson_controller_3.display_name",
            null,
            null);

    public static final Codec<FissionReactorType> CODEC = Codec.STRING.xmap(FISSION_CONTROLLER_TYPES::get,
            FissionReactorType::name);

    public FissionReactorType(@NotNull String name, @NotNull String translationKey, @Nullable BlockEntry<Block> casing,
                              @Nullable BlockEntry<Block> wall) {
        if (FISSION_CONTROLLER_TYPES.get(name) != null)
            throw new IllegalArgumentException(
                    String.format("FissionControllerType with name %s is already registered!", name));

        this.name = name;
        this.translationKey = translationKey;
        this.casing = casing;
        this.wall = wall;

        FISSION_CONTROLLER_TYPES.put(name, this);
    }

    @Override
    @NotNull
    public String name() {
        return this.name;
    }

    @Override
    @NotNull
    public String translationKey() {
        return this.translationKey;
    }

    @Override
    @NotNull
    public BlockEntry<Block> casing() {
        if (this.casing == null)
            throw new IllegalStateException("Casing is not defined for this FissionControllerType");
        return this.casing;
    }

    @Override
    @NotNull
    public BlockEntry<Block> wall() {
        if (this.wall == null) throw new IllegalStateException("Wall is not defined for this FissionControllerType");
        return this.wall;
    }

    @Nullable
    public static FissionReactorType getByName(@Nullable String name) {
        return FISSION_CONTROLLER_TYPES.get(name);
    }

    @NotNull
    public static FissionReactorType getByNameOrDefault(@Nullable String name) {
        var type = getByName(name);
        if (type == null) return TIER_1;
        return type;
    }

    public static Set<FissionReactorType> getAllTypes() {
        return new HashSet<>(FISSION_CONTROLLER_TYPES.values());
    }
}
