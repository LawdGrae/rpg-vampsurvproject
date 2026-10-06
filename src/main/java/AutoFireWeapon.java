import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class AutoFireWeapon extends Weapon {
    private static final double FIRE_INTERVAL = 0.7;
    private static final double PROJECTILE_SPEED = 450.0;
    private static final double PROJECTILE_DAMAGE = 1.0;
    private static final double PROJECTILE_RADIUS = 16.0;
    private static final BufferedImage PROJECTILE_SPRITE = loadProjectileSprite();
    private static final BufferedImage HOLY_PROJECTILE_SPRITE = createStaffMote(
            new Color(255, 246, 180), new Color(255, 205, 80));
    private static final BufferedImage ELEMENTAL_PROJECTILE_SPRITE = createStaffMote(
            new Color(150, 225, 255), new Color(150, 85, 255));
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
        boolean wasGuardian = "greatshield".equals(this.weaponStyle);
        this.weaponStyle = weaponStyle == null ? "sword_shield" : weaponStyle;
        boolean guardian = "greatshield".equals(this.weaponStyle);
        if (wasGuardian != guardian) {
            fireInterval = guardian ? 1.25 : FIRE_INTERVAL;
            projectileDamage = guardian ? 12.0 : PROJECTILE_DAMAGE;
        }
        if ("greatshield".equals(this.weaponStyle)) {
            attackRange = 90.0;
            attackSprite = PROJECTILE_SPRITE;
            maxDrawSize = 42.0;
            damageElement = DamageElement.PHYSICAL;
        } else if ("daggers".equals(this.weaponStyle)) {
            attackRange = 62.0;
            attackSprite = PROJECTILE_SPRITE;
            maxDrawSize = 42.0;
            damageElement = DamageElement.PHYSICAL;
        } else if ("holy_staff".equals(this.weaponStyle)) {
            attackRange = 300.0;
            attackSprite = HOLY_PROJECTILE_SPRITE;
            maxDrawSize = 24.0;
            damageElement = DamageElement.HOLY;
        } else if ("elemental_staff".equals(this.weaponStyle) || "staff".equals(this.weaponStyle)) {
            attackRange = 310.0;
            attackSprite = ELEMENTAL_PROJECTILE_SPRITE;
            maxDrawSize = 24.0;
            damageElement = DamageElement.LIGHTNING;
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
        if ("greatshield".equals(weaponStyle)) {
            return 0.65;
        }
        if ("daggers".equals(weaponStyle)) {
            return 0.38;
        }
        if ("holy_staff".equals(weaponStyle) || "elemental_staff".equals(weaponStyle)
                || "staff".equals(weaponStyle)) {
            return 0.38;
        }
        return 0.42;
    }

    public double getImpactProgress() {
        return "greatshield".equals(weaponStyle) ? 0.52 : 0.43;
    }

    private static BufferedImage loadProjectileSprite() {
        return ResourceLoader.loadImage("/main/resources/weapons/long_sword.png");
    }

    private static BufferedImage createStaffMote(Color core, Color glow) {
        int size = 28;
        int center = size / 2;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        graphics.setPaint(new RadialGradientPaint(center, center, 13.0f,
                new float[] {0.0f, 0.2f, 0.48f, 1.0f},
                new Color[] {new Color(255, 255, 255, 245),
                        new Color(core.getRed(), core.getGreen(), core.getBlue(), 230),
                        new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 95),
                        new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
        graphics.fillOval(1, 1, 26, 26);
        graphics.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(255, 255, 255, 155));
        graphics.drawLine(center - 4, center, center + 4, center);
        graphics.drawLine(center, center - 4, center, center + 4);
        graphics.dispose();
        return image;
    }
}
