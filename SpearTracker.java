package io.github.spearoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Melacak status charge tombak di sisi client (tanpa mixin).
 * Tombak dikenali lewat data component KINETIC_WEAPON, jadi tombak dari mod lain
 * yang memakai komponen yang sama ikut terdeteksi.
 */
public class SpearTracker {

    public enum ChargeState { NONE, LOWERED, SHAKING, DROPPED }

    private static final int DROP_DISPLAY_TICKS = 8;

    private ChargeState state = ChargeState.NONE;
    private int ticksUsing = 0;
    private int delayTicks = 1;
    private int dropTicksLeft = 0;
    private float smoothedLatencyMs = 0f;
    private float speedBps = 0f;
    private float estimatedDamage = 0f;
    private int lungeLevel = 0;

    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            reset();
            return;
        }

        updateLatency(mc, player);

        ItemStack using = player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY;
        KineticWeapon kinetic = using.isEmpty() ? null : using.get(DataComponents.KINETIC_WEAPON);

        if (kinetic != null) {
            if (state == ChargeState.NONE || state == ChargeState.DROPPED) {
                lungeLevel = lungeLevel(player, using);
            }
            delayTicks = Math.max(1, kinetic.delayTicks());
            ticksUsing = player.getTicksUsingItem();
            speedBps = (float) (player.getDeltaMovement().horizontalDistance() * 20.0);

            double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            estimatedDamage = (float) (base + speedBps * kinetic.damageMultiplier());

            state = ticksUsing >= delayTicks ? ChargeState.SHAKING : ChargeState.LOWERED;
            dropTicksLeft = 0;
            return;
        }

        // tidak sedang memakai tombak
        if (state == ChargeState.LOWERED || state == ChargeState.SHAKING) {
            state = ChargeState.DROPPED;
            dropTicksLeft = DROP_DISPLAY_TICKS;
        } else if (state == ChargeState.DROPPED) {
            if (--dropTicksLeft <= 0) {
                reset();
            }
        }
    }

    private void updateLatency(Minecraft mc, LocalPlayer player) {
        ClientPacketListener connection = mc.getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player.getUUID());
        float ms = info == null ? 0f : info.getLatency();
        // exponential moving average supaya angka tidak loncat-loncat
        smoothedLatencyMs = smoothedLatencyMs == 0f ? ms : smoothedLatencyMs * 0.9f + ms * 0.1f;
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

    private void reset() {
        state = ChargeState.NONE;
        ticksUsing = 0;
        dropTicksLeft = 0;
        speedBps = 0f;
        estimatedDamage = 0f;
        lungeLevel = 0;
    }

    public ChargeState state() { return state; }
    public int ticksUsing() { return ticksUsing; }
    public int delayTicks() { return delayTicks; }
    public int dropTicksLeft() { return dropTicksLeft; }
    public float speedBps() { return speedBps; }
    public float estimatedDamage() { return estimatedDamage; }
    public int lungeLevel() { return lungeLevel; }
    public int latencyMs() { return Math.round(smoothedLatencyMs); }

    /** Ping dibulatkan ke tick (1 tick = 50 ms), maksimal 1 detik. */
    public int latencyTicks() { return Math.min(20, Math.round(smoothedLatencyMs / 50f)); }

    /** Client sudah siap, tapi server kemungkinan belum menghitung charge ini selesai. */
    public boolean waitingForServer() {
        return state == ChargeState.SHAKING && ticksUsing < delayTicks + latencyTicks();
    }
}
