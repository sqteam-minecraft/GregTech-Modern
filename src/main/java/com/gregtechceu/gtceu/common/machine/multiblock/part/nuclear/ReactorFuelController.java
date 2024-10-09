package com.gregtechceu.gtceu.common.machine.multiblock.part.nuclear;

import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import com.gregtechceu.gtceu.api.capability.nuclear.IReactorFuelRod;
import com.gregtechceu.gtceu.api.capability.nuclear.ReactorFuel;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.nuclear.IFissionReactor;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.block.FuelRod;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.google.common.primitives.Ints.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.PlutoniumFissionFuel;
import static com.gregtechceu.gtceu.common.data.GTMaterials.UraniumFissionFuel;

@Slf4j
public class ReactorFuelController extends TieredIOPartMachine implements IReactorElement, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ReactorFuelController.class,
            TieredPartMachine.MANAGED_FIELD_HOLDER);

    private IFissionReactor reactor;

    @Getter
    @Persisted
    protected NotifiableItemStackHandler inventory;

    @Persisted
    @DescSynced
    public final ItemStackTransfer storage;

    private int[] fuelRods;
    public boolean needUpdate = false;
    private TickableSubscription subscription;

    public ReactorFuelController(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.BOTH);

        this.storage = new ItemStackTransfer();
        storage.setFilter(stack -> {
            if (stack.getItem() instanceof TagPrefixItem tagPrefix && reactor != null) {
                ReactorFuel fuel = reactor.getFuel();
                if (fuel == null) {
                    reactor.setFuel(Arrays.stream(ReactorFuel.values()).filter(f -> f.getFuel().equals(tagPrefix.material))
                            .findFirst().orElse(null));
                    needUpdate = true;
                    return reactor.getFuel() != null;
                };
                return fuel.getFuel().equals(tagPrefix.material);
            }
            return false;
        });

        this.inventory = createInventory();
        inventory.addChangedListener(() -> {
                ServerLevel level = (ServerLevel) getLevel();
                if (level == null) return;

                fuelRods = new int[storage.getSlots() / 4];
                for (int rod = 0; rod < storage.getSlots() / 4; rod++) {
                    var blockPos = getPos().relative(Direction.Axis.Y, -rod - 1);
                    int fuelRodState = level.getBlockState(blockPos).getValue(FuelRod.RODS);
                    for (int pos = 0; pos < 4; pos++) {
                        var item = storage.getStackInSlot(rod * 4 + pos);
                        int rodsAmount = getRodsAmount(item);
                        fuelRodState &= ~(0b11 << pos * 2);
                        fuelRodState |= rodsAmount << pos * 2;
                    }
                    fuelRods[rod] = fuelRodState;
                }
                needUpdate = true;
        });
    }

    private static int getRodsAmount(ItemStack item) {
        int rodsAmount = 0;
        if (item.getItem() instanceof TagPrefixItem prefixItem) {
            if (prefixItem.tagPrefix == TagPrefix.fuelRodSingle) rodsAmount = 1;
            else if (prefixItem.tagPrefix == TagPrefix.fuelRodDouble) rodsAmount = 2;
            else if (prefixItem.tagPrefix == TagPrefix.fuelRodQuad) rodsAmount = 3;
        }
        return rodsAmount;
    }

    @Override
    public void onLoad() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> {
                if (subscription == null) subscription = subscribeServerTick(null, this::updateFuelRods);
                else {
                    subscription.unsubscribe();
                    subscription = null;
                }
            }));
        }
        super.onLoad();
    }

    public void updateFuelRods() {
        if (getLevel() instanceof ServerLevel level && needUpdate && fuelRods != null) {
            for (int i = 0; i < fuelRods.length; i++) {
                var blockPos = getPos().relative(Direction.Axis.Y, -i - 1);
                var rodState = level.getBlockState(blockPos);
                level.setBlock(blockPos, rodState.setValue(FuelRod.RODS, fuelRods[i])
                        .setValue(FuelRod.FUEL_TYPE, reactor.getFuel() == null ? ReactorFuel.URANIUM : reactor.getFuel()), 2);
            }
        }
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
    public void addedToController(@NotNull IMultiController controller)
    {
        super.addedToController(controller);

        var old = storage.copy();
        storage.setSize(getInventorySize());
        for (int i = 0; i < min(old.getSlots(), storage.getSlots()); i++) {
            storage.setStackInSlot(i, old.getStackInSlot(i));
        }
        markDirty("storage");
        
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null) return;

        BlockPos pos = getPos().above();
        for (int i = storage.getSlots(); i < old.getSlots(); i++) {
            getLevel().addFreshEntity(new ItemEntity(getLevel(), pos.getX(), pos.getY(), pos.getZ(), old.getStackInSlot(i)));
        }
    }

    protected int getInventorySize() {
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null) return 0;

        int size = 0;
        BlockPos pos = getPos();

        for (int i = 1; i < 15; i++) {
            if (level.getBlockState(pos.below(i)).getBlock() instanceof IReactorFuelRod) size++;
            else break;
        }

        return size * 4;
    }

    @Override
    public Widget createUIWidget() {
        int width = 8;
        int height = (storage.getSlots() + 4) / width;

        var group = new WidgetGroup(0, 0, 18 * width + 16+8, 18 * height + 16);
        var containerL = new WidgetGroup(4, 4, 18 * width / 2 + 8, 18 * height + 8);
        var containerR = new WidgetGroup(18 * width / 2 + 12, 4, 18 * width / 2 + 8, 18 * height + 8);
        int index = 0;
        for (int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                if (index == storage.getSlots()) break;

                var slot = new SlotWidget(inventory.storage, index++, 4 + (x % 4) * 18, 4 + y * 18, true, io.support(IO.BOTH))
                        .setBackgroundTexture(GuiTextures.SLOT);

                if (x < 4) containerL.addWidget(slot);
                else containerR.addWidget(slot);
            }
        }

        containerL.setBackground(GuiTextures.BACKGROUND_INVERSE);
        containerR.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(containerL);
        group.addWidget(containerR);

        return group;
    }

    @Nullable
    @Override
    public IFissionReactor getAssignedReactor() {
        return this.reactor;
    }

    @Override
    public void assignToReactor(IFissionReactor reactor) {
        this.reactor = reactor;
    }
}
