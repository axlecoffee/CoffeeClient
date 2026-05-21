/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.property.properties;

import com.google.gson.JsonObject;
import coffee.axle.coffeeclient.property.Property;

import java.util.function.BooleanSupplier;

public class TextProperty extends Property<String> {
    public TextProperty(String name, String value) {
        this(name, value, null);
    }

    public TextProperty(String name, String value, BooleanSupplier dependency) {
        super(name, value, dependency);
    }

    @Override
    public String getValuePrompt() {
        return "text";
    }

    @Override
    public String formatValue() {
        return String.format("&f%s", getValue());
    }

    @Override
    public boolean parseString(String string) {
        return setValue(string);
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return parseString(jsonObject.get(getName()).getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(getName(), getValue());
    }
}
