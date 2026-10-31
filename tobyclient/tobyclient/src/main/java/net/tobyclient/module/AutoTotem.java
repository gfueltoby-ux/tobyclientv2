// language: Java 17, file: AutoTotem.java
// hotbar-first totem swap; fires the moment offhand slot loses its totem
// uses SlotActionType.SWAP / button 40 (vanilla offhand-swap wire format)
package net.tobyclient.module;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {

    public AutoTotem() {
        super("AutoTotem", "Swaps a totem to offhand the moment one is consumed (hotbar priority)");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        PlayerInventory inv = mc.player.getInventory();

        // Check hotbar first (indices 0-8 in main = screen slots 36-44)
        int totemSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        // Fallback to main inventory (indices 9-35 = screen slots 9-35)
        if (totemSlot == -1) {
            for (int i = 9; i < inv.main.size(); i++) {
                if (inv.main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totemSlot = i;
                    break;
                }
            }
        }
        if (totemSlot == -1) return;

        swapToOffhand(mc, totemSlot);
    }

    /**
     * Maps a PlayerInventory.main index to a PlayerScreenHandler screen slot,
     * then sends SWAP (button=40) to move it to the offhand.
     *
     * Wire format: SlotActionType.SWAP + button 40 = offhand swap.
     * ScreenHandler.onSlotClick checks button==40 and routes to offhand.
     */
    static void swapToOffhand(MinecraftClient mc, int invMainIndex) {
        int screenSlot = invMainIndex < 9 ? 36 + invMainIndex : invMainIndex;

        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            mc.player.playerScreenHandler.syncId,
            mc.player.playerScreenHandler.getRevision(),
            screenSlot,
            40,                         // button 40 = offhand slot
            SlotActionType.SWAP,
            mc.player.playerScreenHandler.getSlot(screenSlot).getStack().copy(),
            new Int2ObjectArrayMap<>()  // no predicted changed slots needed
        ));
    }
}
