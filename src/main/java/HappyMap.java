public final class HappyMap extends GameMap {
    public HappyMap() {
        super("Happy Map", "/main/resources/maps/HappyMap.jpg");
        useGrayscale();
        blockDarkGaps();
        // Trees, rocks and walls are blocked; local footprints preserve the paths between them.
        block(0, 0, 96, 1024);
        block(1440, 0, 96, 1024);
        block(160, 95, 125, 90);
        block(485, 110, 128, 96);
        block(870, 78, 122, 98);
        block(1220, 136, 180, 126);
        block(58, 360, 130, 98);
        block(386, 332, 144, 115);
        block(1020, 375, 148, 100);
        block(1300, 530, 175, 110);
        block(174, 665, 185, 118);
        block(740, 700, 108, 94);
        block(1125, 788, 196, 112);
        block(388, 895, 160, 105);
        block(632, 438, 260, 52);
    }
}