package coffee.axle.coffeeclient.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class UpdateEvent extends Event {
    private final boolean pre;

    public UpdateEvent(boolean pre) {
        this.pre = pre;
    }

    public boolean isPre() {
        return pre;
    }

    public boolean isPost() {
        return !pre;
    }
}
