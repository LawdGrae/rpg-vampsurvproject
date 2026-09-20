import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Gem {
    private static final double GEM_SIZE = 16.0;
    private static final double PULL_RADIUS = 50.0;
    private static final double COLLECTION_RADIUS = 14.0;
    private static final BufferedImage GEM_SPRITE = loadSprite();
    private static final int VALUE = 1;

    private double worldX;
    private double worldY;
    private double velocityX;
    private double velocityY;
    private double bobTime;
    private boolean collected;

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

        if (distanceSquared > 0.0001) {
            double distance = Math.sqrt(distanceSquared);
            if (distance <= pickupRadius) {
                double directionX = differenceX / distance;
                double directionY = differenceY / distance;
                double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
                double newSpeed = speed + 12.0;
                velocityX = directionX * newSpeed;
                velocityY = directionY * newSpeed;

                double speedLimit = 1600.0;
                double maxSpeed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
                if (maxSpeed > speedLimit) {
                    velocityX = (velocityX / maxSpeed) * speedLimit;
                    velocityY = (velocityY / maxSpeed) * speedLimit;
                }
            }
        }

        worldX += velocityX * deltaTime;
        worldY += velocityY * deltaTime;

        if (differenceX * differenceX + differenceY * differenceY <= COLLECTION_RADIUS * COLLECTION_RADIUS) {
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
        try {
            return ImageIO.read(Gem.class.getResource("/assets/gem.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load assets/gem.png", exception);
        }
    }
}
