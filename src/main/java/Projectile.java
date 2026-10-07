import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
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
    private final double maxDrawSize;
    private final DamageElement damageElement;
    private double worldX;
    private double worldY;
    private double previousWorldX;
    private double previousWorldY;
    private final double velocityX;
    private final double velocityY;
    private double lifetime;
    private double animationTime;
    private Enemy owner;
    private double splashRadius;

    public void setSplashRadius(double splashRadius) {
        this.splashRadius = Math.max(0.0, splashRadius);
    }

    public double getSplashRadius() {
        return splashRadius;
    }

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite) {
        this(worldX, worldY, targetX, targetY, speed, damage, radius, sprite, 0.0);
    }

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite,
            double animationSpeed) {
        this(worldX, worldY, targetX, targetY, speed, damage, radius, sprite,
                animationSpeed, 0.0);
    }

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite,
            double animationSpeed, double maxDrawSize) {
        this(worldX, worldY, targetX, targetY, speed, damage, radius, sprite,
                animationSpeed, maxDrawSize, DamageElement.PHYSICAL);
    }

    public Projectile(double worldX, double worldY, double targetX, double targetY,
            double speed, double damage, double radius, BufferedImage sprite,
            double animationSpeed, double maxDrawSize, DamageElement damageElement) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.previousWorldX = worldX;
        this.previousWorldY = worldY;
        this.speed = speed;
        this.damage = damage;
        this.radius = radius;
        this.sprite = sprite;
        this.animationSpeed = animationSpeed;
        this.maxDrawSize = maxDrawSize;
        this.damageElement = damageElement;

        int calculatedFrameWidth = sprite.getWidth();
        if (animationSpeed > 0.0 && sprite.getWidth() > sprite.getHeight()) {
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
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) return;
        previousWorldX = worldX;
        previousWorldY = worldY;
        double step = Math.min(deltaTime, Math.max(0.0, MAX_LIFETIME - lifetime));
        lifetime += step;
        animationTime += step;
        worldX += velocityX * speed * step;
        worldY += velocityY * speed * step;
    }

    public boolean isExpired() {
        return lifetime >= MAX_LIFETIME;
    }

    public boolean hits(Enemy enemy) {
        return !enemy.isDead() && Double.isFinite(getCollisionFraction(enemy));
    }

    public boolean hitsPlayer(double playerX, double playerY, double playerCollisionRadius) {
        return Double.isFinite(getPlayerCollisionFraction(playerX, playerY, playerCollisionRadius));
    }

    /** Earliest swept contact in the latest update, or infinity when the segment misses. */
    public double getCollisionFraction(Enemy enemy) {
        return enemy.isDead() ? Double.POSITIVE_INFINITY
                : collisionFraction(enemy.getWorldX(), enemy.getWorldY(), radius + enemy.getCollisionRadius());
    }

    public double getPlayerCollisionFraction(double playerX, double playerY, double playerCollisionRadius) {
        return collisionFraction(playerX, playerY, radius + playerCollisionRadius);
    }

    public double getContactWorldX(double fraction) {
        return previousWorldX + (worldX - previousWorldX) * Math.max(0.0, Math.min(1.0, fraction));
    }

    public double getContactWorldY(double fraction) {
        return previousWorldY + (worldY - previousWorldY) * Math.max(0.0, Math.min(1.0, fraction));
    }

    private double collisionFraction(double targetX, double targetY, double hitRadius) {
        double offsetX = previousWorldX - targetX;
        double offsetY = previousWorldY - targetY;
        double c = offsetX * offsetX + offsetY * offsetY - hitRadius * hitRadius;
        if (c <= 0.0) return 0.0;
        double dx = worldX - previousWorldX;
        double dy = worldY - previousWorldY;
        double a = dx * dx + dy * dy;
        if (a <= 0.000000001) return Double.POSITIVE_INFINITY;
        double b = 2.0 * (offsetX * dx + offsetY * dy);
        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) return Double.POSITIVE_INFINITY;
        double fraction = (-b - Math.sqrt(discriminant)) / (2.0 * a);
        return fraction >= 0.0 && fraction <= 1.0 ? fraction : Double.POSITIVE_INFINITY;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double screenCenterX = centerX + worldX + cameraX;
        double screenCenterY = centerY + worldY + cameraY;
        double angle = Math.atan2(velocityY, velocityX);
        boolean staffMote = owner == null && frameCount == 1 && maxDrawSize > 0.0
                && maxDrawSize <= 28.0
                && (damageElement == DamageElement.HOLY || damageElement == DamageElement.LIGHTNING);
        if (staffMote) {
            drawMagicTrail(graphics, screenCenterX, screenCenterY);
        }

        int sourceX = 0;
        int drawWidth = sprite.getWidth();
        int drawHeight = sprite.getHeight();

        if (frameCount > 1) {
            int frameIndex = (int) (animationTime * animationSpeed) % frameCount;
            sourceX = frameIndex * frameWidth;
            drawWidth = frameWidth;
        }

        int renderWidth = drawWidth;
        int renderHeight = drawHeight;
        if (maxDrawSize > 0.0 && Math.max(renderWidth, renderHeight) > maxDrawSize) {
            double scale = maxDrawSize / Math.max(renderWidth, renderHeight);
            renderWidth = Math.max(1, (int) Math.round(renderWidth * scale));
            renderHeight = Math.max(1, (int) Math.round(renderHeight * scale));
        }

        Graphics2D rotatedGraphics = (Graphics2D) graphics.create();
        rotatedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        rotatedGraphics.translate(screenCenterX, screenCenterY);
        rotatedGraphics.rotate(angle);
        if (staffMote) {
            double pulse = 0.98 + 0.045 * Math.sin(lifetime * 18.0);
            rotatedGraphics.scale(pulse, pulse);
        }
        rotatedGraphics.translate(-renderWidth / 2.0, -renderHeight / 2.0);

        if (frameCount > 1) {
            rotatedGraphics.drawImage(sprite,
                    0, 0, renderWidth, renderHeight,
                    sourceX, 0, sourceX + drawWidth, drawHeight, null);
        } else {
            rotatedGraphics.drawImage(sprite, 0, 0, renderWidth, renderHeight, null);
        }
        rotatedGraphics.dispose();
    }

    private void drawMagicTrail(Graphics2D graphics, double x, double y) {
        double length = Math.min(44.0, lifetime * speed);
        if (length <= 1.0) {
            return;
        }
        Color tint = damageElement == DamageElement.HOLY
                ? new Color(255, 218, 115) : new Color(160, 145, 255);
        Graphics2D trail = (Graphics2D) graphics.create();
        trail.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int index = 7; index >= 0; index--) {
            double from = index / 8.0;
            double to = (index + 1) / 8.0;
            double wave = Math.sin(lifetime * 17.0 - index * 0.6) * from * 1.2;
            Path2D segment = new Path2D.Double();
            segment.moveTo(x - velocityX * length * to - velocityY * wave,
                    y - velocityY * length * to + velocityX * wave);
            segment.lineTo(x - velocityX * length * from, y - velocityY * length * from);
            int alpha = (int) Math.round(65.0 * (1.0 - from) * (1.0 - from));
            trail.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha));
            trail.setStroke(new BasicStroke((float) (1.0 + 6.0 * (1.0 - from)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trail.draw(segment);
            trail.setColor(new Color(255, 250, 225, alpha));
            trail.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trail.draw(segment);
        }
        trail.dispose();
    }

    public double getDamage() {
        return damage;
    }

    public DamageElement getDamageElement() {
        return damageElement;
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
