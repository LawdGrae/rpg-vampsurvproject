import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Headless checks of procedural contact feedback and independent live projectile clocks. */
public final class SkillVfxSmokeTest {
    private SkillVfxSmokeTest() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0 && args[0].equals("--loaded-atlas")) {
            verifyLoadedAtlasPrecedence();
            return;
        }
        verifyImpactFeedback();
        verifyProjectileClocks();
        verifyHolyProjectileReleaseAnchor();
        verifyAtlasInIsolatedRuntime();
        System.out.println("Skill VFX smoke passed: element contacts, clock partitions, draw isolation, "
                + "live projectiles, expiration and reviewed atlas precedence");
    }

    private static void verifyImpactFeedback() {
        for (DamageElement element : DamageElement.values()) {
            String id = switch (element) {
                case FIRE -> "flame_burst";
                case ICE -> "ice_shard";
                case LIGHTNING -> "lightning_strike";
                case SHADOW -> "shadow_strike";
                case HOLY -> "holy_bolt";
                case POISON -> "poison_blade";
                default -> "heavy_slash";
            };
            CombatImpactEffect coarse = new CombatImpactEffect(id, 7.25, -3.5, element);
            CombatImpactEffect fine = new CombatImpactEffect(id, 7.25, -3.5, element);
            coarse.update(0.17);
            for (int part = 0; part < 34; part++) fine.update(0.005);
            BufferedImage image = render(coarse);
            require(visible(image) > 0, element + " skill impact needs visible contact feedback");
            identical(image, render(fine), element + " impact is independent of update partitions");
            identical(image, render(coarse), element + " impact rendering cannot advance its clock");
            assertGraphicsIsolation(g -> coarse.draw(g, 128, 128, 0, 0), element + " impact");
            coarse.update(Double.NaN);
            coarse.update(Double.POSITIVE_INFINITY);
            coarse.update(-1);
            identical(image, render(coarse), element + " invalid updates cannot corrupt contact feedback");
            coarse.update(0.07);
            require(!Arrays.equals(pixels(image), pixels(render(coarse))), element + " contact must decay smoothly");
            coarse.update(10);
            require(coarse.isExpired() && visible(render(coarse)) == 0, element + " contact must expire");
        }
    }

    private static void verifyProjectileClocks() {
        for (String id : new String[] {"flame_burst", "ice_shard"}) {
            AbilityVisualEffect shot = effect(id, 1.8);
            shot.setProjectilePosition(35.25, -10.75, 0.3, 0.21);
            BufferedImage first = render(shot);
            require(visible(first) > 0, id + " live projectile must have travel VFX");
            shot.update(0.4);
            identical(first, render(shot), id + " projectile age belongs to its live actor");
            shot.setProjectilePosition(35.25, -10.75, 0.3, 0.51);
            BufferedImage next = render(shot);
            require(visible(next) > 0 && !Arrays.equals(pixels(first), pixels(next)),
                    id + " travel VFX must animate from the projectile clock");
            identical(next, render(shot), id + " repeated draws must be deterministic");
            assertGraphicsIsolation(g -> shot.draw(g, 128, 128, 0, 0), id + " travel");
            shot.setProjectilePosition(-25.5, 14.25, Math.PI + 0.3, 0.51);
            require(visible(render(shot)) > 0 && !Arrays.equals(pixels(next), pixels(render(shot))),
                    id + " travel VFX must follow its actual location and heading");
            shot.setProjectilePosition(-25.5, 14.25, 0, 1.8);
            require(shot.isExpired() && visible(render(shot)) == 0, id + " completed projectile cannot leave VFX");
        }
    }

    private static void verifyHolyProjectileReleaseAnchor() {
        AbilityDefinition definition = AbilityDefinition.createAll().stream()
                .filter(d -> d.getId().equals("holy_bolt")).findFirst().orElseThrow();
        double duration = AbilityAnimationTiming.duration(definition);
        AbilityVisualEffect baseline = effect("holy_bolt", duration);
        AbilityVisualEffect moving = effect("holy_bolt", duration);
        baseline.update(duration * 0.54);
        moving.update(duration * 0.54);
        BufferedImage launched = render(baseline);
        require(visible(launched) > 0, "Holy Light must show its released projectile");
        moving.setCastOrigin(70, 45);
        moving.setCasterPosition(90, 65);
        identical(launched, render(moving), "Released Holy Light cannot follow subsequent staff movement");
        baseline.update(duration * 0.03);
        moving.update(duration * 0.03);
        identical(render(baseline), render(moving),
                "Released Holy Light must retain its captured launch point on later updates");
    }

    /** A fresh JVM loads fixtures through the production asset path without mutating live caches. */
    private static void verifyAtlasInIsolatedRuntime() throws Exception {
        Path parent = Path.of("out", "vfx-tests").toAbsolutePath().normalize();
        Files.createDirectories(parent);
        Path fixtureRoot = Files.createTempDirectory(parent, "atlas-runtime-");
        try {
            Path effects = fixtureRoot.resolve("src/main/resources/effects");
            Files.createDirectories(effects);
            BufferedImage source = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
            source.setRGB(2, 2, 0xff00ff00);
            ImageIO.write(source, "png", effects.resolve("fixture.png").toFile());
            Properties manifest = new Properties();
            manifest.setProperty("schema.version", "1");
            manifest.setProperty("sheet.fixture.path", "fixture.png");
            manifest.setProperty("sheet.fixture.width", "5");
            manifest.setProperty("sheet.fixture.height", "5");
            manifest.setProperty("skill.heavy_slash.tracks", "action,impact");
            manifest.setProperty("skill.ice_shard.tracks", "travel");
            addTrack(manifest, "heavy_slash", "action", false);
            manifest.setProperty("skill.heavy_slash.action.hitFrame", "0");
            addTrack(manifest, "heavy_slash", "impact", false);
            addTrack(manifest, "ice_shard", "travel", true);
            try (OutputStream output = Files.newOutputStream(effects.resolve("skill_effects.properties"))) {
                manifest.store(output, "Temporary source-neutral reviewed track fixtures");
            }
            Path javaCommand = Path.of(System.getProperty("java.home"), "bin", "java.exe");
            if (!Files.isRegularFile(javaCommand)) javaCommand = javaCommand.resolveSibling("java");
            Path classes = Path.of(SkillVfxSmokeTest.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath();
            Process process = new ProcessBuilder(javaCommand.toString(), "-Djava.awt.headless=true", "-cp",
                    classes.toString(), SkillVfxSmokeTest.class.getName(), "--loaded-atlas")
                    .directory(fixtureRoot.toFile()).redirectErrorStream(true).start();
            if (!process.waitFor(45, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("Isolated atlas precedence check timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            require(process.exitValue() == 0, "Loaded atlas precedence failed:\n" + output);
        } finally {
            try (var paths = Files.walk(fixtureRoot)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void addTrack(Properties manifest, String id, String track, boolean loop) {
        String prefix = "skill." + id + "." + track + ".";
        manifest.setProperty(prefix + "reviewed", "true");
        manifest.setProperty(prefix + "sheet", "fixture");
        manifest.setProperty(prefix + "frames", "0,0,5,5,0.8,0.5,0.5");
        manifest.setProperty(prefix + "canvasWidth", "5");
        manifest.setProperty(prefix + "loop", Boolean.toString(loop));
        manifest.setProperty(prefix + "playOnce", Boolean.toString(!loop));
    }

    private static void verifyLoadedAtlasPrecedence() {
        require(SkillEffectAtlas.getAnimation("heavy_slash", "action") != null,
                "Production path must load the reviewed action fixture");
        AbilityVisualEffect windup = effect("heavy_slash", 0.78);
        windup.update(0.78 * 0.20);
        BufferedImage anticipation = render(windup);
        require(visible(anticipation) > 0, "Reviewed action-only track must retain built-in anticipation");
        require(Arrays.stream(pixels(anticipation)).anyMatch(pixel ->
                        (pixel >>> 24) != 0 && (pixel & 0xffffff) != 0x00ff00),
                "Before release, an action-only fixture cannot replace procedural charge with action art");
        AbilityVisualEffect slash = effect("heavy_slash", 0.78);
        slash.update(0.5);
        onlyFixturePixels(render(slash), "Reviewed action must take precedence over procedural art");
        CombatImpactEffect impact = new CombatImpactEffect("heavy_slash", 0, 0, DamageElement.FIRE);
        impact.update(0.12);
        onlyFixturePixels(render(impact), "Reviewed impact must take precedence over procedural contact art");
        AbilityVisualEffect shot = effect("ice_shard", 1.8);
        shot.setProjectilePosition(0, 0, 0, 0.2);
        onlyFixturePixels(render(shot), "Reviewed travel must take precedence over procedural projectile art");
        System.out.println("Production-path atlas precedence passed");
    }

    private static void onlyFixturePixels(BufferedImage image, String context) {
        require(visible(image) > 0, context + " (fixture must render)");
        for (int pixel : pixels(image)) if ((pixel >>> 24) != 0) {
            require((pixel & 0xffffff) == 0x00ff00, context + " (unexpected fallback colors)");
        }
    }

    private static AbilityVisualEffect effect(String id, double lifetime) {
        AbilityDefinition definition = AbilityDefinition.createAll().stream()
                .filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
        return new AbilityVisualEffect(definition, -45.5, 0.25, 35.25, -10.75,
                90, Color.CYAN, lifetime);
    }

    private interface Drawing { void draw(Graphics2D graphics); }

    private static void assertGraphicsIsolation(Drawing drawing, String context) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.translate(2.25, 4.5);
            g.scale(0.8, 0.9);
            g.setClip(new Rectangle(7, 9, 225, 210));
            g.setComposite(AlphaComposite.SrcOver.derive(0.63f));
            g.setStroke(new BasicStroke(4.25f));
            g.setColor(new Color(83, 71, 115));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            var transform = g.getTransform();
            var clip = g.getClipBounds();
            var composite = g.getComposite();
            var stroke = g.getStroke();
            var color = g.getColor();
            var hints = g.getRenderingHints();
            drawing.draw(g);
            require(transform.equals(g.getTransform()) && clip.equals(g.getClipBounds())
                    && composite.equals(g.getComposite()) && stroke.equals(g.getStroke())
                    && color.equals(g.getColor()) && hints.equals(g.getRenderingHints()),
                    context + " must preserve all caller graphics state");
        } finally { g.dispose(); }
    }

    private static BufferedImage render(AbilityVisualEffect effect) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { effect.draw(g, 128, 128, 0, 0); } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage render(CombatImpactEffect effect) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { effect.draw(g, 128, 128, 0, 0); } finally { g.dispose(); }
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private static int visible(BufferedImage image) {
        int count = 0;
        for (int pixel : pixels(image)) if ((pixel >>> 24) != 0) count++;
        return count;
    }

    private static void identical(BufferedImage a, BufferedImage b, String context) {
        require(Arrays.equals(pixels(a), pixels(b)), context);
    }

    private static void require(boolean condition, String context) {
        if (!condition) throw new AssertionError(context);
    }
}
