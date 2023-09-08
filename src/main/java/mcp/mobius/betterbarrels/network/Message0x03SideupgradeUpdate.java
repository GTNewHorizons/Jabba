package mcp.mobius.betterbarrels.network;

import net.minecraft.client.Minecraft;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;

public class Message0x03SideupgradeUpdate extends SimpleChannelInboundHandler<Message0x03SideupgradeUpdate>
        implements IBarrelMessage {

    public int x, y, z;
    public int[] sideUpgrades = new int[6];
    public int[] sideMetadata = new int[6];

    public Message0x03SideupgradeUpdate() {}

    public Message0x03SideupgradeUpdate(TileEntityBarrel barrel) {
        this.x = barrel.xCoord;
        this.y = barrel.yCoord;
        this.z = barrel.zCoord;

        for (int i = 0; i < 6; i++) this.sideUpgrades[i] = barrel.sideUpgrades[i];

        for (int i = 0; i < 6; i++) this.sideMetadata[i] = barrel.sideMetadata[i];
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
        target.writeInt(x);
        target.writeInt(y);
        target.writeInt(z);

        for (int i = 0; i < 6; i++) target.writeInt(sideUpgrades[i]);

        for (int i = 0; i < 6; i++) target.writeInt(sideMetadata[i]);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
        Message0x03SideupgradeUpdate msg = (Message0x03SideupgradeUpdate) rawmsg;
        msg.x = dat.readInt();
        msg.y = dat.readInt();
        msg.z = dat.readInt();

        for (int i = 0; i < 6; i++) msg.sideUpgrades[i] = dat.readInt();

        for (int i = 0; i < 6; i++) msg.sideMetadata[i] = dat.readInt();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message0x03SideupgradeUpdate msg) throws Exception {
        TileEntityBarrel barrel = (TileEntityBarrel) Minecraft.getMinecraft().theWorld
                .getTileEntity(msg.x, msg.y, msg.z);
        if (barrel != null) {
            barrel.sideUpgrades = msg.sideUpgrades;
            barrel.sideMetadata = msg.sideMetadata;
            Minecraft.getMinecraft().theWorld.markBlockForUpdate(msg.x, msg.y, msg.z);
        }
    }
}
