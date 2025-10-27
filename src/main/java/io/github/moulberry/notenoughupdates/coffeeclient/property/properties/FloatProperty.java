/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdated.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.property.properties;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.coffeeclient.property.Property;

import java.util.function.BooleanSupplier;

public class FloatProperty extends Property<Float> {

    private final float _min;
    private final float _max;

    public FloatProperty(String name, Float value, float min, float max) {
        this(name, value, min, max, null);
    }

    public FloatProperty(String name, Float value, float min, float max, BooleanSupplier booleanSupplier) {
        super(name, value, booleanSupplier);
        this._min = min;
        this._max = max;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%.2f-%.2f", _min, _max);
    }

    @Override
    public String formatValue() {
        return String.format("%.2f", getValue());
    }

    @Override
    public boolean parseString(String string) {
        try {
            float value = Float.parseFloat(string);
            if (value >= _min && value <= _max) {
                return setValue(value);
            }
        } catch (NumberFormatException e) {
        }
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return setValue(jsonObject.get(getName()).getAsFloat());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(getName(), getValue());
    }
}
