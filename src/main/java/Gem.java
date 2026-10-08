import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Gem {
    private static final double GEM_SIZE = 16.0;
    private static final double PULL_RADIUS = 50.0;
    private static final double COLLECTION_RADIUS = 14.0;
    private static final double INITIAL_OUTWARD_SPEED = 165.0;
    private static final double OUTWARD_IMPULSE_DURATION = 1.0 / 60.0;
    private static final double PLAYER_ACCELERATION = 340.0;
    private static final double MAX_SPEED = 6400.0;
    private static final BufferedImage GEM_SPRITE = loadSprite();
    private static final int VALUE = 1;

    private double worldX;
    private double worldY;
    private double velocityX;
    private double velocityY;
    private double bobTime;
    private boolean collected;
    private boolean attractionStarted;
    private double outwardImpulseTimeRemaining;

    public Gem(double worldX, double worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public void update(double deltaTime, double playerX, double playerY) {
        update(deltaTime, playerX, playerY, PULL_RADIUS);
    }

    public void update(double deltaTime, double playerX, double playerY, double pickupRadius) {
        if (collected || !Double.isFinite(deltaTime) || deltaTime < 0.0
                || !Double.isFinite(playerX) || !Double.isFinite(playerY)) {
            return;
        }
        bobTime += deltaTime;
        double differenceX = playerX - worldX;
        double differenceY = playerY - worldY;
        double distance = Math.hypot(differenceX, differenceY);
        if (distance <= COLLECTION_RADIUS) {
            collectAt(playerX, playerY);
            return;
        }
        if (deltaTime == 0.0) {
            return;
        }

        if (!attractionStarted && Double.isFinite(pickupRadius)
                && distance <= Math.max(0.0, pickupRadius)) {
            velocityX -= differenceX / distance * INITIAL_OUTWARD_SPEED;
            velocityY -= differenceY / distance * INITIAL_OUTWARD_SPEED;
            attractionStarted = true;
            outwardImpulseTimeRemaining = OUTWARD_IMPULSE_DURATION;
        }

        if (attractionStarted) {
            // Keep the initial outward kick the same length at every frame rate.
            if (outwardImpulseTimeRemaining > 0.0) {
                double impulseTime = Math.min(deltaTime, outwardImpulseTimeRemaining);
                if (!moveAndCollect(velocityX * impulseTime, velocityY * impulseTime, playerX, playerY)) {
                    return;
                }
                outwardImpulseTimeRemaining -= impulseTime;
                deltaTime -= impulseTime;
                if (deltaTime <= 0.0) {
                    return;
                }
                differenceX = playerX - worldX;
                differenceY = playerY - worldY;
                distance = Math.hypot(differenceX, differenceY);
            }
            double directionX = differenceX / distance;
            double directionY = differenceY / distance;
            double speed = Math.min(MAX_SPEED, Math.hypot(velocityX, velocityY));
            // Integrate acceleration in seconds, including the part of a frame at the speed cap.
            double accelerationTime = Math.min(deltaTime, (MAX_SPEED - speed) / PLAYER_ACCELERATION);
            double travel = speed * accelerationTime
                    + 0.5 * PLAYER_ACCELERATION * accelerationTime * accelerationTime
                    + MAX_SPEED * (deltaTime - accelerationTime);
            double newSpeed = Math.min(MAX_SPEED, speed + PLAYER_ACCELERATION * deltaTime);
            velocityX = directionX * newSpeed;
            velocityY = directionY * newSpeed;
            if (travel >= distance - COLLECTION_RADIUS) {
                collectAt(playerX, playerY);
                return;
            }
            moveAndCollect(directionX * travel, directionY * travel, playerX, playerY);
        } else {
            moveAndCollect(velocityX * deltaTime, velocityY * deltaTime, playerX, playerY);
        }
    }

    private boolean moveAndCollect(double moveX, double moveY, double playerX, double playerY) {
        if (!Double.isFinite(moveX) || !Double.isFinite(moveY)) {
            return false;
        }
        // Coasting gems must collect when their path crosses the player between frames.
        double differenceX = playerX - worldX;
        double differenceY = playerY - worldY;
        double movementSquared = moveX * moveX + moveY * moveY;
        double closestFraction = movementSquared > 0.0
                ? Math.max(0.0, Math.min(1.0, (differenceX * moveX + differenceY * moveY) / movementSquared))
                : 0.0;
        double closestX = differenceX - moveX * closestFraction;
        double closestY = differenceY - moveY * closestFraction;
        if (Math.hypot(closestX, closestY) <= COLLECTION_RADIUS) {
            collectAt(playerX, playerY);
            return false;
        }
        worldX += moveX;
        worldY += moveY;
        return true;
    }

    private void collectAt(double playerX, double playerY) {
        worldX = playerX;
        worldY = playerY;
        velocityX = 0.0;
        velocityY = 0.0;
        collected = true;
    }

    public boolean isCollected() {
        return collected;
    }

    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }

    public int getValue() {
        return VALUE;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        double bobOffset = Math.sin(bobTime * 2.3) * 2.5;
        double glowStrength = 0.15 + 0.15 * Math.sin(bobTime * 4.0);
        int drawSize = 16;
        int screenX = (int) (centerX + worldX + cameraX - drawSize / 2.0);
        int screenY = (int) (centerY + worldY + cameraY - drawSize / 2.0 + bobOffset);

        graphics.setColor(new Color(255, 255, 255, (int) (20 + glowStrength * 40)));
        graphics.fillOval(screenX - 3, screenY - 3, drawSize + 6, drawSize + 6);

        Graphics2D gemGraphics = (Graphics2D) graphics.create();
        gemGraphics.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1.0f));
        gemGraphics.drawImage(GEM_SPRITE, screenX, screenY, drawSize, drawSize, null);
        gemGraphics.dispose();
    }

    private static BufferedImage loadSprite() {
        return ResourceLoader.loadImage("/main/resources/gem.png");
    }
}
