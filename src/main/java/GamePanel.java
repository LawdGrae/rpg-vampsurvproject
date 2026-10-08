import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
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
    private static final int[] FPS_OPTIONS = {30, 45, 60, 90, 120};
    private final BufferedImage grassTile;
    private final ArenaMinimap minimap;
    private final GameLogic gameLogic;
    private final GameMenus menus;
    private final Timer frameTimer;
    private final Map<Integer, Player> selectionPlayers = new HashMap<>();
    private final Map<String, String> heldMovementBindings = new HashMap<>();
    private long lastUpdateNanos = System.nanoTime();
    private double lastDeltaTime;
    private double framesPerSecond;
    private int selectedFpsIndex = 2;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean mouseDown;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(GameUiTheme.BACKGROUND);
        grassTile = ArenaLighting.brighten(ResourceLoader.loadImage("/main/resources/grasstile.png"));
        minimap = new ArenaMinimap(grassTile);
        BufferedImage landscape = ResourceLoader.loadImage("/main/resources/landscape.png");
        gameLogic = new GameLogic();
        menus = new GameMenus(gameLogic, landscape, selectionPlayers,
                this::getTargetFramesPerSecond, this::adjustTargetFramesPerSecond);
        installKeyBindings();
        installMouseHandling();
        frameTimer = new Timer(getFrameDelayMillis(), event -> {
            long now = System.nanoTime();
            double deltaTime = Math.min((now - lastUpdateNanos) / 1_000_000_000.0, 0.1);
            lastUpdateNanos = now;
            lastDeltaTime = deltaTime;
            double instantFps = deltaTime > 0.0 ? 1.0 / deltaTime : 0.0;
            framesPerSecond += (instantFps - framesPerSecond) * 0.1;
            menus.update(deltaTime);
            if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen()
                    && !gameLogic.isPaused() && !gameLogic.isSkillMenuOpen()) {
                gameLogic.update(deltaTime);
            }
            if (gameLogic.isMainMenuOpen() || gameLogic.isCharacterSelectOpen()) {
                for (Player preview : selectionPlayers.values()) preview.updatePreview(deltaTime);
            }
            updateCursor();
            repaint();
        });
        frameTimer.start();
    }

    public void releaseInputState() {
        heldMovementBindings.clear();
        for (String direction : new String[] { "up", "down", "left", "right", "run", "attack", "jump" }) {
            gameLogic.setKeyPressed(direction, false);
        }
        mouseDown = false;
        mouseX = -1;
        mouseY = -1;
        menus.pointer(-1, -1, false);
        updateCursor();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        lastUpdateNanos = System.nanoTime();
        frameTimer.start();
    }

    @Override
    public void removeNotify() {
        frameTimer.stop();
        releaseInputState();
        super.removeNotify();
    }

    private void installKeyBindings() {
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
        bindKey(inputMap, actionMap, "pressed R", "run", true);
        bindKey(inputMap, actionMap, "released R", "run", false);
        bindKey(inputMap, actionMap, "pressed J", "attack", true);
        bindKey(inputMap, actionMap, "released J", "attack", false);
        inputMap.put(KeyStroke.getKeyStroke("pressed SPACE"), "spacePressed");
        actionMap.put("spacePressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                gameLogic.setKeyPressed("jump", true);
                if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen()
                        && !gameLogic.isPaused() && !gameLogic.isSkillMenuOpen()) gameLogic.triggerAbility();
            }
        });
        bindKey(inputMap, actionMap, "released SPACE", "jump", false);
        for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
            final int slot = index;
            inputMap.put(KeyStroke.getKeyStroke("pressed " + (index + 1)), "ability" + index);
            actionMap.put("ability" + index, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    if (!gameLogic.isSkillMenuOpen()) gameLogic.triggerAbility(slot);
                }
            });
        }
        inputMap.put(KeyStroke.getKeyStroke("pressed K"), "toggleSkillMenu");
        actionMap.put("toggleSkillMenu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (!gameLogic.isPaused()) menus.toggleSkills();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "togglePause");
        actionMap.put("togglePause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (gameLogic.isSettingsOpen()) gameLogic.toggleSettings();
                else if (gameLogic.isCharacterSelectOpen()) gameLogic.showMainMenu();
                else if (gameLogic.isSkillMenuOpen()) gameLogic.toggleSkillMenu();
                else if (gameLogic.isGameStarted() && !gameLogic.isUpgradeMenuOpen() && !gameLogic.isGameOver()) {
                    if (gameLogic.isPaused()) gameLogic.resume(); else gameLogic.togglePause();
                }
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "confirmMenu");
        actionMap.put("confirmMenu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (gameLogic.isSettingsOpen()) return;
                if (gameLogic.isMainMenuOpen()) gameLogic.showCharacterSelection();
                else if (gameLogic.isCharacterSelectOpen() || gameLogic.isGameOver()) gameLogic.startGame();
                else if (gameLogic.isPaused()) gameLogic.resume();
            }
        });
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, String key,
            String direction, boolean pressed) {
        inputMap.put(KeyStroke.getKeyStroke(key), key);
        actionMap.put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                String physicalKey = key.substring(key.indexOf(' ') + 1);
                if (pressed) {
                    heldMovementBindings.put(physicalKey, direction);
                } else {
                    heldMovementBindings.remove(physicalKey);
                }
                gameLogic.setKeyPressed(direction, heldMovementBindings.containsValue(direction));
            }
        });
    }

    private void installMouseHandling() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                updatePointer(event, true);
                if (!menus.click(mouseX, mouseY) && gameLogic.isGameStarted()) {
                    for (int index = 0; index < AbilityManager.EQUIPPED_SLOT_COUNT; index++) {
                        if (GameHud.abilityBounds(index, PANEL_WIDTH, PANEL_HEIGHT).contains(mouseX, mouseY)) {
                            gameLogic.triggerAbility(index);
                            break;
                        }
                    }
                }
                updateCursor();
                repaint();
            }
            @Override public void mouseReleased(MouseEvent event) {
                updatePointer(event, false);
                repaint();
            }
            @Override public void mouseExited(MouseEvent event) {
                mouseX = -1; mouseY = -1; mouseDown = false;
                menus.pointer(-1, -1, false);
                updateCursor();
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { updatePointer(event, mouseDown); }
            @Override public void mouseDragged(MouseEvent event) { updatePointer(event, mouseDown); }
        });
    }

    private void updatePointer(MouseEvent event, boolean down) {
        double scale = viewportScale();
        mouseX = (int) Math.floor((event.getX() - viewportX(scale)) / scale);
        mouseY = (int) Math.floor((event.getY() - viewportY(scale)) / scale);
        mouseDown = down;
        menus.pointer(mouseX, mouseY, down);
        updateCursor();
    }

    private void updateCursor() {
        int type = menus.interactive(mouseX, mouseY) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR;
        if (getCursor().getType() != type) setCursor(Cursor.getPredefinedCursor(type));
    }

    private double viewportScale() {
        return Math.max(0.001, Math.min(getWidth() / (double) PANEL_WIDTH, getHeight() / (double) PANEL_HEIGHT));
    }
    private double viewportX(double scale) { return (getWidth() - PANEL_WIDTH * scale) / 2.0; }
    private double viewportY(double scale) { return (getHeight() - PANEL_HEIGHT * scale) / 2.0; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = viewportScale();
        g.translate(viewportX(scale), viewportY(scale));
        g.scale(scale, scale);
        g.clipRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            if (gameLogic.isMainMenuOpen()) { menus.drawMain(g); return; }
            if (gameLogic.isCharacterSelectOpen()) { menus.drawSelection(g); return; }
            drawWorld(g);
            GameHud.draw(g, gameLogic, PANEL_WIDTH, PANEL_HEIGHT, menus.time(), mouseX, mouseY);
            minimap.draw(g, gameLogic, PANEL_WIDTH, PANEL_HEIGHT);
            if (gameLogic.isGameOver()) gameLogic.drawGameOverEffect(g, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
            menus.drawOverlays(g);
            if (gameLogic.isDebugInfoVisible() && !gameLogic.isPaused()
                    && !gameLogic.isSkillMenuOpen() && !gameLogic.isUpgradeMenuOpen()) drawDebugInfo(g);
        } finally {
            g.dispose();
        }
    }

    private void drawWorld(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(gameLogic.getScreenShakeOffsetX(), gameLogic.getScreenShakeOffsetY());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int tileWidth = grassTile.getWidth();
        int tileHeight = grassTile.getHeight();
        // Share the tile origin with collision, including negative world coordinates.
        int tileOriginX = PANEL_WIDTH / 2 - TerrainLayout.SPAWN_TILE_X;
        int tileOriginY = PANEL_HEIGHT / 2 - TerrainLayout.SPAWN_TILE_Y;
        int startX = Math.floorMod((int) Math.floor(gameLogic.getWorldOffsetX()) + tileOriginX,
                tileWidth) - tileWidth;
        int startY = Math.floorMod((int) Math.floor(gameLogic.getWorldOffsetY()) + tileOriginY,
                tileHeight) - tileHeight;
        for (int x = startX; x < PANEL_WIDTH; x += tileWidth) {
            for (int y = startY; y < PANEL_HEIGHT; y += tileHeight) g.drawImage(grassTile, x, y, null);
        }
        gameLogic.drawAbilityGroundEffects(g, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        gameLogic.drawEntities(g, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        gameLogic.drawAbilityBursts(g, PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        g.dispose();
    }

    private int getTargetFramesPerSecond() { return FPS_OPTIONS[selectedFpsIndex]; }
    private int getFrameDelayMillis() { return Math.max(1, Math.round(1000.0f / getTargetFramesPerSecond())); }

    private void adjustTargetFramesPerSecond(int direction) {
        int next = Math.max(0, Math.min(FPS_OPTIONS.length - 1, selectedFpsIndex + direction));
        if (next == selectedFpsIndex) return;
        selectedFpsIndex = next;
        frameTimer.setDelay(getFrameDelayMillis());
        frameTimer.setInitialDelay(getFrameDelayMillis());
        lastUpdateNanos = System.nanoTime();
    }

    private void drawDebugInfo(Graphics2D g) {
        Rectangle r = new Rectangle(536, 20, 208, 55);
        GameUiTheme.panel(g, r, 8);
        GameUiTheme.text(g, String.format("%.0f FPS   /   %d TARGET", framesPerSecond, getTargetFramesPerSecond()),
                GameUiTheme.numeric(12), GameUiTheme.GOLD, r.x + 14, r.y + 22);
        GameUiTheme.text(g, String.format("%.1f ms  ·  %d enemies", lastDeltaTime * 1000.0, gameLogic.getEnemyCount()),
                GameUiTheme.body(11), GameUiTheme.MUTED, r.x + 14, r.y + 41);
    }
}
