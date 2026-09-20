import java.awt.image.BufferedImage;

public abstract class Weapon {
    private static final double MIN_FIRE_INTERVAL = 0.08;

    protected double fireInterval;
    protected final double projectileSpeed;
    protected double projectileDamage;
    protected final double projectileRadius;
    protected final BufferedImage projectileSprite;
    private double critChance;
    private double cooldown;

    protected Weapon(double fireInterval, double projectileSpeed,
            double projectileDamage, double projectileRadius, BufferedImage projectileSprite) {
        this.fireInterval = fireInterval;
        this.projectileSpeed = projectileSpeed;
        this.projectileDamage = projectileDamage;
        this.projectileRadius = projectileRadius;
        this.projectileSprite = projectileSprite;
        this.critChance = 0.0;
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

    public void addDamage(double amount) {
        projectileDamage += amount;
    }

    public void addCritChance(double amount) {
        critChance += amount;
    }

    public void addFireSpeed(double percent) {
        if (percent <= 0.0) {
            return;
        }
        fireInterval = Math.max(MIN_FIRE_INTERVAL, fireInterval * (1.0 - percent));
    }

    public double getProjectileDamage() {
        return projectileDamage;
    }

    public double getCritChance() {
        return critChance;
    }

    public double getFireInterval() {
        return fireInterval;
    }

    protected abstract Projectile createProjectile(double originX, double originY,
            Enemy target);

    protected abstract Projectile createProjectile(double originX, double originY,
            double targetX, double targetY);
}