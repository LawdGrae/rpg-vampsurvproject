/** The shared skill clock drives weapon poses, releases and combat events. */
public final class AbilityAnimationTiming {
    public enum State { IDLE, PREPARE, WINDUP, ATTACK, CAST, TRAVEL, IMPACT, FOLLOW_THROUGH, RECOVERY }
    private AbilityAnimationTiming() { }

    public static boolean isReferenceSkill(String id) {
        return switch (id) {
            case "heavy_slash", "shield_bash", "earth_shatter", "knights_wrath", "iron_guard",
                    "shadow_strike", "shadow_step", "death_mark", "twin_fang", "shadow_assassin",
                    "heal", "holy_bolt", "holy_shield", "divine_light", "blessing",
                    "flame_burst", "ice_shard", "lightning_strike", "elemental_storm", "cataclysm",
                    "shield_fortress", "iron_charge", "earthbreaker", "guardians_roar", "unbreakable" -> true;
            default -> false;
        };
    }

    public static double duration(AbilityDefinition d) {
        return switch (d.getId()) {
            case "heavy_slash" -> 0.78;
            case "shield_bash" -> 0.72;
            case "earth_shatter" -> 0.94;
            case "knights_wrath" -> 1.32;
            case "shadow_strike" -> 0.54;
            case "shadow_step" -> 0.60;
            case "death_mark" -> 0.66;
            case "twin_fang" -> 0.70;
            case "heal" -> 1.02;
            case "holy_bolt" -> 1.10;
            case "holy_shield" -> 1.08;
            case "divine_light" -> 1.26;
            case "flame_burst" -> 1.22;
            case "ice_shard" -> 0.96;
            case "lightning_strike" -> 1.16;
            case "elemental_storm" -> 1.40;
            case "shield_fortress" -> 0.95;
            case "iron_charge" -> 0.90;
            case "earthbreaker" -> 1.15;
            case "guardians_roar" -> 1.05;
            case "silent_execution" -> 0.90;
            default -> switch (d.getEffectType()) {
                case ULTIMATE -> 1.25;
                case EXECUTE, SUMMON, SHIELD, BUFF -> 0.95;
                case DASH, SINGLE_TARGET -> 0.68;
                case HEAL -> 0.85;
                default -> 0.75;
            };
        };
    }

    public static double[] hitProgress(AbilityDefinition d) {
        double[] authored = defaultHitProgress(d);
        // Verified source hit frames share the weapon/body clock. Projectile frame
        // events launch a shot; only its eventual contact can apply damage.
        SkillEffectAtlas.SkillAnimation action = SkillEffectAtlas.getAnimation(d.getId(), "action");
        if (action == null || d.getId().equals("flame_burst") || d.getId().equals("ice_shard")) return authored;
        double[] events = action.getHitTimesSeconds();
        if (events.length != authored.length) return authored;
        double release = releaseProgress(d);
        for (int i = 0; i < events.length; i++) {
            double p = release + events[i] / duration(d);
            if (p < release || p > 0.84) return authored;
            authored[i] = p;
        }
        return authored;
    }

    public static String sourceTimingProblem(AbilityDefinition d, SkillEffectAtlas.SkillAnimation action) {
        if (action == null || d.isPassive() || d.getId().equals("flame_burst") || d.getId().equals("ice_shard")) return null;
        double[] events = action.getHitTimesSeconds();
        if (events.length != defaultHitProgress(d).length) return "action hitFrames must match the skill's number of combat events";
        for (double event : events) {
            double p = releaseProgress(d) + event / duration(d);
            if (p > 0.84) return "action hit frame occurs after follow-through; adjust explicit frame timing before review";
        }
        return null;
    }

    /** Projectile entries are launch events; damage is resolved later by contact. */
    private static double[] defaultHitProgress(AbilityDefinition d) {
        return switch (d.getId()) {
            case "shield_fortress" -> new double[] {0.42};
            case "iron_charge" -> new double[] {0.24};
            case "earthbreaker" -> new double[] {0.58};
            case "guardians_roar" -> new double[] {0.44};
            case "twin_fang" -> new double[] {0.40, 0.62};
            case "knights_wrath" -> new double[] {0.30, 0.44, 0.58, 0.72};
            case "flame_burst" -> new double[] {0.42, 0.53, 0.64};
            case "ice_shard" -> new double[] {0.46};
            case "silent_execution" -> new double[] {0.26, 0.42, 0.58, 0.74};
            case "heavy_slash", "shield_bash", "earth_shatter" -> new double[] {0.60};
            case "shadow_strike", "shadow_step" -> new double[] {0.64};
            case "holy_bolt", "divine_light", "lightning_strike" -> new double[] {0.64};
            case "elemental_storm" -> new double[] {0.46};
            case "heal" -> new double[] {0.64};
            case "holy_shield", "death_mark" -> new double[] {0.50};
            default -> new double[] {0.55};
        };
    }

    public static double[] hitTimes(AbilityDefinition d) {
        double[] times = hitProgress(d);
        for (int i = 0; i < times.length; i++) times[i] *= duration(d);
        return times;
    }

    public static double releaseProgress(AbilityDefinition d) {
        return switch (d.getId()) {
            case "shadow_strike", "shadow_step" -> 0.28;
            case "holy_bolt", "divine_light", "lightning_strike", "heal" -> 0.40;
            case "holy_shield", "death_mark" -> 0.34;
            case "heavy_slash", "shield_bash", "earth_shatter" -> 0.38;
            case "twin_fang" -> 0.24;
            case "knights_wrath" -> 0.22;
            case "fire_bolt", "dark_bolt", "shadow_orb", "quick_shot", "power_arrow",
                    "poison_arrow", "explosive_arrow", "phantom_arrow" -> 0.18;
            default -> defaultHitProgress(d)[0];
        };
    }

    public static State stateAt(AbilityDefinition d, double elapsed) {
        if (d == null || d.isPassive() || elapsed >= duration(d)) return State.IDLE;
        double p = Math.max(0, elapsed / duration(d)), release = releaseProgress(d);
        if (p < 0.10) return State.PREPARE;
        if (p < release) return State.WINDUP;
        if ((d.getId().equals("flame_burst") || d.getId().equals("ice_shard")) && p < 0.78) return State.TRAVEL;
        double hit = hitProgress(d)[0];
        if (Math.abs(p - hit) <= 0.025) return State.IMPACT;
        if (p < hit) return switch (d.getAbilityClass()) {
            case PRIEST, ELEMENTALIST -> State.CAST;
            default -> State.ATTACK;
        };
        return p < 0.82 ? State.FOLLOW_THROUGH : State.RECOVERY;
    }
}
