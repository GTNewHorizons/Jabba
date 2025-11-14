package mcp.mobius.betterbarrels.common.items.upgrades;

import net.minecraft.item.Item;

import mcp.mobius.betterbarrels.BetterBarrels;

public class UpgradeSide {

    /* UPGRADE VALUES */
    public static final int NONE = 0x0;
    public static final int FRONT = 0x1;
    public static final int STICKER = 0x2;
    public static final int HOPPER = 0x3;
    public static final int REDSTONE = 0x4;

    /* UPGRADE META */
    public static final int RS_FULL = 0x0;
    public static final int RS_EMPT = 0x1;
    public static final int RS_PROP = 0x2;

    public static final int HOPPER_PUSH = 0x0;
    public static final int HOPPER_PULL = 0x1;

    public static Item[] mapItem = { null, null, BetterBarrels.itemUpgradeSide, BetterBarrels.itemUpgradeSide,
            BetterBarrels.itemUpgradeSide, };

    public static int[] mapMeta = { 0, 0, 0, 1, 2 };

    public static int[] mapRevMeta = { STICKER, HOPPER, REDSTONE, };

    public static int[] mapReq = { -1, -1, -1, UpgradeCore.Type.HOPPER.ordinal(), UpgradeCore.Type.REDSTONE.ordinal() };
}
