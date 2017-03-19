package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class Message0x01ContentUpdate extends SimpleChannelInboundHandler<Message0x01ContentUpdate> implements IBarrelMessage {
	public int  x, y, z;
	public int  amount;
	public ItemStack stack = null;

	public Message0x01ContentUpdate() {}
	public Message0x01ContentUpdate(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.amount = barrel.getStorage().getAmount();
		this.stack = barrel.getStorage().getItem();
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		target.writeInt(amount);
		BarrelPacketHandler.INSTANCE.writeItemStackToBuffer(target, stack);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x01ContentUpdate msg = (Message0x01ContentUpdate)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		msg.amount = dat.readInt();
		try {
			msg.stack = BarrelPacketHandler.INSTANCE.readItemStackFromBuffer(dat);
		} catch(Exception e) {
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x01ContentUpdate msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null)
			barrel.getStorage().setStoredItemType(msg.stack, msg.amount);
	}
}
