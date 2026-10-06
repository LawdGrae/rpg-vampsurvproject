import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Renders real screens and exercises their visible controls through Swing mouse events. */
public class UiRenderCheck {
    private static File outputDirectory = new File("out");

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length > 0) outputDirectory = new File(args[0]);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create render directory: " + outputDirectory);
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                for (int[] size : new int[][]{{1280, 720}, {1024, 640}, {1920, 1080}, {1400, 700}}) {
                    verifyNavigation(size[0], size[1]);
                }
                renderScreens();
            } catch (Exception exception) { throw new RuntimeException(exception); }
        });
        System.out.println("UI navigation, loadout, settings, upgrades, restart and scaled hit regions passed at four sizes");
        System.exit(0);
    }

    private static GamePanel panel(int width, int height) throws Exception {
        GamePanel panel = new GamePanel();
        ((Timer) field(panel, "frameTimer")).stop();
        panel.setSize(width, height);
        logic(panel).setSoundEnabled(false);
        return panel;
    }

    private static void verifyNavigation(int width, int height) throws Exception {
        GamePanel panel = panel(width, height);
        GameLogic logic = logic(panel);
        GameMenus menus = (GameMenus) field(panel, "menus");
        render(panel);
        for (int index = 0; index < 3; index++) {
            Rectangle bounds = menus.mainButton(index);
            require(new Rectangle(0, 0, 1280, 720).contains(bounds), "Main menu buttons must fit on the canvas");
            require(menus.interactive((int) bounds.getCenterX(), (int) bounds.getCenterY()),
                    "Every main menu button must have a matching interactive region");
            for (int other = 0; other < index; other++) {
                require(!bounds.intersects(menus.mainButton(other)), "Main menu button regions must not overlap");
            }
        }
        click(panel, menus.mainButton(1));
        require(logic.isSettingsOpen(), "Settings must open from the visible main menu button");
        click(panel, menus.settingsToggle(0));
        require(logic.isDebugInfoVisible(), "Frame-counter toggle must respond at scaled coordinates");
        click(panel, menus.fpsButton(true));
        require(((Number) field(panel, "selectedFpsIndex")).intValue() == 3, "FPS increase button must change target");
        click(panel, menus.fpsButton(false));
        require(((Number) field(panel, "selectedFpsIndex")).intValue() == 2, "FPS decrease button must change target");
        click(panel, menus.settingsToggle(1));
        require(logic.isSoundEnabled(), "Sound toggle must respond");
        logic.setSoundEnabled(false);
        click(panel, menus.settingsBack());
        require(!logic.isSettingsOpen(), "Settings back must close the settings modal");
        click(panel, menus.mainButton(0));
        require(logic.isCharacterSelectOpen(), "Begin adventure must open the hero roster");
        for (int index = 0; index < logic.getCharacterCount(); index++) {
            Rectangle bounds = menus.heroBounds(index);
            require(new Rectangle(0, 0, 1280, 720).contains(bounds), "Every hero card must fit on the canvas");
            if (index > 0) require(!bounds.intersects(menus.heroBounds(index - 1)), "Hero cards must not overlap");
            require(menus.interactive((int) bounds.getCenterX(), (int) bounds.getCenterY()),
                    "Every displayed hero must have an interactive hit region");
            click(panel, bounds);
            menus.update(0.4);
            require(logic.getSelectedCharacterIndex() == index, "Every displayed hero must be selectable");
            render(panel);
        }
        int selected = logic.getSelectedCharacterIndex();
        require("Sir Rakki".equals(logic.getSelectedCharacterName()), "Sir Rakki must be selectable from the roster");
        click(panel, menus.backBounds());
        require(logic.isMainMenuOpen(), "Roster back must return to main menu");
        click(panel, menus.mainButton(0));
        click(panel, menus.startBounds());
        require(logic.isGameStarted() && logic.getSelectedCharacterIndex() == selected,
                "Start button must deploy the selected hero");
        panel.getActionMap().get("toggleSkillMenu").actionPerformed(null);
        require(logic.isSkillMenuOpen(), "K must open skills");
        render(panel);
        click(panel, menus.equipBounds(2));
        require(logic.getAbilityManager().getSelectedEquipSlot() == 2, "Loadout slot must be selected");
        String first = logic.getCharacterActiveSkillIds(selected).get(0);
        List<RpgAbility> classSkills = logic.getAbilityManager().getAbilities(logic.getCharacterAbilityClass(selected));
        int ownSkill = 0;
        while (!classSkills.get(ownSkill).getDefinition().getId().equals(first)) ownSkill++;
        click(panel, menus.skillBounds(ownSkill));
        require(logic.getAbilityManager().getEquippedAbilities()[2].getDefinition().getId().equals(first),
                "Hero skill must equip into the selected visible slot");
        click(panel, menus.classBounds(AbilityClass.BLACK_KNIGHT.ordinal()));
        click(panel, menus.skillBounds(0));
        require(logic.getAbilityManager().getEquippedAbilities()[2].getDefinition().getId().equals(first),
                "Previewing another class must preserve hero loadout restrictions");
        click(panel, menus.skillCloseBounds());
        require(!logic.isSkillMenuOpen(), "Skill close button must dismiss the library");
        double mana = logic.getAbilityManager().getMana();
        click(panel, GameHud.abilityBounds(2, 1280, 720));
        require(logic.getAbilityManager().getMana() < mana, "Visible HUD slot must cast its equipped ability");
        panel.getActionMap().get("togglePause").actionPerformed(null);
        require(logic.isPaused(), "Escape must pause gameplay");
        click(panel, menus.pauseButton(1));
        require(logic.isSettingsOpen(), "Settings must open from pause");
        click(panel, menus.settingsBack());
        click(panel, menus.pauseButton(0));
        require(!logic.isPaused(), "Visible resume button must resume gameplay");
        openUpgrades(logic);
        render(panel);
        click(panel, menus.upgradeBounds(0));
        require(!logic.isUpgradeMenuOpen(), "Visible upgrade card must apply and dismiss the choice");
        set(logic, "gameOver", true);
        render(panel);
        click(panel, menus.gameOverButton(0));
        require(!logic.isGameOver() && logic.isGameStarted(), "Try again must reset the run");
        set(logic, "gameOver", true);
        click(panel, menus.gameOverButton(1));
        require(logic.isMainMenuOpen(), "Game-over main menu button must work");
        double worldX = logic.getPlayerWorldX();
        double clock = logic.getGameTimer();
        render(panel); render(panel);
        require(worldX == logic.getPlayerWorldX() && clock == logic.getGameTimer(), "Painting UI must not advance gameplay");
    }

    private static void renderScreens() throws Exception {
        GamePanel panel = panel(1280, 720);
        GameLogic logic = logic(panel);
        GameMenus menus = (GameMenus) field(panel, "menus");
        capture(panel, "main-menu");
        panel.setSize(1024, 640); capture(panel, "main-small");
        panel.setSize(1920, 1080); capture(panel, "main-large");
        panel.setSize(1280, 720);
        Rectangle start = menus.mainButton(0);
        menus.pointer((int) start.getCenterX(), (int) start.getCenterY(), false);
        menus.update(0.4);
        capture(panel, "main-hover");
        menus.pointer((int) start.getCenterX(), (int) start.getCenterY(), true);
        capture(panel, "main-pressed");
        menus.pointer(-1, -1, false);
        menus.update(0.4);
        logic.toggleSettings(); capture(panel, "main-settings");
        logic.toggleSettings();
        logic.showCharacterSelection();
        String[] names = {"eumann", "haze", "yuexin", "ziea", "rakki"};
        for (int index = 0; index < logic.getCharacterCount(); index++) {
            logic.selectCharacter(index);
            menus.update(0.7);
            capture(panel, "hero-" + names[index]);
        }
        panel.setSize(1024, 640); capture(panel, "hero-small");
        panel.setSize(1920, 1080); capture(panel, "hero-large");
        panel.setSize(1280, 720);
        logic.selectCharacter(1); logic.startGame();
        for (int frame = 0; frame < 110; frame++) logic.update(1.0 / 60.0);
        logic.triggerAbility(1);
        logic.update(0.25);
        capture(panel, "gameplay");
        menus.toggleSkills();
        capture(panel, "skills");
        for (AbilityClass type : AbilityClass.values()) {
            click(panel, menus.classBounds(type.ordinal()));
            render(panel);
        }
        logic.toggleSkillMenu(); logic.togglePause();
        capture(panel, "pause");
        logic.toggleSettings(); capture(panel, "settings");
        logic.resume(); openUpgrades(logic); capture(panel, "upgrades");
        set(logic, "upgradeMenuOpen", false); set(logic, "gameOver", true);
        set(logic, "gameTimer", 87.0); set(logic, "level", 4);
        capture(panel, "game-over");
        createContactSheet();
    }

    @SuppressWarnings("unchecked")
    private static void openUpgrades(GameLogic logic) throws Exception {
        List<String> choices = (List<String>) field(logic, "upgradeChoices");
        choices.clear(); choices.addAll(List.of("Vitality", "Swiftness", "Heavy Blows"));
        set(logic, "upgradeMenuOpen", true);
        set(logic, "level", 3);
    }

    private static void click(GamePanel panel, Rectangle bounds) {
        double scale = Math.min(panel.getWidth() / 1280.0, panel.getHeight() / 720.0);
        int x = (int) Math.round((panel.getWidth() - 1280 * scale) / 2 + bounds.getCenterX() * scale);
        int y = (int) Math.round((panel.getHeight() - 720 * scale) / 2 + bounds.getCenterY() * scale);
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static BufferedImage render(GamePanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); panel.paint(g); g.dispose(); return image;
    }

    private static void capture(GamePanel panel, String name) throws Exception {
        File file = new File(outputDirectory, "ui-" + name + ".png");
        ImageIO.write(render(panel), "png", file);
        System.out.println(file.getAbsolutePath());
    }

    private static void createContactSheet() throws Exception {
        String[] names = {"main-menu", "hero-haze", "gameplay", "skills", "settings", "upgrades"};
        BufferedImage image = new BufferedImage(1280, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (int index = 0; index < names.length; index++) {
            BufferedImage screen = ImageIO.read(new File(outputDirectory, "ui-" + names[index] + ".png"));
            g.drawImage(screen, index % 2 * 640, index / 2 * 360, 640, 360, null);
        }
        g.dispose(); ImageIO.write(image, "png", new File(outputDirectory, "ui-overview.png"));
    }

    private static GameLogic logic(GamePanel panel) throws Exception { return (GameLogic) field(panel, "gameLogic"); }
    private static Object field(Object instance, String name) throws Exception {
        Field field = instance.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(instance);
    }
    private static void set(Object instance, String name, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
