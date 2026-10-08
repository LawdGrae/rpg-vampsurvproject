import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.Set;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Headless regression checks for physical key aliases and window focus cleanup. */
public class InputStateSmokeTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel();
            Timer timer = (Timer) field(panel, "frameTimer");
            timer.stop();
            try {
                Object logic = field(panel, "gameLogic");
                Object player = field(logic, "player");
                @SuppressWarnings("unchecked")
                Set<String> pressedKeys = (Set<String>) field(player, "pressedKeys");

                act(panel, "pressed D");
                act(panel, "pressed RIGHT");
                act(panel, "released D");
                require(pressedKeys.contains("right"),
                        "Releasing D must preserve a held right arrow");
                act(panel, "released RIGHT");
                require(!pressedKeys.contains("right"),
                        "Releasing the final right binding must stop movement");

                act(panel, "pressed W");
                act(panel, "pressed LEFT");
                panel.releaseInputState();
                require(pressedKeys.isEmpty(), "Focus loss must clear held movement");
                require(!((Boolean) field(panel, "mouseDown")),
                        "Focus loss must clear mouse input");

                act(panel, "pressed UP");
                act(panel, "released W");
                require(pressedKeys.contains("up"),
                        "A stale release after focus loss must preserve a new arrow press");
                panel.removeNotify();
                require(!timer.isRunning(), "A removed panel must stop its frame timer");
                require(pressedKeys.isEmpty(), "A removed panel must release keyboard input");
            } finally {
                timer.stop();
                panel.releaseInputState();
            }
        });
        System.out.println("InputStateSmokeTest: alias, focus, and timer checks passed");
    }

    private static void act(GamePanel panel, String key) {
        Action action = panel.getActionMap().get(key);
        require(action != null, "Missing binding: " + key);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, key));
    }

    private static Object field(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException exception) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Missing field: " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
