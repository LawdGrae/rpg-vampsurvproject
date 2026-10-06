import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renders held weapons in actual player idle, walking, attack and selection states. */
public class WeaponPoseRenderCheck {
    private static final String[] DIRECTIONS = {"right", "left", "up", "down"};
    private static final String[] NAMES = {"Eumann", "Haze", "Yuexin", "Ziea"};
    private static final String[] POSES = {
            "Idle", "Walk frame 0", "Walk frame 1", "Walk frame 2",
            "Windup 18%", "Hit 48%", "Recovery 88%", "Preview 128px"
    };
    private static final double[] ATTACK_PROGRESS = {0.18, 0.48, 0.88};
    private static final double ATTACK_DURATION = 0.34;
    private static final int ZOOM = 3;
    private static final int POSE_WIDTH = 104;
    private static final int POSE_HEIGHT = 96;
    private static final int LABEL_WIDTH = 72;
    private static final int TITLE_HEIGHT = 36;
    private static final int HEADER_HEIGHT = 28;
    private static final int GAP = 8;
    private static final int TILE_WIDTH = POSE_WIDTH * ZOOM;
    private static final int TILE_HEIGHT = POSE_HEIGHT * ZOOM;
    private static final int PANEL_WIDTH = LABEL_WIDTH + POSES.length * (TILE_WIDTH + GAP);
    private static final int PANEL_HEIGHT = TITLE_HEIGHT + HEADER_HEIGHT
            + DIRECTIONS.length * (TILE_HEIGHT + GAP);
    private static final Color BACKGROUND = new Color(18, 22, 42);
    private static final Color CELL_BACKGROUND = new Color(30, 35, 56);
    private static final Color TEXT = new Color(230, 220, 190);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BufferedImage image = new BufferedImage(PANEL_WIDTH * 2 + GAP,
                PANEL_HEIGHT * 2 + GAP, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        for (int characterIndex = 0; characterIndex < NAMES.length; characterIndex++) {
            int panelX = characterIndex % 2 * (PANEL_WIDTH + GAP);
            int panelY = characterIndex / 2 * (PANEL_HEIGHT + GAP);
            BufferedImage panel = new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D panelGraphics = panel.createGraphics();
            panelGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            panelGraphics.setColor(BACKGROUND);
            panelGraphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            drawCharacterPanel(panelGraphics, characterIndex, 0, 0);
            panelGraphics.dispose();
            graphics.drawImage(panel, panelX, panelY, null);
            writeImage(panel, new File(System.getProperty("java.io.tmpdir"),
                    "weapon-pose-" + NAMES[characterIndex].toLowerCase() + ".png"));
        }
        graphics.dispose();

        File output = args.length > 0 ? new File(args[0])
                : new File(System.getProperty("java.io.tmpdir"), "weapon-pose-check.png");
        writeImage(image, output);
        System.out.println("Rendered 128 gameplay and preview poses: " + output.getAbsolutePath());
    }

    private static void drawCharacterPanel(Graphics2D graphics, int characterIndex,
            int panelX, int panelY) {
        graphics.setColor(TEXT);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        graphics.drawString(NAMES[characterIndex] + " (3x)", panelX + LABEL_WIDTH,
                panelY + 27);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        for (int poseIndex = 0; poseIndex < POSES.length; poseIndex++) {
            int cellX = panelX + LABEL_WIDTH + poseIndex * (TILE_WIDTH + GAP);
            graphics.drawString(POSES[poseIndex], cellX + 8,
                    panelY + TITLE_HEIGHT + 20);
        }

        for (int directionIndex = 0; directionIndex < DIRECTIONS.length; directionIndex++) {
            int cellY = panelY + TITLE_HEIGHT + HEADER_HEIGHT
                    + directionIndex * (TILE_HEIGHT + GAP);
            graphics.setColor(TEXT);
            graphics.drawString(DIRECTIONS[directionIndex], panelX + 8,
                    cellY + TILE_HEIGHT / 2);
            for (int poseIndex = 0; poseIndex < POSES.length; poseIndex++) {
                int cellX = panelX + LABEL_WIDTH + poseIndex * (TILE_WIDTH + GAP);
                if (poseIndex == POSES.length - 1) {
                    graphics.setColor(CELL_BACKGROUND);
                    graphics.fillRect(cellX, cellY, TILE_WIDTH, TILE_HEIGHT);
                    Player player = createPlayer(characterIndex);
                    player.setKeyPressed(DIRECTIONS[directionIndex], true);
                    player.update(0.0);
                    player.setKeyPressed(DIRECTIONS[directionIndex], false);
                    player.drawPreview(graphics, new Rectangle(cellX + TILE_WIDTH / 2 - 64,
                            cellY + TILE_HEIGHT / 2 - 64, 128, 128));
                    continue;
                }
                BufferedImage pose = renderPose(characterIndex, DIRECTIONS[directionIndex],
                        poseIndex);
                graphics.drawImage(pose, cellX, cellY, TILE_WIDTH, TILE_HEIGHT, null);
            }
        }
    }

    private static BufferedImage renderPose(int characterIndex, String direction, int poseIndex) {
        Player player = createPlayer(characterIndex);
        player.setKeyPressed(direction, true);
        player.update(0.0);
        if (poseIndex >= 1 && poseIndex <= 3) {
            // Sample each real walking column after the locomotion ease-in.
            // Gait time is the integral of the blend curve, not raw input time.
            player.update(walkingSampleTime(player.animationSpeed, poseIndex - 1));
        } else {
            player.setKeyPressed(direction, false);
            if (poseIndex >= 4) {
                player.playAttackAnimation(null, ATTACK_DURATION);
                player.update(ATTACK_DURATION * ATTACK_PROGRESS[poseIndex - 4]);
            }
        }

        BufferedImage image = new BufferedImage(POSE_WIDTH, POSE_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(CELL_BACKGROUND);
        graphics.fillRect(0, 0, POSE_WIDTH, POSE_HEIGHT);
        player.draw(graphics, POSE_WIDTH / 2, POSE_HEIGHT / 2 - 4);
        graphics.dispose();
        return image;
    }

    private static double walkingSampleTime(double animationSpeed, int column) {
        double target = (4.01 + column) / animationSpeed;
        double start = 1.0 / animationSpeed;
        double low = 0.0;
        double high = 4.0;
        for (int step = 0; step < 48; step++) {
            double time = (low + high) / 2.0;
            double gait = start + 1.55 * (time - (1.0 - Math.exp(-12.0 * time)) / 12.0);
            if (gait < target) low = time;
            else high = time;
        }
        return (low + high) / 2.0;
    }

    private static void writeImage(BufferedImage image, File output) throws Exception {
        if (!ImageIO.write(image, "png", output)) {
            throw new IllegalStateException("PNG writer unavailable");
        }
        System.out.println(output.getAbsolutePath());
    }

    private static Player createPlayer(int characterIndex) {
        return switch (characterIndex) {
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            default -> new Character_Eumann();
        };
    }
}
