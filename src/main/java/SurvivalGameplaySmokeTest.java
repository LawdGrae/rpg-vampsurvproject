import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/** Regression coverage for rewards, encounter pacing, spawn safety and fresh runs. */
public final class SurvivalGameplaySmokeTest {
    public static void main(String[] args) throws Exception {
        verifyDirector();
        verifySpawnSafety();
        verifyExperienceCarry();
        verifyFreshRun();
        verifyDeathRewards();
        verifySummonSafety();
        verifyBossCadence();
        verifyProjectileOwnerRelease();
        verifyReflectedContactOrder();
        verifySuspendedSimulation();
        verifyLongCombat();
        System.out.println("Assault pacing, streak rewards, offscreen spawns, XP carry, poison loot, summon safety and restart checks passed");
    }

    private static void verifyDirector() {
        SurvivalDirector run = new SurvivalDirector();
        require(!run.update(25.0, false) && run.isWarning(), "A wave must announce itself three seconds early");
        require(run.update(3.0, false) && run.isAssaultActive(), "The first wave must launch once at 28s");
        require(!run.update(1.0, false) && run.getAssaultNumber() == 1, "An active wave must not launch twice");
        run.update(7.0, false);
        require(run.isRecovering(), "An assault must allow recovery time");
        run.update(26.0, false);
        require(run.getAssaultNumber() == 2 && run.getAssault() == SurvivalDirector.Assault.RUSH,
                "Assault formations must change between waves");
        run.update(30.0, true);
        require(!run.isAssaultActive() && !run.isWarning(), "Boss fights must suspend ordinary assaults");
        require(!run.update(1.0, false), "Finishing a boss must not unleash a delayed wave backlog");
        for (int i = 1; i <= 10; i++) {
            require(run.registerKill(false) == (i % 5 == 0), "Each fifth kill must reward exactly once");
        }
        require(run.getKills() == 10 && run.getBestStreak() == 10 && run.getScore() > 1000,
                "Rapid kills must build score and a streak");
        run.update(6.1, false);
        require(run.getStreak() == 0 && run.getBestStreak() == 10, "A streak must expire but keep its best record");
        require(!run.registerKill(false) && run.getStreak() == 1, "A late kill must start a new chain");
        double before = run.getAssaultCountdown();
        run.update(Double.NaN, false);
        run.update(-1.0, false);
        require(before == run.getAssaultCountdown(), "Invalid timer values must not corrupt encounters");
        run.reset();
        require(run.getKills() == 0 && run.getScore() == 0 && run.getBestStreak() == 0,
                "A fresh run must reset all encounter records");
    }

    private static void verifySpawnSafety() throws Exception {
        for (int[] viewport : new int[][]{{1280, 720}, {1920, 1080}, {800, 600}}) {
            GameLogic logic = active();
            logic.setViewportSize(viewport[0], viewport[1]);
            ((Random) field(logic, "random")).setSeed(71);
            Method spawn = method("spawnFixed", int.class);
            List<Enemy> enemies = enemies(logic);
            boolean left = false, right = false, above = false, below = false;
            for (int i = 0; i < 60; i++) {
                spawn.invoke(logic, 1);
                Enemy enemy = enemies.getLast();
                double x = enemy.getWorldX() - logic.getPlayerWorldX();
                double y = enemy.getWorldY() - logic.getPlayerWorldY();
                require(Math.abs(x) > viewport[0] / 2.0 + 75
                                || Math.abs(y) > viewport[1] / 2.0 + 75,
                        "Enemy art must spawn entirely beyond the logical viewport");
                left |= x < -100; right |= x > 100; above |= y < -100; below |= y > 100;
            }
            require(left && right && above && below, "Single-enemy batches must approach from every direction");
            spawn.invoke(logic, 50);
            require(enemies.size() <= 70, "Assaults and spawning must preserve the population cap");
        }
    }

    private static void verifyExperienceCarry() throws Exception {
        GameLogic logic = active();
        method("addExperience", int.class).invoke(logic, 70);
        require(logic.getLevel() == 2 && logic.getCurrentExp() == 60 && logic.isUpgradeMenuOpen(),
                "Leveling must retain excess XP and present one choice at a time");
        logic.chooseUpgrade(0);
        require(logic.getLevel() == 3 && logic.getCurrentExp() == 40 && logic.isUpgradeMenuOpen(),
                "Stored XP must grant the next earned upgrade after choosing");
        logic.chooseUpgrade(0);
        require(logic.getLevel() == 4 && logic.getCurrentExp() == 8 && logic.isUpgradeMenuOpen(),
                "Large rewards must not skip upgrade choices");
        logic.chooseUpgrade(0);
        require(!logic.isUpgradeMenuOpen() && logic.getCurrentExp() == 8,
                "The upgrade menu must close when all earned levels are handled");
    }

    private static void verifyFreshRun() throws Exception {
        GameLogic logic = active();
        Weapon original = (Weapon) field(logic, "weapon");
        double damage = original.getProjectileDamage(), interval = original.getFireInterval();
        original.addDamage(50); original.addCritChance(0.8); original.addFireSpeed(0.5);
        logic.getSurvivalDirector().registerKill(false);
        logic.startGame();
        Weapon fresh = (Weapon) field(logic, "weapon");
        require(fresh != original && fresh.getProjectileDamage() == damage
                        && fresh.getFireInterval() == interval && fresh.getCritChance() == 0,
                "Try again must reset weapon upgrades and cooldowns");
        require(logic.getSurvivalDirector().getKills() == 0, "Try again must reset score and streaks");
    }

    private static void verifyDeathRewards() throws Exception {
        GameLogic logic = active();
        logic.getAbilityManager().spendMana(80);
        Method damage = method("damageEnemy", Enemy.class, double.class);
        for (int i = 0; i < 5; i++) {
            Enemy victim = new TemplateEnemy(90 + i * 15, 60);
            enemies(logic).add(victim);
            damage.invoke(logic, victim, 1000.0);
            damage.invoke(logic, victim, 1000.0);
        }
        require(logic.getSurvivalDirector().getKills() == 5, "Each death must count once");
        require(logic.getAbilityManager().getMana() == 30 && logic.getCurrentExp() == 2,
                "Five rapid kills must restore 10 mana and grant 2 XP");
        require(logic.getEnemyCount() == 0, "The HUD enemy count must exclude fading corpses");
        Enemy poisonVictim = new TemplateEnemy(250, 0);
        poisonVictim.applyPoison(1000, 1.0);
        enemies(logic).add(poisonVictim);
        logic.update(0.1);
        require(poisonVictim.isDead() && poisonVictim.hasLootDropped(), "Poison kills must award loot");
        require(logic.getSurvivalDirector().getKills() == 6, "Poison kills must count toward streaks");
        logic.update(0.1);
        require(logic.getSurvivalDirector().getKills() == 6, "Fading poisoned corpses must not award again");
    }

    private static void verifySummonSafety() throws Exception {
        GameLogic logic = active();
        enemies(logic).add(new TemplateEnemy3(100, 0));
        enemies(logic).add(new TemplateEnemy(50, 0));
        enemies(logic).add(new TemplateEnemy(120, 0));
        // Splash iterates the enemy list. Death summons must not mutate that list mid-hit.
        Projectile splash = new Projectile(0, 0, 100, 0, 450, 1000, 2,
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB));
        splash.setSplashRadius(150);
        @SuppressWarnings("unchecked")
        List<Projectile> projectiles = (List<Projectile>) field(logic, "projectiles");
        projectiles.add(splash);
        method("updateProjectiles", double.class).invoke(logic, 0.2);
        logic.update(0.01);
        long summons = enemies(logic).stream().filter(e -> e instanceof TemplateEnemyMinion && !e.isDead()).count();
        require(summons == 3, "A summoner death must enqueue exactly three living minions safely");
    }

    private static void verifySuspendedSimulation() throws Exception {
        GameLogic logic = new GameLogic();
        logic.update(1.0);
        require(logic.getGameTimer() == 0, "Menus must suspend gameplay at the logic boundary");
        logic.startGame();
        logic.update(RunEntranceAnimation.DURATION);
        logic.togglePause(); logic.update(1.0);
        require(logic.getGameTimer() == 0, "Paused simulation must not advance encounters");
        logic.resume(); logic.toggleSkillMenu(); logic.update(1.0);
        require(logic.getGameTimer() == 0, "Skill selection must suspend the world");
        logic.toggleSkillMenu(); logic.update(1.0);
        require(logic.getGameTimer() == 1.0, "Simulation must resume after closing menus");
    }

    private static void verifyBossCadence() throws Exception {
        GameLogic logic = active();
        set(logic, "gameTimer", 94.95);
        set(logic, "whenToSpawn", 100.0);
        logic.update(0.1);
        RegionalEnemy boss = enemies(logic).stream()
                .filter(e -> e instanceof RegionalEnemy r && r.isBoss() && !r.isDead())
                .map(e -> (RegionalEnemy) e).findFirst().orElseThrow();
        require(logic.isBossEncounterActive(), "The first boss must start at 95 seconds");
        require(!logic.getSurvivalDirector().isAssaultActive(), "Bosses must own encounter pressure");
        boss.takeDamage(boss.getHealth() * 0.72);
        require(boss.getDamage() < logic.getPlayerMaxHealth() * 0.5,
                "The first boss must allow a full-health starting hero to survive two shots even in its last phase");
        method("damageEnemy", Enemy.class, double.class).invoke(logic, boss, 10000.0);
        logic.update(0.01);
        require("Emerald Forest".equals(logic.getActiveRegionName()), "Boss victory must advance the region");
        require(Math.abs(logic.getNextBossCountdown() - 95.0) < 0.001,
                "The next region must have its own boss countdown");
    }

    private static void verifyProjectileOwnerRelease() throws Exception {
        GameLogic logic = active();
        EnemyDefinition ranged = EnemyCatalog.normalEnemies(EnemyRegion.RIVENDALE_TOWN).stream()
                .filter(EnemyDefinition::isRanged).findFirst().orElseThrow();
        RegionalEnemy archer = new RegionalEnemy(ranged, 0.0, 0.0);
        archer.update(1.0, 120.0, 0.0, 20.0);
        Projectile shot = archer.fireAt(120, 0);
        require(shot != null, "The archer must fire a test shot");
        archer.update(4.0, 120.0, 0.0, 20.0);
        require(!archer.canFireAt(120, 0), "An active projectile must reserve its owner");
        java.util.ArrayList<Projectile> shots = new java.util.ArrayList<>();
        shots.add(shot);
        method("trimListToLimit", List.class, int.class).invoke(logic, shots, 0);
        require(archer.canFireAt(120, 0), "Trimming an old projectile must allow its owner to fire again");
    }

    private static void verifyLongCombat() throws Exception {
        String[] directions = {"right", "down", "left", "up"};
        for (int hero = 0; hero < 5; hero++) {
            GameLogic logic = new GameLogic();
            logic.selectCharacter(hero); logic.startGame(); logic.setSoundEnabled(false);
            logic.update(RunEntranceAnimation.DURATION);
            Player player = (Player) field(logic, "player");
            boolean sawBoss = false;
            for (int frame = 0; frame < 3600; frame++) {
                // Keep the fixture alive to exercise three minutes of real combat for every class.
                player.heal(player.getMaxHealth());
                if (logic.isUpgradeMenuOpen()) logic.chooseUpgrade(frame % 3);
                for (int i = 0; i < directions.length; i++) {
                    logic.setKeyPressed(directions[i], i == (frame / 240) % directions.length);
                }
                if (frame % 50 == 0) logic.triggerAbility((frame / 50) % 4);
                logic.update(0.05);
                sawBoss |= logic.isBossEncounterActive();
                require(!logic.isGameOver(), "The protected long-combat fixture must survive");
                require(enemies(logic).size() <= 70, "Long fights must keep enemy population bounded");
                require(Double.isFinite(logic.getPlayerWorldX()) && Double.isFinite(logic.getPlayerWorldY()),
                        "Combat and movement must remain finite");
            }
            require(sawBoss && logic.getSurvivalDirector().getAssaultNumber() >= 2,
                    "Every hero must exercise assault cycles and a boss encounter during the soak check");
        }
    }

    private static void verifyReflectedContactOrder() throws Exception {
        for (boolean enemyFirst : new boolean[]{true, false}) {
            GameLogic logic = active();
            Enemy late = new TemplateEnemy(100, 0);
            Enemy early = new TemplateEnemy(-100, 0);
            if (enemyFirst) enemies(logic).add(early);
            enemies(logic).add(late);
            double earlyHealth = early.getHealth(), lateHealth = late.getHealth();
            Projectile shot = new Projectile(-200, 0, 200, 0, 1000, 2, 1,
                    new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB));
            shot.setOwner(new TemplateEnemy3(500, 0));
            @SuppressWarnings("unchecked")
            List<Projectile> shots = (List<Projectile>) field(logic, "enemyProjectiles");
            shots.add(shot);
            method("updateEnemyProjectiles", double.class).invoke(logic, 0.4);
            require(late.getHealth() == lateHealth, "A reflected shot must not hit an enemy beyond its first contact");
            require(enemyFirst ? early.getHealth() < earlyHealth
                            && logic.getPlayerHealth() == logic.getPlayerMaxHealth()
                            : logic.getPlayerHealth() < logic.getPlayerMaxHealth(),
                    "Reflected shots must resolve the earliest enemy or player along their swept path");
            require(shots.isEmpty(), "A reflected projectile must be removed after first contact");
        }
    }

    private static GameLogic active() {
        GameLogic logic = new GameLogic();
        logic.startGame();
        logic.setSoundEnabled(false);
        logic.update(RunEntranceAnimation.DURATION);
        return logic;
    }
    @SuppressWarnings("unchecked")
    private static List<Enemy> enemies(GameLogic logic) throws Exception { return (List<Enemy>) field(logic, "enemies"); }
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    private static Method method(String name, Class<?>... types) throws Exception {
        Method method = GameLogic.class.getDeclaredMethod(name, types); method.setAccessible(true); return method;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
