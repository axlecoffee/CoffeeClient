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
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.Property;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Config {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final String name;
	private final File file;
	private final boolean save;

	public static String lastConfig;

	public Config(String name, boolean save) {
		this.save = save;
		String configName = name;
		if (name.equals("!") || name.equals("default")) {
			configName = "default";
		}
		this.name = configName;
		lastConfig = configName;
		File configDir = new File("./config/CoffeeClient/");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		this.file = new File(configDir, this.name + ".json");
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
			for (Feature feature : CoffeeClient.featureManager.features.values()) {
				JsonElement featureObj = jsonObject.get(feature.getName());
				if (featureObj != null && featureObj.isJsonObject()) {
					JsonObject object = featureObj.getAsJsonObject();

					ArrayList<Property<?>> list = CoffeeClient.propertyManager.properties.get(feature.getClass());
					if (list != null) {
						for (Property<?> property : list) {
							if (object.has(property.getName())) {
								try {
									property.read(object);
								} catch (Exception e) {
									CoffeeClient.LOGGER.warn(String.format(
											"Failed to load property %s for feature %s",
											property.getName(), feature.getName()));
								}
							}
						}
					}

					if (object.has("toggled")) {
						JsonElement toggled = object.get("toggled");
						if (toggled != null && toggled.isJsonPrimitive()) {
							boolean enabled = toggled.getAsBoolean();
							if (feature.isEnabled() != enabled) {
								feature.setEnabled(enabled);
							}
						}
					}

					if (object.has("key")) {
						JsonElement key = object.get("key");
						if (key != null && key.isJsonPrimitive()) {
							feature.setKey(key.getAsInt());
						}
					}

					if (object.has("hidden")) {
						JsonElement hidden = object.get("hidden");
						if (hidden != null && hidden.isJsonPrimitive()) {
							feature.setHidden(hidden.getAsBoolean());
						}
					}
				}
			}

			ChatUtil.sendFormatted(String.format("&7[&bCoffeeClient&7]&r Loaded config (&a&o%s&r)&r", file.getName()));
		} catch (Exception e) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Failed to load config (&c&o%s&r)&r", file.getName()));
			CoffeeClient.LOGGER.error("Failed to load config: " + file.getName(), e);
		}
	}

	public void save() {
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}

			JsonObject jsonObject = new JsonObject();

			for (Feature feature : CoffeeClient.featureManager.features.values()) {
				JsonObject featureObj = new JsonObject();
				featureObj.addProperty("toggled", feature.isEnabled());
				featureObj.addProperty("key", feature.getKey());
				featureObj.addProperty("hidden", feature.isHidden());

				ArrayList<Property<?>> list = CoffeeClient.propertyManager.properties.get(feature.getClass());
				if (list != null) {
					for (Property<?> property : list) {
						try {
							property.write(featureObj);
						} catch (Exception e) {
							CoffeeClient.LOGGER.warn(String.format(
									"Failed to save property %s for feature %s",
									property.getName(), feature.getName()));
						}
					}
				}

				jsonObject.add(feature.getName(), featureObj);
			}

			PrintWriter printWriter = new PrintWriter(new FileWriter(file));
			printWriter.println(gson.toJson(jsonObject));
			printWriter.close();

			if (this.save) {
				ChatUtil.sendFormatted(
						String.format("&7[&bCoffeeClient&7]&r Saved config (&a&o%s&r)&r", file.getName()));
			}
		} catch (Exception e) {
			ChatUtil.sendFormatted(
					String.format("&7[&bCoffeeClient&7]&r Failed to save config (&c&o%s&r)&r", file.getName()));
			CoffeeClient.LOGGER.error("Failed to save config: " + file.getName(), e);
		}
	}
}
