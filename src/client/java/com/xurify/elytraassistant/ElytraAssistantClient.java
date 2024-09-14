package com.xurify.elytraassistant;

import net.fabricmc.api.ClientModInitializer;

public class ElytraAssistantClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ElytraAssistant.LOGGER.info("Initializing ElytraAssistantClient");
		ModKeybindings.register();
		DisableFireworkRocket.init();
		ElytraSwap.init();
		ElytraInfoTooltip.init();
	}
}