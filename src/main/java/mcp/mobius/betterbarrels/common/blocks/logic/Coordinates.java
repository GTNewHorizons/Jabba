package mcp.mobius.betterbarrels.common.blocks.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.DimensionManager;

public final class Coordinates {

    public final int dim;
    public final double x, y, z;

    public Coordinates(int dim, double x, double y, double z) {
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinates(NBTTagCompound tag) {
        this.dim = tag.getInteger("dim");
        this.x = tag.getDouble("x");
        this.y = tag.getDouble("y");
        this.z = tag.getDouble("z");
    }

    public TileEntity getEntityAt() {
        IBlockAccess world = DimensionManager.getWorld(this.dim);
        if (world == null) return null;
        return world.getTileEntity(
                MathHelper.floor_double(this.x),
                MathHelper.floor_double(this.y),
                MathHelper.floor_double(this.z));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        Coordinates c = (Coordinates) o;
        return (this.dim == c.dim) && (this.x == c.x) && (this.y == c.y) && (this.z == c.z);
    }

    @Override
    public int hashCode() {
        return MathHelper.floor_double(this.dim + 31 * this.x + 877 * this.y + 3187 * this.z);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dim", this.dim);
        tag.setDouble("x", this.x);
        tag.setDouble("y", this.y);
        tag.setDouble("z", this.z);
        return tag;
    }
}
