package com.intruderalerts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class SoundUtil {

    public static void playAlert(MinecraftClient client) {
        client.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
        );
    }
}
