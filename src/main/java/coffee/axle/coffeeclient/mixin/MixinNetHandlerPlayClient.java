package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.events.PacketEvent;
import coffee.axle.coffeeclient.feature.world.BedTracker;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "addToSendQueue", at = @At("HEAD"), cancellable = true)
    public void addToSendQueue(Packet packet, CallbackInfo ci) {
        PacketEvent packetEvent = new PacketEvent(packet, true);
        MinecraftForge.EVENT_BUS.post(packetEvent);
        if (packetEvent.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleChat", at = @At("HEAD"))
    public void handleChat(S02PacketChat packetIn, CallbackInfo ci) {
        if (CoffeeClient.featureManager == null) return;
        CoffeeClient.featureManager.features.values().stream()
                .filter(m -> m instanceof BedTracker)
                .forEach(m -> ((BedTracker) m).onPacketReceive(packetIn));
    }

    @Inject(method = "handlePlayerPosLook", at = @At("RETURN"))
    public void handlePlayerPosLookPost(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        if (CoffeeClient.featureManager == null) return;
        CoffeeClient.featureManager.features.values().stream()
                .filter(m -> m instanceof BedTracker)
                .forEach(m -> ((BedTracker) m).onPacketReceive(packetIn));
    }
}
