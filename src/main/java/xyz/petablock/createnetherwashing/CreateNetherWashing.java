package xyz.petablock.createnetherwashing;

import xyz.petablock.createnetherwashing.fan.NWFanProcessingTypes;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CreateNetherWashing.MOD_ID)
public class CreateNetherWashing {
	public static final String MOD_ID = "createnetherwashing";

	public CreateNetherWashing(IEventBus modEventBus, ModContainer modContainer) {
		NWFanProcessingTypes.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.SERVER, NWConfig.SPEC);
	}
}
