import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Analytic, screen-space effects for the player's drop into a new run. */
public final class RunEntranceAnimation {
    public static final double FALL_DURATION = 0.72;
    public static final double DURATION = 1.04;
    public static final double EFFECT_DURATION = 1.72;

    private static final Color DEFAULT_TINT = new Color(126, 191, 238);
    private static final double SQUASH_ATTACK = 0.055;

    private boolean started;
    private double age;
    private double dropHeight;
    private Color tint = DEFAULT_TINT;

    public void start(double dropHeight, Color tint) {
        this.dropHeight = Double.isFinite(dropHeight)
                ? Math.max(64.0, Math.min(1600.0, dropHeight)) : 180.0;
        this.tint = tint == null ? DEFAULT_TINT : tint;
        age = 0.0;
        started = true;
    }

    public void reset() {
        started = false;
        age = 0.0;
        dropHeight = 0.0;
        tint = DEFAULT_TINT;
    }

    /** Returns true only on the update that crosses the landing time. */
    public boolean update(double dt) {
        if (!started || age >= EFFECT_DURATION || !Double.isFinite(dt) || dt <= 0.0) {
            return false;
        }
        double previousAge = age;
        age = Math.min(EFFECT_DURATION, age + dt);
        return previousAge < FALL_DURATION && age >= FALL_DURATION;
    }

    public boolean isPlaying() {
        return started && age < DURATION;
    }

    public boolean isVisible() {
        return started && age < EFFECT_DURATION;
    }

    public double getRemainingDuration() {
        return started ? Math.max(0.0, DURATION - age) : 0.0;
    }

    public double getAge() {
        return age;
    }

    /** Positive upward displacement from the standing position. */
    public double getHeight() {
        if (!started || age >= FALL_DURATION) {
            return 0.0;
        }
        double progress = age / FALL_DURATION;
        return dropHeight * (1.0 - progress * progress);
    }

    /** Squash amount from zero to one; scale around the planted feet. */
    public double getCompression() {
        if (!started || age < FALL_DURATION || age >= DURATION) {
            return 0.0;
        }
        double landedAge = age - FALL_DURATION;
        if (landedAge < SQUASH_ATTACK) {
            return smoothStep(landedAge / SQUASH_ATTACK);
        }
        return 1.0 - smoothStep((landedAge - SQUASH_ATTACK)
                / (DURATION - FALL_DURATION - SQUASH_ATTACK));
    }

    /** Draw before the player; groundY is the original landing foot position. */
    public void drawGround(Graphics2D graphics, int centerX, double groundY) {
        if (!isVisible() || !Double.isFinite(groundY)) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color lightTint = blend(tint, Color.WHITE, 0.56);
            if (isPlaying()) {
                double approach = Math.min(1.0, age / FALL_DURATION);
                double weight = approach * approach;
                softEllipse(g, centerX, groundY, 33.0 - 12.0 * weight,
                        8.5 - 3.0 * weight, new Color(7, 10, 15), 53.0 + 52.0 * weight);
            }

            if (age < FALL_DURATION) {
                double progress = age / FALL_DURATION;
                double proximity = Math.pow(progress, 5.0);
                double radius = 31.0 - 8.0 * smoothStep(progress);
                softEllipse(g, centerX, groundY - 1.0, 35.0, 9.0,
                        tint, 12.0 + 27.0 * proximity);
                ring(g, centerX, groundY, radius, 0.27, lightTint,
                        20.0 + 36.0 * proximity, 0.8f);
                // Sparse dashes converge on the landing point as the hero accelerates.
                g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3.0 + 0.22;
                    double outer = radius + 5.0 + 6.0 * (1.0 - progress);
                    g.setColor(withAlpha(tint, 20.0 + 34.0 * proximity));
                    g.draw(new java.awt.geom.Line2D.Double(
                            centerX + Math.cos(angle) * (outer - 3.0),
                            groundY + Math.sin(angle) * (outer - 3.0) * 0.27,
                            centerX + Math.cos(angle) * outer,
                            groundY + Math.sin(angle) * outer * 0.27));
                }
                return;
            }

            double landedAge = age - FALL_DURATION;
            double progress = landedAge / (EFFECT_DURATION - FALL_DURATION);
            drawSurface(g, centerX, groundY, landedAge, progress);

            // A wide, low bloom grounds the impact without washing out the sprite.
            double bloom = Math.exp(-landedAge * 9.0);
            softEllipse(g, centerX, groundY - 1.0, 48.0 + 38.0 * (1.0 - bloom),
                    13.0 + 5.0 * (1.0 - bloom), tint, 145.0 * bloom);
            softEllipse(g, centerX, groundY - 1.0, 29.0, 7.5,
                    lightTint, 185.0 * Math.exp(-landedAge * 25.0));

            shockRing(g, centerX, groundY, landedAge, 0.43, 124.0, lightTint, 185.0);
            shockRing(g, centerX, groundY, landedAge - 0.065, 0.49, 97.0, tint, 118.0);
            shockRing(g, centerX, groundY, landedAge - 0.17, 0.47, 72.0, lightTint, 65.0);

            Color dustTint = blend(tint, new Color(194, 180, 155), 0.86);
            for (int i = 0; i < 10; i++) {
                double particleAge = landedAge - (i % 3) * 0.014;
                double life = 0.72 + (i % 4) * 0.07;
                if (particleAge < 0.0 || particleAge >= life) {
                    continue;
                }
                double p = particleAge / life;
                double angle = i * Math.PI * 2.0 / 10.0 + 0.17;
                double distance = 13.0 + (61.0 + (i % 3) * 17.0) * particleAge;
                double x = centerX + Math.cos(angle) * distance;
                double y = groundY + Math.sin(angle) * distance * 0.24 - 3.0 * p;
                double radius = 6.0 + 18.0 * smoothStep(p);
                double fade = smoothStep(Math.min(1.0, particleAge / 0.05))
                        * Math.pow(1.0 - p, 1.65);
                softEllipse(g, x, y, radius, radius * 0.37, dustTint, 125.0 * fade);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Draw after the player. centerY is the standing center, before height is
     * subtracted; footOffset locates the original landing feet below it.
     */
    public void drawFront(Graphics2D graphics, int centerX, int centerY, double footOffset) {
        if (!isVisible() || !Double.isFinite(footOffset)) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color lightTint = blend(tint, Color.WHITE, 0.64);
            if (age < FALL_DURATION) {
                drawFall(g, centerX, centerY, lightTint);
                return;
            }

            double landedAge = age - FALL_DURATION;
            double groundY = centerY + footOffset;
            if (landedAge < 0.14) {
                SkillVfxShapes.sparkle(g, centerX, (int) Math.round(groundY - 2.0),
                        7.0 + 5.0 * landedAge / 0.14, lightTint, Math.PI / 4.0,
                        0.64 * (1.0 - landedAge / 0.14));
            }
            drawPlumes(g, centerX, groundY, landedAge);
            drawDebris(g, centerX, groundY, landedAge);
            drawSparks(g, centerX, groundY, landedAge, lightTint);
        } finally {
            g.dispose();
        }
    }

    private void drawFall(Graphics2D g, double centerX, double centerY, Color lightTint) {
        double progress = age / FALL_DURATION;
        double strength = smoothStep(progress / 0.25);
        double playerY = centerY - getHeight();
        for (int i = 0; i < 6; i++) {
            double side = (i & 1) == 0 ? -1.0 : 1.0;
            int band = i / 2;
            double x = centerX + side * (21.0 + band * 10.0);
            double bottom = playerY + 5.0 - band * 11.0;
            double length = (17.0 + 54.0 * progress * progress) * (1.0 - band * 0.19);
            double sway = side * (3.5 + band * 1.5);
            Path2D.Double ribbon = new Path2D.Double();
            ribbon.moveTo(x + sway, bottom - length);
            ribbon.curveTo(x + sway * 0.65, bottom - length * 0.64,
                    x - side * 1.6, bottom - length * 0.20, x, bottom);
            ribbon.curveTo(x + side * 3.0, bottom - length * 0.14,
                    x + sway * 0.85 + side * 1.3, bottom - length * 0.55,
                    x + sway, bottom - length);
            ribbon.closePath();
            g.setColor(withAlpha(tint, (72.0 - band * 12.0) * strength));
            g.fill(ribbon);

            Path2D.Double core = new Path2D.Double();
            core.moveTo(x + sway * 0.6, bottom - length * 0.73);
            core.quadTo(x + side * 0.2, bottom - length * 0.31, x, bottom - 2.0);
            g.setStroke(new BasicStroke(band == 0 ? 1.5f : 0.9f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(lightTint, (137.0 - band * 27.0) * strength));
            g.draw(core);
        }
        // Restrict the aura to the outline so the hero remains crisp.
        double aura = (0.3 + 0.7 * progress) * strength;
        softEllipse(g, centerX - 21.0, playerY - 6.0, 8.0, 24.0, tint, 36.0 * aura);
        softEllipse(g, centerX + 21.0, playerY - 6.0, 8.0, 24.0, tint, 36.0 * aura);
        for (int i = 0; i < 8; i++) {
            double side = (i & 1) == 0 ? -1.0 : 1.0;
            double cycle = (progress * (1.5 + (i % 3) * 0.23) + i * 0.137) % 1.0;
            double x = centerX + side * (27.0 + (i % 3) * 8.0 + 3.0 * cycle);
            double y = playerY - 8.0 - cycle * 64.0;
            double fade = Math.sin(cycle * Math.PI) * strength;
            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(lightTint, 64.0 * fade));
            g.draw(new java.awt.geom.Line2D.Double(x, y, x + side * 0.8, y - 3.0 - 7.0 * progress));
        }
    }

    private void drawSurface(Graphics2D g, double centerX, double groundY,
            double landedAge, double progress) {
        double reveal = smoothStep(landedAge / 0.055);
        double fade = Math.pow(1.0 - progress, 1.35);
        softEllipse(g, centerX, groundY + 1.0, 35.0, 10.0,
                new Color(19, 17, 22), 75.0 * reveal * fade);
        for (int i = 0; i < 9; i++) {
            double angle = i * Math.PI * 2.0 / 9.0 + 0.12;
            double extent = 28.0 + (i % 4) * 5.0;
            double growth = smoothStep((landedAge - (i % 3) * 0.012) / 0.095);
            if (growth <= 0.0) {
                continue;
            }
            Path2D.Double crack = new Path2D.Double();
            double lastX = centerX;
            double lastY = groundY;
            for (int j = 0; j < 4; j++) {
                double radius = (5.0 + (extent - 5.0) * j / 3.0) * growth;
                double bend = angle + (j % 2 == 0 ? 0.09 : -0.085);
                double x = centerX + Math.cos(bend) * radius;
                double y = groundY + Math.sin(bend) * radius * 0.30;
                if (j == 0) {
                    crack.moveTo(x, y);
                } else {
                    crack.lineTo(x, y);
                }
                lastX = x;
                lastY = y;
            }
            g.setStroke(new BasicStroke(1.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(new Color(20, 18, 23), 91.0 * reveal * fade));
            g.draw(crack);
            g.setStroke(new BasicStroke(0.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(tint, 54.0 * Math.exp(-landedAge * 5.5) * growth));
            g.draw(crack);
            // Small detached cuts avoid a uniform starburst silhouette.
            if (i % 2 == 0) {
                g.setColor(withAlpha(new Color(26, 21, 24), 68.0 * fade * growth));
                g.draw(new java.awt.geom.Line2D.Double(lastX + Math.cos(angle) * 3.0,
                        lastY + Math.sin(angle) * 0.9,
                        lastX + Math.cos(angle + 0.18) * 7.0,
                        lastY + Math.sin(angle + 0.18) * 2.1));
            }
        }
    }

    private void drawPlumes(Graphics2D g, double centerX, double groundY, double landedAge) {
        Color dustTint = blend(tint, new Color(193, 182, 162), 0.88);
        Color dustShade = blend(tint, new Color(119, 114, 109), 0.91);
        for (int i = 0; i < 8; i++) {
            double particleAge = landedAge - (i / 2) * 0.022;
            double life = 0.68 + (i % 3) * 0.095;
            if (particleAge < 0.0 || particleAge >= life) {
                continue;
            }
            double p = particleAge / life;
            double side = (i & 1) == 0 ? -1.0 : 1.0;
            double x = centerX + side * (13.0 + (55.0 + (i / 2) * 17.0) * particleAge);
            double y = groundY - 2.0 - (51.0 + (i % 3) * 15.0) * particleAge
                    + 18.0 * particleAge * particleAge;
            double radius = 5.0 + 18.0 * smoothStep(p);
            double fade = smoothStep(particleAge / 0.048) * Math.pow(1.0 - p, 1.5);
            softEllipse(g, x, y + radius * 0.12, radius * 1.08, radius * 0.76,
                    dustShade, 99.0 * fade);
            softEllipse(g, x - side * radius * 0.22, y - radius * 0.22,
                    radius * 0.82, radius * 0.69, dustTint, 150.0 * fade);
        }
    }

    private void drawDebris(Graphics2D g, double centerX, double groundY, double landedAge) {
        Color rockTint = blend(tint, new Color(141, 132, 116), 0.91);
        Color rockHighlight = blend(rockTint, new Color(221, 207, 180), 0.46);
        for (int i = 0; i < 8; i++) {
            double t = landedAge - (i % 3) * 0.007;
            double life = 0.48 + (i % 4) * 0.038;
            if (t < 0.0 || t >= life) {
                continue;
            }
            double side = (i & 1) == 0 ? -1.0 : 1.0;
            double vx = 56.0 + (i / 2) * 16.0;
            double vy = 90.0 + (i % 4) * 15.0;
            double gravity = 475.0;
            double firstContact = 2.0 * vy / gravity;
            double bounceAge = Math.max(0.0, t - firstContact);
            double height = t <= firstContact ? vy * t - 0.5 * gravity * t * t
                    : Math.max(0.0, vy * 0.23 * bounceAge - 0.5 * gravity * bounceAge * bounceAge);
            double x = centerX + side * (9.0 + vx * t);
            double y = groundY - 1.0 - height;
            double fade = Math.pow(1.0 - t / life, 0.65);
            double radius = 1.55 + (i % 3) * 0.34;
            softEllipse(g, x, groundY, radius * 1.6, 0.8, new Color(17, 15, 17),
                    49.0 * fade * (1.0 - Math.min(1.0, height / 23.0)));
            double rotation = i * 0.73 + side * t * 7.0;
            Path2D.Double rock = new Path2D.Double();
            for (int vertex = 0; vertex < 4; vertex++) {
                double angle = rotation + vertex * Math.PI / 2.0;
                double xOffset = Math.cos(angle) * radius * (vertex % 2 == 0 ? 1.0 : 0.74);
                double yOffset = Math.sin(angle) * radius * 0.72;
                if (vertex == 0) {
                    rock.moveTo(x + xOffset, y + yOffset);
                } else {
                    rock.lineTo(x + xOffset, y + yOffset);
                }
            }
            rock.closePath();
            g.setColor(withAlpha(rockTint, 210.0 * fade));
            g.fill(rock);
            g.setStroke(new BasicStroke(0.65f));
            g.setColor(withAlpha(rockHighlight, 144.0 * fade));
            g.draw(new java.awt.geom.Line2D.Double(x - radius * 0.65, y - radius * 0.25,
                    x + radius * 0.15, y - radius * 0.52));
        }
    }

    private void drawSparks(Graphics2D g, double centerX, double groundY,
            double landedAge, Color lightTint) {
        Color emberTint = blend(tint, new Color(255, 205, 131), 0.42);
        for (int i = 0; i < 12; i++) {
            double t = landedAge - (i % 4) * 0.012;
            double life = 0.27 + (i % 5) * 0.038;
            if (t < 0.0 || t >= life) {
                continue;
            }
            double side = (i & 1) == 0 ? -1.0 : 1.0;
            double vx = 65.0 + (i % 6) * 19.0;
            double vy = 62.0 + (i % 4) * 24.0;
            double offset = 10.0 + (i / 4) * 4.0;
            double fade = Math.pow(1.0 - t / life, 0.8);
            Color color = i % 3 == 0 ? emberTint : lightTint;
            double x = centerX + side * (offset + vx * t);
            double y = groundY - 2.0 - vy * t + 120.0 * t * t;
            // Sample the same analytic trajectory for tapering, frame-rate independent trails.
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int segment = 3; segment >= 0; segment--) {
                double oldT = Math.max(0.0, t - (segment + 1) * 0.011);
                double nextT = Math.max(0.0, t - segment * 0.011);
                g.setColor(withAlpha(color, (148.0 - segment * 34.0) * fade));
                g.draw(new java.awt.geom.Line2D.Double(
                        centerX + side * (offset + vx * oldT),
                        groundY - 2.0 - vy * oldT + 120.0 * oldT * oldT,
                        centerX + side * (offset + vx * nextT),
                        groundY - 2.0 - vy * nextT + 120.0 * nextT * nextT));
            }
            softEllipse(g, x, y, 3.8, 3.8, color, 65.0 * fade);
            g.setColor(withAlpha(lightTint, 226.0 * fade));
            g.fill(new Ellipse2D.Double(x - 0.8, y - 0.8, 1.6, 1.6));
        }
    }

    private static void shockRing(Graphics2D g, double x, double y, double elapsed,
            double duration, double reach, Color color, double alpha) {
        if (elapsed < 0.0 || elapsed >= duration) {
            return;
        }
        double progress = elapsed / duration;
        double radius = 8.0 + (reach - 8.0) * (1.0 - Math.pow(1.0 - progress, 2.6));
        double fade = Math.pow(1.0 - progress, 1.7);
        ring(g, x, y, radius, 0.27, color, alpha * 0.16 * fade, 7.0f);
        ring(g, x, y, radius, 0.27, color, alpha * fade, 1.25f);
        g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(color, alpha * 0.37 * fade));
        for (int i = 0; i < 5; i++) {
            double outer = radius + 3.0 + 4.0 * progress;
            g.draw(new Arc2D.Double(x - outer, y - outer * 0.27,
                    outer * 2.0, outer * 0.54, 11.0 + i * 72.0, 20.0, Arc2D.OPEN));
        }
    }

    private static void ring(Graphics2D g, double x, double y, double radius,
            double aspect, Color color, double alpha, float thickness) {
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(color, alpha));
        g.draw(new Ellipse2D.Double(x - radius, y - radius * aspect,
                radius * 2.0, radius * aspect * 2.0));
    }

    /** Elliptical radial falloff supplies soft light and dust without opaque particle disks. */
    private static void softEllipse(Graphics2D graphics, double x, double y,
            double radiusX, double radiusY, Color color, double alpha) {
        if (alpha < 0.5 || radiusX <= 0.0 || radiusY <= 0.0) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(1.0, radiusY / radiusX);
            g.setPaint(new RadialGradientPaint(0.0f, 0.0f, (float) radiusX,
                    new float[] {0.0f, 0.35f, 0.72f, 1.0f},
                    new Color[] {withAlpha(color, alpha), withAlpha(color, alpha * 0.72),
                            withAlpha(color, alpha * 0.19), withAlpha(color, 0.0)}));
            g.fill(new Ellipse2D.Double(-radiusX, -radiusX, radiusX * 2.0, radiusX * 2.0));
        } finally {
            g.dispose();
        }
    }

    private static double smoothStep(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static Color blend(Color first, Color second, double amount) {
        return new Color((int) Math.round(first.getRed() * (1.0 - amount) + second.getRed() * amount),
                (int) Math.round(first.getGreen() * (1.0 - amount) + second.getGreen() * amount),
                (int) Math.round(first.getBlue() * (1.0 - amount) + second.getBlue() * amount));
    }

    private static Color withAlpha(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) Math.round(Math.max(0.0, Math.min(255.0, alpha))));
    }
}
