package mcp.mobius.betterbarrels.common;

import mcp.mobius.betterbarrels.BetterBarrels;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class JabbaCreativeTab extends CreativeTabs {
	public static JabbaCreativeTab tab = new JabbaCreativeTab();

	public JabbaCreativeTab() {
		super("jabba");
	}

	@Override
	public ItemStack getIconItemStack() {
		return new ItemStack(BetterBarrels.blockBarrel);
	}

	@Override
	public Item getTabIconItem() {
		return Item.getItemFromBlock(BetterBarrels.blockBarrel);
	}
}
