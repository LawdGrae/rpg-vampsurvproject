import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Menu presentation and its matching hit regions, in the game's 1280 x 720 canvas. */
final class GameMenus {
    private static final Color GOLD = GameUiTheme.GOLD;
    private static final Color INK = GameUiTheme.IVORY;
    private static final Color MUTED = GameUiTheme.MUTED;
    private static final Color[] HERO_COLORS = {
        new Color(229, 121, 84), new Color(173, 135, 240),
        new Color(228, 193, 117), new Color(130, 161, 239),
        new Color(224, 180, 86)
    };
    private static final String[] HERO_COPY = {
        "Hold the front line. Break through with steel and fire.",
        "Close the distance. Chain precise strikes from the shadows.",
        "Restore your strength. Turn holy light into protection.",
        "Command fire, ice and lightning. Control the battlefield.",
        "Stand unbroken. Protect with steel, command with force."
    };
    private final GameLogic logic;
    private final BufferedImage landscape;
    private final BufferedImage mainMenuArtwork;
    private final Map<Integer, Player> previews;
    private final IntSupplier targetFps;
    private final IntConsumer adjustFps;
    private final Map<Rectangle, Double> hover = new HashMap<>();
    private final double[] selection;
    private double time;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean pressed;
    private int skillClass;

    GameMenus(GameLogic logic, BufferedImage landscape, Map<Integer, Player> previews,
            IntSupplier targetFps, IntConsumer adjustFps) {
        this.logic = logic;
        this.selection = new double[logic.getCharacterCount()];
        this.selection[logic.getSelectedCharacterIndex()] = 1.0;
        this.landscape = landscape;
        this.mainMenuArtwork = ResourceLoader.loadImage("/main/resources/ui/venoria_main_menu_background.png");
        this.previews = previews;
        this.targetFps = targetFps;
        this.adjustFps = adjustFps;
    }

    double time() { return time; }

    void pointer(int x, int y, boolean down) {
        mouseX = x; mouseY = y; pressed = down;
    }

    void update(double dt) {
        time = (time + dt) % 3600.0;
        double ease = 1.0 - Math.exp(-13.0 * dt);
        hover.replaceAll((bounds, value) -> value + ((bounds.contains(mouseX, mouseY) ? 1.0 : 0.0) - value) * ease);
        for (int index = 0; index < selection.length; index++) {
            selection[index] += ((logic.getSelectedCharacterIndex() == index ? 1.0 : 0.0) - selection[index]) * ease;
        }
    }

    void toggleSkills() {
        if (!logic.isSkillMenuOpen()) skillClass = logic.getCharacterAbilityClass(logic.getSelectedCharacterIndex()).ordinal();
        logic.toggleSkillMenu();
    }

    private double hover(Rectangle bounds) {
        return hover.computeIfAbsent(new Rectangle(bounds), key -> key.contains(mouseX, mouseY) ? 1.0 : 0.0);
    }

    private void button(Graphics2D g, Rectangle bounds, String caption, boolean primary) {
        GameUiTheme.button(g, bounds, caption, hover(bounds), pressed && bounds.contains(mouseX, mouseY), primary);
    }

    private void text(Graphics2D g, String value, Font font, Color color, int x, int y) {
        GameUiTheme.text(g, value, font, color, x, y);
    }

    private void centered(Graphics2D g, String value, Font font, Color color, int x, int y, int width) {
        g.setFont(font);
        text(g, value, font, color, x + (width - g.getFontMetrics().stringWidth(value)) / 2, y);
    }

    private void background(Graphics2D g) {
        Graphics2D bg = (Graphics2D) g.create();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        bg.drawImage(landscape, 0, 0, 1280, 720, null);
        bg.setPaint(new GradientPaint(0, 0, new Color(8, 14, 28, 192), 0, 720, new Color(7, 11, 19, 248)));
        bg.fillRect(0, 0, 1280, 720);
        GameUiTheme.glow(bg, 1000, 245, 450, new Color(80, 107, 162), 0.13);
        GameUiTheme.glow(bg, 220, 610, 400, GOLD, 0.035);
        for (int index = 0; index < 34; index++) {
            double x = (index * 173.7 + Math.sin(time * 0.18 + index) * 10) % 1280;
            double y = (index * 93.1 - time * (2.5 + index % 3)) % 720;
            if (y < 0) y += 720;
            bg.setColor(new Color(226, 218, 192, 15 + index % 4 * 8));
            double size = index % 5 == 0 ? 2.0 : 1.0;
            bg.fill(new Ellipse2D.Double(x, y, size, size));
        }
        bg.dispose();
    }

    void drawMain(Graphics2D g) {
        FantasyMainMenu.drawBackgroundAndTitle(g, mainMenuArtwork, time);
        for (int index = 0; index < 3; index++) {
            Rectangle bounds = mainButton(index);
            FantasyMainMenu.drawButton(g, index, hover(bounds), pressed && bounds.contains(mouseX, mouseY));
        }
        if (logic.isSettingsOpen()) {
            dim(g);
            drawSettings(g);
        }
    }

    Rectangle mainButton(int row) { return FantasyMainMenu.buttonBounds(row); }
    Rectangle backBounds() { return new Rectangle(48, 40, 106, 38); }
    Rectangle heroBounds(int index) {
        int stride = 1020 / logic.getCharacterCount();
        return new Rectangle(136 + index * stride, 130, stride - 12, 42);
    }
    Rectangle startBounds() { return new Rectangle(494, 502, 292, 48); }
    Rectangle selectedSkillBounds(int index) {
        boolean lower = index % 2 == 1;
        int x = index < 2 ? (lower ? 242 : 284) : (lower ? 910 : 868);
        return new Rectangle(x, lower ? 365 : 194, 128, 164);
    }
    Rectangle selectedPassiveBounds() { return new Rectangle(610, 411, 60, 60); }

    void drawSelection(Graphics2D g) {
        background(g);
        GameUiTheme.glow(g, 640, 310, 380, GOLD, 0.13);
        GameUiTheme.glow(g, 640, 548, 220, new Color(153, 49, 37), 0.13);
        button(g, backBounds(), "←  Back", false);
        text(g, "THE HERO ROSTER", GameUiTheme.label(10), GOLD, 1119, 54);
        text(g, "Choose your champion", GameUiTheme.body(11), MUTED, 1119, 73);
        selectionFrame(g, new Rectangle(414, 24, 452, 62), 0.5, false);
        centered(g, logic.getCharacterClassName(logic.getSelectedCharacterIndex()).toUpperCase(),
                GameUiTheme.title(32), INK, 414, 66, 452);
        g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 90));
        g.drawLine(300, 55, 402, 55);
        g.drawLine(878, 55, 980, 55);
        GameUiTheme.emblem(g, 640, 108, 28, GOLD);
        for (int index = 0; index < logic.getCharacterCount(); index++) drawClassChoice(g, index);
        drawSelectedHero(g);
        text(g, "Click a class to inspect  ·  Hover over a skill for details", GameUiTheme.body(11), MUTED, 48, 712);
        text(g, "ENTER  TO BEGIN     ·     ESC  BACK", GameUiTheme.label(10), MUTED, 1042, 712);
        drawTooltip(g);
    }

    private void drawClassChoice(Graphics2D graphics, int index) {
        Rectangle r = heroBounds(index);
        Graphics2D g = (Graphics2D) graphics.create();
        Color accent = HERO_COLORS[index];
        double focus = Math.max(selection[index], hover(r) * 0.52);
        selectionFrame(g, r, focus, false);
        centered(g, logic.getCharacterClassName(index).toUpperCase(), GameUiTheme.label(10),
                focus > 0.4 ? accent : MUTED, r.x, r.y + 15, r.width);
        centered(g, logic.getCharacterNames().get(index), GameUiTheme.title(16),
                focus > 0.4 ? INK : MUTED, r.x, r.y + 33, r.width);
        if (logic.getSelectedCharacterIndex() == index) {
            selectionDiamond(g, r.x + 16, r.y + 21, 4, GOLD);
            selectionDiamond(g, r.x + r.width - 16, r.y + 21, 4, GOLD);
        }
        g.dispose();
    }

    private void drawStage(Graphics2D g, int x, int y, int radius, Color accent, double alpha) {
        GameUiTheme.glow(g, x, y - radius * 0.45, radius * 1.4, accent, alpha * 0.13);
        Graphics2D stage = (Graphics2D) g.create();
        stage.setStroke(new BasicStroke(1f));
        stage.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (45 * alpha)));
        stage.draw(new Ellipse2D.Double(x - radius, y - radius * 1.6, radius * 2, radius * 2));
        stage.draw(new Ellipse2D.Double(x - radius * 0.79, y - radius * 1.39, radius * 1.58, radius * 1.58));
        stage.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (110 * alpha)));
        stage.draw(new Ellipse2D.Double(x - radius * 0.75, y - 10, radius * 1.5, 26));
        GameUiTheme.emblem(stage, x, (int) Math.round(y - radius * 0.60), radius * 2, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 17));
        stage.dispose();
    }

    private void portrait(Graphics2D g, int index, Rectangle bounds) {
        Player player = previews.computeIfAbsent(index, key -> switch (key) {
            case 0 -> new Character_Eumann(); case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin(); case 3 -> new Character_Ziea();
            case 4 -> new Character_Sir_Rakki();
            default -> throw new IllegalArgumentException("Unknown hero " + key);
        });
        player.drawPreview(g, bounds);
    }

    private void drawSelectedHero(Graphics2D g) {
        int selected = logic.getSelectedCharacterIndex();
        Color accent = HERO_COLORS[selected];
        drawStage(g, 640, 405, 154, GOLD, 0.8);
        centered(g, logic.getSelectedCharacterName(), GameUiTheme.title(23), INK, 440, 202, 400);
        portrait(g, selected, new Rectangle(496, 207, 288, 204));

        List<RpgAbility> abilities = logic.getCharacterActiveAbilities(selected);
        for (int index = 0; index < Math.min(4, abilities.size()); index++) {
            Rectangle r = selectedSkillBounds(index);
            RpgAbility ability = abilities.get(index);
            selectionIcon(g, ability, new Rectangle(r.x, r.y, r.width, r.width), hover(r), accent);
            centered(g, Integer.toString(index + 1), GameUiTheme.numeric(10), GOLD, r.x + 4, r.y + 17, 20);
            selectionCaption(g, ability.getName(), GameUiTheme.title(15), INK, r.x - 12, r.y + 148, r.width + 24);
            centered(g, (int) ability.getDefinition().getManaCost() + " MP  ·  "
                    + (int) ability.getDefinition().getCooldownSeconds() + "s", GameUiTheme.numeric(10), MUTED,
                    r.x - 12, r.y + 164, r.width + 24);
        }
        RpgAbility passive = logic.getCharacterPassiveAbility(selected);
        if (passive != null) {
            Rectangle r = selectedPassiveBounds();
            selectionIcon(g, passive, r, hover(r), accent);
            centered(g, "PASSIVE", GameUiTheme.label(8), GOLD, r.x, r.y + 10, r.width);
            centered(g, passive.getName(), GameUiTheme.body(13), INK, 450, 490, 380);
        }
        Rectangle start = startBounds();
        Graphics2D startGraphics = (Graphics2D) g.create();
        if (pressed && start.contains(mouseX, mouseY)) startGraphics.translate(0, 1);
        selectionFrame(startGraphics, start, hover(start), true);
        centered(startGraphics, "START", GameUiTheme.title(28), INK, start.x, start.y + 34, start.width);
        selectionDiamond(startGraphics, start.x + 24, start.y + 24, 5, GOLD);
        selectionDiamond(startGraphics, start.x + start.width - 24, start.y + 24, 5, GOLD);
        startGraphics.dispose();

        selectionFrame(g, new Rectangle(48, 568, 880, 120), 0.25, false);
        GameUiTheme.emblem(g, 130, 628, 104, new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 75));
        text(g, "CLASS DESCRIPTION", GameUiTheme.label(10), GOLD, 216, 592);
        wrapped(g, HERO_COPY[selected], GameUiTheme.title(22), INK, 216, 623, 680, 27, 2);
        text(g, logic.getCharacterRole(selected), GameUiTheme.body(12), MUTED, 216, 670);
        String weapon = logic.getCharacterWeaponName(selected);
        g.setFont(GameUiTheme.body(12));
        text(g, weapon, GameUiTheme.body(12), accent, 896 - g.getFontMetrics().stringWidth(weapon), 670);

        selectionFrame(g, new Rectangle(952, 568, 280, 120), 0.25, false);
        text(g, "CLASS STATS", GameUiTheme.label(10), GOLD, 974, 592);
        selectionStat(g, "MAX HEALTH", logic.getPlayerMaxHealth(), 620);
        selectionStat(g, "MAX MANA", logic.getAbilityManager().getMaxMana(), 647);
        // Preview speed is the unchanged class value, before gameplay passive bonuses.
        selectionStat(g, "BASE SPEED", previews.get(selected).speed, 674);
    }

    /** Selection-only ornaments; no raster resources or shared menu styling are changed. */
    private void selectionFrame(Graphics2D graphics, Rectangle r, double focus, boolean primary) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (focus > 0.05) GameUiTheme.glow(g, r.getCenterX(), r.getCenterY(),
                Math.max(r.width, r.height) * 0.7, primary ? new Color(195, 63, 43) : GOLD, focus * 0.15);
        g.translate(0, 4);
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(selectionShape(r, 0));
        g.translate(0, -4);
        g.setPaint(new GradientPaint(r.x, r.y,
                primary ? new Color(91 + (int) (focus * 22), 30, 29) : new Color(36, 36, 39),
                r.x, r.y + r.height, primary ? new Color(31, 12, 17) : new Color(10, 13, 20)));
        g.fill(selectionShape(r, 0));
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(44, 31, 21));
        g.draw(selectionShape(r, 1));
        g.setStroke(new BasicStroke(1.3f));
        g.setPaint(new GradientPaint(r.x, r.y, new Color(235, 209, 155, (int) (130 + focus * 110)),
                r.x, r.y + r.height, new Color(147, 108, 59, 170)));
        g.draw(selectionShape(r, 1));
        g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 60 + (int) (focus * 65)));
        g.setStroke(new BasicStroke(1f));
        g.draw(selectionShape(r, 5));
        int reach = Math.min(24, r.height / 3);
        g.setColor(new Color(232, 207, 153, 145 + (int) (focus * 90)));
        for (int corner = 0; corner < 4; corner++) {
            int x = corner % 2 == 0 ? r.x : r.x + r.width;
            int y = corner < 2 ? r.y : r.y + r.height;
            int dx = corner % 2 == 0 ? 1 : -1;
            int dy = corner < 2 ? 1 : -1;
            Path2D trim = new Path2D.Double();
            trim.moveTo(x + dx * 7, y + dy * reach);
            trim.lineTo(x + dx * 7, y + dy * 14);
            trim.lineTo(x + dx * 14, y + dy * 7);
            trim.lineTo(x + dx * reach, y + dy * 7);
            g.draw(trim);
        }
        g.dispose();
    }

    private Path2D selectionShape(Rectangle r, int inset) {
        int x = r.x + inset, y = r.y + inset;
        int right = r.x + r.width - inset, bottom = r.y + r.height - inset;
        int cut = Math.min(12, r.height / 4);
        Path2D shape = new Path2D.Double();
        shape.moveTo(x + cut, y);
        shape.lineTo(right - cut, y);
        shape.lineTo(right, y + cut);
        shape.lineTo(right, bottom - cut);
        shape.lineTo(right - cut, bottom);
        shape.lineTo(x + cut, bottom);
        shape.lineTo(x, bottom - cut);
        shape.lineTo(x, y + cut);
        shape.closePath();
        return shape;
    }

    private void selectionDiamond(Graphics2D g, int x, int y, int radius, Color color) {
        Path2D diamond = new Path2D.Double();
        diamond.moveTo(x, y - radius);
        diamond.lineTo(x + radius, y);
        diamond.lineTo(x, y + radius);
        diamond.lineTo(x - radius, y);
        diamond.closePath();
        g.setColor(color);
        g.draw(diamond);
    }

    private void selectionIcon(Graphics2D g, RpgAbility ability, Rectangle r, double focus, Color accent) {
        GameUiTheme.glow(g, r.getCenterX(), r.getCenterY(), r.width * 0.85, accent, 0.08 + focus * 0.18);
        selectionFrame(g, r, focus, false);
        int inset = r.width > 80 ? 13 : 11;
        image(g, ability.getIcon(), new Rectangle(r.x + inset, r.y + inset, r.width - inset * 2, r.height - inset * 2));
        selectionDiamond(g, (int) r.getCenterX(), r.y, r.width > 80 ? 6 : 4, GOLD);
    }

    private void selectionCaption(Graphics2D g, String value, Font font, Color color, int x, int y, int width) {
        g.setFont(font);
        while (g.getFontMetrics().stringWidth(value) > width && font.getSize2D() > 10) {
            font = font.deriveFont(font.getSize2D() - 0.5f);
            g.setFont(font);
        }
        centered(g, value, font, color, x, y, width);
    }

    private void selectionStat(Graphics2D g, String label, double value, int baseline) {
        text(g, label, GameUiTheme.label(11), MUTED, 974, baseline - 2);
        String number = String.format("%.0f", value);
        g.setFont(GameUiTheme.title(22));
        text(g, number, GameUiTheme.title(22), INK, 1208 - g.getFontMetrics().stringWidth(number), baseline);
        if (baseline < 674) {
            g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 45));
            g.drawLine(974, baseline + 8, 1208, baseline + 8);
        }
    }

    void drawOverlays(Graphics2D g) {
        if (logic.isGameOver()) { dim(g); drawGameOver(g); return; }
        if (logic.isUpgradeMenuOpen()) { dim(g); drawUpgrades(g); return; }
        if (logic.isPaused()) { dim(g); if (logic.isSettingsOpen()) drawSettings(g); else drawPause(g); return; }
        if (logic.isSkillMenuOpen()) { dim(g); drawSkills(g); }
        drawTooltip(g);
    }

    private void dim(Graphics2D g) {
        g.setColor(new Color(4, 8, 16, 213));
        g.fillRect(0, 0, 1280, 720);
    }

    Rectangle pauseBounds() { return new Rectangle(438, 154, 404, 412); }
    Rectangle pauseButton(int index) { return new Rectangle(476, 305 + index * 62, 328, 48); }

    private void drawPause(Graphics2D g) {
        Rectangle r = pauseBounds();
        GameUiTheme.panel(g, r, 16);
        GameUiTheme.emblem(g, 640, 209, 38, GOLD);
        centered(g, "Paused", GameUiTheme.title(38), INK, r.x, 267, r.width);
        centered(g, "Take a breath. Your run will wait.", GameUiTheme.body(13), MUTED, r.x, 289, r.width);
        button(g, pauseButton(0), "Resume adventure", true);
        button(g, pauseButton(1), "Settings", false);
        button(g, pauseButton(2), "Return to main menu", false);
        centered(g, "ESC  TO RESUME", GameUiTheme.label(10), MUTED, r.x, 538, r.width);
    }

    Rectangle settingsBounds() { return new Rectangle(402, 152, 476, 416); }
    Rectangle settingsToggle(int index) { return new Rectangle(802, 267 + index * 144, 44, 25); }
    Rectangle fpsButton(boolean increase) { return new Rectangle(increase ? 817 : 724, 335, 29, 30); }
    Rectangle settingsBack() { return new Rectangle(428, 502, 424, 42); }

    private void drawSettings(Graphics2D g) {
        GameUiTheme.panel(g, settingsBounds(), 16);
        text(g, "PREFERENCES", GameUiTheme.label(10), GOLD, 428, 183);
        text(g, "Settings", GameUiTheme.title(34), INK, 428, 222);
        text(g, "Make the adventure feel right for you.", GameUiTheme.body(12), MUTED, 429, 244);
        settingsRow(g, "Frame counter", "Show live performance in the corner", 284);
        settingsRow(g, "Frame rate", "Choose your preferred refresh target", 356);
        settingsRow(g, "Game audio", "Music and skill sounds", 428);
        toggle(g, settingsToggle(0), logic.isDebugInfoVisible());
        toggle(g, settingsToggle(1), logic.isSoundEnabled());
        button(g, fpsButton(false), "−", false);
        button(g, fpsButton(true), "+", false);
        centered(g, Integer.toString(targetFps.getAsInt()), GameUiTheme.numeric(16), GOLD, 756, 357, 55);
        button(g, settingsBack(), "←  Back", false);
    }

    private void settingsRow(Graphics2D g, String name, String description, int y) {
        text(g, name, GameUiTheme.label(14), INK, 430, y);
        text(g, description, GameUiTheme.body(11), MUTED, 430, y + 19);
        GameUiTheme.separator(g, 428, y + 34, 424);
    }

    private void toggle(Graphics2D g, Rectangle r, boolean on) {
        g.setColor(on ? new Color(123, 155, 138) : GameUiTheme.BORDER);
        g.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
        g.setColor(INK);
        g.fillOval(r.x + (on ? r.width - 22 : 3), r.y + 3, 19, 19);
        hover(r);
    }

    Rectangle upgradeBounds(int index) { return new Rectangle(220 + index * 282, 270, 252, 228); }

    private void drawUpgrades(Graphics2D g) {
        GameUiTheme.panel(g, new Rectangle(192, 173, 896, 385), 16);
        centered(g, "LEVEL " + logic.getLevel() + "  ·  NEW POTENTIAL", GameUiTheme.label(11), GOLD, 192, 211, 896);
        centered(g, "Choose your advantage", GameUiTheme.title(34), INK, 192, 250, 896);
        List<String> choices = logic.getUpgradeChoices();
        for (int index = 0; index < Math.min(3, choices.size()); index++) {
            Rectangle r = upgradeBounds(index);
            GameUiTheme.panel(g, r, 12);
            double focus = hover(r);
            GameUiTheme.glow(g, r.getCenterX(), r.y + 67, 95, GOLD, 0.08 + focus * 0.15);
            String choice = choices.get(index);
            String abilityId = switch (choice) {
                case "Vitality" -> "heal"; case "Swiftness" -> "shadow_step";
                case "Magnetism" -> "blessing"; case "Critical Hit" -> "twin_fang";
                case "Rapid Fire" -> "holy_bolt"; default -> "heavy_slash";
            };
            RpgAbility icon = logic.getAbilityManager().getAbilityById(abilityId);
            image(g, icon.getIcon(), new Rectangle(r.x + 92, r.y + 28, 68, 68));
            centered(g, choice, GameUiTheme.title(23), INK, r.x, r.y + 135, r.width);
            centered(g, logic.getUpgradeDescription(choice), GameUiTheme.body(14), GOLD, r.x, r.y + 164, r.width);
            centered(g, "Choose upgrade  →", GameUiTheme.label(11), focus > 0.2 ? INK : MUTED, r.x, r.y + 205, r.width);
        }
        centered(g, "Your choice lasts for this run.", GameUiTheme.body(11), MUTED, 192, 537, 896);
    }

    Rectangle gameOverButton(int index) { return new Rectangle(432 + index * 214, 471, 202, 48); }

    private void drawGameOver(Graphics2D g) {
        Rectangle r = new Rectangle(402, 144, 476, 421);
        GameUiTheme.panel(g, r, 16);
        GameUiTheme.emblem(g, 640, 209, 50, new Color(188, 106, 99));
        centered(g, "RUN ENDED", GameUiTheme.label(11), new Color(218, 149, 132), r.x, 263, r.width);
        centered(g, "Your watch has ended", GameUiTheme.title(32), INK, r.x, 308, r.width);
        centered(g, "Rise again. The next run is yours.", GameUiTheme.body(13), MUTED, r.x, 337, r.width);
        GameUiTheme.separator(g, 434, 364, 412);
        text(g, "SURVIVED", GameUiTheme.label(10), MUTED, 462, 392);
        text(g, timerText(), GameUiTheme.numeric(27), INK, 462, 426);
        text(g, "HERO LEVEL", GameUiTheme.label(10), MUTED, 678, 392);
        text(g, Integer.toString(logic.getLevel()), GameUiTheme.numeric(27), GOLD, 678, 426);
        button(g, gameOverButton(0), "Try again", true);
        button(g, gameOverButton(1), "Main menu", false);
    }

    Rectangle skillCloseBounds() { return new Rectangle(1070, 56, 122, 34); }
    Rectangle equipBounds(int index) { return new Rectangle(270 + index * 230, 120, 218, 64); }
    Rectangle classBounds(int index) {
        int stride = 1104 / AbilityClass.values().length;
        return new Rectangle(88 + index * stride, 212, stride - 9, 40);
    }
    Rectangle skillBounds(int index) { return new Rectangle(88 + index % 2 * 556, 282 + index / 2 * 73, 542, 64); }

    private void drawSkills(Graphics2D g) {
        GameUiTheme.panel(g, new Rectangle(64, 34, 1152, 652), 16);
        text(g, "THE ARSENAL", GameUiTheme.label(10), GOLD, 88, 61);
        text(g, "Skills & loadout", GameUiTheme.title(31), INK, 88, 94);
        button(g, skillCloseBounds(), "Close   [K]", false);
        text(g, "LOADOUT", GameUiTheme.label(11), GOLD, 89, 143);
        wrapped(g, "Select a slot to equip a hero skill.", GameUiTheme.body(12), MUTED, 89, 163, 160, 16, 2);
        RpgAbility[] equipped = logic.getAbilityManager().getEquippedAbilities();
        for (int index = 0; index < equipped.length; index++) {
            Rectangle r = equipBounds(index);
            GameUiTheme.panel(g, r, 8);
            g.setColor(index == logic.getAbilityManager().getSelectedEquipSlot() ? GOLD : GameUiTheme.BORDER);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            if (equipped[index] != null) {
                image(g, equipped[index].getIcon(), new Rectangle(r.x + 9, r.y + 10, 44, 44));
                wrapped(g, equipped[index].getName(), GameUiTheme.label(12), INK, r.x + 61, r.y + 26, 132, 16, 2);
            }
            text(g, Integer.toString(index + 1), GameUiTheme.numeric(10), MUTED, r.x + 199, r.y + 17);
            hover(r);
        }
        for (AbilityClass type : AbilityClass.values()) {
            Rectangle r = classBounds(type.ordinal());
            button(g, r, type.getDisplayName(), type.ordinal() == skillClass);
        }
        AbilityClass type = AbilityClass.values()[skillClass];
        List<RpgAbility> abilities = logic.getAbilityManager().getAbilities(type);
        for (int index = 0; index < abilities.size(); index++) {
            RpgAbility ability = abilities.get(index);
            AbilityDefinition definition = ability.getDefinition();
            Rectangle r = skillBounds(index);
            boolean allowed = canEquip(ability);
            GameUiTheme.panel(g, r, 8);
            double focus = hover(r);
            if (ability.isEquipped() || allowed && focus > 0.02) {
                g.setColor(ability.isEquipped() ? GOLD : new Color(115, 146, 160));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            }
            Graphics2D icon = (Graphics2D) g.create();
            if (!allowed && !ability.isEquipped()) icon.setComposite(AlphaComposite.SrcOver.derive(0.52f));
            image(icon, ability.getIcon(), new Rectangle(r.x + 9, r.y + 10, 44, 44));
            icon.dispose();
            text(g, ability.getName(), GameUiTheme.label(13), allowed || ability.isEquipped() ? INK : MUTED, r.x + 65, r.y + 23);
            clipped(g, definition.getDescription(), GameUiTheme.body(11), MUTED, r.x + 65, r.y + 43, 345);
            String status = definition.isPassive() ? "PASSIVE" : ability.isEquipped() ? "EQUIPPED" : allowed ? "EQUIP  →"
                    : definition.isUnlockedAt(logic.getLevel()) ? "PREVIEW" : "LV " + definition.getUnlockLevel();
            text(g, status, GameUiTheme.label(9), ability.isEquipped() || allowed ? GOLD : MUTED, r.x + 451, r.y + 22);
            text(g, definition.isPassive() ? "ALWAYS ON" : (int) definition.getManaCost() + " MP",
                    GameUiTheme.numeric(10), MUTED, r.x + 451, r.y + 43);
        }
        text(g, "Your hero's active skills can be equipped. Other skills are available to preview.", GameUiTheme.body(11), MUTED, 89, 670);
    }

    private boolean canEquip(RpgAbility ability) {
        return logic.getCharacterActiveSkillIds(logic.getSelectedCharacterIndex()).contains(ability.getDefinition().getId())
                && ability.getDefinition().isUnlockedAt(logic.getLevel());
    }

    private RpgAbility hoveredAbility() {
        if (logic.isCharacterSelectOpen()) {
            List<RpgAbility> abilities = logic.getCharacterActiveAbilities(logic.getSelectedCharacterIndex());
            for (int index = 0; index < Math.min(4, abilities.size()); index++) {
                if (selectedSkillBounds(index).contains(mouseX, mouseY)) return abilities.get(index);
            }
            if (selectedPassiveBounds().contains(mouseX, mouseY)) return logic.getCharacterPassiveAbility(logic.getSelectedCharacterIndex());
        } else if (logic.isSkillMenuOpen()) {
            List<RpgAbility> abilities = logic.getAbilityManager().getAbilities(AbilityClass.values()[skillClass]);
            for (int index = 0; index < abilities.size(); index++) {
                if (skillBounds(index).contains(mouseX, mouseY)) return abilities.get(index);
            }
            for (int index = 0; index < 4; index++) {
                if (equipBounds(index).contains(mouseX, mouseY)) return logic.getAbilityManager().getEquippedAbilities()[index];
            }
        } else if (logic.isGameStarted() && !logic.isPaused()) {
            for (int index = 0; index < 4; index++) {
                if (GameHud.abilityBounds(index, 1280, 720).contains(mouseX, mouseY)) return logic.getAbilityManager().getEquippedAbilities()[index];
            }
        }
        return null;
    }

    private void drawTooltip(Graphics2D g) {
        RpgAbility ability = hoveredAbility();
        if (ability == null) return;
        int x = Math.min(1280 - 330, Math.max(16, mouseX + 17));
        int y = mouseY > 460 ? mouseY - 145 : mouseY + 19;
        y = Math.max(16, Math.min(720 - 147, y));
        Rectangle r = new Rectangle(x, y, 310, 130);
        GameUiTheme.panel(g, r, 10);
        AbilityDefinition definition = ability.getDefinition();
        text(g, ability.getName(), GameUiTheme.title(20), INK, x + 15, y + 29);
        text(g, definition.getAbilityClass().getDisplayName(), GameUiTheme.label(10), GOLD, x + 16, y + 48);
        wrapped(g, definition.getDescription(), GameUiTheme.body(12), MUTED, x + 16, y + 71, 276, 17, 2);
        text(g, (int) definition.getManaCost() + " MP     ·     " + (int) definition.getCooldownSeconds() + "s COOLDOWN",
                GameUiTheme.numeric(11), INK, x + 16, y + 115);
    }

    private String timerText() {
        int seconds = (int) logic.getGameTimer();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void image(Graphics2D graphics, BufferedImage image, Rectangle bounds) {
        if (image == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(bounds.width / (double) image.getWidth(), bounds.height / (double) image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, bounds.x + (bounds.width - w) / 2, bounds.y + (bounds.height - h) / 2, w, h, null);
        g.dispose();
    }

    private void clipped(Graphics2D g, String value, Font font, Color color, int x, int y, int width) {
        g.setFont(font);
        String result = value;
        while (g.getFontMetrics().stringWidth(result) > width && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.equals(value) && result.length() > 3) result = result.substring(0, result.length() - 3) + "…";
        text(g, result, font, color, x, y);
    }

    private void wrapped(Graphics2D g, String value, Font font, Color color, int x, int y, int width, int leading, int limit) {
        g.setFont(font);
        String line = "";
        int row = 0;
        for (String word : value.split("\\s+")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && g.getFontMetrics().stringWidth(next) > width) {
                text(g, line, font, color, x, y + row++ * leading);
                line = word;
                if (row >= limit) return;
            } else line = next;
        }
        if (row < limit) clipped(g, line, font, color, x, y + row * leading, width);
    }

    boolean click(int x, int y) {
        if (logic.isMainMenuOpen()) {
            if (logic.isSettingsOpen()) settingsClick(x, y);
            else if (mainButton(0).contains(x, y)) logic.showCharacterSelection();
            else if (mainButton(1).contains(x, y)) logic.toggleSettings();
            else if (mainButton(2).contains(x, y)) System.exit(0);
            return true;
        }
        if (logic.isCharacterSelectOpen()) {
            if (backBounds().contains(x, y)) logic.showMainMenu();
            else if (startBounds().contains(x, y)) logic.startGame();
            else for (int index = 0; index < logic.getCharacterCount(); index++) {
                if (heroBounds(index).contains(x, y)) { logic.selectCharacter(index); break; }
            }
            return true;
        }
        if (logic.isGameOver()) {
            if (gameOverButton(0).contains(x, y)) logic.startGame();
            else if (gameOverButton(1).contains(x, y)) logic.showMainMenu();
            return true;
        }
        if (logic.isUpgradeMenuOpen()) {
            for (int index = 0; index < 3; index++) if (upgradeBounds(index).contains(x, y)) { logic.chooseUpgrade(index); break; }
            return true;
        }
        if (logic.isPaused()) {
            if (logic.isSettingsOpen()) settingsClick(x, y);
            else if (pauseButton(0).contains(x, y)) logic.resume();
            else if (pauseButton(1).contains(x, y)) logic.toggleSettings();
            else if (pauseButton(2).contains(x, y)) logic.showMainMenu();
            return true;
        }
        if (logic.isSkillMenuOpen()) {
            if (skillCloseBounds().contains(x, y)) logic.toggleSkillMenu();
            else {
                for (int index = 0; index < 4; index++) if (equipBounds(index).contains(x, y)) logic.selectAbilityEquipSlot(index);
                for (int index = 0; index < AbilityClass.values().length; index++) if (classBounds(index).contains(x, y)) skillClass = index;
                List<RpgAbility> abilities = logic.getAbilityManager().getAbilities(AbilityClass.values()[skillClass]);
                for (int index = 0; index < abilities.size(); index++) {
                    if (skillBounds(index).contains(x, y) && canEquip(abilities.get(index))) { logic.equipAbility(abilities.get(index)); break; }
                }
            }
            return true;
        }
        return false;
    }

    private void settingsClick(int x, int y) {
        if (settingsBack().contains(x, y)) logic.toggleSettings();
        else if (settingsToggle(0).contains(x, y)) logic.setDebugInfoVisible(!logic.isDebugInfoVisible());
        else if (settingsToggle(1).contains(x, y)) logic.setSoundEnabled(!logic.isSoundEnabled());
        else if (fpsButton(false).contains(x, y)) adjustFps.accept(-1);
        else if (fpsButton(true).contains(x, y)) adjustFps.accept(1);
    }

    boolean interactive(int x, int y) {
        if (logic.isMainMenuOpen()) {
            if (logic.isSettingsOpen()) return settingsInteractive(x, y);
            for (int index = 0; index < 3; index++) if (mainButton(index).contains(x, y)) return true;
        } else if (logic.isCharacterSelectOpen()) {
            if (backBounds().contains(x, y) || startBounds().contains(x, y)) return true;
            for (int index = 0; index < logic.getCharacterCount(); index++) if (heroBounds(index).contains(x, y)) return true;
        } else if (logic.isGameOver()) return gameOverButton(0).contains(x, y) || gameOverButton(1).contains(x, y);
        else if (logic.isUpgradeMenuOpen()) {
            for (int index = 0; index < 3; index++) if (upgradeBounds(index).contains(x, y)) return true;
        } else if (logic.isPaused()) {
            if (logic.isSettingsOpen()) return settingsInteractive(x, y);
            for (int index = 0; index < 3; index++) if (pauseButton(index).contains(x, y)) return true;
        } else if (logic.isSkillMenuOpen()) {
            if (skillCloseBounds().contains(x, y)) return true;
            for (int index = 0; index < 4; index++) if (equipBounds(index).contains(x, y)) return true;
            for (int index = 0; index < AbilityClass.values().length; index++) if (classBounds(index).contains(x, y)) return true;
            List<RpgAbility> abilities = logic.getAbilityManager().getAbilities(AbilityClass.values()[skillClass]);
            for (int index = 0; index < abilities.size(); index++) if (skillBounds(index).contains(x, y) && canEquip(abilities.get(index))) return true;
        } else if (logic.isGameStarted()) {
            for (int index = 0; index < 4; index++) if (GameHud.abilityBounds(index, 1280, 720).contains(x, y)) return true;
        }
        return false;
    }

    private boolean settingsInteractive(int x, int y) {
        return settingsBack().contains(x, y) || settingsToggle(0).contains(x, y) || settingsToggle(1).contains(x, y)
                || fpsButton(false).contains(x, y) || fpsButton(true).contains(x, y);
    }
}
