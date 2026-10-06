import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class EnemySpriteAssets {
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final Map<String, BufferedImage> STANDALONE_CACHE = new HashMap<>();

    private EnemySpriteAssets() {
    }

    public static BufferedImage spriteFor(EnemyDefinition definition) {
        return loadPreferred(definition.getSpritePath(), definition.getFallbackSpritePath());
    }

    public static BufferedImage deathFor(EnemyDefinition definition) {
        return loadPreferred(definition.getDeathPath(), definition.getFallbackDeathPath());
    }

    public static BufferedImage standaloneSprite(String path) {
        return STANDALONE_CACHE.computeIfAbsent(path,
                sourcePath -> extractStandaloneSprite(load(sourcePath)));
    }

    static BufferedImage extractStandaloneSprite(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        boolean[] background = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int count = 0;
        for (int x = 0; x < width; x++) {
            count = enqueueBackground(x, pixels, background, queue, count);
            count = enqueueBackground((height - 1) * width + x, pixels, background, queue, count);
        }
        for (int y = 0; y < height; y++) {
            count = enqueueBackground(y * width, pixels, background, queue, count);
            count = enqueueBackground(y * width + width - 1, pixels, background, queue, count);
        }
        // Remove only the near-black backdrop connected to an image edge.
        // Enclosed black outlines and details remain opaque.
        for (int head = 0; head < count; head++) {
            int index = queue[head];
            int x = index % width;
            int y = index / width;
            if (x > 0) count = enqueueBackground(index - 1, pixels, background, queue, count);
            if (x + 1 < width) count = enqueueBackground(index + 1, pixels, background, queue, count);
            if (y > 0) count = enqueueBackground(index - width, pixels, background, queue, count);
            if (y + 1 < height) count = enqueueBackground(index + width, pixels, background, queue, count);
        }

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int index = 0; index < pixels.length; index++) {
            if (background[index]) pixels[index] = 0;
            if ((pixels[index] >>> 24) != 0) {
                int x = index % width;
                int y = index / width;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        int cropWidth = maxX - minX + 1;
        int cropHeight = maxY - minY + 1;
        BufferedImage result = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, cropWidth, cropHeight, pixels, minY * width + minX, width);
        return result;
    }

    private static int enqueueBackground(int index, int[] pixels,
            boolean[] background, int[] queue, int count) {
        int pixel = pixels[index];
        boolean nearBlack = (pixel >>> 24) == 0
                || ((pixel >>> 16 & 255) <= 16 && (pixel >>> 8 & 255) <= 16 && (pixel & 255) <= 16);
        if (!background[index] && nearBlack) {
            background[index] = true;
            queue[count++] = index;
        }
        return count;
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
