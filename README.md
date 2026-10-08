# RPG Survivor

A Java 21 desktop survival game with five heroes, automatic attacks, four class
skills, elemental combat and eight regional bosses.

## Run

Run these commands in PowerShell from the project root with JDK 21 or newer:

```powershell
.\build.ps1
java -cp out/classes App
```

The game also launches from `src/main/java/App.java` in the IDE. Keep the project
root as the working directory so its resource files can be found.

## Controls

- **WASD / arrow keys:** move. Basic attacks fire automatically.
- **1–4 / click a skill:** cast an equipped ability.
- **Space:** cast the first equipped ability.
- **K:** inspect and rearrange class skills.
- **Esc:** pause, return from skills, or close settings.
- **Enter:** confirm the main menu or hero selection, resume, or restart.

## Survival flow

Each new run and restart begins with the hero falling into the arena, followed
by a landing squash and a short camera shake. An accelerating wind rush leads
into a layered landing thump, while hero-colored trails, ground bloom,
shockwaves, dust and debris give the impact weight. Controls and the survival
clock begin after the one-second entrance; the last particles fade as play
resumes. Esc pauses the entrance and its sounds. The sound setting also mutes
the entrance.

The brighter arena scenery keeps walls and ruins easy to see. The corner
minimap follows the hero and shows nearby terrain, red enemy markers, gold
boss markers and green XP gems. Its arrow shows movement direction; north
stays at the top. The small home marker marks the run's starting point.

The first assault arrives at 28 seconds, with a three-second warning. Pincer
assaults, horde rushes and encirclements alternate every 34 seconds, followed by
a short period with fewer new enemies. Normal spawns approach from varied
directions beyond the visible arena. Boss fights suspend ordinary assaults.
The first regional boss arrives at 95 seconds; defeating it opens the next
region and restores health.

Kill another enemy within six seconds to keep a streak. Every fifth kill in the
streak restores **10 mana** and gives **2 bonus XP**. Longer streaks earn more
points. The HUD shows the streak timer, total kills, score and encounter countdown;
the end-of-run screen shows the score and best streak.

Collect gems to level up and choose a stat upgrade. Excess XP carries forward,
and each earned level presents its own upgrade choice. A restart resets hero
stats, weapon upgrades, cooldowns and run records.

## Checks and visual previews

```powershell
.\build.ps1
java '-Djava.awt.headless=true' -cp out/classes SurvivalGameplaySmokeTest
java '-Djava.awt.headless=true' -cp out/classes GemPickupSmokeTest
java '-Djava.awt.headless=true' -cp out/classes InputStateSmokeTest
java '-Djava.awt.headless=true' -cp out/classes ArenaMinimapSmokeTest out/survival-review/minimap
java '-Djava.awt.headless=true' -cp out/classes RunEntranceSmokeTest
java '-Djava.awt.headless=true' -cp out/classes RunEntranceAudioSmokeTest
java '-Djava.awt.headless=true' -cp out/classes CombatSkillSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillVfxSmokeTest
java '-Djava.awt.headless=true' -cp out/classes UiRenderCheck out/survival-review
java '-Djava.awt.headless=true' -cp out/classes SurvivalRenderCheck out/survival-review
java '-Djava.awt.headless=true' -cp out/classes ContactVfxRenderCheck out/survival-review
java '-Djava.awt.headless=true' -cp out/classes RunEntranceRenderCheck out/survival-review/run-entrance
```

Combat effects use procedural animation when reviewed sprite tracks are absent.
See [combat integration](docs/combat-vfx-integration.md) for the skill system and
additional verification commands.
