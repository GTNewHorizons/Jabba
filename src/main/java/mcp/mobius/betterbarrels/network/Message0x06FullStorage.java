package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.StorageLocal;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

public class Message0x06FullStorage extends SimpleChannelInboundHandler<Message0x06FullStorage> implements IBarrelMessage {
	public int  x,y,z;
	public NBTTagCompound storageTag = new NBTTagCompound();

	public Message0x06FullStorage() {}
	public Message0x06FullStorage(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.storageTag = barrel.getStorage().writeTagCompound();
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		BarrelPacketHandler.INSTANCE.writeNBTTagCompoundToBuffer(target, storageTag);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x06FullStorage msg = (Message0x06FullStorage)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		try {
			msg.storageTag = BarrelPacketHandler.INSTANCE.readNBTTagCompoundFromBuffer(dat);
		} catch(Exception e) {
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x06FullStorage msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null) {
			StorageLocal storage = new StorageLocal();
			storage.readTagCompound(msg.storageTag);
			barrel.setStorage(storage);
		}
	}
}
