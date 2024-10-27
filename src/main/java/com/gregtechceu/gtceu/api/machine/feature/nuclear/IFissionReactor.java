package com.gregtechceu.gtceu.api.machine.feature.nuclear;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.api.machine.multiblock.FissionReactorType;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface IFissionReactor extends IMachineFeature {

    /**
     * @return a {@link Set} of {@link FissionReactorType} which the fission reactor provides
     */
    Set<FissionReactorType> getTypes();

    boolean setFuel(@Nullable String fuel);

    @Nullable
    String getFuel();

    void updateFuel();
}
