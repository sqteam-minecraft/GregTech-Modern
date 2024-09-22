package com.gregtechceu.gtceu.common.machine.multiblock.part.nuclear;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import net.minecraft.server.level.ServerLevel;

public class ReactorRedstoneControlHatch extends MultiblockPartMachine {

    public ReactorRedstoneControlHatch(IMachineBlockEntity holder) {
        super(holder);
    }

    public int getRedstoneSignalStrength() {
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null) return 0;
        return level.getSignal(getPos(), getFrontFacing());
    }
}
