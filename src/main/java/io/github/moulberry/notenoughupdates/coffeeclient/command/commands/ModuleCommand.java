/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.command.commands;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.command.Command;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.Property;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleCommand extends Command {

    public ModuleCommand() {
        super(new ArrayList<>(CoffeeClient.moduleManager.modules.values().stream()
                .map(Module::getName)
                .collect(Collectors.toList())));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Module module = CoffeeClient.moduleManager.getModule(args.get(0));
        if (module == null) {
            return;
        }

        if (args.size() >= 2) {
            Property<?> property = CoffeeClient.propertyManager.getProperty(module, args.get(1));
            if (property == null) {
                ChatUtil.sendFormatted(String.format("%s%s has no property &o%s&r", CoffeeClient.CLIENT_NAME,
                        module.getName(), args.get(1)));
            } else if (args.size() < 3 && !(property instanceof BooleanProperty)) {
                ChatUtil.sendFormatted(
                        String.format(
                                "%s%s: &o%s&r is set to %s&r (%s)&r",
                                CoffeeClient.CLIENT_NAME,
                                module.getName(),
                                property.getName(),
                                property.formatValue(),
                                property.getValuePrompt()));
            } else {
                String newValue = args.size() < 3 ? null : String.join(" ", args.subList(2, args.size()));
                try {
                    if (property.parseString(newValue)) {
                        ChatUtil.sendFormatted(
                                String.format("%s%s: &o%s&r has been set to %s&r", CoffeeClient.CLIENT_NAME,
                                        module.getName(), property.getName(), property.formatValue()));
                        return;
                    }
                } catch (Exception e) {
                }
                ChatUtil.sendFormatted(
                        String.format("%sInvalid value for property &o%s&r (%s)&r", CoffeeClient.CLIENT_NAME,
                                property.getName(), property.getValuePrompt()));
            }
        } else {
            List<Property<?>> properties = CoffeeClient.propertyManager.properties.get(module.getClass());
            if (properties != null) {
                List<Property<?>> visible = properties.stream().filter(Property::isVisible)
                        .collect(Collectors.toList());
                if (!visible.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%s%s:&r", CoffeeClient.CLIENT_NAME, module.formatModule()));
                    for (Property<?> property : visible) {
                        ChatUtil.sendFormatted(
                                String.format("&7»&r %s: %s&r", property.getName(), property.formatValue()));
                    }
                    return;
                }
            }
            ChatUtil.sendFormatted(
                    String.format("%s%s has no properties&r", CoffeeClient.CLIENT_NAME, module.formatModule()));
        }
    }
}
