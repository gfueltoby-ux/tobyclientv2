// language: Java 17, file: Module.java
package net.tobyclient.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {

    private final String  name;
    private final String  description;
    private       boolean enabled = false;

    protected Module(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    /** Called every client tick while enabled. */
    public void onTick(MinecraftClient mc) {}

    public String  getName()        { return name; }
    public String  getDescription() { return description; }
    public boolean isEnabled()      { return enabled; }
    public void    setEnabled(boolean v) { enabled = v; }
    public void    toggle()         { enabled = !enabled; }
}
