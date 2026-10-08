import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Renders entrance stills and looping actual-game previews for each selectable hero. */
public class RunEntranceRenderCheck {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int CELL_WIDTH = 640;
    private static final int CELL_HEIGHT = 360;
    private static final int LABEL_HEIGHT = 36;
    private static final int TITLE_HEIGHT = 56;
    private static final double[] TIMES = {0.0, 0.28, 0.52, 0.63, 0.72, 0.80, 1.04, 1.20, 1.45, 1.72};

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path output = args.length == 0
                ? Path.of("out", "survival-review", "run-entrance") : Path.of(args[0]);
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                render(output);
            } catch (Exception exception) {
                throw new RuntimeException("Could not render run entrance review frames.", exception);
            }
        });
        System.out.println("Run entrance review: " + output.toAbsolutePath());
        System.out.println("Saved " + TIMES.length * 5 + " full game frames, contact-sheet.png, and five looping entrance GIFs.");
    }

    private static void render(Path output) throws Exception {
        GamePanel panel = new GamePanel();
        Timer frameTimer = (Timer) field(panel, "frameTimer");
        frameTimer.stop();
        panel.setSize(WIDTH, HEIGHT);
        panel.doLayout();
        GameLogic logic = (GameLogic) field(panel, "gameLogic");
        logic.setViewportSize(WIDTH, HEIGHT);
        logic.setSoundEnabled(false);
        int rowHeight = CELL_HEIGHT + LABEL_HEIGHT;
        BufferedImage sheet = new BufferedImage(CELL_WIDTH * TIMES.length,
                TITLE_HEIGHT + rowHeight * 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D review = sheet.createGraphics();
        try {
            review.setColor(new Color(15, 20, 28));
            review.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            review.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
            review.setColor(new Color(231, 237, 246));
            review.drawString("RUN ENTRANCE  /  full game panel  /  five heroes", 18, 35);
            review.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            for (int hero = 0; hero < 5; hero++) {
                logic.selectCharacter(hero);
                if (logic.getSelectedCharacterIndex() != hero) throw new AssertionError("Hero " + hero + " is not selectable.");
                logic.startGame();
                String name = logic.getSelectedCharacterName();
                String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                double previousTime = 0.0;
                for (int frame = 0; frame < TIMES.length; frame++) {
                    double time = TIMES[frame];
                    logic.update(time - previousTime);
                    previousTime = time;
                    BufferedImage image = capture(panel, WIDTH, HEIGHT);
                    String filename = String.format(Locale.ROOT, "hero-%02d-%s-t%03d.png",
                            hero + 1, slug, Math.round(time * 100.0));
                    writePng(image, output.resolve(filename));
                    int x = frame * CELL_WIDTH;
                    int y = TITLE_HEIGHT + hero * rowHeight;
                    review.setColor(new Color(23, 30, 40));
                    review.fillRect(x, y, CELL_WIDTH, LABEL_HEIGHT);
                    review.setColor(new Color(224, 232, 244));
                    review.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
                    String phase = time < RunEntranceAnimation.FALL_DURATION ? "descending"
                            : time < RunEntranceAnimation.DURATION ? "landing" : "playing";
                    review.drawString(String.format(Locale.ROOT, "%s  |  %.2fs  |  %s", name, time, phase), x + 12, y + 24);
                    review.drawImage(image, x, y + LABEL_HEIGHT, CELL_WIDTH, CELL_HEIGHT, null);
                    review.setColor(new Color(55, 65, 80));
                    review.drawRect(x, y, CELL_WIDTH - 1, rowHeight - 1);
                }
                animatedPreview(panel, logic, output.resolve("entrance-" + slug + ".gif"));
            }
        } finally {
            review.dispose();
            frameTimer.stop();
        }
        writePng(sheet, output.resolve("contact-sheet.png"));
    }

    private static BufferedImage capture(GamePanel panel, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(width / (double) WIDTH, height / (double) HEIGHT);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void animatedPreview(GamePanel panel, GameLogic logic, Path path) throws Exception {
        logic.startGame();
        logic.setSoundEnabled(false);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) throw new IOException("GIF writer is unavailable.");
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            if (output == null) throw new IOException("Could not open GIF output: " + path);
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            // 52 frames span 0.00-1.70s; the final pose holds for another 0.60s.
            // GIF delays use centiseconds, so distribute rounding across 30fps.
            for (int frame = 0; frame < 52; frame++) {
                if (frame > 0) logic.update(1.0 / 30.0);
                BufferedImage image = capture(panel, CELL_WIDTH, CELL_HEIGHT);
                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(image), writer.getDefaultWriteParam());
                int delay = frame == 51 ? 60
                        : (int) Math.round((frame + 1) * 100.0 / 30.0)
                                - (int) Math.round(frame * 100.0 / 30.0);
                configureGif(metadata, delay, frame == 0);
                writer.writeToSequence(new IIOImage(image, null, metadata), writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static void configureGif(IIOMetadata metadata, int delay, boolean first) throws Exception {
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode control = metadataNode(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", Integer.toString(delay));
        control.setAttribute("transparentColorIndex", "0");
        if (first) {
            IIOMetadataNode extensions = metadataNode(root, "ApplicationExtensions");
            IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
            loop.setAttribute("applicationID", "NETSCAPE");
            loop.setAttribute("authenticationCode", "2.0");
            loop.setUserObject(new byte[] {1, 0, 0});
            extensions.appendChild(loop);
        }
        metadata.setFromTree(format, root);
    }

    private static IIOMetadataNode metadataNode(IIOMetadataNode root, String name) {
        for (int index = 0; index < root.getLength(); index++) {
            if (root.item(index).getNodeName().equals(name)) return (IIOMetadataNode) root.item(index);
        }
        IIOMetadataNode child = new IIOMetadataNode(name);
        root.appendChild(child);
        return child;
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        if (!ImageIO.write(image, "png", path.toFile())) throw new IOException("PNG writer is unavailable: " + path);
    }
}
