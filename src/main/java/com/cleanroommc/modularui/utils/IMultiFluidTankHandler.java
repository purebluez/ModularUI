package com.cleanroommc.modularui.utils;

import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

public interface IMultiFluidTankHandler extends IFluidHandler {

    int getTankCount();

    IFluidTank getFluidTank(int index);
}
