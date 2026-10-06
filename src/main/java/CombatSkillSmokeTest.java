import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Isolated checks of contact timing, resources and recovery for the five combat loadouts. */
public class CombatSkillSmokeTest {
    static final String[][] SKILLS = {
        {"heavy_slash", "shield_bash", "earth_shatter", "knights_wrath"},
        {"shadow_strike", "shadow_step", "death_mark", "twin_fang"},
        {"heal", "holy_bolt", "holy_shield", "divine_light"},
        {"flame_burst", "ice_shard", "lightning_strike", "elemental_storm"},
        {"shield_fortress", "iron_charge", "earthbreaker", "guardians_roar"}
    };
    private static final String[] PASSIVES = {"iron_guard", "shadow_assassin", "blessing", "cataclysm", "unbreakable"};
    private static final BufferedImage SPRITE = sprite();
    private static final double EPS = 0.000001;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0 && args[0].equals("--contacts")) {
            sweptProjectile();
            enemyMotion();
            projectileSprite();
            System.out.println("Swept contacts, projectile PNG geometry, continuous knockback and poison clocks passed");
            return;
        }
        List<String> failures = new ArrayList<>();
        for (int hero = 0; hero < SKILLS.length; hero++) {
            for (String id : SKILLS[hero]) {
                for (int facing : new int[] {1, -1}) {
                    for (int motion = 0; motion < 3; motion++) {
                        int selected = hero, movement = motion;
                        check(id + " " + facing + " motion " + motion,
                                () -> lifecycle(selected, id, facing, movement), failures);
                    }
                }
                int selected = hero;
                check(id + " denial/repeat", () -> denialAndRepeat(selected, id), failures);
                check(id + " kill/multiple", () -> killAndMultiple(selected, id), failures);
            }
            int selected = hero;
            check(PASSIVES[hero] + " passive", () -> passive(selected), failures);
        }
        check("far melee exclusion", CombatSkillSmokeTest::farMelee, failures);
        check("spell travel", CombatSkillSmokeTest::projectileTravel, failures);
        check("continuous agile movement", CombatSkillSmokeTest::continuousDash, failures);
        check("wave contact order", CombatSkillSmokeTest::waveContactOrder, failures);
        check("moving marks and barriers", CombatSkillSmokeTest::attachedEffects, failures);
        check("release clock partitions", CombatSkillSmokeTest::releasePartitions, failures);
        check("restart clears runtime", CombatSkillSmokeTest::restart, failures);
        check("swept projectile contacts", CombatSkillSmokeTest::sweptProjectile, failures);
        check("projectile PNG geometry", CombatSkillSmokeTest::projectileSprite, failures);
        check("continuous knockback and bounded poison", CombatSkillSmokeTest::enemyMotion, failures);
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("Combat skill smoke passed: 120 directional locomotion casts; all 20 skills, "
                + "five passives, resources, kills, recovery, spell travel, wave arrival and swept contacts");
    }

    private static void lifecycle(int hero, String id, int facing, int movement) throws Exception {
        Fixture f = new Fixture(hero, facing);
        if (movement > 0) {
            if (movement == 2) f.player.increaseSpeed(80);
            f.player.setKeyPressed(facing > 0 ? "right" : "left", true);
            f.tick(0.24);
        }
        double startX = f.player.getWorldX(), startY = f.player.getWorldY();
        f.enemy(startX + facing * 75, startY);
        f.enemy(startX + facing * 135, startY + 12);
        f.player.takeDamage(25);
        double health = f.player.getHealth();
        RpgAbility ability = f.skill(id);
        AbilityDefinition definition = ability.getDefinition();
        double mana = f.manager.getMana();
        f.cast(id);
        near(f.manager.getMana(), mana - definition.getManaCost(), id + " spends mana once");
        near(f.player.getWorldX(), startX, id + " cannot teleport on press");
        near(f.player.getWorldY(), startY, id + " cannot teleport vertically on press");
        require(f.player.isSkillAnimationActive(), id + " starts weapon/body animation");
        double duration = AbilityAnimationTiming.duration(definition);
        double release = AbilityAnimationTiming.hitTimes(definition)[0];
        f.advance(Math.max(0, release - EPS));
        for (TestEnemy enemy : f.enemies) {
            require(enemy.damageCount == 0, id + " cannot damage during anticipation");
            near(number(enemy, "hitReactionTimer"), 0, id + " cannot react before hit");
        }
        if ("heal".equals(id)) near(f.player.getHealth(), health, "Healing Touch waits for release");
        require(Double.isFinite(f.player.getWeaponCastWorldX())
                && Double.isFinite(f.player.getWeaponCastWorldY()), id + " finite physical cast point");
        BufferedImage before = render(f.logic);
        require(Arrays.equals(pixels(before), pixels(render(f.logic))), id + " draw must not advance animation");
        f.advance(duration + 1.7);
        require(!f.player.isSkillAnimationActive(), id + " returns to locomotion/idle");
        if (movement > 0) require(Math.abs(f.player.getWorldX() - startX) > 1,
                id + " held movement resumes naturally");
        int damageCount = f.enemies.stream().mapToInt(enemy -> enemy.damageCount).sum();
        f.advance(0.75);
        require(damageCount == f.enemies.stream().mapToInt(enemy -> enemy.damageCount).sum(),
                id + " no damage repeats after recovery");
        f.advance(8.0);
        require(f.list("scheduledAbilityImpacts").isEmpty(), id + " consumes scheduled impacts");
        require(f.list("skillProjectiles").isEmpty(), id + " cleans up projectile actors");
        require(f.list("skillWaves").isEmpty(), id + " cleans up wave actors");
        require(f.list("abilityVisualEffects").isEmpty(), id + " cleans up finite VFX");
    }

    private static void denialAndRepeat(int hero, String id) throws Exception {
        Fixture f = new Fixture(hero, 1);
        RpgAbility ability = f.skill(id);
        f.cast(id);
        double mana = f.manager.getMana();
        require(!ability.trigger(f.logic, f.manager, 1), id + " rejects repeated press");
        near(f.manager.getMana(), mana, id + " repeat denial preserves mana");
        RpgAbility other = f.skill(SKILLS[hero][SKILLS[hero][0].equals(id) ? 1 : 0]);
        require(!other.trigger(f.logic, f.manager, 1), id + " ongoing action rejects replacement");
        near(other.getCooldownRemaining(), 0, id + " overlap denial preserves other cooldown");
        near(f.manager.getMana(), mana, id + " overlap denial preserves mana");
        f.advance(AbilityAnimationTiming.duration(ability.getDefinition()) + 2);
        f.manager.spendMana(f.manager.getMana());
        require(!other.trigger(f.logic, f.manager, 1), id + " rejects insufficient mana");
        near(f.manager.getMana(), 0, id + " empty mana unchanged");
        near(other.getCooldownRemaining(), 0, id + " empty mana does not start cooldown");
        f.advance(ability.getDefinition().getCooldownSeconds() + 1);
        require(ability.trigger(f.logic, f.manager, 1), id + " can cast after cooldown recovery");
    }

    private static void killAndMultiple(int hero, String id) throws Exception {
        Fixture f = new Fixture(hero, 1);
        double victimX = id.equals("shield_bash") ? 50 : id.equals("shadow_step") ? 160 : 75;
        TestEnemy victim = f.enemy(victimX, 0);
        setNumber(victim, "health", 1);
        f.enemy(105, 4);
        f.enemy(125, -7);
        f.cast(id);
        f.advance(3);
        AbilityDefinition definition = f.skill(id).getDefinition();
        boolean damaging = definition.getDamage() > 0 && definition.getEffectType() != AbilityEffectType.HEAL;
        if (damaging) require(victim.isDead(), id + " resolves a killing impact");
        require(victim.damageCount <= AbilityAnimationTiming.hitTimes(definition).length,
                id + " never damages a corpse again");
        int hits = f.enemies.stream().mapToInt(enemy -> enemy.damageCount).sum();
        f.advance(1);
        require(hits == f.enemies.stream().mapToInt(enemy -> enemy.damageCount).sum(),
                id + " kill does not leave delayed ghost hits");
    }

    private static void passive(int hero) throws Exception {
        Fixture f = new Fixture(hero, 1);
        RpgAbility passive = f.skill(PASSIVES[hero]);
        require(passive.getDefinition().isPassive(), PASSIVES[hero] + " is a trait");
        double mana = f.manager.getMana();
        require(!passive.canTrigger(f.manager, 99) && !passive.trigger(f.logic, f.manager, 99),
                PASSIVES[hero] + " cannot be cast");
        near(f.manager.getMana(), mana, "Passive never spends mana");
        require(!f.player.isSkillAnimationActive(), "Passive cannot lock character movement");
    }

    private static void farMelee() throws Exception {
        for (String id : List.of("heavy_slash", "shield_bash", "earth_shatter", "knights_wrath", "twin_fang")) {
            Fixture f = new Fixture(id.equals("twin_fang") ? 1 : 0, 1);
            TestEnemy enemy = f.enemy(380, 0);
            f.cast(id);
            f.advance(3);
            require(enemy.damageCount == 0, id + " held weapon cannot hit an enemy hundreds of pixels away");
        }
    }

    private static void projectileTravel() throws Exception {
        for (String id : List.of("flame_burst", "ice_shard")) {
            Fixture f = new Fixture(3, 1);
            TestEnemy enemy = f.enemy(300, 0);
            RpgAbility ability = f.skill(id);
            f.cast(id);
            double launch = AbilityAnimationTiming.hitTimes(ability.getDefinition())[0];
            f.advance(launch + 0.02);
            require(!f.list("skillProjectiles").isEmpty(), id + " launches a traveling projectile");
            require(enemy.damageCount == 0, id + " distant target waits for physical projectile arrival");
            f.advance(2);
            require(enemy.damageCount > 0, id + " projectile reaches and damages target");
            require(f.list("skillProjectiles").isEmpty(), id + " impact consumes projectile");
        }
    }

    private static void continuousDash() throws Exception {
        for (String id : List.of("shadow_strike", "shadow_step", "iron_charge")) {
            int hero = id.equals("iron_charge") ? 4 : 1;
            for (int facing : new int[] {1, -1}) {
                Fixture f = new Fixture(hero, facing);
                f.enemy(facing * 150, 0);
                f.cast(id);
                double previousX = f.player.getWorldX();
                double displacement = 0;
                for (int frame = 0; frame < 240; frame++) {
                    f.tick(1.0 / 120);
                    double x = f.player.getWorldX();
                    double step = (x - previousX) * facing;
                    require(step >= -EPS && step < 12,
                            id + " must accelerate continuously along its captured facing");
                    displacement += step;
                    previousX = x;
                }
                require(displacement > 35 && displacement < 300,
                        id + " must move its character through the action: " + displacement);
                require(!f.player.isSkillAnimationActive(), id + " dash settles into idle");
            }
        }
    }

    private static void waveContactOrder() throws Exception {
        for (String id : List.of("elemental_storm", "earthbreaker", "guardians_roar")) {
            Fixture f = new Fixture(id.equals("elemental_storm") ? 3 : 4, 1);
            TestEnemy nearby = f.enemy(50, 0);
            TestEnemy far = f.enemy(id.equals("earthbreaker") ? 160 : 210, 0);
            f.cast(id);
            double nearTime = -1, farTime = -1;
            for (int frame = 0; frame < 240; frame++) {
                f.tick(1.0 / 120);
                boolean roar = id.equals("guardians_roar");
                if (nearTime < 0 && (roar ? nearby.isTaunted() : nearby.damageCount > 0)) nearTime = frame;
                if (farTime < 0 && (roar ? far.isTaunted() : far.damageCount > 0)) farTime = frame;
            }
            require(nearTime >= 0 && farTime > nearTime + 1,
                    id + " expanding wave reaches nearby enemies before distant enemies: " + nearTime + "," + farTime);
            require(nearby.damageCount <= 1 && far.damageCount <= 1, id + " wave hits each enemy once");
        }
    }

    private static void restart() throws Exception {
        Fixture f = new Fixture(3, 1);
        f.enemy(300, 0);
        f.cast("flame_burst");
        f.advance(0.6);
        f.logic.startGame();
        for (String field : List.of("scheduledAbilityImpacts", "skillProjectiles", "skillWaves", "abilityVisualEffects")) {
            require(f.list(field).isEmpty(), "Restart clears " + field);
        }
        near(f.manager.getMana(), f.manager.getMaxMana(), "Restart restores mana");
        require(!((Player) value(f.logic, "player")).isSkillAnimationActive(), "Restart clears action lock");
    }

    private static void releasePartitions() throws Exception {
        for (String id : new String[] {"flame_burst", "ice_shard"}) {
            for (int side : new int[] {-1, 1}) {
                for (boolean moving : new boolean[] {false, true}) {
                    Fixture coarse = new Fixture(3, side), fine = new Fixture(3, side);
                    for (Fixture f : List.of(coarse, fine)) {
                        if (moving) {
                            f.player.setKeyPressed(side > 0 ? "right" : "left", true);
                            f.tick(0.24);
                        }
                        f.enemy(f.player.getWorldX() + side * 280, f.player.getWorldY());
                        f.cast(id);
                    }
                    double time = AbilityAnimationTiming.hitTimes(coarse.skill(id).getDefinition())[0] + 0.08;
                    coarse.tick(time);
                    fine.advance(time);
                    List<Object> a = coarse.list("skillProjectiles"), b = fine.list("skillProjectiles");
                    require(a.size() == 1 && b.size() == 1, id + " partition comparison needs one live shot");
                    for (String field : List.of("x", "y", "age", "dx", "dy")) {
                        near(number(a.get(0), field), number(b.get(0), field), id + " release partition " + field);
                    }
                    BufferedImage before = render(coarse.logic);
                    coarse.player.sampleSkillSourceAt(time * 0.5, time * 0.5);
                    require(Arrays.equals(pixels(before), pixels(render(coarse.logic))), "Sampling release pose cannot mutate live rendering");
                }
            }
        }
    }

    private static void attachedEffects() throws Exception {
        Fixture mark = new Fixture(1, 1);
        TestEnemy target = mark.enemy(100, 0);
        mark.cast("death_mark");
        mark.advance(0.5);
        List<AbilityVisualEffect> marks = mark.list("abilityVisualEffects");
        require(marks.size() == 1 && number(target, "markTimer") > 0, "Death Mark attaches at its hit frame");
        AbilityVisualEffect attached = marks.get(0);
        target.teleportTo(148, -34);
        mark.tick(0.01);
        near(number(attached, "worldX"), target.getWorldX(), "Death Mark follows its enemy horizontally");
        near(number(attached, "worldY"), target.getWorldY(), "Death Mark follows its enemy vertically");
        require(value(attached, "attachedTarget") == target, "Death Mark keeps its actual target reference");
        target.takeDamage(10000);
        mark.tick(0.01);
        require(mark.list("abilityVisualEffects").isEmpty(), "Death Mark expires when its attached enemy dies");

        for (int hero : new int[] {2, 4}) {
            Fixture barrier = new Fixture(hero, 1);
            String id = hero == 2 ? "holy_shield" : "shield_fortress";
            barrier.cast(id);
            barrier.advance(1.2);
            List<AbilityVisualEffect> barriers = barrier.list("abilityVisualEffects");
            require(barriers.size() == 1 && !barriers.get(0).isExpired(), id + " remains active after forming");
            AbilityVisualEffect held = barriers.get(0);
            barrier.player.moveWorld(64, -22);
            barrier.tick(0.01);
            near(number(held, "casterX"), barrier.player.getWorldX(), id + " follows its player horizontally");
            near(number(held, "casterY"), barrier.player.getWorldY(), id + " follows its player vertically");
            double health = barrier.player.getHealth();
            Method incoming = GameLogic.class.getDeclaredMethod("applyIncomingDamage", double.class,
                    Enemy.class, double.class, double.class);
            incoming.setAccessible(true);
            incoming.invoke(barrier.logic, 20.0, null, barrier.player.getWorldX() + 20, barrier.player.getWorldY());
            require(barrier.player.getHealth() < health, id + " receives incoming attack through its defense");
            near(number(held, "blockAge"), 0, id + " barrier flashes exactly on incoming contact");
        }
    }

    private static void sweptProjectile() {
        Projectile p = new Projectile(0, 0, 100, 0, 1000, 12, 2, SPRITE);
        TestEnemy near = new TestEnemy(30, 0), far = new TestEnemy(80, 0), miss = new TestEnemy(50, 30);
        p.update(0.1);
        require(p.hits(near) && p.hits(far) && !p.hits(miss), "Swept collision must not tunnel past narrow targets");
        require(p.getCollisionFraction(near) < p.getCollisionFraction(far), "Projectile contacts have travel order");
        near(p.getContactWorldX(p.getCollisionFraction(near)), 20, "Actual collision point is target surface");
        near(p.getContactWorldY(p.getCollisionFraction(near)), 0, "Actual collision vertical coordinate");
        require(p.hitsPlayer(45, 0, 8) && !p.hitsPlayer(45, 40, 8), "Enemy projectiles also use swept contact");
        near.takeDamage(10000);
        require(!p.hits(near), "Projectile must ignore corpses");
        double x = p.getWorldX();
        p.update(Double.NaN);
        near(p.getWorldX(), x, "Invalid projectile delta cannot corrupt coordinates");
        p.update(100);
        near(p.getWorldX(), 3000, "Projectile cannot move beyond its lifetime");
    }

    private static void enemyMotion() {
        TestEnemy coarse = new TestEnemy(0, 0), fine = new TestEnemy(0, 0);
        coarse.knockAwayFrom(-10, 0, 30);
        fine.knockAwayFrom(-10, 0, 30);
        near(coarse.getWorldX(), 0, "Knockback impulse cannot teleport an enemy at impact");
        coarse.update(0.2, -100, 0, 8);
        for (int i = 0; i < 20; i++) fine.update(0.01, -100, 0, 8);
        require(coarse.getWorldX() > 0 && coarse.getWorldX() < 30, "Enemy knockback moves and decelerates");
        near(coarse.getWorldX(), fine.getWorldX(), "Knockback is independent of time partition");
        coarse.applyPoison(10, 0.1);
        fine.applyPoison(10, 0.1);
        coarse.update(0.5, 0, 0, 8);
        for (int i = 0; i < 50; i++) fine.update(0.01, 0, 0, 8);
        near(coarse.getHealth(), 999, "Poison only damages during its remaining duration");
        near(coarse.getHealth(), fine.getHealth(), "Poison damage is independent of time partition");
    }

    private static void projectileSprite() {
        BufferedImage wide = new BufferedImage(96, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ink = wide.createGraphics();
        ink.setColor(java.awt.Color.RED); ink.fillRect(0, 0, 16, 16);
        ink.setColor(java.awt.Color.CYAN); ink.fillRect(80, 0, 16, 16);
        ink.dispose();
        Projectile projectile = new Projectile(0, 0, 100, 0, 200, 12, 3, wide, 0, 96, DamageElement.ICE);
        BufferedImage image = new BufferedImage(160, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        projectile.draw(g, 80, 48, 0, 0);
        g.dispose();
        require(image.getRGB(35, 48) == java.awt.Color.RED.getRGB()
                && image.getRGB(122, 48) == java.awt.Color.CYAN.getRGB(),
                "Wide single-frame PNGs must retain both ends rather than becoming guessed animation crops");
        require((image.getRGB(80, 36) >>> 24) == 0 && (image.getRGB(80, 60) >>> 24) == 0,
                "Projectile PNG aspect ratio must remain intact");
    }

    static final class Fixture {
        final GameLogic logic = new GameLogic();
        final Player player;
        final AbilityManager manager;
        final List<TestEnemy> enemies;

        Fixture(int hero, int facing) throws Exception {
            logic.setSoundEnabled(false);
            logic.selectCharacter(hero);
            logic.startGame();
            player = (Player) value(logic, "player");
            manager = logic.getAbilityManager();
            enemies = list("enemies");
            player.faceToward(facing * 100, 0);
            setNumber(logic, "whenToSpawn", Double.POSITIVE_INFINITY);
        }

        TestEnemy enemy(double x, double y) {
            TestEnemy enemy = new TestEnemy(x, y);
            enemies.add(enemy);
            return enemy;
        }

        TestEnemy visualEnemy(double x, double y) {
            TestEnemy enemy = new TestEnemy(x, y, true);
            enemies.add(enemy);
            return enemy;
        }

        RpgAbility skill(String id) {
            RpgAbility ability = manager.getAbilityById(id);
            require(ability != null, "Missing skill " + id);
            return ability;
        }

        void cast(String id) {
            require(skill(id).trigger(logic, manager, 1), "Cast denied: " + id);
        }

        void tick(double dt) throws Exception {
            player.update(dt);
            manager.update(dt);
            invoke(logic, "updatePlayerBuffs", dt);
            invoke(logic, "updateAbilityVisualEffects", dt);
            invoke(logic, "updateFloatingTexts", dt);
            for (TestEnemy enemy : enemies) {
                enemy.update(dt, player.getWorldX(), player.getWorldY(), player.getCollisionRadius());
            }
        }

        void advance(double duration) throws Exception {
            double remaining = duration;
            while (remaining > 0.0000000001) {
                double dt = Math.min(1.0 / 120.0, remaining);
                tick(dt);
                remaining -= dt;
            }
        }

        @SuppressWarnings("unchecked") <T> List<T> list(String name) throws Exception {
            return (List<T>) value(logic, name);
        }
    }

    static final class TestEnemy extends Enemy {
        int damageCount;
        TestEnemy(double x, double y) {
            this(x, y, false);
        }
        TestEnemy(double x, double y, boolean actualSprite) {
            super(x, y,
                    actualSprite ? ResourceLoader.loadImage("/main/resources/enemy/LVL1Walk.png") : SPRITE,
                    actualSprite ? ResourceLoader.loadImage("/main/resources/enemy/LVL1Death.png") : SPRITE,
                    0, 4, actualSprite ? 128 : 8, actualSprite ? 128 : 8,
                    actualSprite ? 64 : 40, 8, 10, 1000);
        }
        @Override public void takeDamage(double damage, DamageElement element, double x, double y) {
            if (!isDead() && damage > 0) damageCount++;
            super.takeDamage(damage, element, x, y);
        }
    }

    static BufferedImage render(GameLogic logic) {
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        logic.drawAbilityGroundEffects(g, 320, 200);
        logic.drawEntities(g, 320, 200);
        logic.drawAbilityBursts(g, 320, 200);
        g.dispose();
        return image;
    }

    private static BufferedImage sprite() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(197, 99, 82));
        g.fillRect(1, 1, 6, 6);
        g.dispose();
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }
    static Object value(Object target, String name) throws Exception { return field(target.getClass(), name).get(target); }
    static double number(Object target, String name) throws Exception { return ((Number) value(target, name)).doubleValue(); }
    static void setNumber(Object target, String name, double number) throws Exception { field(target.getClass(), name).setDouble(target, number); }
    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void invoke(Object target, String name, double dt) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, double.class);
        m.setAccessible(true);
        m.invoke(target, dt);
    }
    private static void near(double actual, double expected, String message) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) < EPS,
                message + ": expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void check(String context, CheckedCheck check, List<String> failures) {
        try { check.run(); } catch (Exception | AssertionError e) { failures.add(context + ": " + e); }
    }
    @FunctionalInterface private interface CheckedCheck { void run() throws Exception; }
}
