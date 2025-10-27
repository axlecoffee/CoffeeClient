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

public class CoffeeCommand extends Command {

	public CoffeeCommand() {
		super(new ArrayList<>(Arrays.asList("coffee", "list", "l")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (!CoffeeClient.moduleManager.modules.isEmpty()) {
			ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Modules:&r");
			for (Module module : CoffeeClient.moduleManager.modules.values()) {
				ChatUtil.sendFormatted(
						String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
			}
		}
	}
}
