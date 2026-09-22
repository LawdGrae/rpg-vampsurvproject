import java.util.ArrayList;
import java.util.List;

public class AbilityDefinition {
    private final String id;
    private final String name;
    private final AbilityClass abilityClass;
    private final AbilityEffectType effectType;
    private final String description;
    private final String iconPath;
    private final int unlockLevel;
    private final double cooldownSeconds;
    private final double manaCost;
    private final double damage;
    private final double radius;
    private final double duration;

    public AbilityDefinition(String id, String name, AbilityClass abilityClass,
            AbilityEffectType effectType, String description, int unlockLevel,
            double cooldownSeconds, double manaCost, double damage,
            double radius, double duration) {
        this.id = id;
        this.name = name;
        this.abilityClass = abilityClass;
        this.effectType = effectType;
        this.description = description;
        this.iconPath = "/main/resources/abilities/" + id + ".png";
        this.unlockLevel = unlockLevel;
        this.cooldownSeconds = cooldownSeconds;
        this.manaCost = manaCost;
        this.damage = damage;
        this.radius = radius;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AbilityClass getAbilityClass() {
        return abilityClass;
    }

    public AbilityEffectType getEffectType() {
        return effectType;
    }

    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return iconPath;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getManaCost() {
        return manaCost;
    }

    public double getDamage() {
        return damage;
    }

    public double getRadius() {
        return radius;
    }

    public double getDuration() {
        return duration;
    }

    public boolean isUnlockedAt(int playerLevel) {
        return playerLevel >= unlockLevel;
    }

    public static List<AbilityDefinition> createAll() {
        List<AbilityDefinition> abilities = new ArrayList<>();
        addAssassin(abilities);
        addBlackKnight(abilities);
        addPriest(abilities);
        addRanger(abilities);
        addWarlock(abilities);
        addElementalist(abilities);
        return abilities;
    }

    private static void addAssassin(List<AbilityDefinition> abilities) {
        add(abilities, "shadow_strike", "Shadow Strike", AbilityClass.ASSASSIN, AbilityEffectType.SINGLE_TARGET, "Blink damage to the nearest enemy.", 1, 6, 15, 16, 240, 0);
        add(abilities, "twin_fang", "Twin Fang", AbilityClass.ASSASSIN, AbilityEffectType.AREA_DAMAGE, "Two quick cuts around the hero.", 2, 8, 20, 14, 115, 0);
        add(abilities, "poison_blade", "Poison Blade", AbilityClass.ASSASSIN, AbilityEffectType.POISON, "Poison nearby enemies.", 3, 10, 22, 5, 150, 5);
        add(abilities, "smoke_veil", "Smoke Veil", AbilityClass.ASSASSIN, AbilityEffectType.SLOW, "Smoke slows enemies and softens incoming hits.", 4, 14, 26, 2, 180, 4);
        add(abilities, "shadow_step", "Shadow Step", AbilityClass.ASSASSIN, AbilityEffectType.DASH, "Dash through danger and cut nearby foes.", 5, 9, 24, 12, 120, 0.4);
        add(abilities, "venom_burst", "Venom Burst", AbilityClass.ASSASSIN, AbilityEffectType.POISON, "A larger poison detonation.", 6, 16, 34, 8, 230, 6);
        add(abilities, "phantom_clone", "Phantom Clone", AbilityClass.ASSASSIN, AbilityEffectType.SUMMON, "A phantom burst distracts and damages enemies.", 7, 22, 40, 20, 210, 5);
        add(abilities, "death_mark", "Death Mark", AbilityClass.ASSASSIN, AbilityEffectType.MARK, "Mark enemies to take extra damage.", 8, 18, 38, 8, 250, 6);
        add(abilities, "silent_execution", "Silent Execution", AbilityClass.ASSASSIN, AbilityEffectType.EXECUTE, "Heavy damage, executing weakened targets.", 9, 25, 52, 44, 220, 0);
        add(abilities, "shadow_assassin", "Shadow Assassin", AbilityClass.ASSASSIN, AbilityEffectType.ULTIMATE, "A lethal storm of shadow cuts.", 10, 45, 80, 58, 300, 3);
    }

    private static void addBlackKnight(List<AbilityDefinition> abilities) {
        add(abilities, "heavy_slash", "Heavy Slash", AbilityClass.BLACK_KNIGHT, AbilityEffectType.AREA_DAMAGE, "A broad close-range slash.", 1, 7, 16, 18, 125, 0);
        add(abilities, "shield_bash", "Shield Bash", AbilityClass.BLACK_KNIGHT, AbilityEffectType.STUN, "Stun enemies in front of the hero.", 2, 10, 20, 10, 140, 1.5);
        add(abilities, "iron_guard", "Iron Guard", AbilityClass.BLACK_KNIGHT, AbilityEffectType.SHIELD, "Gain temporary armor.", 3, 14, 24, 0, 0, 5);
        add(abilities, "dark_taunt", "Dark Taunt", AbilityClass.BLACK_KNIGHT, AbilityEffectType.SLOW, "Slow enemies in a wide dark aura.", 4, 15, 28, 4, 230, 4);
        add(abilities, "shield_charge", "Shield Charge", AbilityClass.BLACK_KNIGHT, AbilityEffectType.DASH, "Charge forward and slam nearby enemies.", 5, 12, 30, 18, 150, 0.4);
        add(abilities, "earth_shatter", "Earth Shatter", AbilityClass.BLACK_KNIGHT, AbilityEffectType.STUN, "Crack the ground and stun enemies.", 6, 20, 42, 22, 230, 1.8);
        add(abilities, "counter_strike", "Counter Strike", AbilityClass.BLACK_KNIGHT, AbilityEffectType.BUFF, "Prepare a damage-boosting counter stance.", 7, 18, 36, 0, 0, 6);
        add(abilities, "blood_armor", "Blood Armor", AbilityClass.BLACK_KNIGHT, AbilityEffectType.SHIELD, "Gain stronger armor and recover health.", 8, 24, 48, 12, 160, 7);
        add(abilities, "knights_wrath", "Knight's Wrath", AbilityClass.BLACK_KNIGHT, AbilityEffectType.AREA_DAMAGE, "Release a punishing wrath wave.", 9, 28, 58, 45, 270, 0);
        add(abilities, "dark_fortress", "Dark Fortress", AbilityClass.BLACK_KNIGHT, AbilityEffectType.ULTIMATE, "Become a fortress and crush nearby enemies.", 10, 48, 85, 52, 320, 8);
    }

    private static void addPriest(List<AbilityDefinition> abilities) {
        add(abilities, "holy_bolt", "Holy Bolt", AbilityClass.PRIEST, AbilityEffectType.SINGLE_TARGET, "Holy damage to the nearest enemy.", 1, 5, 12, 14, 280, 0);
        add(abilities, "heal", "Heal", AbilityClass.PRIEST, AbilityEffectType.HEAL, "Restore health.", 2, 10, 22, 20, 0, 0);
        add(abilities, "blessing", "Blessing", AbilityClass.PRIEST, AbilityEffectType.BUFF, "Bless the hero with stronger damage.", 3, 16, 28, 0, 0, 7);
        add(abilities, "holy_shield", "Holy Shield", AbilityClass.PRIEST, AbilityEffectType.SHIELD, "Gain a holy shield.", 4, 18, 34, 0, 0, 6);
        add(abilities, "purify", "Purify", AbilityClass.PRIEST, AbilityEffectType.AREA_DAMAGE, "Burn nearby enemies with cleansing light.", 5, 14, 30, 20, 180, 0);
        add(abilities, "divine_light", "Divine Light", AbilityClass.PRIEST, AbilityEffectType.HEAL, "Heal and damage nearby enemies.", 6, 22, 44, 28, 210, 0);
        add(abilities, "prayer", "Prayer", AbilityClass.PRIEST, AbilityEffectType.BUFF, "Regenerate mana faster for a short time.", 7, 26, 32, 0, 0, 8);
        add(abilities, "sanctuary", "Sanctuary", AbilityClass.PRIEST, AbilityEffectType.SLOW, "Create a slowing sanctuary field.", 8, 28, 52, 18, 260, 6);
        add(abilities, "resurrection", "Resurrection", AbilityClass.PRIEST, AbilityEffectType.HEAL, "Emergency burst heal.", 9, 35, 65, 60, 0, 0);
        add(abilities, "divine_judgment", "Divine Judgment", AbilityClass.PRIEST, AbilityEffectType.ULTIMATE, "Massive holy judgment.", 10, 50, 90, 60, 340, 3);
    }

    private static void addRanger(List<AbilityDefinition> abilities) {
        add(abilities, "quick_shot", "Quick Shot", AbilityClass.RANGER, AbilityEffectType.SINGLE_TARGET, "Fast shot at the nearest enemy.", 1, 4, 10, 12, 340, 0);
        add(abilities, "power_arrow", "Power Arrow", AbilityClass.RANGER, AbilityEffectType.SINGLE_TARGET, "A heavy piercing shot.", 2, 8, 18, 24, 360, 0);
        add(abilities, "multi_shot", "Multi Shot", AbilityClass.RANGER, AbilityEffectType.AREA_DAMAGE, "A fan of arrows around the hero.", 3, 10, 24, 16, 190, 0);
        add(abilities, "poison_arrow", "Poison Arrow", AbilityClass.RANGER, AbilityEffectType.POISON, "Poison enemies near the target.", 4, 12, 28, 5, 210, 5);
        add(abilities, "backstep", "Backstep", AbilityClass.RANGER, AbilityEffectType.DASH, "Leap away and clip nearby enemies.", 5, 9, 22, 10, 130, 0.4);
        add(abilities, "explosive_arrow", "Explosive Arrow", AbilityClass.RANGER, AbilityEffectType.AREA_DAMAGE, "Explode around the nearest enemy.", 6, 16, 38, 30, 230, 0);
        add(abilities, "rain_of_arrows", "Rain of Arrows", AbilityClass.RANGER, AbilityEffectType.SLOW, "Arrow rain slows and damages enemies.", 7, 24, 48, 26, 280, 5);
        add(abilities, "hunters_mark", "Hunter's Mark", AbilityClass.RANGER, AbilityEffectType.MARK, "Mark prey for extra damage.", 8, 18, 34, 8, 320, 7);
        add(abilities, "phantom_arrow", "Phantom Arrow", AbilityClass.RANGER, AbilityEffectType.SINGLE_TARGET, "A spectral arrow strikes hard.", 9, 22, 46, 42, 420, 0);
        add(abilities, "arrow_storm", "Arrow Storm", AbilityClass.RANGER, AbilityEffectType.ULTIMATE, "A storm of arrows fills the area.", 10, 46, 82, 54, 360, 4);
    }

    private static void addWarlock(List<AbilityDefinition> abilities) {
        add(abilities, "dark_bolt", "Dark Bolt", AbilityClass.WARLOCK, AbilityEffectType.SINGLE_TARGET, "Dark magic strikes the nearest enemy.", 1, 5, 13, 15, 290, 0);
        add(abilities, "curse", "Curse", AbilityClass.WARLOCK, AbilityEffectType.MARK, "Curse enemies to take more damage.", 2, 11, 22, 6, 210, 6);
        add(abilities, "life_drain", "Life Drain", AbilityClass.WARLOCK, AbilityEffectType.HEAL, "Drain life from nearby enemies.", 3, 14, 26, 18, 180, 0);
        add(abilities, "shadow_orb", "Shadow Orb", AbilityClass.WARLOCK, AbilityEffectType.AREA_DAMAGE, "A dark orb erupts.", 4, 13, 30, 25, 200, 0);
        add(abilities, "fear", "Fear", AbilityClass.WARLOCK, AbilityEffectType.STUN, "Terrify enemies briefly.", 5, 18, 36, 8, 240, 1.8);
        add(abilities, "soul_burn", "Soul Burn", AbilityClass.WARLOCK, AbilityEffectType.POISON, "Burn souls over time.", 6, 19, 42, 9, 230, 6);
        add(abilities, "demon_summon", "Demon Summon", AbilityClass.WARLOCK, AbilityEffectType.SUMMON, "Summoned shadows rupture the field.", 7, 26, 55, 32, 260, 6);
        add(abilities, "dark_pact", "Dark Pact", AbilityClass.WARLOCK, AbilityEffectType.BUFF, "Gain damage at a health cost.", 8, 23, 20, 0, 0, 8);
        add(abilities, "soul_prison", "Soul Prison", AbilityClass.WARLOCK, AbilityEffectType.SLOW, "Trap enemies in shadow chains.", 9, 30, 64, 34, 300, 6);
        add(abilities, "apocalypse", "Apocalypse", AbilityClass.WARLOCK, AbilityEffectType.ULTIMATE, "Catastrophic dark magic.", 10, 55, 95, 66, 360, 5);
    }

    private static void addElementalist(List<AbilityDefinition> abilities) {
        add(abilities, "fire_bolt", "Fire Bolt", AbilityClass.ELEMENTALIST, AbilityEffectType.SINGLE_TARGET, "Fire damage to the nearest enemy.", 1, 5, 12, 15, 300, 0);
        add(abilities, "ice_shard", "Ice Shard", AbilityClass.ELEMENTALIST, AbilityEffectType.SLOW, "Shard of ice slows nearby enemies.", 2, 8, 18, 12, 170, 3);
        add(abilities, "lightning_strike", "Lightning Strike", AbilityClass.ELEMENTALIST, AbilityEffectType.STUN, "Lightning briefly stuns enemies.", 3, 12, 28, 22, 190, 1.2);
        add(abilities, "flame_burst", "Flame Burst", AbilityClass.ELEMENTALIST, AbilityEffectType.AREA_DAMAGE, "Burst of flame around the hero.", 4, 13, 32, 26, 210, 0);
        add(abilities, "frost_nova", "Frost Nova", AbilityClass.ELEMENTALIST, AbilityEffectType.SLOW, "Freeze the ground in a circle.", 5, 16, 38, 16, 260, 5);
        add(abilities, "thunder_chain", "Thunder Chain", AbilityClass.ELEMENTALIST, AbilityEffectType.AREA_DAMAGE, "Chained thunder hits the pack.", 6, 18, 44, 34, 240, 0);
        add(abilities, "meteor", "Meteor", AbilityClass.ELEMENTALIST, AbilityEffectType.AREA_DAMAGE, "A meteor hits the nearest enemy.", 7, 25, 58, 48, 270, 0);
        add(abilities, "blizzard", "Blizzard", AbilityClass.ELEMENTALIST, AbilityEffectType.SLOW, "A blizzard damages and slows.", 8, 30, 64, 36, 330, 7);
        add(abilities, "elemental_storm", "Elemental Storm", AbilityClass.ELEMENTALIST, AbilityEffectType.ULTIMATE, "Fire, frost, and lightning converge.", 9, 42, 78, 56, 350, 4);
        add(abilities, "cataclysm", "Cataclysm", AbilityClass.ELEMENTALIST, AbilityEffectType.ULTIMATE, "An overwhelming elemental collapse.", 10, 58, 100, 72, 390, 5);
    }

    private static void add(List<AbilityDefinition> abilities, String id,
            String name, AbilityClass abilityClass, AbilityEffectType effectType,
            String description, int unlockLevel, double cooldownSeconds,
            double manaCost, double damage, double radius, double duration) {
        abilities.add(new AbilityDefinition(id, name, abilityClass, effectType,
                description, unlockLevel, cooldownSeconds, manaCost, damage,
                radius, duration));
    }
}
