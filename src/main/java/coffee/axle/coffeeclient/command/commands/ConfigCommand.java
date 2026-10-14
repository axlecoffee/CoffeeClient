/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.command.commands;

import coffee.axle.coffeeclient.config.Config;
import coffee.axle.coffeeclient.command.Command;
import coffee.axle.coffeeclient.util.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ConfigCommand extends Command {

	private static final FileFilter FILE_FILTER = new WildcardFileFilter("*.json");

	public ConfigCommand() {
		super(new ArrayList<>(Arrays.asList("config", "cfg", "c")));
	}

	@Override
	public void runCommand(ArrayList<String> args) {
		if (args.size() < 2) {
			String command = args.get(0).toLowerCase(Locale.ROOT);
			ChatUtil.sendFormatted(
					String.format(
							"&7[&bCoffeeClient&7]&r Usage: .%s &oload&r/&osave&r <&oname&r> | .%s &olist&r | .%s &ofolder&r",
							command, command, command));
		} else {
			String subCommand = args.get(1);
			if (subCommand.equalsIgnoreCase("l")) {
				subCommand = args.size() < 3 ? "list" : "load";
			}
			String sub = subCommand.toLowerCase(Locale.ROOT);
			switch (sub) {
				case "load":
				case "reload":
					if (args.size() < 3) {
						ChatUtil.sendFormatted(
								String.format(
										"&7[&bCoffeeClient&7]&r Missing config name (use '&odefault&r' or '&o!&r' to load default config)&r"));
						return;
					}
					new Config(args.get(2), false).load();
					return;
				case "s":
				case "save":
					if (args.size() < 3) {
						new Config("default", true).save();
						return;
					}
					new Config(args.get(2), true).save();
					return;
				case "list":
					try {
						File[] configs = new File("./config/CoffeeClient/").listFiles(FILE_FILTER);
						if (configs == null) {
							throw new Exception();
						}
						if (configs.length == 0) {
							ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r No configs found (&o%s&r)&r",
									"./config/CoffeeClient/"));
						}
						Arrays.sort(configs, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
						ChatUtil.sendFormatted("&7[&bCoffeeClient&7]&r Configs:&r");
						for (File file : configs) {
							String formatted = ChatUtil.formatColor(String.format("&7»&r &o%s&r", file.getName()));
							String config = String.format(".config load %s",
									FilenameUtils.removeExtension(file.getName()));
							ChatComponentText component = new ChatComponentText(formatted);
							component.setChatStyle(
									new ChatStyle()
											.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, config))
											.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new ChatComponentText(config))));
							ChatUtil.send(component);
						}
					} catch (Exception e) {
						ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r Failed to read (&o%s&r)&r",
								"./config/CoffeeClient/"));
					}
					return;
				case "f":
				case "folder":
				case "dir":
				case "directory":
					try {
						Desktop.getDesktop().open(new File("./config/CoffeeClient/"));
					} catch (Exception e) {
						ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r Failed to open (&o%s&r)&r",
								"./config/CoffeeClient/"));
					}
					return;
				default:
					ChatUtil.sendFormatted(
							String.format("&7[&bCoffeeClient&7]&r Invalid argument (&o%s&r)&r", args.get(1)));
			}
		}
	}
}
