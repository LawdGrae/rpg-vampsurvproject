import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Source-neutral fixtures test the atlas mechanism; no fixtures are packaged as game art. */
public final class SkillEffectAtlasSmokeTest {
    private SkillEffectAtlasSmokeTest() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        require(SkillEffectAtlas.getMetadata().size() == 25, "Exactly 25 requested skill mappings required");
        require(SkillEffectAtlas.getMetadata().stream().filter(SkillEffectAtlas.SkillMetadata::passive).count() == 5,
                "Each reference class needs its own passive");
        require("Spiral Cut".equals(SkillEffectAtlas.getMetadata("twin_fang").name()), "Spiral Cut stable ID");
        require(SkillEffectAtlas.getMetadata("silent_execution") == null, "Legacy execution cannot reuse Spiral Cut art");
        Path parent = Path.of("out", "atlas-tests").toAbsolutePath().normalize();
        Files.createDirectories(parent);
        Path temporary = Files.createTempDirectory(parent, "fixture-");
        try {
            BufferedImage fixture = new BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB);
            fixture.setRGB(0, 1, 0xffff0000);
            fixture.setRGB(4, 1, 0xff0000ff);
            ImageIO.write(fixture, "png", temporary.resolve("fixture.png").toFile());
            Properties properties = manifest();
            SkillEffectAtlas.SkillAnimation animation = inspect(temporary, properties).getAnimation("heavy_slash", "action");
            require(animation != null, "Reviewed transparent source frames should load");
            near(animation.getDurationSeconds(), 0.3, "Explicit frame durations");
            near(animation.getHitTimeSeconds(), 0.15, "Hit event starts at authored frame");
            require(animation.getFrameIndex(0.04) == -1, "Delay hides effect");
            require(animation.getFrameIndex(0.05) == 0, "First frame appears at activation delay");
            require(animation.getFrameIndex(animation.getHitTimeSeconds()) == 1, "Hit event and frame switch agree");
            require(animation.getFrameIndex(0.34) == 1, "Impact frame retains explicit duration");
            require(animation.getFrameIndex(0.36) == -1 && animation.isFinished(0.36), "One-shot expires");
            require(animation.getFrameIndex(Double.NaN) == -1, "Invalid clock cannot draw");
            verifyDrawing(animation);

            properties.setProperty("skill.heavy_slash.action.hitFrame", "-1");
            properties.setProperty("skill.heavy_slash.action.hitFrames", "0,1");
            SkillEffectAtlas.SkillAnimation multiHit = inspect(temporary, properties).getAnimation("heavy_slash", "action");
            require(multiHit != null && multiHit.getHitTimesSeconds().length == 2, "Explicit multi-hit events");
            near(multiHit.getHitTimesSeconds()[0], 0.05, "First hit includes start delay");
            near(multiHit.getHitTimesSeconds()[1], 0.15, "Later hit follows authored frame duration");
            double[] copiedEvents = multiHit.getHitTimesSeconds(); copiedEvents[0] = 99;
            near(multiHit.getHitTimeSeconds(), 0.05, "Event arrays cannot mutate cached metadata");
            properties.setProperty("skill.heavy_slash.action.hitFrames", "1,0");
            rejected(inspect(temporary, properties), "increasing", "Unsorted multi-hit rejection");
            properties.setProperty("skill.heavy_slash.action.hitFrames", "0,0");
            rejected(inspect(temporary, properties), "increasing", "Duplicate multi-hit rejection");
            properties.setProperty("skill.heavy_slash.action.hitFrames", "0,2");
            rejected(inspect(temporary, properties), "inside", "Missing multi-hit frame rejection");
            properties = manifest();

            properties.setProperty("skill.heavy_slash.action.loop", "true");
            properties.setProperty("skill.heavy_slash.action.playOnce", "false");
            animation = inspect(temporary, properties).getAnimation("heavy_slash", "action");
            require(animation.getFrameIndex(0.37) == 0 && !animation.isFinished(100), "Loop wraps without expiring");
            properties.setProperty("skill.heavy_slash.action.frames", "5,0,3,3,0.1,0.5,0.5");
            rejected(inspect(temporary, properties), "outside source", "Out-of-bounds crop rejection");
            properties = manifest();
            properties.setProperty("sheet.fixture.width", "7");
            rejected(inspect(temporary, properties), "dimensions", "Resized source rejects stale rectangles");
            properties = manifest();
            properties.setProperty("skill.heavy_slash.action.frames", "0,0,3,3,NaN,0.5,0.5");
            rejected(inspect(temporary, properties), "non-finite", "Invalid duration rejection");
            properties = manifest();
            properties.setProperty("skill.heavy_slash.action.hitFrame", "2");
            rejected(inspect(temporary, properties), "index", "Nonexistent hit frame rejection");
            properties = manifest();
            properties.setProperty("skill.heavy_slash.action.reviewed", "false");
            rejected(inspect(temporary, properties), "awaiting reviewed", "Unreviewed frames stay unavailable");
            properties = manifest();
            properties.setProperty("skill.heavy_slash.tracks", "passive");
            rejected(inspect(temporary, properties), "unsupported", "Wrong phase cannot replace skill art");
            properties = manifest();
            BufferedImage opaque = new BufferedImage(6, 3, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(opaque, "png", temporary.resolve("fixture.png").toFile());
            rejected(inspect(temporary, properties), "alpha", "Opaque reference requires isolated transparent source");
            System.out.println("Skill atlas metadata, crop validation, timing, transparency, mirroring and nearest-neighbor checks passed");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static Properties manifest() {
        Properties properties = new Properties();
        properties.setProperty("schema.version", "1");
        properties.setProperty("sheet.fixture.path", "fixture.png");
        properties.setProperty("sheet.fixture.width", "6");
        properties.setProperty("sheet.fixture.height", "3");
        properties.setProperty("skill.heavy_slash.tracks", "action");
        String prefix = "skill.heavy_slash.action.";
        properties.setProperty(prefix + "reviewed", "true");
        properties.setProperty(prefix + "sheet", "fixture");
        properties.setProperty(prefix + "frames", "0,0,3,3,0.1,0.5,0.5;3,0,3,3,0.2,0.5,0.5");
        properties.setProperty(prefix + "canvasWidth", "3");
        properties.setProperty(prefix + "startDelaySeconds", "0.05");
        properties.setProperty(prefix + "hitFrame", "1");
        return properties;
    }

    private static SkillEffectAtlas.Atlas inspect(Path directory, Properties properties) throws Exception {
        Path manifest = directory.resolve("fixture.properties");
        try (OutputStream output = Files.newOutputStream(manifest)) { properties.store(output, "temporary atlas fixture"); }
        return SkillEffectAtlas.inspectManifest(manifest);
    }

    private static void verifyDrawing(SkillEffectAtlas.SkillAnimation animation) {
        BufferedImage right = render(animation, false, 0.5, 0.07), left = render(animation, true, 0.5, 0.07);
        int minRight = 32, maxLeft = -1, count = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int pixel = right.getRGB(x, y);
                if ((pixel >>> 24) != 0) {
                    require((pixel & 0xffffff) == 0xff0000, "Nearest-neighbor cannot invent blended colors");
                    require((pixel >>> 24) == 128, "Parent alpha must be applied once");
                    minRight = Math.min(minRight, x); count++;
                }
                if ((left.getRGB(x, y) >>> 24) != 0) maxLeft = Math.max(maxLeft, x);
            }
        }
        require(count == 4, "One source pixel scales to exactly four crisp pixels");
        require(minRight + maxLeft == 31, "Mirroring retains the authored focal point");
        BufferedImage second = render(animation, false, 1, 0.2);
        require(second.getRGB(16, 16) == 0xff0000ff, "Each clock uses its own source frame");
        require(countVisible(render(animation, false, 1, 0.01)) == 0, "No premature visual release");
        require(countVisible(render(animation, false, 1, 0.4)) == 0, "Expired animation cleans up");
    }

    private static BufferedImage render(SkillEffectAtlas.SkillAnimation animation, boolean mirror,
            double parentAlpha, double elapsed) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver.derive((float) parentAlpha));
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            AffineTransform before = graphics.getTransform();
            var composite = graphics.getComposite();
            animation.drawAt(graphics, 16, 16, 6, 0, elapsed, 1, mirror);
            require(before.equals(graphics.getTransform()), "Draw must preserve parent transform");
            require(composite.equals(graphics.getComposite()), "Draw must preserve parent composite");
            require(graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION)
                    == RenderingHints.VALUE_INTERPOLATION_BILINEAR, "Draw must preserve parent sampling hint");
        } finally { graphics.dispose(); }
        return image;
    }

    private static int countVisible(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) != 0) count++;
        }
        return count;
    }

    private static void rejected(SkillEffectAtlas.Atlas atlas, String diagnostic, String message) {
        require(atlas.getAnimationCount() == 0, message);
        require(atlas.getDiagnostics().stream().anyMatch(value -> value.contains(diagnostic)), message + " diagnosis");
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.0000001, message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
