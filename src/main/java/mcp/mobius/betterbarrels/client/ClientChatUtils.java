package mcp.mobius.betterbarrels.client;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientChatUtils {

    private static final char sectionChar = 0x00A7;

    private static EnumChatFormatting getFormattingFromChar(char c) {
        for (EnumChatFormatting format : EnumChatFormatting.values()) {
            if (format.getFormattingCode() == c) return format;
        }
        return EnumChatFormatting.RESET;
    }

    private static void applyStyleFormat(ChatStyle style, EnumChatFormatting format) {
        switch (format) {
            case BLACK:
            case DARK_BLUE:
            case DARK_GREEN:
            case DARK_AQUA:
            case DARK_RED:
            case DARK_PURPLE:
            case GOLD:
            case GRAY:
            case DARK_GRAY:
            case BLUE:
            case GREEN:
            case AQUA:
            case RED:
            case LIGHT_PURPLE:
            case YELLOW:
            case WHITE:
                style.setColor(format);
                break;
            case OBFUSCATED:
                style.setObfuscated(true);
                break;
            case BOLD:
                style.setBold(true);
                break;
            case STRIKETHROUGH:
                style.setStrikethrough(true);
                break;
            case UNDERLINE:
                style.setUnderlined(true);
                break;
            case ITALIC:
                style.setItalic(true);
                break;
            case RESET:
                style.setColor(null);
                style.setObfuscated(null);
                style.setBold(null);
                style.setStrikethrough(null);
                style.setUnderlined(null);
                style.setItalic(null);
                break;
        }
    }

    private static void createChatComponent(IChatComponent parent, String piece) {
        int formatIdx = piece.indexOf(sectionChar);

        if (formatIdx >= 0) {
            // First add the unformatted part to the final message
            if (formatIdx != 0) {
                parent.appendSibling(new ChatComponentText(piece.substring(0, formatIdx)));
            }

            // now to reverse determine the chatsyle formatting for the remaining
            ChatStyle style = new ChatStyle();
            String formattedPiece = piece.substring(formatIdx);

            int codePos;
            for (codePos = 1; codePos < formattedPiece.length(); codePos++) {
                applyStyleFormat(style, getFormattingFromChar(formattedPiece.charAt(codePos)));

                if (codePos + 2 < formattedPiece.length() && formattedPiece.charAt(++codePos) != sectionChar) {
                    break;
                }
            }

            String rest = formattedPiece.substring(codePos);
            int endFormatIdx = rest.indexOf(sectionChar);
            boolean childParent = false;
            if (endFormatIdx >= 0) {
                if (endFormatIdx + 2 <= rest.length()) {
                    if (rest.charAt(endFormatIdx + 1) == 'r') {
                        // endFormatIdx += 2;
                    } else {
                        childParent = true;
                        endFormatIdx--;
                    }
                }
            } else {
                endFormatIdx = rest.length();
            }

            ChatComponentText newChild = new ChatComponentText(rest.substring(0, endFormatIdx));
            newChild.setChatStyle(style);

            parent.appendSibling(newChild);

            if (endFormatIdx < rest.length() - 1) {
                createChatComponent(childParent ? newChild : parent, rest.substring(endFormatIdx));
            }
        } else {
            parent.appendSibling(new ChatComponentText(piece));
        }
    }

    public static void printLocalizedMessage(String key, Object... extraItems) {
        ArrayList<IChatComponent> translatedPieces = Lists.newArrayList(new ChatComponentTranslation(key, extraItems));

        ChatComponentText finalMessage = new ChatComponentText("");

        StringBuilder translatedMessage = new StringBuilder();
        for (IChatComponent chatPiece : translatedPieces) {
            translatedMessage.append(chatPiece.getUnformattedTextForChat());
        }
        ClientChatUtils.createChatComponent(finalMessage, translatedMessage.toString());

        Minecraft.getMinecraft().thePlayer.addChatMessage(finalMessage);
    }
}
