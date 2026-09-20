import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TemplateEnemy2 extends Enemy {
    private static final double SPEED = 75.0;
    // These LVL2 sprites are full-size single-frame images, not a 5-frame strip.
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 30.0;
    private static final double MAX_HEALTH = 5.0;
    private static final double FIRE_INTERVAL = 6.0;
    private static final double PROJECTILE_SPEED = 180.0;
    private static final double PROJECTILE_DAMAGE = 10.0;
    private static final double PROJECTILE_RADIUS = 8.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private static final BufferedImage PROJECTILE_SPRITE = loadProjectileSprite();
    private double fireCooldown = 0.5;
    private boolean hasActiveProjectile;

    public TemplateEnemy2(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
            FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
    }

    public void updateFireCooldown(double deltaTime) {
        if (isDead()) {
            return;
        }
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime;
        }
    }

    public boolean canFire() {
        return !hasActiveProjectile && fireCooldown <= 0;
    }

    public Projectile fireAt(double targetX, double targetY) {
        if (!canFire()) {
            return null;
        }

        fireCooldown = FIRE_INTERVAL;
        hasActiveProjectile = true;
        Projectile projectile = new Projectile(getWorldX(), getWorldY(), targetX, targetY,
                PROJECTILE_SPEED, PROJECTILE_DAMAGE, PROJECTILE_RADIUS, PROJECTILE_SPRITE,
                12.0);
        projectile.setOwner(this);
        return projectile;
    }

    public void onProjectileDestroyed() {
        hasActiveProjectile = false;
    }

    private static BufferedImage loadSpriteSheet() {
        try {
            return ImageIO.read(
                    TemplateEnemy2.class.getResource("/assets/enemy/LVL2Walks.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/enemy/LVL2Walks.png", exception);
        }
    }

    private static BufferedImage loadDeathSheet() {
        try {
            return ImageIO.read(
                    TemplateEnemy2.class.getResource("/assets/enemy/LVL2Death.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/enemy/LVL2Death.png", exception);
        }
    }

    private static BufferedImage loadProjectileSprite() {
        try {
            return ImageIO.read(
                    TemplateEnemy2.class.getResource("/assets/projectiles/enemylv2projectile.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/projectiles/enemylv2projectile.png", exception);
        }
    }
}
