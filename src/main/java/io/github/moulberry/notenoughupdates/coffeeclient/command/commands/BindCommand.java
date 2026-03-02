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
import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BindCommand extends Command {

	public BindCommand() {
		super(new ArrayList<>(Arrays.asList("bind", "b")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (args.size() < 3) {
			if (args.size() == 2 && (args.get(1).equalsIgnoreCase("l") || args.get(1).equalsIgnoreCase("list"))) {
				List<Feature> features = CoffeeClient.featureManager.features.values().stream()
						.filter(feature -> feature.getKey() != 0)
						.collect(Collectors.toList());
				if (features.isEmpty()) {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r No binds&r");
				} else {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Binds:&r");
					for (Feature feature : features) {
						ChatUtil.sendFormatted(
								String.format("%s»&r %s&r", feature.isHidden() ? "&8" : "&7", feature.formatFeature()));
					}
				}
			} else {
				ChatUtil.sendFormatted(
						String.format(
								"&7[&bCoffeeClient&7]&r Usage: .%s <&ofeature&r> <&okey&r>&r | .%s <&ofeature&r> &onone&r | .%s &olist&r",
								args.get(0).toLowerCase(Locale.ROOT),
								args.get(0).toLowerCase(Locale.ROOT),
								args.get(0).toLowerCase(Locale.ROOT)));
			}
		} else {
			String keyInput = args.get(2).toUpperCase();
			int keyIndex = KeyBindUtil.getKeyCode(keyInput);

			if (!args.get(1).equals("*")) {
				Feature feature = CoffeeClient.featureManager.getFeature(args.get(1));
				if (feature == null) {
					ChatUtil.sendFormatted(
							String.format("&7[&bCoffeeClient&7]&r Feature not found (&o%s&r)&r", args.get(1)));
				} else {
					feature.setKey(keyIndex);
					if (keyIndex == 0) {
						ChatUtil.sendFormatted(
								String.format("&7[&bCoffeeClient&7]&r Unbound &o%s&r", feature.getName()));
					} else {
						ChatUtil.sendFormatted(
								String.format("&7[&bCoffeeClient&7]&r Bound &o%s&r to &l[%s]&r", feature.getName(),
										KeyBindUtil.getKeyName(keyIndex)));
					}
				}
			} else {
				for (Feature feature : CoffeeClient.featureManager.features.values()) {
					feature.setKey(keyIndex);
				}
				if (keyIndex == 0) {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Unbound all features&r");
				} else {
					ChatUtil.sendFormatted(
							String.format("&7[&bCoffeeClient&7]&r Bound all features to &l[%s]&r",
									KeyBindUtil.getKeyName(keyIndex)));
				}
			}
		}
	}
}
