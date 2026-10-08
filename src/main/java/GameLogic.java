import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameLogic {
    // The panel width is used to keep new enemies outside the visible area.
    private static final int PANEL_WIDTH = 1280;
    private static final int PANEL_HEIGHT = 720;
    private static final double INITIAL_SPAWN_DELAY = 1.2;
    private static final double REPOSITION_INTERVAL = 10.0;
    private static final double SPAWN_CIRCLE_DIAMETER = PANEL_WIDTH + 200.0;
    private static final double SPAWN_RADIUS = SPAWN_CIRCLE_DIAMETER / 2.0;
    private static final double ENEMY_CLUMP_RADIUS = 40.0;
    private static final int MAX_ENEMIES = 70;
    private static final int MAX_PLAYER_PROJECTILES = 80;
    private static final int MAX_ENEMY_PROJECTILES = 120;
    private static final int MAX_GEMS = 140;
    private static final double TOO_FAR_DISTANCE = PANEL_WIDTH * 2.0;
    private static final double SHOOT_RANGE = 300.0;
    private static final double GEM_PULL_RADIUS = 170.0;
    private static final double GEM_COLLECTION_RADIUS = 18.0;
    private static final double GEM_ACCELERATION = 340.0;
    private static final double GEM_MAX_SPEED = 260.0;
    private static final int MAX_SECRET_JPGS = 3;
    private static final double SECRET_SPAWN_INTERVAL = 12.0;
    private static final double FIRST_BOSS_SPAWN_TIME = 95.0;
    private static final double ELITE_BOSS_DELAY_AFTER_BOSS = 180.0;
    private static final int LEVEL_UP_EXP_BONUS = 6;

    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final List<SecretJpg> secretJpgs = new ArrayList<>();
    private int secretCycleIndex;
    private double secretCycleTimer;
    private Weapon weapon = new AutoFireWeapon();
    private final SurvivalDirector survivalDirector = new SurvivalDirector();
    private final RunEntranceAnimation runEntrance = new RunEntranceAnimation();
    private double entranceOriginX;
    private double entranceOriginY;
    private final List<Enemy> pendingCombatSummons = new ArrayList<>();
    private int viewportWidth = PANEL_WIDTH;
    private int viewportHeight = PANEL_HEIGHT;
    private double assaultDirection;
    private final Ability legacyAbility = new TemplateAbility();
    private final AbilityManager abilityManager = new AbilityManager();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> enemyProjectiles = new ArrayList<>();
    private final List<AbilityVisualEffect> abilityVisualEffects = new ArrayList<>();
    private final List<ScheduledAbilityImpact> scheduledAbilityImpacts = new ArrayList<>();
    private final List<SkillProjectile> skillProjectiles = new ArrayList<>();
    private final List<SkillWave> skillWaves = new ArrayList<>();
    private SkillDash skillDash;
    private final List<CombatImpactEffect> combatImpactEffects = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private PendingMeleeAttack pendingMeleeAttack;
    private final Random random = new Random();
    private final List<Double> spawnQueue = new ArrayList<>();
    private static final List<String> GENERIC_UPGRADE_NAMES = Arrays.asList(
            "Vitality",
            "Swiftness",
            "Magnetism",
            "Critical Hit",
            "Rapid Fire",
            "Heavy Blows"
    );
    private final List<String> upgradeChoices = new ArrayList<>();
    private final List<String> characterNames = Arrays.asList(
            "Eumann",
            "Haze",
            "Yuexin",
            "Ziea",
            "Sire Rakki"
    );
    private final List<String> characterClassNames = Arrays.asList(
            "BLACK KNIGHT",
            "ASSASSIN",
            "PRIEST",
            "ELEMENTALIST",
            "GUARDIAN"
    );
    private final List<String> characterRoles = Arrays.asList(
            "Melee / Tank",
            "Melee / Burst",
            "Support / Holy Magic",
            "Magic / AoE",
            "Tank / Defense / Crowd Control"
    );
    private final List<String> characterWeapons = Arrays.asList(
            "Long Sword + Shield",
            "Twin Daggers",
            "Holy Staff",
            "Elemental Staff",
            "Aegis Greatshield"
    );
    private final List<String> characterPortraitPaths = Arrays.asList(
            "/main/resources/character/CharEumann.png",
            "/main/resources/character/CharHaze.png",
            "/main/resources/character/CharYuexin.png",
            "/main/resources/character/CharacterZiea.png",
            "/main/resources/character/CharSirRakki.png"
    );
    private final List<List<String>> characterWeaponImagePaths = Arrays.asList(
            Arrays.asList("/main/resources/weapons/long_sword.png", "/main/resources/weapons/shield.png"),
            Arrays.asList("/main/resources/weapons/twin_daggers.png"),
            Arrays.asList("/main/resources/weapons/holy_staff.png"),
            Arrays.asList("/main/resources/weapons/elemental_staff.png"),
            Arrays.asList("/main/resources/weapons/aegis_greatshield.png")
    );
    private final List<AbilityClass> characterAbilityClasses = Arrays.asList(
            AbilityClass.BLACK_KNIGHT,
            AbilityClass.ASSASSIN,
            AbilityClass.PRIEST,
            AbilityClass.ELEMENTALIST,
            AbilityClass.GUARDIAN
    );
    private final List<List<String>> characterActiveSkillIds = Arrays.asList(
            Arrays.asList("heavy_slash", "shield_bash", "earth_shatter", "knights_wrath"),
            Arrays.asList("shadow_strike", "shadow_step", "death_mark", "twin_fang"),
            Arrays.asList("heal", "holy_bolt", "holy_shield", "divine_light"),
            Arrays.asList("flame_burst", "ice_shard", "lightning_strike", "elemental_storm"),
            Arrays.asList("shield_fortress", "iron_charge", "earthbreaker", "guardians_roar")
    );
    private final List<String> characterPassiveSkillIds = Arrays.asList(
            "iron_guard",
            "shadow_assassin",
            "blessing",
            "cataclysm",
            "unbreakable"
    );
    private final List<String> characterPassiveNames = Arrays.asList(
            "Berserker's Will",
            "Assassin's Instinct",
            "Faith",
            "Elemental Mastery",
            "Unbreakable"
    );
    private int selectedCharacterIndex;
    private double whenToSpawn = INITIAL_SPAWN_DELAY;
    private double gameTimer;
    private double repositionTimer;
    private double secretSpawnTimer;
    private boolean bossSpawned;
    private boolean bossDefeated;
    private boolean eliteBossSpawned;
    private double eliteBossSpawnTime;
    private boolean finalBossSpawned;
    private double finalBossSpawnTime;
    private double finalBossPulseTimer;
    private double finalBossLaserTimer;
    private double finalBossWallTimer;
    private int regionalBossIndex;
    private double nextRegionalBossSpawnTime = FIRST_BOSS_SPAWN_TIME;
    private int level = 1;
    private int exp = 0;
    private int expToNextLevel = 10;
    private boolean upgradeMenuOpen;
    private boolean mainMenuOpen = true;
    private boolean characterSelectOpen;
    private boolean gameStarted;
    private boolean paused;
    private boolean settingsOpen;
    private boolean skillMenuOpen;
    private boolean soundEnabled = true;
    private boolean debugInfoVisible;
    private boolean gameOver;
    private double gameOverTimer;
    private double damageReductionTimer;
    private double damageReductionMultiplier = 1.0;
    private double damageBoostTimer;
    private double damageBoostMultiplier = 1.0;
    private double passiveDamageTakenMultiplier = 1.0;
    private double passiveAbilityPowerMultiplier = 1.0;
    private double passiveHealingMultiplier = 1.0;
    private double guardianFortressRemaining;
    private double guardianBlockFeedbackRemaining;
    private GuardianCharge guardianCharge;
    private GuardianRoar guardianRoar;
    private double screenShakeTime;
    private double screenShakeDuration;
    private double screenShakeStrength;
    private double manaPulseTime;
    private Color manaPulseColor = new Color(75, 170, 255);
    private final List<ExplosionParticle> explosionParticles = new ArrayList<>();

    public GameLogic() {
        player = createSelectedPlayer();
        syncAutoAttackSprite();
        refreshUpgradeChoices();
    }

    public void setKeyPressed(String direction, boolean pressed) {
        // GamePanel sends input here instead of changing the player directly.
        player.setKeyPressed(direction, pressed);
    }

    public void setViewportSize(int width, int height) {
        if (width > 0 && height > 0) {
            viewportWidth = width;
            viewportHeight = height;
        }
    }

    public SurvivalDirector getSurvivalDirector() { return survivalDirector; }
    public boolean isRunEntrancePlaying() { return runEntrance.isPlaying(); }
    public RunEntranceAnimation getRunEntranceAnimation() { return runEntrance; }
    public boolean isBossEncounterActive() { return isBossWaveActive(); }
    public boolean hasMoreRegionalBosses() { return regionalBossIndex < EnemyRegion.values().length; }
    public double getNextBossCountdown() { return Math.max(0.0, nextRegionalBossSpawnTime - gameTimer); }

    public void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0) return;
        if (gameOver) {
            updateGameOver(deltaTime);
            return;
        }

        if (!gameStarted || paused || settingsOpen || skillMenuOpen || upgradeMenuOpen) {
            return;
        }

        if (runEntrance.isPlaying()) {
            if (player.getHealth() <= 0.0) {
                triggerGameOver();
                return;
            }
            // Consume only the entrance portion of this tick. A longer tick then
            // advances normal gameplay by its remainder, regardless of frame rate.
            double entranceStep = Math.min(deltaTime, runEntrance.getRemainingDuration());
            updateFeedback(entranceStep);
            boolean landed = runEntrance.update(entranceStep);
            player.updatePreview(entranceStep);
            if (landed) {
                double remainingShake = 0.22 - (runEntrance.getAge() - RunEntranceAnimation.FALL_DURATION);
                if (remainingShake > 0.0) addScreenShake(remainingShake, 3.5 * remainingShake / 0.22);
            }
            deltaTime = Math.max(0.0, deltaTime - entranceStep);
            if (runEntrance.isPlaying() || deltaTime <= 0.0) return;
        }
        if (runEntrance.isVisible()) runEntrance.update(deltaTime);

        // Update all game objects once per timer tick.
        player.update(deltaTime);
        updatePendingMeleeAttack(deltaTime);
        abilityManager.update(deltaTime);
        updatePlayerBuffs(deltaTime);
        updateAbilityVisualEffects(deltaTime);
        updateFloatingTexts(deltaTime);
        updateFeedback(deltaTime);
        updateSecretJpgs(deltaTime);

        gameTimer += deltaTime;
        checkBossSpawns();
        updateFinalBossScript(deltaTime);

        // Start small and ramp up the wave size over time instead of instantly
        // surrounding the player with a full ring at startup.
        boolean bossWaveActive = (bossSpawned && !bossDefeated) ||
                (eliteBossSpawned && hasEliteBossAlive()) ||
                (finalBossSpawned && hasFinalBossAlive());
        if (survivalDirector.update(deltaTime, bossWaveActive)) {
            spawnAssault();
        }
        if (!bossWaveActive) {
            while (gameTimer >= whenToSpawn) {
                int enemiesToSpawn = getSpawnBatchSize();
                spawnFixed(enemiesToSpawn);
                whenToSpawn += getSpawnInterval();
            }
            if (!spawnQueue.isEmpty()) {
                spawnQueue.sort(Double::compareTo);
                while (!spawnQueue.isEmpty() && gameTimer >= spawnQueue.getFirst()) {
                    spawnQueue.removeFirst();
                    spawnOne();
                }
            }
        }

        repositionTimer += deltaTime;
        while (repositionTimer >= REPOSITION_INTERVAL) {
            repositionTimer -= REPOSITION_INTERVAL;
            repositionFarEnemies();
        }

        List<Enemy> pendingSummons = new ArrayList<>();
        int enemiesAtStartOfFrame = enemies.size();
        for (int enemyIndex = 0; enemyIndex < enemiesAtStartOfFrame; enemyIndex++) {
            Enemy enemy = enemies.get(enemyIndex);
            // Enemies chase the player's current position in world space.
            enemy.update(deltaTime, player.getWorldX(), player.getWorldY(),
                    player.getCollisionRadius());
            if (enemy.isDead()) {
                dropGem(enemy);
                continue;
            }

            if (enemy instanceof BossEnemy boss && boss.shouldSummon()) {
                pendingSummons.addAll(boss.createSummons(boss.getWorldX(), boss.getWorldY(), random));
                boss.resetSummonCooldown();
            }

            if (enemy instanceof RegionalEnemy regionalEnemy) {
                if (regionalEnemy.phaseChangedThisFrame()) {
                    addFloatingText("PHASE " + regionalEnemy.getBossPhase(),
                            regionalEnemy.getWorldX(),
                            regionalEnemy.getWorldY() - regionalEnemy.getCollisionRadius() * 1.4,
                            new Color(255, 190, 90));
                    addScreenShake(0.16, 5.0);
                }
                if (regionalEnemy.shouldSummon()) {
                    pendingSummons.addAll(regionalEnemy.createSummons(random));
                    regionalEnemy.resetSummonCooldown();
                }
            }

            if (enemy instanceof FinalBossEnemy finalBoss && finalBoss.shouldSummon()) {
                pendingSummons.addAll(finalBoss.createMinions(random, player.getWorldX(), player.getWorldY()));
                finalBoss.resetSummonCooldown();
            }

            if (enemy instanceof EliteBossEnemy eliteBoss && eliteBoss.shouldApplyDebuff()) {
                eliteBoss.applyBreakEffects(player, abilityManager);
            }

            // Damage is time-based, so the amount does not depend on frame rate.
            if (!enemy.isStunned() && enemy.isCollidingWith(player.getWorldX(), player.getWorldY(),
                    player.getCollisionRadius())) {
                if (enemy instanceof TemplateEnemyMinion minion) {
                    applyIncomingDamage(minion.getDamage() * deltaTime, minion,
                            minion.getWorldX(), minion.getWorldY());
                    player.applySlow(TemplateEnemyMinion.SLOW_DURATION,
                            TemplateEnemyMinion.SLOW_MULTIPLIER);
                    if (minion.shouldExplodeOnContact(player.getWorldX(), player.getWorldY(),
                            player.getCollisionRadius())) {
                        triggerMinionExplosion(minion);
                        continue;
                    }
                } else {
                    applyIncomingDamage(enemy.getDamage() * deltaTime, enemy,
                            enemy.getWorldX(), enemy.getWorldY());
                }
            }
        }
        addPendingEnemies(pendingSummons);

        for (int firstIndex = 0; firstIndex < enemies.size(); firstIndex++) {
            // Check every unique enemy pair so enemies push apart instead of clumping.
            for (int secondIndex = firstIndex + 1;
                    secondIndex < enemies.size(); secondIndex++) {
                Enemy firstEnemy = enemies.get(firstIndex);
                Enemy secondEnemy = enemies.get(secondIndex);
                double differenceX = firstEnemy.getWorldX() - secondEnemy.getWorldX();
                double differenceY = firstEnemy.getWorldY() - secondEnemy.getWorldY();
                double broadPhase = firstEnemy.getCollisionRadius()
                        + secondEnemy.getCollisionRadius() + 12.0;
                if (differenceX * differenceX + differenceY * differenceY
                        <= broadPhase * broadPhase) {
                    firstEnemy.separateFrom(secondEnemy);
                }
            }
        }

        Enemy target = findAutoAttackTarget();
        Projectile projectile = null;
        if (player.isAimLocked() && weapon instanceof AutoFireWeapon autoFireWeapon) {
            autoFireWeapon.tickCooldown(deltaTime);
        }
        if (!player.isAimLocked()) {
            if (weapon instanceof AutoFireWeapon autoFireWeapon) {
                if (player.isSkillAnimationActive()) {
                    // Keep the cooldown moving while a skill owns the character's pose.
                    autoFireWeapon.tickCooldown(deltaTime);
                } else if (autoFireWeapon.isRangedAttack()) {
                    if (autoFireWeapon.canAttack(deltaTime)) {
                        if (target != null) {
                            player.faceToward(target.getWorldX(), target.getWorldY());
                        }
                        player.playAttackAnimation(null, autoFireWeapon.getSwingDuration());
                        projectile = autoFireWeapon.createRangedAttack(
                                player.getWeaponCastWorldX(), player.getWeaponCastWorldY(),
                                target, player.getRecentMoveX(), player.getRecentMoveY());
                    }
                } else {
                    Enemy hitTarget = autoFireWeapon.updateMeleeAttack(deltaTime,
                            player.getWorldX(), player.getWorldY(), target);
                    if (hitTarget != null) {
                        player.faceToward(hitTarget.getWorldX(), hitTarget.getWorldY());
                        double swingDuration = autoFireWeapon.getSwingDuration();
                        player.playAttackAnimation(null, swingDuration);
                        scheduleMeleeAttack(autoFireWeapon.getAttackRange(),
                                autoFireWeapon.rollAttackDamage(), swingDuration);
                    }
                }
            } else {
                projectile = weapon.update(deltaTime, player.getWorldX(),
                        player.getWorldY(), target,
                        player.getRecentMoveX(), player.getRecentMoveY());
            }
        }
        if (projectile != null) {
            projectiles.add(projectile);
            trimListToLimit(projectiles, MAX_PLAYER_PROJECTILES);
        }

        spawnEnemyProjectiles(deltaTime);
        updateProjectiles(deltaTime);
        updateEnemyProjectiles(deltaTime);
        updateGems(deltaTime);
        addPendingEnemies(pendingCombatSummons);
        pendingCombatSummons.clear();
        enemies.removeIf(Enemy::isFinishedFading);

        if (player.getHealth() <= 0.0) {
            triggerGameOver();
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }

        gameOver = true;
        runEntrance.reset();
        gameOverTimer = 0.0;
        for (int index = 0; index < 60; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 30.0 + random.nextDouble() * 220.0;
            explosionParticles.add(new ExplosionParticle(
                    player.getWorldX(),
                    player.getWorldY(),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    10.0 + random.nextDouble() * 20.0,
                    0.8 + random.nextDouble() * 0.8));
        }
    }

    private void triggerMinionExplosion(TemplateEnemyMinion minion) {
        double explosionX = minion.getWorldX();
        double explosionY = minion.getWorldY();
        for (int index = 0; index < 18; index++) {
            double angle = (Math.PI * 2.0 * index) / 18.0 + random.nextDouble() * 0.8;
            double speed = 60.0 + random.nextDouble() * 120.0;
            explosionParticles.add(new ExplosionParticle(
                    explosionX,
                    explosionY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    8.0 + random.nextDouble() * 12.0,
                    0.5 + random.nextDouble() * 0.6));
        }
        applyIncomingDamage(minion.getExplosionDamage(), minion, explosionX, explosionY);
        minion.takeDamage(Double.MAX_VALUE);
        dropGem(minion);
    }

    private void updateGameOver(double deltaTime) {
        gameOverTimer += deltaTime;
        Iterator<ExplosionParticle> particleIterator = explosionParticles.iterator();
        while (particleIterator.hasNext()) {
            ExplosionParticle particle = particleIterator.next();
            particle.update(deltaTime);
            if (particle.isExpired()) {
                particleIterator.remove();
            }
        }
    }

    private void updateAbilityVisualEffects(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) return;
        updateSkillDash(deltaTime);
        Iterator<ScheduledAbilityImpact> scheduledIterator = scheduledAbilityImpacts.iterator();
        while (scheduledIterator.hasNext()) {
            ScheduledAbilityImpact impact = scheduledIterator.next();
            impact.update(deltaTime);
            while (impact.hasReadyHit()) {
                fireAbilityImpact(impact, impact.consumeReadyHit());
            }
            impact.previousPlayerX = player.getWorldX();
            impact.previousPlayerY = player.getWorldY();
            if (impact.isFinished()) {
                scheduledIterator.remove();
            }
        }

        updateGuardianActions(deltaTime);
        updateSkillProjectiles(deltaTime);
        updateSkillWaves(deltaTime);

        Iterator<AbilityVisualEffect> effectIterator = abilityVisualEffects.iterator();
        while (effectIterator.hasNext()) {
            AbilityVisualEffect effect = effectIterator.next();
            effect.setCasterPosition(player.getWorldX(), player.getWorldY());
            effect.setCastOrigin(player.getSkillSourceWorldX(), player.getSkillSourceWorldY());
            effect.setSourcePose(player.getSkillSourceRotation(), player.getHeldWeaponSideX() < 0);
            effect.update(deltaTime);
            if (effect.isExpired()) {
                effectIterator.remove();
            }
        }

        Iterator<CombatImpactEffect> impactIterator = combatImpactEffects.iterator();
        while (impactIterator.hasNext()) {
            CombatImpactEffect effect = impactIterator.next();
            effect.update(deltaTime);
            if (effect.isExpired()) {
                impactIterator.remove();
            }
        }
    }

    private void updateFloatingTexts(double deltaTime) {
        Iterator<FloatingText> textIterator = floatingTexts.iterator();
        while (textIterator.hasNext()) {
            FloatingText text = textIterator.next();
            text.update(deltaTime);
            if (text.isExpired()) {
                textIterator.remove();
            }
        }
    }

    private void updateFeedback(double deltaTime) {
        if (screenShakeTime > 0.0) {
            screenShakeTime = Math.max(0.0, screenShakeTime - deltaTime);
        }
        if (manaPulseTime > 0.0) {
            manaPulseTime = Math.max(0.0, manaPulseTime - deltaTime);
        }
    }

    private void updatePlayerBuffs(double deltaTime) {
        guardianFortressRemaining = Math.max(0.0, guardianFortressRemaining - deltaTime);
        guardianBlockFeedbackRemaining = Math.max(0.0, guardianBlockFeedbackRemaining - deltaTime);
        player.setGuardianFortressRemaining(guardianFortressRemaining);
        if (damageReductionTimer > 0.0) {
            damageReductionTimer = Math.max(0.0, damageReductionTimer - deltaTime);
            if (damageReductionTimer <= 0.0) {
                damageReductionMultiplier = 1.0;
            }
        }
        if (damageBoostTimer > 0.0) {
            damageBoostTimer = Math.max(0.0, damageBoostTimer - deltaTime);
            if (damageBoostTimer <= 0.0) {
                damageBoostMultiplier = 1.0;
            }
        }
    }

    public void drawAbilityGroundEffects(Graphics2D graphics, int centerX, int centerY) {
        if (runEntrance.isVisible()) {
            runEntrance.drawGround(graphics,
                    centerX + (int) Math.round(entranceOriginX + getWorldOffsetX()),
                    centerY + entranceOriginY + getWorldOffsetY() + player.getVisualFootOffset());
        }
        for (AbilityVisualEffect effect : abilityVisualEffects) {
            if (effect.getLayer() == AbilityVisualEffect.Layer.GROUND) {
                effect.draw(graphics, centerX, centerY, getWorldOffsetX(), getWorldOffsetY());
            }
        }
    }

    public void drawAbilityBursts(Graphics2D graphics, int centerX, int centerY) {
        if (runEntrance.isVisible()) {
            runEntrance.drawFront(graphics,
                    centerX + (int) Math.round(entranceOriginX + getWorldOffsetX()),
                    centerY + (int) Math.round(entranceOriginY + getWorldOffsetY()),
                    player.getVisualFootOffset());
        }
        for (AbilityVisualEffect effect : abilityVisualEffects) {
            if (effect.getLayer() != AbilityVisualEffect.Layer.GROUND) {
                effect.draw(graphics, centerX, centerY, getWorldOffsetX(), getWorldOffsetY());
            }
        }
        for (CombatImpactEffect effect : combatImpactEffects) {
            effect.draw(graphics, centerX, centerY, getWorldOffsetX(), getWorldOffsetY());
        }
        for (FloatingText text : floatingTexts) {
            text.draw(graphics, centerX, centerY, getWorldOffsetX(), getWorldOffsetY());
        }
    }

    public void drawGameOverEffect(Graphics2D graphics, int centerX, int centerY) {
        if (!gameOver) {
            return;
        }

        for (ExplosionParticle particle : explosionParticles) {
            double screenX = centerX + particle.x + getWorldOffsetX();
            double screenY = centerY + particle.y + getWorldOffsetY();
            double alpha = Math.max(0.0, particle.life / particle.maxLife);
            int radius = (int) Math.round(particle.radius);
            graphics.setColor(new Color(255, 140, 40, (int) (alpha * 220.0)));
            graphics.fillOval((int) screenX - radius, (int) screenY - radius, radius * 2, radius * 2);
        }
    }

    private static class ExplosionParticle {
        private double x;
        private double y;
        private final double velocityX;
        private final double velocityY;
        private final double radius;
        private final double maxLife;
        private double life;

        private ExplosionParticle(double x, double y, double velocityX, double velocityY,
                double radius, double maxLife) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.radius = radius;
            this.maxLife = maxLife;
            this.life = maxLife;
        }

        private void update(double deltaTime) {
            life -= deltaTime;
            x += velocityX * deltaTime;
            y += velocityY * deltaTime;
        }

        private boolean isExpired() {
            return life <= 0.0;
        }
    }

    private Enemy findNearestLivingEnemyOnScreen() {
        Enemy nearestEnemy = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        double maxScreenX = viewportWidth * 0.5;
        double maxScreenY = viewportHeight * 0.5;

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            double differenceX = enemy.getWorldX() - player.getWorldX();
            double differenceY = enemy.getWorldY() - player.getWorldY();
            if (Math.abs(differenceX) > maxScreenX || Math.abs(differenceY) > maxScreenY) {
                continue;
            }

            double distanceSquared = differenceX * differenceX + differenceY * differenceY;
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestEnemy = enemy;
            }
        }
        return nearestEnemy;
    }

    private Enemy findAutoAttackTarget() {
        if (weapon instanceof AutoFireWeapon autoFireWeapon) {
            double targetRange = autoFireWeapon.isRangedAttack()
                    ? autoFireWeapon.getAttackRange() + 80.0
                    : autoFireWeapon.getAttackRange() + player.getCollisionRadius() + 12.0;
            return findNearestLivingEnemyInRange(targetRange);
        }
        Enemy target = findNearestLivingEnemyOnScreen();
        if (target == null) {
            target = findNearestLivingEnemyInRange(SHOOT_RANGE);
        }
        return target;
    }

    private Enemy findNearestLivingEnemyInRange(double maxDistance) {
        Enemy nearestEnemy = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        double maxDistanceSquared = maxDistance * maxDistance;

        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                double distanceSquared = enemy.distanceSquaredTo(player.getWorldX(), player.getWorldY());
                if (distanceSquared <= maxDistanceSquared && distanceSquared < nearestDistance) {
                    nearestDistance = distanceSquared;
                    nearestEnemy = enemy;
                }
            }
        }
        return nearestEnemy;
    }

    private void updateProjectiles(double deltaTime) {
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update(deltaTime);

            boolean hitEnemy = false;
            Enemy nearestContact = null;
            double nearestFraction = Double.POSITIVE_INFINITY;
            for (Enemy enemy : enemies) {
                double fraction = projectile.getCollisionFraction(enemy);
                if (!enemy.isDead() && fraction < nearestFraction) {
                    nearestFraction = fraction;
                    nearestContact = enemy;
                }
            }
            if (nearestContact != null) {
                    Enemy enemy = nearestContact;
                    if (enemy instanceof TemplateEnemy3 bossEnemy) {
                        bossEnemy.registerPlayerProjectileHit();
                        if (bossEnemy.shouldReflectPlayerProjectile(
                                player.getWorldX(), player.getWorldY())) {
                            Projectile reflected = bossEnemy.createReflectedProjectile(
                                    player.getWorldX(), player.getWorldY());
                            if (reflected != null) {
                                enemyProjectiles.add(reflected);
                                trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
                            }
                        }
                    }

                    damageEnemy(enemy, projectile.getDamage(), projectile.getDamageElement(),
                            projectile.getContactWorldX(nearestFraction), projectile.getContactWorldY(nearestFraction));
                    if (projectile.getSplashRadius() > 0.0) {
                        double splashX = enemy.getWorldX();
                        double splashY = enemy.getWorldY();
                        double radiusSquared = projectile.getSplashRadius() * projectile.getSplashRadius();
                        for (Enemy nearby : enemies) {
                            if (nearby != enemy && !nearby.isDead()
                                    && nearby.distanceSquaredTo(splashX, splashY) <= radiusSquared) {
                                damageEnemy(nearby, projectile.getDamage(), projectile.getDamageElement(),
                                        splashX, splashY);
                            }
                        }
                    }
                    hitEnemy = true;
            }

            if (hitEnemy || projectile.isExpired()) {
                projectileIterator.remove();
            }
        }
    }

    private void spawnEnemyProjectiles(double deltaTime) {
        for (Enemy enemy : enemies) {
            if (enemy.isDead() || enemy.isStunned()) continue;
            if (enemy instanceof RegionalEnemy regionalEnemy) {
                if (regionalEnemy.canFireAt(player.getWorldX(), player.getWorldY())) {
                    Projectile projectile = regionalEnemy.fireAt(player.getWorldX(), player.getWorldY());
                    if (projectile != null) {
                        enemyProjectiles.add(projectile);
                        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
                    }
                }
            } else if (enemy instanceof TemplateEnemy2 enemy2) {
                enemy2.updateFireCooldown(deltaTime);
                if (!enemy2.canFire()) {
                    continue;
                }

                double distanceSquared = enemy2.distanceSquaredTo(player.getWorldX(), player.getWorldY());
                double attackRange = 420.0;
                if (distanceSquared <= attackRange * attackRange) {
                    Projectile projectile = enemy2.fireAt(player.getWorldX(), player.getWorldY());
                    if (projectile != null) {
                        enemyProjectiles.add(projectile);
                        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
                    }
                }
            }
        }
    }

    private void updateEnemyProjectiles(double deltaTime) {
        Iterator<Projectile> projectileIterator = enemyProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update(deltaTime);

            boolean hitEnemy = false;
            double playerContact = projectile.getPlayerCollisionFraction(
                    player.getWorldX(), player.getWorldY(), player.getCollisionRadius());
            boolean hitPlayer = Double.isFinite(playerContact);

            if (projectile.getOwner() instanceof TemplateEnemy3) {
                Enemy nearestEnemy = null;
                double nearestContact = Double.POSITIVE_INFINITY;

                for (Enemy enemy : enemies) {
                    if (enemy == projectile.getOwner() || enemy.isDead()) {
                        continue;
                    }

                    double contact = projectile.getCollisionFraction(enemy);
                    if (contact < nearestContact) {
                        nearestContact = contact;
                        nearestEnemy = enemy;
                    }
                }

                if (nearestEnemy != null && nearestContact <= playerContact) {
                    damageEnemy(nearestEnemy, projectile.getDamage(), projectile.getDamageElement(),
                            projectile.getContactWorldX(nearestContact), projectile.getContactWorldY(nearestContact));
                    hitEnemy = true;
                    hitPlayer = false;
                }
            }

            if (hitEnemy || hitPlayer || projectile.isExpired()) {
                releaseProjectileOwner(projectile);
                if (hitPlayer) {
                    applyIncomingDamage(projectile.getDamage(), projectile.getOwner(),
                            projectile.getContactWorldX(playerContact), projectile.getContactWorldY(playerContact));
                }
                projectileIterator.remove();
            }
        }
    }

    private void dropGem(Enemy enemy) {
        if (enemy.hasLootDropped()) {
            return;
        }
        enemy.markLootDropped();
        if (enemy instanceof TemplateEnemy3 summoner && !summoner.hasSummonedMinions()) {
            pendingCombatSummons.addAll(summoner.createSummons(enemy.getWorldX(), enemy.getWorldY(), random));
        }
        boolean bossKill = enemy instanceof BossEnemy || enemy instanceof EliteBossEnemy
                || enemy instanceof FinalBossEnemy
                || (enemy instanceof RegionalEnemy regional && regional.isBoss());
        if (survivalDirector.registerKill(bossKill)) {
            abilityManager.restoreMana(10.0);
            addExperience(2);
            addFloatingText(survivalDirector.getStreak() + " KILL STREAK  +10 MP / +2 XP",
                    player.getWorldX(), player.getWorldY() - 88, new Color(145, 238, 223));
        }
        int gemCount = 1;
        if (enemy instanceof RegionalEnemy regionalEnemy) {
            gemCount = regionalEnemy.isBoss() ? 8 : Math.max(1,
                    regionalEnemy.getDefinition().getExperienceReward() / 5);
            if (regionalEnemy.isBoss()) {
                addFloatingText(regionalEnemy.getDefinition().getLootName(),
                        enemy.getWorldX(), enemy.getWorldY() - enemy.getCollisionRadius() * 1.2,
                        lootTextColor(regionalEnemy.getDefinition().getElement()));
            }
        }
        for (int index = 0; index < gemCount; index++) {
            double angle = Math.PI * 2.0 * index / gemCount;
            gems.add(new Gem(enemy.getWorldX() + Math.cos(angle) * 10.0,
                    enemy.getWorldY() + Math.sin(angle) * 10.0));
        }
        trimListToLimit(gems, MAX_GEMS);
    }

    private Color lootTextColor(DamageElement element) {
        return switch (element) {
            case FIRE, EXPLOSION -> new Color(255, 140, 55);
            case ICE -> new Color(150, 235, 255);
            case POISON -> new Color(110, 245, 90);
            case SHADOW -> new Color(210, 120, 255);
            case LIGHTNING -> new Color(255, 235, 95);
            case HOLY -> new Color(255, 245, 170);
            default -> new Color(255, 220, 140);
        };
    }

    private void updateGems(double deltaTime) {
        Iterator<Gem> gemIterator = gems.iterator();
        while (gemIterator.hasNext()) {
            Gem gem = gemIterator.next();
            gem.update(deltaTime, player.getWorldX(), player.getWorldY(), player.getPickupRadius());
            if (gem.isCollected()) {
                gemIterator.remove();
                addExperience(gem.getValue());
            }
        }
    }

    private void updateSecretJpgs(double deltaTime) {
        if (isBossWaveActive()) {
            secretJpgs.clear();
            secretCycleTimer = 0.0;
            return;
        }

        for (Iterator<SecretJpg> iterator = secretJpgs.iterator(); iterator.hasNext();) {
            SecretJpg secretJpg = iterator.next();
            secretJpg.update(deltaTime);
            if (!secretJpg.isVisible()) {
                iterator.remove();
                secretCycleTimer = 0.0;
            }
        }

        if (!secretJpgs.isEmpty()) {
            return;
        }

        secretCycleTimer += deltaTime;
        if (secretCycleTimer < 2.0) {
            return;
        }

        secretCycleTimer = 0.0;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double offsetX = Math.cos(angle) * 6.0;
        double offsetY = Math.sin(angle) * 6.0;
        double secretX = player.getWorldX() + offsetX;
        double secretY = player.getWorldY() + offsetY;

        SecretJpg secret = (secretCycleIndex % 2 == 0)
                ? new SecretJpg(secretX, secretY)
                : new SecretJpg2(secretX, secretY);
        secretJpgs.add(secret);
        secretCycleIndex++;
    }

    private boolean isBossWaveActive() {
        return (bossSpawned && !bossDefeated)
                || (eliteBossSpawned && hasEliteBossAlive())
                || (finalBossSpawned && hasFinalBossAlive());
    }

    private void updateFinalBossScript(double deltaTime) {
        FinalBossEnemy finalBoss = getAliveFinalBoss();
        if (finalBoss == null || finalBoss.isStunned()) {
            return;
        }

        if (finalBoss.isSplitState()) {
            finalBossPulseTimer += deltaTime;
            finalBossLaserTimer += deltaTime;
            finalBossWallTimer += deltaTime;

            if (finalBossPulseTimer >= 1.4) {
                finalBossPulseTimer = 0.0;
                spawnFinalBossSplitBurst(finalBoss);
            }
            if (finalBossLaserTimer >= 3.0) {
                finalBossLaserTimer = 0.0;
                spawnThinRealLaser(finalBoss);
            }
            if (finalBossWallTimer >= 8.0) {
                finalBossWallTimer = 0.0;
                triggerFinalBossWallLock(finalBoss);
            }
            return;
        }

        finalBossPulseTimer += deltaTime;
        finalBossLaserTimer += deltaTime;
        finalBossWallTimer += deltaTime;

        if (finalBossPulseTimer >= 3.2) {
            finalBossPulseTimer = 0.0;
            spawnFinalBossBurst(finalBoss);
        }
        if (finalBossLaserTimer >= 6.4) {
            finalBossLaserTimer = 0.0;
            spawnFinalBossLaser(finalBoss);
        }
        if (finalBossWallTimer >= 11.5) {
            finalBossWallTimer = 0.0;
            triggerFinalBossWallLock(finalBoss);
        }
    }

    private FinalBossEnemy getAliveFinalBoss() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof FinalBossEnemy finalBoss && !finalBoss.isDead()) {
                return finalBoss;
            }
        }
        return null;
    }

    private void spawnFinalBossBurst(FinalBossEnemy boss) {
        double bossX = boss.getWorldX();
        double bossY = boss.getWorldY();
        BufferedImage projectileSprite = ResourceLoader.loadImage("/main/resources/projectiles/LASER.png");

        for (int index = 0; index < 12; index++) {
            double angle = (Math.PI * 2.0 * index / 12.0) + random.nextDouble() * 0.32;
            double targetX = player.getWorldX() + Math.cos(angle) * 220.0;
            double targetY = player.getWorldY() + Math.sin(angle) * 220.0;
            Projectile shot = new Projectile(bossX, bossY, targetX, targetY,
                    220.0, 14.0, 12.0, projectileSprite, 10.0);
            shot.setOwner(boss);
            enemyProjectiles.add(shot);
        }
        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
    }

    private void spawnFinalBossLaser(FinalBossEnemy boss) {
        double targetX = player.getWorldX();
        double targetY = player.getWorldY();
        BufferedImage projectileSprite = ResourceLoader.loadImage("/main/resources/projectiles/LASER.png");
        Projectile laser = new Projectile(boss.getWorldX(), boss.getWorldY(), targetX, targetY,
                310.0, 22.0, 14.0, projectileSprite, 14.0);
        laser.setOwner(boss);
        enemyProjectiles.add(laser);
        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
    }

    private void spawnFinalBossSplitBurst(FinalBossEnemy boss) {
        BufferedImage projectileSprite = ResourceLoader.loadImage("/main/resources/projectiles/LASER.png");
        double[][] clones = {
                {boss.getWorldX(), boss.getWorldY()},
                {boss.getWorldX() - 100.0, boss.getWorldY() - 20.0},
                {boss.getWorldX() + 110.0, boss.getWorldY() + 18.0}
        };

        for (double[] clone : clones) {
            for (int index = 0; index < 8; index++) {
                double angle = (Math.PI * 2.0 * index / 8.0) + random.nextDouble() * 0.25;
                double targetX = clone[0] + Math.cos(angle) * 180.0;
                double targetY = clone[1] + Math.sin(angle) * 180.0;
                Projectile fakeShot = new Projectile(clone[0], clone[1], targetX, targetY,
                        210.0, 0.0, 8.0, projectileSprite, 12.0);
                fakeShot.setOwner(boss);
                enemyProjectiles.add(fakeShot);
            }
        }
        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
    }

    private void spawnThinRealLaser(FinalBossEnemy boss) {
        double targetX = player.getWorldX();
        double targetY = player.getWorldY();
        BufferedImage projectileSprite = ResourceLoader.loadImage("/main/resources/projectiles/LASER.png");
        Projectile laser = new Projectile(boss.getWorldX(), boss.getWorldY(), targetX, targetY,
                340.0, 16.0, 5.0, projectileSprite, 18.0);
        laser.setOwner(boss);
        enemyProjectiles.add(laser);
        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
    }

    private void triggerFinalBossWallLock(FinalBossEnemy boss) {
        player.applyMovementLock(2.2);
        player.applyAimLock(2.2);
        double baseX = player.getWorldX();
        double baseY = player.getWorldY();
        BufferedImage projectileSprite = ResourceLoader.loadImage("/main/resources/projectiles/LASER.png");
        for (int index = 0; index < 14; index++) {
            double angle = (Math.PI * 2.0 * index / 14.0);
            double targetX = baseX + Math.cos(angle) * 180.0;
            double targetY = baseY + Math.sin(angle) * 180.0;
            Projectile wallShot = new Projectile(boss.getWorldX(), boss.getWorldY(), targetX, targetY,
                    190.0, 18.0, 10.0, projectileSprite, 12.0);
            wallShot.setOwner(boss);
            enemyProjectiles.add(wallShot);
        }
        trimListToLimit(enemyProjectiles, MAX_ENEMY_PROJECTILES);
    }

    private Enemy getRandomEnemyNearPlayer() {
        List<Enemy> candidates = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.distanceSquaredTo(player.getWorldX(), player.getWorldY()) < 700.0 * 700.0) {
                candidates.add(enemy);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void refreshUpgradeChoices() {
        upgradeChoices.clear();
        List<String> remainingUpgrades = new ArrayList<>(GENERIC_UPGRADE_NAMES);
        while (upgradeChoices.size() < 3 && !remainingUpgrades.isEmpty()) {
            int index = random.nextInt(remainingUpgrades.size());
            upgradeChoices.add(remainingUpgrades.remove(index));
        }
    }

    public String getUpgradeDescription(String upgradeName) {
        return switch (upgradeName) {
            case "Vitality" -> "+10 max health";
            case "Swiftness" -> "+12 move speed";
            case "Magnetism" -> "+18 pickup radius";
            case "Critical Hit" -> "+8% crit chance";
            case "Rapid Fire" -> "-15% fire interval";
            case "Heavy Blows" -> "+1.5 damage";
            default -> "";
        };
    }

    private void applyUpgrade(String upgradeName) {
        switch (upgradeName) {
            case "Vitality" -> player.increaseMaxHealth(10.0);
            case "Swiftness" -> player.increaseSpeed(12.0);
            case "Magnetism" -> player.increasePickupRadius(18.0);
            case "Critical Hit" -> weapon.addCritChance(0.08);
            case "Rapid Fire" -> weapon.addFireSpeed(0.15);
            case "Heavy Blows" -> weapon.addDamage(1.5);
            default -> {
            }
        }
    }

    private void addExperience(int amount) {
        if (amount <= 0) return;
        exp += amount;
        openPendingLevelUp();
    }

    private void openPendingLevelUp() {
        if (!upgradeMenuOpen && exp >= expToNextLevel) {
            exp -= expToNextLevel;
            level++;
            if (soundEnabled) {
                AbilitySoundPlayer.playLevelUp();
            }
            expToNextLevel += LEVEL_UP_EXP_BONUS + level * 2;
            refreshUpgradeChoices();
            upgradeMenuOpen = true;
        }
    }

    private void checkBossSpawns() {
        if (bossSpawned && !bossDefeated && !hasBossAlive()) {
            bossDefeated = true;
            regionalBossIndex++;
            player.heal(player.getMaxHealth() - player.getHealth());
            resetSpawnCadence();
            nextRegionalBossSpawnTime = gameTimer + 95.0;
            if (regionalBossIndex < EnemyRegion.values().length) {
                addFloatingText("Next: " + EnemyRegion.byIndex(regionalBossIndex).getDisplayName(),
                        player.getWorldX(), player.getWorldY() - 64.0,
                        new Color(255, 220, 140));
            }
            return;
        }

        if ((!bossSpawned || bossDefeated)
                && regionalBossIndex < EnemyRegion.values().length
                && gameTimer >= nextRegionalBossSpawnTime) {
            EnemyRegion region = EnemyRegion.byIndex(regionalBossIndex);
            EnemyDefinition bossDefinition = EnemyCatalog.bossFor(region);
            if (bossDefinition == null) {
                return;
            }
            bossSpawned = true;
            bossDefeated = false;
            resetSpawnCadence();
            clearEnemiesForBossWave();
            spawnBoss(new RegionalEnemy(bossDefinition,
                    player.getWorldX() + SPAWN_RADIUS * 0.72,
                    player.getWorldY() + SPAWN_RADIUS * 0.32));
            addFloatingText("BOSS: " + bossDefinition.getDisplayName(),
                    player.getWorldX(), player.getWorldY() - 74.0,
                    new Color(255, 150, 80));
            addScreenShake(0.25, 7.0);
            return;
        }
    }

    private void resetSpawnCadence() {
        whenToSpawn = gameTimer + INITIAL_SPAWN_DELAY;
        spawnQueue.clear();
    }

    private boolean hasBossAlive() {
        for (Enemy enemy : enemies) {
            if (((enemy instanceof BossEnemy)
                    || (enemy instanceof RegionalEnemy regionalEnemy && regionalEnemy.isBoss()))
                    && !enemy.isDead()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEliteBossAlive() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof EliteBossEnemy && !enemy.isDead()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFinalBossAlive() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof FinalBossEnemy && !enemy.isDead()) {
                return true;
            }
        }
        return false;
    }

    private void clearEnemiesForBossWave() {
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (!(enemy instanceof BossEnemy)
                    && !(enemy instanceof RegionalEnemy regionalEnemy && regionalEnemy.isBoss())
                    && !(enemy instanceof EliteBossEnemy)
                    && !(enemy instanceof FinalBossEnemy)
                    && !(enemy instanceof TemplateEnemyMinion)
                    && !(enemy instanceof BlueFinalMinion)
                    && !(enemy instanceof GreenFinalMinion)
                    && !(enemy instanceof RedFinalMinion)) {
                enemyIterator.remove();
            }
        }
        projectiles.clear();
        for (Projectile projectile : enemyProjectiles) releaseProjectileOwner(projectile);
        enemyProjectiles.clear();
        pendingCombatSummons.clear();
    }

    private void spawnBoss(Enemy boss) {
        if (boss == null || boss.isDead()) {
            return;
        }
        enemies.add(boss);
    }

    public boolean isUpgradeMenuOpen() {
        return upgradeMenuOpen;
    }

    public boolean isMainMenuOpen() {
        return mainMenuOpen;
    }

    public boolean isCharacterSelectOpen() {
        return characterSelectOpen;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isSettingsOpen() {
        return settingsOpen;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isDebugInfoVisible() {
        return debugInfoVisible;
    }

    public Ability getAbility() {
        RpgAbility[] equippedAbilities = abilityManager.getEquippedAbilities();
        return equippedAbilities.length > 0 && equippedAbilities[0] != null
                ? equippedAbilities[0]
                : legacyAbility;
    }

    private static class FloatingText {
        private static final double MAX_LIFE = 0.85;

        private final String text;
        private final double x;
        private final double y;
        private final Color color;
        private double life = MAX_LIFE;

        private FloatingText(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        private void update(double deltaTime) {
            life -= deltaTime;
        }

        private boolean isExpired() {
            return life <= 0.0;
        }

        private void draw(Graphics2D graphics, int centerX, int centerY,
                double cameraX, double cameraY) {
            double progress = 1.0 - Math.max(0.0, life / MAX_LIFE);
            int alpha = Math.max(0, Math.min(255, (int) Math.round(255.0 * life / MAX_LIFE)));
            Font previousFont = graphics.getFont();
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 16));
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            int screenX = (int) Math.round(centerX + x + cameraX - textWidth / 2.0);
            int screenY = (int) Math.round(centerY + y + cameraY - progress * 34.0);
            graphics.setColor(new Color(0, 0, 0, Math.min(alpha, 190)));
            graphics.drawString(text, screenX - 1, screenY);
            graphics.drawString(text, screenX + 1, screenY);
            graphics.drawString(text, screenX, screenY - 1);
            graphics.drawString(text, screenX, screenY + 1);
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            graphics.drawString(text, screenX, screenY);
            graphics.setFont(previousFont);
        }
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public double getPlayerHealth() {
        return player.getHealth();
    }

    public double getPlayerMaxHealth() {
        return player.getMaxHealth();
    }

    public void triggerAbility() {
        triggerAbility(0);
    }

    public void triggerAbility(int slotIndex) {
        if (gameOver || upgradeMenuOpen || paused || settingsOpen || skillMenuOpen || !gameStarted
                || runEntrance.isPlaying()) {
            return;
        }
        abilityManager.triggerSlot(slotIndex, this, level);
    }

    public void showMainMenu() {
        resetRunState();
        mainMenuOpen = true;
        characterSelectOpen = false;
        gameStarted = false;
        paused = false;
        settingsOpen = false;
        skillMenuOpen = false;
    }

    public void showCharacterSelection() {
        runEntrance.reset();
        mainMenuOpen = false;
        characterSelectOpen = true;
        gameStarted = false;
        paused = false;
        settingsOpen = false;
        skillMenuOpen = false;
    }

    public void startGame() {
        resetRunState();
        mainMenuOpen = false;
        characterSelectOpen = false;
        gameStarted = true;
        paused = false;
        settingsOpen = false;
        skillMenuOpen = false;
        entranceOriginX = player.getWorldX();
        entranceOriginY = player.getWorldY();
        Color entranceColor = switch (selectedCharacterIndex) {
            case 1 -> new Color(196, 134, 244);
            case 2 -> new Color(255, 226, 154);
            case 3 -> new Color(131, 220, 255);
            case 4 -> new Color(230, 188, 104);
            default -> new Color(255, 170, 96);
        };
        runEntrance.start(viewportHeight * 0.5 + 110.0, entranceColor);
    }

    private void resetRunState() {
        runEntrance.reset();
        entranceOriginX = entranceOriginY = 0.0;
        player = createSelectedPlayer();
        weapon = new AutoFireWeapon();
        syncAutoAttackSprite();
        survivalDirector.reset();
        pendingCombatSummons.clear();
        assaultDirection = 0.0;
        gameOver = false;
        gameOverTimer = 0.0;
        explosionParticles.clear();
        enemies.clear();
        gems.clear();
        secretJpgs.clear();
        projectiles.clear();
        enemyProjectiles.clear();
        abilityVisualEffects.clear();
        scheduledAbilityImpacts.clear();
        skillProjectiles.clear();
        skillWaves.clear();
        skillDash = null;
        combatImpactEffects.clear();
        floatingTexts.clear();
        spawnQueue.clear();
        pendingMeleeAttack = null;
        guardianCharge = null;
        guardianRoar = null;
        guardianFortressRemaining = 0.0;
        guardianBlockFeedbackRemaining = 0.0;
        whenToSpawn = INITIAL_SPAWN_DELAY;
        secretCycleIndex = 0;
        secretCycleTimer = 0.0;
        secretSpawnTimer = 0.0;
        bossSpawned = false;
        bossDefeated = false;
        eliteBossSpawned = false;
        eliteBossSpawnTime = 0.0;
        finalBossSpawned = false;
        finalBossSpawnTime = 0.0;
        finalBossPulseTimer = 0.0;
        finalBossLaserTimer = 0.0;
        finalBossWallTimer = 0.0;
        regionalBossIndex = 0;
        nextRegionalBossSpawnTime = FIRST_BOSS_SPAWN_TIME;
        gameTimer = 0.0;
        repositionTimer = 0.0;
        level = 1;
        exp = 0;
        expToNextLevel = 10;
        upgradeMenuOpen = false;
        damageReductionTimer = 0.0;
        damageReductionMultiplier = 1.0;
        damageBoostTimer = 0.0;
        damageBoostMultiplier = 1.0;
        passiveDamageTakenMultiplier = 1.0;
        passiveAbilityPowerMultiplier = 1.0;
        passiveHealingMultiplier = 1.0;
        screenShakeTime = 0.0;
        screenShakeDuration = 0.0;
        screenShakeStrength = 0.0;
        manaPulseTime = 0.0;
        abilityManager.resetRunState();
        refreshSelectedCharacterLoadout();
        applySelectedPassive();
        legacyAbility.reset();
    }

    private Player createSelectedPlayer() {
        Player selectedPlayer = switch (selectedCharacterIndex) {
            case 0 -> new Character_Eumann();
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            case 4 -> new Character_Sir_Rakki();
            default -> new Character_Eumann();
        };
        selectedPlayer.setWorldCollision(WorldCollision.terrain());
        return selectedPlayer;
    }

    private void syncAutoAttackSprite() {
        if (weapon instanceof AutoFireWeapon autoFireWeapon) {
            autoFireWeapon.setAttackSprite(player.getPrimaryWeaponSprite(),
                    player.getDefaultWeaponStyle());
        }
    }

    public void togglePause() {
        paused = !paused;
        if (!paused) {
            settingsOpen = false;
        }
    }

    public void resume() {
        paused = false;
        settingsOpen = false;
    }

    public void toggleSettings() {
        settingsOpen = !settingsOpen;
    }

    public void toggleSkillMenu() {
        if (gameStarted && !upgradeMenuOpen && !gameOver) {
            skillMenuOpen = !skillMenuOpen;
        }
    }

    public boolean isSkillMenuOpen() {
        return skillMenuOpen;
    }

    public void selectAbilityEquipSlot(int slotIndex) {
        abilityManager.selectEquipSlot(slotIndex);
    }

    public void equipAbility(RpgAbility ability) {
        if (ability == null) {
            return;
        }
        String abilityId = ability.getDefinition().getId();
        if (getCharacterActiveSkillIds(selectedCharacterIndex).contains(abilityId)
                && ability.getDefinition().isUnlockedAt(level)) {
            abilityManager.equip(ability);
        }
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void setDebugInfoVisible(boolean visible) {
        debugInfoVisible = visible;
    }

    public void chooseUpgrade(int index) {
        if (!upgradeMenuOpen) {
            return;
        }
        if (index < 0 || index >= upgradeChoices.size()) {
            return;
        }
        applyUpgrade(upgradeChoices.get(index));
        upgradeChoices.clear();
        upgradeMenuOpen = false;
        openPendingLevelUp();
    }

    public List<String> getUpgradeChoices() {
        return upgradeChoices;
    }

    public List<String> getCharacterNames() {
        return characterNames;
    }

    public List<String> getCharacterWeapons() {
        return characterWeapons;
    }

    public String getCharacterClassName(int index) {
        if (index < 0 || index >= characterClassNames.size()) {
            return "";
        }
        return characterClassNames.get(index);
    }

    public String getCharacterRole(int index) {
        if (index < 0 || index >= characterRoles.size()) {
            return "";
        }
        return characterRoles.get(index);
    }

    public String getCharacterWeaponName(int index) {
        if (index < 0 || index >= characterWeapons.size()) {
            return "";
        }
        return characterWeapons.get(index);
    }

    public List<String> getCharacterWeaponImagePaths(int index) {
        if (index < 0 || index >= characterWeaponImagePaths.size()) {
            return Arrays.asList();
        }
        return characterWeaponImagePaths.get(index);
    }

    public int getCharacterCount() {
        return characterNames.size();
    }

    public int getSelectedCharacterIndex() {
        return selectedCharacterIndex;
    }

    public boolean isCharacterSelectable(int index) {
        return index >= 0 && index < characterNames.size()
                && characterAbilityClasses.get(index) != null;
    }

    public void selectCharacter(int index) {
        if (isCharacterSelectable(index)) {
            selectedCharacterIndex = index;
            player = createSelectedPlayer();
            syncAutoAttackSprite();
            abilityManager.resetRunState();
            refreshSelectedCharacterLoadout();
            applySelectedPassive();
            abilityVisualEffects.clear();
            scheduledAbilityImpacts.clear();
            skillProjectiles.clear();
            skillWaves.clear();
            skillDash = null;
            combatImpactEffects.clear();
            floatingTexts.clear();
            pendingMeleeAttack = null;
            guardianCharge = null;
            guardianRoar = null;
            guardianFortressRemaining = 0.0;
            guardianBlockFeedbackRemaining = 0.0;
        }
    }

    public String getSelectedCharacterName() {
        return characterNames.get(selectedCharacterIndex);
    }

    public String getSelectedWeaponName() {
        return characterWeapons.get(selectedCharacterIndex);
    }

    public String getPortraitPath() {
        return getPortraitPath(selectedCharacterIndex);
    }

    public String getPortraitPath(int index) {
        if (index < 0 || index >= characterPortraitPaths.size()) {
            return "/main/resources/portrait_coming_soon.png";
        }
        return characterPortraitPaths.get(index);
    }

    public AbilityClass getCharacterAbilityClass(int index) {
        if (index < 0 || index >= characterAbilityClasses.size()) {
            return null;
        }
        return characterAbilityClasses.get(index);
    }

    public List<String> getCharacterActiveSkillIds(int index) {
        if (index < 0 || index >= characterActiveSkillIds.size()) {
            return Arrays.asList();
        }
        return characterActiveSkillIds.get(index);
    }

    public List<RpgAbility> getCharacterActiveAbilities(int index) {
        List<RpgAbility> activeAbilities = new ArrayList<>();
        for (String skillId : getCharacterActiveSkillIds(index)) {
            RpgAbility ability = abilityManager.getAbilityById(skillId);
            if (ability != null) {
                activeAbilities.add(ability);
            }
        }
        return activeAbilities;
    }

    public RpgAbility getCharacterPassiveAbility(int index) {
        if (index < 0 || index >= characterPassiveSkillIds.size()) {
            return null;
        }
        return abilityManager.getAbilityById(characterPassiveSkillIds.get(index));
    }

    public String getCharacterPassiveName(int index) {
        if (index < 0 || index >= characterPassiveNames.size()) {
            return "";
        }
        return characterPassiveNames.get(index);
    }

    public double getExpProgress() {
        if (expToNextLevel <= 0) {
            return 0.0;
        }
        return Math.min(1.0, exp / (double) expToNextLevel);
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentExp() {
        return exp;
    }

    public int getExpToNextLevel() {
        return expToNextLevel;
    }

    private int availableSlots() {
        return MAX_ENEMIES - enemies.size();
    }

    private int getSpawnBatchSize() {
        int base = gameTimer < 18.0 ? 1 : gameTimer < 60.0 ? 2 : gameTimer < 150.0 ? 3 : 4;
        return base + (survivalDirector.isAssaultActive() ? 1 : 0);
    }

    private double getSpawnInterval() {
        double interval = gameTimer < 90.0 ? 1.2 : gameTimer < 150.0 ? 0.9 : 0.6;
        if (survivalDirector.isRecovering()) return interval * 1.6;
        return survivalDirector.isAssaultActive() ? interval * 0.85 : interval;
    }

    private Enemy createEnemy(double worldX, double worldY) {
        EnemyDefinition definition = EnemyCatalog.randomNormal(getActiveEnemyRegion(), random);
        return new RegionalEnemy(definition, worldX, worldY);
    }

    private EnemyRegion getActiveEnemyRegion() {
        return EnemyRegion.byIndex(regionalBossIndex);
    }

    public String getActiveRegionName() {
        return getActiveEnemyRegion().getDisplayName();
    }

    private void spawnClump(int enemyCount) {
        int enemiesToSpawn = Math.min(enemyCount, availableSlots());

        if (enemiesToSpawn <= 0) {
            return;
        }

        // Place the enemies evenly around a ring centered on the player.
        for (int index = 0; index < enemiesToSpawn; index++) {
            double angle = Math.PI * 2.0 * index / enemiesToSpawn;
            spawnAtAngle(angle);
        }
    }

    private void spawnContinuous(int enemyCount) {
        for (int index = 0; index < enemyCount; index++) {
            spawnQueue.add(whenToSpawn + 0.5 * index);
        }
    }

    private void spawnFixed(int enemyCount) {
        int enemiesToSpawn = Math.min(enemyCount, availableSlots());

        if (enemiesToSpawn <= 0) {
            return;
        }

        // Rotate each batch; a one-enemy batch must not always come from the right.
        double rotation = random.nextDouble() * Math.PI * 2.0;
        for (int index = 0; index < enemiesToSpawn; index++) {
            spawnAtAngle(rotation + Math.PI * 2.0 * index / enemiesToSpawn);
        }
    }

    private void spawnAssault() {
        assaultDirection = random.nextDouble() * Math.PI * 2.0;
        int count = Math.min(12, 5 + survivalDirector.getAssaultNumber());
        for (int index = 0; index < count; index++) {
            double angle = switch (survivalDirector.getAssault()) {
                case PINCER -> assaultDirection + (index % 2) * Math.PI + (random.nextDouble() - 0.5) * 0.5;
                case RUSH -> assaultDirection + (random.nextDouble() - 0.5) * 1.0;
                case ENCIRCLEMENT -> assaultDirection + Math.PI * 2.0 * index / count;
            };
            spawnAtAngle(angle);
        }
        addFloatingText(survivalDirector.getAssault().title().toUpperCase(),
                player.getWorldX(), player.getWorldY() - 88, new Color(255, 175, 112));
    }

    /** Ray intersection with the actual logical viewport keeps every spawn off screen. */
    private double spawnDistance(double angle) {
        double margin = 90.0;
        double xDistance = (viewportWidth * 0.5 + margin) / Math.max(0.00001, Math.abs(Math.cos(angle)));
        double yDistance = (viewportHeight * 0.5 + margin) / Math.max(0.00001, Math.abs(Math.sin(angle)));
        return Math.min(xDistance, yDistance);
    }

    private void spawnAtAngle(double angle) {
        if (availableSlots() <= 0) return;
        double distance = spawnDistance(angle) + random.nextDouble() * 35.0;
        enemies.add(createEnemy(player.getWorldX() + Math.cos(angle) * distance,
                player.getWorldY() + Math.sin(angle) * distance));
    }

    private void spawnOne() {
        if (availableSlots() <= 0) {
            return;
        }

        // Choose a fresh random angle for every individual enemy.
        double angle = random.nextDouble() * Math.PI * 2.0;
        spawnAtAngle(angle);
    }

    private void addPendingEnemies(List<Enemy> pendingEnemies) {
        if (pendingEnemies.isEmpty()) {
            return;
        }
        int slots = availableSlots();
        for (int index = 0; index < pendingEnemies.size() && index < slots; index++) {
            enemies.add(pendingEnemies.get(index));
        }
    }

    private <T> void trimListToLimit(List<T> values, int limit) {
        while (values.size() > limit) {
            T removed = values.removeFirst();
            if (removed instanceof Projectile projectile) releaseProjectileOwner(projectile);
        }
    }

    private void releaseProjectileOwner(Projectile projectile) {
        if (projectile.getOwner() instanceof RegionalEnemy regional) regional.onProjectileDestroyed();
        else if (projectile.getOwner() instanceof TemplateEnemy2 ranged) ranged.onProjectileDestroyed();
    }

    private void repositionFarEnemies() {
        double playerX = player.getWorldX();
        double playerY = player.getWorldY();
        double tooFarDistanceSquared = TOO_FAR_DISTANCE * TOO_FAR_DISTANCE;

        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.distanceSquaredTo(playerX, playerY) > tooFarDistanceSquared) {
                teleportEnemyNearPlayer(enemy, playerX, playerY);
            }
        }
    }

    private void teleportEnemyNearPlayer(Enemy enemy, double playerX, double playerY) {
        // Place the enemy in front of the direction the player is facing.
        double facingX = player.getFacingX();
        double facingY = player.getFacingY();
        double perpendicularX = -facingY;
        double perpendicularY = facingX;
        double sidewaysOffset = (random.nextDouble() * 2.0 - 1.0) * ENEMY_CLUMP_RADIUS;

        double distance = spawnDistance(Math.atan2(facingY, facingX)) + ENEMY_CLUMP_RADIUS;
        double enemyX = playerX + facingX * distance
            + perpendicularX * sidewaysOffset;
        double enemyY = playerY + facingY * distance
            + perpendicularY * sidewaysOffset;
        enemy.teleportTo(enemyX, enemyY);
    }

    public double getWorldOffsetX() {
        return player.getWorldOffsetX();
    }

    public double getWorldOffsetY() {
        return player.getWorldOffsetY();
    }

    public double getPlayerWorldX() {
        return player.getWorldX();
    }

    public double getPlayerWorldY() {
        return player.getWorldY();
    }

    public double getGameTimer() {
        return gameTimer;
    }

    public int getEnemyCount() {
        int living = 0;
        for (Enemy enemy : enemies) if (!enemy.isDead()) living++;
        return living;
    }

    public void drawEntities(Graphics2D graphics, int centerX, int centerY) {
        // Draw living enemies first, then other objects, and finally dead enemies on top.
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                enemy.draw(graphics, centerX, centerY,
                        getWorldOffsetX(), getWorldOffsetY());
            }
        }
        for (Gem gem : gems) {
            gem.draw(graphics, centerX, centerY,
                    getWorldOffsetX(), getWorldOffsetY());
        }
        for (SecretJpg secretJpg : secretJpgs) {
            secretJpg.draw(graphics, centerX, centerY,
                    getWorldOffsetX(), getWorldOffsetY());
        }
        graphics.setColor(java.awt.Color.WHITE);
        for (Projectile projectile : projectiles) {
            projectile.draw(graphics, centerX, centerY,
                getWorldOffsetX(), getWorldOffsetY());
        }
        for (Projectile projectile : enemyProjectiles) {
            projectile.draw(graphics, centerX, centerY,
                getWorldOffsetX(), getWorldOffsetY());
        }
        if (runEntrance.isPlaying()) {
            player.drawEntrance(graphics, centerX, centerY,
                    runEntrance.getHeight(), runEntrance.getCompression());
        } else {
            drawPassiveAura(graphics, centerX, centerY);
            player.draw(graphics, centerX, centerY);
        }

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                enemy.draw(graphics, centerX, centerY,
                        getWorldOffsetX(), getWorldOffsetY());
            }
        }
    }

    public void applyAbilityEffect(AbilityDefinition definition) {
        if (definition == null || definition.isPassive() || runEntrance.isPlaying()) return;
        Color color = colorFor(definition);
        double damage = definition.getDamage() * damageBoostMultiplier
                * passiveAbilityPowerMultiplier;
        if (player instanceof Character_Eumann && player.getHealth() < player.getMaxHealth() * 0.4) damage *= 1.25;
        double radius = definition.getRadius();
        DamageElement element = elementFor(definition);
        double aimRange = AbilityAnimationTiming.isReferenceSkill(definition.getId())
                ? Math.max(radius, definition.getAbilityClass() == AbilityClass.ASSASSIN ? 240 : 300)
                : Math.max(radius, 420.0);
        Enemy target = findNearestLivingEnemyInRange(aimRange);
        if (target != null) {
            player.faceToward(target.getWorldX(), target.getWorldY());
        } else if (definition.getAbilityClass() == AbilityClass.GUARDIAN) {
            player.faceToward(player.getWorldX() + player.getRecentMoveX(),
                    player.getWorldY() + player.getRecentMoveY());
        }
        double animationDuration = animationDurationFor(definition);
        // A replaced automatic swing must not land invisibly during the skill's windup.
        pendingMeleeAttack = null;
        player.playAttackAnimation(definition, animationDuration);
        double originX = abilityOriginX(definition);
        double originY = abilityOriginY(definition);
        double fallbackDistance = fallbackEffectDistance(definition);
        double effectX = target == null ? originX + player.getRecentMoveX() * fallbackDistance
                : target.getWorldX();
        double effectY = target == null ? originY + player.getRecentMoveY() * fallbackDistance
                : target.getWorldY();
        double visualRadius = Math.max(90.0, radius);
        triggerCastFeedback(definition);

        if (definition.getAbilityClass() == AbilityClass.GUARDIAN) {
            applyGuardianAbility(definition, damage, animationDuration);
            return;
        }
        if (AbilityAnimationTiming.isReferenceSkill(definition.getId())) {
            beginReferenceSkill(definition, target, damage, element);
            return;
        }

        switch (definition.getEffectType()) {
            case SINGLE_TARGET -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        90, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, 90, damage, element);
            }
            case AREA_DAMAGE -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case POISON -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case SLOW -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case STUN -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case DASH -> {
                double beforeX = player.getWorldX();
                double beforeY = player.getWorldY();
                dashPlayer(definition.getDuration());
                addAbilityVisual(definition, beforeX, beforeY, player.getWorldX(),
                        player.getWorldY(), visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, beforeX, beforeY,
                        player.getWorldX(), player.getWorldY(), visualRadius, damage, element);
            }
            case HEAL -> {
                addAbilityVisual(definition, originX, originY, originX, originY,
                        Math.max(90, radius), color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        originX, originY, Math.max(90, radius), damage, element);
            }
            case SHIELD -> {
                addAbilityVisual(definition, originX, originY, originX, originY, 150,
                        color, Math.max(animationDuration, definition.getDuration()));
                scheduleAbilityImpact(definition, target, originX, originY,
                        originX, originY, 150, damage, element);
            }
            case BUFF -> {
                addAbilityVisual(definition, originX, originY, originX, originY,
                        130, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        originX, originY, 130, damage, element);
            }
            case MARK -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case EXECUTE -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case SUMMON -> {
                addAbilityVisual(definition, originX, originY, effectX, effectY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        effectX, effectY, visualRadius, damage, element);
            }
            case ULTIMATE -> {
                double centerX = "elemental_storm".equals(definition.getId()) ? effectX : originX;
                double centerY = "elemental_storm".equals(definition.getId()) ? effectY : originY;
                addAbilityVisual(definition, originX, originY, centerX, centerY,
                        visualRadius, color, animationDuration);
                scheduleAbilityImpact(definition, target, originX, originY,
                        centerX, centerY, visualRadius, damage, element);
            }
            default -> {
            }
        }
    }

    public boolean isGuardianActionLocked() {
        return player instanceof Character_Sir_Rakki && player.isSkillAnimationActive();
    }

    public boolean isSkillActionLocked() {
        return runEntrance.isPlaying() || player.isSkillAnimationActive();
    }

    private void beginReferenceSkill(AbilityDefinition d, Enemy target, double damage, DamageElement element) {
        String id = d.getId();
        double x = player.getSkillSourceWorldX(), y = player.getSkillSourceWorldY();
        double dx = player.getRecentMoveX(), dy = player.getRecentMoveY();
        double length = Math.max(0.001, Math.hypot(dx, dy));
        dx /= length; dy /= length;
        double tx = target == null ? x + dx * 180 : target.getWorldX();
        double ty = target == null ? y + dy * 180 : target.getWorldY();
        double life = AbilityAnimationTiming.duration(d);
        if (id.equals("holy_shield") || id.equals("death_mark")) life += d.getDuration();
        if (id.equals("elemental_storm")) { tx = player.getWorldX(); ty = player.getWorldY(); life += 0.30; }
        AbilityVisualEffect visual = addAbilityVisual(d, x, y, tx, ty, Math.max(70, d.getRadius()), colorFor(d), life);
        if (target != null && (id.equals("death_mark") || id.equals("holy_bolt")
                || id.equals("divine_light") || id.equals("lightning_strike"))) visual.attachTarget(target);
        ScheduledAbilityImpact impact = new ScheduledAbilityImpact(d, target, x, y, tx, ty,
                d.getRadius(), damage, element, hitTimesFor(d));
        impact.visual = visual;
        impact.previousPlayerX = player.getWorldX();
        impact.previousPlayerY = player.getWorldY();
        scheduledAbilityImpacts.add(impact);
        if (id.equals("shadow_strike") || id.equals("shadow_step")) {
            double distance = id.equals("shadow_step") ? 160 : target == null ? 115
                    : Math.max(0, Math.min(160, Math.hypot(tx - player.getWorldX(), ty - player.getWorldY()) - 38));
            skillDash = new SkillDash(dx, dy, distance, d);
        }
    }

    private void fireReferenceImpact(ScheduledAbilityImpact impact, int index) {
        AbilityDefinition d = impact.definition;
        String id = d.getId();
        double damage = impact.damage / impact.hitCount();
        double overdue = Math.max(0, impact.elapsed - impact.hitTimes[index]);
        double fraction = impact.lastStep <= 0 ? 1 : Math.max(0, Math.min(1, 1 - overdue / impact.lastStep));
        double x = impact.previousPlayerX + (player.getWorldX() - impact.previousPlayerX) * fraction;
        double y = impact.previousPlayerY + (player.getWorldY() - impact.previousPlayerY) * fraction;
        java.awt.geom.Point2D.Double source = player.sampleSkillSourceAt(impact.hitTimes[index], overdue);
        double sourceX = source.x + x - player.getWorldX(), sourceY = source.y + y - player.getWorldY();
        switch (id) {
            case "flame_burst", "ice_shard" -> {
                double tx = impact.target != null && !impact.target.isDead() ? impact.target.getWorldX() : impact.effectX;
                double ty = impact.target != null && !impact.target.isDead() ? impact.target.getWorldY() : impact.effectY;
                SkillProjectile projectile = new SkillProjectile(d, sourceX, sourceY, tx, ty, damage, impact.element, overdue);
                skillProjectiles.add(projectile);
                abilityVisualEffects.add(projectile.visual);
            }
            case "elemental_storm" -> {
                impact.visual.freezeImpactOrigin(x, y);
                skillWaves.add(new SkillWave(d, x, y, impact.radius, damage, impact.element, 0, 0, overdue, impact.visual));
            }
            case "heal" -> {
                double healing = impact.damage * passiveHealingMultiplier;
                player.heal(healing);
                addFloatingText("+" + (int) Math.round(healing), x, y - 42, new Color(120, 255, 150));
                abilityManager.restoreMana(healing * 0.25);
                impact.visual.freezeImpactOrigin(x, y);
            }
            case "holy_shield" -> {
                applyDamageReduction(0.35, d.getDuration());
                impact.visual.freezeImpactOrigin(x, y);
            }
            case "death_mark" -> {
                if (impact.target != null && !impact.target.isDead()
                        && impact.target.distanceSquaredTo(x, y) <= d.getRadius() * d.getRadius()) {
                    damageSkillEnemy(impact.target, damage, impact.element, sourceX, sourceY, id);
                    if (!impact.target.isDead()) impact.target.applyMark(1.45, d.getDuration());
                } else impact.visual.expire();
            }
            case "holy_bolt", "divine_light", "lightning_strike" -> {
                double tx = impact.target != null && !impact.target.isDead() ? impact.target.getWorldX() : impact.effectX;
                double ty = impact.target != null && !impact.target.isDead() ? impact.target.getWorldY() : impact.effectY;
                impact.visual.freezeImpactOrigin(tx, ty);
                List<Enemy> hits = id.equals("holy_bolt") ? impact.target == null ? List.of() : List.of(impact.target)
                        : enemiesInRadius(tx, ty, d.getRadius());
                for (Enemy enemy : hits) {
                    damageSkillEnemy(enemy, damage, impact.element, sourceX, sourceY, id);
                    if (!id.equals("holy_bolt")) enemy.applyStun(d.getDuration());
                }
            }
            default -> {
                double aimX = impact.effectX - impact.originX, aimY = impact.effectY - impact.originY;
                double length = Math.max(0.001, Math.hypot(aimX, aimY));
                aimX /= length; aimY /= length;
                boolean spinning = id.equals("knights_wrath") || id.equals("twin_fang");
                // A target selected for aiming never becomes a remote melee damage center.
                double reach = switch (id) {
                    case "shield_bash" -> 62;
                    case "shadow_strike", "shadow_step" -> 76;
                    default -> Math.min(d.getRadius(), 105);
                };
                for (Enemy enemy : enemiesInRadius(x, y, reach)) {
                    double ex = enemy.getWorldX() - x, ey = enemy.getWorldY() - y;
                    if (!spinning && ex * aimX + ey * aimY < -enemy.getCollisionRadius()) continue;
                    if (id.equals("shadow_strike") && enemy != impact.target) continue;
                    damageSkillEnemy(enemy, damage, impact.element, x - aimX * 10, y - aimY * 10, id);
                    if (id.equals("shield_bash") || id.equals("earth_shatter")) enemy.applyStun(d.getDuration());
                    enemy.applyDirectionalKnockback(aimX, aimY, id.equals("shield_bash") ? 42 : id.equals("earth_shatter") ? 28 : 10);
                }
                impact.visual.freezeImpactOrigin(sourceX, sourceY);
            }
        }
    }

    private void updateSkillDash(double dt) {
        if (skillDash == null) return;
        SkillDash dash = skillDash;
        double previous = smoothStep((dash.elapsed - dash.start) / dash.travelDuration);
        dash.elapsed += dt;
        double current = smoothStep((dash.elapsed - dash.start) / dash.travelDuration);
        player.moveWorld(dash.dx * dash.distance * (current - previous), dash.dy * dash.distance * (current - previous));
        if (dash.elapsed >= dash.start + dash.travelDuration) skillDash = null;
    }

    private void updateSkillProjectiles(double dt) {
        Iterator<SkillProjectile> iterator = skillProjectiles.iterator();
        while (iterator.hasNext()) {
            SkillProjectile shot = iterator.next();
            double step = shot.firstStep >= 0 ? shot.firstStep : dt;
            shot.firstStep = -1;
            double oldX = shot.x, oldY = shot.y, oldAge = shot.age;
            shot.age = Math.min(1.8, shot.age + step);
            // Integral of an accelerating speed curve, independent of frame rate.
            double distance = projectileDistance(shot.age, shot.speed) - projectileDistance(oldAge, shot.speed);
            shot.x += shot.dx * distance; shot.y += shot.dy * distance;
            Enemy nearest = null;
            double nearestFraction = Double.POSITIVE_INFINITY;
            int count = enemies.size();
            for (int i = 0; i < count; i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.isDead()) continue;
                double fraction = segmentCircleContact(oldX, oldY, shot.x, shot.y,
                        enemy.getWorldX(), enemy.getWorldY(), enemy.getCollisionRadius() + 9);
                if (fraction < nearestFraction) { nearest = enemy; nearestFraction = fraction; }
            }
            if (nearest != null) {
                shot.x = oldX + (shot.x - oldX) * nearestFraction;
                shot.y = oldY + (shot.y - oldY) * nearestFraction;
                if (shot.definition.getId().equals("flame_burst")) {
                    damageSkillEnemy(nearest, shot.damage, shot.element, oldX, oldY, shot.definition.getId());
                    for (Enemy enemy : enemiesInRadius(shot.x, shot.y, 48)) {
                        if (enemy == nearest) continue;
                        damageSkillEnemy(enemy, shot.damage, shot.element, oldX, oldY, shot.definition.getId());
                    }
                } else {
                    damageSkillEnemy(nearest, shot.damage, shot.element, oldX, oldY, shot.definition.getId());
                    nearest.applySlow(0.45, shot.definition.getDuration());
                }
                shot.visual.expire();
                iterator.remove();
            } else if (shot.age >= 1.8) {
                shot.visual.expire(); iterator.remove();
            } else shot.visual.setProjectilePosition(shot.x, shot.y, Math.atan2(shot.dy, shot.dx), shot.age);
        }
    }

    private static double projectileDistance(double age, double speed) {
        return speed * (age - 0.07 * (1 - Math.exp(-age / 0.14)));
    }

    private static double segmentCircleContact(double x1, double y1, double x2, double y2, double cx, double cy, double radius) {
        double dx = x2 - x1, dy = y2 - y1, ox = x1 - cx, oy = y1 - cy;
        double c = ox * ox + oy * oy - radius * radius;
        if (c <= 0) return 0;
        double a = dx * dx + dy * dy, b = 2 * (ox * dx + oy * dy);
        if (a < 1e-10) return Double.POSITIVE_INFINITY;
        double disc = b * b - 4 * a * c;
        if (disc < 0) return Double.POSITIVE_INFINITY;
        double t = (-b - Math.sqrt(disc)) / (2 * a);
        return t >= 0 && t <= 1 ? t : Double.POSITIVE_INFINITY;
    }

    private void updateSkillWaves(double dt) {
        Iterator<SkillWave> iterator = skillWaves.iterator();
        while (iterator.hasNext()) {
            SkillWave wave = iterator.next();
            double step = wave.firstStep >= 0 ? wave.firstStep : dt;
            wave.firstStep = -1;
            wave.age = Math.min(0.60, wave.age + step);
            double radius = wave.radius * smoothStep(wave.age / 0.60);
            wave.visual.setWaveRadius(radius);
            for (Enemy enemy : enemiesInRadius(wave.x, wave.y, radius)) {
                if (wave.hit.contains(enemy)) continue;
                double dx = enemy.getWorldX() - wave.x, dy = enemy.getWorldY() - wave.y;
                if (wave.definition.getId().equals("earthbreaker") && Math.hypot(dx, dy) > 42
                        && dx * wave.dx + dy * wave.dy < -0.2 * Math.hypot(dx, dy)) continue;
                wave.hit.add(enemy);
                damageSkillEnemy(enemy, wave.damage, wave.element, wave.x, wave.y, wave.definition.getId());
                if (wave.definition.getId().equals("earthbreaker")) {
                    enemy.applyKnockdown(wave.definition.getDuration());
                    enemy.applyDirectionalKnockback(dx + wave.dx * 20, dy + wave.dy * 20, 48);
                }
            }
            if (wave.age >= 0.60) iterator.remove();
        }
    }

    private static final class SkillDash {
        final double dx, dy, distance, start, travelDuration;
        double elapsed;
        SkillDash(double dx, double dy, double distance, AbilityDefinition d) {
            this.dx = dx; this.dy = dy; this.distance = distance;
            start = AbilityAnimationTiming.duration(d) * AbilityAnimationTiming.releaseProgress(d);
            travelDuration = AbilityAnimationTiming.duration(d) * 0.30;
        }
    }

    private static final class SkillProjectile {
        final AbilityDefinition definition;
        final double dx, dy, speed, damage;
        final DamageElement element;
        final AbilityVisualEffect visual;
        double x, y, age, firstStep;
        SkillProjectile(AbilityDefinition d, double x, double y, double tx, double ty,
                double damage, DamageElement element, double overdue) {
            definition = d; this.x = x; this.y = y; this.damage = damage; this.element = element; firstStep = overdue;
            double length = Math.max(0.001, Math.hypot(tx - x, ty - y));
            dx = (tx - x) / length; dy = (ty - y) / length;
            speed = d.getId().equals("ice_shard") ? 440 : 360;
            visual = new AbilityVisualEffect(d, x, y, tx, ty, d.getId().equals("ice_shard") ? 72 : 58, Color.WHITE, 1.8);
            visual.setProjectilePosition(x, y, Math.atan2(dy, dx));
        }
    }

    private static final class SkillWave {
        final AbilityDefinition definition;
        final double x, y, radius, damage, dx, dy;
        final DamageElement element;
        final AbilityVisualEffect visual;
        final Set<Enemy> hit = new HashSet<>();
        double age, firstStep;
        SkillWave(AbilityDefinition d, double x, double y, double radius, double damage,
                DamageElement element, double dx, double dy, double overdue, AbilityVisualEffect visual) {
            definition = d; this.x = x; this.y = y; this.radius = radius; this.damage = damage;
            this.element = element; this.dx = dx; this.dy = dy; firstStep = overdue; this.visual = visual;
        }
    }

    private void applyGuardianAbility(AbilityDefinition definition, double damage, double animationDuration) {
        if (definition.isPassive()) return;
        double directionX = player.getRecentMoveX();
        double directionY = player.getRecentMoveY();
        double length = Math.hypot(directionX, directionY);
        if (length < 0.0001) { directionX = player.getHeldWeaponSideX(); directionY = 0; length = 1; }
        directionX /= length;
        directionY /= length;
        double originX = player.getWeaponCastWorldX();
        double originY = player.getWeaponCastWorldY();
        double radius = Math.max(90.0, definition.getRadius());
        double life = switch (definition.getId()) {
            case "shield_fortress" -> hitTimesFor(definition)[0] + definition.getDuration();
            case "earthbreaker" -> animationDuration + 0.65;
            case "guardians_roar" -> animationDuration + 0.35;
            default -> animationDuration;
        };
        AbilityVisualEffect visual = addAbilityVisual(definition, originX, originY,
                originX + directionX * radius, originY + directionY * radius,
                radius, colorFor(definition), life);
        ScheduledAbilityImpact impact = new ScheduledAbilityImpact(definition, null,
                originX, originY, originX + directionX * radius, originY + directionY * radius,
                radius, damage, DamageElement.PHYSICAL, hitTimesFor(definition));
        impact.visual = visual;
        scheduledAbilityImpacts.add(impact);
    }

    private void fireGuardianImpact(ScheduledAbilityImpact impact) {
        AbilityDefinition definition = impact.definition;
        double overdue = Math.max(0.0, impact.elapsed - impact.hitTimes[0]);
        double directionX = impact.effectX - impact.originX;
        double directionY = impact.effectY - impact.originY;
        double length = Math.max(0.0001, Math.hypot(directionX, directionY));
        directionX /= length;
        directionY /= length;
        switch (definition.getId()) {
            case "shield_fortress" -> {
                guardianFortressRemaining = Math.max(0.0, definition.getDuration() - overdue);
                player.setGuardianFortressRemaining(guardianFortressRemaining);
            }
            case "iron_charge" -> guardianCharge = new GuardianCharge(directionX, directionY,
                    definition.getDuration(), impact.damage, overdue);
            case "earthbreaker" -> {
                double originX = player.getWeaponCastWorldX();
                double originY = player.getWeaponCastWorldY();
                impact.visual.freezeImpactOrigin(originX, originY);
                skillWaves.add(new SkillWave(definition, originX, originY, definition.getRadius(),
                        impact.damage, DamageElement.PHYSICAL, directionX, directionY, overdue, impact.visual));
                addScreenShake(0.22, 4.5);
            }
            case "guardians_roar" -> {
                double originX = player.getWorldX();
                double originY = player.getWorldY();
                impact.visual.freezeImpactOrigin(originX, originY);
                guardianRoar = new GuardianRoar(originX, originY, definition.getRadius(),
                        definition.getDuration(), overdue);
                addScreenShake(0.18, 2.5);
            }
            default -> { }
        }
    }

    private void updateGuardianActions(double deltaTime) {
        if (guardianCharge != null) {
            GuardianCharge charge = guardianCharge;
            double step = charge.firstFrameTime >= 0.0 ? charge.firstFrameTime : deltaTime;
            charge.firstFrameTime = -1.0;
            double previous = charge.elapsed / charge.duration;
            charge.elapsed = Math.min(charge.duration, charge.elapsed + step);
            double progress = charge.elapsed / charge.duration;
            double distance = 160.0 * (smoothStep(progress) - smoothStep(previous));
            double beforeX = player.getWeaponCastWorldX();
            double beforeY = player.getWeaponCastWorldY();
            player.applyGuardianChargeMovement(charge.directionX * distance, charge.directionY * distance, step);
            double afterX = player.getWeaponCastWorldX();
            double afterY = player.getWeaponCastWorldY();
            // Swept shield collision prevents tunneling; each enemy is struck once per charge.
            int enemyCount = enemies.size();
            for (int enemyIndex = 0; enemyIndex < enemyCount; enemyIndex++) {
                Enemy enemy = enemies.get(enemyIndex);
                if (enemy.isDead() || charge.hitEnemies.contains(enemy)) continue;
                double hitRadius = enemy.getCollisionRadius() + 24.0;
                if (distanceToSegmentSquared(enemy.getWorldX(), enemy.getWorldY(), beforeX, beforeY, afterX, afterY)
                        <= hitRadius * hitRadius) {
                    charge.hitEnemies.add(enemy);
                    damageSkillEnemy(enemy, charge.damage, DamageElement.PHYSICAL, beforeX - charge.directionX * 20,
                            beforeY - charge.directionY * 20, "iron_charge");
                    enemy.applyStun(0.55);
                    enemy.applyDirectionalKnockback(charge.directionX, charge.directionY, 56.0);
                }
            }
            if (charge.elapsed >= charge.duration) guardianCharge = null;
        }
        if (guardianRoar != null) {
            GuardianRoar roar = guardianRoar;
            double step = roar.firstFrameTime >= 0.0 ? roar.firstFrameTime : deltaTime;
            roar.firstFrameTime = -1.0;
            roar.elapsed = Math.min(0.55, roar.elapsed + step);
            double waveRadius = roar.radius * roar.elapsed / 0.55;
            for (Enemy enemy : enemiesInRadius(roar.originX, roar.originY, waveRadius)) {
                if (roar.hitEnemies.add(enemy)) {
                    enemy.applyTaunt(roar.duration, 0.65);
                    enemy.applyStun(0.4);
                    enemy.applyDirectionalKnockback(enemy.getWorldX() - roar.originX,
                            enemy.getWorldY() - roar.originY, 12.0);
                }
            }
            if (roar.elapsed >= 0.55) guardianRoar = null;
        }
    }

    private static double smoothStep(double progress) {
        double value = Math.max(0.0, Math.min(1.0, progress));
        return value * value * (3.0 - 2.0 * value);
    }

    private static double distanceToSegmentSquared(double x, double y, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double squaredLength = dx * dx + dy * dy;
        double projection = squaredLength <= 0.0001 ? 0.0
                : Math.max(0.0, Math.min(1.0, ((x - x1) * dx + (y - y1) * dy) / squaredLength));
        double offsetX = x - x1 - projection * dx;
        double offsetY = y - y1 - projection * dy;
        return offsetX * offsetX + offsetY * offsetY;
    }

    private static final class GuardianCharge {
        private final double directionX, directionY, duration, damage;
        private final Set<Enemy> hitEnemies = new HashSet<>();
        private double elapsed, firstFrameTime;
        private GuardianCharge(double directionX, double directionY, double duration, double damage, double overdue) {
            this.directionX = directionX; this.directionY = directionY;
            this.duration = Math.max(0.05, duration); this.damage = damage;
            this.firstFrameTime = overdue;
        }
    }

    private static final class GuardianRoar {
        private final double originX, originY, radius, duration;
        private final Set<Enemy> hitEnemies = new HashSet<>();
        private double elapsed, firstFrameTime;
        private GuardianRoar(double originX, double originY, double radius, double duration, double overdue) {
            this.originX = originX; this.originY = originY; this.radius = radius; this.duration = duration;
            this.firstFrameTime = overdue;
        }
    }

    private void applyIncomingDamage(double damage, Enemy source, double hitX, double hitY) {
        if (damage <= 0.0) return;
        double multiplier = damageReductionMultiplier * passiveDamageTakenMultiplier;
        if (damageReductionTimer > 0) {
            for (AbilityVisualEffect effect : abilityVisualEffects) effect.flashBarrier(hitX, hitY);
        }
        if (source != null) multiplier *= source.getAttackDamageMultiplier();
        if (player instanceof Character_Sir_Rakki) {
            if (player.getHealth() < player.getMaxHealth() * 0.4) multiplier *= 0.70;
            if (guardianFortressRemaining > 0.0) {
                multiplier *= 0.20;
                if (guardianBlockFeedbackRemaining <= 0.0) {
                    guardianBlockFeedbackRemaining = 0.12;
                    player.playGuardianBlock();
                    for (AbilityVisualEffect effect : abilityVisualEffects) {
                        effect.flashBarrier(hitX, hitY);
                    }
                }
            }
        }
        player.takeDamage(damage * multiplier);
    }

    private void drawPassiveAura(Graphics2D graphics, int centerX, int centerY) {
        String id = characterPassiveSkillIds.get(selectedCharacterIndex);
        SkillEffectAtlas.SkillAnimation animation = SkillEffectAtlas.getAnimation(id, "passive");
        if (animation == null) return;
        boolean lowHealth = player.getHealth() < player.getMaxHealth() * 0.4;
        double intensity = (id.equals("unbreakable") || id.equals("iron_guard")) && lowHealth ? 0.40 : 0.18;
        intensity *= 0.88 + 0.12 * Math.sin(gameTimer * 2.5);
        animation.drawAt(graphics, centerX, centerY + 12, 62, 0, gameTimer, intensity, false);
    }

    private void scheduleAbilityImpact(AbilityDefinition definition, Enemy target,
            double originX, double originY, double effectX, double effectY,
            double radius, double damage, DamageElement element) {
        scheduledAbilityImpacts.add(new ScheduledAbilityImpact(definition, target,
                originX, originY, effectX, effectY, radius, damage, element,
                hitTimesFor(definition)));
    }

    private void fireAbilityImpact(ScheduledAbilityImpact impact, int hitIndex) {
        AbilityDefinition definition = impact.definition;
        if (definition.getAbilityClass() == AbilityClass.GUARDIAN) {
            fireGuardianImpact(impact);
            return;
        }
        if (AbilityAnimationTiming.isReferenceSkill(definition.getId())) {
            fireReferenceImpact(impact, hitIndex);
            return;
        }
        double targetX = impact.target != null && !impact.target.isDead()
                ? impact.target.getWorldX() : impact.effectX;
        double targetY = impact.target != null && !impact.target.isDead()
                ? impact.target.getWorldY() : impact.effectY;
        double hitDamage = impact.damage / impact.hitCount();

        switch (definition.getEffectType()) {
            case SINGLE_TARGET -> {
                if (impact.target != null && !impact.target.isDead()) {
                    damageEnemy(impact.target, hitDamage, impact.element, impact.originX, impact.originY);
                }
            }
            case AREA_DAMAGE -> damageEnemiesInRadius(targetX, targetY,
                    impact.radius, hitDamage, true, impact.element);
            case POISON -> {
                for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                    damageEnemy(enemy, hitDamage, impact.element, impact.originX, impact.originY);
                    if (hitIndex == 0) {
                        enemy.applyPoison(Math.max(1.0, impact.damage * 0.25),
                                Math.max(3.0, definition.getDuration()));
                    }
                }
            }
            case SLOW -> {
                for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                    damageEnemy(enemy, hitDamage, impact.element, impact.originX, impact.originY);
                    if (hitIndex == 0) {
                        enemy.applySlow(0.45, Math.max(2.5, definition.getDuration()));
                    }
                }
                if (hitIndex == 0) {
                    applyDamageReduction(0.75,
                            Math.min(4.0, Math.max(1.0, definition.getDuration())));
                }
            }
            case STUN -> {
                for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                    damageEnemy(enemy, hitDamage, impact.element, impact.originX, impact.originY);
                    if (hitIndex == 0) {
                        enemy.applyStun(Math.max(0.8, definition.getDuration()));
                    }
                }
            }
            case DASH -> damageEnemiesInRadius(targetX, targetY,
                    impact.radius, hitDamage, true, impact.element);
            case HEAL -> {
                if (hitIndex == 0) {
                    double healing = impact.damage * passiveHealingMultiplier;
                    player.heal(healing);
                    addFloatingText("+" + (int) Math.round(healing),
                            impact.originX, impact.originY - 42, new Color(120, 255, 150));
                    abilityManager.restoreMana(healing * 0.25);
                }
                if (definition.getRadius() > 0.0) {
                    damageEnemiesInRadius(impact.originX, impact.originY,
                            definition.getRadius(), hitDamage * 0.65, false, impact.element);
                }
            }
            case SHIELD -> {
                if (hitIndex == 0) {
                    applyDamageReduction(0.35, Math.max(3.0, definition.getDuration()));
                    if (impact.damage > 0.0) {
                        player.heal(impact.damage);
                        addFloatingText("+" + (int) Math.round(impact.damage),
                                impact.originX, impact.originY - 42, new Color(120, 255, 150));
                    }
                }
            }
            case BUFF -> {
                if (hitIndex == 0) {
                    applyDamageBoost(1.45, Math.max(4.0, definition.getDuration()));
                    if (definition.getAbilityClass() == AbilityClass.PRIEST) {
                        abilityManager.boostManaRegen(Math.max(4.0, definition.getDuration()), 1.8);
                    }
                    if (definition.getAbilityClass() == AbilityClass.WARLOCK) {
                        player.takeDamage(5.0);
                        addFloatingText("-5", impact.originX, impact.originY - 42,
                                new Color(220, 60, 90));
                    }
                }
            }
            case MARK -> {
                for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                    damageEnemy(enemy, hitDamage, impact.element, impact.originX, impact.originY);
                    if (hitIndex == 0) {
                        enemy.applyMark(1.45, Math.max(4.0, definition.getDuration()));
                    }
                }
            }
            case EXECUTE -> {
                for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                    double executeDamage = enemy.getHealthRatio() <= 0.35 ? 9999.0 : hitDamage;
                    damageEnemy(enemy, executeDamage, impact.element, impact.originX, impact.originY);
                }
            }
            case SUMMON -> {
                damageEnemiesInRadius(targetX, targetY, impact.radius, hitDamage,
                        true, impact.element);
                if (hitIndex == 0) {
                    for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                        enemy.applyMark(1.25, Math.max(3.0, definition.getDuration()));
                    }
                }
            }
            case ULTIMATE -> {
                damageEnemiesInRadius(targetX, targetY, impact.radius, hitDamage,
                        true, impact.element);
                if (hitIndex == 0) {
                    for (Enemy enemy : enemiesInRadius(targetX, targetY, impact.radius)) {
                        enemy.applySlow(0.35, Math.max(3.0, definition.getDuration()));
                        enemy.applyPoison(Math.max(1.0, impact.damage * 0.12),
                                Math.max(2.0, definition.getDuration()));
                    }
                    if (definition.getAbilityClass() == AbilityClass.BLACK_KNIGHT) {
                        applyDamageReduction(0.25, Math.max(4.0, definition.getDuration()));
                    }
                }
            }
            default -> {
            }
        }
    }

    private double animationDurationFor(AbilityDefinition definition) {
        return AbilityAnimationTiming.duration(definition);
    }

    private double[] hitTimesFor(AbilityDefinition definition) {
        return AbilityAnimationTiming.hitTimes(definition);
    }

    private double fallbackEffectDistance(AbilityDefinition definition) {
        return switch (definition.getEffectType()) {
            case SINGLE_TARGET -> 170.0;
            case DASH -> 125.0;
            case STUN, AREA_DAMAGE, EXECUTE -> Math.max(110.0, definition.getRadius() * 0.55);
            default -> 0.0;
        };
    }

    private static class ScheduledAbilityImpact {
        private final AbilityDefinition definition;
        private final Enemy target;
        private final double originX;
        private final double originY;
        private final double effectX;
        private final double effectY;
        private final double radius;
        private final double damage;
        private final DamageElement element;
        private final double[] hitTimes;
        private AbilityVisualEffect visual;
        private double elapsed;
        private double lastStep, previousPlayerX, previousPlayerY;
        private int nextHitIndex;

        private ScheduledAbilityImpact(AbilityDefinition definition, Enemy target,
                double originX, double originY, double effectX, double effectY,
                double radius, double damage, DamageElement element, double[] hitTimes) {
            this.definition = definition;
            this.target = target;
            this.originX = originX;
            this.originY = originY;
            this.effectX = effectX;
            this.effectY = effectY;
            this.radius = radius;
            this.damage = damage;
            this.element = element;
            this.hitTimes = hitTimes.clone();
        }

        private void update(double deltaTime) {
            lastStep = deltaTime;
            elapsed += deltaTime;
        }

        private boolean hasReadyHit() {
            return nextHitIndex < hitTimes.length && elapsed >= hitTimes[nextHitIndex];
        }

        private int consumeReadyHit() {
            return nextHitIndex++;
        }

        private boolean isFinished() {
            return nextHitIndex >= hitTimes.length;
        }

        private int hitCount() {
            return Math.max(1, hitTimes.length);
        }
    }

    private void damageEnemiesInRadius(double worldX, double worldY, double radius,
            double damage, boolean knockback) {
        damageEnemiesInRadius(worldX, worldY, radius, damage, knockback, DamageElement.PHYSICAL);
    }

    private void damageEnemiesInRadius(double worldX, double worldY, double radius,
            double damage, boolean knockback, DamageElement element) {
        for (Enemy enemy : enemiesInRadius(worldX, worldY, radius)) {
            damageEnemy(enemy, damage, element, worldX, worldY);
            if (knockback) {
                enemy.knockAwayFrom(worldX, worldY, element == DamageElement.EXPLOSION ? 46.0 : 28.0);
            }
        }
    }

    private void damageEnemiesInMeleeArc(double range, double damage) {
        boolean shieldAttack = "greatshield".equals(player.getDefaultWeaponStyle());
        double originX = player.getWorldX();
        double originY = player.getWorldY();
        double directionX = player.getRecentMoveX();
        double directionY = player.getRecentMoveY();
        double directionLength = Math.hypot(directionX, directionY);
        if (!shieldAttack && Math.abs(directionX) <= 0.001) {
            directionX = player.getHeldWeaponSideX() * 0.85;
            directionLength = Math.hypot(directionX, directionY);
        }
        if (directionLength <= 0.0001) {
            directionX = player.getHeldWeaponSideX();
            directionY = 0.0;
            directionLength = 1.0;
        }
        directionX /= directionLength;
        directionY /= directionLength;

        double maxRange = range + 28.0;
        double maxRangeSquared = maxRange * maxRange;
        double arcCosine = Math.cos(Math.toRadians(shieldAttack ? 58.0 : 82.0));
        List<Enemy> hitEnemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }
            double differenceX = enemy.getWorldX() - originX;
            double differenceY = enemy.getWorldY() - originY;
            double distanceSquared = differenceX * differenceX + differenceY * differenceY;
            if (distanceSquared > maxRangeSquared) {
                continue;
            }
            double distance = Math.max(0.0001, Math.sqrt(distanceSquared));
            double dot = (differenceX / distance) * directionX + (differenceY / distance) * directionY;
            if (dot >= arcCosine || distance <= 34.0) {
                hitEnemies.add(enemy);
            }
        }
        for (Enemy enemy : hitEnemies) {
            damageEnemy(enemy, damage, DamageElement.PHYSICAL, originX, originY);
            if (shieldAttack) {
                enemy.applyDirectionalKnockback(directionX, directionY, 34.0);
                enemy.applyStun(0.25);
            } else {
                enemy.knockAwayFrom(originX, originY, 12.0);
            }
        }
    }

    private void scheduleMeleeAttack(double range, double damage, double swingDuration) {
        double hitProgress = weapon instanceof AutoFireWeapon autoFireWeapon
                ? autoFireWeapon.getImpactProgress() : 0.43;
        double hitDelay = Math.max(0.06, swingDuration * hitProgress);
        pendingMeleeAttack = new PendingMeleeAttack(range, damage, hitDelay);
    }

    private void updatePendingMeleeAttack(double deltaTime) {
        if (pendingMeleeAttack == null) {
            return;
        }
        pendingMeleeAttack.timeUntilHit -= deltaTime;
        if (pendingMeleeAttack.timeUntilHit > 0.0) {
            return;
        }
        damageEnemiesInMeleeArc(pendingMeleeAttack.range, pendingMeleeAttack.damage);
        pendingMeleeAttack = null;
    }

    private static class PendingMeleeAttack {
        private final double range;
        private final double damage;
        private double timeUntilHit;

        private PendingMeleeAttack(double range, double damage, double timeUntilHit) {
            this.range = range;
            this.damage = damage;
            this.timeUntilHit = timeUntilHit;
        }
    }

    private void damageEnemy(Enemy enemy, double damage) {
        damageEnemy(enemy, damage, DamageElement.PHYSICAL, player.getWorldX(), player.getWorldY());
    }

    private void damageEnemy(Enemy enemy, double damage, DamageElement element,
            double originX, double originY) {
        damageSkillEnemy(enemy, damage, element, originX, originY, null);
    }

    private void damageSkillEnemy(Enemy enemy, double damage, DamageElement element,
            double originX, double originY, String skillId) {
        if (enemy == null || enemy.isDead()) {
            return;
        }
        double healthBefore = enemy.getHealth();
        if (skillId == null) enemy.takeDamage(damage, element, originX, originY);
        else enemy.takeSkillDamage(damage, element, originX, originY, skillId);
        double actualDamage = Math.max(0.0, healthBefore - enemy.getHealth());
        double visibleDamage = Math.min(9999, Math.max(1, Math.round(actualDamage)));
        if (actualDamage > 0.0) {
            combatImpactEffects.add(new CombatImpactEffect(skillId, enemy.getWorldX(), enemy.getWorldY(), element));
            if (combatImpactEffects.size() > 80) {
                combatImpactEffects.removeFirst();
            }
        }
        if (actualDamage > 0.0) {
            addFloatingText(String.valueOf((int) visibleDamage), enemy.getWorldX(),
                    enemy.getWorldY() - enemy.getCollisionRadius(), getDamageNumberColor(element));
        }
        if (enemy.isDead()) {
            dropGem(enemy);
        }
    }

    private List<Enemy> enemiesInRadius(double worldX, double worldY, double radius) {
        List<Enemy> matching = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.distanceSquaredTo(worldX, worldY) <= radiusSquared) {
                matching.add(enemy);
            }
        }
        return matching;
    }

    private void dashPlayer(double requestedDuration) {
        double directionX = player.getRecentMoveX();
        double directionY = player.getRecentMoveY();
        double length = Math.hypot(directionX, directionY);
        if (length <= 0.0001) {
            directionX = 1.0;
            directionY = 0.0;
            length = 1.0;
        }
        double distance = requestedDuration <= 0.0 ? 95.0 : 160.0;
        player.moveWorld(directionX / length * distance, directionY / length * distance);
    }

    private void applyDamageReduction(double multiplier, double duration) {
        damageReductionMultiplier = Math.min(damageReductionMultiplier, multiplier);
        damageReductionTimer = Math.max(damageReductionTimer, duration);
    }

    private void applyDamageBoost(double multiplier, double duration) {
        damageBoostMultiplier = Math.max(damageBoostMultiplier, multiplier);
        damageBoostTimer = Math.max(damageBoostTimer, duration);
    }

    private AbilityVisualEffect addAbilityVisual(AbilityDefinition definition, double startX,
            double startY, double targetX, double targetY, double radius,
            Color color, double maxLife) {
        AbilityVisualEffect effect = new AbilityVisualEffect(definition, startX, startY,
                targetX, targetY, radius, color, maxLife);
        effect.setCasterPosition(player.getWorldX(), player.getWorldY());
        effect.setCastOrigin(player.getSkillSourceWorldX(), player.getSkillSourceWorldY());
        effect.setSourcePose(player.getSkillSourceRotation(), player.getHeldWeaponSideX() < 0);
        abilityVisualEffects.add(effect);
        return effect;
    }

    private double abilityOriginX(AbilityDefinition definition) {
        if (isSelfCenteredAbility(definition)) {
            return player.getWorldX();
        }
        return player.getWeaponCastWorldX();
    }

    private double abilityOriginY(AbilityDefinition definition) {
        if (isSelfCenteredAbility(definition)) {
            return player.getWorldY();
        }
        return player.getWeaponCastWorldY();
    }

    private boolean isSelfCenteredAbility(AbilityDefinition definition) {
        return switch (definition.getEffectType()) {
            case HEAL, SHIELD, BUFF -> true;
            default -> false;
        };
    }

    private void refreshSelectedCharacterLoadout() {
        List<String> activeSkillIds = getCharacterActiveSkillIds(selectedCharacterIndex);
        if (activeSkillIds.size() != AbilityManager.EQUIPPED_SLOT_COUNT) {
            throw new IllegalStateException("Character must have exactly four active skills: "
                    + getSelectedCharacterName());
        }
        abilityManager.equipLoadout(activeSkillIds);
    }

    private void applySelectedPassive() {
        passiveDamageTakenMultiplier = 1.0;
        passiveAbilityPowerMultiplier = 1.0;
        passiveHealingMultiplier = 1.0;
        abilityManager.setPassiveManaRegenMultiplier(1.0);

        String passiveId = selectedCharacterIndex >= 0
                && selectedCharacterIndex < characterPassiveSkillIds.size()
                ? characterPassiveSkillIds.get(selectedCharacterIndex) : "";
        switch (passiveId) {
            case "iron_guard" -> passiveDamageTakenMultiplier = 0.88;
            case "shadow_assassin" -> {
                passiveAbilityPowerMultiplier = 1.12;
                player.increaseSpeed(18.0);
            }
            case "blessing" -> {
                passiveHealingMultiplier = 1.15;
                abilityManager.setPassiveManaRegenMultiplier(1.22);
            }
            case "cataclysm" -> passiveAbilityPowerMultiplier = 1.12;
            case "unbreakable" -> passiveDamageTakenMultiplier = 0.80;
            default -> {
            }
        }
    }

    private void triggerCastFeedback(AbilityDefinition definition) {
        if (soundEnabled) {
            AbilitySoundPlayer.play(definition);
        }
        double shake = switch (definition.getEffectType()) {
            case ULTIMATE -> 4.0;
            case EXECUTE, SUMMON -> 2.5;
            case AREA_DAMAGE, STUN, DASH -> 1.8;
            default -> 0.8;
        };
        // Let anticipation and VFX advance together instead of freezing the caster at release.
        addScreenShake(0.14, shake);
    }

    private void addFloatingText(String text, double worldX, double worldY, Color color) {
        floatingTexts.add(new FloatingText(text, worldX, worldY, color));
        if (floatingTexts.size() > 48) {
            floatingTexts.removeFirst();
        }
    }

    private Color getDamageNumberColor(DamageElement element) {
        return switch (element) {
            case FIRE, EXPLOSION -> new Color(255, 120, 55);
            case ICE -> new Color(120, 220, 255);
            case LIGHTNING -> new Color(255, 245, 105);
            case POISON -> new Color(125, 255, 100);
            case SHADOW -> new Color(210, 130, 255);
            case HOLY -> new Color(255, 245, 180);
            default -> new Color(255, 225, 90);
        };
    }

    private void addScreenShake(double duration, double strength) {
        if (screenShakeTime <= 0.0) {
            screenShakeDuration = 0.0;
            screenShakeStrength = 0.0;
        }
        screenShakeDuration = Math.max(screenShakeDuration, duration);
        screenShakeTime = Math.max(screenShakeTime, duration);
        screenShakeStrength = Math.max(screenShakeStrength, strength);
    }

    public int getScreenShakeOffsetX() {
        if (screenShakeTime <= 0.0 || screenShakeDuration <= 0.0) {
            return 0;
        }
        double strength = screenShakeStrength * (screenShakeTime / screenShakeDuration);
        return (int) Math.round(Math.sin(screenShakeTime * 83.0) * strength);
    }

    public int getScreenShakeOffsetY() {
        if (screenShakeTime <= 0.0 || screenShakeDuration <= 0.0) {
            return 0;
        }
        double strength = screenShakeStrength * (screenShakeTime / screenShakeDuration);
        return (int) Math.round(Math.cos(screenShakeTime * 71.0) * strength);
    }

    public void showManaSpent(AbilityDefinition definition) {
        manaPulseTime = 0.18;
        manaPulseColor = new Color(90, 190, 255);
        addFloatingText("-" + (int) definition.getManaCost() + " MP",
                player.getWorldX(), player.getWorldY() - 64, new Color(100, 190, 255));
    }

    public void showAbilityDenied(AbilityDefinition definition) {
        manaPulseTime = 0.22;
        manaPulseColor = new Color(255, 85, 95);
        addFloatingText("WAIT", player.getWorldX(), player.getWorldY() - 64,
                new Color(255, 95, 105));
    }

    public void showInsufficientMana(AbilityDefinition definition) {
        manaPulseTime = 0.22;
        manaPulseColor = new Color(255, 85, 95);
        addFloatingText("NO MP", player.getWorldX(), player.getWorldY() - 64,
                new Color(255, 95, 105));
    }

    public Color getManaPulseColor() {
        return manaPulseTime > 0.0 ? manaPulseColor : new Color(75, 170, 255);
    }

    private Color colorFor(AbilityDefinition definition) {
        return switch (definition.getAbilityClass()) {
            case ASSASSIN -> new Color(130, 70, 210);
            case BLACK_KNIGHT -> new Color(190, 45, 55);
            case PRIEST -> new Color(255, 230, 120);
            case RANGER -> new Color(90, 220, 120);
            case WARLOCK -> new Color(150, 65, 210);
            case ELEMENTALIST -> new Color(90, 190, 255);
            case GUARDIAN -> new Color(224, 180, 86);
        };
    }

    private DamageElement elementFor(AbilityDefinition definition) {
        String id = definition.getId();
        if (id.equals("heavy_slash") || id.equals("earth_shatter") || id.equals("knights_wrath")) return DamageElement.FIRE;
        if (id.contains("fire") || id.contains("flame") || id.contains("meteor")) {
            return DamageElement.FIRE;
        }
        if (id.contains("explosive") || id.contains("cataclysm") || id.contains("apocalypse")) {
            return DamageElement.EXPLOSION;
        }
        if (id.contains("ice") || id.contains("frost") || id.contains("blizzard")) {
            return DamageElement.ICE;
        }
        if (id.contains("lightning") || id.contains("thunder")) {
            return DamageElement.LIGHTNING;
        }
        if (id.contains("poison") || id.contains("venom")) {
            return DamageElement.POISON;
        }
        if (id.contains("holy") || id.contains("divine") || id.contains("heal")
                || id.contains("blessing") || id.contains("prayer")
                || id.contains("sanctuary") || id.contains("resurrection")
                || id.contains("purify")) {
            return DamageElement.HOLY;
        }
        if (definition.getAbilityClass() == AbilityClass.ASSASSIN
                || definition.getAbilityClass() == AbilityClass.WARLOCK
                || id.contains("dark") || id.contains("shadow") || id.contains("soul")
                || id.contains("curse") || id.contains("fear")) {
            return DamageElement.SHADOW;
        }
        return DamageElement.PHYSICAL;
    }
}
