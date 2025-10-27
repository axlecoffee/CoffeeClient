/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.property.properties;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.coffeeclient.property.Property;

import java.util.function.BooleanSupplier;

public class BooleanProperty extends Property<Boolean> {

    public BooleanProperty(String name, Boolean value) {
        this(name, value, null);
    }

    public BooleanProperty(String name, Boolean value, BooleanSupplier booleanSupplier) {
        super(name, value, booleanSupplier);
    }

    @Override
    public String getValuePrompt() {
        return "true/false";
    }

    @Override
    public String formatValue() {
        return getValue() ? "&atrue" : "&cfalse";
    }

    @Override
    public boolean parseString(String string) {
        if (string == null) {
            return setValue(!getValue());
        } else if (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("on") || string.equalsIgnoreCase("1")) {
            return setValue(true);
        } else {
            return (string.equalsIgnoreCase("false") || string.equalsIgnoreCase("off") || string.equalsIgnoreCase("0"))
                    && setValue(false);
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return setValue(jsonObject.get(getName()).getAsBoolean());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(getName(), getValue());
    }
}
