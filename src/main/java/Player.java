import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public abstract class Player {
    private static final int HEALTH_BAR_HEIGHT = 5;
    private static final int HEALTH_BAR_GAP = 4;
    private static final Map<BufferedImage, Rectangle> WEAPON_BOUNDS_CACHE = new IdentityHashMap<>();
    private static final Color SWORD_TRAIL_COLOR = new Color(255, 224, 150);
    private static final Color DAGGER_TRAIL_COLOR = new Color(205, 95, 255);
    private static final Color HOLY_TRAIL_COLOR = new Color(255, 242, 150);
    private static final Color ELEMENTAL_TRAIL_COLOR = new Color(110, 210, 255);

    // Subclasses provide these values so different characters can have different settings.
    protected double speed;
    protected final double animationSpeed;
    protected final int spriteScale;
    protected final int spriteWidth;
    protected final int spriteHeight;
    protected final BufferedImage spriteSheet;
    protected final BufferedImage weaponSprite;
    protected final BufferedImage offhandWeaponSprite;
    protected final String defaultWeaponStyle;

    private final Set<String> pressedKeys = new HashSet<>();
    private double worldOffsetX;
    private double worldOffsetY;
    private double animationTime;
    private int spriteRow = 2;
    private double maxHealth;
    private double health;
    private double pickupRadius;
    private double slowTime;
    private double slowMultiplier = 1.0;
    private double recentMoveX = 1.0;
    private double recentMoveY = 0.0;
    private int heldWeaponSideX = 1;
    private boolean hasRecentMove;
    private boolean movementLocked;
    private boolean aimLocked;
    private double movementLockTime;
    private double aimLockTime;
    private double attackAnimationTime;
    private double attackAnimationDuration = 0.34;
    private int attackSequence;
    private String attackWeaponStyle = "sword";

    protected Player(String spritePath, double speed, double animationSpeed,
            int spriteScale, int spriteWidth, int spriteHeight, double maxHealth) {
        this(spritePath, null, speed, animationSpeed,
                spriteScale, spriteWidth, spriteHeight, maxHealth);
    }

    protected Player(String spritePath, String weaponPath, double speed, double animationSpeed,
            int spriteScale, int spriteWidth, int spriteHeight, double maxHealth) {
        this(spritePath, weaponPath, null, "sword", speed, animationSpeed,
                spriteScale, spriteWidth, spriteHeight, maxHealth);
    }

    protected Player(String spritePath, String weaponPath, String defaultWeaponStyle,
            double speed, double animationSpeed, int spriteScale, int spriteWidth,
            int spriteHeight, double maxHealth) {
        this(spritePath, weaponPath, null, defaultWeaponStyle, speed, animationSpeed,
                spriteScale, spriteWidth, spriteHeight, maxHealth);
    }

    protected Player(String spritePath, String weaponPath, String offhandWeaponPath,
            String defaultWeaponStyle, double speed, double animationSpeed,
            int spriteScale, int spriteWidth, int spriteHeight, double maxHealth) {
        this.speed = speed;
        this.animationSpeed = animationSpeed;
        this.spriteScale = spriteScale;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.pickupRadius = 50.0;
        this.spriteSheet = loadSpriteSheet(spritePath);
        this.weaponSprite = weaponPath == null ? null : ResourceLoader.loadImage(weaponPath);
        this.offhandWeaponSprite = offhandWeaponPath == null ? null : ResourceLoader.loadImage(offhandWeaponPath);
        this.defaultWeaponStyle = defaultWeaponStyle;
        this.attackWeaponStyle = defaultWeaponStyle;
    }

    private BufferedImage loadSpriteSheet(String spritePath) {
        return ResourceLoader.loadImage(spritePath);
    }

    public void setKeyPressed(String direction, boolean pressed) {
        // Keep keys in a set so movement continues while a key is held down.
        if (pressed) {
            pressedKeys.add(direction);
        } else {
            pressedKeys.remove(direction);
        }
    }

    public void update(double deltaTime) {
        attackAnimationTime = Math.max(0.0, attackAnimationTime - deltaTime);
        slowTime = Math.max(0.0, slowTime - deltaTime);
        if (slowTime <= 0) {
            slowMultiplier = 1.0;
        }

        if (movementLockTime > 0.0) {
            movementLockTime = Math.max(0.0, movementLockTime - deltaTime);
            if (movementLockTime <= 0.0) {
                movementLocked = false;
            }
        }
        if (aimLockTime > 0.0) {
            aimLockTime = Math.max(0.0, aimLockTime - deltaTime);
            if (aimLockTime <= 0.0) {
                aimLocked = false;
            }
        }

        int horizontal = horizontalInput();
        int vertical = verticalInput();
        if (movementLocked) {
            horizontal = 0;
            vertical = 0;
        }

        // Sprite rows: 0 = up, 1 = right, 2 = down, 3 = left.
        if (vertical < 0) {
            spriteRow = 0;
        } else if (horizontal > 0) {
            spriteRow = 1;
        } else if (vertical > 0) {
            spriteRow = 2;
        } else if (horizontal < 0) {
            spriteRow = 3;
        }

        double length = Math.sqrt(horizontal * horizontal + vertical * vertical);
        if (length > 0) {
            double effectiveSpeed = speed * slowMultiplier;
            double moveX = horizontal / length * effectiveSpeed * deltaTime;
            double moveY = vertical / length * effectiveSpeed * deltaTime;
            recentMoveX = horizontal / length;
            recentMoveY = vertical / length;
            if (horizontal != 0) {
                heldWeaponSideX = horizontal > 0 ? 1 : -1;
            }
            hasRecentMove = true;

            worldOffsetX -= moveX;
            worldOffsetY -= moveY;
            animationTime += deltaTime;
        } else if (!hasRecentMove) {
            recentMoveX = 1.0;
            recentMoveY = 0.0;
        }
    }

    public void applySlow(double duration, double multiplier) {
        slowTime = Math.max(slowTime, duration);
        slowMultiplier = Math.min(slowMultiplier, multiplier);
    }

    public void applyMovementLock(double duration) {
        movementLocked = true;
        movementLockTime = Math.max(movementLockTime, duration);
    }

    public void applyAimLock(double duration) {
        aimLocked = true;
        aimLockTime = Math.max(aimLockTime, duration);
    }

    public boolean isMovementLocked() {
        return movementLocked || movementLockTime > 0.0;
    }

    public boolean isAimLocked() {
        return aimLocked || aimLockTime > 0.0;
    }

    public double getWorldOffsetX() {
        return worldOffsetX;
    }

    public double getWorldOffsetY() {
        return worldOffsetY;
    }

    public double getWorldX() {
        // Player position is the opposite of the camera/world offset.
        return -worldOffsetX;
    }

    public double getWorldY() {
        return -worldOffsetY;
    }

    public double getCollisionRadius() {
        return spriteWidth * spriteScale / 2.0;
    }

    public void increaseMaxHealth(double amount) {
        maxHealth += amount;
        health += amount;
    }

    public void increaseSpeed(double amount) {
        speed += amount;
    }

    public void increasePickupRadius(double amount) {
        pickupRadius += amount;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getHealth() {
        return health;
    }

    public double getPickupRadius() {
        return pickupRadius;
    }

    public int getFacingX() {
        return spriteRow == 1 ? 1 : spriteRow == 3 ? -1 : 0;
    }

    public int getFacingY() {
        return spriteRow == 0 ? -1 : spriteRow == 2 ? 1 : 0;
    }

    public double getRecentMoveX() {
        return recentMoveX;
    }

    public double getRecentMoveY() {
        return recentMoveY;
    }

    public int getHeldWeaponSideX() {
        return heldWeaponSideX;
    }

    public void faceToward(double targetWorldX, double targetWorldY) {
        double differenceX = targetWorldX - getWorldX();
        double differenceY = targetWorldY - getWorldY();
        double length = Math.hypot(differenceX, differenceY);
        if (length <= 0.0001) {
            return;
        }

        recentMoveX = differenceX / length;
        recentMoveY = differenceY / length;
        hasRecentMove = true;
        if (Math.abs(differenceX) > 0.001) {
            heldWeaponSideX = differenceX >= 0.0 ? 1 : -1;
        }
        if (Math.abs(differenceX) >= Math.abs(differenceY)) {
            spriteRow = differenceX >= 0.0 ? 1 : 3;
        } else {
            spriteRow = differenceY >= 0.0 ? 2 : 0;
        }
    }

    public void takeDamage(double damage) {
        health = Math.max(0, health - damage);
    }

    public void heal(double amount) {
        if (amount > 0.0) {
            health = Math.min(maxHealth, health + amount);
        }
    }

    public void moveWorld(double differenceX, double differenceY) {
        worldOffsetX -= differenceX;
        worldOffsetY -= differenceY;
    }

    public BufferedImage getPrimaryWeaponSprite() {
        return weaponSprite;
    }

    public String getDefaultWeaponStyle() {
        return defaultWeaponStyle;
    }

    public double getWeaponCastWorldX() {
        double sideOffset = spriteWidth * spriteScale * 0.26;
        if ("holy_staff".equals(defaultWeaponStyle) || "elemental_staff".equals(defaultWeaponStyle)
                || "staff".equals(defaultWeaponStyle)) {
            sideOffset = spriteWidth * spriteScale * 0.18;
        }
        return getWorldX() + heldWeaponSideX * sideOffset;
    }

    public double getWeaponCastWorldY() {
        if ("holy_staff".equals(defaultWeaponStyle) || "elemental_staff".equals(defaultWeaponStyle)
                || "staff".equals(defaultWeaponStyle)) {
            return getWorldY() - spriteHeight * spriteScale * 0.16;
        }
        double verticalOffset = getFacingY() < 0 ? -spriteHeight * spriteScale * 0.18
                : spriteHeight * spriteScale * 0.02;
        return getWorldY() + verticalOffset;
    }

    public void playAttackAnimation(AbilityDefinition definition) {
        playAttackAnimation(definition, attackAnimationDuration);
    }

    public void playAttackAnimation(AbilityDefinition definition, double duration) {
        attackAnimationDuration = Math.max(0.12, duration);
        attackAnimationTime = attackAnimationDuration;
        attackSequence++;
        attackWeaponStyle = weaponStyleFor(definition);
    }

    public void draw(Graphics2D graphics, int centerX, int centerY) {
        boolean moving = horizontalInput() != 0 || verticalInput() != 0;
        // The walk cycle is columns 0, 1, 2, 1; column 1 is the idle frame.
        int animationFrame = (int) (animationTime * animationSpeed) % 4;
        int spriteColumn = moving ? (animationFrame == 3 ? 1 : animationFrame) : 1;
        int sourceX = spriteColumn * spriteWidth;
        int sourceY = spriteRow * spriteHeight;
        int renderedWidth = spriteWidth * spriteScale;
        int renderedHeight = spriteHeight * spriteScale;
        int playerX = centerX - renderedWidth / 2;
        int playerY = centerY - renderedHeight / 2;

        Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        drawWeapons(graphics, centerX, centerY, renderedWidth, renderedHeight, true);

        // Draw only one frame from the larger sprite sheet.
        graphics.drawImage(spriteSheet,
                playerX, playerY, playerX + renderedWidth, playerY + renderedHeight,
                sourceX, sourceY, sourceX + spriteWidth, sourceY + spriteHeight, null);
        drawWeapons(graphics, centerX, centerY, renderedWidth, renderedHeight, false);
        if (previousInterpolation == null) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
        }

        // Draw a black background, then cover part of it with the remaining red health.
        int healthBarY = playerY + renderedHeight + HEALTH_BAR_GAP;
        int healthBarWidth = renderedWidth;
        int currentHealthWidth = (int) (healthBarWidth * health / maxHealth);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(playerX, healthBarY, healthBarWidth, HEALTH_BAR_HEIGHT);
        graphics.setColor(Color.RED);
        graphics.fillRect(playerX, healthBarY, currentHealthWidth, HEALTH_BAR_HEIGHT);
    }

    private void drawWeapons(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight, boolean behindCharacter) {
        drawAttachedWeapon(graphics, centerX, centerY, renderedWidth, renderedHeight,
                weaponSprite, false, behindCharacter);
        drawAttachedWeapon(graphics, centerX, centerY, renderedWidth, renderedHeight,
                offhandWeaponSprite, true, behindCharacter);
    }

    private void drawAttachedWeapon(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight, BufferedImage image,
            boolean offhand, boolean behindCharacter) {
        WeaponPose pose = getWeaponPose(offhand, renderedWidth, renderedHeight);
        if (pose.behindCharacter != behindCharacter) {
            return;
        }

        double attackProgress = -1.0;
        if (attackAnimationTime > 0.0) {
            attackProgress = attackAnimationDuration <= 0.0 ? 0.0
                    : 1.0 - attackAnimationTime / attackAnimationDuration;
            pose.applyAttackProgress(attackProgress, heldWeaponSideX, getFacingY());
        }
        if (attackProgress >= 0.0) {
            pose.drawAttackTrail(graphics, centerX, centerY, attackProgress);
        }

        Graphics2D weaponGraphics = (Graphics2D) graphics.create();
        Object previousInterpolation = weaponGraphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        weaponGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX + pose.weaponX, centerY + pose.weaponY);
        transform.rotate(pose.weaponRotation);
        if (image == null) {
            weaponGraphics.transform(transform);
            if (!offhand) {
                drawProceduralWeapon(weaponGraphics, pose.drawHeight, attackAnimationTime > 0.0);
            }
        } else {
            Rectangle visibleBounds = visibleWeaponBounds(image);
            pose.weaponScale = pose.drawHeight / (double) visibleBounds.height;
            transform.scale(pose.flipX ? -pose.weaponScale : pose.weaponScale,
                    pose.weaponScale);
            weaponGraphics.transform(transform);
            int drawX = (int) Math.round(-visibleBounds.width * pose.weaponPivotX);
            int drawY = (int) Math.round(-visibleBounds.height * pose.weaponPivotY);
            weaponGraphics.drawImage(image,
                    drawX, drawY, drawX + visibleBounds.width, drawY + visibleBounds.height,
                    visibleBounds.x, visibleBounds.y,
                    visibleBounds.x + visibleBounds.width,
                    visibleBounds.y + visibleBounds.height, null);
        }
        if (previousInterpolation != null) {
            weaponGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
        }
        weaponGraphics.dispose();
    }

    private static Rectangle visibleWeaponBounds(BufferedImage image) {
        synchronized (WEAPON_BOUNDS_CACHE) {
            Rectangle cached = WEAPON_BOUNDS_CACHE.get(image);
            if (cached != null) {
                return cached;
            }

            int minX = image.getWidth();
            int minY = image.getHeight();
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                    if (alpha > 12) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }

            Rectangle bounds;
            if (maxX < minX || maxY < minY) {
                bounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            } else {
                int padding = 2;
                int x = Math.max(0, minX - padding);
                int y = Math.max(0, minY - padding);
                int right = Math.min(image.getWidth(), maxX + padding + 1);
                int bottom = Math.min(image.getHeight(), maxY + padding + 1);
                bounds = new Rectangle(x, y, Math.max(1, right - x), Math.max(1, bottom - y));
            }
            WEAPON_BOUNDS_CACHE.put(image, bounds);
            return bounds;
        }
    }

    private WeaponPose getWeaponPose(boolean offhand, int renderedWidth, int renderedHeight) {
        int facingX = heldWeaponSideX;
        int facingY = getFacingY();
        boolean left = facingX < 0;
        boolean down = facingY > 0;

        if ("sword_shield".equals(attackWeaponStyle)) {
            if (offhand) {
                double x = left ? -renderedWidth * 0.12 : renderedWidth * 0.12;
                double y = renderedHeight * 0.09;
                if (down) {
                    x = left ? -renderedWidth * 0.11 : renderedWidth * 0.11;
                    y = renderedHeight * 0.12;
                }
                return new WeaponPose(x, y, left ? -0.04 : 0.04, 24,
                        0.50, 0.56, false, false);
            }
            double x = left ? -renderedWidth * 0.09 : renderedWidth * 0.09;
            double y = renderedHeight * 0.02;
            double angle = left ? Math.toRadians(198) : Math.toRadians(162);
            if (down) {
                x = left ? -renderedWidth * 0.08 : renderedWidth * 0.08;
                y = renderedHeight * 0.06;
            }
            return new WeaponPose(x, y, angle, 34, 0.82, 0.17, left, false,
                    11.0, left ? 0.95 : -0.95, SWORD_TRAIL_COLOR, 4.5);
        }

        if ("daggers".equals(attackWeaponStyle)) {
            boolean alternateSlash = attackAnimationTime > 0.0 && attackSequence % 2 == 0;
            double x = left ? -renderedWidth * 0.06 : renderedWidth * 0.06;
            double y = renderedHeight * 0.05;
            double angle = left ? Math.toRadians(12) : Math.toRadians(-12);
            if (down) {
                y = renderedHeight * 0.08;
                angle = left ? Math.toRadians(8) : Math.toRadians(-8);
            }
            if (alternateSlash) {
                angle += left ? Math.toRadians(-16) : Math.toRadians(16);
            }
            double arc = left ? -0.72 : 0.72;
            if (alternateSlash) {
                arc = -arc * 0.86;
            }
            return new WeaponPose(x, y, angle, 24, 0.50, 0.20, left, false,
                    8.0, arc, DAGGER_TRAIL_COLOR, 3.5);
        }

        if ("holy_staff".equals(attackWeaponStyle) || "elemental_staff".equals(attackWeaponStyle)
                || "staff".equals(attackWeaponStyle)) {
            boolean elementalStaff = "elemental_staff".equals(attackWeaponStyle);
            double sideDistance = elementalStaff ? 0.14 : 0.12;
            double x = left ? -renderedWidth * sideDistance : renderedWidth * sideDistance;
            double y = elementalStaff ? renderedHeight * 0.04 : renderedHeight * 0.05;
            double angle = left ? Math.toRadians(-7) : Math.toRadians(7);
            if (down) {
                y = elementalStaff ? renderedHeight * 0.08 : renderedHeight * 0.09;
                double downSideDistance = elementalStaff ? 0.12 : 0.11;
                x = left ? -renderedWidth * downSideDistance : renderedWidth * downSideDistance;
                angle = left ? Math.toRadians(-4) : Math.toRadians(4);
            }
            return new WeaponPose(x, y, angle, elementalStaff ? 42 : 38,
                    0.50, 0.78, left, !elementalStaff,
                    6.0, left ? -0.42 : 0.42,
                    elementalStaff ? ELEMENTAL_TRAIL_COLOR : HOLY_TRAIL_COLOR, 3.0);
        }

        double x = left ? -renderedWidth * 0.34 : renderedWidth * 0.34;
        double y = renderedHeight * 0.08;
        return new WeaponPose(x, y, left ? -Math.PI * 3.0 / 4.0 : -Math.PI / 4.0,
                50, 0.50, 0.65, left, false);
    }

    private static class WeaponPose {
        private final double handAnchorX;
        private final double handAnchorY;
        private double weaponX;
        private double weaponY;
        private double weaponRotation;
        private double weaponScale = 1.0;
        private final int drawHeight;
        private final double weaponPivotX;
        private final double weaponPivotY;
        private final boolean flipX;
        private final boolean behindCharacter;
        private final double attackReach;
        private final double attackArc;
        private final Color trailColor;
        private final double trailWidth;

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter) {
            this(offsetX, offsetY, angle, drawHeight, pivotX, pivotY, flipX,
                    behindCharacter, 0.0, 0.0, null, 0.0);
        }

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter,
                double attackReach, double attackArc) {
            this(offsetX, offsetY, angle, drawHeight, pivotX, pivotY, flipX,
                    behindCharacter, attackReach, attackArc, null, 0.0);
        }

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter,
                double attackReach, double attackArc, Color trailColor, double trailWidth) {
            this.handAnchorX = offsetX;
            this.handAnchorY = offsetY;
            this.weaponX = offsetX;
            this.weaponY = offsetY;
            this.weaponRotation = angle;
            this.drawHeight = drawHeight;
            this.weaponPivotX = pivotX;
            this.weaponPivotY = pivotY;
            this.flipX = flipX;
            this.behindCharacter = behindCharacter;
            this.attackReach = attackReach;
            this.attackArc = attackArc;
            this.trailColor = trailColor;
            this.trailWidth = trailWidth;
        }

        private void applyAttackProgress(double progress, int facingX, int facingY) {
            if (attackArc == 0.0) {
                return;
            }

            double clamped = Math.max(0.0, Math.min(1.0, progress));
            double rotationOffset;
            if (clamped < 0.24) {
                rotationOffset = lerp(0.0, -0.72 * attackArc, smooth(clamped / 0.24));
            } else if (clamped < 0.55) {
                rotationOffset = lerp(-0.72 * attackArc, 1.22 * attackArc,
                        smoother((clamped - 0.24) / 0.31));
            } else if (clamped < 0.78) {
                rotationOffset = lerp(1.22 * attackArc, 0.42 * attackArc,
                        smooth((clamped - 0.55) / 0.23));
            } else {
                rotationOffset = lerp(0.42 * attackArc, 0.0, smooth((clamped - 0.78) / 0.22));
            }

            double hitPulse = Math.sin(Math.min(1.0, clamped / 0.72) * Math.PI);
            double handNudge = hitPulse * Math.min(5.0, attackReach * 0.42);
            double lift = Math.sin(clamped * Math.PI) * (facingY < 0 ? 1.4 : 2.6);
            weaponRotation += rotationOffset;
            weaponX = handAnchorX + facingX * handNudge;
            weaponY = handAnchorY - lift;
        }

        private void drawAttackTrail(Graphics2D graphics, int centerX, int centerY,
                double progress) {
            if (trailColor == null || attackArc == 0.0) {
                return;
            }
            double clamped = Math.max(0.0, Math.min(1.0, progress));
            if (clamped < 0.20 || clamped > 0.76) {
                return;
            }

            double phase = smooth((clamped - 0.20) / 0.56);
            float alpha = (float) (Math.sin(phase * Math.PI) * 0.48);
            if (alpha <= 0.01f) {
                return;
            }

            double handX = centerX + weaponX;
            double handY = centerY + weaponY;
            double radius = drawHeight * (0.54 + 0.28 * phase);
            double middleAngle = weaponRotation - Math.PI / 2.0;
            double sweep = attackArc * 0.58;
            double startAngle = middleAngle - sweep;
            double endAngle = middleAngle + sweep * 0.22;

            Graphics2D trailGraphics = (Graphics2D) graphics.create();
            trailGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            trailGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
            trailGraphics.setStroke(new BasicStroke((float) Math.max(2.0, trailWidth),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trailGraphics.setColor(trailColor);

            int previousX = (int) Math.round(handX + Math.cos(startAngle) * radius);
            int previousY = (int) Math.round(handY + Math.sin(startAngle) * radius * 0.72);
            for (int step = 1; step <= 6; step++) {
                double ratio = step / 6.0;
                double angle = lerp(startAngle, endAngle, ratio);
                double taperedRadius = radius * (1.0 - ratio * 0.16);
                int x = (int) Math.round(handX + Math.cos(angle) * taperedRadius);
                int y = (int) Math.round(handY + Math.sin(angle) * taperedRadius * 0.72);
                trailGraphics.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
            trailGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.65f));
            trailGraphics.setStroke(new BasicStroke((float) Math.max(1.0, trailWidth * 0.45),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trailGraphics.setColor(Color.WHITE);
            trailGraphics.drawLine((int) Math.round(handX), (int) Math.round(handY),
                    previousX, previousY);
            trailGraphics.dispose();
        }

        private static double lerp(double start, double end, double ratio) {
            double clampedRatio = Math.max(0.0, Math.min(1.0, ratio));
            return start + (end - start) * clampedRatio;
        }

        private static double smooth(double value) {
            double clampedValue = Math.max(0.0, Math.min(1.0, value));
            return clampedValue * clampedValue * (3.0 - 2.0 * clampedValue);
        }

        private static double smoother(double value) {
            double clampedValue = Math.max(0.0, Math.min(1.0, value));
            return clampedValue * clampedValue * clampedValue
                    * (clampedValue * (clampedValue * 6.0 - 15.0) + 10.0);
        }
    }

    private String weaponStyleFor(AbilityDefinition definition) {
        if (definition == null) {
            return defaultWeaponStyle;
        }
        String id = definition.getId();
        if (definition.getAbilityClass() == AbilityClass.BLACK_KNIGHT) {
            return "sword_shield";
        }
        if (definition.getAbilityClass() == AbilityClass.ASSASSIN) {
            return "daggers";
        }
        if (definition.getAbilityClass() == AbilityClass.RANGER) {
            return "bow";
        }
        if (definition.getAbilityClass() == AbilityClass.PRIEST) {
            return "holy_staff";
        }
        if (definition.getAbilityClass() == AbilityClass.WARLOCK) {
            return "staff";
        }
        if (definition.getAbilityClass() == AbilityClass.ELEMENTALIST) {
            return "elemental_staff";
        }
        if (id.contains("shield") || id.contains("guard")) {
            return "sword_shield";
        }
        if (id.contains("shatter") || id.contains("slam") || id.contains("heavy")) {
            return "axe";
        }
        return defaultWeaponStyle;
    }

    private void drawProceduralWeapon(Graphics2D graphics, int size, boolean attacking) {
        graphics.setStroke(new BasicStroke(Math.max(3f, size / 12f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (attackWeaponStyle) {
            case "axe" -> drawProceduralAxe(graphics, size);
            case "staff" -> drawProceduralStaff(graphics, size, attacking, new Color(125, 170, 210));
            case "holy_staff" -> drawProceduralStaff(graphics, size, attacking, new Color(255, 232, 130));
            case "elemental_staff" -> drawProceduralStaff(graphics, size, attacking, new Color(100, 210, 255));
            case "bow" -> drawProceduralBow(graphics, size, attacking);
            case "shield" -> drawProceduralShield(graphics, size);
            case "sword_shield" -> drawProceduralSwordAndShield(graphics, size);
            case "daggers" -> drawProceduralDaggers(graphics, size);
            default -> drawProceduralSword(graphics, size);
        }
    }

    private void drawProceduralSword(Graphics2D graphics, int size) {
        graphics.setColor(new Color(78, 52, 38));
        graphics.drawLine(0, size / 5, 0, size / 2);
        graphics.setColor(new Color(230, 232, 218));
        graphics.drawLine(0, -size / 2, 0, size / 5);
        graphics.setColor(new Color(112, 148, 190));
        graphics.drawLine(-size / 5, size / 6, size / 5, size / 6);
    }

    private void drawProceduralAxe(Graphics2D graphics, int size) {
        graphics.setColor(new Color(88, 58, 38));
        graphics.drawLine(0, -size / 2, 0, size / 2);
        graphics.setColor(new Color(215, 212, 196));
        graphics.fillArc(-size / 3, -size / 2, size / 2, size / 2, 255, 190);
        graphics.fillArc(-size / 8, -size / 2, size / 2, size / 2, 95, 190);
    }

    private void drawProceduralStaff(Graphics2D graphics, int size, boolean attacking,
            Color orbColor) {
        graphics.setColor(new Color(100, 68, 44));
        graphics.drawLine(0, -size / 2, 0, size / 2);
        graphics.setColor(attacking ? orbColor.brighter() : orbColor);
        int orb = attacking ? size / 4 : size / 5;
        graphics.fillOval(-orb / 2, -size / 2 - orb / 2, orb, orb);
    }

    private void drawProceduralBow(Graphics2D graphics, int size, boolean attacking) {
        graphics.setColor(new Color(142, 92, 48));
        graphics.drawArc(-size / 3, -size / 2, size / 2, size, -90, 180);
        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(new Color(232, 222, 184));
        int pull = attacking ? size / 6 : 0;
        graphics.drawLine(-size / 12, -size / 2, pull, 0);
        graphics.drawLine(pull, 0, -size / 12, size / 2);
    }

    private void drawProceduralShield(Graphics2D graphics, int size) {
        int width = size / 2;
        int height = size / 2;
        graphics.setColor(new Color(76, 90, 118));
        graphics.fillRoundRect(-width / 2, -height / 2, width, height, 10, 10);
        graphics.setColor(new Color(198, 207, 220));
        graphics.drawRoundRect(-width / 2, -height / 2, width, height, 10, 10);
    }

    private void drawProceduralSwordAndShield(Graphics2D graphics, int size) {
        drawProceduralSword(graphics, size);
        int shieldSize = size / 3;
        graphics.setColor(new Color(68, 82, 112));
        graphics.fillRoundRect(-size / 3, size / 8, shieldSize, shieldSize, 8, 8);
        graphics.setColor(new Color(220, 212, 170));
        graphics.drawRoundRect(-size / 3, size / 8, shieldSize, shieldSize, 8, 8);
    }

    private void drawProceduralDaggers(Graphics2D graphics, int size) {
        graphics.setColor(new Color(82, 54, 42));
        graphics.drawLine(-size / 8, size / 4, -size / 8, size / 2);
        graphics.drawLine(size / 8, size / 5, size / 8, size / 2);
        graphics.setColor(new Color(225, 228, 218));
        graphics.drawLine(-size / 8, -size / 3, -size / 8, size / 4);
        graphics.drawLine(size / 8, -size / 2, size / 8, size / 5);
    }

    private int horizontalInput() {
        return (pressedKeys.contains("right") ? 1 : 0)
                - (pressedKeys.contains("left") ? 1 : 0);
    }

    private int verticalInput() {
        return (pressedKeys.contains("down") ? 1 : 0)
                - (pressedKeys.contains("up") ? 1 : 0);
    }
}
