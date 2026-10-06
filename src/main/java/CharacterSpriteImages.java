import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Restores crisp pixel coverage and local contrast without changing source assets. */
public final class CharacterSpriteImages {
    private static final double SHARPEN_AMOUNT = 2.10;
    private static final int MAX_CHANNEL_CHANGE = 56;
    private static final int COLOR_STEP = 16;
    // Discard faint resampling fringe; surviving source pixels become fully covered.
    private static final int ALPHA_CUTOFF = 96;
    private static final Map<BufferedImage, Map<Long, BufferedImage>> CACHE = new IdentityHashMap<>();

    private CharacterSpriteImages() {
    }

    public static synchronized BufferedImage prepareSheet(BufferedImage source,
            int frameWidth, int frameHeight) {
        Objects.requireNonNull(source, "source");
        if (frameWidth <= 0 || frameHeight <= 0
                || source.getWidth() % frameWidth != 0 || source.getHeight() % frameHeight != 0) {
            throw new IllegalArgumentException("Frames must be positive and fit the whole sprite sheet");
        }
        long geometry = (long) frameWidth << 32 | (frameHeight & 0xffffffffL);
        return CACHE.computeIfAbsent(source, ignored -> new HashMap<>())
                .computeIfAbsent(geometry, ignored -> sharpenFrames(source, frameWidth, frameHeight));
    }

    private static BufferedImage sharpenFrames(BufferedImage source,
            int frameWidth, int frameHeight) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        boolean hasSoftEdges = false;
        for (int pixel : pixels) {
            int alpha = pixel >>> 24;
            if (alpha > 0 && alpha < 255) {
                hasSoftEdges = true;
                break;
            }
        }
        if (!hasSoftEdges) {
            return source;
        }

        int[] prepared = pixels.clone();
        for (int y = 0; y < height; y++) {
            int top = y / frameHeight * frameHeight;
            int bottom = top + frameHeight;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int alpha = pixel >>> 24;
                if (alpha == 0) {
                    continue;
                }
                if (alpha < ALPHA_CUTOFF) {
                    prepared[y * width + x] = 0;
                    continue;
                }
                int left = x / frameWidth * frameWidth;
                int right = left + frameWidth;
                double weight = 0.0;
                double red = 0.0;
                double green = 0.0;
                double blue = 0.0;
                // Ignore hidden colors and neighboring animation cells when estimating blur.
                for (int sampleY = Math.max(top, y - 1); sampleY <= Math.min(bottom - 1, y + 1); sampleY++) {
                    for (int sampleX = Math.max(left, x - 1); sampleX <= Math.min(right - 1, x + 1); sampleX++) {
                        int sample = pixels[sampleY * width + sampleX];
                        if ((sample >>> 24) < ALPHA_CUTOFF) {
                            continue;
                        }
                        int kernel = (sampleX == x ? 2 : 1) * (sampleY == y ? 2 : 1);
                        double sampleWeight = kernel * (sample >>> 24);
                        weight += sampleWeight;
                        red += (sample >>> 16 & 255) * sampleWeight;
                        green += (sample >>> 8 & 255) * sampleWeight;
                        blue += (sample & 255) * sampleWeight;
                    }
                }
                int sharpenedRed = sharpen(pixel >>> 16 & 255, red / weight);
                int sharpenedGreen = sharpen(pixel >>> 8 & 255, green / weight);
                int sharpenedBlue = sharpen(pixel & 255, blue / weight);
                prepared[y * width + x] = 0xff000000
                        | sharpenedRed << 16 | sharpenedGreen << 8 | sharpenedBlue;
            }
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, prepared, 0, width);
        return result;
    }

    private static int sharpen(int channel, double blurred) {
        double detail = Math.max(-MAX_CHANNEL_CHANGE, Math.min(MAX_CHANNEL_CHANGE,
                (channel - blurred) * SHARPEN_AMOUNT));
        if (Math.abs(detail) < 1.0) {
            return channel;
        }
        int sharpened = clamp(channel + detail);
        // Collapse near-identical edge shades while retaining the source hue and contrast bound.
        int quantized = (int) Math.round(sharpened / (double) COLOR_STEP) * COLOR_STEP;
        return clamp(Math.max(channel - MAX_CHANNEL_CHANGE,
                Math.min(channel + MAX_CHANNEL_CHANGE, quantized)));
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
