package mcp.mobius.betterbarrels.common.items.upgrades;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcp.mobius.betterbarrels.BetterBarrels;

public class ItemUpgradeSide extends ItemUpgrade {

    public static IIcon[] upgradeIcons = new IIcon[3];

    public static String[] upgradeNames = { "upgrade.side.sticker", "upgrade.side.hopper", "upgrade.side.redstone" };

    public ItemUpgradeSide() {
        super();
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setMaxStackSize(16);
        this.setUnlocalizedName("upgrade.side.generic");
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item." + ItemUpgradeSide.upgradeNames[Math
                .min(stack.getItemDamage(), ItemUpgradeSide.upgradeNames.length - 1)];
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        for (int i = 0; i < ItemUpgradeSide.upgradeIcons.length; i++) ItemUpgradeSide.upgradeIcons[i] = par1IconRegister
                .registerIcon(BetterBarrels.modid + ":" + "sideupg_" + String.valueOf(i));
    }

    @Override
    public IIcon getIconFromDamage(int i) {
        return ItemUpgradeSide.upgradeIcons[Math.min(i, ItemUpgradeSide.upgradeIcons.length - 1)];
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tabs, List list) {
        for (int i = 0; i < ItemUpgradeSide.upgradeIcons.length; ++i) {
            list.add(new ItemStack(item, 1, i));
        }
    }
}
