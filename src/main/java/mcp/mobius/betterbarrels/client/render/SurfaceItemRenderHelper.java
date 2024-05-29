package mcp.mobius.betterbarrels.client.render;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;

/**
 * RenderHelper but no directional light setting
 */
public class SurfaceItemRenderHelper {

    private static final FloatBuffer ambientColorBuffer = (FloatBuffer) GLAllocation.createDirectFloatBuffer(16)
            .put(0.4f).put(0.4f).put(0.4f).put(1.0f).flip();
    private static final FloatBuffer cachedAmbientColor = GLAllocation.createDirectFloatBuffer(16);

    public static void disableStandardItemLighting() {
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, cachedAmbientColor);
    }

    public static void enableStandardItemLighting() {
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glGetFloat(GL11.GL_LIGHT_MODEL_AMBIENT, cachedAmbientColor);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, ambientColorBuffer);
    }
}
