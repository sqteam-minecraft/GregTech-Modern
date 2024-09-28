package com.gregtechceu.gtceu.common.machine.multiblock.part.nuclear;

import appeng.api.inventories.ItemTransfer;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelConnector;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import static com.google.common.primitives.Ints.*;

public class ReactorFuelController extends TieredIOPartMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ReactorFuelController.class,
            TieredPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    protected  NotifiableItemStackHandler inventory;

    @Persisted
    @DescSynced
    public final ItemStackTransfer storage;

    public ReactorFuelController(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.BOTH);
        this.storage = new ItemStackTransfer();
        this.inventory = createInventory();
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(inventory.storage);
    }

    protected NotifiableItemStackHandler createInventory() {
        return new NotifiableItemStackHandler(this, 4, io, io, x->this.storage);
    }

    @Override
    public void addedToController(IMultiController controller)
    {
        super.addedToController(controller);
        var old = storage.copy();
        storage.setSize(getInventorySize());
        for (int i = 0; i < min(old.getSlots(), storage.getSlots()); i++) {
            storage.setStackInSlot(i, old.getStackInSlot(i));
        }
        markDirty("storage");
        
        ServerLevel level = (ServerLevel) getLevel();
        if (level==null) return;
        BlockPos pos = getPos().above();
        for(int i =  storage.getSlots(); i < old.getSlots(); i++) {
            getLevel().addFreshEntity(new ItemEntity(getLevel(), pos.getX(), pos.getY(), pos.getZ(), old.getStackInSlot(i)));
        }

    }
    protected int getInventorySize() {
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null) return 0;

        int size = 0;
        BlockPos pos = getPos();

        for (int i = 1; i < 15; i++) {
            if (level.getBlockState(pos.below(i)).getBlock() instanceof IReactorFuelRod) {
                size++;
            } else {
                break;
            }
        }

        return size*4;
    }

    @Override
    public Widget createUIWidget() {

        int width =  8;
        int height = (storage.getSlots()+4)/width;

        var group = new WidgetGroup(0, 0, 18 * width + 16+8, 18 * height + 16);
        var containerL = new WidgetGroup(4, 4, 18 * width/2 + 8, 18 * height + 8);
        var containerR = new WidgetGroup(18 * width/2 + 12, 4, 18 * width/2 + 8, 18 * height + 8);
        int index = 0;
        for (int y = 0; y < height; y++) {

            for(int x = 0; x < width; x++){
                if (index == storage.getSlots()) break;
                var slot = new SlotWidget(storage, index++, 4 + (x%4) * 18, 4 + y * 18, true, io.support(IO.BOTH))
                        .setBackgroundTexture(GuiTextures.SLOT);
                if (x < 4){
                    containerL.addWidget(slot);
                }else {
                    containerR.addWidget(slot);
                }

            }

        }

        containerL.setBackground(GuiTextures.BACKGROUND_INVERSE);
        containerR.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(containerL);
        group.addWidget(containerR);

        return group;
    }

    public void updateFuelRods() {

    }
}
