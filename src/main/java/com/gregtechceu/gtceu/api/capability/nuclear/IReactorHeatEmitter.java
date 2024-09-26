package com.gregtechceu.gtceu.api.capability.nuclear;

public interface IReactorHeatEmitter extends IReactorElement
{
    void addHeat(double heat);
    void removeHeat(double heat);
}
