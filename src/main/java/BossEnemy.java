import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BossEnemy extends Enemy {
    private static final double SPEED = 58.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 96;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 0.0;
    private static final double MAX_HEALTH = 220.0;
    private static final double SUMMON_INTERVAL = 4.5;
    private static final double SUMMON_RANGE = 150.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();

    private double summonCooldown;

    public BossEnemy(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
                FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
        summonCooldown = 1.25;
    }

    public double getMaxHealth() {
        return MAX_HEALTH;
    }

    public boolean shouldSummon() {
        return summonCooldown <= 0.0 && !isDead();
    }

    public void resetSummonCooldown() {
        summonCooldown = SUMMON_INTERVAL;
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            return;
        }

        summonCooldown -= deltaTime;
        super.update(deltaTime, targetWorldX, targetWorldY, targetCollisionRadius);
    }

    public List<Enemy> createSummons(double worldX, double worldY, Random random) {
        List<Enemy> summoned = new ArrayList<>();
        int summonCount = 3;
        for (int index = 0; index < summonCount; index++) {
            double angle = (Math.PI * 2.0 * index / summonCount) + random.nextDouble() * 0.8;
            double summonX = worldX + Math.cos(angle) * 42.0;
            double summonY = worldY + Math.sin(angle) * 42.0;
            summoned.add(new TemplateEnemyMinion(summonX, summonY));
        }
        return summoned;
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
        drawBossBar(graphics, centerX, centerY, cameraX, cameraY);
    }

    private void drawBossBar(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (isDead()) {
            return;
        }

        int barWidth = 120;
        int barHeight = 10;
        int x = (int) (centerX + getWorldX() + cameraX - barWidth / 2.0);
        int y = (int) (centerY + getWorldY() + cameraY - RENDER_SIZE / 2.0 - 18.0);

        graphics.setColor(new Color(30, 30, 30, 180));
        graphics.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

        int currentWidth = (int) Math.round(barWidth * (getHealthRatio()));
        graphics.setColor(new Color(255, 70, 80));
        graphics.fillRoundRect(x, y, currentWidth, barHeight, 8, 8);

        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRoundRect(x, y, barWidth, barHeight, 8, 8);
    }

    private static BufferedImage loadSpriteSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/BOSS.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/BOSS.png");
    }
}
