package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;

public class FuelRod extends AppearanceBlock implements IReactorFuelRod {
    public FuelRod(Properties properties) {
        super(properties);
    }

    @Override
    public int getUses() {
        return 0;
    }
}
