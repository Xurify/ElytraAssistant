package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    private static final KeyBinding.Category ELYTRASSISTANT_CATEGORY =
            KeyBinding.Category.create(Identifier.of("elytraassistant", "general"));

    public static KeyBinding elytraToggleKeyBinding;

    public static void register() {
        elytraToggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytraassistant.elytra_toggle",
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                ELYTRASSISTANT_CATEGORY
        ));
        ElytraAssistant.LOGGER.info("Elytra toggle key binding registered");
    }
}
