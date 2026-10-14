/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public class SoundUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void playSound(String soundName) {
        SoundHandler soundHandler = mc.getSoundHandler();
        if (soundHandler != null) {
            PositionedSoundRecord sound = PositionedSoundRecord.create(new ResourceLocation(soundName));
            soundHandler.playSound(sound);
        }
    }
}
