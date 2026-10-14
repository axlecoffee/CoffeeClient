package coffee.axle.coffeeclient.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class KeyEvent extends Event {
    private final int key;

    public KeyEvent() {
        this(0);
    }

    public KeyEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
