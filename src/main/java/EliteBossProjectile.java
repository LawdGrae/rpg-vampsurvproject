import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class EliteBossProjectile {
    private static final int DRAW_SIZE = 18;
    private static final double ORBIT_SPEED = 2.3;
    private static final BufferedImage SPRITE = loadSprite();

    private final EliteBossEnemy owner;
    private double angle;
    private double orbitRadius;
    private double worldX;
    private double worldY;
    private double homeTargetX;
    private double homeTargetY;
    private int shieldHealth = 50;
    private boolean homingMode;
    private double stunTimer;

    public EliteBossProjectile(EliteBossEnemy owner, double angle, double orbitRadius) {
        this.owner = owner;
        this.angle = angle;
        this.orbitRadius = orbitRadius;
        this.worldX = owner.getWorldX() + Math.cos(angle) * orbitRadius;
        this.worldY = owner.getWorldY() + Math.sin(angle) * orbitRadius;
        this.homeTargetX = owner.getWorldX();
        this.homeTargetY = owner.getWorldY();
    }

    public int getShieldHealth() {
        return shieldHealth;
    }

    public void setShieldHealth(int shieldHealth) {
        this.shieldHealth = shieldHealth;
    }

    public boolean isBroken() {
        return shieldHealth <= 0;
    }

    public void update(double deltaTime, double playerX, double playerY) {
        if (owner == null || owner.isDead()) {
            return;
        }

        if (shieldHealth <= 0) {
            homingMode = true;
            stunTimer = Math.max(0.0, stunTimer - deltaTime);
            double differenceX = playerX - worldX;
            double differenceY = playerY - worldY;
            double length = Math.hypot(differenceX, differenceY);
            if (length > 0.0001) {
                worldX += (differenceX / length) * 140.0 * deltaTime;
                worldY += (differenceY / length) * 140.0 * deltaTime;
            }
            homeTargetX = playerX;
            homeTargetY = playerY;
            return;
        }

        angle += ORBIT_SPEED * deltaTime;
        double centerX = owner.getWorldX();
        double centerY = owner.getWorldY();
        worldX = centerX + Math.cos(angle) * orbitRadius;
        worldY = centerY + Math.sin(angle) * orbitRadius;
    }

    public void damage(int amount, double playerX, double playerY) {
        shieldHealth = Math.max(0, shieldHealth - amount);
        if (shieldHealth <= 0) {
            homingMode = true;
            homeTargetX = playerX;
            homeTargetY = playerY;
        }
    }

    public boolean hitsPlayer(double playerX, double playerY, double playerCollisionRadius) {
        double differenceX = playerX - worldX;
        double differenceY = playerY - worldY;
        double hitDistance = DRAW_SIZE + playerCollisionRadius;
        return differenceX * differenceX + differenceY * differenceY <= hitDistance * hitDistance;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        int screenX = (int) (centerX + worldX + cameraX - DRAW_SIZE / 2.0);
        int screenY = (int) (centerY + worldY + cameraY - DRAW_SIZE / 2.0);
        graphics.drawImage(SPRITE, screenX, screenY, DRAW_SIZE, DRAW_SIZE, null);

        if (shieldHealth > 0) {
            graphics.setColor(new Color(120, 230, 255, 120));
            graphics.drawOval(screenX - 6, screenY - 6, DRAW_SIZE + 12, DRAW_SIZE + 12);
        }
    }

    public boolean isHomingMode() {
        return homingMode;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    private static BufferedImage loadSprite() {
        return ResourceLoader.loadImage("/main/resources/projectiles/EliteBossProjectile.png");
    }
}
