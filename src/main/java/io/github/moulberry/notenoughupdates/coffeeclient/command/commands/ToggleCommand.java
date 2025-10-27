/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.command.commands;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.command.Command;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ToggleCommand extends Command {

	public ToggleCommand() {
		super(new ArrayList<>(Arrays.asList("toggle", "t")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (args.size() < 2) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Usage: .%s <&omodule&r>&r",
							args.get(0).toLowerCase(Locale.ROOT)));
		} else {
			Module module = CoffeeClient.moduleManager.getModule(args.get(1));
			if (module == null) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r Module not found (&o%s&r)&r", args.get(1)));
			} else {
				boolean changed = true;
				if (args.size() >= 3) {
					if (args.get(2).equalsIgnoreCase("true")
							|| args.get(2).equalsIgnoreCase("on")
							|| args.get(2).equalsIgnoreCase("1")) {
						changed = !module.isEnabled();
					} else if (args.get(2).equalsIgnoreCase("false")
							|| args.get(2).equalsIgnoreCase("off")
							|| args.get(2).equalsIgnoreCase("0")) {
						changed = module.isEnabled();
					}
				}
				if (changed && module.toggle()) {
					ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r %s: %s&r", module.getName(),
							module.isEnabled() ? "&a&lON" : "&c&lOFF"));
				}
			}
		}
	}
}
