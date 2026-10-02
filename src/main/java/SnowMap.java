public final class SnowMap extends GameMap {
    public SnowMap() {
        super("Snow", "/main/resources/maps/Snow.jpg");
        blockWater();
        // Ice shards, trees, cave mouth, ruins, and cliff faces; leave snowfield routes open.
        block(0, 20, 145, 105);
        block(230, 20, 80, 105);
        block(314, 8, 96, 180);
        block(650, 0, 156, 142);
        block(1185, 0, 351, 150);
        block(538, 320, 164, 175);
        block(1090, 398, 84, 170);
        block(62, 575, 116, 105);
        block(357, 710, 112, 145);
        block(0, 838, 185, 186);
        block(276, 838, 6, 186);
        block(180, 815, 270, 94);
        block(180, 909, 20, 87);
        block(274, 909, 176, 87);
        block(1168, 786, 368, 238);
        block(628, 764, 160, 112);
        block(876, 584, 140, 94);
        // The cliff entrance staircase, the ruined-temple stairs, and the dock stay passable.
        keepTerrainPassage(145, 64, 90, 90);
        keepTerrainPassage(190, 920, 84, 76);
        keepTerrainPassage(944, 205, 104, 106);
    }
}