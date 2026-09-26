import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameLogic {
    // The panel width is used to keep new enemies outside the visible area.
    private static final int PANEL_WIDTH = 800;
    private static final double INITIAL_SPAWN_DELAY = 1.2;
    private static final double REPOSITION_INTERVAL = 10.0;
    private static final double SPAWN_CIRCLE_DIAMETER = PANEL_WIDTH + 200.0;
    private static final double SPAWN_RADIUS = SPAWN_CIRCLE_DIAMETER / 2.0;
    private static final double ENEMY_CLUMP_RADIUS = 40.0;
    private static final int MAX_ENEMIES = 100;
    private static final double TOO_FAR_DISTANCE = PANEL_WIDTH * 2.0;
    private static final double SHOOT_RANGE = 300.0;
    private static final double GEM_PULL_RADIUS = 170.0;
    private static final double GEM_COLLECTION_RADIUS = 18.0;
    private static final double GEM_ACCELERATION = 340.0;
    private static final double GEM_MAX_SPEED = 260.0;
    private static final int LEVEL_UP_EXP_BONUS = 6;

    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final Weapon weapon = new AutoFireWeapon();
    private final Ability legacyAbility = new TemplateAbility();
    private final AbilityManager abilityManager = new AbilityManager();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> enemyProjectiles = new ArrayList<>();
    private final List<AbilityVisualEffect> abilityVisualEffects = new ArrayList<>();
    private final List<CombatImpactEffect> combatImpactEffects = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
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
            "placeholder girl",
            "coming soon...",
            "coming soon...",
            "coming soon...",
            "coming soon..."
    );
    private final List<String> characterWeapons = Arrays.asList(
            "Auto Fire",
            "coming soon...",
            "coming soon...",
            "coming soon...",
            "coming soon..."
    );
    private double whenToSpawn = INITIAL_SPAWN_DELAY;
    private double gameTimer;
    private double repositionTimer;
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
    private boolean debugInfoVisible = true;
    private boolean gameOver;
    private double gameOverTimer;
    private double damageReductionTimer;
    private double damageReductionMultiplier = 1.0;
    private double damageBoostTimer;
    private double damageBoostMultiplier = 1.0;
    private double hitStopRemaining;
    private double screenShakeTime;
    private double screenShakeDuration;
    private double screenShakeStrength;
    private double manaPulseTime;
    private Color manaPulseColor = new Color(75, 170, 255);
    private final List<ExplosionParticle> explosionParticles = new ArrayList<>();

    public GameLogic() {
        player = createDefaultPlayer();
        refreshUpgradeChoices();
    }

    public void setKeyPressed(String direction, boolean pressed) {
        // GamePanel sends input here instead of changing the player directly.
        player.setKeyPressed(direction, pressed);
    }

    public void update(double deltaTime) {
        if (gameOver) {
            updateGameOver(deltaTime);
            return;
        }

        if (upgradeMenuOpen) {
            return;
        }

        if (hitStopRemaining > 0.0) {
            hitStopRemaining = Math.max(0.0, hitStopRemaining - deltaTime);
            abilityManager.update(deltaTime);
            updateAbilityVisualEffects(deltaTime);
            updateFloatingTexts(deltaTime);
            updateFeedback(deltaTime);
            return;
        }

        // Update all game objects once per timer tick.
        player.update(deltaTime);
        abilityManager.update(deltaTime);
        updatePlayerBuffs(deltaTime);
        updateAbilityVisualEffects(deltaTime);
        updateFloatingTexts(deltaTime);
        updateFeedback(deltaTime);

        // Start small and ramp up the wave size over time instead of instantly
        // surrounding the player with a full ring at startup.
        gameTimer += deltaTime;
        while (gameTimer >= whenToSpawn) {
            int enemiesToSpawn = getSpawnBatchSize();
            spawnFixed(enemiesToSpawn);
            whenToSpawn += getSpawnInterval();
        }
        if (!spawnQueue.isEmpty()) {
            spawnQueue.sort(Double::compareTo);
            while (gameTimer >= spawnQueue.getFirst()) {
                spawnQueue.removeFirst();
                spawnOne();
            }
        }

        repositionTimer += deltaTime;
        while (repositionTimer >= REPOSITION_INTERVAL) {
            repositionTimer -= REPOSITION_INTERVAL;
            repositionFarEnemies();
        }

        for (Enemy enemy : enemies) {
            // Enemies chase the player's current position in world space.
            enemy.update(deltaTime, player.getWorldX(), player.getWorldY(),
                    player.getCollisionRadius());

            // Damage is time-based, so the amount does not depend on frame rate.
            if (enemy.isCollidingWith(player.getWorldX(), player.getWorldY(),
                    player.getCollisionRadius())) {
                if (enemy instanceof TemplateEnemyMinion) {
                    player.applySlow(TemplateEnemyMinion.SLOW_DURATION,
                            TemplateEnemyMinion.SLOW_MULTIPLIER);
                } else {
                    player.takeDamage(enemy.getDamage() * deltaTime * damageReductionMultiplier);
                }
            }
        }

        for (int firstIndex = 0; firstIndex < enemies.size(); firstIndex++) {
            // Check every unique enemy pair so enemies push apart instead of clumping.
            for (int secondIndex = firstIndex + 1;
                    secondIndex < enemies.size(); secondIndex++) {
                enemies.get(firstIndex).separateFrom(enemies.get(secondIndex));
            }
        }

        Enemy target = findNearestLivingEnemyOnScreen();
        if (target == null) {
            target = findNearestLivingEnemyInRange(SHOOT_RANGE);
        }
        Projectile projectile = weapon.update(deltaTime, player.getWorldX(),
                player.getWorldY(), target,
                player.getRecentMoveX(), player.getRecentMoveY());
        if (projectile != null) {
            projectiles.add(projectile);
        }

        spawnEnemyProjectiles(deltaTime);
        updateProjectiles(deltaTime);
        updateEnemyProjectiles(deltaTime);
        updateGems(deltaTime);
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
        Iterator<AbilityVisualEffect> effectIterator = abilityVisualEffects.iterator();
        while (effectIterator.hasNext()) {
            AbilityVisualEffect effect = effectIterator.next();
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

    public void drawAbilityBursts(Graphics2D graphics, int centerX, int centerY) {
        for (AbilityVisualEffect effect : abilityVisualEffects) {
            effect.draw(graphics, centerX, centerY, getWorldOffsetX(), getWorldOffsetY());
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
        double maxScreenX = PANEL_WIDTH * 0.65;
        double maxScreenY = 600.0 * 0.65;

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
            for (Enemy enemy : enemies) {
                if (!enemy.isDead() && projectile.hits(enemy)) {
                    if (enemy instanceof TemplateEnemy3 bossEnemy) {
                        bossEnemy.registerPlayerProjectileHit();
                        if (bossEnemy.shouldReflectPlayerProjectile(
                                player.getWorldX(), player.getWorldY())) {
                            Projectile reflected = bossEnemy.createReflectedProjectile(
                                    player.getWorldX(), player.getWorldY());
                            if (reflected != null) {
                                enemyProjectiles.add(reflected);
                            }
                        }
                    }

                    damageEnemy(enemy, projectile.getDamage(), DamageElement.PHYSICAL,
                            projectile.getWorldX(), projectile.getWorldY());
                    hitEnemy = true;
                    break;
                }
            }

            if (hitEnemy || projectile.isExpired()) {
                projectileIterator.remove();
            }
        }
    }

    private void spawnEnemyProjectiles(double deltaTime) {
        for (Enemy enemy : enemies) {
            if (!(enemy instanceof TemplateEnemy2)) {
                continue;
            }

            TemplateEnemy2 enemy2 = (TemplateEnemy2) enemy;
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
            boolean hitPlayer = false;

            if (projectile.getOwner() instanceof TemplateEnemy3) {
                Enemy nearestEnemy = null;
                double nearestDistanceSquared = Double.POSITIVE_INFINITY;

                for (Enemy enemy : enemies) {
                    if (enemy == projectile.getOwner() || enemy.isDead()) {
                        continue;
                    }

                    double distanceSquared = enemy.distanceSquaredTo(
                            projectile.getWorldX(), projectile.getWorldY());
                    if (distanceSquared < nearestDistanceSquared) {
                        nearestDistanceSquared = distanceSquared;
                        nearestEnemy = enemy;
                    }
                }

                if (nearestEnemy != null && projectile.hits(nearestEnemy)) {
                    damageEnemy(nearestEnemy, projectile.getDamage(), DamageElement.PHYSICAL,
                            projectile.getWorldX(), projectile.getWorldY());
                    hitEnemy = true;
                }
            }

            if (!hitEnemy) {
                hitPlayer = projectile.hitsPlayer(
                        player.getWorldX(), player.getWorldY(), player.getCollisionRadius());
            }

            if (hitEnemy || hitPlayer || projectile.isExpired()) {
                if (projectile.getOwner() instanceof TemplateEnemy2) {
                    ((TemplateEnemy2) projectile.getOwner()).onProjectileDestroyed();
                }
                if (hitPlayer) {
                    player.takeDamage(projectile.getDamage());
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
        gems.add(new Gem(enemy.getWorldX(), enemy.getWorldY()));
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
        exp += amount;
        if (exp >= expToNextLevel) {
            exp = 0;
            level++;
            expToNextLevel += LEVEL_UP_EXP_BONUS + level * 2;
            refreshUpgradeChoices();
            upgradeMenuOpen = true;
        }
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
        if (gameOver || upgradeMenuOpen || paused || !gameStarted) {
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
    }

    private void resetRunState() {
        player = createDefaultPlayer();
        gameOver = false;
        gameOverTimer = 0.0;
        explosionParticles.clear();
        enemies.clear();
        gems.clear();
        projectiles.clear();
        enemyProjectiles.clear();
        abilityVisualEffects.clear();
        combatImpactEffects.clear();
        floatingTexts.clear();
        spawnQueue.clear();
        whenToSpawn = INITIAL_SPAWN_DELAY;
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
        hitStopRemaining = 0.0;
        screenShakeTime = 0.0;
        screenShakeDuration = 0.0;
        screenShakeStrength = 0.0;
        manaPulseTime = 0.0;
        abilityManager.resetRunState();
        legacyAbility.reset();
    }

    private Player createDefaultPlayer() {
        return new Player("/main/resources/character/temp_sheet.png", null,
                200.0, 5.0, 2, 16, 18, 100.0) {
        };
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
        if (ability != null && ability.getDefinition().isUnlockedAt(level)) {
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

    public String getSelectedCharacterName() {
        return characterNames.getFirst();
    }

    public String getSelectedWeaponName() {
        return characterWeapons.getFirst();
    }

    public String getPortraitPath() {
        return "/main/resources/portrait_temp.png";
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
        if (gameTimer < 60.0) {
            return 1;
        }
        if (gameTimer < 120.0) {
            return 2;
        }
        if (gameTimer < 180.0) {
            return 3;
        }
        return 4;
    }

    private double getSpawnInterval() {
        if (gameTimer < 90.0) {
            return 1.8;
        }
        if (gameTimer < 150.0) {
            return 1.5;
        }
        return 1.2;
    }

    private Enemy createEnemy(double worldX, double worldY) {
        double level3Chance = 0.0;
        if (level >= 3) {
            level3Chance = 0.12;
        }
        if (level >= 5) {
            level3Chance = 0.18;
        }
        if (gameTimer >= 60.0) {
            level3Chance = Math.min(0.28, level3Chance + 0.08);
        }
        if (random.nextDouble() < level3Chance) {
            return new TemplateEnemy3(worldX, worldY);
        }

        double level2Chance = 0.0;
        if (level >= 2) {
            level2Chance = 0.25;
        }
        if (gameTimer >= 90.0) {
            level2Chance = Math.min(0.55, level2Chance + 0.2);
        }
        if (random.nextDouble() < level2Chance) {
            return new TemplateEnemy2(worldX, worldY);
        }
        return new TemplateEnemy(worldX, worldY);
    }

    private void spawnClump(int enemyCount) {
        int enemiesToSpawn = Math.min(enemyCount, availableSlots());

        if (enemiesToSpawn <= 0) {
            return;
        }

        // Place the enemies evenly around a ring centered on the player.
        for (int index = 0; index < enemiesToSpawn; index++) {
            double angle = Math.PI * 2.0 * index / enemiesToSpawn;
            double enemyX = player.getWorldX() + Math.cos(angle) * SPAWN_RADIUS;
            double enemyY = player.getWorldY() + Math.sin(angle) * SPAWN_RADIUS;
            enemies.add(createEnemy(enemyX, enemyY));
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

        // Place every enemy on one uniform ring centered on the player.
        for (int index = 0; index < enemiesToSpawn; index++) {
            double angle = Math.PI * 2.0 * index / enemiesToSpawn;
            double enemyX = player.getWorldX() + Math.cos(angle) * SPAWN_RADIUS;
            double enemyY = player.getWorldY() + Math.sin(angle) * SPAWN_RADIUS;
            enemies.add(createEnemy(enemyX, enemyY));
        }
    }

    private void spawnOne() {
        if (availableSlots() <= 0) {
            return;
        }

        // Choose a fresh random angle for every individual enemy.
        double angle = random.nextDouble() * Math.PI * 2.0;
        double enemyX = player.getWorldX() + Math.cos(angle) * SPAWN_RADIUS;
        double enemyY = player.getWorldY() + Math.sin(angle) * SPAWN_RADIUS;
        enemies.add(createEnemy(enemyX, enemyY));
    }

    private void repositionFarEnemies() {
        double playerX = player.getWorldX();
        double playerY = player.getWorldY();
        double tooFarDistanceSquared = TOO_FAR_DISTANCE * TOO_FAR_DISTANCE;

        for (Enemy enemy : enemies) {
            if (enemy.distanceSquaredTo(playerX, playerY) > tooFarDistanceSquared) {
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

        double enemyX = playerX + facingX * SPAWN_RADIUS
            + perpendicularX * sidewaysOffset;
        double enemyY = playerY + facingY * SPAWN_RADIUS
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
        return enemies.size();
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
        graphics.setColor(java.awt.Color.WHITE);
        for (Projectile projectile : projectiles) {
            projectile.draw(graphics, centerX, centerY,
                getWorldOffsetX(), getWorldOffsetY());
        }
        for (Projectile projectile : enemyProjectiles) {
            projectile.draw(graphics, centerX, centerY,
                getWorldOffsetX(), getWorldOffsetY());
        }
        player.draw(graphics, centerX, centerY);

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                enemy.draw(graphics, centerX, centerY,
                        getWorldOffsetX(), getWorldOffsetY());
            }
        }
    }

    public void drawCollisionAreas(Graphics2D graphics, int centerX, int centerY) {
        // Collision debug overlays are disabled; no solid map collisions remain.
    }

    public void applyAbilityEffect(AbilityDefinition definition) {
        double originX = player.getWorldX();
        double originY = player.getWorldY();
        Color color = colorFor(definition);
        double damage = definition.getDamage() * damageBoostMultiplier;
        double radius = definition.getRadius();
        DamageElement element = elementFor(definition);
        Enemy target = findNearestLivingEnemyInRange(Math.max(radius, 420.0));
        double effectX = target == null ? originX : target.getWorldX();
        double effectY = target == null ? originY : target.getWorldY();
        triggerCastFeedback(definition);
        player.playAttackAnimation(definition);

        switch (definition.getEffectType()) {
            case SINGLE_TARGET -> {
                if (target != null) {
                    damageEnemy(target, damage, element, originX, originY);
                    addAbilityVisual(definition, originX, originY, effectX, effectY, 90, color, 0.45);
                }
            }
            case AREA_DAMAGE -> {
                damageEnemiesInRadius(effectX, effectY, radius, damage, true, element);
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.55);
            }
            case POISON -> {
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    damageEnemy(enemy, damage, element, originX, originY);
                    enemy.applyPoison(Math.max(1.0, damage * 0.25), Math.max(3.0, definition.getDuration()));
                }
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.7);
            }
            case SLOW -> {
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    damageEnemy(enemy, damage, element, originX, originY);
                    enemy.applySlow(0.45, Math.max(2.5, definition.getDuration()));
                }
                applyDamageReduction(0.75, Math.min(4.0, Math.max(1.0, definition.getDuration())));
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.75);
            }
            case STUN -> {
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    damageEnemy(enemy, damage, element, originX, originY);
                    enemy.applyStun(Math.max(0.8, definition.getDuration()));
                }
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.55);
            }
            case DASH -> {
                double beforeX = player.getWorldX();
                double beforeY = player.getWorldY();
                dashPlayer(definition.getDuration());
                damageEnemiesInRadius(player.getWorldX(), player.getWorldY(), radius, damage, true, element);
                addAbilityVisual(definition, beforeX, beforeY, player.getWorldX(), player.getWorldY(), radius, color, 0.55);
            }
            case HEAL -> {
                player.heal(damage);
                addFloatingText("+" + (int) Math.round(damage), originX, originY - 42, new Color(120, 255, 150));
                abilityManager.restoreMana(damage * 0.25);
                if (radius > 0.0) {
                    damageEnemiesInRadius(originX, originY, radius, damage * 0.65, false, element);
                }
                addAbilityVisual(definition, originX, originY, originX, originY, Math.max(90, radius), color, 0.75);
            }
            case SHIELD -> {
                applyDamageReduction(0.35, Math.max(3.0, definition.getDuration()));
                if (damage > 0.0) {
                    player.heal(damage);
                    addFloatingText("+" + (int) Math.round(damage), originX, originY - 42, new Color(120, 255, 150));
                }
                addAbilityVisual(definition, originX, originY, originX, originY, 150,
                        color, Math.max(0.9, definition.getDuration()));
            }
            case BUFF -> {
                applyDamageBoost(1.45, Math.max(4.0, definition.getDuration()));
                if (definition.getAbilityClass() == AbilityClass.PRIEST) {
                    abilityManager.boostManaRegen(Math.max(4.0, definition.getDuration()), 1.8);
                }
                if (definition.getAbilityClass() == AbilityClass.WARLOCK) {
                    player.takeDamage(5.0);
                    addFloatingText("-5", originX, originY - 42, new Color(220, 60, 90));
                }
                addAbilityVisual(definition, originX, originY, originX, originY, 130, color, 0.75);
            }
            case MARK -> {
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    damageEnemy(enemy, damage, element, originX, originY);
                    enemy.applyMark(1.45, Math.max(4.0, definition.getDuration()));
                }
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.75);
            }
            case EXECUTE -> {
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    damageEnemy(enemy, enemy.getHealthRatio() <= 0.35 ? 9999.0 : damage,
                            element, originX, originY);
                }
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 0.7);
            }
            case SUMMON -> {
                damageEnemiesInRadius(effectX, effectY, radius, damage, true, element);
                for (Enemy enemy : enemiesInRadius(effectX, effectY, radius)) {
                    enemy.applyMark(1.25, Math.max(3.0, definition.getDuration()));
                }
                addAbilityVisual(definition, originX, originY, effectX, effectY, radius, color, 1.0);
            }
            case ULTIMATE -> {
                damageEnemiesInRadius(originX, originY, radius, damage, true, element);
                for (Enemy enemy : enemiesInRadius(originX, originY, radius)) {
                    enemy.applySlow(0.35, Math.max(3.0, definition.getDuration()));
                    enemy.applyPoison(Math.max(1.0, damage * 0.12), Math.max(2.0, definition.getDuration()));
                }
                if (definition.getAbilityClass() == AbilityClass.BLACK_KNIGHT) {
                    applyDamageReduction(0.25, Math.max(4.0, definition.getDuration()));
                }
                addAbilityVisual(definition, originX, originY, originX, originY, radius, color, 1.2);
            }
            default -> {
            }
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

    private void damageEnemy(Enemy enemy, double damage) {
        damageEnemy(enemy, damage, DamageElement.PHYSICAL, player.getWorldX(), player.getWorldY());
    }

    private void damageEnemy(Enemy enemy, double damage, DamageElement element,
            double originX, double originY) {
        if (enemy == null || enemy.isDead()) {
            return;
        }
        double healthBefore = enemy.getHealth();
        enemy.takeDamage(damage, element, originX, originY);
        double actualDamage = Math.max(0.0, healthBefore - enemy.getHealth());
        double visibleDamage = Math.min(9999, Math.max(1, Math.round(actualDamage)));
        if (actualDamage > 0.0) {
            combatImpactEffects.add(new CombatImpactEffect(enemy.getWorldX(), enemy.getWorldY(), element));
            if (combatImpactEffects.size() > 80) {
                combatImpactEffects.removeFirst();
            }
        }
        addFloatingText(String.valueOf((int) visibleDamage), enemy.getWorldX(),
                enemy.getWorldY() - enemy.getCollisionRadius(), getDamageNumberColor(element));
        if (enemy.isDead()) {
            if (enemy instanceof TemplateEnemy3 bossEnemy && !bossEnemy.hasSummonedMinions()) {
                enemies.addAll(bossEnemy.createSummons(enemy.getWorldX(), enemy.getWorldY(), random));
            }
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

    private void addAbilityVisual(AbilityDefinition definition, double startX,
            double startY, double targetX, double targetY, double radius,
            Color color, double maxLife) {
        abilityVisualEffects.add(new AbilityVisualEffect(definition, startX, startY,
                targetX, targetY, radius, color, maxLife));
    }

    private void triggerCastFeedback(AbilityDefinition definition) {
        if (soundEnabled) {
            AbilitySoundPlayer.play(definition);
        }
        double shake = switch (definition.getEffectType()) {
            case ULTIMATE -> 10.0;
            case EXECUTE, SUMMON -> 7.0;
            case AREA_DAMAGE, STUN, DASH -> 5.0;
            default -> 2.5;
        };
        double stop = switch (definition.getEffectType()) {
            case ULTIMATE -> 0.12;
            case EXECUTE -> 0.1;
            case SINGLE_TARGET, AREA_DAMAGE, STUN -> 0.06;
            default -> 0.035;
        };
        addScreenShake(0.2, shake);
        hitStopRemaining = Math.max(hitStopRemaining, stop);
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
        addFloatingText("NO MANA", player.getWorldX(), player.getWorldY() - 64,
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
        };
    }

    private DamageElement elementFor(AbilityDefinition definition) {
        String id = definition.getId();
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
