import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Gem {
    private static final double GEM_SIZE = 16.0;
    private static final double PULL_RADIUS = 50.0;
    private static final double COLLECTION_RADIUS = 14.0;
    private static final double INITIAL_OUTWARD_SPEED = 165.0;
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

    public Gem(double worldX, double worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public void update(double deltaTime, double playerX, double playerY) {
        update(deltaTime, playerX, playerY, PULL_RADIUS);
    }

    public void update(double deltaTime, double playerX, double playerY, double pickupRadius) {
        bobTime += deltaTime;
        double differenceX = playerX - worldX;
        double differenceY = playerY - worldY;
        double distanceSquared = differenceX * differenceX + differenceY * differenceY;
        boolean outwardImpulseApplied = false;

        if (distanceSquared > 0.0001) {
            double distance = Math.sqrt(distanceSquared);
            if (!attractionStarted && distance <= pickupRadius) {
                double directionX = differenceX / distance;
                double directionY = differenceY / distance;
                velocityX -= directionX * INITIAL_OUTWARD_SPEED;
                velocityY -= directionY * INITIAL_OUTWARD_SPEED;
                attractionStarted = true;
                outwardImpulseApplied = true;
            }
        }

        if (attractionStarted && !outwardImpulseApplied && distanceSquared > 0.0001) {
            double distance = Math.sqrt(distanceSquared);
            double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            double newSpeed = Math.min(MAX_SPEED, speed + PLAYER_ACCELERATION * deltaTime);
            velocityX = differenceX / distance * newSpeed;
            velocityY = differenceY / distance * newSpeed;
        }

        worldX += velocityX * deltaTime;
        worldY += velocityY * deltaTime;

        double collectionX = playerX - worldX;
        double collectionY = playerY - worldY;
        if (collectionX * collectionX + collectionY * collectionY
                <= COLLECTION_RADIUS * COLLECTION_RADIUS) {
            collected = true;
        }
    }

    public boolean isCollected() {
        return collected;
    }

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
