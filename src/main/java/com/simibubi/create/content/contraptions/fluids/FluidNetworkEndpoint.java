package com.simibubi.create.content.contraptions.fluids;

import java.lang.ref.WeakReference;

import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

class FluidNetworkEndpoint {
	BlockFace location;
	protected LazyOptional<IFluidHandler> handler;

	public FluidNetworkEndpoint(IWorld world, BlockFace location, LazyOptional<IFluidHandler> handler) {
		this.location = location;
		this.handler = handler;
		this.handler.addListener($ -> onHandlerInvalidated(world));
	}

	protected void onHandlerInvalidated(IWorld world) {
		IFluidHandler tank = handler.orElse(null);
		if (tank != null)
			return;
		TileEntity tileEntity = world.getTileEntity(location.getConnectedPos());
		if (tileEntity == null)
			return;
		LazyOptional<IFluidHandler> capability =
			tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, location.getOppositeFace());
		if (capability.isPresent()) {
			handler = capability;
			handler.addListener($ -> onHandlerInvalidated(world));
		}
	}

	public FluidStack provideFluid() {
		IFluidHandler tank = provideHandler().orElse(null);
		if (tank == null)
			return FluidStack.EMPTY;
		return tank.drain(1, FluidAction.SIMULATE);
	}

	public LazyOptional<IFluidHandler> provideHandler() {
		return handler;
	}

}

class PumpEndpoint extends FluidNetworkEndpoint {

	PumpTileEntity pumpTE;

	public PumpEndpoint(BlockFace location, PumpTileEntity pumpTE) {
		super(pumpTE.getWorld(), location, LazyOptional.empty());
		this.pumpTE = pumpTE;
	}

	@Override
	protected void onHandlerInvalidated(IWorld world) {}

	@Override
	public FluidStack provideFluid() {
		return pumpTE.providedFluid;
	}

}

class InterPumpEndpoint extends FluidNetworkEndpoint {

	Couple<Pair<BlockFace, WeakReference<PumpTileEntity>>> pumps;

	private InterPumpEndpoint(IWorld world, BlockFace location, LazyOptional<IFluidHandler> handler) {
		super(world, location, handler);
	}

	public InterPumpEndpoint(IWorld world, BlockFace location, PumpTileEntity source, PumpTileEntity interfaced,
		BlockFace sourcePos, BlockFace interfacedPos) {
		this(world, location, LazyOptional.empty());
		handler = LazyOptional.of(() -> new InterPumpFluidHandler(this));
		pumps = Couple.create(Pair.of(sourcePos, new WeakReference<>(source)),
			Pair.of(interfacedPos, new WeakReference<>(interfaced)));
	}

	public InterPumpEndpoint opposite(IWorld world) {
		InterPumpEndpoint interPumpEndpoint = new InterPumpEndpoint(world, this.location.getOpposite(), handler);
		interPumpEndpoint.pumps = pumps.copy();
		return interPumpEndpoint;
	}

	public Couple<Pair<BlockFace, WeakReference<PumpTileEntity>>> getPumps() {
		return pumps;
	}

	public boolean isPulling(boolean first) {
		Pair<BlockFace, WeakReference<PumpTileEntity>> pair = getPumps().get(first);
		PumpTileEntity pumpTileEntity = pair.getSecond()
			.get();
		if (pumpTileEntity == null || pumpTileEntity.isRemoved())
			return false;
		return pumpTileEntity.isPullingOnSide(pumpTileEntity.isFront(pair.getFirst()
			.getFace()));
	}

	public int getTransferSpeed(boolean first) {
		PumpTileEntity pumpTileEntity = getPumps().get(first)
			.getSecond()
			.get();
		if (pumpTileEntity == null || pumpTileEntity.isRemoved())
			return 0;
		return pumpTileEntity.getFluidTransferSpeed();
	}

	@Override
	public LazyOptional<IFluidHandler> provideHandler() {
		if (isPulling(true) == isPulling(false))
			return LazyOptional.empty();
		if (getTransferSpeed(true) > getTransferSpeed(false))
			return LazyOptional.empty();
		return super.provideHandler();
	}

	@Override
	public FluidStack provideFluid() {
		if (!provideHandler().isPresent())
			return FluidStack.EMPTY;

		Couple<Pair<BlockFace, WeakReference<PumpTileEntity>>> pumps = getPumps();
		for (boolean current : Iterate.trueAndFalse) {
			if (isPulling(current))
				continue;

			Pair<BlockFace, WeakReference<PumpTileEntity>> pair = pumps.get(current);
			BlockFace blockFace = pair.getFirst();
			PumpTileEntity pumpTileEntity = pair.getSecond()
				.get();
			if (pumpTileEntity == null)
				continue;
			if (pumpTileEntity.networks == null)
				continue;
			FluidNetwork fluidNetwork = pumpTileEntity.networks.get(pumpTileEntity.isFront(blockFace.getFace()));
			for (FluidNetworkFlow fluidNetworkFlow : fluidNetwork.flows) {
				for (FluidNetworkEndpoint fne : fluidNetworkFlow.outputEndpoints) {
					if (!(fne instanceof InterPumpEndpoint))
						continue;
					InterPumpEndpoint ipe = (InterPumpEndpoint) fne;
					if (!ipe.location.isEquivalent(location))
						continue;

					FluidStack heldFluid = fluidNetworkFlow.fluidStack;
					if (heldFluid.isEmpty())
						return heldFluid;
					FluidStack copy = heldFluid.copy();
					copy.setAmount(1);
					return heldFluid;
				}
			}
		}
		return FluidStack.EMPTY;
	}

}
