package io.github.spearoptimizer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SpearHud {
    private static final int BAR_W = 90;
    private static final int BAR_H = 6;

    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_CHARGING = 0xFFFF9800;
    private static final int COLOR_SYNC = 0xFFFFEB3B;
    private static final int COLOR_READY = 0xFF4CAF50;
    private static final int COLOR_DROPPED = 0xFF9E9E9E;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private SpearHud() {}

    static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SpearTracker t = SpearOptimizerClient.tracker();
        SpearTracker.ChargeState state = t.state();
        if (state == SpearTracker.ChargeState.NONE) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        int total = t.delayTicks() + t.latencyTicks();
        float progress = Math.min(1f, (t.ticksUsing() + partial) / Math.max(1, total));

        int color;
        String label;
        switch (state) {
            case LOWERED -> { color = COLOR_CHARGING; label = "Charging"; }
            case SHAKING -> {
                if (t.waitingForServer()) { color = COLOR_SYNC; label = "Sync..."; }
                else { color = COLOR_READY; label = "READY"; }
            }
            default -> { color = COLOR_DROPPED; label = "Released"; }
        }

        // efek getar saat siap
        int shake = 0;
        if (state == SpearTracker.ChargeState.SHAKING) {
            shake = (int) Math.round(Math.sin(System.currentTimeMillis() / 25.0) * 1.5);
        }

        int x = (sw - BAR_W) / 2 + shake;
        int y = sh / 2 + 22;

        // bar + isi
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, COLOR_BG);
        if (state == SpearTracker.ChargeState.DROPPED) progress = 1f;
        g.fill(x, y, x + Math.round(BAR_W * progress), y + BAR_H, color);

        // penanda: titik di mana charge dianggap siap di sisi client (sebelum kompensasi ping)
        if (t.latencyTicks() > 0) {
            int markX = x + Math.round(BAR_W * (t.delayTicks() / (float) total));
            g.fill(markX, y - 2, markX + 1, y + BAR_H + 2, COLOR_TEXT);
        }

        // teks status
        g.centeredText(mc.font, Component.literal(label), sw / 2 + shake, y - 11, color);

        // preview damage + info tambahan
        if (state != SpearTracker.ChargeState.DROPPED) {
            String info = String.format("~%.1f dmg | %.1f b/s | %d ms", t.estimatedDamage(), t.speedBps(), t.latencyMs());
            if (t.lungeLevel() > 0) info += " | Lunge " + t.lungeLevel();
            g.centeredText(mc.font, Component.literal(info), sw / 2, y + BAR_H + 4, COLOR_TEXT);
        }
    }
}
