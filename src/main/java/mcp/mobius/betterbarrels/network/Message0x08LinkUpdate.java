package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;

public class Message0x08LinkUpdate extends SimpleChannelInboundHandler<Message0x08LinkUpdate> implements IBarrelMessage {
	public int  x,y,z;
	public boolean isLinked;

	public Message0x08LinkUpdate() {}
	public Message0x08LinkUpdate(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.isLinked = barrel.isLinked;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		target.writeBoolean(isLinked);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x08LinkUpdate msg = (Message0x08LinkUpdate)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		msg.isLinked = dat.readBoolean();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x08LinkUpdate msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null) {
			barrel.isLinked = msg.isLinked;
			Minecraft.getMinecraft().theWorld.markBlockForUpdate(msg.x, msg.y, msg.z);
		}
	}
}
