import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class WeaponPoseRenderCheck {
    private static final String[] DIRECTIONS = {"right", "left", "up", "down"};
    private static final String[] NAMES = {"Eumann", "Haze", "Yuexin", "Ziea"};

    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(384, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setColor(new Color(18, 22, 42));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        graphics.setColor(new Color(230, 220, 190));
        for (int characterIndex = 0; characterIndex < NAMES.length; characterIndex++) {
            int rowY = 32 + characterIndex * 74;
            graphics.drawString(NAMES[characterIndex], 8, rowY + 16);
            for (int directionIndex = 0; directionIndex < DIRECTIONS.length; directionIndex++) {
                int centerX = 84 + directionIndex * 74;
                drawPose(graphics, characterIndex, centerX, rowY, DIRECTIONS[directionIndex]);
            }
        }

        graphics.dispose();
        File output = new File(System.getProperty("java.io.tmpdir"), "weapon-pose-check.png");
        ImageIO.write(image, "png", output);
        System.out.println(output.getAbsolutePath());
    }

    private static void drawPose(Graphics2D graphics, int characterIndex,
            int centerX, int centerY, String direction) {
        Player player = createPlayer(characterIndex);
        player.setKeyPressed(direction, true);
        player.update(0.08);
        player.setKeyPressed(direction, false);
        player.draw(graphics, centerX, centerY);
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
