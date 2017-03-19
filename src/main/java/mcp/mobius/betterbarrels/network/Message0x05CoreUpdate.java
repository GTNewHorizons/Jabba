package mcp.mobius.betterbarrels.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;

import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import mcp.mobius.betterbarrels.common.items.upgrades.UpgradeCore;
import net.minecraft.client.Minecraft;

public class Message0x05CoreUpdate extends SimpleChannelInboundHandler<Message0x05CoreUpdate> implements IBarrelMessage {
	public int  x,y,z;
	public int  nStorageUpg = 0;
	public boolean hasRedstone = false;
	public boolean hasHopper   = false;
	public boolean hasEnder    = false;
	public boolean hasVoid     = false;
	public boolean hasCreative = false;
	public ArrayList<UpgradeCore> upgrades = new ArrayList<UpgradeCore>();

	public Message0x05CoreUpdate() {}
	public Message0x05CoreUpdate(TileEntityBarrel barrel) {
		this.x = barrel.xCoord;
		this.y = barrel.yCoord;
		this.z = barrel.zCoord;
		this.nStorageUpg = barrel.coreUpgrades.nStorageUpg;
		this.hasRedstone = barrel.coreUpgrades.hasRedstone;
		this.hasHopper = barrel.coreUpgrades.hasHopper;
		this.hasEnder = barrel.coreUpgrades.hasEnder;
		this.hasVoid = barrel.coreUpgrades.hasVoid;
		this.hasCreative = barrel.coreUpgrades.hasCreative;

		for (UpgradeCore i : barrel.coreUpgrades.upgradeList)
			upgrades.add(i);
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
		target.writeInt(x);
		target.writeInt(y);
		target.writeInt(z);
		target.writeInt(upgrades.size());
		for (UpgradeCore i : upgrades)
			target.writeInt(i.ordinal());
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
		Message0x05CoreUpdate msg = (Message0x05CoreUpdate)rawmsg;
		msg.x = dat.readInt();
		msg.y = dat.readInt();
		msg.z = dat.readInt();
		int size = dat.readInt();
		for (int i = 0; i < size; i++)
			msg.upgrades.add(UpgradeCore.values()[dat.readInt()]);

		for (UpgradeCore i : msg.upgrades) {
			if (i.type == UpgradeCore.Type.STORAGE) {
				msg.nStorageUpg += i.slotsUsed;
			} else if (i == UpgradeCore.ENDER)
				msg.hasEnder = true;
			else if (i == UpgradeCore.HOPPER)
				msg.hasHopper = true;
			else if (i == UpgradeCore.REDSTONE)
				msg.hasRedstone = true;
			else if (i == UpgradeCore.VOID)
				msg.hasVoid = true;
			else if (i == UpgradeCore.CREATIVE)
				msg.hasCreative = true;
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x05CoreUpdate msg) throws Exception {
		TileEntityBarrel barrel = (TileEntityBarrel)Minecraft.getMinecraft().theWorld.getTileEntity(msg.x, msg.y, msg.z);
		if (barrel != null) {
			barrel.coreUpgrades.upgradeList  = msg.upgrades;
			barrel.coreUpgrades.hasRedstone  = msg.hasRedstone;
			barrel.coreUpgrades.hasHopper    = msg.hasHopper;
			barrel.coreUpgrades.hasEnder     = msg.hasEnder;
			barrel.coreUpgrades.nStorageUpg  = msg.nStorageUpg;
			barrel.setVoid(msg.hasVoid);
			barrel.setCreative(msg.hasCreative);
		}
	}
}
