import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/** Samples original pixel images directly onto the final device pixel grid. */
public final class PixelArtRenderer {
    private PixelArtRenderer() {
    }

    static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    }

    public static void drawImage(Graphics2D graphics, BufferedImage image, Rectangle bounds) {
        if (image == null || bounds.width <= 0 || bounds.height <= 0) return;
        AffineTransform device = graphics.getTransform();
        double scaleX = Math.hypot(device.getScaleX(), device.getShearY());
        double scaleY = Math.hypot(device.getShearX(), device.getScaleY());
        double fit = Math.min(bounds.width * scaleX / image.getWidth(),
                bounds.height * scaleY / image.getHeight());
        if (!(fit > 0.0) || !Double.isFinite(fit)) return;
        // Large originals must shrink to fit their existing UI boxes. Enlargements
        // instead give every source pixel the same whole number of device pixels.
        double scale = fit >= 1.0 ? Math.floor(fit + 1e-9) : fit;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        Point2D center = device.transform(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), null);
        int x = (int) Math.round(center.getX() - width / 2.0);
        int y = (int) Math.round(center.getY() - height / 2.0);
        Graphics2D pixels = (Graphics2D) graphics.create();
        try {
            // The parent already includes viewport and HiDPI scaling. Resolve it
            // once here, rather than resampling a resized icon through that scale.
            pixels.setTransform(new AffineTransform());
            configure(pixels);
            pixels.drawImage(image, x, y, width, height, null);
        } finally {
            pixels.dispose();
        }
    }
}
