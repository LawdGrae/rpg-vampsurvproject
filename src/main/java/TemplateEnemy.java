import java.awt.image.BufferedImage;

public class TemplateEnemy extends Enemy {
    private static final double SPEED = 50.0;
    private static final double ANIMATION_SPEED = 4.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 64;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 20.0;
    private static final double MAX_HEALTH = 5.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();

    public TemplateEnemy(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
            FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
    }

    private static BufferedImage loadSpriteSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/LVL1Walk.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/LVL1Death.png");
    }
}
