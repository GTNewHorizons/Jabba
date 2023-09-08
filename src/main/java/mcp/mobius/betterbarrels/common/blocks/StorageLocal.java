package mcp.mobius.betterbarrels.common.blocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.blocks.logic.Coordinates;
import mcp.mobius.betterbarrels.common.blocks.logic.ItemImmut;
import mcp.mobius.betterbarrels.common.blocks.logic.OreDictPair;

public class StorageLocal implements IBarrelStorage {

    private ItemStack inputStack = null; // Slot 0
    private ItemStack prevInputStack = null; // Slot 0
    private ItemStack outputStack = null; // Slot 1
    private ItemStack prevOutputStack = null;
    private ItemStack itemTemplate = null;

    private int totalAmount = 0; // Total number of items
    private int stackAmount = 64; // Number of items in a stack

    private int basestacks = BetterBarrels.stacksSize; // Base amount of stacks in the barrel, before upgrades
    private int maxstacks = BetterBarrels.stacksSize; // Maximum amount of stacks in the barrel (post upgrade)
    private int totalCapacity = 64 * maxstacks; // Cached total maximum amount of stored item
    private int upgCapacity = 0; // Current capacity upgrade level
    private boolean keepLastItem = false; // Ghosting mod. If true, we don't reset the item type when the barrel is
                                          // empty
    private boolean deleteExcess = false; // Void mod, when true, extra added items are deleted
    private boolean alwaysProvide = false; // Creative mod, when true will always return a stack with the requested
                                           // amount

    private Set<Coordinates> linkedStorages = new HashSet<Coordinates>();

    private ItemImmut cachedBarrelOreItem = null;
    private static HashMap<OreDictPair, Boolean> oreDictCache = new HashMap<OreDictPair, Boolean>();

    public StorageLocal() {
        this.markDirty();
    }

    public StorageLocal(NBTTagCompound tag) {
        this.readTagCompound(tag);
        this.markDirty();
    }

    public StorageLocal(int nupgrades) {
        for (int i = 0; i < nupgrades; i++) {
            this.addStorageUpgrade();
        }
        this.markDirty();
    }

    private ItemStack getStackFromSlot(int slot) {
        return slot == 0 ? this.inputStack : this.outputStack;
    }

    private int getFreeSpace() {
        return this.totalCapacity - (deleteExcess ? 0 : this.totalAmount);
    }

    // IBarrelStorage Interface //
    @Override
    public boolean hasItem() {
        return this.itemTemplate != null;
    }

    @Override
    public ItemStack getItem() {
        return this.itemTemplate;
    }

    @Override
    public ItemStack getItemForRender() {
        return itemTemplate;
    }

    @Override
    public void setItem(ItemStack stack) {
        if (stack != null) {
            this.itemTemplate = stack.copy();
            this.itemTemplate.stackSize = 0;
            this.stackAmount = stack.getMaxStackSize();
            this.cachedBarrelOreItem = new ItemImmut(
                    Item.getIdFromItem(this.itemTemplate.getItem()),
                    this.itemTemplate.getItemDamage());
            this.getItemForRender();
        } else {
            this.itemTemplate = null;
            this.stackAmount = 64;
            this.cachedBarrelOreItem = null;
        }
        totalCapacity = maxstacks * stackAmount;
    }

    @Override
    public boolean sameItem(ItemStack stack) {
        if (this.itemTemplate == null) {
            if (this.keepLastItem) return false;
            return true;
        }
        if (stack == null) return false;

        if (!this.itemTemplate.isItemEqual(stack)) {
            OreDictPair orePair = new OreDictPair(
                    this.cachedBarrelOreItem,
                    new ItemImmut(Item.getIdFromItem(stack.getItem()), stack.getItemDamage()));

            if (!oreDictCache.containsKey(orePair)) {
                int[] oreIDsBarrel = OreDictionary.getOreIDs(this.itemTemplate);
                int[] oreIDsStack = OreDictionary.getOreIDs(stack);

                boolean equivalent = false;

                if (oreIDsBarrel.length > 0 && oreIDsStack.length > 0) {
                    for (int barrelOreID : oreIDsBarrel) {
                        String oreNameBarrel = OreDictionary.getOreName(barrelOreID);
                        boolean stackIsMetal = BetterBarrels.allowOreDictUnification
                                && (oreNameBarrel.startsWith("ingot") || oreNameBarrel.startsWith("ore")
                                        || oreNameBarrel.startsWith("dust")
                                        || oreNameBarrel.startsWith("nugget"));

                        if (!stackIsMetal) continue;

                        for (int stackOreID : oreIDsStack) {
                            equivalent = barrelOreID == stackOreID;
                            if (equivalent) {
                                break;
                            }
                        }
                        if (equivalent) {
                            break;
                        }
                    }
                }
                oreDictCache.put(orePair, equivalent);
                // System.out.printf("Added ore pair for %d:%d | %d:%d = %s\n", this.getItem().itemID,
                // this.getItem().getItemDamage(), stack.itemID, stack.getItemDamage(), oreDictCache.get(orePair));
            }
            return oreDictCache.get(orePair);
        }

        return ItemStack.areItemStackTagsEqual(this.itemTemplate, stack);
    }

    /* NBT MANIPULATION */
    @Override
    public NBTTagCompound writeTagCompound() {
        NBTTagCompound retTag = new NBTTagCompound();

        retTag.setInteger("amount", this.totalAmount);
        retTag.setInteger("maxstacks", this.maxstacks);
        retTag.setInteger("upgCapacity", this.upgCapacity);

        if (this.itemTemplate != null) {
            NBTTagCompound var3 = new NBTTagCompound();
            this.itemTemplate.writeToNBT(var3);
            retTag.setTag("current_item", var3);
        }
        if (this.keepLastItem) retTag.setBoolean("keepLastItem", this.keepLastItem);
        if (this.deleteExcess) retTag.setBoolean("deleteExcess", this.deleteExcess);
        if (this.alwaysProvide) retTag.setBoolean("alwaysProvide", this.alwaysProvide);
        return retTag;
    }

    @Override
    public void readTagCompound(NBTTagCompound tag) {
        this.totalAmount = tag.getInteger("amount");
        this.maxstacks = tag.getInteger("maxstacks");
        this.upgCapacity = tag.getInteger("upgCapacity");
        this.itemTemplate = tag.hasKey("current_item")
                ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("current_item"))
                : null;
        this.keepLastItem = tag.hasKey("keepLastItem") ? tag.getBoolean("keepLastItem") : false;
        this.deleteExcess = tag.hasKey("deleteExcess") ? tag.getBoolean("deleteExcess") : false;
        this.alwaysProvide = tag.hasKey("alwaysProvide") ? tag.getBoolean("alwaysProvide") : false;
        this.setItem(this.itemTemplate);

        // Sanity Check!
        if (itemTemplate != null && totalAmount < 0) {
            totalAmount = 0;
            if (!keepLastItem) keepLastItem = true;
        }

        // Fake stack setup on load
        if (!deleteExcess && (totalCapacity - totalAmount) < stackAmount) {
            inputStack = itemTemplate.copy();
            inputStack.stackSize = stackAmount - (totalCapacity - totalAmount);
            prevInputStack = inputStack.copy();
        }
    }

    /* MANUAL STACK */
    @Override
    public int addStack(ItemStack stack) {
        boolean skip = stack == null || !sameItem(stack);
        if (itemTemplate == null && keepLastItem && stack != null) skip = false;

        if (skip) return 0;

        int deposit;

        if (inputStack == null) {
            inputStack = stack;
            deposit = stack.stackSize;
        } else {
            deposit = Math.min(stack.stackSize, stackAmount - inputStack.stackSize);
            inputStack.stackSize += deposit;
        }

        markDirty();

        stack.stackSize -= deposit;

        deposit = deleteExcess ? stackAmount : deposit;

        return deposit;
    }

    @Override
    public ItemStack getStack() {
        if (this.itemTemplate != null) return this.getStack(this.stackAmount);
        else return null;
    }

    @Override
    public ItemStack getStack(int amount) {
        this.markDirty();

        ItemStack retStack = null;
        if (this.itemTemplate != null) {
            amount = Math.min(amount, this.stackAmount);
            if (!this.alwaysProvide) amount = Math.min(amount, this.totalAmount);

            retStack = this.itemTemplate.copy();
            if (!this.alwaysProvide) this.outputStack.stackSize -= amount;
            retStack.stackSize = amount;
        }

        this.markDirty();
        return retStack;
    }

    /* STATUS MANIPULATION */
    @Override
    public boolean switchGhosting() {
        this.keepLastItem = !this.keepLastItem;
        this.markDirty();
        return this.keepLastItem;
    }

    @Override
    public boolean isGhosting() {
        return this.keepLastItem;
    }

    @Override
    public void setGhosting(boolean locked) {
        keepLastItem = locked;
        if (!locked && totalAmount <= 0) setItem(null);
    }

    @Override
    public boolean isVoid() {
        return this.deleteExcess;
    }

    @Override
    public void setVoid(boolean delete) {
        this.deleteExcess = delete;
    }

    @Override
    public boolean isCreative() {
        return this.alwaysProvide;
    }

    @Override
    public void setCreative(boolean infinite) {
        this.alwaysProvide = infinite;
    }

    /* AMOUNT HANDLING */
    @Override
    public int getAmount() {
        return this.totalAmount;
    }

    @Override
    public void setAmount(int amount) {
        this.totalAmount = amount;
    }

    protected void recalcCapacities() {
        maxstacks = basestacks * (upgCapacity + 1);
        totalCapacity = maxstacks * stackAmount;
    }

    @Override
    public void setBaseStacks(int basestacks) {
        this.basestacks = basestacks;
        recalcCapacities();
    }

    @Override
    public int getMaxStacks() {
        return this.maxstacks;
    }

    @Override
    public void addStorageUpgrade() {
        this.upgCapacity += 1;
        recalcCapacities();
    }

    @Override
    public void rmStorageUpgrade() {
        this.upgCapacity -= 1;
        recalcCapacities();
    }

    // ISidedInventory Interface //
    private static final int[] accessibleSides = new int[] { 0, 1 };

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return accessibleSides;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
        if (slot == 1) return false;
        if (this.getFreeSpace() <= 0) return false;
        return this.sameItem(itemstack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        if (slot == 0) return false;
        if (!this.hasItem()) return false;
        if (itemstack == null) return true;
        return this.sameItem(itemstack); // perhaps append?: && this.totalAmount >= itemstack.stackSize
    }

    // IInventory Interface //
    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        this.markDirty();
        return this.getStackFromSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int quantity) {
        if (slot == 0) throw new RuntimeException("[JABBA] Tried to decr the stack size of the input slot");

        ItemStack stack = this.outputStack.copy();
        int stackSize = Math.min(quantity, stack.stackSize);
        stack.stackSize = stackSize;
        this.outputStack.stackSize -= stackSize;

        this.markDirty();
        return stack;
    }

    @Override
    public ItemStack decrStackSize_Hopper(int slot, int quantity) {
        if (slot == 0) throw new RuntimeException("[JABBA] Tried to decr the stack size of the input slot");

        ItemStack stack = this.outputStack.copy();
        int stackSize = Math.min(quantity, stack.stackSize);
        stack.stackSize = stackSize;
        this.outputStack.stackSize -= stackSize;

        // this.markDirty();
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return this.getStackFromSlot(slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack itemstack) {
        if (slot == 0) this.inputStack = itemstack;
        else this.outputStack = itemstack;

        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "jabba.localstorage";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        if (BetterBarrels.exposeFullStorageSize) {
            return this.totalCapacity;
        }
        return 64;
    }

    @Override
    public void markDirty() {
        // TODO : Might need to do some cleanup here

        // Handle Input
        if (inputStack != null) {
            if (itemTemplate == null) setItem(inputStack);

            if ((totalCapacity - totalAmount) > 0) {
                if (prevInputStack == null) totalAmount += inputStack.stackSize;
                else totalAmount += inputStack.stackSize - prevInputStack.stackSize;

                // Sanity Check!
                if (totalAmount > totalCapacity) totalAmount = totalCapacity;
            }
            if (deleteExcess || (totalCapacity - totalAmount) >= stackAmount) {
                // Provides logic shortcutting for when the barrel is not void and not (near)full
                inputStack = null;
                prevInputStack = null;
            } else {
                // fake stack stuff so the inventory appears full in certain mods that do not support the DSU...
                if (prevInputStack == null) inputStack = itemTemplate.copy(); // don't rely upon passed in object
                inputStack.stackSize = stackAmount - (totalCapacity - totalAmount);
                prevInputStack = inputStack.copy();
            }
        }

        // Handle changes in output
        if (!alwaysProvide && prevOutputStack != null) {
            if (outputStack != null) totalAmount -= prevOutputStack.stackSize - outputStack.stackSize;
            else totalAmount -= prevOutputStack.stackSize;

            // Sanity Check!
            if (totalAmount < 0) totalAmount = 0;
        }

        // Handle emptying of the barrel
        if (totalAmount == 0 && !keepLastItem) {
            setItem(null);
            outputStack = null;
            prevOutputStack = null;
            inputStack = null;
            prevInputStack = null;
        } else if (itemTemplate != null) {
            // Make sure an output stack exists if we are supposed to have one
            if (outputStack == null) outputStack = itemTemplate.copy();

            // Adjust output stack to the correct size
            outputStack.stackSize = alwaysProvide ? totalCapacity : totalAmount;
            if (!BetterBarrels.exposeFullStorageSize)
                outputStack.stackSize = Math.min(outputStack.stackSize, stackAmount);

            // Copy current output stack to previous to check for changes
            prevOutputStack = outputStack.copy();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return true;
    } // This is not handled here but rather in the TE itself

    @Override
    public void openInventory() {} // Unused

    @Override
    public void closeInventory() {} // Unused

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
        return this.sameItem(itemstack);
    }

    // IDeepStorageUnit Interface //
    @Override
    public ItemStack getStoredItemType() {
        if (this.itemTemplate != null) {
            ItemStack stack = this.itemTemplate.copy();
            stack.stackSize = this.alwaysProvide ? this.totalCapacity : this.totalAmount;
            return stack;
        } else {
            if (this.keepLastItem) {
                return new ItemStack(Blocks.end_portal, 0);
            } else {
                return null;
            }
        }
    }

    @Override
    public void setStoredItemCount(int amount) {
        if (amount > totalCapacity) amount = totalCapacity;
        this.totalAmount = amount;
        this.markDirty();
    }

    @Override
    public void setStoredItemType(ItemStack type, int amount) {
        this.setItem(type);
        if (amount > totalCapacity) amount = totalCapacity;
        this.totalAmount = amount;
        this.markDirty();
    }

    @Override
    public int getMaxStoredCount() {
        return this.deleteExcess ? this.totalCapacity + this.stackAmount : this.totalCapacity;
    }
}
