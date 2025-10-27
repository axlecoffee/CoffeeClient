/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient;

import io.github.moulberry.notenoughupdates.coffeeclient.command.CommandManager;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.BindCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.CoffeeCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.ConfigCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.ModuleCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.ToggleCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.config.Config;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.module.ModuleManager;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.HUDModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.TestModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.ESPModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.NameTagsModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.ItemESPModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.BedESPModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.NoHitDelayModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.IndicatorsModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.NoJumpDelayModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.ChamsModule;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.BedTrackerModule;
import io.github.moulberry.notenoughupdates.coffeeclient.property.Property;
import io.github.moulberry.notenoughupdates.coffeeclient.property.PropertyManager;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class CoffeeClient {

	public static final String CLIENT_NAME = "&7[&bCoffeeClient&7]&r ";
	public static final Logger LOGGER = LogManager.getLogger("CoffeeClient");

	public static ModuleManager moduleManager;
	public static CommandManager commandManager;
	public static PropertyManager propertyManager;

	private static boolean initialized = false;

	public static void init() {
		if (initialized) {
			LOGGER.warn("CoffeeClient already initialized!");
			return;
		}

		LOGGER.info("Initializing CoffeeClient...");

		moduleManager = new ModuleManager();
		commandManager = new CommandManager();
		propertyManager = new PropertyManager();

		registerModules();
		registerCommands();
		discoverProperties();

		MinecraftForge.EVENT_BUS.register(moduleManager);
		MinecraftForge.EVENT_BUS.register(commandManager);

		new Config("default", false).load();

		initialized = true;
		LOGGER.info("CoffeeClient initialized successfully!");
	}

	private static void registerModules() {
		moduleManager.registerModule(HUDModule.class, new HUDModule());
		moduleManager.registerModule(TestModule.class, new TestModule());
		moduleManager.registerModule(ESPModule.class, new ESPModule());
		moduleManager.registerModule(NameTagsModule.class, new NameTagsModule());
		moduleManager.registerModule(ItemESPModule.class, new ItemESPModule());
		moduleManager.registerModule(BedESPModule.class, new BedESPModule());
		moduleManager.registerModule(NoHitDelayModule.class, new NoHitDelayModule());
		moduleManager.registerModule(IndicatorsModule.class, new IndicatorsModule());
		moduleManager.registerModule(NoJumpDelayModule.class, new NoJumpDelayModule());
		moduleManager.registerModule(ChamsModule.class, new ChamsModule());
		moduleManager.registerModule(BedTrackerModule.class, new BedTrackerModule());
	}

	private static void registerCommands() {
		commandManager.commands.add(new CoffeeCommand());
		commandManager.commands.add(new BindCommand());
		commandManager.commands.add(new ToggleCommand());
		commandManager.commands.add(new ConfigCommand());
		commandManager.commands.add(new ModuleCommand());
	}

	/**
	 * Discovers properties from all registered modules using reflection
	 */
	private static void discoverProperties() {
		for (Module module : moduleManager.modules.values()) {
			ArrayList<Property<?>> moduleProperties = new ArrayList<>();

			for (Field field : module.getClass().getDeclaredFields()) {
				if (Property.class.isAssignableFrom(field.getType())) {
					try {
						field.setAccessible(true);
						Property<?> property = (Property<?>) field.get(module);
						if (property != null) {
							moduleProperties.add(property);
						}
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to access property field: " + field.getName(), e);
					}
				}
			}

			if (!moduleProperties.isEmpty()) {
				propertyManager.properties.put(module.getClass(), moduleProperties);
				LOGGER.info("Discovered " + moduleProperties.size() + " properties for " + module.getName());
			}
		}
	}
}
