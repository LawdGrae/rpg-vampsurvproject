import java.awt.image.BufferedImage;

/** Bakes a gentle exposure lift into arena art without changing the cached source. */
public final class ArenaLighting {
    private static final double GAMMA = 0.81;
    private static final int[] RED = channelCurve(1.015, 0.35);
    private static final int[] GREEN = channelCurve(1.025, 0.45);
    private static final int[] BLUE = channelCurve(1.005, 0.20);

    private ArenaLighting() {
    }

    /** Returns a new ARGB image with the same dimensions and alpha as the source. */
    public static BufferedImage brighten(BufferedImage source) {
        if (source == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            source.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                int pixel = row[x];
                int alpha = pixel & 0xff000000;
                if (alpha != 0) {
                    row[x] = alpha
                            | RED[(pixel >>> 16) & 0xff] << 16
                            | GREEN[(pixel >>> 8) & 0xff] << 8
                            | BLUE[pixel & 0xff];
                }
            }
            result.setRGB(0, y, width, 1, row, 0, width);
        }
        return result;
    }

    private static int[] channelCurve(double gain, double lift) {
        int[] curve = new int[256];
        for (int value = 0; value < curve.length; value++) {
            double input = value / 255.0;
            double exposed = Math.pow(input, GAMMA) * 255.0;
            // Roll the tiny channel adjustments off at white and keep true black intact.
            double adjusted = exposed + exposed * (gain - 1.0) * (1.0 - input)
                    + lift * 4.0 * input * (1.0 - input);
            curve[value] = (int) Math.round(Math.max(0.0, Math.min(255.0, adjusted)));
        }
        return curve;
    }
}
