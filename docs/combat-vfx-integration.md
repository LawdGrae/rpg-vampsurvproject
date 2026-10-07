# Combat integration status

The motion and combat integration uses Java 21. Skills now have continuous
procedural effects when their reviewed PNG tracks are unavailable, including
weapon trails, elemental casts, projectile travel, shields and contact accents.
Effects use the existing combat clocks and physical attachment points, with
smooth formation and fading instead of abruptly appearing static shapes.

The two original VFX PNG references remain absent from the workspace. Their
source crops have not been approved. A loaded, reviewed action, travel or impact
track takes precedence over its procedural fallback; the fallback is independent
art and does not claim to reproduce those reference frames.

The references contain labels, portraits, overlapping artwork and unequal
examples. They cannot safely be sliced into a regular grid. Import instructions,
explicit frame metadata and the visual review command are in
[the atlas README](../src/main/resources/effects/README.md).

## Implemented behavior

| Class | Active loadout (stable internal IDs) | Passive |
| --- | --- | --- |
| Black Knight | Flame Slash (heavy_slash), Shield Bash, Rising Strike (earth_shatter), Whirlwind (knights_wrath) | Berserker's Will (iron_guard) |
| Assassin | Shadow Strike, Blink Step (shadow_step), Death Mark, Spiral Cut (twin_fang) | Assassin's Instinct (shadow_assassin) |
| Priest | Healing Touch (heal), Holy Light (holy_bolt), Divine Barrier (holy_shield), Judgment (divine_light) | Faith (blessing) |
| Elementalist | Fire Storm (flame_burst), Ice Spear (ice_shard), Thunder Break (lightning_strike), Elemental Nova (elemental_storm) | Elemental Mastery (cataclysm) |
| Guardian | Shield Fortress, Iron Charge, Earthbreaker, Guardian's Roar | Unbreakable |

Skill poses use individual anticipation, action and recovery curves. Equipment
rotates around its calibrated grip; the animated wrist and fingers use the same
attachment. Spell buildup uses the staff head, physical effects use blade tips
or shield rims, and source sampling uses the exact event pose within a timer tick.
Existing body art supplies walking rows rather than authored attack/body-turn
frames, so body acting uses foot-pivot transforms and animated wrists.
True multi-angle body rotation requires additional character frames.

Assassin steps travel along an eased path. Fire Storm launches three accelerating
shots; Ice Spear launches one. Swept collision resolves first contact and starts
impact at the actual hit. Nova and Earthbreaker expand from their physical origin;
an enemy is hit once when the wave reaches it. Deliberate Whirlwind and Spiral Cut
pulses divide their total damage across explicit hit windows.

Death Mark follows the selected living enemy and disappears on death. Barriers
follow the player and receive block feedback. All five passives are excluded from
active slots and mana/cooldowns. Berserker's Will and Unbreakable grow stronger at
low health. Original source-sprite imports remain pending.

## Verification

Run from the project root:

```powershell
.\build.ps1
java '-Djava.awt.headless=true' -cp out/classes CombatSkillSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillPoseSmokeTest
java '-Djava.awt.headless=true' -cp out/classes AnimationVfxSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillVfxSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillEffectAtlasSmokeTest
java '-Djava.awt.headless=true' -cp out/classes GuardianCombatSmokeTest
java '-Djava.awt.headless=true' -cp out/classes CombatMotionRenderCheck out/combat-motion-final
java '-Djava.awt.headless=true' -cp out/classes SkillVfxRenderCheck out/skill-vfx-preview
```

Combat checks cover 120 casts across idle/walking/faster movement in both
directions, all 20 active skills, five passives, resource/cooldown/overlap denial,
repeat use, kills, multiple targets, mark/barrier attachment, recovery and cleanup.
The game has no run key, so faster movement uses the existing speed upgrade.
Additional checks cover launch-clock partition consistency, projectile travel,
wave arrival order, continuous knockback and poison expiration.

`SkillVfxRenderCheck` exports five contact sheets containing 240 runtime snapshots
and five looping GIFs with all 20 active skills. They show actual character poses,
ground/front layering, cast buildup, live projectiles, impact accents and recovery.
The stills cover both horizontal facings, and the GIFs show the full cast and fade.

`SkillVfxSmokeTest` checks deterministic contact feedback across time partitions,
draw-state isolation, invalid timer inputs, projectile-owned animation clocks and
cleanup. A fresh JVM loads temporary reviewed action/travel/impact pixel fixtures
through the production resource path and verifies they take precedence over
procedural art. These fixtures are removed and never become game assets.

Source-frame appearance, crop identification and source-art hit timing still
require importing and visually reviewing the original PNGs. Procedural preview
approval does not approve an unresolved atlas crop.
