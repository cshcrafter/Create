package com.simibubi.create.content.contraptions.fluids;

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class FluidReactions {

	public static void handlePipeFlowCollision(World world, BlockPos pos, FluidStack fluid, FluidStack fluid2) {
		Fluid f1 = fluid.getFluid();
		Fluid f2 = fluid2.getFluid();
		BlockHelper.destroyBlock(world, pos, 1);
		if (f1 == Fluids.WATER && f2 == Fluids.LAVA || f2 == Fluids.WATER && f1 == Fluids.LAVA)
			world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
	}

	public static void handlePipeSpillCollision(World world, BlockPos pos, Fluid pipeFluid, IFluidState worldFluid) {
		Fluid pf = FluidHelper.convertToStill(pipeFluid);
		Fluid wf = worldFluid.getFluid();
		if (pf == Fluids.WATER && wf == Fluids.LAVA)
			world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
		if (pf == Fluids.WATER && wf == Fluids.FLOWING_LAVA)
			world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
		else if (pf == Fluids.LAVA && wf == Fluids.WATER)
			world.setBlockState(pos, Blocks.STONE.getDefaultState());
		else if (pf == Fluids.LAVA && wf == Fluids.FLOWING_WATER)
			world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
	}

}
