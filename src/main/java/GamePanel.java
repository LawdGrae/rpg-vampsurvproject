import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int EXP_BAR_HEIGHT = 12;
    private static final int EXP_BAR_Y = 18;
    private static final int[] FPS_OPTIONS = {30, 45, 60, 90, 120};

    private static final Font TIMES_NEW_ROMAN = new Font("Times New Roman", Font.BOLD, 18);

    private final BufferedImage grassTile;
    private final BufferedImage landscape;
    private final GameLogic gameLogic;
    private final Timer frameTimer;
    private long lastUpdateNanos = System.nanoTime();
    private double lastDeltaTime;
    private double framesPerSecond;
    private int selectedFpsIndex = 2;
    private int mouseX = -1;
    private int mouseY = -1;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        grassTile = loadGrassTile();
        landscape = loadLandscape();
        gameLogic = new GameLogic();
        installKeyBindings();
        installUpgradeClickHandling();

        frameTimer = new Timer(getFrameDelayMillis(), event -> {
            // Measure real elapsed time so movement is independent of frame rate.
            long currentTimeNanos = System.nanoTime();
            double deltaTime = (currentTimeNanos - lastUpdateNanos) / 1_000_000_000.0;
            lastUpdateNanos = currentTimeNanos;
            deltaTime = Math.min(deltaTime, 0.1);
            lastDeltaTime = deltaTime;
            double instantFramesPerSecond = deltaTime > 0 ? 1.0 / deltaTime : 0;
            framesPerSecond = framesPerSecond * 0.9 + instantFramesPerSecond * 0.1;

            if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen()
                    && !gameLogic.isPaused() && !gameLogic.isSkillMenuOpen()) {
                gameLogic.update(deltaTime);
            }
            repaint();
        });
        frameTimer.start();
    }

    private BufferedImage loadGrassTile() {
        return ResourceLoader.loadImage("/main/resources/grasstile.png");
    }

    private BufferedImage loadLandscape() {
        return ResourceLoader.loadImage("/main/resources/landscape.png");
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
        inputMap.put(KeyStroke.getKeyStroke("pressed SPACE"), "spacePressed");
        actionMap.put("spacePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                gameLogic.setKeyPressed("jump", true);
                if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen() && !gameLogic.isPaused()) {
                    gameLogic.triggerAbility();
                }
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("released SPACE"), "spaceReleased");
        actionMap.put("spaceReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                gameLogic.setKeyPressed("jump", false);
            }
        });
        bindKey(inputMap, actionMap, "pressed R", "run", true);
        bindKey(inputMap, actionMap, "released R", "run", false);
        bindKey(inputMap, actionMap, "pressed J", "attack", true);
        bindKey(inputMap, actionMap, "released J", "attack", false);// Haze add

        for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
            final int slotIndex = index;
            inputMap.put(KeyStroke.getKeyStroke("pressed " + (index + 1)), "ability" + index);
            actionMap.put("ability" + index, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    gameLogic.triggerAbility(slotIndex);
                }
            });
        }

        inputMap.put(KeyStroke.getKeyStroke("pressed K"), "toggleSkillMenu");
        actionMap.put("toggleSkillMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                gameLogic.toggleSkillMenu();
            }
        });

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

                if (gameLogic.isSkillMenuOpen()) {
                    handleSkillMenuClick(event);
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

                if (gameLogic.isGameOver()) {
                    handleGameOverClick(event);
                    return;
                }

                if (gameLogic.isPaused()) {
                    handlePauseMenuClick(event);
                    return;
                }

                if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen()) {
                    for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
                        if (contains(event, getHotbarSlotBounds(index))) {
                            gameLogic.triggerAbility(index);
                            return;
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                mouseX = event.getX();
                mouseY = event.getY();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                mouseX = event.getX();
                mouseY = event.getY();
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

        Graphics2D worldGraphics = (Graphics2D) graphics2D.create();
        worldGraphics.translate(gameLogic.getScreenShakeOffsetX(), gameLogic.getScreenShakeOffsetY());

        int tileWidth = grassTile.getWidth();
        int tileHeight = grassTile.getHeight();
        int startX = Math.floorMod((int) gameLogic.getWorldOffsetX(), tileWidth) - tileWidth;
        int startY = Math.floorMod((int) gameLogic.getWorldOffsetY(), tileHeight) - tileHeight;

        for (int tileX = startX; tileX < PANEL_WIDTH; tileX += tileWidth) {
            for (int tileY = startY; tileY < PANEL_HEIGHT; tileY += tileHeight) {
                worldGraphics.drawImage(grassTile, tileX, tileY, null);
            }
        }

        gameLogic.drawEntities(worldGraphics, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        gameLogic.drawAbilityBursts(worldGraphics, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        worldGraphics.dispose();
        drawExperienceBar(graphics2D);
        drawGameTimer(graphics2D);
        drawAbilityHud(graphics2D);
        if (gameLogic.isGameOver()) {
            gameLogic.drawGameOverEffect(graphics2D, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
            drawGameOverScreen(graphics2D);
            return;
        }
        drawUpgradeMenu(graphics2D);
        drawPauseMenu(graphics2D);
        drawSkillMenu(graphics2D);
        drawAbilityTooltip(graphics2D);
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

        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 16));
        graphics.drawString("V0.0.1", PANEL_WIDTH - 80, PANEL_HEIGHT - 22);
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
                String portraitPath = index == 0 ? gameLogic.getPortraitPath() : "/main/resources/portrait_coming_soon.png";
                BufferedImage portrait = ResourceLoader.loadImage(portraitPath);
                graphics.drawImage(portrait, x + 20, y + 22, 80, 90, null);
            } catch (IllegalStateException ignored) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(x + 20, y + 22, 80, 90);
            }

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 15));
            String name = names.get(index);
            if (selectable) {
                graphics.drawString(name, x + 10, y + 140);
            } else {
                graphics.setColor(new Color(255, 220, 120));
                graphics.setFont(new Font("Times New Roman", Font.ITALIC, 12));
                graphics.drawString("coming soon...", x + 18, y + 140);
            }

            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            if (selectable) {
                graphics.setColor(Color.WHITE);
                graphics.drawString(weapons.get(index), x + 10, y + 164);
            } else {
                graphics.drawString(" ", x + 10, y + 164);
            }
            graphics.drawString(" ", x + 10, y + 188);

            if (selectable) {
                graphics.setColor(new Color(140, 235, 160));
                graphics.setFont(new Font("Times New Roman", Font.BOLD, 14));
                graphics.drawString("Selected", x + 22, y + 212);
                drawCharacterAbilityPreview(graphics, x + 12, y + 176);
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

    private void drawAbilityHud(Graphics2D graphics) {
        AbilityManager manager = gameLogic.getAbilityManager();
        int barWidth = 224;
        int barX = (PANEL_WIDTH - barWidth) / 2;
        int manaY = PANEL_HEIGHT - 82;
        double manaRatio = manager.getMana() / manager.getMaxMana();

        graphics.setColor(new Color(15, 18, 28, 220));
        graphics.fillRoundRect(barX, manaY, barWidth, 10, 8, 8);
        graphics.setColor(gameLogic.getManaPulseColor());
        graphics.fillRoundRect(barX, manaY, (int) Math.round(barWidth * manaRatio), 10, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 11));
        graphics.drawString(String.format("Mana %.0f/%.0f", manager.getMana(), manager.getMaxMana()),
                barX + 72, manaY - 4);

        RpgAbility[] equipped = manager.getEquippedAbilities();
        for (int index = 0; index < equipped.length; index++) {
            drawAbilitySlot(graphics, getHotbarSlotBounds(index), equipped[index], index + 1,
                    true, gameLogic.getLevel());
        }
    }

    private void drawCharacterAbilityPreview(Graphics2D graphics, int x, int y) {
        List<RpgAbility> abilities = gameLogic.getAbilityManager().getAbilities(AbilityClass.ASSASSIN);
        for (int index = 0; index < Math.min(4, abilities.size()); index++) {
            Rectangle bounds = new Rectangle(x + index * 24, y, 20, 20);
            drawAbilityIcon(graphics, bounds, abilities.get(index), true, gameLogic.getLevel());
        }
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
            String upgradeName = gameLogic.getUpgradeChoices().get(index);
            String description = gameLogic.getUpgradeDescription(upgradeName);
            graphics.setColor(new Color(60, 60, 60));
            graphics.fillRoundRect(x, y, cardWidth, cardHeight, 12, 12);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 15));
            graphics.drawString(upgradeName, x + 12, y + 28);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            graphics.drawString(description, x + 12, y + 48);
            graphics.drawString("+ minor boost", x + 12, y + 66);
        }
    }

    private void drawGameOverScreen(Graphics2D graphics) {
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.setColor(new Color(255, 90, 90));
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 64));
        String title = "GAME OVER";
        int titleWidth = graphics.getFontMetrics().stringWidth(title);
        graphics.drawString(title, (PANEL_WIDTH - titleWidth) / 2, 200);

        graphics.setColor(new Color(255, 220, 140));
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        graphics.drawString("The hero was overwhelmed.", 245, 250);
        graphics.drawString("The battlefield will remember this moment.", 150, 285);

        drawMenuButton(graphics, getTryAgainButtonBounds(), "Try Again");
        drawMenuButton(graphics, getGameOverMainMenuButtonBounds(), "Main Menu");
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
        int rowHeight = 42;
        int start = y + 78;

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        graphics.drawString("Show FPS", left, start + 22);
        graphics.drawString("Target FPS", left, start + 22 + rowHeight);
        graphics.drawString("Sound", left, start + 22 + rowHeight * 2);
        graphics.drawString("Back", left, start + 22 + rowHeight * 3);

        drawToggleButton(graphics, left + 180, start - 10, 36, 24, gameLogic.isDebugInfoVisible());
        drawFpsAdjuster(graphics, left + 142, start - 12 + rowHeight);
        drawToggleButton(graphics, left + 180, start - 10 + rowHeight * 2, 36, 24, gameLogic.isSoundEnabled());
        drawMenuButton(graphics, new Rectangle(left + 160, start + rowHeight * 3 - 18, 90, 32), "Back");
    }

    private void drawFpsAdjuster(Graphics2D graphics, int x, int y) {
        Rectangle decreaseButton = new Rectangle(x, y, 26, 28);
        Rectangle increaseButton = new Rectangle(x + 94, y, 26, 28);
        Rectangle valueBox = new Rectangle(x + 30, y, 60, 28);

        drawSmallButton(graphics, decreaseButton, "<");
        graphics.setColor(new Color(38, 38, 38));
        graphics.fillRoundRect(valueBox.x, valueBox.y, valueBox.width, valueBox.height, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 16));
        String value = Integer.toString(getTargetFramesPerSecond());
        int textWidth = graphics.getFontMetrics().stringWidth(value);
        graphics.drawString(value, valueBox.x + (valueBox.width - textWidth) / 2, valueBox.y + 20);
        drawSmallButton(graphics, increaseButton, ">");
    }

    private void drawSmallButton(Graphics2D graphics, Rectangle bounds, String text) {
        graphics.setColor(new Color(80, 80, 80));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 16));
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, bounds.x + (bounds.width - textWidth) / 2, bounds.y + 20);
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

    private void handleGameOverClick(MouseEvent event) {
        if (contains(event, getTryAgainButtonBounds())) {
            gameLogic.startGame();
            return;
        }
        if (contains(event, getGameOverMainMenuButtonBounds())) {
            gameLogic.showMainMenu();
        }
    }

    private Rectangle getTryAgainButtonBounds() {
        return new Rectangle(290, 320, 220, 46);
    }

    private Rectangle getGameOverMainMenuButtonBounds() {
        return new Rectangle(290, 382, 220, 46);
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
            int start = y + 78;
            int rowHeight = 42;
            Rectangle backButton = new Rectangle(left + 160, start + rowHeight * 3 - 18, 90, 32);
            if (contains(event, backButton)) {
                gameLogic.toggleSettings();
                return;
            }
            Rectangle fpsToggle = new Rectangle(left + 180, start - 10, 36, 24);
            Rectangle fpsDecreaseButton = new Rectangle(left + 142, start - 12 + rowHeight, 26, 28);
            Rectangle fpsIncreaseButton = new Rectangle(left + 236, start - 12 + rowHeight, 26, 28);
            Rectangle soundToggle = new Rectangle(left + 180, start - 10 + rowHeight * 2, 36, 24);
            if (contains(event, fpsToggle)) {
                gameLogic.setDebugInfoVisible(!gameLogic.isDebugInfoVisible());
                return;
            }
            if (contains(event, fpsDecreaseButton)) {
                adjustTargetFramesPerSecond(-1);
                return;
            }
            if (contains(event, fpsIncreaseButton)) {
                adjustTargetFramesPerSecond(1);
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

    private void handleSkillMenuClick(MouseEvent event) {
        for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
            Rectangle bounds = getSkillMenuSlotBounds(index);
            if (contains(event, bounds)) {
                gameLogic.selectAbilityEquipSlot(index);
                return;
            }
        }

        RpgAbility clickedAbility = abilityAtSkillMenuPoint(event.getX(), event.getY());
        if (clickedAbility != null) {
            gameLogic.equipAbility(clickedAbility);
        }
    }

    private boolean contains(MouseEvent event, Rectangle rectangle) {
        return event.getX() >= rectangle.x && event.getX() <= rectangle.x + rectangle.width
                && event.getY() >= rectangle.y && event.getY() <= rectangle.y + rectangle.height;
    }

    private boolean containsPoint(int x, int y, Rectangle rectangle) {
        return x >= rectangle.x && x <= rectangle.x + rectangle.width
                && y >= rectangle.y && y <= rectangle.y + rectangle.height;
    }

    private Rectangle getHotbarSlotBounds(int index) {
        int slotSize = 46;
        int gap = 10;
        int totalWidth = AbilityManager.EQUIPPED_SLOT_COUNT * slotSize
                + (AbilityManager.EQUIPPED_SLOT_COUNT - 1) * gap;
        int startX = (PANEL_WIDTH - totalWidth) / 2;
        return new Rectangle(startX + index * (slotSize + gap), PANEL_HEIGHT - 66,
                slotSize, slotSize);
    }

    private Rectangle getSkillMenuSlotBounds(int index) {
        int slotSize = 46;
        int gap = 10;
        int startX = 290;
        return new Rectangle(startX + index * (slotSize + gap), 76, slotSize, slotSize);
    }

    private void drawAbilitySlot(Graphics2D graphics, Rectangle bounds, RpgAbility ability,
            int shortcut, boolean showCooldown, int playerLevel) {
        graphics.setColor(new Color(18, 20, 28, 225));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(new Color(210, 210, 220));
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        if (ability != null) {
            drawAbilityIcon(graphics, bounds, ability,
                    ability.getDefinition().isUnlockedAt(playerLevel), playerLevel);
            if (showCooldown) {
                drawCooldownOverlay(graphics, bounds, ability);
            }
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 11));
            graphics.drawString(String.valueOf((int) ability.getDefinition().getManaCost()),
                    bounds.x + 3, bounds.y + bounds.height - 4);
        }

        graphics.setColor(new Color(255, 255, 255, 210));
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 12));
        graphics.drawString(String.valueOf(shortcut), bounds.x + bounds.width - 10, bounds.y + 13);
    }

    private void drawAbilityIcon(Graphics2D graphics, Rectangle bounds,
            RpgAbility ability, boolean unlocked, int playerLevel) {
        graphics.drawImage(ability.getIcon(), bounds.x + 4, bounds.y + 4,
                bounds.width - 8, bounds.height - 8, null);
        if (!unlocked) {
            graphics.setColor(new Color(0, 0, 0, 170));
            graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            graphics.setColor(new Color(255, 220, 130));
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 10));
            graphics.drawString("LV " + ability.getDefinition().getUnlockLevel(),
                    bounds.x + 8, bounds.y + bounds.height / 2 + 4);
        }
    }

    private void drawCooldownOverlay(Graphics2D graphics, Rectangle bounds, RpgAbility ability) {
        double ratio = ability.getCooldownRatio();
        if (ratio <= 0.0) {
            return;
        }
        int overlayHeight = (int) Math.round(bounds.height * ratio);
        graphics.setColor(new Color(0, 0, 0, 165));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, overlayHeight, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 13));
        String text = String.valueOf((int) Math.ceil(ability.getCooldownRemaining()));
        graphics.drawString(text, bounds.x + bounds.width / 2 - 5,
                bounds.y + bounds.height / 2 + 4);
    }

    private void drawSkillMenu(Graphics2D graphics) {
        if (!gameLogic.isSkillMenuOpen()) {
            return;
        }

        Graphics2D overlay = (Graphics2D) graphics.create();
        overlay.setComposite(AlphaComposite.SrcOver.derive(0.88f));
        overlay.setColor(new Color(9, 10, 15));
        overlay.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        overlay.dispose();

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 28));
        graphics.drawString("Skills", 36, 54);

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        graphics.drawString("Select a slot, then choose an unlocked ability.", 116, 53);

        RpgAbility[] equipped = gameLogic.getAbilityManager().getEquippedAbilities();
        for (int index = 0; index < equipped.length; index++) {
            Rectangle bounds = getSkillMenuSlotBounds(index);
            if (index == gameLogic.getAbilityManager().getSelectedEquipSlot()) {
                graphics.setColor(new Color(255, 220, 120, 180));
                graphics.fillRoundRect(bounds.x - 3, bounds.y - 3,
                        bounds.width + 6, bounds.height + 6, 10, 10);
            }
            drawAbilitySlot(graphics, bounds, equipped[index], index + 1, false, gameLogic.getLevel());
        }

        int columnWidth = 126;
        int startX = 24;
        int startY = 145;
        int iconSize = 30;
        int rowHeight = 38;
        for (AbilityClass abilityClass : AbilityClass.values()) {
            int column = abilityClass.ordinal();
            int x = startX + column * columnWidth;
            graphics.setColor(new Color(255, 255, 255, 220));
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 14));
            graphics.drawString(abilityClass.getDisplayName(), x, startY - 14);

            List<RpgAbility> abilities = gameLogic.getAbilityManager().getAbilities(abilityClass);
            for (int index = 0; index < abilities.size(); index++) {
                RpgAbility ability = abilities.get(index);
                Rectangle bounds = getSkillMenuAbilityBounds(abilityClass.ordinal(), index);
                boolean unlocked = ability.getDefinition().isUnlockedAt(gameLogic.getLevel());
                drawAbilityIcon(graphics, bounds, ability, unlocked, gameLogic.getLevel());
                if (ability.isEquipped()) {
                    graphics.setColor(new Color(120, 235, 150, 210));
                    graphics.drawRoundRect(bounds.x - 1, bounds.y - 1,
                            bounds.width + 2, bounds.height + 2, 8, 8);
                }
                graphics.setColor(unlocked ? Color.WHITE : new Color(170, 170, 170));
                graphics.setFont(new Font("Times New Roman", Font.PLAIN, 10));
                drawClippedString(graphics, ability.getName(),
                        bounds.x + iconSize + 5, bounds.y + 13, columnWidth - iconSize - 8);
            }
        }
    }

    private Rectangle getSkillMenuAbilityBounds(int classIndex, int abilityIndex) {
        int columnWidth = 126;
        int startX = 24;
        int startY = 145;
        int rowHeight = 38;
        return new Rectangle(startX + classIndex * columnWidth, startY + abilityIndex * rowHeight,
                30, 30);
    }

    private RpgAbility abilityAtSkillMenuPoint(int x, int y) {
        for (AbilityClass abilityClass : AbilityClass.values()) {
            List<RpgAbility> abilities = gameLogic.getAbilityManager().getAbilities(abilityClass);
            for (int index = 0; index < abilities.size(); index++) {
                if (containsPoint(x, y, getSkillMenuAbilityBounds(abilityClass.ordinal(), index))) {
                    return abilities.get(index);
                }
            }
        }
        return null;
    }

    private RpgAbility hoveredAbility() {
        for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
            if (containsPoint(mouseX, mouseY, getHotbarSlotBounds(index))) {
                return gameLogic.getAbilityManager().getEquippedAbilities()[index];
            }
        }
        if (gameLogic.isSkillMenuOpen()) {
            return abilityAtSkillMenuPoint(mouseX, mouseY);
        }
        return null;
    }

    private void drawAbilityTooltip(Graphics2D graphics) {
        RpgAbility ability = hoveredAbility();
        if (ability == null) {
            return;
        }
        AbilityDefinition definition = ability.getDefinition();
        int x = Math.min(mouseX + 14, PANEL_WIDTH - 230);
        int y = Math.min(mouseY + 16, PANEL_HEIGHT - 96);
        graphics.setColor(new Color(15, 16, 22, 235));
        graphics.fillRoundRect(x, y, 220, 86, 8, 8);
        graphics.setColor(new Color(230, 230, 235));
        graphics.drawRoundRect(x, y, 220, 86, 8, 8);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 14));
        graphics.drawString(definition.getName(), x + 10, y + 20);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        graphics.drawString(definition.getAbilityClass().getDisplayName(), x + 10, y + 36);
        graphics.drawString("Mana " + (int) definition.getManaCost()
                + "  Cooldown " + (int) definition.getCooldownSeconds() + "s",
                x + 10, y + 52);
        drawClippedString(graphics, definition.getDescription(), x + 10, y + 70, 196);
    }

    private void drawClippedString(Graphics2D graphics, String text, int x, int y, int maxWidth) {
        String clipped = text;
        while (graphics.getFontMetrics().stringWidth(clipped) > maxWidth && clipped.length() > 3) {
            clipped = clipped.substring(0, clipped.length() - 4) + "...";
        }
        graphics.drawString(clipped, x, y);
    }

    private int getTargetFramesPerSecond() {
        return FPS_OPTIONS[selectedFpsIndex];
    }

    private int getFrameDelayMillis() {
        return Math.max(1, Math.round(1000.0f / getTargetFramesPerSecond()));
    }

    private void adjustTargetFramesPerSecond(int direction) {
        int newIndex = Math.max(0, Math.min(FPS_OPTIONS.length - 1, selectedFpsIndex + direction));
        if (newIndex == selectedFpsIndex) {
            return;
        }

        selectedFpsIndex = newIndex;
        frameTimer.setDelay(getFrameDelayMillis());
        frameTimer.setInitialDelay(getFrameDelayMillis());
        lastUpdateNanos = System.nanoTime();
        framesPerSecond = getTargetFramesPerSecond();
    }

    private void drawDebugInfo(Graphics2D graphics) {
        if (!gameLogic.isDebugInfoVisible()) {
            return;
        }

        String fpsText = String.format("FPS %.0f/%d", framesPerSecond, getTargetFramesPerSecond());
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 13));
        int textWidth = graphics.getFontMetrics().stringWidth(fpsText);
        int boxWidth = textWidth + 16;
        int boxX = PANEL_WIDTH - boxWidth - 12;
        int boxY = 34;
        graphics.setColor(new Color(0, 0, 0, 155));
        graphics.fillRoundRect(boxX, boxY, boxWidth, 22, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.drawString(fpsText, boxX + 8, boxY + 15);
    }
}
