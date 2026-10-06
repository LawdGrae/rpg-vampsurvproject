import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/** Continuous world-space skill effects; static skill icons stay in the HUD. */
public class AbilityVisualEffect {
    public enum Layer { GROUND, FRONT }
    private static final Color FIRE = new Color(255, 103, 38);
    private static final Color EMBER = new Color(255, 219, 130);
    private static final Color ICE = new Color(130, 225, 255);
    private static final Color HOLY = new Color(255, 225, 140);
    private static final Color SHADOW = new Color(170, 92, 232);
    private static final Color POISON = new Color(114, 231, 105);
    private static final Color STEEL = new Color(195, 212, 228);
    private final AbilityDefinition definition;
    private double startX, startY, casterX, casterY;
    private double worldX, worldY;
    private final double radius, maxLife, actionDuration, aimAngle;
    private final double[] hits;
    private final Color color;
    private final int variant;
    private final Layer layer;
    private final GuardianVisualEffect guardian;
    private double life;
    private double sourceAngle, waveRadius = -1, blockAge = 1, impactX, impactY;
    private boolean mirrored, projectileDriven, impactFrozen;
    private Enemy attachedTarget;
    private final double[] trailX = new double[8], trailY = new double[8], trailBirth = new double[8];
    private double previousCasterX, previousCasterY, nextTrailTime;
    private int trailCount, trailIndex;

    public AbilityVisualEffect(AbilityDefinition definition, double startX,
            double startY, double worldX, double worldY, double radius,
            Color color, double maxLife) {
        this.definition = definition;
        this.startX = this.casterX = startX;
        this.startY = this.casterY = startY;
        previousCasterX = startX; previousCasterY = startY;
        this.worldX = worldX;
        this.worldY = worldY;
        this.radius = Math.max(40, Math.min(420, radius));
        this.color = color;
        this.maxLife = Double.isFinite(maxLife) ? Math.max(0.05, maxLife) : 0.6;
        this.life = this.maxLife;
        this.actionDuration = AbilityAnimationTiming.duration(definition);
        this.hits = AbilityAnimationTiming.hitProgress(definition);
        nextTrailTime = AbilityAnimationTiming.releaseProgress(definition) * actionDuration;
        this.aimAngle = Math.atan2(worldY - startY, worldX - startX);
        this.variant = definition.getId().hashCode();
        this.layer = switch (definition.getId()) {
            case "heal", "blessing", "holy_shield", "sanctuary",
                    "flame_burst", "frost_nova", "elemental_storm", "dark_taunt",
                    "smoke_veil", "venom_burst", "iron_guard", "earthbreaker",
                    "guardians_roar" -> Layer.GROUND;
            default -> Layer.FRONT;
        };
        this.guardian = GuardianVisualEffect.supports(definition.getId())
                ? new GuardianVisualEffect(definition, startX, startY, worldX, worldY,
                        this.radius, this.maxLife) : null;
    }

    public void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0) return;
        double previousAge = age();
        if (Double.isFinite(deltaTime) && deltaTime > 0 && !projectileDriven) life = Math.max(0, life - deltaTime);
        if (definition.getId().equals("shadow_strike") || definition.getId().equals("shadow_step")) {
            while (nextTrailTime <= Math.min(age(), hits[0] * actionDuration) + 1e-10) {
                double fraction = deltaTime <= 0 ? 1 : Math.max(0, Math.min(1, (nextTrailTime - previousAge) / deltaTime));
                trailX[trailIndex] = previousCasterX + (casterX - previousCasterX) * fraction;
                trailY[trailIndex] = previousCasterY + (casterY - previousCasterY) * fraction;
                trailBirth[trailIndex] = nextTrailTime;
                trailIndex = (trailIndex + 1) % trailX.length; trailCount = Math.min(trailX.length, trailCount + 1);
                nextTrailTime += 0.028;
            }
        }
        previousCasterX = casterX; previousCasterY = casterY;
        blockAge += Math.max(0, deltaTime);
        if (attachedTarget != null) {
            if (attachedTarget.isDead()) { if (definition.getId().equals("death_mark")) expire(); }
            else if (!impactFrozen) { worldX = attachedTarget.getWorldX(); worldY = attachedTarget.getWorldY(); }
        }
        if (guardian != null) guardian.update(deltaTime);
    }
    public boolean isExpired() { return life <= 0; }
    public Layer getLayer() { return projectileDriven ? Layer.FRONT : layer; }
    public void setCasterPosition(double x, double y) {
        if (Double.isFinite(x) && Double.isFinite(y)) { casterX = x; casterY = y; }
        if (guardian != null) guardian.setCasterPosition(x, y);
    }
    public void setCastOrigin(double x, double y) {
        if (projectileDriven) return;
        if (AbilityAnimationTiming.isReferenceSkill(definition.getId())) {
            if (Double.isFinite(x) && Double.isFinite(y)) { startX = x; startY = y; }
            return;
        }
        if (guardian != null) {
            guardian.setShieldPosition(x, y);
            return;
        }
        // Keep the equipped weapon's anticipation attached, then freeze projectile launch.
        if (definition.getEffectType() != AbilityEffectType.DASH
                && Double.isFinite(x) && Double.isFinite(y)
                && (progress() < AbilityAnimationTiming.releaseProgress(definition) || isMelee())) {
            startX = x; startY = y;
        }
    }
    /** Pins a physical ground slam or released force wave to its combat event. */
    public void freezeImpactOrigin(double x, double y) {
        if (!impactFrozen) { impactX = x; impactY = y; impactFrozen = true; }
        if (guardian != null) guardian.freezeImpactOrigin(x, y);
    }

    public void setSourcePose(double angle, boolean mirror) {
        if (!projectileDriven && Double.isFinite(angle)) { sourceAngle = angle; mirrored = mirror; }
    }
    public void attachTarget(Enemy target) { attachedTarget = target; }
    public void expire() { life = 0; }
    public void setWaveRadius(double radius) { waveRadius = Math.max(0, radius); }
    public void setProjectilePosition(double x, double y, double angle) {
        projectileDriven = true; worldX = x; worldY = y; sourceAngle = angle;
    }
    public void setProjectilePosition(double x, double y, double angle, double elapsed) {
        setProjectilePosition(x, y, angle);
        life = Math.max(0, maxLife - elapsed);
    }

    /** Brief contact sparks on the live shield barrier when it blocks damage. */
    public void flashBarrier(double x, double y) {
        if (definition.getId().equals("holy_shield") || definition.getId().equals("shield_fortress")) blockAge = 0;
        if (guardian != null) guardian.flashBarrier(x, y);
    }
    private double age() { return maxLife - life; }
    private double progress() { return Math.min(1, age() / actionDuration); }
    private boolean isMelee() {
        return switch (definition.getId()) {
            case "heavy_slash", "twin_fang", "silent_execution", "knights_wrath",
                    "poison_blade", "counter_strike", "shield_bash" -> true;
            default -> false;
        };
    }
    private boolean isPersistent() {
        return maxLife > actionDuration * 1.4 && switch (definition.getEffectType()) {
            case SHIELD, BUFF, HEAL, SLOW, SUMMON, POISON -> true;
            default -> false;
        };
    }

    public void draw(Graphics2D graphics, int centerX, int centerY, double cameraX, double cameraY) {
        if (isExpired()) return;
        if (AbilityAnimationTiming.isReferenceSkill(definition.getId())) {
            drawSourceFrames(graphics, centerX + cameraX, centerY + cameraY);
            return;
        }
        if (guardian != null) {
            guardian.draw(graphics, centerX, centerY, cameraX, cameraY);
            return;
        }
        double p = progress();
        double a = isPersistent() ? smooth(p / 0.14) * (1 - smooth((age() / maxLife - 0.78) / 0.22))
                : envelope(p, 0, 0.1, 0.72, 1);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centerX + cameraX, centerY + cameraY);
        double anticipation = envelope(p, 0, 0.06, Math.max(0.18, hits[0] - 0.2), hits[0]);
        double chargeX = definition.getId().equals("earth_shatter") ? worldX : startX;
        double chargeY = definition.getId().equals("earth_shatter") ? worldY + 15 : startY;
        Color chargeColor = switch (definition.getAbilityClass()) {
            case BLACK_KNIGHT -> EMBER;
            case ASSASSIN, WARLOCK -> SHADOW;
            case PRIEST -> HOLY;
            case ELEMENTALIST -> ICE;
            default -> color;
        };
        glow(g, chargeX, chargeY, 12 + 6 * smooth(p / 0.2), chargeColor, anticipation * 0.3);
        ring(g, chargeX, chargeY, 8 + 6 * smooth(p / 0.2), chargeColor, 1,
                anticipation * 0.4, 0.7);
        drawAbility(g, p, a);
        g.dispose();
    }

    /** Tracks attach to physical sources; a missing source is never replaced with a shape. */
    private void drawSourceFrames(Graphics2D graphics, double cameraX, double cameraY) {
        String id = definition.getId();
        if (definition.isPassive()) return; // The selected trait is drawn by GameLogic.
        double releaseAge = AbilityAnimationTiming.releaseProgress(definition) * actionDuration;
        double hitAge = hits[0] * actionDuration;
        double elapsed = age();
        if (projectileDriven) {
            drawTrack(graphics, "travel", worldX + cameraX, worldY + cameraY, radius,
                    sourceAngle, elapsed, 1 - smooth((elapsed / maxLife - 0.85) / 0.15), false);
            return;
        }
        double buildup = smooth(elapsed / 0.12) * (1 - smooth((elapsed - releaseAge + 0.08) / 0.08));
        drawTrack(graphics, "buildup", startX + cameraX, startY + cameraY,
                20 + 28 * smooth(elapsed / Math.max(0.01, releaseAge)), 0, elapsed, buildup, mirrored);
        if (elapsed < releaseAge) return;
        if (id.equals("death_mark") && elapsed < hitAge) return;
        for (int i = 0; i < trailCount; i++) {
            double age = elapsed - trailBirth[i];
            if (age < 0 || age >= 0.20) continue;
            drawTrack(graphics, "travel", trailX[i] + cameraX, trailY[i] + cameraY,
                    62, 0, age, (1 - age / 0.20) * 0.35, mirrored);
        }
        if (id.equals("flame_burst") || id.equals("ice_shard")) {
            // A forming projectile is attached to the staff; released shots own travel.
            drawTrack(graphics, "action", startX + cameraX, startY + cameraY, 38,
                    sourceAngle, elapsed - releaseAge, 1 - smooth((elapsed / actionDuration - 0.65) / 0.15), false);
            return;
        }
        double fade = 1 - smooth((elapsed / maxLife - 0.78) / 0.22);
        double grow = smooth((elapsed - releaseAge) / Math.max(0.05, hitAge - releaseAge));
        double x = worldX, y = worldY, width = Math.min(220, radius), angle = 0;
        boolean mirror = false;
        if (id.equals("heavy_slash") || id.equals("earth_shatter") || id.equals("shield_bash")
                || id.equals("shadow_strike") || id.equals("twin_fang") || id.equals("knights_wrath")) {
            x = startX; y = startY; width = Math.min(125, radius); angle = sourceAngle;
        } else if (id.equals("shadow_step")) {
            x = casterX; y = casterY; width = 76; mirror = mirrored;
        } else if (id.equals("heal")) {
            x = startX + (casterX - startX) * grow;
            y = startY + (casterY - startY) * grow; width = 70;
            if (elapsed < hitAge) {
                drawTrack(graphics, "travel", x + cameraX, y + cameraY, 36, 0, elapsed - releaseAge, fade, false);
                return;
            }
            x = casterX; y = casterY;
        } else if (id.equals("holy_shield") || id.equals("shield_fortress")) {
            x = startX + (casterX - startX) * grow;
            y = startY + (casterY - startY) * grow;
            width = 35 + 60 * grow;
        } else if (id.equals("iron_charge")) {
            x = startX; y = startY; width = 90; mirror = mirrored;
        } else if (id.equals("earthbreaker") || id.equals("guardians_roar") || id.equals("elemental_storm")) {
            x = impactFrozen ? impactX : casterX; y = impactFrozen ? impactY : casterY;
            width = waveRadius >= 0 ? Math.max(1, waveRadius * 2) : 40 + grow * 30;
            if (id.equals("guardians_roar") && elapsed >= hitAge) width = Math.max(1, radius * 2 * Math.min(1, (elapsed - hitAge) / 0.55));
        } else if (id.equals("death_mark")) { width = 45; }
        else if (id.equals("holy_bolt") || id.equals("divine_light") || id.equals("lightning_strike")) {
            x = impactFrozen ? impactX : worldX; y = impactFrozen ? impactY : worldY;
            width = 25 + 80 * grow;
        }
        drawTrack(graphics, "action", x + cameraX, y + cameraY, width, angle,
                elapsed - releaseAge, fade * smooth((elapsed - releaseAge) / 0.07), mirror);
        if (blockAge < 0.24) drawTrack(graphics, "barrier_block", x + cameraX, y + cameraY,
                46, 0, blockAge, 1 - blockAge / 0.24, false);
    }

    private void drawTrack(Graphics2D graphics, String track, double x, double y, double width,
            double angle, double elapsed, double alpha, boolean mirror) {
        SkillEffectAtlas.SkillAnimation animation = SkillEffectAtlas.getAnimation(definition.getId(), track);
        if (animation == null || alpha <= 0) return;
        boolean held = track.equals("action") && (definition.getId().equals("death_mark")
                || definition.getId().equals("holy_shield") || definition.getId().equals("shield_fortress"));
        if (held && !animation.isLooping()) {
            elapsed = Math.min(elapsed, animation.getStartDelaySeconds() + animation.getDurationSeconds() - 1e-9);
            alpha *= 0.94 + 0.06 * Math.sin(age() * 3.5);
        }
        animation.drawAt(graphics, x, y, width, animation.isDirectional() ? angle : 0,
                elapsed, alpha, mirror);
    }

    private void drawAbility(Graphics2D g, double p, double a) {
        double sx = startX, sy = startY, x = worldX, y = worldY, cx = casterX, cy = casterY;
        double hit = hits[0];
        switch (definition.getId()) {
            case "heavy_slash", "counter_strike" -> {
                slash(g, sx, sy, aimAngle, radius * 0.7, FIRE, p, hit);
                impact(g, sx + Math.cos(aimAngle) * radius * 0.55,
                        sy + Math.sin(aimAngle) * radius * 0.4, radius * 0.42, EMBER, p, hit);
            }
            case "twin_fang", "poison_blade" -> {
                Color c = definition.getId().equals("poison_blade") ? POISON : SHADOW;
                slash(g, sx, sy, aimAngle - 0.3, radius * 0.57, c, p, hit);
                slash(g, sx, sy, aimAngle + 0.55, radius * 0.63,
                        c == POISON ? c : new Color(237, 96, 153), p, hits.length > 1 ? hits[1] : hit + 0.12);
                impact(g, cx, cy, radius * 0.58, c, p, hit);
            }
            case "shadow_strike", "shadow_step", "backstep", "shield_charge" -> {
                Color c = definition.getId().equals("shield_charge") ? STEEL : SHADOW;
                dash(g, sx, sy, x, y, c, p, a, hit);
                if (c == SHADOW) slash(g, x, y, aimAngle, radius * 0.4, c, p, hit);
                else barrier(g, x, y, 45, c, p, a * 0.6);
            }
            case "silent_execution", "shadow_assassin", "phantom_clone" -> {
                boolean atTarget = definition.getId().equals("silent_execution");
                double ox = atTarget ? x : cx, oy = atTarget ? y : cy;
                mist(g, ox, oy, radius * 0.32, new Color(37, 22, 52), p, a, 7);
                for (int i = 0; i < hits.length; i++) slash(g, ox, oy,
                        aimAngle + (i % 2 == 0 ? -0.65 : 0.7) + (atTarget ? 0 : i * 1.3),
                        radius * (0.44 + i * 0.04), i == hits.length - 1 ? new Color(240, 208, 255) : SHADOW, p, hits[i]);
                if (!atTarget) eye(g, ox, oy - 35, 22, p, a);
            }
            case "smoke_veil", "fear" -> {
                mist(g, cx, cy, radius * 0.65, new Color(31, 21, 43), p, a, 12);
                ring(g, cx, cy + 17, radius * (0.25 + easeOut(p) * 0.6), SHADOW, 2, a * 0.5, 0.58);
                if (definition.getId().equals("fear")) eye(g, cx, cy - 20, 30, p, a);
            }
            case "venom_burst" -> {
                burst(g, x, y, radius, POISON, p, a, hit);
                bubbles(g, x, y, radius * 0.6, p, a);
            }
            case "death_mark", "curse", "hunters_mark" -> {
                Color c = definition.getId().equals("hunters_mark") ? new Color(248, 111, 91) : SHADOW;
                sigil(g, x, y + 13, radius * 0.3, c, p, a, false);
                eye(g, x, y - 36 - Math.sin(p * Math.PI) * 6, 19, p, a);
                ribbon(g, sx, sy, x, y, c, 2.5, a * 0.35, 9 * Math.sin(p * Math.PI));
            }
            case "shield_bash" -> {
                double thrust = easeOut(phase(p, hit - 0.24, hit));
                barrier(g, sx + Math.cos(aimAngle) * 25 * thrust,
                        sy + Math.sin(aimAngle) * 25 * thrust, 40, STEEL, p,
                        envelope(p, 0, 0.1, hit, 0.9));
                impact(g, x, y, radius * 0.4, EMBER, p, hit);
                dust(g, x, y + 20, radius * 0.5, phase(p, hit, 1), a * smooth((p - hit) / 0.08));
            }
            case "iron_guard", "holy_shield", "dark_fortress", "blood_armor" -> {
                Color c = switch (definition.getId()) {
                    case "holy_shield" -> HOLY;
                    case "blood_armor", "dark_fortress" -> new Color(208, 83, 117);
                    default -> STEEL;
                };
                barrier(g, cx, cy - 10, Math.min(90, radius * 0.6), c, p, a * (0.25 + smooth(p / hit) * 0.75));
                sigil(g, cx, cy + 24, Math.min(125, radius * 0.62), c, p, a * 0.65, false);
                rising(g, cx, cy + 20, radius * 0.42, c, p, a * 0.65, 10);
            }
            case "dark_taunt" -> {
                ring(g, cx, cy + 20, radius * (0.2 + easeOut(p) * 0.65), new Color(217, 87, 98), 3, a, 0.58);
                mist(g, cx, cy, radius * 0.5, new Color(69, 30, 45), p, a * 0.6, 7);
                sparks(g, cx, cy, new Color(232, 120, 119), 14, radius * 0.7, p, a);
            }
            case "earth_shatter" -> groundSlam(g, x, y + 15, radius, p, a, hit);
            case "knights_wrath" -> {
                for (int i = 0; i < hits.length; i++) slash(g, sx, sy, aimAngle + i * 1.25,
                        radius * (0.4 + i * 0.055), FIRE, p, hits[i]);
                // The held swing leads into the same target field used by scheduled damage.
                for (double strike : hits) impact(g, x, y, radius * 0.84, FIRE, p, strike);
                dust(g, x, y + 22, radius * 0.65, phase(p, hit, 1), a * 0.55);
                sparks(g, x, y, EMBER, 20, radius * 0.8, phase(p, hit, 1), a);
            }
            case "holy_bolt", "fire_bolt", "dark_bolt", "ice_shard", "shadow_orb",
                    "quick_shot", "power_arrow", "poison_arrow", "explosive_arrow", "phantom_arrow" -> projectile(g, p, a, hit);
            case "heal", "divine_light", "resurrection", "blessing", "prayer", "sanctuary" -> {
                Color c = definition.getId().equals("heal") ? new Color(140, 243, 173) : HOLY;
                double size = Math.min(radius, definition.getId().equals("heal") ? 100 : 210);
                sigil(g, cx, cy + 24, size * 0.68, c, p, a, true);
                rising(g, cx, cy + 20, size * 0.58, c, p, a, 18);
                glow(g, cx, cy - 12, 45 + 8 * Math.sin(age() * 3), c, a * 0.16);
                if (definition.getId().equals("divine_light") || definition.getId().equals("resurrection"))
                    pillar(g, cx, cy + 18, size * 0.48, c, p, a * 0.75);
                if (definition.getId().equals("blessing")) {
                    for (int i = 0; i < 4; i++) star(g, cx + (unit(i, 3) - 0.5) * 60,
                            cy + 18 - phase(p, i * 0.1, 1) * 90, 5, c, a * 0.65);
                }
            }
            case "purify", "divine_judgment" -> {
                pillar(g, x, y, radius * 0.7, HOLY, p, a);
                sigil(g, x, y + 14, radius * 0.65, HOLY, p, a * 0.65, true);
                impact(g, x, y, radius * 0.55, HOLY, p, hit);
                rising(g, x, y, radius * 0.5, HOLY, p, a, 16);
            }
            case "multi_shot", "rain_of_arrows", "arrow_storm" -> arrowVolley(g, p, a, hit);
            case "life_drain" -> {
                ribbon(g, x, y, cx, cy, new Color(233, 90, 137), 5, a, 14 * Math.sin(p * Math.PI * 2));
                for (int i = 0; i < 8; i++) {
                    double t = phase(p, i * 0.06, hit + i * 0.04);
                    glow(g, x + (cx - x) * t, y + (cy - y) * t - Math.sin(t * Math.PI) * 12,
                            5, new Color(233, 90, 137), a * 0.6);
                }
                rising(g, cx, cy, radius * 0.25, new Color(233, 90, 137), p, a, 8);
            }
            case "soul_burn", "dark_pact", "demon_summon", "soul_prison", "apocalypse" -> shadowField(g, p, a, hit);
            case "lightning_strike", "thunder_chain" -> lightningStrike(g, x, y, radius, p, a, hit);
            case "flame_burst" -> {
                ribbon(g, sx, sy, x, y, FIRE, 3, envelope(p, 0.14, 0.25, hit, hit + 0.12) * 0.4,
                        Math.sin(p * Math.PI) * 10);
                burst(g, x, y + 16, radius, FIRE, p, a, hit);
            }
            case "frost_nova" -> frost(g, cx, cy + 18, radius, p, a, hit);
            case "meteor" -> meteor(g, x, y, p, a, hit);
            case "blizzard" -> {
                mist(g, x, y - 25, radius * 0.65, ICE, p, a * 0.28, 8);
                sigil(g, x, y + 20, radius * 0.72, ICE, p, a * 0.6, false);
                for (int i = 0; i < 22; i++) {
                    double t = phase(p, unit(i, 1) * 0.25, 0.7 + unit(i, 2) * 0.3);
                    star(g, x + (unit(i, 3) - 0.5) * radius * 1.4 + Math.sin(t * 4 + i) * 14,
                            y - 100 + (unit(i, 4) - 0.5) * radius * 0.6 + t * 130,
                            3 + unit(i, 5) * 3, ICE, a * Math.sin(t * Math.PI));
                }
            }
            case "elemental_storm", "cataclysm" -> {
                ribbon(g, sx, sy, x, y, SHADOW, 3, envelope(p, 0.12, 0.25, hit, hit + 0.1) * 0.35,
                        Math.sin(p * Math.PI) * 14);
                elementalStorm(g, x, y, p, a);
            }
            default -> impact(g, x, y, radius * 0.6, color, p, hit);
        }
    }

    private void slash(Graphics2D g, double x, double y, double angle, double reach, Color c, double p, double hit) {
        double from = Math.max(0.025, hit - 0.24), to = Math.min(1, hit + 0.26);
        double t = phase(p, from, to), alpha = envelope(p, from, Math.min(hit, from + 0.09), hit + 0.025, to);
        if (alpha < 0.002) return;
        double head = angle - 1.05 + easeOut(phase(p, from, hit + 0.065)) * 2.1;
        double tail = head - (0.18 + Math.sin(t * Math.PI) * 0.9);
        Path2D ribbon = new Path2D.Double(), edge = new Path2D.Double();
        for (int i = 0; i <= 20; i++) {
            double f = i / 20.0, at = tail + (head - tail) * f;
            double r = reach * (0.82 + 0.18 * Math.sin(f * Math.PI * 0.5));
            double px = x + Math.cos(at) * r, py = y + Math.sin(at) * r * 0.7;
            if (i == 0) { ribbon.moveTo(px, py); edge.moveTo(px, py); }
            else { ribbon.lineTo(px, py); edge.lineTo(px, py); }
        }
        for (int i = 20; i >= 0; i--) {
            double f = i / 20.0, at = tail + (head - tail) * f;
            double r = reach * (0.82 + 0.18 * Math.sin(f * Math.PI * 0.5) - Math.sin(f * Math.PI) * 0.17);
            ribbon.lineTo(x + Math.cos(at) * r, y + Math.sin(at) * r * 0.7);
        }
        ribbon.closePath();
        g.setColor(withAlpha(c, 110 * alpha)); g.fill(ribbon);
        stroke(g, edge, c, 8, alpha * 0.16);
        stroke(g, edge, c, 3.8, alpha * 0.65);
        stroke(g, edge, new Color(255, 244, 226), 1.3, alpha * 0.8);
        glow(g, x + Math.cos(head) * reach, y + Math.sin(head) * reach * 0.7, 14, c, alpha * 0.45);
    }

    private void projectile(Graphics2D g, double p, double a, double hit) {
        String id = definition.getId();
        Color c = switch (id) {
            case "holy_bolt", "power_arrow" -> HOLY;
            case "ice_shard", "phantom_arrow" -> ICE;
            case "fire_bolt", "explosive_arrow" -> FIRE;
            case "dark_bolt", "shadow_orb" -> SHADOW;
            case "poison_arrow" -> POISON;
            default -> new Color(206, 236, 165);
        };
        double release = AbilityAnimationTiming.releaseProgress(definition);
        double t = smooth(phase(p, release, hit));
        double bend = Math.min(30, Math.hypot(worldX - startX, worldY - startY) * 0.08);
        double px = startX + (worldX - startX) * t, py = startY + (worldY - startY) * t - Math.sin(t * Math.PI) * bend;
        double flight = envelope(p, 0.13, 0.22, hit, hit + 0.065);
        glow(g, startX, startY, 16, c, envelope(p, 0, 0.06, 0.16, 0.28) * 0.55);
        for (int i = 7; i >= 1; i--) {
            double old = smooth(phase(p - i * 0.012, release, hit));
            double next = smooth(phase(p - (i - 1) * 0.012, release, hit));
            stroke(g, new Line2D.Double(startX + (worldX - startX) * old, startY + (worldY - startY) * old - Math.sin(old * Math.PI) * bend,
                    startX + (worldX - startX) * next, startY + (worldY - startY) * next - Math.sin(next * Math.PI) * bend),
                    c, 2 + (8 - i) * 0.9, flight * (1 - i / 8.0) * 0.55);
        }
        double angle = Math.atan2(worldY - startY - Math.cos(t * Math.PI) * Math.PI * bend, worldX - startX);
        if (id.equals("ice_shard") || id.contains("arrow") || id.equals("quick_shot"))
            shard(g, px, py, angle, id.equals("ice_shard") ? 24 : 14, c, flight);
        else {
            glow(g, px, py, id.equals("shadow_orb") ? 22 : 15, c, flight * 0.85);
            glow(g, px, py, 5, Color.WHITE, flight * 0.8);
            if (id.equals("holy_bolt")) star(g, px, py, 7, HOLY, flight);
        }
        impact(g, worldX, worldY, Math.min(92, radius * (id.equals("shadow_orb") ? 0.42 : 0.28)), c, p, hit);
        if (id.equals("explosive_arrow") || id.equals("fire_bolt")) flames(g, worldX, worldY,
                Math.min(70, radius * 0.32), phase(p, hit, 1), a * smooth((p - hit) / 0.06), 8);
    }

    private void burst(Graphics2D g, double x, double y, double size, Color c, double p, double a, double hit) {
        double t = phase(p, hit - 0.015, 1), release = smooth((p - hit + 0.015) / 0.05);
        sigil(g, x, y, size * 0.3, c, p, envelope(p, 0, 0.1, hit - 0.1, hit + 0.12) * 0.75, false);
        ring(g, x, y, size * (0.18 + easeOut(t) * 0.68), c, 5 * (1 - t) + 1, a * release * 0.75, 0.58);
        glow(g, x, y - 12, size * (0.25 + t * 0.22), c, a * release * (1 - t) * 0.2);
        sparks(g, x, y, c == FIRE ? EMBER : c, 22, size * 0.86, t, a * release);
        if (c == FIRE) flames(g, x, y, size * 0.64, t, a * release, 15);
        else mist(g, x, y, size * 0.5, c, t, a * release * 0.35, 8);
    }

    private void groundSlam(Graphics2D g, double x, double y, double size, double p, double a, double hit) {
        double t = phase(p, hit - 0.01, 1), release = smooth((p - hit + 0.01) / 0.04);
        glow(g, x, y, size * 0.24, EMBER, envelope(p, hit - 0.07, hit, hit + 0.04, hit + 0.2) * 0.5);
        ring(g, x, y, size * (0.08 + easeOut(t) * 0.88), EMBER, 3, a * release * 0.5, 0.55);
        for (int i = 0; i < 9; i++) {
            double angle = i * Math.PI * 2 / 9 + unit(i, 4) * 0.25;
            double length = size * (0.65 + unit(i, 2) * 0.3) * easeOut(t);
            Path2D crack = new Path2D.Double(); crack.moveTo(x, y);
            crack.lineTo(x + Math.cos(angle - 0.07) * length * 0.53, y + Math.sin(angle - 0.07) * length * 0.31);
            crack.lineTo(x + Math.cos(angle + 0.05) * length, y + Math.sin(angle + 0.05) * length * 0.55);
            stroke(g, crack, new Color(64, 43, 37), 4.5, a * release * 0.8);
            stroke(g, crack, new Color(229, 154, 88), 1, a * release * 0.5);
            double distance = size * (0.1 + t * (0.36 + unit(i, 5) * 0.3));
            shard(g, x + Math.cos(angle) * distance, y + Math.sin(angle) * distance * 0.55 - Math.sin(t * Math.PI) * (25 + unit(i, 6) * 35),
                    angle + t * 3, 4 + unit(i, 7) * 6, new Color(147, 108, 76), a * release);
        }
        dust(g, x, y, size * 0.65, t, a * release);
    }

    private void lightningStrike(Graphics2D g, double x, double y, double size, double p, double a, double hit) {
        double release = envelope(p, hit - 0.02, hit + 0.018, hit + 0.12, Math.min(1, hit + 0.3));
        sigil(g, x, y + 14, size * 0.35, ICE, p, envelope(p, 0, 0.1, hit - 0.1, hit + 0.15) * 0.5, false);
        boolean chain = definition.getId().equals("thunder_chain");
        lightning(g, chain ? startX : x - size * 0.12, chain ? startY : y - Math.min(300, size * 1.5), x, y, p, release);
        impact(g, x, y, size * 0.45, ICE, p, hit);
        if (chain) for (int i = 0; i < 3; i++) lightning(g, x, y, x + (unit(i, 1) - 0.5) * size,
                y + (unit(i, 2) - 0.5) * size * 0.7, p + i * 0.2, release * 0.65);
    }
    private void lightning(Graphics2D g, double sx, double sy, double x, double y, double p, double a) {
        if (a < 0.002) return;
        Path2D bolt = new Path2D.Double(); bolt.moveTo(sx, sy);
        double length = Math.max(1, Math.hypot(x - sx, y - sy)), ox = -(y - sy) / length, oy = (x - sx) / length;
        for (int i = 1; i < 8; i++) {
            double t = i / 8.0, jitter = (unit(i, 8) - 0.5) * 34 + Math.sin(p * 18 + i * 2.1) * 5;
            bolt.lineTo(sx + (x - sx) * t + ox * jitter, sy + (y - sy) * t + oy * jitter);
        }
        bolt.lineTo(x, y);
        stroke(g, bolt, ICE, 13, a * 0.1); stroke(g, bolt, ICE, 5, a * 0.65);
        stroke(g, bolt, new Color(237, 252, 255), 1.7, a * 0.95); glow(g, x, y, 28, ICE, a * 0.45);
    }
    private void elementalStorm(Graphics2D g, double x, double y, double p, double a) {
        sigil(g, x, y + 22, radius * 0.64, SHADOW, p, a * 0.6, false);
        Color[] elements = { FIRE, ICE, HOLY };
        for (int i = 0; i < 3; i++) {
            double onset = i * 0.09, activation = smooth((p - onset) / 0.15);
            double angle = p * Math.PI * 2.4 + i * Math.PI * 2 / 3;
            double reach = radius * (0.22 + 0.2 * smooth(p / 0.65));
            double px = x + Math.cos(angle) * reach, py = y + Math.sin(angle) * reach * 0.6;
            for (int j = 1; j <= 5; j++) glow(g, x + Math.cos(angle - j * 0.13) * reach,
                    y + Math.sin(angle - j * 0.13) * reach * 0.6, 5 + (6 - j) * 1.2,
                    elements[i], a * activation * (1 - j / 6.0) * 0.3);
            glow(g, px, py, 22, elements[i], a * activation * 0.5);
            if (i == 0) flames(g, px, py, 20, phase(p, 0, 1), a * activation, 4);
            else if (i == 1) shard(g, px, py, angle + Math.PI / 2, 15, ICE, a * activation);
            else lightning(g, px - 15, py - 34, px, py, p, a * activation * 0.65);
            impact(g, px, py, radius * 0.24, elements[i], p, hits[Math.min(i, hits.length - 1)]);
        }
    }
    private void frost(Graphics2D g, double x, double y, double size, double p, double a, double hit) {
        double t = phase(p, hit - 0.01, 1), release = smooth((p - hit + 0.01) / 0.05);
        ring(g, x, y, size * (0.15 + easeOut(t) * 0.72), ICE, 3, a * release, 0.58);
        for (int i = 0; i < 14; i++) {
            double angle = i * Math.PI * 2 / 14, distance = size * (0.17 + easeOut(t) * 0.61);
            shard(g, x + Math.cos(angle) * distance, y + Math.sin(angle) * distance * 0.58,
                    angle - Math.PI / 2, 10 + unit(i, 2) * 10, ICE, a * release * 0.8);
        }
        mist(g, x, y, size * 0.5, ICE, t, a * release * 0.2, 7);
        sparks(g, x, y, ICE, 16, size * 0.8, t, a * release);
    }
    private void meteor(Graphics2D g, double x, double y, double p, double a, double hit) {
        sigil(g, x, y + 12, radius * 0.46, FIRE, p, envelope(p, 0, 0.1, hit - 0.1, hit + 0.1) * 0.6, false);
        double t = smooth(phase(p, 0.13, hit)), mx = x - (1 - t) * 175, my = y - (1 - t) * 280;
        double flight = envelope(p, 0, 0.08, hit - 0.015, hit + 0.05);
        ribbon(g, mx - 54, my - 87, mx, my, FIRE, 24, flight * 0.6, 0);
        glow(g, mx, my, 35, FIRE, flight * 0.8);
        shard(g, mx, my, 0.9 + p, 23, new Color(174, 88, 46), flight);
        glow(g, mx, my, 12, EMBER, flight * 0.8);
        burst(g, x, y, radius, FIRE, p, a, hit);
        dust(g, x, y + 20, radius * 0.62, phase(p, hit, 1), a * smooth((p - hit) / 0.05) * 0.7);
    }
    private void shadowField(Graphics2D g, double p, double a, double hit) {
        boolean centered = definition.getId().equals("dark_pact") || definition.getId().equals("apocalypse");
        double x = centered ? casterX : worldX, y = centered ? casterY : worldY;
        sigil(g, x, y + 20, radius * 0.6, SHADOW, p, a * 0.7, false);
        mist(g, x, y, radius * 0.45, new Color(52, 26, 70), p, a * 0.8, 9);
        rising(g, x, y, radius * 0.5, SHADOW, p, a, 14);
        if (definition.getId().equals("soul_prison")) for (int i = 0; i < 5; i++) {
            double angle = i * Math.PI / 2.5 + age() * 0.4;
            double px = x + Math.cos(angle) * radius * 0.4, py = y + Math.sin(angle) * radius * 0.22;
            ribbon(g, px, py - 75 * smooth(p / hit), px, py, SHADOW, 3, a * 0.7, Math.sin(age() * 5 + i) * 7);
        }
        else if (definition.getId().equals("apocalypse")) for (int i = 0; i < 5; i++) {
            double px = x + (unit(i, 1) - 0.5) * radius, py = y + (unit(i, 2) - 0.5) * radius * 0.6;
            double onset = hit + (i - 2) * 0.035, t = phase(p, 0.08 + i * 0.04, onset);
            glow(g, px - (1 - t) * 40, py - (1 - t) * 190, 18, SHADOW,
                    envelope(p, i * 0.04, i * 0.04 + 0.08, onset, onset + 0.08));
            impact(g, px, py, 60, SHADOW, p, onset);
        }
        else if (definition.getId().equals("demon_summon")) eye(g, x, y - 42, 31, p, a);
        else if (definition.getId().equals("soul_burn")) for (int i = 0; i < 7; i++)
            glow(g, x + (unit(i, 1) - 0.5) * radius * 0.6, y - p * 70 + Math.sin(age() * 8 + i) * 10, 14, SHADOW, a * 0.4);
    }
    private void arrowVolley(Graphics2D g, double p, double a, double hit) {
        boolean fan = definition.getId().equals("multi_shot");
        int count = fan ? 5 : definition.getId().equals("arrow_storm") ? 28 : 12;
        if (!fan) sigil(g, worldX, worldY + 12, radius * 0.67, new Color(201, 234, 162), p, a * 0.4, false);
        for (int i = 0; i < count; i++) {
            double delay = fan ? Math.abs(i - 2) * 0.025 : unit(i, 0) * 0.14;
            double t = smooth(phase(p, 0.12 + delay, hit + delay));
            double tx = fan ? startX + Math.cos(aimAngle + (i - 2) * 0.22) * radius : worldX + (unit(i, 2) - 0.5) * radius * 1.3;
            double ty = fan ? startY + Math.sin(aimAngle + (i - 2) * 0.22) * radius : worldY + (unit(i, 3) - 0.5) * radius * 0.7;
            double bx = fan ? startX : tx - 38, by = fan ? startY : ty - 170;
            double px = bx + (tx - bx) * t, py = by + (ty - by) * t;
            double alpha = envelope(p, delay, delay + 0.1, hit + delay, Math.min(1, hit + delay + 0.09));
            ribbon(g, px - (tx - bx) * 0.09, py - (ty - by) * 0.09, px, py, HOLY, 2, alpha * 0.7, 0);
            shard(g, px, py, Math.atan2(ty - by, tx - bx), 10, new Color(232, 247, 199), alpha);
        }
    }
    private void dash(Graphics2D g, double sx, double sy, double x, double y, Color c, double p, double a, double hit) {
        double t = easeOut(phase(p, 0.14, hit));
        for (int i = 0; i < 7; i++) {
            double old = easeOut(phase(p - i * 0.022, 0.14, hit));
            double px = sx + (x - sx) * old, py = sy + (y - sy) * old;
            double at = a * (1 - i / 8.0) * envelope(p, 0.1, 0.18, hit, hit + 0.16);
            ribbon(g, px - Math.cos(aimAngle) * 22, py - Math.sin(aimAngle) * 22, px, py,
                    c, 8 - i * 0.6, at * 0.55, i % 2 * 7);
        }
        glow(g, sx + (x - sx) * t, sy + (y - sy) * t, 23, c, envelope(p, 0.1, 0.18, hit, hit + 0.1) * 0.3);
        dust(g, sx, sy + 20, 45, p, a * 0.4);
    }
    private void impact(Graphics2D g, double x, double y, double size, Color c, double p, double onset) {
        double t = phase(p, onset - 0.008, 1), a = envelope(p, onset - 0.008, onset + 0.04, onset + 0.12, 1);
        if (a < 0.002) return;
        glow(g, x, y, Math.max(12, size * (0.34 + t * 0.25)), c, a * (1 - t) * 0.4);
        ring(g, x, y + 8, size * (0.13 + easeOut(t) * 0.66), c, 2.5 - t * 1.6, a * 0.5, 0.68);
        sparks(g, x, y, c, 12, size, t, a);
    }

    private void barrier(Graphics2D g, double x, double y, double size, Color c, double p, double a) {
        double r = size * (0.78 + 0.22 * smooth(p / hits[0]) + Math.sin(age() * 4) * 0.025);
        glow(g, x, y, r * 0.85, c, a * 0.07);
        Path2D path = new Path2D.Double(); path.moveTo(x, y - r * 0.75);
        path.curveTo(x + r * 0.7, y - r * 0.54, x + r * 0.76, y + r * 0.17, x, y + r * 0.85);
        path.curveTo(x - r * 0.76, y + r * 0.17, x - r * 0.7, y - r * 0.54, x, y - r * 0.75);
        stroke(g, path, c, 9, a * 0.1); stroke(g, path, c, 2.4, a * 0.7);
        stroke(g, new Arc2D.Double(x - r * 0.55, y - r * 0.7, r * 1.1, r * 1.3, 40 + age() * 80, 80, Arc2D.OPEN),
                Color.WHITE, 1, a * 0.6);
    }
    private void sigil(Graphics2D g, double x, double y, double size, Color c, double p, double a, boolean holy) {
        double r = size * (0.75 + smooth(p / 0.24) * 0.25);
        ring(g, x, y, r, c, 1.6, a * 0.65, 0.48); ring(g, x, y, r * 0.75, c, 1, a * 0.45, 0.48);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + age() * 0.6;
            double px = x + Math.cos(angle) * r * 0.87, py = y + Math.sin(angle) * r * 0.42;
            if (holy) star(g, px, py, 4.5, c, a * 0.75); else shard(g, px, py, angle, 4, c, a * 0.65);
        }
    }
    private void pillar(Graphics2D g, double x, double y, double size, Color c, double p, double a) {
        double strength = a * smooth(p / hits[0]), height = Math.min(310, size * 2.2), width = Math.max(8, size * 0.17);
        for (int i = -2; i <= 2; i++) {
            Path2D beam = new Path2D.Double(); double offset = i * width * 0.55;
            beam.moveTo(x + offset, y);
            beam.curveTo(x + offset + Math.sin(age() * 6 + i) * 7, y - height * 0.45,
                    x + offset * 0.7, y - height * 0.8, x + offset * 0.35, y - height);
            stroke(g, beam, c, i == 0 ? width * 1.7 : width * 0.6, strength * 0.1);
            stroke(g, beam, Color.WHITE, i == 0 ? 2 : 0.8, strength * (i == 0 ? 0.5 : 0.22));
        }
        glow(g, x, y - 20, size * 0.7, c, strength * 0.12);
    }

    /** Seed selects direction and velocity once, never from a changing integer spread. */
    private void sparks(Graphics2D g, double x, double y, Color c, int count, double spread, double p, double a) {
        for (int i = 0; i < count; i++) {
            double t = phase(p, unit(i, 6) * 0.06, 0.8 + unit(i, 7) * 0.2), angle = unit(i, 0) * Math.PI * 2;
            double distance = spread * (0.38 + unit(i, 1) * 0.62) * easeOut(t);
            double px = x + Math.cos(angle) * distance, py = y + Math.sin(angle) * distance * 0.7 + t * t * 13;
            double pa = a * Math.sin(t * Math.PI) * (0.6 + unit(i, 2) * 0.4), length = 2 + (1 - t) * (3 + unit(i, 3) * 5);
            stroke(g, new Line2D.Double(px, py, px - Math.cos(angle) * length, py - Math.sin(angle) * length * 0.7),
                    c, 0.9 + unit(i, 4) * 1.5, pa * 0.85);
            if (i % 4 == 0) glow(g, px, py, 5, c, pa * 0.25);
        }
    }
    private void rising(Graphics2D g, double x, double y, double spread, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            double delay = unit(i, 0) * 0.25;
            double t = isPersistent() ? (age() * 0.7 + unit(i, 5)) % 1 : phase(p, delay, 0.8 + delay * 0.7);
            double px = x + (unit(i, 1) - 0.5) * spread * 1.8 + Math.sin(t * 4 + i) * 6;
            double py = y + (unit(i, 2) - 0.5) * spread * 0.48 - t * (60 + unit(i, 3) * 55), pa = a * Math.sin(t * Math.PI);
            star(g, px, py, 1.5 + unit(i, 4) * 2.5, c, pa * 0.8);
            if (i % 3 == 0) glow(g, px, py, 6, c, pa * 0.23);
        }
    }
    private void mist(Graphics2D g, double x, double y, double spread, Color c, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            double angle = unit(i, 0) * Math.PI * 2, distance = spread * (0.1 + unit(i, 1) * 0.75) * (0.65 + p * 0.5);
            double px = x + Math.cos(angle) * distance + Math.sin(age() * 3 + i) * 5;
            double py = y + Math.sin(angle) * distance * 0.5 - p * (10 + unit(i, 2) * 20);
            glow(g, px, py, (15 + unit(i, 3) * 24) * (0.8 + p * 0.5), c, a * 0.12);
        }
    }
    private void dust(Graphics2D g, double x, double y, double spread, double p, double a) {
        mist(g, x, y, spread, new Color(153, 120, 86), p, a * 0.75, 6);
        sparks(g, x, y, new Color(189, 156, 110), 10, spread, p, a * 0.6);
    }
    private void bubbles(Graphics2D g, double x, double y, double spread, double p, double a) {
        for (int i = 0; i < 9; i++) {
            double t = isPersistent() ? (age() * 0.5 + unit(i, 0)) % 1 : phase(p, unit(i, 0) * 0.22, 1);
            double px = x + (unit(i, 1) - 0.5) * spread * 1.6 + Math.sin(age() * 4 + i) * 8;
            double py = y + (unit(i, 2) - 0.5) * spread * 0.5 - t * 55;
            ring(g, px, py, (3 + unit(i, 3) * 5) * (0.7 + t * 0.4), POISON, 1.2, a * Math.sin(t * Math.PI) * 0.7, 1);
        }
    }
    private void flames(Graphics2D g, double x, double y, double spread, double p, double a, int count) {
        for (int i = 0; i < count; i++) {
            double t = phase(p, unit(i, 0) * 0.14, 0.8 + unit(i, 1) * 0.2);
            double px = x + (unit(i, 2) - 0.5) * spread * 1.6 * (0.4 + easeOut(t) * 0.6);
            double py = y + (unit(i, 3) - 0.5) * spread * 0.5 - t * (15 + unit(i, 4) * 25);
            double height = (17 + unit(i, 5) * 26) * Math.sin(t * Math.PI), sway = Math.sin(age() * 9 + i) * 7;
            Path2D flame = new Path2D.Double(); flame.moveTo(px - height * 0.2, py);
            flame.curveTo(px - height * 0.33, py - height * 0.42, px + sway, py - height * 0.7, px + sway, py - height);
            flame.curveTo(px + height * 0.38, py - height * 0.55, px + height * 0.3, py, px - height * 0.2, py);
            g.setColor(withAlpha(FIRE, a * Math.sin(t * Math.PI) * 94)); g.fill(flame);
            stroke(g, new Line2D.Double(px, py - height * 0.13, px + sway * 0.4, py - height * 0.65), EMBER, 1.3, a * Math.sin(t * Math.PI) * 0.45);
        }
    }
    private void eye(Graphics2D g, double x, double y, double size, double p, double a) {
        double h = size * (0.25 + 0.1 * Math.sin(p * Math.PI));
        Path2D eye = new Path2D.Double(); eye.moveTo(x - size, y);
        eye.curveTo(x - size * 0.4, y - h, x + size * 0.4, y - h, x + size, y);
        eye.curveTo(x + size * 0.4, y + h, x - size * 0.4, y + h, x - size, y);
        stroke(g, eye, SHADOW, 2, a * 0.8); glow(g, x, y, 5, SHADOW, a * 0.65);
        stroke(g, new Line2D.Double(x, y - h * 0.65, x, y + h * 0.65), Color.WHITE, 1.5, a * 0.6);
    }
    private static void ribbon(Graphics2D g, double sx, double sy, double x, double y, Color c, double width, double a, double bend) {
        Path2D path = new Path2D.Double(); path.moveTo(sx, sy);
        double dx = x - sx, dy = y - sy, length = Math.max(1, Math.hypot(dx, dy)), ox = -dy / length * bend, oy = dx / length * bend;
        path.curveTo(sx + dx * 0.3 + ox, sy + dy * 0.3 + oy, sx + dx * 0.7 + ox, sy + dy * 0.7 + oy, x, y);
        stroke(g, path, c, width * 2.6, a * 0.1); stroke(g, path, c, width, a * 0.55);
        stroke(g, path, Color.WHITE, Math.max(0.7, width * 0.24), a * 0.5);
    }
    private static void shard(Graphics2D g, double x, double y, double angle, double size, Color c, double a) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        Path2D shard = new Path2D.Double(); shard.moveTo(x + dx * size, y + dy * size);
        shard.lineTo(x - dx * size * 0.55 - dy * size * 0.22, y - dy * size * 0.55 + dx * size * 0.22);
        shard.lineTo(x - dx * size * 0.8, y - dy * size * 0.8);
        shard.lineTo(x - dx * size * 0.55 + dy * size * 0.22, y - dy * size * 0.55 - dx * size * 0.22); shard.closePath();
        g.setColor(withAlpha(c, 145 * a)); g.fill(shard);
        stroke(g, new Line2D.Double(x + dx * size, y + dy * size, x - dx * size * 0.7, y - dy * size * 0.7), Color.WHITE, 0.9, a * 0.65);
    }
    private static void star(Graphics2D g, double x, double y, double size, Color c, double a) {
        stroke(g, new Line2D.Double(x - size, y, x + size, y), c, 1.1, a * 0.85);
        stroke(g, new Line2D.Double(x, y - size * 1.3, x, y + size * 1.3), c, 1.1, a * 0.85);
    }
    private static void ring(Graphics2D g, double x, double y, double r, Color c, double width, double a, double verticalScale) {
        if (r <= 0 || a < 0.002) return;
        Shape ring = new Ellipse2D.Double(x - r, y - r * verticalScale, r * 2, r * 2 * verticalScale);
        stroke(g, ring, c, width * 3, a * 0.08); stroke(g, ring, c, width, a * 0.68);
    }
    private static void glow(Graphics2D g, double x, double y, double r, Color c, double a) {
        if (a < 0.002 || r < 0.2) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r,
                new float[] {0, 0.32f, 1}, new Color[] {withAlpha(c, 180 * a), withAlpha(c, 85 * a), withAlpha(c, 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }
    private static void stroke(Graphics2D g, Shape shape, Color c, double width, double a) {
        if (a < 0.002) return;
        g.setStroke(new BasicStroke((float) Math.max(0.1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, a * 255)); g.draw(shape);
    }
    private double unit(int index, int salt) {
        int value = variant ^ (index + 1) * 0x45d9f3b ^ (salt + 1) * 0x119de1f3;
        value ^= value >>> 16; value *= 0x45d9f3b; value ^= value >>> 16;
        return (value & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }
    private static double phase(double p, double from, double to) { return Math.max(0, Math.min(1, (p - from) / Math.max(0.0001, to - from))); }
    private static double smooth(double p) { double t = Math.max(0, Math.min(1, p)); return t * t * (3 - 2 * t); }
    private static double easeOut(double p) { double t = Math.max(0, Math.min(1, p)); return 1 - Math.pow(1 - t, 3); }
    private static double envelope(double p, double start, double peak, double fade, double end) { return smooth(phase(p, start, peak)) * (1 - smooth(phase(p, fade, end))); }
    private static Color withAlpha(Color c, double a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, Math.round(a)))); }
}
