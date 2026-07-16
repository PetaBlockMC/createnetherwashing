package xyz.petablock.createnetherwashing.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import xyz.petablock.createnetherwashing.fan.BasinWashingType;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/**
 * Scales the initial fan processing time by the wash column's distance/ice multiplier.
 * Targets the single AllConfigs...fanProcessingTime.get() call in each processing path.
 */
@Mixin(value = FanProcessing.class, remap = false)
public class FanProcessingMixin {
	@ModifyExpressionValue(
		method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;"
			+ "Lnet/minecraft/world/level/Level;"
			+ "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)"
			+ "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;"))
	private static Object createnetherwashing$scaleBeltProcessingTime(Object original, TransportedItemStack transported,
		Level world, FanProcessingType type) {
		return BasinWashingType.scaleProcessingTime(original, type);
	}

	@ModifyExpressionValue(
		method = "decrementProcessingTime",
		at = @At(value = "INVOKE",
			target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;"))
	private static Object createnetherwashing$scaleEntityProcessingTime(Object original, ItemEntity entity,
		FanProcessingType type) {
		return BasinWashingType.scaleProcessingTime(original, type);
	}
}
