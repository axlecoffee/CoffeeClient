package coffee.axle.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

// freezes inbound play packets while a module holds the buffer, replays them in order on release
public class IncomingPacketBuffer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int HEALTH_METADATA_ID = 6;

    private static final Deque<Packet<INetHandlerPlayClient>> queuedPackets = new ConcurrentLinkedDeque<>();
    private static boolean enabled;

    @SuppressWarnings("unchecked")
    public static boolean buffer(Packet<?> packet) {
        if (!enabled) {
            return false;
        }
        Packet<INetHandlerPlayClient> playPacket = (Packet<INetHandlerPlayClient>) packet;

        if (packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn) {
            disable();
            return false;
        }
        if (packet instanceof S00PacketKeepAlive || packet instanceof S06PacketUpdateHealth) {
            return false;
        }
        if (packet instanceof S19PacketEntityStatus) {
            byte opcode = ((S19PacketEntityStatus) packet).getOpCode();
            if (opcode == 2 || opcode == 3) {
                return false;
            }
        }
        if (packet instanceof S1CPacketEntityMetadata) {
            List<WatchableObject> metadata = ((S1CPacketEntityMetadata) packet).func_149376_c();
            if (metadata != null) {
                for (WatchableObject entry : metadata) {
                    if (entry.getDataValueId() == HEALTH_METADATA_ID) {
                        return false;
                    }
                }
            }
        }

        queuedPackets.offer(playPacket);
        return true;
    }

    public static void enable() {
        enabled = true;
    }

    @SuppressWarnings("unchecked")
    public static void offer(Packet<?> packet) {
        queuedPackets.offer((Packet<INetHandlerPlayClient>) packet);
    }

    public static void disable() {
        enabled = false;
        if (mc.getNetHandler() == null) {
            queuedPackets.clear();
            return;
        }
        // replay in arrival order, stop early if a handler re-enables the buffer
        while (!queuedPackets.isEmpty() && !enabled) {
            Packet<INetHandlerPlayClient> packet = queuedPackets.poll();
            try {
                packet.processPacket(mc.getNetHandler());
            } catch (ThreadQuickExitException ignored) {
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
