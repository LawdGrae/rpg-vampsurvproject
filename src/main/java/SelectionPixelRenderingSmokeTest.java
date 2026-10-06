import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Source fidelity and physical pixel grids for class-selection rendering only. */
public class SelectionPixelRenderingSmokeTest {
    private static final String[] NAMES = {"Eumann", "Haze", "Yuexin", "Ziea", "Sire Rakki"};
    private static final String[] SHEETS = {
        "CharEumann.png", "CharHaze.png", "CharYuexin.png", "CharacterZiea.png", "CharSirRakki.png"
    };
    private static final int[][] WINDOWS = {
        {1280, 720}, {1024, 640}, {1920, 1080}, {1400, 700}
    };
    private static final double[] DPI_SCALES = {1.0, 1.25, 1.5, 2.0};
    private static final Rectangle PORTRAIT = new Rectangle(496, 207, 288, 204);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Map<BufferedImage, int[]> sources = snapshotSelectionSources();
        checkDirectImageSampling();
        checkRawFrames();
        checkPortraits(args.length == 0 ? null : Path.of(args[0]));
        for (var source : sources.entrySet()) {
            require(Arrays.equals(source.getValue(), pixels(source.getKey())),
                    "Selection rendering must leave every original source pixel unchanged");
        }
        System.out.println("Selection pixel rendering: original frames, nearest sampling, graphics isolation "
                + "and all five idle portraits passed at four window sizes and four DPI scales");
    }

    private static Map<BufferedImage, int[]> snapshotSelectionSources() {
        Map<BufferedImage, int[]> sources = new LinkedHashMap<>();
        for (String sheet : SHEETS) snapshot(sources, "/main/resources/character/" + sheet);
        for (String weapon : new String[] {"long_sword", "shield", "twin_daggers", "holy_staff",
                "elemental_staff", "aegis_greatshield"}) {
            snapshot(sources, "/main/resources/weapons/" + weapon + ".png");
        }
        GameLogic logic = new GameLogic();
        logic.setSoundEnabled(false);
        for (int index = 0; index < logic.getCharacterCount(); index++) {
            for (RpgAbility ability : logic.getCharacterActiveAbilities(index)) {
                sources.put(ability.getIcon(), pixels(ability.getIcon()));
            }
            BufferedImage passive = logic.getCharacterPassiveAbility(index).getIcon();
            sources.put(passive, pixels(passive));
        }
        return sources;
    }

    private static void snapshot(Map<BufferedImage, int[]> sources, String path) {
        BufferedImage source = ResourceLoader.loadImage(path);
        sources.put(source, pixels(source));
    }

    private static void checkDirectImageSampling() {
        // The small non-square image tests integer enlargement and aspect ratio.
        // The large image matches the mandatory downsampling of existing skill icons.
        for (BufferedImage source : new BufferedImage[] {checkerboard(7, 5), checkerboard(512, 512)}) {
            int[] before = pixels(source);
            for (int[] window : WINDOWS) {
                for (double dpi : DPI_SCALES) {
                    for (Rectangle bounds : new Rectangle[] {
                            new Rectangle(297, 207, 102, 102), new Rectangle(621, 422, 38, 38)}) {
                        BufferedImage actual = canvas(window, dpi);
                        Graphics2D graphics = actual.createGraphics();
                        graphics.transform(viewport(window, dpi));
                        smoothHints(graphics);
                        GraphicsState state = new GraphicsState(graphics);
                        PixelArtRenderer.drawImage(graphics, source, bounds);
                        state.requireUnchanged(graphics, "PixelArtRenderer");
                        graphics.dispose();

                        Rectangle drawn = opaqueBounds(actual);
                        require(drawn.width > 0 && drawn.height > 0, "Pixel image must be visible");
                        Point2D center = viewport(window, dpi).transform(
                                new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), null);
                        require(Math.abs(drawn.getCenterX() - center.getX()) <= 1.0
                                && Math.abs(drawn.getCenterY() - center.getY()) <= 1.0,
                                "Pixel image must retain its intended center in physical pixels");
                        double deviceScale = viewport(window, dpi).getScaleX();
                        double fit = Math.min(bounds.width * deviceScale / source.getWidth(),
                                bounds.height * deviceScale / source.getHeight());
                        if (fit >= 1.0) {
                            int integerScale = (int) Math.floor(fit + 1e-9);
                            require(drawn.width == source.getWidth() * integerScale
                                    && drawn.height == source.getHeight() * integerScale,
                                    "Enlarged source pixels must form equal whole-pixel squares");
                        } else {
                            require(Math.abs(drawn.width - source.getWidth() * fit) <= 1.0
                                    && Math.abs(drawn.height - source.getHeight() * fit) <= 1.0,
                                    "Oversized original icons must shrink proportionally inside the current frame");
                        }
                        BufferedImage reference = new BufferedImage(drawn.width, drawn.height,
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics2D expected = reference.createGraphics();
                        expected.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        expected.drawImage(source, 0, 0, drawn.width, drawn.height, null);
                        expected.dispose();
                        require(Arrays.equals(actual.getRGB(drawn.x, drawn.y, drawn.width, drawn.height,
                                        null, 0, drawn.width), pixels(reference)),
                                "Final image must sample the original once with nearest-neighbor interpolation");
                    }
                }
            }
            require(Arrays.equals(before, pixels(source)), "Pixel helper must never filter its source image");
        }
    }

    private static void checkRawFrames() throws Exception {
        Field rawFrames = Player.class.getDeclaredField("previewSpriteFrames");
        rawFrames.setAccessible(true);
        var staffIsolation = Player.class.getDeclaredMethod("withoutEmbeddedStaff", BufferedImage.class);
        staffIsolation.setAccessible(true);
        for (int index = 0; index < NAMES.length; index++) {
            Player player = player(index);
            BufferedImage original = ResourceLoader.loadImage("/main/resources/character/" + SHEETS[index]);
            int[] before = pixels(original);
            // Ziea retains the existing runtime staff-isolation mask, without sharpening.
            BufferedImage expected = index == 3
                    ? (BufferedImage) staffIsolation.invoke(player, original) : original;
            BufferedImage[][] frames = (BufferedImage[][]) rawFrames.get(player);
            require(frames.length == 4, NAMES[index] + " must retain all four directions");
            for (int row = 0; row < frames.length; row++) {
                require(frames[row].length == 3, NAMES[index] + " must retain all three source columns");
                for (int column = 0; column < frames[row].length; column++) {
                    BufferedImage frame = frames[row][column];
                    require(frame.getWidth() == 64 && frame.getHeight() == 64,
                            "Selection frames must keep original 64 by 64 geometry");
                    require(Arrays.equals(pixels(frame),
                                    expected.getRGB(column * 64, row * 64, 64, 64, null, 0, 64)),
                            NAMES[index] + " selection frames must use unsharpened original colors and alpha");
                }
            }
            require(Arrays.equals(before, pixels(original)), "Frame isolation must not edit the sprite sheet");
        }
    }

    private static void checkPortraits(Path outputDirectory) throws Exception {
        if (outputDirectory != null) Files.createDirectories(outputDirectory);
        Map<BufferedImage, BufferedImage> filteredBefore = filteredWeaponCache();
        for (int index = 0; index < NAMES.length; index++) {
            Player player = player(index);
            for (int phase = 0; phase < 2; phase++) {
                if (phase > 0) player.updatePreview(0.37);
                Map<String, Object> animationBefore = animationState(player);
                for (int[] window : WINDOWS) {
                    for (double dpi : DPI_SCALES) {
                        BufferedImage image = canvas(window, dpi);
                        Graphics2D graphics = image.createGraphics();
                        AffineTransform transform = viewport(window, dpi);
                        graphics.transform(transform);
                        smoothHints(graphics);
                        GraphicsState state = new GraphicsState(graphics);
                        player.drawPixelArtPreview(graphics, PORTRAIT);
                        state.requireUnchanged(graphics, NAMES[index] + " portrait");
                        graphics.dispose();
                        int block = (int) Math.floor(Math.min(PORTRAIT.width / 64.0,
                                PORTRAIT.height / 64.0) * transform.getScaleX() + 1e-9);
                        Point2D center = transform.transform(new Point2D.Double(
                                PORTRAIT.getCenterX(), PORTRAIT.getCenterY()), null);
                        checkNativeGrid(image, block, (int) Math.rint(center.getX()),
                                (int) Math.rint(center.getY()), NAMES[index] + " phase " + phase);
                        if (phase == 0) {
                            checkOriginalHeadPixels(player, image, block, (int) Math.rint(center.getX()),
                                    (int) Math.rint(center.getY()), NAMES[index]);
                        }
                        require(filteredBefore.equals(filteredWeaponCache()),
                                NAMES[index] + " selection weapons must bypass the bilinear filter cache");
                        require(!opaqueBounds(image).isEmpty(), NAMES[index] + " portrait must remain visible");
                        if (outputDirectory != null && phase == 0 && window[0] == 1280 && dpi == 1.0) {
                            Path destination = outputDirectory.resolve("selection-pixel-"
                                    + NAMES[index].toLowerCase().replace(' ', '-') + ".png");
                            // Diagnostics are new artifacts; never overwrite an existing PNG.
                            try (OutputStream output = Files.newOutputStream(destination,
                                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                                require(ImageIO.write(image, "png", output), "PNG writer must be available");
                            }
                        }
                    }
                }
                require(animationBefore.equals(animationState(player)),
                        "Painting must not advance or alter existing animation or gameplay state");
            }
        }
    }

    private static void checkOriginalHeadPixels(Player player, BufferedImage image, int block,
            int centerX, int centerY, String name) throws Exception {
        Field rawField = Player.class.getDeclaredField("previewSpriteFrames");
        Field preparedField = Player.class.getDeclaredField("spriteFrames");
        rawField.setAccessible(true);
        preparedField.setAccessible(true);
        BufferedImage original = ((BufferedImage[][]) rawField.get(player))[2][1];
        BufferedImage prepared = ((BufferedImage[][]) preparedField.get(player))[2][1];
        int opaqueSamples = 0;
        int distinguishableSamples = 0;
        BufferedImage foreground = foregroundWeapons(player);
        StringBuilder mismatches = new StringBuilder();
        // At visual time zero the planted body transform is the identity. This
        // central head region is clear of held weapons, hands and armor effects.
        for (int y = 14; y <= 22; y++) {
            for (int x = 29; x <= 34; x++) {
                int expected = original.getRGB(x, y);
                if (expected >>> 24 != 255) continue;
                int physicalX = centerX + (x - 32) * block + block / 2;
                int physicalY = centerY + (y - 32) * block + block / 2;
                int actual = image.getRGB(physicalX, physicalY);
                if (actual != expected) {
                    mismatches.append(String.format(
                            " source (%d,%d), device (%d,%d), block %d: original %08x, rendered %08x, prepared %08x, foreground %08x;",
                            x, y, physicalX, physicalY, block, expected, actual, prepared.getRGB(x, y),
                            foreground.getRGB(x, y)));
                }
                opaqueSamples++;
                if (prepared.getRGB(x, y) != expected) distinguishableSamples++;
            }
        }
        require(opaqueSamples > 0 && distinguishableSamples > 0,
                name + " head checks must distinguish original pixels from prepared gameplay colors");
        require(mismatches.length() == 0, name + " original head sample mismatches:" + mismatches);
    }

    private static BufferedImage foregroundWeapons(Player player) throws Exception {
        BufferedImage layer = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = layer.createGraphics();
        PixelArtRenderer.configure(graphics);
        var drawWeapons = Player.class.getDeclaredMethod("drawWeapons", Graphics2D.class,
                int.class, int.class, int.class, int.class, boolean.class, boolean.class);
        drawWeapons.setAccessible(true);
        try {
            drawWeapons.invoke(player, graphics, 32, 32, 64, 64, false, true);
        } finally {
            graphics.dispose();
        }
        return layer;
    }

    @SuppressWarnings("unchecked")
    private static Map<BufferedImage, BufferedImage> filteredWeaponCache() throws Exception {
        Field field = Player.class.getDeclaredField("FILTERED_HELD_WEAPONS");
        field.setAccessible(true);
        Map<BufferedImage, BufferedImage> cache = (Map<BufferedImage, BufferedImage>) field.get(null);
        synchronized (cache) {
            return new IdentityHashMap<>(cache);
        }
    }

    private static void checkNativeGrid(BufferedImage image, int block, int centerX, int centerY,
            String context) {
        require(block >= 1, "Supported displays must fit native character pixels");
        // Character/equipment composition uses a 3-frame-wide source layer. Its even
        // dimensions anchor the enlarged grid on the snapped physical portrait center.
        int left = centerX - 96 * block;
        int top = centerY - 96 * block;
        int right = centerX + 96 * block;
        int bottom = centerY + 96 * block;
        for (int y = top; y < bottom; y += block) {
            for (int x = left; x < right; x += block) {
                if (x < 0 || y < 0 || x + block > image.getWidth() || y + block > image.getHeight()) continue;
                int pixel = image.getRGB(x, y);
                for (int offsetY = 0; offsetY < block; offsetY++) {
                    for (int offsetX = 0; offsetX < block; offsetX++) {
                        if (image.getRGB(x + offsetX, y + offsetY) != pixel) {
                            throw new IllegalStateException(context
                                    + " must enlarge every composed native pixel into a uniform square");
                        }
                    }
                }
            }
        }
    }

    private static Map<String, Object> animationState(Player player) throws Exception {
        Map<String, Object> state = new LinkedHashMap<>();
        for (String name : new String[] {"animationTime", "visualTime", "locomotionBlend", "spriteRow",
                "worldOffsetX", "worldOffsetY", "attackAnimationTime", "heldWeaponSideX", "health"}) {
            Field field = Player.class.getDeclaredField(name);
            field.setAccessible(true);
            state.put(name, field.get(player));
        }
        return state;
    }

    private static Player player(int index) {
        return switch (index) {
            case 0 -> new Character_Eumann();
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            case 4 -> new Character_Sir_Rakki();
            default -> throw new IllegalArgumentException("Unknown character " + index);
        };
    }

    private static AffineTransform viewport(int[] window, double dpi) {
        double scale = Math.min(window[0] / 1280.0, window[1] / 720.0);
        AffineTransform transform = AffineTransform.getScaleInstance(dpi, dpi);
        transform.translate((window[0] - 1280 * scale) / 2.0, (window[1] - 720 * scale) / 2.0);
        transform.scale(scale, scale);
        return transform;
    }

    private static BufferedImage canvas(int[] window, double dpi) {
        return new BufferedImage((int) Math.ceil(window[0] * dpi), (int) Math.ceil(window[1] * dpi),
                BufferedImage.TYPE_INT_ARGB);
    }

    private static BufferedImage checkerboard(int width, int height) {
        BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] colors = {0xfff21b35, 0xff16cd5e, 0xff2247f4, 0xfff2d718};
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) source.setRGB(x, y, colors[(x + y * 3) % colors.length]);
        }
        return source;
    }

    private static Rectangle opaqueBounds(BufferedImage image) {
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 == 0) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        return right < left ? new Rectangle() : new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private static void smoothHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class GraphicsState {
        private final AffineTransform transform;
        private final RenderingHints hints;
        private final Paint paint;
        private final Composite composite;
        private final Stroke stroke;
        private final Font font;
        private final Area clip;

        private GraphicsState(Graphics2D graphics) {
            transform = graphics.getTransform();
            hints = graphics.getRenderingHints();
            paint = graphics.getPaint();
            composite = graphics.getComposite();
            stroke = graphics.getStroke();
            font = graphics.getFont();
            Shape shape = graphics.getClip();
            clip = shape == null ? null : new Area(shape);
        }

        private void requireUnchanged(Graphics2D graphics, String context) {
            require(transform.equals(graphics.getTransform()) && hints.equals(graphics.getRenderingHints())
                            && paint.equals(graphics.getPaint()) && composite.equals(graphics.getComposite())
                            && stroke.equals(graphics.getStroke()) && font.equals(graphics.getFont()),
                    context + " must preserve ancestor rendering settings for text and smooth UI");
            Shape shape = graphics.getClip();
            if (clip == null || shape == null) {
                require(clip == null && shape == null, context + " must preserve the ancestor clip");
            } else {
                Area difference = new Area(shape);
                difference.exclusiveOr(clip);
                require(difference.isEmpty(), context + " must preserve the ancestor clip");
            }
        }
    }
}
