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
				List<Module> modules = CoffeeClient.moduleManager.modules.values().stream()
						.filter(module -> module.getKey() != 0)
						.collect(Collectors.toList());
				if (modules.isEmpty()) {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r No binds&r");
				} else {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Binds:&r");
					for (Module module : modules) {
						ChatUtil.sendFormatted(
								String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
					}
				}
			} else {
				ChatUtil.sendFormatted(
						String.format(
								"&7[&bCoffeeClient&7]&r Usage: .%s <&omodule&r> <&okey&r>&r | .%s <&omodule&r> &onone&r | .%s &olist&r",
								args.get(0).toLowerCase(Locale.ROOT),
								args.get(0).toLowerCase(Locale.ROOT),
								args.get(0).toLowerCase(Locale.ROOT)));
			}
		} else {
			String keyInput = args.get(2).toUpperCase();
			int keyIndex = KeyBindUtil.getKeyCode(keyInput);

			if (!args.get(1).equals("*")) {
				Module module = CoffeeClient.moduleManager.getModule(args.get(1));
				if (module == null) {
					ChatUtil.sendFormatted(
							String.format("&7[&bCoffeeClient&7]&r Module not found (&o%s&r)&r", args.get(1)));
				} else {
					module.setKey(keyIndex);
					if (keyIndex == 0) {
						ChatUtil.sendFormatted(
								String.format("&7[&bCoffeeClient&7]&r Unbound &o%s&r", module.getName()));
					} else {
						ChatUtil.sendFormatted(
								String.format("&7[&bCoffeeClient&7]&r Bound &o%s&r to &l[%s]&r", module.getName(),
										KeyBindUtil.getKeyName(keyIndex)));
					}
				}
			} else {
				for (Module module : CoffeeClient.moduleManager.modules.values()) {
					module.setKey(keyIndex);
				}
				if (keyIndex == 0) {
					ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Unbound all modules&r");
				} else {
					ChatUtil.sendFormatted(
							String.format("&7[&bCoffeeClient&7]&r Bound all modules to &l[%s]&r",
									KeyBindUtil.getKeyName(keyIndex)));
				}
			}
		}
	}
}
