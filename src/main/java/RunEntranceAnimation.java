import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Analytic, screen-space effects for the player's drop into a new run. */
public final class RunEntranceAnimation {
    public static final double FALL_DURATION = 0.72;
    public static final double DURATION = 1.04;
    public static final double EFFECT_DURATION = 1.38;

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
            if (isPlaying()) {
                double approach = Math.min(1.0, age / FALL_DURATION);
                double width = 31.0 - 12.0 * approach * approach;
                double height = 8.0 - 3.5 * approach * approach;
                int strength = (int) Math.round(24.0 + 47.0 * approach * approach);
                for (int layer = 2; layer >= 0; layer--) {
                    double spread = layer * (2.4 - 1.6 * approach);
                    g.setColor(new Color(8, 12, 17, layer == 0 ? strength : strength / 5));
                    g.fill(new Ellipse2D.Double(centerX - width - spread,
                            groundY - height - spread * 0.28,
                            (width + spread) * 2.0, (height + spread * 0.28) * 2.0));
                }
            }

            if (age < FALL_DURATION) {
                double progress = age / FALL_DURATION;
                double fade = Math.min(1.0, (FALL_DURATION - age) / 0.09);
                double radius = 27.0 - 3.0 * progress;
                g.setStroke(new BasicStroke(1.0f));
                g.setColor(withAlpha(blend(tint, Color.WHITE, 0.35), 31.0 * fade));
                g.draw(new Ellipse2D.Double(centerX - radius, groundY - radius * 0.27,
                        radius * 2.0, radius * 0.54));
                return;
            }

            double landedAge = age - FALL_DURATION;
            double effectLength = EFFECT_DURATION - FALL_DURATION;
            double progress = landedAge / effectLength;
            Color lightTint = blend(tint, Color.WHITE, 0.5);
            double radius = 12.0 + 65.0 * (1.0 - Math.pow(1.0 - progress, 2.0));
            double alpha = 0.72 * Math.pow(1.0 - progress, 1.7);
            SkillVfxGlow.draw(g, centerX, groundY - 2.0, 18.0, lightTint,
                    0.34 * Math.exp(-landedAge * 24.0));
            SkillVfxShapes.shockwave(g, centerX, (int) Math.round(groundY),
                    radius, lightTint, 0.0, alpha, 0.28);
            if (landedAge >= 0.075) {
                double secondProgress = (landedAge - 0.075) / (effectLength - 0.075);
                SkillVfxShapes.shockwave(g, centerX, (int) Math.round(groundY),
                        9.0 + 48.0 * secondProgress, tint, 0.0,
                        0.38 * Math.pow(1.0 - secondProgress, 2.0), 0.28);
            }

            Color dustTint = blend(tint, new Color(207, 194, 168), 0.78);
            for (int i = 0; i < 4; i++) {
                double side = (i & 1) == 0 ? -1.0 : 1.0;
                double distance = 8.0 + (30.0 + i * 5.0) * landedAge;
                double dustX = centerX + side * distance;
                double dustY = groundY + (i < 2 ? 1.5 : -2.0);
                double size = 3.0 + 5.0 * progress;
                g.setColor(withAlpha(dustTint, 75.0 * Math.pow(1.0 - progress, 1.6)));
                g.fill(new Ellipse2D.Double(dustX - size, dustY - size * 0.32,
                        size * 2.0, size * 0.64));
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
                double progress = age / FALL_DURATION;
                double streakAlpha = smoothStep(Math.min(1.0, progress / 0.28));
                double playerY = centerY - getHeight();
                for (int i = 0; i < 4; i++) {
                    double side = (i & 1) == 0 ? -1.0 : 1.0;
                    double streakX = centerX + side * (23.0 + (i / 2) * 8.0);
                    double bottomY = playerY - 4.0 - (i / 2) * 10.0;
                    double length = (12.0 + 34.0 * progress) * (i < 2 ? 1.0 : 0.68);
                    Path2D.Double streak = new Path2D.Double();
                    streak.moveTo(streakX + side * 2.0, bottomY - length);
                    streak.quadTo(streakX + side, bottomY - length * 0.45,
                            streakX, bottomY);
                    g.setStroke(new BasicStroke(i < 2 ? 2.0f : 1.2f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(withAlpha(lightTint, (i < 2 ? 106.0 : 65.0) * streakAlpha));
                    g.draw(streak);
                }
                return;
            }

            double landedAge = age - FALL_DURATION;
            double groundY = centerY + footOffset;
            if (landedAge < 0.13) {
                SkillVfxShapes.sparkle(g, centerX, (int) Math.round(groundY - 2.0),
                        7.0 + 4.0 * landedAge / 0.13, lightTint, Math.PI / 4.0,
                        0.63 * (1.0 - landedAge / 0.13));
            }

            Color dustTint = blend(tint, new Color(207, 194, 168), 0.78);
            Color rockTint = blend(tint, new Color(145, 137, 122), 0.84);
            // Eight deterministic particles here and four on the ground.
            for (int i = 0; i < 8; i++) {
                boolean dust = i < 4;
                double life = dust ? 0.56 + i * 0.025 : 0.36 + (i - 4) * 0.032;
                if (landedAge >= life) {
                    continue;
                }
                double progress = landedAge / life;
                double side = (i & 1) == 0 ? -1.0 : 1.0;
                double velocityX = dust ? 31.0 + i * 7.0 : 39.0 + (i - 4) * 9.0;
                double velocityY = dust ? 37.0 + i * 5.0 : 85.0 + (i - 4) * 11.0;
                double gravity = dust ? 100.0 : 440.0;
                double x = centerX + side * (5.0 + velocityX * landedAge);
                double y = Math.min(groundY - 1.0,
                        groundY - 1.0 - velocityY * landedAge
                                + 0.5 * gravity * landedAge * landedAge);
                double fade = Math.pow(1.0 - progress, dust ? 1.2 : 0.7);
                if (dust) {
                    double radius = 2.6 + 3.0 * progress;
                    g.setColor(withAlpha(dustTint, 132.0 * fade));
                    g.fill(new Ellipse2D.Double(x - radius, y - radius * 0.75,
                            radius * 2.0, radius * 1.5));
                } else {
                    double radius = 2.0 + (i - 4) * 0.32;
                    double rotation = i * 0.73 + side * landedAge * 5.0;
                    Path2D.Double rock = new Path2D.Double();
                    for (int vertex = 0; vertex < 4; vertex++) {
                        double angle = rotation + vertex * Math.PI / 2.0;
                        double rockX = x + Math.cos(angle) * radius;
                        double rockY = y + Math.sin(angle) * radius * 0.7;
                        if (vertex == 0) {
                            rock.moveTo(rockX, rockY);
                        } else {
                            rock.lineTo(rockX, rockY);
                        }
                    }
                    rock.closePath();
                    g.setColor(withAlpha(rockTint, 185.0 * fade));
                    g.fill(rock);
                }
            }
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
