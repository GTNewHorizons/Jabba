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
        dependencies = "after:Waila;after:NotEnoughItems;required-after:gtnhlib;")
public class BetterBarrels {

    private static boolean DEBUG = Boolean.parseBoolean(System.getProperty("mcp.mobius.debugJabba", "false"));

    public static void debug(String msg) {
        if (DEBUG) log.log(Level.WARN, msg);
    }

    public static final String modid = "JABBA";

    public static Logger log = LogManager.getLogger(modid);

    // The instance of your mod that Forge uses.
    @Instance(modid)
    public static BetterBarrels instance;

    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(
            clientSide = "mcp.mobius.betterbarrels.client.ClientProxy",
            serverSide = "mcp.mobius.betterbarrels.common.BaseProxy")
    public static BaseProxy proxy;

    /* CONFIG PARAMS */
    private static Configuration config = null;
    public static boolean disableDollyStacking;
    public static boolean fullBarrelTexture = true;
    public static boolean highRezTexture = true;
    public static boolean showUpgradeSymbols = true;
    public static boolean diamondDollyActive = true;
    public static int stacksSize = 64;
    public static int maxCraftableTier = StructuralLevel.defaultUpgradeMaterialsList.length;
    public static String upgradeItemStr = "minecraft:fence";

    public static Block blockBarrel = null;
    public static Block blockMiniBarrel = null;
    public static Block blockBarrelShelf = null;
    public static Item itemUpgradeStructural = null;
    public static Item itemUpgradeCore = null;
    public static Item itemUpgradeSide = null;
    public static Item itemMover = null;
    public static Item itemMoverDiamond = null;
    public static Item itemTuningFork = null;
    public static Item itemLockingPlanks = null;
    public static Item itemHammer = null;

    public static Item itemFoldedMover = null;
    public static long limiterDelay = 500;

    public static int blockBarrelRendererID = -1;

    public static boolean allowVerticalPlacement = true;
    public static float verticalPlacementRange = 1f;

    public static boolean exposeFullStorageSize = false;
    public static boolean reverseBehaviourClickLeft = false;
    public static boolean allowOreDictUnification = true;

    public static boolean renderStackAndText = false;
    public static float renderDistance = 16F;
    public static String[] BlacklistedTileEntiyClassNames = new String[] {
            "ic2.core.block.machine.tileentity.TileEntityNuke" };
    public static HashSet<Class<? extends TileEntity>> BlacklistedTileEntityClasses = new HashSet<Class<? extends TileEntity>>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());

        try {
            config.load();

            diamondDollyActive = config.get(Configuration.CATEGORY_GENERAL, "diamondDollyActive", true)
                    .getBoolean(true);
            limiterDelay = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "packetLimiterDelay",
                    500,
                    "Controls the minimum delay (in ms) between two server/client sync. Lower values mean closer to realtime, and more network usage.")
                    .getInt();

            String[] materialsList = config
                    .get(
                            Configuration.CATEGORY_GENERAL,
                            "materialList",
                            StructuralLevel.defaultUpgradeMaterialsList,
                            "A structural tier will be created for each material in this list, even if not craftable")
                    .getStringList();
            if (materialsList.length > 18) { // limit max upgrade size to 18 due to internal int storage type on barrel
                String[] trimedList = new String[18];
                for (int i = 0; i < 18; i++) trimedList[i] = materialsList[i];
                materialsList = trimedList;
                config.get(Configuration.CATEGORY_GENERAL, "materialList", trimedList).set(trimedList);
            }
            debug("00 - Loaded materials list: " + Arrays.toString(materialsList));
            StructuralLevel.createLevelArray(materialsList);
            maxCraftableTier = Math.min(
                    18,
                    Math.min(
                            materialsList.length,
                            config.get(
                                    Configuration.CATEGORY_GENERAL,
                                    "maxCraftableTier",
                                    materialsList.length,
                                    "Maximum tier to generate crafting recipes for").getInt()));
            BetterBarrels.debug("01 - Max craftable tier: " + maxCraftableTier);
            proxy.initialiseClientData(
                    config.get(
                            Configuration.CATEGORY_GENERAL,
                            "colorOverrides",
                            new int[] { 0, 0 },
                            "This list contains paired numbers: first is the tier level this color applies to, second is the color. The color value is the RGB color as a single int")
                            .getIntList());

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

            allowVerticalPlacement = config.getBoolean(
                    "allowVerticalPlacement",
                    Configuration.CATEGORY_GENERAL,
                    true,
                    "If true, barrels can be initially placed and dollyed so that their front side can be on the top or bottom. The front side is the side with the initial sticker applied.");
            verticalPlacementRange = config.getFloat(
                    "verticalPlacementRange",
                    Configuration.CATEGORY_GENERAL,
                    0.79f,
                    0f,
                    1f,
                    "This is used when testing a players aim for block placement.  If the aim value is greater than or equal to this setting, it is determined you are attempting to place a block facing down.  The reverse is true for placing blocks facing up. 0 = dead ahead, 1 = directly above.");

            exposeFullStorageSize = config.getBoolean(
                    "exposeFullStorageSize",
                    "experimental",
                    false,
                    "If true, barrels will expose their full contents through the standard MC inventory interfaces. This will allow mods that do not support the DSU to see the full contents of the barrel. *** WARNING *** This will allow mods that do not properly handle inventories to empty out a barrel in one go. Use at your own risk. If you do find such a game breaking mod, please report to that mods' author and ask them to handle inventories better. Otherwise, please enjoy this experimental feature ^_^");

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

            renderDistance = config.getFloat(
                    "renderDistance",
                    Configuration.CATEGORY_GENERAL,
                    10000f,
                    0f,
                    10000f,
                    "Render Distance (square) for stack and text on barrel.");
            renderStackAndText = config.getBoolean("renderStackAndText", Configuration.CATEGORY_GENERAL, true, "");

            // Blacklisted TileEntities for the Dolly
            BlacklistedTileEntiyClassNames = config.getStringList(
                    "BlacklistedTileEntiyClassNames",
                    Configuration.CATEGORY_GENERAL,
                    BlacklistedTileEntiyClassNames,
                    "The Canonical Class-Names of TileEntities that should be ignored when using a Dolly.");

            disableDollyStacking = config.getBoolean(
                    "disableDollyStacking",
                    Configuration.CATEGORY_GENERAL,
                    false,
                    "Disables the ability to collapse and stack the dollies");

            // fullBarrelTexture = config.get(Configuration.CATEGORY_GENERAL, "fullBarrelTexture",
            // true).getBoolean(true);
            // highRezTexture = config.get(Configuration.CATEGORY_GENERAL, "highRezTexture", false).getBoolean(false);
            // showUpgradeSymbols = config.get(Configuration.CATEGORY_GENERAL, "showUpgradeSymbols",
            // false).getBoolean(false);
        } catch (Exception e) {
            FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "BlockBarrel has a problem loading it's configuration");
            FMLLog.severe(e.getMessage());
        } finally {
            if (config.hasChanged()) config.save();
        }

        // Process Dolly Blacklist
        for (String className : BlacklistedTileEntiyClassNames) {
            Class aClass;
            try {
                aClass = Class.forName(className, false, getClass().getClassLoader());
                if (aClass != null && TileEntity.class.isAssignableFrom(aClass)) {
                    Class<? extends TileEntity> aTileClass = aClass;
                    BlacklistedTileEntityClasses.add(aTileClass);
                    log.log(Level.INFO, "Blacklisted " + className + " from Dolly.");
                } else {
                    if (aClass == null) {
                        log.log(Level.INFO, "Class " + className + " is Null.");

                    }
                    if (!TileEntity.class.isAssignableFrom(aClass)) {
                        log.log(Level.INFO, "Class " + className + " does not extend TileEntity.");
                    }
                }
            } catch (ClassNotFoundException e) {
                log.log(Level.INFO, "Did not find " + className + ", unable to blacklist from Dolly.");
            }
        }

        proxy.registerEventHandler();

        // log.setLevel(Level.FINEST);
        blockBarrel = new BlockBarrel();
        itemUpgradeStructural = new ItemUpgradeStructural();
        itemUpgradeCore = new ItemUpgradeCore();
        itemUpgradeSide = new ItemUpgradeSide();
        itemMover = new ItemBarrelMover();
        itemMoverDiamond = new ItemDiamondMover();
        itemHammer = new ItemBarrelHammer();
        itemTuningFork = new ItemTuningFork();
        itemFoldedMover = new ItemFoldedBarrelMover();

        GameRegistry.registerBlock(blockBarrel, "barrel");
        // GameRegistry.registerBlock(blockMiniBarrel);
        // GameRegistry.registerBlock(blockBarrelShelf);
        // GameRegistry.registerTileEntity(TileEntityMiniBarrel.class, "TileEntityMiniBarrel");
        // GameRegistry.registerTileEntity(TileEntityBarrelShelf.class, "TileEntityBarrelShelf");

        GameRegistry.registerItem(itemUpgradeStructural, "upgradeStructural");
        GameRegistry.registerItem(itemUpgradeCore, "upgradeCore");
        GameRegistry.registerItem(itemUpgradeSide, "upgradeSide");
        GameRegistry.registerItem(itemMover, "mover");
        GameRegistry.registerItem(itemFoldedMover, "moverFolded");
        GameRegistry.registerItem(itemMoverDiamond, "moverDiamond");
        GameRegistry.registerItem(itemHammer, "hammer");
        GameRegistry.registerItem(itemTuningFork, "tuningFork");

        BarrelPacketHandler.INSTANCE.ordinal();
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        if (!Loader.isModLoaded("dreamcraft")) {
            RecipeHandler.instance().registerRecipes();
        }
        GameRegistry.registerTileEntity(TileEntityBarrel.class, "TileEntityBarrel");
        FMLCommonHandler.instance().bus().register(ServerTickHandler.INSTANCE);
        proxy.registerRenderers();
        FMLInterModComms.sendMessage("Waila", "register", "mcp.mobius.betterbarrels.BBWailaProvider.callbackRegister");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        RecipeHandler.instance().registerOres();
        if (!Loader.isModLoaded("dreamcraft")) {
            RecipeHandler.instance().registerLateRecipes();
        }
        proxy.postInit();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        BSpaceStorageHandler.instance().writeToFile();
    }
}
