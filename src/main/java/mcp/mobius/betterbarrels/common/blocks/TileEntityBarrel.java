package mcp.mobius.betterbarrels.common.blocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.capability.item.ItemIO;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;
import com.gtnewhorizon.gtnhlib.item.AbstractInventoryIterator;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.ItemStackPredicate;
import com.gtnewhorizon.gtnhlib.item.ItemTransfer;
import com.gtnewhorizon.gtnhlib.item.SimpleItemIO;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.ServerTickHandler;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.bspace.BSpaceStorageHandler;
import mcp.mobius.betterbarrels.common.LocalizedChat;
import mcp.mobius.betterbarrels.common.items.ItemBarrelHammer;
import mcp.mobius.betterbarrels.common.items.ItemTuningFork;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeSide;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeStructural;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeSide;
import mcp.mobius.betterbarrels.network.BarrelPacketHandler;
import mcp.mobius.betterbarrels.network.Message0x00FulleTileEntityNBT;
import mcp.mobius.betterbarrels.network.Message0x01ContentUpdate;
import mcp.mobius.betterbarrels.network.Message0x02GhostUpdate;
import mcp.mobius.betterbarrels.network.Message0x03SideupgradeUpdate;
import mcp.mobius.betterbarrels.network.Message0x04Structuralupdate;
import mcp.mobius.betterbarrels.network.Message0x05CoreUpdate;
import mcp.mobius.betterbarrels.network.Message0x06FullStorage;
import mcp.mobius.betterbarrels.network.Message0x08LinkUpdate;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.IDeepStorageUnit", modid = "MineFactoryReloaded")
public class TileEntityBarrel extends TileEntity implements ISidedInventory, IDeepStorageUnit, CapabilityProvider {

    private static int version = 5;

    private long clickTime = -20; // Click timer for double click handling

    IBarrelStorage storage = new StorageLocal();
    public ForgeDirection orientation = ForgeDirection.UNKNOWN;
    public ForgeDirection rotation = ForgeDirection.UNKNOWN;
    public int[] sideUpgrades = { UpgradeSide.NONE, UpgradeSide.NONE, UpgradeSide.NONE, UpgradeSide.NONE,
            UpgradeSide.NONE, UpgradeSide.NONE };
    public int[] sideMetadata = { 0, 0, 0, 0, 0, 0 };
    public boolean isTicking = false;
    public boolean isLinked = false;
    public byte nTicks = 0;
    public int id = -1;
    public long timeSinceLastUpd = System.currentTimeMillis();
    public boolean overlaying = false;

    private Message0x01ContentUpdate lastContentMessage;
    private Message0x02GhostUpdate lastGhostMessage;

    public BarrelCoreUpgrades coreUpgrades;

    public TileEntityBarrel() {
        coreUpgrades = new BarrelCoreUpgrades(this);
    }

    public void setLinked(boolean linked) {
        this.isLinked = linked;
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x08LinkUpdate(this), this.worldObj.provider.dimensionId);
    }

    public boolean getLinked() {
        return this.isLinked;
    }

    public IBarrelStorage getStorage() {
        IBarrelStorage ret;
        // If I'm enderish, I should request the storage from the Manager. Otherwise, do the usual stuff
        if (this.coreUpgrades.hasEnder && !this.worldObj.isRemote) {
            ret = BSpaceStorageHandler.instance().getStorage(this.id);
        } else {
            ret = this.storage;
        }

        if (ret == null) {
            BetterBarrels.log.error(
                    String.format(
                            "This is the most unusual case. Storage appears to be null for [%d %d %d %d] with id [%d]",
                            this.worldObj.provider.dimensionId,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            this.id));

            if (this.storage == null) {
                this.storage = new StorageLocal();
                BetterBarrels.log.error("Local storage was null. Created a new one.");
            }

            if (this.coreUpgrades.hasEnder && !this.worldObj.isRemote) {
                this.id = BSpaceStorageHandler.instance().getNextBarrelID();

                BetterBarrels.log.error(
                        String.format(
                                "Barrel is BSpaced. Generating new ID for it and registering the storage with the main handler."));

                BSpaceStorageHandler.instance().registerEnderBarrel(this.id, this.storage);
            }

            if (this.coreUpgrades.hasEnder && !this.worldObj.isRemote) {
                ret = BSpaceStorageHandler.instance().getStorage(this.id);
            } else {
                ret = this.storage;
            }

        }

        if (ret == null) {
            // BetterBarrels.log.severe("Barrel at X: " + this.xCoord + " Y: " + this.yCoord + " Z: " + this.zCoord + "
            // has no storage." + (this.coreUpgrades.hasEnder ? " It thinks it is a BSpace barrel with ID: " + this.id:
            // ""));
            throw new RuntimeException(
                    String.format(
                            "Attempts to salvage [%d %d %d %d] with id [%d] have failed ! Please contact your closest modder to bitch at him.",
                            this.worldObj.provider.dimensionId,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            this.id));
        }

        return ret;
    }

    public void setStorage(IBarrelStorage storage) {
        this.storage = storage;
    }

    public void setVoid(boolean delete) {
        this.coreUpgrades.hasVoid = delete;
        this.storage.setVoid(delete);
    }

    public void setCreative(boolean infinite) {
        this.coreUpgrades.hasCreative = infinite;
        this.storage.setCreative(infinite);
    }

    /* UPDATE HANDLING */
    @Override
    public boolean canUpdate() {
        if (this.worldObj != null && this.worldObj.isRemote) return false;
        else return this.isTicking;
    }

    @Override
    public void updateEntity() {
        if (this.worldObj.isRemote) return;

        if (++this.nTicks % 8 == 0) {
            IBarrelStorage storage = this.getStorage();

            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (this.sideUpgrades[side.ordinal()] == UpgradeSide.HOPPER) {
                    boolean push = (this.sideMetadata[side.ordinal()] & 1) == UpgradeSide.HOPPER_PUSH;

                    // shortcut out if no work
                    if (push) {
                        if (!storage.hasItem() || storage.getAmount() <= 0) continue;
                    } else {
                        if (storage.getAmount() >= storage.getMaxStoredCount()) continue;
                    }

                    TileEntity adjacent = this.getWorldObj().getTileEntity(
                            this.xCoord + side.offsetX,
                            this.yCoord + side.offsetY,
                            this.zCoord + side.offsetZ);

                    transfer(this, side, adjacent, push, storage.getItem());
                }
            }

            this.nTicks = 0;
        }
    }

    private void transfer(TileEntity barrel, ForgeDirection side, TileEntity adj, boolean push, ItemStack filter) {
        ItemTransfer transfer = new ItemTransfer();

        if (push) {
            transfer.push(barrel, side, adj);
        } else {
            transfer.pull(barrel, side, adj);
        }

        transfer.setMaxTotalTransferred(16);
        transfer.setStacksToTransfer(16);
        transfer.setMaxItemsPerTransfer(16);

        if (filter != null) {
            transfer.setFilter(ItemStackPredicate.matches(filter));
        }

        transfer.transfer();
    }

    void startTicking() {
        this.isTicking = true;
        if (!this.worldObj.loadedTileEntityList.contains(this)) this.worldObj.addTileEntity(this);
    }

    void stopTicking() {
        this.isTicking = false;
        if (this.worldObj.loadedTileEntityList.contains(this)) this.worldObj.loadedTileEntityList.remove(this);
    }

    /* REDSTONE HANDLING */
    static final int[] sideSwitch = { 1, 0, 3, 2, 5, 4 };

    public int getRedstonePower(int side) {
        if (!this.coreUpgrades.hasRedstone) return 0;

        side = sideSwitch[side];

        IBarrelStorage store = this.getStorage();
        int currentAmount = store.getAmount();
        int maxStorable = store.getMaxStoredCount();

        if (this.coreUpgrades.hasVoid && store.hasItem()) maxStorable -= store.getItem().getMaxStackSize();

        if (this.sideUpgrades[side] == UpgradeSide.REDSTONE && this.sideMetadata[side] == UpgradeSide.RS_FULL
                && currentAmount == maxStorable)
            return 15;
        else if (this.sideUpgrades[side] == UpgradeSide.REDSTONE && this.sideMetadata[side] == UpgradeSide.RS_EMPT
                && currentAmount == 0)
            return 15;
        else if (this.sideUpgrades[side] == UpgradeSide.REDSTONE && this.sideMetadata[side] == UpgradeSide.RS_PROP)
            if (currentAmount == 0) return 0;
            else if (currentAmount == maxStorable) return 15;
            else return MathHelper.floor_float(((float) currentAmount / (float) maxStorable) * 14) + 1;
        else return 0;
    }

    /* PLAYER INTERACTIONS */

    public void leftClick(EntityPlayer player) {
        if (this.worldObj.isRemote) return;

        ItemStack extractedStack = null;
        if ((BetterBarrels.reverseBehaviourClickLeft && !player.isSneaking())
                || (!BetterBarrels.reverseBehaviourClickLeft && player.isSneaking()))
            extractedStack = this.getStorage().getStack(1);
        else extractedStack = this.getStorage().getStack();

        if ((extractedStack != null) && (extractedStack.stackSize > 0)) {
            // try to add to player inventory first, otherwise drop
            if (!player.inventory.addItemStackToInventory(extractedStack)) {
                Utils.dropItemInWorld(this, player, extractedStack, 0.02);
            }
        }

        this.markDirty();
    }

    public void rightClick(EntityPlayer player, int side) {
        if (this.worldObj.isRemote) return;

        ItemStack stack = player.getHeldItem();

        if (!player.isSneaking()) {
            if (stack != null && (stack.getItem() instanceof ItemBarrelHammer))
                this.configSide(stack, player, ForgeDirection.getOrientation(side));
            else this.manualStackAdd(player);
        } else {
            if (stack == null) this.switchLocked();
            else if (stack.getItem() instanceof ItemUpgradeSide)
                this.applySideUpgrade(stack, player, ForgeDirection.getOrientation(side));
            else if (stack.getItem() instanceof ItemUpgradeCore) coreUpgrades.applyUpgrade(stack, player);
            else if (stack.getItem() instanceof ItemUpgradeStructural) coreUpgrades.applyStructural(stack, player);
            else if (stack.getItem() instanceof ItemBarrelHammer)
                this.removeUpgrade(stack, player, ForgeDirection.getOrientation(side));
            else if (stack.getItem() instanceof ItemTuningFork) {
                if (stack.getItemDamage() == 0) this.tuneFork(stack, player, ForgeDirection.getOrientation(side));
                else this.tuneBarrel(stack, player, ForgeDirection.getOrientation(side));
            } else this.manualStackAdd(player);
        }
    }

    /* THE TUNING FORK */
    private void tuneFork(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        if (!this.coreUpgrades.hasEnder) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_NOREACT);
            return;
        }

        // if (this.getStorage().hasItem()){
        // BarrelPacketHandler.sendChat(player, "Barrel content is preventing it from resonating.");
        // return;
        // }

        // Here we sync the fork to the original barrel frequency if the fork is not already tuned.
        // stack.setItemDamage(stack.getMaxDamage());
        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_FORK_RESONATING);
        stack.setItemDamage(1);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("tuneID", this.id);
        stack.getTagCompound().setInteger("structural", coreUpgrades.levelStructural);
        stack.getTagCompound().setInteger("storage", coreUpgrades.nStorageUpg);
        stack.getTagCompound().setBoolean("void", coreUpgrades.hasVoid);
        stack.getTagCompound().setBoolean("creative", coreUpgrades.hasCreative);
    }

    private void tuneBarrel(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        if (!this.coreUpgrades.hasEnder) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_NOREACT);
            return;
        }

        if (this.getStorage().hasItem()) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_CONTENT);
            return;
        }

        int structural = stack.getTagCompound().getInteger("structural");
        int storage = stack.getTagCompound().getInteger("storage");
        int barrelID = stack.getTagCompound().getInteger("tuneID");
        boolean hasVoid = stack.getTagCompound().getBoolean("void");
        boolean hasCreative = stack.getTagCompound().getBoolean("creative");

        if (coreUpgrades.levelStructural != structural || coreUpgrades.nStorageUpg != storage
                || coreUpgrades.hasVoid != hasVoid
                || coreUpgrades.hasCreative != hasCreative) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSAPCE_STRUCTURE);
            return;
        }

        if (this.id == barrelID) {
            stack.setItemDamage(1);
            return;
        }

        if (BSpaceStorageHandler.instance().getBarrel(barrelID) == null
                || !BSpaceStorageHandler.instance().getBarrel(barrelID).coreUpgrades.hasEnder) {
            BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_FORK_LOST);
            stack.setItemDamage(0);
            stack.setTagCompound(new NBTTagCompound());
            return;
        }

        BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.BSPACE_RESONATING);
        stack.setItemDamage(0);
        stack.setTagCompound(new NBTTagCompound());

        BSpaceStorageHandler.instance().linkStorages(barrelID, this.id);
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x02GhostUpdate(this), this.worldObj.provider.dimensionId);
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x06FullStorage(this), this.worldObj.provider.dimensionId);
    }

    /* UPGRADE ACTIONS */

    private void configSide(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        int type = this.sideUpgrades[side.ordinal()];

        boolean sendChange = false;

        if (type == UpgradeSide.REDSTONE) {
            this.sideMetadata[side.ordinal()] = this.sideMetadata[side.ordinal()] + 1;
            if (this.sideMetadata[side.ordinal()] > UpgradeSide.RS_PROP)
                this.sideMetadata[side.ordinal()] = UpgradeSide.RS_FULL;
            sendChange = true;
        }

        if (type == UpgradeSide.HOPPER) {
            // hack: using rs property values until side upgrade overhaul occurs
            if ((this.sideMetadata[side.ordinal()] & 1) == UpgradeSide.RS_FULL)
                this.sideMetadata[side.ordinal()] = (sideMetadata[side.ordinal()] & 62) | UpgradeSide.RS_EMPT;
            else this.sideMetadata[side.ordinal()] = (sideMetadata[side.ordinal()] & 62) | UpgradeSide.RS_FULL;
            sendChange = true;
        }

        if (sendChange) {
            this.markDirty();
            BarrelPacketHandler.INSTANCE
                    .sendToDimension(new Message0x03SideupgradeUpdate(this), this.worldObj.provider.dimensionId);
        }
    }

    void removeUpgradeFacades(EntityPlayer player) {
        for (ForgeDirection s : ForgeDirection.VALID_DIRECTIONS) {
            int sideType = this.sideUpgrades[s.ordinal()];
            if ((UpgradeSide.mapReq[sideType] != -1)
                    && (!coreUpgrades.hasUpgrade(UpgradeCore.values()[UpgradeSide.mapReq[sideType]])))
                this.dropSideUpgrade(player, s);
        }
    }

    private void removeUpgrade(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        int type = this.sideUpgrades[side.ordinal()];

        if (type != UpgradeSide.NONE && type != UpgradeSide.FRONT) {
            this.dropSideUpgrade(player, side);
        } else {
            coreUpgrades.removeUpgrade(stack, player, side);
        }

        this.worldObj.notifyBlockChange(
                this.xCoord,
                this.yCoord,
                this.zCoord,
                this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x03SideupgradeUpdate(this), this.worldObj.provider.dimensionId);
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x04Structuralupdate(this), this.worldObj.provider.dimensionId);
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x05CoreUpdate(this), this.worldObj.provider.dimensionId);
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x06FullStorage(this), this.worldObj.provider.dimensionId);
    }

    private void dropSideUpgrade(EntityPlayer player, ForgeDirection side) {
        int type = this.sideUpgrades[side.ordinal()];
        ItemStack droppedStack = new ItemStack(UpgradeSide.mapItem[type], 1, UpgradeSide.mapMeta[type]);
        Utils.dropItemInWorld(this, player, droppedStack, 0.02);
        this.sideUpgrades[side.ordinal()] = UpgradeSide.NONE;
        this.sideMetadata[side.ordinal()] = UpgradeSide.NONE;
    }

    private void applySideUpgrade(ItemStack stack, EntityPlayer player, ForgeDirection side) {
        int type = UpgradeSide.mapRevMeta[stack.getItemDamage()];
        if (this.sideUpgrades[side.ordinal()] != UpgradeSide.NONE) {
            return;
        }

        if (type == UpgradeSide.STICKER) {
            this.sideUpgrades[side.ordinal()] = UpgradeSide.STICKER;
            this.sideMetadata[side.ordinal()] = UpgradeSide.NONE;
        }

        else if (type == UpgradeSide.REDSTONE) {
            if (coreUpgrades.hasRedstone) {
                this.sideUpgrades[side.ordinal()] = UpgradeSide.REDSTONE;
                this.sideMetadata[side.ordinal()] = UpgradeSide.RS_FULL;
            } else {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.FACADE_REDSTONE);
                return;
            }
        }

        else if (type == UpgradeSide.HOPPER) {
            if (coreUpgrades.hasHopper) {
                this.sideUpgrades[side.ordinal()] = UpgradeSide.HOPPER;
                this.sideMetadata[side.ordinal()] = 2;
            } else {
                BarrelPacketHandler.sendLocalizedChat(player, LocalizedChat.FACADE_HOPPER);
                return;
            }
        }

        if (!player.capabilities.isCreativeMode) {
            stack.stackSize -= 1;
        }

        this.markDirty();
        BarrelPacketHandler.INSTANCE
                .sendToDimension(new Message0x03SideupgradeUpdate(this), this.worldObj.provider.dimensionId);
    }

    /*
     * private void unlinkBarrel(EntityPlayer player){ if (BSpaceStorageHandler.instance().hasLinks(this.id)){
     * BarrelPacketHandler.sendChat(player, "The resonance vanishes..."); this.storage =
     * BSpaceStorageHandler.instance().unlinkStorage(this.id);
     * PacketDispatcher.sendPacketToAllInDimension(Packet0x06FullStorage.create(this),
     * this.worldObj.provider.dimensionId); } }
     */

    /* OTHER ACTIONS */

    private void switchLocked() {
        this.getStorage().switchGhosting();
        this.markDirty();
    }

    public void setLocked(boolean locked) {
        this.getStorage().setGhosting(locked);
        this.markDirty();
    }

    private void manualStackAdd(EntityPlayer player) {
        ItemStack heldStack = player.inventory.getCurrentItem();
        this.getStorage().addStack(heldStack);

        if (this.worldObj.getTotalWorldTime() - this.clickTime < 10L) {
            InventoryPlayer playerInv = player.inventory;
            for (int invSlot = 0; invSlot < playerInv.getSizeInventory(); ++invSlot) {
                ItemStack slotStack = playerInv.getStackInSlot(invSlot);

                // We add the items to the barrel and update player inventory
                if (this.getStorage().addStack(slotStack) > 0) {
                    if (slotStack.stackSize == 0) playerInv.setInventorySlotContents(invSlot, (ItemStack) null);
                }
            }
        }

        BetterBarrels.proxy.updatePlayerInventory(player);
        this.clickTime = this.worldObj.getTotalWorldTime();

        this.markDirty();
    }

    /* SAVING AND LOADING OF DATA */

    @Override
    public void writeToNBT(NBTTagCompound NBTTag) {
        if (this.id == -1) this.id = BSpaceStorageHandler.instance().getNextBarrelID();

        BSpaceStorageHandler.instance()
                .updateBarrel(this.id, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);

        super.writeToNBT(NBTTag);
        NBTTag.setInteger("version", TileEntityBarrel.version);
        NBTTag.setInteger("orientation", this.orientation.ordinal());
        NBTTag.setInteger("rotation", this.rotation.ordinal());
        NBTTag.setIntArray("sideUpgrades", this.sideUpgrades);
        this.coreUpgrades.writeToNBT(NBTTag);
        NBTTag.setIntArray("sideMeta", this.sideMetadata);
        NBTTag.setBoolean("ticking", this.isTicking);
        NBTTag.setBoolean("linked", this.isLinked);
        NBTTag.setByte("nticks", this.nTicks);
        NBTTag.setTag("storage", this.getStorage().writeTagCompound());
        NBTTag.setInteger("bspaceid", this.id);

    }

    @Override
    public void readFromNBT(NBTTagCompound NBTTag) {
        super.readFromNBT(NBTTag);

        // Handling of backward compatibility
        int saveVersion = NBTTag.getInteger("version");
        if (saveVersion == 2) {
            this.readFromNBT_v2(NBTTag);
            return;
        }

        this.orientation = ForgeDirection.getOrientation(NBTTag.getInteger("orientation"));
        this.rotation = NBTTag.hasKey("rotation") ? ForgeDirection.getOrientation(NBTTag.getInteger("rotation"))
                : this.orientation;
        this.sideUpgrades = NBTTag.getIntArray("sideUpgrades");
        this.sideMetadata = NBTTag.getIntArray("sideMeta");
        this.coreUpgrades = new BarrelCoreUpgrades(this);
        this.coreUpgrades.readFromNBT(NBTTag, saveVersion);
        this.isTicking = NBTTag.getBoolean("ticking");
        this.isLinked = NBTTag.hasKey("linked") ? NBTTag.getBoolean("linked") : false;
        this.nTicks = NBTTag.getByte("nticks");
        this.id = NBTTag.getInteger("bspaceid");

        if (this.coreUpgrades.hasEnder && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
            this.storage = BSpaceStorageHandler.instance().getStorage(this.id);
        else this.getStorage().readTagCompound(NBTTag.getCompoundTag("storage"));

        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            if (this.isTicking) this.startTicking();
        }

        // make sure any side upgrades are configured in a sane way
        for (int side = 0; side < sideUpgrades.length; side++) {
            switch (sideUpgrades[side]) {
                case UpgradeSide.HOPPER:
                    int amount = sideMetadata[side] >> 1;
                    if (!(amount == 1 || amount == 2 || amount == 4 || amount == 8 || amount == 16)) {
                        sideMetadata[side] = (1 << 1) | (sideMetadata[side] & 1);
                    }
                    break;
            }
        }
    }

    /* V2 COMPATIBILITY METHODS */

    private void readFromNBT_v2(NBTTagCompound NBTTag) {
        int blockOrientation = NBTTag.getInteger("barrelOrient");
        int upgradeCapacity = NBTTag.getInteger("upgradeCapacity");
        int blockOriginalOrient = NBTTag.hasKey("barrelOrigOrient") ? NBTTag.getInteger("barrelOrigOrient")
                : blockOrientation;
        StorageLocal storage = new StorageLocal();
        storage.readTagCompound(NBTTag.getCompoundTag("storage"));

        // We fix the labels and orientation
        this.orientation = this.convertOrientationFlagToForge(blockOriginalOrient).get(0);
        this.rotation = this.orientation;

        ArrayList<ForgeDirection> stickers = this.convertOrientationFlagToForge(blockOrientation);
        for (ForgeDirection s : stickers) this.sideUpgrades[s.ordinal()] = UpgradeSide.STICKER;
        this.sideUpgrades[this.orientation.ordinal()] = UpgradeSide.FRONT;

        // We fix the structural and core upgrades
        coreUpgrades.levelStructural = upgradeCapacity;
        int freeSlots = coreUpgrades.getFreeSlots();
        for (int i = 0; i < freeSlots; i++) {
            this.coreUpgrades.upgradeList.add(UpgradeCore.STORAGE);
            this.getStorage().addStorageUpgrade();
            coreUpgrades.nStorageUpg += 1;
        }

        // Fix for the content
        this.getStorage().setStoredItemType(storage.getItem(), storage.getAmount());
        this.getStorage().setGhosting(storage.isGhosting());

        // We get a new id
        this.id = BSpaceStorageHandler.instance().getNextBarrelID();

        // We update the rendering if possible
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }

        // this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, 1, 1 & 2);
    }

    private ArrayList<ForgeDirection> convertOrientationFlagToForge(int flags) {
        ArrayList<ForgeDirection> directions = new ArrayList<ForgeDirection>();
        for (int i = 0; i < 4; i++) if (((1 << i) & flags) != 0) directions.add(ForgeDirection.getOrientation(i + 2));
        return directions;
    }

    /* END OF V2 COMPATIBILITY METHODS */

    @Override
    public Packet getDescriptionPacket() {
        return BarrelPacketHandler.INSTANCE.channels.get(Side.SERVER)
                .generatePacketFrom(new Message0x00FulleTileEntityNBT(this));
    }

    /* OTHER */
    /*
     * @Override public void markDirty() { super.markDirty(); if (coreUpgrades.hasRedstone || coreUpgrades.hasHopper)
     * this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord,
     * this.yCoord, this.zCoord)); if (!this.worldObj.isRemote) { if (coreUpgrades.hasEnder && this.isLinked) {
     * BSpaceStorageHandler.instance().updateAllBarrels(this.id); } else { this.sendContentSyncPacket(false);
     * this.sendGhostSyncPacket(false); } } }
     */

    @Override
    public void markDirty() {
        super.markDirty();
        ServerTickHandler.INSTANCE.markDirty(this);
        // this.onInventoryChangedExec();
    }

    public void markDirtyExec() {
        super.markDirty();
        if (coreUpgrades.hasRedstone || coreUpgrades.hasHopper) this.worldObj.notifyBlockChange(
                this.xCoord,
                this.yCoord,
                this.zCoord,
                this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
        if (!this.worldObj.isRemote) {
            this.sendContentSyncPacket(false);
            this.sendGhostSyncPacket(false);
        }
    }

    public boolean sendContentSyncPacket(boolean force) {
        IBarrelStorage tempStore = this.getStorage();

        // send if: forced, no previous sent packet, no item(emptying barrel), if the amount differs, or if the item
        // differs
        if (force || lastContentMessage == null /* || !tempStore.hasItem() */
                || lastContentMessage.amount != tempStore.getAmount()
                || !tempStore.sameItem(lastContentMessage.stack)) {
            lastContentMessage = new Message0x01ContentUpdate(this);

            BarrelPacketHandler.INSTANCE.sendToAllAround(
                    lastContentMessage,
                    new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 500));
            return true;
        }

        return false;
    }

    public boolean sendGhostSyncPacket(boolean force) {
        if (force || lastGhostMessage == null || lastGhostMessage.locked != this.getStorage().isGhosting()) {
            lastGhostMessage = new Message0x02GhostUpdate(this);

            BarrelPacketHandler.INSTANCE.sendToAllAround(
                    lastGhostMessage,
                    new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 500));
            return true;
        }

        return false;
    }

    /********************************************/
    /* ISidedInventory Interface Implementation */
    /********************************************/

    @Override
    public int getSizeInventory() {
        return this.getStorage().getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int islot) {
        ItemStack stack = this.getStorage().getStackInSlot(islot);
        ServerTickHandler.INSTANCE.markDirty(this);
        return stack;
    }

    protected AxisAlignedBB aabbBlockBelow = null;

    @Override
    public ItemStack decrStackSize(int islot, int quantity) {
        TileEntity ent = this.worldObj.getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        ItemStack stack;
        // TODO Kotl Remove Code fix dupe
        /*
         * if (ent instanceof TileEntityHopper) { stack = this.getStorage().decrStackSize_Hopper(islot, quantity); }
         * else
         */ if (ent == null) { // not a tile ent, check if a minecart hopper
            if (aabbBlockBelow == null) {
                aabbBlockBelow = AxisAlignedBB
                        .getBoundingBox(xCoord, yCoord - 1, zCoord, xCoord + 1, yCoord, zCoord + 1);
            }
            List list = this.worldObj.selectEntitiesWithinAABB(
                    EntityMinecartHopper.class,
                    aabbBlockBelow,
                    IEntitySelector.selectAnything);
            if (list.size() > 0) {
                stack = this.getStorage().decrStackSize_Hopper(islot, quantity);
            } else {
                // not a minecart hopper, assume something else...
                stack = this.getStorage().decrStackSize(islot, quantity);
            }
        } else { // at this point we know there is a valid tile below, and it's not a hopper
            stack = this.getStorage().decrStackSize(islot, quantity);
        }

        this.markDirty();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int islot, ItemStack stack) {
        this.getStorage().setInventorySlotContents(islot, stack);
        this.markDirty();
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int var1) {
        return null;
    }

    @Override
    public String getInventoryName() {
        return "mcp.mobius.betterbarrel";
    }

    @Override
    public int getInventoryStackLimit() {
        return this.getStorage().getInventoryStackLimit();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer var1) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false
                : var1.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return this.getStorage().isItemValidForSlot(i, itemstack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // TODO : Prevent sides with an hopper upgrade to react as a valid slot.
        if (this.sideUpgrades[side] == UpgradeSide.HOPPER) return new int[] { 1 };
        else return this.getStorage().getAccessibleSlotsFromSide(side);
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
        // TODO : Prevent sides with an hopper upgrade to react as a valid slot.
        if (this.sideUpgrades[side] == UpgradeSide.HOPPER) return false;
        else return this.getStorage().canInsertItem(slot, itemstack, side);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        // TODO : Prevent sides with an hopper upgrade to react as a valid slot.
        return this.getStorage().canExtractItem(slot, itemstack, side);
    }

    /*********************************************/
    /* IDeepStorageUnit Interface Implementation */
    /*********************************************/

    @Override
    public ItemStack getStoredItemType() {
        return this.getStorage().getStoredItemType();
    }

    @Override
    public void setStoredItemCount(int amount) {
        this.getStorage().setStoredItemCount(amount);
        this.markDirty();
    }

    @Override
    public void setStoredItemType(ItemStack type, int amount) {
        this.getStorage().setStoredItemType(type, amount);
        this.markDirty();
    }

    @Override
    public int getMaxStoredCount() {
        return this.getStorage().getMaxStoredCount();
    }

    @Override
    public <T> @Nullable T getCapability(@NotNull Class<T> capability, @NotNull ForgeDirection side) {
        if (capability == ItemSource.class || capability == ItemSink.class || capability == ItemIO.class) {
            return capability.cast(new BarrelItemIO(this));
        }

        return null;
    }

    private static class BarrelItemIO extends SimpleItemIO {

        public final TileEntityBarrel barrel;

        public BarrelItemIO(TileEntityBarrel barrel) {
            this.barrel = barrel;
        }

        private static final int[] SLOTS = { 0 };

        @Override
        protected @NotNull InventoryIterator iterator(int[] allowedSlots) {
            return new AbstractInventoryIterator(SLOTS, allowedSlots) {

                @Override
                protected ItemStack getStackInSlot(int slot) {
                    if (slot != 0) return null;

                    return ItemUtil.copy(barrel.getStoredItemType());
                }

                @Override
                public ItemStack extract(int amount, boolean forced) {
                    ItemStack stored = barrel.getStoredItemType();

                    if (stored == null) return null;

                    int toExtract = Math.min(amount, stored.stackSize);

                    barrel.setStoredItemCount(stored.stackSize - toExtract);

                    return ItemUtil.copyAmount(toExtract, stored);
                }

                @Override
                public int insert(ImmutableItemStack stack, boolean forced) {
                    ItemStack stored = barrel.getStoredItemType();

                    if (stored != null && !stack.matches(stored)) return stack.getStackSize();

                    int storedAmount = stored == null ? 0 : stored.stackSize;
                    int toInsert = Math.min(stack.getStackSize(), barrel.getMaxStoredCount() - storedAmount);

                    if (stored == null) {
                        barrel.setStoredItemType(stack.toStack(1), 1);
                    }

                    barrel.setStoredItemCount(storedAmount + toInsert);

                    return stack.getStackSize() - toInsert;
                }
            };
        }
    }
}
