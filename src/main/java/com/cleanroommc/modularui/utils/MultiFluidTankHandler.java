package com.cleanroommc.modularui.utils;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;

public class MultiFluidTankHandler implements IMultiFluidTankHandler {

    private final IFluidTank[] tanks;

    public MultiFluidTankHandler(IFluidTank... tanks) {
        Objects.requireNonNull(tanks);
        for (IFluidTank tank : tanks) {
            Objects.requireNonNull(tank);
        }
        this.tanks = Arrays.copyOf(tanks, tanks.length);
    }

    public MultiFluidTankHandler(int count, int capacity) {
        this(count, i -> new FluidTank(capacity));
    }

    public MultiFluidTankHandler(int count, IntFunction<IFluidTank> tankBuilder) {
        this.tanks = new IFluidTank[count];
        for (int i = 0; i < count; i++) {
            this.tanks[i] = Objects.requireNonNull(tankBuilder.apply(i));
        }
    }

    @Override
    public int getTankCount() {
        return this.tanks.length;
    }

    @Override
    public IFluidTank getFluidTank(int index) {
        return this.tanks[index];
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] prop = new IFluidTankProperties[this.tanks.length];
        for (int i = 0; i < prop.length; i++) {
            boolean canDrain = true;
            boolean canFill = true;
            if (this.tanks[i] instanceof FluidTank tank) {
                canDrain = tank.canDrain();
                canFill = tank.canFill();
            }
            prop[i] = new FluidTankProperties(this.tanks[i].getFluid(), this.tanks[i].getCapacity(), canFill, canDrain);
        }
        return prop;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (isFluidEmpty(resource)) return 0;
        int fillAmount = resource.amount;
        FluidStack toFill = resource.copy();
        // first find tanks with matching fluid
        for (IFluidTank tank : this.tanks) {
            if (!isFluidEmpty(tank.getFluid()) && resource.isFluidEqual(tank.getFluid())) {
                fillAmount -= tank.fill(toFill, doFill);
                toFill.amount = fillAmount;
                if (fillAmount <= 0) return resource.amount;
            }
        }
        // if still fluid there, insert into empty tanks
        for (IFluidTank tank : this.tanks) {
            if (isFluidEmpty(tank.getFluid())) {
                fillAmount -= tank.fill(toFill, doFill);
                toFill.amount = fillAmount;
                if (fillAmount <= 0) return resource.amount;
            }
        }
        return resource.amount - fillAmount;
    }

    @Override
    public @Nullable FluidStack drain(FluidStack resource, boolean doDrain) {
        if (isFluidEmpty(resource)) return null;
        return drain(0, resource, doDrain);
    }

    private @Nullable FluidStack drain(int startIndex, FluidStack resource, boolean doDrain) {
        if (startIndex >= this.tanks.length) return null;
        int drainAmount = resource.amount;
        for (int i = startIndex; i < this.tanks.length; i++) {
            IFluidTank tank = this.tanks[i];
            if (isFluidEmpty(tank.getFluid()) || !resource.isFluidEqual(tank.getFluid())) continue;
            FluidStack d = this.tanks[i].drain(drainAmount, doDrain);
            if (!isFluidEmpty(d)) {
                drainAmount -= d.amount;
                if (drainAmount <= 0) return resource.copy();
            }
        }
        if (drainAmount == resource.amount) return null;
        FluidStack drained = resource.copy();
        drained.amount -= drainAmount;
        return drained;
    }

    @Override
    public @Nullable FluidStack drain(int maxDrain, boolean doDrain) {
        for (int i = 0; i < this.tanks.length; i++) {
            FluidStack drained = this.tanks[i].drain(maxDrain, doDrain);
            if (!isFluidEmpty(drained)) {
                // if already drained enough from this slot return
                if (drained.amount >= maxDrain) return drained;
                // otherwise drain from all other slots with the same fluid
                FluidStack toDrain = drained.copy();
                toDrain.amount = maxDrain - toDrain.amount;
                FluidStack drained2 = drain(i + 1, toDrain, doDrain);
                if (isFluidEmpty(drained2)) return drained;
                drained.amount += drained2.amount;
                return drained;
            }
        }
        return null;
    }

    public static boolean isFluidEmpty(FluidStack f) {
        return f == null || f.getFluid() == null || f.amount <= 0;
    }
}
