package io.github.moulberry.notenoughupdates.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.awt.Color;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isBot(EntityPlayer player) {
        if (player == mc.thePlayer) {
            return false;
        }
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(player.getName());
        if (playerInfo == null) {
            return true;
        }
        if (player.getName().startsWith("\u00a7k")) {
            return player.isInvisible();
        }
        if (playerInfo.getResponseTime() < 1) {
            return true;
        }
        ScorePlayerTeam playerTeam = playerInfo.getPlayerTeam();
        if (playerTeam == null)
            return false;
        if (!playerTeam.getTeamName().isEmpty())
            return false;
        return playerTeam.getColorPrefix().equals("\u00a7c");
    }

    public static Color getTeamColor(EntityPlayer player, float alpha) {
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (playerInfo != null) {
            ScorePlayerTeam playerTeam = playerInfo.getPlayerTeam();
            if (playerTeam != null) {
                String colorPrefix = playerTeam.getColorPrefix();
                if (colorPrefix != null && colorPrefix.length() >= 2 && colorPrefix.startsWith("\u00a7")) {
                    char colorCode = colorPrefix.charAt(1);
                    int color = mc.fontRendererObj.getColorCode(colorCode);
                    return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (int) (alpha * 255));
                }
            }
        }
        return new Color(1.0f, 1.0f, 1.0f, alpha);
    }

    public static String stripName(Entity entity) {
        return entity.getDisplayName().getFormattedText()
                .replaceAll("\u00a7\\S$", "")
                .replaceAll("(?i)\u00a7r", "\u00a7f")
                .trim();
    }

    public static boolean isSameTeam(EntityPlayer player) {
        if (mc.thePlayer == null || player == null) {
            return false;
        }
        return mc.thePlayer.isOnSameTeam(player);
    }
}
