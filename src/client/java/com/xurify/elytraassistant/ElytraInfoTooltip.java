package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ElytraInfoTooltip {

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, lines, list) -> {
            if (stack.getItem() instanceof ElytraItem && ElytraAssistant.CONFIG.displaySettings.showFlightTime) {
                addElytraTooltip(stack, list, context);
            }
        });
    }

    private static void addElytraTooltip(ItemStack itemStack, List<Text> tooltipList, Item.TooltipContext context) {
        int maxDurability = itemStack.getMaxDamage();
        int currentDurability = maxDurability - itemStack.getDamage();
        float durabilityPercentage = (float) currentDurability / maxDurability;

        var unbreakingEnchantment = context.getRegistryLookup().getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        int unbreakingLevel = EnchantmentHelper.getLevel(unbreakingEnchantment, itemStack);

        float durabilityMultiplier = calculateUnbreakingMultiplier(unbreakingLevel);
        int estimatedSeconds = Math.round(currentDurability * durabilityMultiplier);
        String timeString = formatTime(estimatedSeconds);

        Formatting timeColor = getColorForFlightTime(durabilityPercentage, unbreakingLevel);
        MutableText flightTimeText = Text.literal(timeString).formatted(timeColor);
        tooltipList.add(Text.translatable("elytraassistant.tooltip.flightTime", flightTimeText).formatted(Formatting.GRAY));
    }

    private static float calculateUnbreakingMultiplier(int level) {
        if (level <= 0) return 1.0f;
        return (float) (level + 1) / (float) (level * 0.25 + 0.75);
    }

    private static String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static Formatting getColorForFlightTime(float durabilityPercentage, int unbreakingLevel) {
        float effectiveDurability = durabilityPercentage * (1 + unbreakingLevel * 0.5f);
        
        if (effectiveDurability > 0.75f) return Formatting.GREEN;
        if (effectiveDurability > 0.6f) return Formatting.YELLOW;
        if (effectiveDurability > 0.4f) return Formatting.GOLD;
        if (effectiveDurability > 0.25f) return Formatting.RED;
        return Formatting.DARK_RED;
    }
}