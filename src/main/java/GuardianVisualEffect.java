import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Physical Guardian effects driven by the same clock and anchors as combat. */
final class GuardianVisualEffect {
    private static final Color GOLD = new Color(235, 181, 75);
    private static final Color LIGHT_GOLD = new Color(255, 226, 145);
    private static final Color STEEL = new Color(175, 190, 200);
    private static final Color DARK_STEEL = new Color(47, 61, 72);
    private static final Color CRACK = new Color(40, 33, 26);
    private static final Color ROCK = new Color(113, 98, 78);
    private static final Color ROCK_LIGHT = new Color(172, 145, 109);
    private static final Color DUST = new Color(153, 129, 94);
    private static final BasicStroke FINE = stroke(0.9f);
    private static final BasicStroke SMALL = stroke(1.5f);
    private static final BasicStroke EDGE = stroke(2.4f);
    private static final BasicStroke HEAVY = stroke(4.8f);
    private static final BasicStroke WIDE = stroke(8f);
    private static final AlphaComposite[] OPACITY = opacityCache();
    private static final BufferedImage DUST_SPRITE = dustSprite();
    private static final Path2D SHIELD = shieldShape(1.0);
    private static final Path2D SHIELD_INSET = shieldShape(0.83);
    private static final double[] ROCK_X = {-0.62, -0.78, -0.22, 0.62, 0.76, 0.21};
    private static final double[] ROCK_Y = {0.53, -0.23, -0.81, -0.49, 0.24, 0.72};

    private final String id;
    private final double radius, maxLife, hitAge;
    private final BasicStroke shieldFine, shieldSmall;
    private final double directionX, directionY;
    private final double[] seeds = new double[96];
    private final double[] trailX = new double[24];
    private final double[] trailY = new double[24];
    private final double[] trailBirth = new double[24];
    private final Path2D path = new Path2D.Double();
    private final Path2D secondaryPath = new Path2D.Double();
    private final Line2D line = new Line2D.Double();
    private final Ellipse2D ellipse = new Ellipse2D.Double();
    private double casterX, casterY, shieldX, shieldY, impactX, impactY;
    private double previousCasterX, previousCasterY;
    private double age, nextTrailSample, blockAge = 1, blockX, blockY;
    private int trailIndex;
    private boolean impactFrozen;

    static boolean supports(String id) {
        return switch (id) {
            case "shield_fortress", "iron_charge", "earthbreaker", "guardians_roar", "unbreakable" -> true;
            default -> false;
        };
    }

    GuardianVisualEffect(AbilityDefinition definition, double startX, double startY,
            double targetX, double targetY, double radius, double maxLife) {
        id = definition.getId();
        this.radius = radius;
        this.maxLife = maxLife;
        double barrierHeight = Math.max(61, Math.min(94, radius * 0.62));
        shieldFine = stroke((float) (FINE.getLineWidth() / barrierHeight));
        shieldSmall = stroke((float) (SMALL.getLineWidth() / barrierHeight));
        hitAge = AbilityAnimationTiming.duration(definition)
                * AbilityAnimationTiming.hitProgress(definition)[0];
        casterX = shieldX = impactX = startX;
        casterY = shieldY = impactY = startY;
        previousCasterX = casterX;
        previousCasterY = casterY;
        double length = Math.hypot(targetX - startX, targetY - startY);
        directionX = length > 0.001 ? (targetX - startX) / length : 1;
        directionY = length > 0.001 ? (targetY - startY) / length : 0;
        int seed = id.hashCode();
        for (int index = 0; index < seeds.length; index++) {
            int value = seed ^ (index + 1) * 0x45d9f3b;
            value ^= value >>> 16;
            value *= 0x45d9f3b;
            value ^= value >>> 16;
            seeds[index] = (value & 0x7fffffff) / (double) Integer.MAX_VALUE;
        }
        java.util.Arrays.fill(trailBirth, -10);
        nextTrailSample = hitAge;
    }

    void setCasterPosition(double x, double y) {
        if (Double.isFinite(x) && Double.isFinite(y)) {
            casterX = x; casterY = y;
            if (age == 0) { previousCasterX = x; previousCasterY = y; }
        }
    }

    void setShieldPosition(double x, double y) {
        if (Double.isFinite(x) && Double.isFinite(y)) { shieldX = x; shieldY = y; }
    }

    void freezeImpactOrigin(double x, double y) {
        if (!impactFrozen && Double.isFinite(x) && Double.isFinite(y)) {
            impactX = x;
            impactY = y;
            impactFrozen = true;
        }
    }

    void flashBarrier(double x, double y) {
        if (!id.equals("shield_fortress") || !Double.isFinite(x) || !Double.isFinite(y)) return;
        blockX = x;
        blockY = y;
        blockAge = 0;
    }

    void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime <= 0) return;
        double previousAge = age;
        age = Math.min(maxLife, age + deltaTime);
        blockAge += deltaTime;
        if (!impactFrozen && age >= hitAge) {
            // Combat normally supplies the exact pose at the event. This also makes
            // previewing a stand-alone effect deterministic.
            if (id.equals("earthbreaker")) freezeImpactOrigin(shieldX, shieldY);
            else if (id.equals("guardians_roar")) freezeImpactOrigin(casterX, casterY);
        }
        if (id.equals("iron_charge")) {
            while (nextTrailSample <= Math.min(age, hitAge + 0.5) + 1e-10) {
                double fraction = clamp((nextTrailSample - previousAge) / deltaTime);
                trailX[trailIndex] = previousCasterX + (casterX - previousCasterX) * fraction
                        - directionX * 14;
                trailY[trailIndex] = previousCasterY + (casterY - previousCasterY) * fraction
                        + 22 - directionY * 14;
                trailBirth[trailIndex] = nextTrailSample;
                trailIndex = (trailIndex + 1) % trailX.length;
                nextTrailSample += 0.028;
            }
        }
        previousCasterX = casterX;
        previousCasterY = casterY;
    }

    void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (age >= maxLife) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centerX + cameraX, centerY + cameraY);
        switch (id) {
            case "shield_fortress" -> fortress(g);
            case "iron_charge" -> { windup(g); charge(g); }
            case "earthbreaker" -> { windup(g); earthbreaker(g); }
            case "guardians_roar" -> { windup(g); roar(g); }
            case "unbreakable" -> passive(g);
            default -> { }
        }
        g.dispose();
    }

    private void windup(Graphics2D g) {
        double alpha = smooth(age / 0.13)
                * (1 - smooth((age - hitAge + 0.055) / 0.09));
        if (alpha < 0.003) return;
        // A small metallic rim accent follows the preparing weapon. Ground impact
        // and travelling pressure effects remain absent until the shared hit event.
        segment(g, shieldX - 5, shieldY - 12, shieldX - 7, shieldY + 5,
                STEEL, FINE, alpha * 0.52);
        segment(g, shieldX + 6, shieldY - 8, shieldX + 6, shieldY + 9,
                GOLD, FINE, alpha * 0.64);
        for (int i = 0; i < 4; i++) {
            double t = (age * 1.3 + seeds[i]) % 1;
            spark(g, shieldX + (seeds[i + 4] - 0.5) * 19,
                    shieldY + 9 - t * 24, 1.1,
                    i % 2 == 0 ? STEEL : LIGHT_GOLD, alpha * Math.sin(t * Math.PI) * 0.46);
        }
    }

    private void fortress(Graphics2D g) {
        double reveal = smooth((age - hitAge) / 0.15);
        double fade = 1 - smooth((age - maxLife + 0.36) / 0.36);
        double alpha = reveal * fade;
        if (alpha > 0.005) {
            double height = Math.max(61, Math.min(94, radius * 0.62));
            double width = height * (0.57 + Math.abs(directionX) * 0.12);
            double assembly = 0.78 + reveal * 0.22;
            double x = shieldX + directionX * 5, y = shieldY - 4;
            g.translate(x, y);
            g.scale(width * assembly, height * assembly);
            fill(g, SHIELD, DARK_STEEL, alpha * 0.17);
            // Faceted steel plates and gold ribs grow directly out of the held shield.
            outline(g, SHIELD, GOLD, shieldFine, alpha * 0.78);
            outline(g, SHIELD_INSET, STEEL, shieldFine, alpha * 0.52);
            path.reset();
            path.moveTo(0, -0.48); path.lineTo(0, 0.56);
            path.moveTo(-0.45, -0.31); path.lineTo(0, -0.1); path.lineTo(0.45, -0.31);
            path.moveTo(-0.42, 0.1); path.lineTo(0, 0.31); path.lineTo(0.42, 0.1);
            outline(g, path, GOLD, shieldFine, alpha * 0.42);
            if (blockAge < 0.18) {
                double flash = 1 - smooth(blockAge / 0.18);
                fill(g, SHIELD, LIGHT_GOLD, flash * 0.27);
                outline(g, SHIELD, LIGHT_GOLD, shieldSmall, flash);
            }
            g.scale(1 / (width * assembly), 1 / (height * assembly));
            g.translate(-x, -y);
            // Two visible tethers meet at the weapon face, so the structure cannot float.
            segment(g, shieldX, shieldY, x - width * 0.31, y - height * 0.2,
                    GOLD, SMALL, alpha * 0.28);
            segment(g, shieldX, shieldY, x + width * 0.31, y + height * 0.1,
                    GOLD, SMALL, alpha * 0.28);
        }
        double anticipation = age < hitAge ? smooth(age / 0.15) * 0.3 : alpha;
        for (int i = 0; i < 9; i++) {
            double t = (age * 0.43 + seeds[i]) % 1;
            double x = shieldX + (seeds[i + 9] - 0.5) * 43;
            double y = shieldY + 25 - t * 67;
            double pa = anticipation * Math.sin(t * Math.PI) * 0.6;
            spark(g, x, y, 1.8, GOLD, pa);
        }
        if (blockAge < 0.18) contactSparks(g, blockX, blockY, blockAge / 0.18,
                1 - smooth(blockAge / 0.18), 30);
    }

    private void charge(Graphics2D g) {
        for (int i = 0; i < trailX.length; i++) {
            double t = (age - trailBirth[i]) / 0.42;
            if (t < 0 || t >= 1) continue;
            double spread = 10 + seeds[i] * 6 + t * 18;
            dust(g, trailX[i] - directionX * t * 16,
                    trailY[i] - directionY * t * 16 - t * 4, spread,
                    Math.sin(Math.PI * Math.min(1, t * 2)) * (1 - t) * 0.49);
        }
        double active = smooth((age - hitAge) / 0.05)
                * (1 - smooth((age - hitAge - 0.43) / 0.12));
        if (active < 0.003) return;
        // Small glints scrape along the physical leading shield, never a colored dash.
        for (int i = 0; i < 8; i++) {
            double t = ((age - hitAge) * 3.7 + seeds[i]) % 1;
            double x = shieldX + directionX * (7 - t * 16)
                    - directionY * (seeds[i + 8] - 0.5) * 32;
            double y = shieldY + directionY * (7 - t * 16)
                    + directionX * (seeds[i + 8] - 0.5) * 32 + t * t * 10;
            segment(g, x, y, x + directionX * (3 + t * 4),
                    y + directionY * (3 + t * 4) - 1,
                    i % 3 == 0 ? STEEL : LIGHT_GOLD, FINE,
                    active * Math.sin(t * Math.PI) * 0.85);
        }
    }

    private void earthbreaker(Graphics2D g) {
        double time = age - hitAge;
        if (time < 0 || !impactFrozen) return;
        double reach = smooth(time / 0.31);
        double fade = 1 - smooth((age - maxLife + 0.42) / 0.42);
        double aim = Math.atan2(directionY, directionX);
        // Branching cracks spread through the ground from the shield's exact lower tip.
        for (int ray = 0; ray < 11; ray++) {
            double angle = ray < 8 ? aim + (ray - 3.5) * 0.37
                    : aim + Math.PI + (ray - 9) * 0.57;
            double length = radius * (0.44 + seeds[ray] * 0.46);
            path.reset(); path.moveTo(impactX, impactY);
            double lastX = impactX, lastY = impactY;
            for (int step = 1; step <= 6; step++) {
                double fraction = Math.min(reach, step / 6.0);
                double bend = Math.sin(step * 2.6 + seeds[ray + 11] * 6) * length * 0.058;
                double x = impactX + Math.cos(angle) * length * fraction - Math.sin(angle) * bend * fraction;
                double y = impactY + Math.sin(angle) * length * fraction * 0.7 + Math.cos(angle) * bend * fraction;
                path.lineTo(x, y);
                if (step == 3 && reach > 0.45) {
                    secondaryPath.reset(); secondaryPath.moveTo(x, y);
                    secondaryPath.lineTo(x + Math.cos(angle + 0.65) * length * 0.18 * reach,
                            y + Math.sin(angle + 0.65) * length * 0.13 * reach);
                    outline(g, secondaryPath, CRACK, SMALL, fade * 0.8);
                }
                lastX = x; lastY = y;
                if (fraction >= reach) break;
            }
            outline(g, path, CRACK, HEAVY, fade * 0.77);
            outline(g, path, GOLD, FINE, fade * (1 - smooth(time / 0.55)) * 0.8);
            if (time < 0.38) dust(g, lastX, lastY, 11 + time * 25, fade * 0.19);
        }
        double waveTime = clamp(time / 0.64);
        double waveReach = radius * (0.13 + easeOut(waveTime) * 0.82);
        directionalWave(g, impactX, impactY, aim, waveReach,
                (1 - waveTime) * fade * 0.65, time);
        for (int i = 0; i < 17; i++) {
            double angle = aim + (seeds[i] - 0.5) * 4.1;
            double travel = (26 + seeds[i + 17] * radius * 0.35) * time;
            double up = (100 + seeds[i + 34] * 93) * time - 208 * time * time;
            double groundX = impactX + Math.cos(angle) * travel;
            double groundY = impactY + Math.sin(angle) * travel * 0.65;
            double bounce = Math.max(0, up);
            if (up < 0 && time > 0.48) {
                dust(g, groundX, groundY, 9 + time * 13, fade * 0.24);
            }
            double size = 3 + seeds[i + 51] * 6;
            if (bounce > 0) {
                ellipse.setFrame(groundX - size, groundY - size * 0.26, size * 2, size * 0.52);
                fill(g, ellipse, CRACK, fade * 0.18);
            }
            rock(g, groundX, groundY - bounce, size,
                    seeds[i + 68] * 3 + time * (seeds[i] - 0.5) * 9, fade);
        }
        if (time < 0.32) contactSparks(g, impactX, impactY, time / 0.32,
                1 - smooth(time / 0.32), 37);
        for (int i = 0; i < 8; i++) {
            double angle = aim + (i - 3.5) * 0.5;
            double distance = radius * (0.07 + seeds[i] * 0.11 + easeOut(clamp(time)) * 0.25);
            dust(g, impactX + Math.cos(angle) * distance,
                    impactY + Math.sin(angle) * distance * 0.56 - time * 7,
                    17 + seeds[i + 8] * 12 + time * 25,
                    fade * smooth(time / 0.06) * (1 - clamp(time / 1.2)) * 0.32);
        }
    }

    private void roar(Graphics2D g) {
        double time = age - hitAge;
        if (time < 0 || !impactFrozen) return;
        double t = clamp(time / Math.max(0.62, maxLife - hitAge));
        // Gameplay reaches each enemy on this same linear .55-second front.
        double reach = radius * clamp(time / 0.55);
        double alpha = smooth(time / 0.065) * (1 - smooth((t - 0.46) / 0.54));
        // Pressure fronts are broad broken ribbons with irregular crests and open gaps.
        // Their expansion reads as moving air; no filled circular aura is used.
        for (int lobe = 0; lobe < 3; lobe++) {
            double from = lobe * Math.PI * 2 / 3 + 0.16;
            double to = from + Math.PI * 2 / 3 - 0.37;
            pressureRibbon(g, impactX, impactY, reach, from, to,
                    Math.max(2, 8 * (1 - t)), alpha * 0.58);
            pressureRibbon(g, impactX, impactY, reach * 0.83,
                    from + 0.13, to - 0.09, 2.5, alpha * 0.27);
        }
        for (int i = 0; i < 15; i++) {
            double angle = i * Math.PI * 2 / 15 + seeds[i] * 0.1;
            double x = impactX + Math.cos(angle) * reach;
            double y = impactY + 23 + Math.sin(angle) * reach * 0.57;
            dust(g, x, y, 10 + t * 17, alpha * 0.2);
            if (i % 2 == 0) segment(g, x, y - 7, x + Math.cos(angle) * 10,
                    y + Math.sin(angle) * 6 - 7, LIGHT_GOLD, FINE, alpha * 0.38);
        }
    }

    private void passive(Graphics2D g) {
        double alpha = smooth(age / 0.2) * (1 - smooth((age - maxLife + 0.25) / 0.25));
        for (int i = 0; i < 5; i++) {
            double t = (age * 0.32 + seeds[i]) % 1;
            spark(g, casterX + (seeds[i + 5] - 0.5) * 35,
                    casterY + 18 - t * 45, 1.2, GOLD, alpha * Math.sin(t * Math.PI) * 0.3);
        }
        segment(g, casterX - 12, casterY - 12, casterX - 7, casterY - 17,
                LIGHT_GOLD, FINE, alpha * 0.24);
        segment(g, casterX + 7, casterY - 17, casterX + 12, casterY - 12,
                LIGHT_GOLD, FINE, alpha * 0.24);
    }

    private void directionalWave(Graphics2D g, double x, double y, double aim,
            double reach, double alpha, double time) {
        path.reset(); secondaryPath.reset();
        for (int step = 0; step <= 24; step++) {
            double angle = aim - 1.18 + step * 2.36 / 24;
            double uneven = 1 + Math.sin(step * 2.8) * 0.044;
            double px = x + Math.cos(angle) * reach * uneven;
            double py = y + Math.sin(angle) * reach * uneven * 0.67;
            if (step == 0) { path.moveTo(px, py); secondaryPath.moveTo(px, py + 3); }
            else { path.lineTo(px, py); secondaryPath.lineTo(px, py + 3); }
            if (step % 4 == 0) dust(g, px, py + 4, 12 + time * 18, alpha * 0.44);
        }
        outline(g, secondaryPath, DUST, WIDE, alpha * 0.18);
        outline(g, path, GOLD, EDGE, alpha * 0.7);
        outline(g, path, LIGHT_GOLD, FINE, alpha * 0.72);
    }

    private void pressureRibbon(Graphics2D g, double x, double y, double reach,
            double from, double to, double width, double alpha) {
        path.reset();
        for (int step = 0; step <= 25; step++) {
            double angle = from + (to - from) * step / 25;
            double r = reach * (1 + Math.sin(angle * 11) * 0.022 + Math.sin(angle * 23) * 0.012);
            double px = x + Math.cos(angle) * r;
            double py = y + Math.sin(angle) * r;
            if (step == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        outline(g, path, GOLD, WIDE, alpha * 0.12);
        outline(g, path, LIGHT_GOLD, width > 4 ? HEAVY : SMALL, alpha * 0.6);
        outline(g, path, STEEL, FINE, alpha * 0.36);
    }

    private void rock(Graphics2D g, double x, double y, double size, double angle, double alpha) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        path.reset();
        for (int i = 0; i < ROCK_X.length; i++) {
            double px = x + (ROCK_X[i] * cos - ROCK_Y[i] * sin) * size;
            double py = y + (ROCK_X[i] * sin + ROCK_Y[i] * cos) * size;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.closePath();
        fill(g, path, ROCK, alpha * 0.92);
        outline(g, path, CRACK, FINE, alpha * 0.48);
        segment(g, x - size * 0.36, y - size * 0.3, x + size * 0.15,
                y - size * 0.52, ROCK_LIGHT, SMALL, alpha * 0.7);
    }

    private void contactSparks(Graphics2D g, double x, double y, double t, double alpha, double spread) {
        for (int i = 0; i < 10; i++) {
            double angle = seeds[i] * Math.PI * 2;
            double travel = easeOut(t) * spread * (0.35 + seeds[i + 10] * 0.65);
            double px = x + Math.cos(angle) * travel;
            double py = y + Math.sin(angle) * travel * 0.65 + t * t * 6;
            segment(g, px, py, px - Math.cos(angle) * (2 + (1 - t) * 6),
                    py - Math.sin(angle) * (2 + (1 - t) * 6),
                    i % 3 == 0 ? STEEL : LIGHT_GOLD, FINE, alpha * 0.78);
        }
    }

    private void dust(Graphics2D g, double x, double y, double size, double alpha) {
        if (alpha < 0.008) return;
        setAlpha(g, alpha);
        g.drawImage(DUST_SPRITE, (int) Math.round(x - size), (int) Math.round(y - size * 0.48),
                (int) Math.ceil(size * 2), (int) Math.ceil(size * 0.96), null);
    }

    private void spark(Graphics2D g, double x, double y, double size, Color color, double alpha) {
        segment(g, x - size, y, x + size, y, color, FINE, alpha);
        segment(g, x, y - size, x, y + size, LIGHT_GOLD, FINE, alpha * 0.7);
    }

    private void segment(Graphics2D g, double x1, double y1, double x2, double y2,
            Color color, BasicStroke stroke, double alpha) {
        line.setLine(x1, y1, x2, y2);
        outline(g, line, color, stroke, alpha);
    }

    private static void fill(Graphics2D g, Shape shape, Color color, double alpha) {
        if (alpha < 0.003) return;
        setAlpha(g, alpha); g.setColor(color); g.fill(shape);
    }

    private static void outline(Graphics2D g, Shape shape, Color color,
            BasicStroke stroke, double alpha) {
        if (alpha < 0.003) return;
        setAlpha(g, alpha); g.setColor(color); g.setStroke(stroke); g.draw(shape);
    }

    private static void setAlpha(Graphics2D g, double alpha) {
        int index = (int) Math.round(clamp(alpha) * (OPACITY.length - 1));
        g.setComposite(OPACITY[index]);
    }

    private static Path2D shieldShape(double scale) {
        Path2D result = new Path2D.Double();
        result.moveTo(0, -0.57 * scale);
        result.lineTo(0.48 * scale, -0.36 * scale);
        result.lineTo(0.43 * scale, 0.18 * scale);
        result.lineTo(0.28 * scale, 0.42 * scale);
        result.lineTo(0, 0.61 * scale);
        result.lineTo(-0.28 * scale, 0.42 * scale);
        result.lineTo(-0.43 * scale, 0.18 * scale);
        result.lineTo(-0.48 * scale, -0.36 * scale);
        result.closePath();
        return result;
    }

    private static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static AlphaComposite[] opacityCache() {
        AlphaComposite[] result = new AlphaComposite[65];
        for (int i = 0; i < result.length; i++) {
            result[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 64f);
        }
        return result;
    }

    private static BufferedImage dustSprite() {
        BufferedImage result = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                double dx = (x - 31.5) / 31.5, dy = (y - 31.5) / 31.5;
                double edge = Math.max(0, 1 - Math.sqrt(dx * dx + dy * dy));
                double shape = 0.76 + Math.sin(x * 0.22 + y * 0.13) * 0.12;
                int alpha = (int) Math.round(edge * edge * shape * 162);
                result.setRGB(x, y, (alpha << 24) | (DUST.getRGB() & 0xffffff));
            }
        }
        return result;
    }

    private static double clamp(double t) { return Math.max(0, Math.min(1, t)); }
    private static double smooth(double t) { t = clamp(t); return t * t * (3 - 2 * t); }
    private static double easeOut(double t) { t = clamp(t); return 1 - Math.pow(1 - t, 3); }
}
