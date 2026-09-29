import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EliteBossEnemy extends Enemy {
    private static final double SPEED = 0.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 120;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 26.0;
    private static final double MAX_HEALTH = 600.0;
    private static final double MAX_SHIELD = 50.0;
    private static final double SHIELD_SUMMON_COOLDOWN = 12.0;
    private static final int ORBITING_PROJECTILE_COUNT = 6;
    private static final double ATTACK_INTERVAL = 0.72;
    private static final double PROJECTILE_REPLENISH_INTERVAL = 0.85;
    private static final double MIN_DISTANCE = 220.0;
    private static final double MAX_DISTANCE = 340.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private static final BufferedImage ATTACK_SPRITE = loadAttackSprite();

    private final List<EliteBossProjectile> shieldProjectiles = new ArrayList<>();
    private double shieldHealth = MAX_SHIELD;
    private double shieldCooldown;
    private boolean shieldDown;
    private double manaLockTimer;
    private double moveLockTimer;
    private double aimLockTimer;
    private boolean debuffTriggered;
    private double attackCooldown = 0.5;
    private double projectileReplenishCooldown;
    private double nextProjectileAngle;
    private double pendingAttackDamage;
    private double attackAnimationTimer;
    private boolean breakEffectsApplied;

    public EliteBossEnemy(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
                FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
        shieldCooldown = SHIELD_SUMMON_COOLDOWN;
        respawnShield();
    }

    public double getMaxHealth() {
        return MAX_HEALTH;
    }

    public double getShieldHealth() {
        return shieldHealth;
    }

    public boolean hasShield() {
        return !shieldDown && shieldHealth > 0.0;
    }

    public boolean isShieldDown() {
        return shieldDown;
    }

    public void respawnShield() {
        shieldProjectiles.clear();
        shieldHealth = MAX_SHIELD;
        shieldDown = false;
        debuffTriggered = false;
        breakEffectsApplied = false;
        nextProjectileAngle = 0.35;
        for (int index = 0; index < ORBITING_PROJECTILE_COUNT; index++) {
            addOrbitingProjectile(index);
        }
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
            return;
        }

        super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
        if (isDead()) {
            return;
        }

        updateShieldProjectiles(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
        attackAnimationTimer = Math.max(0.0, attackAnimationTimer - deltaTime);

        if (shieldDown) {
            shieldCooldown -= deltaTime;
            if (shieldCooldown <= 0.0) {
                respawnShield();
                shieldCooldown = SHIELD_SUMMON_COOLDOWN;
            }
        }

        if (manaLockTimer > 0.0) {
            manaLockTimer = Math.max(0.0, manaLockTimer - deltaTime);
        }
        if (moveLockTimer > 0.0) {
            moveLockTimer = Math.max(0.0, moveLockTimer - deltaTime);
        }
        if (aimLockTimer > 0.0) {
            aimLockTimer = Math.max(0.0, aimLockTimer - deltaTime);
        }

        double differenceX = targetWorldX - getWorldX();
        double differenceY = targetWorldY - getWorldY();
        double distance = Math.hypot(differenceX, differenceY);
        if (distance < MIN_DISTANCE) {
            double moveX = (differenceX / (distance + 0.0001)) * (MIN_DISTANCE - distance) * 0.8;
            double moveY = (differenceY / (distance + 0.0001)) * (MIN_DISTANCE - distance) * 0.8;
            teleportTo(getWorldX() - moveX * deltaTime, getWorldY() - moveY * deltaTime);
        } else if (distance > MAX_DISTANCE) {
            double moveX = (differenceX / (distance + 0.0001)) * (distance - MAX_DISTANCE) * 0.6;
            double moveY = (differenceY / (distance + 0.0001)) * (distance - MAX_DISTANCE) * 0.6;
            teleportTo(getWorldX() + moveX * deltaTime, getWorldY() + moveY * deltaTime);
        }

        attackCooldown -= deltaTime;
        if (attackCooldown <= 0.0 && distance <= 700.0) {
            fireOrbitingProjectile(targetWorldX, targetWorldY);
            attackCooldown = ATTACK_INTERVAL;
            attackAnimationTimer = 0.28;
        }
    }

    private void updateShieldProjectiles(double deltaTime, double playerX, double playerY,
            double playerCollisionRadius) {
        Iterator<EliteBossProjectile> iterator = shieldProjectiles.iterator();
        while (iterator.hasNext()) {
            EliteBossProjectile projectile = iterator.next();
            projectile.update(deltaTime, playerX, playerY);

            if (projectile.isLaunched() && projectile.hitsPlayer(
                    playerX, playerY, playerCollisionRadius)) {
                if (projectile.markSpent()) {
                    pendingAttackDamage += projectile.getAttackDamage();
                }
            } else if (projectile.isBroken() && projectile.hitsPlayer(
                    playerX, playerY, playerCollisionRadius)) {
                triggerShieldBreakDebuff();
                projectile.markSpent();
            }

            if (projectile.isExpired() || projectile.isSpent()) {
                iterator.remove();
            }
        }

        projectileReplenishCooldown = Math.max(0.0, projectileReplenishCooldown - deltaTime);
        if (shieldProjectiles.size() < ORBITING_PROJECTILE_COUNT
                && projectileReplenishCooldown <= 0.0) {
            addOrbitingProjectile(shieldProjectiles.size());
            projectileReplenishCooldown = PROJECTILE_REPLENISH_INTERVAL;
        }
    }

    private void fireOrbitingProjectile(double playerX, double playerY) {
        for (EliteBossProjectile projectile : shieldProjectiles) {
            if (projectile.isOrbiting()) {
                projectile.launchAt(playerX, playerY);
                attackAnimationTimer = 0.28;
                return;
            }
        }
        if (shieldProjectiles.size() < ORBITING_PROJECTILE_COUNT) {
            addOrbitingProjectile(shieldProjectiles.size());
            shieldProjectiles.getLast().launchAt(playerX, playerY);
            attackAnimationTimer = 0.28;
        }
    }

    private void addOrbitingProjectile(int index) {
        double angle = nextProjectileAngle;
        nextProjectileAngle += (Math.PI * 2.0 / ORBITING_PROJECTILE_COUNT);
        shieldProjectiles.add(new EliteBossProjectile(this, angle, 126.0 + index % 3 * 4.0));
    }

    public double consumePendingAttackDamage() {
        double damage = pendingAttackDamage;
        pendingAttackDamage = 0.0;
        return damage;
    }

    public boolean damageShieldProjectileAt(double worldX, double worldY, double damage,
            double playerX, double playerY) {
        if (shieldDown || damage <= 0.0) {
            return false;
        }
        double hitDistance = 18.0 + 10.0;
        double hitDistanceSquared = hitDistance * hitDistance;
        for (EliteBossProjectile projectile : shieldProjectiles) {
            if (!projectile.isOrbiting()) {
                continue;
            }
            double differenceX = projectile.getWorldX() - worldX;
            double differenceY = projectile.getWorldY() - worldY;
            if (differenceX * differenceX + differenceY * differenceY <= hitDistanceSquared) {
                int damageAmount = Math.max(1, (int) Math.ceil(damage));
                projectile.damage(damageAmount, playerX, playerY);
                shieldHealth = Math.max(0.0, shieldHealth - damage);
                if (shieldHealth <= 0.0) {
                    shieldDown = true;
                    shieldCooldown = SHIELD_SUMMON_COOLDOWN;
                }
                return true;
            }
        }
        return false;
    }

    public void triggerShieldBreakDebuff() {
        if (debuffTriggered) {
            return;
        }
        shieldDown = true;
        debuffTriggered = true;
        breakEffectsApplied = false;
        manaLockTimer = 10.0;
        moveLockTimer = 10.0;
        aimLockTimer = 10.0;
        shieldCooldown = 10.0;
    }

    public boolean shouldApplyDebuff() {
        return shieldDown && debuffTriggered && !breakEffectsApplied
            && (moveLockTimer > 0.0 || aimLockTimer > 0.0 || manaLockTimer > 0.0);
    }

    public void applyBreakEffects(Player player, AbilityManager abilityManager) {
        player.applyMovementLock(10.0);
        player.applyAimLock(10.0);
        abilityManager.lockMana(10.0);
        debuffTriggered = true;
        breakEffectsApplied = true;
    }

    public boolean isMovementLocked() {
        return moveLockTimer > 0.0;
    }

    public boolean isAimLocked() {
        return aimLockTimer > 0.0;
    }

    public boolean isManaLocked() {
        return manaLockTimer > 0.0;
    }

    public void attackPlayer(double playerX, double playerY) {
        if (isDead()) {
            return;
        }

        fireOrbitingProjectile(playerX, playerY);
    }

    public List<EliteBossProjectile> getShieldProjectiles() {
        return new ArrayList<>(shieldProjectiles);
    }

    @Override
    public void takeDamage(double damage, DamageElement element, double originX, double originY) {
        double adjustedDamage = damage * (shieldDown ? 2.0 : 1.0);
        if (!shieldDown && shieldHealth > 0.0) {
            double absorbed = Math.min(shieldHealth, adjustedDamage * 0.5);
            shieldHealth -= absorbed;
            adjustedDamage -= absorbed;
            if (shieldHealth <= 0.0) {
                shieldDown = true;
                shieldCooldown = SHIELD_SUMMON_COOLDOWN;
            }
        }
        super.takeDamage(adjustedDamage, element, originX, originY);
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (!isDead() && attackAnimationTimer > 0.0) {
            int screenX = (int) (centerX + getWorldX() + cameraX - RENDER_SIZE / 2.0);
            int screenY = (int) (centerY + getWorldY() + cameraY - RENDER_SIZE / 2.0);
            graphics.drawImage(ATTACK_SPRITE, screenX, screenY, RENDER_SIZE, RENDER_SIZE, null);
        } else {
            super.draw(graphics, centerX, centerY, cameraX, cameraY);
        }
        drawBossBar(graphics, centerX, centerY, cameraX, cameraY);
        if (!isDead()) {
            for (EliteBossProjectile projectile : shieldProjectiles) {
                projectile.draw(graphics, centerX, centerY, cameraX, cameraY);
            }
        }
    }

    private void drawBossBar(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (isDead()) {
            return;
        }

        int barWidth = 160;
        int barHeight = 12;
        int x = (int) (centerX + getWorldX() + cameraX - barWidth / 2.0);
        int y = (int) (centerY + getWorldY() + cameraY - RENDER_SIZE / 2.0 - 26.0);

        graphics.setColor(new Color(25, 25, 25, 200));
        graphics.fillRoundRect(x, y, barWidth, barHeight, 10, 10);

        double healthRatio = getHealthRatio();
        int currentWidth = (int) Math.round(barWidth * healthRatio);
        graphics.setColor(new Color(180, 30, 60));
        graphics.fillRoundRect(x, y, currentWidth, barHeight, 10, 10);

        graphics.setColor(new Color(255, 255, 255, 200));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRoundRect(x, y, barWidth, barHeight, 10, 10);

        if (!shieldDown && shieldProjectiles.size() > 0) {
            int shieldBarWidth = 160;
            int shieldBarX = x;
            int shieldBarY = y + 16;
            graphics.setColor(new Color(25, 25, 25, 200));
            graphics.fillRoundRect(shieldBarX, shieldBarY, shieldBarWidth, 8, 8, 8);
            int shieldCurrent = (int) Math.round(shieldBarWidth * (shieldHealth / MAX_SHIELD));
            graphics.setColor(new Color(80, 220, 255));
            graphics.fillRoundRect(shieldBarX, shieldBarY, shieldCurrent, 8, 8, 8);
        }
    }

    private static BufferedImage loadSpriteSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/ELITEBOSS.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/ELITEBOSS.png");
    }

    private static BufferedImage loadAttackSprite() {
        return ResourceLoader.loadImage("/main/resources/enemy/ELITEBOSSATK.png");
    }
}
