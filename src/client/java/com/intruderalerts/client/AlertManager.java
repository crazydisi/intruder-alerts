package com.intruderalerts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class AlertManager {

    private final SettingsManager settingsManager;

    public AlertManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void alert(String playerName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        sendChatAlert(client, playerName);
        sendToastAlert(client, playerName);
        if (settingsManager.isSoundEnabled()) {
            playSoundAlert(client);
        }
    }

    private void sendChatAlert(Minecraft client, String playerName) {
        Component message = Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.translatable("intruderalerts.alert.entered_render_distance",
                        Component.literal(playerName).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));

        client.gui.getChat().addClientSystemMessage(message);
    }

    private void sendToastAlert(Minecraft client, String playerName) {
        SystemToast.add(
                client.getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("intruderalerts.alert.toast.title"),
                Component.translatable("intruderalerts.alert.toast.body", playerName)
        );
    }

    private void playSoundAlert(Minecraft client) {
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.0f)
        );
    }
}
