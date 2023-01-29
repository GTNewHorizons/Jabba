package mcp.mobius.betterbarrels.common.items.dolly;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemDiamondMover extends ItemBarrelMover {

    public ItemDiamondMover() {
        super();
        this.setMaxDamage(6);
        this.type = DollyType.DIAMOND;
    }

    @Override
    protected boolean canPickSpawners() {
        return true;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }

        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("Container")) {
            return this.pickupContainer(stack, player, world, x, y, z);
        }

        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Container")) {
            boolean isSpawner = stack.getTagCompound().getCompoundTag("Container").getBoolean("isSpawner");
            boolean ret = this.placeContainer(stack, player, world, x, y, z, side);
            if (isSpawner) {
                if (ret) stack.setItemDamage(stack.getItemDamage() + 1);
                if (stack.getItemDamage() >= stack.getMaxDamage()) stack.stackSize -= 1;
            }
            return ret;
        }

        return false;
    }
}
