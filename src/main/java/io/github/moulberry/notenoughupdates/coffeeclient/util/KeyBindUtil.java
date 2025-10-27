/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.util;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeyBindUtil {

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
}
