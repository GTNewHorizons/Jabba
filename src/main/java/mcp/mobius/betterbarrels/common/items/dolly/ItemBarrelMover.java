package mcp.mobius.betterbarrels.common.items.dolly;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameData;
import forestry.factory.recipes.MemorizedRecipe;
import forestry.factory.recipes.RecipeMemory;
import forestry.factory.tiles.TileWorktable;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.common.JabbaCreativeTab;
import mcp.mobius.betterbarrels.common.LocalizedChat;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.network.BarrelPacketHandler;

public class ItemBarrelMover extends Item {

    protected IIcon text_empty = null;
    protected IIcon text_filled = null;
    protected DollyType type = DollyType.NORMAL;

    // Tag key to prevent dolly folding immediately after placing a block
    private static final String PREVENT_FOLD_TAG_KEY = "prevent_fold";
    // List of known TileEntity classes (or interfaces) that the dolly can move
    protected static ArrayList<Class> classExtensions = new ArrayList<Class>();
    // List of class names corresponding to classExtensions, used for initialization
    protected static ArrayList<String> classExtensionsNames = new ArrayList<String>();
    // Map for quick lookup of loaded extension classes by name
    protected static HashMap<String, Class> classMap = new HashMap<String, Class>();

    // List of known spawner class names for special handling
    protected static ArrayList<String> spawnerClassExtensionsNames = new ArrayList<String>();
    // Set of loaded spawner classes
    protected static Set<Class<?>> spawnerClasses = new HashSet<>();

    // Set of additional TileEntity classes that can be moved, configured externally
    protected static HashSet<Class<? extends TileEntity>> extraMovableTileEntityClasses = new HashSet<>(); // Added from
                                                                                                           // modified
                                                                                                           // version

    // Reflection helper to get the NBTTagCompound write method
    protected Method tagCompoundWrite = Utils.ReflectionHelper.getMethod(
            NBTTagCompound.class,
            new String[] { "a", "func_74734_a", "write" },
            new Class[] { java.io.DataOutput.class });

    // Enum defining the types of dollies
    protected enum DollyType {
        NORMAL,
        DIAMOND;
    }

    // Static block to initialize known movable TileEntity classes
    static {
        // Add class names for various mods' TileEntities
        classExtensionsNames.add("appeng.tile.storage.TileSkyChest"); // Applied Energistics 2

        classExtensionsNames.add("cpw.mods.ironchest.TileEntityIronChest"); // Iron Chests

        classExtensionsNames.add("buildcraft.energy.TileEngine"); // BuildCraft
        classExtensionsNames.add("buildcraft.factory.TileTank"); // BuildCraft

        // classExtensionsNames.add("ic2.api.energy.tile.IEnergySink"); // IC2
        // (Commented out in original)
        // classExtensionsNames.add("ic2.api.energy.tile.IEnergySource"); // IC2
        // (Commented out in original)
        classExtensionsNames.add("ic2.api.tile.IWrenchable"); // IndustrialCraft 2 API

        classExtensionsNames.add("mods.railcraft.common.blocks.machine.beta.TileEngine"); // Railcraft

        classExtensionsNames.add("forestry.core.gadgets.Engine"); // Forestry
        classExtensionsNames.add("forestry.apiculture.tiles.TileApiaristChest"); // Forestry
        classExtensionsNames.add("forestry.arboriculture.tiles.TileArboristChest"); // Forestry
        classExtensionsNames.add("forestry.lepidopterology.tiles.TileLepidopteristChest"); // Forestry
        classExtensionsNames.add("forestry.factory.tiles.TileWorktable"); // Forestry

        classExtensionsNames.add("bluedart.tile.TileEntityForceEngine"); // DartCraft

        classExtensionsNames.add("thermalexpansion.block.engine.TileEngineRoot"); // Thermal Expansion
        classExtensionsNames.add("thermalexpansion.block.machine.TileMachineRoot"); // Thermal Expansion

        // classExtensionsNames.add("factorization.common.TileEntityBarrel"); //
        // Factorization (Commented out in original)

        classExtensionsNames.add("dmillerw.cchests.block.tile.TileChest"); // CompactChests

        classExtensionsNames.add("net.mcft.copy.betterstorage.block.tileentity.TileEntityReinforcedChest"); // BetterStorage
        classExtensionsNames.add("net.mcft.copy.betterstorage.block.tileentity.TileEntityLocker"); // BetterStorage
        classExtensionsNames.add("net.mcft.copy.betterstorage.block.tileentity.TileEntityCardboardBox"); // BetterStorage
        classExtensionsNames.add("net.mcft.copy.betterstorage.tile.entity.TileEntityConnectable"); // BetterStorage API
        classExtensionsNames.add("net.mcft.copy.betterstorage.block.tileentity.TileEntityConnectable"); // BetterStorage
        classExtensionsNames.add("net.mcft.copy.betterstorage.api.lock.ILockable"); // BetterStorage API
        classExtensionsNames.add("net.mcft.copy.betterstorage.api.ILockable"); // BetterStorage API (Older?)

        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityBookcase"); // Bibliocraft
        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityPotionShelf"); // Bibliocraft
        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityWeaponRack"); // Bibliocraft
        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityGenericShelf"); // Bibliocraft
        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityArmorStand"); // Bibliocraft
        classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityLabel"); // Bibliocraft
        // classExtensionsNames.add("jds.bibliocraft.tileentities.TileEntityWeaponCase");
        // // Bibliocraft (Commented out in original)

        classExtensionsNames.add("com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers"); // Storage
                                                                                                        // Drawers
        classExtensionsNames.add("com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityCompDrawers"); // Storage
                                                                                                            // Drawers

        classExtensionsNames.add("com.bluepowermod.tile.TileBase"); // Blue Power

        classExtensionsNames.add("com.rwtema.extrautils.tileentity.chests.TileFullChest"); // Extra Utilities

        classExtensionsNames.add("team.chisel.block.tileentity.TileEntityPresent"); // Chisel

        classExtensionsNames.add("ganymedes01.etfuturum.tileentities.TileEntityBarrel"); // Et Futurum

        // Attempt to load the classes by name
        for (String s : classExtensionsNames) {
            try {
                Class<?> loadedClass = Class.forName(s);
                classExtensions.add(loadedClass);
                classMap.put(s, loadedClass);
            } catch (ClassNotFoundException e) {
                classExtensions.add(null); // Add null if class is not found
            }
        }

        // Initialize spawner classes
        spawnerClassExtensionsNames.add("chylex.hee.tileentity.TileEntityCustomSpawner"); // Hardcore Ender Expansion
        spawnerClasses.add(TileEntityMobSpawner.class); // Vanilla Spawner

        // Attempt to load spawner classes
        for (String s : spawnerClassExtensionsNames) {
            try {
                spawnerClasses.add(Class.forName(s));
            } catch (ClassNotFoundException ignored) {}
        }
    }

    /**
     * Initializes the set of extra movable TileEntity classes from the configuration. This method should be called
     * after the configuration has been loaded. New method in modified version.
     */
    public static void initializeExtraMovableClasses() {
        extraMovableTileEntityClasses.clear();
        for (String className : BetterBarrels.extraDollyMovableTileEntityClassNames) { // Iterate through configured
                                                                                       // class names
            if (className == null || className.trim().isEmpty()) {
                continue; // Skip empty entries
            }
            try {
                Class<?> aClass = Class.forName(className.trim()); // Try loading the class
                if (TileEntity.class.isAssignableFrom(aClass)) { // Check if it's a TileEntity subclass
                    @SuppressWarnings("unchecked")
                    Class<? extends TileEntity> teClass = (Class<? extends TileEntity>) aClass;
                    extraMovableTileEntityClasses.add(teClass); // Add to the set
                    BetterBarrels.log.log(Level.INFO, "Adding " + className + " to Dolly movable list via config."); // Log
                                                                                                                     // success
                } else {
                    BetterBarrels.log.log(
                            Level.WARN,
                            "Class " + className + " from Dolly config is not a TileEntity subclass. Ignoring."); // Log
                                                                                                                  // warning
                                                                                                                  // if
                                                                                                                  // not
                                                                                                                  // a
                                                                                                                  // TE
                }
            } catch (ClassNotFoundException e) {
                BetterBarrels.log
                        .log(Level.WARN, "Could not find class " + className + " specified in Dolly config. Ignoring."); // Log
                                                                                                                         // warning
                                                                                                                         // if
                                                                                                                         // class
                                                                                                                         // not
                                                                                                                         // found
            } catch (Throwable t) {
                BetterBarrels.log.log(Level.ERROR, "Error processing class " + className + " for Dolly config.", t); // Log
                                                                                                                     // error
                                                                                                                     // on
                                                                                                                     // other
                                                                                                                     // issues
            }
        }
    }

    // Constructor
    public ItemBarrelMover() {
        super();
        this.setMaxStackSize(1); // Dolly is not stackable when holding a block
        // this.setHasSubtypes(true); // Commented out in original
        // this.setMaxDamage(0); // Commented out in original
        this.setCreativeTab(JabbaCreativeTab.tab); // Set creative tab
        this.setNoRepair(); // Cannot be repaired
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        // Register empty and filled icons based on dolly type
        this.itemIcon = par1IconRegister
                .registerIcon(BetterBarrels.modid + ":dolly_" + this.type.name().toLowerCase() + "_empty");
        this.text_empty = this.itemIcon;
        this.text_filled = par1IconRegister
                .registerIcon(BetterBarrels.modid + ":dolly_" + this.type.name().toLowerCase() + "_filled");
    }

    @Override
    public String getUnlocalizedName() {
        return getUnlocalizedName(null);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        // Return different names based on whether the dolly is holding a block
        if (stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("Container"))
            return "item.dolly." + this.type.name().toLowerCase() + ".full";
        else return "item.dolly." + this.type.name().toLowerCase() + ".empty";
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        return this.getIconIndex(stack);
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        // Return different icons based on whether the dolly is holding a block
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Container")) return this.text_filled;
        else return this.text_empty;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false; // Server-side only
        }

        // Check for block protection (e.g., spawn protection)
        if (FMLCommonHandler.instance().getMinecraftServerInstance().isBlockProtected(world, x, y, z, player)) {
            return false;
        }

        // If dolly is empty, try to pick up a container
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("Container")) {
            return this.pickupContainer(stack, player, world, x, y, z);
        }

        // If dolly is full, try to place the container
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Container")) {
            // Set a temporary tag to prevent folding on right-click release
            stack.getTagCompound().setBoolean(PREVENT_FOLD_TAG_KEY, true);
            return this.placeContainer(stack, player, world, x, y, z, side);
        }

        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return itemStack; // Server-side only
        }

        // Check config to disable folding/stacking
        if (BetterBarrels.disableDollyStacking) {
            return itemStack;
        }

        // Prevent folding if the prevent tag is present (set during block placement)
        if (itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey(PREVENT_FOLD_TAG_KEY)) {
            itemStack.getTagCompound().removeTag(PREVENT_FOLD_TAG_KEY); // Remove the tag
            return itemStack;
        }

        // Fold the dolly if sneaking, it's a normal dolly, and it's empty
        if (player.isSneaking() && type == DollyType.NORMAL
                && (!itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("Container"))) {
            // Diamond dollies can't be folded because they can be damaged.
            final EntityItem newItem = new EntityItem(
                    world,
                    player.posX,
                    player.posY,
                    player.posZ,
                    new ItemStack(BetterBarrels.itemFoldedMover, 1)); // Spawn folded dolly item
            newItem.delayBeforeCanPickup = 0; // Allow immediate pickup
            world.spawnEntityInWorld(newItem);

            itemStack.stackSize -= 1; // Decrease stack size of the current dolly
        }

        return itemStack;
    }

    /**
     * Places the container stored in the dolly into the world.
     * 
     * @param stack  The dolly ItemStack.
     * @param player The player placing the container.
     * @param world  The world.
     * @param x      X coordinate of the clicked block.
     * @param y      Y coordinate of the clicked block.
     * @param z      Z coordinate of the clicked block.
     * @param side   The side of the block that was clicked.
     * @return True if placement was successful, false otherwise.
     */
    protected boolean placeContainer(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
        NBTTagCompound nbtStack = stack.getTagCompound();
        NBTTagCompound nbtContainerStack = nbtStack.getCompoundTag("Container"); // NBT data for the stored block/TE

        // Get stored block information
        Block storedBlock;
        if (nbtContainerStack.hasKey("ID")) { // Check for older ID key
            storedBlock = Block.getBlockById(nbtContainerStack.getInteger("ID"));
        } else { // Use name key
            storedBlock = Block.getBlockFromName(nbtContainerStack.getString("Block"));
        }
        int blockMeta = nbtContainerStack.getInteger("Meta"); // Stored metadata
        String TEClassName = nbtContainerStack.getString("TEClass"); // Stored TileEntity class name
        NBTTagCompound nbtContainer = nbtStack.getCompoundTag("Container").getCompoundTag("NBT"); // Stored TileEntity
                                                                                                  // NBT data

        ForgeDirection targSide = ForgeDirection.getOrientation(side); // Direction player clicked
        // if (world.isBlockSolidOnSide(x, y, z, targSide)) {return false;} // Commented
        // out in original

        // Calculate target placement coordinates
        int targX = x;
        int targY = y;
        int targZ = z;

        Block targetBlock = world.getBlock(targX, targY, targZ); // Block being clicked on

        // Adjust placement if clicking on snow layer
        if (targetBlock == Blocks.snow) targSide = ForgeDirection.UP;

        // Adjust placement coordinates if the clicked block is not replaceable
        if (targetBlock != Blocks.vine && targetBlock != Blocks.tallgrass && targetBlock != Blocks.deadbush
        // && (Block.blocksList[targetBlock] == null ||
        // !Block.blocksList[targetBlock].isBlockReplaceable(world, targX, targY,
        // targZ))) { // Old check
                && (targetBlock == null || !targetBlock.isReplaceable(world, targX, targY, targZ))) { // Check if block
                                                                                                      // is replaceable
            // Move placement coords based on clicked side
            if (targSide.equals(ForgeDirection.NORTH)) targZ -= 1;
            if (targSide.equals(ForgeDirection.SOUTH)) targZ += 1;
            if (targSide.equals(ForgeDirection.WEST)) targX -= 1;
            if (targSide.equals(ForgeDirection.EAST)) targX += 1;
            if (targSide.equals(ForgeDirection.UP)) targY += 1;
            if (targSide.equals(ForgeDirection.DOWN)) targY -= 1;
        }

        // Check if the block can be placed at the target location
        if (!(world.canPlaceEntityOnSide(storedBlock, targX, targY, targZ, false, side, (Entity) null, stack))) {
            return false;
        }

        // Update coordinates in the TileEntity NBT
        nbtContainer.setInteger("x", targX);
        nbtContainer.setInteger("y", targY);
        nbtContainer.setInteger("z", targZ);

        // === Orientation Adjustments for Various Mods ===

        /* Vanilla chest */
        if (TEClassName.contains("net.minecraft.tileentity.TileEntityChest"))
            blockMeta = this.getBarrelOrientationOnPlacement(player).ordinal(); // Set meta based on player facing

        /* AE2 sky stone chest orientation correction */
        else if (TEClassName.contains("appeng.tile.storage.TileSkyChest") && nbtContainer.hasKey("orientation_forward"))
            nbtContainer.setString("orientation_forward", this.getBarrelOrientationOnPlacement(player).toString()); // Set
                                                                                                                    // AE2
                                                                                                                    // orientation

        /* Buildcraft engines orientation correction */
        else if (TEClassName.contains("buildcraft.energy.TileEngine") && nbtContainer.hasKey("orientation"))
            nbtContainer.setInteger("orientation", 1); // Set BC engine orientation (always up?)

        /* Railcraft engines orientation correction */
        else if (TEClassName.contains("mods.railcraft.common.blocks.machine.beta") && nbtContainer.hasKey("direction"))
            nbtContainer.setByte("direction", (byte) 1); // Set RC engine orientation (always up?)

        /* Forestry engines orientation correction */
        else if (TEClassName.contains("forestry.energy.gadgets") && nbtContainer.hasKey("Orientation"))
            nbtContainer.setInteger("Orientation", 1); // Set Forestry engine orientation (always up?)

        /* Forestry chests and worktable orientation correction */
        else if ((TEClassName.contains("forestry.apiculture.tiles.TileApiaristChest")
                || TEClassName.contains("forestry.arboriculture.tiles.TileArboristChest")
                || TEClassName.contains("forestry.lepidopterology.tiles.TileLepidopteristChest")
                || TEClassName.contains("forestry.factory.tiles.TileWorktable")) && nbtContainer.hasKey("Orientation"))
            nbtContainer.setInteger("Orientation", this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                            // Forestry
                                                                                                            // chest/worktable
                                                                                                            // orientation

        /* Dartcraft engines orientation correction */
        else if (TEClassName.contains("bluedart.tile.TileEntityForceEngine") && nbtContainer.hasKey("facing"))
            nbtContainer.setByte("facing", (byte) 1); // Set Dartcraft engine orientation (always up?)

        /* Thermal Expansion engines */
        else if (TEClassName.contains("thermalexpansion.block.engine") && nbtContainer.hasKey("side.facing"))
            nbtContainer.setByte("side.facing", (byte) 1); // Set TE engine orientation (always up?)

        /* Iron chest orientation correction */
        else if (TEClassName.contains("cpw.mods.ironchest") && nbtContainer.hasKey("facing"))
            nbtContainer.setByte("facing", (byte) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set Iron
                                                                                                           // Chest
                                                                                                           // orientation

        /* IC2 Orientation correction part1 */
        else if (TEClassName.contains("ic2.core.block") && nbtContainer.hasKey("facing"))
            nbtContainer.setShort("facing", (short) 6); // Set IC2 facing to default (6=none?), fixed later

        /* Gregtech Orientation Correction */
        else if (TEClassName.contains("gregtechmod") && nbtContainer.hasKey("mFacing"))
            nbtContainer.setShort("mFacing", (short) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                              // GregTech
                                                                                                              // facing

        /* Dmillerw (CompactChests) Orientation Correction */
        else if (TEClassName.contains("dmillerw.cchests.block.tile") && nbtContainer.hasKey("orientation"))
            nbtContainer.setByte("orientation", (byte) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                                // CompactChests
                                                                                                                // orientation

        /* BetterStorage Orientation Correction */
        else if (TEClassName.contains("net.mcft.copy.betterstorage.block.tileentity")
                && nbtContainer.hasKey("orientation"))
            nbtContainer.setByte("orientation", (byte) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                                // BetterStorage
                                                                                                                // orientation

        /* Bibliocraft orientation correction block */
        else if (TEClassName.contains("jds.bibliocraft.tileentities")) {
            // Use helper for Bibliocraft angle keys
            orientBibliocraftTileToPlayer(nbtContainer, "bookcaseAngle", player);
            orientBibliocraftTileToPlayer(nbtContainer, "potionshelfAngle", player);
            orientBibliocraftTileToPlayer(nbtContainer, "rackAngle", player);
            orientBibliocraftTileToPlayer(nbtContainer, "genericShelfAngle", player);
            orientBibliocraftTileToPlayer(nbtContainer, "labelAngle", player);
        }

        /* Bibliocraft Armor Stand (uses metadata for orientation) */
        else if (TEClassName.contains("jds.bibliocraft.tileentities.TileEntityArmorStand"))
            blockMeta = this.fromForgeToBiblio(this.getBarrelOrientationOnPlacement(player)); // Set metadata based on
                                                                                              // Bibliocraft orientation

        /* Extra Utilities chest orientation correction (uses metadata) */
        else if (TEClassName.contains("com.rwtema.extrautils.tileentity.chests.TileFullChest"))
            // Adjust Bibliocraft rotation for Extra Utilities
            blockMeta = (this.fromForgeToBiblio(this.getBarrelOrientationOnPlacement(player)) == 5 ? 0 // SOUTH maps to
                                                                                                       // 0
                    : this.fromForgeToBiblio(this.getBarrelOrientationOnPlacement(player)) + 1); // Others incremented?
                                                                                                 // (Seems complex,
                                                                                                 // check XU source if
                                                                                                 // needed) ->
                                                                                                 // Simplified from
                                                                                                 // original

        /* Chisel Present Chest orientation correction */
        else if (TEClassName.contains("team.chisel.block.tileentity.TileEntityPresent")
                && nbtContainer.hasKey("rotation"))
            nbtContainer.setInteger("rotation", (byte) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                                // Chisel
                                                                                                                // present
                                                                                                                // rotation

        /* Factorization barrel (Commented out in original) */
        // if (TEClassName.contains("factorization.common.TileEntityBarrel") &&
        // nbtContainer.hasKey("facing"))
        // nbtContainer.setByte("facing",
        // (byte)this.getBarrelOrientationOnPlacement(player).ordinal());

        /* Storage Drawers Orientation Correction */
        else if (TEClassName.contains("com.jaquadro.minecraft.storagedrawers.block.tile") && nbtContainer.hasKey("Dir"))
            nbtContainer.setInteger("Dir", (short) this.getBarrelOrientationOnPlacement(player).ordinal()); // Set
                                                                                                            // Storage
                                                                                                            // Drawers
                                                                                                            // direction

        /* Blue Power Orientation Correction */
        else if (TEClassName.contains("com.bluepowermod.tile") && nbtContainer.hasKey("rotation")) {
            try {
                // Reflection to check if vertical rotation is allowed
                Class blockClazz = storedBlock.getClass();
                Method allowVertical = null;
                while (allowVertical == null && !blockClazz.equals(Object.class)) {
                    try {
                        allowVertical = blockClazz.getDeclaredMethod("canRotateVertical");
                    } catch (NoSuchMethodException e) {
                        blockClazz = blockClazz.getSuperclass(); // Check superclass
                    }
                }

                allowVertical.setAccessible(true);
                boolean vertAllowed = ((Boolean) allowVertical.invoke(storedBlock, (Object[]) null)).booleanValue();
                // Set BluePower rotation based on player facing and vertical allowance
                nbtContainer
                        .setInteger("rotation", (short) Utils.getDirectionFacingEntity(player, vertAllowed).ordinal());
            } catch (Exception e) {
                BetterBarrels.log.warn(
                        "Unable to rotate BluePower machine. place machine will not be rotated to be facing player."); // Log
                                                                                                                       // warning
                                                                                                                       // on
                                                                                                                       // failure
            }
        }

        /* Thermal Expansion Machine Orientation Correction */
        else if (TEClassName.contains("thermalexpansion.block.machine") && nbtContainer.hasKey("side.facing")) {
            ForgeDirection side_facing = ForgeDirection.getOrientation(nbtContainer.getByte("side.facing")); // Old
                                                                                                             // facing
            ForgeDirection new_facing = this.getBarrelOrientationOnPlacement(player); // New facing
            byte[] side_array_old = nbtContainer.getByteArray("side.array"); // Old side config
            byte[] side_array_new = side_array_old.clone(); // New side config

            int rotations = 0; // Calculate rotations needed
            while (side_facing != new_facing) {
                rotations += 1;
                side_facing = side_facing.getRotation(ForgeDirection.UP); // Rotate around Y axis
            }

            // Rotate the side configurations (sides 2-5: N, S, W, E)
            for (int i = 2; i < 6; i++) {
                ForgeDirection new_direction = ForgeDirection.getOrientation(i);
                for (int j = 0; j < rotations; j++) new_direction = new_direction.getRotation(ForgeDirection.DOWN); // Rotate
                                                                                                                    // opposite
                                                                                                                    // way
                                                                                                                    // to
                                                                                                                    // map
                                                                                                                    // old
                                                                                                                    // to
                                                                                                                    // new
                side_array_new[i] = side_array_old[new_direction.ordinal()];
            }

            nbtContainer.setByteArray("side.array", side_array_new); // Set new side config
            nbtContainer.setByte("side.facing", (byte) new_facing.ordinal()); // Set new facing
        }

        /* Et Futurum Barrel Orientation Correction */
        else if (TEClassName.contains("ganymedes01.etfuturum.tileentities.TileEntityBarrel")) {
            blockMeta = this.getBarrelOrientationOnPlacement(player).ordinal(); // Set meta based on player facing
        }

        /* Better barrel craziness (Own barrel orientation) */
        // Check if it's our barrel TE (excluding Et Futurum barrel which shares the
        // name)
        // else if
        // (!TEClassName.contains("ganymedes01.etfuturum.tileentities.TileEntityBarrel")
        // // Redundant check now based on TE id check below
        // && nbtContainer.getString("id").equals("TileEntityBarrel")) {
        else if (nbtContainer.getString("id").equals("TileEntityBarrel")) { // Check NBT id for our TileEntityBarrel
            ForgeDirection newBarrelRotation = Utils.getDirectionFacingEntity(player, false); // Horizontal facing
            ForgeDirection oldBarrelRotation = ForgeDirection.getOrientation(nbtContainer.getInteger("rotation")); // Old
                                                                                                                   // horizontal
                                                                                                                   // facing
            ForgeDirection newBarrelOrientation = Utils
                    .getDirectionFacingEntity(player, BetterBarrels.allowVerticalPlacement); // Facing potentially
                                                                                             // vertical
            ForgeDirection oldBarrelOrientation = ForgeDirection.getOrientation(nbtContainer.getInteger("orientation")); // Old
                                                                                                                         // potentially
                                                                                                                         // vertical
                                                                                                                         // facing
            int[] newSideArray = new int[6]; // New upgrade array
            int[] oldSideArray = nbtContainer.getIntArray("sideUpgrades"); // Old upgrade array
            int[] newSideMetaArray = new int[6]; // New meta array
            int[] oldSideMetaArray = nbtContainer.getIntArray("sideMeta"); // Old meta array

            /*
             * Note: the barrel should never have these values set as unknown, but this will prevent the code from
             * crashing or infinite looping
             */
            if (oldBarrelRotation == ForgeDirection.UNKNOWN) oldBarrelRotation = ForgeDirection.SOUTH;
            if (oldBarrelOrientation == ForgeDirection.UNKNOWN) oldBarrelOrientation = ForgeDirection.SOUTH;

            /* Normalize the barrel so it is upright and front facing player */
            // If the barrel was originally placed vertically (top/bottom face as front)
            if (oldBarrelOrientation == ForgeDirection.UP || oldBarrelOrientation == ForgeDirection.DOWN) {
                // Rotate side arrays to simulate the barrel being upright with the original
                // 'rotation' side now facing forward
                ForgeDirection rot = oldBarrelRotation.getRotation(oldBarrelOrientation);
                for (int i = 0; i < 6; i++) {
                    int j = ForgeDirection.getOrientation(i).getRotation(rot).ordinal();
                    newSideArray[j] = oldSideArray[i];
                    newSideMetaArray[j] = oldSideMetaArray[i];
                }
                oldBarrelOrientation = oldBarrelRotation; // Treat the normalized orientation as the 'rotation' side
                oldSideArray = newSideArray.clone();
                oldSideMetaArray = newSideMetaArray.clone();
            }

            /* Rotate around vertical axis */
            int numberRotationsVAxis = 0; // Calculate rotations needed around Y axis
            while (newBarrelRotation != oldBarrelRotation) {
                numberRotationsVAxis += 1;
                oldBarrelRotation = oldBarrelRotation.getRotation(ForgeDirection.UP);
            }

            // Apply Y-axis rotation to side arrays
            for (int i = 0; i < 6; i++) {
                ForgeDirection idir = ForgeDirection.getOrientation(i);
                for (int rot = 0; rot < numberRotationsVAxis; rot++) {
                    idir = idir.getRotation(ForgeDirection.UP);
                }
                newSideArray[idir.ordinal()] = oldSideArray[i];
                newSideMetaArray[idir.ordinal()] = oldSideMetaArray[i];
            }

            /* if new orientation is up/down, rotate appropriately */
            // If the new placement is vertical
            if (newBarrelOrientation == ForgeDirection.UP || newBarrelOrientation == ForgeDirection.DOWN) {
                oldSideArray = newSideArray.clone(); // Start from the Y-rotated state
                oldSideMetaArray = newSideMetaArray.clone();
                // Calculate rotation needed to tilt the barrel up/down
                ForgeDirection rot = newBarrelRotation.getRotation(newBarrelOrientation.getOpposite());
                // Apply tilt rotation to side arrays
                for (int i = 0; i < 6; i++) {
                    int j = ForgeDirection.getOrientation(i).getRotation(rot).ordinal();
                    newSideArray[j] = oldSideArray[i];
                    newSideMetaArray[j] = oldSideMetaArray[i];
                }
            }

            // Store final orientation, rotation, and side arrays
            nbtContainer.setInteger("orientation", newBarrelOrientation.ordinal());
            nbtContainer.setInteger("rotation", newBarrelRotation.ordinal());
            nbtContainer.setIntArray("sideUpgrades", newSideArray);
            nbtContainer.setIntArray("sideMeta", newSideMetaArray);
        }

        // === Place the Block and TileEntity ===

        // Place the block in the world with calculated meta and flags (1=update,
        // 2=notify neighbors)
        world.setBlock(targX, targY, targZ, storedBlock, blockMeta, 1 + 2);
        // Get the newly placed TileEntity
        TileEntity entity = world.getTileEntity(targX, targY, targZ);
        // Read the stored NBT data (including corrected coordinates and orientation)
        // into the TE
        entity.readFromNBT(nbtContainer);

        /* IC2 orientation fix part2 */
        // If the TE is wrenchable (IC2), apply specific orientation fix
        if (classMap.get("ic2.api.tile.IWrenchable") != null
                && classMap.get("ic2.api.tile.IWrenchable").isInstance(entity))
            this.fixIC2Orientation(entity, player, targY);

        /* Post-Placement Fixes */

        // Vanilla Chest needs metadata update after TE is placed
        if (TEClassName.contains("net.minecraft.tileentity.TileEntityChest"))
            world.setBlockMetadataWithNotify(targX, targY, targZ, blockMeta, 1 + 2);

        // Forestry Worktable needs recipe outputs recalculated
        else if (TEClassName.contains("forestry.factory.tiles.TileWorktable")) {
            // Need to manually rebuild recipe outputs for memorized recipes.
            // They don't get rebuilt when reading from NBT, for some reason.
            RecipeMemory recipeMemory = ((TileWorktable) entity).getMemory();
            for (int i = 0; i < RecipeMemory.capacity; i++) {
                MemorizedRecipe recipe = recipeMemory.getRecipe(i);
                if (recipe != null) {
                    recipe.calculateRecipeOutput(world); // Recalculate output
                }
            }
        }

        // Remove the container data from the dolly ItemStack
        stack.getTagCompound().removeTag("Container");

        // Mark the block for update to sync client/server
        world.markBlockForUpdate(targX, targY, targZ);

        return true; // Placement successful
    }

    /**
     * Helper method to set Bibliocraft orientation based on player facing.
     * 
     * @param nbtContainer The TileEntity NBT.
     * @param angleKey     The NBT key for the angle (e.g., "bookcaseAngle").
     * @param player       The player placing the block.
     */
    private void orientBibliocraftTileToPlayer(NBTTagCompound nbtContainer, String angleKey, EntityPlayer player) {
        if (nbtContainer.hasKey(angleKey))
            nbtContainer.setInteger(angleKey, this.fromForgeToBiblio(this.getBarrelOrientationOnPlacement(player))); // Convert
                                                                                                                     // ForgeDirection
                                                                                                                     // to
                                                                                                                     // Bibliocraft
                                                                                                                     // angle
    }

    /**
     * Applies specific orientation fixes for IC2 machines using reflection.
     * 
     * @param entity The IC2 TileEntity.
     * @param player The player placing the block.
     * @param targY  The Y coordinate of placement.
     */
    private void fixIC2Orientation(TileEntity entity, EntityPlayer player, int targY) {
        try {
            // Get IC2 methods via reflection
            Method setFacing = classMap.get("ic2.api.tile.IWrenchable")
                    .getMethod("setFacing", new Class[] { short.class });
            Method wrenchCanSetFacing = classMap.get("ic2.api.tile.IWrenchable")
                    .getMethod("wrenchCanSetFacing", new Class[] { EntityPlayer.class, int.class });

            // Determine target facing, trying vertical first if allowed
            ForgeDirection targetFacingVertical = this.getBarrelOrientationOnPlacement(player, targY, true);
            ForgeDirection targetFacingHorizontal = this.getBarrelOrientationOnPlacement(player, targY, false);

            // Check if the machine allows setting the vertical facing
            if ((Boolean) wrenchCanSetFacing.invoke(entity, player, (short) targetFacingVertical.ordinal()))
                setFacing.invoke(entity, (short) targetFacingVertical.ordinal()); // Set vertical facing
            else setFacing.invoke(entity, (short) targetFacingHorizontal.ordinal()); // Set horizontal facing

        } catch (Exception e) {
            BetterBarrels.log.warn("Failed to apply IC2 orientation fix for " + entity.getClass().getName(), e); // Log
                                                                                                                 // warning
                                                                                                                 // on
                                                                                                                 // failure
                                                                                                                 // (Log
                                                                                                                 // added
                                                                                                                 // for
                                                                                                                 // clarity)
            // e.printStackTrace(); // Original exception handling
        }
    }

    /**
     * Checks if a TileEntity is movable by the dolly.
     * 
     * @param te The TileEntity to check.
     * @return True if movable, false otherwise.
     */
    private boolean isTEMovable(TileEntity te) {
        // Spawners handled separately based on dolly type
        if (tileIsASpawner(te)) return this.canPickSpawners();
        // Barrels and Chests are always movable by default
        if (te instanceof TileEntityBarrel) return true;
        if (te instanceof TileEntityChest) return true;

        Class<? extends TileEntity> teClass = te.getClass(); // Get the TE class

        // Check blacklist first
        if (isTileBlacklisted(teClass)) {
            return false;
        }
        // Check hardcoded known movable classes/interfaces
        for (Class c : classExtensions) {
            if (c != null && c.isInstance(te)) return true;
        }
        // Check additional classes from config
        for (Class<? extends TileEntity> extraClass : extraMovableTileEntityClasses) {
            // Use isAssignableFrom to check if the TE is an instance of or subclass of the
            // configured class
            if (extraClass.isAssignableFrom(teClass)) {
                return true;
            }
        }

        return false; // Not movable if none of the checks pass
    }

    /**
     * Checks if a TileEntity is a known spawner type.
     * 
     * @param te The TileEntity.
     * @return True if it's a known spawner.
     */
    private static boolean tileIsASpawner(TileEntity te) {
        for (Class<?> c : spawnerClasses) {
            if (c != null && c.isInstance(te)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a TileEntity class is blacklisted in the config. Uses isAssignableFrom for broader checking (includes
     * subclasses).
     * 
     * @param clazz The TileEntity class to check.
     * @return True if blacklisted, false otherwise.
     */
    private boolean isTileBlacklisted(Class<? extends TileEntity> clazz) {
        for (Class<? extends TileEntity> blacklistedClass : BetterBarrels.BlacklistedTileEntityClasses) {
            // Check if the provided class is assignable from (is instance of or subclass
            // of) the blacklisted class
            if (blacklistedClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies specific fixes required before picking up BetterStorage blocks. Checks for locks and disconnects
     * connectable blocks.
     * 
     * @param container The TileEntity being picked up.
     * @return True if pickup can proceed, false if prevented (e.g., locked).
     */
    private boolean pickupBetterStorageFix(TileEntity container) {
        // Check ILockable interface (newer API?)
        if (classMap.get("net.mcft.copy.betterstorage.api.lock.ILockable") != null
                && classMap.get("net.mcft.copy.betterstorage.api.lock.ILockable").isInstance(container)) {
            try {
                Method getLock = classMap.get("net.mcft.copy.betterstorage.api.lock.ILockable")
                        .getDeclaredMethod("getLock", (Class[]) null);
                Object lock = getLock.invoke(container, (Object[]) null);
                if (lock != null) return false; // Don't pick up if locked
            } catch (Exception e) {
                BetterBarrels.log.warn(
                        "Error checking BetterStorage lock (api.lock.ILockable) for " + container.getClass().getName(),
                        e); // Log warning
                // System.out.printf("%s \n", e); // Original logging
                return false; // Prevent pickup on error
            }
        }

        // Check ILockable interface (older API?)
        if (classMap.get("net.mcft.copy.betterstorage.api.ILockable") != null
                && classMap.get("net.mcft.copy.betterstorage.api.ILockable").isInstance(container)) {
            try {
                Method getLock = classMap.get("net.mcft.copy.betterstorage.api.ILockable")
                        .getDeclaredMethod("getLock", (Class[]) null);
                Object lock = getLock.invoke(container, (Object[]) null);
                if (lock != null) return false; // Don't pick up if locked
            } catch (Exception e) {
                BetterBarrels.log.warn(
                        "Error checking BetterStorage lock (api.ILockable) for " + container.getClass().getName(),
                        e); // Log
                            // warning
                // System.out.printf("%s \n", e); // Original logging
                return false; // Prevent pickup on error
            }
        }

        // Check TileEntityConnectable (newer location?)
        if (classMap.get("net.mcft.copy.betterstorage.tile.entity.TileEntityConnectable") != null && classMap
                .get("net.mcft.copy.betterstorage.tile.entity.TileEntityConnectable").isInstance(container)) {
            try {
                // Disconnect before picking up
                Method disconnect = classMap.get("net.mcft.copy.betterstorage.tile.entity.TileEntityConnectable")
                        .getDeclaredMethod("disconnect", (Class[]) null);
                disconnect.invoke(container, (Object[]) null);
            } catch (Exception e) {
                BetterBarrels.log.warn(
                        "Error disconnecting BetterStorage connectable (tile.entity) for "
                                + container.getClass().getName(),
                        e); // Log warning
                // System.out.printf("%s \n", e); // Original logging
                return false; // Prevent pickup on error
            }
        }

        // Check TileEntityConnectable (older location?)
        if (classMap.get("net.mcft.copy.betterstorage.block.tileentity.TileEntityConnectable") != null && classMap
                .get("net.mcft.copy.betterstorage.block.tileentity.TileEntityConnectable").isInstance(container)) {
            try {
                // Disconnect before picking up
                Method disconnect = classMap.get("net.mcft.copy.betterstorage.block.tileentity.TileEntityConnectable")
                        .getDeclaredMethod("disconnect", (Class[]) null);
                disconnect.invoke(container, (Object[]) null);
            } catch (Exception e) {
                BetterBarrels.log.warn(
                        "Error disconnecting BetterStorage connectable (block.tileentity) for "
                                + container.getClass().getName(),
                        e); // Log warning
                // System.out.printf("%s \n", e); // Original logging
                return false; // Prevent pickup on error
            }
        }

        return true; // Pickup can proceed
    }

    /**
     * Determines if this specific dolly type can pick up spawners. Overridden by ItemDiamondMover.
     * 
     * @return False for normal dolly.
     */
    protected boolean canPickSpawners() {
        return false;
    }

    /**
     * Picks up a container (Block + TileEntity) from the world and stores it in the dolly.
     * 
     * @param stack  The dolly ItemStack.
     * @param player The player picking up the container.
     * @param world  The world.
     * @param x      X coordinate of the container.
     * @param y      Y coordinate of the container.
     * @param z      Z coordinate of the container.
     * @return True if pickup was successful, false otherwise.
     */
    protected boolean pickupContainer(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity containerTE = world.getTileEntity(x, y, z); // Get the TileEntity
        if (containerTE == null) return false; // No TE at location
        // System.out.println(containerTE.getClass().toString()); // Debugging line from
        // original

        // Get block info
        Block storedBlock = world.getBlock(x, y, z);
        int blockMeta = world.getBlockMetadata(x, y, z);
        NBTTagCompound nbtContainer = new NBTTagCompound(); // To store TE data
        NBTTagCompound nbtTarget = new NBTTagCompound(); // To store in the ItemStack

        // Check if the TE is movable by this dolly
        if (!isTEMovable(containerTE)) return false;

        // Apply BetterStorage specific checks/fixes
        if (!this.pickupBetterStorageFix(containerTE)) return false;

        // Write the TileEntity's data to NBT
        containerTE.writeToNBT(nbtContainer);

        // Store block, meta, class name, and TE NBT in the target tag
        nbtTarget.setString("Block", GameData.getBlockRegistry().getNameForObject(storedBlock)); // Use registry name
        nbtTarget.setInteger("Meta", blockMeta);
        nbtTarget.setString("TEClass", containerTE.getClass().getName());
        nbtTarget.setBoolean("isSpawner", tileIsASpawner(containerTE)); // Flag if it's a spawner
        nbtTarget.setTag("NBT", nbtContainer); // TODO: Check this, seems the nbt classes were streamlined somewhat
                                               // (Comment from original)

        // Check NBT size limit (1MB) to prevent excessive network/storage usage
        if (tagCompoundWrite != null) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                DataOutputStream outStream = new DataOutputStream(byteStream);
                // Use reflection to write NBT to a stream to get its size
                tagCompoundWrite.invoke(nbtTarget, outStream);
                outStream.close();

                if (byteStream.toByteArray().length > 1048576) { // 1MB limit
                    // 1MB limit... MC limits at 2MB, but anything above this really starts to slow
                    // down the game (Comment from original)
                    BarrelPacketHandler.INSTANCE.sendLocalizedChat(player, LocalizedChat.DOLLY_TOO_COMPLEX); // Send
                                                                                                             // warning
                                                                                                             // message
                    return false; // Prevent pickup if too large
                }
            } catch (Throwable t) {
                BetterBarrels.log.warn("Failed to check NBT size before dolly pickup", t); // Log warning
            }
        }

        // Ensure the ItemStack has a tag compound
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        } else if (stack.getTagCompound().hasKey("Container")) {
            stack.getTagCompound().removeTag("Container"); // Remove existing container if present (shouldn't happen)
        }

        // Store the prepared NBT data under the "Container" key in the ItemStack
        stack.getTagCompound().setTag("Container", nbtTarget);

        // Original code to unregister ender barrels (commented out)
        // if (containerTE instanceof TileEntityBarrel) {
        // BSpaceStorageHandler.instance().unregisterEnderBarrel(((TileEntityBarrel)containerTE).id);
        // }

        // Remove the block and TileEntity from the world
        try {
            if (containerTE instanceof TileEntityChest) {
                ((TileEntityChest) containerTE).closeInventory(); // Close chest inventory first
            }
            world.removeTileEntity(x, y, z); // Remove TE
            world.setBlock(x, y, z, Blocks.air, 0, 1 + 2); // Set block to air, update and notify neighbors
        } catch (Exception e) {
            BetterBarrels.log.error("Error removing block/TE after dolly pickup at " + x + "," + y + "," + z, e); // Log
                                                                                                                  // error
            // e.printStackTrace(); // Original handling
            // Potentially revert ItemStack changes if removal fails? Current code doesn't.
        }

        return true; // Pickup successful
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
        if (world.isRemote) {
            return; // Server-side only
        }

        // Apply slowness effects if holding a container
        if ((stack.hasTagCompound()) && stack.getTagCompound().hasKey("Container")
                && (entity instanceof EntityPlayer)) {

            int amplifier = 1; // Default amplifier
            // Check for "amount" tag in the NBT data
            if (stack.getTagCompound().hasKey("amount")) {
                amplifier = Math.min(4, stack.getTagCompound().getInteger("amount") / 2048); // Calculate amplifier
                                                                                             // based on amount
            }

            // Apply mining and movement slowness
            ((EntityPlayer) entity).addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 10, amplifier)); // Short
                                                                                                             // duration,
                                                                                                             // applied
                                                                                                             // every
                                                                                                             // tick
            ((EntityPlayer) entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 10, amplifier)); // Short
                                                                                                              // duration,
                                                                                                              // applied
                                                                                                              // every
                                                                                                              // tick
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List tooltip, boolean p_77624_4_) {
        super.addInformation(itemStack, player, tooltip, p_77624_4_);

        // Add folding hint for normal, empty dollies if folding is enabled
        if (type == DollyType.NORMAL && !BetterBarrels.disableDollyStacking
                && (!itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("Container"))) {
            tooltip.add(StatCollector.translateToLocal("item.dolly.folding_hint.1"));
            tooltip.add(StatCollector.translateToLocal("item.dolly.folding_hint.2"));
        }
    }

    /**
     * Gets the orientation the block should have based on player facing (horizontal only).
     * 
     * @param player The player placing the block.
     * @return The ForgeDirection the block should face.
     */
    private ForgeDirection getBarrelOrientationOnPlacement(EntityPlayer player) {
        return this.getBarrelOrientationOnPlacement(player, 0, false); // Calls the main method with allowVertical=false
    }

    /**
     * Gets the orientation the block should have based on player facing and placement context.
     * 
     * @param player        The player placing the block.
     * @param targY         The Y coordinate of the placement location.
     * @param allowVertical If true, allows UP/DOWN orientation based on player look vector and position relative to
     *                      targY.
     * @return The ForgeDirection the block should face.
     */
    private ForgeDirection getBarrelOrientationOnPlacement(EntityPlayer player, int targY, boolean allowVertical) {

        ForgeDirection barrelOrientation = ForgeDirection.UNKNOWN;
        Vec3 playerLook = player.getLookVec(); // Player's look vector

        // Determine horizontal facing based on dominant look direction (X or Z)
        if (Math.abs(playerLook.xCoord) >= Math.abs(playerLook.zCoord)) { // Looking more East/West
            if (playerLook.xCoord > 0) barrelOrientation = ForgeDirection.WEST; // Player looking positive X -> block
                                                                                // faces West
            else barrelOrientation = ForgeDirection.EAST; // Player looking negative X -> block faces East
        } else { // Looking more North/South
            if (playerLook.zCoord > 0) barrelOrientation = ForgeDirection.NORTH; // Player looking positive Z -> block
                                                                                 // faces North
            else barrelOrientation = ForgeDirection.SOUTH; // Player looking negative Z -> block faces South
        }

        // Check for vertical placement if allowed
        if (allowVertical && player.posY > targY + BetterBarrels.verticalPlacementRange) {
            barrelOrientation = ForgeDirection.UP;
        } else if (allowVertical && playerLook.yCoord > BetterBarrels.verticalPlacementRange) {
            barrelOrientation = ForgeDirection.DOWN;
        }

        return barrelOrientation;
    }

    // Helper method (unused in current code, but present in original) to convert
    // old orientation flags to ForgeDirections
    private ArrayList<ForgeDirection> convertOrientationFlagToForge(int flags) {
        ArrayList<ForgeDirection> directions = new ArrayList<ForgeDirection>();
        // Flags seem to correspond to sides 2-5 (N, S, W, E)
        for (int i = 0; i < 4; i++) if (((1 << i) & flags) != 0) directions.add(ForgeDirection.getOrientation(i + 2));
        return directions;
    }

    // Helper method (unused in current code, but present in original) to convert
    // ForgeDirections back to old orientation flags
    private int convertForgeToOrientationFlag(ArrayList<ForgeDirection> directions) {
        int flags = 0;
        for (ForgeDirection direction : directions) {
            // Assumes directions are only N, S, W, E (ordinal 2-5)
            flags += (1 << (direction.ordinal() - 2));
        }
        return flags;
    }

    /**
     * Attempts to get a display name for the block associated with a TileEntity. Used for logging/debugging purposes
     * 
     * @param tileEntity The TileEntity.
     * @return The display name or "<Unknown>".
     */
    private String getBlockName(TileEntity tileEntity) {
        // Get the block at the TE's location
        Block teBlock = tileEntity.getWorldObj().getBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);

        ItemStack pick = null;
        try {
            // Try to get the pick block ItemStack
            pick = teBlock.getPickBlock(
                    null, // No MovingObjectPosition available here
                    tileEntity.getWorldObj(),
                    tileEntity.xCoord,
                    tileEntity.yCoord,
                    tileEntity.zCoord);
            if (pick != null) return pick.getDisplayName(); // Return its display name
        } catch (Throwable e) {
            BetterBarrels.log.warn("Error getting pick block for TE " + tileEntity.getClass().getName(), e); // Log
                                                                                                             // warning
        }

        return "<Unknown>"; // Fallback name
    }

    private ForgeDirection fromMCToForge(short side) {
        switch (side) {
            case 0:
                return ForgeDirection.DOWN;
            case 1:
                return ForgeDirection.UP;
            case 2:
                return ForgeDirection.EAST;
            case 3:
                return ForgeDirection.WEST;
            case 4:
                return ForgeDirection.NORTH;
            case 5:
                return ForgeDirection.SOUTH;
        }
        return ForgeDirection.UNKNOWN;
    }

    private short fromForgeToMC(ForgeDirection side) {
        switch (side) {
            case DOWN:
                return (short) 0;
            case UP:
                return (short) 1;
            case NORTH:
                return (short) 4;
            case SOUTH:
                return (short) 5;
            case WEST:
                return (short) 3;
            case EAST:
                return (short) 2;
            case UNKNOWN:
                return (short) -1;
        }
        return -1;
    }

    private short fromForgeToBiblio(ForgeDirection side) {
        switch (side) {
            case DOWN:
                return 0;
            case UP:
                return 1;
            case EAST:
                return 2;
            case WEST:
                return 3;
            case NORTH:
                return 4;
            case SOUTH:
                return 5;
            case UNKNOWN:
                return -1;
            default:
                return -1;
        }
    }

}
