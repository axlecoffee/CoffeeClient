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
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.FeatureCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.HideCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.ShowCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.command.commands.ToggleCommand;
import io.github.moulberry.notenoughupdates.coffeeclient.config.Config;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.FeatureManager;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.combat.*;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.render.*;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.player.*;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.world.*;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.misc.*;
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

	public static FeatureManager featureManager;
	public static CommandManager commandManager;
	public static PropertyManager propertyManager;

	private static boolean initialized = false;

	public static void init() {
		if (initialized) {
			LOGGER.warn("CoffeeClient already initialized!");
			return;
		}

		LOGGER.info("Initializing CoffeeClient...");

		featureManager = new FeatureManager();
		commandManager = new CommandManager();
		propertyManager = new PropertyManager();

		registerFeatures();
		registerCommands();
		discoverProperties();

		MinecraftForge.EVENT_BUS.register(featureManager);
		MinecraftForge.EVENT_BUS.register(commandManager);

		new Config("default", false).load();

		initialized = true;
		LOGGER.info("CoffeeClient initialized successfully!");
	}

	private static void registerFeatures() {
		// Render
		featureManager.registerFeature(HUD.class, new HUD());
		featureManager.registerFeature(ESP.class, new ESP());
		featureManager.registerFeature(NameTags.class, new NameTags());
		featureManager.registerFeature(ItemESP.class, new ItemESP());
		featureManager.registerFeature(BedESP.class, new BedESP());
		featureManager.registerFeature(Bedplates.class, new Bedplates());
		featureManager.registerFeature(Chams.class, new Chams());
		featureManager.registerFeature(Indicators.class, new Indicators());
		featureManager.registerFeature(Trajectories.class, new Trajectories());
		featureManager.registerFeature(FullBright.class, new FullBright());
		featureManager.registerFeature(AntiObfuscate.class, new AntiObfuscate());
		featureManager.registerFeature(AntiDebuff.class, new AntiDebuff());

		// Combat
		featureManager.registerFeature(AimAssist.class, new AimAssist());
		featureManager.registerFeature(AutoClicker.class, new AutoClicker());
		featureManager.registerFeature(KillAura.class, new KillAura());
		featureManager.registerFeature(WTap.class, new WTap());
		featureManager.registerFeature(Velocity.class, new Velocity());
		featureManager.registerFeature(NoHitDelay.class, new NoHitDelay());
		featureManager.registerFeature(AntiFireball.class, new AntiFireball());

		// Player
		featureManager.registerFeature(Eagle.class, new Eagle());
		featureManager.registerFeature(InvWalk.class, new InvWalk());
		featureManager.registerFeature(FastPlace.class, new FastPlace());
		featureManager.registerFeature(AutoTool.class, new AutoTool());
		featureManager.registerFeature(NoJumpDelay.class, new NoJumpDelay());

		// World
		featureManager.registerFeature(BedTracker.class, new BedTracker());

		// Misc
		featureManager.registerFeature(Test.class, new Test());
	}

	private static void registerCommands() {
		commandManager.commands.add(new CoffeeCommand());
		commandManager.commands.add(new BindCommand());
		commandManager.commands.add(new ToggleCommand());
		commandManager.commands.add(new ConfigCommand());
		commandManager.commands.add(new FeatureCommand());
		commandManager.commands.add(new HideCommand());
		commandManager.commands.add(new ShowCommand());
	}

	/**
	 * Discovers properties from all registered features using reflection
	 */
	private static void discoverProperties() {
		for (Feature feature : featureManager.features.values()) {
			ArrayList<Property<?>> featureProperties = new ArrayList<>();

			for (Field field : feature.getClass().getDeclaredFields()) {
				if (Property.class.isAssignableFrom(field.getType())) {
					try {
						field.setAccessible(true);
						Property<?> property = (Property<?>) field.get(feature);
						if (property != null) {
							featureProperties.add(property);
						}
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to access property field: " + field.getName(), e);
					}
				}
			}

			if (!featureProperties.isEmpty()) {
				propertyManager.properties.put(feature.getClass(), featureProperties);
				LOGGER.info("Discovered " + featureProperties.size() + " properties for " + feature.getName());
			}
		}
	}
}
