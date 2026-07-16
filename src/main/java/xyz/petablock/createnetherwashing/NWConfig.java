package xyz.petablock.createnetherwashing;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NWConfig {
	public static final ModConfigSpec SPEC;

	private static final ModConfigSpec.IntValue RPM_PER_BLOCK;
	private static final ModConfigSpec.IntValue BASE_CONSUMPTION;
	private static final ModConfigSpec.DoubleValue STEP_INCREASE;
	private static final ModConfigSpec.DoubleValue ICE_DIVISOR;
	private static final ModConfigSpec.BooleanValue DRAIN_REQUIRES_ITEMS;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.push("basinWashing");
		RPM_PER_BLOCK = builder
			.comment("RPM required per block of gap between basin and fan (gap 1 -> x, gap 2 -> 2x, gap 3 -> 3x)")
			.defineInRange("rpmPerBlock", 64, 1, 256);
		BASE_CONSUMPTION = builder
			.comment("Water consumed in mB per second while washing at the base RPM (rpmPerBlock)")
			.defineInRange("baseConsumptionMbPerSecond", 100, 1, 8000);
		STEP_INCREASE = builder
			.comment("Fractional increase of water consumption per additional rpmPerBlock of fan speed (0.25 = +25% per 64 RPM)")
			.defineInRange("consumptionStepIncrease", 0.25, 0.0, 10.0);
		ICE_DIVISOR = builder
			.comment("Water consumption divisor while the column is fully encased in a blue ice tub")
			.defineInRange("iceConsumptionDivisor", 5.0, 1.0, 100.0);
		DRAIN_REQUIRES_ITEMS = builder
			.comment("If true, water is only consumed while washable items are actually in the column;",
				"if false, an active column (fan spinning fast enough above a watered basin) always consumes water")
			.define("drainRequiresItems", false);
		builder.pop();
		SPEC = builder.build();
	}

	public static int rpmPerBlock() {
		return SPEC.isLoaded() ? RPM_PER_BLOCK.get() : 64;
	}

	public static int baseConsumption() {
		return SPEC.isLoaded() ? BASE_CONSUMPTION.get() : 100;
	}

	public static double stepIncrease() {
		return SPEC.isLoaded() ? STEP_INCREASE.get() : 0.25;
	}

	public static double iceDivisor() {
		return SPEC.isLoaded() ? ICE_DIVISOR.get() : 5.0;
	}

	public static boolean drainRequiresItems() {
		return SPEC.isLoaded() && DRAIN_REQUIRES_ITEMS.get();
	}
}
