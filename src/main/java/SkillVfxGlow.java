import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Reuses small soft-light textures instead of rasterizing a gradient per particle. */
final class SkillVfxGlow {
    private static final int SIZE = 64;
    private static final Map<Integer, BufferedImage> TEXTURES = new HashMap<>();

    private SkillVfxGlow() { }

    static void draw(Graphics2D g, double x, double y, double radius, Color color, double alpha) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(radius)
                || !Double.isFinite(alpha) || alpha < 0.002 || radius < 0.2 || color.getAlpha() == 0) return;
        BufferedImage texture = texture(color);
        Composite previous = g.getComposite();
        float opacity = (float) (Math.max(0, Math.min(1, alpha)) * color.getAlpha() / 255.0);
        try {
            g.setComposite(previous instanceof AlphaComposite composite
                    ? composite.derive(composite.getAlpha() * opacity)
                    : AlphaComposite.SrcOver.derive(opacity));
            AffineTransform transform = AffineTransform.getTranslateInstance(x - radius, y - radius);
            transform.scale(radius * 2 / SIZE, radius * 2 / SIZE);
            g.drawImage(texture, transform, null);
        } finally {
            g.setComposite(previous);
        }
    }

    private static synchronized BufferedImage texture(Color color) {
        int rgb = color.getRGB() & 0xffffff;
        BufferedImage existing = TEXTURES.get(rgb);
        if (existing != null) return existing;
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = (x + 0.5 - SIZE * 0.5) / (SIZE * 0.5);
                double dy = (y + 0.5 - SIZE * 0.5) / (SIZE * 0.5);
                double distance = Math.sqrt(dx * dx + dy * dy);
                double halo = Math.pow(Math.max(0, 1 - distance * distance), 3);
                double core = Math.pow(Math.max(0, 1 - distance), 5);
                int opacity = (int) Math.round(220 * (halo * 0.57 + core * 0.43));
                image.setRGB(x, y, (opacity << 24) | rgb);
            }
        }
        // The runtime uses a fixed palette; bound the cache for custom ability colors.
        if (TEXTURES.size() < 64) TEXTURES.put(rgb, image);
        return image;
    }
}
