import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** The unobtrusive combat interface. Rendering never advances gameplay or UI state. */
public final class GameHud {
    private static final Color INK = GameUiTheme.BACKGROUND;
    private static final Color SURFACE = GameUiTheme.SURFACE_RAISED;
    private static final Color GOLD = GameUiTheme.GOLD;
    private static final Color IVORY = GameUiTheme.IVORY;
    private static final Color MUTED = GameUiTheme.MUTED;
    private static final Color CYAN = new Color(116, 191, 222);
    private static final Font BODY = GameUiTheme.body(11f);
    private static final Font SMALL = GameUiTheme.body(10f);
    private static final Font LABEL = GameUiTheme.label(10f);
    private static final Font DISPLAY = GameUiTheme.title(24f);
    private static final Map<String, BufferedImage> HEADS = new HashMap<>();

    private GameHud() { }

    public static void draw(Graphics2D graphics, GameLogic logic, int width, int height,
            double uiTime, int mouseX, int mouseY) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawVitals(g, logic, width, uiTime);
        drawSurvival(g, logic, width);
        drawHotbar(g, logic, width, height, uiTime, mouseX, mouseY);
        g.dispose();
    }

    /** These exact rectangles also drive the game's ability hover and click handling. */
    public static Rectangle abilityBounds(int index, int width, int height) {
        int slotWidth = Math.max(58, Math.min(104, (width - 92) / 4 - 10));
        int gap = width < 520 ? 6 : 10;
        int totalWidth = slotWidth * AbilityManager.EQUIPPED_SLOT_COUNT
                + gap * (AbilityManager.EQUIPPED_SLOT_COUNT - 1);
        return new Rectangle((width - totalWidth) / 2 + index * (slotWidth + gap),
                height - 118, slotWidth, 90);
    }

    private static void drawVitals(Graphics2D g, GameLogic logic, int width, double time) {
        int x = 20;
        int y = 20;
        int w = Math.min(336, Math.max(220, width - 220));
        panel(g, x, y, w, 142, 14);
        g.setColor(alpha(GOLD, 180));
        g.fillRoundRect(x + 22, y, 44, 2, 2, 2);

        BufferedImage head = HEADS.computeIfAbsent(logic.getPortraitPath(), GameHud::portraitHead);
        g.setPaint(new GradientPaint(x + 12, y + 12, new Color(48, 44, 37),
                x + 65, y + 68, new Color(14, 19, 27)));
        g.fill(new Ellipse2D.Double(x + 15, y + 13, 58, 58));
        Graphics2D portrait = (Graphics2D) g.create();
        portrait.clip(new Ellipse2D.Double(x + 18, y + 16, 52, 52));
        portrait.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        portrait.drawImage(head, x + 16, y + 13, 56, 56, null);
        portrait.dispose();
        g.setColor(alpha(GOLD, 140));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(x + 15, y + 13, 58, 58));
        g.setColor(alpha(IVORY, 25));
        g.draw(new Ellipse2D.Double(x + 19, y + 17, 50, 50));

        g.setFont(DISPLAY.deriveFont(22f));
        g.setColor(IVORY);
        g.drawString(logic.getSelectedCharacterName(), x + 85, y + 37);
        g.setFont(LABEL.deriveFont(9f));
        g.setColor(GOLD);
        String className = logic.getCharacterClassName(logic.getSelectedCharacterIndex());
        g.drawString(className, x + 86, y + 55);
        g.setFont(SMALL);
        g.setColor(MUTED);
        g.drawString("LEVEL", x + w - 57, y + 25);
        g.setFont(DISPLAY.deriveFont(21f));
        g.setColor(IVORY);
        centered(g, Integer.toString(logic.getLevel()), x + w - 65, y + 51, 52);

        double health = logic.getPlayerHealth();
        double maxHealth = logic.getPlayerMaxHealth();
        double hpRatio = maxHealth > 0 ? clamp(health / maxHealth) : 0;
        AbilityManager manager = logic.getAbilityManager();
        double manaRatio = clamp(manager.getMana() / manager.getMaxMana());
        int barX = x + 48;
        int barW = w - 68;

        g.setFont(LABEL.deriveFont(9f));
        g.setColor(MUTED);
        g.drawString("HP", x + 18, y + 88);
        g.drawString("MP", x + 18, y + 113);
        g.setFont(SMALL);
        g.setColor(IVORY);
        right(g, String.format("%.0f / %.0f", health, maxHealth), x + w - 20, y + 79);
        g.setColor(manager.isManaLocked() ? new Color(206, 149, 167) : CYAN);
        right(g, manager.isManaLocked() ? "MANA SEALED" : String.format("%.0f / %.0f",
                manager.getMana(), manager.getMaxMana()), x + w - 20, y + 104);
        resource(g, barX, y + 84, barW, 7, hpRatio,
                new Color(128, 45, 58), new Color(230, 105, 102), time);
        resource(g, barX, y + 109, barW, 6, manaRatio,
                new Color(42, 77, 120), CYAN, time);

        g.setFont(SMALL.deriveFont(9f));
        g.setColor(MUTED);
        g.drawString("EXPERIENCE", x + 18, y + 130);
        right(g, logic.getCurrentExp() + " / " + logic.getExpToNextLevel(),
                x + w - 18, y + 130);
        resource(g, x + 18, y + 135, w - 36, 2, logic.getExpProgress(),
                new Color(120, 93, 48), GOLD, time);
        if (hpRatio > 0 && hpRatio < 0.25) {
            g.setColor(new Color(236, 102, 102, 60 + (int) (35 * Math.sin(time * 3))));
            g.drawRoundRect(x, y, w, 142, 14, 14);
        }
    }

    private static void drawSurvival(Graphics2D g, GameLogic logic, int width) {
        int w = width < 700 ? 142 : 172;
        int x = width - w - 20;
        int y = 20;
        panel(g, x, y, w, 86, 13);
        g.setFont(LABEL.deriveFont(9f));
        g.setColor(GOLD);
        centered(g, "SURVIVAL", x, y + 21, w);
        int seconds = Math.max(0, (int) logic.getGameTimer());
        g.setFont(GameUiTheme.numeric(26f));
        g.setColor(IVORY);
        centered(g, String.format("%02d : %02d", seconds / 60, seconds % 60), x, y + 50, w);
        g.setColor(alpha(IVORY, 14));
        g.drawLine(x + 20, y + 59, x + w - 20, y + 59);
        g.setFont(SMALL);
        g.setColor(MUTED);
        int enemies = logic.getEnemyCount();
        centered(g, enemies + (enemies == 1 ? " active enemy" : " active enemies"), x, y + 75, w);
    }

    private static void drawHotbar(Graphics2D g, GameLogic logic, int width, int height,
            double time, int mouseX, int mouseY) {
        Rectangle first = abilityBounds(0, width, height);
        Rectangle last = abilityBounds(3, width, height);
        int x = first.x - 18;
        int y = first.y - 25;
        int w = last.x + last.width - first.x + 36;
        panel(g, x, y, w, 137, 16);
        g.setFont(LABEL.deriveFont(9f));
        g.setColor(GOLD);
        g.drawString("ABILITIES", x + 19, y + 16);
        g.setFont(SMALL.deriveFont(9f));
        g.setColor(MUTED);
        right(g, "1 – 4  CAST", x + w - 19, y + 16);
        RpgAbility[] equipped = logic.getAbilityManager().getEquippedAbilities();
        for (int i = 0; i < equipped.length; i++) {
            Rectangle bounds = abilityBounds(i, width, height);
            drawAbility(g, bounds, equipped[i], logic.getAbilityManager(),
                    logic.getLevel(), i + 1, bounds.contains(mouseX, mouseY), time);
        }
        g.setFont(SMALL.deriveFont(9f));
        g.setColor(alpha(MUTED, 190));
        centered(g, "WASD  MOVE      K  SKILLS      ESC  PAUSE", x, y + 129, w);
    }

    private static void drawAbility(Graphics2D g, Rectangle r, RpgAbility ability,
            AbilityManager manager, int level, int key, boolean hovered, double time) {
        boolean unlocked = ability != null && ability.getDefinition().isUnlockedAt(level);
        boolean manaAvailable = ability != null && !manager.isManaLocked()
                && manager.getMana() >= ability.getDefinition().getManaCost();
        boolean ready = unlocked && manaAvailable && ability.isReady();
        Color accent = ability == null ? MUTED : abilityAccent(ability.getDefinition().getAbilityClass());
        g.setPaint(new GradientPaint(r.x, r.y, hovered ? new Color(44, 45, 54) : SURFACE,
                r.x, r.y + r.height, new Color(13, 18, 26, 255)));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 9, 9);
        g.setColor(hovered ? alpha(GOLD, 220) : alpha(ready ? accent : MUTED, ready ? 100 : 42));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 9, 9);
        g.setColor(alpha(IVORY, 25));
        g.drawLine(r.x + 7, r.y + 1, r.x + r.width - 7, r.y + 1);

        int iconSize = Math.min(46, r.width - 18);
        int iconX = r.x + (r.width - iconSize) / 2;
        int iconY = r.y + 9;
        g.setColor(new Color(8, 12, 18, 175));
        g.fillRoundRect(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4, 9, 9);
        if (ability != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            drawImageInside(g, ability.getIcon(), iconX + 2, iconY + 2, iconSize - 4, iconSize - 4);
            if (!unlocked || !manaAvailable) {
                g.setColor(new Color(10, 14, 21, unlocked ? 115 : 195));
                g.fillRoundRect(iconX, iconY, iconSize, iconSize, 7, 7);
            }
            if (unlocked && !ability.isReady()) {
                Shape clip = g.getClip();
                g.clip(new RoundRectangle2D.Double(iconX, iconY, iconSize, iconSize, 7, 7));
                g.setColor(new Color(7, 11, 18, 185));
                g.fill(new Arc2D.Double(iconX - iconSize * 0.25, iconY - iconSize * 0.25,
                        iconSize * 1.5, iconSize * 1.5, 90,
                        -360 * ability.getCooldownRatio(), Arc2D.PIE));
                g.setClip(clip);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(alpha(accent, 195));
                g.draw(new Arc2D.Double(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4,
                        90, -360 * (1.0 - ability.getCooldownRatio()), Arc2D.OPEN));
                g.setFont(GameUiTheme.numeric(17f));
                String cooldown = ability.getCooldownRemaining() >= 10
                        ? Integer.toString((int) Math.ceil(ability.getCooldownRemaining()))
                        : String.format("%.1f", ability.getCooldownRemaining());
                g.setColor(new Color(0, 0, 0, 220));
                centered(g, cooldown, iconX + 1, iconY + iconSize / 2 + 7, iconSize);
                g.setColor(IVORY);
                centered(g, cooldown, iconX, iconY + iconSize / 2 + 6, iconSize);
            } else if (!unlocked) {
                lock(g, iconX + iconSize / 2, iconY + iconSize / 2, GOLD);
            } else if (ready) {
                g.setColor(alpha(accent, hovered ? 200 : 80 + (int) (8 * Math.sin(time * 1.8 + key * 0.4))));
                g.fillRoundRect(r.x + 13, r.y + r.height - 2, r.width - 26, 2, 2, 2);
            }
        }

        g.setPaint(new GradientPaint(r.x + 6, r.y + 5, new Color(71, 65, 50),
                r.x + 6, r.y + 23, new Color(30, 31, 33)));
        g.fillRoundRect(r.x + 6, r.y + 5, 18, 19, 4, 4);
        g.setColor(alpha(GOLD, 90));
        g.drawRoundRect(r.x + 6, r.y + 5, 18, 19, 4, 4);
        g.setFont(LABEL);
        g.setColor(IVORY);
        centered(g, Integer.toString(key), r.x + 6, r.y + 19, 18);

        g.setFont(BODY.deriveFont(r.width < 88 ? 9f : 10f));
        g.setColor(unlocked ? IVORY : MUTED);
        centered(g, ellipsize(g, ability == null ? "Empty" : ability.getName(), r.width - 12),
                r.x + 6, r.y + 69, r.width - 12);
        g.setFont(SMALL.deriveFont(9f));
        g.setColor(!unlocked ? GOLD : manaAvailable ? MUTED : new Color(186, 133, 146));
        String detail = ability == null ? "K to equip" : !unlocked
                ? "LEVEL " + ability.getDefinition().getUnlockLevel()
                : !manaAvailable ? manager.isManaLocked() ? "MANA SEALED" : "LOW MANA"
                : String.format("%.0f MP", ability.getDefinition().getManaCost());
        centered(g, detail, r.x, r.y + 83, r.width);
    }

    private static void panel(Graphics2D g, int x, int y, int w, int h, int radius) {
        GameUiTheme.panel(g, new Rectangle(x, y, w, h), radius);
    }

    private static void resource(Graphics2D g, int x, int y, int w, int h, double ratio,
            Color dark, Color bright, double time) {
        g.setColor(new Color(2, 7, 13, 210));
        g.fillRoundRect(x, y, w, h, h, h);
        int filled = (int) Math.round(w * clamp(ratio));
        if (filled > 0) {
            Shape clip = g.getClip();
            g.clip(new RoundRectangle2D.Double(x, y, w, h, h, h));
            g.setPaint(new GradientPaint(x, y, dark, x + w, y, bright));
            g.fillRect(x, y, filled, h);
            g.setColor(alpha(bright, 115));
            g.drawLine(x + 2, y, x + filled - 1, y);
            g.setClip(clip);
        }
    }

    private static BufferedImage portraitHead(String path) {
        BufferedImage sheet = ResourceLoader.loadImage(path);
        int fw = sheet.getWidth() / 3;
        int fh = sheet.getHeight() / 4;
        sheet = CharacterSpriteImages.prepareSheet(sheet, fw, fh);
        // A square source keeps the face's proportions in the square badge.
        int size = Math.max(1, Math.min(fw, fh) * 5 / 8);
        int x = fw + (fw - size) / 2;
        int y = fh * 2 + Math.min(fh - size, (int) Math.round(fh * 0.03));
        return sheet.getSubimage(x, y, size, size);
    }

    private static void drawImageInside(Graphics2D g, BufferedImage image,
            int x, int y, int width, int height) {
        if (image == null) return;
        double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        g.drawImage(image, x + (width - w) / 2, y + (height - h) / 2, w, h, null);
    }

    private static void lock(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Arc2D.Double(x - 4, y - 8, 8, 10, 0, 180, Arc2D.OPEN));
        g.fillRoundRect(x - 6, y - 3, 12, 10, 3, 3);
        g.setColor(INK);
        g.fillOval(x - 1, y, 2, 3);
    }

    private static Color abilityAccent(AbilityClass abilityClass) {
        return switch (abilityClass) {
            case BLACK_KNIGHT -> new Color(205, 119, 84);
            case ASSASSIN -> new Color(168, 132, 226);
            case PRIEST -> new Color(220, 187, 112);
            case ELEMENTALIST -> new Color(112, 170, 226);
            case RANGER -> new Color(130, 189, 140);
            case WARLOCK -> new Color(165, 115, 173);
            case GUARDIAN -> new Color(224, 180, 86);
        };
    }

    private static String ellipsize(Graphics2D g, String text, int maxWidth) {
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        int length = text.length();
        while (length > 1 && g.getFontMetrics().stringWidth(text.substring(0, length) + "…") > maxWidth) {
            length--;
        }
        return text.substring(0, length) + "…";
    }

    private static void centered(Graphics2D g, String text, int x, int baseline, int width) {
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }

    private static void right(Graphics2D g, String text, int x, int baseline) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), baseline);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
