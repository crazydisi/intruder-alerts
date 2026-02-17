package com.intruderalerts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AlertManager {

    private final SettingsManager settingsManager;

    public AlertManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void alert(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        sendChatAlert(client, playerName);
        sendToastAlert(client, playerName);
        if (settingsManager.isSoundEnabled()) {
            playSoundAlert(client);
        }
    }

    private void sendChatAlert(MinecraftClient client, String playerName) {
        Text message = Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.translatable("intruderalerts.alert.entered_render_distance",
                        Text.literal(playerName).formatted(Formatting.YELLOW)).formatted(Formatting.RED));

        client.inGameHud.getChatHud().addMessage(message);
    }

    private void sendToastAlert(MinecraftClient client, String playerName) {
        SystemToast.show(
                client.getToastManager(),
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.translatable("intruderalerts.alert.toast.title"),
                Text.translatable("intruderalerts.alert.toast.body", playerName)
        );
    }

    private void playSoundAlert(MinecraftClient client) {
        SoundUtil.playAlert(client);
    }
}
