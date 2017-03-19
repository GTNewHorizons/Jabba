package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;

public class Message0x07ForceRender extends SimpleChannelInboundHandler<Message0x07ForceRender> implements IBarrelMessage {
	public int  x,y,z;

	public Message0x07ForceRender() {}
	public Message0x07ForceRender(int xCoord, int yCoord, int zCoord) {
		this.x = xCoord;
		this.y = yCoord;
		this.z = zCoord;
	}
	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x07ForceRender msg = (Message0x07ForceRender)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x07ForceRender msg) throws Exception {
		Minecraft.getMinecraft().theWorld.markBlockForUpdate(msg.x, msg.y, msg.z);
	}
}
