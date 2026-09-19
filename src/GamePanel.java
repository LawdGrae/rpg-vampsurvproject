import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

    private final BufferedImage grassTile;
    private final GameLogic gameLogic;
    private long lastUpdateNanos = System.nanoTime();
    private double lastDeltaTime;
    private double framesPerSecond;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        grassTile = loadGrassTile();
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

            if (!gameLogic.isUpgradeMenuOpen()) {
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
                if (!gameLogic.isUpgradeMenuOpen()) {
                    return;
                }

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
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics;

        int tileWidth = grassTile.getWidth();
        int tileHeight = grassTile.getHeight();
        int startX = Math.floorMod((int) gameLogic.getWorldOffsetX(), tileWidth) - tileWidth;
        int startY = Math.floorMod((int) gameLogic.getWorldOffsetY(), tileHeight) - tileHeight;

        // Repeat the tile so the world appears infinite as the camera moves.
        for (int tileX = startX; tileX < PANEL_WIDTH; tileX += tileWidth) {
            for (int tileY = startY; tileY < PANEL_HEIGHT; tileY += tileHeight) {
                graphics2D.drawImage(grassTile, tileX, tileY, null);
            }
        }

        gameLogic.drawEntities(graphics2D, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        drawExperienceBar(graphics2D);
        drawGameTimer(graphics2D);
        drawUpgradeMenu(graphics2D);
        drawDebugInfo(graphics2D);
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
        graphics.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        graphics.setFont(new Font("SansSerif", Font.BOLD, 20));
        graphics.drawString("Choose an upgrade", left + 10, top - 20);

        for (int index = 0; index < 3; index++) {
            int x = left + index * (cardWidth + gap);
            int y = top;
            graphics.setColor(new Color(60, 60, 60));
            graphics.fillRoundRect(x, y, cardWidth, cardHeight, 12, 12);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
            graphics.drawString(gameLogic.getUpgradeChoices().get(index), x + 12, y + 28);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 12));
            graphics.drawString("(placeholder)", x + 12, y + 48);
            graphics.drawString("+ small bonus", x + 12, y + 70);
        }
    }

    private void drawDebugInfo(Graphics2D graphics) {
        if (!DEBUG_ENABLED) {
            return;
        }

        gameLogic.drawCollisionAreas(graphics, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);

        // Add future debug values in this method so they stay together.
        graphics.setColor(Color.WHITE);
        graphics.drawString(String.format("FPS: %.1f", framesPerSecond), 10, 20);
        graphics.drawString(String.format("Delta time: %.4f s", lastDeltaTime), 10, 38);
        graphics.drawString(String.format("World X: %.1f", gameLogic.getPlayerWorldX()), 10, 56);
        graphics.drawString(String.format("World Y: %.1f", gameLogic.getPlayerWorldY()), 10, 74);
        graphics.drawString("Enemies: " + gameLogic.getEnemyCount(), 10, 92);
    }
}