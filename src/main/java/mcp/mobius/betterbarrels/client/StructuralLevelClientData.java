package mcp.mobius.betterbarrels.client;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.registry.LanguageRegistry;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.common.StructuralLevel;

public class StructuralLevelClientData {

    private AccessibleTextureAtlasSprite iconBlockSide;
    private AccessibleTextureAtlasSprite iconBlockLabel;
    private AccessibleTextureAtlasSprite iconBlockTop;
    private AccessibleTextureAtlasSprite iconBlockTopLabel;
    private AccessibleTextureAtlasSprite iconItem;
    private ItemStack materialStack;
    private StructuralLevel level;
    private int textColor = 0xFFFFFFFF;
    private String name;
    private int colorOverride = -1;

    public StructuralLevelClientData(StructuralLevel inlevel) {
        this.level = inlevel;
    }

    public IIcon getIconSide() {
        return this.iconBlockSide;
    }

    public IIcon getIconTop() {
        return this.iconBlockTop;
    }

    public IIcon getIconLabel() {
        return this.iconBlockLabel;
    }

    public IIcon getIconLabelTop() {
        return this.iconBlockTopLabel;
    }

    public IIcon getIconItem() {
        return this.iconItem;
    }

    public int getTextColor() {
        return this.textColor;
    }

    public String getMaterialName() {
        return this.name;
    }

    public void setColorOverride(int override) {
        this.colorOverride = override;
    }

    public void cacheStackAndName() {
        BetterBarrels.debug(
                "15 - Looking up user friendly name for " + (level.material.isOreDict() ? level.material.name
                        : (level.material.modDomain + ":" + level.material.name + ":" + level.material.meta)));
        materialStack = level.material.getStack();
        name = materialStack.getDisplayName();

        if (name.indexOf(".name") > 0) {
            name = LanguageRegistry.instance().getStringLocalization(name);
        }
        BetterBarrels.debug("16 - Found: " + name);
    }

    // Begin the crazy icon stuff
    /*
     * Basic process: register dummy icons, this is to get entries and values(position offsets, etc..) into the texture
     * sheet use opengl functions to access the texture sheet and read the base pieces manipulate as desired use opengl
     * to replace modified array into original texture sheet since or registered Icons just store offsets to the texture
     * sheet End result: final icon used is dynamically generated at runtime, at every resource manager reload
     */
    private static class BaseTextures {

        public int[] labelBackground;
        public int[] labelBorder;
        public int[] topBackground;
        public int[] topBorder;
        public int[] topLabel;
        public int[] sideBackground;
        public int[] sideBorder;
        public int[] item;
        public int[] itemArrow;
    }

    private static BaseTextures baseTexturePixels;

    public static void loadBaseTextureData() {
        BetterBarrels.debug("08 - Pre-loading component texture data.");
        StructuralLevelClientData.baseTexturePixels = new BaseTextures();

        StructuralLevelClientData.baseTexturePixels.labelBorder = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_label_border");
        StructuralLevelClientData.baseTexturePixels.labelBackground = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_label_background");
        StructuralLevelClientData.baseTexturePixels.topBorder = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_top_border");
        StructuralLevelClientData.baseTexturePixels.topBackground = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_top_background");
        StructuralLevelClientData.baseTexturePixels.topLabel = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_top_label");
        StructuralLevelClientData.baseTexturePixels.sideBorder = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_side_border");
        StructuralLevelClientData.baseTexturePixels.sideBackground = getPixelsForTexture(
                false,
                BetterBarrels.modid + ":barrel_side_background");
        StructuralLevelClientData.baseTexturePixels.item = getPixelsForTexture(
                true,
                BetterBarrels.modid + ":capaupg_base");
        StructuralLevelClientData.baseTexturePixels.itemArrow = getPixelsForTexture(
                true,
                BetterBarrels.modid + ":capaupg_color");
    }

    public static void unloadBaseTextureData() {
        BetterBarrels.debug("39 - Unloading preloaded texture data");
        StructuralLevelClientData.baseTexturePixels = null;
    }

    private static class AccessibleTextureAtlasSprite extends TextureAtlasSprite {

        protected int textureType;

        AccessibleTextureAtlasSprite(String par1Str, int textype) {
            super(par1Str);
            textureType = textype;
        }

        private static Method fixPixels = Utils.ReflectionHelper.getMethod(
                TextureAtlasSprite.class,
                new String[] { "a", "func_147961_a", "fixTransparentPixels" },
                new Class[] { int[][].class },
                Level.ERROR,
                "Unable to locate required method 'fixTransparentPixels' for texture generation.  Please post this error at the error tracker along with a copy of your ForgeModLoader-client-0.log.");
        private static Method setupAnisotropic = Utils.ReflectionHelper.getMethod(
                TextureAtlasSprite.class,
                new String[] { "a", "func_147960_a", "prepareAnisotropicFiltering" },
                new Class[] { int[][].class, int.class, int.class },
                Level.ERROR,
                "Unable to locate required method 'prepareAnisotropicFiltering' for texture generation.  Please post this error at the error tracker along with a copy of your ForgeModLoader-client-0.log.");
        private static Field useAnisotropic = Utils.ReflectionHelper.getField(
                TextureAtlasSprite.class,
                new String[] { "k", "field_147966_k", "useAnisotropicFiltering" },
                Level.ERROR,
                "Unable to locate required field 'useAnisotropicFiltering' for texture generation.  Please post this error at the error tracker along with a copy of your ForgeModLoader-client-0.log.");
        private static Field texmapMipMapLevels = Utils.ReflectionHelper
                .getField(TextureMap.class, new String[] { "j", "field_147636_j", "mipmapLevels" });
        private static Field texmapAnisotropic = Utils.ReflectionHelper
                .getField(TextureMap.class, new String[] { "k", "field_147637_k", "anisotropicFiltering" });

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
            if (textureType == 1 || location.getResourcePath().endsWith("_0")) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public boolean load(IResourceManager manager, ResourceLocation location) {
            // just load up one of the base textures to reserve space on the texture sheet
            try {
                boolean useanisotropicFiltering = textureType == 0
                        ? (texmapAnisotropic.getInt(Minecraft.getMinecraft().getTextureMapBlocks()) > 1F)
                        : false;
                int mipmapLevels = textureType == 0
                        ? texmapMipMapLevels.getInt(Minecraft.getMinecraft().getTextureMapBlocks())
                        : 0;

                BufferedImage[] abufferedimage = new BufferedImage[1 + mipmapLevels];
                abufferedimage[0] = ImageIO.read(
                        manager.getResource(
                                new ResourceLocation(
                                        location.getResourceDomain(),
                                        (textureType == 0 ? "textures/blocks/barrel_top_border.png"
                                                : "textures/items/capaupg_base.png")))
                                .getInputStream());

                this.loadSprite(abufferedimage, null, useanisotropicFiltering);

                return false; // yes, this is opposite of what the javadoc states... the place it is used is also
                              // flipped
            } catch (Throwable t) {
                BetterBarrels.log.error(t);
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        public void replaceTextureData(int[] pixels, int mipmapLevels) throws Exception {
            BetterBarrels.debug("37p1 - entering texture replacement with " + mipmapLevels + " mipmap levels.");
            int[][] aint = new int[1 + mipmapLevels][];
            aint[0] = pixels;
            AccessibleTextureAtlasSprite.fixPixels.invoke(this, (Object) aint);
            boolean useAnisotropic = AccessibleTextureAtlasSprite.useAnisotropic.getBoolean(this);
            aint = (int[][]) AccessibleTextureAtlasSprite.setupAnisotropic.invoke(
                    this,
                    aint,
                    useAnisotropic ? this.width - 16 : this.width,
                    useAnisotropic ? this.height - 16 : this.height);
            aint = TextureUtil.generateMipmapData(mipmapLevels, this.width, aint);
            BetterBarrels.debug(
                    "37 - Attempting to replace texture for [" + this.getIconName()
                            + "] with an array of ["
                            + (aint != null ? aint[0].length : "(null)")
                            + "] pixels, current texture dims are ["
                            + this.width
                            + "x"
                            + this.height
                            + "] for a total size of "
                            + (this.width * this.height));
            BetterBarrels.debug(this.toString());
            if (aint[0].length != (this.height * this.width)) {
                throw new Exception(
                        "Attempting to replace texture image data with "
                                + (aint[0].length > (this.height * this.width) ? "too much" : "too little")
                                + " data.");
            }
            BetterBarrels.debug("38 - Calling Minecraft Texture upload utility method");
            TextureUtil.uploadTextureMipmap(aint, this.width, this.height, this.originX, this.originY, false, false);
            this.clearFramesTextureData();
        }
    }

    private static StructuralLevelClientData.AccessibleTextureAtlasSprite registerIcon(IIconRegister par1IconRegister,
            String key) {
        TextureMap texmap = (TextureMap) par1IconRegister;
        StructuralLevelClientData.AccessibleTextureAtlasSprite ret = new AccessibleTextureAtlasSprite(
                key,
                texmap.getTextureType());
        if (texmap.setTextureEntry(key, ret)) {
            return ret;
        } else {
            return (StructuralLevelClientData.AccessibleTextureAtlasSprite) (texmap.getTextureExtry(key));
        }
    }

    public void registerItemIcon(IIconRegister par1IconRegister, int ordinal) {
        this.iconItem = StructuralLevelClientData
                .registerIcon(par1IconRegister, BetterBarrels.modid + ":blanks/capacity/" + String.valueOf(ordinal));
    }

    public void registerBlockIcons(IIconRegister par1IconRegister, int ordinal) {
        this.iconBlockSide = StructuralLevelClientData
                .registerIcon(par1IconRegister, BetterBarrels.modid + ":barrel_side_" + String.valueOf(ordinal));
        this.iconBlockTop = StructuralLevelClientData
                .registerIcon(par1IconRegister, BetterBarrels.modid + ":barrel_top_" + String.valueOf(ordinal));
        this.iconBlockLabel = StructuralLevelClientData
                .registerIcon(par1IconRegister, BetterBarrels.modid + ":barrel_label_" + String.valueOf(ordinal));
        this.iconBlockTopLabel = StructuralLevelClientData
                .registerIcon(par1IconRegister, BetterBarrels.modid + ":barrel_labeltop_" + String.valueOf(ordinal));
    }

    private class PixelARGB {

        int A, R, G, B;
        int combined;
        private int addCount = 0;

        PixelARGB(final int pixel) {
            A = (pixel >> 24) & 0xFF;
            R = (pixel >> 16) & 0xFF;
            G = (pixel >> 8) & 0xFF;
            B = pixel & 0xFF;
            combined = pixel;
        }

        PixelARGB(final int alpha, final int red, final int green, final int blue) {
            A = alpha;
            R = red;
            G = green;
            B = blue;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
        }

        PixelARGB alphaAdd(PixelARGB add) {
            addCount++;
            A += add.A;
            R += (add.R * add.A) / 255;
            G += (add.G * add.G) / 255;
            B += (add.B * add.B) / 255;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
            return this;
        }

        PixelARGB normalize() {
            if (addCount == 0) return this;
            R = R * 255 / A;
            G = G * 255 / A;
            B = B * 255 / A;
            A = A / addCount;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
            addCount = 0;
            return this;
        }

        PixelARGB addIgnoreAlpha(PixelARGB add) {
            addCount++;
            R += add.R;
            G += add.G;
            B += add.B;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
            return this;
        }

        PixelARGB addSkipTransparent(PixelARGB add) {
            if (add.A == 0) return this;
            addCount++;
            R += add.R;
            G += add.G;
            B += add.B;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
            return this;
        }

        PixelARGB normalizeIgnoreAlpha() {
            if (addCount == 0) return this;
            R = R / addCount;
            G = G / addCount;
            B = B / addCount;
            combined = ((A & 0xFF) << 24) + ((R & 0xFF) << 16) + ((G & 0xFF) << 8) + (B & 0xFF);
            addCount = 0;
            return this;
        }

        PixelARGB YIQContrastTextColor() {
            int color = (((R * 299) + (G * 587) + (B * 114)) / 1000) >= 128 ? 0 : 255;
            return new PixelARGB(255, color, color, color);
        }
    }

    private void grainMergeArrayWithColor(int[] pixels, PixelARGB color) {
        BetterBarrels.debug("35 - Running grain merge on material with color");
        for (int i = 0; i < pixels.length; i++) {
            PixelARGB pix = new PixelARGB(pixels[i]);
            if (pix.A == 0) pixels[i] = 0;
            else pixels[i] = (new PixelARGB(
                    255,
                    Math.max(0, (Math.min(255, pix.R + color.R - 128))),
                    Math.max(0, (Math.min(255, pix.G + color.G - 128))),
                    Math.max(0, (Math.min(255, pix.B + color.B - 128))))).combined;
        }
        BetterBarrels.debug("36 - sanity check, pixels.length:" + pixels.length);
    }

    private void mergeArraysBasedOnAlpha(int[] target, int[] merge) {
        // Merge arrays, ignoring any transparent pixels in the merge array
        for (int i = 0; i < merge.length; i++) {
            PixelARGB targetPixel = new PixelARGB(target[i]);
            PixelARGB mergePixel = new PixelARGB(merge[i]);
            target[i] = mergePixel.A == 0 ? targetPixel.combined : mergePixel.combined;
        }
    }

    private PixelARGB averageColorFromArray(int[] pixels) {
        PixelARGB totals = new PixelARGB(0);
        for (int pixel : pixels) {
            totals.alphaAdd(new PixelARGB(pixel));
        }
        return totals.normalize();
    }

    private PixelARGB averageColorFromArrayB(int[] pixels) {
        PixelARGB totals = new PixelARGB(0);
        for (int pixel : pixels) {
            // totals.addIgnoreAlpha(new PixelARGB(pixel));
            totals.addSkipTransparent(new PixelARGB(pixel));
        }
        return totals.normalizeIgnoreAlpha();
    }

    private static int[] getPixelsForTexture(boolean item, ResourceLocation resourcelocation) {
        BetterBarrels.debug("09 - Entering texture load method for texture : " + resourcelocation.toString());
        ResourceLocation resourcelocation1 = new ResourceLocation(
                resourcelocation.getResourceDomain(),
                String.format(
                        "%s/%s%s",
                        new Object[] { (item ? "textures/items" : "textures/blocks"),
                                resourcelocation.getResourcePath(), ".png" }));
        BetterBarrels.debug("11 - Modified resource path : " + resourcelocation1.toString());
        int[] pixels = null;
        try {
            pixels = TextureUtil.readImageData(Minecraft.getMinecraft().getResourceManager(), resourcelocation1);
        } catch (Throwable t) {
            BetterBarrels.log.warn("JABBA-Debug Problem loading texture: " + resourcelocation);
        }
        BetterBarrels.debug("12 - read texture data of length : " + (pixels != null ? pixels.length : "(null)"));
        return pixels;
    }

    private static int[] getPixelsForTexture(boolean item, String location) {
        return getPixelsForTexture(item, new ResourceLocation(location));
    }

    private static int[] getPixelsForTexture(boolean item, IIcon icon) {
        return getPixelsForTexture(item, new ResourceLocation(icon.getIconName()));
    }

    public boolean generateIcons() {
        BetterBarrels.debug("17 - Entering Texture Generation for Structural Tier: " + this.level);
        int terrainTextureId = Minecraft.getMinecraft().renderEngine.getTexture(TextureMap.locationBlocksTexture)
                .getGlTextureId();
        int itemTextureId = Minecraft.getMinecraft().renderEngine.getTexture(TextureMap.locationItemsTexture)
                .getGlTextureId();
        if (terrainTextureId != 0 && itemTextureId != 0) {
            // Store previous texture
            int previousTextureID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            // Copy the block textures we need into arrays
            int[] labelBorderPixels = baseTexturePixels.labelBorder.clone();
            BetterBarrels.debug("18 - " + labelBorderPixels.length);
            int[] labelBackgroundPixels = baseTexturePixels.labelBackground.clone();
            BetterBarrels.debug("19 - " + labelBackgroundPixels.length);
            int[] topBorderPixels = baseTexturePixels.topBorder.clone();
            BetterBarrels.debug("20 - " + topBorderPixels.length);
            int[] topBackgroundPixels = baseTexturePixels.topBackground.clone();
            BetterBarrels.debug("21 - " + topBackgroundPixels.length);
            int[] topLabelBorderPixels = baseTexturePixels.topBorder.clone();
            BetterBarrels.debug("22 - " + topLabelBorderPixels.length);
            int[] topLabelBackgroundPixels = baseTexturePixels.topLabel.clone();
            BetterBarrels.debug("23 - " + topLabelBackgroundPixels.length);
            int[] sideBorderPixels = baseTexturePixels.sideBorder.clone();
            BetterBarrels.debug("24 - " + sideBorderPixels.length);
            int[] sideBackgroundPixels = baseTexturePixels.sideBackground.clone();
            BetterBarrels.debug("25 - " + sideBackgroundPixels.length);

            // Copy the item textures we need into arrays
            int[] itemBasePixels = baseTexturePixels.item.clone();
            BetterBarrels.debug("26 - " + itemBasePixels.length);
            int[] itemArrowPixels = baseTexturePixels.itemArrow.clone();
            BetterBarrels.debug("27 - " + itemArrowPixels.length);
            int[] itemRomanPixels = getPixelsForTexture(true, this.iconItem);
            BetterBarrels.debug("28 - " + itemRomanPixels.length);

            int[] materialPixels = null;
            boolean foundSourceMaterial = false;
            if (colorOverride == -1) {
                try {
                    while (level.material.meta >= 0) {
                        cacheStackAndName();
                        Block materialBlock = Block.getBlockFromItem(materialStack.getItem());
                        Item materialItem = materialStack.getItem();

                        if (materialBlock != Blocks.air
                                && !materialBlock.getUnlocalizedName().equalsIgnoreCase("tile.ForgeFiller")) {
                            BetterBarrels.debug("32 - Block found");
                            materialPixels = getPixelsForTexture(
                                    false,
                                    materialBlock.getIcon(0, materialStack.getItemDamage()));
                            foundSourceMaterial = true;
                            BetterBarrels.debug(
                                    "33 - Loaded texture data for [" + this.name
                                            + "]: read an array of length: "
                                            + (materialPixels != null ? materialPixels.length : "(null)"));
                        } else if (materialItem != null) {
                            BetterBarrels.debug("30 - Item found, attempting to load");
                            materialPixels = getPixelsForTexture(
                                    true,
                                    materialItem.getIconFromDamage(materialStack.getItemDamage()));
                            foundSourceMaterial = true;
                            BetterBarrels.debug(
                                    "30 - Loaded texture data for [" + this.name
                                            + "]: read an array of length: "
                                            + (materialPixels != null ? materialPixels.length : "(null)"));
                        }
                        if (materialPixels != null || !level.material.isOreDict()) {
                            break;
                        }
                        if (level.material.meta != -1) {
                            level.material.meta++;
                        }
                    }
                } catch (Throwable t) {
                    BetterBarrels.debug("34 - MATERIAL LOOKUP ERROR");
                    BetterBarrels.log.error("Error loading resource material texture: " + t.getMessage());
                    t.printStackTrace();
                } finally {
                    // nothing found, skip out
                    if (!foundSourceMaterial) {
                        BetterBarrels.log.error(
                                "Encountered an issue while locating the requested source material["
                                        + (level.material.isOreDict() ? level.material.name
                                                : (level.material.modDomain + ":"
                                                        + level.material.name
                                                        + ":"
                                                        + level.material.meta))
                                        + "].  Ore Dictionary returned "
                                        + materialStack.getUnlocalizedName()
                                        + " as the first itemStack for that request.");
                    } else {
                        if (materialPixels == null) {
                            materialPixels = new int[1];
                            BetterBarrels.debug("13 - No texture data read, creating empty array of for color black");
                        }
                    }
                }
            } else {
                // Grab the first name for the item, but the color from the override
                // TODO: Change this later? maybe have name override also?
                cacheStackAndName();
                materialPixels = new int[1];
                materialPixels[0] = colorOverride;
                foundSourceMaterial = true;
            }

            PixelARGB color;
            if (foundSourceMaterial) {
                // color = averageColorFromArray(materialPixels); // This makes iron... more red, kind of a neat rusty
                // look, but meh
                color = averageColorFromArrayB(materialPixels);
                BetterBarrels.debug(
                        "Calculated Color for [" + this.name
                                + "]: {R: "
                                + color.R
                                + ", G: "
                                + color.G
                                + ", B: "
                                + color.B
                                + "}");
            } else {
                color = new PixelARGB(255, 205, 205, 205);
                BetterBarrels.debug(
                        "Using default color for " + name
                                + " due to not being able to load its texture for color calculation.");
            }

            this.textColor = color.YIQContrastTextColor().combined;

            grainMergeArrayWithColor(labelBorderPixels, color);
            grainMergeArrayWithColor(topBorderPixels, color);
            grainMergeArrayWithColor(topLabelBorderPixels, color);
            grainMergeArrayWithColor(sideBorderPixels, color);
            grainMergeArrayWithColor(itemArrowPixels, color);

            this.textColor = averageColorFromArrayB(labelBorderPixels).YIQContrastTextColor().combined;

            int mipmapLevels = Utils.ReflectionHelper.getFieldValue(
                    Integer.class,
                    Minecraft.getMinecraft().gameSettings.mipmapLevels,
                    Minecraft.getMinecraft().getTextureMapBlocks(),
                    TextureMap.class,
                    new String[] { "j", "field_147636_j", "mipmapLevels" },
                    Level.WARN,
                    "Unable to reflect Block TextureMap mipmapLevels. Defaulting to GameSettings mipmapLevels");

            try {
                mergeArraysBasedOnAlpha(labelBorderPixels, labelBackgroundPixels);
                mergeArraysBasedOnAlpha(topBorderPixels, topBackgroundPixels);
                mergeArraysBasedOnAlpha(topLabelBorderPixels, topLabelBackgroundPixels);
                mergeArraysBasedOnAlpha(sideBorderPixels, sideBackgroundPixels);
                mergeArraysBasedOnAlpha(itemBasePixels, itemArrowPixels);
                mergeArraysBasedOnAlpha(itemBasePixels, itemRomanPixels);

                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, terrainTextureId);
                this.iconBlockLabel.replaceTextureData(labelBorderPixels, mipmapLevels);
                this.iconBlockTop.replaceTextureData(topBorderPixels, mipmapLevels);
                this.iconBlockTopLabel.replaceTextureData(topLabelBorderPixels, mipmapLevels);
                this.iconBlockSide.replaceTextureData(sideBorderPixels, mipmapLevels);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, itemTextureId);
                this.iconItem.replaceTextureData(itemBasePixels, 0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextureID);
                GL11.glPopAttrib();
                return true;
            } catch (Exception e) {
                BetterBarrels.log.error("caught exception while generating icons: " + e.toString() + e.getMessage());
            }
        }
        return false;
    }
}
