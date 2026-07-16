package xyz.petablock.createnetherwashing.fan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

/**
 * A validated basin-washing setup: a basin holding a washing fluid, an encased fan
 * {@code gap} blocks above it facing down and pulling air up through the column.
 */
public record WashColumn(BlockPos basinPos, BlockPos fanPos, int gap, boolean iced, float rpm, Fluid fluid) {
}
