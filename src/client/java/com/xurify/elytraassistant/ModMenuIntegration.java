package com.xurify.elytraassistant;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.function.Supplier;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      try {
        Supplier<Screen> screenSupplier = AutoConfigClient.getConfigScreen(ModConfig.class, parent);
        Screen screen = screenSupplier == null ? null : screenSupplier.get();
        if (screen == null) {
          ElytraAssistant.LOGGER.error(
              "[ElytraAssistant] Could not create the configuration screen.");
          return parent;
        }
        return screen;
      } catch (Exception exception) {
        ElytraAssistant.LOGGER.error(
            "[ElytraAssistant] Could not open the configuration screen.", exception);
        return parent;
      }
    };
  }
}
