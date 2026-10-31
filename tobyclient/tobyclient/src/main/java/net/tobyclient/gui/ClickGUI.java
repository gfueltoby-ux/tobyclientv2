// language: Java 17, file: ClickGUI.java
// Minimal dark click-GUI: one panel, one button per module.
// RSHIFT or ESC closes. Left-click toggles. shouldPause=false so the game keeps ticking.
package net.tobyclient.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.tobyclient.TobyClient;
import net.tobyclient.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGUI extends Screen {

    // Panel layout constants
    private static final int PX       = 8;    // panel left
    private static final int PY       = 8;    // panel top
    private static final int PW       = 130;  // panel width
    private static final int TITLE_H  = 14;
    private static final int BTN_H    = 16;
    private static final int PAD      = 2;

    // Colours (ARGB)
    private static final int COL_PANEL      = 0xCC0D0D1A;
    private static final int COL_TITLE_BAR  = 0xFF1A1A30;
    private static final int COL_BTN_ON     = 0x8820AA55;
    private static final int COL_BTN_OFF    = 0x88222233;
    private static final int COL_TXT_ON     = 0xFF55FF88;
    private static final int COL_TXT_OFF    = 0xFF888899;
    private static final int COL_TITLE_TXT  = 0xFFFF4444;

    public ClickGUI() {
        super(Text.literal("TobyClient"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<Module> modules = TobyClient.moduleManager.getModules();
        int panelH = TITLE_H + PAD + modules.size() * (BTN_H + PAD) + PAD;

        // Background
        ctx.fill(PX, PY, PX + PW, PY + panelH, COL_PANEL);

        // Title bar
        ctx.fill(PX, PY, PX + PW, PY + TITLE_H, COL_TITLE_BAR);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("TobyClient"),
            PX + PW / 2, PY + 3, COL_TITLE_TXT);

        // Module buttons
        int y = PY + TITLE_H + PAD;
        for (Module m : modules) {
            boolean on  = m.isEnabled();
            boolean hot = mouseX >= PX + PAD && mouseX <= PX + PW - PAD
                       && mouseY >= y        && mouseY <= y + BTN_H;

            int bgColor  = on  ? COL_BTN_ON  : COL_BTN_OFF;
            int txtColor = on  ? COL_TXT_ON  : COL_TXT_OFF;
            if (hot) bgColor = bgColor | 0x44000000; // slight highlight on hover

            ctx.fill(PX + PAD, y, PX + PW - PAD, y + BTN_H, bgColor);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(m.getName()),
                PX + PAD + 3, y + (BTN_H - 8) / 2, txtColor);

            y += BTN_H + PAD;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        List<Module> modules = TobyClient.moduleManager.getModules();
        int y = PY + TITLE_H + PAD;
        for (Module m : modules) {
            if (mouseX >= PX + PAD && mouseX <= PX + PW - PAD
             && mouseY >= y        && mouseY <= y + BTN_H) {
                m.toggle();
                return true;
            }
            y += BTN_H + PAD;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
