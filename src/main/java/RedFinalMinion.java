import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class RedFinalMinion extends Enemy {
    private static final double SPEED = 34.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 14.0;
    private static final double MAX_HEALTH = 28.0;
    private static final double FIRE_INTERVAL = 2.5;
    private static final double PROJECTILE_SPEED = 160.0;
    private static final double PROJECTILE_DAMAGE = 12.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private static final BufferedImage PROJECTILE_SPRITE = loadProjectileSprite();

    private double fireCooldown;

    public RedFinalMinion(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
                FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
        fireCooldown = 0.6;
    }

    public void updateFireCooldown(double deltaTime) {
        if (isDead()) {
            return;
        }
        fireCooldown = Math.max(0.0, fireCooldown - deltaTime);
    }

    public boolean canFire() {
        return fireCooldown <= 0.0 && !isDead();
    }

    public Projectile fireAt(double targetX, double targetY) {
        if (!canFire()) {
            return null;
        }
        fireCooldown = FIRE_INTERVAL;
        Projectile projectile = new Projectile(getWorldX(), getWorldY(), targetX, targetY,
                PROJECTILE_SPEED, PROJECTILE_DAMAGE, 8.0, PROJECTILE_SPRITE, 10.0);
        projectile.setOwner(this);
        return projectile;
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            return;
        }
        fireCooldown = Math.max(0.0, fireCooldown - deltaTime);
        super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
    }

    private static BufferedImage loadSpriteSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/REDMINION.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/REDMINION.png");
    }

    private static BufferedImage loadProjectileSprite() {
        return ResourceLoader.loadImage("/main/resources/projectiles/enemylv2projectile.png");
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
    }
}
