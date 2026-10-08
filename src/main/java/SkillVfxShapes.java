import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Small, deterministic magic shapes; all angles are in radians. */
final class SkillVfxShapes {
    private static final double TAU = Math.PI * 2.0;
    private SkillVfxShapes() { }

    static void castSigil(Graphics2D g, double x, double y, double radius,
                          Color color, double rotation, double alpha) {
        if (g == null || color == null || !valid(x, y, radius, rotation, alpha)) return;
        double a = opacity(color, alpha), r = Math.min(radius, 8192.0);
        if (a <= 0.0) return;
        Graphics2D p = (Graphics2D) g.create();
        try {
            p.setComposite(AlphaComposite.SrcOver);
            SkillVfxGlow.draw(p, x, y, r * .72, opaque(color), a * .09);
            p.translate(x, y);
            p.rotate(rotation % TAU);
            p.setStroke(stroke(Math.min(5.0, Math.max(1.5, r * .08))));
            p.setColor(tint(color, a * .13, .18));
            ring(p, r, 8, 31.0, 0.0);
            ring(p, r * .68, 8, 24.0, 22.5);
            p.setStroke(stroke(Math.min(2.1, Math.max(.7, r * .035))));
            p.setColor(tint(color, a * .78, .25));
            ring(p, r, 8, 31.0, 0.0);
            ring(p, r * .68, 8, 24.0, 22.5);
            Path2D.Double runes = new Path2D.Double();
            for (int i = 0; i < 8; i++) {
                double angle = i * TAU / 8.0;
                double c = Math.cos(angle), s = Math.sin(angle), middle = r * .84;
                double length = r * .055, width = r * .022;
                runes.moveTo(c * (middle + length), s * (middle + length));
                runes.lineTo(c * middle - s * width, s * middle + c * width);
                runes.lineTo(c * (middle - length), s * (middle - length));
                runes.lineTo(c * middle + s * width, s * middle - c * width);
                runes.closePath();
                if ((i & 1) == 0) {
                    runes.moveTo(c * r * .59, s * r * .59);
                    runes.lineTo(c * r * .47, s * r * .47);
                }
            }
            p.setColor(tint(color, a * .82, .48));
            p.fill(runes);
            p.draw(runes);
        } finally { p.dispose(); }
    }

    static void crescent(Graphics2D g, double x, double y, double angle,
                         double radius, double thickness, Color color, double alpha) {
        if (g == null || color == null || !valid(x, y, radius, angle, alpha)
                || !Double.isFinite(thickness) || thickness <= 0.0) return;
        double a = opacity(color, alpha), r = Math.min(radius, 8192.0);
        double width = Math.min(thickness, r * .55);
        if (a <= 0.0) return;
        Graphics2D p = (Graphics2D) g.create();
        try {
            p.setComposite(AlphaComposite.SrcOver);
            p.translate(x, y);
            p.rotate(angle % TAU);
            Path2D.Double blade = taperedArc(r, width, 1.0);
            p.setStroke(stroke(Math.max(.6, Math.min(8.0, width * .6))));
            p.setColor(tint(color, a * .14, .12));
            p.draw(blade);
            p.setColor(tint(color, a * .87, .18));
            p.fill(blade);
            p.setStroke(stroke(Math.max(.5, Math.min(1.5, width * .12))));
            p.setColor(tint(color, a * .48, .55));
            p.draw(blade);
            p.setColor(tint(color, a * .93, .88));
            p.fill(taperedArc(r, width, .23));
        } finally { p.dispose(); }
    }

    static void shockwave(Graphics2D g, double x, double y, double radius,
                          Color color, double rotation, double alpha, double verticalScale) {
        if (g == null || color == null || !valid(x, y, radius, rotation, alpha)
                || !Double.isFinite(verticalScale) || verticalScale <= 0.0) return;
        double a = opacity(color, alpha), r = Math.min(radius, 8192.0);
        double width = Math.min(r * .18, Math.max(1.2, Math.min(7.0, r * .07)));
        if (a <= 0.0) return;
        Graphics2D p = (Graphics2D) g.create();
        try {
            p.setComposite(AlphaComposite.SrcOver);
            p.translate(x, y);
            p.scale(1.0, Math.max(.08, Math.min(2.0, verticalScale)));
            Ellipse2D.Double rim = new Ellipse2D.Double(-r, -r, r * 2.0, r * 2.0);
            p.setStroke(stroke(Math.max(.8, width * 2.0)));
            p.setColor(tint(color, a * .10, .08));
            p.draw(rim);
            p.setStroke(stroke(Math.max(.6, width * .5)));
            p.setColor(tint(color, a * .24, .42));
            p.draw(rim);
            Path2D.Double shards = new Path2D.Double();
            Path2D.Double rays = new Path2D.Double();
            for (int i = 0; i < 14; i++) {
                double angle = rotation % TAU + i * TAU / 14.0;
                double left = angle - .12, right = angle + .12;
                point(shards, r, left, true);
                point(shards, r + width * .28, angle, false);
                point(shards, r, right, false);
                point(shards, r - width * .65, right, false);
                point(shards, r - width, angle, false);
                point(shards, r - width * .65, left, false);
                shards.closePath();
                if ((i & 1) == 0) {
                    point(rays, r + width * .75, angle, true);
                    point(rays, r + width * 2.8, angle, false);
                }
            }
            p.setColor(tint(color, a * .72, .32));
            p.fill(shards);
            p.setStroke(stroke(Math.max(.6, Math.min(1.6, width * .25))));
            p.setColor(tint(color, a * .55, .66));
            p.draw(rays);
        } finally { p.dispose(); }
    }

    static void sparkle(Graphics2D g, double x, double y, double radius,
                        Color color, double rotation, double alpha) {
        if (g == null || color == null || !valid(x, y, radius, rotation, alpha)) return;
        double a = opacity(color, alpha), r = Math.min(radius, 8192.0);
        if (a <= 0.0) return;
        Graphics2D p = (Graphics2D) g.create();
        try {
            p.setComposite(AlphaComposite.SrcOver);
            SkillVfxGlow.draw(p, x, y, r * .8, opaque(color), a * .16);
            p.translate(x, y);
            p.rotate(rotation % TAU);
            Path2D.Double star = new Path2D.Double();
            for (int i = 0; i < 8; i++) point(star, (i & 1) == 0 ? r : r * .21,
                    -Math.PI / 2.0 + i * TAU / 8.0, i == 0);
            star.closePath();
            p.setColor(tint(color, a * .86, .28));
            p.fill(star);
            p.scale(.36, .36);
            p.setColor(tint(color, a * .95, .94));
            p.fill(star);
        } finally { p.dispose(); }
    }

    private static Path2D.Double taperedArc(double radius, double width, double core) {
        Path2D.Double shape = new Path2D.Double();
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step <= 18; step++) {
                double t = (side == 0 ? step : 18 - step) / 18.0;
                double taper = Math.sin(Math.PI * t), angle = (t - .5) * 2.2;
                double distance = radius - width * taper * (.5 + (side == 0 ? -.5 : .5) * core);
                point(shape, distance, angle, side == 0 && step == 0);
            }
        }
        shape.closePath();
        return shape;
    }

    private static void ring(Graphics2D g, double radius, int count, double sweep, double offset) {
        Arc2D.Double arc = new Arc2D.Double();
        for (int i = 0; i < count; i++) {
            arc.setArc(-radius, -radius, radius * 2.0, radius * 2.0,
                    i * 360.0 / count + offset - sweep * .5, sweep, Arc2D.OPEN);
            g.draw(arc);
        }
    }

    private static void point(Path2D.Double path, double radius, double angle, boolean move) {
        double x = radius * Math.cos(angle), y = radius * Math.sin(angle);
        if (move) path.moveTo(x, y); else path.lineTo(x, y);
    }

    private static boolean valid(double x, double y, double radius, double angle, double alpha) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(radius)
                && radius > 0.0 && Double.isFinite(angle) && Double.isFinite(alpha);
    }

    private static double opacity(Color color, double alpha) {
        return Math.max(0.0, Math.min(1.0, alpha)) * color.getAlpha() / 255.0;
    }

    private static BasicStroke stroke(double width) {
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static Color opaque(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color tint(Color color, double alpha, double white) {
        return new Color((int) Math.round(color.getRed() + (255 - color.getRed()) * white),
                (int) Math.round(color.getGreen() + (255 - color.getGreen()) * white),
                (int) Math.round(color.getBlue() + (255 - color.getBlue()) * white),
                (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0));
    }
}
