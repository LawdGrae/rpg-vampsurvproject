import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** Compact contact flashes with continuous, seeded particle motion. */
public class CombatImpactEffect {
    private static final double MAX_LIFE = 0.38;
    private static final Color WARM = new Color(255, 172, 78);
    private static final Color FROST = new Color(133, 224, 255);
    private static final Color ELECTRIC = new Color(127, 203, 255);
    private static final Color VOID = new Color(184, 115, 255);
    private static final Color SACRED = new Color(255, 223, 126);
    private static final Color VENOM = new Color(146, 236, 99);
    private static final Color STEEL = new Color(255, 216, 153);
    private final double worldX, worldY;
    private final DamageElement element;
    private final int seed;
    private final boolean skillImpact;
    private final SkillEffectAtlas.SkillAnimation animation;
    private final Color color;
    private final Particle[] particles;
    private final double duration;
    private double life;

    public CombatImpactEffect(double worldX, double worldY, DamageElement element) {
        this(null, worldX, worldY, element);
    }

    /** Reviewed PNG tracks take precedence; missing tracks still receive a contact accent. */
    public CombatImpactEffect(String skillId, double worldX, double worldY, DamageElement element) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.element = element;
        this.seed = (int) Math.round(worldX * 31 + worldY * 17) ^ element.ordinal() * 0x45d9f3b;
        this.skillImpact = skillId != null;
        this.animation = skillId == null ? null : SkillEffectAtlas.getAnimation(skillId, "impact");
        this.color = switch (element) {
            case FIRE, EXPLOSION -> WARM;
            case ICE -> FROST;
            case LIGHTNING -> ELECTRIC;
            case SHADOW -> VOID;
            case HOLY -> SACRED;
            case POISON -> VENOM;
            default -> STEEL;
        };
        int count = element == DamageElement.EXPLOSION ? 16 : skillImpact ? 12
                : element == DamageElement.PHYSICAL ? 7 : 9;
        this.particles = new Particle[animation == null ? count : 0];
        for (int i = 0; i < particles.length; i++) {
            double angle = (i + unit(i, 0) * 0.75) / count * Math.PI * 2;
            particles[i] = new Particle(angle, 17 + unit(i, 1) * (skillImpact ? 29 : 23),
                    1.0 + unit(i, 2) * 1.8, unit(i, 3) * 0.08,
                    0.68 + unit(i, 4) * 0.30, 3.0 + unit(i, 5) * 6.0);
        }
        this.duration = animation == null ? MAX_LIFE : Math.max(0.05, animation.getDurationSeconds());
        this.life = duration;
    }
    public void update(double deltaTime) {
        if (Double.isFinite(deltaTime) && deltaTime > 0) life = Math.max(0, life - deltaTime);
    }
    public boolean isExpired() { return life <= 0; }

    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (isExpired()) return;
        double p = Math.min(1, 1 - life / duration);
        double x = centerX + worldX + cameraX, y = centerY + worldY + cameraY;
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        if (animation != null) {
            animation.drawAt(graphics, x, y, 76.0, 0.0, duration - life,
                    1.0 - smooth(Math.max(0.0, (p - 0.78) / 0.22)), false);
            return;
        }
        double alpha = 1 - smooth(p);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(x, y);
            double flash = Math.pow(1 - p, 5);
            double expansion = 1 - Math.pow(1 - p, 3);
            double size = skillImpact ? 1.15 : 0.86;
            glow(g, 0, 0, (20 + expansion * 18) * size, color, flash * 0.48 + alpha * 0.07);
            glow(g, 0, 0, (5 + expansion * 7) * size, Color.WHITE, flash * 0.82);
            drawShockRings(g, p, expansion, alpha, size);
            drawContactFlare(g, p, flash, size);
            drawContactCore(g, p, expansion, size);
            for (int i = 0; i < particles.length; i++) {
                drawParticle(g, particles[i], i, p);
            }
        } finally {
            g.dispose();
        }
    }

    private void drawContactCore(Graphics2D g, double p, double expansion, double size) {
        double coreProgress = Math.min(1, p / 0.48);
        double strength = Math.pow(1 - coreProgress, 2);
        if (strength < 0.003) {
            return;
        }

        // A small, crisp contact point keeps the hit readable inside the soft glow.
        double rotation = ((seed >>> 8) & 1023) * Math.PI * 2 / 1024;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        double coreLength = (skillImpact ? 5.2 : 3.6) * size * (1 - coreProgress * 0.55);
        double coreWidth = coreLength * 0.38;
        Path2D.Double core = new Path2D.Double();
        core.moveTo(-sin * coreLength, cos * coreLength);
        core.lineTo(cos * coreWidth, sin * coreWidth);
        core.lineTo(sin * coreLength, -cos * coreLength);
        core.lineTo(-cos * coreWidth, -sin * coreWidth);
        core.closePath();
        g.setColor(withAlpha(Color.WHITE, strength * 230));
        g.fill(core);

        if (!skillImpact) {
            return;
        }

        // Six restrained facets form a short crown around powerful skill contacts.
        // Their fixed count and short lifetime keep crowded combat inexpensive.
        double innerRadius = (5.5 + expansion * 11) * size;
        double facetLength = (2.5 + (1 - coreProgress) * 4.5) * size;
        double facetWidth = (0.65 + (1 - coreProgress) * 1.2) * size;
        for (int i = 0; i < 6; i++) {
            double angle = rotation + i * Math.PI / 3;
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            double midpoint = innerRadius + facetLength * 0.45;
            Path2D.Double facet = new Path2D.Double();
            facet.moveTo(dx * innerRadius, dy * innerRadius);
            facet.lineTo(dx * midpoint - dy * facetWidth, dy * midpoint + dx * facetWidth);
            facet.lineTo(dx * (innerRadius + facetLength), dy * (innerRadius + facetLength));
            facet.lineTo(dx * midpoint + dy * facetWidth, dy * midpoint - dx * facetWidth);
            facet.closePath();
            g.setColor(withAlpha(color, strength * 112));
            g.fill(facet);
            stroke(g, facet, Color.WHITE, 0.7 * size, strength * 0.36);
        }
    }

    private void drawShockRings(Graphics2D g, double p, double expansion, double alpha, double size) {
        double radius = (4 + expansion * 29) * size;
        double opacity = alpha * (skillImpact ? 0.48 : 0.24);
        double turn = Math.floorMod(seed, 90) + p * 18;
        for (int part = 0; part < 3; part++) {
            Arc2D arc = new Arc2D.Double(-radius, -radius * 0.65, radius * 2, radius * 1.3,
                    turn + part * 120, 88, Arc2D.OPEN);
            stroke(g, arc, color, 4.2 * (1 - p) + 0.6, opacity * 0.16);
            stroke(g, arc, color, 1.4 * (1 - p) + 0.55, opacity);
        }
        if (skillImpact && p > 0.09) {
            double t = Math.min(1, (p - 0.09) / 0.91);
            double inner = (3 + (1 - Math.pow(1 - t, 3)) * 21) * size;
            double a = smooth(t / 0.13) * (1 - smooth(t)) * 0.28;
            stroke(g, new Ellipse2D.Double(-inner, -inner * 0.65, inner * 2, inner * 1.3),
                    Color.WHITE, 0.8, a);
        }
    }

    private void drawContactFlare(Graphics2D g, double p, double flash, double size) {
        double reach = (6 + (1 - Math.pow(1 - p, 2)) * 15) * size;
        if (element == DamageElement.HOLY || element == DamageElement.LIGHTNING) {
            double flare = (1 - smooth(p / 0.58)) * 0.8;
            stroke(g, new Line2D.Double(-reach, 0, reach, 0), color, 4, flare * 0.14);
            stroke(g, new Line2D.Double(-reach, 0, reach, 0), Color.WHITE, 1.15, flare);
            stroke(g, new Line2D.Double(0, -reach * 1.35, 0, reach * 1.35),
                    Color.WHITE, 1.0, flare * 0.85);
        } else if (element == DamageElement.PHYSICAL || element == DamageElement.FIRE
                || element == DamageElement.EXPLOSION) {
            double diagonal = Math.PI * (0.15 + unit(2, 8) * 0.30);
            double dx = Math.cos(diagonal) * reach, dy = Math.sin(diagonal) * reach;
            stroke(g, new Line2D.Double(-dx, -dy, dx, dy), color, 5, flash * 0.18);
            stroke(g, new Line2D.Double(-dx, -dy, dx, dy), Color.WHITE, 1.5, flash * 0.85);
            stroke(g, new Line2D.Double(-dy * 0.55, dx * 0.55, dy * 0.55, -dx * 0.55),
                    color, 1.0, flash * 0.65);
        }
    }

    private void drawParticle(Graphics2D g, Particle particle, int index, double p) {
        if (p < particle.delay()) return;
        double t = Math.min(1, (p - particle.delay()) / (1 - particle.delay()));
        double appear = smooth(t / 0.09);
        double alpha = appear * (1 - smooth(t)) * particle.opacity();
        // Analytic drag keeps motion consistent at any update frequency and moving as it fades.
        double distance = 3 + particle.distance() * t * (1.28 - 0.28 * t);
        double angle = particle.angle();
        double px = Math.cos(angle) * distance, py = Math.sin(angle) * distance * 0.70;
        if (element == DamageElement.FIRE || element == DamageElement.EXPLOSION) py += t * t * 12;
        if (element == DamageElement.SHADOW || element == DamageElement.POISON) py -= t * t * 13;
        double size = particle.size() * (1 - t * 0.4);
        if (element == DamageElement.ICE) {
            double spin = angle + t * (index % 2 == 0 ? 0.32 : -0.32);
            double dx = Math.cos(spin), dy = Math.sin(spin), length = size * 3.1;
            Path2D shard = new Path2D.Double();
            shard.moveTo(px + dx * length, py + dy * length);
            shard.lineTo(px - dy * size, py + dx * size);
            shard.lineTo(px - dx * length * 0.65, py - dy * length * 0.65);
            shard.lineTo(px + dy * size, py - dx * size);
            shard.closePath();
            g.setColor(withAlpha(color, 180 * alpha));
            g.fill(shard);
            stroke(g, new Line2D.Double(px - dx * length * 0.5, py - dy * length * 0.5,
                    px + dx * length, py + dy * length), Color.WHITE, 0.75, alpha * 0.84);
        } else if (element == DamageElement.LIGHTNING) {
            double bend = (index % 2 == 0 ? 1 : -1) * (3 + particle.size());
            Path2D bolt = new Path2D.Double();
            bolt.moveTo(px * 0.20, py * 0.20);
            bolt.lineTo(px * 0.49 - Math.sin(angle) * bend, py * 0.49 + Math.cos(angle) * bend);
            bolt.lineTo(px * 0.65 + Math.sin(angle) * bend * 0.6,
                    py * 0.65 - Math.cos(angle) * bend * 0.6);
            bolt.lineTo(px, py);
            double lightning = alpha * (1 - smooth(t / 0.82));
            stroke(g, bolt, color, 5, lightning * 0.15);
            stroke(g, bolt, color, 2.1, lightning * 0.65);
            stroke(g, bolt, Color.WHITE, 0.85, lightning * 0.88);
        } else if (element == DamageElement.SHADOW) {
            Path2D wisp = new Path2D.Double();
            wisp.moveTo(px * 0.68, py * 0.68);
            wisp.quadTo(px - Math.sin(angle) * 7, py + Math.cos(angle) * 7, px, py);
            stroke(g, wisp, color, size * 2.0, alpha * 0.13);
            stroke(g, wisp, color, size * 0.68, alpha * 0.56);
            if (index % 3 == 0) glow(g, px, py, size * 3.6, color, alpha * 0.23);
        } else if (element == DamageElement.POISON) {
            double bubble = size * (1.1 + t * 0.45);
            stroke(g, new Ellipse2D.Double(px - bubble, py - bubble, bubble * 2, bubble * 2),
                    color, 1.0, alpha * 0.62);
            g.setColor(withAlpha(Color.WHITE, alpha * 135));
            g.fill(new Ellipse2D.Double(px - bubble * 0.52, py - bubble * 0.60, 0.95, 0.95));
            if (index % 4 == 0) glow(g, px, py, bubble * 3, color, alpha * 0.16);
        } else {
            double length = 1.2 + particle.length() * (1 - t * 0.70);
            Line2D spark = new Line2D.Double(px, py, px - Math.cos(angle) * length,
                    py - Math.sin(angle) * length * 0.70);
            stroke(g, spark, color, size * 2.2, alpha * 0.14);
            stroke(g, spark, index % 3 == 0 ? Color.WHITE : color, size * 0.65, alpha * 0.90);
            if (index % 4 == 0) glow(g, px, py, size * 2.8, color, alpha * 0.20);
            if (element == DamageElement.HOLY && index % 3 == 0) {
                stroke(g, new Line2D.Double(px - size * 1.5, py, px + size * 1.5, py),
                        Color.WHITE, 0.8, alpha * 0.65);
                stroke(g, new Line2D.Double(px, py - size * 2, px, py + size * 2),
                        Color.WHITE, 0.8, alpha * 0.65);
            }
        }
    }

    private static void glow(Graphics2D g, double x, double y, double radius, Color c, double a) {
        SkillVfxGlow.draw(g, x, y, radius, c, a);
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
    private record Particle(double angle, double distance, double size, double delay,
            double opacity, double length) { }
}
