# Elytra Assistant

Elytra Assistant is a client-side Minecraft mod that elevates your Elytra flying experience with intelligent activation/swapping, 
flight time estimation, and quality-of-life improvements. Enhance your aerial adventures and optimize 
your resources with smart, seamless controls.

## Features

1. **Elytra Auto-Equip / Chestplate Swap**
    - Automatically swaps between your Elytra and Chestplate as needed.
    - Equips Elytra when jumping from a high place or during flight.
    - Switches back to Chestplate upon landing for better protection.

2. **Estimated Remaining Flight Time Tooltip**
    - Displays an estimate of remaining flight time on the Elytra tooltip.
    - Takes into account the current durability and Unbreaking enchantment level for accurate estimates.

3. **Firework Rocket Waste Prevention**
    - Configurable restriction modes: Off, Elytra equipped, or Flight only.
    - Blocks restricted rockets in air, on blocks, and on entities.
    - Helps conserve resources and avoid wasting fireworks unintentionally.

## Installation

1. Ensure you have Fabric Loader installed.
2. Download the latest version of Elytra Assistant from the releases page.
3. Place the downloaded JAR file in your Minecraft mods folder.
4. Launch Minecraft with the Fabric profile.

## Usage

Once installed, the mod works automatically:

- The Elytra/Chestplate swap occurs automatically based on your actions.
- Hover over an Elytra in your inventory to see the estimated flight time tooltip.
- Configure **Rocket restriction** in Mod Menu:
  - **Off** allows rockets normally.
  - **Elytra equipped** blocks rockets while an Elytra is in the chest slot, unless gliding.
  - **Flight only** blocks rockets unless gliding.
- Restricted rockets are blocked regardless of whether the target is air, a block, or an entity.
- Includes a key bind for manual swapping (default key: ~).

## Configuration

(If your mod has configurable options, describe how to access and modify them here.)

## Development

- **Java:** Java 25 is required for Minecraft 26.x.
- **Build:** `./gradlew build` (or `gradlew.bat build` on Windows)
- **Formatting:** The project uses [Spotless](https://github.com/diffplug/spotless) for Java and JSON. If the build fails on `spotlessJsonCheck` or `spotlessJavaCheck`, run `./gradlew spotlessApply` and try again.

## Compatibility

Elytra Assistant is a client-side mod and should be compatible with most Minecraft servers, including vanilla servers. However, always check server rules regarding the use of client-side mods.

Minecraft 26.1, 26.1.1, and 26.1.2 use one tested `26.1.x` compatibility build. Minecraft 26.2 uses its own build.

## Support

If you encounter any issues or have suggestions for improvement, please open an issue on our GitHub repository.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
