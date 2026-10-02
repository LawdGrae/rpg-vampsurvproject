public final class DessertMap extends GameMap {
    public DessertMap() {
        super("Desert", "/main/resources/maps/Dessert.jpg");
        blockWater();
        // Rock walls and outcrops only; open sand routes lead to every stair/portal.
        block(0, 0, 174, 85);
        block(238, 0, 130, 72);
        block(620, 0, 190, 90);
        block(862, 0, 145, 70);
        block(1282, 0, 254, 86);
        block(250, 180, 145, 108);
        block(470, 70, 95, 208);
        block(790, 110, 120, 120);
        block(1048, 190, 120, 190);
        block(0, 350, 160, 155);
        block(588, 312, 184, 132);
        block(1370, 292, 166, 160);
        block(410, 470, 206, 78);
            block(700, 430, 50, 100);
            block(840, 430, 50, 100);
            block(750, 430, 90, 30);
        block(1110, 560, 228, 125);
        block(180, 640, 220, 85);
            block(0, 900, 86, 124);
        block(435, 882, 236, 142);
        block(890, 914, 220, 110);
        block(1300, 910, 236, 114);
        // Portal pairs move the player horizontally across the desert.
        addHorizontalPortal(176, 122, 1138, 96, 46);
        addHorizontalPortal(138, 916, 1378, 865, 46);
    }
}