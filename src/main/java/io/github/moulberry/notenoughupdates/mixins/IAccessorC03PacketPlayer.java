package io.github.moulberry.notenoughupdates.mixins;

import net.minecraft.network.play.client.C03PacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(C03PacketPlayer.class)
public interface IAccessorC03PacketPlayer {

    @Accessor("yaw")
    float getYaw();

    @Accessor("yaw")
    void setYaw(float yaw);

    @Accessor("pitch")
    float getPitch();

    @Accessor("pitch")
    void setPitch(float pitch);

    @Accessor("rotating")
    boolean getRotating();

    @Accessor("rotating")
    void setRotating(boolean rotating);
}
