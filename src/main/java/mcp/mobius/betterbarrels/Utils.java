package mcp.mobius.betterbarrels;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.Level;

public class Utils {

    public static void dropItemInWorld(TileEntity source, EntityPlayer player, ItemStack stack, double speedfactor) {
        int hitOrientation = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        double stackCoordX = 0.0D, stackCoordY = 0.0D, stackCoordZ = 0.0D;

        switch (hitOrientation) {
            case 0:
                stackCoordX = source.xCoord + 0.5D;
                stackCoordY = source.yCoord + 0.5D;
                stackCoordZ = source.zCoord - 0.25D;
                break;
            case 1:
                stackCoordX = source.xCoord + 1.25D;
                stackCoordY = source.yCoord + 0.5D;
                stackCoordZ = source.zCoord + 0.5D;
                break;
            case 2:
                stackCoordX = source.xCoord + 0.5D;
                stackCoordY = source.yCoord + 0.5D;
                stackCoordZ = source.zCoord + 1.25D;
                break;
            case 3:
                stackCoordX = source.xCoord - 0.25D;
                stackCoordY = source.yCoord + 0.5D;
                stackCoordZ = source.zCoord + 0.5D;
                break;
        }

        EntityItem droppedEntity = new EntityItem(source.getWorldObj(), stackCoordX, stackCoordY, stackCoordZ, stack);

        if (player != null) {
            Vec3 motion = Vec3.createVectorHelper(
                    player.posX - stackCoordX,
                    player.posY - stackCoordY,
                    player.posZ - stackCoordZ);
            motion.normalize();
            droppedEntity.motionX = motion.xCoord;
            droppedEntity.motionY = motion.yCoord;
            droppedEntity.motionZ = motion.zCoord;
            double offset = 0.25D;
            droppedEntity.moveEntity(motion.xCoord * offset, motion.yCoord * offset, motion.zCoord * offset);
        }

        droppedEntity.motionX *= speedfactor;
        droppedEntity.motionY *= speedfactor;
        droppedEntity.motionZ *= speedfactor;

        source.getWorldObj().spawnEntityInWorld(droppedEntity);
    }

    public static ForgeDirection getDirectionFacingEntity(EntityLivingBase player, boolean allowVertical) {
        Vec3 playerLook = player.getLookVec();

        if (allowVertical) {
            if (playerLook.yCoord <= -BetterBarrels.verticalPlacementRange) {
                return ForgeDirection.UP;
            } else if (playerLook.yCoord >= BetterBarrels.verticalPlacementRange) {
                return ForgeDirection.DOWN;
            }
        }

        if (Math.abs(playerLook.xCoord) >= Math.abs(playerLook.zCoord)) {
            if (playerLook.xCoord > 0) return ForgeDirection.WEST;
            else return ForgeDirection.EAST;
        } else {
            if (playerLook.zCoord > 0) return ForgeDirection.NORTH;
            else return ForgeDirection.SOUTH;
        }
    }

    public static class Material {

        public String name;
        public String modDomain;
        public int meta = 0;
        ArrayList<ItemStack> ores = null;
        static final ItemStack portalStack = new ItemStack(Blocks.portal);

        public Material(String in) {
            if (in.contains("Ore.")) {
                name = in.split("\\.")[1];
            } else if (in.contains(":")) {
                int splitCh = in.indexOf(':');

                modDomain = in.substring(0, splitCh);
                String itemStr = in.substring(splitCh + 1, in.length());

                int metaCh = itemStr.indexOf(':');
                int wildCh = itemStr.indexOf('*');

                if (metaCh >= 0) {
                    if (wildCh == metaCh + 1) {
                        meta = OreDictionary.WILDCARD_VALUE;
                    } else {
                        meta = Integer.parseInt(itemStr.substring(metaCh + 1, itemStr.length()));
                    }

                    name = itemStr.substring(0, metaCh);
                } else {
                    name = itemStr;
                    meta = 0;
                }
            } else {
                BetterBarrels.log.error("Unable to parse input string into oreDict or item:" + in);
            }
        }

        public boolean isOreDict() {
            return this.name != null && this.modDomain == null;
        }

        public ItemStack getStack() {
            return (ItemStack) getStack(false);
        }

        public Object getStack(boolean raw) {
            ItemStack ret = portalStack;
            if (this.isOreDict()) {
                if (raw) return name;
                if (ores == null) {
                    ores = OreDictionary.getOres(this.name);
                }

                if (ores.size() > 0) {
                    if (meta >= ores.size()) {
                        meta = -1;
                    }
                    ret = ores.get(meta >= 0 ? meta : 0);
                }
                BetterBarrels.debug("05 - Looking up [" + this.name + "] and found: " + ret.getDisplayName());
            } else {
                try {
                    ret = new ItemStack((Item) Item.itemRegistry.getObject(modDomain + ":" + name), 1, this.meta);
                    BetterBarrels.debug(
                            "05 - Looking up [" + (this.modDomain + ":" + this.name + ":" + this.meta)
                                    + "] and found: "
                                    + ret.getDisplayName());
                } catch (Throwable t) {
                    BetterBarrels.log.error(
                            "Error while trying to initialize material with name "
                                    + (this.modDomain + ":" + this.name + ":" + this.meta));
                }
            }
            return ret;
        }
    }

    public static class ReflectionHelper {

        static public Method getMethod(Class targetClass, String[] targetNames, Class[] targetParams) {
            return getMethod(
                    targetClass,
                    targetNames,
                    targetParams,
                    Level.ERROR,
                    "Unable to reflect requested method[" + targetNames.toString()
                            + "] with a paramter signature of ["
                            + targetParams.toString()
                            + "] in class["
                            + targetClass.getCanonicalName()
                            + "]");
        }

        static public Method getMethod(Class targetClass, String[] targetNames, Class[] targetParams, Level errorLevel,
                String errorMessage) {
            Method foundMethod = null;
            for (String methodName : targetNames) {
                try {
                    foundMethod = targetClass.getDeclaredMethod(methodName, targetParams);
                    if (foundMethod != null) {
                        foundMethod.setAccessible(true);
                        break;
                    }
                } catch (Throwable t) {}
            }
            if (foundMethod == null && errorMessage != null) {
                BetterBarrels.log.log(errorLevel, errorMessage);
            }
            return foundMethod;
        }

        static public Field getField(Class targetClass, String[] targetNames) {
            return getField(
                    targetClass,
                    targetNames,
                    Level.ERROR,
                    "Unable to reflect requested field[" + targetNames.toString()
                            + "] in class["
                            + targetClass.getCanonicalName()
                            + "]");
        }

        static public Field getField(Class targetClass, String[] targetNames, Level errorLevel, String errorMessage) {
            Field foundField = null;
            for (String fieldName : targetNames) {
                try {
                    foundField = targetClass.getDeclaredField(fieldName);
                    if (foundField != null) {
                        foundField.setAccessible(true);
                        break;
                    }
                } catch (Throwable t) {}
            }
            if (foundField == null && errorMessage != null) {
                BetterBarrels.log.log(errorLevel, errorMessage);
            }
            return foundField;
        }

        static public <T> T getFieldValue(Class<T> returnType, Object targetObject, Class targetClass,
                String[] targetNames) {
            if (!returnType.isPrimitive()) {
                return getFieldValue(
                        returnType,
                        null,
                        targetObject,
                        targetClass,
                        targetNames,
                        Level.ERROR,
                        "Unable to reflect and return value for requested field[" + targetNames.toString()
                                + "] in class["
                                + targetClass.getCanonicalName()
                                + "], defaulting to null or 0");
            } else {
                return getFieldValue(
                        returnType,
                        returnType.cast(0),
                        targetObject,
                        targetClass,
                        targetNames,
                        Level.ERROR,
                        "Unable to reflect and return value for requested field[" + targetNames.toString()
                                + "] in class["
                                + targetClass.getCanonicalName()
                                + "], defaulting to null or 0");
            }
        }

        static public <T> T getFieldValue(Class<T> returnType, T errorValue, Object targetObject, Class targetClass,
                String[] targetNames, Level errorLevel, String errorMessage) {
            T returnValue = errorValue;
            Field foundField = getField(targetClass, targetNames, errorLevel, errorMessage);
            if (foundField != null) {
                try {
                    returnValue = returnType.cast(foundField.get(targetObject));
                    BetterBarrels.debug(
                            "Reflected field [" + foundField.getName()
                                    + "] and found value ["
                                    + returnValue
                                    + "], had a backup value of "
                                    + errorValue);
                } catch (Throwable t) {
                    BetterBarrels.log.error(
                            "Unable to cast found field [" + foundField.getName()
                                    + "] to return type ["
                                    + returnType.getName()
                                    + "]. Defaulting to provided error value.");
                }
            }
            return returnValue;
        }
    }

    public static String romanNumeral(int num) {
        LinkedHashMap<String, Integer> numeralConversion = new LinkedHashMap<String, Integer>();
        numeralConversion.put("M", 1000);
        numeralConversion.put("CM", 900);
        numeralConversion.put("D", 500);
        numeralConversion.put("CD", 400);
        numeralConversion.put("C", 100);
        numeralConversion.put("XC", 90);
        numeralConversion.put("L", 50);
        numeralConversion.put("XL", 40);
        numeralConversion.put("X", 10);
        numeralConversion.put("IX", 9);
        numeralConversion.put("V", 5);
        numeralConversion.put("IV", 4);
        numeralConversion.put("I", 1);

        String result = new String();

        while (numeralConversion.size() > 0) {
            String romanKey = (String) numeralConversion.keySet().toArray()[0];
            Integer arabicValue = (Integer) numeralConversion.values().toArray()[0];
            if (num < arabicValue) {
                numeralConversion.remove(romanKey);
            } else {
                num -= arabicValue;
                result += romanKey;
            }
        }

        return result;
    }

    /*
     * Best Effort attempts to get the TE in the world without creating it. Note: this will return null if the TE does
     * not exist, so be sure to check against For the IBlockAccess it will call one of the two known methods, if it
     * encounters another implementation from another mod it will pass along to the standard getTileEntity, thus
     * probably creating one it it does not exist.
     */
    private static Field chunkCacheWorld = ReflectionHelper
            .getField(ChunkCache.class, new String[] { "e", "field_72815_e", "worldObj" });

    public static TileEntity getTileEntityPreferNotCreating(IBlockAccess blockAccess, int x, int y, int z) {
        if (blockAccess instanceof World) return getTileEntityWithoutCreating((World) blockAccess, x, y, z);
        else if (blockAccess instanceof ChunkCache)
            return getTileEntityWithoutCreating((ChunkCache) blockAccess, x, y, z);
        else return blockAccess.getTileEntity(x, y, z);
    }

    public static TileEntity getTileEntityWithoutCreating(ChunkCache chunkCache, int x, int y, int z) {
        try {
            return getTileEntityWithoutCreating((World) chunkCacheWorld.get(chunkCache), x, y, z);
        } catch (Throwable t) {
            return null;
        }
    }

    public static TileEntity getTileEntityWithoutCreating(World world, int x, int y, int z) {
        if (world.blockExists(x, y, z)) // Method is supposed to be quick, so prevent chunk from being created if it
                                        // does not yet exist
            return world.getChunkFromBlockCoords(x, z).getTileEntityUnsafe(x & 0x0F, y, z & 0x0F);
        else return null;
    }
}
