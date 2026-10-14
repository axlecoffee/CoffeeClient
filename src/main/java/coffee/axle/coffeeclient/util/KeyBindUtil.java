/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.util;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Map;

public class KeyBindUtil {
	private static final Map<Integer, Boolean> keyStates = new HashMap<>();

	public static String getKeyName(int keyCode) {
		if (keyCode < 0) {
			return "MOUSE" + (keyCode + 100);
		} else if (keyCode == 0) {
			return "NONE";
		} else {
			String keyName = Keyboard.getKeyName(keyCode);
			return keyName != null ? keyName : "UNKNOWN";
		}
	}

	public static int getKeyCode(String keyName) {
		if (keyName == null || keyName.isEmpty()) {
			return 0;
		}

		String upperKey = keyName.toUpperCase();

		if (upperKey.equals("NONE") || upperKey.equals("NULL")) {
			return 0;
		}

		int keyCode = Keyboard.getKeyIndex(upperKey);

		if (keyCode == 0) {
			int mouseButton = Mouse.getButtonIndex(upperKey);
			if (mouseButton != -1) {
				keyCode = mouseButton - 100;
			}
		}

		return keyCode;
	}

	public static boolean isKeyDown(int keyCode) {
		if (keyCode < 0) {
			return Mouse.isButtonDown(keyCode + 100);
		} else if (keyCode > 0) {
			return Keyboard.isKeyDown(keyCode);
		}
		return false;
	}

	public static void updateKeyState(int keyCode) {
		boolean currentState = isKeyDown(keyCode);
		keyStates.put(keyCode, currentState);
	}

	public static void setKeyBindState(int keyCode, boolean pressed) {
		keyStates.put(keyCode, pressed);
		KeyBinding.setKeyBindState(keyCode, pressed);
	}

	public static void pressKeyOnce(int keyCode) {
		KeyBinding.onTick(keyCode);
	}
}
