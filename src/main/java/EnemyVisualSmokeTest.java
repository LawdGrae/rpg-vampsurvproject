import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Headless checks for crisp enemy sprites, complete frames and boss fading. */
public class EnemyVisualSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        verifyCrispPixelsAndFacing();
        verifyFrameProgression();
        verifyMinionCutouts();
        verifyStandaloneImages();
        verifyFinalBossFading();
        if (args.length > 0) renderContactSheet(Path.of(args[0]));
        System.out.println("Enemy pixel clarity, animation, source bounds and boss fading passed");
    }

    private static void verifyCrispPixelsAndFacing() {
        BufferedImage source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) source.setRGB(x, y, x < 2 ? Color.RED.getRGB() : Color.BLUE.getRGB());
        }
        Enemy enemy = fixture(source, source);
        BufferedImage right = render(enemy);
        for (int y = 32; y < 96; y++) {
            for (int x = 32; x < 96; x++) {
                require(right.getRGB(x, y) == (x < 64 ? Color.RED.getRGB() : Color.BLUE.getRGB()),
                        "Inherited bicubic interpolation must not blend sprite pixels");
            }
        }
        enemy.update(0.0, -100, 0, 1);
        BufferedImage left = render(enemy);
        require(left.getRGB(40, 40) == Color.BLUE.getRGB()
                && left.getRGB(88, 40) == Color.RED.getRGB(), "Left-facing sprite must mirror correctly");
    }

    private static void verifyFrameProgression() {
        BufferedImage sheet = new BufferedImage(12, 4, BufferedImage.TYPE_INT_ARGB);
        int[] colors = {Color.RED.getRGB(), Color.GREEN.getRGB(), Color.BLUE.getRGB()};
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 12; x++) sheet.setRGB(x, y, colors[x / 4]);
        }
        Enemy enemy = fixture(sheet, sheet);
        require(render(enemy).getRGB(64, 64) == colors[0], "Walk must start at the first frame");
        enemy.update(0.26, 100, 0, 1);
        require(render(enemy).getRGB(64, 64) == colors[1], "Walk frames must advance with update time");
        enemy.update(0.5, 100, 0, 1);
        require(render(enemy).getRGB(64, 64) == colors[0], "Walk must loop using the actual frame count");

        enemy.takeDamage(100);
        enemy.update(0.4, 100, 0, 1);
        require(countColor(render(enemy), colors[1]) > 1000,
                "Death frames must advance alongside the elemental fade");
        enemy.update(0.4, 100, 0, 1);
        require(countColor(render(enemy), colors[2]) > 1000,
                "The last death frame must play before fading completes");
        enemy.update(0.3, 100, 0, 1);
        require(enemy.isFinishedFading() && countVisible(render(enemy)) == 0,
                "A completed death must render no pixels");
    }

    private static void verifyStandaloneImages() {
        BufferedImage tall = new BufferedImage(4, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 4; x++) tall.setRGB(x, y, y < 4 ? Color.RED.getRGB() : Color.BLUE.getRGB());
        }
        BufferedImage result = render(fixture(tall, tall));
        require(countVisible(result) == 32 * 64 && result.getRGB(40, 50) == 0,
                "Standalone portrait sprites must retain their aspect ratio");
        require(result.getRGB(64, 90) == Color.BLUE.getRGB(),
                "Standalone sprites must include their bottom source rows");

        BufferedImage minion = render(new BlueFinalMinion(0, 0));
        int colored = 0;
        for (int y = 32; y < 96; y++) {
            for (int x = 32; x < 96; x++) {
                int rgb = minion.getRGB(x, y);
                if ((rgb & 255) > 80) colored++;
            }
        }
        require(colored > 100, "A final minion must show its body instead of the image's empty corner");

        FinalBossEnemy boss = new FinalBossEnemy(0, 0);
        BufferedImage bossSource = ResourceLoader.loadImage("/main/resources/enemy/FINALBOSS.png");
        BufferedImage bossImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bossImage.createGraphics();
        boss.drawSpriteFrame(graphics, bossSource, 20, 20, 0, false, false);
        graphics.dispose();
        int lowestVisibleY = -1;
        for (int y = 0; y < bossImage.getHeight(); y++) {
            for (int x = 0; x < bossImage.getWidth(); x++) {
                if ((bossImage.getRGB(x, y) >>> 24) != 0) lowestVisibleY = y;
            }
        }
        require(lowestVisibleY > 20 + boss.renderSize * 128 / bossSource.getHeight(),
                "Final boss source rows below 128 must render without cropping");
        boss.triggerSplitState();
        render(boss);
    }

    private static void verifyMinionCutouts() {
        BufferedImage source = new BufferedImage(9, 9, BufferedImage.TYPE_INT_RGB);
        for (int y = 2; y < 7; y++) {
            for (int x = 2; x < 7; x++) source.setRGB(x, y, Color.BLUE.getRGB());
        }
        source.setRGB(4, 4, Color.BLACK.getRGB());
        source.setRGB(2, 2, Color.BLACK.getRGB());
        BufferedImage cutout = EnemySpriteAssets.extractStandaloneSprite(source);
        require(cutout.getWidth() == 5 && cutout.getHeight() == 5,
                "Minion extraction must crop empty margins");
        require(cutout.getRGB(0, 0) == 0 && cutout.getRGB(2, 2) == Color.BLACK.getRGB(),
                "Minion extraction must remove the connected backdrop and preserve enclosed black details");
        require(source.getRGB(0, 0) == Color.BLACK.getRGB(),
                "Preparing a cutout must preserve the original source image");
        String path = "/main/resources/enemy/BLUEMINION.png";
        BufferedImage original = ResourceLoader.loadImage(path);
        int[] originalPixels = original.getRGB(0, 0, original.getWidth(), original.getHeight(), null, 0, original.getWidth());
        BufferedImage first = EnemySpriteAssets.standaloneSprite(path);
        require(first == EnemySpriteAssets.standaloneSprite(path), "Prepared minion sprites must be cached and reused");
        require(Arrays.equals(originalPixels,
                original.getRGB(0, 0, original.getWidth(), original.getHeight(), null, 0, original.getWidth())),
                "Preparing the actual minion asset must preserve its original pixels");

        Enemy[] minions = {new BlueFinalMinion(0, 0), new GreenFinalMinion(0, 0), new RedFinalMinion(0, 0)};
        for (Enemy minion : minions) {
            BufferedImage body = render(minion);
            require(countVisible(body) > 500 && countVisible(body) < 64 * 64,
                    "Minion body must render visibly without an opaque background square");
            minion.takeDamage(Double.MAX_VALUE);
            minion.update(1.1, 100, 0, 1);
            require(minion.isFinishedFading() && countVisible(render(minion)) == 0,
                    "Every final minion must finish its death fade");
        }
    }

    private static void verifyFinalBossFading() {
        FinalBossEnemy boss = new FinalBossEnemy(0, 0);
        boss.triggerSplitState();
        boss.takeDamage(Double.MAX_VALUE);
        boss.update(1.1, 100, 0, 1);
        require(boss.isFinishedFading() && countVisible(render(boss)) == 0,
                "A dead split boss must stop drawing clones and finish its fade");
    }

    private static void renderContactSheet(Path output) throws Exception {
        Enemy[] enemies = {new TemplateEnemy(0, 0), new TemplateEnemy2(0, 0), new TemplateEnemy3(0, 0),
                new BossEnemy(0, 0), new EliteBossEnemy(0, 0), new FinalBossEnemy(0, 0),
                new BlueFinalMinion(0, 0), new GreenFinalMinion(0, 0), new RedFinalMinion(0, 0)};
        String[] names = {"Level 1", "Level 2", "Level 3", "Boss", "Elite boss", "Final boss",
                "Blue minion", "Green minion", "Red minion"};
        BufferedImage sheet = new BufferedImage(1200, 840, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(30, 46, 39));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        for (int index = 0; index < enemies.length; index++) {
            int x = index % 3 * 400;
            int y = index / 3 * 280;
            graphics.setColor(new Color(50, 67, 60));
            graphics.drawRect(x, y, 399, 279);
            graphics.setColor(new Color(235, 240, 230));
            graphics.drawString(names[index] + (index < 6 ? " (game size)" : " (game size / 3x inset)"), x + 16, y + 24);
            if (index < 6) {
                enemies[index].draw(graphics, x + 200, y + 155, 0, 0);
            } else {
                enemies[index].draw(graphics, x + 60, y + 155, 0, 0);
                Graphics2D zoom = (Graphics2D) graphics.create();
                zoom.translate(x + 260, y + 155);
                zoom.scale(3, 3);
                enemies[index].draw(zoom, 0, 0, 0, 0);
                zoom.dispose();
            }
        }
        graphics.dispose();
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        ImageIO.write(sheet, "png", output.toFile());
    }

    private static Enemy fixture(BufferedImage sprite, BufferedImage death) {
        return new Enemy(0, 0, sprite, death, 0, 4, 4, 4, 64, 1, 1, 10) { };
    }

    private static BufferedImage render(Enemy enemy) {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.MAGENTA);
        graphics.setStroke(new BasicStroke(3.5f));
        var hints = graphics.getRenderingHints();
        var stroke = graphics.getStroke();
        var transform = graphics.getTransform();
        var composite = graphics.getComposite();
        enemy.draw(graphics, 64, 64, 0, 0);
        require(hints.equals(graphics.getRenderingHints()) && stroke.equals(graphics.getStroke())
                && transform.equals(graphics.getTransform()) && composite.equals(graphics.getComposite())
                && graphics.getColor().equals(Color.MAGENTA), "Enemy rendering must preserve caller graphics state");
        graphics.dispose();
        return image;
    }

    private static int countColor(BufferedImage image, int expected) {
        return (int) Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .filter(pixel -> (pixel >>> 24) != 0 && (pixel & 0xFFFFFF) == (expected & 0xFFFFFF)).count();
    }

    private static int countVisible(BufferedImage image) {
        return (int) Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .filter(pixel -> (pixel >>> 24) != 0).count();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
