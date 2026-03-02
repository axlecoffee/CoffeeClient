package io.github.moulberry.notenoughupdates.coffeeclient.feature;

import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;

public abstract class Feature {

    protected final String _name;
    protected final boolean _defaultEnabled;
    protected final int _defaultKey;
    protected final boolean _defaultHidden;
    protected boolean _enabled;
    protected int _key;
    protected boolean _hidden;

    public Feature(String name, boolean enabled) {
        this(name, enabled, false);
    }

    public Feature(String name, boolean enabled, boolean hidden) {
        this._name = name;
        this._enabled = this._defaultEnabled = enabled;
        this._key = this._defaultKey = 0;
        this._hidden = this._defaultHidden = hidden;
    }

    public String getName() {
        return this._name;
    }

    public String formatFeature() {
        return String.format(
                "%s%s &r(%s&r)",
                this._key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this._key)),
                this._name,
                this._enabled ? "&a&lON" : "&c&lOFF");
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this._enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this._enabled != enabled) {
            this._enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        boolean enabled = !this._enabled;
        this.setEnabled(enabled);
        return this._enabled == enabled;
    }

    public int getKey() {
        return this._key;
    }

    public void setKey(int key) {
        this._key = key;
    }

    public boolean isHidden() {
        return this._hidden;
    }

    public void setHidden(boolean hidden) {
        this._hidden = hidden;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String propertyName) {
    }
}
