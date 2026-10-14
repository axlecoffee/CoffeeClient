/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package coffee.axle.coffeeclient.property.properties;

import com.google.gson.JsonObject;
import coffee.axle.coffeeclient.property.Property;

import java.util.function.BooleanSupplier;

public class ModeProperty extends Property<Integer> {

    private final String[] _modes;

    public ModeProperty(String name, Integer value, String[] modes) {
        this(name, value, modes, null);
    }

    public ModeProperty(String name, Integer value, String[] modes, BooleanSupplier check) {
        super(name, value, check);
        this._modes = modes;
    }

    @Override
    public String getValuePrompt() {
        return String.join("/", _modes);
    }

    public String getModeString() {
        return _modes[getValue()];
    }

    @Override
    public String formatValue() {
        return getModeString();
    }

    @Override
    public boolean parseString(String string) {
        String valueStr = string.replace("_", "");
        for (int i = 0; i < _modes.length; i++) {
            if (valueStr.equalsIgnoreCase(_modes[i].replace("_", ""))) {
                return setValue(i);
            }
        }
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return parseString(jsonObject.get(getName()).getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(getName(), getModeString());
    }

    public void nextMode() {
        int current = getValue();
        int next = current + 1;
        if (next >= _modes.length) {
            next = 0;
        }
        setValue(next);
    }
}
