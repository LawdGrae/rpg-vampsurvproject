public final class CityMap extends GameMap {
    public CityMap() {
        super("City", "/main/resources/maps/City.jpg");
        // Building footprints and tree rows; road corridors remain open.
        block(40, 92, 286, 225);
        block(435, 105, 120, 192);
        block(640, 100, 190, 180);
        block(1082, 100, 365, 245);
        block(80, 380, 190, 165);
        block(434, 346, 126, 194);
        block(628, 366, 64, 54);
        block(920, 370, 74, 56);
        block(634, 500, 76, 42);
        block(922, 496, 78, 46);
        block(1100, 392, 340, 145);
        block(50, 620, 204, 300);
        block(438, 628, 140, 285);
        block(610, 650, 410, 225);
        block(1080, 620, 365, 300);
        block(0, 0, 1536, 54);
        block(0, 970, 1536, 54);
    }
}