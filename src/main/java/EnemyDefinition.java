public class EnemyDefinition {
    private final String id;
    private final String displayName;
    private final EnemyRegion region;
    private final boolean boss;
    private final boolean ranged;
    private final int weight;
    private final double speed;
    private final double animationSpeed;
    private final int frameWidth;
    private final int frameHeight;
    private final int renderSize;
    private final double collisionRadius;
    private final double damage;
    private final double maxHealth;
    private final double attackRange;
    private final double attackCooldown;
    private final double projectileSpeed;
    private final double knockbackResistance;
    private final int experienceReward;
    private final int goldReward;
    private final DamageElement element;
    private final String lootName;
    private final String spritePath;
    private final String deathPath;
    private final String fallbackSpritePath;
    private final String fallbackDeathPath;

    public EnemyDefinition(String id, String displayName, EnemyRegion region,
            boolean boss, boolean ranged, int weight, double speed,
            double animationSpeed, int frameWidth, int frameHeight,
            int renderSize, double collisionRadius, double damage,
            double maxHealth, double attackRange, double attackCooldown,
            double projectileSpeed, double knockbackResistance,
            int experienceReward, int goldReward, DamageElement element,
            String lootName, String spritePath, String deathPath,
            String fallbackSpritePath, String fallbackDeathPath) {
        this.id = id;
        this.displayName = displayName;
        this.region = region;
        this.boss = boss;
        this.ranged = ranged;
        this.weight = weight;
        this.speed = speed;
        this.animationSpeed = animationSpeed;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.renderSize = renderSize;
        this.collisionRadius = collisionRadius;
        this.damage = damage;
        this.maxHealth = maxHealth;
        this.attackRange = attackRange;
        this.attackCooldown = attackCooldown;
        this.projectileSpeed = projectileSpeed;
        this.knockbackResistance = knockbackResistance;
        this.experienceReward = experienceReward;
        this.goldReward = goldReward;
        this.element = element;
        this.lootName = lootName;
        this.spritePath = spritePath;
        this.deathPath = deathPath;
        this.fallbackSpritePath = fallbackSpritePath;
        this.fallbackDeathPath = fallbackDeathPath;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EnemyRegion getRegion() {
        return region;
    }

    public boolean isBoss() {
        return boss;
    }

    public boolean isRanged() {
        return ranged;
    }

    public int getWeight() {
        return weight;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getRenderSize() {
        return renderSize;
    }

    public double getCollisionRadius() {
        return collisionRadius;
    }

    public double getDamage() {
        return damage;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public double getAttackCooldown() {
        return attackCooldown;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public double getKnockbackResistance() {
        return knockbackResistance;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public DamageElement getElement() {
        return element;
    }

    public String getLootName() {
        return lootName;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public String getDeathPath() {
        return deathPath;
    }

    public String getFallbackSpritePath() {
        return fallbackSpritePath;
    }

    public String getFallbackDeathPath() {
        return fallbackDeathPath;
    }
}
