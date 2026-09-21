import java.awt.Color;
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

    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final Weapon weapon = new TemplateWeapon();
    private final Ability ability = new TemplateAbility();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> enemyProjectiles = new ArrayList<>();
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
            "placeholder weapon",
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
    private boolean soundEnabled = true;
    private boolean debugInfoVisible;
    private boolean gameOver;
    private double gameOverTimer;
    private final List<ExplosionParticle> explosionParticles = new ArrayList<>();
    private final List<AbilityBurst> abilityBursts = new ArrayList<>();

    public GameLogic() {
        player = new TemplateCharacter();
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

        // Update all game objects once per timer tick.
        player.update(deltaTime);
        ability.update(deltaTime);
        updateAbilityBursts(deltaTime);

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
                    player.takeDamage(enemy.getDamage() * deltaTime);
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

    private void updateAbilityBursts(double deltaTime) {
        Iterator<AbilityBurst> burstIterator = abilityBursts.iterator();
        while (burstIterator.hasNext()) {
            AbilityBurst burst = burstIterator.next();
            burst.update(deltaTime);
            if (burst.isExpired()) {
                burstIterator.remove();
            }
        }
    }

    public void drawAbilityBursts(Graphics2D graphics, int centerX, int centerY) {
        for (AbilityBurst burst : abilityBursts) {
            double alpha = Math.max(0.0, burst.life / burst.maxLife);
            int radius = (int) Math.round(burst.radius * (1.0 + (1.0 - alpha) * 1.8));
            int screenX = (int) Math.round(centerX + burst.x + getWorldOffsetX());
            int screenY = (int) Math.round(centerY + burst.y + getWorldOffsetY());
            graphics.setColor(new Color(255, 200, 80, (int) (alpha * 180.0)));
            graphics.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
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

    private static class AbilityBurst {
        private double x;
        private double y;
        private final double radius;
        private final double maxLife;
        private double life;

        private AbilityBurst(double x, double y, double radius, double maxLife) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.maxLife = maxLife;
            this.life = maxLife;
        }

        private void update(double deltaTime) {
            life -= deltaTime;
        }

        private boolean isExpired() {
            return life <= 0.0;
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

                    enemy.takeDamage(projectile.getDamage());
                    if (enemy.isDead()) {
                        if (enemy instanceof TemplateEnemy3 bossEnemy
                                && !bossEnemy.hasSummonedMinions()) {
                            enemies.addAll(bossEnemy.createSummons(
                                    enemy.getWorldX(), enemy.getWorldY(), random));
                        }
                        dropGem(enemy);
                    }
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
                    nearestEnemy.takeDamage(projectile.getDamage());
                    if (nearestEnemy.isDead()) {
                        dropGem(nearestEnemy);
                    }
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
        return ability;
    }

    public void triggerAbility() {
        if (!ability.isReady()) {
            return;
        }

        ability.trigger(player.getWorldX(), player.getWorldY(), enemies);
        createAbilityBurst(player.getWorldX(), player.getWorldY());
    }

    private void createAbilityBurst(double originX, double originY) {
        abilityBursts.add(new AbilityBurst(0.0, 0.0, 140.0, 0.5));
        for (int index = 0; index < 18; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 30.0 + random.nextDouble() * 80.0;
            explosionParticles.add(new ExplosionParticle(
                    originX,
                    originY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    5.0 + random.nextDouble() * 12.0,
                    0.3 + random.nextDouble() * 0.5));
        }
    }

    public void showMainMenu() {
        resetRunState();
        mainMenuOpen = true;
        characterSelectOpen = false;
        gameStarted = false;
        paused = false;
        settingsOpen = false;
    }

    public void showCharacterSelection() {
        mainMenuOpen = false;
        characterSelectOpen = true;
        gameStarted = false;
        paused = false;
        settingsOpen = false;
    }

    public void startGame() {
        resetRunState();
        mainMenuOpen = false;
        characterSelectOpen = false;
        gameStarted = true;
        paused = false;
        settingsOpen = false;
    }

    private void resetRunState() {
        gameOver = false;
        gameOverTimer = 0.0;
        explosionParticles.clear();
        abilityBursts.clear();
        enemies.clear();
        gems.clear();
        projectiles.clear();
        enemyProjectiles.clear();
        spawnQueue.clear();
        whenToSpawn = INITIAL_SPAWN_DELAY;
        gameTimer = 0.0;
        repositionTimer = 0.0;
        level = 1;
        exp = 0;
        expToNextLevel = 10;
        upgradeMenuOpen = false;
        ability.reset();
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
}