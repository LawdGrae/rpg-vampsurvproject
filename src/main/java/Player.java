import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public abstract class Player {
    private static final int HEALTH_BAR_HEIGHT = 5;
    private static final int HEALTH_BAR_GAP = 4;

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
    private boolean hasRecentMove;
    private boolean movementLocked;
    private boolean aimLocked;
    private double movementLockTime;
    private double aimLockTime;
    private double damageFlashTime;
    private double attackAnimationTime;
    private double attackAnimationDuration = 0.34;
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
        damageFlashTime = Math.max(0.0, damageFlashTime - deltaTime);
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

    public boolean isPetrified() {
        return isMovementLocked() && isAimLocked();
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

    public void takeDamage(double damage) {
        double previousHealth = health;
        health = Math.max(0, health - damage);
        if (health < previousHealth) {
            damageFlashTime = 0.3;
        }
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

    public void playAttackAnimation(AbilityDefinition definition) {
        attackAnimationTime = attackAnimationDuration;
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

        if (damageFlashTime > 0.0) {
            int alpha = (int) Math.round(210.0 * damageFlashTime / 0.3);
            graphics.setColor(new Color(255, 55, 55, alpha));
            graphics.setStroke(new BasicStroke(4f));
            graphics.drawOval(playerX - 8, playerY - 8,
                renderedWidth + 16, renderedHeight + 16);
        }

        if (isPetrified()) {
            String status = "PETRIFIED";
            graphics.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 16));
            int textWidth = graphics.getFontMetrics().stringWidth(status);
            int textX = centerX - textWidth / 2;
            int textY = playerY - 12;
            graphics.setColor(new Color(0, 0, 0, 190));
            graphics.fillRoundRect(textX - 9, textY - 19, textWidth + 18, 25, 10, 10);
            graphics.setColor(new Color(225, 235, 245));
            graphics.drawString(status, textX, textY);
        } else if (damageFlashTime > 0.0) {
            String status = "HIT";
            graphics.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 15));
            int textWidth = graphics.getFontMetrics().stringWidth(status);
            int textX = centerX - textWidth / 2;
            int textY = playerY - 12;
            graphics.setColor(new Color(0, 0, 0, 190));
            graphics.fillRoundRect(textX - 8, textY - 18, textWidth + 16, 24, 10, 10);
            graphics.setColor(new Color(255, 125, 110));
            graphics.drawString(status, textX, textY);
        }
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

        double attackProgress = attackAnimationDuration <= 0.0 ? 0.0
                : attackAnimationTime / attackAnimationDuration;
        double swing = Math.sin((1.0 - attackProgress) * Math.PI);

        if (attackAnimationTime > 0.0) {
            pose.offsetX += getFacingX() * pose.attackReach * swing;
            pose.offsetY += getFacingY() * pose.attackReach * swing;
            pose.angle += pose.attackArc * swing - 0.25 * attackProgress;
        }

        Graphics2D weaponGraphics = (Graphics2D) graphics.create();
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX + pose.offsetX, centerY + pose.offsetY);
        transform.rotate(pose.angle);
        if (image == null) {
            weaponGraphics.transform(transform);
            if (!offhand) {
                drawProceduralWeapon(weaponGraphics, pose.drawHeight, attackAnimationTime > 0.0);
            }
        } else {
            double scale = pose.drawHeight / (double) image.getHeight();
            transform.scale(pose.flipX ? -scale : scale, scale);
            transform.translate(-image.getWidth() * pose.pivotX, -image.getHeight() * pose.pivotY);
            weaponGraphics.drawImage(image, transform, null);
        }
        weaponGraphics.dispose();
    }

    private WeaponPose getWeaponPose(boolean offhand, int renderedWidth, int renderedHeight) {
        int facingX = getFacingX();
        int facingY = getFacingY();
        boolean left = facingX < 0;
        boolean up = facingY < 0;
        boolean down = facingY > 0;

        if ("sword_shield".equals(attackWeaponStyle)) {
            if (offhand) {
                double x = left ? -renderedWidth * 0.20 : renderedWidth * 0.20;
                double y = up ? -renderedHeight * 0.04 : renderedHeight * 0.08;
                if (down) {
                    x = renderedWidth * 0.20;
                    y = renderedHeight * 0.13;
                }
                return new WeaponPose(x, y, left ? -0.08 : 0.08, 32,
                        0.50, 0.50, false, up);
            }
            double x = left ? -renderedWidth * 0.30 : renderedWidth * 0.30;
            double y = down ? renderedHeight * 0.15 : up ? -renderedHeight * 0.14 : renderedHeight * 0.04;
            double angle = left ? Math.toRadians(18) : Math.toRadians(-18);
            if (up) {
                angle = Math.toRadians(-55);
                x = renderedWidth * 0.10;
            } else if (down) {
                angle = Math.toRadians(0);
                x = -renderedWidth * 0.18;
            }
            return new WeaponPose(x, y, angle, 44, 0.67, 0.22, left, up, 5.0, left ? -0.35 : 0.35);
        }

        if ("daggers".equals(attackWeaponStyle)) {
            double x = left ? -renderedWidth * 0.10 : renderedWidth * 0.10;
            double y = up ? -renderedHeight * 0.06 : renderedHeight * 0.11;
            return new WeaponPose(x, y, left ? Math.toRadians(8) : Math.toRadians(-8),
                    42, 0.50, 0.48, left, up, 4.0, left ? -0.28 : 0.28);
        }

        if ("holy_staff".equals(attackWeaponStyle) || "elemental_staff".equals(attackWeaponStyle)
                || "staff".equals(attackWeaponStyle)) {
            double x = left ? -renderedWidth * 0.25 : renderedWidth * 0.25;
            double y = up ? -renderedHeight * 0.10 : renderedHeight * 0.05;
            double angle = left ? Math.toRadians(-10) : Math.toRadians(10);
            if (down) {
                y = renderedHeight * 0.12;
                x = renderedWidth * 0.20;
                angle = Math.toRadians(8);
            }
            return new WeaponPose(x, y, angle, 50, 0.50, 0.66, left, up, 4.0, left ? -0.20 : 0.20);
        }

        double x = left ? -renderedWidth * 0.34 : renderedWidth * 0.34;
        double y = renderedHeight * 0.08;
        return new WeaponPose(x, y, left ? -Math.PI * 3.0 / 4.0 : -Math.PI / 4.0,
                50, 0.50, 0.65, left, up);
    }

    private static class WeaponPose {
        private double offsetX;
        private double offsetY;
        private double angle;
        private final int drawHeight;
        private final double pivotX;
        private final double pivotY;
        private final boolean flipX;
        private final boolean behindCharacter;
        private final double attackReach;
        private final double attackArc;

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter) {
            this(offsetX, offsetY, angle, drawHeight, pivotX, pivotY, flipX,
                    behindCharacter, 0.0, 0.0);
        }

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter,
                double attackReach, double attackArc) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.angle = angle;
            this.drawHeight = drawHeight;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.flipX = flipX;
            this.behindCharacter = behindCharacter;
            this.attackReach = attackReach;
            this.attackArc = attackArc;
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
