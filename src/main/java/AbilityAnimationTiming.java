/** Shared visual and damage timing for a skill's anticipation, strike and recovery. */
public final class AbilityAnimationTiming {
    private AbilityAnimationTiming() {
    }

    public static double duration(AbilityDefinition definition) {
        return switch (definition.getId()) {
            case "shield_fortress" -> 0.95;
            case "iron_charge" -> 0.9;
            case "earthbreaker" -> 1.15;
            case "guardians_roar" -> 1.05;
            case "heavy_slash", "shield_bash", "holy_bolt", "fire_bolt" -> 0.64;
            case "earth_shatter", "silent_execution", "divine_light",
                    "lightning_strike", "flame_burst" -> 0.9;
            case "knights_wrath", "elemental_storm", "shadow_assassin",
                    "cataclysm" -> 1.25;
            case "iron_guard", "holy_shield", "blessing" -> 0.95;
            case "shadow_strike", "shadow_step", "ice_shard" -> 0.72;
            case "twin_fang" -> 0.76;
            case "heal" -> 0.82;
            default -> switch (definition.getEffectType()) {
                case ULTIMATE -> 1.15;
                case EXECUTE, SUMMON -> 0.95;
                case DASH, SINGLE_TARGET -> 0.68;
                case SHIELD, BUFF, HEAL -> 0.85;
                default -> 0.75;
            };
        };
    }

    public static double[] hitProgress(AbilityDefinition definition) {
        return switch (definition.getId()) {
            case "shield_fortress" -> new double[] {0.42};
            case "iron_charge" -> new double[] {0.24};
            case "earthbreaker" -> new double[] {0.58};
            case "guardians_roar" -> new double[] {0.44};
            case "twin_fang" -> new double[] {0.34, 0.58};
            case "silent_execution" -> new double[] {0.26, 0.42, 0.58, 0.74};
            case "knights_wrath" -> new double[] {0.2, 0.36, 0.52, 0.74};
            case "elemental_storm" -> new double[] {0.34, 0.54, 0.78};
            case "shadow_assassin" -> new double[] {0.32, 0.5, 0.68};
            case "heavy_slash", "shield_bash", "shadow_strike",
                    "holy_bolt", "fire_bolt", "ice_shard" -> new double[] {0.62};
            case "earth_shatter", "lightning_strike", "divine_light",
                    "flame_burst" -> new double[] {0.56};
            case "iron_guard", "holy_shield", "blessing", "heal" -> new double[] {0.48};
            default -> new double[] {0.55};
        };
    }

    public static double[] hitTimes(AbilityDefinition definition) {
        double duration = duration(definition);
        double[] times = hitProgress(definition);
        for (int index = 0; index < times.length; index++) {
            times[index] = Math.max(0.05, times[index] * duration);
        }
        return times;
    }

    /** The weapon releases a projectile before its later impact at the target. */
    public static double releaseProgress(AbilityDefinition definition) {
        return switch (definition.getId()) {
            case "holy_bolt", "fire_bolt", "dark_bolt", "ice_shard", "shadow_orb",
                    "quick_shot", "power_arrow", "poison_arrow", "explosive_arrow",
                    "phantom_arrow" -> 0.18;
            default -> hitProgress(definition)[0];
        };
    }
}
