import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Deterministic checks of Guardian combat without wave spawning or automatic attacks. */
public class GuardianCombatSmokeTest {
    private static final double EPSILON = 0.000001;
    private static final BufferedImage TEST_SPRITE = new BufferedImage(8, 8,
            BufferedImage.TYPE_INT_ARGB);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        List<String> failures = new ArrayList<>();
        check("stats and weapon", GuardianCombatSmokeTest::verifyStats, failures);
        check("physical shield bash", GuardianCombatSmokeTest::verifyBash, failures);
        check("Earthbreaker impact", GuardianCombatSmokeTest::verifyEarthbreaker, failures);
        check("continuous charge and swept contacts", GuardianCombatSmokeTest::verifyCharge, failures);
        check("Fortress and incoming damage routes", GuardianCombatSmokeTest::verifyFortress, failures);
        check("Unbreakable health threshold", GuardianCombatSmokeTest::verifyPassive, failures);
        check("Roar propagation and taunt expiry", GuardianCombatSmokeTest::verifyRoar, failures);
        check("resource denial and restart", GuardianCombatSmokeTest::verifyDenialAndRestart, failures);
        check("large delta partition", GuardianCombatSmokeTest::verifyLargeDelta, failures);
        if (!failures.isEmpty()) {
            throw new IllegalStateException(String.join("\n", failures));
        }
        System.out.println("Guardian combat smoke passed: impacts, charge, defense, taunt, resources and restart");
    }

    private static void verifyStats() throws Exception {
        Fixture fixture = new Fixture();
        require(fixture.player instanceof Character_Sir_Rakki, "Rakki must use his own player class");
        near(fixture.player.getMaxHealth(), 220.0, "Guardian maximum health");
        require("greatshield".equals(fixture.player.getDefaultWeaponStyle()), "Guardian weapon style");
        require(fixture.player.getPrimaryWeaponSprite() == ResourceLoader.loadImage(
                "/main/resources/weapons/aegis_greatshield.png"), "Actual Aegis PNG must be held");
        require(fixture.player.getPrimaryWeaponSprite().getColorModel().hasAlpha(), "Aegis PNG alpha");
        Player haze = new Character_Haze();
        fixture.player.setKeyPressed("right", true);
        haze.setKeyPressed("right", true);
        fixture.player.update(0.5);
        haze.update(0.5);
        require(fixture.player.getWorldX() > 0.0
                && fixture.player.getWorldX() < haze.getWorldX(), "Guardian must move slower than Haze");
    }

    private static void verifyEarthbreaker() throws Exception {
        Fixture fixture = new Fixture();
        TestEnemy front = fixture.enemy(100.0, 0.0);
        TestEnemy behind = fixture.enemy(-150.0, 0.0);
        RpgAbility skill = fixture.skill("earthbreaker");
        fixture.cast(skill);
        double hitTime = hitTime(skill);
        fixture.tick(hitTime - EPSILON);
        near(front.getHealth(), 1000.0, "Earthbreaker cannot hit during windup");
        require(!front.isKnockedDown(), "Earthbreaker cannot knock down before impact");
        fixture.tick(EPSILON * 2.0);
        near(front.getHealth(), 1000.0, "Earthbreaker wave must travel from shield impact to front enemy");
        fixture.tick(0.5);
        near(front.getHealth(), 974.0, "Earthbreaker single wave contact damage");
        require(front.isKnockedDown() && front.isStunned(), "Earthbreaker knocks down when its wave arrives");
        near(behind.getHealth(), 1000.0, "Earthbreaker fan excludes enemies far behind the shield");
        double afterImpact = front.getHealth();
        for (int frame = 0; frame < 180; frame++) fixture.tick(1.0 / 120.0);
        near(front.getHealth(), afterImpact, "Earthbreaker cannot apply duplicate damage");
        require(fixture.list("scheduledAbilityImpacts").isEmpty(), "Earthbreaker scheduled hit cleaned up");
        require(!fixture.player.isSkillAnimationActive(), "Earthbreaker must recover to idle");
    }

    private static void verifyBash() throws Exception {
        Fixture fixture = new Fixture();
        TestEnemy front = fixture.enemy(0.0, -90.0);
        TestEnemy side = fixture.enemy(70.0, -40.0);
        fixture.player.faceToward(0.0, -100.0);
        fixture.player.playAttackAnimation(null, 0.65);
        invoke(fixture.logic, "scheduleMeleeAttack",
                new Class<?>[] {double.class, double.class, double.class}, 90.0, 12.0, 0.65);
        double hit = 0.65 * 0.52;
        fixture.player.update(hit - EPSILON);
        invoke(fixture.logic, "updatePendingMeleeAttack", new Class<?>[] {double.class}, hit - EPSILON);
        near(front.getHealth(), 1000.0, "Shield bash cannot damage during windup");
        fixture.player.update(EPSILON * 2);
        invoke(fixture.logic, "updatePendingMeleeAttack", new Class<?>[] {double.class}, EPSILON * 2);
        near(front.getHealth(), 988.0, "Shield bash hits at its physical impact frame");
        near(side.getHealth(), 1000.0, "Up-facing shield bash must preserve its facing cone");
        require(front.isStunned(), "Shield bash staggers the struck enemy");
        invoke(fixture.logic, "updatePendingMeleeAttack", new Class<?>[] {double.class}, 1.0);
        near(front.getHealth(), 988.0, "Shield bash damage cannot repeat during recovery");
    }

    private static void verifyCharge() throws Exception {
        Fixture fixture = new Fixture();
        RpgAbility skill = fixture.skill("iron_charge");
        double startX = fixture.player.getWorldX();
        double startY = fixture.player.getWorldY();
        fixture.cast(skill);
        double shieldY = fixture.player.getWeaponCastWorldY();
        TestEnemy first = fixture.enemy(startX + 75.0, shieldY);
        TestEnemy second = fixture.enemy(startX + 145.0, shieldY);
        TestEnemy missed = fixture.enemy(startX + 105.0, shieldY + 100.0);
        near(fixture.player.getWorldX(), startX, "Iron Charge must not teleport on cast");
        fixture.tick(hitTime(skill) - EPSILON);
        near(fixture.player.getWorldX(), startX, "Iron Charge must stay still during preparation");
        near(first.getHealth(), 1000.0, "Iron Charge cannot damage ahead of preparation");
        fixture.tick(EPSILON * 2.0);
        double previousX = fixture.player.getWorldX();
        for (int frame = 0; frame < 60; frame++) {
            fixture.tick(0.01);
            double currentX = fixture.player.getWorldX();
            require(currentX >= previousX - EPSILON && currentX - previousX < 6.0,
                    "Iron Charge must move continuously forward");
            previousX = currentX;
        }
        near(fixture.player.getWorldX() - startX, 160.0, "Iron Charge total displacement");
        near(fixture.player.getWorldY(), startY, "Iron Charge captured direction");
        near(first.getHealth(), 982.0, "First swept shield contact hits once");
        near(second.getHealth(), 982.0, "Second swept shield contact hits once");
        near(missed.getHealth(), 1000.0, "Charge damage must follow shield contact");
        require(number(first, "knockbackVelocityX") > 0.0, "Charge knockback follows direction");
        near(number(first, "knockbackVelocityY"), 0.0, "Charge must not use radial knockback");
        double beforeKnockback = first.getWorldX();
        first.update(0.05, 0.0, 0.0, 32.0);
        require(first.getWorldX() > beforeKnockback, "Charge knockback must physically move an enemy");
        fixture.tick(0.5);
        near(first.getHealth(), 982.0, "Charge cannot damage again during recovery");
        require(value(fixture.logic, "guardianCharge") == null, "Charge action must clean up");
        require(!fixture.player.isSkillAnimationActive(), "Charge recovery must finish");
    }

    private static void verifyFortress() throws Exception {
        Fixture fixture = new Fixture();
        RpgAbility skill = fixture.skill("shield_fortress");
        fixture.cast(skill);
        near(hitTime(skill), 0.399, "Fortress activation frame");
        fixture.tick(hitTime(skill) - EPSILON);
        require(!fixture.player.isGuardianFortressActive(), "Fortress cannot activate before shield locks");
        fixture.tick(EPSILON * 2.0);
        require(fixture.player.isGuardianFortressActive(), "Fortress activates at lock frame");
        near(number(fixture.logic, "guardianFortressRemaining"), 6.0 - EPSILON,
                "Fortress six-second duration begins at impact");
        require(fixture.player.isGuardianKnockbackImmune(), "Fortress knockback immunity");
        fixture.player.applySlow(2.0, 0.2);
        fixture.player.applyMovementLock(2.0);
        fixture.player.applyAimLock(2.0);
        near(number(fixture.player, "slowTime"), 0.0, "Fortress prevents slow");
        near(number(fixture.player, "movementLockTime"), 0.0, "Fortress prevents movement debuff");
        near(number(fixture.player, "aimLockTime"), 0.0, "Fortress prevents aim debuff");
        verifyDamageRoutes(fixture, 0.8 * 0.2);
        require(number(fixture.player, "guardianBlockTime") > 0.0, "Incoming hit produces shield block reaction");
        double remaining = number(fixture.logic, "guardianFortressRemaining");
        fixture.tick(remaining - EPSILON);
        require(fixture.player.isGuardianFortressActive(), "Fortress remains until its duration ends");
        fixture.tick(EPSILON * 2.0);
        require(!fixture.player.isGuardianFortressActive()
                && !fixture.player.isGuardianKnockbackImmune(), "Fortress immunity must expire");
        require(fixture.list("abilityVisualEffects").isEmpty(), "Fortress barrier must expire with its defense");
    }

    private static void verifyPassive() throws Exception {
        Fixture fixture = new Fixture();
        verifyDamageRoutes(fixture, 0.8);
        setNumber(fixture.player, "health", 88.0);
        double before = fixture.player.getHealth();
        fixture.incoming(20.0, null);
        near(before - fixture.player.getHealth(), 16.0, "Exactly 40% health uses ordinary Unbreakable");
        setNumber(fixture.player, "health", 87.0);
        before = fixture.player.getHealth();
        fixture.incoming(20.0, null);
        near(before - fixture.player.getHealth(), 11.2, "Below 40% health grants extra 30% resistance");
        near(fixture.player.getGuardianResistanceMultiplier(), 0.35, "Wounded Guardian status resistance");
        TestEnemy tauntedSource = fixture.enemy(500.0, 0.0);
        tauntedSource.applyTaunt(5.0, 0.65);
        setNumber(fixture.player, "health", 200.0);
        before = fixture.player.getHealth();
        fixture.incoming(20.0, tauntedSource);
        near(before - fixture.player.getHealth(), 10.4, "Taunt attack reduction combines with Unbreakable");
    }

    private static void verifyDamageRoutes(Fixture fixture, double multiplier) throws Exception {
        TestEnemy contact = fixture.enemy(fixture.player.getWorldX(), fixture.player.getWorldY());
        double before = fixture.player.getHealth();
        fixture.logic.update(0.1);
        near(before - fixture.player.getHealth(), 10.0 * multiplier,
                "Enemy contact uses centralized incoming defense");
        fixture.enemies.remove(contact);
        Projectile projectile = new Projectile(fixture.player.getWorldX(), fixture.player.getWorldY(),
                fixture.player.getWorldX() + 1.0, fixture.player.getWorldY(),
                0.0, 20.0, 2.0, TEST_SPRITE, 1.0);
        projectile.setOwner(contact);
        fixture.<Projectile>list("enemyProjectiles").add(projectile);
        before = fixture.player.getHealth();
        invoke(fixture.logic, "updateEnemyProjectiles", new Class<?>[] {double.class}, 0.0);
        near(before - fixture.player.getHealth(), 20.0 * multiplier,
                "Enemy projectile uses centralized incoming defense");
        require(fixture.list("enemyProjectiles").isEmpty(), "Colliding projectile must be consumed");
        TemplateEnemyMinion minion = new TemplateEnemyMinion(fixture.player.getWorldX(), fixture.player.getWorldY());
        before = fixture.player.getHealth();
        invoke(fixture.logic, "triggerMinionExplosion", new Class<?>[] {TemplateEnemyMinion.class}, minion);
        near(before - fixture.player.getHealth(), minion.getExplosionDamage() * multiplier,
                "Minion explosion uses centralized incoming defense");
        require(minion.isDead(), "Exploding minion must be consumed");
    }

    private static void verifyRoar() throws Exception {
        Fixture fixture = new Fixture();
        TestEnemy nearEnemy = fixture.enemy(50.0, 0.0);
        TestEnemy farEnemy = fixture.enemy(200.0, 0.0);
        TestEnemy outside = fixture.enemy(260.0, 0.0);
        EnemyDefinition rangedDefinition = EnemyCatalog.normalEnemies(EnemyRegion.RIVENDALE_TOWN)
                .stream().filter(EnemyDefinition::isRanged).findFirst().orElseThrow();
        RegionalEnemy ranged = new RegionalEnemy(rangedDefinition, 180.0, 0.0);
        fixture.enemies.add(ranged);
        ranged.update(0.1, 0.0, 0.0, 32.0);
        near(ranged.getWorldX(), 180.0, "Ranged enemy normally stops at attack range");
        RpgAbility skill = fixture.skill("guardians_roar");
        fixture.cast(skill);
        fixture.tick(hitTime(skill) - EPSILON);
        require(!nearEnemy.isTaunted() && !farEnemy.isTaunted(), "Roar cannot taunt during windup");
        fixture.tick(EPSILON * 2.0);
        require(!nearEnemy.isTaunted() && !farEnemy.isTaunted(), "Roar wave must travel to each enemy");
        fixture.tick(0.12);
        require(nearEnemy.isTaunted() && !farEnemy.isTaunted(), "Roar reaches nearby enemies first");
        fixture.tick(0.4);
        require(farEnemy.isTaunted() && ranged.isTaunted(), "Roar wave reaches enemies in range");
        require(!outside.isTaunted(), "Roar must exclude enemies beyond its radius");
        near(farEnemy.getAttackDamageMultiplier(), 0.65, "Roar attack reduction");
        near(nearEnemy.getHealth(), 1000.0, "Roar is crowd control rather than damage");
        require(farEnemy.isStunned(), "Roar impact must stagger enemies");
        ranged.update(0.6, 0.0, 0.0, 32.0);
        require(ranged.getWorldX() < 180.0, "Taunted ranged enemies must approach Guardian");
        fixture.tick(0.04);
        require(value(fixture.logic, "guardianRoar") == null, "Roar propagation action must finish");
        nearEnemy.update(5.1, 0.0, 0.0, 32.0);
        farEnemy.update(5.1, 0.0, 0.0, 32.0);
        require(!nearEnemy.isTaunted() && !farEnemy.isTaunted(), "Roar taunt must expire");
        near(farEnemy.getAttackDamageMultiplier(), 1.0, "Attack power restored after taunt");
    }

    private static void verifyDenialAndRestart() throws Exception {
        Fixture fixture = new Fixture();
        RpgAbility charge = fixture.skill("iron_charge");
        RpgAbility earthbreaker = fixture.skill("earthbreaker");
        fixture.cast(charge);
        double mana = fixture.manager.getMana();
        require(!earthbreaker.trigger(fixture.logic, fixture.manager, 1), "Guardian action cannot be replaced");
        near(fixture.manager.getMana(), mana, "Denied action must not spend mana");
        near(earthbreaker.getCooldownRemaining(), 0.0, "Denied action must not start cooldown");
        fixture.tick(1.0);
        mana = fixture.manager.getMana();
        require(!charge.trigger(fixture.logic, fixture.manager, 1), "Charge cooldown must deny repeat cast");
        near(fixture.manager.getMana(), mana, "Cooldown denial must not spend mana");
        fixture.manager.spendMana(fixture.manager.getMana());
        require(!earthbreaker.trigger(fixture.logic, fixture.manager, 1), "Insufficient mana must deny cast");
        near(fixture.manager.getMana(), 0.0, "Mana denial must not spend resources");
        RpgAbility passive = fixture.skill("unbreakable");
        require(!passive.canTrigger(fixture.manager, 1)
                && !passive.trigger(fixture.logic, fixture.manager, 1), "Unbreakable cannot trigger");
        fixture.logic.startGame();
        Player restarted = (Player) value(fixture.logic, "player");
        near(restarted.getHealth(), 220.0, "Restart resets health");
        near(fixture.manager.getMana(), 100.0, "Restart resets mana");
        near(charge.getCooldownRemaining(), 0.0, "Restart resets cooldowns");
        require(!restarted.isSkillAnimationActive(), "Restart must clear animation lock");
        require(value(fixture.logic, "guardianCharge") == null
                && value(fixture.logic, "guardianRoar") == null
                && number(fixture.logic, "guardianFortressRemaining") == 0.0,
                "Restart must clear Guardian actions");
        require(fixture.list("scheduledAbilityImpacts").isEmpty()
                && fixture.list("abilityVisualEffects").isEmpty(), "Restart must clear scheduled effects");
        fixture.logic.update(RunEntranceAnimation.DURATION);
        require(fixture.skill("shield_fortress").trigger(fixture.logic, fixture.manager, 1),
                "Restarted Guardian can cast again");
        restarted.update(0.5);
        invoke(fixture.logic, "updateAbilityVisualEffects", new Class<?>[] {double.class}, 0.5);
        require(restarted.isGuardianFortressActive(), "Restarted Fortress can activate");
        fixture.logic.selectCharacter(1);
        fixture.logic.startGame();
        Player haze = (Player) value(fixture.logic, "player");
        require(haze instanceof Character_Haze && !haze.isGuardianFortressActive(),
                "Switching hero must clear Guardian state");
        near(number(fixture.logic, "passiveDamageTakenMultiplier"), 1.0,
                "Switching hero must remove Unbreakable");
        for (RpgAbility equipped : fixture.manager.getEquippedAbilities()) {
            require(equipped.getDefinition().getAbilityClass() == AbilityClass.ASSASSIN,
                    "Returning to Haze restores his active loadout");
        }
    }

    private static void verifyLargeDelta() throws Exception {
        Fixture coarse = new Fixture();
        Fixture fine = new Fixture();
        for (Fixture fixture : List.of(coarse, fine)) {
            fixture.cast(fixture.skill("iron_charge"));
            fixture.enemy(95.0, fixture.player.getWeaponCastWorldY());
        }
        coarse.tick(0.9);
        for (int frame = 0; frame < 90; frame++) fine.tick(0.01);
        near(coarse.player.getWorldX(), 160.0, "Coarse delta charge displacement");
        near(coarse.player.getWorldX(), fine.player.getWorldX(), "Charge delta partition independence");
        near(coarse.enemies.get(0).getHealth(), 982.0, "Coarse delta swept contact");
        near(coarse.enemies.get(0).getHealth(), fine.enemies.get(0).getHealth(),
                "Charge hit count must be independent of delta partition");
        Fixture coarseShield = new Fixture();
        Fixture fineShield = new Fixture();
        coarseShield.cast(coarseShield.skill("shield_fortress"));
        fineShield.cast(fineShield.skill("shield_fortress"));
        coarseShield.tick(0.8);
        for (int frame = 0; frame < 80; frame++) fineShield.tick(0.01);
        near(number(coarseShield.logic, "guardianFortressRemaining"),
                number(fineShield.logic, "guardianFortressRemaining"),
                "Fortress duration must account for late impact in a coarse delta");
    }

    private static double hitTime(RpgAbility skill) {
        return AbilityAnimationTiming.hitTimes(skill.getDefinition())[0];
    }

    private static final class Fixture {
        private final GameLogic logic = new GameLogic();
        private final AbilityManager manager;
        private final Player player;
        private final List<Enemy> enemies;

        private Fixture() throws Exception {
            field(GameLogic.class, "soundEnabled").setBoolean(logic, false);
            logic.selectCharacter(4);
            logic.startGame();
            logic.update(RunEntranceAnimation.DURATION);
            manager = logic.getAbilityManager();
            player = (Player) value(logic, "player");
            enemies = list("enemies");
            setNumber(logic, "whenToSpawn", Double.POSITIVE_INFINITY);
        }

        private TestEnemy enemy(double x, double y) {
            TestEnemy enemy = new TestEnemy(x, y);
            enemies.add(enemy);
            return enemy;
        }

        private RpgAbility skill(String id) {
            RpgAbility skill = manager.getAbilityById(id);
            require(skill != null, "Missing Guardian skill " + id);
            return skill;
        }

        private void cast(RpgAbility skill) {
            require(skill.trigger(logic, manager, 1), "Guardian cast denied: " + skill.getDefinition().getId());
        }

        private void tick(double delta) throws Exception {
            player.update(delta);
            manager.update(delta);
            invoke(logic, "updatePlayerBuffs", new Class<?>[] {double.class}, delta);
            invoke(logic, "updateAbilityVisualEffects", new Class<?>[] {double.class}, delta);
        }

        private void incoming(double damage, Enemy source) throws Exception {
            invoke(logic, "applyIncomingDamage", new Class<?>[] {
                    double.class, Enemy.class, double.class, double.class},
                    damage, source, player.getWeaponCastWorldX(), player.getWeaponCastWorldY());
        }

        @SuppressWarnings("unchecked")
        private <T> List<T> list(String name) throws Exception {
            return (List<T>) value(logic, name);
        }
    }

    private static final class TestEnemy extends Enemy {
        private TestEnemy(double x, double y) {
            super(x, y, TEST_SPRITE, TEST_SPRITE, 0.0, 0.0, 8, 8, 8, 8.0, 100.0, 1000.0);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Class<?> current = type;
        while (current != null) {
            try {
                Field found = current.getDeclaredField(name);
                found.setAccessible(true);
                return found;
            } catch (NoSuchFieldException missing) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object value(Object target, String name) throws Exception {
        return field(target.getClass(), name).get(target);
    }

    private static double number(Object target, String name) throws Exception {
        return field(target.getClass(), name).getDouble(target);
    }

    private static void setNumber(Object target, String name, double value) throws Exception {
        field(target.getClass(), name).setDouble(target, value);
    }

    private static void invoke(Object target, String name, Class<?>[] parameterTypes,
            Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }

    private static void near(double actual, double expected, String context) {
        require(Math.abs(actual - expected) <= EPSILON,
                context + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void check(String name, CheckedCheck test, List<String> failures) {
        try {
            test.run();
        } catch (Exception | AssertionError failure) {
            failures.add(name + ": " + failure);
        }
    }

    @FunctionalInterface
    private interface CheckedCheck {
        void run() throws Exception;
    }
}
