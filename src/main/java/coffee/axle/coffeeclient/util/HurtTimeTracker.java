package coffee.axle.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

import java.util.List;

public class HurtTimeTracker {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int HEALTH_ID = 6;

    private boolean healthDecreasePending = false;
    private int remainingHurtTicks = 0;

    public int remainingHurtTicks() {
        return this.remainingHurtTicks;
    }

    public void tick() {
        if (this.remainingHurtTicks > 0) {
            this.remainingHurtTicks--;
        }
        if (this.healthDecreasePending) {
            this.healthDecreasePending = false;
            this.remainingHurtTicks = mc.thePlayer.hurtTime;
        }
    }

    public void handlePacket(Packet<?> packet) {
        if (packet instanceof S06PacketUpdateHealth) {
            if (((S06PacketUpdateHealth) packet).getHealth() < mc.thePlayer.getHealth()) {
                this.healthDecreasePending = true;
            }
            return;
        }
        if (packet instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata metadata = (S1CPacketEntityMetadata) packet;
            if (metadata.getEntityId() != mc.thePlayer.getEntityId()) {
                return;
            }
            List<DataWatcher.WatchableObject> entries = metadata.func_149376_c();
            if (entries == null) {
                return;
            }
            for (DataWatcher.WatchableObject entry : entries) {
                if (entry.getDataValueId() == HEALTH_ID
                        && (Float) entry.getObject() < mc.thePlayer.getHealth()) {
                    this.healthDecreasePending = true;
                    break;
                }
            }
        }
    }
}
