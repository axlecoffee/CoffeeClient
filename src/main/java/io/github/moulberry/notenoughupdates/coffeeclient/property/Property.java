/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.property;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public abstract class Property<T> {

    private final String _name;
    private final Predicate<T> _validator;
    private final BooleanSupplier _visibleChecker;
    private T _value;
    private Feature _owner;

    protected Property(String name, Object value, BooleanSupplier visibleChecker) {
        this(name, value, null, visibleChecker);
    }

    protected Property(String name, Object value, Predicate<T> predicate, BooleanSupplier visibleChecker) {
        this._name = name;
        this._validator = predicate;
        this._visibleChecker = visibleChecker;
        this._value = (T) value;
        this._owner = null;
    }

    public String getName() {
        return this._name;
    }

    public abstract String getValuePrompt();

    public boolean isVisible() {
        return this._visibleChecker == null || this._visibleChecker.getAsBoolean();
    }

    public T getValue() {
        return this._value;
    }

    public abstract String formatValue();

    public boolean setValue(Object object) {
        if (this._validator != null && !this._validator.test((T) object)) {
            return false;
        } else {
            this._value = (T) object;
            if (this._owner != null) {
                this._owner.verifyValue(this._name);
            }
            return true;
        }
    }

    public void setOwner(Feature feature) {
        this._owner = feature;
    }

    public abstract boolean parseString(String string);

    public abstract boolean read(JsonObject jsonObject);

    public abstract void write(JsonObject jsonObject);
}
