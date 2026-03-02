/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.command.commands;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.command.Command;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class HideCommand extends Command {

	public HideCommand() {
		super(new ArrayList<>(Arrays.asList("hide", "h")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (args.size() < 2) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Usage: .%s <&ofeature&r>&r",
							args.get(0).toLowerCase(Locale.ROOT)));
		} else if (!args.get(1).equals("*")) {
			Feature feature = CoffeeClient.featureManager.getFeature(args.get(1));
			if (feature == null) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r Feature &o%s&r not found&r", args.get(1)));
			} else if (feature.isHidden()) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r &o%s&r is already hidden in HUD&r", feature.getName()));
			} else {
				feature.setHidden(true);
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r &o%s&r has been hidden in HUD&r", feature.getName()));
			}
		} else {
			for (Feature feature : CoffeeClient.featureManager.features.values()) {
				feature.setHidden(true);
			}
			ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r All features have been hidden in HUD&r");
		}
	}
}
