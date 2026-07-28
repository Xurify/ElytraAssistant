# Rocket Restriction Design

## Goal

Replace two overlapping rocket toggles with one explicit mode.

## Modes

- `Off`: allow rockets normally.
- `While wearing Elytra`: block rockets while an Elytra is equipped unless the player is fall-flying.
- `Flight only`: block rockets unless the player is fall-flying.

## Compatibility

Keep the existing boolean fields as hidden legacy config fields. On first load of an older config, map the two values to the equivalent mode, mark the migration complete, and save the result. Existing behavior maps as follows:

| Existing values | New mode |
| --- | --- |
| both off | Off |
| wearing-Elytra on only | While wearing Elytra |
| global restriction on | Flight only |

## Runtime behavior

Use the selected mode in item-in-air, block-use, and entity-use callbacks. A player who is fall-flying may always use a rocket for an Elytra boost.

## Validation

Build the default 26.2 target and the 26.1.x target. Manually test each new mode on the existing MultiMC instances.
