package mcp.mobius.betterbarrels.client.render;

import java.nio.FloatBuffer;

import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;

/**
 * Inventory item lighting for an item drawn on a barrel face, kept when a shader pack lights the world.
 */
public final class SurfaceItemShader {

    private static final String DOMAIN = "jabba";
    private static final String VERTEX = "shaders/surface_item.vert.glsl";
    private static final String FRAGMENT = "shaders/surface_item.frag.glsl";
    private static final int TEXTURE_UNIT = 0;
    private static final int LIGHTMAP_UNIT = 1;

    // Vanilla GUI item lights (RenderHelper.enableStandardItemLighting, RenderHelper.enableGUIStandardItemLighting).
    private static final double[] LIGHT_0 = { 0.2D, 1.0D, -0.7D };
    private static final double[] LIGHT_1 = { -0.2D, 1.0D, 0.7D };
    private static final double LIGHT_PITCH = 165.0D;
    private static final double LIGHT_YAW = -30.0D;

    private static boolean loaded = false;
    private static SurfaceItemShader instance = null;

    private final int program;
    private final int alphaCutoffLocation;
    private final int faceInverseLocation;
    private final int blockFrameLocation;
    private final int tangentULocation;
    private final int tangentVLocation;
    private final int cornerAoLocation;
    private final int cornerBlockLightLocation;
    private final int cornerSkyLightLocation;

    private SurfaceItemShader(int program) {
        this.program = program;
        this.alphaCutoffLocation = GL20.glGetUniformLocation(program, "u_AlphaCutoff");
        this.faceInverseLocation = GL20.glGetUniformLocation(program, "u_FaceInverse");
        this.blockFrameLocation = GL20.glGetUniformLocation(program, "u_BlockFrame");
        this.tangentULocation = GL20.glGetUniformLocation(program, "u_TangentU");
        this.tangentVLocation = GL20.glGetUniformLocation(program, "u_TangentV");
        this.cornerAoLocation = GL20.glGetUniformLocation(program, "u_CornerAo");
        this.cornerBlockLightLocation = GL20.glGetUniformLocation(program, "u_CornerBlockLight");
        this.cornerSkyLightLocation = GL20.glGetUniformLocation(program, "u_CornerSkyLight");

        final int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "u_Texture"), TEXTURE_UNIT);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "u_Lightmap"), LIGHTMAP_UNIT);
        uniformLight(GL20.glGetUniformLocation(program, "u_Light0"), LIGHT_0);
        uniformLight(GL20.glGetUniformLocation(program, "u_Light1"), LIGHT_1);
        GL20.glUseProgram(previous);
    }

    public static SurfaceItemShader get() {
        if (!loaded) {
            loaded = true;
            final int program = new ShaderProgram(DOMAIN, VERTEX, FRAGMENT).getProgram();
            if (program != 0) {
                instance = new SurfaceItemShader(program);
            }
        }
        return instance;
    }

    public int begin(float alphaCutoff, FloatBuffer faceInverse, FloatBuffer blockFrame, ForgeDirection side,
            float[] ao, float[] blockLight, float[] skyLight) {
        final int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        GL20.glUniform1f(alphaCutoffLocation, alphaCutoff);
        GL20.glUniformMatrix3(faceInverseLocation, false, faceInverse);
        GL20.glUniformMatrix4(blockFrameLocation, false, blockFrame);
        uniformAxis(tangentULocation, BarrelLightingContext.getTangentU(side));
        uniformAxis(tangentVLocation, BarrelLightingContext.getTangentV(side));
        uniformCorners(cornerAoLocation, ao);
        uniformCorners(cornerBlockLightLocation, blockLight);
        uniformCorners(cornerSkyLightLocation, skyLight);
        return previousProgram;
    }

    public void end(int previousProgram) {
        GL20.glUseProgram(previousProgram);
    }

    private static void uniformAxis(int location, ForgeDirection axis) {
        GL20.glUniform3f(location, axis.offsetX, axis.offsetY, axis.offsetZ);
    }

    private static void uniformCorners(int location, float[] corners) {
        GL20.glUniform4f(location, corners[0], corners[1], corners[2], corners[3]);
    }

    private static void uniformLight(int location, double[] light) {
        final double length = Math.sqrt(light[0] * light[0] + light[1] * light[1] + light[2] * light[2]);
        final double x = light[0] / length;
        final double pitchCos = Math.cos(Math.toRadians(LIGHT_PITCH));
        final double pitchSin = Math.sin(Math.toRadians(LIGHT_PITCH));
        final double y = (light[1] * pitchCos - light[2] * pitchSin) / length;
        final double z = (light[1] * pitchSin + light[2] * pitchCos) / length;
        final double yawCos = Math.cos(Math.toRadians(LIGHT_YAW));
        final double yawSin = Math.sin(Math.toRadians(LIGHT_YAW));
        GL20.glUniform3f(location, (float) (x * yawCos + z * yawSin), (float) y, (float) (-x * yawSin + z * yawCos));
    }
}
