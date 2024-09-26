package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import org.jetbrains.annotations.Nullable;

public class FuelRod extends AppearanceBlock implements IReactorFuelRod {
    @Persisted
    private double heat;
    @Nullable
    private IFissionReactor reactor;

    public FuelRod(Properties properties) {
        super(properties);
    }

    @Override
    public int getUses() {
        return 0;
    }

    @Override
    public void addHeat(double heat)
    {
        this.heat += heat;
    }

    @Override
    public void removeHeat(double heat)
    {
        this.heat -= heat;
    }

    @Nullable
    @Override
    public IFissionReactor getFissionReactor()
    {
        return reactor;
    }

    @Override
    public void setReactor(IFissionReactor reactor)
    {
        this.reactor = reactor;
    }
}
