import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Run directly; assertions do not require the JVM's -ea flag. */
public final class SkillVfxShapesSmokeTest {
    private static final int SIZE = 256;
    private static final double CENTER = SIZE / 2.0;
    private static final double RADIUS = 32.0;
    private static final double ROTATION = 0.37;
    private static final Color COLOR = new Color(90, 190, 240);
    private static final double[] NON_FINITE = {
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    };
    private static int checks;

    @FunctionalInterface
    private interface Painter {
        void paint(Graphics2D graphics);
    }

    @FunctionalInterface
    private interface Effect {
        void paint(Graphics2D graphics, double x, double y, double radius,
                Color color, double rotation, double alpha);
    }

    private static final String[] NAMES = {
        "castSigil", "crescent", "shockwave", "sparkle"
    };

    private static final Effect[] EFFECTS = {
        (g, x, y, r, c, angle, a) ->
            SkillVfxShapes.castSigil(g, x, y, r, c, angle, a),
        (g, x, y, r, c, angle, a) ->
            SkillVfxShapes.crescent(g, x, y, angle, r, 8.0, c, a),
        (g, x, y, r, c, angle, a) ->
            SkillVfxShapes.shockwave(g, x, y, r, c, angle, a, 0.6),
        (g, x, y, r, c, angle, a) ->
            SkillVfxShapes.sparkle(g, x, y, r, c, angle, a)
    };

    private SkillVfxShapesSmokeTest() {
    }

    public static void main(String[] args) {
        for (int i = 0; i < EFFECTS.length; i++) {
            testEffect(NAMES[i], EFFECTS[i]);
        }
        testShapeSpecificInvalidArguments();
        testShapeSpecificExtremeArguments();
        System.out.println("SkillVfxShapes smoke test passed (" + checks + " checks).");
    }

    private static void testEffect(String name, Effect effect) {
        BufferedImage first = render(g -> drawStandard(effect, g, COLOR, 1.0));
        BufferedImage second = render(g -> drawStandard(effect, g, COLOR, 1.0));
        require(alphaSum(first) > 0, name + " must draw visible pixels");
        require(Arrays.equals(pixels(first), pixels(second)),
                name + " must produce deterministic pixels");
        require(isBounded(first), name + " must stay near the requested center");

        BufferedImage partialAlpha = render(g -> drawStandard(effect, g, COLOR, 0.25));
        BufferedImage partialColor = render(g -> drawStandard(effect, g,
                new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 64), 1.0));
        require(alphaSum(partialAlpha) > 0 && alphaSum(partialAlpha) < alphaSum(first),
                name + " must apply fractional effect alpha");
        require(alphaSum(partialColor) > 0 && alphaSum(partialColor) < alphaSum(first),
                name + " must respect the color's alpha");
        BufferedImage overAlpha = render(g -> drawStandard(effect, g, COLOR, 2.0));
        require(Arrays.equals(pixels(first), pixels(overAlpha)),
                name + " must clamp alpha greater than one");

        // Each invalid call gets its own empty image so a valid draw cannot mask it.
        assertBlank(name + " zero alpha", g -> drawStandard(effect, g, COLOR, 0.0));
        assertBlank(name + " negative alpha", g -> drawStandard(effect, g, COLOR, -1.0));
        assertBlank(name + " extreme negative alpha",
                g -> drawStandard(effect, g, COLOR, -Double.MAX_VALUE));
        assertBlank(name + " transparent color", g -> drawStandard(effect, g,
                new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 0), 1.0));
        assertBlank(name + " transparent color with clamped alpha", g -> drawStandard(effect, g,
                new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 0), 2.0));
        assertBlank(name + " null color", g -> drawStandard(effect, g, null, 1.0));
        assertBlank(name + " zero radius", g -> effect.paint(g,
                CENTER, CENTER, 0.0, COLOR, ROTATION, 1.0));
        assertBlank(name + " negative radius", g -> effect.paint(g,
                CENTER, CENTER, -1.0, COLOR, ROTATION, 1.0));

        for (double invalid : NON_FINITE) {
            assertBlank(name + " invalid x " + invalid, g -> effect.paint(g,
                    invalid, CENTER, RADIUS, COLOR, ROTATION, 1.0));
            assertBlank(name + " invalid y " + invalid, g -> effect.paint(g,
                    CENTER, invalid, RADIUS, COLOR, ROTATION, 1.0));
            assertBlank(name + " invalid radius " + invalid, g -> effect.paint(g,
                    CENTER, CENTER, invalid, COLOR, ROTATION, 1.0));
            assertBlank(name + " invalid rotation " + invalid, g -> effect.paint(g,
                    CENTER, CENTER, RADIUS, COLOR, invalid, 1.0));
            assertBlank(name + " invalid alpha " + invalid,
                    g -> drawStandard(effect, g, COLOR, invalid));
        }

        try {
            drawStandard(effect, null, COLOR, 1.0);
        } catch (RuntimeException exception) {
            throw new AssertionError(name + " must accept null graphics as a no-op", exception);
        }
        require(true, name + " null graphics");

        testFiniteExtremes(name, effect);
        testGraphicsIsolation(name, effect);
    }

    private static void testShapeSpecificInvalidArguments() {
        double[] invalidSizes = {
            0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double invalid : invalidSizes) {
            assertBlank("crescent invalid thickness " + invalid,
                    g -> SkillVfxShapes.crescent(g, CENTER, CENTER, ROTATION,
                            RADIUS, invalid, COLOR, 1.0));
            assertBlank("shockwave invalid vertical scale " + invalid,
                    g -> SkillVfxShapes.shockwave(g, CENTER, CENTER, RADIUS,
                            COLOR, ROTATION, 1.0, invalid));
        }
    }

    private static void testFiniteExtremes(String name, Effect effect) {
        double[][] inputs = {
            {Double.MAX_VALUE, CENTER, RADIUS, ROTATION, 1.0},
            {-Double.MAX_VALUE, CENTER, RADIUS, ROTATION, 1.0},
            {CENTER, Double.MAX_VALUE, RADIUS, ROTATION, 1.0},
            {CENTER, -Double.MAX_VALUE, RADIUS, ROTATION, 1.0},
            {CENTER, CENTER, Double.MAX_VALUE, ROTATION, 1.0},
            {CENTER, CENTER, Double.MIN_VALUE, ROTATION, 1.0},
            {CENTER, CENTER, RADIUS, Double.MAX_VALUE, 1.0},
            {CENTER, CENTER, RADIUS, -Double.MAX_VALUE, 1.0},
            {CENTER, CENTER, RADIUS, ROTATION, Double.MAX_VALUE},
            {CENTER, CENTER, RADIUS, ROTATION, Double.MIN_VALUE},
            {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                    Double.MAX_VALUE, Double.MAX_VALUE}
        };
        for (int i = 0; i < inputs.length; i++) {
            final double[] values = inputs[i];
            assertDoesNotThrow(name + " finite extreme case " + i,
                    g -> effect.paint(g, values[0], values[1], values[2],
                            COLOR, values[3], values[4]));
        }
    }

    private static void testShapeSpecificExtremeArguments() {
        for (double extreme : new double[] {Double.MIN_VALUE, Double.MAX_VALUE}) {
            assertDoesNotThrow("crescent extreme thickness " + extreme,
                    g -> SkillVfxShapes.crescent(g, CENTER, CENTER, ROTATION,
                            RADIUS, extreme, COLOR, 1.0));
            assertDoesNotThrow("shockwave extreme vertical scale " + extreme,
                    g -> SkillVfxShapes.shockwave(g, CENTER, CENTER, RADIUS,
                            COLOR, ROTATION, 1.0, extreme));
        }
    }

    private static void testGraphicsIsolation(String name, Effect effect) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.translate(13.0, 17.0);
            graphics.rotate(0.11);
            graphics.scale(0.9, 0.8);
            graphics.setClip(new RoundRectangle2D.Double(20.0, 24.0, 210.0, 195.0, 15.0, 15.0));
            graphics.setColor(Color.MAGENTA);
            graphics.setPaint(new GradientPaint(0.0f, 0.0f, Color.RED,
                    100.0f, 90.0f, Color.BLUE, true));
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.37f));
            graphics.setStroke(new BasicStroke(7.0f, BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_BEVEL, 10.0f, new float[] {3.0f, 5.0f}, 1.0f));
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

            AffineTransform transform = graphics.getTransform();
            Paint paint = graphics.getPaint();
            Composite composite = graphics.getComposite();
            Stroke stroke = graphics.getStroke();
            Shape clip = graphics.getClip();
            Color color = graphics.getColor();
            RenderingHints hints = graphics.getRenderingHints();

            drawStandard(effect, graphics, COLOR, 0.7);
            assertSameGraphicsState(name + " valid draw", graphics,
                    transform, paint, composite, stroke, clip, color, hints);
            drawStandard(effect, graphics, COLOR, Double.NaN);
            assertSameGraphicsState(name + " invalid draw", graphics,
                    transform, paint, composite, stroke, clip, color, hints);

            // A helper must leave the parent's context usable for subsequent drawing.
            graphics.setTransform(new AffineTransform());
            graphics.setClip(null);
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, 2, 2);
            require(image.getRGB(0, 0) == Color.GREEN.getRGB(),
                    name + " must leave parent graphics usable");
        } finally {
            graphics.dispose();
        }
    }

    private static void assertSameGraphicsState(String name, Graphics2D graphics,
            AffineTransform transform, Paint paint, Composite composite, Stroke stroke,
            Shape clip, Color color, RenderingHints hints) {
        require(transform.equals(graphics.getTransform()), name + " changed parent transform");
        require(paint.equals(graphics.getPaint()), name + " changed parent paint");
        require(composite.equals(graphics.getComposite()), name + " changed parent composite");
        require(stroke.equals(graphics.getStroke()), name + " changed parent stroke");
        Area difference = new Area(clip);
        difference.exclusiveOr(new Area(graphics.getClip()));
        require(difference.isEmpty(), name + " changed parent clip");
        require(color.equals(graphics.getColor()), name + " changed parent color");
        require(hints.equals(graphics.getRenderingHints()), name + " changed parent rendering hints");
    }

    private static void drawStandard(Effect effect, Graphics2D graphics, Color color, double alpha) {
        effect.paint(graphics, CENTER, CENTER, RADIUS, color, ROTATION, alpha);
    }

    private static BufferedImage render(Painter painter) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            painter.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void assertBlank(String name, Painter painter) {
        require(alphaSum(render(painter)) == 0, name + " must draw no pixels");
    }

    private static void assertDoesNotThrow(String name, Painter painter) {
        try {
            render(painter);
        } catch (RuntimeException exception) {
            throw new AssertionError(name + " must not throw", exception);
        }
        require(true, name);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    private static long alphaSum(BufferedImage image) {
        long sum = 0;
        for (int pixel : pixels(image)) {
            sum += pixel >>> 24;
        }
        return sum;
    }

    private static boolean isBounded(BufferedImage image) {
        // A generous envelope verifies locality without duplicating any shape's geometry.
        double envelope = RADIUS * 3.0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0
                        && (Math.abs(x - CENTER) > envelope || Math.abs(y - CENTER) > envelope)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
