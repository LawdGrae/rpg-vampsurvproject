import java.awt.image.BufferedImage;

public abstract class Weapon {
    protected final double fireInterval;
    protected final double projectileSpeed;
    protected final double projectileDamage;
    protected final double projectileRadius;
    protected final BufferedImage projectileSprite;
    private double cooldown;

    protected Weapon(double fireInterval, double projectileSpeed,
            double projectileDamage, double projectileRadius, BufferedImage projectileSprite) {
        this.fireInterval = fireInterval;
        this.projectileSpeed = projectileSpeed;
        this.projectileDamage = projectileDamage;
        this.projectileRadius = projectileRadius;
        this.projectileSprite = projectileSprite;
    }

    public Projectile update(double deltaTime, double originX, double originY,
            Enemy target) {
        cooldown -= deltaTime;
        if (target == null || cooldown > 0) {
            return null;
        }

        cooldown += fireInterval;
        return createProjectile(originX, originY, target);
    }

    protected abstract Projectile createProjectile(double originX, double originY,
            Enemy target);
}