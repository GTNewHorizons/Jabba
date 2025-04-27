package mcp.mobius.betterbarrels;

import java.util.Arrays;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import mcp.mobius.betterbarrels.bspace.BSpaceStorageHandler;
import mcp.mobius.betterbarrels.common.BaseProxy;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.blocks.BlockBarrel;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.common.items.ItemBarrelHammer;
import mcp.mobius.betterbarrels.common.items.ItemTuningFork;
import mcp.mobius.betterbarrels.common.items.dolly.ItemBarrelMover;
import mcp.mobius.betterbarrels.common.items.dolly.ItemDiamondMover;
import mcp.mobius.betterbarrels.common.items.dolly.ItemFoldedBarrelMover;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeCore;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeSide;
import mcp.mobius.betterbarrels.common.items.upgrades.ItemUpgradeStructural;
import mcp.mobius.betterbarrels.network.BarrelPacketHandler;

@Mod(
        modid = BetterBarrels.modid,
        name = BetterBarrels.modid,
        version = "GRADLETOKEN_VERSION",
        dependencies = "after:Waila;after:NotEnoughItems") // Standard mod annotation
public class BetterBarrels {

    // Debug flag, can be enabled via system property
    private static boolean DEBUG = Boolean.parseBoolean(System.getProperty("mcp.mobius.debugJabba", "false"));

    // Debug logging helper
    public static void debug(String msg) {
        if (DEBUG) log.log(Level.WARN, msg);
    }

    // Mod ID and Logger
    public static final String modid = "JABBA";
    public static Logger log = LogManager.getLogger(modid);

    // Mod instance
    @Instance(modid)
    public static BetterBarrels instance;

    // Proxy for client/server specific code
    @SidedProxy(
            clientSide = "mcp.mobius.betterbarrels.client.ClientProxy",
            serverSide = "mcp.mobius.betterbarrels.common.BaseProxy")
    public static BaseProxy proxy;

    /* CONFIG PARAMS */
    private static Configuration config = null; // Configuration object
    // Configurable options with defaults
    public static boolean disableDollyStacking; // Disables folding/stacking of normal dollies
    // public static boolean fullBarrelTexture = true; // Removed in modified version (Present in original)
    // public static boolean highRezTexture = true; // Removed in modified version (Present in original)
    // public static boolean showUpgradeSymbols = true; // Removed in modified version (Present in original)
    public static boolean diamondDollyActive = true; // Enables the diamond dolly
    public static int stacksSize = 64; // Base stack capacity per tier
    public static int maxCraftableTier = StructuralLevel.defaultUpgradeMaterialsList.length; // Max tier with recipes
    public static String upgradeItemStr = "minecraft:fence"; // Item used for structural upgrades

    // Blocks
    public static Block blockBarrel = null;
    // public static Block blockMiniBarrel = null; // Removed in modified version (Present in original)
    // public static Block blockBarrelShelf = null; // Removed in modified version (Present in original)
    // Items
    public static Item itemUpgradeStructural = null;
    public static Item itemUpgradeCore = null;
    public static Item itemUpgradeSide = null;
    public static Item itemMover = null; // Normal Dolly
    public static Item itemMoverDiamond = null; // Diamond Dolly
    public static Item itemTuningFork = null; // For BSpace linking
    // public static Item itemLockingPlanks = null; // Removed in modified version (Present in original)
    public static Item itemHammer = null; // For configuration
    public static Item itemFoldedMover = null; // Stackable folded dolly

    // Network/Misc Config
    public static long limiterDelay = 500; // Packet limiter delay (ms)
    public static int blockBarrelRendererID = -1; // Renderer ID for barrel block
    public static boolean allowVerticalPlacement = true; // Allow placing barrels facing up/down
    public static float verticalPlacementRange = 0.79f; // Threshold for detecting vertical placement aim

    // Experimental/Behavior Config
    public static boolean exposeFullStorageSize = false; // Expose full inventory size (experimental)
    public static boolean reverseBehaviourClickLeft = false; // Reverse left/shift-left click behavior
    public static boolean allowOreDictUnification = true; // Unify common ore dictionary items in barrels
    public static boolean renderStackAndText = false; // Render item count/text on barrels (client-side)
    public static float renderDistance = 10000f; // Max squared distance for rendering stack/text

    // Dolly Blacklist/Whitelist Config
    public static String[] BlacklistedTileEntiyClassNames = new String[] {
            "ic2.core.block.machine.tileentity.TileEntityNuke" }; // Default blacklist
    public static HashSet<Class<? extends TileEntity>> BlacklistedTileEntityClasses = new HashSet<Class<? extends TileEntity>>(); // Loaded
                                                                                                                                  // blacklist
                                                                                                                                  // classes
    public static String[] extraDollyMovableTileEntityClassNames = new String[0]; // Additional movable TEs from config

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile()); // Load config file

        try {
            config.load();

            // Load general settings
            diamondDollyActive = config.get(Configuration.CATEGORY_GENERAL, "diamondDollyActive", true)
                    .getBoolean(true);
            limiterDelay = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "packetLimiterDelay",
                    500,
                    "Controls the minimum delay (in ms) between two server/client sync. Lower values mean closer to realtime, and more network usage.")
                    .getInt();

            // Load structural upgrade materials
            String[] materialsList = config
                    .get(
                            Configuration.CATEGORY_GENERAL,
                            "materialList",
                            StructuralLevel.defaultUpgradeMaterialsList,
                            "A structural tier will be created for each material in this list, even if not craftable")
                    .getStringList();
            if (materialsList.length > 18) { // Limit max upgrade size to 18 due to internal storage limits
                String[] trimedList = new String[18];
                for (int i = 0; i < 18; i++) trimedList[i] = materialsList[i];
                materialsList = trimedList;
                config.get(Configuration.CATEGORY_GENERAL, "materialList", trimedList).set(trimedList); // Save trimmed
                                                                                                        // list back to
                                                                                                        // config
            }
            debug("00 - Loaded materials list: " + Arrays.toString(materialsList));
            StructuralLevel.createLevelArray(materialsList); // Initialize structural levels
            maxCraftableTier = Math.min(
                    18, // Hard limit
                    Math.min(
                            materialsList.length, // Limit by available materials
                            config.get(
                                    Configuration.CATEGORY_GENERAL,
                                    "maxCraftableTier",
                                    materialsList.length,
                                    "Maximum tier to generate crafting recipes for").getInt())); // Configurable limit
            BetterBarrels.debug("01 - Max craftable tier: " + maxCraftableTier);
            // Load client-side color overrides
            proxy.initialiseClientData(
                    config.get(
                            Configuration.CATEGORY_GENERAL,
                            "colorOverrides",
                            new int[] { 0, 0 },
                            "This list contains paired numbers: first is the tier level this color applies to, second is the color. The color value is the RGB color as a single int")
                            .getIntList());

            // Load capacity and recipe settings
            stacksSize = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "stacksSize",
                    BetterBarrels.stacksSize,
                    "How many stacks the base barrel and each upgrade will provide").getInt();
            upgradeItemStr = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "tierUpgradeItem",
                    BetterBarrels.upgradeItemStr,
                    "The name of the item to use for the strutural tier upgrade recipes. Default is \"minecraft:fence\" for Vanilla Fence. The format is Ore.name for an ore dictionary lookup, or itemDomain:itemname[:meta] for a direct item, not this is case-sensitive.")
                    .getString();

            // Load placement settings
            allowVerticalPlacement = config.getBoolean(
                    "allowVerticalPlacement",
                    Configuration.CATEGORY_GENERAL,
                    true,
                    "If true, barrels can be initially placed and dollyed so that their front side can be on the top or bottom. The front side is the side with the initial sticker applied.");
            verticalPlacementRange = config.getFloat(
                    "verticalPlacementRange",
                    Configuration.CATEGORY_GENERAL,
                    0.79f, // Default value
                    0f,
                    1f, // Range
                    "This is used when testing a players aim for block placement.  If the aim value is greater than or equal to this setting, it is determined you are attempting to place a block facing down.  The reverse is true for placing blocks facing up. 0 = dead ahead, 1 = directly above.");

            // Load experimental settings
            exposeFullStorageSize = config.getBoolean(
                    "exposeFullStorageSize",
                    "experimental",
                    false,
                    "If true, barrels will expose their full contents through the standard MC inventory interfaces. This will allow mods that do not support the DSU to see the full contents of the barrel. *** WARNING *** This will allow mods that do not properly handle inventories to empty out a barrel in one go. Use at your own risk. If you do find such a game breaking mod, please report to that mods' author and ask them to handle inventories better. Otherwise, please enjoy this experimental feature ^_^");

            // Load behavior settings
            reverseBehaviourClickLeft = config.getBoolean(
                    "reverseBehaviourClickLeft",
                    Configuration.CATEGORY_GENERAL,
                    false,
                    "If true, punching a barrel will remove one item and shift punching a stack.");
            allowOreDictUnification = config.getBoolean(
                    "allowOreDictUnification",
                    Configuration.CATEGORY_GENERAL,
                    true,
                    "If true, Jabba will try unificate 'ingot' 'ore' 'dust' and 'nugget' using oredict");

            // Load rendering settings
            renderDistance = config.getFloat(
                    "renderDistance",
                    Configuration.CATEGORY_GENERAL,
                    10000f, // Default (effectively infinite?)
                    0f,
                    10000f,
                    "Render Distance (square) for stack and text on barrel.");
            renderStackAndText = config.getBoolean(
                    "renderStackAndText",
                    Configuration.CATEGORY_GENERAL,
                    true,
                    "Enable/disable rendering of item count and text on barrel front."); // Added comment clarification

            // Load Dolly blacklist
            BlacklistedTileEntiyClassNames = config.getStringList(
                    "BlacklistedTileEntiyClassNames",
                    Configuration.CATEGORY_GENERAL,
                    BlacklistedTileEntiyClassNames,
                    "The Canonical Class-Names of TileEntities that should be ignored when using a Dolly.");

            // Load Dolly extra movable list (whitelist)
            extraDollyMovableTileEntityClassNames = config.getStringList(
                    "ExtraDollyMovableTileEntities",
                    Configuration.CATEGORY_GENERAL,
                    extraDollyMovableTileEntityClassNames, // Default is empty array
                    "A list of additional canonical TileEntity class names that the Dolly should be able to move (use with caution).");

            // Load Dolly stacking option
            disableDollyStacking = config.getBoolean(
                    "disableDollyStacking",
                    Configuration.CATEGORY_GENERAL,
                    false,
                    "Disables the ability to collapse and stack the dollies");

            // Removed config options (were present in original)
            // fullBarrelTexture = config.get(Configuration.CATEGORY_GENERAL, "fullBarrelTexture",
            // true).getBoolean(true);
            // highRezTexture = config.get(Configuration.CATEGORY_GENERAL, "highRezTexture", false).getBoolean(false);
            // showUpgradeSymbols = config.get(Configuration.CATEGORY_GENERAL, "showUpgradeSymbols",
            // false).getBoolean(false);

        } catch (Exception e) {
            FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "JABBA has a problem loading its configuration"); // Updated
                                                                                                                  // mod
                                                                                                                  // name
            // FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "BlockBarrel has a problem loading it's
            // configuration"); // Original message
            // FMLLog.severe(e.getMessage()); // Redundant with logging exception
        } finally {
            if (config.hasChanged()) config.save(); // Save config if changes were made (e.g., list trimming)
        }

        // Process Dolly Blacklist
        for (String className : BlacklistedTileEntiyClassNames) {
            if (className == null || className.trim().isEmpty()) continue; // Skip empty entries
            Class aClass;
            try {
                aClass = Class.forName(className.trim(), false, getClass().getClassLoader()); // Load class
                // Check if it's a valid TileEntity subclass
                if (aClass != null && TileEntity.class.isAssignableFrom(aClass)) {
                    @SuppressWarnings("unchecked") // Cast is safe due to isAssignableFrom check
                    Class<? extends TileEntity> aTileClass = aClass;
                    BlacklistedTileEntityClasses.add(aTileClass); // Add class to the blacklist set
                    log.log(Level.INFO, "Blacklisted " + className.trim() + " from Dolly."); // Log success
                } else {
                    // Log reasons for not blacklisting
                    if (aClass == null) { // Should not happen if Class.forName succeeded? Defensive check.
                        log.log(Level.WARN, "Class " + className.trim() + " is Null after loading? Cannot blacklist.");
                    } else { // Not a subclass of TileEntity
                        log.log(
                                Level.WARN,
                                "Class " + className.trim() + " does not extend TileEntity. Cannot blacklist.");
                    }
                }
            } catch (ClassNotFoundException e) {
                log.log(Level.WARN, "Did not find class " + className.trim() + ", unable to blacklist from Dolly."); // Log
                                                                                                                     // class
                                                                                                                     // not
                                                                                                                     // found
            } catch (Throwable t) { // Catch other potential errors during class loading
                log.log(Level.ERROR, "Error processing blacklist entry " + className.trim() + " for Dolly.", t);
            }
        }

        // Register event handlers via proxy
        proxy.registerEventHandler();

        // Instantiate Blocks and Items
        blockBarrel = new BlockBarrel();
        itemUpgradeStructural = new ItemUpgradeStructural();
        itemUpgradeCore = new ItemUpgradeCore();
        itemUpgradeSide = new ItemUpgradeSide();
        itemMover = new ItemBarrelMover();
        itemMoverDiamond = new ItemDiamondMover();
        itemHammer = new ItemBarrelHammer();
        itemTuningFork = new ItemTuningFork();
        itemFoldedMover = new ItemFoldedBarrelMover();

        // Register Blocks
        GameRegistry.registerBlock(blockBarrel, "barrel");
        // GameRegistry.registerBlock(blockMiniBarrel); // Removed (Commented out in original)
        // GameRegistry.registerBlock(blockBarrelShelf); // Removed (Commented out in original)
        // Register Tile Entities (moved to init phase)
        // GameRegistry.registerTileEntity(TileEntityMiniBarrel.class, "TileEntityMiniBarrel"); // Removed (Commented
        // out in original)
        // GameRegistry.registerTileEntity(TileEntityBarrelShelf.class, "TileEntityBarrelShelf"); // Removed (Commented
        // out in original)

        // Register Items
        GameRegistry.registerItem(itemUpgradeStructural, "upgradeStructural");
        GameRegistry.registerItem(itemUpgradeCore, "upgradeCore");
        GameRegistry.registerItem(itemUpgradeSide, "upgradeSide");
        GameRegistry.registerItem(itemMover, "mover");
        GameRegistry.registerItem(itemFoldedMover, "moverFolded");
        GameRegistry.registerItem(itemMoverDiamond, "moverDiamond");
        GameRegistry.registerItem(itemHammer, "hammer");
        GameRegistry.registerItem(itemTuningFork, "tuningFork");

        // Initialize Packet Handler
        BarrelPacketHandler.INSTANCE.ordinal(); // Ensures the packet handler class is loaded and registered
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        // Register recipes unless integration mod is loaded
        if (!Loader.isModLoaded("dreamcraft")) {
            RecipeHandler.instance().registerRecipes();
        }
        // Register Tile Entities in init phase
        GameRegistry.registerTileEntity(TileEntityBarrel.class, "TileEntityBarrel");
        // Register server tick handler
        FMLCommonHandler.instance().bus().register(ServerTickHandler.INSTANCE);
        // Register renderers via proxy
        proxy.registerRenderers();
        // Register Waila provider via IMC message
        FMLInterModComms.sendMessage("Waila", "register", "mcp.mobius.betterbarrels.BBWailaProvider.callbackRegister");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Register ore dictionary entries
        RecipeHandler.instance().registerOres();
        // Register late recipes (dependant on ore dictionary)
        if (!Loader.isModLoaded("dreamcraft")) {
            RecipeHandler.instance().registerLateRecipes();
        }

        // Initialize the extra movable classes for the Dolly after config is loaded
        ItemBarrelMover.initializeExtraMovableClasses();

        // Post-init proxy tasks (e.g., client-side setup)
        proxy.postInit();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // Save BSpace data when server stops
        BSpaceStorageHandler.instance().writeToFile();
    }
}
