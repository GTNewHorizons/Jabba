package mcp.mobius.betterbarrels.client.render;

import net.minecraft.client.renderer.RenderHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Inventory item lighting for an item drawn on a barrel face.
 * Inspired from StorageDrawer's implementation
 */
public class SurfaceItemRenderHelper {

    private static final float FACE_LIGHT_SCALE = 2.6F;
    private static final float FACE_LIGHT_YAW = 171.6F;
    private static final float FACE_LIGHT_PITCH = 84.9F;

    public static void enableStandardItemLighting() {
        GL11.glPushMatrix();
        GL11.glScalef(FACE_LIGHT_SCALE, FACE_LIGHT_SCALE, 1.0F);
        GL11.glRotatef(FACE_LIGHT_YAW, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(FACE_LIGHT_PITCH, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_NORMALIZE);
    }
}
