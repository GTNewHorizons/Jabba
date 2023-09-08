package mcp.mobius.betterbarrels.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;

public class Message0x00FulleTileEntityNBT extends SimpleChannelInboundHandler<Message0x00FulleTileEntityNBT>
        implements IBarrelMessage {

    public int x, y, z;
    public NBTTagCompound fullTETag = new NBTTagCompound();

    public Message0x00FulleTileEntityNBT() {}

    public Message0x00FulleTileEntityNBT(TileEntityBarrel barrel) {
        this.x = barrel.xCoord;
        this.y = barrel.yCoord;
        this.z = barrel.zCoord;
        barrel.writeToNBT(this.fullTETag);
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
        target.writeInt(x);
        target.writeInt(y);
        target.writeInt(z);
        BarrelPacketHandler.INSTANCE.writeNBTTagCompoundToBuffer(target, fullTETag);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
        Message0x00FulleTileEntityNBT msg = (Message0x00FulleTileEntityNBT) rawmsg;
        msg.x = dat.readInt();
        msg.y = dat.readInt();
        msg.z = dat.readInt();
        try {
            msg.fullTETag = BarrelPacketHandler.INSTANCE.readNBTTagCompoundFromBuffer(dat);
        } catch (Exception e) {}
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message0x00FulleTileEntityNBT msg) throws Exception {
        TileEntityBarrel barrel = (TileEntityBarrel) Minecraft.getMinecraft().theWorld
                .getTileEntity(msg.x, msg.y, msg.z);
        if (barrel != null) {
            barrel.readFromNBT(msg.fullTETag);
        }
    }
}
