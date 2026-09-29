import java.awt.image.BufferedImage;

public class AutoFireWeapon extends Weapon {
    private static final double FIRE_INTERVAL = 0.7;
    private static final double PROJECTILE_SPEED = 450.0;
    private static final double PROJECTILE_DAMAGE = 1.0;
    private static final double PROJECTILE_RADIUS = 16.0;
    private static final BufferedImage PROJECTILE_SPRITE = loadProjectileSprite();
    private static final BufferedImage HOLY_PROJECTILE_SPRITE =
            ResourceLoader.loadImage("/main/resources/abilities/holy_bolt.png");
    private static final BufferedImage ELEMENTAL_PROJECTILE_SPRITE =
            ResourceLoader.loadImage("/main/resources/abilities/fire_bolt.png");
    private double attackCooldown;
    private double attackRange = 118.0;
    private String weaponStyle = "sword_shield";
    private BufferedImage attackSprite = PROJECTILE_SPRITE;
    private double maxDrawSize = 42.0;
    private DamageElement damageElement = DamageElement.PHYSICAL;

    public AutoFireWeapon() {
        super(FIRE_INTERVAL, PROJECTILE_SPEED, PROJECTILE_DAMAGE,
                PROJECTILE_RADIUS, PROJECTILE_SPRITE);
    }

    @Override
    public Projectile update(double deltaTime, double originX, double originY,
            Enemy target, double fallbackDirectionX, double fallbackDirectionY) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaTime);
        if (!isRangedAttack() || attackCooldown > 0.0) {
            return null;
        }

        double targetX;
        double targetY;
        if (target != null && !target.isDead()) {
            targetX = target.getWorldX();
            targetY = target.getWorldY();
        } else {
            double length = Math.hypot(fallbackDirectionX, fallbackDirectionY);
            if (length <= 0.0001) {
                fallbackDirectionX = 1.0;
                fallbackDirectionY = 0.0;
                length = 1.0;
            }
            targetX = originX + fallbackDirectionX / length * attackRange;
            targetY = originY + fallbackDirectionY / length * attackRange;
        }

        attackCooldown = fireInterval;
        return createProjectile(originX, originY, targetX, targetY);
    }

    public boolean isRangedAttack() {
        return "holy_staff".equals(weaponStyle) || "elemental_staff".equals(weaponStyle)
                || "staff".equals(weaponStyle);
    }

    public boolean canAttack(double deltaTime) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaTime);
        return attackCooldown <= 0.0;
    }

    public void resetAttackCooldown() {
        attackCooldown = fireInterval;
    }

    public void tickCooldown(double deltaTime) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaTime);
    }

    public Projectile createRangedAttack(double originX, double originY,
            Enemy target, double fallbackDirectionX, double fallbackDirectionY) {
        if (!isRangedAttack() || attackCooldown > 0.0) {
            return null;
        }

        double targetX;
        double targetY;
        if (target != null && !target.isDead()) {
            targetX = target.getWorldX();
            targetY = target.getWorldY();
        } else {
            double length = Math.hypot(fallbackDirectionX, fallbackDirectionY);
            if (length <= 0.0001) {
                fallbackDirectionX = 1.0;
                fallbackDirectionY = 0.0;
                length = 1.0;
            }
            targetX = originX + fallbackDirectionX / length * attackRange;
            targetY = originY + fallbackDirectionY / length * attackRange;
        }

        attackCooldown = fireInterval;
        return createProjectile(originX, originY, targetX, targetY);
    }

    public Enemy createMeleeAttack(double originX, double originY, Enemy target) {
        if (isRangedAttack() || attackCooldown > 0.0 || target == null || target.isDead()) {
            return null;
        }

        double differenceX = target.getWorldX() - originX;
        double differenceY = target.getWorldY() - originY;
        double hitRange = attackRange + target.getCollisionRadius();
        if (differenceX * differenceX + differenceY * differenceY > hitRange * hitRange) {
            return null;
        }

        attackCooldown = fireInterval;
        return target;
    }

    public Enemy updateMeleeAttack(double deltaTime, double originX, double originY,
            Enemy target) {
        tickCooldown(deltaTime);
        return createMeleeAttack(originX, originY, target);
    }

    public Projectile updateRangedAttack(double deltaTime, double originX, double originY,
            Enemy target, double fallbackDirectionX, double fallbackDirectionY) {
        tickCooldown(deltaTime);
        return createRangedAttack(originX, originY, target, fallbackDirectionX, fallbackDirectionY);
    }

    /* Legacy update path is kept for Weapon compatibility. GameLogic uses the
       melee/ranged helpers above so sword users do not throw their weapon. */
    public Projectile updateLegacy(double deltaTime, double originX, double originY,
            Enemy target, double fallbackDirectionX, double fallbackDirectionY) {
        tickCooldown(deltaTime);
        return null;
    }

    @Override
    protected Projectile createProjectile(double originX, double originY, Enemy target) {
        return createProjectile(originX, originY, target.getWorldX(), target.getWorldY());
    }

    @Override
    protected Projectile createProjectile(double originX, double originY,
            double targetX, double targetY) {
        return new Projectile(originX, originY, targetX, targetY,
                projectileSpeed, projectileDamage, PROJECTILE_RADIUS,
                attackSprite, 0.0, maxDrawSize, damageElement);
    }

    public void setAttackSprite(BufferedImage sprite, String weaponStyle) {
        this.weaponStyle = weaponStyle == null ? "sword_shield" : weaponStyle;
        if ("daggers".equals(this.weaponStyle)) {
            attackRange = 62.0;
            attackSprite = PROJECTILE_SPRITE;
            maxDrawSize = 42.0;
            damageElement = DamageElement.PHYSICAL;
        } else if ("holy_staff".equals(this.weaponStyle)) {
            attackRange = 300.0;
            attackSprite = HOLY_PROJECTILE_SPRITE;
            maxDrawSize = 36.0;
            damageElement = DamageElement.HOLY;
        } else if ("elemental_staff".equals(this.weaponStyle) || "staff".equals(this.weaponStyle)) {
            attackRange = 310.0;
            attackSprite = ELEMENTAL_PROJECTILE_SPRITE;
            maxDrawSize = 38.0;
            damageElement = DamageElement.FIRE;
        } else {
            attackRange = 78.0;
            attackSprite = PROJECTILE_SPRITE;
            maxDrawSize = 42.0;
            damageElement = DamageElement.PHYSICAL;
        }
    }

    public double getAttackRange() {
        return attackRange;
    }

    public double getSwingDuration() {
        if ("daggers".equals(weaponStyle)) {
            return 0.30;
        }
        if ("holy_staff".equals(weaponStyle) || "elemental_staff".equals(weaponStyle)
                || "staff".equals(weaponStyle)) {
            return 0.38;
        }
        return 0.42;
    }

    private static BufferedImage loadProjectileSprite() {
        return ResourceLoader.loadImage("/main/resources/weapons/long_sword.png");
    }
}
