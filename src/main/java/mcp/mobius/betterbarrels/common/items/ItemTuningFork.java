package mcp.mobius.betterbarrels.common.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.JabbaCreativeTab;

public class ItemTuningFork extends Item implements IOverlayItem {

    private static IIcon text_hand = null;

    public ItemTuningFork() {
        super();
        this.setMaxDamage(30); // Time it stays tuned, in sec.
        this.setMaxStackSize(1);
        this.setUnlocalizedName("fork");
        this.setNoRepair();
        this.setCreativeTab(JabbaCreativeTab.tab);
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = par1IconRegister.registerIcon(BetterBarrels.modid + ":" + "bspace_fork_inv");
        text_hand = par1IconRegister.registerIcon(BetterBarrels.modid + ":" + "bspace_fork_hand");
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        return text_hand;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity player, int par4, boolean par5) {
        if (world.getTotalWorldTime() % 20 == 0) {

            if (stack.getItemDamage() != 0) stack.setItemDamage(stack.getItemDamage() + 1);

            if (stack.getItemDamage() == this.getMaxDamage()) {
                stack.setTagCompound(new NBTTagCompound());
                stack.setItemDamage(0);
            }
        }
    }
}
