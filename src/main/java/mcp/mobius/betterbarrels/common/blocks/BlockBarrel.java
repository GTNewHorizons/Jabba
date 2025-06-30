package mcp.mobius.betterbarrels.common.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.IconFlipped;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import fox.spiteful.avaritia.items.ItemMatterCluster;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.bspace.BSpaceStorageHandler;
import mcp.mobius.betterbarrels.common.JabbaCreativeTab;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeSide;

public class BlockBarrel extends BlockContainer {

    public static IIcon text_sidehopper = null;
    public static IIcon text_siders = null;
    public static IIcon text_lock = null;
    public static IIcon text_linked = null;
    public static IIcon text_locklinked = null;

    public BlockBarrel() {
        super(new Material(MapColor.woodColor) {

            {
                this.setBurning();
                this.setAdventureModeExempt();
            }
        });
        this.setHardness(2.0F);
        this.setResistance(5.0F);
        this.setHarvestLevel("axe", 1);
        this.setBlockName("blockbarrel");
        this.setCreativeTab(JabbaCreativeTab.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return new TileEntityBarrel();
    }

    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        BlockBarrel.text_sidehopper = iconRegister.registerIcon(BetterBarrels.modid + ":" + "facade_hopper");
        BlockBarrel.text_siders = iconRegister.registerIcon(BetterBarrels.modid + ":" + "facade_redstone");
        BlockBarrel.text_lock = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_locked");
        BlockBarrel.text_linked = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_linked");
        BlockBarrel.text_locklinked = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_lockedlinked");
        for (int i = 0; i < StructuralLevel.LEVELS.length; i++)
            StructuralLevel.LEVELS[i].clientData.registerBlockIcons(iconRegister, i);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack par6ItemStack) {
        // We get the orientation and check if the TE is already properly created.
        // If so we set the entity value to the correct orientation and set the block meta to 1 to kill the normal block
        // rendering.

        TileEntity te = world.getTileEntity(x, y, z);

        if (!(te instanceof TileEntityBarrel)) {
            BetterBarrels.log.error(
                    "TileEntity for barrel placed at (X:" + x
                            + ", Y:"
                            + y
                            + ", Z:"
                            + z
                            + ") is "
                            + (te == null ? "null!" : "of the wrong type(" + te.getClass().getCanonicalName() + ")!"));
            return;
        }

        TileEntityBarrel barrelEntity = (TileEntityBarrel) te;

        barrelEntity.orientation = Utils.getDirectionFacingEntity(entity, BetterBarrels.allowVerticalPlacement);
        barrelEntity.rotation = Utils.getDirectionFacingEntity(entity, false);

        barrelEntity.sideUpgrades[barrelEntity.orientation.ordinal()] = UpgradeSide.FRONT;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return this.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (player.capabilities.isCreativeMode && !player.isSneaking()) {
            this.onBlockClicked(world, x, y, z, player);
            return false;
        } else {
            return world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            ((TileEntityBarrel) tileEntity).leftClick(player);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
            float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            ((TileEntityBarrel) tileEntity).rightClick(player, side);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.isRemote) return;

        // gets the TE in the world without creating it. since we're doing destruction cleanup, we don't want to create
        // just to destroy
        TileEntity te = Utils.getTileEntityWithoutCreating(world, x, y, z);
        if (te == null) {
            return;
        } else if (te instanceof TileEntityBarrel barrelEntity) {

            // We drop the structural upgrades
            if (barrelEntity.coreUpgrades.levelStructural > 0) {
                int currentUpgrade = barrelEntity.coreUpgrades.levelStructural;
                while (currentUpgrade > 0) {
                    ItemStack droppedStack = new ItemStack(BetterBarrels.itemUpgradeStructural, 1, currentUpgrade - 1);
                    spawnStackInWorld(world, x, y, z, droppedStack);
                    currentUpgrade -= 1;
                }
            }

            // We drop the core upgrades
            for (UpgradeCore core : barrelEntity.coreUpgrades.upgradeList) {
                ItemStack droppedStack = new ItemStack(BetterBarrels.itemUpgradeCore, 1, core.ordinal());
                spawnStackInWorld(world, x, y, z, droppedStack);
            }

            // We drop the side upgrades
            for (int i = 0; i < 6; i++) {
                Item upgrade = UpgradeSide.mapItem[barrelEntity.sideUpgrades[i]];
                if (upgrade != null) {
                    ItemStack droppedStack = new ItemStack(
                            upgrade,
                            1,
                            UpgradeSide.mapMeta[barrelEntity.sideUpgrades[i]]);
                    spawnStackInWorld(world, x, y, z, droppedStack);
                }
            }

            // These can potential affect dropped stack quantities, ensure they are removed before dropping items
            barrelEntity.setCreative(false);
            barrelEntity.setVoid(false);

            // We drop the stacks
            if (barrelEntity.getStorage().hasItem() && !barrelEntity.getLinked()) {
                barrelEntity.updateEntity();
                dropBarrelContents(world, x, y, z, barrelEntity);
            }

            try {
                BSpaceStorageHandler.instance().unregisterEnderBarrel(barrelEntity.id);
            } catch (Exception e) {
                BetterBarrels.log.info("Tried to remove the barrel from the index without a valid entity");
            }
        } else {
            BetterBarrels.log.error(
                    "TileEntity for barrel being broken at (X:" + x
                            + ", Y:"
                            + y
                            + ", Z:"
                            + z
                            + ") is of the wrong type("
                            + te.getClass().getCanonicalName()
                            + ")!");
        }

        // All finished here, let's ensure the TE is cleaned up...
        world.removeTileEntity(x, y, z);
    }

    private static void forEachStackOfBarrel(TileEntityBarrel barrel, Consumer<ItemStack> action) {
        while (barrel.getStorage().getAmount() > 0) {
            ItemStack stack = barrel.getStorage().getStack();
            if (stack == null || stack.stackSize == 0) break;
            action.accept(stack);
        }
    }

    /**
     * Spawns a copy of the stack in the world, breaks it down in multiple stacks if the stack size exceeds the
     * {@link ItemStack#getMaxStackSize()}
     */
    private static void spawnStackInWorld(World world, int x, int y, int z, ItemStack stack) {
        final Random rand = world.rand;
        while (stack.stackSize > 0) {
            final int stackSize = Math.min(stack.stackSize, stack.getMaxStackSize());
            stack.stackSize -= stackSize;
            final ItemStack newStack = new ItemStack(stack.getItem(), stackSize, stack.getItemDamage());
            if (stack.hasTagCompound()) {
                newStack.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
            }
            final EntityItem entityItem = new EntityItem(
                    world,
                    x + rand.nextFloat() * 0.8f + 0.1f,
                    y + rand.nextFloat() * 0.8f + 0.1f,
                    z + rand.nextFloat() * 0.8f + 0.1f,
                    newStack);
            entityItem.motionX = rand.nextGaussian() * 0.05f;
            entityItem.motionY = rand.nextGaussian() * 0.05f + 0.2f;
            entityItem.motionZ = rand.nextGaussian() * 0.05f;
            world.spawnEntityInWorld(entityItem);
        }
    }

    /**
     * Drops stacks with an "illegal" size that will contain all the items in one stack. The downside of this method is
     * that if the ItemStack is still on the ground when the chunk is saved (stopping game, or going away). It will not
     * save the size of the ItemStack correctly since the size is stored as a byte (max 255)
     * {@link net.minecraft.item.ItemStack#writeToNBT(NBTTagCompound)}, ITEMS WILL BE LOST !!
     */
    private static void dropMergedStacks(TileEntityBarrel barrel, World world, int x, int y, int z) {
        final ItemStack storedStack = barrel.getStorage().getItem();
        if (storedStack.isStackable()) {
            final ItemStack copy = storedStack.copy();
            copy.stackSize = barrel.getStorage().getAmount();
            dropBigStackInWorld(world, x, y, z, copy);
        } else {
            dropAllStacksOfBarrel(barrel, world, x, y, z);
        }
    }

    /**
     * Drops an ItemStack with an "illegal" size that will contain all the items in one stack. The downside of this
     * method is that if the ItemStack is still on the ground when the chunk is saved (stopping game, or going away). It
     * will not save the size of the ItemStack correctly since the size is stored as a byte (max 255)
     * {@link net.minecraft.item.ItemStack#writeToNBT(NBTTagCompound)}, ITEMS WILL BE LOST !!
     */
    private static void dropBigStackInWorld(World world, int x, int y, int z, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        Random rand = world.rand;
        float ex = rand.nextFloat() * 0.8f + 0.1f;
        float ey = rand.nextFloat() * 0.8f + 0.1f;
        float ez = rand.nextFloat() * 0.8f + 0.1f;
        EntityItem entity = new EntityItem(world, x + ex, y + ey, z + ez, stack);
        if (stack.hasTagCompound()) {
            entity.getEntityItem().setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
        }
        world.spawnEntityInWorld(entity);
    }

    private static void dropBarrelContents(World world, int x, int y, int z, TileEntityBarrel barrel) {
        final int stacksToSpawn = countAmountOfStacksToSpawn(barrel);
        if (stacksToSpawn == 0) return;
        if (stacksToSpawn <= 64) {
            dropAllStacksOfBarrel(barrel, world, x, y, z);
        } else if (Loader.isModLoaded("Avaritia")) {
            dropAvaritiaClusters(barrel, world, x, y, z);
        } else {
            dropMergedStacks(barrel, world, x, y, z);
        }
    }

    /**
     * Counts the amount of stacks that would drop if we were to break this barrel.
     */
    private static int countAmountOfStacksToSpawn(TileEntityBarrel barrel) {
        final ItemStack stack = barrel.getStorage().getItem();
        if (stack == null || stack.getItem() == null) return 0;
        int stackCount = 0;
        final int maxStackSize = stack.getMaxStackSize();
        final int storedItemCount = barrel.getStorage().getAmount();
        stackCount += storedItemCount / maxStackSize;
        if (storedItemCount % maxStackSize != 0) stackCount++;
        return stackCount;
    }

    /**
     * Drops Avaritia matter clusters with all the items.
     */
    @Optional.Method(modid = "Avaritia")
    private static void dropAvaritiaClusters(TileEntityBarrel barrel, World world, int x, int y, int z) {
        List<ItemStack> list = new ArrayList<>();
        forEachStackOfBarrel(barrel, list::add);
        List<ItemStack> clusters = ItemMatterCluster.makeClusters(list);
        for (ItemStack stack : clusters) {
            spawnStackInWorld(world, x, y, z, stack);
        }
    }

    /**
     * Drops all the stacks contained is this barrel.
     */
    private static void dropAllStacksOfBarrel(TileEntityBarrel barrel, World world, int x, int y, int z) {
        forEachStackOfBarrel(barrel, stack -> spawnStackInWorld(world, x, y, z, stack));
    }

    /* REDSTONE HANDLING */

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return this.isProvidingWeakPower(world, x, y, z, side);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x, y, z);

        if (te == null || !(te instanceof TileEntityBarrel)) {
            return 0;
        }

        return ((TileEntityBarrel) te).getRedstonePower(side);
    }

    private int redstoneToMC(int redSide) {
        switch (redSide) {
            default:
            case -1:
                return 1;
            case 0:
                return 2;
            case 1:
                return 5;
            case 2:
                return 3;
            case 3:
                return 4;
        }
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x, y, z);

        if (te == null || !(te instanceof TileEntityBarrel)) {
            return super.canConnectRedstone(world, x, y, z, side);
        }

        TileEntityBarrel barrel = (TileEntityBarrel) te;

        if (barrel.sideUpgrades[redstoneToMC(side)] == UpgradeSide.REDSTONE) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int dir) {
        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x, y, z);

        if (te == null || !(te instanceof TileEntityBarrel)) {
            return 0;
        }

        IBarrelStorage store = ((TileEntityBarrel) te).getStorage();
        int currentAmount = store.getAmount();
        int maxStorable = store.getMaxStoredCount();

        if (currentAmount == 0) return 0;
        else if (currentAmount == maxStorable) return 15;
        else return MathHelper.floor_float(((float) currentAmount / (float) maxStorable) * 14) + 1;
    }

    /* End Redstone Stuff */

    // Will allow for torches to be placed on non-labeled sides
    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        if (side == ForgeDirection.DOWN) {
            // prevent barrels from "blocking" chests from opening beneath them
            return false;
        }

        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x, y, z);

        if (te == null || !(te instanceof TileEntityBarrel)) {
            return super.isSideSolid(world, x, y, z, side);
        }

        TileEntityBarrel barrel = (TileEntityBarrel) te;

        if (barrel.sideUpgrades[side.ordinal()] == UpgradeSide.FRONT
                || barrel.sideUpgrades[side.ordinal()] == UpgradeSide.STICKER) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        // return false;
        // this method may be useful for certain planned upgrades ;)
        // for now, skipping some superclass logic...
        return isSideSolid(world, x, y, z, ForgeDirection.UP);
    }

    /* Rendering stuff */
    @Override
    public int getRenderType() {
        BetterBarrels.proxy.checkRenderers();

        return BetterBarrels.blockBarrelRendererID;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x, y, z);

        if (te == null || !(te instanceof TileEntityBarrel)) {
            return Blocks.planks.getIcon(world, x, y, z, side);
        }

        TileEntityBarrel barrel = (TileEntityBarrel) te;

        int levelStructural = barrel.coreUpgrades.levelStructural;

        boolean ghosting = barrel.getStorage().isGhosting();
        boolean linked = barrel.getLinked();
        boolean sideIsLabel = (barrel.sideUpgrades[side] == UpgradeSide.FRONT
                || barrel.sideUpgrades[side] == UpgradeSide.STICKER);

        // note, this default should always be overwritten, just setting a default to prevent NPE's
        IIcon ret = StructuralLevel.LEVELS[levelStructural].clientData.getIconLabel();

        if (barrel.overlaying) {
            if (barrel.sideUpgrades[side] == UpgradeSide.HOPPER) {
                ret = BlockBarrel.text_sidehopper;
            } else if (barrel.sideUpgrades[side] == UpgradeSide.REDSTONE) {
                ret = BlockBarrel.text_siders;
            } else if (sideIsLabel) {
                if (ghosting && linked) {
                    ret = BlockBarrel.text_locklinked;
                } else if (ghosting) {
                    ret = BlockBarrel.text_lock;
                } else if (linked) {
                    ret = BlockBarrel.text_linked;
                }
            }
        } else {
            if ((side == 0 || side == 1) && sideIsLabel) {
                ret = StructuralLevel.LEVELS[levelStructural].clientData.getIconLabelTop();
            } else if ((side == 0 || side == 1) && !sideIsLabel) {
                ret = StructuralLevel.LEVELS[levelStructural].clientData.getIconTop();
            } else if (sideIsLabel) {
                ret = StructuralLevel.LEVELS[levelStructural].clientData.getIconLabel();
            } else {
                ret = StructuralLevel.LEVELS[levelStructural].clientData.getIconSide();
            }
        }

        return side == 0 ? new IconFlipped(ret, true, false) : ret;
    }

    @Override
    public IIcon getIcon(int p_149691_1_, int p_149691_2_) {
        return BlockBarrel.text_linked;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[side];
        TileEntity te = Utils.getTileEntityPreferNotCreating(world, x - dir.offsetX, y - dir.offsetY, z - dir.offsetZ);

        if (te == null || !(te instanceof TileEntityBarrel) || !((TileEntityBarrel) te).overlaying) {
            return super.shouldSideBeRendered(world, x, y, z, side);
        }

        TileEntityBarrel barrel = (TileEntityBarrel) te;

        boolean ghosting = barrel.getStorage().isGhosting();
        boolean linked = barrel.getLinked();
        boolean sideIsLabel = (barrel.sideUpgrades[side] == UpgradeSide.FRONT
                || barrel.sideUpgrades[side] == UpgradeSide.STICKER);

        if (barrel.sideUpgrades[side] == UpgradeSide.HOPPER) {
            return true;
        } else if (barrel.sideUpgrades[side] == UpgradeSide.REDSTONE) {
            return true;
        } else if (sideIsLabel) {
            return ghosting || linked;
        }

        return false;
    }
}
