/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class ChatUtil {

	private static final Minecraft mc = Minecraft.getMinecraft();

	public static void sendChatMessage(String message) {
		if (mc.thePlayer != null) {
			mc.thePlayer.sendChatMessage(message);
		}
	}

	public static String formatColor(String text) {
		return text.replace("&", "§");
	}

	public static void send(ChatComponentText text) {
		if (mc.thePlayer != null) {
			mc.thePlayer.addChatMessage(text);
		}
	}

	public static void sendFormatted(String text) {
		send(new ChatComponentText(formatColor(text)));
	}

	public static void sendRaw(String text) {
		send(new ChatComponentText(text));
	}

	public static void sendMessage(String message) {
		sendFormatted("&7[&bCoffeeClient&7]&r " + message);
	}
}
