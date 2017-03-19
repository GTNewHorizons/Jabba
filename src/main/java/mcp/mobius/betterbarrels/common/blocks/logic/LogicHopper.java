package mcp.mobius.betterbarrels.common.blocks.logic;

import mcp.mobius.betterbarrels.common.blocks.IBarrelStorage;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeSide;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public enum LogicHopper {
	INSTANCE;

	private boolean isStorage(TileEntity inventory) {
		if (inventory instanceof IDeepStorageUnit) {
			return true;
		}
		if (inventory instanceof IInventory) {
			return true;
		}
		return false;
	}

	public boolean run(TileEntityBarrel barrel) {
		boolean transaction = false;
		IBarrelStorage store = barrel.getStorage();

		for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			if (barrel.sideUpgrades[side.ordinal()] == UpgradeSide.HOPPER) {
				//shortcut out if no work
				if ((barrel.sideMetadata[side.ordinal()] & 1) == UpgradeSide.RS_FULL && (!store.hasItem() || store.getAmount() <= 0)) {
					continue;
				}
				if ((barrel.sideMetadata[side.ordinal()] & 1) == UpgradeSide.RS_EMPT && (store.getAmount() >= store.getMaxStoredCount())) {
					continue;
				}

				TileEntity targetEntity = barrel.getWorldObj().getTileEntity(barrel.xCoord + side.offsetX, barrel.yCoord + side.offsetY, barrel.zCoord + side.offsetZ);
				if (isStorage(targetEntity)) {
					if ((barrel.sideMetadata[side.ordinal()] & 1) == UpgradeSide.RS_FULL) {
						// Output mode
						
						int free;
						if ((free = this.freeSpaceForStack(targetEntity, side.getOpposite(), store, barrel.sideMetadata[side.ordinal()] >> 1)) > 0 &&
								this.pushItemToInventory(targetEntity, side.getOpposite(), store, Math.min(barrel.sideMetadata[side.ordinal()] >> 1, free))) {
							store.markDirty();
							transaction = true;
							targetEntity.markDirty();
						}
					} else {
						// Input mode
						ItemStack pulledStack = pullMatchingItemFromInventory(targetEntity, side.getOpposite(), store, barrel.sideMetadata[side.ordinal()] >> 1);
						if (pulledStack != null) {
							if (store.hasItem()) {
								store.setStoredItemCount(store.getAmount() + pulledStack.stackSize);
							} else {
								store.setStoredItemType(pulledStack, pulledStack.stackSize);
							}
							transaction = true;
							targetEntity.markDirty();
						}
					}
				}
			}
		}
		return transaction;
	}

	private int freeSpaceForStack(TileEntity target, ForgeDirection side, IBarrelStorage barrel, int maxSeeking) {
		ItemStack stack = barrel.getStackInSlot(1);
		int ret = 0;
		if (target instanceof IDeepStorageUnit) {
			IDeepStorageUnit dsu = (IDeepStorageUnit)target;
			ItemStack is = dsu.getStoredItemType();
			if (is == null) {
				ret = dsu.getMaxStoredCount();
			} else if (barrel.sameItem(is)) {
				ret = dsu.getMaxStoredCount() - is.stackSize;
			}
		} else if (target instanceof ISidedInventory) {
			ISidedInventory sinv = (ISidedInventory)target;
			int[] islots = sinv.getAccessibleSlotsFromSide(side.ordinal());

			for (int index : islots) {
				if (!sinv.canInsertItem(index, stack, side.ordinal())) {
					continue;
				}
				ItemStack is = sinv.getStackInSlot(index);
				if (is == null) {
					ret += sinv.getInventoryStackLimit();
				} else {
					ret += is.getMaxStackSize() - is.stackSize;
				}
				if (ret >= maxSeeking) {
					break;
				}
			}
		} else if (target instanceof IInventory) {
			IInventory inv = (IInventory)target;
			for (int index = 0; index < inv.getSizeInventory(); index++) {
				if (!inv.isItemValidForSlot(index, stack)) {
					continue;
				}
				ItemStack is = inv.getStackInSlot(index);
				if (is == null) {
					ret += inv.getInventoryStackLimit();
				} else {
					ret += is.getMaxStackSize() - is.stackSize;
				}
				if (ret >= maxSeeking) {
					break;
				}
			}
		}
		return ret;
	}

	private boolean pushItemToInventory(TileEntity target, ForgeDirection side, IBarrelStorage barrel, int maxTransfer) {
		ItemStack stack = barrel.getStackInSlot(1);
		int transferAmount = Math.min(maxTransfer, stack.stackSize);

		if (target instanceof IDeepStorageUnit) {
			IDeepStorageUnit dsu = (IDeepStorageUnit)target;
			ItemStack is = dsu.getStoredItemType();

			if (is == null) {
				is = stack.copy();
				dsu.setStoredItemType(is, transferAmount);
				stack.stackSize -= transferAmount;
				return true;
			} else if (barrel.sameItem(is)) {
				dsu.setStoredItemCount(is.stackSize + transferAmount);
				stack.stackSize -= transferAmount;
				return true;
			}
		} else if (target instanceof ISidedInventory  && side.ordinal() > -1) {
			ISidedInventory sinv = (ISidedInventory)target;
			int[] islots = sinv.getAccessibleSlotsFromSide(side.ordinal());
			int maxInventoryStackLimit = sinv.getInventoryStackLimit();
			int transferred = 0;
			for (int slot : islots) {
				if (transferred == transferAmount) {
					break;
				}
				if (!sinv.canInsertItem(slot, stack, side.ordinal())) {
					continue;
				}
				ItemStack targetStack = sinv.getStackInSlot(slot);

				int slotTransferAmount = Math.min((transferAmount - transferred), maxInventoryStackLimit);

				if (targetStack == null) {
					targetStack = stack.copy();
					targetStack.stackSize = slotTransferAmount;
					sinv.setInventorySlotContents(slot, targetStack);
					stack.stackSize -= slotTransferAmount;
					transferred += slotTransferAmount;
				} else if (barrel.sameItem(targetStack)) {
					slotTransferAmount = Math.min(slotTransferAmount, (targetStack.getMaxStackSize() - targetStack.stackSize));
					if (slotTransferAmount > 0) {
						targetStack.stackSize += slotTransferAmount;
						stack.stackSize -= slotTransferAmount;
						transferred += slotTransferAmount;
					}
				}
			}
			if (transferred > 0) {
				return true;
			}
		} else if (target instanceof IInventory) {
			IInventory inv = (IInventory)target;
			int nslots = inv.getSizeInventory();
			int maxInventoryStackLimit = inv.getInventoryStackLimit();
			int transferred = 0;
			for (int slot = 0; slot < nslots; slot++) {
				if (transferred == transferAmount) {
					break;
				}
				if (!inv.isItemValidForSlot(slot, stack)) {
					continue;
				}
				ItemStack targetStack = inv.getStackInSlot(slot);

				int slotTransferAmount = Math.min((transferAmount - transferred), maxInventoryStackLimit);

				if (targetStack == null) {
					targetStack = stack.copy();
					targetStack.stackSize = slotTransferAmount;
					inv.setInventorySlotContents(slot, targetStack);
					stack.stackSize -= slotTransferAmount;
					transferred += slotTransferAmount;
				} else if (barrel.sameItem(targetStack)) {
					slotTransferAmount = Math.min(slotTransferAmount, (targetStack.getMaxStackSize() - targetStack.stackSize));
					if (slotTransferAmount > 0) {
						targetStack.stackSize += slotTransferAmount;
						stack.stackSize -= slotTransferAmount;
						transferred += slotTransferAmount;
					}
				}
			}
			if (transferred > 0) {
				return true;
			}
		}

		return false;
	}

	private ItemStack pullMatchingItemFromInventory(TileEntity source, ForgeDirection side, IBarrelStorage barrel, int maxTransfer) {
		if (source instanceof IDeepStorageUnit) {
			IDeepStorageUnit dsu = (IDeepStorageUnit)source;
			ItemStack stack = dsu.getStoredItemType();
			if (stack != null && barrel.sameItem(stack) && stack.stackSize > 0) {
				stack = stack.copy();
				int transferAmount = Math.min(maxTransfer, stack.stackSize);
				dsu.setStoredItemCount(stack.stackSize - transferAmount);
				stack.stackSize = transferAmount;
				return stack;
			}
		} else if (source instanceof ISidedInventory  && side.ordinal() > -1) {
			ISidedInventory sinv = (ISidedInventory)source;
			int[] islots = sinv.getAccessibleSlotsFromSide(side.ordinal());
			ItemStack testStack = barrel.hasItem() ? barrel.getItem().copy() : null;
			if (testStack != null) {
				testStack.stackSize = maxTransfer;
			}
			for (int slot : islots) {
				ItemStack stack = sinv.getStackInSlot(slot);
				if ((testStack != null || stack != null) && !sinv.canExtractItem(slot, (testStack != null ? testStack : stack), side.ordinal())) {
					continue;
				}
				if (stack != null && barrel.sameItem(stack) && stack.stackSize > 0) {
					return sinv.decrStackSize(slot, Math.min(maxTransfer, stack.stackSize));
				}
			}
		} else if (source instanceof IInventory) {
			IInventory inv = (IInventory)source;
			int nslots = inv.getSizeInventory();
			for (int slot = 0; slot < nslots; slot++) {
				ItemStack stack = inv.getStackInSlot(slot);
				if (stack != null && barrel.sameItem(stack) && stack.stackSize > 0) {
					return inv.decrStackSize(slot, Math.min(maxTransfer, stack.stackSize));
				}
			}
		}

		return null;
	}
}
