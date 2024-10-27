package com.gregtechceu.gtceu.api.nuclear;


import com.gregtechceu.gtceu.api.capability.nuclear.IReactorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PositionedComponent {
    private final IReactorElement component;
    private final BlockPos position;

    public PositionedComponent(IReactorElement component, BlockPos position) {
        this.component = component;
        this.position = position;
    }

    public IReactorElement getComponent() {
        return component;
    }

    public BlockPos getPosition() {
        return position;
    }

    // Use position for equals() and hashCode()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PositionedComponent that = (PositionedComponent) o;

        return position.equals(that.position);
    }

    public BlockState getBlockState(Level level){
        return level.getBlockState(this.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    public String getId() {
        return component.getClass().getSimpleName() + "@" + position.getX() + "," + position.getY() + "," + position.getZ();
    }
}

