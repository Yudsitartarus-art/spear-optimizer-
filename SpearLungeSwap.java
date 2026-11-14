package io.github.spearoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.lwjgl.glfw.GLFW;

public final class SpearLungeSwap {
    private static final int REPEAT_TICKS = 5;
    private static final int WARN_COOLDOWN_TICKS = 40;

    private static KeyMapping lungeKey;
    private static int cooldown = 0;
    private static int warnCooldown = 0;

    private SpearLungeSwap() {}

    public static void init() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(SpearOptimizerClient.MOD_ID, "main"));
        lungeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spearoptimizer.lunge_swap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category));
        ClientTickEvents.END_CLIENT_TICK.register(SpearLungeSwap::tick);
    }

    private static void tick(Minecraft mc) {
        if (cooldown > 0) cooldown--;
        if (warnCooldown > 0) warnCooldown--;

        boolean clicked = false;
        while (lungeKey.consumeClick()) clicked = true;
        boolean wanted = clicked || lungeKey.isDown();

        LocalPlayer player = mc.player;
        if (!wanted || player == null || mc.gameMode == null || mc.getConnection() == null
                || mc.screen != null || cooldown > 0) {
            return;
        }

        if (perform(mc, player)) {
            cooldown = REPEAT_TICKS;
        } else if (warnCooldown == 0) {
            player.displayClientMessage(Component.literal("Tidak ada tombak ber-Lunge di hotbar"), true);
            warnCooldown = WARN_COOLDOWN_TICKS;
        }
    }

    private static boolean perform(Minecraft mc, LocalPlayer player) {
        Inventory inv = player.getInventory();
        int original = inv.getSelectedSlot();

        int best = -1;
        int bestLevel = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.get(DataComponents.PIERCING_WEAPON) == null) continue;
            int level = lungeLevel(player, stack);
            if (level > bestLevel) {
                bestLevel = level;
                best = i;
            }
        }
        if (best < 0) return false;

        PiercingWeapon weapon = inv.getItem(best).get(DataComponents.PIERCING_WEAPON);
        if (weapon == null) return false;

        ClientPacketListener connection = mc.getConnection();
        boolean swap = best != original;

        if (swap) {
            inv.setSelectedSlot(best);
            connection.send(new ServerboundSetCarriedItemPacket(best));
        }

        mc.gameMode.piercingAttack(weapon);
        player.swing(InteractionHand.MAIN_HAND);

        if (swap) {
            inv.setSelectedSlot(original);
            connection.send(new ServerboundSetCarriedItemPacket(original));
        }
        return true;
    }

    private static int lungeLevel(LocalPlayer player, ItemStack stack) {
        try {
            Holder<Enchantment> lunge = player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LUNGE);
            return EnchantmentHelper.getItemEnchantmentLevel(lunge, stack);
        } catch (Throwable t) {
            return 0;
        }
    }
}
