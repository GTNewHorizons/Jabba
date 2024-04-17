package mcp.mobius.betterbarrels.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;

public class BlockBarrelRenderer implements ISimpleBlockRenderingHandler {

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;

        IIcon iconSide, iconTop, iconLabel;
        iconSide = StructuralLevel.LEVELS[0].clientData.getIconSide();
        iconTop = StructuralLevel.LEVELS[0].clientData.getIconTop();
        iconLabel = StructuralLevel.LEVELS[0].clientData.getIconLabel();

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, iconTop);
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, iconTop);
        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, iconSide);
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, iconSide);
        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, iconSide);
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, iconLabel);
        tessellator.draw();
    }

    private static int[][] forgeFacingtoMCTopBottomRotate = { { 0, 0, 0, 3, 1, 2, 0 }, { 0, 0, 3, 0, 1, 2, 0 } };

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block tile, int modelId,
            RenderBlocks renderer) {
        TileEntityBarrel barrel = (TileEntityBarrel) world.getTileEntity(x, y, z);

        if (barrel == null) return false;

        renderer.uvRotateBottom = forgeFacingtoMCTopBottomRotate[0][barrel.rotation.ordinal()];
        renderer.uvRotateTop = forgeFacingtoMCTopBottomRotate[1][barrel.rotation.ordinal()];

        barrel.overlaying = false;
        boolean renderedBarrel = renderer.renderStandardBlock(tile, x, y, z);
        barrel.overlaying = true;
        boolean renderedOverlay = renderer.renderStandardBlock(tile, x, y, z);
        barrel.overlaying = false;

        renderer.uvRotateBottom = 0;
        renderer.uvRotateTop = 0;

        return renderedBarrel || renderedOverlay;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelID) {
        return true;
    }

    @Override
    public int getRenderId() {
        return BetterBarrels.blockBarrelRendererID;
    }
}
