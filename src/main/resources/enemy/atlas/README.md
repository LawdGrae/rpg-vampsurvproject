Enemy atlas crops go here.

Use transparent PNG files named by enemy id, for example:

- slime.png
- goblin_archer.png
- king_goblin.png
- ancient_treant.png
- frost_dragon.png
- nightmare_lord.png

Optional death frames can use the matching `_death.png` suffix, for example
`king_goblin_death.png`.

The Java enemy catalog loads these paths first. If a crop is missing, the game
falls back to the existing bundled enemy sheets so development remains runnable.
