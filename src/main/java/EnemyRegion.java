public enum EnemyRegion {
    RIVENDALE_TOWN("Rivendale Town", 0),
    EMERALD_FOREST("Emerald Forest", 1),
    STONEHILL_PASS("Stonehill Pass", 2),
    FROSTPEAK_MOUNTAINS("Frostpeak Mountains", 3),
    SHADOWGRAVE_RUINS("Shadowgrave Ruins", 4),
    ASHEN_VOLCANO("Ashen Volcano", 5),
    CRIMSON_CITADEL("Crimson Citadel", 6),
    CURSED_WOODS("Cursed Woods", 7);

    private final String displayName;
    private final int levelIndex;

    EnemyRegion(String displayName, int levelIndex) {
        this.displayName = displayName;
        this.levelIndex = levelIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public static EnemyRegion byIndex(int index) {
        EnemyRegion[] regions = values();
        int clamped = Math.max(0, Math.min(regions.length - 1, index));
        return regions[clamped];
    }
}
