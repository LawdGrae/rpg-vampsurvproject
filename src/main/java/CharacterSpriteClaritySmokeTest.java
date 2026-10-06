import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Checks atlas isolation, transparency safety and the bundled character art. */
public class CharacterSpriteClaritySmokeTest {
    private static final String[] NAMES = {"Eumann", "Haze", "Yuexin", "Ziea"};
    private static final String[] FILES = {"CharEumann.png", "CharHaze.png", "CharYuexin.png", "CharacterZiea.png"};

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        checkFrameIsolation();
        checkHiddenColorSafety();
        checkPixelArtAndInvalidGeometry();
        checkBundledCharacters();
        boolean sourceOnly = Arrays.asList(args).contains("--source-only");
        if (!sourceOnly) {
            checkPortraitScaling();
            checkPortraitPixelGrid();
        }
        String outputPath = Arrays.stream(args).filter(argument -> !argument.equals("--source-only"))
                .findFirst().orElse(null);
        if (outputPath != null) {
            renderComparison(new File(outputPath));
        }
        System.out.println(sourceOnly
                ? "Character source clarity: atlas isolation, transparency, cache and source preservation passed"
                : "Character clarity: source art, HiDPI portraits and idle/attack pixel grids passed");
    }

    private static void checkFrameIsolation() {
        BufferedImage source = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
        int[] colors = {0xb4cc4422, 0xb43366dd, 0xb422aa66, 0xb4ddaa33};
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                source.setRGB(x, y, colors[y / 3 * 2 + x / 3]);
            }
        }
        int[] before = pixels(source);
        BufferedImage prepared = CharacterSpriteImages.prepareSheet(source, 3, 3);
        require(prepared != source, "Soft art must receive a separate prepared sheet");
        require(prepared == CharacterSpriteImages.prepareSheet(source, 3, 3), "Prepared art must be cached");
        require(Arrays.equals(before, pixels(source)), "Preparation must leave original pixels intact");
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                require((prepared.getRGB(x, y) & 0xffffff) == (source.getRGB(x, y) & 0xffffff),
                        "Neighboring frame colors must not bleed across the seam");
            }
        }
        require(CharacterSpriteImages.prepareSheet(source, 6, 3) != prepared,
                "The cache must distinguish frame geometry");
    }

    private static void checkHiddenColorSafety() {
        BufferedImage source = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                source.setRGB(x, y, 0xb4aa5522);
            }
        }
        source.setRGB(1, 1, 0x0000ff00);
        source.setRGB(0, 0, 0xffaa5522);
        BufferedImage prepared = CharacterSpriteImages.prepareSheet(source, 3, 3);
        require(prepared.getRGB(1, 1) == source.getRGB(1, 1), "Fully transparent pixels must stay unchanged");
        require(prepared.getRGB(0, 0) >>> 24 == 255, "Opaque art must remain opaque");
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x != 1 || y != 1) {
                    require((prepared.getRGB(x, y) & 0xffffff) == 0xaa5522,
                            "Hidden transparent colors must not create a visible halo");
                }
            }
        }
    }

    private static void checkPixelArtAndInvalidGeometry() {
        BufferedImage pixelArt = ResourceLoader.loadImage("/main/resources/character/temp_sheet.png");
        require(CharacterSpriteImages.prepareSheet(pixelArt, 16, 18) == pixelArt,
                "Crisp binary-alpha pixel art should remain unchanged");
        for (int[] geometry : new int[][] {{0, 18}, {16, -1}, {17, 18}, {16, 73}}) {
            try {
                CharacterSpriteImages.prepareSheet(pixelArt, geometry[0], geometry[1]);
                throw new IllegalStateException("Invalid frame geometry must be rejected");
            } catch (IllegalArgumentException expected) {
                // Invalid dimensions are rejected before a cached image is returned.
            }
        }
    }

    private static void checkBundledCharacters() {
        for (String file : FILES) {
            BufferedImage source = ResourceLoader.loadImage("/main/resources/character/" + file);
            int[] before = pixels(source);
            BufferedImage prepared = CharacterSpriteImages.prepareSheet(source, 64, 64);
            require(prepared.getWidth() == source.getWidth() && prepared.getHeight() == source.getHeight(),
                    "Character frame geometry must remain unchanged");
            int changed = 0;
            int originalSoft = 0;
            int preparedSoft = 0;
            for (int index = 0; index < before.length; index++) {
                int actual = prepared.getRGB(index % source.getWidth(), index / source.getWidth());
                int alpha = before[index] >>> 24;
                if (alpha == 0) require(actual == before[index], "Transparent source pixels must remain untouched");
                if (alpha == 255) require(actual >>> 24 == 255, "Opaque character details must remain opaque");
                if (actual >>> 24 > 0) {
                    for (int shift : new int[] {0, 8, 16}) {
                        require(Math.abs((actual >>> shift & 255) - (before[index] >>> shift & 255)) <= 56,
                                "Sharpening must retain bounded source colors for " + file);
                    }
                }
                if (actual != before[index]) changed++;
                if (alpha > 0 && alpha < 255) originalSoft++;
                if (actual >>> 24 > 0 && actual >>> 24 < 255) preparedSoft++;
            }
            require(changed > 0 && originalSoft > 0 && preparedSoft == 0,
                    "Soft source fringe must become crisp pixel coverage for " + file);
            require(Arrays.equals(before, pixels(source)), "Bundled source must remain unchanged for " + file);
        }
    }

    private static void checkPortraitScaling() throws Exception {
        // Reflection lets the source-art checks compile independently while Player is edited.
        Class<?> playerType = Class.forName("Player");
        var sheet = playerType.getDeclaredField("spriteSheet");
        sheet.setAccessible(true);
        var drawPreview = playerType.getMethod("drawPreview", Graphics2D.class, Rectangle.class);
        var draw = playerType.getMethod("draw", Graphics2D.class, int.class, int.class);
        for (String name : NAMES) {
            Class<?> character = Class.forName("Character_" + name);
            Object player = character.getConstructor().newInstance();
            Object restarted = character.getConstructor().newInstance();
            require(sheet.get(player) == sheet.get(restarted),
                    "Restarting a character must reuse its prepared art");
            for (double deviceScale : new double[] {1.0, 1.25, 1.5, 2.0}) {
                BufferedImage actual = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
                Graphics2D preview = actual.createGraphics();
                preview.scale(deviceScale, deviceScale);
                preview.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                drawPreview.invoke(player, preview, new Rectangle(64, 64, 192, 192));
                preview.dispose();

                BufferedImage expected = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
                Graphics2D reference = expected.createGraphics();
                reference.translate(160 * deviceScale, 160 * deviceScale);
                int wholePixels = (int) Math.floor(3 * deviceScale);
                reference.scale(wholePixels, wholePixels);
                draw.invoke(player, reference, 0, 0);
                reference.dispose();
                require(Arrays.equals(pixels(actual), pixels(expected)),
                        "Portrait must match a whole-pixel gameplay render at device scale " + deviceScale);
            }
        }
    }

    private static void checkPortraitPixelGrid() throws Exception {
        Class<?> playerType = Class.forName("Player");
        var preview = playerType.getMethod("drawPreview", Graphics2D.class, Rectangle.class);
        var update = playerType.getMethod("updatePreview", double.class);
        var attack = playerType.getMethod("playAttackAnimation", Class.forName("AbilityDefinition"), double.class);
        for (String name : NAMES) {
            Object player = Class.forName("Character_" + name).getConstructor().newInstance();
            for (int phase = 0; phase < 3; phase++) {
                if (phase == 1) update.invoke(player, 0.37);
                if (phase == 2) {
                    attack.invoke(player, null, 0.34);
                    update.invoke(player, 0.13);
                }
                BufferedImage image = new BufferedImage(384, 384, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                preview.invoke(player, graphics, new Rectangle(96, 96, 192, 192));
                graphics.dispose();
                // A three-times portrait must enlarge each composed native pixel into one
                // uniform 3x3 block, even while breathing or rotating a held weapon.
                for (int y = 0; y < 384; y += 3) {
                    for (int x = 0; x < 384; x += 3) {
                        int expected = image.getRGB(x, y);
                        for (int offsetY = 0; offsetY < 3; offsetY++) {
                            for (int offsetX = 0; offsetX < 3; offsetX++) {
                                require(image.getRGB(x + offsetX, y + offsetY) == expected,
                                        name + " phase " + phase + " must retain its native portrait pixel grid");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderComparison(File output) throws Exception {
        BufferedImage image = new BufferedImage(624, 1220, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(24, 30, 44));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(232, 226, 213));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.drawString("Original", 58, 32);
        graphics.drawString("Prepared", 364, 32);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int row = 0; row < FILES.length; row++) {
            BufferedImage source = ResourceLoader.loadImage("/main/resources/character/" + FILES[row]);
            BufferedImage prepared = CharacterSpriteImages.prepareSheet(source, 64, 64);
            int top = 62 + row * 290;
            graphics.setColor(new Color(232, 226, 213));
            graphics.drawString(NAMES[row], 20, top + 16);
            graphics.drawImage(source, 20, top + 26, 276, top + 282, 64, 128, 128, 192, null);
            graphics.drawImage(prepared, 330, top + 26, 586, top + 282, 64, 128, 128, 192, null);
        }
        graphics.dispose();
        File parent = output.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create " + parent);
        }
        ImageIO.write(image, "png", output);
        System.out.println(output.getAbsolutePath());
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
