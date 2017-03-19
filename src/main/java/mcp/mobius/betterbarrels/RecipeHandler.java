package mcp.mobius.betterbarrels;

import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.registry.GameRegistry;

public class RecipeHandler {
	public static RecipeHandler _instance = new RecipeHandler();
	private RecipeHandler() {}
	public static RecipeHandler instance() { return RecipeHandler._instance; }

	public void registerOres() {
		OreDictionary.registerOre("ingotIron",  Items.iron_ingot);
		OreDictionary.registerOre("ingotGold",  Items.gold_ingot);
		OreDictionary.registerOre("slimeball",  Items.slime_ball);
		OreDictionary.registerOre("gemDiamond", Items.diamond);
		OreDictionary.registerOre("gemEmerald", Items.emerald);
		OreDictionary.registerOre("chestWood",  Blocks.chest);
		OreDictionary.registerOre("stickWood",  Items.stick);
		OreDictionary.registerOre("obsidian",   Blocks.obsidian);
		OreDictionary.registerOre("whiteStone", Blocks.end_stone);
		OreDictionary.registerOre("transdimBlock", Blocks.ender_chest);

		Block CBEnderChest = Block.getBlockFromName("EnderStorage:enderChest");
		if (CBEnderChest != null) {
			for (int meta=0; meta < 4096; meta++) {
				OreDictionary.registerOre("transdimBlock", new ItemStack(CBEnderChest, 1, meta));
			}
		}
	}

	private Object upgradeItem = null;

	public void registerRecipes() {
		// Unique Items
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.blockBarrel), new Object[]
				{"W-W", "WCW", "WWW",
			Character.valueOf('C'), "chestWood",
			Character.valueOf('W'), "logWood",
			Character.valueOf('-'), "slabWood"}));

		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemMover,1,0), new Object[]
				{"  X", " PX", "XXX",
			Character.valueOf('X'), "ingotIron",
			Character.valueOf('P'), "plankWood"}));

		if (BetterBarrels.diamondDollyActive){
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemMoverDiamond,1,0), new Object[]
					{"   ", " P ", "XXX",
				Character.valueOf('X'), "gemDiamond",
				Character.valueOf('P'), BetterBarrels.itemMover}));
		}

		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemHammer, 1, 0), new Object[]
				{"III","ISI", " S ",
			Character.valueOf('I'), "ingotIron",
			Character.valueOf('S'), "stickWood"}));

		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemTuningFork, 1, 0), new Object[]
				{" P "," EP", "P  ",
			Character.valueOf('P'), "ingotIron",
			Character.valueOf('E'), Items.ender_pearl}));

		// Core Upgrades
		addCoreUpgrade(0, BetterBarrels.blockBarrel);
		addCoreUpgrade(1, "transdimBlock");
		addCoreUpgrade(2, Blocks.redstone_block);
		addCoreUpgrade(3, Blocks.hopper);
		addCoreUpgrade(UpgradeCore.VOID.ordinal(), Blocks.obsidian);

		// Side Upgrades
		addSideUpgrade(0, "slimeball", Items.paper);
		addSideUpgrade(1, Blocks.hopper, "plankWood");
		addSideUpgrade(2, Items.redstone, "plankWood");

		// Storage upgrades
		UpgradeCore prevStorage = UpgradeCore.STORAGE;
		for (UpgradeCore core : UpgradeCore.values()) {
			if(core.type == UpgradeCore.Type.STORAGE && core.slotsUsed > 1) {
				if (core.slotsUsed <= StructuralLevel.LEVELS[BetterBarrels.maxCraftableTier].getMaxCoreSlots())
					addCoreUpgradeUpgrade(core.ordinal(), prevStorage.ordinal());
				addCoreUpgradeUpgradeReverse(core.ordinal(), prevStorage.ordinal());
				prevStorage = core;
			}
		}
	}

	public void registerLateRecipes() {
		try {
			Utils.Material mat = new Utils.Material(BetterBarrels.upgradeItemStr);
			upgradeItem = mat.getStack(true);
		} catch (Throwable t) {
			BetterBarrels.log.error("Requested item with id " + BetterBarrels.upgradeItemStr + " for tier upgrade recipes was not found, using the default of vanilla fence");
			upgradeItem = new ItemStack(Blocks.fence);
		}

		for (int i = 0, max = Math.min(StructuralLevel.LEVELS.length-1, BetterBarrels.maxCraftableTier); i < max; i++) {
			this.addStructuralUpgrade(i, StructuralLevel.LEVELS[i+1].material.getStack(true));
		}
	}

	private void addCoreUpgradeUpgrade(int resultMeta, int sourceMeta) {
		GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(BetterBarrels.itemUpgradeCore, 1, resultMeta), new Object[]
				{new ItemStack(BetterBarrels.itemUpgradeCore, 1, sourceMeta),
			new ItemStack(BetterBarrels.itemUpgradeCore, 1, sourceMeta),
			new ItemStack(BetterBarrels.itemUpgradeCore, 1, sourceMeta)}));
	}

	private void addCoreUpgradeUpgradeReverse(int resultMeta, int sourceMeta) {
		GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(BetterBarrels.itemUpgradeCore, 3, sourceMeta), new Object[]
				{new ItemStack(BetterBarrels.itemUpgradeCore, 1, resultMeta)}));
	}

	private void addStructuralUpgrade(int level, Object variableComponent) {
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemUpgradeStructural, 1, level), new Object[]
				{"PBP", "B B", "PBP",
			Character.valueOf('P'), upgradeItem,
			Character.valueOf('B'), variableComponent}));
	}

	private void addCoreUpgrade(int meta, Object variableComponent) {
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemUpgradeCore, 1, meta), new Object[]
				{" P ", " B ", " P ",
			Character.valueOf('P'), Blocks.piston,
			Character.valueOf('B'), variableComponent}));
	}

	private void addSideUpgrade(int meta, Object center, Object border) {
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(BetterBarrels.itemUpgradeSide, 4, meta), new Object[]
				{" P ", "PBP", " P ",
			Character.valueOf('P'), border,
			Character.valueOf('B'), center}));
	}
}
