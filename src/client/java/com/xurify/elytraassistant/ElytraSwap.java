package com.xurify.elytraassistant;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraSwap {
  private static final int RECENTLY_AIRBORNE_THRESHOLD = 10;
  private static final double RUNNING_VELOCITY_THRESHOLD = 0.1;
  private static final long DOUBLE_JUMP_WINDOW = 20L;
  private static final int CHESTPLATE_ARMOR_SLOT = 6;
  private static final int HOTBAR_SIZE = 9;
  private static final int HOTBAR_CONTAINER_OFFSET = 36;

  private static ItemStack originalChestItem = ItemStack.EMPTY;
  private static ItemStack lastWornChestplate = ItemStack.EMPTY;
  private static ItemStack lastWornElytra = ItemStack.EMPTY;

  private static boolean isToggling = false;
  private static boolean hadArmorBeforeFlight = false;
  private static boolean prevTickOnGround = true;
  private static boolean prevTickJumpKeyPressed = false;
  private static boolean cachedHasElytra = false;
  private static int lastInventoryHash = 0;
  private static long lastJumpTick = 0;

  private static final AirState airState = new AirState();
  private static final CachedPlayerState cachedPlayerState = new CachedPlayerState();

  public static void init() {
    ClientTickEvents.END_CLIENT_TICK.register(ElytraSwap::onClientTick);
  }

  private static void onClientTick(Minecraft client) {
    handleElytraToggleKeyPress(client);

    if (!isValidTickState(client)) {
      return;
    }

    PlayerState playerState = cachedPlayerState.get(client);

    if (playerState == null) {
      return;
    }

    boolean hasElytraInInventory = hasElytraInInventory(client);

    airState.update(playerState, hasElytraInInventory);

    handleJumpKeyPress(playerState, client, hasElytraInInventory);

    if (hasElytraInInventory) {
      handleMidAirActivation(playerState, client);
      handleFallingDetection(playerState, client);
      handleLandingDetection(playerState, client);
    }

    updatePreviousTickState(playerState);
  }

  private static boolean isValidTickState(Minecraft client) {
    return Optional.ofNullable(client.player)
        .flatMap(player -> Optional.ofNullable(client.level))
        .map(world -> ElytraAssistant.CONFIG.elytraActivationSettings.autoElytraEnabled)
        .orElse(false);
  }

  private static void handleElytraToggleKeyPress(Minecraft client) {
    if (ModKeybindings.elytraToggleKeyBinding.consumeClick() && !isToggling) {
      isToggling = true;
      toggleElytraChestplate(client, true);
      isToggling = false;
    }
  }

  private static void updatePreviousTickState(PlayerState state) {
    prevTickOnGround = state.isOnGround;
    prevTickJumpKeyPressed = state.wasJumpKeyPressed;
  }

  private static void handleJumpKeyPress(
      PlayerState state, Minecraft client, boolean hasElytraInInventory) {
    if (state.wasJumpKeyPressed && !prevTickJumpKeyPressed) {
      logJumpDebugInfo(state);
    }

    if (hasElytraInInventory
        && !state.isOnGround
        && !state.isInFluid
        && state.wasJumpKeyPressed
        && !prevTickJumpKeyPressed) {
      if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
        logInfo("Attempting to equip Elytra - Double Jump");
        tryEquipElytra(client, true);
      }
      lastJumpTick = state.currentTick;
    }
  }

  private static void handleMidAirActivation(PlayerState state, Minecraft client) {
    if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
      return;
    }

    if (!state.isOnGround
        && !state.isInFluid
        && !state.wasRecentlyAirborne
        && !state.isClimbing
        && airState.getAirTicks()
            >= ElytraAssistant.CONFIG.sensitivityTweaks.midAirActivationThreshold
        && state.wasJumpKeyPressed
        && !state.player.getAbilities().flying) {

      logMidAirActivationAttempt(state);
      if (state.isInMidAirBalance || state.isMovingDown || state.isMovingUp) {
        tryEquipElytra(client, true);
      }
    }
  }

  private static void handleFallingDetection(PlayerState state, Minecraft client) {
    if (state.currentTick - lastJumpTick <= DOUBLE_JUMP_WINDOW) {
      return;
    }

    boolean isGliding = state.isGliding;
    if (!state.isOnGround
        && state.hasBeenInAir
        && state.hasFallenEnough
        && !isGliding
        && state.wasJumpKeyPressed) {
      logInfo("Attempting to equip Elytra - Significant Fall");
      tryEquipElytra(client, true);
    } else if (!state.isOnGround && state.isMovingUpFast && !isGliding && state.wasJumpKeyPressed) {
      logInfo("Attempting to equip Elytra - Upward Boost");
      tryEquipElytra(client, true);
    }
  }

  private static void handleLandingDetection(PlayerState state, Minecraft client) {
    if (!prevTickOnGround && state.isOnGround && state.hasBeenInAir) {
      if (hadArmorBeforeFlight) {
        logInfo("Attempting to restore original armor - Land");
        tryRestoreOriginalChestplate(client);
      }
    }

    if (state.isOnGround && !prevTickOnGround) {
      lastJumpTick = 0L;
    }
  }

  private static boolean hasElytraInInventory(Minecraft client) {
    if (client.player == null) return false;

    int currentHash = calculateInventoryHash(client.player);

    if (currentHash != lastInventoryHash) {
      cachedHasElytra = checkForElytraInInventory(client);
      lastInventoryHash = currentHash;
    }
    return cachedHasElytra;
  }

  private static int calculateInventoryHash(LocalPlayer player) {
    int hash = 7;
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      ItemStack stack = player.getInventory().getItem(i);
      if (!stack.isEmpty()) {
        hash = 31 * hash + stack.getItem().hashCode();
      }
    }
    return hash;
  }

  private static boolean checkForElytraInInventory(Minecraft client) {
    return Optional.ofNullable(client.player)
        .map(
            player -> {
              for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem() == Items.ELYTRA) {
                  return true;
                }
              }
              return false;
            })
        .orElse(false);
  }

  private static boolean isElytraEquipped(Minecraft client) {
    return Optional.ofNullable(client.player)
        .map(player -> player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
        .orElse(false);
  }

  public static void toggleElytraChestplate(Minecraft client, boolean shouldAutoActivateElytra) {
    Optional.ofNullable(client.player)
        .ifPresent(
            player -> {
              ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
              if (isChestplate(chestItem)) {
                tryEquipElytra(client, shouldAutoActivateElytra);
              } else if (isElytra(chestItem)) {
                tryEquipChestplate(client);
              }
            });
  }

  private static void tryRestoreOriginalChestplate(Minecraft client) {
    Optional.ofNullable(client.player)
        .ifPresent(
            player -> {
              ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
              if (isChestplate(currentChest)) {
                logInfo("Already wearing a chestplate");
                return;
              }

              if (!originalChestItem.isEmpty()) {
                int originalChestSlot = findExactItemSlot(client, originalChestItem.getItem());
                if (originalChestSlot != -1) {
                  swapItems(client, originalChestSlot);
                  logInfo("Restored original chestplate");
                  return;
                }
              }

              int chestplateSlot =
                  findItemSlot(
                      client, item -> isChestplate(item.getDefaultInstance()), lastWornChestplate);
              if (chestplateSlot != -1) {
                swapItems(client, chestplateSlot);
                logInfo("Chestplate equipped (fallback)");
              }
            });
  }

  private static boolean isElytra(ItemStack stack) {
    return stack.getItem() == Items.ELYTRA;
  }

  private static final Set<Item> CHESTPLATE_ITEMS =
      Set.of(
          Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
          Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE,
          Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);

  private static boolean isChestplate(ItemStack stack) {
    return CHESTPLATE_ITEMS.contains(stack.getItem());
  }

  public static void tryEquipElytra(Minecraft client, boolean shouldActivate) {
    Optional.ofNullable(client.player)
        .ifPresent(
            player -> {
              ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
              if (currentChest.getItem() == Items.ELYTRA) return;

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
                logInfo("Elytra equipped and activated");
              }
            });
  }

  public static void tryEquipChestplate(Minecraft client) {
    Optional.ofNullable(client.player)
        .ifPresent(
            player -> {
              ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
              if (isChestplate(currentChest)) {
                logInfo("Already wearing a chestplate");
                return;
              }

              int chestplateSlot =
                  findItemSlot(
                      client,
                      item -> {
                        logInfo("Looking for chestplate...");
                        return isChestplate(item.getDefaultInstance());
                      },
                      lastWornChestplate);
              if (chestplateSlot != -1) {
                swapItems(client, chestplateSlot);
                logInfo("Chestplate equipped");
              }
            });
  }

  private static int findItemSlot(
      Minecraft client, Predicate<Item> itemPredicate, ItemStack preferredItem) {
    return Optional.ofNullable(client.player)
        .map(
            player -> {
              if (!preferredItem.isEmpty()) {
                int preferredSlot = findExactItemSlot(client, preferredItem.getItem());
                if (preferredSlot != -1) return preferredSlot;
              }

              for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (itemPredicate.test(stack.getItem())) {
                  return i;
                }
              }
              return -1;
            })
        .orElse(-1);
  }

  private static int findExactItemSlot(Minecraft client, Item item) {
    return Optional.ofNullable(client.player)
        .map(
            player -> {
              for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem() == item) {
                  return i;
                }
              }
              return -1;
            })
        .orElse(-1);
  }

  private static void swapItems(Minecraft client, int inventorySlot) {
    if (client.player == null || client.gameMode == null) {
      logError("Cannot swap items: client state invalid", null);
      return;
    }

    if (inventorySlot == -1) {
      return;
    }

    LocalPlayer player = client.player;
    MultiPlayerGameMode manager = client.gameMode;

    ItemStack currentChestSlot = player.getInventory().getItem(inventorySlot);

    logInfoFormat(
        "Current swap slot: %d Item: %s",
        inventorySlot, currentChestSlot.getHoverName().getString());

    if (currentChestSlot.getItem() == Items.ELYTRA) {
      lastWornElytra = currentChestSlot.copy();
    } else if (isChestplate(currentChestSlot)) {
      lastWornChestplate = currentChestSlot.copy();
    }

    try {
      int containerSlot = inventoryToContainerSlot(inventorySlot);

      logInfoFormat(
          "Converting inventory slot %d to container slot %d", inventorySlot, containerSlot);

      manager.handleContainerInput(0, containerSlot, 0, ContainerInput.PICKUP, player);
      manager.handleContainerInput(0, CHESTPLATE_ARMOR_SLOT, 0, ContainerInput.PICKUP, player);
      manager.handleContainerInput(0, containerSlot, 0, ContainerInput.PICKUP, player);

      logInfo("Swapped items in inventory");
    } catch (NullPointerException exception) {
      logError("Error swapping items", exception);
    }
  }

  private static int inventoryToContainerSlot(int inventorySlot) {
    // Hotbar is 0-8 in inventory but 36-44 in container
    if (inventorySlot >= 0 && inventorySlot < ElytraSwap.HOTBAR_SIZE) {
      return inventorySlot + ElytraSwap.HOTBAR_CONTAINER_OFFSET;
    }
    // Main inventory is 9-35 in both
    else if (inventorySlot >= ElytraSwap.HOTBAR_SIZE && inventorySlot <= 35) {
      return inventorySlot;
    }
    return inventorySlot;
  }

  private static void activateElytra(Minecraft client) {
    Optional.ofNullable(client.player)
        .ifPresent(
            player -> {
              try {
                if (client.getConnection() != null) {
                  client
                      .getConnection()
                      .send(
                          new ServerboundPlayerCommandPacket(
                              player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
              } catch (NullPointerException exception) {
                logError("Error activating Elytra", exception);
              }
            });
  }

  private static void logInfo(String message) {
    if (ElytraAssistant.CONFIG.debugSettings.enableLogs) {
      ElytraAssistant.LOGGER.info(message);
    }
  }

  private static void logInfoFormat(String format, Object... args) {
    if (ElytraAssistant.CONFIG.debugSettings.enableLogs) {
      ElytraAssistant.LOGGER.info(String.format(format, args));
    }
  }

  private static void logError(String message, Exception e) {
    if (ElytraAssistant.CONFIG.debugSettings.enableLogs) {
      ElytraAssistant.LOGGER.error(message, e);
    }
  }

  private static void logJumpDebugInfo(PlayerState state) {
    logInfo("===========================");
    logInfo("Jump key pressed. Debug info:");
    logInfo("On ground: " + state.isOnGround);
    logInfo("Previous tick on ground: " + prevTickOnGround);
    logInfo("Air ticks: " + airState.getAirTicks());
    logInfo("Last jump tick: " + lastJumpTick);
    logInfo("Current tick: " + state.currentTick);
    logInfo("Vertical velocity: " + state.player.getDeltaMovement().y);
    logInfo("Fall distance: " + state.player.fallDistance);
    logInfo("Has been in air: " + state.hasBeenInAir);
    logInfo("Is moving down: " + state.isMovingDown);
    logInfo("Is moving up fast: " + state.isMovingUpFast);
    logInfo("Has fallen enough: " + state.hasFallenEnough);
    logInfo("Is climbing: " + state.isClimbing);
    logInfo("Is in fluid: " + state.isInFluid);
    logInfo("Was recently in airborne: " + state.wasRecentlyAirborne);
    logInfo("Is submerged in water: " + state.isSubmergedInWater);
    logInfo("Is swimming: " + state.isSwimming);
    logInfo("Is fall flying: " + state.player.getAbilities().flying);
    logInfo("isRunning: " + state.isRunning);
    logInfo("prevTickJumpKeyPressed: " + prevTickJumpKeyPressed);
    logInfo("ticksSinceGrounded: " + airState.getTicksSinceGrounded());
    logInfo("player.getDeltaMovement().y: " + state.player.getDeltaMovement().y);
    logInfo(
        "Current chest item: "
            + state.player.getItemBySlot(EquipmentSlot.CHEST).getItem().toString());
  }

  private static void logMidAirActivationAttempt(PlayerState state) {
    if (state.isInMidAirBalance) {
      logInfo("Attempting to equip Elytra - Mid-air Balance");
    } else if (state.isMovingDown) {
      logInfo("Attempting to equip Elytra - Mid-air Falling");
    } else {
      logInfo("Attempting to equip Elytra - Mid-air Rising");
    }
  }

  private static class PlayerState {
    final LocalPlayer player;
    final Minecraft client;
    final long currentTick;
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

    PlayerState(Minecraft client) {
      this.client = client;
      this.player = client.player;

      if (player == null) {
        throw new IllegalStateException("Player is null");
      }

      if (client.level == null) {
        throw new IllegalStateException("World is null");
      }

      final double verticalVelocityThreshold =
          ElytraAssistant.CONFIG.sensitivityTweaks.verticalVelocityThreshold / 1000.0;
      final double minFallDistance =
          ElytraAssistant.CONFIG.sensitivityTweaks.minFallDistance / 1000.0;

      final Vec3 velocity = player.getDeltaMovement();
      final double verticalVel = velocity.y;
      final double horizontalLength = velocity.horizontalDistance();

      this.currentTick = client.level.getGameTime();
      this.wasJumpKeyPressed = client.options.keyJump.isDown();
      this.wasRecentlyAirborne = airState.wasRecentlyAirborne();
      this.isOnGround = player.onGround();
      this.isInFluid = player.isInWater() || player.isUnderWater();
      this.isSubmergedInWater = player.isUnderWater();
      this.isSwimming = player.isSwimming();
      this.isClimbing = player.onClimbable();
      this.isRunning = player.isSprinting() && horizontalLength > RUNNING_VELOCITY_THRESHOLD;
      this.hasBeenInAir = airState.hasBeenInAir();
      this.isInMidAirBalance = Math.abs(verticalVel) <= verticalVelocityThreshold;
      this.isMovingDown = verticalVel < -verticalVelocityThreshold;
      this.isMovingUp = verticalVel > verticalVelocityThreshold;
      this.isMovingUpFast = verticalVel > Math.min(verticalVelocityThreshold * 5, 1.0);
      this.hasFallenEnough = player.fallDistance > minFallDistance;
      this.isGliding = player.isFallFlying();
    }
  }

  private static class AirState {
    private int ticksSinceGrounded = 0;
    private int airTicks = 0;
    private int airTime = 0;
    private boolean wasInAir = false;

    public void update(PlayerState state, boolean hasElytraInInventory) {
      if (!state.isOnGround) {
        airTicks++;
        airTime++;
        ticksSinceGrounded++;
        wasInAir = true;
      } else {
        airTicks = 0;
        ticksSinceGrounded = 0;
        if (hasElytraInInventory
            && !isElytraEquipped(state.client)
            && wasInAir
            && airTime > ElytraAssistant.CONFIG.sensitivityTweaks.airTicksThreshold) {
          if (hadArmorBeforeFlight) {
            logInfo("Landing detected. Attempting to equip Chestplate");
            tryRestoreOriginalChestplate(state.client);
          } else {
            logInfo("Landing detected. Player had no armor before flight.");
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

    public int getAirTicks() {
      return airTicks;
    }

    public boolean hasBeenInAir() {
      return airTicks > ElytraAssistant.CONFIG.sensitivityTweaks.airTicksThreshold;
    }

    public boolean wasRecentlyAirborne() {
      return airTicks < RECENTLY_AIRBORNE_THRESHOLD;
    }

    public int getTicksSinceGrounded() {
      return ticksSinceGrounded;
    }
  }

  private static class CachedPlayerState {
    private long lastUpdateTick = -1;
    private PlayerState cachedState;

    public PlayerState get(Minecraft client) {
      if (client.level == null || client.player == null) {
        return null;
      }

      long currentTick = client.level.getGameTime();
      if (lastUpdateTick != currentTick || cachedState == null) {
        try {
          cachedState = new PlayerState(client);
          lastUpdateTick = currentTick;
        } catch (IllegalStateException e) {
          return null;
        }
      }
      return cachedState;
    }
  }
}
