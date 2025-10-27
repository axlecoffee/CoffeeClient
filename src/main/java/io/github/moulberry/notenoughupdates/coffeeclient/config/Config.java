/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final File file;
	private final boolean save;

	public Config(String name, boolean save) {
		this.save = save;
		File configDir = new File("./config/CoffeeClient/");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		this.file = new File(configDir, name + ".json");
	}

	public void load() {
		try {
			if (!file.exists()) {
				ChatUtil.sendFormatted(String.format(
						"&7[&bCoffeeClient&7]&r Config file not found (&c&o%s&r). Creating default config...&r",
						file.getName()));
				save();
				return;
			}

			JsonElement parsed = new JsonParser().parse(new BufferedReader(new FileReader(file)));
			if (parsed == null || !parsed.isJsonObject()) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r Invalid config format (&c&o%s&r)&r", file.getName()));
				return;
			}

			JsonObject jsonObject = parsed.getAsJsonObject();
			for (Module module : CoffeeClient.moduleManager.modules.values()) {
				JsonElement moduleObj = jsonObject.get(module.getName());
				if (moduleObj != null && moduleObj.isJsonObject()) {
					JsonObject object = moduleObj.getAsJsonObject();

					if (object.has("toggled")) {
						JsonElement toggled = object.get("toggled");
						if (toggled != null && toggled.isJsonPrimitive()) {
							boolean enabled = toggled.getAsBoolean();
							if (module.isEnabled() != enabled) {
								module.setEnabled(enabled);
							}
						}
					}

					if (object.has("key")) {
						JsonElement key = object.get("key");
						if (key != null && key.isJsonPrimitive()) {
							module.setKey(key.getAsInt());
						}
					}

					if (object.has("hidden")) {
						JsonElement hidden = object.get("hidden");
						if (hidden != null && hidden.isJsonPrimitive()) {
							module.setHidden(hidden.getAsBoolean());
						}
					}
				}
			}

			ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r Loaded config (&o%s&r)&r", file.getName()));
		} catch (Exception e) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Failed to load config (&c&o%s&r)&r", file.getName()));
			CoffeeClient.LOGGER.error("Failed to load config: " + file.getName(), e);
		}
	}

	public void save() {
		try {
			JsonObject jsonObject = new JsonObject();

			for (Module module : CoffeeClient.moduleManager.modules.values()) {
				JsonObject moduleObj = new JsonObject();
				moduleObj.addProperty("toggled", module.isEnabled());
				moduleObj.addProperty("key", module.getKey());
				moduleObj.addProperty("hidden", module.isHidden());
				jsonObject.add(module.getName(), moduleObj);
			}

			FileWriter writer = new FileWriter(file);
			writer.write(gson.toJson(jsonObject));
			writer.close();

			if (this.save) {
				ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r Saved config (&o%s&r)&r", file.getName()));
			}
		} catch (Exception e) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Failed to save config (&c&o%s&r)&r", file.getName()));
			CoffeeClient.LOGGER.error("Failed to save config: " + file.getName(), e);
		}
	}
}
