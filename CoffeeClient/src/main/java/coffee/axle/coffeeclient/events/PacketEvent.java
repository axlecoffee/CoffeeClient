package coffee.axle.coffeeclient.events;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PacketEvent extends Event {
    private final Packet<?> packet;
    private final boolean send;

    public PacketEvent(Packet<?> packet, boolean send) {
        this.packet = packet;
        this.send = send;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public boolean isSend() {
        return send;
    }

    public boolean isReceive() {
        return !send;
    }
}
