public final class ArenaMap extends GameMap {
    public ArenaMap() {
        super("Arena", "/main/resources/maps/Arena.jpg");
        blockWater();
        // The blue stripe in the Poké Ball is painted floor, not water.
        keepTerrainPassage(600, 350, 336, 336);
        // Start on the open southern approach, below the central logo.
        setStartingPoint(768, 650);
        keepTerrainPassage(710, 140, 118, 210);
        keepTerrainPassage(430, 452, 186, 66);
        keepTerrainPassage(920, 452, 186, 66);
        keepTerrainPassage(710, 674, 118, 174);
        // Outer arena walls, raised cliffs, and fence lines. Stairways into the arena are clear.
        block(0, 0, 1536, 42);
        block(0, 982, 1536, 42);
        block(0, 40, 36, 940);
        block(1500, 40, 36, 940);
        block(190, 145, 355, 42);
        block(990, 145, 355, 42);
        block(200, 845, 360, 40);
        block(980, 845, 360, 40);
        block(305, 248, 160, 116);
        block(1070, 248, 160, 116);
        block(300, 662, 160, 112);
        block(1070, 662, 160, 112);
        block(598, 362, 42, 305);
        block(896, 362, 42, 305);
    }
}