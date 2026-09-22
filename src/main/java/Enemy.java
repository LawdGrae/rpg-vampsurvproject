import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
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
    protected final BufferedImage deathSheet;

    private double worldX;
    private double worldY;
    private double animationTime;
    private boolean facingLeft;
    private double health;
    private double deathTime;
    private boolean lootDropped;
    private double poisonTimer;
    private double poisonDamagePerSecond;
    private double slowTimer;
    private double slowMultiplier = 1.0;
    private double stunTimer;
    private double markTimer;
    private double markDamageMultiplier = 1.0;
    private double hitReactionTimer;
    private double hitDirectionX = 1.0;
    private double hitDirectionY;
    private DamageElement lastHitElement = DamageElement.PHYSICAL;
    private DamageElement deathElement = DamageElement.PHYSICAL;

    private static final double DEATH_DURATION = 1.05;

    protected Enemy(double worldX, double worldY, BufferedImage spriteSheet,
            BufferedImage deathSheet, double speed, double animationSpeed,
            int frameWidth, int frameHeight, int renderSize,
            double collisionRadius, double damage, double maxHealth) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.spriteSheet = spriteSheet;
        this.deathSheet = deathSheet;
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
            deathTime += deltaTime;
            return;
        }

        updateStatusEffects(deltaTime);
        if (isDead()) {
            return;
        }
        if (stunTimer > 0.0) {
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
            double effectiveSpeed = speed * slowMultiplier;
            worldX += differenceX / distance * effectiveSpeed * deltaTime;
            worldY += differenceY / distance * effectiveSpeed * deltaTime;
        }
    }

    public boolean isCollidingWith(double targetX, double targetY,
            double targetCollisionRadius) {
        if (isDead()) {
            return false;
        }

        double differenceX = worldX - targetX;
        double differenceY = worldY - targetY;
        double distanceSquared = differenceX * differenceX + differenceY * differenceY;
        double minimumDistance = collisionRadius + targetCollisionRadius;

        return distanceSquared <= minimumDistance * minimumDistance;
    }

    public void takeDamage(double damage) {
        takeDamage(damage, DamageElement.PHYSICAL, worldX - hitDirectionX, worldY - hitDirectionY);
    }

    public void takeDamage(double damage, DamageElement element, double originX, double originY) {
        if (!isDead()) {
            health = Math.max(0, health - damage * markDamageMultiplier);
            lastHitElement = element;
            hitReactionTimer = 0.18;
            double differenceX = worldX - originX;
            double differenceY = worldY - originY;
            double length = Math.hypot(differenceX, differenceY);
            if (length > 0.0001) {
                hitDirectionX = differenceX / length;
                hitDirectionY = differenceY / length;
            }
            if (isDead()) {
                deathElement = element;
            }
        }
    }

    public void applyPoison(double damagePerSecond, double duration) {
        if (isDead()) {
            return;
        }
        poisonDamagePerSecond = Math.max(poisonDamagePerSecond, damagePerSecond);
        poisonTimer = Math.max(poisonTimer, duration);
    }

    public void applySlow(double multiplier, double duration) {
        if (isDead()) {
            return;
        }
        slowMultiplier = Math.min(slowMultiplier, multiplier);
        slowTimer = Math.max(slowTimer, duration);
    }

    public void applyStun(double duration) {
        if (!isDead()) {
            stunTimer = Math.max(stunTimer, duration);
        }
    }

    public void applyMark(double multiplier, double duration) {
        if (isDead()) {
            return;
        }
        markDamageMultiplier = Math.max(markDamageMultiplier, multiplier);
        markTimer = Math.max(markTimer, duration);
    }

    public void knockAwayFrom(double originX, double originY, double distance) {
        if (isDead()) {
            return;
        }
        double differenceX = worldX - originX;
        double differenceY = worldY - originY;
        double length = Math.hypot(differenceX, differenceY);
        if (length <= 0.0001) {
            differenceX = 1.0;
            differenceY = 0.0;
            length = 1.0;
        }
        worldX += differenceX / length * distance;
        worldY += differenceY / length * distance;
    }

    public double getHealthRatio() {
        return maxHealth <= 0.0 ? 0.0 : health / maxHealth;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isFinishedFading() {
        return isDead() && deathTime >= DEATH_DURATION;
    }

    public boolean hasLootDropped() {
        return lootDropped;
    }

    public void markLootDropped() {
        lootDropped = true;
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

        double hitOffset = hitReactionTimer > 0.0 ? hitReactionTimer / 0.18 * 8.0 : 0.0;
        int screenX = (int) (centerX + worldX + cameraX - renderSize / 2.0
                + hitDirectionX * hitOffset);
        int screenY = (int) (centerY + worldY + cameraY - renderSize / 2.0
                + hitDirectionY * hitOffset);
        double drawTime = isDead() ? deathTime : animationTime;
        int animationFrame = isDead() ? (int) (drawTime * animationSpeed) % 5 : 0;
        int sourceX = animationFrame * frameWidth;

        // The enemy frames are arranged horizontally in one row.
        BufferedImage imageToDraw = isDead() ? deathSheet : spriteSheet;
        Composite oldComposite = graphics.getComposite();

        if (!isDead() && animationSpeed == 0.0) {
            sourceX = 0;
        }

        if (isDead()) {
            drawElementalDeath(graphics, screenX, screenY, drawTime);
            graphics.setComposite(oldComposite);
            return;
        }

        if (facingLeft) {
            Graphics2D flippedGraphics = (Graphics2D) graphics.create();
            flippedGraphics.translate(screenX + renderSize, screenY);
            flippedGraphics.scale(-1, 1);
            flippedGraphics.drawImage(imageToDraw,
                0, 0, renderSize, renderSize,
                sourceX, 0, sourceX + frameWidth, frameHeight, null);
            flippedGraphics.dispose();
        } else {
            graphics.drawImage(imageToDraw,
                screenX, screenY, screenX + renderSize, screenY + renderSize,
                sourceX, 0, sourceX + frameWidth, frameHeight, null);
        }

        drawHitReaction(graphics, screenX, screenY);
        graphics.setComposite(oldComposite);
        drawStatusGlow(graphics, screenX, screenY);
    }

    private void drawHitReaction(Graphics2D graphics, int screenX, int screenY) {
        if (hitReactionTimer <= 0.0) {
            return;
        }
        Color color = elementColor(lastHitElement);
        int alpha = (int) Math.round(150 * hitReactionTimer / 0.18);
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        graphics.fillOval(screenX + renderSize / 6, screenY + renderSize / 6,
                renderSize * 2 / 3, renderSize * 2 / 3);
        graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(255, 255, 255, Math.min(210, alpha + 40)));
        graphics.drawLine(screenX + renderSize / 2,
                screenY + renderSize / 2,
                (int) (screenX + renderSize / 2 - hitDirectionX * 34),
                (int) (screenY + renderSize / 2 - hitDirectionY * 34));
    }

    private void drawElementalDeath(Graphics2D graphics, int screenX, int screenY, double drawTime) {
        double progress = Math.min(1.0, drawTime / DEATH_DURATION);
        int alpha = Math.max(0, (int) Math.round(255 * (1.0 - progress)));
        int centerX = screenX + renderSize / 2;
        int centerY = screenY + renderSize / 2;

        Graphics2D deathGraphics = (Graphics2D) graphics.create();
        deathGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha / 255.0f));
        deathGraphics.drawImage(deathSheet,
                screenX, screenY, screenX + renderSize, screenY + renderSize,
                0, 0, frameWidth, frameHeight, null);
        deathGraphics.dispose();

        switch (deathElement) {
            case FIRE, EXPLOSION -> drawFireDeath(graphics, centerX, centerY, progress, alpha);
            case ICE -> drawIceDeath(graphics, centerX, centerY, progress, alpha);
            case LIGHTNING -> drawLightningDeath(graphics, centerX, centerY, progress, alpha);
            case POISON -> drawPoisonDeath(graphics, centerX, centerY, progress, alpha);
            case SHADOW -> drawShadowDeath(graphics, centerX, centerY, progress, alpha);
            case HOLY -> drawHolyDeath(graphics, centerX, centerY, progress, alpha);
            default -> drawPhysicalDeath(graphics, centerX, centerY, progress, alpha);
        }
    }

    private void updateStatusEffects(double deltaTime) {
        if (poisonTimer > 0.0) {
            poisonTimer = Math.max(0.0, poisonTimer - deltaTime);
            health = Math.max(0.0, health - poisonDamagePerSecond * deltaTime);
            if (health <= 0.0) {
                deathElement = DamageElement.POISON;
            }
            if (poisonTimer <= 0.0) {
                poisonDamagePerSecond = 0.0;
            }
        }
        if (slowTimer > 0.0) {
            slowTimer = Math.max(0.0, slowTimer - deltaTime);
            if (slowTimer <= 0.0) {
                slowMultiplier = 1.0;
            }
        }
        if (hitReactionTimer > 0.0) {
            hitReactionTimer = Math.max(0.0, hitReactionTimer - deltaTime);
        }
        if (stunTimer > 0.0) {
            stunTimer = Math.max(0.0, stunTimer - deltaTime);
        }
        if (markTimer > 0.0) {
            markTimer = Math.max(0.0, markTimer - deltaTime);
            if (markTimer <= 0.0) {
                markDamageMultiplier = 1.0;
            }
        }
    }

    private void drawStatusGlow(Graphics2D graphics, int screenX, int screenY) {
        if (poisonTimer <= 0.0 && slowTimer <= 0.0 && stunTimer <= 0.0 && markTimer <= 0.0) {
            return;
        }
        if (poisonTimer > 0.0) {
            graphics.setColor(new Color(90, 220, 80, 95));
        } else if (stunTimer > 0.0) {
            graphics.setColor(new Color(255, 230, 90, 95));
        } else if (markTimer > 0.0) {
            graphics.setColor(new Color(210, 60, 255, 95));
        } else {
            graphics.setColor(new Color(90, 180, 255, 85));
        }
        graphics.fillOval(screenX + renderSize / 4, screenY - 6, renderSize / 2, 8);
    }

    private void drawPhysicalDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setColor(new Color(110, 90, 70, Math.min(150, alpha)));
        graphics.fillOval(x - renderSize / 3, y + renderSize / 4, renderSize * 2 / 3, 8);
        drawFragments(graphics, x, y, new Color(120, 95, 70), progress, alpha, 7);
    }

    private void drawFireDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        for (int index = 0; index < 8; index++) {
            int ox = deathOffset(index, 0, renderSize / 2);
            int oy = deathOffset(index, 1, renderSize / 3) - (int) (progress * 34);
            graphics.setColor(new Color(255, 85 + index * 12 % 120, 25, Math.min(200, alpha)));
            graphics.fillOval(x + ox - 8, y + oy - 18, 16, 36);
        }
        drawFragments(graphics, x, y, new Color(80, 70, 65), progress, alpha, 9);
    }

    private void drawIceDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setColor(new Color(170, 240, 255, Math.min(170, alpha)));
        graphics.drawOval(x - renderSize / 2, y - renderSize / 2, renderSize, renderSize);
        drawFragments(graphics, x, y, new Color(190, 245, 255), progress, alpha, 12);
    }

    private void drawLightningDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(255, 240, 80, Math.min(220, alpha)));
        for (int index = 0; index < 5; index++) {
            graphics.drawLine(x + deathOffset(index, 0, 30), y - renderSize / 2,
                    x + deathOffset(index, 1, 38), y + renderSize / 2);
        }
        drawFragments(graphics, x, y, new Color(255, 255, 180), progress, alpha, 8);
    }

    private void drawPoisonDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setColor(new Color(70, 220, 70, Math.min(150, alpha)));
        for (int index = 0; index < 10; index++) {
            int size = 10 + index % 8;
            graphics.fillOval(x + deathOffset(index, 0, renderSize / 2) - size / 2,
                    y + deathOffset(index, 1, renderSize / 2) - (int) (progress * 45),
                    size, size);
        }
    }

    private void drawShadowDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setColor(new Color(45, 20, 70, Math.min(180, alpha)));
        for (int index = 0; index < 10; index++) {
            int size = 18 + index % 18;
            graphics.fillOval(x + deathOffset(index, 0, renderSize / 2) - size / 2,
                    y + deathOffset(index, 1, renderSize / 2) - size / 2,
                    size, size);
        }
    }

    private void drawHolyDeath(Graphics2D graphics, int x, int y, double progress, int alpha) {
        graphics.setColor(new Color(255, 245, 180, Math.min(190, alpha)));
        graphics.fillRect(x - renderSize / 5, y - renderSize, renderSize / 3, renderSize);
        drawFragments(graphics, x, y, new Color(255, 245, 180), progress, alpha, 10);
    }

    private void drawFragments(Graphics2D graphics, int x, int y, Color color,
            double progress, int alpha, int count) {
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.min(190, alpha)));
        for (int index = 0; index < count; index++) {
            int ox = deathOffset(index, 0, (int) (renderSize * progress));
            int oy = deathOffset(index, 1, (int) (renderSize * progress));
            int size = 5 + index % 8;
            graphics.fillRect(x + ox, y + oy, size, size);
        }
    }

    private int deathOffset(int index, int salt, int spread) {
        int value = index * 1103515245 + salt * 12345 + deathElement.ordinal() * 97;
        value ^= value >>> 16;
        int range = Math.max(1, spread * 2 + 1);
        return Math.floorMod(value, range) - spread;
    }

    private Color elementColor(DamageElement element) {
        return switch (element) {
            case FIRE, EXPLOSION -> new Color(255, 95, 35);
            case ICE -> new Color(160, 235, 255);
            case LIGHTNING -> new Color(255, 240, 80);
            case POISON -> new Color(75, 235, 75);
            case SHADOW -> new Color(115, 55, 180);
            case HOLY -> new Color(255, 240, 160);
            default -> new Color(255, 220, 140);
        };
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
