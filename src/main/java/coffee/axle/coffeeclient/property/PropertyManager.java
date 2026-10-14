/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package coffee.axle.coffeeclient.property;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.Feature;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyManager {

    public LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public PropertyManager(Iterable<Feature> features) {
        for (Feature feature : features) {
            ArrayList<Property<?>> featureProperties = new ArrayList<>();
            for (Field field : feature.getClass().getDeclaredFields()) {
                if (Property.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Property<?> property = (Property<?>) field.get(feature);
                        if (property != null) {
                            property.setOwner(feature);
                            featureProperties.add(property);
                        }
                    } catch (IllegalAccessException e) {
                        CoffeeClient.LOGGER.error("Failed to access property field: " + field.getName(), e);
                    }
                }
            }
            if (!featureProperties.isEmpty()) {
                properties.put(feature.getClass(), featureProperties);
            }
        }
    }

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
