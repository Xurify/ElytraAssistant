# Prompt: Update firework restriction callbacks for other MC versions

Use this prompt when porting `DisableFireworkRocket.java` to another Minecraft version.

```text
We're porting @src/client/java/com/xurify/elytraassistant/DisableFireworkRocket.java to Minecraft [VERSION].

Preserve this behavior:

1. Inspect the held item in both hands through the item-use and block-use callbacks.
2. If it is a firework rocket and the configured restriction applies, return the callback's blocking result.
3. Do not whitelist interactive blocks. A restricted rocket must not launch when aimed at a bell, chest, door, workstation, entity, or any other target.
4. Allow rockets while the player is actually fall-flying so Elytra boosts still work.
5. Keep the OFF, WHILE_WEARING_ELYTRA, and FLIGHT_ONLY modes unchanged.
6. Use the target version's callback and result types, then run the full Gradle check.

Do not reintroduce a block-class whitelist. Returning PASS for an interactive block allows vanilla to continue and can cause the rocket to launch.
```

## Validation checklist

- Restricted rocket aimed at air: blocked.
- Restricted rocket aimed at a generic block: blocked.
- Restricted rocket aimed at an interactive block: blocked.
- Restricted rocket used on an entity: blocked if vanilla falls back to item use.
- Rocket while fall-flying: allowed.
- OFF mode: allowed.
- Both hands tested.
