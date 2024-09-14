package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;
import java.util.function.Predicate;

public class ElytraSwap {
    private static final int RECENTLY_AIRBORNE_THRESHOLD = 10;
    private static final double RUNNING_VELOCITY_THRESHOLD = 0.1;
    private static final long DOUBLE_JUMP_WINDOW = 20L;

    private static boolean isToggling = false;
    private static ItemStack lastWornChestplate = ItemStack.EMPTY;
    private static ItemStack lastWornElytra = ItemStack.EMPTY;
    private static boolean prevTickOnGround = true;
    private static boolean prevTickJumpKeyPressed = false;
    private static int ticksSinceGrounded = 0;
    private static int airTicks = 0;
    private static long lastJumpTick = 0;
    private static boolean wasInAir = false;
    private static int airTime = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ElytraSwap::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        handleElytraToggleKeyPress(client);

        if (!isValidTickState(client)) return;

        PlayerState playerState = new PlayerState(client);

        updateAirState(playerState);
        handleJumpKeyPress(playerState, client);
        handleMidAirActivation(playerState, client);
        handleFallingDetection(playerState, client);
        handleLandingDetection(playerState, client);

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
            toggleElytraChestplate(client);
            isToggling = false;
        }
    }

    private static void updateAirState(PlayerState state) {
        if (!state.isOnGround) {
            airTicks++;
            airTime++;
            ticksSinceGrounded++;
            wasInAir = true;
        } else {
            airTicks = 0;
            ticksSinceGrounded = 0;
            if (!isElytraEquipped(state.client) && wasInAir && airTime > ElytraAssistant.CONFIG.elytraActivationSettings.airTicksThreshold) {
                logInfo("Landing detected. Attempting to equip Chestplate", state.areLogsEnabled);
                tryEquipChestplate(state.client);
                wasInAir = false;
                airTime = 0;
            }
        }

        if (state.isInFluid) {
            airTicks = 0;
        }
    }

    private static void handleJumpKeyPress(PlayerState state, MinecraftClient client) {
        if (state.areLogsEnabled && state.wasJumpKeyPressed && !prevTickJumpKeyPressed) {
            logJumpDebugInfo(state);
        }

        if (!state.isOnGround && !state.isInFluid && state.wasJumpKeyPressed && !prevTickJumpKeyPressed) {
            if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
                logInfo("Attempting to equip Elytra - Double Jump", state.areLogsEnabled);
                tryEquipElytra(client, true);
            }
            lastJumpTick = state.currentTick;
        }
    }

    private static void handleMidAirActivation(PlayerState state, MinecraftClient client) {
        if (!state.isOnGround && !state.isInFluid && !state.wasRecentlyAirborne && !state.isClimbing
                && airTicks >= ElytraAssistant.CONFIG.elytraActivationSettings.midAirActivationThreshold
                && state.wasJumpKeyPressed && !state.player.isFallFlying()) {

            logMidAirActivationAttempt(state);
            if (state.isInMidAirBalance || state.isMovingDown || state.isMovingUp) {
                tryEquipElytra(client, true);
            }
        }
    }

    private static void handleFallingDetection(PlayerState state, MinecraftClient client) {
        if (!state.isOnGround && state.hasBeenInAir && state.hasFallenEnough && !state.player.isFallFlying() && state.wasJumpKeyPressed) {
            logInfo("Attempting to equip Elytra - Significant Fall", state.areLogsEnabled);
            tryEquipElytra(client, true);
        } else if (!state.isOnGround && state.isMovingUpFast && !state.player.isFallFlying() && state.wasJumpKeyPressed) {
            logInfo("Attempting to equip Elytra - Upward Boost", state.areLogsEnabled);
            tryEquipElytra(client, true);
        }
    }

    private static void handleLandingDetection(PlayerState state, MinecraftClient client) {
        if (!prevTickOnGround && state.isOnGround && state.hasBeenInAir) {
            logInfo("Attempting to equip Chestplate - Land", state.areLogsEnabled);
            tryEquipChestplate(client);
        }

        if (state.isOnGround && !prevTickOnGround) {
            lastJumpTick = 0L;
        }
    }

    private static void updatePreviousTickState(PlayerState state) {
        prevTickOnGround = state.isOnGround;
        prevTickJumpKeyPressed = state.wasJumpKeyPressed;
    }

    private static boolean isElytraEquipped(MinecraftClient client) {
        return Optional.ofNullable(client.player)
                .map(player -> player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)
                .orElse(false);
    }

    public static void toggleElytraChestplate(MinecraftClient client) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isChestplate(chestItem)) {
                tryEquipElytra(client, false);
            } else if (isElytra(chestItem)) {
                tryEquipChestplate(client);
            }
        });
    }

    private static boolean isElytra(ItemStack stack) {
        return stack.getItem() instanceof ElytraItem || stack.getItem() instanceof FabricElytraItem;
    }

    private static boolean isChestplate(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem && ((ArmorItem) stack.getItem()).getSlotType() == EquipmentSlot.CHEST;
    }

    public static void tryEquipElytra(MinecraftClient client, boolean shouldActivate) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
            if (currentChest.getItem() instanceof ElytraItem) return;

            int elytraSlot = findItemSlot(client, item -> item instanceof ElytraItem, lastWornElytra);
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
            if (currentChest.getItem() instanceof ArmorItem) return;

            int chestplateSlot = findItemSlot(client,
                    item -> item instanceof ArmorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST,
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
                        if (preferredSlot != -1) return preferredSlot;
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

    private static void swapItems(MinecraftClient client, int slot) {
        Optional.ofNullable(client.player)
                .flatMap(player -> Optional.ofNullable(client.interactionManager)
                        .map(manager -> new PlayerManagerPair(player, manager)))
                .ifPresent(pair -> {
                    if (slot == -1) return;

                    ItemStack currentChestSlot = pair.player.getInventory().getArmorStack(2);

                    if (currentChestSlot.getItem() instanceof ElytraItem) {
                        lastWornElytra = currentChestSlot.copy();
                    } else if (currentChestSlot.getItem() instanceof ArmorItem) {
                        lastWornChestplate = currentChestSlot.copy();
                    }

                    try {
                        pair.manager.clickSlot(0, slot, 0, SlotActionType.PICKUP, pair.player);
                        pair.manager.clickSlot(0, 6, 0, SlotActionType.PICKUP, pair.player);
                        pair.manager.clickSlot(0, slot, 0, SlotActionType.PICKUP, pair.player);
                    } catch (NullPointerException exception) {
                        logError("Error swapping items", exception, ElytraAssistant.CONFIG.debugSettings.enableLogs);
                    }
                });
    }

    private static void activateElytra(MinecraftClient client) {
        Optional.ofNullable(client.player).ifPresent(player -> {
            try {
                client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                player.startFallFlying();
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
        if (!state.areLogsEnabled) return;

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
        logInfo("Is fall flying: " + state.player.isFallFlying(), true);
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
        final boolean isOnGround;
        final boolean isInFluid;
        final boolean isSubmergedInWater;
        final boolean isSwimming;
        final boolean isClimbing;
        final boolean wasJumpKeyPressed;
        final boolean wasRecentlyAirborne;
        final boolean isRunning;
        final boolean hasBeenInAir;
        final boolean isInMidAirBalance;
        final boolean isMovingDown;
        final boolean isMovingUp;
        final boolean isMovingUpFast;
        final boolean hasFallenEnough;
        final long currentTick;
        final boolean areLogsEnabled;

        PlayerState(MinecraftClient client) {
            this.client = client;
            this.player = client.player;
            this.isOnGround = player.isOnGround();
            this.isInFluid = player.isInFluid() || player.isTouchingWater() || player.isSubmergedInWater();
            this.isSubmergedInWater = player.isSubmergedInWater();
            this.isSwimming = player.isSwimming();
            this.isClimbing = player.isClimbing();
            this.wasJumpKeyPressed = client.options.jumpKey.isPressed();
            this.wasRecentlyAirborne = airTicks < RECENTLY_AIRBORNE_THRESHOLD;
            this.isRunning = player.isSprinting() && player.getVelocity().horizontalLength() > RUNNING_VELOCITY_THRESHOLD;
            this.hasBeenInAir = airTicks > ElytraAssistant.CONFIG.elytraActivationSettings.airTicksThreshold;
            this.isInMidAirBalance = Math.abs(player.getVelocity().y) <= ElytraAssistant.CONFIG.elytraActivationSettings.verticalVelocityThreshold / 1000.0;
            this.isMovingDown = player.getVelocity().y < -ElytraAssistant.CONFIG.elytraActivationSettings.verticalVelocityThreshold / 1000.0;
            this.isMovingUp = player.getVelocity().y > ElytraAssistant.CONFIG.elytraActivationSettings.verticalVelocityThreshold / 1000.0;
            this.isMovingUpFast = player.getVelocity().y > ElytraAssistant.CONFIG.elytraActivationSettings.upwardVelocityThreshold / 1000.0;
            this.hasFallenEnough = player.fallDistance > ElytraAssistant.CONFIG.elytraActivationSettings.minFallDistance / 1000.0;
            this.currentTick = client.world.getTime();
            this.areLogsEnabled = ElytraAssistant.CONFIG.debugSettings.enableLogs;
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