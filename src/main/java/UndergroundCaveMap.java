public final class UndergroundCaveMap extends GameMap {
    public UndergroundCaveMap() {
        super("Underground Cave", "/main/resources/maps/UndergroundCave.png");
        blockGlowingPurpleGaps();
        // Only purple ruins, crystals, dead trees and the outer chasm edges are solid.
        block(0, 0, 92, 1024);
        block(1440, 0, 96, 1024);
        block(154, 74, 62, 172);
        block(276, 74, 62, 172);
        block(216, 74, 60, 52);
        block(1110, 282, 78, 190);
        block(1254, 282, 61, 190);
        block(1188, 282, 66, 58);
        block(104, 712, 232, 198);
        block(1224, 742, 192, 190);
        block(468, 758, 120, 168);
        block(742, 132, 82, 95);
        block(100, 330, 80, 138);
        block(840, 510, 105, 115);
        block(1370, 494, 120, 185);
        block(570, 34, 45, 120);
        block(940, 860, 74, 150);
    }
}