package mcp.mobius.betterbarrels;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

public class BBWailaProvider implements IWailaDataProvider {

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        TileEntityBarrel tebarrel = (TileEntityBarrel) accessor.getTileEntity();
        ItemStack barrelStack = tebarrel.getStorage().getItem();

        currenttip.add(
                StatCollector.translateToLocalFormatted(
                        "text.jabba.waila.structural",
                        tebarrel.coreUpgrades.levelStructural));
        currenttip.add(
                StatCollector.translateToLocalFormatted(
                        "text.jabba.waila.upgrades",
                        tebarrel.coreUpgrades.getFreeSlots(),
                        tebarrel.coreUpgrades.getMaxUpgradeSlots()));

        if (barrelStack != null) {
            if (config.getConfig("bb.itemtype")) currenttip.add(barrelStack.getDisplayName());
            if (config.getConfig("bb.itemnumb")) currenttip.add(
                    StatCollector.translateToLocalFormatted(
                            "text.jabba.waila.items",
                            tebarrel.getStorage().getAmount(),
                            tebarrel.getStorage().getItem().getMaxStackSize() * tebarrel.getStorage().getMaxStacks()));
            if (config.getConfig("bb.space")) currenttip.add(
                    StatCollector.translateToLocalFormatted(
                            "text.jabba.waila.stacks",
                            tebarrel.getStorage().getMaxStacks()));
        } else {
            if (config.getConfig("bb.itemtype"))
                currenttip.add(StatCollector.translateToLocal("text.jabba.waila.empty"));
            if (config.getConfig("bb.space")) currenttip.add(
                    StatCollector.translateToLocalFormatted(
                            "text.jabba.waila.stacks",
                            tebarrel.getStorage().getMaxStacks()));
        }

        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return currenttip;
    }

    public static void callbackRegister(IWailaRegistrar registrar) {
        registrar.addConfig(
                StatCollector.translateToLocal("itemGroup.jabba"),
                "bb.itemtype",
                StatCollector.translateToLocal("text.jabba.waila.key.content"));
        registrar.addConfig(
                StatCollector.translateToLocal("itemGroup.jabba"),
                "bb.itemnumb",
                StatCollector.translateToLocal("text.jabba.waila.key.quantity"));
        registrar.addConfig(
                StatCollector.translateToLocal("itemGroup.jabba"),
                "bb.space",
                StatCollector.translateToLocal("text.jabba.waila.key.stacks"));
        registrar.registerBodyProvider(new BBWailaProvider(), TileEntityBarrel.class);
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
            int y, int z) {
        return tag;
    }
}

// registrar.addConfig("Better barrels", "bb.itemtype", "Barrel content");
// registrar.addConfig("Better barrels", "bb.itemnumb", "Items quantity");
// registrar.addConfig("Better barrels", "bb.space", "Max stacks");
