package mcp.mobius.betterbarrels.common.items.dolly;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.JabbaCreativeTab;

public class ItemFoldedBarrelMover extends Item {

    public ItemFoldedBarrelMover() {
        super();
        this.setCreativeTab(JabbaCreativeTab.tab);
        this.setNoRepair();
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = par1IconRegister.registerIcon(BetterBarrels.modid + ":dolly_normal_folded");
    }

    @Override
    public String getUnlocalizedName() {
        return getUnlocalizedName(null);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.dolly.normal.folded";
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (!world.isRemote && itemStack.stackSize > 0) {
            final EntityItem newItem = new EntityItem(
                    world,
                    player.posX,
                    player.posY,
                    player.posZ,
                    new ItemStack(BetterBarrels.itemMover, 1));
            newItem.delayBeforeCanPickup = 0;
            world.spawnEntityInWorld(newItem);

            itemStack.stackSize -= 1;
        }
        return itemStack;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List tooltip, boolean p_77624_4_) {
        super.addInformation(itemStack, player, tooltip, p_77624_4_);
        tooltip.add(StatCollector.translateToLocal("item.dolly.folded_hint"));
    }
}
