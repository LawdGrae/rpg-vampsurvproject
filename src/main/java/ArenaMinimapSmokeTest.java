import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Headless checks for minimap projection, marker lifecycle, and read-only painting. */
public final class ArenaMinimapSmokeTest {
    private ArenaMinimapSmokeTest() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                checkProjection();
                checkMarkerLifecycle();
                checkPainting();
                if (args.length > 0) {
                    savePreviews(Path.of(args[0]));
                }
            } catch (Throwable problem) {
                failure[0] = problem;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("Minimap smoke check failed", failure[0]);
        }
        System.out.println("Arena minimap smoke checks passed.");
    }

    private static void checkProjection() {
        Rectangle inner = innerBounds(1280, 720);
        Point2D.Double center = ArenaMinimap.project(235.0, -410.0, 235.0, -410.0, inner);
        near(center.x, inner.getCenterX(), "player projects to horizontal center");
        near(center.y, inner.getCenterY(), "player projects to vertical center");

        Point2D.Double east = ArenaMinimap.project(355.0, -410.0, 235.0, -410.0, inner);
        Point2D.Double west = ArenaMinimap.project(115.0, -410.0, 235.0, -410.0, inner);
        Point2D.Double north = ArenaMinimap.project(235.0, -530.0, 235.0, -410.0, inner);
        Point2D.Double south = ArenaMinimap.project(235.0, -290.0, 235.0, -410.0, inner);
        require(east.x > center.x && west.x < center.x, "east and west retain their direction");
        require(north.y < center.y && south.y > center.y, "the map stays north-up");
        near(east.y, center.y, "horizontal motion does not move vertically");
        near(north.x, center.x, "vertical motion does not move horizontally");
        near(east.x - center.x, center.x - west.x, "opposite directions use the same scale");
        near(east.x - center.x, center.y - north.y, "world axes use the same scale");

        double[][] translations = {{3200.0, 1700.0}, {-9000.0, -6000.0}, {-1536.0, -1024.0}};
        for (double[] shift : translations) {
            Point2D.Double translated = ArenaMinimap.project(
                    355.0 + shift[0], -410.0 + shift[1],
                    235.0 + shift[0], -410.0 + shift[1], inner);
            near(translated.x, east.x, "translating the world preserves horizontal placement");
            near(translated.y, east.y, "translating the world preserves vertical placement");
        }
        Point2D.Double distant = ArenaMinimap.project(10000.0, 10000.0, 0.0, 0.0, inner);
        require(!inner.contains(distant), "distant positions project outside the map for clipping");

        int rightGap = -1;
        for (int[] size : new int[][] {{1024, 640}, {1280, 720}, {1920, 1080}}) {
            Rectangle panel = ArenaMinimap.bounds(size[0], size[1]);
            require(new Rectangle(0, 0, size[0], size[1]).contains(panel), "panel fits viewport");
            int gap = size[0] - panel.x - panel.width;
            if (rightGap < 0) {
                rightGap = gap;
            }
            require(gap == rightGap, "panel keeps its right margin when resizing");
        }
    }

    private static void checkMarkerLifecycle() throws Exception {
        GameLogic logic = game();
        require(logic.getMapMarkers().isEmpty(), "a fresh run has no stale markers");
        double px = logic.getPlayerWorldX();
        double py = logic.getPlayerWorldY();
        TemplateEnemy enemy = new TemplateEnemy(px + 200, py + 100);
        BossEnemy boss = new BossEnemy(px - 300, py + 250);
        TemplateEnemy dead = new TemplateEnemy(px + 50, py + 50);
        dead.takeDamage(100000);
        Gem gem = new Gem(px + 150, py - 180);
        Gem collected = new Gem(px - 80, py - 80);
        collected.update(0, px - 80, py - 80);
        enemies(logic).add(enemy);
        enemies(logic).add(boss);
        enemies(logic).add(dead);
        gems(logic).add(gem);
        gems(logic).add(collected);

        List<GameLogic.MapMarker> snapshot = logic.getMapMarkers();
        require(snapshot.size() == 3, "only living enemies and available gems appear");
        require(hasMarker(snapshot, px + 200, py + 100, GameLogic.MapMarkerKind.ENEMY),
                "normal enemy retains its world coordinates and type");
        require(hasMarker(snapshot, px - 300, py + 250, GameLogic.MapMarkerKind.BOSS),
                "boss retains its world coordinates and type");
        require(hasMarker(snapshot, px + 150, py - 180, GameLogic.MapMarkerKind.GEM),
                "gem retains its world coordinates and type");
        boolean immutable = false;
        try {
            snapshot.add(null);
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "callers cannot modify a marker snapshot");

        enemy.takeDamage(100000);
        gem.update(0, px + 150, py - 180);
        List<GameLogic.MapMarker> remaining = logic.getMapMarkers();
        require(remaining.size() == 1 && remaining.get(0).kind() == GameLogic.MapMarkerKind.BOSS,
                "new snapshots remove killed enemies and collected gems");
        require(snapshot.size() == 3, "previous snapshots remain stable");
        logic.startGame();
        require(logic.getMapMarkers().isEmpty(), "restarting clears the previous run's markers");
    }

    private static void checkPainting() throws Exception {
        ArenaMinimap minimap = new ArenaMinimap(terrain());
        GameLogic logic = game();
        TemplateEnemy nearEnemy = new TemplateEnemy(logic.getPlayerWorldX() + 250,
                logic.getPlayerWorldY() + 100);
        enemies(logic).add(nearEnemy);
        Map<String, Object> before = gameplayState(logic);
        double timeBefore = logic.getGameTimer();
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.translate(2, 3);
            graphics.rotate(0.003);
            graphics.setClip(new Rectangle(100, 100, 1160, 590));
            graphics.setPaint(new Color(91, 127, 161));
            graphics.setBackground(new Color(12, 23, 34));
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.63f));
            graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            AffineTransform transform = graphics.getTransform();
            Shape clip = graphics.getClip();
            Object paint = graphics.getPaint();
            Object composite = graphics.getComposite();
            Object stroke = graphics.getStroke();
            Font font = graphics.getFont();
            Color background = graphics.getBackground();
            RenderingHints hints = (RenderingHints) graphics.getRenderingHints().clone();

            minimap.draw(graphics, logic, 1280, 720);
            require(transform.equals(graphics.getTransform()), "painting preserves the caller's transform");
            Area clipDifference = new Area(clip);
            clipDifference.exclusiveOr(new Area(graphics.getClip()));
            require(clipDifference.isEmpty(), "painting preserves the caller's clip");
            require(paint.equals(graphics.getPaint()) && composite.equals(graphics.getComposite())
                    && stroke.equals(graphics.getStroke()) && font.equals(graphics.getFont())
                    && background.equals(graphics.getBackground()) && hints.equals(graphics.getRenderingHints()),
                    "painting preserves the caller's graphics settings");
        } finally {
            graphics.dispose();
        }
        near(logic.getGameTimer(), timeBefore, "painting does not advance gameplay time");
        require(before.equals(gameplayState(logic)), "painting leaves gameplay and audio state unchanged");

        enemies(logic).clear();
        BufferedImage empty = render(minimap, logic, 1280, 720);
        enemies(logic).add(new TemplateEnemy(logic.getPlayerWorldX() + 10000,
                logic.getPlayerWorldY() - 10000));
        BufferedImage distant = render(minimap, logic, 1280, 720);
        require(equalRegion(empty, distant, innerBounds(1280, 720)),
                "distant enemies cannot paint into the map");
        Rectangle center = innerBounds(1280, 720);
        require((empty.getRGB((int) center.getCenterX(), (int) center.getCenterY()) >>> 24) != 0,
                "the player and terrain produce a visible map");
        for (int[] size : new int[][] {{1024, 640}, {1920, 1080}}) {
            BufferedImage resized = render(minimap, logic, size[0], size[1]);
            Rectangle area = innerBounds(size[0], size[1]);
            require((resized.getRGB((int) area.getCenterX(), (int) area.getCenterY()) >>> 24) != 0,
                    "the map remains visible after resizing");
        }
    }

    private static void savePreviews(Path directory) throws Exception {
        Files.createDirectories(directory);
        GamePanel panel = new GamePanel();
        ((Timer) field(panel, "frameTimer")).stop();
        GameLogic logic = (GameLogic) field(panel, "gameLogic");
        logic.setSoundEnabled(false);
        logic.startGame();
        logic.update(RunEntranceAnimation.DURATION);
        seedPreview(logic);
        savePanel(panel, directory.resolve("map-arena.png"), 1280, 720);

        Player player = (Player) field(logic, "player");
        player.setWorldCollision(null);
        setPlayerPosition(player, -650.0, -10.0);
        enemies(logic).clear();
        gems(logic).clear();
        seedPreview(logic);
        savePanel(panel, directory.resolve("map-negative.png"), 1280, 720);
        savePanel(panel, directory.resolve("map-small.png"), 1024, 640);
        savePanel(panel, directory.resolve("map-large.png"), 1920, 1080);
        System.out.println("Saved minimap previews to " + directory.toAbsolutePath());
    }

    private static void seedPreview(GameLogic logic) throws Exception {
        double px = logic.getPlayerWorldX();
        double py = logic.getPlayerWorldY();
        enemies(logic).add(new TemplateEnemy(px + 250, py + 100));
        enemies(logic).add(new BossEnemy(px - 350, py + 300));
        gems(logic).add(new Gem(px + 180, py - 230));
    }

    private static void savePanel(GamePanel panel, Path path, int width, int height) throws Exception {
        panel.setSize(width, height);
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = frame.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        require(ImageIO.write(frame, "png", path.toFile()), "PNG writer is available");
    }

    private static GameLogic game() {
        GameLogic logic = new GameLogic();
        logic.setSoundEnabled(false);
        logic.startGame();
        logic.update(RunEntranceAnimation.DURATION);
        return logic;
    }

    private static Rectangle innerBounds(int width, int height) {
        Rectangle panel = ArenaMinimap.bounds(width, height);
        return new Rectangle(panel.x + 10, panel.y + 32, 152, 152);
    }

    private static BufferedImage render(ArenaMinimap minimap, GameLogic logic, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            minimap.draw(graphics, logic, width, height);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage terrain() {
        BufferedImage texture = new BufferedImage(1536, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = texture.createGraphics();
        try {
            graphics.setColor(new Color(36, 68, 37));
            graphics.fillRect(0, 0, texture.getWidth(), texture.getHeight());
            graphics.setColor(new Color(61, 91, 44));
            for (int y = 0; y < texture.getHeight(); y += 64) {
                for (int x = (y / 64 % 2) * 32; x < texture.getWidth(); x += 64) {
                    graphics.fillRect(x, y, 18, 18);
                }
            }
        } finally {
            graphics.dispose();
        }
        return texture;
    }

    private static boolean equalRegion(BufferedImage left, BufferedImage right, Rectangle area) {
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasMarker(List<GameLogic.MapMarker> markers, double x, double y,
            GameLogic.MapMarkerKind kind) {
        return markers.stream().anyMatch(marker -> marker.kind() == kind
                && Math.abs(marker.worldX() - x) < 0.000001
                && Math.abs(marker.worldY() - y) < 0.000001);
    }

    private static Map<String, Object> gameplayState(GameLogic logic) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        snapshotScalars(result, "logic", logic);
        snapshotScalars(result, "player", field(logic, "player"));
        for (int i = 0; i < enemies(logic).size(); i++) {
            snapshotScalars(result, "enemy" + i, enemies(logic).get(i));
        }
        for (int i = 0; i < gems(logic).size(); i++) {
            snapshotScalars(result, "gem" + i, gems(logic).get(i));
        }
        result.put("markers", logic.getMapMarkers());
        for (Field candidate : GameLogic.class.getDeclaredFields()) {
            String name = candidate.getName().toLowerCase();
            if (!Modifier.isStatic(candidate.getModifiers()) && (name.contains("audio") || name.contains("sound"))) {
                candidate.setAccessible(true);
                Object audio = candidate.get(logic);
                if (audio != null && !candidate.getType().isPrimitive()) {
                    snapshotScalars(result, candidate.getName(), audio);
                }
            }
        }
        return result;
    }

    private static void snapshotScalars(Map<String, Object> result, String prefix, Object owner)
            throws Exception {
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field candidate : type.getDeclaredFields()) {
                if (!Modifier.isStatic(candidate.getModifiers()) && (candidate.getType().isPrimitive()
                        || candidate.getType().isEnum() || candidate.getType() == String.class)) {
                    candidate.setAccessible(true);
                    result.put(prefix + "." + type.getSimpleName() + "." + candidate.getName(), candidate.get(owner));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Enemy> enemies(GameLogic logic) throws Exception {
        return (List<Enemy>) field(logic, "enemies");
    }

    @SuppressWarnings("unchecked")
    private static List<Gem> gems(GameLogic logic) throws Exception {
        return (List<Gem>) field(logic, "gems");
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void setPlayerPosition(Player player, double worldX, double worldY) throws Exception {
        Field offsetX = Player.class.getDeclaredField("worldOffsetX");
        Field offsetY = Player.class.getDeclaredField("worldOffsetY");
        offsetX.setAccessible(true);
        offsetY.setAccessible(true);
        offsetX.setDouble(player, -worldX);
        offsetY.setDouble(player, -worldY);
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.000001, message + ": " + actual + " vs " + expected);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
