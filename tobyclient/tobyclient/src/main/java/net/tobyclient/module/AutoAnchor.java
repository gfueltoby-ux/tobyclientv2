// language: Java 17, file: AutoAnchor.java
// target: Windows/Linux, Fabric 1.20.4
//
// Respawn Anchors explode only in Overworld and The End.
// State machine — three phases per target cycle:
//   PLACE   — place a respawn anchor adjacent to the target
//   CHARGE  — use glowstone on the anchor up to 4 times (full charge)
//   EXPLODE — use the charged anchor with a non-glowstone item to trigger the explosion
//
// All block interactions go through interactionManager.interactBlock, same as
// the vanilla 'use' action, so anti-cheat sees normal use packets.
// 100ms between actions to keep packet rate plausible.
package net.tobyclient.module;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AutoAnchor extends Module {

    private enum Phase { PLACE, CHARGE, EXPLODE }

    private static final double RANGE      = 4.5;
    private static final long   ACTION_MS  = 100L;
    private static final int    MAX_CHARGE = 4;

    private Phase    phase        = Phase.PLACE;
    private BlockPos anchorPos    = null;
    private int      chargeCount  = 0;
    private long     lastAction   = 0L;

    public AutoAnchor() {
        super("AutoAnchor", "Auto-places, charges, and detonates respawn anchors (OW/End only)");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Anchors only explode outside the Nether
        RegistryKey<World> dim = mc.world.getRegistryKey();
        if (dim.equals(World.NETHER)) return;

        long now = System.currentTimeMillis();
        if (now - lastAction < ACTION_MS) return;

        PlayerEntity target = nearestTarget(mc);
        if (target == null) { reset(); return; }

        switch (phase) {
            case PLACE   -> doPlace(mc, target, now);
            case CHARGE  -> doCharge(mc, now);
            case EXPLODE -> doExplode(mc, now);
        }
    }

    // ── phases ───────────────────────────────────────────────────────────────

    private void doPlace(MinecraftClient mc, PlayerEntity target, long now) {
        int anchorSlot = hotbarSlot(mc, Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) return;

        // Try to place at target's feet; if occupied, try one block lower
        BlockPos candidate = BlockPos.ofFloored(target.getPos());
        if (!mc.world.getBlockState(candidate).isAir()) {
            candidate = candidate.down();
        }
        if (!mc.world.getBlockState(candidate).isAir()) return;

        // The face we click is the top face of the block below the placement slot
        BlockPos support = candidate.down();
        if (mc.world.getBlockState(support).isAir()) return; // nothing to place on

        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = anchorSlot;
        useBlock(mc, support, Direction.UP);
        mc.player.getInventory().selectedSlot = prev;

        anchorPos   = candidate;
        chargeCount = 0;
        phase       = Phase.CHARGE;
        lastAction  = now;
    }

    private void doCharge(MinecraftClient mc, long now) {
        if (!anchorStillThere(mc)) { reset(); return; }

        if (chargeCount >= MAX_CHARGE) { phase = Phase.EXPLODE; lastAction = now; return; }

        int glowSlot = hotbarSlot(mc, Items.GLOWSTONE);
        if (glowSlot == -1) { phase = Phase.EXPLODE; lastAction = now; return; }

        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = glowSlot;
        useBlock(mc, anchorPos, Direction.UP);
        mc.player.getInventory().selectedSlot = prev;

        chargeCount++;
        lastAction = now;
        if (chargeCount >= MAX_CHARGE) phase = Phase.EXPLODE;
    }

    private void doExplode(MinecraftClient mc, long now) {
        if (!anchorStillThere(mc)) { reset(); return; }

        // Using a charged anchor WITHOUT glowstone in hand triggers the explosion
        int safeSlot = nonGlowstoneHotbarSlot(mc);
        if (safeSlot == -1) return;

        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = safeSlot;
        useBlock(mc, anchorPos, Direction.UP);
        mc.player.getInventory().selectedSlot = prev;

        reset();
        lastAction = now;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void useBlock(MinecraftClient mc, BlockPos pos, Direction face) {
        Vec3d hitVec = Vec3d.ofCenter(pos).add(0, face == Direction.UP ? 0.5 : 0, 0);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, face, pos, false));
    }

    private boolean anchorStillThere(MinecraftClient mc) {
        return anchorPos != null &&
               mc.world.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR;
    }

    private PlayerEntity nearestTarget(MinecraftClient mc) {
        PlayerEntity best = null;
        double closest = RANGE * RANGE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isDead() || p.isCreative()) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d < closest) { closest = d; best = p; }
        }
        return best;
    }

    private int hotbarSlot(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int nonGlowstoneHotbarSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) return i;
        }
        return -1;
    }

    private void reset() {
        phase       = Phase.PLACE;
        anchorPos   = null;
        chargeCount = 0;
    }
}
