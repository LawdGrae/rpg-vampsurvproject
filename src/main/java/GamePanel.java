import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int PANEL_WIDTH = 1280;
    private static final int PANEL_HEIGHT = 720;
    private static final int EXP_BAR_HEIGHT = 12;
    private static final int EXP_BAR_Y = 18;
    private static final int[] FPS_OPTIONS = {30, 45, 60, 90, 120};

    private static final Color PANEL_DARK = new Color(15, 16, 24, 226);
    private static final Color PANEL_MID = new Color(28, 27, 36, 232);
    private static final Color GOLD = new Color(214, 172, 86);
    private static final Color GOLD_LIGHT = new Color(255, 226, 142);
    private static final Color TEXT_SOFT = new Color(225, 218, 202);
    private static final Color TEXT_MUTED = new Color(167, 158, 145);
    private static final Color BUTTON_TOP = new Color(74, 66, 80);
    private static final Color BUTTON_BOTTOM = new Color(38, 37, 48);
    private static final Color BUTTON_HOVER_TOP = new Color(106, 89, 88);
    private static final Color BUTTON_HOVER_BOTTOM = new Color(61, 48, 58);
    private static final Color HEALTH_RED = new Color(212, 58, 64);
    private static final Color MANA_BLUE = new Color(75, 170, 255);
    private static final Color EXP_CYAN = new Color(70, 207, 225);

    private static final Font TITLE_FONT = new Font("Times New Roman", Font.BOLD, 54);
    private static final Font HEADER_FONT = new Font("Times New Roman", Font.BOLD, 32);
    private static final Font BUTTON_FONT = new Font("Times New Roman", Font.BOLD, 20);
    private static final Font LABEL_FONT = new Font("Times New Roman", Font.BOLD, 13);
    private static final Font BODY_FONT = new Font("Times New Roman", Font.PLAIN, 14);

    private final BufferedImage grassTile;
    private final BufferedImage landscape;
    private final GameLogic gameLogic;
    private final Timer frameTimer;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private long lastUpdateNanos = System.nanoTime();
    private double lastDeltaTime;
    private double framesPerSecond;
    private int selectedFpsIndex = 2;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean mouseDown;

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
        return loadCachedImage("/main/resources/grasstile.png");
    }

    private BufferedImage loadLandscape() {
        return loadCachedImage("/main/resources/landscape.png");
    }

    private BufferedImage loadCachedImage(String path) {
        return imageCache.computeIfAbsent(path, ResourceLoader::loadImage);
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
                if (gameLogic.isSettingsOpen()) {
                    gameLogic.toggleSettings();
                } else if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen()) {
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
                mouseDown = true;
                mouseX = event.getX();
                mouseY = event.getY();
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

            @Override
            public void mouseReleased(MouseEvent event) {
                mouseDown = false;
                mouseX = event.getX();
                mouseY = event.getY();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                mouseDown = false;
                mouseX = -1;
                mouseY = -1;
                repaint();
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
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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
        worldGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        worldGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        worldGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        int tileWidth = grassTile.getWidth();
        int tileHeight = grassTile.getHeight();
        int startX = Math.floorMod((int) gameLogic.getWorldOffsetX(), tileWidth) - tileWidth;
        int startY = Math.floorMod((int) gameLogic.getWorldOffsetY(), tileHeight) - tileHeight;

        for (int tileX = startX; tileX < PANEL_WIDTH; tileX += tileWidth) {
            for (int tileY = startY; tileY < PANEL_HEIGHT; tileY += tileHeight) {
                worldGraphics.drawImage(grassTile, tileX, tileY, null);
            }
        }

        gameLogic.drawAbilityGroundEffects(worldGraphics, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
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
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        graphics.drawImage(landscape, 0, 0, panelWidth, panelHeight, null);
        drawVignette(graphics);

        Rectangle panel = getMainMenuPanelBounds();
        drawPanel(graphics, panel, 24);

        drawCenteredString(graphics, "RPG", TITLE_FONT, GOLD_LIGHT,
                panel.x, panel.y + clamp(panel.height / 4, 112, 150), panel.width);

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        graphics.setColor(TEXT_SOFT);
        drawCenteredString(graphics, "Placeholder adventure",
                panel.x, panel.y + clamp(panel.height / 3, 154, 190), panel.width);

        drawMenuButton(graphics, getMainMenuPlayButtonBounds(), "Play");
        drawMenuButton(graphics, getMainMenuSettingsButtonBounds(), "Settings");
        drawMenuButton(graphics, getMainMenuQuitButtonBounds(), "Quit");

        graphics.setColor(TEXT_MUTED);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        graphics.drawString("V0.0.1", panelWidth - 80, panelHeight - 22);

        if (gameLogic.isSettingsOpen()) {
            drawDimOverlay(graphics, 150);
            drawSettingsFrame(graphics);
        }
    }

    private Rectangle getMainMenuPanelBounds() {
        int panelWidth = Math.max(PANEL_WIDTH, getWidth());
        int panelHeight = Math.max(PANEL_HEIGHT, getHeight());
        int width = clamp((int) (panelWidth * 0.44), 560, 760);
        int height = clamp((int) (panelHeight * 0.66), 452, 560);
        return new Rectangle((panelWidth - width) / 2, (panelHeight - height) / 2,
                width, height);
    }

    private Rectangle getMainMenuButtonBounds(int row) {
        Rectangle panel = getMainMenuPanelBounds();
        int buttonWidth = clamp((int) (panel.width * 0.42), 220, 330);
        int buttonHeight = clamp(panel.height / 10, 46, 56);
        int gap = clamp(panel.height / 28, 18, 28);
        int totalHeight = buttonHeight * 3 + gap * 2;
        int startY = panel.y + (int) (panel.height * 0.58) - totalHeight / 2;
        return new Rectangle(panel.x + (panel.width - buttonWidth) / 2,
                startY + row * (buttonHeight + gap), buttonWidth, buttonHeight);
    }

    private Rectangle getMainMenuPlayButtonBounds() {
        return getMainMenuButtonBounds(0);
    }

    private Rectangle getMainMenuSettingsButtonBounds() {
        return getMainMenuButtonBounds(1);
    }

    private Rectangle getMainMenuQuitButtonBounds() {
        return getMainMenuButtonBounds(2);
    }

    private void drawCharacterSelection(Graphics2D graphics) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        graphics.drawImage(landscape, 0, 0, panelWidth, panelHeight, null);
        drawVignette(graphics);

        drawPanel(graphics, new Rectangle(28, 36, panelWidth - 56, panelHeight - 62), 24);

        drawCenteredString(graphics, "Choose your hero", HEADER_FONT, GOLD_LIGHT,
                0, getSelectionTitleY(), panelWidth);

        List<String> names = gameLogic.getCharacterNames();
        int characterCount = gameLogic.getCharacterCount();

        for (int index = 0; index < characterCount; index++) {
            Rectangle card = getCharacterCardBounds(index);
            int x = card.x;
            int y = card.y;
            int cardWidth = card.width;
            boolean selectable = gameLogic.isCharacterSelectable(index);
            boolean selected = gameLogic.getSelectedCharacterIndex() == index;
            boolean hovered = containsPoint(mouseX, mouseY, card);
            Color accent = getCharacterAccentColor(index);
            double pulse = selected ? 0.5 + 0.5 * Math.sin(System.nanoTime() / 280_000_000.0) : 0.0;

            if (selected) {
                graphics.setColor(new Color(255, 220, 120, 48 + (int) (pulse * 42)));
                graphics.fillRoundRect(x - 6, y - 6, cardWidth + 12, card.height + 12, 24, 24);
            }

            graphics.setPaint(new GradientPaint(x, y,
                    selectable ? blend(new Color(28, 30, 39), accent, selected ? 0.26 : 0.12) : new Color(30, 30, 35),
                    x, y + card.height, selectable ? new Color(11, 13, 20) : new Color(18, 18, 22)));
            graphics.fillRoundRect(x, y, cardWidth, card.height, 18, 18);
            graphics.setColor(selectable ? (selected ? GOLD_LIGHT : hovered ? accent.brighter() : accent) : new Color(94, 91, 96));
            graphics.drawRoundRect(x, y, cardWidth, card.height, 18, 18);
            graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), selectable ? 65 : 25));
            graphics.fillRoundRect(x + 8, y + 8, cardWidth - 16, 4, 8, 8);

            String name = names.get(index);
            if (selectable) {
                drawCharacterPortrait(graphics, index, getPortraitBounds(card));

                graphics.setColor(TEXT_SOFT);
                graphics.setFont(scaledFont(LABEL_FONT, card, 1.12));
                drawCenteredClippedString(graphics, name.toUpperCase(), x + 10,
                        getNameY(card), cardWidth - 20);
                graphics.setColor(accent.brighter());
                graphics.setFont(scaledFont(new Font("Times New Roman", Font.BOLD, 11), card, 1.0));
                drawCenteredClippedString(graphics, gameLogic.getCharacterClassName(index),
                        x + 10, getClassY(card), cardWidth - 20);
                graphics.setColor(TEXT_SOFT);
                graphics.setFont(scaledFont(new Font("Times New Roman", Font.PLAIN, 11), card, 1.0));
                drawCenteredClippedString(graphics, gameLogic.getCharacterRole(index),
                        x + 10, getRoleY(card), cardWidth - 20);
                drawCenteredClippedString(graphics, gameLogic.getCharacterWeaponName(index),
                        x + 10, getWeaponNameY(card), cardWidth - 20);
            } else {
                graphics.setColor(new Color(255, 220, 120));
                graphics.setFont(new Font("Times New Roman", Font.ITALIC, 12));
                drawCenteredString(graphics, "?", HEADER_FONT.deriveFont((float) clamp(card.width / 3, 36, 72)),
                        new Color(170, 166, 176), x, y + clamp(card.height / 4, 92, 150), cardWidth);
                graphics.setColor(new Color(255, 220, 120));
                graphics.setFont(new Font("Times New Roman", Font.ITALIC, clamp(card.width / 14, 12, 18)));
                drawCenteredClippedString(graphics, "coming soon...", x + 12,
                        y + clamp(card.height / 3, 130, 190), cardWidth - 24);
            }

            if (selectable) {
                drawCharacterAbilityPreview(graphics, index, accent, selected);
                graphics.setColor(selected ? new Color(140, 235, 160) : TEXT_MUTED);
                graphics.setFont(LABEL_FONT);
                drawCenteredString(graphics, selected ? "Selected" : "Available",
                        x, y + card.height - 16, cardWidth);
            }
        }

        drawMenuButton(graphics, getCharacterStartButtonBounds(), "Start");
        drawSelectionTooltip(graphics);
    }

    private void drawCharacterPortrait(Graphics2D graphics, int characterIndex,
            int x, int y, int width, int height) {
        try {
            BufferedImage portrait = loadCachedImage(gameLogic.getPortraitPath(characterIndex));
            if (gameLogic.isCharacterSelectable(characterIndex)) {
                int frameWidth = portrait.getWidth() / 3;
                int frameHeight = portrait.getHeight() / 4;
                int sourceX = frameWidth;
                int sourceY = frameHeight * 2;
                drawImageInside(graphics, portrait.getSubimage(sourceX, sourceY, frameWidth, frameHeight),
                        new Rectangle(x, y, width, height), true);
            } else {
                drawImageInside(graphics, portrait, new Rectangle(x, y, width, height), true);
            }
        } catch (IllegalStateException ignored) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(x, y, width, height);
        }
    }

    private void drawCharacterPortrait(Graphics2D graphics, int characterIndex, Rectangle bounds) {
        drawCharacterPortrait(graphics, characterIndex, bounds.x, bounds.y, bounds.width, bounds.height);
        if (gameLogic.isCharacterSelectable(characterIndex)) {
            drawSelectionWeapons(graphics, characterIndex, bounds);
        }
    }

    private void drawSelectionWeapons(Graphics2D graphics, int characterIndex, Rectangle bounds) {
        List<String> weaponPaths = gameLogic.getCharacterWeaponImagePaths(characterIndex);
        if (weaponPaths.isEmpty()) {
            return;
        }

        for (int index = 0; index < weaponPaths.size(); index++) {
            BufferedImage image = loadCachedImage(weaponPaths.get(index));
            drawSelectionWeapon(graphics, image, characterIndex, index, bounds);
        }
    }

    private void drawSelectionWeapon(Graphics2D graphics, BufferedImage image,
            int characterIndex, int weaponIndex, Rectangle bounds) {
        double centerX = bounds.x + bounds.width / 2.0;
        double centerY = bounds.y + bounds.height / 2.0;
        double scaleBase = bounds.height / 64.0;
        double offsetX = 0.0;
        double offsetY = 0.0;
        double angle = 0.0;
        double height = bounds.height * 0.74;
        double pivotX = 0.5;
        double pivotY = 0.68;

        switch (characterIndex) {
            case 0 -> {
                if (weaponIndex == 0) {
                    offsetX = -16.0 * scaleBase;
                    offsetY = 22.0 * scaleBase;
                    angle = Math.toRadians(-18);
                    height = bounds.height * 0.62;
                    pivotY = 0.72;
                } else {
                    offsetX = 16.0 * scaleBase;
                    offsetY = 11.0 * scaleBase;
                    angle = Math.toRadians(7);
                    height = bounds.height * 0.52;
                    pivotY = 0.52;
                }
            }
            case 1 -> {
                offsetX = 0.0;
                offsetY = 15.0 * scaleBase;
                angle = 0.0;
                height = bounds.height * 0.58;
                pivotY = 0.54;
            }
            case 2 -> {
                offsetX = 17.0 * scaleBase;
                offsetY = 4.0 * scaleBase;
                angle = Math.toRadians(13);
                height = bounds.height * 0.82;
                pivotY = 0.72;
            }
            case 3 -> {
                offsetX = -17.0 * scaleBase;
                offsetY = 3.0 * scaleBase;
                angle = Math.toRadians(-16);
                height = bounds.height * 0.84;
                pivotY = 0.72;
            }
            default -> {
            }
        }

        drawTransformedImage(graphics, image, centerX + offsetX, centerY + offsetY,
                height, pivotX, pivotY, angle, false);
    }

    private void drawExperienceBar(Graphics2D graphics) {
        double progress = gameLogic.getExpProgress();
        int barWidth = 278;
        int barX = 22;
        int barY = 72;

        drawResourceBar(graphics, barX, barY, barWidth, EXP_BAR_HEIGHT, progress,
                EXP_CYAN, new Color(28, 77, 91), "EXP "
                        + gameLogic.getCurrentExp() + "/" + gameLogic.getExpToNextLevel());

        double healthRatio = gameLogic.getPlayerMaxHealth() <= 0.0
                ? 0.0
                : gameLogic.getPlayerHealth() / gameLogic.getPlayerMaxHealth();
        drawResourceBar(graphics, barX, 38, barWidth, 16, healthRatio,
                HEALTH_RED, new Color(90, 25, 33), String.format("HP %.0f/%.0f",
                        gameLogic.getPlayerHealth(), gameLogic.getPlayerMaxHealth()));

        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(LABEL_FONT);
        graphics.drawString("LVL " + gameLogic.getLevel(), barX, 28);
    }

    private void drawGameTimer(Graphics2D graphics) {
        int totalSeconds = (int) gameLogic.getGameTimer();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timerText = String.format("%02d:%02d", minutes, seconds);
        int textWidth = graphics.getFontMetrics().stringWidth(timerText);

        int x = (PANEL_WIDTH - textWidth) / 2;
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(x - 15, 12, textWidth + 30, 26, 12, 12);
        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(LABEL_FONT);
        graphics.drawString(timerText, x, 30);
    }

    private void drawAbilityHud(Graphics2D graphics) {
        AbilityManager manager = gameLogic.getAbilityManager();
        int barWidth = 224;
        int barX = (PANEL_WIDTH - barWidth) / 2;
        int manaY = PANEL_HEIGHT - 82;
        double manaRatio = manager.getMana() / manager.getMaxMana();

        drawResourceBar(graphics, barX, manaY, barWidth, 11, manaRatio,
                gameLogic.getManaPulseColor(), new Color(28, 47, 76),
                String.format("MP %.0f/%.0f", manager.getMana(), manager.getMaxMana()));

        RpgAbility[] equipped = manager.getEquippedAbilities();
        for (int index = 0; index < equipped.length; index++) {
            drawAbilitySlot(graphics, getHotbarSlotBounds(index), equipped[index], index + 1,
                    true, gameLogic.getLevel());
        }
    }

    private void drawCharacterAbilityPreview(Graphics2D graphics, int characterIndex,
            Color accent, boolean selected) {
        Rectangle card = getCharacterCardBounds(characterIndex);
        List<RpgAbility> abilities = gameLogic.getCharacterActiveAbilities(characterIndex);
        int iconSize = getActiveSkillIconSize(card);
        int gap = getActiveSkillGap(card);
        int y = getActiveSkillIconY(card);

        for (int index = 0; index < Math.min(AbilityManager.EQUIPPED_SLOT_COUNT, abilities.size()); index++) {
            Rectangle bounds = getActiveSkillIconBounds(characterIndex, index);
            graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    selected ? 110 : 62));
            graphics.fillRoundRect(bounds.x - 3, bounds.y - 3,
                    bounds.width + 6, bounds.height + 6, 8, 8);
            drawAbilityIcon(graphics, bounds, abilities.get(index), true, gameLogic.getLevel());
            graphics.setColor(new Color(255, 255, 255, 120));
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 10));
            graphics.drawString(String.valueOf(index + 1), bounds.x + 2, bounds.y + 10);
        }

        drawPassivePreview(graphics, characterIndex, card, accent, selected);
    }

    private void drawPassivePreview(Graphics2D graphics, int characterIndex, Rectangle card,
            Color accent, boolean selected) {
        RpgAbility passive = gameLogic.getCharacterPassiveAbility(characterIndex);
        if (passive == null) {
            return;
        }

        Rectangle bounds = getPassiveIconBounds(characterIndex);
        graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                selected ? 80 : 44));
        graphics.fillRoundRect(bounds.x - 4, bounds.y - 4,
                bounds.width + 8, bounds.height + 8, 10, 10);
        graphics.setColor(selected ? GOLD_LIGHT : accent.brighter());
        graphics.drawRoundRect(bounds.x - 3, bounds.y - 3,
                bounds.width + 6, bounds.height + 6, 10, 10);
        drawAbilityIcon(graphics, bounds, passive, true, gameLogic.getLevel());

        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, Math.max(9, bounds.width / 4)));
        drawCenteredString(graphics, "PASSIVE", card.x, bounds.y - 7, card.width);
        if (card.width >= 154) {
            graphics.setColor(TEXT_SOFT);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, Math.max(9, bounds.width / 4)));
            drawCenteredClippedString(graphics, gameLogic.getCharacterPassiveName(characterIndex),
                    card.x + 8, bounds.y + bounds.height + 14, card.width - 16);
        }
    }

    private Rectangle getActiveSkillIconBounds(int characterIndex, int skillIndex) {
        Rectangle card = getCharacterCardBounds(characterIndex);
        int iconSize = getActiveSkillIconSize(card);
        int gap = getActiveSkillGap(card);
        int totalWidth = AbilityManager.EQUIPPED_SLOT_COUNT * iconSize
                + (AbilityManager.EQUIPPED_SLOT_COUNT - 1) * gap;
        int x = card.x + (card.width - totalWidth) / 2;
        return new Rectangle(x + skillIndex * (iconSize + gap), getActiveSkillIconY(card),
                iconSize, iconSize);
    }

    private Rectangle getPassiveIconBounds(int characterIndex) {
        Rectangle card = getCharacterCardBounds(characterIndex);
        int iconSize = getPassiveIconSize(card);
        return new Rectangle(card.x + (card.width - iconSize) / 2, getPassiveIconY(card),
                iconSize, iconSize);
    }

    private Rectangle getCharacterCardBounds(int index) {
        int characterCount = gameLogic.getCharacterCount();
        int panelWidth = Math.max(620, getWidth());
        int panelHeight = Math.max(560, getHeight());
        int margin = clamp(panelWidth / 22, 28, 88);
        int gap = clamp(panelWidth / 80, 10, 24);
        int availableWidth = Math.max(560, panelWidth - margin * 2);
        int cardWidth = Math.max(112,
                (availableWidth - gap * (characterCount - 1)) / characterCount);
        int totalCardWidth = characterCount * cardWidth + (characterCount - 1) * gap;
        int startX = Math.max(20, (panelWidth - totalCardWidth) / 2);
        int y = getSelectionCardTop();
        int maxCardHeight = panelHeight - y - 98;
        int cardHeight = clamp((int) (cardWidth * 1.95), 356, maxCardHeight);
        return new Rectangle(startX + index * (cardWidth + gap), y, cardWidth, cardHeight);
    }

    private Rectangle getCharacterStartButtonBounds() {
        int panelWidth = Math.max(PANEL_WIDTH, getWidth());
        Rectangle firstCard = getCharacterCardBounds(0);
        return new Rectangle((panelWidth - 200) / 2, firstCard.y + firstCard.height + 24, 200, 48);
    }

    private int getSelectionTitleY() {
        return clamp(getHeight() / 9, 72, 116);
    }

    private int getSelectionCardTop() {
        return getSelectionTitleY() + clamp(getHeight() / 28, 20, 36);
    }

    private Rectangle getPortraitBounds(Rectangle card) {
        int portraitHeight = clamp((int) (card.height * 0.26), 84, 172);
        int portraitWidth = Math.min(card.width - 28, (int) (portraitHeight * 1.08));
        return new Rectangle(card.x + (card.width - portraitWidth) / 2,
                card.y + clamp(card.height / 34, 10, 18), portraitWidth, portraitHeight);
    }

    private int getNameY(Rectangle card) {
        Rectangle portrait = getPortraitBounds(card);
        return portrait.y + portrait.height + clamp(card.height / 22, 16, 26);
    }

    private int getClassY(Rectangle card) {
        return getNameY(card) + clamp(card.height / 26, 14, 22);
    }

    private int getRoleY(Rectangle card) {
        return getClassY(card) + clamp(card.height / 30, 13, 20);
    }

    private int getWeaponNameY(Rectangle card) {
        return getRoleY(card) + clamp(card.height / 30, 13, 20);
    }

    private int getActiveSkillIconSize(Rectangle card) {
        return clamp((card.width - 36) / 5, 22, 44);
    }

    private int getActiveSkillGap(Rectangle card) {
        return clamp(card.width / 38, 5, 10);
    }

    private int getActiveSkillIconY(Rectangle card) {
        return getWeaponNameY(card) + clamp(card.height / 16, 22, 42);
    }

    private int getPassiveIconSize(Rectangle card) {
        return clamp((int) (getActiveSkillIconSize(card) * 1.16), 26, 52);
    }

    private int getPassiveIconY(Rectangle card) {
        return getActiveSkillIconY(card) + getActiveSkillIconSize(card)
                + clamp(card.height / 15, 24, 48);
    }

    private Color getCharacterAccentColor(int index) {
        return switch (index) {
            case 0 -> new Color(230, 88, 52);
            case 1 -> new Color(158, 74, 232);
            case 2 -> new Color(245, 195, 78);
            case 3 -> new Color(124, 92, 255);
            default -> new Color(110, 108, 118);
        };
    }

    private Color blend(Color base, Color overlay, double amount) {
        double clampedAmount = Math.max(0.0, Math.min(1.0, amount));
        double baseAmount = 1.0 - clampedAmount;
        return new Color(
                (int) (base.getRed() * baseAmount + overlay.getRed() * clampedAmount),
                (int) (base.getGreen() * baseAmount + overlay.getGreen() * clampedAmount),
                (int) (base.getBlue() * baseAmount + overlay.getBlue() * clampedAmount),
                base.getAlpha());
    }

    private Font scaledFont(Font font, Rectangle card, double multiplier) {
        float size = (float) clamp((int) Math.round(card.width / 12.0 * multiplier),
                Math.max(10, font.getSize() - 2), font.getSize() + 9);
        return font.deriveFont(size);
    }

    private void drawImageInside(Graphics2D graphics, BufferedImage image,
            Rectangle bounds, boolean pixelArt) {
        if (image == null || bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        double scale = Math.min(bounds.width / (double) image.getWidth(),
                bounds.height / (double) image.getHeight());
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int x = bounds.x + (bounds.width - width) / 2;
        int y = bounds.y + (bounds.height - height) / 2;

        Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                pixelArt ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                        : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, x, y, width, height, null);
        restoreInterpolation(graphics, previousInterpolation);
    }

    private void drawTransformedImage(Graphics2D graphics, BufferedImage image,
            double anchorX, double anchorY, double drawHeight, double pivotX,
            double pivotY, double angle, boolean flipX) {
        if (image == null || drawHeight <= 0.0) {
            return;
        }

        Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double scale = drawHeight / image.getHeight();
        AffineTransform transform = new AffineTransform();
        transform.translate(anchorX, anchorY);
        transform.rotate(angle);
        transform.scale(flipX ? -scale : scale, scale);
        transform.translate(-image.getWidth() * pivotX, -image.getHeight() * pivotY);
        graphics.drawImage(image, transform, null);
        restoreInterpolation(graphics, previousInterpolation);
    }

    private void restoreInterpolation(Graphics2D graphics, Object previousInterpolation) {
        if (previousInterpolation == null) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            return;
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void drawSelectionTooltip(Graphics2D graphics) {
        RpgAbility hoveredAbility = hoveredSelectionAbility();
        String passiveName = hoveredSelectionPassiveName();
        if (hoveredAbility == null && passiveName.isEmpty()) {
            return;
        }

        int x = Math.min(mouseX + 14, getWidth() - 230);
        int y = Math.min(mouseY + 16, getHeight() - 96);
        drawPanel(graphics, new Rectangle(x, y, 220, 86), 8);
        graphics.setFont(LABEL_FONT);
        graphics.setColor(GOLD_LIGHT);

        if (hoveredAbility != null) {
            AbilityDefinition definition = hoveredAbility.getDefinition();
            graphics.drawString(definition.getName(), x + 10, y + 20);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            graphics.setColor(TEXT_SOFT);
            graphics.drawString(definition.getAbilityClass().getDisplayName(), x + 10, y + 36);
            graphics.drawString("Mana " + (int) definition.getManaCost()
                    + "  Cooldown " + (int) definition.getCooldownSeconds() + "s",
                    x + 10, y + 52);
            drawClippedString(graphics, definition.getDescription(), x + 10, y + 70, 196);
            return;
        }

        graphics.drawString(passiveName, x + 10, y + 20);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        graphics.setColor(TEXT_SOFT);
        graphics.drawString("Passive", x + 10, y + 38);
        drawClippedString(graphics, "Always-on trait for this hero.", x + 10, y + 58, 196);
    }

    private RpgAbility hoveredSelectionAbility() {
        for (int characterIndex = 0; characterIndex < gameLogic.getCharacterCount(); characterIndex++) {
            if (!gameLogic.isCharacterSelectable(characterIndex)) {
                continue;
            }
            List<RpgAbility> abilities = gameLogic.getCharacterActiveAbilities(characterIndex);
            for (int skillIndex = 0; skillIndex < Math.min(AbilityManager.EQUIPPED_SLOT_COUNT, abilities.size()); skillIndex++) {
                if (containsPoint(mouseX, mouseY, getActiveSkillIconBounds(characterIndex, skillIndex))) {
                    return abilities.get(skillIndex);
                }
            }
        }
        return null;
    }

    private String hoveredSelectionPassiveName() {
        for (int characterIndex = 0; characterIndex < gameLogic.getCharacterCount(); characterIndex++) {
            if (gameLogic.isCharacterSelectable(characterIndex)
                    && containsPoint(mouseX, mouseY, getPassiveIconBounds(characterIndex))) {
                return gameLogic.getCharacterPassiveName(characterIndex);
            }
        }
        return "";
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

        drawDimOverlay(graphics, 185);

        drawPanel(graphics, new Rectangle(left - 22, top - 52,
                3 * cardWidth + 2 * gap + 44, cardHeight + 78), 18);

        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(HEADER_FONT.deriveFont(24f));
        graphics.drawString("Choose an upgrade", left + 8, top - 20);

        for (int index = 0; index < 3; index++) {
            int x = left + index * (cardWidth + gap);
            int y = top;
            String upgradeName = gameLogic.getUpgradeChoices().get(index);
            String description = gameLogic.getUpgradeDescription(upgradeName);
            Rectangle card = new Rectangle(x, y, cardWidth, cardHeight);
            boolean hovered = containsPoint(mouseX, mouseY, card);
            graphics.setPaint(new GradientPaint(x, y, hovered ? new Color(86, 69, 75) : new Color(48, 45, 56),
                    x, y + cardHeight, hovered ? new Color(52, 39, 50) : new Color(28, 27, 36)));
            graphics.fillRoundRect(x, y, cardWidth, cardHeight, 12, 12);
            graphics.setColor(hovered ? GOLD_LIGHT : GOLD);
            graphics.drawRoundRect(x, y, cardWidth, cardHeight, 12, 12);
            graphics.setColor(TEXT_SOFT);
            graphics.setFont(LABEL_FONT);
            graphics.drawString(upgradeName, x + 12, y + 28);
            graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            graphics.drawString(description, x + 12, y + 48);
            graphics.drawString("+ minor boost", x + 12, y + 66);
        }
    }

    private void drawGameOverScreen(Graphics2D graphics) {
        drawDimOverlay(graphics, 190);

        graphics.setFont(new Font("Times New Roman", Font.BOLD, 64));
        String title = "GAME OVER";
        int titleWidth = graphics.getFontMetrics().stringWidth(title);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int titleY = panelHeight / 2 - 100;
        graphics.setColor(new Color(0, 0, 0, 185));
        graphics.drawString(title, (panelWidth - titleWidth) / 2 + 3, titleY + 3);
        graphics.setColor(new Color(255, 90, 90));
        graphics.drawString(title, (panelWidth - titleWidth) / 2, titleY);

        graphics.setColor(TEXT_SOFT);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 22));
        drawCenteredString(graphics, "The hero was overwhelmed.", 0, titleY + 50, panelWidth);
        drawCenteredString(graphics, "The battlefield will remember this moment.", 0, titleY + 85, panelWidth);

        drawMenuButton(graphics, getTryAgainButtonBounds(), "Try Again");
        drawMenuButton(graphics, getGameOverMainMenuButtonBounds(), "Main Menu");
    }

    private void drawPauseMenu(Graphics2D graphics) {
        if (!gameLogic.isPaused()) {
            return;
        }

        drawDimOverlay(graphics, 180);

        Rectangle menuBounds = getSettingsFrameBounds();
        int menuWidth = menuBounds.width;
        int x = menuBounds.x;
        int y = menuBounds.y;

        drawPanel(graphics, menuBounds, 18);

        String title = gameLogic.isSettingsOpen() ? "Settings" : "Paused";
        drawCenteredString(graphics, title, HEADER_FONT, GOLD_LIGHT, x, y + 52, menuWidth);

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

        graphics.setColor(TEXT_SOFT);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 21));
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
        graphics.setColor(new Color(20, 21, 29, 230));
        graphics.fillRoundRect(valueBox.x, valueBox.y, valueBox.width, valueBox.height, 8, 8);
        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(LABEL_FONT);
        String value = Integer.toString(getTargetFramesPerSecond());
        int textWidth = graphics.getFontMetrics().stringWidth(value);
        graphics.drawString(value, valueBox.x + (valueBox.width - textWidth) / 2, valueBox.y + 20);
        drawSmallButton(graphics, increaseButton, ">");
    }

    private void drawSmallButton(Graphics2D graphics, Rectangle bounds, String text) {
        boolean hovered = containsPoint(mouseX, mouseY, bounds);
        graphics.setColor(hovered ? new Color(104, 85, 83) : new Color(54, 52, 63));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(hovered ? GOLD_LIGHT : GOLD);
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(TEXT_SOFT);
        graphics.setFont(LABEL_FONT);
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, bounds.x + (bounds.width - textWidth) / 2, bounds.y + 20);
    }

    private void drawMenuButton(Graphics2D graphics, Rectangle bounds, String text) {
        boolean hovered = containsPoint(mouseX, mouseY, bounds);
        boolean pressed = hovered && mouseDown;
        int yOffset = pressed ? 2 : 0;

        Graphics2D buttonGraphics = (Graphics2D) graphics.create();
        buttonGraphics.setPaint(new GradientPaint(bounds.x, bounds.y,
                hovered ? BUTTON_HOVER_TOP : BUTTON_TOP,
                bounds.x, bounds.y + bounds.height,
                hovered ? BUTTON_HOVER_BOTTOM : BUTTON_BOTTOM));
        buttonGraphics.fillRoundRect(bounds.x, bounds.y + yOffset, bounds.width, bounds.height, 12, 12);
        buttonGraphics.setStroke(new BasicStroke(hovered ? 2f : 1f));
        buttonGraphics.setColor(hovered ? GOLD_LIGHT : GOLD);
        buttonGraphics.drawRoundRect(bounds.x, bounds.y + yOffset, bounds.width, bounds.height, 12, 12);
        buttonGraphics.dispose();

        graphics.setColor(pressed ? new Color(235, 220, 190) : TEXT_SOFT);
        graphics.setFont(BUTTON_FONT);
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, bounds.x + (bounds.width - textWidth) / 2,
                bounds.y + yOffset + bounds.height / 2 + 7);
    }

    private void drawToggleButton(Graphics2D graphics, int x, int y, int width, int height, boolean enabled) {
        Rectangle bounds = new Rectangle(x, y, width, height);
        boolean hovered = containsPoint(mouseX, mouseY, bounds);
        graphics.setColor(enabled ? new Color(86, 168, 104) : new Color(91, 88, 97));
        graphics.fillRoundRect(x, y, width, height, 12, 12);
        graphics.setColor(hovered ? GOLD_LIGHT : new Color(205, 199, 190));
        graphics.drawRoundRect(x, y, width, height, 12, 12);
        graphics.setColor(new Color(238, 234, 222));
        graphics.fillOval(enabled ? x + width - 15 : x + 3, y + 3, height - 6, height - 6);
    }

    private void drawSettingsFrame(Graphics2D graphics) {
        Rectangle bounds = getSettingsFrameBounds();
        drawPanel(graphics, bounds, 18);
        drawCenteredString(graphics, "Settings", HEADER_FONT, GOLD_LIGHT,
                bounds.x, bounds.y + 52, bounds.width);
        drawSettingsMenu(graphics, bounds.x, bounds.y);
    }

    private Rectangle getSettingsFrameBounds() {
        int menuWidth = 340;
        int menuHeight = 260;
        return new Rectangle((getWidth() - menuWidth) / 2,
                (getHeight() - menuHeight) / 2, menuWidth, menuHeight);
    }

    private void drawDimOverlay(Graphics2D graphics, int alpha) {
        graphics.setColor(new Color(0, 0, 0, alpha));
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawVignette(Graphics2D graphics) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        graphics.setColor(new Color(0, 0, 0, 82));
        graphics.fillRect(0, 0, panelWidth, panelHeight);
        graphics.setColor(new Color(0, 0, 0, 90));
        graphics.fillRect(0, 0, panelWidth, 74);
        graphics.fillRect(0, panelHeight - 74, panelWidth, 74);
    }

    private void drawPanel(Graphics2D graphics, Rectangle bounds, int arc) {
        Graphics2D panelGraphics = (Graphics2D) graphics.create();
        panelGraphics.setPaint(new GradientPaint(bounds.x, bounds.y, PANEL_MID,
                bounds.x, bounds.y + bounds.height, PANEL_DARK));
        panelGraphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
        panelGraphics.setStroke(new BasicStroke(2f));
        panelGraphics.setColor(GOLD);
        panelGraphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
        panelGraphics.setStroke(new BasicStroke(1f));
        panelGraphics.setColor(new Color(255, 255, 255, 38));
        panelGraphics.drawRoundRect(bounds.x + 5, bounds.y + 5,
                bounds.width - 10, bounds.height - 10, Math.max(arc - 6, 6), Math.max(arc - 6, 6));
        panelGraphics.dispose();
    }

    private void drawCenteredText(Graphics2D graphics, String text, Font font, Color color, int x, int y) {
        drawCenteredString(graphics, text, font, color, x, y, PANEL_WIDTH);
    }

    private void drawCenteredString(Graphics2D graphics, String text, int x, int y, int width) {
        drawCenteredString(graphics, text, graphics.getFont(), graphics.getColor(), x, y, width);
    }

    private void drawCenteredString(Graphics2D graphics, String text, Font font,
            Color color, int x, int y, int width) {
        graphics.setFont(font);
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.drawString(text, x + (width - textWidth) / 2 + 2, y + 2);
        graphics.setColor(color);
        graphics.drawString(text, x + (width - textWidth) / 2, y);
    }

    private void drawResourceBar(Graphics2D graphics, int x, int y, int width, int height,
            double ratio, Color fillColor, Color emptyColor, String label) {
        double clampedRatio = Math.max(0.0, Math.min(1.0, ratio));
        int filledWidth = (int) Math.round(width * clampedRatio);
        graphics.setColor(new Color(0, 0, 0, 145));
        graphics.fillRoundRect(x - 2, y - 2, width + 4, height + 4, 10, 10);
        graphics.setColor(emptyColor);
        graphics.fillRoundRect(x, y, width, height, 8, 8);
        graphics.setColor(fillColor);
        graphics.fillRoundRect(x, y, filledWidth, height, 8, 8);
        graphics.setColor(new Color(255, 255, 255, 70));
        graphics.fillRoundRect(x, y, filledWidth, Math.max(2, height / 2), 8, 8);
        graphics.setColor(GOLD);
        graphics.drawRoundRect(x, y, width, height, 8, 8);
        if (label != null && !label.isEmpty()) {
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 11));
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.setColor(new Color(0, 0, 0, 165));
            graphics.drawString(label, x + (width - textWidth) / 2 + 1, y + height - 3);
            graphics.setColor(TEXT_SOFT);
            graphics.drawString(label, x + (width - textWidth) / 2, y + height - 4);
        }
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
        int width = 220;
        return new Rectangle((getWidth() - width) / 2, getHeight() / 2 + 20, width, 46);
    }

    private Rectangle getGameOverMainMenuButtonBounds() {
        int width = 220;
        return new Rectangle((getWidth() - width) / 2, getHeight() / 2 + 82, width, 46);
    }

    private void handlePauseMenuClick(MouseEvent event) {
        if (gameLogic.isSettingsOpen()) {
            Rectangle settingsFrame = getSettingsFrameBounds();
            int x = settingsFrame.x;
            int y = settingsFrame.y;
            handleSettingsClick(event, x, y);
            return;
        }

        Rectangle pauseFrame = getSettingsFrameBounds();
        int x = pauseFrame.x;
        int y = pauseFrame.y;
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
        if (gameLogic.isSettingsOpen()) {
            Rectangle settingsFrame = getSettingsFrameBounds();
            int x = settingsFrame.x;
            int y = settingsFrame.y;
            handleSettingsClick(event, x, y);
            return;
        }

        if (contains(event, getMainMenuPlayButtonBounds())) {
            gameLogic.showCharacterSelection();
            return;
        }
        if (contains(event, getMainMenuSettingsButtonBounds())) {
            gameLogic.toggleSettings();
            return;
        }
        if (contains(event, getMainMenuQuitButtonBounds())) {
            System.exit(0);
        }
    }

    private void handleSettingsClick(MouseEvent event, int x, int y) {
        int left = x + 52;
        int start = y + 78;
        int rowHeight = 42;
        Rectangle backButton = new Rectangle(left + 160, start + rowHeight * 3 - 18, 90, 32);
        Rectangle fpsToggle = new Rectangle(left + 180, start - 10, 36, 24);
        Rectangle fpsDecreaseButton = new Rectangle(left + 142, start - 12 + rowHeight, 26, 28);
        Rectangle fpsIncreaseButton = new Rectangle(left + 236, start - 12 + rowHeight, 26, 28);
        Rectangle soundToggle = new Rectangle(left + 180, start - 10 + rowHeight * 2, 36, 24);

        if (contains(event, backButton)) {
            gameLogic.toggleSettings();
            return;
        }
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
        }
    }

    private void handleCharacterSelectionClick(MouseEvent event) {
        int characterCount = gameLogic.getCharacterCount();

        for (int index = 0; index < characterCount; index++) {
            Rectangle hitBox = getCharacterCardBounds(index);
            if (contains(event, hitBox) && gameLogic.isCharacterSelectable(index)) {
                gameLogic.selectCharacter(index);
                repaint();
                return;
            }
        }

        Rectangle startButton = getCharacterStartButtonBounds();
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
        boolean hovered = containsPoint(mouseX, mouseY, bounds);
        graphics.setColor(hovered ? new Color(45, 42, 55, 238) : new Color(18, 20, 28, 225));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(hovered ? GOLD_LIGHT : new Color(155, 146, 139));
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        if (ability != null) {
            drawAbilityIcon(graphics, bounds, ability,
                    ability.getDefinition().isUnlockedAt(playerLevel), playerLevel);
            if (showCooldown) {
                drawCooldownOverlay(graphics, bounds, ability);
            }
            graphics.setColor(TEXT_SOFT);
            graphics.setFont(new Font("Times New Roman", Font.BOLD, 11));
            graphics.drawString(String.valueOf((int) ability.getDefinition().getManaCost()),
                    bounds.x + 3, bounds.y + bounds.height - 4);
        }

        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 12));
        graphics.drawString(String.valueOf(shortcut), bounds.x + bounds.width - 10, bounds.y + 13);
    }

    private void drawAbilityIcon(Graphics2D graphics, Rectangle bounds,
            RpgAbility ability, boolean unlocked, int playerLevel) {
        drawImageInside(graphics, ability.getIcon(),
                new Rectangle(bounds.x + 4, bounds.y + 4,
                        bounds.width - 8, bounds.height - 8), true);
        if (!unlocked) {
            graphics.setColor(new Color(0, 0, 0, 170));
            graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            graphics.setColor(GOLD_LIGHT);
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
        graphics.setColor(TEXT_SOFT);
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

        drawPanel(graphics, new Rectangle(20, 24, PANEL_WIDTH - 40, PANEL_HEIGHT - 48), 18);
        graphics.setColor(GOLD_LIGHT);
        graphics.setFont(new Font("Times New Roman", Font.BOLD, 28));
        graphics.drawString("Skills", 42, 62);

        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        graphics.setColor(TEXT_SOFT);
        graphics.drawString("Select a slot, then choose an unlocked ability.", 116, 53);

        RpgAbility[] equipped = gameLogic.getAbilityManager().getEquippedAbilities();
        for (int index = 0; index < equipped.length; index++) {
            Rectangle bounds = getSkillMenuSlotBounds(index);
            if (index == gameLogic.getAbilityManager().getSelectedEquipSlot()) {
                graphics.setColor(new Color(255, 220, 120, 150));
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
            graphics.setColor(GOLD_LIGHT);
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
                graphics.setColor(unlocked ? TEXT_SOFT : TEXT_MUTED);
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
        drawPanel(graphics, new Rectangle(x, y, 220, 86), 8);
        graphics.setFont(LABEL_FONT);
        graphics.setColor(GOLD_LIGHT);
        graphics.drawString(definition.getName(), x + 10, y + 20);
        graphics.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        graphics.setColor(TEXT_SOFT);
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

    private void drawCenteredClippedString(Graphics2D graphics, String text,
            int x, int y, int maxWidth) {
        String clipped = text;
        while (graphics.getFontMetrics().stringWidth(clipped) > maxWidth && clipped.length() > 3) {
            clipped = clipped.substring(0, clipped.length() - 4) + "...";
        }
        int textWidth = graphics.getFontMetrics().stringWidth(clipped);
        graphics.drawString(clipped, x + (maxWidth - textWidth) / 2, y);
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
        graphics.setFont(LABEL_FONT);
        int textWidth = graphics.getFontMetrics().stringWidth(fpsText);
        int boxWidth = textWidth + 16;
        int boxX = PANEL_WIDTH - boxWidth - 12;
        int boxY = 34;
        graphics.setColor(new Color(0, 0, 0, 170));
        graphics.fillRoundRect(boxX, boxY, boxWidth, 22, 8, 8);
        graphics.setColor(GOLD_LIGHT);
        graphics.drawString(fpsText, boxX + 8, boxY + 15);
    }
}
