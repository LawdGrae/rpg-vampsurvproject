import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GreenFinalMinion extends Enemy {
    private static final double SPEED = 62.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 18.0;
    private static final double MAX_HEALTH = 30.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();
    private static final double HEAL_INTERVAL = 4.0;
    private double healCooldown = HEAL_INTERVAL;

    public GreenFinalMinion(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
            SPRITE_SHEET.getWidth(), SPRITE_SHEET.getHeight(), RENDER_SIZE,
            COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
    }

    public double getHealAmount() {
        return getDamage();
    }

    public boolean canHeal() {
        return !isDead() && healCooldown <= 0.0;
    }

    public void resetHealCooldown() {
        healCooldown = HEAL_INTERVAL;
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
            return;
        }
        healCooldown = Math.max(0.0, healCooldown - deltaTime);
        super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
    }

    private static BufferedImage loadSpriteSheet() {
        return prepareMinionSprite(ResourceLoader.loadImage("/main/resources/enemy/GREENMINION.png"));
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
