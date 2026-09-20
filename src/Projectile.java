import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Projectile {
    private static final double MAX_LIFETIME = 3.0;
    private final double damage;
    private final double speed;
    private final BufferedImage sprite;
    private final double radius;
    private final int frameCount;
    private final int frameWidth;
    private final double animationSpeed;
    private double worldX;
    private double worldY;
    private final double velocityX;
    private final double velocityY;
    private double lifetime;
    private double animationTime;
    private Enemy owner;

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite) {
        this(worldX, worldY, targetX, targetY, speed, damage, radius, sprite, 0.0);
    }

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite,
            double animationSpeed) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = speed;
        this.damage = damage;
        this.radius = radius;
        this.sprite = sprite;
        this.animationSpeed = animationSpeed;

        int calculatedFrameWidth = sprite.getWidth();
        if (sprite.getWidth() > sprite.getHeight()) {
            int maxFrames = sprite.getWidth() / Math.max(1, sprite.getHeight());
            if (maxFrames > 1) {
                calculatedFrameWidth = sprite.getWidth() / maxFrames;
            }
        }
        this.frameWidth = Math.max(1, calculatedFrameWidth);
        this.frameCount = Math.max(1, sprite.getWidth() / frameWidth);

        double differenceX = targetX - worldX;
        double differenceY = targetY - worldY;
        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);
        velocityX = distance == 0 ? 0 : differenceX / distance;
        velocityY = distance == 0 ? 0 : differenceY / distance;
    }

    public void update(double deltaTime) {
        lifetime += deltaTime;
        animationTime += deltaTime;
        worldX += velocityX * speed * deltaTime;
        worldY += velocityY * speed * deltaTime;
    }

    public boolean isExpired() {
        return lifetime >= MAX_LIFETIME;
    }

    public boolean hits(Enemy enemy) {
        double differenceX = enemy.getWorldX() - worldX;
        double differenceY = enemy.getWorldY() - worldY;
        double hitDistance = radius + enemy.getCollisionRadius();
        return differenceX * differenceX + differenceY * differenceY
                <= hitDistance * hitDistance;
    }

    public boolean hitsPlayer(double playerX, double playerY, double playerCollisionRadius) {
        double differenceX = playerX - worldX;
        double differenceY = playerY - worldY;
        double hitDistance = radius + playerCollisionRadius;
        return differenceX * differenceX + differenceY * differenceY <= hitDistance * hitDistance;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double screenCenterX = centerX + worldX + cameraX;
        double screenCenterY = centerY + worldY + cameraY;
        double angle = Math.atan2(velocityY, velocityX);

        int sourceX = 0;
        int drawWidth = sprite.getWidth();
        int drawHeight = sprite.getHeight();

        if (frameCount > 1) {
            int frameIndex = (int) (animationTime * animationSpeed) % frameCount;
            sourceX = frameIndex * frameWidth;
            drawWidth = frameWidth;
        }

        Graphics2D rotatedGraphics = (Graphics2D) graphics.create();
        rotatedGraphics.translate(screenCenterX, screenCenterY);
        rotatedGraphics.rotate(angle);
        rotatedGraphics.translate(-drawWidth / 2.0, -drawHeight / 2.0);

        if (frameCount > 1) {
            rotatedGraphics.drawImage(sprite,
                    0, 0, drawWidth, drawHeight,
                    sourceX, 0, sourceX + drawWidth, drawHeight, null);
        } else {
            rotatedGraphics.drawImage(sprite, 0, 0, drawWidth, drawHeight, null);
        }
        rotatedGraphics.dispose();
    }

    public double getDamage() {
        return damage;
    }

    public Enemy getOwner() {
        return owner;
    }

    public void setOwner(Enemy owner) {
        this.owner = owner;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }
}