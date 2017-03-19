package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;

public class Message0x02GhostUpdate extends SimpleChannelInboundHandler<Message0x02GhostUpdate> implements IBarrelMessage {
	public int  x,y,z;
	public boolean  locked;

	public Message0x02GhostUpdate() {}
	public Message0x02GhostUpdate(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.locked = barrel.getStorage().isGhosting();
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		target.writeBoolean(locked);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x02GhostUpdate msg = (Message0x02GhostUpdate)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		msg.locked = dat.readBoolean();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x02GhostUpdate msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null) {
			barrel.getStorage().setGhosting(msg.locked);
			Minecraft.getMinecraft().theWorld.markBlockForUpdate(msg.x, msg.y, msg.z);
		}
	}
}
