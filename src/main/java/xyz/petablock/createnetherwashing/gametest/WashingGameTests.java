package xyz.petablock.createnetherwashing.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import xyz.petablock.createnetherwashing.CreateNetherWashing;
import xyz.petablock.createnetherwashing.fan.WashColumn;
import xyz.petablock.createnetherwashing.fan.WashColumnResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateNetherWashing.MOD_ID)
@PrefixGameTestTemplate(false)
public class WashingGameTests {
	private static final String TEMPLATE = "empty_column";
	private static final BlockPos BASIN_POS = new BlockPos(1, 1, 1);
	private static final int WATER = 1000;

	@GameTest(template = TEMPLATE, timeoutTicks = 400)
	public static void gap1Washes(GameTestHelper helper) {
		setupColumn(helper, 1, 64, false);
		ItemEntity[] item = spawnGravelDelayed(helper, 2.5f);
		helper.succeedWhen(() -> {
			helper.assertTrue(item[0] != null, "item not spawned yet");
			helper.assertTrue("createnetherwashing:basin_washing".equals(processingType(item[0])),
				"item not processed by basin_washing, got: '" + processingType(item[0]) + "'");
			helper.assertTrue(waterAmount(helper) < WATER, "no water was drained");
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void gap2Needs128Rpm(GameTestHelper helper) {
		setupColumn(helper, 2, 64, false);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(resolve(helper) == null, "column resolved despite insufficient RPM");
			helper.assertTrue(waterAmount(helper) == WATER, "water was drained without washing");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 400)
	public static void gap2At128WashesSlower(GameTestHelper helper) {
		setupColumn(helper, 2, 128, false);
		ItemEntity[] item = spawnGravelDelayed(helper, 3.5f);
		helper.succeedWhen(() -> {
			helper.assertTrue(item[0] != null, "item not spawned yet");
			helper.assertTrue("createnetherwashing:basin_washing_mid".equals(processingType(item[0])),
				"item not processed by basin_washing_mid, got: '" + processingType(item[0]) + "'");
			// 1.5x time multiplier: initial time 226 (vs 151 unscaled) - proves the mixin applied
			helper.assertTrue(processingTime(item[0]) > 160, "processing time not scaled by 1.5x");
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void gap3Needs192Rpm(GameTestHelper helper) {
		setupColumn(helper, 3, 128, false);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(resolve(helper) == null, "gap-3 column resolved at 128 RPM");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void gap4NeverWashes(GameTestHelper helper) {
		setupColumn(helper, 4, 256, false);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(resolve(helper) == null, "gap-4 column resolved but max gap is 3");
			helper.assertTrue(waterAmount(helper) == WATER, "water was drained without washing");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 400)
	public static void iceTubNormalizesDistance(GameTestHelper helper) {
		setupColumn(helper, 3, 192, true);
		ItemEntity[] item = spawnGravelDelayed(helper, 3.5f);
		helper.succeedWhen(() -> {
			WashColumn column = resolve(helper);
			helper.assertTrue(column != null && column.iced(), "ice tub not detected");
			helper.assertTrue(item[0] != null, "item not spawned yet");
			// full speed tier despite gap 3
			helper.assertTrue("createnetherwashing:basin_washing".equals(processingType(item[0])),
				"ice tub column should wash at the full-speed tier, got: '" + processingType(item[0]) + "'");
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void idleColumnDrains(GameTestHelper helper) {
		setupColumn(helper, 1, 64, false);
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(resolve(helper) != null, "column should be valid");
			helper.assertTrue(waterAmount(helper) < WATER, "active column should consume water even without items");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void fanTouchingDepotStillDrains(GameTestHelper helper) {
		// user-reported setup: basin, item handler (belt/depot) directly on top, fan directly above that
		setupColumn(helper, 1, 64, false);
		helper.setBlock(BASIN_POS.above(), AllBlocks.DEPOT.getDefaultState());
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(resolve(helper) != null, "column with depot in the gap should be valid");
			helper.assertTrue(waterAmount(helper) < WATER, "column with touching fan should consume water");
			helper.succeed();
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void blowingFanDoesNotWash(GameTestHelper helper) {
		// fan blows down at the basin instead of pulling
		setupColumn(helper, 1, 64, false, Direction.DOWN);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(resolve(helper) == null, "blowing fan should not create a wash column");
			helper.succeed();
		});
	}

	// ---- setup & helpers ----

	private static void setupColumn(GameTestHelper helper, int gap, int rpm, boolean ice) {
		setupColumn(helper, gap, rpm, ice, Direction.UP);
	}

	private static void setupColumn(GameTestHelper helper, int gap, int rpm, boolean ice, Direction airflow) {
		BlockPos fanPos = BASIN_POS.above(gap + 1);
		BlockPos motorPos = fanPos.above();
		helper.setBlock(BASIN_POS, AllBlocks.BASIN.getDefaultState());
		helper.setBlock(fanPos, AllBlocks.ENCASED_FAN.getDefaultState()
			.setValue(EncasedFanBlock.FACING, Direction.DOWN));
		helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
			.setValue(CreativeMotorBlock.FACING, Direction.DOWN));

		if (ice) {
			for (int i = 1; i <= gap; i++) {
				BlockPos p = BASIN_POS.above(i);
				helper.setBlock(p.west(), Blocks.BLUE_ICE.defaultBlockState());
				helper.setBlock(p.east(), Blocks.BLUE_ICE.defaultBlockState());
				helper.setBlock(p.north(), Blocks.BLUE_ICE.defaultBlockState());
				helper.setBlock(p.south(), Blocks.STONE.defaultBlockState());
			}
		}

		if (helper.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor)
			motor.generatedSpeed.setValue(airflow == Direction.UP ? -rpm : rpm);
		if (helper.getBlockEntity(BASIN_POS) instanceof BasinBlockEntity basin)
			basin.inputTank.getCapability()
				.fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);

		// The motor value sign that yields the wanted airflow depends on kinetic conventions;
		// verify and fix once rotation has propagated (twice, in case a flip needs to settle).
		// Tests assert no earlier than tick 20.
		helper.runAfterDelay(4, () -> setAirflow(helper, gap, airflow));
		helper.runAfterDelay(8, () -> setAirflow(helper, gap, airflow));
	}

	/**
	 * Spawns gravel only after airflow direction is settled (tick 12, past the fixers at 4/8);
	 * spawning while the fan still blows down would push the item into the basin, which eats it.
	 */
	private static ItemEntity[] spawnGravelDelayed(GameTestHelper helper, float y) {
		ItemEntity[] holder = new ItemEntity[1];
		helper.runAfterDelay(12, () -> holder[0] = helper.spawnItem(Items.GRAVEL, 1.5f, y, 1.5f));
		return holder;
	}

	private static void setAirflow(GameTestHelper helper, int gap, Direction wanted) {
		BlockPos fanPos = BASIN_POS.above(gap + 1);
		if (helper.getBlockEntity(fanPos) instanceof EncasedFanBlockEntity fan
			&& fan.getAirFlowDirection() != null && fan.getAirFlowDirection() != wanted
			&& helper.getBlockEntity(fanPos.above()) instanceof CreativeMotorBlockEntity motor)
			motor.generatedSpeed.setValue(-motor.generatedSpeed.getValue());
	}

	private static WashColumn resolve(GameTestHelper helper) {
		return WashColumnResolver.resolveFromBasin(helper.getLevel(), helper.absolutePos(BASIN_POS));
	}

	private static int waterAmount(GameTestHelper helper) {
		if (!(helper.getBlockEntity(BASIN_POS) instanceof BasinBlockEntity basin))
			return -1;
		IFluidHandler handler = basin.inputTank.getCapability();
		int total = 0;
		for (int i = 0; i < handler.getTanks(); i++)
			total += handler.getFluidInTank(i).getAmount();
		return total;
	}

	private static CompoundTag processingTag(ItemEntity item) {
		return item.getPersistentData()
			.getCompound("CreateData")
			.getCompound("Processing");
	}

	private static String processingType(ItemEntity item) {
		return processingTag(item).getString("Type");
	}

	private static int processingTime(ItemEntity item) {
		return processingTag(item).getInt("Time");
	}
}
