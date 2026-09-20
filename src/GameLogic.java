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
    private static final int LEVEL_UP_EXP_BONUS = 8;

    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final Weapon weapon = new TemplateWeapon();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Random random = new Random();
    private final List<Double> spawnQueue = new ArrayList<>();
    private final List<String> upgradeChoices = Arrays.asList(
            "Rapid Fire",
            "Heavy Blows",
            "Arcane Magnet"
    );
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

    public GameLogic() {
        player = new TemplateCharacter();
    }

    public void setKeyPressed(String direction, boolean pressed) {
        // GamePanel sends input here instead of changing the player directly.
        player.setKeyPressed(direction, pressed);
    }

    public void update(double deltaTime) {
        if (upgradeMenuOpen) {
            return;
        }

        // Update all game objects once per timer tick.
        player.update(deltaTime);

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
                player.takeDamage(enemy.getDamage() * deltaTime);
            }
        }

        for (int firstIndex = 0; firstIndex < enemies.size(); firstIndex++) {
            // Check every unique enemy pair so enemies push apart instead of clumping.
            for (int secondIndex = firstIndex + 1;
                    secondIndex < enemies.size(); secondIndex++) {
                enemies.get(firstIndex).separateFrom(enemies.get(secondIndex));
            }
        }

        Enemy target = findNearestLivingEnemyInRange(SHOOT_RANGE);
        Projectile projectile = null;
        if (target != null) {
            projectile = weapon.update(deltaTime, player.getWorldX(),
                    player.getWorldY(), target);
        }
        if (projectile != null) {
            projectiles.add(projectile);
        }

        updateProjectiles(deltaTime);
        updateGems(deltaTime);
        enemies.removeIf(Enemy::isFinishedFading);
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
                    enemy.takeDamage(projectile.getDamage());
                    if (enemy.isDead()) {
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
            gem.update(deltaTime, player.getWorldX(), player.getWorldY());
            if (gem.isCollected()) {
                gemIterator.remove();
                addExperience(gem.getValue());
            }
        }
    }

    private void addExperience(int amount) {
        exp += amount;
        if (exp >= expToNextLevel) {
            exp = 0;
            level++;
            expToNextLevel += LEVEL_UP_EXP_BONUS + level * 2;
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

    public void showMainMenu() {
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
        mainMenuOpen = false;
        characterSelectOpen = false;
        gameStarted = true;
        paused = false;
        settingsOpen = false;
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
        return "/assets/portrait_temp.png";
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
        if (gameTimer < 6.0) {
            return 1;
        }
        if (gameTimer < 14.0) {
            return 2;
        }
        if (gameTimer < 24.0) {
            return 3;
        }
        return 4;
    }

    private double getSpawnInterval() {
        if (gameTimer < 8.0) {
            return 3.0;
        }
        if (gameTimer < 18.0) {
            return 2.2;
        }
        return 1.8;
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
            enemies.add(new TemplateEnemy(enemyX, enemyY));
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
            enemies.add(new TemplateEnemy(enemyX, enemyY));
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
        enemies.add(new TemplateEnemy(enemyX, enemyY));
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