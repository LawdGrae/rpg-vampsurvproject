import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Exports real gameplay with combat progress, an assault warning and a run summary. */
public final class SurvivalRenderCheck {
    public static void main(String[] args) throws Exception {
        File output = new File(args.length > 0 ? args[0] : "out/survival-review");
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GamePanel panel = new GamePanel();
                ((Timer) field(panel, "frameTimer")).stop();
                panel.setSize(1280, 720);
                GameLogic logic = (GameLogic) field(panel, "gameLogic");
                logic.selectCharacter(3);
                logic.startGame();
                logic.setSoundEnabled(false);
                logic.update(RunEntranceAnimation.DURATION);
                @SuppressWarnings("unchecked")
                List<Enemy> enemies = (List<Enemy>) field(logic, "enemies");
                for (int i = 0; i < 8; i++) logic.getSurvivalDirector().registerKill(false);
                logic.getSurvivalDirector().update(25.0, false);
                for (int i = 0; i < 14; i++) {
                    double angle = Math.PI * 2.0 * i / 14;
                    EnemyDefinition definition = EnemyCatalog.normalEnemies(EnemyRegion.RIVENDALE_TOWN)
                            .get(i % EnemyCatalog.normalEnemies(EnemyRegion.RIVENDALE_TOWN).size());
                    enemies.add(new RegionalEnemy(definition, Math.cos(angle) * (155 + i * 9),
                            Math.sin(angle) * (145 + i * 5)));
                }
                for (int i = 0; i < 8; i++) logic.getSurvivalDirector().registerKill(false);
                set(logic, "gameTimer", 25.0);
                set(logic, "whenToSpawn", 26.2);
                set(logic, "level", 8);
                logic.triggerAbility(0);
                for (int i = 0; i < 20; i++) logic.update(0.02);
                capture(panel, output, "survival-warning.png");
                logic.getSurvivalDirector().update(3.0, false);
                Method assault = GameLogic.class.getDeclaredMethod("spawnAssault");
                assault.setAccessible(true); assault.invoke(logic);
                set(logic, "gameTimer", 28.0);
                set(logic, "whenToSpawn", 29.2);
                capture(panel, output, "survival-combat.png");
                ((Player) field(logic, "player")).takeDamage(10000);
                logic.update(0.02);
                capture(panel, output, "survival-summary.png");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("Survival UI previews exported to " + output.getAbsolutePath());
        System.exit(0);
    }

    private static void capture(GamePanel panel, File output, String name) throws Exception {
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", new File(output, name));
    }
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
}
