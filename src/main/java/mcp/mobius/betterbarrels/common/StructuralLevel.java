package mcp.mobius.betterbarrels.common;

import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;

public class StructuralLevel {

    public static final String[] defaultUpgradeMaterialsList = new String[] { "Ore.plankWood", "Ore.ingotIron",
            "Ore.ingotGold", "Ore.gemDiamond", "Ore.obsidian", "Ore.whiteStone", "Ore.gemEmerald" };
    public static StructuralLevel[] LEVELS;

    public Utils.Material material;
    private int maxCoreSlots;
    public int levelNum;

    @SideOnly(Side.CLIENT)
    public mcp.mobius.betterbarrels.client.StructuralLevelClientData clientData;

    private StructuralLevel(String materialin, final int level) {
        this.levelNum = level;
        this.maxCoreSlots = 0;

        if (level > 0) {
            this.material = new Utils.Material(materialin);

            for (int i = 0; i < level; i++) this.maxCoreSlots += MathHelper.floor_double(Math.pow(2, i));

            BetterBarrels.debug(
                    "03 - Created structural entry for ["
                            + (this.material.isOreDict() ? this.material.name
                                    : (this.material.modDomain + ":" + this.material.name + ":" + this.material.meta))
                            + "] with "
                            + this.maxCoreSlots
                            + " slots.");
        }
    }

    public static void createLevelArray(String[] matsList) {
        LEVELS = new StructuralLevel[Math.min(18, matsList.length) + 1];
        BetterBarrels.debug("02 - Creating materials array of length " + LEVELS.length);
        LEVELS[0] = new StructuralLevel(null, 0);
        for (int i = 1; i < LEVELS.length; i++) {
            LEVELS[i] = new StructuralLevel(matsList[i - 1], i);
        }
    }

    public int getMaxCoreSlots() {
        return this.maxCoreSlots;
    }
}
