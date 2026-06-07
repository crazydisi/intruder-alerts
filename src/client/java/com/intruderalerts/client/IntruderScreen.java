package com.intruderalerts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class IntruderScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 4;

    private final TrustManager trustManager;
    private final ZoneManager zoneManager;
    private final HistoryManager historyManager;
    private final IgnoreManager ignoreManager;
    private final SettingsManager settingsManager;
    private final PlayerTracker playerTracker;
    private final AlertManager alertManager;

    public IntruderScreen(TrustManager trustManager, ZoneManager zoneManager,
                          HistoryManager historyManager, IgnoreManager ignoreManager,
                          SettingsManager settingsManager, PlayerTracker playerTracker,
                          AlertManager alertManager) {
        super(Component.translatable("intruderalerts.menu.title"));
        this.trustManager = trustManager;
        this.zoneManager = zoneManager;
        this.historyManager = historyManager;
        this.ignoreManager = ignoreManager;
        this.settingsManager = settingsManager;
        this.playerTracker = playerTracker;
        this.alertManager = alertManager;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int leftX = centerX - BUTTON_WIDTH / 2;
        int y = Math.max(20, this.height / 2 - 130);

        addRenderableWidget(new StringWidget(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("intruderalerts.menu.title").withStyle(ChatFormatting.BOLD),
                this.font));
        y += BUTTON_HEIGHT + SPACING;

        boolean enabled = IntruderAlertsClient.isEnabled();
        addRenderableWidget(new StringWidget(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable(enabled ? "intruderalerts.menu.status.enabled" : "intruderalerts.menu.status.disabled")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                this.font));
        y += BUTTON_HEIGHT + SPACING;

        addRenderableWidget(Button.builder(
                        Component.translatable(enabled ? "intruderalerts.menu.toggle.disable" : "intruderalerts.menu.toggle.enable"),
                        btn -> {
                            boolean now = IntruderAlertsClient.toggleEnabled();
                            if (!now) {
                                playerTracker.clearTracking();
                            }
                            this.rebuildWidgets();
                        })
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING;

        boolean sound = settingsManager.isSoundEnabled();
        addRenderableWidget(Button.builder(
                        Component.translatable(sound ? "intruderalerts.menu.sound.disable" : "intruderalerts.menu.sound.enable"),
                        btn -> {
                            settingsManager.toggleSound();
                            this.rebuildWidgets();
                        })
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING * 3;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.show.trusted"),
                        btn -> closeAndPrint(c -> CommandRegistrar.printTrustList(trustManager, c)))
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.show.zones"),
                        btn -> closeAndPrint(c -> CommandRegistrar.printZoneList(zoneManager, c)))
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.show.ignored"),
                        btn -> closeAndPrint(c -> CommandRegistrar.printIgnoreList(ignoreManager, c)))
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.show.history"),
                        btn -> closeAndPrint(c -> CommandRegistrar.printHistoryList(historyManager, null, c)))
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING * 3;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.test"),
                        btn -> {
                            this.onClose();
                            alertManager.alert(Component.translatable("intruderalerts.menu.demo_name").getString());
                        })
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + SPACING;

        addRenderableWidget(Button.builder(
                        Component.translatable("intruderalerts.menu.done"),
                        btn -> this.onClose())
                .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void closeAndPrint(Consumer<Consumer<Component>> action) {
        this.onClose();
        action.accept(msg -> this.minecraft.gui.getChat().addClientSystemMessage(msg));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}
