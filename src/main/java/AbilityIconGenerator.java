import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class AbilityIconGenerator {
    private static final int SIZE = 1024;
    private static final int CENTER = SIZE / 2;

    public static void main(String[] args) throws IOException {
        Path outputDirectory = Path.of("src", "main", "resources", "abilities");
        Files.createDirectories(outputDirectory);
        int index = 0;
        for (AbilityDefinition definition : AbilityDefinition.createAll()) {
            BufferedImage image = createIcon(definition, index);
            ImageIO.write(image, "png",
                    outputDirectory.resolve(definition.getId() + ".png").toFile());
            index++;
        }
    }

    private static BufferedImage createIcon(AbilityDefinition definition, int index) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, SIZE, SIZE);
        graphics.setComposite(AlphaComposite.SrcOver);

        Color primary = primaryColor(definition.getAbilityClass());
        Color secondary = secondaryColor(definition.getEffectType());
        int variant = Math.abs(definition.getId().hashCode());

        drawGlow(graphics, primary, secondary, variant);
        drawSigil(graphics, definition, primary, secondary, variant);
        drawHighlights(graphics, primary, secondary, variant);

        graphics.dispose();
        return image;
    }

    private static void drawGlow(Graphics2D graphics, Color primary,
            Color secondary, int variant) {
        for (int ring = 0; ring < 7; ring++) {
            int radius = 390 - ring * 38;
            int alpha = 18 + ring * 10;
            graphics.setColor(new Color(primary.getRed(), primary.getGreen(),
                    primary.getBlue(), alpha));
            graphics.fillOval(CENTER - radius, CENTER - radius, radius * 2, radius * 2);
        }
        graphics.setPaint(new GradientPaint(230, 220, withAlpha(primary, 215),
                810, 790, withAlpha(secondary, 205)));
        graphics.fillOval(238, 238, 548, 548);

        graphics.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        graphics.setColor(Color.BLACK);
        graphics.fillOval(290, 290, 432, 432);
        graphics.setComposite(AlphaComposite.SrcOver);

        graphics.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(secondary, 180));
        graphics.drawOval(228, 228, 568, 568);
        graphics.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(Color.WHITE, 150));
        graphics.drawOval(260, 260, 504, 504);
    }

    private static void drawSigil(Graphics2D graphics, AbilityDefinition definition,
            Color primary, Color secondary, int variant) {
        graphics.setStroke(new BasicStroke(38f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(Color.BLACK, 90));
        drawEffectShape(graphics, definition.getEffectType(), 12, variant);
        graphics.setStroke(new BasicStroke(30f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(secondary, 235));
        drawEffectShape(graphics, definition.getEffectType(), 0, variant);
        graphics.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(Color.WHITE, 210));
        drawEffectShape(graphics, definition.getEffectType(), -8, variant);

        drawClassAccent(graphics, definition.getAbilityClass(), primary, variant);
    }

    private static void drawEffectShape(Graphics2D graphics, AbilityEffectType effect,
            int offset, int variant) {
        switch (effect) {
            case SINGLE_TARGET -> drawBlade(graphics, offset, variant);
            case AREA_DAMAGE -> drawBurst(graphics, offset, variant);
            case POISON -> drawDroplet(graphics, offset, variant);
            case SLOW -> drawSnowflake(graphics, offset, variant);
            case STUN -> drawLightning(graphics, offset, variant);
            case DASH -> drawDash(graphics, offset, variant);
            case HEAL -> drawHaloCross(graphics, offset, variant);
            case SHIELD -> drawShield(graphics, offset, variant);
            case BUFF -> drawRuneSwirl(graphics, offset, variant);
            case MARK -> drawMark(graphics, offset, variant);
            case EXECUTE -> drawCrescent(graphics, offset, variant);
            case SUMMON -> drawPortal(graphics, offset, variant);
            case ULTIMATE -> drawUltimateStar(graphics, offset, variant);
            default -> drawBurst(graphics, offset, variant);
        }
    }

    private static void drawClassAccent(Graphics2D graphics, AbilityClass abilityClass,
            Color primary, int variant) {
        graphics.setColor(withAlpha(primary, 210));
        graphics.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (abilityClass) {
            case ASSASSIN -> {
                graphics.drawArc(270, 300, 480, 430, 210, 105);
                graphics.drawArc(270, 300, 480, 430, 25, 105);
            }
            case BLACK_KNIGHT -> {
                graphics.drawLine(512, 244, 512, 780);
                graphics.drawArc(330, 285, 364, 420, 205, 130);
            }
            case PRIEST -> {
                graphics.drawOval(340, 258, 344, 344);
                graphics.drawArc(306, 370, 412, 290, 215, 110);
            }
            case RANGER -> {
                graphics.drawArc(305, 238, 470, 540, 112, 136);
                graphics.drawLine(334, 512, 716, 512);
            }
            case WARLOCK -> {
                graphics.drawArc(292, 292, 440, 440, 35, 290);
                graphics.drawLine(350, 710, 680, 310);
            }
            case ELEMENTALIST -> {
                graphics.drawOval(326, 326, 372, 372);
                graphics.drawLine(512, 230, 512, 794);
            }
            default -> {
            }
        }
    }

    private static void drawBlade(Graphics2D graphics, int offset, int variant) {
        Path2D path = new Path2D.Double();
        path.moveTo(350 + offset, 725 - offset);
        path.curveTo(430, 510, 540, 350, 700 - offset, 260 + offset);
        path.curveTo(650, 440, 560, 600, 420 + offset, 780 - offset);
        path.closePath();
        graphics.fill(path);
    }

    private static void drawBurst(Graphics2D graphics, int offset, int variant) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0 + (variant % 18) * Math.PI / 180.0;
            int inner = 92 + offset;
            int outer = 275 + offset;
            graphics.drawLine((int) (CENTER + Math.cos(angle) * inner),
                    (int) (CENTER + Math.sin(angle) * inner),
                    (int) (CENTER + Math.cos(angle) * outer),
                    (int) (CENTER + Math.sin(angle) * outer));
        }
        graphics.fillOval(422 + offset, 422 + offset, 180 - offset, 180 - offset);
    }

    private static void drawDroplet(Graphics2D graphics, int offset, int variant) {
        Path2D path = new Path2D.Double();
        path.moveTo(CENTER, 250 + offset);
        path.curveTo(690, 455, 650, 745, CENTER, 780 - offset);
        path.curveTo(370, 745, 334, 455, CENTER, 250 + offset);
        path.closePath();
        graphics.fill(path);
        graphics.fillOval(610, 610, 72, 72);
        graphics.fillOval(348, 650, 54, 54);
    }

    private static void drawSnowflake(Graphics2D graphics, int offset, int variant) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * i / 3.0;
            int outer = 270 + offset;
            int branch = 92;
            int x = (int) (CENTER + Math.cos(angle) * outer);
            int y = (int) (CENTER + Math.sin(angle) * outer);
            graphics.drawLine(CENTER, CENTER, x, y);
            graphics.drawLine(x, y, (int) (x - Math.cos(angle + 0.75) * branch),
                    (int) (y - Math.sin(angle + 0.75) * branch));
            graphics.drawLine(x, y, (int) (x - Math.cos(angle - 0.75) * branch),
                    (int) (y - Math.sin(angle - 0.75) * branch));
        }
    }

    private static void drawLightning(Graphics2D graphics, int offset, int variant) {
        Path2D path = new Path2D.Double();
        path.moveTo(570, 218 + offset);
        path.lineTo(390, 545);
        path.lineTo(520, 545);
        path.lineTo(450, 806 - offset);
        path.lineTo(655, 450);
        path.lineTo(525, 450);
        path.closePath();
        graphics.fill(path);
    }

    private static void drawDash(Graphics2D graphics, int offset, int variant) {
        graphics.drawArc(260, 355 + offset, 520, 260, 190, 220);
        graphics.drawLine(600, 300 + offset, 760, 430);
        graphics.drawLine(600, 560 - offset, 760, 430);
    }

    private static void drawHaloCross(Graphics2D graphics, int offset, int variant) {
        graphics.draw(new Ellipse2D.Double(340, 250 + offset, 344, 120));
        graphics.fillRoundRect(470, 365, 84, 330, 42, 42);
        graphics.fillRoundRect(350, 488, 324, 84, 42, 42);
    }

    private static void drawShield(Graphics2D graphics, int offset, int variant) {
        Path2D path = new Path2D.Double();
        path.moveTo(CENTER, 235 + offset);
        path.curveTo(640, 285, 720, 315, 735, 330);
        path.curveTo(720, 570, 650, 710, CENTER, 790 - offset);
        path.curveTo(374, 710, 304, 570, 289, 330);
        path.curveTo(304, 315, 384, 285, CENTER, 235 + offset);
        path.closePath();
        graphics.fill(path);
    }

    private static void drawRuneSwirl(Graphics2D graphics, int offset, int variant) {
        for (int i = 0; i < 3; i++) {
            graphics.draw(new Arc2D.Double(286 + i * 58, 286 + i * 58 + offset,
                    452 - i * 116, 452 - i * 116, 20 + i * 52, 260, Arc2D.OPEN));
        }
    }

    private static void drawMark(Graphics2D graphics, int offset, int variant) {
        graphics.drawOval(314, 314, 396, 396);
        graphics.drawLine(512, 250 + offset, 512, 774 - offset);
        graphics.drawLine(250 + offset, 512, 774 - offset, 512);
        graphics.fillOval(462, 462, 100, 100);
    }

    private static void drawCrescent(Graphics2D graphics, int offset, int variant) {
        graphics.fillOval(305, 240 + offset, 440, 560);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillOval(425, 200 + offset, 390, 620);
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private static void drawPortal(Graphics2D graphics, int offset, int variant) {
        AffineTransform oldTransform = graphics.getTransform();
        graphics.rotate(Math.toRadians(variant % 45), CENTER, CENTER);
        graphics.drawOval(296, 296 + offset, 432, 432);
        graphics.drawOval(362, 362 + offset, 300, 300);
        graphics.drawLine(CENTER, 250, CENTER, 774);
        graphics.setTransform(oldTransform);
    }

    private static void drawUltimateStar(Graphics2D graphics, int offset, int variant) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 16; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / 16.0;
            double radius = i % 2 == 0 ? 300 + offset : 125 + offset;
            double x = CENTER + Math.cos(angle) * radius;
            double y = CENTER + Math.sin(angle) * radius;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        graphics.fill(path);
    }

    private static void drawHighlights(Graphics2D graphics, Color primary,
            Color secondary, int variant) {
        graphics.setColor(withAlpha(Color.WHITE, 180));
        graphics.fillOval(340 + variant % 80, 320, 72, 72);
        graphics.setColor(withAlpha(secondary, 130));
        graphics.fillOval(650 - variant % 90, 650, 96, 96);
        graphics.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(primary, 180));
        graphics.drawArc(190, 190, 644, 644, variant % 360, 78);
    }

    private static Color primaryColor(AbilityClass abilityClass) {
        return switch (abilityClass) {
            case ASSASSIN -> new Color(126, 70, 210);
            case BLACK_KNIGHT -> new Color(175, 38, 48);
            case PRIEST -> new Color(255, 235, 126);
            case RANGER -> new Color(80, 215, 118);
            case WARLOCK -> new Color(125, 42, 170);
            case ELEMENTALIST -> new Color(70, 180, 255);
        };
    }

    private static Color secondaryColor(AbilityEffectType effectType) {
        return switch (effectType) {
            case POISON -> new Color(85, 255, 95);
            case SLOW -> new Color(120, 230, 255);
            case STUN -> new Color(255, 226, 75);
            case HEAL -> new Color(255, 250, 190);
            case SHIELD -> new Color(120, 170, 255);
            case MARK, EXECUTE -> new Color(255, 70, 170);
            case ULTIMATE -> new Color(255, 130, 70);
            default -> new Color(235, 245, 255);
        };
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
