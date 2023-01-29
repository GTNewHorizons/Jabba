package mcp.mobius.betterbarrels.common.blocks.logic;

public final class ItemImmut {

    public final int id, meta;

    public ItemImmut(int id, int meta) {
        this.id = id;
        this.meta = meta;
    }

    @Override
    public boolean equals(Object o) {
        ItemImmut c = (ItemImmut) o;
        return (this.id == c.id) && (this.meta == c.meta);
    }

    @Override
    public int hashCode() {
        return this.meta + this.id * 32768;
    }
}
