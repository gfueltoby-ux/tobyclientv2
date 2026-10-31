// language: Java 17, file: AutoInventoryTotem.java
// slower cadence (100ms cooldown) — scans all 36 main slots + ensures offhand
// is kept stocked even without an immediate-death trigger
package net.tobyclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

public class AutoInventoryTotem extends Module {

    private long lastSwap = 0L;
    private static final long COOLDOWN_MS = 100L;

    public AutoInventoryTotem() {
        super("AutoInventoryTotem", "Continuously moves a totem from anywhere in inventory to offhand");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        long now = System.currentTimeMillis();
        if (now - lastSwap < COOLDOWN_MS) return;

        PlayerInventory inv = mc.player.getInventory();

        // Full scan: hotbar (0-8) then main inventory (9-35)
        int totemSlot = -1;
        for (int i = 0; i < inv.main.size(); i++) {
            if (inv.main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        if (totemSlot == -1) return;

        // Delegate to AutoTotem's shared helper (same SWAP-40 packet)
        AutoTotem.swapToOffhand(mc, totemSlot);
        lastSwap = now;
    }
}
