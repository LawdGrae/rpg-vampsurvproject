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
        AbilityClass requestedClass = args.length == 0 ? null
                : AbilityClass.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
        int index = 0;
        for (AbilityDefinition definition : AbilityDefinition.createAll()) {
            if (requestedClass != null && definition.getAbilityClass() != requestedClass) {
                continue;
            }
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
        Color secondary = definition.getAbilityClass() == AbilityClass.GUARDIAN
                ? new Color(255, 226, 158) : secondaryColor(definition.getEffectType());
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
        if (definition.getAbilityClass() == AbilityClass.GUARDIAN) {
            drawGuardianSigil(graphics, definition, primary, secondary);
            return;
        }
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
            case GUARDIAN -> {
                graphics.drawArc(290, 252, 444, 536, 25, 130);
                graphics.drawLine(512, 272, 512, 756);
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

    /** Guardian icons keep distinct physical shield, charge, rock and roar silhouettes. */
    private static void drawGuardianSigil(Graphics2D graphics,
            AbilityDefinition definition, Color gold, Color highlight) {
        graphics.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        switch (definition.getId()) {
            case "shield_fortress" -> {
                graphics.setColor(withAlpha(gold, 180));
                Path2D barrier = new Path2D.Double();
                barrier.moveTo(278, 286);
                barrier.lineTo(512, 226);
                barrier.lineTo(746, 286);
                barrier.lineTo(716, 588);
                barrier.lineTo(512, 806);
                barrier.lineTo(308, 588);
                barrier.closePath();
                graphics.draw(barrier);
                drawGuardianShield(graphics, 512, 496, 0.75, 0, gold, highlight);
                graphics.setColor(highlight);
                graphics.drawLine(282, 464, 230, 448);
                graphics.drawLine(744, 464, 794, 448);
                graphics.drawLine(336, 280, 306, 232);
                graphics.drawLine(688, 280, 718, 232);
            }
            case "iron_charge" -> {
                graphics.setColor(gold);
                graphics.setStroke(new BasicStroke(34f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                graphics.drawLine(246, 426, 492, 426);
                graphics.drawLine(200, 512, 490, 512);
                graphics.drawLine(246, 598, 492, 598);
                drawGuardianShield(graphics, 616, 508, 0.6, -0.18, gold, highlight);
                graphics.setColor(highlight);
                graphics.drawLine(730, 392, 806, 508);
                graphics.drawLine(806, 508, 730, 624);
            }
            case "earthbreaker" -> {
                graphics.setColor(new Color(176, 150, 110));
                graphics.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                graphics.drawLine(240, 710, 388, 710);
                graphics.drawLine(638, 710, 790, 710);
                graphics.setColor(highlight);
                Path2D crack = new Path2D.Double();
                crack.moveTo(514, 666);
                crack.lineTo(416, 746);
                crack.lineTo(442, 774);
                crack.lineTo(348, 826);
                crack.moveTo(514, 666);
                crack.lineTo(608, 744);
                crack.lineTo(584, 768);
                crack.lineTo(676, 814);
                graphics.draw(crack);
                drawGuardianShield(graphics, 514, 482, 0.64, 0, gold, highlight);
                graphics.setColor(gold);
                graphics.fillPolygon(new int[] {270, 312, 346, 300},
                        new int[] {530, 484, 586, 624}, 4);
                graphics.fillPolygon(new int[] {690, 744, 776, 724},
                        new int[] {544, 480, 580, 616}, 4);
                graphics.drawLine(326, 696, 270, 654);
                graphics.drawLine(700, 696, 766, 654);
            }
            case "guardians_roar" -> {
                graphics.setColor(gold);
                graphics.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                graphics.drawArc(254, 286, 516, 452, 300, 120);
                graphics.drawArc(194, 234, 636, 556, 300, 120);
                graphics.drawArc(294, 300, 436, 416, 120, 120);
                graphics.setColor(new Color(53, 61, 70));
                Path2D helm = new Path2D.Double();
                helm.moveTo(432, 700);
                helm.lineTo(392, 420);
                helm.lineTo(452, 306);
                helm.lineTo(566, 306);
                helm.lineTo(626, 418);
                helm.lineTo(610, 524);
                helm.lineTo(678, 560);
                helm.lineTo(604, 598);
                helm.lineTo(584, 700);
                helm.closePath();
                graphics.fill(helm);
                graphics.setColor(highlight);
                graphics.draw(helm);
                graphics.drawLine(428, 438, 600, 438);
                graphics.drawLine(548, 458, 548, 606);
                graphics.drawLine(580, 568, 646, 568);
            }
            case "unbreakable" -> {
                drawGuardianShield(graphics, 512, 508, 0.72, 0, gold, highlight);
                graphics.setColor(highlight);
                graphics.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                graphics.drawLine(512, 374, 512, 646);
                graphics.drawLine(448, 460, 576, 460);
                graphics.setColor(gold);
                graphics.drawArc(270, 240, 484, 552, 42, 96);
                graphics.drawLine(326, 310, 294, 268);
                graphics.drawLine(698, 310, 730, 268);
                graphics.drawLine(512, 220, 512, 182);
            }
            default -> drawGuardianShield(graphics, 512, 508, 0.72, 0,
                    gold, highlight);
        }
    }

    private static void drawGuardianShield(Graphics2D graphics, double x, double y,
            double scale, double rotation, Color gold, Color highlight) {
        AffineTransform saved = graphics.getTransform();
        graphics.translate(x, y);
        graphics.rotate(rotation);
        graphics.scale(scale, scale);
        Path2D shield = new Path2D.Double();
        shield.moveTo(0, -296);
        shield.lineTo(194, -240);
        shield.lineTo(188, 82);
        shield.lineTo(130, 214);
        shield.lineTo(0, 326);
        shield.lineTo(-130, 214);
        shield.lineTo(-188, 82);
        shield.lineTo(-194, -240);
        shield.closePath();
        graphics.setPaint(new GradientPaint(-194, -240, new Color(98, 112, 130),
                194, 240, new Color(32, 40, 54)));
        graphics.fill(shield);
        graphics.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        graphics.setColor(gold);
        graphics.draw(shield);
        graphics.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        graphics.setColor(highlight);
        graphics.draw(shield);
        graphics.setColor(gold);
        graphics.drawLine(0, -230, 0, 248);
        graphics.setColor(new Color(252, 216, 130));
        graphics.fillPolygon(new int[] {0, 80, 62, 0, -62, -80},
                new int[] {-154, -104, -14, 58, -14, -104}, 6);
        graphics.setColor(new Color(48, 38, 28));
        graphics.fillOval(-44, -94, 22, 14);
        graphics.fillOval(22, -94, 22, 14);
        graphics.fillPolygon(new int[] {-24, 24, 0}, new int[] {-48, -48, -24}, 3);
        graphics.setTransform(saved);
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
            case GUARDIAN -> new Color(235, 170, 52);
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
