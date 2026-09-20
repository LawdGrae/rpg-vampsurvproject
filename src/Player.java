import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

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

    protected Player(String spritePath, double speed, double animationSpeed,
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
    }

    private BufferedImage loadSpriteSheet(String spritePath) {
        try {
            return ImageIO.read(Player.class.getResource(spritePath));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load " + spritePath, exception);
        }
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

            worldOffsetX -= moveX;
            worldOffsetY -= moveY;
            animationTime += deltaTime;
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

    public void takeDamage(double damage) {
        health = Math.max(0, health - damage);
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

        // Draw a black background, then cover part of it with the remaining red health.
        int healthBarY = playerY + renderedHeight + HEALTH_BAR_GAP;
        int healthBarWidth = renderedWidth;
        int currentHealthWidth = (int) (healthBarWidth * health / maxHealth);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(playerX, healthBarY, healthBarWidth, HEALTH_BAR_HEIGHT);
        graphics.setColor(Color.RED);
        graphics.fillRect(playerX, healthBarY, currentHealthWidth, HEALTH_BAR_HEIGHT);
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