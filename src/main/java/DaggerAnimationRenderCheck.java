import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Shows actual held-dagger animation in every facing direction. */
public class DaggerAnimationRenderCheck {
    private static final String[] DIRECTIONS = {"down", "right", "up", "left"};
    private static final int[][] VECTORS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private static final int CELL_WIDTH = 288;
    private static final int HEIGHT = 340;
    private static final Color BACKGROUND = new Color(15, 19, 35);
    private static final Color PANEL = new Color(37, 28, 57);
    private static final Color TEXT = new Color(231, 225, 202);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File directory = new File(args.length > 0 ? args[0] : "out");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create " + directory);
        }
        Player[] players = new Player[4];
        for (int index = 0; index < players.length; index++) {
            players[index] = new Character_Haze();
            players[index].faceToward(VECTORS[index][0] * 100, VECTORS[index][1] * 100);
            players[index].update(0.6);
        }
        AbilityDefinition twinFang = ability("twin_fang");
        AbilityDefinition execution = ability("silent_execution");
        AutoFireWeapon weapon = new AutoFireWeapon();
        weapon.setAttackSprite(null, "daggers");
        int width = CELL_WIDTH * players.length;
        BufferedImage sheet = new BufferedImage(width, HEIGHT * 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D sheetGraphics = sheet.createGraphics();
        int sheetRow = 0;
        File animation = new File(directory, "dagger-animation.gif");
        File frames = new File(directory, "dagger-poses-fixed.png");
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(animation)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < 144; frame++) {
                String state = frame < 20 ? "Upright ready stance" : frame < 42 ? "Walking"
                        : frame < 52 ? "Settle" : frame < 63 ? "Automatic swing"
                        : frame < 72 ? "Recovery" : frame < 93 ? "Twin Fang combo"
                        : frame < 104 ? "Recovery" : frame < 127 ? "Silent Execution combo" : "Recovery";
                BufferedImage image = new BufferedImage(width, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(BACKGROUND);
                graphics.fillRect(0, 0, width, HEIGHT);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
                graphics.setColor(TEXT);
                graphics.drawString("Haze · " + state, 16, 28);
                for (int index = 0; index < players.length; index++) {
                    Player player = players[index];
                    if (frame == 20) player.setKeyPressed(DIRECTIONS[index], true);
                    if (frame == 42) player.setKeyPressed(DIRECTIONS[index], false);
                    if (frame == 52) player.playAttackAnimation(null, weapon.getSwingDuration());
                    if (frame == 72) player.playAttackAnimation(twinFang, AbilityAnimationTiming.duration(twinFang));
                    if (frame == 104) player.playAttackAnimation(execution, AbilityAnimationTiming.duration(execution));
                    player.update(1.0 / 24.0);
                    int left = index * CELL_WIDTH;
                    graphics.setColor(PANEL);
                    graphics.fillRoundRect(left + 6, 41, CELL_WIDTH - 12, 263, 18, 18);
                    graphics.setColor(TEXT);
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    graphics.drawString(DIRECTIONS[index], left + 18, 67);
                    Graphics2D scene = (Graphics2D) graphics.create();
                    scene.clipRect(left + 8, 74, CELL_WIDTH - 16, 226);
                    scene.translate(left + CELL_WIDTH / 2.0, 181);
                    scene.scale(2.5, 2.5);
                    player.draw(scene, 0, 0);
                    scene.dispose();
                }
                graphics.setColor(TEXT);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                graphics.drawString("Gameplay renderer · 24 frames/sec · handles stay attached while blades swing", 16, HEIGHT - 13);
                graphics.dispose();
                writer.writeToSequence(new IIOImage(image, null,
                        CharacterAnimationRenderCheck.gifMetadata(writer, image, frame)), null);
                if (List.of(0, 28, 55, 78, 82, 113).contains(frame)) {
                    sheetGraphics.drawImage(image, 0, sheetRow++ * HEIGHT, null);
                }
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
            sheetGraphics.dispose();
        }
        ImageIO.write(sheet, "png", frames);
        System.out.println(animation.getAbsolutePath());
        System.out.println(frames.getAbsolutePath());
    }

    private static AbilityDefinition ability(String id) {
        return AbilityDefinition.createAll().stream().filter(definition -> definition.getId().equals(id))
                .findFirst().orElseThrow();
    }
}
