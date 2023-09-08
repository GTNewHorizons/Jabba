package mcp.mobius.betterbarrels.network;

import java.io.IOException;
import java.util.EnumMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.LocalizedChat;

public enum BarrelPacketHandler {

    INSTANCE;

    public EnumMap<Side, FMLEmbeddedChannel> channels;

    private BarrelPacketHandler() {
        this.channels = NetworkRegistry.INSTANCE.newChannel(BetterBarrels.modid, new BarrelCodec());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            addClientHandlers();
            // addServerHandlers();
        } else {
            // addServerHandlers();
        }

    }

    private void addClientHandlers() {
        FMLEmbeddedChannel channel = this.channels.get(Side.CLIENT);
        String codec = channel.findChannelHandlerNameForType(BarrelCodec.class);

        channel.pipeline().addAfter(codec, "ClientHandler", new Message0x00FulleTileEntityNBT());
        channel.pipeline().addAfter("ClientHandler", "ContentUpdate", new Message0x01ContentUpdate());
        channel.pipeline().addAfter("ContentUpdate", "GhostUpdate", new Message0x02GhostUpdate());
        channel.pipeline().addAfter("GhostUpdate", "Sideupgradeupdate", new Message0x03SideupgradeUpdate());
        channel.pipeline().addAfter("Sideupgradeupdate", "Structuralupdate", new Message0x04Structuralupdate());
        channel.pipeline().addAfter("Structuralupdate", "CoreUpdate", new Message0x05CoreUpdate());
        channel.pipeline().addAfter("CoreUpdate", "FullStorage", new Message0x06FullStorage());
        channel.pipeline().addAfter("FullStorage", "ForceRender", new Message0x07ForceRender());
        channel.pipeline().addAfter("ForceRender", "LinkUpdate", new Message0x08LinkUpdate());
        channel.pipeline().addAfter("LinkUpdate", "LocalizedChat", new Message0x09LocalizedChat());
    }

    /*
     * private void addServerHandlers() { FMLEmbeddedChannel channel = this.channels.get(Side.SERVER); String codec =
     * channel.findChannelHandlerNameForType(BarrelCodec.class); channel.pipeline().addAfter(codec, "TERequest", new
     * Message0x01TERequest()); }
     */

    private class BarrelCodec extends FMLIndexedMessageToMessageCodec<IBarrelMessage> {

        public BarrelCodec() {
            addDiscriminator(0, Message0x00FulleTileEntityNBT.class);
            addDiscriminator(1, Message0x01ContentUpdate.class);
            addDiscriminator(2, Message0x02GhostUpdate.class);
            addDiscriminator(3, Message0x03SideupgradeUpdate.class);
            addDiscriminator(4, Message0x04Structuralupdate.class);
            addDiscriminator(5, Message0x05CoreUpdate.class);
            addDiscriminator(6, Message0x06FullStorage.class);
            addDiscriminator(7, Message0x07ForceRender.class);
            addDiscriminator(8, Message0x08LinkUpdate.class);
            addDiscriminator(9, Message0x09LocalizedChat.class);
        }

        @Override
        public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
            msg.encodeInto(ctx, msg, target);
        }

        @Override
        public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage msg) {
            msg.decodeInto(ctx, dat, msg);
        }
    }

    public void sendTo(IBarrelMessage message, EntityPlayerMP player) {
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channels.get(Side.SERVER).writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendToDimension(IBarrelMessage message, int dimensionId) {
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimensionId);
        channels.get(Side.SERVER).writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendToAllAround(IBarrelMessage message, NetworkRegistry.TargetPoint point) {
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        channels.get(Side.SERVER).writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /*
     * public void sendToServer(IBarrelMessage message) {
     * channels.get(Side.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.
     * TOSERVER);
     * channels.get(Side.CLIENT).writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE); }
     */

    public static void sendLocalizedChat(EntityPlayer player, LocalizedChat message, Object... extraNumbers) {
        if (player instanceof EntityPlayerMP) {
            BarrelPacketHandler.INSTANCE
                    .sendTo(new Message0x09LocalizedChat(message, extraNumbers), (EntityPlayerMP) player);
        } else {}
    }

    public void writeNBTTagCompoundToBuffer(ByteBuf target, NBTTagCompound tag) throws IOException {
        if (tag == null) {
            target.writeShort(-1);
        } else {
            byte[] abyte = CompressedStreamTools.compress(tag);
            target.writeShort((short) abyte.length);
            target.writeBytes(abyte);
        }
    }

    public NBTTagCompound readNBTTagCompoundFromBuffer(ByteBuf dat) throws IOException {
        short short1 = dat.readShort();

        if (short1 < 0) {
            return null;
        } else {
            byte[] abyte = new byte[short1];
            dat.readBytes(abyte);
            return CompressedStreamTools.func_152457_a(abyte, NBTSizeTracker.field_152451_a);
        }
    }

    public void writeItemStackToBuffer(ByteBuf target, ItemStack stack) throws IOException {
        if (stack == null) {
            target.writeShort(-1);
        } else {
            target.writeShort(Item.getIdFromItem(stack.getItem()));
            target.writeByte(stack.stackSize);
            target.writeShort(stack.getItemDamage());
            NBTTagCompound nbttagcompound = null;

            if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
                nbttagcompound = stack.stackTagCompound;
            }

            this.writeNBTTagCompoundToBuffer(target, nbttagcompound);
        }
    }

    public ItemStack readItemStackFromBuffer(ByteBuf dat) throws IOException {
        ItemStack itemstack = null;
        short short1 = dat.readShort();

        if (short1 >= 0) {
            byte b0 = dat.readByte();
            short short2 = dat.readShort();
            itemstack = new ItemStack(Item.getItemById(short1), b0, short2);
            itemstack.stackTagCompound = this.readNBTTagCompoundFromBuffer(dat);
        }

        return itemstack;
    }
}
