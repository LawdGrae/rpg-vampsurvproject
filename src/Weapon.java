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
        if (target == null) {
            return null;
        }
        return update(deltaTime, originX, originY,
                target.getWorldX(), target.getWorldY());
    }

    public Projectile update(double deltaTime, double originX, double originY,
            double targetX, double targetY) {
        cooldown -= deltaTime;
        if (cooldown > 0) {
            return null;
        }

        cooldown += fireInterval;
        return createProjectile(originX, originY, targetX, targetY);
    }

    protected abstract Projectile createProjectile(double originX, double originY,
            Enemy target);

    protected abstract Projectile createProjectile(double originX, double originY,
            double targetX, double targetY);
}