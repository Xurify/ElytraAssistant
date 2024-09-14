package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

import java.util.HashSet;
import java.util.Set;

public class DisableFireworkRocket {
    private static final Set<Class<? extends Block>> INTERACTIVE_BLOCKS = new HashSet<>();

    static {
        INTERACTIVE_BLOCKS.add(DoorBlock.class);
        INTERACTIVE_BLOCKS.add(TrapdoorBlock.class);
        INTERACTIVE_BLOCKS.add(FenceGateBlock.class);
        INTERACTIVE_BLOCKS.add(ChestBlock.class);
        INTERACTIVE_BLOCKS.add(EnderChestBlock.class);
        INTERACTIVE_BLOCKS.add(ShulkerBoxBlock.class);
        INTERACTIVE_BLOCKS.add(BarrelBlock.class);
        INTERACTIVE_BLOCKS.add(ButtonBlock.class);
        INTERACTIVE_BLOCKS.add(LeverBlock.class);
        INTERACTIVE_BLOCKS.add(CraftingTableBlock.class);
        INTERACTIVE_BLOCKS.add(AnvilBlock.class);
        INTERACTIVE_BLOCKS.add(AbstractFurnaceBlock.class);
        INTERACTIVE_BLOCKS.add(BrewingStandBlock.class);
        INTERACTIVE_BLOCKS.add(EnchantingTableBlock.class);
        INTERACTIVE_BLOCKS.add(BeaconBlock.class);
        INTERACTIVE_BLOCKS.add(SignBlock.class);
        INTERACTIVE_BLOCKS.add(BedBlock.class);
        INTERACTIVE_BLOCKS.add(JukeboxBlock.class);
        INTERACTIVE_BLOCKS.add(NoteBlock.class);
        INTERACTIVE_BLOCKS.add(RedstoneOreBlock.class);
        INTERACTIVE_BLOCKS.add(CampfireBlock.class);
        INTERACTIVE_BLOCKS.add(ComposterBlock.class);
        INTERACTIVE_BLOCKS.add(SweetBerryBushBlock.class);
        INTERACTIVE_BLOCKS.add(CakeBlock.class);
        INTERACTIVE_BLOCKS.add(RespawnAnchorBlock.class);
        INTERACTIVE_BLOCKS.add(LoomBlock.class);
        INTERACTIVE_BLOCKS.add(CartographyTableBlock.class);
        INTERACTIVE_BLOCKS.add(GrindstoneBlock.class);
        INTERACTIVE_BLOCKS.add(BellBlock.class);
        INTERACTIVE_BLOCKS.add(HopperBlock.class);
        INTERACTIVE_BLOCKS.add(DispenserBlock.class);
        INTERACTIVE_BLOCKS.add(DropperBlock.class);
        INTERACTIVE_BLOCKS.add(StructureBlock.class);
        INTERACTIVE_BLOCKS.add(LecternBlock.class);
        INTERACTIVE_BLOCKS.add(FlowerPotBlock.class);
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!ElytraAssistant.CONFIG.generalSettings.preventFireworkGroundPlacement) {
                return ActionResult.PASS;
            }

            ItemStack heldItem = player.getStackInHand(hand);
            if (heldItem.getItem() != Items.FIREWORK_ROCKET || !(player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) {
                return ActionResult.PASS;
            }

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (isInteractiveBlock(block)) {
                ElytraAssistant.LOGGER.debug("Allowing interaction with {} while holding firework rocket and wearing Elytra", block.getName());
                return ActionResult.PASS;
            }

            if (!world.getBlockState(hitResult.getBlockPos()).getCollisionShape(world, hitResult.getBlockPos()).isEmpty()) {
                ElytraAssistant.LOGGER.info("Prevented firework rocket placement while wearing Elytra");
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isInteractiveBlock(Block block) {
        return INTERACTIVE_BLOCKS.contains(block.getClass());
    }
}
