package mcp.mobius.betterbarrels.common.blocks;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.Optional;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.IDeepStorageUnit", modid = "MineFactoryReloaded")
public interface IBarrelStorage extends ISidedInventory, IDeepStorageUnit {

    public boolean hasItem();

    public ItemStack getItem();

    public ItemStack getItemForRender();

    public void setItem(ItemStack stack);

    public boolean sameItem(ItemStack stack);

    /* STORAGE HANDLING */
    public int getAmount();

    public void setAmount(int amount);

    public int getMaxStacks();

    public void setBaseStacks(int basestacks);

    /* NBT MANIPULATION */
    public NBTTagCompound writeTagCompound();

    public void readTagCompound(NBTTagCompound tag);

    /* MANUAL STACK */
    public int addStack(ItemStack stack);

    public ItemStack getStack();

    public ItemStack getStack(int amount);

    /* STATUS MANIPULATION */
    public boolean switchGhosting();

    public boolean isGhosting();

    public void setGhosting(boolean locked);

    public boolean isVoid();

    public void setVoid(boolean delete);

    public boolean isCreative();

    public void setCreative(boolean delete);

    // public void upgCapacity(int level);
    public void addStorageUpgrade();

    public void rmStorageUpgrade();

    public ItemStack decrStackSize_Hopper(int slot, int quantity);

    // Provide IDeepStorageUnit methods, even when the interface is stripped
    @Override
    ItemStack getStoredItemType();

    @Override
    void setStoredItemCount(int var1);

    @Override
    void setStoredItemType(ItemStack var1, int var2);

    @Override
    int getMaxStoredCount();
}
