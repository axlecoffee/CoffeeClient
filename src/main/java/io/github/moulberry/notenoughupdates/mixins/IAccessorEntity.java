package io.github.moulberry.notenoughupdates.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IAccessorEntity {
    @Accessor("isInWeb")
    boolean getIsInWeb();

    @Accessor("isInWeb")
    void setIsInWeb(boolean isInWeb);
}
