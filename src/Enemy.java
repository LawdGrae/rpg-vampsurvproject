import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class Enemy {
    protected final double speed;
    protected final double animationSpeed;
    protected final int frameWidth;
    protected final int frameHeight;
    protected final int renderSize;
    protected final double collisionRadius;
    protected final double damage;
    protected final double maxHealth;
    protected final BufferedImage spriteSheet;

    private double worldX;
    private double worldY;
    private double animationTime;
    private boolean facingLeft;
    private double health;
    private double fadeTime;

    private static final double FADE_DURATION = 0.5;

    protected Enemy(double worldX, double worldY, BufferedImage spriteSheet,
            double speed, double animationSpeed, int frameWidth, int frameHeight,
            int renderSize, double collisionRadius, double damage, double maxHealth) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.spriteSheet = spriteSheet;
        this.speed = speed;
        this.animationSpeed = animationSpeed;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.renderSize = renderSize;
        this.collisionRadius = collisionRadius;
        this.damage = damage;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            fadeTime += deltaTime;
            return;
        }

        animationTime += deltaTime;

        // Build a vector from this enemy to the target player.
        double differenceX = targetWorldX - worldX;
        double differenceY = targetWorldY - worldY;
        facingLeft = differenceX < 0;
        double distanceSquared = differenceX * differenceX + differenceY * differenceY;
        double minimumDistance = targetCollisionRadius + collisionRadius;

        // Stay outside the collision distance while moving toward the player.
        if (distanceSquared > minimumDistance * minimumDistance) {
            double distance = Math.sqrt(distanceSquared);
            // Dividing by distance turns the vector into a direction of length 1.
            worldX += differenceX / distance * speed * deltaTime;
            worldY += differenceY / distance * speed * deltaTime;
        }
    }

    public boolean isCollidingWith(double targetX, double targetY,
            double targetCollisionRadius) {
        double differenceX = worldX - targetX;
        double differenceY = worldY - targetY;
        double distanceSquared = differenceX * differenceX + differenceY * differenceY;
        double minimumDistance = collisionRadius + targetCollisionRadius;

        return distanceSquared <= minimumDistance * minimumDistance;
    }

    public void takeDamage(double damage) {
        if (!isDead()) {
            health = Math.max(0, health - damage);
        }
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isFinishedFading() {
        return isDead() && fadeTime >= FADE_DURATION;
    }

    public double distanceSquaredTo(double targetX, double targetY) {
        double differenceX = worldX - targetX;
        double differenceY = worldY - targetY;
        return differenceX * differenceX + differenceY * differenceY;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public double getCollisionRadius() {
        return collisionRadius;
    }

    public double getDamage() {
        return damage;
    }

    public void teleportTo(double worldX, double worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public void separateFrom(Enemy other) {
        double differenceX = worldX - other.worldX;
        double differenceY = worldY - other.worldY;
        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);
        double minimumDistance = collisionRadius + other.collisionRadius;

        if (distance < minimumDistance) {
            // Split the overlap correction between both enemies.
            if (distance == 0) {
                differenceX = 1;
                differenceY = 0;
                distance = 1;
            }
            double pushDistance = (minimumDistance - distance) / 2.0;
            double directionX = differenceX / distance;
            double directionY = differenceY / distance;
            worldX += directionX * pushDistance;
            worldY += directionY * pushDistance;
            other.worldX -= directionX * pushDistance;
            other.worldY -= directionY * pushDistance;
        }
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (isFinishedFading()) {
            return;
        }

        // Convert world coordinates into screen coordinates using the camera offset.
        int screenX = (int) (centerX + worldX + cameraX - renderSize / 2.0);
        int screenY = (int) (centerY + worldY + cameraY - renderSize / 2.0);
        int animationFrame = (int) (animationTime * animationSpeed) % 4;
        int sourceX = animationFrame * frameWidth;

        // The four enemy frames are arranged horizontally in one row.
        int rightEdge = screenX + renderSize;
        int leftEdge = screenX;
        if (facingLeft) {
            int temporaryEdge = leftEdge;
            leftEdge = rightEdge;
            rightEdge = temporaryEdge;
        }

        Composite oldComposite = graphics.getComposite();
        if (isDead()) {
            float alpha = (float) Math.max(0, 1.0 - fadeTime / FADE_DURATION);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        graphics.drawImage(spriteSheet,
            leftEdge, screenY, rightEdge, screenY + renderSize,
            sourceX, 0, sourceX + frameWidth, frameHeight, null);
        graphics.setComposite(oldComposite);
    }

    public void drawCollisionArea(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (isDead()) {
            return;
        }
        int diameter = (int) (collisionRadius * 2.0);
        int screenCenterX = (int) (centerX + worldX + cameraX);
        int screenCenterY = (int) (centerY + worldY + cameraY);
        int circleX = screenCenterX - diameter / 2;
        int circleY = screenCenterY - diameter / 2;
        graphics.setColor(new Color(255, 0, 0, 150));
        graphics.fillOval(circleX, circleY, diameter, diameter);
    }
}