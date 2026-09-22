import java.awt.image.BufferedImage;

public class TemplateWeapon extends Weapon {
    private static final double FIRE_INTERVAL = 0.7;
    private static final double PROJECTILE_SPEED = 450.0;
    private static final double PROJECTILE_DAMAGE = 1.0;
    private static final double PROJECTILE_RADIUS = 6.0;
    private static final BufferedImage PROJECTILE_SPRITE = loadProjectileSprite();

    public TemplateWeapon() {
        super(FIRE_INTERVAL, PROJECTILE_SPEED, PROJECTILE_DAMAGE,
                PROJECTILE_RADIUS, PROJECTILE_SPRITE);
    }

    @Override
    public Projectile update(double deltaTime, double originX, double originY,
            Enemy target, double fallbackDirectionX, double fallbackDirectionY) {
        if (target != null && !target.isDead()) {
            return super.update(deltaTime, originX, originY, target.getWorldX(), target.getWorldY());
        }
        return super.update(deltaTime, originX, originY,
                originX + fallbackDirectionX,
                originY + fallbackDirectionY);
    }

    @Override
    protected Projectile createProjectile(double originX, double originY, Enemy target) {
        return createProjectile(originX, originY, target.getWorldX(), target.getWorldY());
    }

    @Override
    protected Projectile createProjectile(double originX, double originY,
            double targetX, double targetY) {
        return new Projectile(originX, originY, targetX, targetY,
                projectileSpeed, projectileDamage, projectileRadius, projectileSprite);
    }

    private static BufferedImage loadProjectileSprite() {
        return ResourceLoader.loadImage("/main/resources/projectiles/temp_bullet.png");
    }
}
