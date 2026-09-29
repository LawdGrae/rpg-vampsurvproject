import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class EnemyCatalog {
    private static final Map<EnemyRegion, List<EnemyDefinition>> NORMAL_BY_REGION =
            new EnumMap<>(EnemyRegion.class);
    private static final Map<EnemyRegion, EnemyDefinition> BOSS_BY_REGION =
            new EnumMap<>(EnemyRegion.class);
    private static final List<EnemyDefinition> ALL_DEFINITIONS = new ArrayList<>();

    static {
        addNormal(EnemyRegion.RIVENDALE_TOWN, "slime", "Slime", false, 12, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "forest_slime", "Forest Slime", false, 10, DamageElement.POISON);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "rock_slime", "Rock Slime", false, 8, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "fire_slime", "Fire Slime", false, 7, DamageElement.FIRE);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "ice_slime", "Ice Slime", false, 7, DamageElement.ICE);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "poison_slime", "Poison Slime", false, 7, DamageElement.POISON);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "goblin", "Goblin", false, 12, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "goblin_archer", "Goblin Archer", true, 8, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.RIVENDALE_TOWN, "goblin_warrior", "Goblin Warrior", false, 7, DamageElement.PHYSICAL);
        addBoss(EnemyRegion.RIVENDALE_TOWN, "king_goblin", "King Goblin",
                DamageElement.PHYSICAL, "Goblin King's Weapon");

        addNormal(EnemyRegion.EMERALD_FOREST, "forest_slime", "Forest Slime", false, 11, DamageElement.POISON);
        addNormal(EnemyRegion.EMERALD_FOREST, "mushroom_monster", "Mushroom Monster", false, 9, DamageElement.POISON);
        addNormal(EnemyRegion.EMERALD_FOREST, "wood_sprite", "Wood Sprite", true, 8, DamageElement.POISON);
        addNormal(EnemyRegion.EMERALD_FOREST, "wolf", "Wolf", false, 10, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.EMERALD_FOREST, "dire_wolf", "Dire Wolf", false, 7, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.EMERALD_FOREST, "treant", "Treant", false, 6, DamageElement.POISON);
        addNormal(EnemyRegion.EMERALD_FOREST, "lizardman", "Lizardman", false, 8, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.EMERALD_FOREST, "lizardman_archer", "Lizardman Archer", true, 6, DamageElement.PHYSICAL);
        addBoss(EnemyRegion.EMERALD_FOREST, "ancient_treant", "Ancient Treant",
                DamageElement.POISON, "Ancient Wood");

        addNormal(EnemyRegion.STONEHILL_PASS, "skeleton", "Skeleton", false, 11, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.STONEHILL_PASS, "skeleton_archer", "Skeleton Archer", true, 8, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.STONEHILL_PASS, "undead_warrior", "Undead Warrior", false, 8, DamageElement.SHADOW);
        addNormal(EnemyRegion.STONEHILL_PASS, "orc", "Orc", false, 10, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.STONEHILL_PASS, "orc_shaman", "Orc Shaman", true, 6, DamageElement.LIGHTNING);
        addNormal(EnemyRegion.STONEHILL_PASS, "stone_golem", "Stone Golem", false, 5, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.STONEHILL_PASS, "harpy", "Harpy", false, 7, DamageElement.PHYSICAL);
        addBoss(EnemyRegion.STONEHILL_PASS, "stone_giant", "Stone Giant",
                DamageElement.PHYSICAL, "Stone Giant Core");

        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "ice_slime", "Ice Slime", false, 10, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "frost_wolf", "Frost Wolf", false, 9, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "ice_elemental", "Ice Elemental", true, 7, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "yeti", "Yeti", false, 7, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "frost_archer", "Frost Archer", true, 7, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "ice_mage", "Ice Mage", true, 6, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "snow_golem", "Snow Golem", false, 5, DamageElement.ICE);
        addNormal(EnemyRegion.FROSTPEAK_MOUNTAINS, "frost_knight", "Frost Knight", false, 6, DamageElement.ICE);
        addBoss(EnemyRegion.FROSTPEAK_MOUNTAINS, "frost_dragon", "Frost Dragon",
                DamageElement.ICE, "Dragon Ice");

        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "wraith", "Wraith", true, 10, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "cursed_archer", "Cursed Archer", true, 8, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "undead_knight", "Undead Knight", false, 8, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "necromancer", "Necromancer", true, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "banshee", "Banshee", true, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "shadow_beast", "Shadow Beast", false, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.SHADOWGRAVE_RUINS, "dread_wraith", "Dread Wraith", false, 5, DamageElement.SHADOW);
        addBoss(EnemyRegion.SHADOWGRAVE_RUINS, "lich_king", "Lich King",
                DamageElement.SHADOW, "Lich Essence");

        addNormal(EnemyRegion.ASHEN_VOLCANO, "fire_slime", "Fire Slime", false, 10, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "lava_elemental", "Lava Elemental", true, 8, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "magma_golem", "Magma Golem", false, 6, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "fire_imp", "Fire Imp", true, 9, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "hell_hound", "Hell Hound", false, 8, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "fire_demon", "Fire Demon", true, 6, DamageElement.FIRE);
        addNormal(EnemyRegion.ASHEN_VOLCANO, "magma_knight", "Magma Knight", false, 6, DamageElement.FIRE);
        addBoss(EnemyRegion.ASHEN_VOLCANO, "fire_lord", "Fire Lord",
                DamageElement.FIRE, "Fire Core");

        addNormal(EnemyRegion.CRIMSON_CITADEL, "crimson_knight", "Crimson Knight", false, 10, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "dark_paladin", "Dark Paladin", false, 8, DamageElement.SHADOW);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "vampire", "Vampire", false, 9, DamageElement.SHADOW);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "blood_knight", "Blood Knight", false, 8, DamageElement.PHYSICAL);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "succubus", "Succubus", true, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "demon_knight", "Demon Knight", false, 7, DamageElement.FIRE);
        addNormal(EnemyRegion.CRIMSON_CITADEL, "abyss_mage", "Abyss Mage", true, 6, DamageElement.SHADOW);
        addBoss(EnemyRegion.CRIMSON_CITADEL, "crimson_emperor", "Crimson Emperor",
                DamageElement.SHADOW, "Crimson Relic");

        addNormal(EnemyRegion.CURSED_WOODS, "poison_slime", "Poison Slime", false, 10, DamageElement.POISON);
        addNormal(EnemyRegion.CURSED_WOODS, "cursed_treant", "Cursed Treant", false, 8, DamageElement.POISON);
        addNormal(EnemyRegion.CURSED_WOODS, "dark_ent", "Dark Ent", false, 8, DamageElement.SHADOW);
        addNormal(EnemyRegion.CURSED_WOODS, "shadow_spider", "Shadow Spider", false, 9, DamageElement.SHADOW);
        addNormal(EnemyRegion.CURSED_WOODS, "cursed_arcanist", "Cursed Arcanist", true, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.CURSED_WOODS, "specter", "Specter", true, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.CURSED_WOODS, "nightmare", "Nightmare", false, 7, DamageElement.SHADOW);
        addNormal(EnemyRegion.CURSED_WOODS, "witch", "Witch", true, 6, DamageElement.POISON);
        addBoss(EnemyRegion.CURSED_WOODS, "nightmare_lord", "Nightmare Lord",
                DamageElement.SHADOW, "Nightmare Essence");
    }

    private EnemyCatalog() {
    }

    public static List<EnemyDefinition> allDefinitions() {
        return Collections.unmodifiableList(ALL_DEFINITIONS);
    }

    public static List<EnemyDefinition> normalEnemies(EnemyRegion region) {
        return NORMAL_BY_REGION.getOrDefault(region, List.of());
    }

    public static EnemyDefinition randomNormal(EnemyRegion region, Random random) {
        List<EnemyDefinition> definitions = normalEnemies(region);
        if (definitions.isEmpty()) {
            definitions = normalEnemies(EnemyRegion.RIVENDALE_TOWN);
        }

        int totalWeight = 0;
        for (EnemyDefinition definition : definitions) {
            totalWeight += definition.getWeight();
        }

        int roll = random.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        for (EnemyDefinition definition : definitions) {
            cumulative += definition.getWeight();
            if (roll < cumulative) {
                return definition;
            }
        }
        return definitions.get(definitions.size() - 1);
    }

    public static EnemyDefinition bossFor(EnemyRegion region) {
        return BOSS_BY_REGION.get(region);
    }

    private static void addNormal(EnemyRegion region, String id, String name,
            boolean ranged, int weight, DamageElement element) {
        int tier = region.getLevelIndex();
        double health = 7.0 + tier * 5.5 + (ranged ? 2.0 : 0.0);
        double damage = 7.0 + tier * 1.9 + (ranged ? 1.5 : 0.0);
        double speed = Math.max(36.0, 58.0 + tier * 3.0 - (ranged ? 7.0 : 0.0));
        int renderSize = 52 + Math.min(20, tier * 3);
        int exp = 3 + tier * 2;
        int gold = 2 + tier;
        EnemyDefinition definition = definition(id, name, region, false, ranged, weight,
                speed, 4.0, 128, 128, renderSize, renderSize * 0.42,
                damage, health, ranged ? 380.0 : renderSize * 0.58,
                ranged ? Math.max(1.2, 2.7 - tier * 0.12) : 0.7,
                ranged ? 170.0 + tier * 18.0 : 0.0,
                Math.min(0.45, tier * 0.05), exp, gold, element,
                name + " Material", fallbackSpriteFor(region, ranged),
                fallbackDeathFor(region, ranged));
        add(definition);
        NORMAL_BY_REGION.computeIfAbsent(region, unused -> new ArrayList<>()).add(definition);
    }

    private static void addBoss(EnemyRegion region, String id, String name,
            DamageElement element, String lootName) {
        int tier = region.getLevelIndex();
        int renderSize = region == EnemyRegion.FROSTPEAK_MOUNTAINS ? 150 : 126 + tier * 4;
        EnemyDefinition definition = definition(id, name, region, true, true, 0,
                48.0 + tier * 2.2, 3.0, 128, 128, renderSize,
                renderSize * 0.43, 230.0 + tier * 72.0,
                260.0 + tier * 110.0, 440.0, Math.max(1.0, 3.2 - tier * 0.14),
                190.0 + tier * 20.0, 0.72, 60 + tier * 14,
                85 + tier * 24, element, lootName,
                fallbackBossSpriteFor(region), fallbackBossSpriteFor(region));
        add(definition);
        BOSS_BY_REGION.put(region, definition);
    }

    private static EnemyDefinition definition(String id, String name, EnemyRegion region,
            boolean boss, boolean ranged, int weight, double speed,
            double animationSpeed, int frameWidth, int frameHeight,
            int renderSize, double collisionRadius, double damage,
            double maxHealth, double attackRange, double attackCooldown,
            double projectileSpeed, double knockbackResistance,
            int experienceReward, int goldReward, DamageElement element,
            String lootName, String fallbackSpritePath, String fallbackDeathPath) {
        String base = "/main/resources/enemy/atlas/" + id + ".png";
        String death = "/main/resources/enemy/atlas/" + id + "_death.png";
        return new EnemyDefinition(id, name, region, boss, ranged, weight, speed,
                animationSpeed, frameWidth, frameHeight, renderSize, collisionRadius,
                damage, maxHealth, attackRange, attackCooldown, projectileSpeed,
                knockbackResistance, experienceReward, goldReward, element, lootName,
                base, death, fallbackSpritePath, fallbackDeathPath);
    }

    private static void add(EnemyDefinition definition) {
        ALL_DEFINITIONS.add(definition);
    }

    private static String fallbackSpriteFor(EnemyRegion region, boolean ranged) {
        if (ranged) {
            return "/main/resources/enemy/LVL2Walks.png";
        }
        return switch (region) {
            case FROSTPEAK_MOUNTAINS -> "/main/resources/enemy/BLUEMINION.png";
            case SHADOWGRAVE_RUINS, CURSED_WOODS -> "/main/resources/enemy/LVL3Walk.png";
            case ASHEN_VOLCANO, CRIMSON_CITADEL -> "/main/resources/enemy/REDMINION.png";
            default -> "/main/resources/enemy/LVL1Walk.png";
        };
    }

    private static String fallbackDeathFor(EnemyRegion region, boolean ranged) {
        if (ranged) {
            return "/main/resources/enemy/LVL2Death.png";
        }
        return switch (region) {
            case SHADOWGRAVE_RUINS, CURSED_WOODS -> "/main/resources/enemy/LVL3Death.png";
            case FROSTPEAK_MOUNTAINS, ASHEN_VOLCANO, CRIMSON_CITADEL ->
                    "/main/resources/enemy/LVL2Death.png";
            default -> "/main/resources/enemy/LVL1Death.png";
        };
    }

    private static String fallbackBossSpriteFor(EnemyRegion region) {
        return switch (region) {
            case CRIMSON_CITADEL, CURSED_WOODS -> "/main/resources/enemy/FINALBOSS.png";
            case ASHEN_VOLCANO, SHADOWGRAVE_RUINS -> "/main/resources/enemy/ELITEBOSS.png";
            default -> "/main/resources/enemy/BOSS.png";
        };
    }
}
