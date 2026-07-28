package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
  private static final KeyMapping.Category ELYTRASSISTANT_CATEGORY =
      KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytraassistant", "general"));

  public static KeyMapping elytraToggleKeyBinding;

  public static void register() {
    elytraToggleKeyBinding =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.elytraassistant.elytra_toggle",
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                ELYTRASSISTANT_CATEGORY));
    ElytraAssistant.LOGGER.info("Elytra toggle key binding registered");
  }
}
