import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Projectile {
    private static final double MAX_LIFETIME = 3.0;
    private final double damage;
    private final double speed;
    private final BufferedImage sprite;
    private final double radius;
    private double worldX;
    private double worldY;
    private final double velocityX;
    private final double velocityY;
        private double lifetime;

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = speed;
        this.damage = damage;
        this.radius = radius;
        this.sprite = sprite;

        double differenceX = targetX - worldX;
        double differenceY = targetY - worldY;
        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);
        velocityX = distance == 0 ? 0 : differenceX / distance;
        velocityY = distance == 0 ? 0 : differenceY / distance;
    }

    public void update(double deltaTime) {
        lifetime += deltaTime;
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

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double screenCenterX = centerX + worldX + cameraX;
        double screenCenterY = centerY + worldY + cameraY;
        double angle = Math.atan2(velocityY, velocityX);

        // The asset points right, so angle 0 is its default orientation.
        AffineTransform transform = AffineTransform.getTranslateInstance(
            screenCenterX, screenCenterY);
        transform.rotate(angle);
        transform.translate(-sprite.getWidth() / 2.0, -sprite.getHeight() / 2.0);
        graphics.drawImage(sprite, transform, null);
    }

    public double getDamage() {
        return damage;
    }
}