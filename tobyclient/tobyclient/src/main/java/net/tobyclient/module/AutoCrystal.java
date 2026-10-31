// language: Java 17, file: AutoCrystal.java
// target: Windows/Linux, Fabric 1.20.4
//
// Two-phase per-tick loop:
//   EXPLODE — attack any placed end crystal within reach before placing a new one
//   PLACE   — find best obsidian/bedrock surface near target, place crystal there
//
// Placement validity: base must be obsidian or bedrock; two air blocks above;
// no entity occupying the crystal's footprint; within PLACE_RANGE of the player.
// Best position: minimizes distance to target (crude damage proxy; proper explosion
// math needs raycast occlusion per-block — add if needed).
package net.tobyclient.module;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoCrystal extends Module {

    private static final double PLACE_RANGE   = 5.0;
    private static final double TARGET_RANGE  = 8.0;
    private static final long   PLACE_DELAY   = 50L;  // ms between placements
    private static final long   EXPLODE_DELAY = 50L;  // ms between detonations

    private long lastPlace   = 0L;
    private long lastExplode = 0L;

    public AutoCrystal() {
        super("AutoCrystal", "Auto-places and detonates end crystals on nearby players");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        int crystalSlot = findHotbarSlot(mc, Items.END_CRYSTAL);
        if (crystalSlot == -1) return;

        PlayerEntity target = nearestTarget(mc);
        if (target == null) return;

        long now = System.currentTimeMillis();

        // --- EXPLODE ---
        if (now - lastExplode >= EXPLODE_DELAY) {
            Box searchBox = mc.player.getBoundingBox().expand(PLACE_RANGE + 2.0);
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                    EndCrystalEntity.class, searchBox, e -> true)) {
                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                lastExplode = now;
                break; // one per tick is enough
            }
        }

        // --- PLACE ---
        if (now - lastPlace >= PLACE_DELAY) {
            BlockPos best = bestPlacement(mc, target);
            if (best != null) {
                int prev = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = crystalSlot;

                // Interact with the TOP face of the support block
                Vec3d hitVec = Vec3d.ofCenter(best).add(0, 0.5, 0);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, Direction.UP, best, false));

                mc.player.getInventory().selectedSlot = prev;
                lastPlace = now;
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PlayerEntity nearestTarget(MinecraftClient mc) {
        PlayerEntity best = null;
        double closest = TARGET_RANGE * TARGET_RANGE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isDead() || p.isCreative()) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d < closest) { closest = d; best = p; }
        }
        return best;
    }

    private BlockPos bestPlacement(MinecraftClient mc, PlayerEntity target) {
        Vec3d tp = target.getPos();
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Check the block directly under the target's feet
                BlockPos support = new BlockPos(
                    (int) Math.floor(tp.x + x),
                    (int) Math.floor(tp.y - 1),
                    (int) Math.floor(tp.z + z)
                );
                if (validPlacement(mc, support)) candidates.add(support);
            }
        }

        return candidates.stream()
            .min(Comparator.comparingDouble(pos ->
                target.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5)))
            .orElse(null);
    }

    /**
     * @param support The block the crystal will sit ON TOP of (must be obsidian/bedrock).
     *                Crystal occupies support.up() and support.up(2).
     */
    private boolean validPlacement(MinecraftClient mc, BlockPos support) {
        var base = mc.world.getBlockState(support).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return false;

        BlockPos slot1 = support.up();
        BlockPos slot2 = support.up(2);
        if (!mc.world.getBlockState(slot1).isAir()) return false;
        if (!mc.world.getBlockState(slot2).isAir()) return false;

        // No entity already in the crystal's footprint
        Box footprint = new Box(slot1).expand(0.5, 0.0, 0.5);
        if (!mc.world.getOtherEntities(null, footprint).isEmpty()) return false;

        // Reachable from player eye
        double dist = mc.player.getEyePos()
            .squaredDistanceTo(Vec3d.ofCenter(slot1));
        return dist <= PLACE_RANGE * PLACE_RANGE;
    }

    private int findHotbarSlot(MinecraftClient mc, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
