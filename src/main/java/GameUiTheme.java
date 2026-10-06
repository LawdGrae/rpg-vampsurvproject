import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/** Shared visual language for the game's menus and heads-up display. */
public final class GameUiTheme {
    public static final Color BACKGROUND = new Color(10, 14, 24);
    public static final Color SURFACE = new Color(19, 25, 38);
    public static final Color SURFACE_RAISED = new Color(28, 36, 51);
    public static final Color BORDER = new Color(57, 69, 87);
    public static final Color GOLD = new Color(204, 174, 110);
    public static final Color IVORY = new Color(239, 233, 219);
    public static final Color MUTED = new Color(153, 166, 187);

    private static final Font TITLE = new Font("Georgia", Font.PLAIN, 24);
    private static final Font LABEL = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font NUMBER = new Font("Consolas", Font.PLAIN, 14);

    private GameUiTheme() {
    }

    public static Font title(float size) {
        return TITLE.deriveFont(Math.max(1f, size));
    }

    public static Font label(float size) {
        return LABEL.deriveFont(Math.max(1f, size));
    }

    public static Font body(float size) {
        return BODY.deriveFont(Math.max(1f, size));
    }

    public static Font numeric(float size) {
        return NUMBER.deriveFont(Math.max(1f, size));
    }

    public static void panel(Graphics2D graphics, Rectangle bounds, int arc) {
        if (bounds.width <= 0 || bounds.height <= 0) return;
        Graphics2D g = prepared(graphics);
        try {
            double radius = Math.max(0, arc);
            for (int step = 3; step >= 1; step--) {
                g.setColor(new Color(0, 0, 0, 12 + (4 - step) * 9));
                g.fill(new RoundRectangle2D.Double(bounds.x - step, bounds.y + step * 2,
                        bounds.width + step * 2, bounds.height, radius + step, radius + step));
            }
            RoundRectangle2D shape = new RoundRectangle2D.Double(bounds.x + 0.5, bounds.y + 0.5,
                    Math.max(0, bounds.width - 1), Math.max(0, bounds.height - 1), radius, radius);
            g.setPaint(new GradientPaint(bounds.x, bounds.y, new Color(28, 35, 50, 247),
                    bounds.x + bounds.width * 0.25f, bounds.y + bounds.height,
                    new Color(12, 17, 28, 251)));
            g.fill(shape);
            g.setColor(new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), 190));
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
            g.clip(shape);
            g.setPaint(new GradientPaint(bounds.x, bounds.y, new Color(194, 214, 244, 24),
                    bounds.x, bounds.y + Math.min(65, bounds.height), new Color(194, 214, 244, 0)));
            g.fillRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, Math.min(65, bounds.height - 2));
            if (bounds.width > 300 && bounds.height > 140) {
                g.setColor(withAlpha(GOLD, 68));
                int inset = Math.max(8, Math.min(15, arc / 2));
                Path2D corners = new Path2D.Double();
                corners.moveTo(bounds.x + inset, bounds.y + inset + 12);
                corners.lineTo(bounds.x + inset, bounds.y + inset + 4);
                corners.lineTo(bounds.x + inset + 4, bounds.y + inset);
                corners.lineTo(bounds.x + inset + 18, bounds.y + inset);
                corners.moveTo(bounds.x + bounds.width - inset - 18,
                        bounds.y + bounds.height - inset);
                corners.lineTo(bounds.x + bounds.width - inset - 4,
                        bounds.y + bounds.height - inset);
                corners.lineTo(bounds.x + bounds.width - inset,
                        bounds.y + bounds.height - inset - 4);
                corners.lineTo(bounds.x + bounds.width - inset,
                        bounds.y + bounds.height - inset - 12);
                g.draw(corners);
            }
        } finally {
            g.dispose();
        }
    }

    public static void button(Graphics2D graphics, Rectangle bounds, String caption,
            double hover, boolean pressed, boolean primary) {
        if (bounds.width <= 0 || bounds.height <= 0) return;
        Graphics2D g = prepared(graphics);
        try {
            float amount = (float) clamp(hover);
            int offset = pressed ? 1 : 0;
            double x = bounds.x + 0.5;
            double y = bounds.y + offset + 0.5;
            double width = Math.max(0, bounds.width - 1);
            double height = Math.max(0, bounds.height - 1 - offset);
            g.setColor(new Color(0, 0, 0, pressed ? 40 : 78));
            g.fill(new RoundRectangle2D.Double(x, y + (pressed ? 2 : 4), width, height, 8, 8));
            Color top = primary ? blend(new Color(208, 181, 125), new Color(235, 212, 165), amount)
                    : blend(new Color(36, 45, 61), new Color(52, 65, 85), amount);
            Color bottom = primary ? blend(new Color(164, 133, 79), new Color(198, 163, 102), amount)
                    : blend(new Color(22, 29, 43), new Color(32, 42, 58), amount);
            if (pressed) {
                top = blend(top, Color.BLACK, 0.14f);
                bottom = blend(bottom, Color.BLACK, 0.1f);
            }
            RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, width, height, 8, 8);
            g.setPaint(new GradientPaint((float) x, (float) y, top,
                    (float) x, (float) (y + height), bottom));
            g.fill(shape);
            g.setStroke(new BasicStroke(1f));
            g.setColor(primary ? new Color(245, 224, 179, 125)
                    : blend(BORDER, withAlpha(GOLD, 185), amount * 0.68f));
            g.draw(shape);
            g.setColor(primary ? new Color(255, 246, 220, 80) : new Color(216, 230, 250, 24));
            g.draw(new Line2D.Double(x + 8, y + 1, x + width - 8, y + 1));
            float fontSize = Math.min(17f, Math.max(12f, bounds.height * 0.31f));
            Font font = label(fontSize);
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            while (metrics.stringWidth(caption) > bounds.width - 32 && fontSize > 10f) {
                fontSize -= 1f;
                font = label(fontSize);
                g.setFont(font);
                metrics = g.getFontMetrics();
            }
            Color ink = primary ? new Color(26, 25, 28) : IVORY;
            Rectangle labelBounds = new Rectangle(bounds.x + 12, bounds.y + offset,
                    Math.max(0, bounds.width - 24), bounds.height - offset);
            centeredText(g, caption, font, ink, labelBounds);
        } finally {
            g.dispose();
        }
    }

    public static void text(Graphics2D graphics, String value, Font font, Color color,
            int x, int baseline) {
        Graphics2D g = prepared(graphics);
        try {
            g.setFont(font);
            g.setColor(color);
            g.drawString(value, x, baseline);
        } finally {
            g.dispose();
        }
    }

    public static void centeredText(Graphics2D graphics, String value, Font font, Color color,
            Rectangle bounds) {
        Graphics2D g = prepared(graphics);
        try {
            g.setFont(font);
            g.setColor(color);
            FontMetrics metrics = g.getFontMetrics();
            float x = bounds.x + (bounds.width - metrics.stringWidth(value)) * 0.5f;
            float baseline = bounds.y + (bounds.height - metrics.getHeight()) * 0.5f + metrics.getAscent();
            g.drawString(value, x, baseline);
        } finally {
            g.dispose();
        }
    }

    public static void glow(Graphics2D graphics, double x, double y, double radius,
            Color color, double alpha) {
        if (!(radius > 0) || !Double.isFinite(radius) || !Double.isFinite(x) || !Double.isFinite(y)) return;
        int opacity = (int) Math.round(color.getAlpha() * clamp(alpha));
        if (opacity <= 0) return;
        Graphics2D g = prepared(graphics);
        try {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) radius,
                    new float[] {0f, 0.42f, 1f}, new Color[] {
                            withAlpha(color, opacity), withAlpha(color, opacity / 2), withAlpha(color, 0)}));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        } finally {
            g.dispose();
        }
    }

    public static void separator(Graphics2D graphics, int x, int y, int width) {
        if (width <= 0) return;
        Graphics2D g = prepared(graphics);
        try {
            g.setStroke(new BasicStroke(1f));
            g.setPaint(new GradientPaint(x, y, withAlpha(BORDER, 0),
                    x + width * 0.5f, y, withAlpha(BORDER, 230)));
            g.draw(new Line2D.Double(x, y + 0.5, x + width * 0.5, y + 0.5));
            g.setPaint(new GradientPaint(x + width * 0.5f, y, withAlpha(BORDER, 230),
                    x + width, y, withAlpha(BORDER, 0)));
            g.draw(new Line2D.Double(x + width * 0.5, y + 0.5, x + width, y + 0.5));
        } finally {
            g.dispose();
        }
    }

    /** A small heraldic star, designed to stay legible without a raster asset. */
    public static void emblem(Graphics2D graphics, int centerX, int centerY, int size, Color color) {
        if (size <= 0) return;
        Graphics2D g = prepared(graphics);
        try {
            double radius = size * 0.5;
            g.setStroke(new BasicStroke(Math.max(1f, size / 50f), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            Path2D crest = new Path2D.Double();
            for (int point = 0; point < 8; point++) {
                double angle = -Math.PI * 0.5 + point * Math.PI * 0.25;
                double reach = (point & 1) == 0 ? radius : radius * 0.28;
                double px = centerX + Math.cos(angle) * reach;
                double py = centerY + Math.sin(angle) * reach;
                if (point == 0) crest.moveTo(px, py);
                else crest.lineTo(px, py);
            }
            crest.closePath();
            g.setColor(withAlpha(color, Math.min(38, color.getAlpha())));
            g.fill(crest);
            g.setColor(color);
            g.draw(crest);
            Path2D diamond = new Path2D.Double();
            diamond.moveTo(centerX, centerY - radius * 0.16);
            diamond.lineTo(centerX + radius * 0.11, centerY);
            diamond.lineTo(centerX, centerY + radius * 0.16);
            diamond.lineTo(centerX - radius * 0.11, centerY);
            diamond.closePath();
            g.fill(diamond);
            if (size >= 28) {
                g.setColor(withAlpha(color, Math.min(80, color.getAlpha())));
                double diagonal = radius * 0.58;
                g.draw(new Line2D.Double(centerX - diagonal, centerY - diagonal,
                        centerX - diagonal * 0.68, centerY - diagonal * 0.68));
                g.draw(new Line2D.Double(centerX + diagonal, centerY + diagonal,
                        centerX + diagonal * 0.68, centerY + diagonal * 0.68));
                g.draw(new Line2D.Double(centerX + diagonal, centerY - diagonal,
                        centerX + diagonal * 0.68, centerY - diagonal * 0.68));
                g.draw(new Line2D.Double(centerX - diagonal, centerY + diagonal,
                        centerX - diagonal * 0.68, centerY + diagonal * 0.68));
            }
        } finally {
            g.dispose();
        }
    }

    private static Graphics2D prepared(Graphics2D graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return copy;
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static Color blend(Color first, Color second, float fraction) {
        double mix = clamp(fraction);
        return new Color((int) Math.round(first.getRed() * (1 - mix) + second.getRed() * mix),
                (int) Math.round(first.getGreen() * (1 - mix) + second.getGreen() * mix),
                (int) Math.round(first.getBlue() * (1 - mix) + second.getBlue() * mix),
                (int) Math.round(first.getAlpha() * (1 - mix) + second.getAlpha() * mix));
    }
}
