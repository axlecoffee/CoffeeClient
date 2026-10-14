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
import java.util.Locale;

public class ToggleCommand extends Command {

	public ToggleCommand() {
		super(new ArrayList<>(Arrays.asList("toggle", "t")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (args.size() < 2) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Usage: .%s <&ofeature&r>&r",
							args.get(0).toLowerCase(Locale.ROOT)));
		} else {
			Feature feature = CoffeeClient.featureManager.getFeature(args.get(1));
			if (feature == null) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r Feature not found (&o%s&r)&r", args.get(1)));
			} else {
				boolean changed = true;
				if (args.size() >= 3) {
					if (args.get(2).equalsIgnoreCase("true")
							|| args.get(2).equalsIgnoreCase("on")
							|| args.get(2).equalsIgnoreCase("1")) {
						changed = !feature.isEnabled();
					} else if (args.get(2).equalsIgnoreCase("false")
							|| args.get(2).equalsIgnoreCase("off")
							|| args.get(2).equalsIgnoreCase("0")) {
						changed = feature.isEnabled();
					}
				}
				if (changed && feature.toggle()) {
					ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r %s: %s&r", feature.getName(),
							feature.isEnabled() ? "&a&lON" : "&c&lOFF"));
				}
			}
		}
	}
}
