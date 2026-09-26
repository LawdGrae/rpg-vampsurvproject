import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public class CombatImpactEffect {
    private static final double MAX_LIFE = 0.32;

    private final double worldX;
    private final double worldY;
    private final DamageElement element;
    private final int seed;
    private double life = MAX_LIFE;

    public CombatImpactEffect(double worldX, double worldY, DamageElement element) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.element = element;
        this.seed = Math.abs((int) Math.round(worldX * 31.0 + worldY * 17.0)
                ^ element.ordinal() * 1103515245);
    }

    public void update(double deltaTime) {
        life -= deltaTime;
    }

    public boolean isExpired() {
        return life <= 0.0;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double progress = 1.0 - Math.max(0.0, life / MAX_LIFE);
        double alpha = Math.max(0.0, life / MAX_LIFE);
        int x = (int) Math.round(centerX + worldX + cameraX);
        int y = (int) Math.round(centerY + worldY + cameraY);

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (element) {
            case FIRE, EXPLOSION -> drawFireImpact(g, x, y, progress, alpha);
            case ICE -> drawIceImpact(g, x, y, progress, alpha);
            case LIGHTNING -> drawElectricImpact(g, x, y, progress, alpha);
            case SHADOW -> drawShadowImpact(g, x, y, progress, alpha);
            case HOLY -> drawHolyImpact(g, x, y, progress, alpha);
            case POISON -> drawPoisonImpact(g, x, y, progress, alpha);
            default -> drawPhysicalImpact(g, x, y, progress, alpha);
        }

        g.dispose();
    }

    private void drawFireImpact(Graphics2D g, int x, int y, double p, double a) {
        drawFlash(g, x, y, 34 + 24 * p, new Color(255, 105, 30), a);
        for (int i = 0; i < 14; i++) {
            double angle = angle(i);
            double distance = 8 + p * (24 + i % 5 * 5);
            int px = (int) Math.round(x + Math.cos(angle) * distance);
            int py = (int) Math.round(y + Math.sin(angle) * distance);
            int size = 4 + i % 4;
            g.setColor(withAlpha(i % 2 == 0 ? new Color(255, 210, 70) : new Color(255, 65, 30), 220 * a));
            g.fillRect(px - size / 2, py - size / 2, size, size);
        }
    }

    private void drawIceImpact(Graphics2D g, int x, int y, double p, double a) {
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 9; i++) {
            double angle = angle(i);
            double length = 16 + p * (28 + i % 3 * 8);
            int ex = (int) Math.round(x + Math.cos(angle) * length);
            int ey = (int) Math.round(y + Math.sin(angle) * length);
            g.setColor(withAlpha(new Color(175, 245, 255), 210 * a));
            g.drawLine(x, y, ex, ey);
            drawShard(g, ex, ey, angle, 8 + i % 5, a);
        }
        drawFlash(g, x, y, 28 + 16 * p, new Color(205, 250, 255), a * 0.8);
    }

    private void drawElectricImpact(Graphics2D g, int x, int y, double p, double a) {
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 7; i++) {
            double angle = angle(i);
            int ex = (int) Math.round(x + Math.cos(angle) * (22 + 26 * p));
            int ey = (int) Math.round(y + Math.sin(angle) * (14 + 20 * p));
            g.setColor(withAlpha(new Color(105, 210, 255), 210 * a));
            g.drawLine(x, y, (x + ex) / 2 + offset(i, 0, 8), (y + ey) / 2 + offset(i, 1, 8));
            g.setColor(withAlpha(Color.WHITE, 220 * a));
            g.drawLine((x + ex) / 2 + offset(i, 0, 8), (y + ey) / 2 + offset(i, 1, 8), ex, ey);
        }
        drawFlash(g, x, y, 42, new Color(160, 230, 255), a);
    }

    private void drawShadowImpact(Graphics2D g, int x, int y, double p, double a) {
        drawFlash(g, x, y, 38 + 20 * p, new Color(75, 35, 115), a * 0.9);
        for (int i = 0; i < 10; i++) {
            int size = 12 + i % 7;
            int px = x + offset(i, 0, (int) (34 + 24 * p));
            int py = y + offset(i, 1, (int) (24 + 20 * p)) - (int) (18 * p);
            g.setColor(withAlpha(new Color(28, 18, 38), 150 * a));
            g.fillOval(px - size / 2, py - size / 2, size, size);
        }
    }

    private void drawHolyImpact(Graphics2D g, int x, int y, double p, double a) {
        drawFlash(g, x, y, 36 + 22 * p, new Color(255, 240, 145), a);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 200 * a));
        g.drawLine(x, y - 18, x, y + 18);
        g.drawLine(x - 18, y, x + 18, y);
    }

    private void drawPoisonImpact(Graphics2D g, int x, int y, double p, double a) {
        for (int i = 0; i < 11; i++) {
            int size = 7 + i % 8;
            int px = x + offset(i, 0, (int) (28 + 22 * p));
            int py = y + offset(i, 1, (int) (22 + 14 * p));
            g.setColor(withAlpha(new Color(75, 230, 65), 175 * a));
            g.fillOval(px - size / 2, py - size / 2, size, size);
        }
    }

    private void drawPhysicalImpact(Graphics2D g, int x, int y, double p, double a) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            double angle = -0.9 + i * 0.45;
            int length = (int) Math.round(26 + p * 26);
            g.setColor(withAlpha(new Color(255, 225, 115), 190 * a));
            g.drawLine(x, y, x + (int) (Math.cos(angle) * length),
                    y + (int) (Math.sin(angle) * length));
        }
    }

    private void drawShard(Graphics2D g, int x, int y, double angle, double size, double a) {
        Path2D shard = new Path2D.Double();
        shard.moveTo(x + Math.cos(angle) * size, y + Math.sin(angle) * size);
        shard.lineTo(x + Math.cos(angle + 2.35) * size * 0.55,
                y + Math.sin(angle + 2.35) * size * 0.55);
        shard.lineTo(x + Math.cos(angle - 2.35) * size * 0.55,
                y + Math.sin(angle - 2.35) * size * 0.55);
        shard.closePath();
        g.setColor(withAlpha(new Color(205, 250, 255), 210 * a));
        g.fill(shard);
    }

    private void drawFlash(Graphics2D g, int x, int y, double size, Color color, double alpha) {
        g.setColor(withAlpha(color, 85 * alpha));
        g.fillOval((int) (x - size / 2.0), (int) (y - size / 2.0), (int) size, (int) size);
    }

    private double angle(int index) {
        return Math.PI * 2.0 * index / 11.0 + (seed % 31) * 0.017;
    }

    private int offset(int index, int salt, int spread) {
        int value = seed + index * 1103515245 + salt * 12345;
        value ^= value >>> 16;
        int range = Math.max(1, spread * 2 + 1);
        return Math.floorMod(value, range) - spread;
    }

    private Color withAlpha(Color base, double alpha) {
        int safeAlpha = Math.max(0, Math.min(255, (int) Math.round(alpha)));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), safeAlpha);
    }
}
