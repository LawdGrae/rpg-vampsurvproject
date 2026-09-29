import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BlueFinalMinion extends Enemy {
    private static final double SPEED = 26.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 0.0;
    private static final double MAX_HEALTH = 25.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private double shieldPulseCooldown = 5.0;

    public BlueFinalMinion(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
            SPRITE_SHEET.getWidth(), SPRITE_SHEET.getHeight(), RENDER_SIZE,
            COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
            return;
        }
        shieldPulseCooldown = Math.max(0.0, shieldPulseCooldown - deltaTime);
        super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
    }

    public double getShieldValue() {
        return 12.0;
    }

    public boolean canRestoreShield() {
        return !isDead() && shieldPulseCooldown <= 0.0;
    }

    public void resetShieldPulseCooldown() {
        shieldPulseCooldown = 5.0;
    }

    public void explodeOnDeath() {
        if (!isDead()) {
            takeDamage(Double.MAX_VALUE);
        }
    }

    private static BufferedImage loadSpriteSheet() {
        return prepareMinionSprite(ResourceLoader.loadImage("/main/resources/enemy/BLUEMINION.png"));
    }

    private static BufferedImage loadDeathSheet() {
        return SPRITE_SHEET;
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
    }
}
