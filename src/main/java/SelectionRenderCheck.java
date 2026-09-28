import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;

public class SelectionRenderCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        GamePanel panel = new GamePanel();
        panel.setSize(1280, 720);
        Field logicField = GamePanel.class.getDeclaredField("gameLogic");
        logicField.setAccessible(true);
        GameLogic logic = (GameLogic) logicField.get(panel);
        logic.showCharacterSelection();

        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        File output = new File(System.getProperty("java.io.tmpdir"), "selection-icons-fixed.png");
        ImageIO.write(image, "png", output);
        System.out.println(output.getAbsolutePath());
        System.exit(0);
    }
}
