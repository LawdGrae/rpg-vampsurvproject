/** Keeps one reusable scene instance for every authored world map. */
public final class MapCatalog {
    private static final GameMap CITY = new CityMap();
    private static final GameMap BEACH_RUINS = new BeachRuinsMap();
    private static final GameMap ARENA = new ArenaMap();
    private static final GameMap SNOW = new SnowMap();
    private static final GameMap UNDERGROUND_CAVE = new UndergroundCaveMap();
    private static final GameMap DESERT = new DessertMap();
    private static final GameMap HAPPY = new HappyMap();
    private static final GameMap[] SELECTABLE_MAPS = {
        CITY, BEACH_RUINS, ARENA, DESERT, SNOW, UNDERGROUND_CAVE, HAPPY
    };

    private MapCatalog() {
    }

    public static GameMap forRegion(EnemyRegion region) {
        return switch (region) {
            case RIVENDALE_TOWN -> CITY;
            case EMERALD_FOREST -> HAPPY;
            case STONEHILL_PASS -> BEACH_RUINS;
            case FROSTPEAK_MOUNTAINS -> SNOW;
            case SHADOWGRAVE_RUINS -> UNDERGROUND_CAVE;
            case ASHEN_VOLCANO -> DESERT;
            case CRIMSON_CITADEL -> ARENA;
            case CURSED_WOODS -> HAPPY;
        };
    }

    public static int selectableMapCount() {
        return SELECTABLE_MAPS.length;
    }

    public static GameMap selectableMap(int index) {
        if (index < 0 || index >= SELECTABLE_MAPS.length) {
            throw new IndexOutOfBoundsException("No selectable map at index " + index);
        }
        return SELECTABLE_MAPS[index];
    }
}