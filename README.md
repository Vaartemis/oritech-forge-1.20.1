# Oritech Backported (Forge 1.20.1)

Unofficial Forge 1.20.1 backport of [Oritech](https://github.com/Rearth/Oritech) 1.2.13 by Rearth, published with his permission.
This is **not** the official, supporter-verified Oritech. Report backport bugs here, not upstream.

- Requires Forge 47.x, Architectury 9.2+, GeckoLib 4.
- Code: CC0 (as upstream). Some models and textures are by the Techarium team and licensed CC BY-NC 4.0: no commercial use. See LICENSE.
- Recipes, tags and loot tables were converted from the 1.21 data format (`tools/data`). Items that do not exist in 1.20.1
  were replaced (crafter -> crafting table, wind charge -> phantom membrane, heavy core -> netherite block); the Miniaturization
  and Gigantism augments need Pehkui (optional), since 1.20.1 has no scale attribute; without Pehkui they are hidden. Mining speed bonuses use Apothic Attributes when it is installed.

## Additions in this port

- Machines remember the player who placed them. Two player attributes, `oritech:machine_speed` and
  `oritech:machine_efficiency` (base 0, no effect by default), make that player's machines faster (`1 + value` times) and
  cheaper in energy (`1 / (1 + value)`). Modpacks can raise them with any attribute source.
- Stat `oritech:machine_operations` counts the recipes a player's machines finish.

## Build

`KIT=... LIBS=... tools/build.sh` (javac against Mojang-mapped jars, then reobfuscation to SRG). `tools/verify_srg.sh <jar>` checks
that no Mojang names are left. Data conversion: `tools/data/convert.py <upstream root> <out>`, then `fix121.py <resources>`.
