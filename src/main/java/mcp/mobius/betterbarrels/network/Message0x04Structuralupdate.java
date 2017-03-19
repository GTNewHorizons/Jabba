package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;

public class Message0x04Structuralupdate extends SimpleChannelInboundHandler<Message0x04Structuralupdate> implements IBarrelMessage {
	public int  x,y,z;
	public int  level;

	public Message0x04Structuralupdate() {}
	public Message0x04Structuralupdate(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.level = barrel.coreUpgrades.levelStructural;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		target.writeInt(level);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x04Structuralupdate msg = (Message0x04Structuralupdate)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		msg.level = dat.readInt();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x04Structuralupdate msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null) {
			barrel.coreUpgrades.levelStructural = msg.level;
			Minecraft.getMinecraft().theWorld.markBlockForUpdate(msg.x, msg.y, msg.z);
		}
	}
}
