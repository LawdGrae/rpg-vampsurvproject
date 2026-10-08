import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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
    private static final BasicStroke SOFT = stroke(12f);
    private static final AlphaComposite[] OPACITY = opacityCache();
    private static final BufferedImage DUST_SPRITE = dustSprite();
    private static final Path2D SHIELD = shieldShape(1.0);
    private static final Path2D SHIELD_INSET = shieldShape(0.83);
    private static final double[] ROCK_X = {-0.62, -0.78, -0.22, 0.62, 0.76, 0.21};
    private static final double[] ROCK_Y = {0.53, -0.23, -0.81, -0.49, 0.24, 0.72};

    private final String id;
    private final double radius, maxLife, hitAge;
    private final BasicStroke shieldFine, shieldSmall, shieldGlow;
    private final double directionX, directionY;
    private final double[] seeds = new double[96];
    private final double[] trailX = new double[24];
    private final double[] trailY = new double[24];
    private final double[] trailBirth = new double[24];
    private final double[] shieldTrailX = new double[24];
    private final double[] shieldTrailY = new double[24];
    private final Path2D path = new Path2D.Double();
    private final Path2D secondaryPath = new Path2D.Double();
    private final Line2D line = new Line2D.Double();
    private final Ellipse2D ellipse = new Ellipse2D.Double();
    private final AffineTransform spriteTransform = new AffineTransform();
    private double casterX, casterY, shieldX, shieldY, impactX, impactY;
    private double previousCasterX, previousCasterY, previousShieldX, previousShieldY;
    private double age, nextTrailSample, blockAge = 1, blockX, blockY;
    private double waveRadius = -1;
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
        this.radius = Double.isFinite(radius) ? Math.max(0, radius) : 100;
        this.maxLife = Double.isFinite(maxLife) ? Math.max(0.05, maxLife) : 1;
        double barrierHeight = Math.max(61, Math.min(94, this.radius * 0.62));
        shieldFine = stroke((float) (FINE.getLineWidth() / barrierHeight));
        shieldSmall = stroke((float) (SMALL.getLineWidth() / barrierHeight));
        shieldGlow = stroke((float) (WIDE.getLineWidth() / barrierHeight));
        hitAge = AbilityAnimationTiming.duration(definition)
                * AbilityAnimationTiming.hitProgress(definition)[0];
        casterX = shieldX = impactX = Double.isFinite(startX) ? startX : 0;
        casterY = shieldY = impactY = Double.isFinite(startY) ? startY : 0;
        previousCasterX = previousShieldX = casterX;
        previousCasterY = previousShieldY = casterY;
        double length = Math.hypot(targetX - startX, targetY - startY);
        directionX = Double.isFinite(length) && length > 0.001 ? (targetX - startX) / length : 1;
        directionY = Double.isFinite(length) && length > 0.001 ? (targetY - startY) / length : 0;
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
        if (Double.isFinite(x) && Double.isFinite(y)) {
            shieldX = x; shieldY = y;
            if (age == 0) { previousShieldX = x; previousShieldY = y; }
        }
    }

    void setWaveRadius(double value) {
        if (Double.isFinite(value)) waveRadius = Math.max(0, value);
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
        double elapsed = age - previousAge;
        if (elapsed <= 0) return;
        blockAge = Math.min(1, blockAge + elapsed);
        if (!impactFrozen && age >= hitAge) {
            // Combat normally supplies the exact pose at the event. This also makes
            // previewing a stand-alone effect deterministic.
            if (id.equals("earthbreaker")) freezeImpactOrigin(shieldX, shieldY);
            else if (id.equals("guardians_roar")) freezeImpactOrigin(casterX, casterY);
        }
        if (id.equals("iron_charge")) {
            while (nextTrailSample <= Math.min(age, hitAge + 0.5) + 1e-10) {
                double fraction = clamp((nextTrailSample - previousAge) / elapsed);
                trailX[trailIndex] = previousCasterX + (casterX - previousCasterX) * fraction
                        - directionX * 14;
                trailY[trailIndex] = previousCasterY + (casterY - previousCasterY) * fraction
                        + 22 - directionY * 14;
                trailBirth[trailIndex] = nextTrailSample;
                shieldTrailX[trailIndex] = previousShieldX + (shieldX - previousShieldX) * fraction;
                shieldTrailY[trailIndex] = previousShieldY + (shieldY - previousShieldY) * fraction;
                trailIndex = (trailIndex + 1) % trailX.length;
                nextTrailSample += 0.028;
            }
        }
        previousCasterX = casterX;
        previousCasterY = casterY;
        previousShieldX = shieldX;
        previousShieldY = shieldY;
    }

    void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (age >= maxLife || !Double.isFinite(cameraX) || !Double.isFinite(cameraY)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(centerX + cameraX, centerY + cameraY);
            switch (id) {
                case "shield_fortress" -> fortress(g);
                case "iron_charge" -> { windup(g); charge(g); }
                case "earthbreaker" -> { windup(g); earthbreaker(g); }
                case "guardians_roar" -> { windup(g); roar(g); }
                case "unbreakable" -> passive(g);
                default -> { }
            }
        } finally { g.dispose(); }
    }

    private void windup(Graphics2D g) {
        double alpha = smooth(age / 0.13)
                * (1 - smooth((age - hitAge + 0.055) / 0.09));
        if (alpha < 0.003) return;
        // A small metallic rim accent follows the preparing weapon. Ground impact
        // and travelling pressure effects remain absent until the shared hit event.
        glow(g, shieldX, shieldY - 4, 21, GOLD, alpha * 0.32);
        double chargeAngle = Math.atan2(directionY, directionX);
        double chargeRadius = 13 + smooth(age / Math.max(0.08, hitAge)) * 7;
        SkillVfxShapes.castSigil(g, shieldX, shieldY - 4, chargeRadius,
                GOLD, chargeAngle + age * 1.7, alpha * 0.48);
        SkillVfxShapes.crescent(g, shieldX, shieldY - 4, chargeAngle,
                chargeRadius + 3, 3.5, LIGHT_GOLD, alpha * 0.42);
        // Highlights stay close to the shield while it gathers pressure.
        for (int rune = 0; rune < 3; rune++) {
            double runeAngle = chargeAngle + age * 2.2 + rune * Math.PI * 2 / 3;
            SkillVfxShapes.sparkle(g,
                    shieldX + Math.cos(runeAngle) * chargeRadius,
                    shieldY - 4 + Math.sin(runeAngle) * chargeRadius,
                    2.3, LIGHT_GOLD, runeAngle, alpha * 0.56);
        }
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
            glow(g, x, y, height * 0.56, GOLD, alpha * 0.22);
            // The assembled barrier has a grounded footprint and a lit rune face.
            double footprintRadius = 29 + reveal * 13;
            SkillVfxShapes.shockwave(g, casterX, casterY + 18,
                    footprintRadius, GOLD, age * 0.17, alpha * 0.32, 0.30);
            SkillVfxShapes.shockwave(g, casterX, casterY + 18,
                    footprintRadius * 0.76, LIGHT_GOLD, -age * 0.13,
                    alpha * 0.17, 0.30);
            Graphics2D barrier = (Graphics2D) g.create();
            try {
                barrier.translate(x, y);
                barrier.scale(width * assembly, height * assembly);
                outline(barrier, SHIELD, GOLD, shieldGlow, alpha * 0.16);
                fill(barrier, SHIELD, DARK_STEEL, alpha * 0.34);
                // Individual plates slide into the same connected shield silhouette.
                for (int side = -1; side <= 1; side += 2) {
                    double offset = (1 - reveal) * 0.18 * side;
                    path.reset();
                    path.moveTo(offset, -0.51);
                    path.lineTo(side * 0.42 + offset, -0.32);
                    path.lineTo(side * 0.35 + offset, 0.15);
                    path.lineTo(offset, 0.45);
                    path.closePath();
                    fill(barrier, path, side < 0 ? STEEL : GOLD, alpha * 0.12);
                    outline(barrier, path, STEEL, shieldFine, alpha * 0.19);
                }
                outline(barrier, SHIELD, GOLD, shieldSmall, alpha * 0.93);
                outline(barrier, SHIELD_INSET, STEEL, shieldFine, alpha * 0.68);
                path.reset();
                path.moveTo(0, -0.48); path.lineTo(0, 0.56);
                path.moveTo(-0.45, -0.31); path.lineTo(0, -0.1); path.lineTo(0.45, -0.31);
                path.moveTo(-0.42, 0.1); path.lineTo(0, 0.31); path.lineTo(0.42, 0.1);
                outline(barrier, path, GOLD, shieldFine, alpha * 0.6);
                // A narrow sheen crosses the facets without hiding the character.
                double sheen = ((age - hitAge) / 1.65) % 1;
                Graphics2D highlight = (Graphics2D) barrier.create();
                try {
                    highlight.clip(SHIELD_INSET);
                    double band = -0.85 + sheen * 1.9;
                    path.reset();
                    path.moveTo(-0.6, band); path.lineTo(0.6, band - 0.36);
                    path.lineTo(0.6, band - 0.24); path.lineTo(-0.6, band + 0.12);
                    path.closePath();
                    fill(highlight, path, LIGHT_GOLD, alpha * 0.19 * Math.sin(sheen * Math.PI));
                } finally { highlight.dispose(); }
                if (blockAge < 0.18) {
                    double flash = 1 - smooth(blockAge / 0.18);
                    fill(barrier, SHIELD, LIGHT_GOLD, flash * 0.27);
                    outline(barrier, SHIELD, LIGHT_GOLD, shieldSmall, flash);
                }
            } finally {
                barrier.dispose();
            }
            SkillVfxShapes.castSigil(g, x, y - height * 0.13,
                    height * 0.23, LIGHT_GOLD, age * 0.12, alpha * 0.28);
            double sheenPulse = 0.55 + 0.45 * Math.sin(age * 5.5);
            SkillVfxShapes.sparkle(g, x - width * 0.28, y - height * 0.26,
                    4.5, LIGHT_GOLD, -0.18, alpha * sheenPulse * 0.67);
            SkillVfxShapes.sparkle(g, x + width * 0.25, y + height * 0.06,
                    3, GOLD, 0.20, alpha * (1 - sheenPulse * 0.45) * 0.57);
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
        if (blockAge < 0.18) {
            double flash = 1 - smooth(blockAge / 0.18);
            glow(g, blockX, blockY, 32, LIGHT_GOLD, flash * 0.7);
            contactSparks(g, blockX, blockY, blockAge / 0.18, flash, 30);
        }
    }

    private void charge(Graphics2D g) {
        // Fixed-time samples produce the same continuous swept rim at any frame rate.
        for (int sample = 1; sample < trailX.length; sample++) {
            int from = (trailIndex + sample - 1) % trailX.length;
            int to = (trailIndex + sample) % trailX.length;
            double t = (age - trailBirth[to]) / 0.29;
            if (t < 0 || t >= 1 || trailBirth[from] < hitAge
                    || trailBirth[to] <= trailBirth[from]) continue;
            double alpha = (1 - smooth(t)) * 0.64;
            for (int ribbon = -1; ribbon <= 1; ribbon += 2) {
                double spread = ribbon * (10 + t * 11);
                double x1 = shieldTrailX[from] - directionY * spread;
                double y1 = shieldTrailY[from] + directionX * spread;
                double x2 = shieldTrailX[to] - directionY * spread;
                double y2 = shieldTrailY[to] + directionX * spread;
                path.reset(); path.moveTo(x1, y1);
                path.quadTo((x1 + x2) * 0.5 - directionY * ribbon * 2,
                        (y1 + y2) * 0.5 + directionX * ribbon * 2, x2, y2);
                outline(g, path, GOLD, WIDE, alpha * 0.1);
                outline(g, path, ribbon < 0 ? STEEL : GOLD, SMALL, alpha * 0.62);
                outline(g, path, LIGHT_GOLD, FINE, alpha * 0.32);
            }
        }
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
        glow(g, shieldX + directionX * 3, shieldY + directionY * 3, 24,
                LIGHT_GOLD, active * 0.36);
        double pressureAngle = Math.atan2(directionY, directionX);
        double pressureX = shieldX + directionX * 8;
        double pressureY = shieldY + directionY * 8;
        SkillVfxShapes.crescent(g, pressureX, pressureY, pressureAngle,
                29, 7, GOLD, active * 0.46);
        SkillVfxShapes.crescent(g, pressureX, pressureY, pressureAngle,
                25, 2.2, LIGHT_GOLD, active * 0.75);
        SkillVfxShapes.sparkle(g, shieldX + directionX * 31,
                shieldY + directionY * 31, 4.2, LIGHT_GOLD,
                pressureAngle + Math.PI / 4, active * 0.60);
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
        double reach = waveRadius >= 0 ? clamp(waveRadius / Math.max(1, radius)) : smooth(time / 0.60);
        double fade = 1 - smooth((age - maxLife + 0.42) / 0.42);
        double aim = Math.atan2(directionY, directionX);
        double shockRadius = reach * radius;
        if (shockRadius > 3) {
            // The bright edge follows the same radius as the damaging ground front.
            SkillVfxShapes.crescent(g, impactX, impactY + 6, aim,
                    shockRadius, 6 + 3 * (1 - reach), GOLD, fade * 0.30);
            SkillVfxShapes.crescent(g, impactX, impactY + 6, aim,
                    Math.max(1, shockRadius - 3), 2.0, LIGHT_GOLD, fade * 0.53);
            for (int chip = 0; chip < 7; chip++) {
                double chipAngle = aim + (chip - 3) * 0.20;
                double chipDistance = Math.max(0, shockRadius - 4 - (chip % 3) * 3);
                double chipX = impactX + Math.cos(chipAngle) * chipDistance;
                double chipY = impactY + 6 + Math.sin(chipAngle) * chipDistance;
                SkillVfxShapes.sparkle(g, chipX, chipY,
                        1.8 + (chip % 2), ROCK_LIGHT, chipAngle,
                        fade * (0.24 + (chip % 3) * 0.07));
            }
        }
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
            double heat = fade * (1 - smooth(time / 0.55));
            outline(g, path, GOLD, WIDE, heat * 0.11);
            outline(g, path, GOLD, FINE, heat * 0.85);
            if (time < 0.38) dust(g, lastX, lastY, 11 + time * 25, fade * 0.19);
        }
        double waveTime = clamp(time / 0.60);
        double waveReach = waveRadius >= 0 ? waveRadius : radius * smooth(waveTime);
        directionalWave(g, impactX, impactY, aim, waveReach,
                smooth(time / 0.035) * (1 - smooth((time - 0.35) / 0.5)) * fade * 0.82, time);
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
        if (time < 0.32) {
            double flash = 1 - smooth(time / 0.32);
            glow(g, impactX, impactY, 38 + time * 35, GOLD, flash * 0.7);
            contactSparks(g, impactX, impactY, time / 0.32, flash, 37);
        }
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
        if (reach > 3) {
            SkillVfxShapes.shockwave(g, impactX, impactY + 6, reach,
                    LIGHT_GOLD, -time * 0.19, alpha * 0.30, 0.72);
            SkillVfxShapes.shockwave(g, impactX, impactY + 6, reach * 0.81,
                    GOLD, time * 0.28, alpha * 0.20, 0.72);
        }
        if (time < 0.26) {
            SkillVfxShapes.castSigil(g, impactX, impactY + 6,
                    22 + smooth(time / 0.26) * 12, LIGHT_GOLD,
                    -time * 0.65, (1 - smooth(time / 0.26)) * 0.30);
        }
        if (time < 0.24) glow(g, impactX, impactY + 6, 38, LIGHT_GOLD,
                (1 - smooth(time / 0.24)) * 0.48);
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
            double y = impactY + Math.sin(angle) * reach;
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
        if (reach < 0.4 || alpha < 0.003) return;
        path.reset(); secondaryPath.reset();
        for (int step = 0; step <= 38; step++) {
            double angle = aim - 1.77 + step * 3.54 / 38;
            double uneven = 1 + Math.sin(step * 1.15) * 0.009;
            double px = x + Math.cos(angle) * reach * uneven;
            double py = y + Math.sin(angle) * reach * uneven;
            double insideX = x + Math.cos(angle) * Math.max(0, reach - 7);
            double insideY = y + Math.sin(angle) * Math.max(0, reach - 7);
            if (step == 0) { path.moveTo(px, py); secondaryPath.moveTo(insideX, insideY); }
            else { path.lineTo(px, py); secondaryPath.lineTo(insideX, insideY); }
            if (step % 4 == 0) {
                dust(g, px, py + 4, 12 + time * 18, alpha * 0.45);
                glow(g, px, py, 15, GOLD, alpha * 0.23);
            }
        }
        outline(g, secondaryPath, DUST, SOFT, alpha * 0.23);
        outline(g, path, GOLD, SOFT, alpha * 0.15);
        outline(g, path, GOLD, EDGE, alpha * 0.7);
        outline(g, path, LIGHT_GOLD, FINE, alpha * 0.87);
        outline(g, secondaryPath, ROCK_LIGHT, SMALL, alpha * 0.32);
    }

    private void pressureRibbon(Graphics2D g, double x, double y, double reach,
            double from, double to, double width, double alpha) {
        if (reach < 0.4 || alpha < 0.003) return;
        path.reset(); secondaryPath.reset();
        // Dense curves and tapered ends let each pressure crest flow through the air.
        for (int step = 0; step <= 48; step++) {
            double u = step / 48.0;
            double angle = from + (to - from) * u;
            double center = pressureRadius(reach, angle);
            double taper = Math.pow(Math.sin(u * Math.PI), 0.7);
            double r = center + width * 0.5 * taper;
            double px = x + Math.cos(angle) * r;
            double py = y + Math.sin(angle) * r;
            double cx = x + Math.cos(angle) * center;
            double cy = y + Math.sin(angle) * center;
            if (step == 0) {
                path.moveTo(px, py); secondaryPath.moveTo(cx, cy);
            } else {
                path.lineTo(px, py); secondaryPath.lineTo(cx, cy);
            }
        }
        for (int step = 48; step >= 0; step--) {
            double u = step / 48.0;
            double angle = from + (to - from) * u;
            double r = pressureRadius(reach, angle) - width * 0.5 * Math.pow(Math.sin(u * Math.PI), 0.7);
            path.lineTo(x + Math.cos(angle) * r, y + Math.sin(angle) * r);
        }
        path.closePath();
        outline(g, secondaryPath, GOLD, SOFT, alpha * 0.12);
        fill(g, path, LIGHT_GOLD, alpha * 0.48);
        outline(g, path, LIGHT_GOLD, FINE, alpha * 0.32);
        outline(g, secondaryPath, STEEL, FINE, alpha * 0.5);
    }

    private double pressureRadius(double reach, double angle) {
        return reach * (1 + Math.sin(angle * 11 - age * 2) * 0.012
                + Math.sin(angle * 23 - age * 3) * 0.006);
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
        spriteTransform.setToTranslation(x - size, y - size * 0.48);
        spriteTransform.scale(size * 2 / DUST_SPRITE.getWidth(), size * 0.96 / DUST_SPRITE.getHeight());
        g.drawImage(DUST_SPRITE, spriteTransform, null);
    }

    private static void glow(Graphics2D g, double x, double y, double size, Color color, double alpha) {
        setAlpha(g, 1);
        SkillVfxGlow.draw(g, x, y, size, color, alpha);
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
        AlphaComposite[] result = new AlphaComposite[257];
        for (int i = 0; i < result.length; i++) {
            result[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 256f);
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
