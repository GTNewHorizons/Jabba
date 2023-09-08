package mcp.mobius.betterbarrels.common.blocks;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.bspace.BSpaceStorageHandler;
import mcp.mobius.betterbarrels.common.LocalizedChat;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.items.ItemBarrelHammer.HammerMode;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import mcp.mobius.betterbarrels.network.BarrelPacketHandler;
import mcp.mobius.betterbarrels.network.Message0x04Structuralupdate;
import mcp.mobius.betterbarrels.network.Message0x05CoreUpdate;
import mcp.mobius.betterbarrels.network.Message0x06FullStorage;

public class BarrelCoreUpgrades {

    private TileEntityBarrel barrel;

    public ArrayList<UpgradeCore> upgradeList = new ArrayList<UpgradeCore>();

    public int levelStructural = 0;
    public int nStorageUpg = 0;
    public boolean hasRedstone = false;
    public boolean hasHopper = false;
    public boolean hasEnder = false;
    public boolean hasVoid = false;
    public boolean hasCreative = false;

    public BarrelCoreUpgrades(TileEntityBarrel barrel) {
        this.barrel = barrel;
    }

    /* SLOT HANDLING */

    public int getMaxUpgradeSlots() {
        return StructuralLevel.LEVELS[this.levelStructural].getMaxCoreSlots();
    }

    public int getUsedSlots() {
        int nslots = 0;
        for (UpgradeCore core : this.upgradeList) nslots += core.slotsUsed;
        return nslots;
    }

    public int getFreeSlots() {
        return getMaxUpgradeSlots() - getUsedSlots();
    }

    public boolean hasUpgrade(UpgradeCore upgrade) {
        for (UpgradeCore core : this.upgradeList) if (core == upgrade) return true;
        return false;
    }

    public boolean hasUpgradeType(UpgradeCore.Type upgradeType) {
        for (UpgradeCore core : this.upgradeList) if (core.type == upgradeType) return true;
        return false;
    }

    private int findUpgradeIndex(UpgradeCore.Type type, Boolean first, Boolean exclude) {
        if (first) {
            for (int i = 0; i < upgradeList.size(); i++) {
                if (exclude) {
                    if (this.upgradeList.get(i).type != type) return i;
                } else {
                    if (this.upgradeList.get(i).type == type) return i;
                }
            }
        } else {
            for (int i = upgradeList.size() - 1; i >= 0; i--) {
                if (exclude) {
                    if (this.upgradeList.get(i).type != type) return i;
                } else {
                    if (this.upgradeList.get(i).type == type) return i;
                }
            }
        }
        return -1;
    }

    private void createAndDropItem(Item item, int meta, EntityPlayer player) {
        ItemStack droppedStack = new ItemStack(item, 1, meta);
        Utils.dropItemInWorld(barrel, player, droppedStack, 0.02);
    }

    private void removeAndDropUpgrade(UpgradeCore upgrade, EntityPlayer player) {
        int coreIndex = this.findUpgradeIndex(upgrade.type, true, false);
        if (coreIndex >= 0) {
            this.upgradeList.remove(coreIndex);
            createAndDropItem(BetterBarrels.itemUpgradeCore, upgrade.ordinal(), player);
        }
    }

    private void removeEnder(EntityPlayer player) {
        if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_REMOVE);
            barrel.storage = new StorageLocal(nStorageUpg);
        }
        BSpaceStorageHandler.instance().unregisterEnderBarrel(barrel.id);
    }

    private void removeStorage(EntityPlayer player) {
        int indexLastUpdate = this.findUpgradeIndex(UpgradeCore.Type.STORAGE, false, false);
        if (indexLastUpdate == -1) return;

        UpgradeCore core = this.upgradeList.get(indexLastUpdate);

        if (barrel.getStorage().getItem() != null) {
            int newMaxStoredItems = (barrel.getStorage().getMaxStacks() - (64 * core.slotsUsed))
                    * barrel.getStorage().getItem().getMaxStackSize();

            if (barrel.getStorage().getAmount() > newMaxStoredItems) {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.STACK_REMOVE);
                return;
            }
        }

        this.upgradeList.remove(indexLastUpdate);
        createAndDropItem(BetterBarrels.itemUpgradeCore, core.ordinal(), player);
        for (int i = 0; i < core.slotsUsed; i++) barrel.getStorage().rmStorageUpgrade();
        this.nStorageUpg -= core.slotsUsed;
    }

    public void removeUpgrade(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        switch (HammerMode.getMode(stack)) {
            default:
            case NORMAL:
                int indexLastUpdate = findUpgradeIndex(UpgradeCore.Type.STORAGE, false, true);
                if (indexLastUpdate != -1) {

                    UpgradeCore core = this.upgradeList.get(indexLastUpdate);

                    if (core.type == UpgradeCore.Type.VOID && BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                        return;
                    }

                    this.upgradeList.remove(indexLastUpdate);
                    createAndDropItem(BetterBarrels.itemUpgradeCore, core.ordinal(), player);

                    this.hasRedstone = this.hasUpgrade(UpgradeCore.REDSTONE);
                    this.hasHopper = this.hasUpgrade(UpgradeCore.HOPPER);
                    this.hasEnder = this.hasUpgrade(UpgradeCore.ENDER);
                    barrel.setVoid(this.hasUpgrade(UpgradeCore.VOID));
                    barrel.setCreative(this.hasUpgrade(UpgradeCore.CREATIVE));

                    if (core.type == UpgradeCore.Type.ENDER) {
                        removeEnder(player);
                    }

                    if (this.hasHopper) barrel.startTicking();
                    else barrel.stopTicking();

                    barrel.removeUpgradeFacades(player);
                } else if (this.upgradeList.size() > 0) {
                    removeStorage(player);
                } else if (this.levelStructural > 0) {
                    createAndDropItem(BetterBarrels.itemUpgradeStructural, this.levelStructural - 1, player);
                    this.levelStructural -= 1;
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case REDSTONE:
                if (this.hasUpgrade(UpgradeCore.REDSTONE)) {
                    removeAndDropUpgrade(UpgradeCore.REDSTONE, player);
                    this.hasRedstone = false;
                    barrel.removeUpgradeFacades(player);
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case BSPACE:
                if (this.hasUpgrade(UpgradeCore.ENDER)) {
                    removeAndDropUpgrade(UpgradeCore.ENDER, player);
                    this.hasEnder = false;
                    removeEnder(player);
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case HOPPER:
                if (this.hasUpgrade(UpgradeCore.HOPPER)) {
                    barrel.stopTicking();
                    removeAndDropUpgrade(UpgradeCore.HOPPER, player);
                    this.hasHopper = false;
                    barrel.removeUpgradeFacades(player);
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case STORAGE:
                if (this.hasUpgradeType(UpgradeCore.Type.STORAGE)) {
                    if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                        break;
                    }
                    removeStorage(player);
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case STRUCTURAL:
                if (this.levelStructural > 0) {
                    if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                        break;
                    }

                    int newLevel = this.levelStructural - 1;
                    int newTotalSlots = 0;
                    for (int i = 0; i < newLevel; i++) newTotalSlots += MathHelper.floor_double(Math.pow(2, i));

                    if (newTotalSlots < this.getUsedSlots()) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.UPGRADE_REMOVE);
                    } else {
                        createAndDropItem(BetterBarrels.itemUpgradeStructural, this.levelStructural - 1, player);
                        this.levelStructural = newLevel;
                    }
                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case VOID:
                if (this.hasUpgrade(UpgradeCore.VOID)) {
                    if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                        break;
                    }

                    removeAndDropUpgrade(UpgradeCore.VOID, player);
                    barrel.setVoid(false);

                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
            case CREATIVE:
                if (this.hasUpgrade(UpgradeCore.CREATIVE)) {
                    if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                        break;
                    }

                    removeAndDropUpgrade(UpgradeCore.CREATIVE, player);
                    barrel.setCreative(false);

                } else {
                    BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BONK);
                }
                break;
        }
    }

    void applyUpgrade(ItemStack stack, EntityPlayer player) {
        UpgradeCore core = UpgradeCore.values()[stack.getItemDamage()];

        if (!(core.type == UpgradeCore.Type.STORAGE) && this.hasUpgrade(core)) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.COREUPGRADE_EXISTS);
            return;
        }

        if (core.slotsUsed > this.getFreeSlots()) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.UPGRADE_INSUFFICIENT, core.slotsUsed);
            return;
        }

        if (core.type == UpgradeCore.Type.STORAGE) {
            if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                return;
            }

            this.upgradeList.add(core);
            for (int i = 0; i < core.slotsUsed; i++) barrel.getStorage().addStorageUpgrade();
            this.nStorageUpg += core.slotsUsed;

            BarrelPacketHandler.INSTANCE
                    .sendToDimension(new Message0x06FullStorage(barrel), barrel.getWorldObj().provider.dimensionId);
        }

        if (core == UpgradeCore.REDSTONE) {
            this.upgradeList.add(UpgradeCore.REDSTONE);
            this.hasRedstone = true;
        }

        else if (core == UpgradeCore.HOPPER) {
            this.upgradeList.add(UpgradeCore.HOPPER);
            this.hasHopper = true;
            barrel.startTicking();
        }

        else if (core == UpgradeCore.ENDER) {
            this.upgradeList.add(UpgradeCore.ENDER);
            this.hasEnder = true;
            BSpaceStorageHandler.instance().registerEnderBarrel(barrel.id, barrel.storage);
        }

        else if (core == UpgradeCore.VOID) {
            if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                return;
            }

            this.upgradeList.add(UpgradeCore.VOID);
            barrel.setVoid(true);
        }

        else if (core == UpgradeCore.CREATIVE) {
            if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
                return;
            }

            this.upgradeList.add(UpgradeCore.CREATIVE);
            barrel.setCreative(true);
        }

        if (!player.capabilities.isCreativeMode) {
            stack.stackSize -= 1;
        }

        barrel.markDirty();
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x05CoreUpdate(barrel), barrel.getWorldObj().provider.dimensionId);
    }

    void applyStructural(ItemStack stack, EntityPlayer player) {
        if (BSpaceStorageHandler.instance().hasLinks(barrel.id)) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_PREVENT);
            return;
        }

        if (stack.getItemDamage() == this.levelStructural) {
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize -= 1;
            }
            this.levelStructural += 1;
        } else if ((player instanceof EntityPlayerMP) && (stack.getItemDamage() == (this.levelStructural - 1))) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.UPGRADE_EXISTS);
        } else if ((player instanceof EntityPlayerMP) && (stack.getItemDamage() < this.levelStructural)) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.DOWNGRADE);
        } else if ((player instanceof EntityPlayerMP) && (stack.getItemDamage() > this.levelStructural)) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.UPGRADE_REQUIRED, stack.getItemDamage());
        }

        barrel.markDirty();
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x04Structuralupdate(barrel), barrel.getWorldObj().provider.dimensionId);
    }

    /* OTHER */

    public void writeToNBT(NBTTagCompound NBTTag) {
        int[] savedUpgrades = new int[upgradeList.size()];
        Iterator<UpgradeCore> iterator = upgradeList.iterator();
        for (int i = 0; i < savedUpgrades.length; i++) {
            savedUpgrades[i] = iterator.next().ordinal();
        }

        NBTTag.setIntArray("coreUpgrades", savedUpgrades);
        NBTTag.setInteger("structural", this.levelStructural);
        NBTTag.setBoolean("redstone", this.hasRedstone);
        NBTTag.setBoolean("hopper", this.hasHopper);
        NBTTag.setBoolean("ender", this.hasEnder);
        NBTTag.setBoolean("void", this.hasVoid);
        NBTTag.setBoolean("creative", this.hasCreative);
        NBTTag.setInteger("nStorageUpg", this.nStorageUpg);
    }

    public void readFromNBT(NBTTagCompound NBTTag, int saveVersion) {
        int[] savedUpgrades = NBTTag.getIntArray("coreUpgrades");
        this.upgradeList = new ArrayList<UpgradeCore>();
        for (int i = 0; i < savedUpgrades.length; i++)
            this.upgradeList.add(UpgradeCore.values()[savedUpgrades[i] + (saveVersion == 3 ? -1 : 0)]);

        this.levelStructural = NBTTag.getInteger("structural");
        if (levelStructural >= StructuralLevel.LEVELS.length) {
            levelStructural = StructuralLevel.LEVELS.length - 1;
            BetterBarrels.log.warn(
                    "Barrel located at (X:" + barrel.xCoord
                            + " , Y:"
                            + barrel.yCoord
                            + " , Z:"
                            + barrel.zCoord
                            + ")"
                            + (barrel.getWorldObj()
                                    != null ? " located in dimension [" + barrel.getWorldObj().provider.dimensionId + "]" : " which is not located in a dimension ?!?")
                            + " is being downgraded due to its current structural level no longer existing. Reccomended to examine the barrel and fix as required.");
        }
        this.hasRedstone = NBTTag.getBoolean("redstone");
        this.hasHopper = NBTTag.getBoolean("hopper");
        this.hasEnder = NBTTag.getBoolean("ender");
        if (saveVersion < 5) {
            this.nStorageUpg = NBTTag.getByte("nStorageUpg");
        } else {
            this.nStorageUpg = NBTTag.getInteger("nStorageUpg");
        }
        barrel.setVoid(NBTTag.getBoolean("void"));
        barrel.setCreative(NBTTag.getBoolean("creative"));
    }
}
