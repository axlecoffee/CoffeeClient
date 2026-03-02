/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.mixins;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerControllerMP.class)
public interface IAccessorPlayerControllerMP {

    @Invoker("syncCurrentPlayItem")
    void callSyncCurrentPlayItem();

    @Accessor("currentPlayerItem")
    int getCurrentPlayerItem();

    @Accessor("currentPlayerItem")
    void setCurrentPlayerItem(int item);

    @Accessor("isHittingBlock")
    boolean getIsHittingBlock();
}
