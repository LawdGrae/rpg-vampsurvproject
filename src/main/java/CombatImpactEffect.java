import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/** Small, continuous impact accents; particles retain their velocity as the hit decays. */
public class CombatImpactEffect {
    private static final double MAX_LIFE = 0.38;
    private final double worldX, worldY;
    private final DamageElement element;
    private final int seed;
    private double life = MAX_LIFE;

    public CombatImpactEffect(double worldX, double worldY, DamageElement element) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.element = element;
        this.seed = (int) Math.round(worldX * 31 + worldY * 17) ^ element.ordinal() * 0x45d9f3b;
    }
    public void update(double deltaTime) {
        if (Double.isFinite(deltaTime) && deltaTime > 0) life = Math.max(0, life - deltaTime);
    }
    public boolean isExpired() { return life <= 0; }

    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (isExpired()) return;
        double p = Math.min(1, 1 - life / MAX_LIFE);
        double alpha = 1 - smooth(p);
        Color c = switch (element) {
            case FIRE, EXPLOSION -> new Color(255, 157, 68);
            case ICE -> new Color(163, 232, 255);
            case LIGHTNING -> new Color(139, 213, 255);
            case SHADOW -> new Color(176, 100, 226);
            case HOLY -> new Color(255, 231, 157);
            case POISON -> new Color(126, 220, 105);
            default -> new Color(255, 225, 164);
        };
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centerX + worldX + cameraX, centerY + worldY + cameraY);
        double core = Math.pow(1 - p, 3);
        glow(g, 0, 0, 19 + p * 15, c, core * 0.55);
        glow(g, 0, 0, 5 + p * 2, Color.WHITE, core * 0.7);
        int count = element == DamageElement.EXPLOSION ? 16 : element == DamageElement.PHYSICAL ? 7 : 11;
        double travel = 1 - Math.pow(1 - p, 2);
        for (int i = 0; i < count; i++) {
            double angle = unit(i, 0) * Math.PI * 2;
            double speed = 23 + unit(i, 1) * 29;
            double px = Math.cos(angle) * (4 + travel * speed);
            double py = Math.sin(angle) * (4 + travel * speed) * 0.7;
            if (element == DamageElement.FIRE || element == DamageElement.EXPLOSION) py += p * p * 19;
            if (element == DamageElement.SHADOW || element == DamageElement.POISON) py -= p * 12;
            double pa = alpha * (0.65 + unit(i, 2) * 0.35);
            double size = 1.5 + unit(i, 3) * 2.5;
            if (element == DamageElement.ICE) {
                Path2D shard = new Path2D.Double();
                double length = size * 2.6;
                shard.moveTo(px + Math.cos(angle) * length, py + Math.sin(angle) * length);
                shard.lineTo(px + Math.cos(angle + 2.4) * length * 0.45, py + Math.sin(angle + 2.4) * length * 0.45);
                shard.lineTo(px + Math.cos(angle - 2.4) * length * 0.45, py + Math.sin(angle - 2.4) * length * 0.45);
                shard.closePath(); g.setColor(withAlpha(c, 180 * pa)); g.fill(shard);
            } else if (element == DamageElement.LIGHTNING) {
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(px * 0.38, py * 0.38);
                bolt.lineTo(px * 0.64 + Math.sin(p * 10 + i) * 3, py * 0.64 + Math.cos(p * 10 + i) * 3);
                bolt.lineTo(px, py);
                stroke(g, bolt, c, 5, pa * 0.13); stroke(g, bolt, Color.WHITE, 1.3, pa * 0.75);
            } else if (element == DamageElement.SHADOW) {
                glow(g, px, py, size * (2.8 + p * 1.4), c, pa * 0.25);
            } else if (element == DamageElement.POISON) {
                g.setColor(withAlpha(c, pa * 145));
                g.setStroke(new BasicStroke(1.1f));
                g.draw(new Ellipse2D.Double(px - size, py - size, size * 2, size * 2));
            } else {
                double length = 2 + (1 - p) * (4 + unit(i, 4) * 5);
                stroke(g, new Line2D.Double(px, py, px - Math.cos(angle) * length, py - Math.sin(angle) * length * 0.7),
                        i % 3 == 0 ? Color.WHITE : c, size * 0.5, pa * 0.8);
                if (i % 4 == 0) glow(g, px, py, size * 2, c, pa * 0.25);
            }
        }
        if (element == DamageElement.HOLY) {
            double reach = 8 + travel * 10;
            stroke(g, new Line2D.Double(-reach, 0, reach, 0), c, 1.5, alpha * 0.7);
            stroke(g, new Line2D.Double(0, -reach * 1.2, 0, reach * 1.2), Color.WHITE, 1.2, alpha * 0.8);
        }
        if (element != DamageElement.PHYSICAL) {
            double r = 6 + travel * 26;
            Arc2D arc = new Arc2D.Double(-r, -r * 0.65, r * 2, r * 1.3,
                    seed % 80 + p * 30, 240, Arc2D.OPEN);
            stroke(g, arc, c, 1.4, alpha * 0.3);
        }
        g.dispose();
    }

    private static void glow(Graphics2D g, double x, double y, double radius, Color c, double a) {
        if (a < 0.003) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) radius,
                new float[] {0, 0.3f, 1}, new Color[] {withAlpha(c, 220 * a), withAlpha(c, 95 * a), withAlpha(c, 0)}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }
    private static void stroke(Graphics2D g, java.awt.Shape shape, Color c, double width, double a) {
        if (a < 0.003) return;
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 255 * a)); g.draw(shape);
    }
    private double unit(int index, int salt) {
        int value = seed ^ (index + 1) * 0x45d9f3b ^ (salt + 1) * 0x119de1f3;
        value ^= value >>> 16; value *= 0x45d9f3b; value ^= value >>> 16;
        return (value & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }
    private static double smooth(double p) { double t = Math.max(0, Math.min(1, p)); return t * t * (3 - 2 * t); }
    private static Color withAlpha(Color c, double a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, Math.round(a)))); }
}
