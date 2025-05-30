package com.xurify.elytraassistant;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public class DisableFireworkRocket {
    private static final Set<Class<? extends Block>> INTERACTIVE_BLOCKS = new HashSet<>();

    static {
        // Doors, gates, trapdoors - hand interaction (open/close)
        INTERACTIVE_BLOCKS.add(DoorBlock.class);
        INTERACTIVE_BLOCKS.add(TrapdoorBlock.class);
        INTERACTIVE_BLOCKS.add(FenceGateBlock.class);
        
        // Storage blocks - hand interaction (open GUI)
        INTERACTIVE_BLOCKS.add(ChestBlock.class);
        INTERACTIVE_BLOCKS.add(EnderChestBlock.class);
        INTERACTIVE_BLOCKS.add(ShulkerBoxBlock.class);
        INTERACTIVE_BLOCKS.add(BarrelBlock.class);
        
        // Redstone components - hand interaction
        INTERACTIVE_BLOCKS.add(ButtonBlock.class);
        INTERACTIVE_BLOCKS.add(LeverBlock.class);
        INTERACTIVE_BLOCKS.add(RepeaterBlock.class);
        INTERACTIVE_BLOCKS.add(ComparatorBlock.class);
        INTERACTIVE_BLOCKS.add(DaylightDetectorBlock.class);
        
        // Workstation blocks - hand interaction (open GUI)
        INTERACTIVE_BLOCKS.add(CraftingTableBlock.class);
        INTERACTIVE_BLOCKS.add(AnvilBlock.class);
        INTERACTIVE_BLOCKS.add(AbstractFurnaceBlock.class);
        INTERACTIVE_BLOCKS.add(FurnaceBlock.class);
        INTERACTIVE_BLOCKS.add(BlastFurnaceBlock.class);
        INTERACTIVE_BLOCKS.add(SmokerBlock.class);
        INTERACTIVE_BLOCKS.add(BrewingStandBlock.class);
        INTERACTIVE_BLOCKS.add(EnchantingTableBlock.class);
        INTERACTIVE_BLOCKS.add(LoomBlock.class);
        INTERACTIVE_BLOCKS.add(CartographyTableBlock.class);
        INTERACTIVE_BLOCKS.add(GrindstoneBlock.class);
        INTERACTIVE_BLOCKS.add(StonecutterBlock.class);
        INTERACTIVE_BLOCKS.add(SmithingTableBlock.class);
        INTERACTIVE_BLOCKS.add(FletchingTableBlock.class);
        
        // Special blocks - hand interaction
        INTERACTIVE_BLOCKS.add(BeaconBlock.class);
        INTERACTIVE_BLOCKS.add(BedBlock.class);
        INTERACTIVE_BLOCKS.add(NoteBlock.class);
        INTERACTIVE_BLOCKS.add(CampfireBlock.class);
        INTERACTIVE_BLOCKS.add(ComposterBlock.class);
        INTERACTIVE_BLOCKS.add(CakeBlock.class);
        INTERACTIVE_BLOCKS.add(RespawnAnchorBlock.class);
        INTERACTIVE_BLOCKS.add(BellBlock.class);
        INTERACTIVE_BLOCKS.add(LecternBlock.class);
        INTERACTIVE_BLOCKS.add(DragonEggBlock.class);
        
        // Berry blocks - hand interaction (harvest)
        INTERACTIVE_BLOCKS.add(SweetBerryBushBlock.class);
        INTERACTIVE_BLOCKS.add(CaveVinesBodyBlock.class);
        INTERACTIVE_BLOCKS.add(CaveVinesHeadBlock.class);

        // Candles - hand interaction (light/extinguish)
        INTERACTIVE_BLOCKS.add(CandleBlock.class);
        INTERACTIVE_BLOCKS.add(CandleCakeBlock.class);
        
        // Signs - hand interaction (edit text)
        INTERACTIVE_BLOCKS.add(SignBlock.class);
        INTERACTIVE_BLOCKS.add(WallSignBlock.class);
        INTERACTIVE_BLOCKS.add(HangingSignBlock.class);
        INTERACTIVE_BLOCKS.add(WallHangingSignBlock.class);
        
        // Redstone machines - hand interaction (open GUI)
        INTERACTIVE_BLOCKS.add(HopperBlock.class);
        INTERACTIVE_BLOCKS.add(DispenserBlock.class);
        INTERACTIVE_BLOCKS.add(DropperBlock.class);

        // Decorative interactive blocks
        INTERACTIVE_BLOCKS.add(DecoratedPotBlock.class);

        // Creative/command blocks - hand interaction (open GUI)
        INTERACTIVE_BLOCKS.add(StructureBlock.class);
        INTERACTIVE_BLOCKS.add(JigsawBlock.class);
        INTERACTIVE_BLOCKS.add(CommandBlock.class);
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack heldItem = player.getStackInHand(hand);
            boolean isWearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
            boolean isHoldingFireworkRocket = heldItem.getItem() == Items.FIREWORK_ROCKET;


            if (!isHoldingFireworkRocket) {
                return ActionResult.PASS;
            }

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (isInteractiveBlock(block)) {
                return ActionResult.PASS;
            }

            if (ElytraAssistant.CONFIG.fireworkRockets.disableDecorativeExplosionsWhileWearingElytra
                    && isWearingElytra) {
                return ActionResult.FAIL;
            }

            if (ElytraAssistant.CONFIG.fireworkRockets.disableDecorativeExplosionsCompletely) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isInteractiveBlock(Block block) {
        return INTERACTIVE_BLOCKS.stream().anyMatch(blockClass -> blockClass.isInstance(block));
    }
}