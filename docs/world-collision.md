# Terrain collision

The repeating `src/main/resources/grasstile.png` includes walls, rocks, trees,
and chasms in its artwork. `TerrainLayout` describes their solid ground
footprints in image pixels. Tree branches, grass, and arch openings stay
walkable. Update these shapes when replacing the map image.

The spawn maps to tile pixel `(640, 360)`. `GamePanel` and `WorldCollision`
share that origin so the collision shapes follow the scenery as the camera
moves, including across negative coordinates and repeated tile edges.

Gameplay players receive the terrain in `GameLogic.createSelectedPlayer()`.
Walking, skill dashes, and guardian charges all call `Player.moveWorld()`.
Collision uses a small box at the character's feet, separate from combat's
larger hit radius. Movement advances in short steps and resolves each axis
independently to stop fast movement at thin walls and allow sliding along
their edges.

Standalone portrait players and isolated combat fixtures can omit the terrain.
This collision layer applies to player movement; enemy navigation and attacks
retain their existing behavior.

Run `WorldCollisionSmokeTest` after `build.ps1` to check obstacles, tile seams,
wall sliding, movement abilities, and fresh spawns.
