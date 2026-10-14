/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.command.commands;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.command.Command;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class CoffeeCommand extends Command {

	public CoffeeCommand() {
		super(new ArrayList<>(Arrays.asList("coffee", "list", "l")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (!CoffeeClient.featureManager.features.isEmpty()) {
			ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Features:&r");
			for (Feature feature : CoffeeClient.featureManager.features.values()) {
				ChatUtil.sendFormatted(
						String.format("%s»&r %s&r", feature.isHidden() ? "&8" : "&7", feature.formatFeature()));
			}
		}
	}
}
