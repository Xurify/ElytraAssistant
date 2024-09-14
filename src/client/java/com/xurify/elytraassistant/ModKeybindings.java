package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    public static KeyBinding elytraToggleKeyBinding;

    public static void register() {
        elytraToggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytraassistant.elytra_toggle",
                GLFW.GLFW_KEY_GRAVE_ACCENT, 
                "category.elytraassistant.general"
        ));
        ElytraAssistant.LOGGER.info("Elytra toggle key binding registered");
    }
}
