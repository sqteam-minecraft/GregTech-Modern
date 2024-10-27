package com.gregtechceu.gtceu.common.block;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StaticObjectProperty<T extends StringRepresentable & Comparable<T>> extends Property<T> {
    private final ImmutableSet<T> values;
    private final Map<String, T> nameToValueMap;
    protected StaticObjectProperty(String name, Class<T> clazz, Collection<T> values) {
        super(name, clazz);
        this.values = ImmutableSet.copyOf(values);
        this.nameToValueMap = values.stream().collect(Collectors.toMap(
                T::getSerializedName,
                Function.identity()
        ));
    }

    public static <T extends StringRepresentable & Comparable<T>> StaticObjectProperty<T> create(String name, Class<T> clazz, Collection<T> values) {
        return new StaticObjectProperty<>(name, clazz, values);
    }

    @Override
    public @NotNull Collection<T> getPossibleValues() {
        return values;
    }

    @Override
    public @NotNull String getName(T t) {
        return t.getSerializedName();
    }

    @Override
    public @NotNull Optional<T> getValue(@NotNull String s) {
        return Optional.ofNullable(nameToValueMap.get(s));
    }

}
