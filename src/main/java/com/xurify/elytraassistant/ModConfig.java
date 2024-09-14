package com.xurify.elytraassistant;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = ElytraAssistant.MOD_ID)
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public GeneralSettings generalSettings = new GeneralSettings();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public ElytraActivationSettings elytraActivationSettings = new ElytraActivationSettings();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public DisplaySettings displaySettings = new DisplaySettings();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public DebugSettings debugSettings = new DebugSettings();

    public static class GeneralSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean preventFireworkGroundPlacement = false;
    }

    public static class ElytraActivationSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean autoElytraEnabled = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
        public int upwardVelocityThreshold = 500; // Stored as int, will be divided by 1000 when used

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int airTicksThreshold = 5;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 10000)
        public int minFallDistance = 3000; // Stored as int, will be divided by 1000 when used

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
        public int verticalVelocityThreshold = 100; // Default value of 0.1 * 1000

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int midAirActivationThreshold = 10;
    }

    public static class DisplaySettings {
        @ConfigEntry.Gui.Tooltip
        public boolean showFlightTime = true;
    }

    public static class DebugSettings {
        public boolean enableLogs = false;
    }
}