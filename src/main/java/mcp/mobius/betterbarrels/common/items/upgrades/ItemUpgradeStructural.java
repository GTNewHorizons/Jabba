package mcp.mobius.betterbarrels.common.items.upgrades;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcp.mobius.betterbarrels.common.StructuralLevel;

public class ItemUpgradeStructural extends ItemUpgrade {

    public ItemUpgradeStructural() {
        super();
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setMaxStackSize(16);
        this.setUnlocalizedName("item.upgrade.structural.generic");
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.upgrade.structural." + String.valueOf(stack.getItemDamage() + 1);
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        for (int i = 1; i < StructuralLevel.LEVELS.length; i++)
            StructuralLevel.LEVELS[i].clientData.registerItemIcon(par1IconRegister, i);
    }

    @Override
    public IIcon getIconFromDamage(int i) {
        return StructuralLevel.LEVELS[Math.min(i + 1, StructuralLevel.LEVELS.length - 1)].clientData.getIconItem();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tabs, List list) {
        for (int i = 1; i < StructuralLevel.LEVELS.length; ++i) {
            list.add(new ItemStack(item, 1, i - 1));
        }
    }
}
