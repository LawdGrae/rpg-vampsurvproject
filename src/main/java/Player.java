import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
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

        int horizontal = horizontalInput();
        int vertical = verticalInput();

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

        // Draw only one frame from the larger sprite sheet.
        graphics.drawImage(spriteSheet,
                playerX, playerY, playerX + renderedWidth, playerY + renderedHeight,
                sourceX, sourceY, sourceX + spriteWidth, sourceY + spriteHeight, null);
        drawWeapon(graphics, centerX, centerY, renderedWidth, renderedHeight);

        // Draw a black background, then cover part of it with the remaining red health.
        int healthBarY = playerY + renderedHeight + HEALTH_BAR_GAP;
        int healthBarWidth = renderedWidth;
        int currentHealthWidth = (int) (healthBarWidth * health / maxHealth);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(playerX, healthBarY, healthBarWidth, HEALTH_BAR_HEIGHT);
        graphics.setColor(Color.RED);
        graphics.fillRect(playerX, healthBarY, currentHealthWidth, HEALTH_BAR_HEIGHT);
    }

    private void drawWeapon(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight) {
        int facingX = getFacingX();
        int facingY = getFacingY();
        int weaponSize = Math.max(44, Math.min(64, renderedHeight + 24));
        double offsetX = renderedWidth * 0.62;
        double offsetY = renderedHeight * 0.12;
        double angle = -Math.PI / 4.0;
        double attackProgress = attackAnimationDuration <= 0.0 ? 0.0
                : attackAnimationTime / attackAnimationDuration;
        double swing = Math.sin((1.0 - attackProgress) * Math.PI);

        if (facingX < 0) {
            offsetX = -renderedWidth * 0.62;
            angle = -Math.PI * 3.0 / 4.0;
        } else if (facingY < 0) {
            offsetX = renderedWidth * 0.25;
            offsetY = -renderedHeight * 0.38;
            angle = -Math.PI / 2.0;
        } else if (facingY > 0) {
            offsetX = renderedWidth * 0.25;
            offsetY = renderedHeight * 0.42;
            angle = Math.PI / 2.0;
        }

        if (attackAnimationTime > 0.0) {
            offsetX += facingX * 13.0 * swing;
            offsetY += facingY * 13.0 * swing;
            angle += (facingX < 0 ? -1.0 : 1.0) * (0.75 * swing - 0.35 * attackProgress);
        }

        Graphics2D weaponGraphics = (Graphics2D) graphics.create();
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX + offsetX, centerY + offsetY);
        transform.rotate(angle);
        if (weaponSprite == null) {
            weaponGraphics.transform(transform);
            drawProceduralWeapon(weaponGraphics, weaponSize, attackAnimationTime > 0.0);
        } else {
            transform.scale(weaponSize / (double) weaponSprite.getWidth(),
                    weaponSize / (double) weaponSprite.getHeight());
            transform.translate(-weaponSprite.getWidth() / 2.0, -weaponSprite.getHeight() / 2.0);
            weaponGraphics.drawImage(weaponSprite, transform, null);
        }
        weaponGraphics.dispose();
    }

    private String weaponStyleFor(AbilityDefinition definition) {
        if (definition == null) {
            return "sword";
        }
        String id = definition.getId();
        if (definition.getAbilityClass() == AbilityClass.RANGER) {
            return "bow";
        }
        if (definition.getAbilityClass() == AbilityClass.PRIEST
                || definition.getAbilityClass() == AbilityClass.WARLOCK
                || definition.getAbilityClass() == AbilityClass.ELEMENTALIST) {
            return "staff";
        }
        if (id.contains("shield") || id.contains("guard")) {
            return "shield";
        }
        if (id.contains("shatter") || id.contains("slam") || id.contains("heavy")) {
            return "axe";
        }
        return "sword";
    }

    private void drawProceduralWeapon(Graphics2D graphics, int size, boolean attacking) {
        graphics.setStroke(new BasicStroke(Math.max(3f, size / 12f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (attackWeaponStyle) {
            case "axe" -> drawProceduralAxe(graphics, size);
            case "staff" -> drawProceduralStaff(graphics, size, attacking);
            case "bow" -> drawProceduralBow(graphics, size, attacking);
            case "shield" -> drawProceduralShield(graphics, size);
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

    private void drawProceduralStaff(Graphics2D graphics, int size, boolean attacking) {
        graphics.setColor(new Color(100, 68, 44));
        graphics.drawLine(0, -size / 2, 0, size / 2);
        graphics.setColor(attacking ? new Color(140, 225, 255) : new Color(125, 170, 210));
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

    private int horizontalInput() {
        return (pressedKeys.contains("right") ? 1 : 0)
                - (pressedKeys.contains("left") ? 1 : 0);
    }

    private int verticalInput() {
        return (pressedKeys.contains("down") ? 1 : 0)
                - (pressedKeys.contains("up") ? 1 : 0);
    }
}
