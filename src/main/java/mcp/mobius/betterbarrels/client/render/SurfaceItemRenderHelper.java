package mcp.mobius.betterbarrels.client.render;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;

/**
 * RenderHelper but no directional light setting
 */
public class SurfaceItemRenderHelper {

    private static FloatBuffer colorBuffer = GLAllocation.createDirectFloatBuffer(16);
    private static FloatBuffer cachedLightSetting = GLAllocation.createDirectFloatBuffer(16);

    public static void disableStandardItemLighting() {
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, cachedLightSetting);
    }

    public static void enableStandardItemLighting() {
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        float f = 0.4F;
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glGetFloat(GL11.GL_LIGHT_MODEL_AMBIENT, cachedLightSetting);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(f, f, f, 1.0F));
    }

    private static FloatBuffer setColorBuffer(float p_74521_0_, float p_74521_1_, float p_74521_2_, float p_74521_3_) {
        colorBuffer.clear();
        colorBuffer.put(p_74521_0_).put(p_74521_1_).put(p_74521_2_).put(p_74521_3_);
        colorBuffer.flip();
        return colorBuffer;
    }
}
