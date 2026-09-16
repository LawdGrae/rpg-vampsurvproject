import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameLogic {
    // The panel width is used to keep new enemies outside the visible area.
    private static final int PANEL_WIDTH = 800;
    private static final double SPAWN_INTERVAL = 4.5;
    private static final double REPOSITION_INTERVAL = 10.0;
    private static final double SPAWN_CIRCLE_DIAMETER = PANEL_WIDTH + 200.0;
    private static final double SPAWN_RADIUS = SPAWN_CIRCLE_DIAMETER / 2.0;
    private static final double ENEMY_CLUMP_RADIUS = 40.0;
    private static final int MAX_ENEMIES = 100;
    private static final double TOO_FAR_DISTANCE = PANEL_WIDTH * 2.0;

    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final Weapon weapon = new TemplateWeapon();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Random random = new Random();
    private final List<Double> spawnQueue = new ArrayList<>();
    private double whenToSpawn = 0.0;
    private double gameTimer;
    private double repositionTimer;

    public GameLogic() {
        player = new TemplateCharacter();
    }

    public void setKeyPressed(String direction, boolean pressed) {
        // GamePanel sends input here instead of changing the player directly.
        player.setKeyPressed(direction, pressed);
    }

    public void update(double deltaTime) {
        // Update all game objects once per timer tick.
        player.update(deltaTime);

        // Spawn one enemy at each short scheduled time.
        gameTimer += deltaTime;
        while (gameTimer >= whenToSpawn) {
            whenToSpawn += SPAWN_INTERVAL;
            spawnFixed(20);
        }
        
        if(!spawnQueue.isEmpty()) {
            spawnQueue.sort(Double::compareTo);
            while(gameTimer >= spawnQueue.getFirst()) {
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

        Enemy target = findNearestLivingEnemy();
        Projectile projectile;
        if (target != null) {
            projectile = weapon.update(deltaTime, player.getWorldX(),
                    player.getWorldY(), target);
        } else {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 250.0 + random.nextDouble() * 700.0;
            double targetX = player.getWorldX() + Math.cos(angle) * distance;
            double targetY = player.getWorldY() + Math.sin(angle) * distance;
            projectile = weapon.update(deltaTime, player.getWorldX(),
                    player.getWorldY(), targetX, targetY);
        }
        if (projectile != null) {
            projectiles.add(projectile);
        }

        updateProjectiles(deltaTime);
        enemies.removeIf(Enemy::isFinishedFading);
    }

    private Enemy findNearestLivingEnemy() {
        Enemy nearestEnemy = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                double distance = enemy.distanceSquaredTo(player.getWorldX(), player.getWorldY());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
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
                    hitEnemy = true;
                    break;
                }
            }

            if (hitEnemy || projectile.isExpired()) {
                projectileIterator.remove();
            }
        }
    }

    private int availableSlots() {
        return MAX_ENEMIES - enemies.size();
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
        for(int index = 0; index < enemyCount; index++) {
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
        // Draw enemies first so the player appears above them if they overlap visually.
        for (Enemy enemy : enemies) {
            enemy.draw(graphics, centerX, centerY,
                    getWorldOffsetX(), getWorldOffsetY());
        }
        graphics.setColor(java.awt.Color.WHITE);
        for (Projectile projectile : projectiles) {
            projectile.draw(graphics, centerX, centerY,
                getWorldOffsetX(), getWorldOffsetY());
        }
        player.draw(graphics, centerX, centerY);
    }

    public void drawCollisionAreas(Graphics2D graphics, int centerX, int centerY) {
        double cameraX = getWorldOffsetX();
        double cameraY = getWorldOffsetY();

        for (Enemy enemy : enemies) {
            enemy.drawCollisionArea(graphics, centerX, centerY, cameraX, cameraY);
        }
        player.drawCollisionArea(graphics, centerX, centerY);
    }
}