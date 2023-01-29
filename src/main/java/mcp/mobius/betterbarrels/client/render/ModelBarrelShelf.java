package mcp.mobius.betterbarrels.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelBarrelShelf extends ModelBase {

    // fields
    ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape3;
    ModelRenderer Shape4;
    ModelRenderer Shape5;
    ModelRenderer Shape6;
    ModelRenderer Shape7;

    public ModelBarrelShelf() {
        textureWidth = 64;
        textureHeight = 32;

        Shape1 = new ModelRenderer(this, 0, 0);
        Shape1.addBox(0F, 0F, 0F, 32, 32, 1);
        Shape1.setRotationPoint(15F, 16F, 16F);
        Shape1.setTextureSize(64, 32);
        Shape1.mirror = true;
        setRotation(Shape1, 0F, 1.570796F, 0F);

        Shape2 = new ModelRenderer(this, 0, 0);
        Shape2.addBox(0F, 0F, 0F, 31, 32, 1);
        Shape2.setRotationPoint(-16F, 16F, 15F);
        Shape2.setTextureSize(64, 32);
        Shape2.mirror = true;
        setRotation(Shape2, 0F, 0F, 0F);

        Shape3 = new ModelRenderer(this, 0, 0);
        Shape3.addBox(0F, 0F, 0F, 31, 32, 1);
        Shape3.setRotationPoint(-16F, 16F, -16F);
        Shape3.setTextureSize(64, 32);
        Shape3.mirror = true;
        setRotation(Shape3, 0F, 0F, 0F);

        Shape4 = new ModelRenderer(this, 0, 0);
        Shape4.addBox(0F, 0F, 0F, 31, 1, 30);
        Shape4.setRotationPoint(-16F, 16F, -15F);
        Shape4.setTextureSize(64, 32);
        Shape4.mirror = true;
        setRotation(Shape4, 0F, 0F, 0F);

        Shape5 = new ModelRenderer(this, 0, 0);
        Shape5.addBox(0F, 0F, 0F, 31, 1, 30);
        Shape5.setRotationPoint(-16F, 47F, -15F);
        Shape5.setTextureSize(64, 32);
        Shape5.mirror = true;
        setRotation(Shape5, 0F, 0F, 0F);

        Shape6 = new ModelRenderer(this, 0, 0);
        Shape6.addBox(0F, 0F, 0F, 31, 1, 30);
        Shape6.setRotationPoint(-16F, 31.5F, -15F);
        Shape6.setTextureSize(64, 32);
        Shape6.mirror = true;
        setRotation(Shape6, 0F, 0F, 0F);

        Shape7 = new ModelRenderer(this, 0, 0);
        Shape7.addBox(0F, 0F, 0F, 31, 1, 30);
        Shape7.setRotationPoint(-16F, 47F, -0.5F);
        Shape7.setTextureSize(64, 32);
        Shape7.mirror = true;
        setRotation(Shape7, 1.570796F, 0F, 0F);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        super.render(entity, f, f1, f2, f3, f4, f5);
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        Shape1.render(f5);
        Shape2.render(f5);
        Shape3.render(f5);
        Shape4.render(f5);
        Shape5.render(f5);
        Shape6.render(f5);
        Shape7.render(f5);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }

    public void render() {
        // float scale = 0.0625F;
        float scale = 1F / 32F;

        this.Shape1.render(scale);
        this.Shape2.render(scale);
        this.Shape3.render(scale);
        this.Shape4.render(scale);
        this.Shape5.render(scale);
        this.Shape6.render(scale);
        this.Shape7.render(scale);
    }
}

/*
 * public class ModelBarrelShelf extends ModelBase { private ModelRenderer shape1; private ModelRenderer shape2; private
 * ModelRenderer shape3; private ModelRenderer shape4; private ModelRenderer shape5; private ModelRenderer shape6;
 * private ModelRenderer shape7; public ModelBarrelShelf(){ textureWidth = 64; textureHeight = 32; shape1 = new
 * ModelRenderer(this, 0, 0); shape1.addBox(0F, 0F, 0F, 16, 16, 1); shape1.setRotationPoint(7F, 8F, 8F);
 * shape1.setTextureSize(64, 32); shape1.mirror = true; setRotation(shape1, 0F, 1.570796F, 0F); shape2 = new
 * ModelRenderer(this, 0, 0); shape2.addBox(0F, 0F, 0F, 15, 16, 1); shape2.setRotationPoint(-8F, 8F, 7F);
 * shape2.setTextureSize(64, 32); shape2.mirror = true; setRotation(shape2, 0F, 0F, 0F); shape3 = new
 * ModelRenderer(this, 0, 0); shape3.addBox(0F, 0F, 0F, 15, 16, 1); shape3.setRotationPoint(-8F, 8F, -8F);
 * shape3.setTextureSize(64, 32); shape3.mirror = true; setRotation(shape3, 0F, 0F, 0F); shape4 = new
 * ModelRenderer(this, 0, 0); shape4.addBox(0F, 0F, 0F, 15, 1, 14); shape4.setRotationPoint(-8F, 8F, -7F);
 * shape4.setTextureSize(64, 32); shape4.mirror = true; setRotation(shape4, 0F, 0F, 0F); shape5 = new
 * ModelRenderer(this, 0, 0); shape5.addBox(0F, 0F, 0F, 15, 1, 14); shape5.setRotationPoint(-8F, 23F, -7F);
 * shape5.setTextureSize(64, 32); shape5.mirror = true; setRotation(shape5, 0F, 0F, 0F); shape6 = new
 * ModelRenderer(this, 0, 0); shape6.addBox(0F, 0F, 0F, 15, 1, 14); shape6.setRotationPoint(-8F, 15.5F, -7F);
 * shape6.setTextureSize(64, 32); shape6.mirror = true; setRotation(shape6, 0F, 0F, 0F); shape7 = new
 * ModelRenderer(this, 0, 0); shape7.addBox(0F, 0F, 0F, 15, 1, 14); shape7.setRotationPoint(-8F, 23F, -0.5F);
 * shape7.setTextureSize(64, 32); shape7.mirror = true; setRotation(shape7, 1.570796F, 0F, 0F); } public void
 * render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5){ super.render(entity, f, f1, f2, f3,
 * f4, f5); super.setRotationAngles(f, f1, f2, f3, f4, f5, entity); this.shape1.render(f5); this.shape2.render(f5);
 * this.shape3.render(f5); this.shape4.render(f5); this.shape5.render(f5); this.shape6.render(f5);
 * this.shape7.render(f5); } private void setRotation(ModelRenderer model, float x, float y, float z){
 * model.rotateAngleX = x; model.rotateAngleY = y; model.rotateAngleZ = z; } public void render(){ float scale =
 * 0.0625F; this.shape1.render(scale); this.shape2.render(scale); this.shape3.render(scale); this.shape4.render(scale);
 * this.shape5.render(scale); this.shape6.render(scale); this.shape7.render(scale); } } /* public class
 * ModelBarrelShelve extends ModelBase { public ModelBarrelShelve(){ } public void renderAll(){ } }
 */
