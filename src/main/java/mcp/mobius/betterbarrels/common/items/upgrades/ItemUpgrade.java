package mcp.mobius.betterbarrels.common.items.upgrades;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import mcp.mobius.betterbarrels.common.JabbaCreativeTab;
import mcp.mobius.betterbarrels.common.items.IOverlayItem;

public class ItemUpgrade extends Item implements IOverlayItem {

    public ItemUpgrade() {
        super();
        this.setCreativeTab(JabbaCreativeTab.tab);
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }
}
