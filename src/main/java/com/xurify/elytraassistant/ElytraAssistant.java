package com.xurify.elytraassistant;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraAssistant implements ModInitializer {
  public static final String MOD_ID = "elytraassistant";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static ModConfig CONFIG;

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing ElytraAssistant");
    AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
    CONFIG = configHolder.getConfig();
    if (CONFIG.fireworkRockets.migrateLegacyRestriction()) {
      configHolder.save();
    }
  }
}
