/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.property;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyManager {

    public LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public Property<?> getProperty(Module module, String string) {
        ArrayList<Property<?>> props = properties.get(module.getClass());
        if (props == null) {
            return null;
        }
        for (Property<?> property : props) {
            if (property.getName().replace("-", "").equalsIgnoreCase(string.replace("-", ""))) {
                return property;
            }
        }
        return null;
    }
}
