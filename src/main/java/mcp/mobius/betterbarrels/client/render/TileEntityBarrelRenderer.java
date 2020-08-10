package mcp.mobius.betterbarrels.client.render;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.common.blocks.logic.Coordinates;
import mcp.mobius.betterbarrels.common.items.IOverlayItem;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeSide;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

public class TileEntityBarrelRenderer extends TileEntityBaseRenderer {

	public static TileEntityBarrelRenderer _instance = null;

	protected static ItemStack coreStorage  = new ItemStack(BetterBarrels.itemUpgradeCore, 0, 0);
	protected static ItemStack coreEnder    = new ItemStack(BetterBarrels.itemUpgradeCore, 0, 1);
	protected static ItemStack coreRedstone = new ItemStack(BetterBarrels.itemUpgradeCore, 0, 2);
	protected static ItemStack coreHopper   = new ItemStack(BetterBarrels.itemUpgradeCore, 0, 3);
	protected static ItemStack coreVoid     = new ItemStack(BetterBarrels.itemUpgradeCore, 0, UpgradeCore.VOID.ordinal());
	protected static ItemStack coreCreative = new ItemStack(BetterBarrels.itemUpgradeCore, 0, UpgradeCore.CREATIVE.ordinal());

	public static TileEntityBarrelRenderer instance() {
		if (_instance == null)
			_instance = new TileEntityBarrelRenderer();
		return _instance;
	}

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double xpos, double ypos, double zpos, float var8) {
		if (tileEntity instanceof TileEntityBarrel && BetterBarrels.renderStackAndText &&
				(xpos + 0.5) * (xpos + 0.5) + (ypos + 0.5) * (ypos + 0.5) + (zpos + 0.5) * (zpos + 0.5) < BetterBarrels.renderDistance) {
			this.saveBoundTexture();
			//int[][] savedGLState = modifyGLState(new int[]{ GL11.GL_BLEND }, new int[]{ GL11.GL_LIGHTING });
			// the following is how it was set, enabling lighting makes it look better with smooth lighting, mostly... bottom labels are abit dark
			// will need to actually properly calculate lighting, but for now this looks better on smooth lit barrels than the full bright it was before
			// found another bug: disabling lighting causes grass to look white, and a few other quirks, putting this on hold for the momment
			int[][] savedGLState = modifyGLState(new int[]{ GL11.GL_BLEND, GL11.GL_LIGHTING }, null);

			ForgeDirection orientation    = ((TileEntityBarrel) tileEntity).orientation;
			ForgeDirection rotation       = ((TileEntityBarrel) tileEntity).rotation;
			TileEntityBarrel barrelEntity = (TileEntityBarrel)tileEntity;
			Coordinates barrelPos         = new Coordinates(0, xpos, ypos, zpos);

			boolean isHammer = this.mc.thePlayer.getHeldItem() != null ? this.mc.thePlayer.getHeldItem().getItem() instanceof IOverlayItem ? true : false : false;
			boolean hasItem  = barrelEntity.getStorage().hasItem();

			int color = StructuralLevel.LEVELS[barrelEntity.coreUpgrades.levelStructural].clientData.getTextColor();

			for (ForgeDirection forgeSide: ForgeDirection.VALID_DIRECTIONS) {
				boolean isTopBottom = forgeSide == ForgeDirection.DOWN || forgeSide == ForgeDirection.UP;
				if (hasItem &&  this.isItemDisplaySide(barrelEntity, forgeSide)) {
					this.setLight(barrelEntity, forgeSide);

					this.renderStackOnBlock(barrelEntity.getStorage().getItemForRender(), forgeSide, isTopBottom ? rotation: orientation, barrelPos, 8.0F, 65.0F, isTopBottom ? 64.0F: 75.0F);
					String barrelString = this.getBarrelString(barrelEntity);
					this.renderTextOnBlock(barrelString, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 128.0F, 10.0F, color, TileEntityBaseRenderer.ALIGNCENTER);
				}
			}

			if (isHammer) {
				for (ForgeDirection forgeSide: ForgeDirection.VALID_DIRECTIONS) {
					boolean isTopBottom = forgeSide == ForgeDirection.DOWN || forgeSide == ForgeDirection.UP;
					this.setLight(barrelEntity, forgeSide);
					if (barrelEntity.sideUpgrades[forgeSide.ordinal()] == UpgradeSide.REDSTONE) {
						int index = barrelEntity.sideMetadata[forgeSide.ordinal()] +  2 * 16;
						this.renderIconOnBlock(index, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2F, 256.0F - 32F, 0, -0.01F);
					}
					else if (barrelEntity.sideUpgrades[forgeSide.ordinal()] == UpgradeSide.HOPPER) {
						int index = (barrelEntity.sideMetadata[forgeSide.ordinal()] & 1) +  2 * 16;
						this.renderIconOnBlock(index, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2F, 256.0F - 32F, 0, -0.01F);
					}
					else if (this.isItemDisplaySide(barrelEntity, forgeSide)) {
						int offsetY = 256 - 32;

						if (barrelEntity.coreUpgrades.levelStructural > 0) {
							this.renderIconOnBlock(StructuralLevel.LEVELS[barrelEntity.coreUpgrades.levelStructural].clientData.getIconItem(), 1, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2F, 0.0F, 0, -0.001F);
							this.renderTextOnBlock("x"+String.valueOf(barrelEntity.coreUpgrades.levelStructural), forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 37.0F, 0 + 15.0F, color, TileEntityBaseRenderer.ALIGNLEFT);
							if (barrelEntity.coreUpgrades.getFreeSlots() > 0) {
								String freeSlots = String.valueOf(barrelEntity.coreUpgrades.getFreeSlots());
								if (freeSlots.length() < 4)
									this.renderTextOnBlock(freeSlots, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 254F, 127.0F, color, TileEntityBaseRenderer.ALIGNRIGHT);
								else
									this.renderTextOnBlock(freeSlots, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 248F, 134.0F, 90F, color, TileEntityBaseRenderer.ALIGNCENTER);
							}
						}

						if (barrelEntity.coreUpgrades.nStorageUpg > 0) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreStorage, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 256.0F - 32F, 0);
							this.renderTextOnBlock(String.valueOf(barrelEntity.coreUpgrades.nStorageUpg) + "x", forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 256.0F - 32F , 15.0F, color, TileEntityBaseRenderer.ALIGNRIGHT);
						}

						if (barrelEntity.coreUpgrades.hasRedstone) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreRedstone, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 0.0F, offsetY);
							offsetY -= 35;
						}
						if (barrelEntity.coreUpgrades.hasHopper) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreHopper, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 0.0F, offsetY);
							offsetY -= 35;
						}
						if (barrelEntity.coreUpgrades.hasEnder) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreEnder, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 0.0F, offsetY);
							offsetY -= 35;
						}
						if (barrelEntity.coreUpgrades.hasVoid) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreVoid, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 0.0F, offsetY);
							offsetY -= 35;
						}
						if (barrelEntity.coreUpgrades.hasCreative) {
							this.renderStackOnBlock(TileEntityBarrelRenderer.coreCreative, forgeSide, isTopBottom ? rotation: orientation, barrelPos, 2.0F, 0.0F, offsetY);
							offsetY -= 35;
						}
					}
				}
			}

			this.restoreGlState(savedGLState);
			this.loadBoundTexture();
		}
	}

	protected String getBarrelString(TileEntityBarrel barrel) {
		String outstring = null;
		if (!barrel.getStorage().hasItem()) return "";

		int maxstacksize = barrel.getStorage().getItem().getMaxStackSize();
		//int amount  = Math.min(barrel.storage.getAmount(), (int)Math.pow(2, barrel.upgradeCapacity) * barrel.storage.getBaseStacks() * barrel.storage.getItem().stackSize);
		int amount = barrel.getStorage().getAmount();

		if (barrel.coreUpgrades.hasCreative) {
			outstring = "-";
		} else if (maxstacksize != 1){
			int nstacks = amount/maxstacksize;
			int remains = amount%maxstacksize;

			if ((nstacks > 0) && (remains > 0)) {
				outstring = String.format("%s*%s + %s", nstacks, maxstacksize, remains);
			} else if ((nstacks == 0) && (remains > 0)) {
				outstring = String.format("%s", remains);
			} else if ((nstacks > 0) && (remains == 0)) {
				outstring = String.format("%s*%s", nstacks, maxstacksize);
			} else if (amount == 0) {
				outstring = "0";
			}
		}
		else if (maxstacksize == 1) {
			outstring = String.format("%s", amount);
		} else {
			outstring = "";
		}

		//if (amount < barrel.storage.getAmount())
		//	outstring += " [...]";

		return outstring;
	}

	protected boolean isItemDisplaySide(TileEntityBarrel barrel, ForgeDirection forgeSide) {
		if (barrel.sideUpgrades[forgeSide.ordinal()] == UpgradeSide.NONE)    return false;
		if (barrel.sideUpgrades[forgeSide.ordinal()] == UpgradeSide.FRONT)   return true;
		if (barrel.sideUpgrades[forgeSide.ordinal()] == UpgradeSide.STICKER) return true;
		return false;
	}
}
