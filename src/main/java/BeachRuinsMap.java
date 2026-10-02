public final class BeachRuinsMap extends GameMap {
    public BeachRuinsMap() {
        super("Beach Ruins", "/main/resources/maps/BeachRuins.jpg");
        blockWater();
        // Painted bridges and stair runs cross narrow channels; keep only their decks walkable.
        keepTerrainPassage(276, 180, 48, 100);
        keepTerrainPassage(530, 182, 48, 105);
        keepTerrainPassage(784, 278, 50, 100);
        keepTerrainPassage(1056, 342, 48, 98);
        keepTerrainPassage(1440, 278, 48, 110);
        keepTerrainPassage(316, 526, 46, 110);
        keepTerrainPassage(1008, 536, 50, 104);
        keepTerrainPassage(1224, 786, 52, 112);
        keepTerrainPassage(344, 888, 64, 74);
        keepTerrainPassage(850, 948, 56, 76);
        // Chunky shoreline rocks, palms, and ruin walls. Bridges/stairs are deliberately open.
        block(104, 36, 118, 112);
        block(584, 28, 104, 155);
        block(1210, 34, 190, 115);
        block(108, 320, 155, 105);
        block(658, 335, 68, 125);
        block(812, 335, 66, 125);
        block(726, 335, 86, 28);
        block(1245, 294, 160, 110);
        block(227, 570, 132, 94);
        block(663, 585, 125, 112);
        block(1160, 562, 184, 105);
        block(145, 820, 190, 120);
        block(692, 852, 186, 105);
        block(1222, 812, 200, 140);
    }
}