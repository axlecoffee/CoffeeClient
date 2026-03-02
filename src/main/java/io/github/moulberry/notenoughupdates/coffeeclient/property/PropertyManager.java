/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.property;

import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyManager {

    public LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public Property<?> getProperty(Feature feature, String string) {
        ArrayList<Property<?>> props = properties.get(feature.getClass());
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
