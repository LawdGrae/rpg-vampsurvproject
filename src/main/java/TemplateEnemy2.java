import java.awt.image.BufferedImage;

public class TemplateEnemy2 extends Enemy {
    private static final double SPEED = 45.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 72;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 30.0;
    private static final double MAX_HEALTH = 8.0;
    private static final double FIRE_INTERVAL = 6.0;
    private static final double PROJECTILE_SPEED = 180.0;
    private static final double PROJECTILE_DAMAGE = DAMAGE;
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
        return ResourceLoader.loadImage("/main/resources/enemy/LVL2Walks.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/LVL2Death.png");
    }

    private static BufferedImage loadProjectileSprite() {
        return ResourceLoader.loadImage("/main/resources/projectiles/enemylv2projectile.png");
    }
}
