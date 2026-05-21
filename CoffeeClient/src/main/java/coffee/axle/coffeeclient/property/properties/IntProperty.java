/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package coffee.axle.coffeeclient.property.properties;

import com.google.gson.JsonObject;
import coffee.axle.coffeeclient.property.Property;

import java.util.function.BooleanSupplier;

public class IntProperty extends Property<Integer> {

    private final int _min;
    private final int _max;

    public IntProperty(String name, Integer value, int min, int max) {
        this(name, value, min, max, null);
    }

    public IntProperty(String name, Integer value, int min, int max, BooleanSupplier booleanSupplier) {
        super(name, value, booleanSupplier);
        this._min = min;
        this._max = max;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%d-%d", _min, _max);
    }

    @Override
    public String formatValue() {
        return String.valueOf(getValue());
    }

    @Override
    public boolean parseString(String string) {
        try {
            int value = Integer.parseInt(string);
            if (value >= _min && value <= _max) {
                return setValue(value);
            }
        } catch (NumberFormatException e) {
        }
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return setValue(jsonObject.get(getName()).getAsInt());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(getName(), getValue());
    }
}
