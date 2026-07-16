package xyz.petablock.createnetherwashing.fan;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fan processing applied inside a valid {@link WashColumn}. Item processing delegates to
 * Create's splashing, so every overworld fan-washing recipe works. Three instances are
 * registered, one per distance tier; the time multiplier is applied by FanProcessingMixin.
 */
public class BasinWashingType implements FanProcessingType {
	private final float timeMultiplier;

	public BasinWashingType(float timeMultiplier) {
		this.timeMultiplier = timeMultiplier;
	}

	public float getTimeMultiplier() {
		return timeMultiplier;
	}

	/**
	 * Scales the initial fan processing time (hooked via mixin on both the belt and the
	 * item-entity code path in FanProcessing).
	 */
	public static Object scaleProcessingTime(Object originalTime, FanProcessingType type) {
		if (type instanceof BasinWashingType washing && originalTime instanceof Integer time)
			return (int) (time * washing.timeMultiplier);
		return originalTime;
	}

	@Override
	public boolean isValidAt(Level level, BlockPos pos) {
		WashColumn column = WashColumnResolver.resolve(level, pos);
		return column != null && NWFanProcessingTypes.forColumn(column) == this;
	}

	@Override
	public int getPriority() {
		return 450;
	}

	@Override
	public boolean canProcess(ItemStack stack, Level level) {
		return AllFanProcessingTypes.SPLASHING.canProcess(stack, level);
	}

	@Override
	@Nullable
	public List<ItemStack> process(ItemStack stack, Level level) {
		return AllFanProcessingTypes.SPLASHING.process(stack, level);
	}

	@Override
	public void spawnProcessingParticles(Level level, Vec3 pos) {
		if (level.random.nextInt(8) != 0)
			return;
		Vector3f color = new Color(0x88CCFF).asVectorF();
		level.addParticle(new DustParticleOptions(color, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
			pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
		level.addParticle(ParticleTypes.SPIT, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
			pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
	}

	@Override
	public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
		particleAccess.setColor(Color.mixColors(0x66AAFF, 0x99DDFF, random.nextFloat()));
		particleAccess.setAlpha(.75f);
		if (random.nextFloat() < 1 / 32f)
			particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE, .125f);
		if (random.nextFloat() < 1 / 64f)
			particleAccess.spawnExtraParticle(ParticleTypes.CLOUD, .0625f);
	}

	@Override
	public void affectEntity(Entity entity, Level level) {
		AllFanProcessingTypes.SPLASHING.affectEntity(entity, level);
	}
}
