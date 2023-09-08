package mcp.mobius.betterbarrels.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.common.LocalizedChat;

public class Message0x09LocalizedChat extends SimpleChannelInboundHandler<Message0x09LocalizedChat>
        implements IBarrelMessage {

    public int messageID;
    public int extraCount;
    ArrayList<SupportedExtraTypes> extraTypesList = new ArrayList<SupportedExtraTypes>();
    ArrayList<Object> extraValuesList = new ArrayList<Object>();

    enum SupportedExtraTypes {

        INT(Integer.class),
        STR(String.class),
        FLT(Float.class);
        // bool byte char double long short

        Class clazz;

        private SupportedExtraTypes(Class clazz) {
            this.clazz = clazz;
        }

        public static SupportedExtraTypes getType(int i) {
            return SupportedExtraTypes.values()[i];
        }

        public static SupportedExtraTypes getTypeFromObject(Object o) {
            for (SupportedExtraTypes supportedType : SupportedExtraTypes.values()) {
                if (o.getClass().isAssignableFrom(supportedType.clazz)) {
                    return supportedType;
                }
            }
            return null;
        }
    };

    public Message0x09LocalizedChat() {}

    public Message0x09LocalizedChat(LocalizedChat message, Object... extraItems) {
        this.messageID = message.ordinal();

        for (Object extraObject : extraItems) {
            SupportedExtraTypes type = SupportedExtraTypes.getTypeFromObject(extraObject);
            if (type == null) {
                BetterBarrels.log.warn(
                        "Localized Chat Packet has no support for : " + extraObject.getClass().getCanonicalName());
                continue;
            }
            this.extraTypesList.add(type);
            this.extraValuesList.add(extraObject);
        }
        this.extraCount = extraTypesList.size();
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, IBarrelMessage msg, ByteBuf target) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);

        outStream.writeInt(this.messageID);
        outStream.writeInt(this.extraCount);
        for (int i = 0; i < this.extraCount; i++) {
            SupportedExtraTypes type = this.extraTypesList.get(i);

            outStream.writeByte(type.ordinal());

            switch (type) {
                case INT:
                    outStream.writeInt((Integer) this.extraValuesList.get(i));
                    break;
                case STR:
                    outStream.writeUTF((String) this.extraValuesList.get(i));
                    break;
                case FLT:
                    outStream.writeFloat((Float) this.extraValuesList.get(i));
                    break;
            }
        }
        outStream.close();
        target.writeBytes(byteStream.toByteArray());
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IBarrelMessage rawmsg) {
        DataInputStream inStream = new DataInputStream(
                new ByteArrayInputStream(dat.array(), dat.arrayOffset(), dat.capacity()));
        Message0x09LocalizedChat msg = (Message0x09LocalizedChat) rawmsg;

        try {
            msg.messageID = inStream.readInt();
            msg.extraCount = inStream.readInt();

            for (int i = 0; i < this.extraCount; i++) {
                SupportedExtraTypes type = SupportedExtraTypes.getType(inStream.readByte());

                msg.extraTypesList.add(type);

                switch (type) {
                    case INT:
                        msg.extraValuesList.add(inStream.readInt());
                        break;
                    case STR:
                        msg.extraValuesList.add(inStream.readUTF());
                        break;
                    case FLT:
                        msg.extraValuesList.add(inStream.readFloat());
                        break;
                }
            }
        } catch (Throwable t) {}
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message0x09LocalizedChat msg) throws Exception {
        mcp.mobius.betterbarrels.client.ClientChatUtils.printLocalizedMessage(
                LocalizedChat.values()[msg.messageID].localizationKey,
                msg.extraValuesList.toArray());
    }
}
