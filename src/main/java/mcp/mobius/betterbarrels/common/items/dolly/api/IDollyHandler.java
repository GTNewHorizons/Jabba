package mcp.mobius.betterbarrels.common.items.dolly.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface IDollyHandler {

    void onContainerPickup(World world, int x, int y, int z, TileEntity tileEntity);
}
