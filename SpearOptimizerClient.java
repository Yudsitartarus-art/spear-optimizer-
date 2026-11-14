package io.github.spearoptimizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class SpearOptimizerClient implements ClientModInitializer {
    public static final String MOD_ID = "spearoptimizer";

    private static final SpearTracker TRACKER = new SpearTracker();

    public static SpearTracker tracker() {
        return TRACKER;
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(TRACKER::tick);
        SpearLungeSwap.init();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "spear_hud"),
                SpearHud::extract
        );
    }
}
