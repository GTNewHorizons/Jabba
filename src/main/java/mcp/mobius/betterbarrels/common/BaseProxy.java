package mcp.mobius.betterbarrels.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;

import mcp.mobius.betterbarrels.bspace.BBEventHandler;

public class BaseProxy {

    public void registerEventHandler() {
        MinecraftForge.EVENT_BUS.register(new BBEventHandler());
    }

    public void registerRenderers() {
        // Nothing here as this is the server side proxy
    }

    public void updatePlayerInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            playerMP.sendContainerToPlayer(playerMP.inventoryContainer);
        }
    }

    public void postInit() {}

    public void checkRenderers() {}

    public void initialiseClientData(int[] overrideColorData) {}
}
