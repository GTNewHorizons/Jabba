package mcp.mobius.betterbarrels.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Lighting of the blocks surrounding a barrel, as of the last time the barrel was drawn.
 */
public final class BarrelLightingContext {

    public static final int CORNER_COUNT = 4;

    private static final int LIGHT_CHANNEL_MASK = 0xFFFF;
    private static final int SKY_LIGHT_SHIFT = 16;
    private static final float FULL_AO = 1.0F;

    /**
     * Mixed brightness of the barrel and its 26 neighbors, within a 3×3×3 cube centered on the barrel.
     */
    private final int[][][] MBFB = new int[3][3][3];

    /**
     * Ambient occlusion light value of the barrel and its 26 neighbors, within a 3×3×3 cube centered on the barrel.
     */
    private final float[][][] AOLV = new float[3][3][3];

    /**
     * Whether the lighting values must not be used, before the first population and during each population.
     */
    public volatile boolean dirty = true;

    private boolean aoDisabled = true;

    public void populate(IBlockAccess acc, int x, int y, int z, Block blk) {
        dirty = true;
        final int[][][] MBFB = this.MBFB;
        final boolean aoDisabled = Minecraft.getMinecraft().gameSettings.ambientOcclusion == 0;
        this.aoDisabled = aoDisabled;
        if (aoDisabled) {
            MBFB[1][1][1] = blk.getMixedBrightnessForBlock(acc, x, y, z);
            MBFB[0][1][1] = blk.getMixedBrightnessForBlock(acc, x - 1, y, z);
            MBFB[2][1][1] = blk.getMixedBrightnessForBlock(acc, x + 1, y, z);
            MBFB[1][0][1] = blk.getMixedBrightnessForBlock(acc, x, y - 1, z);
            MBFB[1][2][1] = blk.getMixedBrightnessForBlock(acc, x, y + 1, z);
            MBFB[1][1][0] = blk.getMixedBrightnessForBlock(acc, x, y, z - 1);
            MBFB[1][1][2] = blk.getMixedBrightnessForBlock(acc, x, y, z + 1);
        } else {
            final float[][][] AOLV = this.AOLV;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        MBFB[dx + 1][dy + 1][dz + 1] = blk.getMixedBrightnessForBlock(acc, x + dx, y + dy, z + dz);
                        AOLV[dx + 1][dy + 1][dz + 1] = acc.getBlock(x + dx, y + dy, z + dz)
                                .getAmbientOcclusionLightValue();
                    }
                }
            }
        }
        dirty = false;
    }

    public void getFaceCorners(RenderBlocks renderBlocks, ForgeDirection side, float[] ao, float[] blockLight,
            float[] skyLight) {
        final ForgeDirection u = getTangentU(side);
        final ForgeDirection v = getTangentV(side);
        final int fx = 1 + side.offsetX, fy = 1 + side.offsetY, fz = 1 + side.offsetZ;
        final int center = MBFB[fx][fy][fz];

        for (int corner = 0; corner < CORNER_COUNT; corner++) {
            if (aoDisabled) {
                setCorner(corner, FULL_AO, center, ao, blockLight, skyLight);
                continue;
            }

            final int su = (corner & 1) != 0 ? 1 : -1;
            final int sv = (corner & 2) != 0 ? 1 : -1;
            final int ux = fx + su * u.offsetX, uy = fy + su * u.offsetY, uz = fz + su * u.offsetZ;
            final int vx = fx + sv * v.offsetX, vy = fy + sv * v.offsetY, vz = fz + sv * v.offsetZ;
            final int dx = ux + sv * v.offsetX, dy = uy + sv * v.offsetY, dz = uz + sv * v.offsetZ;

            final float cornerAo = (AOLV[fx][fy][fz] + AOLV[ux][uy][uz] + AOLV[vx][vy][vz] + AOLV[dx][dy][dz])
                    / CORNER_COUNT;
            final int cornerBrightness = renderBlocks
                    .getAoBrightness(MBFB[ux][uy][uz], MBFB[vx][vy][vz], MBFB[dx][dy][dz], center);
            setCorner(corner, cornerAo, cornerBrightness, ao, blockLight, skyLight);
        }
    }

    public static ForgeDirection getTangentU(ForgeDirection side) {
        return side.offsetX != 0 ? ForgeDirection.SOUTH : ForgeDirection.EAST;
    }

    public static ForgeDirection getTangentV(ForgeDirection side) {
        return side.offsetY != 0 ? ForgeDirection.SOUTH : ForgeDirection.UP;
    }

    private static void setCorner(int corner, float cornerAo, int brightness, float[] ao, float[] blockLight,
            float[] skyLight) {
        ao[corner] = cornerAo;
        blockLight[corner] = brightness & LIGHT_CHANNEL_MASK;
        skyLight[corner] = brightness >>> SKY_LIGHT_SHIFT & LIGHT_CHANNEL_MASK;
    }
}
