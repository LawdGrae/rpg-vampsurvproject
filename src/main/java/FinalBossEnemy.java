import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FinalBossEnemy extends Enemy {
    private static final double SPEED = 0.0;
    private static final double ANIMATION_SPEED = 0.0;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int RENDER_SIZE = 200;
    private static final double COLLISION_RADIUS = RENDER_SIZE / 2.0;
    private static final double DAMAGE = 28.0;
    private static final double MAX_HEALTH = 1000.0;
    private static final double MAX_SHIELD = 100.0;
    private static final double SUMMON_INTERVAL = 9.0;
    private static final double BLUE_REGEN_INTERVAL = 5.0;
    private static final double LASER_COOLDOWN = 4.0;
    private static final double WALL_COOLDOWN = 12.0;
    private static final double SPLIT_DURATION = 60.0;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();
    private static final BufferedImage DEATH_SHEET = loadDeathSheet();

    private double shieldHealth = MAX_SHIELD;
    private double summonCooldown;
    private double blueRegenCooldown;
    private double laserCooldown;
    private double wallCooldown;
    private double arenaPulse;
    private boolean arenaLocked;
    private boolean splitState;
    private double splitTimer;

    public FinalBossEnemy(double worldX, double worldY) {
        super(worldX, worldY, SPRITE_SHEET, DEATH_SHEET, SPEED, ANIMATION_SPEED,
                FRAME_WIDTH, FRAME_HEIGHT, RENDER_SIZE, COLLISION_RADIUS, DAMAGE, MAX_HEALTH);
        summonCooldown = 1.5;
        blueRegenCooldown = BLUE_REGEN_INTERVAL;
        laserCooldown = LASER_COOLDOWN;
        wallCooldown = WALL_COOLDOWN;
    }

    public double getShieldHealth() {
        return shieldHealth;
    }

    public boolean hasShield() {
        return shieldHealth > 0.0 && !isDead();
    }

    public boolean shouldSummon() {
        return !splitState && summonCooldown <= 0.0 && !isDead();
    }

    public void resetSummonCooldown() {
        summonCooldown = SUMMON_INTERVAL;
    }

    public boolean isSplitState() {
        return splitState;
    }

    public void triggerSplitState() {
        if (isDead() || splitState) {
            return;
        }
        splitState = true;
        splitTimer = SPLIT_DURATION;
    }

    public void updateSplitState(double deltaTime) {
        if (!splitState) {
            return;
        }
        splitTimer = Math.max(0.0, splitTimer - deltaTime);
        if (splitTimer <= 0.0) {
            splitState = false;
            splitTimer = 0.0;
        }
    }

    public void takeDamage(double damage, DamageElement element, double originX, double originY) {
        if (shieldHealth > 0.0) {
            double absorbed = Math.min(shieldHealth, damage);
            shieldHealth -= absorbed;
            damage -= absorbed;
        }
        if (damage > 0.0) {
            super.takeDamage(damage, element, originX, originY);
            if (!splitState && !isDead()) {
                triggerSplitState();
            }
        }
    }

    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        if (isDead()) {
            return;
        }

        updateSplitState(deltaTime);
        summitShieldRegeneration(deltaTime);
        summonCooldown -= deltaTime;
        laserCooldown -= deltaTime;
        wallCooldown -= deltaTime;
        arenaPulse = Math.max(0.0, arenaPulse - deltaTime);

        if (blueRegenCooldown <= 0.0 && !hasBlueMinionsNearby()) {
            shieldHealth = Math.min(MAX_SHIELD, shieldHealth + 10.0);
            blueRegenCooldown = BLUE_REGEN_INTERVAL;
        }
        blueRegenCooldown -= deltaTime;
        if (blueRegenCooldown <= 0.0) {
            blueRegenCooldown = BLUE_REGEN_INTERVAL;
        }

        if (wallCooldown <= 0.0) {
            arenaLocked = true;
            wallCooldown = WALL_COOLDOWN;
        } else {
            arenaLocked = false;
        }

        if (laserCooldown <= 0.0) {
            laserCooldown = LASER_COOLDOWN;
        }
    }

    private void summitShieldRegeneration(double deltaTime) {
        if (shieldHealth < MAX_SHIELD && !hasBlueMinionsNearby()) {
            blueRegenCooldown -= deltaTime;
            if (blueRegenCooldown <= 0.0) {
                shieldHealth = Math.min(MAX_SHIELD, shieldHealth + 10.0);
                blueRegenCooldown = BLUE_REGEN_INTERVAL;
            }
        }
    }

    private boolean hasBlueMinionsNearby() {
        return false;
    }

    public boolean shouldFireLaser() {
        return laserCooldown <= 0.0 && !isDead();
    }

    public boolean shouldThrowWalls() {
        return wallCooldown <= 0.0 && !isDead();
    }

    public boolean isArenaLocked() {
        return arenaLocked;
    }

    public List<Enemy> createMinions(Random random, double playerX, double playerY) {
        List<Enemy> spawned = new ArrayList<>();
        double baseX = getWorldX();
        double baseY = getWorldY();

        for (int index = 0; index < 3; index++) {
            double angle = (Math.PI * 2.0 * index / 3.0) + random.nextDouble() * 0.8;
            spawned.add(new BlueFinalMinion(baseX + Math.cos(angle) * 110.0,
                    baseY + Math.sin(angle) * 110.0));
        }
        for (int index = 0; index < 6; index++) {
            double angle = (Math.PI * 2.0 * index / 6.0) + random.nextDouble() * 0.7;
            spawned.add(new GreenFinalMinion(baseX + Math.cos(angle) * 150.0,
                    baseY + Math.sin(angle) * 150.0));
        }
        for (int index = 0; index < 10; index++) {
            double angle = (Math.PI * 2.0 * index / 10.0) + random.nextDouble() * 0.5;
            spawned.add(new RedFinalMinion(baseX + Math.cos(angle) * 220.0,
                    baseY + Math.sin(angle) * 220.0));
        }
        return spawned;
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (splitState) {
            drawSplitBoss(graphics, centerX, centerY, cameraX, cameraY);
            return;
        }
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
        drawBossBar(graphics, centerX, centerY, cameraX, cameraY);
        if (arenaPulse > 0.0) {
            graphics.setColor(new Color(80, 160, 255, 80));
            graphics.setStroke(new BasicStroke(3f));
            graphics.drawOval(
                    (int) (centerX + getWorldX() + cameraX - 210),
                    (int) (centerY + getWorldY() + cameraY - 210),
                    420, 420);
        }
    }

    private void drawSplitBoss(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double baseX = getWorldX();
        double baseY = getWorldY();
        double[][] offsets = {
                {0.0, 0.0},
                {-110.0, -30.0},
                {110.0, 26.0}
        };

        for (double[] offset : offsets) {
            int screenX = (int) (centerX + baseX + offset[0] + cameraX - RENDER_SIZE / 2.0);
            int screenY = (int) (centerY + baseY + offset[1] + cameraY - RENDER_SIZE / 2.0);
            BufferedImage fakeSprite = desaturateSprite(SPRITE_SHEET);
            graphics.drawImage(fakeSprite, screenX, screenY, RENDER_SIZE, RENDER_SIZE, null);
        }

        int barX = (int) (centerX + baseX + cameraX - 90);
        int barY = (int) (centerY + baseY + cameraY - RENDER_SIZE / 2.0 - 20.0);
        graphics.setColor(new Color(20, 20, 20, 180));
        graphics.fillRoundRect(barX, barY, 180, 10, 8, 8);
        graphics.setColor(new Color(255, 150, 60));
        graphics.fillRoundRect(barX, barY, (int) (180 * getHealthRatio()), 10, 8, 8);
    }

    private BufferedImage desaturateSprite(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int gray = (r + g + b) / 3;
                int newR = (r + gray) / 2;
                int newG = (g + gray) / 2;
                int newB = (b + gray) / 2;
                result.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
            }
        }
        return result;
    }

    private void drawBossBar(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        if (isDead()) {
            return;
        }

        int barWidth = 180;
        int barHeight = 12;
        int x = (int) (centerX + getWorldX() + cameraX - barWidth / 2.0);
        int y = (int) (centerY + getWorldY() + cameraY - RENDER_SIZE / 2.0 - 28.0);

        graphics.setColor(new Color(25, 25, 25, 200));
        graphics.fillRoundRect(x, y, barWidth, barHeight, 12, 12);
        int healthWidth = (int) Math.round(barWidth * getHealthRatio());
        graphics.setColor(new Color(200, 40, 75));
        graphics.fillRoundRect(x, y, healthWidth, barHeight, 12, 12);

        int shieldWidth = (int) Math.round(barWidth * (shieldHealth / MAX_SHIELD));
        int shieldY = y + 18;
        graphics.setColor(new Color(25, 25, 25, 200));
        graphics.fillRoundRect(x, shieldY, barWidth, 8, 8, 8);
        graphics.setColor(new Color(100, 200, 255));
        graphics.fillRoundRect(x, shieldY, shieldWidth, 8, 8, 8);
    }

    private static BufferedImage loadSpriteSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/FINALBOSS.png");
    }

    private static BufferedImage loadDeathSheet() {
        return ResourceLoader.loadImage("/main/resources/enemy/FINALBOSS.png");
    }
}
