import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** Presentation of the title screen; input and game state remain in GameMenus. */
final class FantasyMainMenu {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final Color IVORY = new Color(249, 235, 210);
    private static final Color BRASS = new Color(181, 133, 78);
    private static final Color PALE_GOLD = new Color(240, 209, 156);
    private static final Font LOGO_FONT = GameUiTheme.title(128f);

    private FantasyMainMenu() {
    }

    static Rectangle buttonBounds(int index) {
        return switch (index) {
            case 0 -> new Rectangle(486, 555, 308, 80);
            case 1 -> new Rectangle(228, 543, 248, 58);
            case 2 -> new Rectangle(804, 543, 248, 58);
            default -> throw new IllegalArgumentException("Unknown title button: " + index);
        };
    }

    static void drawBackgroundAndTitle(Graphics2D graphics, BufferedImage artwork, double time) {
        Graphics2D g = prepared(graphics);
        try {
            g.setColor(new Color(13, 11, 15));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            if (artwork != null && artwork.getWidth() > 0 && artwork.getHeight() > 0) {
                double scale = Math.max(WIDTH / (double) artwork.getWidth(),
                        HEIGHT / (double) artwork.getHeight());
                int width = (int) Math.ceil(artwork.getWidth() * scale);
                int height = (int) Math.ceil(artwork.getHeight() * scale);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(artwork, (WIDTH - width) / 2, (HEIGHT - height) / 2,
                        width, height, null);
            }

            // Keep the painted landscape visible, with darkness confined to the foreground.
            g.setPaint(new GradientPaint(0, 492, new Color(5, 3, 6, 0),
                    0, HEIGHT, new Color(5, 3, 6, 90)));
            g.fillRect(0, 492, WIDTH, HEIGHT - 492);
            double clock = Double.isFinite(time) ? Math.max(0, time) : 0;
            GameUiTheme.glow(g, 640, 347, 240, new Color(160, 15, 11, 48),
                    0.68 + Math.sin(clock * 0.9) * 0.08);
            drawEmbers(g, clock);
            drawTitle(g);
        } finally {
            g.dispose();
        }
    }

    static void drawButton(Graphics2D graphics, int index, double hover, boolean pressed) {
        Rectangle bounds = buttonBounds(index);
        boolean primary = index == 0;
        double amount = Double.isFinite(hover) ? Math.max(0, Math.min(1, hover)) : 0;
        Graphics2D g = prepared(graphics);
        try {
            int offset = pressed ? 2 : 0;
            double x = bounds.x + 1;
            double y = bounds.y + offset + 1;
            double width = bounds.width - 2;
            double height = bounds.height - offset - 2;
            Shape outer = frameShape(x, y, width, height, primary);
            if (primary || amount > 0.01) {
                Color glow = primary ? new Color(244, 37, 22, 145)
                        : new Color(223, 167, 91, 112);
                GameUiTheme.glow(g, bounds.getCenterX(), bounds.getCenterY(),
                        primary ? 172 : 135, glow,
                        (primary ? 0.34 + amount * 0.22 : amount * 0.34) * (pressed ? 0.45 : 1));
            }

            g.translate(0, 4);
            g.setColor(new Color(0, 0, 0, 170));
            g.fill(outer);
            g.translate(0, -4);
            g.setPaint(new LinearGradientPaint((float) x, (float) y,
                    (float) x, (float) (y + height),
                    new float[] {0f, 0.19f, 0.49f, 0.7f, 1f},
                    new Color[] {PALE_GOLD, new Color(100, 71, 40),
                            new Color(235, 210, 168), new Color(90, 61, 34), BRASS}));
            g.fill(outer);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(20, 12, 8));
            g.draw(outer);

            Shape inner = frameShape(x + 5, y + 5, width - 10, height - 10, primary);
            Color top = primary ? new Color(82 + (int) (amount * 23), 14, 15)
                    : new Color(22 + (int) (amount * 14), 22 + (int) (amount * 10), 23);
            Color bottom = primary ? new Color(27 + (int) (amount * 21), 5, 10)
                    : new Color(5 + (int) (amount * 9), 7 + (int) (amount * 7), 11);
            if (pressed) {
                top = top.darker();
                bottom = bottom.darker();
            }
            g.setPaint(new GradientPaint((float) x, (float) y, top,
                    (float) x, (float) (y + height), bottom));
            g.fill(inner);
            g.setColor(new Color(245, 213, 166, 175 + (int) (amount * 65)));
            g.setStroke(new BasicStroke(0.9f));
            g.draw(inner);

            Graphics2D surface = (Graphics2D) g.create();
            try {
                surface.clip(inner);
                surface.setPaint(new GradientPaint((float) x, (float) y,
                        new Color(255, 227, 188, 26 + (int) (amount * 14)),
                        (float) x, (float) (y + height * 0.58), new Color(255, 227, 188, 0)));
                surface.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                surface.setStroke(new BasicStroke(0.6f));
                surface.setColor(new Color(207, 166, 118, 22));
                for (int side : new int[] {-1, 1}) {
                    double edge = side < 0 ? x + 20 : x + width - 20;
                    Path2D veins = new Path2D.Double();
                    veins.moveTo(edge, y + height * 0.25);
                    veins.lineTo(edge + side * 13, y + height * 0.37);
                    veins.lineTo(edge + side * 5, y + height * 0.5);
                    veins.lineTo(edge + side * 21, y + height * 0.77);
                    surface.draw(veins);
                }
                if (primary) {
                    surface.setPaint(new GradientPaint((float) bounds.getCenterX(),
                            (float) (y + height - 11), new Color(255, 92, 56, 190),
                            (float) x, (float) (y + height - 11), new Color(225, 21, 17, 0)));
                    surface.setStroke(new BasicStroke(1.5f));
                    surface.draw(new Line2D.Double(x + 28, y + height - 11,
                            x + width - 28, y + height - 11));
                }
            } finally {
                surface.dispose();
            }

            drawEndOrnament(g, x + 13, y + height * 0.5, primary ? 14 : 10, -1);
            drawEndOrnament(g, x + width - 13, y + height * 0.5, primary ? 14 : 10, 1);
            if (primary) {
                drawGem(g, bounds.getCenterX(), y + 7, 12, true);
                drawGem(g, bounds.getCenterX(), y + height - 7, 11, true);
            } else {
                g.setColor(new Color(244, 221, 183, 145));
                g.draw(new Line2D.Double(x + 32, y + 8, x + width - 32, y + 8));
            }

            String caption = switch (index) {
                case 0 -> "START";
                case 1 -> "SETTINGS";
                default -> "EXIT GAME";
            };
            Font font = GameUiTheme.title(primary ? 38f : 23f);
            Rectangle label = new Rectangle(bounds.x + 28, bounds.y + offset,
                    bounds.width - 56, bounds.height - offset);
            GameUiTheme.centeredText(g, caption, font, new Color(0, 0, 0, 230),
                    new Rectangle(label.x + 1, label.y + 2, label.width, label.height));
            GameUiTheme.centeredText(g, caption, font,
                    amount > 0.5 ? new Color(255, 245, 225) : IVORY, label);
        } finally {
            g.dispose();
        }
    }

    private static void drawTitle(Graphics2D g) {
        GlyphVector glyphs = LOGO_FONT.createGlyphVector(g.getFontRenderContext(), "VENORIA");
        Shape raw = glyphs.getOutline();
        Rectangle2D ink = raw.getBounds2D();
        double x = (WIDTH - ink.getWidth()) * 0.5 - ink.getX();
        double baseline = 427;
        AffineTransform placement = AffineTransform.getTranslateInstance(x, baseline);
        Shape logo = placement.createTransformedShape(raw);
        Rectangle2D letter = placement.createTransformedShape(glyphs.getGlyphOutline(3)).getBounds2D();
        double crestX = letter.getCenterX();
        double crestY = letter.getCenterY();

        drawSword(g, crestX, 261, 494);
        Shape subtitle = centeredOutline(g, "CHRONICLES OF", GameUiTheme.title(30f), 640, 311);
        metallicText(g, subtitle, 285, 312, false);
        metallicText(g, logo, 327, 427, true);

        // A fine blade and crimson compass echo the illustrated seal without hiding the title.
        double radius = Math.min(letter.getWidth(), letter.getHeight()) * 0.47;
        GameUiTheme.glow(g, crestX, crestY, radius * 1.75, new Color(241, 34, 17, 116), 0.68);
        g.setColor(new Color(35, 6, 8, 95));
        g.fill(new Ellipse2D.Double(crestX - radius + 4, crestY - radius + 4,
                radius * 2 - 8, radius * 2 - 8));
        g.setStroke(new BasicStroke(2.2f));
        g.setPaint(new GradientPaint((float) crestX, (float) (crestY - radius), IVORY,
                (float) crestX, (float) (crestY + radius), BRASS));
        g.draw(new Ellipse2D.Double(crestX - radius, crestY - radius, radius * 2, radius * 2));
        g.setColor(new Color(237, 103, 75, 150));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Ellipse2D.Double(crestX - radius + 5, crestY - radius + 5,
                radius * 2 - 10, radius * 2 - 10));
        Path2D compass = star(crestX, crestY, radius * 0.86, radius * 0.13);
        g.setColor(new Color(255, 181, 142, 95));
        g.setStroke(new BasicStroke(4f));
        g.draw(compass);
        g.setColor(new Color(255, 222, 181));
        g.fill(compass);
        g.setColor(new Color(180, 23, 19));
        g.fill(diamond(crestX, crestY, 6, 9));

        g.setStroke(new BasicStroke(1.2f));
        g.setPaint(new GradientPaint(280, 438, new Color(97, 15, 13, 0),
                (float) crestX, 438, new Color(247, 134, 98, 235)));
        g.draw(new Line2D.Double(280, 438, crestX - 15, 438));
        g.setPaint(new GradientPaint((float) crestX, 438, new Color(247, 134, 98, 235),
                1000, 438, new Color(97, 15, 13, 0)));
        g.draw(new Line2D.Double(crestX + 15, 438, 1000, 438));
        drawEndOrnament(g, crestX - 23, 439, 12, -1);
        drawEndOrnament(g, crestX + 23, 439, 12, 1);
    }

    private static void metallicText(Graphics2D g, Shape text, float top, float bottom, boolean large) {
        Graphics2D shadow = (Graphics2D) g.create();
        try {
            shadow.translate(2, large ? 5 : 3);
            shadow.setColor(new Color(0, 0, 0, 225));
            shadow.setStroke(new BasicStroke(large ? 9f : 5f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            shadow.draw(text);
            shadow.fill(text);
        } finally {
            shadow.dispose();
        }
        g.setStroke(new BasicStroke(large ? 4f : 2.5f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.setColor(new Color(88, 26, 18));
        g.draw(text);
        g.setStroke(new BasicStroke(large ? 1.8f : 1f));
        g.setColor(new Color(229, 193, 145));
        g.draw(text);
        g.setPaint(new LinearGradientPaint(0, top, 0, bottom,
                new float[] {0f, 0.33f, 0.57f, 0.6f, 0.82f, 1f},
                new Color[] {new Color(255, 243, 220), new Color(224, 221, 215),
                        new Color(249, 244, 230), new Color(149, 132, 115),
                        new Color(229, 209, 179), new Color(253, 235, 206)}));
        g.fill(text);
        Graphics2D engraving = (Graphics2D) g.create();
        try {
            engraving.clip(text);
            engraving.setStroke(new BasicStroke(0.5f));
            engraving.setColor(new Color(46, 24, 18, 24));
            for (int y = (int) top + 4; y < bottom; y += 6) {
                engraving.draw(new Line2D.Double(220, y, 1070, y - 5));
            }
        } finally {
            engraving.dispose();
        }
    }

    private static void drawSword(Graphics2D g, double x, double top, double bottom) {
        double guardY = 331;
        Path2D blade = new Path2D.Double();
        blade.moveTo(x, bottom);
        blade.lineTo(x - 9, guardY + 24);
        blade.lineTo(x - 5, guardY + 7);
        blade.lineTo(x + 5, guardY + 7);
        blade.lineTo(x + 9, guardY + 24);
        blade.closePath();
        g.setPaint(new GradientPaint((float) (x - 9), 0, new Color(60, 27, 20),
                (float) x, 0, PALE_GOLD, true));
        g.fill(blade);
        g.setColor(new Color(36, 15, 13));
        g.setStroke(new BasicStroke(2f));
        g.draw(blade);
        g.setStroke(new BasicStroke(0.9f));
        g.setColor(new Color(255, 223, 179, 220));
        g.draw(new Line2D.Double(x, guardY + 12, x, bottom - 3));
        g.setColor(new Color(129, 31, 19));
        g.draw(new Line2D.Double(x + 3, guardY + 20, x + 2, bottom - 23));

        Path2D handle = new Path2D.Double();
        handle.moveTo(x, top);
        handle.lineTo(x + 8, top + 21);
        handle.lineTo(x + 4, top + 43);
        handle.lineTo(x + 6, guardY - 3);
        handle.lineTo(x - 6, guardY - 3);
        handle.lineTo(x - 4, top + 43);
        handle.lineTo(x - 8, top + 21);
        handle.closePath();
        g.setPaint(new GradientPaint((float) (x - 7), 0, new Color(86, 41, 22),
                (float) (x + 4), 0, PALE_GOLD));
        g.fill(handle);
        g.setColor(new Color(19, 9, 7));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(handle);
        g.setColor(IVORY);
        g.setStroke(new BasicStroke(0.7f));
        g.draw(new Line2D.Double(x, top + 5, x, guardY - 6));
        drawEndOrnament(g, x - 14, guardY, 15, -1);
        drawEndOrnament(g, x + 14, guardY, 15, 1);
        drawGem(g, x, guardY, 9, true);
        drawGem(g, x, top + 25, 6, false);
    }

    private static void drawEmbers(Graphics2D g, double time) {
        for (int index = 0; index < 28; index++) {
            double phase = (time * (0.017 + index % 4 * 0.006) + index * 0.137) % 1;
            double x = (index * 149.3 + 79) % WIDTH + Math.sin(time * 0.55 + index) * 12;
            double y = HEIGHT + 18 - phase * (HEIGHT + 35);
            double alpha = Math.sin(phase * Math.PI) * 0.6;
            if (alpha < 0.03) continue;
            double radius = index % 5 == 0 ? 2.1 : 1.05;
            GameUiTheme.glow(g, x, y, radius * 5, new Color(234, 57, 26, 130), alpha);
            g.setColor(new Color(255, 158, 97, (int) (alpha * 220)));
            g.fill(new Ellipse2D.Double(x - radius * 0.5, y - radius,
                    radius, radius * 1.8));
        }
    }

    private static Shape centeredOutline(Graphics2D g, String value, Font font,
            double centerX, double baseline) {
        Shape shape = font.createGlyphVector(g.getFontRenderContext(), value).getOutline();
        Rectangle2D bounds = shape.getBounds2D();
        return AffineTransform.getTranslateInstance(centerX - bounds.getCenterX(), baseline)
                .createTransformedShape(shape);
    }

    private static Path2D frameShape(double x, double y, double width, double height,
            boolean primary) {
        double cut = primary ? 24 : 17;
        Path2D path = new Path2D.Double();
        path.moveTo(x + cut, y + 3);
        path.lineTo(x + width - cut, y + 3);
        path.lineTo(x + width - 8, y + height * 0.31);
        path.lineTo(x + width, y + height * 0.5);
        path.lineTo(x + width - 8, y + height * 0.69);
        path.lineTo(x + width - cut, y + height - 3);
        path.lineTo(x + cut, y + height - 3);
        path.lineTo(x + 8, y + height * 0.69);
        path.lineTo(x, y + height * 0.5);
        path.lineTo(x + 8, y + height * 0.31);
        path.closePath();
        return path;
    }

    private static void drawEndOrnament(Graphics2D g, double x, double y, double size, int direction) {
        Path2D ornament = new Path2D.Double();
        ornament.moveTo(x + direction * size, y);
        ornament.lineTo(x + direction * size * 0.25, y - size * 0.3);
        ornament.lineTo(x, y - size * 0.7);
        ornament.lineTo(x - direction * size * 0.25, y - size * 0.2);
        ornament.lineTo(x - direction * size * 0.62, y);
        ornament.lineTo(x - direction * size * 0.25, y + size * 0.2);
        ornament.lineTo(x, y + size * 0.7);
        ornament.lineTo(x + direction * size * 0.25, y + size * 0.3);
        ornament.closePath();
        g.setPaint(new GradientPaint((float) x, (float) (y - size), IVORY,
                (float) x, (float) (y + size), new Color(108, 63, 32)));
        g.fill(ornament);
        g.setStroke(new BasicStroke(0.8f));
        g.setColor(new Color(48, 25, 16));
        g.draw(ornament);
        g.setColor(new Color(242, 211, 165));
        g.draw(new Line2D.Double(x - direction * size * 0.25, y,
                x + direction * size * 0.63, y));
    }

    private static void drawGem(Graphics2D g, double x, double y, double size, boolean red) {
        Shape socket = diamond(x, y, size * 0.7, size);
        g.setPaint(new GradientPaint((float) x, (float) (y - size), IVORY,
                (float) x, (float) (y + size), BRASS));
        g.fill(socket);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(45, 19, 14));
        g.draw(socket);
        Shape gem = diamond(x, y, size * 0.42, size * 0.67);
        g.setPaint(new GradientPaint((float) x, (float) (y - size * 0.7),
                red ? new Color(255, 143, 103) : new Color(255, 245, 210),
                (float) x, (float) (y + size * 0.7),
                red ? new Color(119, 7, 10) : new Color(145, 96, 47)));
        g.fill(gem);
        g.setColor(new Color(255, 237, 203, 210));
        g.draw(new Line2D.Double(x, y - size * 0.5, x, y));
    }

    private static Path2D diamond(double x, double y, double horizontal, double vertical) {
        Path2D path = new Path2D.Double();
        path.moveTo(x, y - vertical);
        path.lineTo(x + horizontal, y);
        path.lineTo(x, y + vertical);
        path.lineTo(x - horizontal, y);
        path.closePath();
        return path;
    }

    private static Path2D star(double x, double y, double outer, double inner) {
        Path2D path = new Path2D.Double();
        for (int index = 0; index < 8; index++) {
            double angle = -Math.PI * 0.5 + index * Math.PI * 0.25;
            double radius = index % 2 == 0 ? outer : inner;
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            if (index == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.closePath();
        return path;
    }

    private static Graphics2D prepared(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }
}
