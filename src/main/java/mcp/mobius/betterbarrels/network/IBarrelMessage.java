package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface IBarrelMessage {
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception;
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage msg);

}
