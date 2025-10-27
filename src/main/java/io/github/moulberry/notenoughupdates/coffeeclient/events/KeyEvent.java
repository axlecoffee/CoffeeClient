package io.github.moulberry.notenoughupdates.coffeeclient.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class KeyEvent extends Event {
    private final int key;

    public KeyEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
