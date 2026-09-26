import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

public class AbilityVisualEffect {
    private final AbilityDefinition definition;
    private final double startX;
    private final double startY;
    private final double worldX;
    private final double worldY;
    private final double radius;
    private final Color color;
    private final double maxLife;
    private final int variant;
    private double life;

    public AbilityVisualEffect(AbilityDefinition definition, double startX,
            double startY, double worldX, double worldY, double radius,
            Color color, double maxLife) {
        this.definition = definition;
        this.startX = startX;
        this.startY = startY;
        this.worldX = worldX;
        this.worldY = worldY;
        this.radius = Math.max(40.0, radius);
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.variant = Math.abs(definition.getId().hashCode());
    }

    public void update(double deltaTime) {
        life -= deltaTime;
    }

    public boolean isExpired() {
        return life <= 0.0;
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double progress = 1.0 - Math.max(0.0, life / maxLife);
        double alpha = Math.max(0.0, life / maxLife);
        int sx = screenX(centerX, cameraX, startX);
        int sy = screenY(centerY, cameraY, startY);
        int x = screenX(centerX, cameraX, worldX);
        int y = screenY(centerY, cameraY, worldY);

        Graphics2D effectGraphics = (Graphics2D) graphics.create();
        effectGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        drawByAbility(effectGraphics, sx, sy, x, y, progress, alpha);
        effectGraphics.dispose();
    }

    private void drawByAbility(Graphics2D g, int sx, int sy, int x, int y,
            double p, double a) {
        switch (definition.getId()) {
            case "shadow_strike" -> {
                drawShadowTeleport(g, sx, sy, x, y, p, a);
            }
            case "twin_fang" -> {
                drawSlash(g, x - 12, y, -34, radius * 0.8, new Color(230, 55, 70), a);
                drawSlash(g, x + 12, y, 34, radius * 0.8, new Color(170, 75, 230), a);
                drawParticles(g, x, y, new Color(210, 40, 60), 14, 95, p, a);
            }
            case "poison_blade" -> {
                drawTrail(g, sx, sy, x, y, new Color(35, 235, 70), 16, a);
                drawSlash(g, x, y, -20, radius * 0.65, new Color(90, 255, 90), a);
                drawBubbles(g, x, y, radius * 0.6, new Color(80, 255, 70), p, a);
            }
            case "smoke_veil" -> {
                drawSmoke(g, sx, sy, radius, new Color(18, 18, 24), p, a, 18);
                drawOrbit(g, sx, sy, radius * 0.55, new Color(85, 55, 120), p, a, 5);
            }
            case "shadow_step" -> {
                drawDashStep(g, sx, sy, x, y, new Color(85, 45, 135), p, a);
                drawShadowTeleport(g, sx, sy, x, y, p, a * 0.72);
            }
            case "venom_burst" -> {
                drawToxicSplash(g, x, y, radius, p, a);
                drawBubbles(g, x, y, radius, new Color(65, 220, 55), p, a);
                drawClouds(g, x, y, radius * 0.65, new Color(75, 170, 55), p, a, 7);
            }
            case "phantom_clone" -> {
                drawHumanoid(g, sx + 45, sy, new Color(70, 40, 110), a * 0.75);
                drawSlash(g, x, y, -18, radius * 0.75, new Color(170, 100, 240), a);
                drawSmoke(g, sx + 45, sy, 80, new Color(38, 26, 55), p, a, 8);
            }
            case "death_mark" -> {
                drawLink(g, sx, sy, x, y, new Color(150, 75, 230), a);
                drawSkullMark(g, x, y - 60, new Color(150, 50, 230), a);
                drawRing(g, x, y, 55 + Math.sin(p * Math.PI * 4.0) * 12, new Color(90, 35, 135), 10, a);
            }
            case "silent_execution" -> {
                drawAfterimages(g, sx, sy, x, y, new Color(35, 20, 45), a, 6);
                for (int i = 0; i < 4; i++) {
                    drawSlash(g, x, y, -70 + i * 38, radius * (0.55 + i * 0.08), Color.WHITE, a * 0.9);
                }
                drawImpactBurst(g, x, y, new Color(30, 10, 35), p, a);
            }
            case "shadow_assassin" -> {
                drawSmoke(g, sx, sy, radius, new Color(20, 15, 30), p, a, 20);
                drawOrbitingBlades(g, sx, sy, radius * 0.55, new Color(140, 70, 220), p, a, 7);
                drawRing(g, sx, sy, radius * p, new Color(85, 45, 135), 22, a);
            }
            case "heavy_slash" -> {
                drawWeaponSwing(g, sx, sy, directionAngle(sx, sy, x, y) + 0.35, "axe",
                        new Color(195, 190, 175), p, a);
                drawSlash(g, sx, sy, 15 + p * 70, radius * 1.45, new Color(255, 210, 115), a);
                drawDust(g, x, y + 30, radius * 0.8, p, a);
                drawPhysicalImpactLines(g, x, y, radius * 0.55, p, a);
            }
            case "shield_bash" -> {
                drawShieldShape(g, x, y, 70, new Color(175, 185, 200), a);
                drawShockwave(g, x, y, radius, new Color(210, 210, 220), p, a);
                drawSparks(g, x, y, new Color(255, 210, 120), 12, p, a);
            }
            case "iron_guard" -> {
                drawShieldShape(g, sx, sy, radius * 0.85, new Color(150, 165, 185), a);
                drawOrbit(g, sx, sy, radius * 0.75, new Color(220, 230, 240), p, a, 6);
            }
            case "dark_taunt" -> {
                drawShockwaveSpikes(g, sx, sy, radius, new Color(150, 25, 35), p, a);
                drawAngerRunes(g, sx, sy, radius * 0.65, new Color(230, 60, 70), p, a);
            }
            case "shield_charge" -> {
                drawDashStep(g, sx, sy, x, y, new Color(180, 185, 195), p, a);
                drawGuardianBarrier(g, x, y, 95, new Color(180, 185, 195), p, a);
                drawDust(g, sx, sy + 36, radius, p, a);
            }
            case "earth_shatter" -> {
                drawGroundSlam(g, sx, sy, radius, p, a);
            }
            case "counter_strike" -> {
                drawShieldShape(g, sx, sy, radius * 0.75, new Color(235, 240, 255), a);
                drawSlash(g, sx, sy, -10, radius, new Color(250, 80, 70), a);
                drawFlash(g, sx, sy, radius * 0.55, Color.WHITE, a);
            }
            case "blood_armor" -> {
                drawAura(g, sx, sy, radius, new Color(130, 10, 25), p, a);
                drawDrops(g, sx, sy, radius * 0.75, new Color(200, 20, 45), p, a);
            }
            case "knights_wrath" -> {
                drawWhirlwind(g, sx, sy, radius, new Color(230, 50, 55), p, a);
            }
            case "dark_fortress" -> {
                drawGuardianBarrier(g, sx, sy, radius, new Color(95, 45, 65), p, a);
                drawRing(g, sx, sy, radius * 0.9, new Color(170, 40, 60), 20, a);
                drawOrbit(g, sx, sy, radius * 0.82, new Color(40, 25, 35), p, a, 10);
            }
            case "holy_bolt" -> {
                drawTrail(g, sx, sy, x, y, new Color(255, 245, 170), 12, a);
                drawFlash(g, x, y, 55, new Color(255, 250, 190), a);
                drawParticles(g, x, y, new Color(255, 230, 120), 10, 75, p, a);
            }
            case "heal" -> {
                drawHealingAura(g, sx, sy, radius, p, a);
            }
            case "blessing" -> {
                drawHolyCircle(g, sx, sy, radius * 0.8, new Color(255, 225, 105), p, a);
                drawOrbit(g, sx, sy, radius * 0.65, new Color(255, 250, 180), p, a, 7);
            }
            case "holy_shield" -> {
                drawGuardianBarrier(g, sx, sy, radius * 0.9, new Color(255, 245, 180), p, a);
                drawHolyCircle(g, sx, sy, radius, new Color(255, 235, 135), p, a);
            }
            case "purify" -> {
                drawSmoke(g, x, y, radius * 0.45, new Color(45, 35, 70), p, a, 7);
                drawFlash(g, x, y, radius * 0.75, new Color(255, 255, 230), a);
                drawRising(g, x, y, radius, new Color(255, 230, 100), p, a, 10);
            }
            case "divine_light" -> {
                drawHolyPillar(g, sx, sy, radius * 0.55, new Color(255, 250, 205), p, a);
                drawHolyCircle(g, sx, sy, radius, new Color(255, 220, 90), p, a);
                drawHealingAura(g, sx, sy, radius * 0.75, p, a * 0.85);
            }
            case "prayer" -> {
                drawHolyCircle(g, sx, sy, radius * 0.75, new Color(255, 230, 140), p, a);
                drawRising(g, sx, sy, radius * 0.85, new Color(255, 245, 175), p, a, 16);
            }
            case "sanctuary" -> {
                drawHolyCircle(g, sx, sy, radius, new Color(255, 220, 95), p, a);
                drawRing(g, sx, sy, radius * 0.75, new Color(255, 250, 180), 8, a);
            }
            case "resurrection" -> {
                drawHolyPillar(g, sx, sy, radius * 0.7, new Color(255, 235, 120), p, a);
                drawHolyCircle(g, sx, sy, radius * 0.9, new Color(255, 245, 180), p, a);
                drawRising(g, sx, sy, radius, Color.WHITE, p, a, 18);
            }
            case "divine_judgment" -> {
                drawJudgmentSymbol(g, x, y - 150, radius * 0.7, new Color(255, 245, 170), a);
                drawHolyPillar(g, x, y, radius, new Color(255, 250, 210), p, a);
                drawImpactBurst(g, x, y, new Color(255, 225, 100), p, a);
            }
            case "quick_shot" -> {
                drawArrow(g, sx, sy, x, y, new Color(230, 245, 190), a, 1.0);
                drawImpactBurst(g, x, y, new Color(210, 255, 160), p, a * 0.75);
            }
            case "power_arrow" -> {
                drawArrow(g, sx, sy, x, y, new Color(255, 230, 95), a, 1.5);
                drawShockwave(g, x, y, radius * 0.7, new Color(255, 180, 70), p, a);
            }
            case "multi_shot" -> {
                for (int i = -2; i <= 2; i++) {
                    drawArrow(g, sx - 12 * i, sy + 18, sx + 115, sy + i * 34, new Color(200, 255, 165), a, 0.85);
                }
            }
            case "poison_arrow" -> {
                drawArrow(g, sx, sy, x, y, new Color(85, 255, 75), a, 1.0);
                drawClouds(g, x, y, radius * 0.55, new Color(80, 190, 55), p, a, 5);
            }
            case "backstep" -> {
                drawDashStep(g, x, y, sx, sy, new Color(160, 210, 210), p, a);
                drawDust(g, sx, sy + 32, radius * 0.75, p, a);
            }
            case "explosive_arrow" -> {
                drawArrow(g, sx, sy, x, y, new Color(255, 100, 45), a, 1.15);
                drawFireBurst(g, x, y, radius, p, a);
            }
            case "rain_of_arrows" -> {
                drawTargetZone(g, x, y, radius, new Color(180, 255, 150), a);
                for (int i = 0; i < 12; i++) {
                    int ox = particleOffset(i, 0, (int) radius);
                    int fall = (int) (220 * p);
                    drawArrow(g, x + ox, y - 180 + fall, x + ox / 2,
                            y + particleOffset(i, 1, (int) radius / 2), new Color(230, 255, 190), a, 0.75);
                }
            }
            case "hunters_mark" -> {
                drawLink(g, sx, sy, x, y, new Color(255, 70, 70), a);
                drawTargetZone(g, x, y, radius * 0.45, new Color(255, 70, 70), a);
            }
            case "phantom_arrow" -> {
                for (int i = 0; i < 3; i++) {
                    drawArrow(g, sx - i * 12, sy + i * 12, x + i * 10, y - i * 10,
                            new Color(120, 175, 255), a * (0.9 - i * 0.18), 1.0);
                }
            }
            case "arrow_storm" -> {
                drawTargetZone(g, x, y, radius, new Color(180, 255, 140), a);
                for (int i = 0; i < 44; i++) {
                    int ox = particleOffset(i, 0, (int) radius);
                    int oy = particleOffset(i, 1, (int) radius / 2);
                    drawArrow(g, x + ox, y - 230 + (int) (260 * p), x + ox / 2, y + oy,
                            new Color(230, 255, 180), a * 0.78, 0.58);
                }
                drawDust(g, x, y + 28, radius, p, a);
            }
            case "dark_bolt" -> {
                drawTrail(g, sx, sy, x, y, new Color(55, 25, 85), 18, a);
                drawImpactBurst(g, x, y, new Color(90, 45, 150), p, a);
            }
            case "curse" -> {
                drawSkullMark(g, x, y - 58, new Color(140, 55, 220), a);
                drawOrbit(g, x, y, radius * 0.45, new Color(80, 35, 130), p, a, 5);
            }
            case "life_drain" -> {
                drawLink(g, x, y, sx, sy, new Color(180, 25, 55), a);
                drawParticles(g, (x + sx) / 2, (y + sy) / 2, new Color(220, 50, 75), 12, 90, p, a);
                drawRising(g, sx, sy, radius * 0.5, new Color(170, 30, 55), p, a, 8);
            }
            case "shadow_orb" -> {
                drawTrail(g, sx, sy, x, y, new Color(65, 25, 95), 28, a);
                drawOrb(g, x, y, radius * 0.45, new Color(85, 35, 145), p, a);
                drawLightning(g, x - 45, y, x + 45, y, new Color(185, 90, 255), a);
            }
            case "fear" -> {
                drawRing(g, sx, sy, radius * p, new Color(35, 20, 45), 28, a);
                drawEyes(g, sx, sy, radius * 0.65, new Color(210, 80, 255), a);
            }
            case "soul_burn" -> {
                drawFlames(g, x, y, radius * 0.75, new Color(145, 45, 230), p, a, 12);
                drawSparks(g, x, y, new Color(255, 75, 150), 12, p, a);
            }
            case "demon_summon" -> {
                drawMagicCircle(g, x, y, radius, new Color(115, 35, 160), p, a);
                drawSmoke(g, x, y, radius * 0.8, new Color(25, 15, 30), p, a, 13);
                drawHorns(g, x, y - 40, new Color(180, 75, 230), a);
            }
            case "dark_pact" -> {
                drawAura(g, sx, sy, radius * 0.75, new Color(95, 25, 130), p, a);
                drawDrops(g, sx, sy, radius * 0.55, new Color(210, 30, 65), p, a);
                drawLink(g, sx - 50, sy - 20, sx + 50, sy + 20, new Color(160, 45, 180), a);
            }
            case "soul_prison" -> {
                drawCage(g, x, y, radius * 0.65, new Color(120, 60, 190), a);
                drawChains(g, x, y, radius * 0.65, new Color(70, 45, 105), p, a);
            }
            case "apocalypse" -> {
                drawMagicCircle(g, sx, sy, radius, new Color(95, 25, 130), p, a);
                for (int i = 0; i < 7; i++) {
                    int ox = particleOffset(i, 0, (int) radius);
                    drawMeteorShard(g, sx + ox, sy - 220 + (int) (250 * p), new Color(95, 30, 130), a);
                }
                drawImpactBurst(g, sx, sy, new Color(55, 20, 75), p, a);
            }
            case "fire_bolt" -> {
                drawTrail(g, sx, sy, x, y, new Color(255, 95, 35), 20, a);
                drawFireBurst(g, x, y, radius * 0.55, p, a);
            }
            case "ice_shard" -> {
                drawIceSpearSequence(g, sx, sy, x, y, radius, p, a);
            }
            case "lightning_strike" -> {
                drawSkyLightningStrike(g, x, y, radius, p, a);
            }
            case "flame_burst" -> {
                drawFireSwordSlash(g, sx, sy, x, y, radius, p, a);
            }
            case "frost_nova" -> {
                drawIceWave(g, sx, sy, radius, p, a);
                for (int i = 0; i < 10; i++) {
                    drawIceCrystal(g, sx + particleOffset(i, 0, (int) radius),
                            sy + particleOffset(i, 1, (int) radius), 28, new Color(190, 250, 255), a);
                }
            }
            case "thunder_chain" -> {
                int lastX = sx;
                int lastY = sy;
                for (int i = 0; i < 4; i++) {
                    int nextX = x + particleOffset(i, 0, 110);
                    int nextY = y + particleOffset(i, 1, 85);
                    drawLightning(g, lastX, lastY, nextX, nextY, new Color(255, 240, 80), a);
                    lastX = nextX;
                    lastY = nextY;
                }
            }
            case "meteor" -> {
                drawTargetCracks(g, x, y, radius * 0.65, new Color(255, 95, 45), p, a);
                drawGiantMeteor(g, x - 180 + (int) (180 * p), y - 310 + (int) (310 * p),
                        new Color(255, 95, 45), p, a);
                drawRocks(g, x, y, radius, p, a);
                drawFireBurst(g, x, y, radius, p, a);
            }
            case "blizzard" -> {
                drawClouds(g, x, y - 120, radius * 0.9, new Color(130, 205, 235), p, a, 8);
                drawSnow(g, x, y, radius, p, a, 22);
                drawRing(g, x, y, radius * 0.8, new Color(170, 240, 255), 10, a);
            }
            case "elemental_storm" -> {
                drawOrbit(g, sx, sy, radius * 0.55, new Color(255, 95, 40), p, a, 4);
                drawOrbit(g, sx, sy, radius * 0.7, new Color(150, 235, 255), p + 0.35, a, 4);
                drawOrbit(g, sx, sy, radius * 0.85, new Color(255, 240, 85), p + 0.7, a, 4);
                drawShockwave(g, sx, sy, radius, new Color(200, 220, 255), p, a);
            }
            case "cataclysm" -> {
                drawMagicCircle(g, sx, sy, radius, new Color(255, 120, 45), p, a);
                drawLightning(g, sx - 120, sy - 220, sx + 60, sy, new Color(255, 245, 80), a);
                drawFireBurst(g, sx - 50, sy, radius * 0.75, p, a);
                drawRing(g, sx + 50, sy, radius * p, new Color(150, 235, 255), 18, a);
                drawImpactBurst(g, sx, sy, new Color(255, 255, 220), p, a);
            }
            default -> drawImpactBurst(g, x, y, color, p, a);
        }
    }

    private int screenX(int centerX, double cameraX, double value) {
        return (int) Math.round(centerX + value + cameraX);
    }

    private int screenY(int centerY, double cameraY, double value) {
        return (int) Math.round(centerY + value + cameraY);
    }

    private void drawTrail(Graphics2D g, int sx, int sy, int x, int y, Color c, double width, double a) {
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 130 * a));
        g.drawLine(sx, sy, x, y);
        g.setStroke(new BasicStroke((float) Math.max(3.0, width * 0.32), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 120 * a));
        g.drawLine(sx, sy, x, y);
    }

    private void drawAfterimages(Graphics2D g, int sx, int sy, int x, int y, Color c, double a, int count) {
        for (int i = 1; i <= count; i++) {
            double t = i / (double) (count + 1);
            drawHumanoid(g, (int) Math.round(sx + (x - sx) * t),
                    (int) Math.round(sy + (y - sy) * t), c, a * (0.9 - t * 0.55));
        }
    }

    private void drawHumanoid(Graphics2D g, int x, int y, Color c, double a) {
        g.setColor(withAlpha(c, 170 * a));
        g.fillOval(x - 10, y - 44, 20, 20);
        g.fillRoundRect(x - 13, y - 24, 26, 46, 12, 12);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x - 12, y - 8, x - 32, y + 16);
        g.drawLine(x + 12, y - 8, x + 32, y + 16);
    }

    private void drawSlash(Graphics2D g, int x, int y, double angleDegrees, double length, Color c, double a) {
        int arcSize = (int) Math.round(length);
        int start = (int) Math.round(angleDegrees - 55);
        g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 120 * a));
        g.draw(new Arc2D.Double(x - arcSize / 2.0, y - arcSize / 2.0,
                arcSize, arcSize, start, 110, Arc2D.OPEN));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 180 * a));
        g.draw(new Arc2D.Double(x - arcSize / 2.0, y - arcSize / 2.0,
                arcSize, arcSize, start + 8, 94, Arc2D.OPEN));
    }

    private void drawImpactBurst(Graphics2D g, int x, int y, Color c, double p, double a) {
        drawFlash(g, x, y, radius * (0.22 + p * 0.55), c, a);
        drawParticles(g, x, y, c, 12, radius * 0.8, p, a);
    }

    private void drawFlash(Graphics2D g, int x, int y, double size, Color c, double a) {
        int r = (int) Math.round(size);
        g.setColor(withAlpha(c, 90 * a));
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setColor(withAlpha(Color.WHITE, 110 * a));
        g.fillOval(x - r / 3, y - r / 3, r * 2 / 3, r * 2 / 3);
    }

    private void drawRing(Graphics2D g, int x, int y, double ringRadius, Color c, double width, double a) {
        int r = Math.max(1, (int) Math.round(ringRadius));
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 150 * a));
        g.drawOval(x - r, y - r, r * 2, r * 2);
    }

    private void drawShockwave(Graphics2D g, int x, int y, double ringRadius, Color c, double p, double a) {
        drawRing(g, x, y, ringRadius * (0.25 + p * 0.9), c, 12, a);
    }

    private void drawParticles(Graphics2D g, int x, int y, Color c, int count, double spread, double p, double a) {
        g.setColor(withAlpha(c, 185 * a));
        for (int i = 0; i < count; i++) {
            int ox = particleOffset(i, 0, (int) (spread * p));
            int oy = particleOffset(i, 1, (int) (spread * p));
            int size = 3 + (variant + i) % 6;
            g.fillOval(x + ox - size / 2, y + oy - size / 2, size, size);
        }
    }

    private void drawBubbles(Graphics2D g, int x, int y, double spread, Color c, double p, double a) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 150 * a));
        for (int i = 0; i < 12; i++) {
            int size = 10 + (variant + i * 7) % 22;
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) (spread * 0.65)) - (int) (50 * p);
            g.drawOval(x + ox - size / 2, y + oy - size / 2, size, size);
        }
    }

    private void drawSmoke(Graphics2D g, int x, int y, double spread, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            int size = 34 + (variant + i * 11) % 58;
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) spread) - (int) (20 * p);
            g.setColor(withAlpha(c, 80 * a));
            g.fillOval(x + ox - size / 2, y + oy - size / 2, size, size);
        }
    }

    private void drawClouds(Graphics2D g, int x, int y, double spread, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            int size = 48 + (variant + i * 13) % 58;
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) (spread * 0.45));
            g.setColor(withAlpha(c, 65 * a));
            g.fillOval(x + ox - size / 2, y + oy - size / 2, size, size);
        }
    }

    private void drawOrbit(Graphics2D g, int x, int y, double orbitRadius, Color c, double p, double a, int count) {
        g.setColor(withAlpha(c, 170 * a));
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * (i / (double) count + p);
            int px = (int) Math.round(x + Math.cos(angle) * orbitRadius);
            int py = (int) Math.round(y + Math.sin(angle) * orbitRadius * 0.65);
            g.fillOval(px - 6, py - 6, 12, 12);
        }
    }

    private void drawOrbitingBlades(Graphics2D g, int x, int y, double orbitRadius, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * (i / (double) count + p);
            int px = (int) Math.round(x + Math.cos(angle) * orbitRadius);
            int py = (int) Math.round(y + Math.sin(angle) * orbitRadius * 0.72);
            drawSlash(g, px, py, Math.toDegrees(angle), 70, c, a * 0.75);
        }
    }

    private void drawDust(Graphics2D g, int x, int y, double spread, double p, double a) {
        drawParticles(g, x, y, new Color(145, 118, 85), 12, spread * 0.65, p, a);
        drawClouds(g, x, y, spread * 0.5, new Color(105, 85, 65), p, a, 5);
    }

    private void drawShieldShape(Graphics2D g, int x, int y, double size, Color c, double a) {
        Path2D shield = new Path2D.Double();
        shield.moveTo(x, y - size);
        shield.curveTo(x + size * 0.7, y - size * 0.65, x + size * 0.7, y + size * 0.2, x, y + size);
        shield.curveTo(x - size * 0.7, y + size * 0.2, x - size * 0.7, y - size * 0.65, x, y - size);
        g.setColor(withAlpha(c, 70 * a));
        g.fill(shield);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 170 * a));
        g.draw(shield);
    }

    private void drawCracks(Graphics2D g, int x, int y, double spread, Color c, double p, double a) {
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 180 * a));
        for (int i = 0; i < 9; i++) {
            double angle = Math.PI * 2.0 * i / 9.0 + variant % 10;
            int ex = (int) Math.round(x + Math.cos(angle) * spread * p);
            int ey = (int) Math.round(y + Math.sin(angle) * spread * 0.62 * p);
            g.drawLine(x, y, ex, ey);
            g.drawLine(ex, ey, ex + particleOffset(i, 0, 28), ey + particleOffset(i, 1, 28));
        }
    }

    private void drawRocks(Graphics2D g, int x, int y, double spread, double p, double a) {
        g.setColor(withAlpha(new Color(105, 78, 58), 180 * a));
        for (int i = 0; i < 10; i++) {
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) spread / 2) - (int) (45 * Math.sin(p * Math.PI));
            int size = 8 + (variant + i) % 12;
            g.fillRect(x + ox, y + oy, size, size);
        }
    }

    private void drawAura(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        drawRing(g, x, y, size * 0.65 + Math.sin(p * Math.PI) * 14, c, 18, a);
        drawClouds(g, x, y, size * 0.55, c, p, a, 8);
    }

    private void drawDrops(Graphics2D g, int x, int y, double spread, Color c, double p, double a) {
        g.setColor(withAlpha(c, 180 * a));
        for (int i = 0; i < 10; i++) {
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) spread) + (int) (25 * p);
            g.fillOval(x + ox - 4, y + oy - 8, 8, 16);
        }
    }

    private void drawHolyCircle(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        drawRing(g, x, y, size * 0.7, c, 8, a);
        drawRing(g, x, y, size * (0.3 + p * 0.45), Color.WHITE, 5, a);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            drawCross(g, (int) Math.round(x + Math.cos(angle) * size * 0.52),
                    (int) Math.round(y + Math.sin(angle) * size * 0.52), 14, c, a);
        }
    }

    private void drawHolyPillar(Graphics2D g, int x, int y, double size, Color c,
            double p, double a) {
        int width = (int) Math.round(size * (0.28 + 0.08 * Math.sin(p * Math.PI)));
        int height = (int) Math.round(size * 1.9);
        Path2D pillar = new Path2D.Double();
        pillar.moveTo(x - width, y);
        pillar.curveTo(x - width * 0.65, y - height * 0.55,
                x - width * 0.45, y - height, x, y - height);
        pillar.curveTo(x + width * 0.45, y - height,
                x + width * 0.65, y - height * 0.55, x + width, y);
        pillar.closePath();
        g.setColor(withAlpha(c, 76 * a));
        g.fill(pillar);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 150 * a));
        g.drawLine(x, y - height, x, y);
        drawRing(g, x, y, size * (0.22 + p * 0.38), c, 6, a);
    }

    private void drawRising(Graphics2D g, int x, int y, double spread, Color c, double p, double a, int count) {
        g.setColor(withAlpha(c, 170 * a));
        for (int i = 0; i < count; i++) {
            int ox = particleOffset(i, 0, (int) spread);
            int oy = particleOffset(i, 1, (int) spread / 2) - (int) (90 * p);
            int size = 5 + (variant + i) % 8;
            g.fillOval(x + ox, y + oy, size, size);
        }
    }

    private void drawJudgmentSymbol(Graphics2D g, int x, int y, double size, Color c, double a) {
        drawRing(g, x, y, size * 0.55, c, 8, a);
        drawCross(g, x, y, (int) (size * 0.45), c, a);
    }

    private void drawCross(Graphics2D g, int x, int y, int size, Color c, double a) {
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 180 * a));
        g.drawLine(x, y - size, x, y + size);
        g.drawLine(x - size, y, x + size, y);
    }

    private void drawArrow(Graphics2D g, int sx, int sy, int x, int y, Color c, double a, double scale) {
        drawTrail(g, sx, sy, x, y, c, 8 * scale, a);
        double angle = Math.atan2(y - sy, x - sx);
        int length = (int) Math.round(42 * scale);
        Path2D head = new Path2D.Double();
        head.moveTo(x, y);
        head.lineTo(x - Math.cos(angle - 0.55) * length, y - Math.sin(angle - 0.55) * length);
        head.lineTo(x - Math.cos(angle) * length * 0.55, y - Math.sin(angle) * length * 0.55);
        head.lineTo(x - Math.cos(angle + 0.55) * length, y - Math.sin(angle + 0.55) * length);
        head.closePath();
        g.setColor(withAlpha(c, 210 * a));
        g.fill(head);
    }

    private void drawTargetZone(Graphics2D g, int x, int y, double size, Color c, double a) {
        drawRing(g, x, y, size * 0.65, c, 5, a);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 150 * a));
        g.drawLine((int) (x - size * 0.45), y, (int) (x + size * 0.45), y);
        g.drawLine(x, (int) (y - size * 0.45), x, (int) (y + size * 0.45));
    }

    private void drawLink(Graphics2D g, int sx, int sy, int x, int y, Color c, double a) {
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 170 * a));
        g.drawLine(sx, sy, x, y);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 120 * a));
        g.drawLine(sx, sy, x, y);
    }

    private void drawSkullMark(Graphics2D g, int x, int y, Color c, double a) {
        g.setColor(withAlpha(c, 155 * a));
        g.fillOval(x - 24, y - 24, 48, 48);
        g.setColor(withAlpha(Color.BLACK, 150 * a));
        g.fillOval(x - 13, y - 8, 10, 10);
        g.fillOval(x + 4, y - 8, 10, 10);
        g.fillRect(x - 8, y + 12, 16, 13);
    }

    private void drawMagicCircle(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        drawRing(g, x, y, size * 0.75, c, 8, a);
        drawRing(g, x, y, size * 0.45, c, 5, a);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * (i / 6.0 + p * 0.25);
            drawDiamond(g, (int) Math.round(x + Math.cos(angle) * size * 0.58),
                    (int) Math.round(y + Math.sin(angle) * size * 0.58), 18, c, a);
        }
    }

    private void drawDiamond(Graphics2D g, int x, int y, int size, Color c, double a) {
        Path2D path = new Path2D.Double();
        path.moveTo(x, y - size);
        path.lineTo(x + size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - size, y);
        path.closePath();
        g.setColor(withAlpha(c, 170 * a));
        g.fill(path);
    }

    private void drawFireBurst(Graphics2D g, int x, int y, double size, double p, double a) {
        drawFlames(g, x, y, size, new Color(255, 95, 35), p, a, 14);
        drawSparks(g, x, y, new Color(255, 220, 80), 14, p, a);
    }

    private void drawFlames(Graphics2D g, int x, int y, double size, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            int ox = particleOffset(i, 0, (int) size);
            int oy = particleOffset(i, 1, (int) (size * 0.55));
            Path2D flame = new Path2D.Double();
            flame.moveTo(x + ox, y + oy - 42);
            flame.curveTo(x + ox + 25, y + oy - 15, x + ox + 10, y + oy + 35, x + ox, y + oy + 42);
            flame.curveTo(x + ox - 22, y + oy + 20, x + ox - 12, y + oy - 20, x + ox, y + oy - 42);
            g.setColor(withAlpha(c, 95 * a));
            g.fill(flame);
        }
    }

    private void drawSparks(Graphics2D g, int x, int y, Color c, int count, double p, double a) {
        drawParticles(g, x, y, c, count, radius * 0.55, p, a);
    }

    private void drawIceCrystal(Graphics2D g, int x, int y, double size, Color c, double a) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 180 * a));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * i / 3.0;
            g.drawLine(x, y, (int) (x + Math.cos(angle) * size),
                    (int) (y + Math.sin(angle) * size));
        }
    }

    private void drawLightning(Graphics2D g, int sx, int sy, int x, int y, Color c, double a) {
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 190 * a));
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(sx, sy);
        for (int i = 1; i <= 4; i++) {
            double t = i / 5.0;
            bolt.lineTo(sx + (x - sx) * t + particleOffset(i, 0, 28),
                    sy + (y - sy) * t + particleOffset(i, 1, 28));
        }
        bolt.lineTo(x, y);
        g.draw(bolt);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 190 * a));
        g.draw(bolt);
    }

    private void drawSnow(Graphics2D g, int x, int y, double spread, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            drawIceCrystal(g, x + particleOffset(i, 0, (int) spread),
                    y + particleOffset(i, 1, (int) spread), 10 + i % 8,
                    new Color(210, 250, 255), a * 0.75);
        }
    }

    private void drawMeteorShard(Graphics2D g, int x, int y, Color c, double a) {
        drawTrail(g, x - 85, y - 115, x, y, c, 24, a);
        g.setColor(withAlpha(c, 200 * a));
        g.fillOval(x - 26, y - 26, 52, 52);
        g.setColor(withAlpha(Color.WHITE, 130 * a));
        g.fillOval(x - 11, y - 11, 22, 22);
    }

    private void drawOrb(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        drawFlash(g, x, y, size, c, a);
        drawRing(g, x, y, size * 0.9, c, 7, a);
        drawOrbit(g, x, y, size * 0.8, Color.WHITE, p, a, 4);
    }

    private void drawEyes(Graphics2D g, int x, int y, double spread, Color c, double a) {
        g.setColor(withAlpha(c, 170 * a));
        for (int i = 0; i < 5; i++) {
            int px = x + particleOffset(i, 0, (int) spread);
            int py = y + particleOffset(i, 1, (int) spread / 2);
            g.fillOval(px - 16, py - 7, 32, 14);
            g.setColor(withAlpha(Color.BLACK, 140 * a));
            g.fillOval(px - 4, py - 4, 8, 8);
            g.setColor(withAlpha(c, 170 * a));
        }
    }

    private void drawHorns(Graphics2D g, int x, int y, Color c, double a) {
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 180 * a));
        g.drawArc(x - 70, y - 20, 80, 70, 35, 160);
        g.drawArc(x - 10, y - 20, 80, 70, -15, 160);
    }

    private void drawCage(Graphics2D g, int x, int y, double size, Color c, double a) {
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 170 * a));
        for (int i = -2; i <= 2; i++) {
            g.drawLine((int) (x + i * size / 3.0), (int) (y - size),
                    (int) (x + i * size / 3.0), (int) (y + size));
        }
        drawRing(g, x, y, size, c, 6, a);
    }

    private void drawChains(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 180 * a));
        g.drawLine((int) (x - size), (int) (y - size * 0.55), (int) (x + size), (int) (y + size * 0.55));
        g.drawLine((int) (x + size), (int) (y - size * 0.55), (int) (x - size), (int) (y + size * 0.55));
    }

    private void drawAngerRunes(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * i / 6.0;
            drawSlash(g, (int) (x + Math.cos(angle) * size),
                    (int) (y + Math.sin(angle) * size * 0.7), Math.toDegrees(angle), 38, c, a);
        }
    }

    private void drawToxicSplash(Graphics2D g, int x, int y, double size, double p, double a) {
        g.setColor(withAlpha(new Color(70, 230, 55), 135 * a));
        for (int i = 0; i < 9; i++) {
            double angle = Math.PI * 2.0 * i / 9.0 + 0.3;
            double length = size * (0.35 + p * 0.55) * (0.75 + (i % 3) * 0.15);
            Path2D splash = new Path2D.Double();
            splash.moveTo(x, y);
            splash.lineTo(x + Math.cos(angle - 0.18) * length * 0.45,
                    y + Math.sin(angle - 0.18) * length * 0.45);
            splash.lineTo(x + Math.cos(angle) * length,
                    y + Math.sin(angle) * length);
            splash.lineTo(x + Math.cos(angle + 0.18) * length * 0.45,
                    y + Math.sin(angle + 0.18) * length * 0.45);
            splash.closePath();
            g.fill(splash);
        }
    }

    private void drawShockwaveSpikes(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        g.setColor(withAlpha(c, 120 * a));
        for (int i = 0; i < 14; i++) {
            double angle = Math.PI * 2.0 * i / 14.0;
            double inner = size * (0.22 + p * 0.38);
            double outer = size * (0.42 + p * 0.6);
            Path2D spike = new Path2D.Double();
            spike.moveTo(x + Math.cos(angle - 0.08) * inner, y + Math.sin(angle - 0.08) * inner);
            spike.lineTo(x + Math.cos(angle) * outer, y + Math.sin(angle) * outer);
            spike.lineTo(x + Math.cos(angle + 0.08) * inner, y + Math.sin(angle + 0.08) * inner);
            spike.closePath();
            g.fill(spike);
        }
    }

    private void drawIceSpear(Graphics2D g, int sx, int sy, int x, int y, double size, Color c, double a) {
        double angle = Math.atan2(y - sy, x - sx);
        double backX = x - Math.cos(angle) * size;
        double backY = y - Math.sin(angle) * size;
        double sideX = Math.cos(angle + Math.PI / 2.0) * size * 0.18;
        double sideY = Math.sin(angle + Math.PI / 2.0) * size * 0.18;
        Path2D spear = new Path2D.Double();
        spear.moveTo(x + Math.cos(angle) * size * 0.25, y + Math.sin(angle) * size * 0.25);
        spear.lineTo(backX + sideX, backY + sideY);
        spear.lineTo(backX - Math.cos(angle) * size * 0.25, backY - Math.sin(angle) * size * 0.25);
        spear.lineTo(backX - sideX, backY - sideY);
        spear.closePath();
        g.setColor(withAlpha(c, 190 * a));
        g.fill(spear);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 180 * a));
        g.draw(spear);
    }

    private void drawIceWave(Graphics2D g, int x, int y, double size, double p, double a) {
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0 * i / 16.0;
            double distance = size * (0.25 + p * 0.75);
            int px = (int) Math.round(x + Math.cos(angle) * distance);
            int py = (int) Math.round(y + Math.sin(angle) * distance * 0.72);
            drawIceCrystal(g, px, py, 18 + i % 4 * 5, new Color(170, 245, 255), a);
        }
    }

    private void drawTargetCracks(Graphics2D g, int x, int y, double size, Color c, double p, double a) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 155 * a));
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2.0 * i / 10.0;
            int ex = (int) Math.round(x + Math.cos(angle) * size * (0.25 + p * 0.7));
            int ey = (int) Math.round(y + Math.sin(angle) * size * 0.55 * (0.25 + p * 0.7));
            g.drawLine(x, y, ex, ey);
            g.drawLine(ex, ey, ex + particleOffset(i, 0, 28), ey + particleOffset(i, 1, 20));
        }
    }

    private void drawGiantMeteor(Graphics2D g, int x, int y, Color c, double p, double a) {
        drawTrail(g, x - 130, y - 170, x, y, c, 46, a);
        drawSmoke(g, x - 70, y - 85, 70, new Color(75, 55, 45), p, a, 6);
        g.setColor(withAlpha(new Color(95, 55, 35), 230 * a));
        g.fillOval(x - 46, y - 42, 92, 84);
        g.setColor(withAlpha(c, 190 * a));
        g.fillOval(x - 32, y - 30, 64, 58);
        g.setColor(withAlpha(new Color(255, 230, 120), 180 * a));
        g.fillOval(x - 14, y - 12, 28, 24);
    }

    private double directionAngle(int sx, int sy, int x, int y) {
        return Math.atan2(y - sy, x - sx);
    }

    private void drawWeaponSwing(Graphics2D g, int x, int y, double angle,
            String weaponType, Color c, double p, double a) {
        double windup = Math.sin(Math.min(1.0, p) * Math.PI);
        int handle = weaponType.equals("axe") ? 72 : 60;
        int blade = weaponType.equals("axe") ? 30 : 44;
        double swingAngle = angle - 0.85 + windup * 1.7;
        int hx = (int) Math.round(x + Math.cos(swingAngle) * 18);
        int hy = (int) Math.round(y + Math.sin(swingAngle) * 18);
        int tipX = (int) Math.round(hx + Math.cos(swingAngle) * handle);
        int tipY = (int) Math.round(hy + Math.sin(swingAngle) * handle);

        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(new Color(98, 65, 40), 210 * a));
        g.drawLine(hx, hy, tipX, tipY);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 235 * a));
        if (weaponType.equals("axe")) {
            int sideX = (int) Math.round(Math.cos(swingAngle + Math.PI / 2.0) * blade);
            int sideY = (int) Math.round(Math.sin(swingAngle + Math.PI / 2.0) * blade);
            g.drawLine(tipX - sideX, tipY - sideY, tipX + sideX, tipY + sideY);
        } else {
            g.drawLine(hx, hy, tipX, tipY);
        }
    }

    private void drawFireSwordSlash(Graphics2D g, int sx, int sy, int x, int y,
            double size, double p, double a) {
        double angle = directionAngle(sx, sy, x, y);
        int travelX = (int) Math.round(sx + Math.cos(angle) * size * (0.2 + p * 0.75));
        int travelY = (int) Math.round(sy + Math.sin(angle) * size * (0.2 + p * 0.75));
        drawWeaponSwing(g, sx, sy, angle, "sword", new Color(235, 235, 220), p, a);
        drawSlash(g, travelX, travelY, Math.toDegrees(angle) + 30, size * 0.95,
                new Color(255, 92, 28), a);
        drawSlash(g, travelX + particleOffset(1, 0, 18), travelY + particleOffset(1, 1, 18),
                Math.toDegrees(angle) + 5, size * 0.62, new Color(255, 190, 48), a * 0.8);
        drawFlames(g, travelX, travelY, size * 0.42, new Color(255, 80, 28), p, a, 9);
        drawSparks(g, travelX, travelY, new Color(255, 215, 80), 16, p, a);
        if (p > 0.62) {
            drawFlash(g, x, y, 50 + 28 * (p - 0.62), new Color(255, 88, 35), a);
        }
    }

    private void drawShadowTeleport(Graphics2D g, int sx, int sy, int x, int y,
            double p, double a) {
        drawSmoke(g, sx, sy, 64, new Color(18, 14, 24), p, a, 7);
        drawHumanoid(g, sx, sy, new Color(24, 18, 34), a * (1.0 - p * 0.65));
        for (int i = 1; i <= 5; i++) {
            double t = Math.min(1.0, p + i * 0.05);
            int px = (int) Math.round(sx + (x - sx) * t);
            int py = (int) Math.round(sy + (y - sy) * t);
            drawHumanoid(g, px, py, new Color(42, 22, 65), a * (0.6 - i * 0.08));
        }
        drawSmoke(g, x, y, 70, new Color(32, 20, 45), p, a, 8);
        drawSlash(g, x, y, -35 + p * 85, radius * 0.9, new Color(115, 55, 190), a);
        drawParticles(g, x, y, new Color(34, 20, 48), 16, radius * 0.55, p, a);
        if (p > 0.55) {
            drawFlash(g, x, y, 42, new Color(45, 20, 65), a * 0.8);
        }
    }

    private void drawSkyLightningStrike(Graphics2D g, int x, int y, double size,
            double p, double a) {
        int topY = y - (int) Math.round(size * 1.55);
        drawBranchedLightning(g, x + particleOffset(0, 0, 36), topY, x, y,
                new Color(105, 220, 255), a);
        drawRing(g, x, y + 8, size * (0.2 + p * 0.38), new Color(105, 210, 255), 6, a);
        drawFlash(g, x, y, 44, new Color(185, 240, 255), a);
        drawParticles(g, x, y, new Color(135, 225, 255), 18, size * 0.5, p, a);
    }

    private void drawBranchedLightning(Graphics2D g, int sx, int sy, int x, int y,
            Color c, double a) {
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(sx, sy);
        int lastX = sx;
        int lastY = sy;
        for (int i = 1; i <= 6; i++) {
            double t = i / 7.0;
            int px = (int) Math.round(sx + (x - sx) * t + particleOffset(i, 0, 34));
            int py = (int) Math.round(sy + (y - sy) * t + particleOffset(i, 1, 18));
            bolt.lineTo(px, py);
            if (i > 1 && i < 6) {
                int branchX = px + particleOffset(i, 2, 48);
                int branchY = py + particleOffset(i, 3, 36);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(withAlpha(c, 155 * a));
                g.drawLine(px, py, branchX, branchY);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(withAlpha(Color.WHITE, 170 * a));
                g.drawLine(px, py, branchX, branchY);
            }
            lastX = px;
            lastY = py;
        }
        bolt.lineTo(x, y);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 210 * a));
        g.draw(bolt);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(Color.WHITE, 235 * a));
        g.draw(bolt);
    }

    private void drawWhirlwind(Graphics2D g, int x, int y, double size, Color c,
            double p, double a) {
        drawRing(g, x, y, size, new Color(180, 190, 185), 4, a * 0.7);
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2.0 * (p * 1.8 + i / 5.0);
            int px = (int) Math.round(x + Math.cos(angle) * size * 0.42);
            int py = (int) Math.round(y + Math.sin(angle) * size * 0.42);
            drawSlash(g, px, py, Math.toDegrees(angle) + 80,
                    size * (0.55 + i * 0.08), c, a * (0.88 - i * 0.08));
        }
        drawWeaponSwing(g, x, y, p * Math.PI * 4.0, "sword", Color.WHITE, p, a);
        drawDust(g, x, y + 30, size * 0.75, p, a);
    }

    private void drawGroundSlam(Graphics2D g, int x, int y, double size, double p, double a) {
        drawWeaponSwing(g, x, y - 20, Math.PI / 2.0, "axe", new Color(210, 205, 185), p, a);
        drawRing(g, x, y, size * p, new Color(205, 155, 90), 10, a);
        drawCracks(g, x, y, size, new Color(95, 68, 45), Math.min(1.0, p * 1.15), a);
        drawRocks(g, x, y, size * 0.65, p, a);
        drawDust(g, x, y + 24, size, p, a);
        if (p < 0.28) {
            drawFlash(g, x, y, 70, new Color(225, 180, 105), a);
        }
    }

    private void drawPhysicalImpactLines(Graphics2D g, int x, int y, double size,
            double p, double a) {
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 7; i++) {
            double angle = -0.95 + i * 0.32;
            int inner = (int) Math.round(size * (0.1 + p * 0.15));
            int outer = (int) Math.round(size * (0.45 + p * 0.55));
            int x1 = (int) Math.round(x + Math.cos(angle) * inner);
            int y1 = (int) Math.round(y + Math.sin(angle) * inner);
            int x2 = (int) Math.round(x + Math.cos(angle) * outer);
            int y2 = (int) Math.round(y + Math.sin(angle) * outer);
            g.setColor(withAlpha(i % 2 == 0 ? new Color(255, 230, 135) : Color.WHITE,
                    175 * a));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawGuardianBarrier(Graphics2D g, int x, int y, double size,
            Color c, double p, double a) {
        double pulse = Math.sin(p * Math.PI * 6.0) * 6.0;
        drawRing(g, x, y, size * 0.72 + pulse, c, 9, a);
        drawRing(g, x, y, size * 0.52 - pulse * 0.4, Color.WHITE, 3, a * 0.75);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 90 * a));
        g.drawArc((int) (x - size * 0.72), (int) (y - size * 0.88),
                (int) (size * 1.44), (int) (size * 1.5), 20, 140);
        g.drawArc((int) (x - size * 0.72), (int) (y - size * 0.42),
                (int) (size * 1.44), (int) (size * 1.05), 200, 140);
        drawOrbit(g, x, y, size * 0.65, c, p, a, 8);
        if (p < 0.22 || p > 0.78) {
            drawRing(g, x + particleOffset(2, 0, 24), y + particleOffset(2, 1, 24),
                    size * 0.26 + p * 18, Color.WHITE, 4, a * 0.8);
        }
    }

    private void drawDashStep(Graphics2D g, int sx, int sy, int x, int y, Color c,
            double p, double a) {
        drawAfterimages(g, sx, sy, x, y, c, a, 6);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, 160 * a));
        for (int i = 0; i < 6; i++) {
            double t = i / 6.0;
            int px = (int) Math.round(sx + (x - sx) * t);
            int py = (int) Math.round(sy + (y - sy) * t);
            int backX = (int) Math.round(px - (x - sx) * 0.16);
            int backY = (int) Math.round(py - (y - sy) * 0.16 + particleOffset(i, 1, 12));
            g.drawLine(backX, backY, px + particleOffset(i, 0, 8), py);
        }
        drawDust(g, sx, sy + 28, 58, p, a * 0.85);
        drawParticles(g, x, y, c, 10, 45, p, a);
    }

    private void drawHealingAura(Graphics2D g, int x, int y, double size, double p, double a) {
        drawRing(g, x, y + 18, size * 0.42 + Math.sin(p * Math.PI) * 10,
                new Color(105, 255, 150), 7, a);
        drawRising(g, x, y + 20, size * 0.65, new Color(120, 255, 155), p, a, 18);
        drawRising(g, x, y + 10, size * 0.45, Color.WHITE, p + 0.2, a * 0.8, 8);
        for (int i = 0; i < 6; i++) {
            int px = x + particleOffset(i, 0, (int) (size * 0.45));
            int py = y + particleOffset(i, 1, (int) (size * 0.35)) - (int) (70 * p);
            drawCross(g, px, py, 8 + i % 3 * 2, new Color(180, 255, 190), a * 0.85);
        }
        if (p < 0.3) {
            drawFlash(g, x, y, 46 + 26 * p, new Color(145, 255, 165), a);
        }
    }

    private void drawIceSpearSequence(Graphics2D g, int sx, int sy, int x, int y,
            double size, double p, double a) {
        double eased = Math.min(1.0, p * 1.25);
        int px = (int) Math.round(sx + (x - sx) * eased);
        int py = (int) Math.round(sy + (y - sy) * eased);
        drawIceSpear(g, sx, sy, px, py, Math.max(54, size * 0.42),
                new Color(170, 240, 255), a);
        drawTrail(g, sx, sy, px, py, new Color(105, 210, 255), 10, a * 0.75);
        for (int i = 0; i < 9; i++) {
            double t = i / 9.0;
            int fx = (int) Math.round(sx + (px - sx) * t + particleOffset(i, 0, 16));
            int fy = (int) Math.round(sy + (py - sy) * t + particleOffset(i, 1, 16));
            drawIceCrystal(g, fx, fy, 7 + i % 5, new Color(210, 250, 255), a * 0.8);
        }
        if (p > 0.7) {
            drawFlash(g, x, y, 42, new Color(205, 250, 255), a);
            for (int i = 0; i < 8; i++) {
                drawIceCrystal(g, x + particleOffset(i, 0, 42),
                        y + particleOffset(i, 1, 34), 14 + i % 6,
                        new Color(180, 245, 255), a);
            }
        }
    }

    private int particleOffset(int index, int salt, int spread) {
        int value = variant + index * 1103515245 + salt * 12345;
        value ^= value >>> 16;
        int range = Math.max(1, spread * 2 + 1);
        return Math.floorMod(value, range) - spread;
    }

    private Color withAlpha(Color base, double alpha) {
        int safeAlpha = Math.max(0, Math.min(255, (int) Math.round(alpha)));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), safeAlpha);
    }
}
