import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class EnemySpriteAssets {
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private EnemySpriteAssets() {
    }

    public static BufferedImage spriteFor(EnemyDefinition definition) {
        return loadPreferred(definition.getSpritePath(), definition.getFallbackSpritePath());
    }

    public static BufferedImage deathFor(EnemyDefinition definition) {
        return loadPreferred(definition.getDeathPath(), definition.getFallbackDeathPath());
    }

    public static BufferedImage projectileFor(DamageElement element) {
        String path = switch (element) {
            case FIRE, EXPLOSION -> "/main/resources/abilities/fire_bolt.png";
            case ICE -> "/main/resources/abilities/ice_shard.png";
            case LIGHTNING -> "/main/resources/abilities/lightning_strike.png";
            case POISON -> "/main/resources/abilities/poison_arrow.png";
            case SHADOW -> "/main/resources/abilities/shadow_orb.png";
            case HOLY -> "/main/resources/abilities/holy_bolt.png";
            default -> "/main/resources/projectiles/enemylv2projectile.png";
        };
        return load(path);
    }

    private static BufferedImage loadPreferred(String primaryPath, String fallbackPath) {
        try {
            return load(primaryPath);
        } catch (IllegalStateException ignored) {
            return load(fallbackPath);
        }
    }

    private static BufferedImage load(String path) {
        return CACHE.computeIfAbsent(path, ResourceLoader::loadImage);
    }
}
