package mcp.mobius.betterbarrels.bspace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.DimensionManager;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.ServerTickHandler;
import mcp.mobius.betterbarrels.common.blocks.IBarrelStorage;
import mcp.mobius.betterbarrels.common.blocks.StorageLocal;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.common.blocks.logic.Coordinates;

public class BSpaceStorageHandler {

    private int version = 1;

    public static BSpaceStorageHandler _instance = new BSpaceStorageHandler();

    private BSpaceStorageHandler() {}

    public static BSpaceStorageHandler instance() {
        return BSpaceStorageHandler._instance;
    }

    private HashMap<Integer, Coordinates> barrels = new HashMap<Integer, Coordinates>();

    // This variable should store a map between barrels and what storage to access.
    private HashMap<Integer, IBarrelStorage> storageMap = new HashMap<Integer, IBarrelStorage>();

    // This is the original storage map, established prior to linkage (used to restore barrels when the upgrade is
    // removed)
    private HashMap<Integer, IBarrelStorage> storageMapOriginal = new HashMap<Integer, IBarrelStorage>();

    // Table of links to restore proper object sharing on load and transmit signals between barrels
    private HashMap<Integer, HashSet<Integer>> links = new HashMap<Integer, HashSet<Integer>>();

    private int maxBarrelID = 0;

    public int getNextBarrelID() {
        this.maxBarrelID += 1;
        return this.maxBarrelID;
    }

    public void updateBarrel(int id, int dim, int x, int y, int z) {
        Coordinates coord = new Coordinates(dim, x, y, z);
        if (!coord.equals(this.barrels.get(id))) {
            this.barrels.put(id, coord);
            this.writeToFile();
        }
    }

    public void registerEnderBarrel(int id, IBarrelStorage storage) {
        this.storageMap.put(id, storage);
        this.storageMapOriginal.put(id, storage);
        this.writeToFile();
    }

    public IBarrelStorage unregisterEnderBarrel(int id) {
        IBarrelStorage storage = this.storageMapOriginal.get(id);
        this.storageMap.remove(id);
        this.storageMapOriginal.remove(id);
        this.unlinkStorage(id);

        this.writeToFile();
        return storage;
    }

    public IBarrelStorage getStorage(int id) {
        IBarrelStorage storage = this.storageMap.get(id);
        // if (storage == null)
        // throw new RuntimeException("What the hell ?");
        return storage;
    }

    public IBarrelStorage getStorageOriginal(int id) {
        return this.storageMapOriginal.get(id);
    }

    public TileEntityBarrel getBarrel(int id) {
        if (this.barrels.containsKey(id)) {
            Coordinates coord = this.barrels.get(id);
            IBlockAccess world = DimensionManager.getWorld(coord.dim);
            if (world == null) return null;
            TileEntity te = world.getTileEntity(
                    MathHelper.floor_double(coord.x),
                    MathHelper.floor_double(coord.y),
                    MathHelper.floor_double(coord.z));
            if (!(te instanceof TileEntityBarrel)) return null;
            TileEntityBarrel barrel = (TileEntityBarrel) te;
            if (barrel.id != id) return null;
            return barrel;
        }
        return null;
    }

    // Need a way to get a stored inventory

    // Need a way to handle a request for a new inventory

    public void linkStorages(int sourceID, int targetID) {
        this.unlinkStorage(targetID);

        this.storageMap.put(targetID, this.storageMap.get(sourceID));

        if (!links.containsKey(sourceID)) links.put(sourceID, new HashSet<Integer>());

        // We create a new set for this barrel
        links.put(targetID, new HashSet<Integer>());

        // We remove all references of this target in the current link table
        for (HashSet<Integer> set : links.values()) set.remove(targetID);

        // We add this target to the source
        links.get(sourceID).add(targetID);

        // We add the source, target and all previous targets to a tempo hashset
        HashSet<Integer> transferSet = new HashSet<Integer>();
        transferSet.add(sourceID);
        transferSet.add(targetID);
        transferSet.addAll(links.get(sourceID));

        // We update the hashset of all the elements in the source hashset and remove self referencing.
        for (Integer i : links.get(sourceID)) {
            links.get(i).clear();
            links.get(i).addAll(transferSet);
            links.get(i).remove(i);

            TileEntityBarrel barrel = this.getBarrel(i);
            if (barrel != null) barrel.setLinked(true);
        }

        TileEntityBarrel source = this.getBarrel(sourceID);
        if (source != null) source.setLinked(true);

        this.cleanUpLinks();
        this.writeToFile();
    }

    private void cleanUpLinks() {
        // Finally, we cleanup the mess by removing barrels without link data anymore
        HashSet<Integer> keys = new HashSet<Integer>(links.keySet());
        for (Integer i : keys) {
            if (links.get(i).size() == 0) {
                links.remove(i);

                TileEntityBarrel barrel = this.getBarrel(i);
                if (barrel != null) barrel.setLinked(false);
            }
        }
    }

    public IBarrelStorage unlinkStorage(int sourceID) {
        if (!this.links.containsKey(sourceID)) return this.storageMapOriginal.get(sourceID);

        HashSet<Integer> copy = new HashSet<Integer>(this.links.get(sourceID));
        for (Integer targetID : copy) this.links.get(targetID).remove(sourceID);

        this.links.remove(sourceID);

        TileEntityBarrel barrel = this.getBarrel(sourceID);
        if (barrel != null) barrel.setLinked(false);

        this.cleanUpLinks();
        this.writeToFile();

        return this.storageMapOriginal.get(sourceID);
    }

    private void relinkStorages() {
        for (Integer source : this.links.keySet()) for (Integer target : this.links.get(source)) {
            this.storageMap.put(target, this.storageMap.get(source));
        }
    }

    public boolean hasLinks(int sourceID) {
        return this.links.containsKey(sourceID);
    }

    public void updateAllBarrels(int sourceID) {
        if (!this.links.containsKey(sourceID)) return;

        TileEntityBarrel source = this.getBarrel(sourceID);
        if (source == null) return;

        boolean updateRequiredContent = source.sendContentSyncPacket(false);
        boolean updateRequiredGhost = source.sendGhostSyncPacket(false);

        for (Integer targetID : this.links.get(sourceID)) {
            TileEntityBarrel target = this.getBarrel(targetID);
            if (target != null) {
                target.getStorage().setGhosting(source.getStorage().isGhosting());
                target.sendContentSyncPacket(updateRequiredContent);
                target.sendGhostSyncPacket(updateRequiredGhost);
            }
        }
    }

    public void markAllDirty(int sourceID) {
        if (!this.links.containsKey(sourceID)) return;

        TileEntityBarrel source = this.getBarrel(sourceID);
        if (source == null) return;

        for (Integer targetID : this.links.get(sourceID)) {
            TileEntityBarrel target = this.getBarrel(targetID);
            if (target != null) {
                ServerTickHandler.INSTANCE.markDirty(target, false);
            }
        }
    }

    /* ==================================== */
    /* NBT HANDLING */
    /* ==================================== */

    private void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("version", this.version);
        nbt.setInteger("maxBarrelID", this.maxBarrelID);

        NBTTagCompound coords = new NBTTagCompound();
        for (Integer key : this.barrels.keySet())
            coords.setTag(String.valueOf(key), this.barrels.get(key).writeToNBT());
        nbt.setTag("barrelCoords", coords);

        NBTTagCompound stores = new NBTTagCompound();
        for (Integer key : this.storageMap.keySet())
            stores.setTag(String.valueOf(key), this.storageMap.get(key).writeTagCompound());
        nbt.setTag("storages", stores);

        NBTTagCompound storesOriginal = new NBTTagCompound();
        for (Integer key : this.storageMapOriginal.keySet())
            storesOriginal.setTag(String.valueOf(key), this.storageMapOriginal.get(key).writeTagCompound());
        nbt.setTag("storagesOriginal", storesOriginal);

        NBTTagCompound list = new NBTTagCompound();
        for (Integer key : this.links.keySet())
            list.setIntArray(String.valueOf(key), this.convertInts(this.links.get(key)));
        nbt.setTag("links", list);

    }

    private void readFromNBT(NBTTagCompound nbt) { // TODO: check this
        this.maxBarrelID = nbt.hasKey("maxBarrelID") ? nbt.getInteger("maxBarrelID") : 0;
        this.links = new HashMap<Integer, HashSet<Integer>>();

        if (nbt.hasKey("barrelCoords")) {
            NBTTagCompound tag = nbt.getCompoundTag("barrelCoords");
            for (Object key : tag.func_150296_c()) {
                this.barrels.put(Integer.valueOf((String) key), new Coordinates(tag.getCompoundTag((String) key)));
            }
        }

        if (nbt.hasKey("storages")) {
            NBTTagCompound tag = nbt.getCompoundTag("storages");
            for (Object key : tag.func_150296_c()) {
                this.storageMap.put(Integer.valueOf((String) key), new StorageLocal(tag.getCompoundTag((String) key)));
            }
        }

        if (nbt.hasKey("storagesOriginal")) {
            NBTTagCompound tag = nbt.getCompoundTag("storagesOriginal");
            for (Object key : tag.func_150296_c()) {
                this.storageMapOriginal
                        .put(Integer.valueOf((String) key), new StorageLocal(tag.getCompoundTag((String) key)));
            }
        }

        if (nbt.hasKey("links")) {
            NBTTagCompound tag = nbt.getCompoundTag("links");
            for (Object key : tag.func_150296_c()) {
                this.links.put(Integer.valueOf((String) key), this.convertHashSet(tag.getIntArray((String) key)));
            }

            this.relinkStorages();
        }
    }

    /* ==================================== */
    /* FILE HANDLING */
    /* ==================================== */

    private File saveDir;
    private File[] saveFiles;
    private int saveTo;
    private NBTTagCompound saveTag;

    public void writeToFile() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) return;

        try {
            this.writeToNBT(saveTag);

            File saveFile = saveFiles[saveTo];
            if (!saveFile.exists()) saveFile.createNewFile();
            DataOutputStream dout = new DataOutputStream(new FileOutputStream(saveFile));
            CompressedStreamTools.writeCompressed(saveTag, dout);
            dout.close();
            FileOutputStream fout = new FileOutputStream(saveFiles[2]);
            fout.write(saveTo);
            fout.close();
            saveTo ^= 1;
        } catch (Exception e) {
            // if (!saveDir.exists() && MinecraftServer.getServer().isHardcore())
            BetterBarrels.log.info(
                    "JABBA state directory missing. Skipping saving state. If you are in hardcore mode, this is a perfectly normal situation, otherwise, please report to my bugtracker.\n");
            // else
            // throw new RuntimeException(e);
        }
    }

    public void loadFromFile() {
        System.out.printf("Attemping to load JABBA data.\n");

        saveDir = new File(DimensionManager.getCurrentSaveRootDirectory(), "JABBA");
        try {
            if (!saveDir.exists()) saveDir.mkdirs();
            saveFiles = new File[] { new File(saveDir, "data1.dat"), new File(saveDir, "data2.dat"),
                    new File(saveDir, "lock.dat") };

            boolean dataLoaded = false;

            if (saveFiles[2].exists() && saveFiles[2].length() > 0) {
                FileInputStream fin = new FileInputStream(saveFiles[2]);
                saveTo = fin.read() ^ 1;
                fin.close();

                try {
                    if (saveFiles[saveTo ^ 1].exists()) {
                        DataInputStream din = new DataInputStream(new FileInputStream(saveFiles[saveTo ^ 1]));
                        saveTag = CompressedStreamTools.readCompressed(din);
                        din.close();
                        dataLoaded = true;
                    }
                } catch (ZipException e) {
                    if (saveFiles[saveTo].exists()) {
                        DataInputStream din = new DataInputStream(new FileInputStream(saveFiles[saveTo]));
                        saveTag = CompressedStreamTools.readCompressed(din);
                        din.close();
                        dataLoaded = true;
                    }
                }
            }

            if (!dataLoaded) {
                saveTag = new NBTTagCompound();
            }
        } catch (Exception e) {
            if (e instanceof ZipException) {
                BetterBarrels.log.log(Level.ERROR, "Primary and Backup JABBA data files have been corrupted.");
            }
            throw new RuntimeException(e);
        }

        this.readFromNBT(saveTag);
    }

    /* ==================================== */
    /* TYPE CONVERSION */
    /* ==================================== */

    private int[] convertInts(Set<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

    private HashSet<Integer> convertHashSet(int[] list) {
        HashSet<Integer> ret = new HashSet<Integer>();
        for (int i = 0; i < list.length; i++) ret.add(list[i]);
        return ret;

    }
}
