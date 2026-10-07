import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public abstract class Player {
    private static final int HEALTH_BAR_HEIGHT = 5;
    private static final int HEALTH_BAR_GAP = 4;
    private static final Map<BufferedImage, Rectangle> WEAPON_BOUNDS_CACHE = new IdentityHashMap<>();
    private static final Map<BufferedImage, BufferedImage[]> HELD_WEAPON_IMAGES = new IdentityHashMap<>();
    private static final Map<BufferedImage, BufferedImage> FILTERED_HELD_WEAPONS = new IdentityHashMap<>();
    private static final Map<BufferedImage, BufferedImage> BLOCK_FLASH_IMAGES = new IdentityHashMap<>();
    private static final Map<BufferedImage, BufferedImage> BODY_WITHOUT_STAFF = new IdentityHashMap<>();
    // Source-frame wrist pixels: rows up/right/down/left, columns walk/idle/walk.
    // Each entry contains the primary hand followed by the offhand.
    private static final int[][][][] SWORD_HANDS = {
        {{{43, 40}, {20, 35}}, {{44, 42}, {21, 42}}, {{44, 40}, {21, 40}}},
        {{{34, 40}, {37, 36}}, {{32, 42}, {37, 38}}, {{34, 40}, {37, 36}}},
        {{{20, 40}, {44, 40}}, {{20, 42}, {44, 42}}, {{20, 40}, {44, 40}}},
        {{{27, 40}, {39, 35}}, {{29, 36}, {41, 36}}, {{27, 40}, {39, 35}}}
    };
    private static final int[][][][] DAGGER_HANDS = {
        {{{24, 40}, {40, 40}}, {{24, 40}, {40, 40}}, {{24, 40}, {40, 40}}},
        {{{34, 40}, {40, 40}}, {{34, 40}, {40, 40}}, {{34, 40}, {40, 40}}},
        {{{24, 41}, {40, 41}}, {{24, 41}, {40, 41}}, {{24, 41}, {40, 41}}},
        {{{29, 40}, {24, 40}}, {{29, 40}, {24, 40}}, {{29, 40}, {24, 40}}}
    };
    private static final int[][][] HOLY_STAFF_HANDS = {
        {{44, 43}, {44, 43}, {44, 43}},
        {{37, 43}, {37, 43}, {37, 43}},
        {{21, 39}, {21, 39}, {21, 39}},
        {{28, 43}, {28, 43}, {28, 43}}
    };
    private static final int[][][] ELEMENTAL_STAFF_HANDS = {
        {{45, 46}, {45, 46}, {45, 46}},
        {{38, 43}, {37, 40}, {38, 43}},
        {{19, 40}, {19, 40}, {19, 40}},
        {{27, 40}, {27, 40}, {31, 42}}
    };
    // Aegis uses Rakki's left forearm, measured independently in all twelve frames.
    // Rows are up/right/down/left; the pivot is the shield's central arm strap.
    private static final int[][][] GREATSHIELD_HANDS = {
        {{15, 36}, {16, 37}, {15, 36}},
        {{37, 41}, {37, 42}, {38, 41}},
        {{43, 40}, {43, 41}, {43, 40}},
        {{26, 41}, {26, 42}, {25, 41}}
    };
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
    private final BufferedImage[][] spriteFrames;
    private final BufferedImage[][] previewSpriteFrames;
    private final BufferedImage characterLayer;
    protected final BufferedImage weaponSprite;
    protected final BufferedImage offhandWeaponSprite;
    protected final String defaultWeaponStyle;
    private final BufferedImage heldPrimarySprite;
    private final BufferedImage heldOffhandSprite;

    private final Set<String> pressedKeys = new HashSet<>();
    private double worldOffsetX;
    private double worldOffsetY;
    private WorldCollision worldCollision;
    private double animationTime;
    private double visualTime;
    private double locomotionBlend;
    private double motionDirectionX;
    private double motionDirectionY;
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
    private boolean skillAnimation;
    private double attackStrikeProgress = 0.43;
    private double[] attackBeatProgress = {0.43};
    private boolean autoStaffRecoil;
    private double primaryAttackCarry;
    private double offhandAttackCarry;
    private double primaryReachCarryX;
    private double primaryReachCarryY;
    private double offhandReachCarryX;
    private double offhandReachCarryY;
    private String attackSkillId = "";
    private AbilityDefinition activeAbility;
    private double daggerPrimaryTurnCarry;
    private double daggerOffhandTurnCarry;
    private MotionPose attackMotionCarry = new MotionPose();
    private String guardianSkillId = "";
    private double guardianFortressRemaining;
    private double guardianFortressBlend;
    private double guardianBlockTime;
    private double guardianChargeVisualTime;
    private GuardianMotion guardianMotionCarry = new GuardianMotion();

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
        // Begin on the planted idle frame before easing into the first footstep.
        this.animationTime = 1.0 / Math.max(1.0, animationSpeed);
        this.spriteScale = spriteScale;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.pickupRadius = 50.0;
        BufferedImage loadedSheet = loadSpriteSheet(spritePath);
        BufferedImage bodySheet = "elemental_staff".equals(defaultWeaponStyle)
                ? withoutEmbeddedStaff(loadedSheet) : loadedSheet;
        this.spriteSheet = CharacterSpriteImages.prepareSheet(bodySheet, spriteWidth, spriteHeight);
        spriteFrames = new BufferedImage[spriteSheet.getHeight() / spriteHeight]
                [spriteSheet.getWidth() / spriteWidth];
        previewSpriteFrames = new BufferedImage[spriteFrames.length][spriteFrames[0].length];
        for (int row = 0; row < spriteFrames.length; row++) {
            for (int column = 0; column < spriteFrames[row].length; column++) {
                spriteFrames[row][column] = spriteSheet.getSubimage(column * spriteWidth,
                        row * spriteHeight, spriteWidth, spriteHeight);
                // Keep existing equipment isolation, but sample body colors/alpha
                // before the gameplay image preparation/sharpening pass.
                previewSpriteFrames[row][column] = bodySheet.getSubimage(column * spriteWidth,
                        row * spriteHeight, spriteWidth, spriteHeight);
            }
        }
        // Leave room for held weapons and attack poses around the source frame.
        characterLayer = new BufferedImage(spriteWidth * spriteScale * 3,
                spriteHeight * spriteScale * 3, BufferedImage.TYPE_INT_ARGB);
        BufferedImage[] heldImages = heldWeaponImages(
                weaponPath == null ? null : ResourceLoader.loadImage(weaponPath), defaultWeaponStyle);
        this.weaponSprite = heldImages[0];
        this.offhandWeaponSprite = heldImages[1] != null ? heldImages[1]
                : offhandWeaponPath == null ? null : ResourceLoader.loadImage(offhandWeaponPath);
        this.defaultWeaponStyle = defaultWeaponStyle;
        this.attackWeaponStyle = defaultWeaponStyle;
        heldPrimarySprite = weaponSprite;
        heldOffhandSprite = offhandWeaponSprite;
    }

    private BufferedImage loadSpriteSheet(String spritePath) {
        return ResourceLoader.loadImage(spritePath);
    }

    private BufferedImage withoutEmbeddedStaff(BufferedImage source) {
        synchronized (BODY_WITHOUT_STAFF) {
            return BODY_WITHOUT_STAFF.computeIfAbsent(source, this::removeEmbeddedStaff);
        }
    }

    private BufferedImage removeEmbeddedStaff(BufferedImage source) {
        BufferedImage body = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = body.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        // Ziea's original frames include a violet staff. Remove only its pixels from
        // the runtime body layer before drawing the equipped staff in the same hand.
        for (int row = 1; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                int left = row == 1 ? 28 : row == 2 ? 9 : column == 2 ? 12 : 8;
                int right = row == 1 ? 59 : row == 2 ? 24 : column == 2 ? 38 : 34;
                int top = row == 2 ? 13 : column == 2 && row == 3 ? 18 : 16;
                for (int y = top; y < spriteHeight; y++) {
                    for (int x = left; x <= right; x++) {
                        int pixelX = column * spriteWidth + x;
                        int pixelY = row * spriteHeight + y;
                        int rgb = body.getRGB(pixelX, pixelY);
                        int red = (rgb >>> 16) & 255;
                        int green = (rgb >>> 8) & 255;
                        int blue = rgb & 255;
                        if (blue > red * 1.10 && blue > green * 1.18) {
                            body.setRGB(pixelX, pixelY, 0);
                        }
                    }
                }
            }
        }
        return body;
    }

    private static synchronized BufferedImage[] heldWeaponImages(BufferedImage image, String style) {
        if (image == null) {
            return new BufferedImage[] {null, null};
        }
        return HELD_WEAPON_IMAGES.computeIfAbsent(image, source -> {
            if ("daggers".equals(style)) {
                int halfWidth = source.getWidth() / 2;
                return new BufferedImage[] {
                    source.getSubimage(0, 0, halfWidth, source.getHeight()),
                    source.getSubimage(halfWidth, 0, source.getWidth() - halfWidth, source.getHeight())
                };
            }
            if ("holy_staff".equals(style)) {
                // Exclude the neighboring purple icon fragment at the left edge of the asset.
                int left = (int) Math.round(source.getWidth() * 48.0 / 435.0);
                return new BufferedImage[] {
                    source.getSubimage(left, 0, source.getWidth() - left, source.getHeight()), null
                };
            }
            return new BufferedImage[] {source, null};
        });
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
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) {
            return;
        }
        attackAnimationTime = Math.max(0.0, attackAnimationTime - deltaTime);
        advanceGuardianState(deltaTime);
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
        if (isMovementLocked()) {
            horizontal = 0;
            vertical = 0;
        }

        // Sprite rows: 0 = up, 1 = right, 2 = down, 3 = left.
        if (!isAimLocked() && attackAnimationTime <= 0.0) {
            if (vertical < 0) {
                setSpriteRow(0);
            } else if (horizontal > 0) {
                setSpriteRow(1);
            } else if (vertical > 0) {
                setSpriteRow(2);
            } else if (horizontal < 0) {
                setSpriteRow(3);
            }
        }

        double length = Math.sqrt(horizontal * horizontal + vertical * vertical);
        if (length > 0) {
            double effectiveSpeed = speed * slowMultiplier
                    * ("greatshield".equals(defaultWeaponStyle)
                    ? 1.0 - guardianFortressBlend * 0.35 : 1.0);
            double moveX = horizontal / length * effectiveSpeed * deltaTime;
            double moveY = vertical / length * effectiveSpeed * deltaTime;
            if (!isAimLocked() && attackAnimationTime <= 0.0) {
                recentMoveX = horizontal / length;
                recentMoveY = vertical / length;
                if (horizontal != 0) {
                    heldWeaponSideX = horizontal > 0 ? 1 : -1;
                }
            }
            hasRecentMove = true;

            moveWorld(moveX, moveY);
        } else if (!hasRecentMove) {
            recentMoveX = 1.0;
            recentMoveY = 0.0;
        }
        boolean charging = guardianChargeVisualTime > 0.0;
        advanceVisualAnimation(deltaTime, charging ? recentMoveX
                        : length > 0.0 ? horizontal / length : 0.0,
                charging ? recentMoveY : length > 0.0 ? vertical / length : 0.0,
                length > 0.0 || charging);
    }

    /** Advance selection portraits without moving the player or changing combat state. */
    public void updatePreview(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) {
            return;
        }
        attackAnimationTime = Math.max(0.0, attackAnimationTime - deltaTime);
        advanceGuardianState(deltaTime);
        advanceVisualAnimation(deltaTime, 0.0, 0.0, false);
    }

    private void advanceGuardianState(double deltaTime) {
        guardianFortressRemaining = Math.max(0.0, guardianFortressRemaining - deltaTime);
        guardianBlockTime = Math.max(0.0, guardianBlockTime - deltaTime);
        guardianChargeVisualTime = Math.max(0.0, guardianChargeVisualTime - deltaTime);
        double target = guardianFortressRemaining > 0.0 ? 1.0 : 0.0;
        guardianFortressBlend = target + (guardianFortressBlend - target)
                * Math.exp(-8.0 * deltaTime);
    }

    public void setGuardianFortressRemaining(double remaining) {
        if ("greatshield".equals(defaultWeaponStyle) && remaining > 0.0
                && guardianFortressRemaining <= 0.0) {
            slowTime = 0.0;
            slowMultiplier = 1.0;
            movementLocked = false;
            aimLocked = false;
            movementLockTime = 0.0;
            aimLockTime = 0.0;
        }
        guardianFortressRemaining = Math.max(0.0, remaining);
    }

    public boolean isGuardianFortressActive() {
        return "greatshield".equals(defaultWeaponStyle) && guardianFortressRemaining > 0.0;
    }

    public boolean isGuardianKnockbackImmune() {
        return isGuardianFortressActive();
    }

    public double getGuardianResistanceMultiplier() {
        if (!"greatshield".equals(defaultWeaponStyle)) {
            return 1.0;
        }
        return isGuardianFortressActive() ? 0.0 : health < maxHealth * 0.40 ? 0.35 : 0.65;
    }

    public void playGuardianBlock() {
        if ("greatshield".equals(defaultWeaponStyle)) {
            guardianBlockTime = 0.20;
        }
    }

    /** Displacement, rather than velocity: combat integrates the charge and collision sweep. */
    public void applyGuardianChargeMovement(double differenceX, double differenceY, double deltaTime) {
        if (!Double.isFinite(differenceX) || !Double.isFinite(differenceY)
                || !Double.isFinite(deltaTime) || deltaTime < 0.0) {
            return;
        }
        moveWorld(differenceX, differenceY);
        guardianChargeVisualTime = Math.max(guardianChargeVisualTime, deltaTime * 2.0 + 0.025);
    }

    private void advanceVisualAnimation(double deltaTime, double directionX,
            double directionY, boolean moving) {
        // Both idle waves repeat together every 78.2 seconds; keep phase precision.
        visualTime = (visualTime + deltaTime) % 78.2;
        double target = moving ? 1.0 : 0.0;
        double response = moving ? 12.0 : 9.0;
        double decay = Math.exp(-response * deltaTime);
        // Integrate the easing curve so gait timing is identical at any update rate.
        double gaitTime = target * deltaTime
                + (locomotionBlend - target) * (1.0 - decay) / response;
        double gaitPeriod = 4.0 / Math.max(1.0, animationSpeed);
        animationTime = (animationTime + Math.max(0.0, gaitTime) * 1.55) % gaitPeriod;
        locomotionBlend = target + (locomotionBlend - target) * decay;
        double turnDecay = Math.exp(-10.0 * deltaTime);
        motionDirectionX = directionX + (motionDirectionX - directionX) * turnDecay;
        motionDirectionY = directionY + (motionDirectionY - directionY) * turnDecay;
        double wristDecay = Math.exp(-14.0 * deltaTime);
        daggerPrimaryTurnCarry *= wristDecay;
        daggerOffhandTurnCarry *= wristDecay;
    }

    private void setSpriteRow(int nextRow) {
        if (nextRow == spriteRow) {
            return;
        }
        if (!"daggers".equals(defaultWeaponStyle)) {
            spriteRow = nextRow;
            return;
        }
        int width = spriteWidth * spriteScale;
        int height = spriteHeight * spriteScale;
        double primaryAngle = animatedWeaponPose(false, width, height).weaponRotation;
        double offhandAngle = animatedWeaponPose(true, width, height).weaponRotation;
        spriteRow = nextRow;
        daggerPrimaryTurnCarry = 0.0;
        daggerOffhandTurnCarry = 0.0;
        // Keep the wrist angle through a turn; the handle stays on the new sprite's hand.
        daggerPrimaryTurnCarry = primaryAngle
                - animatedWeaponPose(false, width, height).weaponRotation;
        daggerOffhandTurnCarry = offhandAngle
                - animatedWeaponPose(true, width, height).weaponRotation;
    }

    public void applySlow(double duration, double multiplier) {
        double resistance = getGuardianResistanceMultiplier();
        if (resistance <= 0.0) {
            return;
        }
        slowTime = Math.max(slowTime, duration * resistance);
        slowMultiplier = Math.min(slowMultiplier, 1.0 - (1.0 - multiplier) * resistance);
    }

    public void applyMovementLock(double duration) {
        duration *= getGuardianResistanceMultiplier();
        if (duration <= 0.0) {
            return;
        }
        movementLocked = true;
        movementLockTime = Math.max(movementLockTime, duration);
    }

    public void applyAimLock(double duration) {
        duration *= getGuardianResistanceMultiplier();
        if (duration <= 0.0) {
            return;
        }
        aimLocked = true;
        aimLockTime = Math.max(aimLockTime, duration);
    }

    public boolean isMovementLocked() {
        return movementLocked || movementLockTime > 0.0
                || ("greatshield".equals(defaultWeaponStyle) && isSkillAnimationActive());
    }

    public boolean isAimLocked() {
        return aimLocked || aimLockTime > 0.0
                || ("greatshield".equals(defaultWeaponStyle) && attackAnimationTime > 0.0);
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
        if (isAimLocked()) {
            return;
        }
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
            setSpriteRow(differenceX >= 0.0 ? 1 : 3);
        } else {
            setSpriteRow(differenceY >= 0.0 ? 2 : 0);
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
        if (!Double.isFinite(differenceX) || !Double.isFinite(differenceY)) return;
        if (worldCollision == null) {
            worldOffsetX -= differenceX;
            worldOffsetY -= differenceY;
            return;
        }
        // Use the feet, not the large combat hit circle or the held weapon.
        double feetOffset = spriteHeight * spriteScale * 0.44;
        Point2D.Double destination = worldCollision.move(getWorldX(), getWorldY() + feetOffset,
                differenceX, differenceY, spriteWidth * spriteScale * 0.18,
                spriteHeight * spriteScale * 0.10);
        worldOffsetX = -destination.x;
        worldOffsetY = -(destination.y - feetOffset);
    }

    public void setWorldCollision(WorldCollision worldCollision) {
        this.worldCollision = worldCollision;
    }

    public BufferedImage getPrimaryWeaponSprite() {
        return weaponSprite;
    }

    public String getDefaultWeaponStyle() {
        return defaultWeaponStyle;
    }

    public double getWeaponCastWorldX() {
        return getWorldX() + weaponCastOffset(false)[0];
    }

    public double getWeaponCastWorldY() {
        return getWorldY() + weaponCastOffset(false)[1];
    }

    /** Exact transformed blade tip, shield rim or staff head used by the rendered pose. */
    public double getSkillSourceWorldX() {
        return getWorldX() + weaponCastOffset(usesOffhandSkillSource())[0];
    }

    public double getSkillSourceWorldY() {
        return getWorldY() + weaponCastOffset(usesOffhandSkillSource())[1];
    }

    /** Sample the release pose inside the latest tick without advancing live animation. */
    public Point2D.Double sampleSkillSourceAt(double elapsed, double secondsAgo) {
        if (!Double.isFinite(elapsed) || !Double.isFinite(secondsAgo)) {
            return new Point2D.Double(getSkillSourceWorldX(), getSkillSourceWorldY());
        }
        double savedAttack = attackAnimationTime, savedVisual = visualTime;
        double savedGait = animationTime, savedBlend = locomotionBlend;
        double savedDirectionX = motionDirectionX, savedDirectionY = motionDirectionY;
        double savedPrimary = daggerPrimaryTurnCarry, savedOffhand = daggerOffhandTurnCarry;
        double rewind = Math.max(0, Math.min(1.5, secondsAgo));
        try {
            attackAnimationTime = Math.max(1e-9, attackAnimationDuration - elapsed);
            visualTime = (visualTime - rewind + 78.2) % 78.2;
            double ix = isMovementLocked() ? 0 : horizontalInput();
            double iy = isMovementLocked() ? 0 : verticalInput();
            double length = Math.hypot(ix, iy);
            boolean moving = length > 0;
            double target = moving ? 1 : 0, response = moving ? 12 : 9;
            locomotionBlend = Math.max(0, Math.min(1, target + (savedBlend - target) * Math.exp(response * rewind)));
            double gaitTime = target * rewind + (locomotionBlend - target) * (1 - Math.exp(-response * rewind)) / response;
            double period = 4 / Math.max(1, animationSpeed);
            animationTime = ((savedGait - Math.max(0, gaitTime) * 1.55) % period + period) % period;
            double dx = moving ? ix / length : 0, dy = moving ? iy / length : 0;
            motionDirectionX = dx + (savedDirectionX - dx) * Math.exp(10 * rewind);
            motionDirectionY = dy + (savedDirectionY - dy) * Math.exp(10 * rewind);
            daggerPrimaryTurnCarry = savedPrimary * Math.exp(14 * rewind);
            daggerOffhandTurnCarry = savedOffhand * Math.exp(14 * rewind);
            double[] offset = weaponCastOffset(usesOffhandSkillSource());
            return new Point2D.Double(getWorldX() + offset[0], getWorldY() + offset[1]);
        } finally {
            attackAnimationTime = savedAttack; visualTime = savedVisual;
            animationTime = savedGait; locomotionBlend = savedBlend;
            motionDirectionX = savedDirectionX; motionDirectionY = savedDirectionY;
            daggerPrimaryTurnCarry = savedPrimary; daggerOffhandTurnCarry = savedOffhand;
        }
    }

    /** World angle of the weapon's grip-to-source vector, including the body pose. */
    public double getSkillSourceRotation() {
        boolean offhand = usesOffhandSkillSource();
        WeaponPose pose = animatedWeaponPose(offhand, spriteWidth * spriteScale,
                spriteHeight * spriteScale);
        Point2D.Double grip = new Point2D.Double(pose.weaponX, pose.weaponY);
        characterTransform(0, 0).transform(grip, grip);
        double[] tip = weaponCastOffset(offhand);
        return Math.atan2(tip[1] - grip.y, tip[0] - grip.x);
    }

    public double getAttackAnimationProgress() {
        return attackAnimationTime <= 0.0 ? 1.0
                : Math.max(0.0, Math.min(1.0, 1.0 - attackAnimationTime / attackAnimationDuration));
    }

    public AbilityAnimationTiming.State getSkillState() {
        if (!isSkillAnimationActive()) return AbilityAnimationTiming.State.IDLE;
        return AbilityAnimationTiming.stateAt(activeAbility,
                attackAnimationDuration - attackAnimationTime);
    }

    private boolean usesOffhandSkillSource() {
        return "shield_bash".equals(attackSkillId)
                && heldOffhandSprite != null;
    }

    private double[] weaponCastOffset(boolean offhand) {
        WeaponPose pose = animatedWeaponPose(offhand, spriteWidth * spriteScale,
                spriteHeight * spriteScale);
        double tipX = pose.weaponPivotX;
        double tipY = pose.weaponPivotY;
        if ("sword_shield".equals(defaultWeaponStyle)) {
            tipX = offhand ? (spriteRow == 0 || spriteRow == 2 ? 0.50 : 0.96) : 0.09;
            tipY = offhand ? (spriteRow == 0 ? 0.10 : spriteRow == 2 ? 0.94 : 0.52) : 0.94;
        } else if ("daggers".equals(defaultWeaponStyle)) {
            tipX = offhand ? 0.91 : 0.09;
            tipY = 0.92;
        } else if ("holy_staff".equals(defaultWeaponStyle)) {
            tipX = 202.0 / 387.0;
            tipY = 0.25;
        } else if ("elemental_staff".equals(defaultWeaponStyle)) {
            tipX = 0.59;
            tipY = 0.25;
        } else if ("staff".equals(defaultWeaponStyle)) {
            tipY = 0.10;
        } else if ("greatshield".equals(defaultWeaponStyle)) {
            // The force and damage originate at the physical rim, not the body center.
            if ("earthbreaker".equals(guardianSkillId) && attackAnimationTime > 0.0) {
                tipX = 0.50;
                tipY = 0.98;
            } else if (spriteRow == 0 || spriteRow == 2) {
                tipX = 0.50;
                tipY = spriteRow == 0 ? 0.12 : 0.72;
            } else {
                tipX = 0.96;
                tipY = 0.50;
            }
        }
        BufferedImage image = offhand ? heldOffhandSprite : heldPrimarySprite;
        Rectangle bounds = image == null ? new Rectangle(0, 0, 1, 1)
                : visibleWeaponBounds(image);
        double scale = pose.drawHeight / (double) bounds.height;
        double x = (tipX - pose.weaponPivotX) * bounds.width * scale * (pose.flipX ? -1 : 1);
        double y = (tipY - pose.weaponPivotY) * pose.drawHeight;
        double cosine = Math.cos(pose.weaponRotation);
        double sine = Math.sin(pose.weaponRotation);
        Point2D.Double tip = new Point2D.Double(pose.weaponX + x * cosine - y * sine,
                pose.weaponY + x * sine + y * cosine);
        // Use the exact body transform used by rendering, including the foot pivot.
        characterTransform(0, 0).transform(tip, tip);
        return new double[] {tip.x, tip.y};
    }

    public void playAttackAnimation(AbilityDefinition definition) {
        playAttackAnimation(definition, attackAnimationDuration);
    }

    public void playAttackAnimation(AbilityDefinition definition, double duration) {
        if ("greatshield".equals(defaultWeaponStyle)) {
            // A skill can interrupt a basic bash; ease out of its actual arm/body pose.
            guardianMotionCarry = guardianMotion();
            guardianMotionCarry.lift -= guardianFortressBlend * 5.0;
            guardianMotionCarry.reach -= guardianFortressBlend * 1.5;
            guardianMotionCarry.rotation -= guardianFortressBlend * -0.06;
            guardianMotionCarry.crouch -= guardianFortressBlend * 0.7;
        }
        int width = spriteWidth * spriteScale;
        int height = spriteHeight * spriteScale;
        WeaponPose primary = animatedWeaponPose(false, width, height);
        WeaponPose offhand = animatedWeaponPose(true, width, height);
        WeaponPose primaryBase = getWeaponPose(false, width, height);
        WeaponPose offhandBase = getWeaponPose(true, width, height);
        primaryAttackCarry = Math.IEEEremainder(primary.weaponRotation
                - primaryBase.weaponRotation, Math.PI * 2.0);
        offhandAttackCarry = Math.IEEEremainder(offhand.weaponRotation
                - offhandBase.weaponRotation, Math.PI * 2.0);
        primaryReachCarryX = primary.weaponX - primaryBase.weaponX;
        primaryReachCarryY = primary.weaponY - primaryBase.weaponY;
        offhandReachCarryX = offhand.weaponX - offhandBase.weaponX;
        offhandReachCarryY = offhand.weaponY - offhandBase.weaponY;
        attackMotionCarry = currentAttackMotion();
        attackAnimationDuration = Math.max(0.12, Double.isFinite(duration) ? duration : 0.34);
        attackAnimationTime = attackAnimationDuration;
        attackSequence++;
        attackWeaponStyle = weaponStyleFor(definition);
        attackSkillId = definition == null ? "" : definition.getId();
        activeAbility = definition;
        guardianSkillId = definition == null ? "" : definition.getId();
        attackStrikeProgress = definition == null ? ("greatshield".equals(defaultWeaponStyle) ? 0.52 : 0.43)
                : definition.getAbilityClass() == AbilityClass.BLACK_KNIGHT
                        || definition.getAbilityClass() == AbilityClass.ASSASSIN
                        ? AbilityAnimationTiming.hitProgress(definition)[0]
                        : AbilityAnimationTiming.releaseProgress(definition);
        double[] skillHits = definition == null ? null : AbilityAnimationTiming.hitProgress(definition);
        boolean meleeCombo = ("sword_shield".equals(defaultWeaponStyle)
                || "daggers".equals(defaultWeaponStyle)) && skillHits != null && skillHits.length > 1;
        attackBeatProgress = meleeCombo ? skillHits : new double[] {attackStrikeProgress};
        autoStaffRecoil = definition == null && defaultWeaponStyle.contains("staff");
        skillAnimation = definition != null;
    }

    public boolean isSkillAnimationActive() {
        return skillAnimation && attackAnimationTime > 0.0;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY) {
        graphics = (Graphics2D) graphics.create();
        drawCharacter(graphics, centerX, centerY, currentSpriteColumn());
        int renderedWidth = spriteWidth * spriteScale;
        int renderedHeight = spriteHeight * spriteScale;
        int playerX = centerX - renderedWidth / 2;
        int playerY = centerY - renderedHeight / 2;
        int healthBarY = playerY + renderedHeight + HEALTH_BAR_GAP;
        int currentHealthWidth = (int) (renderedWidth * health / maxHealth);
        if (health < maxHealth) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(6, 10, 18, 185));
            graphics.fillRoundRect(playerX, healthBarY, renderedWidth, HEALTH_BAR_HEIGHT, 4, 4);
            graphics.setPaint(new GradientPaint(playerX, healthBarY, new Color(148, 55, 72),
                    playerX + renderedWidth, healthBarY, new Color(231, 107, 101)));
            graphics.fillRoundRect(playerX, healthBarY, currentHealthWidth, HEALTH_BAR_HEIGHT, 4, 4);
        }
        graphics.dispose();
    }

    /** Selection portraits use the same hands, equipment and layers as gameplay. */
    public void drawPreview(Graphics2D graphics, Rectangle bounds) {
        drawPreview(graphics, bounds, false);
    }

    /** Class selection samples original body/equipment pixels without image filters. */
    public void drawPixelArtPreview(Graphics2D graphics, Rectangle bounds) {
        drawPreview(graphics, bounds, true);
    }

    private void drawPreview(Graphics2D graphics, Rectangle bounds, boolean sourcePixels) {
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }
        double scale = Math.min(bounds.width / (double) (spriteWidth * spriteScale),
                bounds.height / (double) (spriteHeight * spriteScale));
        AffineTransform deviceTransform = graphics.getTransform();
        double deviceScale = Math.min(Math.hypot(deviceTransform.getScaleX(), deviceTransform.getShearY()),
                Math.hypot(deviceTransform.getShearX(), deviceTransform.getScaleY()));
        double pixelScale = scale * spriteScale * deviceScale;
        // Whole device pixels keep enlarged portraits crisp, including on HiDPI displays.
        if (pixelScale >= 1.0) {
            scale = Math.floor(pixelScale + 1e-9) / (spriteScale * deviceScale);
        }
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        Point2D deviceCenter = deviceTransform.transform(center, null);
        deviceCenter.setLocation(Math.rint(deviceCenter.getX()), Math.rint(deviceCenter.getY()));
        try {
            center = deviceTransform.inverseTransform(deviceCenter, null);
        } catch (NoninvertibleTransformException ignored) {
            // Degenerate offscreen transforms have no visible pixels to align.
        }
        Graphics2D previewGraphics = (Graphics2D) graphics.create();
        previewGraphics.translate(center.getX(), center.getY());
        previewGraphics.scale(scale, scale);
        drawCharacter(previewGraphics, 0, 0, 1, sourcePixels);
        previewGraphics.dispose();
    }

    private int currentSpriteColumn() {
        // The walk cycle is columns 0, 1, 2, 1; column 1 is the idle frame.
        int animationFrame = (int) (animationTime * animationSpeed) % 4;
        return locomotionBlend > 0.06 ? (animationFrame == 3 ? 1 : animationFrame) : 1;
    }

    private void drawCharacter(Graphics2D graphics, int centerX, int centerY, int spriteColumn) {
        drawCharacter(graphics, centerX, centerY, spriteColumn, false);
    }

    private void drawCharacter(Graphics2D graphics, int centerX, int centerY, int spriteColumn, boolean sourcePixels) {
        Graphics2D layer = characterLayer.createGraphics();
        try {
            if (sourcePixels) PixelArtRenderer.configure(layer);
            layer.setComposite(AlphaComposite.Clear);
            layer.fillRect(0, 0, characterLayer.getWidth(), characterLayer.getHeight());
            layer.setComposite(AlphaComposite.SrcOver);
            renderCharacter(layer, characterLayer.getWidth() / 2,
                    characterLayer.getHeight() / 2, spriteColumn, sourcePixels);
        } finally {
            layer.dispose();
        }
        // Resolve motion and equipment on the source grid before enlarging the pixels.
        Graphics2D display = (Graphics2D) graphics.create();
        try {
            if (sourcePixels) PixelArtRenderer.configure(display);
            display.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            display.drawImage(characterLayer, centerX - characterLayer.getWidth() / 2,
                    centerY - characterLayer.getHeight() / 2, null);
        } finally {
            display.dispose();
        }
    }

    private void renderCharacter(Graphics2D graphics, int centerX, int centerY, int spriteColumn, boolean sourcePixels) {
        int renderedWidth = spriteWidth * spriteScale;
        int renderedHeight = spriteHeight * spriteScale;
        int playerX = -renderedWidth / 2;
        int playerY = -renderedHeight / 2;

        graphics = (Graphics2D) graphics.create();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawContactShadow(graphics, centerX, centerY, renderedWidth, renderedHeight);
        graphics.transform(characterTransform(centerX, centerY));
        if (isSkillAnimationActive() && "shadow_step".equals(attackSkillId)) {
            // Fade across the actual dash; keep the moving body visible as it returns.
            graphics.setComposite(AlphaComposite.SrcOver.derive((float) blinkOpacity()));
        }

        drawWeapons(graphics, 0, 0, renderedWidth, renderedHeight, true, sourcePixels);

        // A separate frame prevents rotated/scaled sampling from touching adjacent poses.
        BufferedImage frame = (sourcePixels ? previewSpriteFrames : spriteFrames)[spriteRow][spriteColumn];
        graphics.drawImage(frame,
                playerX, playerY, renderedWidth, renderedHeight, null);
        if ("greatshield".equals(defaultWeaponStyle)) {
            drawGuardianShieldArm(graphics, renderedWidth, renderedHeight);
        } else if (isSkillAnimationActive()) {
            drawSkillArms(graphics, renderedWidth, renderedHeight, frame);
        }
        drawWeapons(graphics, 0, 0, renderedWidth, renderedHeight, false, sourcePixels);
        drawGripHands(graphics, 0, 0, renderedWidth, renderedHeight, spriteColumn, frame);
        graphics.dispose();
    }

    private void drawContactShadow(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight) {
        Graphics2D shadow = (Graphics2D) graphics.create();
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        double width = renderedWidth * 0.40;
        double height = renderedHeight * 0.085;
        double groundY = centerY + renderedHeight * 0.44;
        // Stay on the ground while the planted sprite breathes or lifts into a step.
        for (int layer = 0; layer < 3; layer++) {
            double inset = layer * width * 0.12;
            shadow.setColor(new Color(0, 0, 0, 15 + layer * 9));
            shadow.fill(new Ellipse2D.Double(centerX - width / 2.0 + inset,
                    groundY - height / 2.0 + layer * height * 0.13,
                    width - inset * 2.0, height * (1.0 - layer * 0.26)));
        }
        shadow.dispose();
    }

    /** Body and held equipment share one pose around planted feet. */
    private AffineTransform characterTransform(int centerX, int centerY) {
        double blend = locomotionBlend;
        double stride = animationTime * animationSpeed * Math.PI / 2.0;
        double step = Math.sin(stride);
        double breath = Math.sin(visualTime * Math.PI * 2.0 / 3.4);
        double weightShift = Math.sin(visualTime * Math.PI * 2.0 / 4.6);
        boolean caster = defaultWeaponStyle.contains("staff");
        double size = spriteHeight * spriteScale / 64.0;
        MotionPose attack = currentAttackMotion();
        double x = (step * 0.45 * blend + weightShift * 0.28 * (1.0 - blend)
                + attack.x) * size;
        double y = (-step * step * 1.15 * blend + attack.y) * size;
        double tilt = motionDirectionX * 0.036 + step * 0.009 * blend
                + weightShift * (caster ? 0.014 : 0.010) * (1.0 - blend) + attack.tilt;
        double stretchX = 1.0 - breath * 0.007 * (1.0 - blend * 0.65)
                - Math.abs(step) * 0.004 * blend + attack.stretchX;
        double stretchY = 1.0 + breath * (caster ? 0.018 : 0.014)
                * (1.0 - blend * 0.65) - Math.abs(step) * 0.006 * blend + attack.stretchY;
        double feetY = spriteHeight * spriteScale * 0.43;
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX + x, centerY + feetY + y);
        transform.rotate(tilt);
        transform.scale(stretchX, stretchY);
        transform.translate(0.0, -feetY);
        return transform;
    }

    private MotionPose currentAttackMotion() {
        MotionPose pose = new MotionPose();
        if ("greatshield".equals(defaultWeaponStyle)) {
            GuardianMotion motion = guardianMotion();
            double effort = motion.lean;
            pose.x = recentMoveX * effort * 2.8;
            pose.y = motion.crouch + recentMoveY * effort * 0.8;
            pose.tilt = recentMoveX * effort * 0.07;
            pose.stretchX = motion.roar * 0.014;
            pose.stretchY = -motion.crouch * 0.007 + motion.roar * 0.018;
            return pose;
        }
        if (attackAnimationTime <= 0.0) {
            return pose;
        }
        double elapsedProgress = 1.0 - attackAnimationTime / attackAnimationDuration;
        if (skillAnimation && hasDistinctSkillMotion()) {
            pose = skillBodyMotion(elapsedProgress);
            double carry = attackCarryWeight(elapsedProgress);
            pose.x += attackMotionCarry.x * carry;
            pose.y += attackMotionCarry.y * carry;
            pose.tilt += attackMotionCarry.tilt * carry;
            pose.stretchX += attackMotionCarry.stretchX * carry;
            pose.stretchY += attackMotionCarry.stretchY * carry;
            return pose;
        }
        double progress = strikeAlignedProgress(elapsedProgress);
        boolean dagger = "daggers".equals(defaultWeaponStyle);
        double effort = dagger ? daggerSwingEffort(elapsedProgress)
                : autoStaffRecoil ? recoilEffort(elapsedProgress) : attackEffort(progress);
        double windup = progress < 0.24 ? WeaponPose.smoother(progress / 0.24)
                : 1.0 - WeaponPose.smoother((progress - 0.24) / 0.31);
        if (dagger) {
            windup = Math.max(0.0, -effort) * 0.6;
        }
        if (autoStaffRecoil) {
            windup = Math.abs(effort) * 0.3;
        }
        boolean caster = attackWeaponStyle.contains("staff");
        double side = Math.abs(recentMoveX) > 0.15 ? recentMoveX : heldWeaponSideX * 0.60;
        pose.x = recentMoveX * effort * (caster ? 1.15 : 2.05);
        pose.y = recentMoveY * effort * (caster ? 0.30 : 0.65);
        pose.tilt = side * effort * (caster ? 0.038 : 0.075)
                * (dagger ? 1.0 : attackBeatDirection(elapsedProgress));
        pose.stretchX = windup * 0.008;
        pose.stretchY = -windup * (caster ? 0.018 : 0.026);
        double carry = attackCarryWeight(elapsedProgress);
        pose.x += attackMotionCarry.x * carry;
        pose.y += attackMotionCarry.y * carry;
        pose.tilt += attackMotionCarry.tilt * carry;
        pose.stretchX += attackMotionCarry.stretchX * carry;
        pose.stretchY += attackMotionCarry.stretchY * carry;
        return pose;
    }

    private boolean hasDistinctSkillMotion() {
        return switch (attackSkillId) {
            case "heavy_slash", "shield_bash", "earth_shatter", "knights_wrath",
                    "shadow_strike", "shadow_step", "death_mark", "twin_fang",
                    "heal", "holy_bolt", "holy_shield", "divine_light",
                    "flame_burst", "ice_shard", "lightning_strike", "elemental_storm" -> true;
            default -> false;
        };
    }

    /** A planted preparation, accelerated release, follow-through and soft recovery. */
    private double skillCurve(double p, double prepare, double strike, double follow) {
        double hit = Math.max(0.12, Math.min(0.82, attackStrikeProgress));
        double windup = hit * 0.46;
        double followTime = hit + (1.0 - hit) * 0.36;
        double arrivalSpeed = (strike - prepare) / (hit - windup);
        double followSpeed = (follow - strike) / (followTime - hit);
        // A cutting blade keeps moving through contact; a thrust or staff lift can plant.
        double hitSpeed = arrivalSpeed * followSpeed > 0.0
                ? Math.copySign(Math.min(Math.abs(arrivalSpeed), Math.abs(followSpeed)), arrivalSpeed)
                : 0.0;
        if (p < windup) {
            return WeaponPose.lerp(0.0, prepare, WeaponPose.smoother(p / windup));
        }
        if (p < hit) {
            return hermite(prepare, strike, 0.0, hitSpeed * (hit - windup),
                    (p - windup) / (hit - windup));
        }
        if (p < followTime) {
            return hermite(strike, follow, hitSpeed * (followTime - hit), 0.0,
                    (p - hit) / (followTime - hit));
        }
        return follow * (1.0 - WeaponPose.smoother((p - followTime) / (1.0 - followTime)));
    }

    private static double hermite(double from, double to, double fromVelocity,
            double toVelocity, double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        double t2 = t * t;
        double t3 = t2 * t;
        return (2.0 * t3 - 3.0 * t2 + 1.0) * from
                + (t3 - 2.0 * t2 + t) * fromVelocity
                + (-2.0 * t3 + 3.0 * t2) * to
                + (t3 - t2) * toVelocity;
    }

    private double skillSpin(double p, double turns) {
        double begin = Math.min(0.16, attackBeatProgress[0] * 0.5);
        return Math.PI * 2.0 * turns * WeaponPose.smoother((p - begin) / (0.86 - begin));
    }

    private double skillEnvelope(double p) {
        return WeaponPose.smoother(p / 0.18)
                * (1.0 - WeaponPose.smoother((p - 0.78) / 0.22));
    }

    private double blinkOpacity() {
        double p = getAttackAnimationProgress();
        double release = activeAbility == null ? attackStrikeProgress
                : AbilityAnimationTiming.releaseProgress(activeAbility);
        double fadeStart = release * 0.52;
        double fadeOut = WeaponPose.smoother((p - fadeStart)
                / Math.max(0.04, release - fadeStart));
        double fadeIn = WeaponPose.smoother((p - release - 0.09) / 0.22);
        return 1.0 - 0.88 * fadeOut * (1.0 - fadeIn);
    }

    private MotionPose skillBodyMotion(double p) {
        MotionPose pose = new MotionPose();
        double side = heldWeaponSideX;
        double effort = skillCurve(p, -0.35, 1.0, 0.48);
        double envelope = skillEnvelope(p);
        switch (attackSkillId) {
            case "heavy_slash" -> {
                pose.x = recentMoveX * effort * 2.8;
                pose.y = recentMoveY * effort * 0.9;
                pose.tilt = side * effort * 0.09;
                pose.stretchY = -skillCurve(p, 0.7, 0.12, 0.0) * 0.026;
            }
            case "shield_bash" -> {
                pose.x = recentMoveX * effort * 3.8;
                pose.y = recentMoveY * effort * 1.4 + skillCurve(p, 0.7, 0.9, 0.4);
                pose.tilt = side * effort * 0.09;
            }
            case "earth_shatter" -> {
                pose.x = recentMoveX * effort * 1.5;
                pose.y = skillCurve(p, 1.3, -2.8, -1.1);
                pose.tilt = side * skillCurve(p, 0.045, -0.055, -0.025);
                pose.stretchY = skillCurve(p, -0.024, 0.02, 0.008);
            }
            case "knights_wrath", "twin_fang" -> {
                double spin = skillSpin(p, "knights_wrath".equals(attackSkillId) ? 2.0 : 1.0);
                pose.x = Math.sin(spin) * envelope * 1.8;
                pose.y = -Math.abs(Math.sin(spin)) * envelope * 1.4;
                pose.tilt = side * Math.sin(spin) * envelope * 0.15;
                pose.stretchX = -Math.abs(Math.sin(spin)) * envelope * 0.055;
            }
            case "shadow_strike" -> {
                pose.x = recentMoveX * effort * 3.1;
                pose.y = recentMoveY * effort * 1.0 + skillCurve(p, 1.2, -1.0, 0.4);
                pose.tilt = side * effort * 0.105;
            }
            case "shadow_step" -> {
                pose.x = recentMoveX * effort * 2.2;
                pose.y = envelope * 1.3;
                pose.tilt = side * effort * 0.10;
                pose.stretchY = -envelope * 0.025;
            }
            case "death_mark" -> {
                pose.y = -skillCurve(p, 0.3, 0.7, 0.3);
                pose.tilt = side * skillCurve(p, -0.025, 0.042, 0.02);
            }
            default -> {
                boolean elementalist = "elemental_staff".equals(defaultWeaponStyle);
                pose.x = recentMoveX * effort * (elementalist ? 1.5 : 0.65);
                pose.y = -skillCurve(p, 0.25, elementalist ? 1.25 : 0.7, 0.4);
                pose.tilt = side * effort * (elementalist ? 0.048 : 0.027);
                pose.stretchY = skillCurve(p, -0.014, 0.011, 0.003);
                if ("elemental_storm".equals(attackSkillId)) {
                    pose.y -= envelope * 1.1;
                    pose.stretchX += envelope * 0.018;
                }
            }
        }
        return pose;
    }

    private static class GuardianMotion {
        private double lift;
        private double reach;
        private double rotation;
        private double lean;
        private double crouch;
        private double roar;
    }

    /** Shield poses have physical windup, extension, impact and recovery beats. */
    private GuardianMotion guardianMotion() {
        GuardianMotion pose = new GuardianMotion();
        pose.lift = guardianFortressBlend * 5.0;
        pose.reach = guardianFortressBlend * 1.5;
        pose.rotation = guardianFortressBlend * -0.06;
        pose.crouch = guardianFortressBlend * 0.7;
        if (attackAnimationTime <= 0.0) {
            return pose;
        }
        double p = Math.max(0.0, Math.min(1.0,
                1.0 - attackAnimationTime / attackAnimationDuration));
        double side = spriteRow == 3 ? -1.0 : 1.0;
        switch (guardianSkillId) {
            case "shield_fortress" -> {
                double brace = p < attackStrikeProgress
                        ? WeaponPose.smoother(p / attackStrikeProgress) : 1.0;
                double recover = guardianFortressRemaining > 0.0 ? 1.0
                        : 1.0 - WeaponPose.smoother((p - 0.76) / 0.24);
                double settle = guardianFortressRemaining > 0.0
                        ? WeaponPose.smoother((p - 0.68) / 0.32) : 0.0;
                pose.lift = Math.max(pose.lift, brace * recover * WeaponPose.lerp(7.0, 5.0, settle));
                pose.reach = Math.max(pose.reach, brace * recover * WeaponPose.lerp(2.0, 1.5, settle));
                pose.rotation -= brace * recover * (1.0 - settle) * 0.04 * side;
                pose.crouch = Math.max(pose.crouch, brace * recover * WeaponPose.lerp(1.2, 0.7, settle));
            }
            case "iron_charge" -> {
                double prepare = WeaponPose.smoother(p / attackStrikeProgress);
                double recover = 1.0 - WeaponPose.smoother((p - 0.80) / 0.20);
                double launch = WeaponPose.smoother((p - attackStrikeProgress) / 0.12);
                pose.lift -= prepare * recover * 2.2;
                pose.reach += (prepare * 1.0 + launch * 4.5) * recover;
                pose.rotation += side * prepare * recover * 0.14;
                pose.lean = prepare * recover * (0.3 + launch * 1.2);
                pose.crouch += prepare * recover * 1.5;
            }
            case "earthbreaker" -> {
                double windup = attackStrikeProgress * 0.61;
                if (p < windup) {
                    double raise = WeaponPose.smoother(p / windup);
                    pose.lift += raise * 15.0;
                    pose.rotation -= side * raise * 0.42;
                    pose.crouch -= raise * 0.8;
                } else if (p < attackStrikeProgress) {
                    double slam = WeaponPose.smoother((p - windup) / (attackStrikeProgress - windup));
                    pose.lift += WeaponPose.lerp(15.0, -1.8 - guardianFortressBlend * 5.0, slam);
                    pose.reach += slam * 3.5;
                    pose.rotation -= side * (1.0 - slam) * 0.42;
                    pose.crouch += slam * 1.8;
                    pose.lean = slam * 0.5;
                } else {
                    double recover = 1.0 - WeaponPose.smoother((p - attackStrikeProgress)
                            / (1.0 - attackStrikeProgress));
                    pose.lift -= (1.8 + guardianFortressBlend * 5.0) * recover;
                    pose.reach += 3.5 * recover;
                    pose.crouch += 1.8 * recover;
                    pose.lean = 0.5 * recover;
                }
            }
            case "guardians_roar" -> {
                double roar = p < attackStrikeProgress
                        ? WeaponPose.smoother(p / attackStrikeProgress)
                        : 1.0 - WeaponPose.smoother((p - attackStrikeProgress) / (1.0 - attackStrikeProgress));
                pose.lift += roar * 6.0;
                pose.reach += roar;
                pose.rotation -= side * roar * 0.10;
                pose.roar = roar;
                pose.crouch -= roar * 0.6;
            }
            default -> {
                double aligned = strikeAlignedProgress(p);
                if (aligned < 0.24) {
                    double raise = WeaponPose.smoother(aligned / 0.24);
                    pose.lift += raise * 6.0;
                    pose.reach -= raise;
                    pose.rotation -= side * raise * 0.13;
                    pose.lean = -raise * 0.35;
                } else if (aligned < 0.55) {
                    double bash = WeaponPose.smoother((aligned - 0.24) / 0.31);
                    pose.lift += WeaponPose.lerp(6.0, 1.0, bash);
                    pose.reach += WeaponPose.lerp(-1.0, 7.0, bash);
                    pose.rotation += side * WeaponPose.lerp(-0.13, 0.11, bash);
                    pose.lean = WeaponPose.lerp(-0.35, 1.0, bash);
                } else {
                    double recover = 1.0 - WeaponPose.smoother((aligned - 0.55) / 0.45);
                    pose.lift += recover;
                    pose.reach += recover * 7.0;
                    pose.rotation += side * recover * 0.11;
                    pose.lean = recover;
                }
            }
        }
        double carry = 1.0 - WeaponPose.smoother(p / 0.18);
        pose.lift += guardianMotionCarry.lift * carry;
        pose.reach += guardianMotionCarry.reach * carry;
        pose.rotation += guardianMotionCarry.rotation * carry;
        pose.lean += guardianMotionCarry.lean * carry;
        pose.crouch += guardianMotionCarry.crouch * carry;
        pose.roar += guardianMotionCarry.roar * carry;
        return pose;
    }

    private void drawGuardianArmorGlints(Graphics2D graphics, int width, int height) {
        double scale = height / 64.0;
        boolean wounded = health < maxHealth * 0.40;
        int alpha = wounded ? 135 : 52;
        graphics.setStroke(new BasicStroke((float) Math.max(0.7, scale)));
        graphics.setColor(new Color(244, 197, 88, alpha));
        double chestY = -height * 0.05;
        // Short armor seams and drifting motes; the passive has no ground aura.
        graphics.drawLine((int) (-width * 0.18), (int) (chestY - height * 0.06),
                (int) (-width * 0.12), (int) chestY);
        graphics.drawLine((int) (width * 0.12), (int) chestY,
                (int) (width * 0.18), (int) (chestY - height * 0.06));
        for (int i = 0; i < (wounded ? 5 : 2); i++) {
            double phase = (visualTime * 0.58 + i * 0.29) % 1.0;
            int x = (int) Math.round(Math.sin(i * 2.31) * width * 0.25);
            int y = (int) Math.round(height * 0.23 - phase * height * 0.43);
            int fade = (int) (Math.sin(phase * Math.PI) * alpha);
            graphics.setColor(new Color(255, 217, 123, Math.max(0, fade)));
            graphics.fillRect(x, y, Math.max(1, (int) Math.round(scale)),
                    Math.max(1, (int) Math.round(scale)));
        }
    }

    private void drawGuardianShieldArm(Graphics2D graphics, int width, int height) {
        WeaponPose pose = animatedWeaponPose(false, width, height);
        if (pose.behindCharacter) {
            return;
        }
        int[] source = handPixel(false);
        double startX = (source[0] / (double) spriteWidth - 0.5) * width;
        double startY = (source[1] / (double) spriteHeight - 0.5) * height;
        double scale = height / 64.0;
        graphics.setStroke(new BasicStroke((float) (4.0 * scale),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(80, 72, 59));
        graphics.draw(new java.awt.geom.Line2D.Double(startX, startY,
                pose.weaponX, pose.weaponY));
        graphics.setStroke(new BasicStroke((float) (2.1 * scale),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(202, 149, 62));
        graphics.draw(new java.awt.geom.Line2D.Double(startX, startY,
                pose.weaponX, pose.weaponY));
    }

    private static double attackEffort(double progress) {
        if (progress < 0.24) {
            return -0.42 * WeaponPose.smoother(progress / 0.24);
        }
        if (progress < 0.55) {
            return WeaponPose.lerp(-0.42, 1.0, WeaponPose.smoother((progress - 0.24) / 0.31));
        }
        if (progress < 0.78) {
            return WeaponPose.lerp(1.0, 0.18, WeaponPose.smoother((progress - 0.55) / 0.23));
        }
        return 0.18 * (1.0 - WeaponPose.smoother((progress - 0.78) / 0.22));
    }

    private static double attackCarryWeight(double progress) {
        return 1.0 - WeaponPose.smoother(progress / 0.26);
    }

    private static double recoilEffort(double progress) {
        // Automatic bolts leave immediately; the body responds with a quick recoil.
        return progress < 0.15 ? -0.62 * WeaponPose.smoother(progress / 0.15)
                : -0.62 * (1.0 - WeaponPose.smoother((progress - 0.15) / 0.70));
    }

    private double strikeAlignedProgress(double progress) {
        // Each combo returns continuously through its grip before the next strike.
        // Ranged skills extend on launch, while melee skills extend on damage frames.
        int beat = attackBeatIndex(progress);
        double start = beat == 0 ? 0.0
                : (attackBeatProgress[beat - 1] + attackBeatProgress[beat]) * 0.5;
        double end = beat == attackBeatProgress.length - 1 ? 1.0
                : (attackBeatProgress[beat] + attackBeatProgress[beat + 1]) * 0.5;
        double strike = Math.max(start + 0.025, Math.min(end - 0.025, attackBeatProgress[beat]));
        return progress <= strike ? (progress - start) / (strike - start) * 0.55
                : 0.55 + (progress - strike) / (end - strike) * 0.45;
    }

    private int attackBeatIndex(double progress) {
        for (int beat = 0; beat < attackBeatProgress.length - 1; beat++) {
            double boundary = (attackBeatProgress[beat] + attackBeatProgress[beat + 1]) * 0.5;
            if (progress < boundary) {
                return beat;
            }
        }
        return attackBeatProgress.length - 1;
    }

    private double attackBeatDirection(double progress) {
        return attackBeatIndex(progress) % 2 == 0 ? 1.0 : -1.0;
    }

    private double daggerSwingEffort(double progress) {
        double firstHit = attackBeatProgress[0];
        double windupEnd = firstHit * 0.38;
        if (progress < windupEnd) {
            return -0.34 * WeaponPose.smoother(progress / windupEnd);
        }
        if (progress < firstHit) {
            return WeaponPose.lerp(-0.34, 1.0,
                    WeaponPose.smoother((progress - windupEnd) / (firstHit - windupEnd)));
        }
        // Move directly from one blade extension to the next, without squeezing
        // another full windup/strike/recovery into every short combo interval.
        for (int beat = 1; beat < attackBeatProgress.length; beat++) {
            if (progress < attackBeatProgress[beat]) {
                double from = attackBeatProgress[beat - 1];
                double to = attackBeatProgress[beat];
                double previous = beat % 2 == 1 ? 1.0 : -1.0;
                return WeaponPose.lerp(previous, -previous,
                        WeaponPose.smoother((progress - from) / (to - from)));
            }
        }
        double lastHit = attackBeatProgress[attackBeatProgress.length - 1];
        double lastExtension = attackBeatProgress.length % 2 == 1 ? 1.0 : -1.0;
        return lastExtension * (1.0 - WeaponPose.smoother((progress - lastHit) / (1.0 - lastHit)));
    }

    private static class MotionPose {
        private double x;
        private double y;
        private double tilt;
        private double stretchX;
        private double stretchY;
    }

    private void drawWeapons(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight, boolean behindCharacter, boolean sourcePixels) {
        drawAttachedWeapon(graphics, centerX, centerY, renderedWidth, renderedHeight,
                heldPrimarySprite, false, behindCharacter, sourcePixels);
        drawAttachedWeapon(graphics, centerX, centerY, renderedWidth, renderedHeight,
                heldOffhandSprite, true, behindCharacter, sourcePixels);
    }

    private void drawAttachedWeapon(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight, BufferedImage image,
            boolean offhand, boolean behindCharacter, boolean sourcePixels) {
        if (offhand && image == null) {
            return;
        }
        WeaponPose pose = animatedWeaponPose(offhand, renderedWidth, renderedHeight);
        if (pose.behindCharacter != behindCharacter) {
            return;
        }

        double attackProgress = -1.0;
        if (attackAnimationTime > 0.0) {
            attackProgress = attackAnimationDuration <= 0.0 ? 0.0
                    : strikeAlignedProgress(1.0 - attackAnimationTime / attackAnimationDuration);
        }
        if (attackProgress >= 0.0 && !skillAnimation) {
            pose.drawAttackTrail(graphics, centerX, centerY, attackProgress);
        }

        Graphics2D weaponGraphics = (Graphics2D) graphics.create();
        Object previousInterpolation = weaponGraphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        if (sourcePixels) PixelArtRenderer.configure(weaponGraphics);
        weaponGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                !sourcePixels && ("daggers".equals(defaultWeaponStyle) || "greatshield".equals(defaultWeaponStyle))
                        ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX + pose.weaponX, centerY + pose.weaponY);
        transform.rotate(pose.weaponRotation);
        if (image == null) {
            weaponGraphics.transform(transform);
            if (!offhand) {
                drawProceduralWeapon(weaponGraphics, pose.drawHeight, attackAnimationTime > 0.0);
            }
        } else if (!sourcePixels && ("daggers".equals(defaultWeaponStyle) || "greatshield".equals(defaultWeaponStyle))) {
            BufferedImage filtered = filteredHeldWeapon(image);
            double scale = pose.drawHeight / (double) filtered.getHeight();
            transform.scale(pose.flipX ? -scale : scale, scale);
            transform.translate(-filtered.getWidth() * pose.weaponPivotX,
                    -filtered.getHeight() * pose.weaponPivotY);
            weaponGraphics.drawImage(filtered, transform, null);
            if ("greatshield".equals(defaultWeaponStyle) && guardianBlockTime > 0.0) {
                weaponGraphics.setComposite(AlphaComposite.SrcOver.derive(
                        (float) (guardianBlockTime / 0.20 * 0.68)));
                weaponGraphics.drawImage(blockFlashImage(filtered), transform, null);
            }
        } else {
            Rectangle visibleBounds = visibleWeaponBounds(image);
            pose.weaponScale = pose.drawHeight / (double) visibleBounds.height;
            transform.scale(pose.flipX ? -pose.weaponScale : pose.weaponScale,
                    pose.weaponScale);
            transform.translate(-visibleBounds.x - visibleBounds.width * pose.weaponPivotX,
                    -visibleBounds.y - visibleBounds.height * pose.weaponPivotY);
            weaponGraphics.drawImage(image, transform, null);
            if (sourcePixels && "greatshield".equals(defaultWeaponStyle) && guardianBlockTime > 0.0) {
                weaponGraphics.setComposite(AlphaComposite.SrcOver.derive(
                        (float) (guardianBlockTime / 0.20 * 0.68)));
                weaponGraphics.drawImage(blockFlashImage(image), transform, null);
            }
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

    private static BufferedImage filteredHeldWeapon(BufferedImage image) {
        synchronized (FILTERED_HELD_WEAPONS) {
            return FILTERED_HELD_WEAPONS.computeIfAbsent(image, source -> {
                Rectangle bounds = visibleWeaponBounds(source);
                BufferedImage current = source.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
                int targetHeight = 44;
                int targetWidth = Math.max(1, (int) Math.round(bounds.width * targetHeight / (double) bounds.height));
                // Gradual reduction averages fine weapon detail before rotation.
                while (current.getWidth() != targetWidth || current.getHeight() != targetHeight) {
                    int width = Math.max(targetWidth, current.getWidth() / 2);
                    int height = Math.max(targetHeight, current.getHeight() / 2);
                    BufferedImage smaller = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D filter = smaller.createGraphics();
                    filter.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    filter.drawImage(current, 0, 0, width, height, null);
                    filter.dispose();
                    current = smaller;
                }
                return current;
            });
        }
    }

    private static BufferedImage blockFlashImage(BufferedImage image) {
        synchronized (BLOCK_FLASH_IMAGES) {
            return BLOCK_FLASH_IMAGES.computeIfAbsent(image, source -> {
                BufferedImage flash = new BufferedImage(source.getWidth(), source.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        flash.setRGB(x, y, (source.getRGB(x, y) & 0xff000000) | 0x00ffeac0);
                    }
                }
                return flash;
            });
        }
    }

    private WeaponPose getWeaponPose(boolean offhand, int renderedWidth, int renderedHeight) {
        WeaponPose pose = baseWeaponPose(offhand, renderedWidth, renderedHeight);
        double breath = Math.sin(visualTime * Math.PI * 2.0 / 3.4);
        double stride = Math.sin(animationTime * animationSpeed * Math.PI / 2.0);
        // Tiny wrist counter-motion stays centered on the grip, never on the body center.
        pose.weaponRotation += breath * 0.010 * (1.0 - locomotionBlend * 0.8)
                * (offhand ? -0.5 : 1.0)
                + stride * locomotionBlend * (offhand ? -0.018 : 0.026);
        if ("daggers".equals(defaultWeaponStyle)) {
            pose.weaponRotation += offhand ? daggerOffhandTurnCarry : daggerPrimaryTurnCarry;
        }
        return pose;
    }

    private WeaponPose baseWeaponPose(boolean offhand, int renderedWidth, int renderedHeight) {
        int[] hand = handPixel(offhand);
        double x = (hand[0] / (double) spriteWidth - 0.5) * renderedWidth;
        double y = (hand[1] / (double) spriteHeight - 0.5) * renderedHeight;
        double sizeScale = renderedHeight / 64.0;
        boolean back = spriteRow == 0;
        boolean left = spriteRow == 3;
        boolean handOnLeft = hand[0] < spriteWidth / 2;

        if ("greatshield".equals(defaultWeaponStyle)) {
            double angle = left ? -0.035 : 0.035;
            // Grip stays at the central arm strap, never at the texture's corner.
            return new WeaponPose(x, y, angle, (int) Math.round(40 * sizeScale),
                    0.50, 0.51, left, back);
        }

        if ("sword_shield".equals(defaultWeaponStyle)) {
            if (offhand) {
                return new WeaponPose(x, y, left ? -0.04 : 0.04,
                        (int) Math.round(22 * sizeScale), 0.50, 0.52, left,
                        back || spriteRow == 1);
            }
            double angle = Math.toRadians(handOnLeft ? 195 : 165);
            double arc = switch (spriteRow) {
                case 0 -> -0.24; // Bring the blade overhead when striking away from the camera.
                case 2 -> -2.45; // Sweep down in front of the body when facing the camera.
                default -> handOnLeft ? -0.95 : 0.95;
            };
            return new WeaponPose(x, y, angle, (int) Math.round(34 * sizeScale),
                    0.83, 0.16, handOnLeft, back,
                    arc, SWORD_TRAIL_COLOR, 3.0);
        }

        if ("daggers".equals(defaultWeaponStyle)) {
            boolean side = spriteRow == 1 || spriteRow == 3;
            // The source art has its handle at the top and tip at the bottom.
            // Turn around the calibrated handle so every stance holds the blade upright.
            boolean flip = true;
            double angle = Math.PI + Math.toRadians(handOnLeft ? 12 : -12);
            double arc = (handOnLeft ? 0.72 : -0.72) * (offhand ? 0.82 : 1.0);
            if (back) {
                arc *= 0.35;
            } else if (spriteRow == 2) {
                arc *= 0.4;
            }
            if (attackSequence % 2 == 0) {
                arc = -arc;
            }
            WeaponPose pose = new WeaponPose(x, y, angle, (int) Math.round(22 * sizeScale),
                    offhand ? 0.402 : 0.585, 0.17,
                    flip, back || (offhand && side), arc, DAGGER_TRAIL_COLOR, 3.5);
            BufferedImage dagger = offhand ? heldOffhandSprite : heldPrimarySprite;
            Rectangle bounds = visibleWeaponBounds(dagger);
            // Both asset halves curve in different directions; measure their actual tips.
            double tipX = offhand ? 0.91 : 0.09;
            double bladeX = (tipX - pose.weaponPivotX) * bounds.width
                    * pose.drawHeight / (double) bounds.height * (flip ? -1 : 1);
            double bladeY = (0.92 - pose.weaponPivotY) * pose.drawHeight;
            pose.trailBladeAngle = Math.atan2(bladeY, bladeX);
            pose.trailRadius = Math.hypot(bladeX, bladeY);
            pose.trailVerticalScale = 1.0;
            return pose;
        }

        if ("holy_staff".equals(defaultWeaponStyle) || "elemental_staff".equals(defaultWeaponStyle)) {
            boolean elementalStaff = "elemental_staff".equals(defaultWeaponStyle);
            // Preserve an outward lean so the head stays clear of the character's face.
            double angle = Math.toRadians(handOnLeft ? -8 : 8);
            return new WeaponPose(x, y, angle, (int) Math.round((elementalStaff ? 42 : 38) * sizeScale),
                    elementalStaff ? 0.249 : 202.0 / 387.0,
                    elementalStaff ? 0.654 : 488.0 / 670.0,
                    handOnLeft, back, handOnLeft ? -0.24 : 0.24,
                    elementalStaff ? ELEMENTAL_TRAIL_COLOR : HOLY_TRAIL_COLOR, 3.0);
        }

        return new WeaponPose(x, y, left ? -Math.PI * 3.0 / 4.0 : -Math.PI / 4.0,
                50, 0.50, 0.65, left, false);
    }

    private int[] handPixel(boolean offhand) {
        int column = currentSpriteColumn();
        return switch (defaultWeaponStyle) {
            case "sword_shield" -> SWORD_HANDS[spriteRow][column][offhand ? 1 : 0];
            case "daggers" -> DAGGER_HANDS[spriteRow][column][offhand ? 1 : 0];
            case "holy_staff" -> HOLY_STAFF_HANDS[spriteRow][column];
            case "elemental_staff" -> ELEMENTAL_STAFF_HANDS[spriteRow][column];
            case "greatshield" -> GREATSHIELD_HANDS[spriteRow][column];
            default -> new int[] {spriteWidth * (spriteRow == 3 ? 1 : 3) / 4,
                    spriteHeight * 5 / 8};
        };
    }

    private WeaponPose animatedWeaponPose(boolean offhand, int renderedWidth, int renderedHeight) {
        WeaponPose pose = getWeaponPose(offhand, renderedWidth, renderedHeight);
        if ("greatshield".equals(defaultWeaponStyle)) {
            GuardianMotion motion = guardianMotion();
            double scale = renderedHeight / 64.0;
            pose.weaponX += recentMoveX * motion.reach * scale;
            pose.weaponY += (recentMoveY * motion.reach - motion.lift) * scale;
            pose.weaponRotation += motion.rotation;
            return pose;
        }
        if (attackAnimationTime > 0.0) {
            double progress = 1.0 - attackAnimationTime / attackAnimationDuration;
            pose.attackDirection = attackBeatDirection(progress);
            if (skillAnimation && hasDistinctSkillMotion()) {
                applySkillWeaponMotion(pose, offhand, progress, renderedHeight / 64.0);
            } else if ("daggers".equals(defaultWeaponStyle)) {
                double extension = daggerSwingEffort(progress);
                pose.weaponRotation += pose.attackArc * extension;
                double previous = Math.max(0.0, progress - 0.045 / attackAnimationDuration);
                pose.trailSweep = pose.attackArc * (extension - daggerSwingEffort(previous));
                pose.trailOpacity = Math.min(0.46, Math.abs(pose.trailSweep) * 1.1);
            } else if (autoStaffRecoil) {
                pose.weaponRotation += pose.attackArc * recoilEffort(progress) * 0.50;
            } else {
                pose.applyAttackProgress(strikeAlignedProgress(progress));
            }
            pose.weaponRotation += (offhand ? offhandAttackCarry : primaryAttackCarry)
                    * attackCarryWeight(progress);
            pose.weaponX += (offhand ? offhandReachCarryX : primaryReachCarryX)
                    * attackCarryWeight(progress);
            pose.weaponY += (offhand ? offhandReachCarryY : primaryReachCarryY)
                    * attackCarryWeight(progress);
        }
        return pose;
    }

    /** Move each hand and rotate equipment around its calibrated grip, never its center. */
    private void applySkillWeaponMotion(WeaponPose pose, boolean offhand, double p, double scale) {
        double side = heldWeaponSideX;
        double reach = 0.0;
        double lift = 0.0;
        double envelope = skillEnvelope(p);
        switch (attackSkillId) {
            case "heavy_slash" -> {
                if (!offhand) {
                    pose.weaponRotation += pose.attackArc * skillCurve(p, -0.70, 1.18, 1.46);
                    reach = skillCurve(p, -1.5, 4.0, 2.4);
                    lift = skillCurve(p, 2.0, -1.8, -2.2);
                } else {
                    lift = skillCurve(p, 2.8, 1.8, 0.6);
                    pose.weaponRotation -= side * envelope * 0.09;
                }
            }
            case "shield_bash" -> {
                if (offhand) {
                    reach = skillCurve(p, -3.0, 10.0, 5.5);
                    lift = skillCurve(p, 2.0, 1.0, 0.0);
                    pose.weaponRotation += side * skillCurve(p, -0.12, 0.16, 0.08);
                    pose.behindCharacter = spriteRow == 0;
                } else {
                    reach = skillCurve(p, -0.8, -1.6, -0.4);
                    lift = skillCurve(p, 2.0, 2.8, 0.8);
                    pose.weaponRotation -= side * envelope * 0.18;
                }
            }
            case "earth_shatter" -> {
                if (!offhand) {
                    pose.weaponRotation += side * skillCurve(p, 1.45, -0.46, -0.90);
                    lift = skillCurve(p, -3.0, 7.0, 5.0);
                    reach = skillCurve(p, -0.8, 3.0, 2.0);
                } else {
                    lift = envelope * 2.5;
                }
            }
            case "knights_wrath" -> {
                if (!offhand) {
                    double spin = skillSpin(p, 2.0) * side;
                    pose.weaponRotation += spin;
                    pose.weaponX += side * (Math.cos(spin) - 1.0) * envelope * 3.6 * scale;
                    pose.weaponY += Math.sin(spin) * envelope * 3.2 * scale;
                    lift = envelope;
                } else {
                    lift = envelope * 3.0;
                    pose.weaponRotation += Math.sin(skillSpin(p, 2.0)) * 0.12;
                }
            }
            case "shadow_strike" -> {
                double effort = skillCurve(p, -0.65, 1.15, 1.45);
                pose.weaponRotation += pose.attackArc * effort * (offhand ? 0.6 : 1.2);
                reach = skillCurve(p, -2.0, offhand ? 3.0 : 8.0, offhand ? 1.5 : 4.0);
                lift = skillCurve(p, 1.8, 1.0, -0.4);
            }
            case "shadow_step" -> {
                pose.weaponRotation += side * (offhand ? -1.0 : 1.0) * envelope * 0.65;
                reach = -envelope * 1.6;
                lift = envelope * 2.0;
            }
            case "death_mark" -> {
                if (!offhand) {
                    pose.weaponRotation += side * skillCurve(p, -0.2, -0.8, -0.4);
                    reach = skillCurve(p, -1.0, 4.0, 2.0);
                    lift = skillCurve(p, 2.0, 3.8, 1.8);
                }
            }
            case "twin_fang" -> {
                double spin = skillSpin(p, 1.0) * side * (offhand ? -1.0 : 1.0);
                pose.weaponRotation += spin;
                double orbit = spin + (offhand ? Math.PI : 0.0);
                pose.weaponX += Math.sin(orbit) * envelope * 4.0 * scale;
                pose.weaponY += (Math.cos(orbit) - (offhand ? -1.0 : 1.0)) * envelope * 2.2 * scale;
                lift = envelope * 2.0;
                if (spriteRow != 0) pose.behindCharacter = false;
            }
            default -> {
                // The raised staff head remains the spell's source throughout buildup.
                double height = switch (attackSkillId) {
                    case "heal" -> 6.0;
                    case "holy_shield" -> 4.5;
                    case "divine_light", "lightning_strike" -> 9.5;
                    case "elemental_storm" -> 8.0;
                    default -> 7.5;
                };
                lift = skillCurve(p, height * 0.65, height, height * 0.56);
                reach = skillCurve(p, -1.0, "holy_shield".equals(attackSkillId) ? 4.0 : 2.5, 1.0);
                if ("divine_light".equals(attackSkillId) || "lightning_strike".equals(attackSkillId)) {
                    pose.weaponRotation += side * skillCurve(p, -0.18, -0.38, -0.20);
                } else if ("elemental_storm".equals(attackSkillId)) {
                    pose.weaponRotation += side * skillCurve(p, -0.25, 0.04, 0.28);
                } else {
                    pose.weaponRotation += side * skillCurve(p, -0.22, 0.24, 0.12);
                }
            }
        }
        pose.weaponX += recentMoveX * reach * scale;
        pose.weaponY += (recentMoveY * reach - lift) * scale;
    }

    private void drawSkillArms(Graphics2D graphics, int width, int height, BufferedImage frame) {
        for (int index = 0; index < 2; index++) {
            boolean offhand = index == 1;
            if ((offhand ? heldOffhandSprite : heldPrimarySprite) == null) continue;
            WeaponPose pose = animatedWeaponPose(offhand, width, height);
            if (pose.behindCharacter) continue;
            int[] hand = handPixel(offhand);
            double startX = (hand[0] / (double) spriteWidth - 0.5) * width;
            double startY = (hand[1] / (double) spriteHeight - 0.5) * height;
            double distance = Math.hypot(pose.weaponX - startX, pose.weaponY - startY);
            if (distance < 0.6 * spriteScale) continue;
            // Extend the existing sleeve pixels, keeping the original art's palette.
            int sleeve = frame.getRGB(Math.max(0, hand[0] - 1), Math.max(0, hand[1] - 2));
            if ((sleeve >>> 24) == 0) sleeve = frame.getRGB(hand[0], hand[1]);
            if ((sleeve >>> 24) == 0) continue;
            graphics.setColor(new Color(sleeve, true));
            int steps = Math.max(1, (int) Math.ceil(distance / spriteScale));
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                int x = (int) Math.round(WeaponPose.lerp(startX, pose.weaponX, t));
                int y = (int) Math.round(WeaponPose.lerp(startY, pose.weaponY, t));
                graphics.fillRect(x - spriteScale, y - spriteScale, spriteScale * 3, spriteScale * 3);
            }
        }
    }

    private void drawGripHands(Graphics2D graphics, int centerX, int centerY,
            int renderedWidth, int renderedHeight, int spriteColumn, BufferedImage frame) {
        if ("greatshield".equals(defaultWeaponStyle)) {
            // The forearm and hand are behind a greatshield; the actual face remains unpainted.
            return;
        }
        for (boolean offhand : new boolean[] {false, true}) {
            if ((offhand ? heldOffhandSprite : heldPrimarySprite) == null) {
                continue;
            }
            WeaponPose pose = animatedWeaponPose(offhand, renderedWidth, renderedHeight);
            if (pose.behindCharacter) {
                continue;
            }
            int[] hand = handPixel(offhand);
            int x = hand[0] - 1;
            int y = hand[1] - 1;
            Point2D.Double grip = gripHandPosition(offhand, renderedWidth, renderedHeight);
            int drawX = centerX + (int) grip.x - spriteScale;
            int drawY = centerY + (int) grip.y - spriteScale;
            // Restore fingers/glove pixels over the handle to visibly close the grip.
            graphics.drawImage(frame, drawX, drawY,
                    drawX + 3 * spriteScale, drawY + 3 * spriteScale,
                    x, y, x + 3, y + 3, null);
        }
    }

    /** The hand patch uses the nearest source-grid pixel around the continuous grip. */
    private Point2D.Double gripHandPosition(boolean offhand, int width, int height) {
        WeaponPose pose = animatedWeaponPose(offhand, width, height);
        return new Point2D.Double(Math.round(pose.weaponX), Math.round(pose.weaponY));
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
        private boolean behindCharacter;
        private final double attackArc;
        private double attackDirection = 1.0;
        private final Color trailColor;
        private final double trailWidth;
        private double trailBladeAngle;
        private double trailRadius;
        private double trailVerticalScale = 0.72;
        private double trailSweep = Double.NaN;
        private double trailOpacity;

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter) {
            this(offsetX, offsetY, angle, drawHeight, pivotX, pivotY, flipX,
                    behindCharacter, 0.0, null, 0.0);
        }

        private WeaponPose(double offsetX, double offsetY, double angle, int drawHeight,
                double pivotX, double pivotY, boolean flipX, boolean behindCharacter,
                double attackArc, Color trailColor, double trailWidth) {
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
            this.attackArc = attackArc;
            this.trailColor = trailColor;
            this.trailWidth = trailWidth;
            this.trailBladeAngle = Math.toRadians(flipX ? 58 : 122);
            this.trailRadius = drawHeight * 0.94;
        }

        private void applyAttackProgress(double progress) {
            if (attackArc == 0.0) {
                return;
            }

            double clamped = Math.max(0.0, Math.min(1.0, progress));
            double rotationOffset;
            if (clamped < 0.24) {
                rotationOffset = lerp(0.0, -0.72 * attackArc, smoother(clamped / 0.24));
            } else if (clamped < 0.55) {
                rotationOffset = lerp(-0.72 * attackArc, 1.22 * attackArc,
                        smoother((clamped - 0.24) / 0.31));
            } else if (clamped < 0.78) {
                rotationOffset = lerp(1.22 * attackArc, 0.42 * attackArc,
                        smoother((clamped - 0.55) / 0.23));
            } else {
                rotationOffset = lerp(0.42 * attackArc, 0.0, smoother((clamped - 0.78) / 0.22));
            }

            weaponRotation += rotationOffset * attackDirection;
            weaponX = handAnchorX;
            weaponY = handAnchorY;
        }

        private void drawAttackTrail(Graphics2D graphics, int centerX, int centerY,
                double progress) {
            if (trailColor == null || attackArc == 0.0
                    || trailColor == HOLY_TRAIL_COLOR || trailColor == ELEMENTAL_TRAIL_COLOR) {
                return;
            }
            double clamped = Math.max(0.0, Math.min(1.0, progress));
            boolean dagger = Double.isFinite(trailSweep);
            if (!dagger && (clamped < 0.20 || clamped > 0.76)) {
                return;
            }

            double phase = smooth((clamped - 0.20) / 0.56);
            float alpha = (float) (dagger ? trailOpacity : Math.sin(phase * Math.PI) * 0.48);
            if (alpha <= 0.01f) {
                return;
            }

            double handX = centerX + weaponX;
            double handY = centerY + weaponY;
            double radius = trailRadius;
            double middleAngle = weaponRotation + trailBladeAngle;
            double sweep = dagger ? trailSweep : attackArc * attackDirection * 0.58;
            double startAngle = middleAngle - sweep;
            double endAngle = dagger ? middleAngle : middleAngle + sweep * 0.22;

            Graphics2D trailGraphics = (Graphics2D) graphics.create();
            trailGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D.Double ribbon = new Path2D.Double();
            ribbon.moveTo(handX + Math.cos(startAngle) * radius,
                    handY + Math.sin(startAngle) * radius * trailVerticalScale);
            for (int step = 1; step <= 24; step++) {
                double ratio = step / 24.0;
                double angle = lerp(startAngle, endAngle, ratio);
                double taperedRadius = dagger ? radius : radius * (1.0 - ratio * 0.16);
                ribbon.lineTo(handX + Math.cos(angle) * taperedRadius,
                        handY + Math.sin(angle) * taperedRadius * trailVerticalScale);
            }
            trailGraphics.setColor(trailColor);
            trailGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.24f));
            trailGraphics.setStroke(new BasicStroke((float) (trailWidth * 2.1),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trailGraphics.draw(ribbon);
            trailGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
            trailGraphics.setStroke(new BasicStroke((float) Math.max(1.0, trailWidth * 0.62),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trailGraphics.draw(ribbon);
            trailGraphics.setColor(trailColor.brighter());
            trailGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.72f));
            trailGraphics.setStroke(new BasicStroke((float) Math.max(0.7, trailWidth * 0.24),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trailGraphics.draw(ribbon);
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
        if ("greatshield".equals(defaultWeaponStyle)) {
            return "greatshield";
        }
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
