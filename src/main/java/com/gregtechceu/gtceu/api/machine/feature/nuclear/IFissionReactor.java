package com.gregtechceu.gtceu.api.machine.feature.nuclear;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.api.machine.multiblock.FissionReactorType;

import java.util.Set;

public interface IFissionReactor extends IMachineFeature
{
    /**
     * @return a {@link Set} of {@link FissionReactorType} which the fission reactor provides
     */
    Set<FissionReactorType> getTypes();
}
