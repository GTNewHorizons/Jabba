package mcp.mobius.betterbarrels.api.events;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge; // This was added only for documentation

/**
 * Event sent before a Dolly places its stored block into the world.
 *
 * <p>With this event other mods can validate and prevent
 * Dolly placement. Canceling this event prevents The dolly from
 * placing the block in the world.
 *
 * <p>This event is {@link Cancelable}.
 * <p>This event is posted on {@link MinecraftForge#EVENT_BUS}.
 */
@Cancelable
public class DollyPlaceEvent extends Event {

    public final EntityPlayer player;
    public final World world;
    public final Block block;
    public final int targX, targY, targZ;
    public final int metadata;
    public final NBTTagCompound tileNBT;

    /**
     * Creates the {@link DollyPlaceEvent}
     *
     * @param player Player attempting to place
     * @param world World where the block will be placed
     * @param block Stored Block that will be placed
     * @param targX Target X coordinate
     * @param targY Target Y coordinate
     * @param targZ Target Z coordinate
     * @param metadata Stored Block Metadata
     * @param tileNBT Stored Block TileEntity NBT Data
     */
    public DollyPlaceEvent(EntityPlayer player, World world, Block block, int targX, int targY, int targZ,
                           int metadata, NBTTagCompound tileNBT) {

        this.player = player;
        this.world = world;
        this.block = block;
        this.targX = targX;
        this.targY = targY;
        this.targZ = targZ;
        this.metadata = metadata;
        this.tileNBT = (NBTTagCompound) tileNBT.copy();
    }
}
