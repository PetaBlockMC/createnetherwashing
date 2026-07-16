package xyz.petablock.createnetherwashing.fan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import xyz.petablock.createnetherwashing.CreateNetherWashing;
import xyz.petablock.createnetherwashing.NWConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Drains water from tracked basins while their wash column is actively processing items.
 * Columns are discovered lazily whenever {@link WashColumnResolver} validates one (which
 * happens every time a fan rebuilds its air current) and re-validated here every tick.
 */
@EventBusSubscriber(modid = CreateNetherWashing.MOD_ID)
public class WashColumnTracker {
	/** basin pos -> fractional mB consumption accumulator, per dimension. Server thread only. */
	private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> ACTIVE = new HashMap<>();

	public static void track(ServerLevel level, BlockPos basinPos) {
		ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>())
			.putIfAbsent(basinPos, 0f);
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		Map<BlockPos, Float> columns = ACTIVE.get(level.dimension());
		if (columns == null || columns.isEmpty())
			return;

		// Iterate a snapshot: resolveFromBasin() may re-track columns while we mutate the map.
		for (BlockPos basinPos : new ArrayList<>(columns.keySet())) {
			if (!level.isLoaded(basinPos)) {
				columns.remove(basinPos);
				continue;
			}
			WashColumn column = WashColumnResolver.resolveFromBasin(level, basinPos);
			if (column == null) {
				columns.remove(basinPos);
				continue;
			}
			if (NWConfig.drainRequiresItems() && !hasWashableItems(level, column))
				continue;

			float accumulator = columns.get(basinPos) + consumptionPerTick(column);
			int whole = (int) accumulator;
			if (whole > 0) {
				drain(level, column, whole);
				accumulator -= whole;
			}
			columns.put(basinPos, accumulator);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE.clear();
	}

	/**
	 * Linear scaling: baseConsumption mB/s at rpmPerBlock, +stepIncrease per additional
	 * rpmPerBlock of speed ("smooth stepping"), divided by iceDivisor inside an ice tube.
	 */
	private static float consumptionPerTick(WashColumn column) {
		float rpmPerBlock = NWConfig.rpmPerBlock();
		float extraSteps = Math.max(0, (column.rpm() - rpmPerBlock) / rpmPerBlock);
		double perSecond = NWConfig.baseConsumption() * (1 + NWConfig.stepIncrease() * extraSteps);
		if (column.iced())
			perSecond /= NWConfig.iceDivisor();
		return (float) (perSecond / 20);
	}

	private static boolean hasWashableItems(ServerLevel level, WashColumn column) {
		BlockPos basinPos = column.basinPos();
		AABB columnBox = new AABB(basinPos.getX(), basinPos.getY() + 1, basinPos.getZ(),
			basinPos.getX() + 1, column.fanPos().getY(), basinPos.getZ() + 1);
		if (!level.getEntitiesOfClass(ItemEntity.class, columnBox,
				entity -> AllFanProcessingTypes.SPLASHING.canProcess(entity.getItem(), level))
			.isEmpty())
			return true;

		for (int i = 1; i <= column.gap(); i++) {
			TransportedItemStackHandlerBehaviour behaviour =
				BlockEntityBehaviour.get(level, basinPos.above(i), TransportedItemStackHandlerBehaviour.TYPE);
			if (behaviour == null)
				continue;
			MutableBoolean found = new MutableBoolean(false);
			behaviour.handleProcessingOnAllItems(transported -> {
				if (found.isFalse() && AllFanProcessingTypes.SPLASHING.canProcess(transported.stack, level))
					found.setTrue();
				return TransportedResult.doNothing();
			});
			if (found.isTrue())
				return true;
		}
		return false;
	}

	private static void drain(ServerLevel level, WashColumn column, int amount) {
		if (!(level.getBlockEntity(column.basinPos()) instanceof BasinBlockEntity basin))
			return;
		basin.inputTank.getCapability()
			.drain(new FluidStack(column.fluid(), amount), IFluidHandler.FluidAction.EXECUTE);
	}
}
