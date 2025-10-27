package io.github.moulberry.notenoughupdates.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IAccessorMinecraft {
    @Accessor("rightClickDelayTimer")
    int getRightClickDelayTimer();

    @Accessor("rightClickDelayTimer")
    void setRightClickDelayTimer(int value);

    @Invoker("clickMouse")
    void invokeClickMouse();

    @Invoker("rightClickMouse")
    void invokeRightClickMouse();
}
