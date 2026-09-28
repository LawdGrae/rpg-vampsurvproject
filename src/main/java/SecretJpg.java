import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SecretJpg {
    private static final int DRAW_SIZE = 64;
    private static final double VISIBLE_TIME = 5.0;
    private static final double HIDDEN_TIME = 5.0;
    private static final BufferedImage SPRITE = loadSprite();

    private double worldX;
    private double worldY;
    private final double bobOffset;
    private double visibilityTimer;
    private boolean visible = true;
    private double moveX;
    private double moveY;
    private double patrolTimer;
    private final double patrolSpeed = 42.0;
    private final double patrolRadius = 180.0;
    private double anchorX;
    private double anchorY;

    public SecretJpg(double worldX, double worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.anchorX = worldX;
        this.anchorY = worldY;
        this.bobOffset = Math.random() * 8.0;
        this.visibilityTimer = VISIBLE_TIME;
        chooseRandomDirection();
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

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
            double angle = Math.random() * Math.PI * 2.0;
            double distance = 100.0 + Math.random() * 220.0;
            anchorX = worldX + Math.cos(angle) * distance;
            anchorY = worldY + Math.sin(angle) * distance;
            chooseRandomDirection();
        }

        if (visible) {
            patrolTimer -= deltaTime;
            worldX += moveX * deltaTime;
            worldY += moveY * deltaTime;

            double differenceX = worldX - anchorX;
            double differenceY = worldY - anchorY;
            double distanceFromAnchor = Math.hypot(differenceX, differenceY);
            if (distanceFromAnchor > patrolRadius || patrolTimer <= 0.0) {
                double angle = Math.atan2(differenceY, differenceX) + Math.PI;
                moveX = Math.cos(angle) * patrolSpeed;
                moveY = Math.sin(angle) * patrolSpeed;
                patrolTimer = 1.2 + Math.random() * 1.8;
            }
        }
    }

    private void chooseRandomDirection() {
        double angle = Math.random() * Math.PI * 2.0;
        moveX = Math.cos(angle) * patrolSpeed;
        moveY = Math.sin(angle) * patrolSpeed;
        patrolTimer = 1.2 + Math.random() * 1.8;
    }

    public boolean isVisible() {
        return visible;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (!visible) {
            return;
        }

        int screenX = (int) (centerX + worldX + cameraX - DRAW_SIZE / 2.0);
        int screenY = (int) (centerY + worldY + cameraY - DRAW_SIZE / 2.0 + bobOffset);
        graphics.drawImage(SPRITE, screenX, screenY, DRAW_SIZE, DRAW_SIZE, null);
    }

    private static BufferedImage loadSprite() {
        return ResourceLoader.loadImage("/main/resources/enemy/SECRET.jpg");
    }
}
