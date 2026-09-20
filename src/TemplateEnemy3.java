import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class TemplateEnemy3 extends Enemy {
    private static final double SPEED = 48.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 30.0;
    private static final double MAX_HEALTH = 12.0;
    private static final int REFLECT_EVERY_HITS = 4;
    private static final double REFLECTED_PROJECTILE_SPEED = 420.0;
    private static final double REFLECTED_PROJECTILE_DAMAGE = 1.0;
    private static final double REFLECTED_PROJECTILE_RADIUS = 7.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private static final BufferedImage REFLECTED_PROJECTILE_SPRITE = loadReflectedProjectileSprite();

    private int playerProjectileHits;
    private boolean summonedMinions;

    public TemplateEnemy3(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
            FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
    }

    public int getReflectEveryHits() {
        return REFLECT_EVERY_HITS;
    }

    public double getMaxHealth() {
        return MAX_HEALTH;
    }

    public void registerPlayerProjectileHit() {
        playerProjectileHits++;
    }

    public boolean shouldReflectPlayerProjectile(double playerX, double playerY) {
        double differenceX = playerX - getWorldX();
        double differenceY = playerY - getWorldY();
        double distanceSquared = differenceX * differenceX + differenceY * differenceY;
        double reflectRange = 220.0;
        return playerProjectileHits > 0
                && playerProjectileHits % REFLECT_EVERY_HITS == 0
                && distanceSquared <= reflectRange * reflectRange;
    }

    public boolean hasSummonedMinions() {
        return summonedMinions;
    }

    public List<Enemy> createSummons(double worldX, double worldY, Random random) {
        if (summonedMinions) {
            return List.of();
        }

        summonedMinions = true;
        final int summonCount = 3;
        List<Enemy> minions = new ArrayList<>();
        for (int index = 0; index < summonCount; index++) {
            double angle = (Math.PI * 2.0 * index / summonCount) + random.nextDouble() * 1.2;
            double offset = 42.0 + random.nextDouble() * 18.0;
            double summonX = worldX + Math.cos(angle) * offset;
            double summonY = worldY + Math.sin(angle) * offset;
            minions.add(new TemplateEnemyMinion(summonX, summonY));
        }
        return minions;
    }

    public Projectile createReflectedProjectile(double playerX, double playerY) {
        if (isDead()) {
            return null;
        }

        Projectile reflectedProjectile = new Projectile(
                getWorldX(), getWorldY(), playerX, playerY,
                REFLECTED_PROJECTILE_SPEED, REFLECTED_PROJECTILE_DAMAGE,
                REFLECTED_PROJECTILE_RADIUS, REFLECTED_PROJECTILE_SPRITE);
        reflectedProjectile.setOwner(this);
        return reflectedProjectile;
    }

    private static BufferedImage loadSpriteSheet() {
        try {
            return ImageIO.read(
                    TemplateEnemy3.class.getResource("/assets/enemy/LVL3Walk.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/enemy/LVL3Walk.png", exception);
        }
    }

    private static BufferedImage loadDeathSheet() {
        try {
            return ImageIO.read(
                    TemplateEnemy3.class.getResource("/assets/enemy/LVL3Death.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/enemy/LVL3Death.png", exception);
        }
    }

    private static BufferedImage loadReflectedProjectileSprite() {
        try {
            return ImageIO.read(
                    TemplateEnemy3.class.getResource("/assets/projectiles/temp_bullet.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load assets/projectiles/temp_bullet.png", exception);
        }
    }
}
