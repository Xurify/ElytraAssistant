# Prompt: Update interactive blocks for DisableFireworkRocket (other MC versions)

Use this prompt when porting or updating `DisableFireworkRocket.java` for a different Minecraft version (e.g. 1.20.x, 1.21.x). Paste it into your AI assistant with the target version and the current file.

---

## Prompt (copy below)

```
We're updating the INTERACTIVE_BLOCKS list in @src/client/java/com/xurify/elytraassistant/DisableFireworkRocket.java for Minecraft [VERSION, e.g. 1.21.1].

Goal: The list should include only blocks that have a generic right-click (use block) interaction — i.e. something meaningful happens when you right-click the block with an empty hand or with any item. We return PASS for these so that when the player is holding firework rockets and right-clicks the block, the block interaction happens instead of the rocket firing.

Do the following:

1) ADD any vanilla blocks that have generic right-click behavior but are missing from the list. Examples that often exist: doors, trapdoors, fence gates; chests, ender chest, shulker, barrel; buttons, lever, repeater, comparator, daylight detector; crafting table, crafter, anvil, furnaces, brewing stand, enchanting table, loom, cartography table, grindstone, stonecutter, smithing table; beacon, bed, note block, composter, bell, lectern, dragon egg; berry bushes and cave vines; candles/candle cake; all sign types; hopper, dispenser, dropper; jukebox (empty hand takes disc); chiseled bookshelf (empty hand takes book); flower pot (empty hand takes plant); structure/jigsaw/command blocks. If the target version's mappings (e.g. Yarn) don't have a class for a block (e.g. FletchingTableBlock), add a Blocks.* identity check in isInteractiveBlock() instead.

2) REMOVE any blocks that do NOT have generic right-click behavior — i.e. they only respond to a specific item (e.g. beehive/bee nest only with bottle or shears; cauldron only with bottle/bucket/dye; respawn anchor only with glowstone; end portal frame only with eye of ender; vault only with trial key; decorated pot only with brush; campfire has no GUI and extinguish/cook are item-based). For those, right-click with rockets does nothing in vanilla anyway, so including them doesn't change behavior and clutters the list.

3) ORGANIZE the list: one section comment per logical group (e.g. "Doors, gates, trapdoors", "Storage blocks", "Redstone components", "Workstation blocks", "Special blocks", "Berry blocks", "Candles", "Signs", "Redstone machines", "Blocks - insert/take or take/place on right-click", "Creative/command blocks"). No per-line comments unless necessary (e.g. Fletching table handled by identity). Consistent formatting with the rest of the file.

After editing, ensure the project still compiles for the target version (e.g. run gradle compileJava). If a block class doesn't exist in this version's mappings, use a Blocks.* identity check in isInteractiveBlock() and remove the class from the set and imports.
```

---

## Quick reference: blocks to exclude (item-only)

- Beehive / Bee nest (bottle or shears only)
- Cauldron (bottle, bucket, dye, etc.)
- Campfire (shovel/water to extinguish; food on stick to cook)
- Respawn anchor (glowstone only)
- End portal frame (eye of ender only)
- Decorated pot (brush for loot)
- Vault (trial key only)

## Quick reference: blocks that typically have generic right-click

- Doors, trapdoors, fence gates
- Chest, ender chest, shulker, barrel, shelf (if present)
- Buttons, lever, repeater, comparator, daylight detector
- All workstations that open a GUI (crafting, crafter, anvil, furnaces, brewing, enchanting, loom, cartography, grindstone, stonecutter, smithing; fletching via identity if no class)
- Beacon, bed, note block, composter, bell, lectern, dragon egg
- Berry bushes, cave vines; candles / candle cake
- All sign types
- Hopper, dispenser, dropper
- Jukebox (empty hand takes disc), chiseled bookshelf (empty hand takes book), flower pot (empty hand takes plant)
- Structure, jigsaw, command blocks
