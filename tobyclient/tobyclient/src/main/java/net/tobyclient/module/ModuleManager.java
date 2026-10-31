// language: Java 17, file: ModuleManager.java
package net.tobyclient.module;

import net.minecraft.client.MinecraftClient;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules;

    public ModuleManager() {
        modules = List.of(
            new AutoTotem(),
            new AutoInventoryTotem(),
            new AutoCrystal(),
            new AutoAnchor()
        );
    }

    public void tick(MinecraftClient mc) {
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }

    public List<Module> getModules() { return modules; }
}
