import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
    private static final double SHIELD_REGEN_INTERVAL = 60.0;
    private static final double BEAM_COOLDOWN = 2.4;
    private static final double SHIELD_SUMMON_COOLDOWN = 60.0;
    private static final double MIN_DISTANCE = 220.0;
    private static final double MAX_DISTANCE = 340.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();

    private final List<EliteBossProjectile> shieldProjectiles = new ArrayList<>();
    private double shieldHealth = MAX_SHIELD;
    private double beamCooldown;
    private double shieldCooldown;
    private boolean shieldDown;
    private double manaLockTimer;
    private double moveLockTimer;
    private double aimLockTimer;
    private double nextShieldRegen;
    private boolean debuffTriggered;

    public EliteBossEnemy(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
                FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
        beamCooldown = 0.8;
        shieldCooldown = SHIELD_SUMMON_COOLDOWN;
        nextShieldRegen = SHIELD_REGEN_INTERVAL;
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
        for (int index = 0; index < 6; index++) {
            double angle = (Math.PI * 2.0 * index / 6.0) + 0.35;
            shieldProjectiles.add(new EliteBossProjectile(this, angle, 126.0 + index * 4.0));
        }
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            return;
        }

        if (shieldDown) {
            shieldHealth = Math.max(0.0, shieldHealth - deltaTime * 0.6);
        } else if (shieldProjectiles.size() == 0) {
            respawnShield();
        }

        if (!shieldDown && shieldHealth <= 0.0) {
            shieldDown = true;
            shieldCooldown = SHIELD_SUMMON_COOLDOWN;
            nextShieldRegen = SHIELD_REGEN_INTERVAL;
        }

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

        beamCooldown -= deltaTime;
        if (beamCooldown <= 0.0) {
            beamCooldown = BEAM_COOLDOWN;
            if (distance <= 600.0) {
                attackPlayer(targetWorldX, targetWorldY);
            }
        }

        for (EliteBossProjectile projectile : shieldProjectiles) {
            projectile.update(deltaTime, targetWorldX, targetWorldY);
            if (projectile.isBroken()) {
                if (projectile.hitsPlayer(targetWorldX, targetWorldY, 32.0)) {
                    triggerShieldBreakDebuff();
                }
            }
        }
    }

    public void triggerShieldBreakDebuff() {
        if (debuffTriggered) {
            return;
        }
        shieldDown = true;
        debuffTriggered = true;
        manaLockTimer = 10.0;
        moveLockTimer = 10.0;
        aimLockTimer = 10.0;
        shieldCooldown = 10.0;
    }

    public boolean shouldApplyDebuff() {
        return shieldDown && debuffTriggered && (moveLockTimer > 0.0 || aimLockTimer > 0.0 || manaLockTimer > 0.0);
    }

    public void applyBreakEffects(Player player, AbilityManager abilityManager) {
        player.applyMovementLock(10.0);
        player.applyAimLock(10.0);
        abilityManager.lockMana(10.0);
        debuffTriggered = true;
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

        double damageMultiplier = shieldDown ? 2.5 : 1.0;
        double beamDamage = (DAMAGE * damageMultiplier);
        double dx = playerX - getWorldX();
        double dy = playerY - getWorldY();
        double dist = Math.hypot(dx, dy);
        if (dist > 0.0001) {
            // This is a visual beam source; actual damage is processed by GameLogic.
        }
    }

    public List<EliteBossProjectile> getShieldProjectiles() {
        return new ArrayList<>(shieldProjectiles);
    }

    @Override
    public void takeDamage(double damage, DamageElement element, double originX, double originY) {
        super.takeDamage(damage * (shieldDown ? 2.0 : 1.0), element, originX, originY);
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
        drawBossBar(graphics, centerX, centerY, cameraX, cameraY);
        for (EliteBossProjectile projectile : shieldProjectiles) {
            projectile.draw(graphics, centerX, centerY, cameraX, cameraY);
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
}
