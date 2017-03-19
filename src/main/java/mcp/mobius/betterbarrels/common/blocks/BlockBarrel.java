package mcp.mobius.betterbarrels.common.blocks;

import java.util.Random;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.bspace.BSpaceStorageHandler;
import mcp.mobius.betterbarrels.common.JabbaCreativeTab;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeSide;
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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBarrel extends BlockContainer{

	public static IIcon   text_sidehopper = null;
	public static IIcon   text_siders     = null;
	public static IIcon   text_lock       = null;
	public static IIcon   text_linked     = null;
	public static IIcon   text_locklinked = null;

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
		BlockBarrel.text_sidehopper  = iconRegister.registerIcon(BetterBarrels.modid + ":" + "facade_hopper");
		BlockBarrel.text_siders      = iconRegister.registerIcon(BetterBarrels.modid + ":" + "facade_redstone");
		BlockBarrel.text_lock        = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_locked");
		BlockBarrel.text_linked      = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_linked");
		BlockBarrel.text_locklinked  = iconRegister.registerIcon(BetterBarrels.modid + ":" + "overlay_lockedlinked");
		for (int i = 0; i < StructuralLevel.LEVELS.length; i++)
			StructuralLevel.LEVELS[i].clientData.registerBlockIcons(iconRegister, i);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack par6ItemStack) {
		// We get the orientation and check if the TE is already properly created.
		// If so we set the entity value to the correct orientation and set the block meta to 1 to kill the normal block rendering.

		TileEntity te = world.getTileEntity(x, y, z);

		if (te == null || !(te instanceof TileEntityBarrel)) {
			BetterBarrels.log.error("TileEntity for barrel placed at (X:" + x + ", Y:" + y + ", Z:" + z + ") is " + (te == null ? "null!": "of the wrong type(" + te.getClass().getCanonicalName() + ")!"));
			return;
		}

		TileEntityBarrel barrelEntity = (TileEntityBarrel)te;

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
		if (player.capabilities.isCreativeMode && !player.isSneaking() ) {
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
			((TileEntityBarrel)tileEntity).leftClick(player);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			((TileEntityBarrel)tileEntity).rightClick(player, side);
		}
		return true;
	}

	private void dropStack(World world, ItemStack stack, int x, int y, int z) {
		Random random = new Random();
		float var10 = random.nextFloat() * 0.8F + 0.1F;
		float var11 = random.nextFloat() * 0.8F + 0.1F;
		EntityItem items;

		for (float var12 = random.nextFloat() * 0.8F + 0.1F; stack.stackSize > 0; world.spawnEntityInWorld(items)) {
			int var13 = random.nextInt(21) + 10;

			if (var13 > stack.stackSize) {
				var13 = stack.stackSize;
			}

			stack.stackSize -= var13;
			items = new EntityItem(world, x + var10, y + var11, z + var12, new ItemStack(stack.getItem(), var13, stack.getItemDamage()));
			float var15 = 0.05F;
			items.motionX = (float)random.nextGaussian() * var15;
			items.motionY = (float)random.nextGaussian() * var15 + 0.2F;
			items.motionZ = (float)random.nextGaussian() * var15;

			if (stack.hasTagCompound()) {
				items.getEntityItem().setTagCompound((NBTTagCompound)stack.getTagCompound().copy());
			}
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (world.isRemote) return;

		//gets the TE in the world without creating it. since we're doing destruction cleanup, we don't want to create just to destroy
		TileEntity te = Utils.getTileEntityWithoutCreating(world, x, y, z);
		if (te == null) {
			return;
		} else if (te instanceof TileEntityBarrel) {
			TileEntityBarrel barrelEntity = (TileEntityBarrel)te;

			// We drop the structural upgrades
			if (barrelEntity.coreUpgrades.levelStructural > 0) {
				int currentUpgrade = barrelEntity.coreUpgrades.levelStructural;
				while (currentUpgrade > 0) {
					ItemStack droppedStack = new ItemStack(BetterBarrels.itemUpgradeStructural, 1, currentUpgrade-1);
					this.dropStack(world, droppedStack, x, y, z);
					currentUpgrade -= 1;
				}
			}

			// We drop the core upgrades
			for (UpgradeCore core : barrelEntity.coreUpgrades.upgradeList) {
				ItemStack droppedStack = new ItemStack(BetterBarrels.itemUpgradeCore, 1, core.ordinal());
				this.dropStack(world, droppedStack, x, y, z);
			}

			// We drop the side upgrades
			for (int i = 0; i < 6; i++) {
				Item upgrade = UpgradeSide.mapItem[barrelEntity.sideUpgrades[i]];
				if (upgrade != null) {
					ItemStack droppedStack = new ItemStack(upgrade, 1, UpgradeSide.mapMeta[barrelEntity.sideUpgrades[i]]);
					this.dropStack(world, droppedStack, x, y, z);
				}
			}
			
			// These can potential affect dropped stack quantities, ensure they are removed before dropping items
			barrelEntity.setCreative(false);
			barrelEntity.setVoid(false);

			// We drop the stacks
			if (barrelEntity.getStorage().hasItem() && !barrelEntity.getLinked()) {
				barrelEntity.updateEntity();
				int ndroppedstacks = 0;
				ItemStack droppedstack = barrelEntity.getStorage().getStack();
				// TODO : is this just an amount limiter to prevent too many items spawning into the world?
				// limits max number of dropped stacks to 64, perhaps should be limited to 64 * storage upgrade count?
				while ((droppedstack != null) && (ndroppedstacks <= 64)) { //TODO: shouldn't this be the max stack size of the barrel, not 64?
					ndroppedstacks += 1;

					if (droppedstack != null)
						this.dropStack(world, droppedstack, x, y, z);

					droppedstack = barrelEntity.getStorage().getStack();
				}
			}

			try {
				BSpaceStorageHandler.instance().unregisterEnderBarrel(barrelEntity.id);
			} catch (Exception e) {
				BetterBarrels.log.info("Tried to remove the barrel from the index without a valid entity");
			}
		} else {
			BetterBarrels.log.error("TileEntity for barrel being broken at (X:" + x + ", Y:" + y + ", Z:" + z + ") is of the wrong type(" + te.getClass().getCanonicalName() + ")!");
		}

		// All finished here, let's ensure the TE is cleaned up...
		world.removeTileEntity(x, y, z);
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

		return ((TileEntityBarrel)te).getRedstonePower(side);
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

		TileEntityBarrel barrel = (TileEntityBarrel)te;

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

		IBarrelStorage store = ((TileEntityBarrel)te).getStorage();
		int currentAmount = store.getAmount();
		int maxStorable = store.getMaxStoredCount();

		if (currentAmount == 0)
			return 0;
		else if (currentAmount == maxStorable)
			return 15;
		else
			return MathHelper.floor_float(((float)currentAmount / (float)maxStorable) * 14) + 1;
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

		TileEntityBarrel barrel = (TileEntityBarrel)te;

		if (barrel.sideUpgrades[side.ordinal()] == UpgradeSide.FRONT || barrel.sideUpgrades[side.ordinal()] == UpgradeSide.STICKER) {
			return false;
		}

		return true;
	}

	@Override
	public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
		//return false;
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

		TileEntityBarrel barrel = (TileEntityBarrel)te;

		int levelStructural = barrel.coreUpgrades.levelStructural;

		boolean ghosting = barrel.getStorage().isGhosting();
		boolean linked = barrel.getLinked();
		boolean sideIsLabel = (barrel.sideUpgrades[side] == UpgradeSide.FRONT || barrel.sideUpgrades[side] == UpgradeSide.STICKER);

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

		return side == 0 ? new IconFlipped(ret, true, false): ret;
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

		if (te == null || !(te instanceof TileEntityBarrel) || !((TileEntityBarrel)te).overlaying) {
			return super.shouldSideBeRendered(world, x, y, z, side);
		}

		TileEntityBarrel barrel = (TileEntityBarrel)te;

		boolean ghosting = barrel.getStorage().isGhosting();
		boolean linked = barrel.getLinked();
		boolean sideIsLabel = (barrel.sideUpgrades[side] == UpgradeSide.FRONT || barrel.sideUpgrades[side] == UpgradeSide.STICKER);

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
