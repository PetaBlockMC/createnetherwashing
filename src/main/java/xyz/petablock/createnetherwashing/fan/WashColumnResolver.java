package xyz.petablock.createnetherwashing.fan;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import xyz.petablock.createnetherwashing.NWConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Validates basin-washing geometry: a basin holding a splashing-catalyst fluid, an encased fan
 * 1-3 blocks above it facing down with its airflow reversed (pulling air up through the basin),
 * spinning at >= rpmPerBlock x gap. All blocks between basin and fan must be passable to items
 * (air, fan-transparent, or a belt/depot).
 */
public class WashColumnResolver {
	public static final int MAX_GAP = 3;

	/**
	 * Resolves the wash column that {@code pos} is part of, i.e. {@code pos} lies strictly
	 * between a valid basin (below) and fan (above). This is the query {@link BasinWashingType}
	 * uses for positions along a fan's air current.
	 */
	@Nullable
	public static WashColumn resolve(Level level, BlockPos pos) {
		if (!isPassable(level, pos))
			return null;

		BlockPos.MutableBlockPos cursor = pos.mutable();
		BlockPos basinPos = null;
		for (int down = 1; down <= MAX_GAP; down++) {
			cursor.move(Direction.DOWN);
			BlockEntity be = level.getBlockEntity(cursor);
			if (be instanceof BasinBlockEntity) {
				basinPos = cursor.immutable();
				break;
			}
			if (!isPassable(level, cursor))
				return null;
		}
		if (basinPos == null)
			return null;

		WashColumn column = resolveFromBasin(level, basinPos);
		if (column == null || pos.getY() >= column.fanPos().getY())
			return null;
		return column;
	}

	/**
	 * Resolves a wash column from a known basin position; used both by {@link #resolve} and by
	 * {@link WashColumnTracker} to re-validate tracked columns each tick.
	 */
	@Nullable
	public static WashColumn resolveFromBasin(Level level, BlockPos basinPos) {
		if (!(level.getBlockEntity(basinPos) instanceof BasinBlockEntity basin))
			return null;
		Fluid fluid = findWashingFluid(basin);
		if (fluid == null)
			return null;

		BlockPos.MutableBlockPos cursor = basinPos.mutable();
		EncasedFanBlockEntity fan = null;
		int gap = 0;
		for (int i = 1; i <= MAX_GAP + 1; i++) {
			cursor.move(Direction.UP);
			if (level.getBlockEntity(cursor) instanceof EncasedFanBlockEntity f) {
				fan = f;
				gap = i - 1;
				break;
			}
			if (i == MAX_GAP + 1 || !isPassable(level, cursor))
				return null;
		}
		if (fan == null || gap < 1)
			return null;

		// Fan must face down at the basin and be pulling: airflow up, away from the basin.
		if (fan.getAirflowOriginSide() != Direction.DOWN || fan.getAirFlowDirection() != Direction.UP)
			return null;

		float rpm = Math.abs(fan.getSpeed());
		if (rpm < (float) NWConfig.rpmPerBlock() * gap)
			return null;

		boolean iced = isIceTube(level, basinPos, gap);
		WashColumn column = new WashColumn(basinPos, cursor.immutable(), gap, iced, rpm, fluid);
		if (level instanceof ServerLevel serverLevel)
			WashColumnTracker.track(serverLevel, column.basinPos());
		return column;
	}

	private static boolean isPassable(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir())
			return true;
		if (AllTags.AllBlockTags.FAN_TRANSPARENT.matches(state))
			return true;
		// Belts, depots and other transported-item handlers may sit inside the column.
		if (BlockEntityBehaviour.get(level, pos, TransportedItemStackHandlerBehaviour.TYPE) != null)
			return true;
		return state.getCollisionShape(level, pos)
			.isEmpty();
	}

	@Nullable
	private static Fluid findWashingFluid(BasinBlockEntity basin) {
		IFluidHandler handler = basin.inputTank.getCapability();
		for (int i = 0; i < handler.getTanks(); i++) {
			FluidStack stack = handler.getFluidInTank(i);
			if (!stack.isEmpty() && AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(stack.getFluid()))
				return stack.getFluid();
		}
		return null;
	}

	/**
	 * A column is an "ice tube" when every gap block is horizontally enclosed by at least
	 * 3 blue ice blocks, with the remaining side (if any) closed off by a solid face.
	 */
	private static boolean isIceTube(Level level, BlockPos basinPos, int gap) {
		for (int i = 1; i <= gap; i++) {
			BlockPos p = basinPos.above(i);
			int ice = 0;
			int closed = 0;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos n = p.relative(d);
				BlockState s = level.getBlockState(n);
				if (s.is(Blocks.BLUE_ICE))
					ice++;
				else if (s.isFaceSturdy(level, n, d.getOpposite()))
					closed++;
			}
			if (ice < 3 || ice + closed < 4)
				return false;
		}
		return true;
	}
}
