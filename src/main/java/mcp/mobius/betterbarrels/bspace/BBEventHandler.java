package mcp.mobius.betterbarrels.bspace;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcp.mobius.betterbarrels.common.items.dolly.ItemBarrelMover;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeStructural;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;

public class BBEventHandler {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0)
            BSpaceStorageHandler.instance().loadFromFile();
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event.itemStack.getItem() instanceof ItemUpgradeCore) {
            event.toolTip.add(
                    1,
                    StatCollector.translateToLocal("text.jabba.tooltip.slots.used")
                            + UpgradeCore.values()[event.itemStack.getItemDamage()].slotsUsed);
        }

        if (event.itemStack.getItem() instanceof ItemUpgradeStructural) {
            int nslots = 0;
            for (int i = 0; i < event.itemStack.getItemDamage() + 1; i++)
                nslots += MathHelper.floor_double(Math.pow(2, i));

            event.toolTip.add(1, StatCollector.translateToLocal("text.jabba.tooltip.slots.provided") + nslots);
        }

        if (event.itemStack.getItem() instanceof ItemBarrelMover) {
            if (event.itemStack.hasTagCompound() && event.itemStack.getTagCompound().hasKey("Container")) {
                NBTTagCompound tag = event.itemStack.getTagCompound().getCompoundTag("Container");
                Block storedBlock;
                if (tag.hasKey("ID")) {
                    storedBlock = Block.getBlockById(tag.getInteger("ID"));
                } else {
                    storedBlock = Block.getBlockFromName(tag.getString("Block"));
                }
                int meta = tag.getInteger("Meta");
                String tip = new ItemStack(storedBlock, 0, meta).getDisplayName();
                if (tag.getBoolean("isSpawner")) {
                    tip = String.format("%s (%s)", tip, tag.getCompoundTag("NBT").getString("EntityId"));
                }
                event.toolTip.add(1, tip);
            } else {
                event.toolTip.add(1, StatCollector.translateToLocal("text.jabba.tooltip.empty"));
            }
        }
    }
}
