package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ElytraSwap {
    private static final int RECENTLY_AIRBORNE_THRESHOLD = 10;
    private static final double RUNNING_VELOCITY_THRESHOLD = 0.1;
    private static final long DOUBLE_JUMP_WINDOW = 20L;

    private static ItemStack originalChestItem = ItemStack.EMPTY;
    private static ItemStack lastWornChestplate = ItemStack.EMPTY;
    private static ItemStack lastWornElytra = ItemStack.EMPTY;

    private static boolean isToggling = false;
    private static boolean hadArmorBeforeFlight = false;
    private static boolean prevTickOnGround = true;
    private static boolean prevTickJumpKeyPressed = false;
    private static boolean wasInAir = false;
    private static boolean cachedHasElytra = false;
    
    private static long lastInventoryCheck = 0;
    private static int ticksSinceGrounded = 0;
    private static int airTicks = 0;
    private static long lastJumpTick = 0;
    private static int airTime = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ElytraSwap::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        handleElytraToggleKeyPress(client);

        if (!isValidTickState(client))
            return;

        PlayerState playerState = new PlayerState(client);

        boolean hasElytraInInventory = hasElytraInInventory(client);

        updateAirState(playerState, hasElytraInInventory);
        handleJumpKeyPress(playerState, client, hasElytraInInventory);

        if (hasElytraInInventory) {
            handleMidAirActivation(playerState, client);
            handleFallingDetection(playerState, client);
            handleLandingDetection(playerState, client);
        }

        updatePreviousTickState(playerState);
    }

    private static boolean isValidTickState(MinecraftClient client) {
        return Optional.ofNullable(client.player)
                .flatMap(player -> Optional.ofNullable(client.world))
                .map(world -> ElytraAssistant.CONFIG.elytraActivationSettings.autoElytraEnabled)
                .orElse(false);
    }

    private static void handleElytraToggleKeyPress(MinecraftClient client) {
        if (ModKeybindings.elytraToggleKeyBinding.wasPressed() && !isToggling) {
            isToggling = true;
            toggleElytraChestplate(client, true);
            isToggling = false;
        }
    }

    private static void updateAirState(PlayerState state, boolean hasElytraInInventory) {
        if (!state.isOnGround) {
            airTicks++;
            airTime++;
            ticksSinceGrounded++;
            wasInAir = true;
        } else {
            airTicks = 0;
            ticksSinceGrounded = 0;
            if (hasElytraInInventory && !isElytraEquipped(state.client) && wasInAir
                    && airTime > ElytraAssistant.CONFIG.sensitivityTweaks.airTicksThreshold) {

                if (hadArmorBeforeFlight) {
                    logInfo("Landing detected. Attempting to equip Chestplate", state.areLogsEnabled);
                    tryRestoreOriginalChestplate(state.client);
                } else {
                    logInfo("Landing detected. Player had no armor before flight.", state.areLogsEnabled);
                }
                wasInAir = false;
                airTime = 0;
                originalChestItem = ItemStack.EMPTY;
                hadArmorBeforeFlight = false;
            }
        }

        if (state.isInFluid) {
            airTicks = 0;
        }
    }

    private static void updatePreviousTickState(PlayerState state) {
        prevTickOnGround = state.isOnGround;
        prevTickJumpKeyPressed = state.wasJumpKeyPressed;
    }

    private static void handleJumpKeyPress(PlayerState state, MinecraftClient client, boolean hasElytraInInventory) {
        if (state.areLogsEnabled && state.wasJumpKeyPressed && !prevTickJumpKeyPressed) {
            logJumpDebugInfo(state);
        }

        if (hasElytraInInventory && !state.isOnGround && !state.isInFluid && state.wasJumpKeyPressed
                && !prevTickJumpKeyPressed) {
            if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
                logInfo("Attempting to equip Elytra - Double Jump", state.areLogsEnabled);
                tryEquipElytra(client, true);
            }
            lastJumpTick = state.currentTick;
        }
    }

    private static void handleMidAirActivation(PlayerState state, MinecraftClient client) {
        if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
            return;
        }

        if (!state.isOnGround && !state.isInFluid && !state.wasRecentlyAirborne && !state.isClimbing
                && airTicks >= ElytraAssistant.CONFIG.sensitivityTweaks.midAirActivationThreshold
                && state.wasJumpKeyPressed && !state.player.getAbilities().flying) {

            logMidAirActivationAttempt(state);
            if (state.isInMidAirBalance || state.isMovingDown || state.isMovingUp) {
                tryEquipElytra(client, true);
            }
        }
    }

    private static void handleFallingDetection(PlayerState state, MinecraftClient client) {
        if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
            return;
        }

        boolean isGliding = state.isGliding;
        if (!state.isOnGround && state.hasBeenInAir && state.hasFallenEnough && !isGliding && state.wasJumpKeyPressed) {
            logInfo("Attempting to equip Elytra - Significant Fall", state.areLogsEnabled);
            tryEquipElytra(client, true);
        } else if (!state.isOnGround && state.isMovingUpFast && !isGliding && state.wasJumpKeyPressed) {
            logInfo("Attempting to equip Elytra - Upward Boost", state.areLogsEnabled);
            tryEquipElytra(client, true);
        }
    }

    private static void handleLandingDetection(PlayerState state, MinecraftClient client) {
        if (!prevTickOnGround && state.isOnGround && state.hasBeenInAir) {
            if (hadArmorBeforeFlight) {
                logInfo("Attempting to restore original armor - Land", state.areLogsEnabled);
                tryRestoreOriginalChestplate(client);
            } else {
                logInfo("Landing detected but player had no armor before flight", state.areLogsEnabled);
            }
        }

        if (state.isOnGround && !prevTickOnGround) {
            lastJumpTick = 0L;
        }
    }

    private static boolean hasElytraInInventory(MinecraftClient client) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInventoryCheck > 100) {
            cachedHasElytra = checkForElytraInInventory(client);
            lastInventoryCheck = currentTime;
        }
        return cachedHasElytra;
    }
    
    private static boolean checkForElytraInInventory(MinecraftClient client) {
        return Optional.ofNullable(client.player)
                .map(player -> {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        if (player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    private static boolean isElytraEquipped(MinecraftClient client) {
        return Optional.ofNullable(client.player)
                .map(player -> player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
                .orElse(false);
    }

    public static void toggleElytraChestplate(MinecraftClient client, boolean shouldAutoActivateElytra) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isChestplate(chestItem)) {
                tryEquipElytra(client, shouldAutoActivateElytra);
            } else if (isElytra(chestItem)) {
                tryEquipChestplate(client);
            }
        });
    }

    private static void tryRestoreOriginalChestplate(MinecraftClient client) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isChestplate(currentChest)) {
                logInfo("Already wearing a chestplate", ElytraAssistant.CONFIG.debugSettings.enableLogs);
                return;
            }

            if (!originalChestItem.isEmpty()) {
                int originalChestSlot = findExactItemSlot(client, originalChestItem.getItem());
                if (originalChestSlot != -1) {
                    swapItems(client, originalChestSlot);
                    logInfo("Restored original chestplate", ElytraAssistant.CONFIG.debugSettings.enableLogs);
                    return;
                }
            }

            int chestplateSlot = findItemSlot(client,
                    item -> isChestplate(item.getDefaultStack()),
                    lastWornChestplate);
            if (chestplateSlot != -1) {
                swapItems(client, chestplateSlot);
                logInfo("Chestplate equipped (fallback)", ElytraAssistant.CONFIG.debugSettings.enableLogs);
            }
        });
    }

    private static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    private static final Set<Item> CHESTPLATE_ITEMS = Set.of(
            Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);

    private static boolean isChestplate(ItemStack stack) {
        return CHESTPLATE_ITEMS.contains(stack.getItem());
    }

    public static void tryEquipElytra(MinecraftClient client, boolean shouldActivate) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
            if (currentChest.getItem() == Items.ELYTRA)
                return;

            if (!currentChest.isEmpty()) {
                originalChestItem = currentChest.copy();
                hadArmorBeforeFlight = isChestplate(currentChest);
            } else {
                originalChestItem = ItemStack.EMPTY;
                hadArmorBeforeFlight = false;
            }

            int elytraSlot = findItemSlot(client, item -> item == Items.ELYTRA, lastWornElytra);
            if (elytraSlot != -1) {
                swapItems(client, elytraSlot);
                if (shouldActivate) {
                    activateElytra(client);
                }
                logInfo("Elytra equipped and activated", ElytraAssistant.CONFIG.debugSettings.enableLogs);
            }
        });
    }

    public static void tryEquipChestplate(MinecraftClient client) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isChestplate(currentChest)) {
                logInfo("Already wearing a chestplate", ElytraAssistant.CONFIG.debugSettings.enableLogs);
                return;
            }

            int chestplateSlot = findItemSlot(client,
                    item -> {
                        logInfo("Looking for chestplate...", ElytraAssistant.CONFIG.debugSettings.enableLogs);
                        return isChestplate(item.getDefaultStack());
                    },
                    lastWornChestplate);
            if (chestplateSlot != -1) {
                swapItems(client, chestplateSlot);
                logInfo("Chestplate equipped", ElytraAssistant.CONFIG.debugSettings.enableLogs);
            }
        });
    }

    private static int findItemSlot(MinecraftClient client, Predicate<Item> itemPredicate, ItemStack preferredItem) {
        return Optional.ofNullable(client.player)
                .map(player -> {
                    if (!preferredItem.isEmpty()) {
                        int preferredSlot = findExactItemSlot(client, preferredItem.getItem());
                        if (preferredSlot != -1)
                            return preferredSlot;
                    }

                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (itemPredicate.test(stack.getItem())) {
                            return i;
                        }
                    }
                    return -1;
                })
                .orElse(-1);
    }

    private static int findExactItemSlot(MinecraftClient client, Item item) {
        return Optional.ofNullable(client.player)
                .map(player -> {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        if (player.getInventory().getStack(i).getItem() == item) {
                            return i;
                        }
                    }
                    return -1;
                })
                .orElse(-1);
    }

    private static void swapItems(MinecraftClient client, int inventorySlot) {
        if (client.player == null || client.interactionManager == null) {
            logError("Cannot swap items: client state invalid", null, true);
            return;
        }
        Optional.ofNullable(client.player)
                .flatMap(player -> Optional.ofNullable(client.interactionManager)
                        .map(manager -> new PlayerManagerPair(player, manager)))
                .ifPresent(pair -> {
                    if (inventorySlot == -1)
                        return;

                    ItemStack currentChestSlot = pair.player.getInventory().getStack(inventorySlot);
                    logInfo("Current swap slot: " + inventorySlot + " Item: " + currentChestSlot.getName().getString(),
                            true);

                    if (currentChestSlot.getItem() == Items.ELYTRA) {
                        lastWornElytra = currentChestSlot.copy();
                    } else if (isChestplate(currentChestSlot)) {
                        lastWornChestplate = currentChestSlot.copy();
                    }

                    try {
                        int containerSlot = inventoryToContainerSlot(inventorySlot);
                        int chestplateArmorSlot = 6; // This is the chestplate armor slot in the container
                        logInfo("Converting inventory slot " + inventorySlot + " to container slot " + containerSlot,
                                true);
                        pair.manager.clickSlot(0, containerSlot, 0, SlotActionType.PICKUP, pair.player);
                        pair.manager.clickSlot(0, chestplateArmorSlot, 0, SlotActionType.PICKUP, pair.player);
                        pair.manager.clickSlot(0, containerSlot, 0, SlotActionType.PICKUP, pair.player);
                        logInfo("Swapped items in inventory", ElytraAssistant.CONFIG.debugSettings.enableLogs);
                    } catch (NullPointerException exception) {
                        logError("Error swapping items", exception, ElytraAssistant.CONFIG.debugSettings.enableLogs);
                    }
                });
    }

    private static int inventoryToContainerSlot(int inventorySlot) {
        // Hotbar is 0-8 in inventory but 36-44 in container
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return inventorySlot + 36;
        }
        // Main inventory is 9-35 in both
        else if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        }
        return inventorySlot;
    }

    private static void activateElytra(MinecraftClient client) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            try {
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendPacket(
                            new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    // player.getAbilities().flying = false;
                }
            } catch (NullPointerException exception) {
                logError("Error activating Elytra", exception, ElytraAssistant.CONFIG.debugSettings.enableLogs);
            }
        });
    }

    private static void logInfo(String message, boolean enableDebug) {
        if (enableDebug) {
            ElytraAssistant.LOGGER.info(message);
        }
    }

    private static void logError(String message, Exception e, boolean enableDebug) {
        if (enableDebug) {
            ElytraAssistant.LOGGER.error(message, e);
        }
    }

    private static void logJumpDebugInfo(PlayerState state) {
        if (!state.areLogsEnabled)
            return;

        logInfo("===========================", true);
        logInfo("Jump key pressed. Debug info:", true);
        logInfo("On ground: " + state.isOnGround, true);
        logInfo("Previous tick on ground: " + prevTickOnGround, true);
        logInfo("Air ticks: " + airTicks, true);
        logInfo("Last jump tick: " + lastJumpTick, true);
        logInfo("Current tick: " + state.currentTick, true);
        logInfo("Vertical velocity: " + state.player.getVelocity().y, true);
        logInfo("Fall distance: " + state.player.fallDistance, true);
        logInfo("Has been in air: " + state.hasBeenInAir, true);
        logInfo("Is moving down: " + state.isMovingDown, true);
        logInfo("Is moving up fast: " + state.isMovingUpFast, true);
        logInfo("Has fallen enough: " + state.hasFallenEnough, true);
        logInfo("Is climbing: " + state.isClimbing, true);
        logInfo("Is in fluid: " + state.isInFluid, true);
        logInfo("Was recently in airborne: " + state.wasRecentlyAirborne, true);
        logInfo("Is submerged in water: " + state.isSubmergedInWater, true);
        logInfo("Is swimming: " + state.isSwimming, true);
        logInfo("Is fall flying: " + state.player.getAbilities().flying, true);
        logInfo("isRunning: " + state.isRunning, true);
        logInfo("prevTickJumpKeyPressed: " + prevTickJumpKeyPressed, true);
        logInfo("ticksSinceGrounded: " + ticksSinceGrounded, true);
        logInfo("player.getVelocity().y: " + state.player.getVelocity().y, true);
        logInfo("Current chest item: " + state.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString(), true);
    }

    private static void logMidAirActivationAttempt(PlayerState state) {
        if (state.isInMidAirBalance) {
            logInfo("Attempting to equip Elytra - Mid-air Balance", state.areLogsEnabled);
        } else if (state.isMovingDown) {
            logInfo("Attempting to equip Elytra - Mid-air Falling", state.areLogsEnabled);
        } else {
            logInfo("Attempting to equip Elytra - Mid-air Rising", state.areLogsEnabled);
        }
    }

    private static class PlayerState {
        final ClientPlayerEntity player;
        final MinecraftClient client;
        final long currentTick;
        final boolean areLogsEnabled;
        final boolean wasJumpKeyPressed;
        final boolean wasRecentlyAirborne;
        final boolean isOnGround;
        final boolean isInFluid;
        final boolean isSubmergedInWater;
        final boolean isSwimming;
        final boolean isClimbing;
        final boolean isRunning;
        final boolean isGliding;
        final boolean hasBeenInAir;
        final boolean hasFallenEnough;
        final boolean isInMidAirBalance;
        final boolean isMovingDown;
        final boolean isMovingUp;
        final boolean isMovingUpFast;

        PlayerState(MinecraftClient client) {
            this.client = client;
            this.player = client.player;

            if (player == null) {
                throw new IllegalStateException("Player is null");
            }

            if (client.world == null) {
                throw new IllegalStateException("World is null");
            }

            this.currentTick = client.world.getTime();
            this.areLogsEnabled = ElytraAssistant.CONFIG.debugSettings.enableLogs;
            this.isOnGround = player.isOnGround();
            this.isInFluid = player.isInFluid() || player.isTouchingWater() || player.isSubmergedInWater();
            this.isSubmergedInWater = player.isSubmergedInWater();
            this.isSwimming = player.isSwimming();
            this.isClimbing = player.isClimbing();
            this.wasJumpKeyPressed = client.options.jumpKey.isPressed();
            this.wasRecentlyAirborne = airTicks < RECENTLY_AIRBORNE_THRESHOLD;
            this.isRunning = player.isSprinting()
                    && player.getVelocity().horizontalLength() > RUNNING_VELOCITY_THRESHOLD;
            this.hasBeenInAir = airTicks > ElytraAssistant.CONFIG.sensitivityTweaks.airTicksThreshold;
            this.isInMidAirBalance = Math
                    .abs(player.getVelocity().y) <= ElytraAssistant.CONFIG.sensitivityTweaks.verticalVelocityThreshold
                            / 1000.0;
            this.isMovingDown = player
                    .getVelocity().y < -ElytraAssistant.CONFIG.sensitivityTweaks.verticalVelocityThreshold / 1000.0;
            this.isMovingUp = player
                    .getVelocity().y > ElytraAssistant.CONFIG.sensitivityTweaks.verticalVelocityThreshold / 1000.0;
            this.isMovingUpFast = player.getVelocity().y > Math
                    .min(ElytraAssistant.CONFIG.sensitivityTweaks.verticalVelocityThreshold * 5, 1000) / 1000.0;
            this.hasFallenEnough = player.fallDistance > ElytraAssistant.CONFIG.sensitivityTweaks.minFallDistance
                    / 1000.0;
            this.isGliding = player.isGliding();
        }
    }

    private static class PlayerManagerPair {
        final ClientPlayerEntity player;
        final ClientPlayerInteractionManager manager;

        PlayerManagerPair(ClientPlayerEntity player, ClientPlayerInteractionManager manager) {
            this.player = player;
            this.manager = manager;
        }
    }
}