/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.command;

import coffee.axle.coffeeclient.CoffeeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {

	private static final Minecraft mc = Minecraft.getMinecraft();
	public final List<Command> commands = new ArrayList<>();

	private Field inputField = null;
	private Field sentHistoryField = null;

	public CommandManager() {
		try {
			inputField = GuiChat.class.getDeclaredField("field_146415_a");
			inputField.setAccessible(true);
		} catch (Exception e) {
			try {
				inputField = GuiChat.class.getDeclaredField("inputField");
				inputField.setAccessible(true);
			} catch (Exception ex) {
				CoffeeClient.LOGGER.error("Failed to get GuiChat inputField", ex);
			}
		}

		try {
			sentHistoryField = GuiChat.class.getDeclaredField("field_146416_h");
			sentHistoryField.setAccessible(true);
		} catch (Exception e) {
			try {
				sentHistoryField = GuiChat.class.getDeclaredField("sentHistoryCursor");
				sentHistoryField.setAccessible(true);
			} catch (Exception ex) {
				CoffeeClient.LOGGER.error("Failed to get GuiChat sentHistoryCursor", ex);
			}
		}
	}

	private void addToHistory(String command) {
		try {
			if (mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
				mc.ingameGUI.getChatGUI().addToSentMessages(command);
			}
		} catch (Exception e) {
			CoffeeClient.LOGGER.error("Failed to add command to history", e);
		}
	}

	public void handleCommand(String msg) {
		String[] split = msg.substring(1).split(" ");
		ArrayList<String> args = new ArrayList<>(Arrays.asList(split));

		if (args.isEmpty()) {
			return;
		}

		String commandName = args.get(0).toLowerCase();

		for (Command command : this.commands) {
			for (String name : command.names) {
				if (name.equalsIgnoreCase(commandName)) {
					try {
						command.runCommand(args);
					} catch (Exception e) {
						CoffeeClient.LOGGER.error("Error running command: " + commandName, e);
					}
					return;
				}
			}
		}
	}

	public boolean isTypingCommand(String msg) {
		if (msg == null || msg.length() < 2) {
			return false;
		}
		return msg.charAt(0) == '.' && Character.isLetterOrDigit(msg.charAt(1));
	}

	@SubscribeEvent
	public void onKeyTyped(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		if (mc.currentScreen instanceof GuiChat && Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
			try {
				if (inputField != null) {
					GuiTextField textField = (GuiTextField) inputField.get(mc.currentScreen);
					String message = textField.getText();
					if (isTypingCommand(message)) {
						addToHistory(message);
						handleCommand(message);
						mc.displayGuiScreen(null);
						event.setCanceled(true);
					}
				}
			} catch (Exception e) {
				CoffeeClient.LOGGER.error("Error intercepting chat command", e);
			}
		}
	}
}
