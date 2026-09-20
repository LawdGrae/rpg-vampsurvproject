import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final boolean DEBUG_ENABLED = false;
    private static final int EXP_BAR_HEIGHT = 12;
    private static final int EXP_BAR_Y = 18;

    private static final Font TIMES_NEW_ROMAN = new Font("Times New Roman", Font.BOLD, 18);

    private final BufferedImage grassTile;
    private final BufferedImage landscape;
    private final GameLogic gameLogic;
    private long lastUpdateNanos = System.nanoTime();
    private double lastDeltaTime;
    private double framesPerSecond;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        grassTile = loadGrassTile();
        landscape = loadLandscape();
        gameLogic = new GameLogic();
        installKeyBindings();
        installUpgradeClickHandling();

        Timer timer = new Timer(16, event -> {
            // Measure real elapsed time so movement is independent of frame rate.
            long currentTimeNanos = System.nanoTime();
            double deltaTime = (currentTimeNanos - lastUpdateNanos) / 1_000_000_000.0;
            lastUpdateNanos = currentTimeNanos;
            deltaTime = Math.min(deltaTime, 0.1);
            lastDeltaTime = deltaTime;
            double instantFramesPerSecond = deltaTime > 0 ? 1.0 / deltaTime : 0;
            framesPerSecond = framesPerSecond * 0.9 + instantFramesPerSecond * 0.1;

            if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen() && !gameLogic.isPaused()) {
                gameLogic.update(deltaTime);
            }
            repaint();
        });
        timer.start();
    }

    private BufferedImage loadGrassTile() {
        try {
            return ImageIO.read(GamePanel.class.getResource("/assets/grasstile.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load assets/grasstile.png", exception);
        }
    }

    private BufferedImage loadLandscape() {
        try {
            return ImageIO.read(GamePanel.class.getResource("/assets/landscape.png"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load assets/landscape.png", exception);
        }
    }

    private void installKeyBindings() {
        // InputMap finds an action; ActionMap runs that action.
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, "pressed W", "up", true);
        bindKey(inputMap, actionMap, "released W", "up", false);
        bindKey(inputMap, actionMap, "pressed UP", "up", true);
        bindKey(inputMap, actionMap, "released UP", "up", false);
        bindKey(inputMap, actionMap, "pressed S", "down", true);
        bindKey(inputMap, actionMap, "released S", "down", false);
        bindKey(inputMap, actionMap, "pressed DOWN", "down", true);
        bindKey(inputMap, actionMap, "released DOWN", "down", false);
        bindKey(inputMap, actionMap, "pressed A", "left", true);
        bindKey(inputMap, actionMap, "released A", "left", false);
        bindKey(inputMap, actionMap, "pressed LEFT", "left", true);
        bindKey(inputMap, actionMap, "released LEFT", "left", false);
        bindKey(inputMap, actionMap, "pressed D", "right", true);
        bindKey(inputMap, actionMap, "released D", "right", false);
        bindKey(inputMap, actionMap, "pressed RIGHT", "right", true);
        bindKey(inputMap, actionMap, "released RIGHT", "right", false);

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "togglePause");
        actionMap.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!gameLogic.isUpgradeMenuOpen()) {
                    if (gameLogic.isPaused()) {
                        gameLogic.resume();
                    } else {
                        gameLogic.togglePause();
                    }
                }
            }
        });
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, String keyStroke,
            String direction, boolean pressed) {
        String actionName = keyStroke;
        inputMap.put(KeyStroke.getKeyStroke(keyStroke), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // Store the key state in the game logic until the key is released.
                gameLogic.setKeyPressed(direction, pressed);
            }
        });
    }

    private void installUpgradeClickHandling() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (gameLogic.isMainMenuOpen()) {
                    handleMainMenuClick(event);
                    return;
                }

                if (gameLogic.isCharacterSelectOpen()) {
                    handleCharacterSelectionClick(event);
                    return;
                }

                if (gameLogic.isUpgradeMenuOpen()) {
                    int left = (PANEL_WIDTH - 440) / 2;
                    int top = 150;
                    int cardWidth = 120;
                    int gap = 20;
                    int cardHeight = 100;

                    for (int index = 0; index < 3; index++) {
                        int x = left + index * (cardWidth + gap);
                        int y = top;
                        if (event.getX() >= x && event.getX() <= x + cardWidth
                                && event.getY() >= y && event.getY() <= y + cardHeight) {
                            gameLogic.chooseUpgrade(index);
                            return;
                        }
                    }
                    return;
                }

                if (gameLogic.isPaused()) {
                    handlePauseMenuClick(event);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics;

        if (gameLogic.isMainMenuOpen()) {
            drawMainMenu(graphics2D);
            return;
        }

        if (gameLogic.isCharacterSelectOpen()) {
            drawCharacterSelection(graphics2D);
            return;
        }

        int tileWidth = grassTile.getWidth();
        int tileHeight = grassTile.getHeight();
        int startX = Math.floorMod((int) gameLogic.getWorldOffsetX(), tileWidth) - tileWidth;
        int startY = Math.floorMod((int) gameLogic.getWorldOffsetY(), tileHeight) - tileHeight;

        for (int tileX = startX; tileX < PANEL_WIDTH; tileX += tileWidth) {
            for (int tileY = startY; tileY < PANEL_HEIGHT; tileY += tileHeight) {
                graphics2D.drawImage(grassTile, tileX, tileY, null);
            }
        }

        gameLogic.drawEntities(graphics2D, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        drawExperienceBar(graphics2D);
        drawGameTimer(graphics2D);
        drawUpgradeMenu(graphics2D);
        drawPauseMenu(graphics2D);
        drawDebugInfo(graphics2D);
    }

    private void drawMainMenu(Graphics2D graphics) {
        graphics.drawImage(landscape, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);

        graphics.setColor(new Color(0, 0, 0, 170));
        graphics.fillRoundRect(120, 90, 560, 420, 24, 24);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 52));
        graphics.drawString("RPG", 355, 180);

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        graphics.drawString("Placeholder adventure", 285, 220);

        drawMenuButton(graphics, new Rectangle(290, 270, 220, 52), "Play");
        drawMenuButton(graphics, new Rectangle(290, 340, 220, 52), "Settings");
        drawMenuButton(graphics, new Rectangle(290, 410, 220, 52), "Quit");
    }

    private void drawCharacterSelection(Graphics2D graphics) {
        graphics.drawImage(landscape, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);

        graphics.setColor(new Color(0, 0, 0, 170));
        graphics.fillRoundRect(70, 60, 660, 500, 24, 24);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 34));
        graphics.drawString("Choose your hero", 270, 110);

        List<String> names = gameLogic.getCharacterNames();
        List<String> weapons = gameLogic.getCharacterWeapons();
        int cardWidth = 120;
        int gap = 18;
        int totalCardWidth = 5 * cardWidth + 4 * gap;
        int startX = (PANEL_WIDTH - totalCardWidth) / 2;
        int startY = 150;

        for (int index = 0; index < 5; index++) {
            int x = startX + index * (cardWidth + gap);
            int y = startY;
            boolean selectable = index == 0;

            graphics.setColor(selectable ? new Color(80, 120, 180) : new Color(50, 50, 50));
            graphics.fillRoundRect(x, y, cardWidth, 230, 18, 18);

            try {
                String portraitPath = index == 0 ? gameLogic.getPortraitPath() : "/assets/portrait_coming_soon.png";
                BufferedImage portrait = ImageIO.read(GamePanel.class.getResource(portraitPath));
                graphics.drawImage(portrait, x + 20, y + 22, 80, 90, null);
            } catch (IOException | IllegalArgumentException ignored) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(x + 20, y + 22, 80, 90);
            }

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 15));
            String name = names.get(index);
            graphics.drawString(name, x + 10, y + 140);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            graphics.drawString(weapons.get(index), x + 10, y + 164);

            if (!selectable) {
                graphics.setColor(new Color(255, 220, 120));
                graphics.setFont(new Font("Times New Roman", Font.ITALIC, 12));
                graphics.drawString("coming soon...", x + 18, y + 188);
            }

            if (selectable) {
                graphics.setColor(new Color(140, 235, 160));
                graphics.setFont(new Font("Times New Roman", Font.BOLD, 14));
                graphics.drawString("Selected", x + 22, y + 212);
            }
        }

        drawMenuButton(graphics, new Rectangle(300, 450, 200, 48), "Start");
    }

    private void drawExperienceBar(Graphics2D graphics) {
        double progress = gameLogic.getExpProgress();
        int barWidth = PANEL_WIDTH;
        int barX = 0;
        int barY = EXP_BAR_Y;
        int filledWidth = (int) Math.round(barWidth * progress);

        graphics.setColor(new Color(51, 204, 255));
        graphics.fillRect(barX, barY, filledWidth, EXP_BAR_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 12));
        graphics.drawString("LVL " + gameLogic.getLevel(), 10, 12);
    }

    private void drawGameTimer(Graphics2D graphics) {
        int totalSeconds = (int) gameLogic.getGameTimer();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timerText = String.format("%02d:%02d", minutes, seconds);
        int textWidth = graphics.getFontMetrics().stringWidth(timerText);

        graphics.setColor(Color.WHITE);
        graphics.drawString(timerText, (PANEL_WIDTH - textWidth) / 2, 24);
    }

    private void drawUpgradeMenu(Graphics2D graphics) {
        if (!gameLogic.isUpgradeMenuOpen()) {
            return;
        }

        int left = (PANEL_WIDTH - 440) / 2;
        int top = 150;
        int cardWidth = 120;
        int gap = 20;
        int cardHeight = 100;

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.setColor(new Color(25, 25, 25, 220));
        graphics.fillRoundRect(left - 10, top - 10, 3 * cardWidth + 2 * gap + 20, cardHeight + 20, 16, 16);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 20));
        graphics.drawString("Choose an upgrade", left + 10, top - 20);

        for (int index = 0; index < 3; index++) {
            int x = left + index * (cardWidth + gap);
            int y = top;
            graphics.setColor(new Color(60, 60, 60));
            graphics.fillRoundRect(x, y, cardWidth, cardHeight, 12, 12);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 16));
            graphics.drawString(gameLogic.getUpgradeChoices().get(index), x + 12, y + 28);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            graphics.drawString("(placeholder)", x + 12, y + 48);
            graphics.drawString("+ small bonus", x + 12, y + 70);
        }
    }

    private void drawPauseMenu(Graphics2D graphics) {
        if (!gameLogic.isPaused()) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        int menuWidth = 340;
        int menuHeight = 260;
        int x = (PANEL_WIDTH - menuWidth) / 2;
        int y = 140;

        graphics.setColor(new Color(26, 26, 26, 220));
        graphics.fillRoundRect(x, y, menuWidth, menuHeight, 18, 18);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 36));
        String title = gameLogic.isSettingsOpen() ? "Settings" : "Paused";
        graphics.drawString(title, x + 110, y + 52);

        if (gameLogic.isSettingsOpen()) {
            drawSettingsMenu(graphics, x, y);
            return;
        }

        drawMenuButton(graphics, new Rectangle(x + 75, y + 90, 190, 40), "Resume");
        drawMenuButton(graphics, new Rectangle(x + 75, y + 145, 190, 40), "Settings");
        drawMenuButton(graphics, new Rectangle(x + 75, y + 200, 190, 40), "Main Menu");
    }

    private void drawSettingsMenu(Graphics2D graphics, int x, int y) {
        int left = x + 52;
        int width = 236;
        int rowHeight = 42;
        int start = y + 90;

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        graphics.drawString("Show FPS", left, start + 22);
        graphics.drawString("Sound", left, start + 22 + rowHeight);
        graphics.drawString("Back", left, start + 22 + rowHeight * 2);

        drawToggleButton(graphics, left + 180, start - 10, 36, 24, gameLogic.isDebugInfoVisible());
        drawToggleButton(graphics, left + 180, start - 10 + rowHeight, 36, 24, gameLogic.isSoundEnabled());
        drawMenuButton(graphics, new Rectangle(left + 160, start + rowHeight * 2 - 18, 90, 32), "Back");
    }

    private void drawMenuButton(Graphics2D graphics, Rectangle bounds, String text) {
        graphics.setColor(new Color(80, 80, 80));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 20));
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, bounds.x + (bounds.width - textWidth) / 2,
                bounds.y + 25);
    }

    private void drawToggleButton(Graphics2D graphics, int x, int y, int width, int height, boolean enabled) {
        graphics.setColor(enabled ? new Color(90, 180, 90) : new Color(120, 120, 120));
        graphics.fillRoundRect(x, y, width, height, 12, 12);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(enabled ? x + width - 14 : x + 2, y + 2, 10, 10);
    }

    private void handlePauseMenuClick(MouseEvent event) {
        if (gameLogic.isSettingsOpen()) {
            int x = (PANEL_WIDTH - 340) / 2;
            int y = 140;
            if (event.getX() >= x + 75 && event.getX() <= x + 315 && event.getY() >= y + 210 && event.getY() <= y + 245) {
                gameLogic.toggleSettings();
                return;
            }

            int left = x + 52;
            int start = y + 90;
            Rectangle fpsToggle = new Rectangle(left + 180, start - 10, 36, 24);
            Rectangle soundToggle = new Rectangle(left + 180, start - 10 + 42, 36, 24);
            if (contains(event, fpsToggle)) {
                gameLogic.setDebugInfoVisible(!gameLogic.isDebugInfoVisible());
                return;
            }
            if (contains(event, soundToggle)) {
                gameLogic.setSoundEnabled(!gameLogic.isSoundEnabled());
                return;
            }
            return;
        }

        int x = (PANEL_WIDTH - 340) / 2;
        int y = 140;
        Rectangle resumeButton = new Rectangle(x + 75, y + 90, 190, 40);
        Rectangle settingsButton = new Rectangle(x + 75, y + 145, 190, 40);
        Rectangle mainMenuButton = new Rectangle(x + 75, y + 200, 190, 40);

        if (contains(event, resumeButton)) {
            gameLogic.resume();
            return;
        }
        if (contains(event, settingsButton)) {
            gameLogic.toggleSettings();
            return;
        }
        if (contains(event, mainMenuButton)) {
            gameLogic.showMainMenu();
        }
    }

    private void handleMainMenuClick(MouseEvent event) {
        Rectangle playButton = new Rectangle(290, 270, 220, 52);
        Rectangle settingsButton = new Rectangle(290, 340, 220, 52);
        Rectangle quitButton = new Rectangle(290, 410, 220, 52);

        if (contains(event, playButton)) {
            gameLogic.showCharacterSelection();
            return;
        }
        if (contains(event, settingsButton)) {
            gameLogic.toggleSettings();
            return;
        }
        if (contains(event, quitButton)) {
            System.exit(0);
        }
    }

    private void handleCharacterSelectionClick(MouseEvent event) {
        int cardWidth = 120;
        int gap = 18;
        int totalCardWidth = 5 * cardWidth + 4 * gap;
        int startX = (PANEL_WIDTH - totalCardWidth) / 2;
        int startY = 150;

        for (int index = 0; index < 5; index++) {
            int x = startX + index * (cardWidth + gap);
            Rectangle hitBox = new Rectangle(x, startY, cardWidth, 230);
            if (contains(event, hitBox) && index == 0) {
                gameLogic.startGame();
                return;
            }
        }

        Rectangle startButton = new Rectangle(300, 450, 200, 48);
        if (contains(event, startButton)) {
            gameLogic.startGame();
        }
    }

    private boolean contains(MouseEvent event, Rectangle rectangle) {
        return event.getX() >= rectangle.x && event.getX() <= rectangle.x + rectangle.width
                && event.getY() >= rectangle.y && event.getY() <= rectangle.y + rectangle.height;
    }

    private void drawDebugInfo(Graphics2D graphics) {
        if (!DEBUG_ENABLED || !gameLogic.isDebugInfoVisible()) {
            return;
        }

        gameLogic.drawCollisionAreas(graphics, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);

        // Add future debug values in this method so they stay together.
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        graphics.drawString(String.format("FPS: %.1f", framesPerSecond), 10, 20);
        graphics.drawString(String.format("Delta time: %.4f s", lastDeltaTime), 10, 38);
        graphics.drawString(String.format("World X: %.1f", gameLogic.getPlayerWorldX()), 10, 56);
        graphics.drawString(String.format("World Y: %.1f", gameLogic.getPlayerWorldY()), 10, 74);
        graphics.drawString("Enemies: " + gameLogic.getEnemyCount(), 10, 92);
    }
}