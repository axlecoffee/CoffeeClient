/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.LinkedHashMap;

public class ModuleManager {

	private static final Minecraft mc = Minecraft.getMinecraft();
	public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

	public Module getModule(String name) {
		for (Module module : this.modules.values()) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}

	public Module getModule(Class<?> clazz) {
		return this.modules.get(clazz);
	}

	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		if (mc.currentScreen == null) {
			int keyCode = Keyboard.getEventKey();
			if (keyCode != 0 && Keyboard.getEventKeyState()) {
				for (Module module : this.modules.values()) {
					if (module.getKey() == keyCode) {
						module.toggle();
					}
				}
			}
		}
	}

	public void registerModule(Class<?> clazz, Module module) {
		this.modules.put(clazz, module);
		MinecraftForge.EVENT_BUS.register(module);
	}
}
