import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RegionalEnemy extends Enemy {
    private final EnemyDefinition definition;
    private final BufferedImage projectileSprite;
    private double fireCooldown;
    private double summonCooldown;
    private boolean hasActiveProjectile;
    private int lastPhase = 1;

    public RegionalEnemy(EnemyDefinition definition, double worldX, double worldY) {
        super(worldX, worldY, EnemySpriteAssets.spriteFor(definition),
                EnemySpriteAssets.deathFor(definition),
                definition.getSpeed(), definition.getAnimationSpeed(),
                definition.getFrameWidth(), definition.getFrameHeight(),
                definition.getRenderSize(), definition.getCollisionRadius(),
                definition.getDamage(), definition.getMaxHealth());
        this.definition = definition;
        this.projectileSprite = EnemySpriteAssets.projectileFor(definition.getElement());
        this.fireCooldown = definition.isBoss() ? 1.4 : 0.65;
        this.summonCooldown = definition.isBoss() ? 5.0 : Double.POSITIVE_INFINITY;
    }

    public EnemyDefinition getDefinition() {
        return definition;
    }

    public String getDisplayName() {
        return definition.getDisplayName();
    }

    public EnemyRegion getRegion() {
        return definition.getRegion();
    }

    public boolean isBoss() {
        return definition.isBoss();
    }

    public int getBossPhase() {
        if (!definition.isBoss()) {
            return 0;
        }
        double ratio = getHealthRatio();
        if (ratio <= 0.33) {
            return 3;
        }
        if (ratio <= 0.66) {
            return 2;
        }
        return 1;
    }

    public boolean phaseChangedThisFrame() {
        int phase = getBossPhase();
        if (phase > lastPhase) {
            lastPhase = phase;
            return true;
        }
        return false;
    }

    @Override
    public double getDamage() {
        if (!definition.isBoss()) {
            return super.getDamage();
        }
        return super.getDamage() * switch (getBossPhase()) {
            case 3 -> 1.45;
            case 2 -> 1.2;
            default -> 1.0;
        };
    }

    @Override
    public void update(double deltaTime, double targetWorldX, double targetWorldY,
            double targetCollisionRadius) {
        double stopRadius = definition.isRanged()
                ? Math.max(targetCollisionRadius, definition.getAttackRange() - collisionRadius)
                : targetCollisionRadius;
        super.update(deltaTime, targetWorldX, targetWorldY, stopRadius);
        if (isDead()) {
            return;
        }
        fireCooldown = Math.max(0.0, fireCooldown - deltaTime);
        if (definition.isBoss()) {
            double phaseMultiplier = getBossPhase() == 3 ? 0.74 : getBossPhase() == 2 ? 0.86 : 1.0;
            summonCooldown = Math.max(0.0, summonCooldown - deltaTime * (1.0 / phaseMultiplier));
        }
    }

    @Override
    public void knockAwayFrom(double originX, double originY, double distance) {
        double resistedDistance = distance * (1.0 - definition.getKnockbackResistance());
        super.knockAwayFrom(originX, originY, resistedDistance);
    }

    public boolean canFireAt(double targetX, double targetY) {
        if (isDead() || hasActiveProjectile || fireCooldown > 0.0) {
            return false;
        }
        if (!definition.isRanged() && !definition.isBoss()) {
            return false;
        }
        double distanceSquared = distanceSquaredTo(targetX, targetY);
        return distanceSquared <= definition.getAttackRange() * definition.getAttackRange();
    }

    public Projectile fireAt(double targetX, double targetY) {
        if (!canFireAt(targetX, targetY)) {
            return null;
        }
        double phaseScale = definition.isBoss() ? 0.9 - (getBossPhase() - 1) * 0.12 : 1.0;
        fireCooldown = Math.max(0.55, definition.getAttackCooldown() * phaseScale);
        hasActiveProjectile = true;
        Projectile projectile = new Projectile(getWorldX(), getWorldY(), targetX, targetY,
                definition.getProjectileSpeed(), getDamage(), 10.0,
                projectileSprite, 12.0, definition.isBoss() ? 42.0 : 28.0,
                definition.getElement());
        projectile.setOwner(this);
        return projectile;
    }

    public void onProjectileDestroyed() {
        hasActiveProjectile = false;
    }

    public boolean shouldSummon() {
        return definition.isBoss() && !isDead() && summonCooldown <= 0.0
                && getBossPhase() >= 2;
    }

    public void resetSummonCooldown() {
        summonCooldown = getBossPhase() == 3 ? 5.5 : 7.5;
    }

    public List<Enemy> createSummons(Random random) {
        if (!shouldSummon()) {
            return List.of();
        }
        int count = getBossPhase() == 3 ? 4 : 2;
        List<Enemy> summons = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0 * index / count + random.nextDouble() * 0.7;
            double distance = 72.0 + random.nextDouble() * 36.0;
            EnemyDefinition summonDefinition = EnemyCatalog.randomNormal(definition.getRegion(), random);
            summons.add(new RegionalEnemy(summonDefinition,
                    getWorldX() + Math.cos(angle) * distance,
                    getWorldY() + Math.sin(angle) * distance));
        }
        return summons;
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        super.draw(graphics, centerX, centerY, cameraX, cameraY);
        if (definition.isBoss() && !isDead()) {
            drawBossUi(graphics, centerX);
            drawPhaseAura(graphics, centerX, centerY, cameraX, cameraY);
        }
    }

    private void drawBossUi(Graphics2D graphics, int centerX) {
        int width = 360;
        int height = 18;
        int x = centerX - width / 2;
        int y = 18;

        graphics.setColor(new Color(15, 10, 12, 210));
        graphics.fillRoundRect(x, y, width, height, 8, 8);
        graphics.setColor(new Color(80, 28, 28, 230));
        graphics.fillRoundRect(x + 2, y + 2, width - 4, height - 4, 7, 7);

        int fillWidth = (int) Math.round((width - 4) * getHealthRatio());
        Color phaseColor = getBossPhase() == 3
                ? new Color(255, 80, 30)
                : getBossPhase() == 2
                        ? new Color(190, 55, 210)
                        : new Color(230, 55, 70);
        graphics.setColor(phaseColor);
        graphics.fillRoundRect(x + 2, y + 2, fillWidth, height - 4, 7, 7);

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(new Color(255, 220, 140, 230));
        graphics.drawRoundRect(x, y, width, height, 8, 8);

        Font previous = graphics.getFont();
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 15));
        String text = "BOSS: " + definition.getDisplayName().toUpperCase()
                + "  LV " + (definition.getRegion().getLevelIndex() + 1)
                + "  PHASE " + getBossPhase();
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.setColor(new Color(0, 0, 0, 200));
        graphics.drawString(text, centerX - textWidth / 2 + 1, y - 3 + 1);
        graphics.setColor(new Color(255, 242, 205));
        graphics.drawString(text, centerX - textWidth / 2, y - 3);
        graphics.setFont(previous);
    }

    private void drawPhaseAura(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        int phase = getBossPhase();
        if (phase <= 1) {
            return;
        }
        int screenX = (int) Math.round(centerX + getWorldX() + cameraX);
        int screenY = (int) Math.round(centerY + getWorldY() + cameraY);
        int radius = (int) Math.round(definition.getRenderSize() * (phase == 3 ? 0.72 : 0.58));
        Color color = definition.getElement() == DamageElement.FIRE
                ? new Color(255, 95, 30, 75)
                : definition.getElement() == DamageElement.ICE
                        ? new Color(120, 220, 255, 70)
                        : definition.getElement() == DamageElement.POISON
                                ? new Color(80, 230, 90, 70)
                                : new Color(185, 65, 255, 70);
        graphics.setColor(color);
        graphics.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        graphics.setColor(new Color(255, 220, 120, phase == 3 ? 105 : 65));
        graphics.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
    }
}
