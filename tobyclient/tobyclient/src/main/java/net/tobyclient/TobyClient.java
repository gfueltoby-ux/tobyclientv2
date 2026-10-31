// language: Java 17, file: TobyClient.java, target: Fabric 1.20.4
// entrypoint — registers the RSHIFT GUI key and drives the module tick loop
package net.tobyclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.tobyclient.gui.ClickGUI;
import net.tobyclient.module.ModuleManager;

public class TobyClient implements ClientModInitializer {

    public static final String NAME    = "TobyClient";
    public static final String VERSION = "1.0.0";

    public static ModuleManager moduleManager;
    public static ClickGUI      clickGUI;

    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();
        clickGUI      = new ClickGUI();

        // Right Shift → open GUI
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tobyclient.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "TobyClient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // Key press handled outside a screen
            if (mc.currentScreen == null) {
                while (guiKey.wasPressed()) {
                    mc.setScreen(clickGUI);
                }
            }

            // Drive modules
            if (mc.player != null && mc.world != null) {
                moduleManager.tick(mc);
            }
        });
    }
}
