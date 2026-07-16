package xyz.petablock.createnetherwashing.fan;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import xyz.petablock.createnetherwashing.CreateNetherWashing;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NWFanProcessingTypes {
	private static final DeferredRegister<FanProcessingType> REGISTER =
		DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, CreateNetherWashing.MOD_ID);

	public static final DeferredHolder<FanProcessingType, BasinWashingType> BASIN_WASHING =
		REGISTER.register("basin_washing", () -> new BasinWashingType(1.0f));
	public static final DeferredHolder<FanProcessingType, BasinWashingType> BASIN_WASHING_MID =
		REGISTER.register("basin_washing_mid", () -> new BasinWashingType(1.5f));
	public static final DeferredHolder<FanProcessingType, BasinWashingType> BASIN_WASHING_FAR =
		REGISTER.register("basin_washing_far", () -> new BasinWashingType(2.0f));

	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}

	/**
	 * The processing type tier for a column: an ice tub washes at full speed at any distance,
	 * otherwise each extra block of gap slows processing down.
	 */
	public static BasinWashingType forColumn(WashColumn column) {
		if (column.iced() || column.gap() <= 1)
			return BASIN_WASHING.get();
		return column.gap() == 2 ? BASIN_WASHING_MID.get() : BASIN_WASHING_FAR.get();
	}
}
