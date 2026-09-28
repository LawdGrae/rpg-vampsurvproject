import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SecretJpg2 extends SecretJpg {
    private static final int DRAW_SIZE = 72;
    private static final double VISIBLE_TIME = 4.5;
    private static final double HIDDEN_TIME = 6.0;
    private static final BufferedImage SPRITE = loadSprite();

    private double worldX;
    private double worldY;
    private double visibilityTimer;
    private boolean visible = true;
    private double moveX;
    private double moveY;
    private double patrolTimer;
    private final double patrolSpeed = 32.0;
    private final double patrolRadius = 210.0;
    private Enemy coverTarget;

    public SecretJpg2(double worldX, double worldY) {
        super(worldX, worldY);
        this.worldX = worldX;
        this.worldY = worldY;
        this.visibilityTimer = VISIBLE_TIME;
        chooseRandomDirection();
    }

    public void setCoverTarget(Enemy enemy) {
        this.coverTarget = enemy;
    }

    @Override
    public double getWorldX() {
        return worldX;
    }

    @Override
    public double getWorldY() {
        return worldY;
    }

    @Override
    public void update(double deltaTime) {
        visibilityTimer -= deltaTime;
        if (visible && visibilityTimer <= 0.0) {
            visible = false;
            visibilityTimer = HIDDEN_TIME;
            return;
        }

        if (!visible && visibilityTimer <= 0.0) {
            visible = true;
            visibilityTimer = VISIBLE_TIME;
            chooseRandomDirection();
        }

        if (!visible) {
            return;
        }

        if (coverTarget != null && !coverTarget.isDead()) {
            double targetX = coverTarget.getWorldX();
            double targetY = coverTarget.getWorldY();
            worldX += (targetX - worldX) * 0.08;
            worldY += (targetY - worldY) * 0.08;
            if (Math.random() < 0.08) {
                chooseRandomDirection();
            }
            return;
        }

        patrolTimer -= deltaTime;
        worldX += moveX * deltaTime;
        worldY += moveY * deltaTime;

        double distanceFromCenter = Math.hypot(worldX, worldY);
        if (distanceFromCenter > patrolRadius || patrolTimer <= 0.0) {
            chooseRandomDirection();
            patrolTimer = 1.0 + Math.random() * 2.0;
        }
    }

    private void chooseRandomDirection() {
        double angle = Math.random() * Math.PI * 2.0;
        moveX = Math.cos(angle) * patrolSpeed;
        moveY = Math.sin(angle) * patrolSpeed;
        patrolTimer = 1.0 + Math.random() * 2.0;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (!visible) {
            return;
        }

        int screenX = (int) (centerX + worldX + cameraX - DRAW_SIZE / 2.0);
        int screenY = (int) (centerY + worldY + cameraY - DRAW_SIZE / 2.0);
        graphics.drawImage(SPRITE, screenX, screenY, DRAW_SIZE, DRAW_SIZE, null);
    }

    private static BufferedImage loadSprite() {
        return ResourceLoader.loadImage("/main/resources/enemy/SECRET2.jpg");
    }
}
