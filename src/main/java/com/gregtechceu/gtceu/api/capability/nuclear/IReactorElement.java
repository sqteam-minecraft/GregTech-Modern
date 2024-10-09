package com.gregtechceu.gtceu.api.capability.nuclear;

import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;

import javax.annotation.Nullable;

/**
 * Implement this interface in order to make a BlockEntity into a block that can be part of a fission reactor
 */
public interface IReactorElement {
    /**
     * @return the fission reactor the machine is part of
     */
    @Nullable
    IFissionReactor getAssignedReactor();

    /**
     * sets the element's fission reactor to the provided one
     *
     * @param reactor the fission reactor to assign to this reactor element
     */
    void assignToReactor(IFissionReactor reactor);
}
