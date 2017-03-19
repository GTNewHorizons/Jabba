package mcp.mobius.betterbarrels.client.render;

import mcp.mobius.betterbarrels.common.blocks.logic.Coordinates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

public abstract class TileEntityBaseRenderer extends TileEntitySpecialRenderer {
	protected float scale = 1f/256f;
	protected RenderBlocks renderBlocks = new RenderBlocks();
	protected RenderItem   renderItem   = new RenderItem();

	protected Minecraft      mc            = Minecraft.getMinecraft();
	protected TextureManager texManager    = mc.renderEngine;
	protected FontRenderer   renderFont    = mc.fontRenderer;

	protected static ResourceLocation itemsSheetRes    = new ResourceLocation("jabba", "textures/sheets/items.png");
	//protected static ResourceLocation blocksSheetRes   = new ResourceLocation("jabba", "textures/sheets/blocks.png");
	//protected static ResourceLocation blocks32SheetRes = new ResourceLocation("jabba", "textures/sheets/blocks32.png");
	//protected static ResourceLocation blankRes         = new ResourceLocation("jabba", "blank.png");

	protected int   boundTexIndex;

	protected static byte ALIGNLEFT = 0x00;
	protected static byte ALIGNCENTER = 0x01;
	protected static byte ALIGNRIGHT = 0x02;

	protected void setLight(TileEntity tileEntity, ForgeDirection side){
		int ambientLight = tileEntity.getWorldObj().getLightBrightnessForSkyBlocks(tileEntity.xCoord + side.offsetX, tileEntity.yCoord + side.offsetY, tileEntity.zCoord + side.offsetZ, 0);
		int var6 = ambientLight % 65536;
		int var7 = ambientLight / 65536;
		float var8 = 1.0F;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, var6 * var8, var7 * var8);
	}

	protected void renderTextOnBlock(String renderString, ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy, int red, int green, int blue, int alpha, byte align){
		int color = (alpha << 24) | (red << 16) | (blue << 8) | green;
		this.renderTextOnBlock(renderString, side, orientation, barrelPos, size, posx, posy, color, align);
	}

	protected void renderTextOnBlock(String renderString, ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy, int color, byte align) {
		this.renderTextOnBlock(renderString, side, orientation, barrelPos, size, posx, posy, 0F, color, align);
	}

	protected void renderTextOnBlock(String renderString, ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy, float angle, int color, byte align){
		if (renderString == null || renderString.equals("")) { return; }

		int stringWidth = this.func_147498_b().getStringWidth(renderString);

		GL11.glPushMatrix();

		this.alignRendering(side, orientation, barrelPos);
		this.moveRendering(size, posx, posy, -0.001);

		GL11.glRotatef(angle, 0.0f, 0.0f, 1.0f);

		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_LIGHTING);

		switch (align){
			case 0:
				this.func_147498_b().drawString(renderString, 0, 0, color);
				break;
			case 1:
				this.func_147498_b().drawString(renderString, -stringWidth / 2, 0, color);
				break;
			case 2:
				this.func_147498_b().drawString(renderString, -stringWidth, 0, color);
				break;
		}

		GL11.glDepthMask(true);
		GL11.glPopMatrix();
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
	}

	protected void renderStackOnBlock(ItemStack stack, ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy){
		if (stack == null) { return; }

		int[][] savedGLState =saveGLState(new int[]{ GL11.GL_ALPHA_TEST, GL11.GL_LIGHTING });
		GL11.glPushMatrix();

		this.alignRendering(side, orientation, barrelPos);
		this.moveRendering(size, posx, posy, -0.001);

		if (!ForgeHooksClient.renderInventoryItem(this.renderBlocks, this.texManager, stack, true, 0.0F, 0.0F, 0.0F)) {
			this.renderItem.renderItemIntoGUI(this.renderFont, this.texManager, stack, 0, 0);
		}

		GL11.glPopMatrix();
		restoreGlState(savedGLState);
	}

	protected void renderIconOnBlock(IIcon icon, int sheet,  ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy, double zdepth){
		if (icon == null) { return ; }

		int[][] savedGLState = modifyGLState(new int[]{ GL11.GL_LIGHTING }, new int[]{ GL11.GL_ALPHA_TEST });
		GL11.glPushMatrix();

		this.alignRendering(side, orientation, barrelPos);
		this.moveRendering(size, posx, posy, zdepth);

		this.texManager.bindTexture(sheet == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
		this.drawIcon(0, 0, icon, side);

		GL11.glPopMatrix();
		restoreGlState(savedGLState);
	}

	protected void renderIconOnBlock(int index,  ForgeDirection side, ForgeDirection orientation, Coordinates barrelPos, float size, double posx, double posy, double zdepth){
		GL11.glPushMatrix();

		this.alignRendering(side, orientation, barrelPos);
		this.moveRendering(size/2, posx, posy, zdepth);

		this.texManager.bindTexture(itemsSheetRes);
		this.drawTexturedModalRect(0, 0, 32*(index%16), 32*(index/16), 32, 32);

		GL11.glPopMatrix();
	}

	protected void alignRendering(ForgeDirection side, ForgeDirection orientation, Coordinates position){
		GL11.glTranslated(position.x + 0.5F, position.y + 0.5F, position.z + 0.5F);     // We align the rendering on the center of the block
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(this.getRotationYForSide(side, orientation), 0.0F, 1.0F, 0.0F); // We rotate it so it face the right face
		GL11.glRotatef(this.getRotationXForSide(side), 1.0F, 0.0F, 0.0F);
		GL11.glTranslated(-0.5F, -0.5F, -0.5f);
	}

	protected void moveRendering(float size, double posX, double posY, double posz){
		GL11.glTranslated(0, 0, posz);
		GL11.glScalef(scale, scale, -0.0001f);			  // We flatten the rendering and scale it to the right size
		GL11.glTranslated(posX, posY, 0);		  // Finally, we translate the icon itself to the correct position
		GL11.glScalef(size, size, 1.0f);
	}

	static final int orientRotation[] = {0,0,0,2,3,1,0};
	protected float getRotationYForSide(ForgeDirection side, ForgeDirection orientation){
		int sideRotation[] = {orientRotation[orientation.ordinal()],orientRotation[orientation.ordinal()],0,2,3,1};
		return sideRotation[side.ordinal()] * 90F;
	}

	static final int sideRotation[] = {1,3,0,0,0,0};
	protected float getRotationXForSide(ForgeDirection side){
		return sideRotation[side.ordinal()] * 90F;
	}

	protected void drawIcon(int posX, int posY, IIcon icon, ForgeDirection side) {
		float minU = icon.getMinU();
		float minV = icon.getMinV();
		float maxU = icon.getMaxU();
		float maxV = icon.getMaxV();
		int sizeX = 16;//icon.getIconWidth();
		int sizeY = 16;//icon.getIconHeight();
		Tessellator var9 = Tessellator.instance;
		var9.startDrawingQuads();
		//var9.setNormal(-side.offsetX, -side.offsetY, -side.offsetZ);
		var9.addVertexWithUV(posX + 0,     posY + sizeY, 0, minU, maxV);
		var9.addVertexWithUV(posX + sizeX, posY + sizeY, 0, maxU, maxV);
		var9.addVertexWithUV(posX + sizeX, posY + 0,     0, maxU, minV);
		var9.addVertexWithUV(posX + 0,     posY + 0,     0, minU, minV);
		var9.draw();
	}

	protected void drawTexturedModalRect(int posX, int posY, int textureX, int textureY, int sizeX, int sizeY) {
		float scaleX = 0.00390625F;
		float scaleY = 0.00390625F;
		float zLevel = 0.0F;
		Tessellator var9 = Tessellator.instance;
		var9.startDrawingQuads();
		var9.addVertexWithUV(posX + 0,     posY + sizeY, zLevel, (textureX + 0) * scaleX,     (textureY + sizeY) * scaleY);
		var9.addVertexWithUV(posX + sizeX, posY + sizeY, zLevel, (textureX + sizeX) * scaleX, (textureY + sizeY) * scaleY);
		var9.addVertexWithUV(posX + sizeX, posY + 0,     zLevel, (textureX + sizeX) * scaleX, (textureY + 0) * scaleY);
		var9.addVertexWithUV(posX + 0,     posY + 0,     zLevel, (textureX + 0) * scaleX,     (textureY + 0) * scaleY);
		var9.draw();
	}

	protected void saveBoundTexture(){
		boundTexIndex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
	}

	protected void loadBoundTexture(){
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTexIndex);
	}

	protected int[][] saveGLState(int[] bitsToSave) {
		if (bitsToSave == null) { return null; }

		int[][] savedGLState = new int[bitsToSave.length][2];
		int count = 0;

		for (int glBit : bitsToSave) {
			savedGLState[count][0] = glBit;
			savedGLState[count++][1] = GL11.glIsEnabled(glBit) ? 1: 0;
		}
		return savedGLState;
	}

	protected int[][] modifyGLState(int[] bitsToDisable, int[] bitsToEnable) {
		return modifyGLState(bitsToDisable, bitsToEnable, null);
	}

	protected int[][] modifyGLState(int[] bitsToDisable, int[] bitsToEnable, int[] bitsToSave) {
		if (bitsToDisable == null && bitsToEnable == null && bitsToSave == null) { return null; }

		int[][] savedGLState = new int[(bitsToDisable != null ? bitsToDisable.length: 0) + (bitsToEnable != null ? bitsToEnable.length: 0) + (bitsToSave != null ? bitsToSave.length: 0)][2];
		int count = 0;

		if (bitsToDisable != null) {
			for (int glBit : bitsToDisable) {
				savedGLState[count][0] = glBit;
				savedGLState[count++][1] = GL11.glIsEnabled(glBit) ? 1: 0;
				GL11.glDisable(glBit);
			}
		}
		if (bitsToEnable != null) {
			for (int glBit : bitsToEnable) {
				savedGLState[count][0] = glBit;
				savedGLState[count++][1] = GL11.glIsEnabled(glBit) ? 1: 0;
				GL11.glEnable(glBit);
			}
		}
		if (bitsToSave != null) {
			for (int glBit : bitsToSave) {
				savedGLState[count][0] = glBit;
				savedGLState[count++][1] = GL11.glIsEnabled(glBit) ? 1: 0;
			}
		}

		return savedGLState;
	}

	protected void restoreGlState(int[][] savedGLState) {
		if (savedGLState == null) { return; }

		for(int[] glBit : savedGLState) {
			if (glBit[1] == 1)
				GL11.glEnable(glBit[0]);
			else
				GL11.glDisable(glBit[0]);
		}
	}
}
