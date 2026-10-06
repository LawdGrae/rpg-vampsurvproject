import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
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
        background(g);
        text(g, "SURVIVE THE NIGHT", GameUiTheme.label(13), GOLD, 88, 145);
        text(g, "RPG", GameUiTheme.title(100), INK, 82, 258);
        text(g, "One hero. An unrelenting world.", GameUiTheme.title(26), INK, 88, 306);
        text(g, "Choose your champion and make every moment count.", GameUiTheme.body(14), MUTED, 88, 334);
        button(g, mainButton(0), "Begin adventure     →", true);
        button(g, mainButton(1), "Settings", false);
        button(g, mainButton(2), "Exit game", false);
        int selected = logic.getSelectedCharacterIndex();
        Color accent = HERO_COLORS[selected];
        drawStage(g, 950, 420, 165, accent, 0.7);
        portrait(g, selected, new Rectangle(775, 185, 350, 350));
        centered(g, logic.getSelectedCharacterName(), GameUiTheme.title(32), INK, 740, 588, 420);
        centered(g, logic.getCharacterClassName(selected).toUpperCase(), GameUiTheme.label(12), accent, 740, 614, 420);
        GameUiTheme.separator(g, 88, 658, 1104);
        text(g, "WASD  MOVE     ·     1–4  SKILLS     ·     ESC  PAUSE", GameUiTheme.label(11), MUTED, 88, 689);
        text(g, "FIVE HEROES. ONE LAST STAND.", GameUiTheme.label(11), GOLD, 896, 689);
        if (logic.isSettingsOpen()) {
            dim(g);
            drawSettings(g);
        }
    }

    Rectangle mainButton(int row) { return new Rectangle(88, 374 + row * 66, 300, 52); }
    Rectangle backBounds() { return new Rectangle(1110, 62, 106, 36); }
    Rectangle heroBounds(int index) {
        int stride = 1152 / logic.getCharacterCount();
        return new Rectangle(64 + index * stride, 138, stride - 12, 352);
    }
    Rectangle startBounds() { return new Rectangle(1002, 571, 190, 52); }
    Rectangle selectedSkillBounds(int index) { return new Rectangle(330 + index * 155, 558, 144, 78); }
    Rectangle selectedPassiveBounds() { return new Rectangle(86, 625, 25, 25); }

    void drawSelection(Graphics2D g) {
        background(g);
        text(g, "THE HERO ROSTER", GameUiTheme.label(11), GOLD, 65, 45);
        text(g, "Choose your champion", GameUiTheme.title(40), INK, 64, 96);
        text(g, HERO_COPY[logic.getSelectedCharacterIndex()], GameUiTheme.body(13), MUTED, 66, 120);
        button(g, backBounds(), "←  Back", false);
        for (int index = 0; index < logic.getCharacterCount(); index++) drawHeroCard(g, index);
        drawSelectedHero(g);
        text(g, "Click a hero to inspect  ·  Hover over a skill for details", GameUiTheme.body(11), MUTED, 65, 698);
        text(g, "MORE CHAMPIONS TO COME", GameUiTheme.label(10), MUTED, 1026, 698);
        drawTooltip(g);
    }

    private void drawHeroCard(Graphics2D graphics, int index) {
        Rectangle r = heroBounds(index);
        Graphics2D g = (Graphics2D) graphics.create();
        boolean selected = logic.getSelectedCharacterIndex() == index;
        Color accent = HERO_COLORS[index];
        double focus = Math.max(selection[index], hover(r) * 0.52);
        GameUiTheme.panel(g, r, 14);
        g.clip(new RoundRectangle2D.Double(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 14, 14));
        GameUiTheme.glow(g, r.getCenterX(), r.y + 150, 182, accent, 0.13 + focus * 0.18);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (36 + focus * 76)));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3, 14, 14);
        g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), (int) (selection[index] * 170)));
        g.drawRoundRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3, 14, 14);
        text(g, logic.getCharacterClassName(index).toUpperCase(), GameUiTheme.label(11), accent, r.x + 22, r.y + 29);
        if (selected) {
            g.setColor(GOLD);
            g.fillOval(r.x + r.width - 38, r.y + 17, 18, 18);
            g.setColor(GameUiTheme.BACKGROUND);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int checkX = r.x + r.width - 34;
            g.drawLine(checkX, r.y + 26, checkX + 3, r.y + 29);
            g.drawLine(checkX + 3, r.y + 29, checkX + 8, r.y + 22);
        } else {
            text(g, String.format("%02d", index + 1), GameUiTheme.numeric(12), MUTED, r.x + r.width - 37, r.y + 29);
        }
        drawStage(g, (int) r.getCenterX(), r.y + 197, 79, accent, 0.38 + focus * 0.3);
        portrait(g, index, new Rectangle(r.x + (r.width - 192) / 2, r.y + 50, 192, 192));
        GameUiTheme.separator(g, r.x + 24, r.y + 246, r.width - 48);
        text(g, logic.getCharacterNames().get(index), GameUiTheme.title(30), INK, r.x + 22, r.y + 285);
        clipped(g, logic.getCharacterRole(index), GameUiTheme.body(r.width < 240 ? 11 : 13), MUTED, r.x + 23, r.y + 309, r.width - 46);
        text(g, logic.getCharacterWeaponName(index), GameUiTheme.body(11), accent, r.x + 23, r.y + 330);
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
        GameUiTheme.panel(g, new Rectangle(64, 512, 1152, 158), 14);
        text(g, "YOUR CHAMPION", GameUiTheme.label(10), GOLD, 86, 537);
        text(g, logic.getSelectedCharacterName(), GameUiTheme.title(26), INK, 86, 571);
        text(g, logic.getCharacterClassName(selected), GameUiTheme.label(12), accent, 87, 595);
        RpgAbility passive = logic.getCharacterPassiveAbility(selected);
        if (passive != null) {
            image(g, passive.getIcon(), selectedPassiveBounds());
            text(g, "PASSIVE", GameUiTheme.label(9), MUTED, 119, 633);
            text(g, passive.getName(), GameUiTheme.body(11), INK, 119, 649);
        }
        text(g, "STARTING ABILITIES", GameUiTheme.label(10), MUTED, 330, 539);
        List<RpgAbility> abilities = logic.getCharacterActiveAbilities(selected);
        for (int index = 0; index < Math.min(4, abilities.size()); index++) {
            Rectangle r = selectedSkillBounds(index);
            g.setPaint(new GradientPaint(r.x, r.y, new Color(33, 41, 57), r.x, r.y + r.height, new Color(18, 25, 38)));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(hover(r) > 0.02 ? accent : GameUiTheme.BORDER);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            RpgAbility ability = abilities.get(index);
            image(g, ability.getIcon(), new Rectangle(r.x + 7, r.y + 9, 44, 44));
            text(g, Integer.toString(index + 1), GameUiTheme.numeric(9), MUTED, r.x + 129, r.y + 14);
            wrapped(g, ability.getName(), GameUiTheme.label(11), INK, r.x + 57, r.y + 27, 78, 14, 2);
            text(g, (int) ability.getDefinition().getManaCost() + " MANA", GameUiTheme.numeric(9), MUTED, r.x + 10, r.y + 68);
        }
        g.setColor(new Color(135, 192, 170));
        g.fillOval(1004, 541, 5, 5);
        text(g, "Ready to deploy", GameUiTheme.body(11), MUTED, 1017, 549);
        button(g, startBounds(), "Enter battlefield  →", true);
        text(g, "ENTER  TO BEGIN", GameUiTheme.label(9), MUTED, 1035, 649);
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
