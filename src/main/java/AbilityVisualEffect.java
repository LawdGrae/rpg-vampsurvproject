import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

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
    private double blockX, blockY;
    private double launchX, launchY, previousSourceX, previousSourceY;
    private boolean launchFrozen;
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
        launchX = previousSourceX = startX;
        launchY = previousSourceY = startY;
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
        double releaseAge = AbilityAnimationTiming.releaseProgress(definition) * actionDuration;
        if (!projectileDriven && !launchFrozen && age() >= releaseAge) {
            double fraction = deltaTime <= 0 ? 1 : Math.max(0, Math.min(1, (releaseAge - previousAge) / deltaTime));
            launchX = previousSourceX + (startX - previousSourceX) * fraction;
            launchY = previousSourceY + (startY - previousSourceY) * fraction;
            launchFrozen = true;
        }
        previousSourceX = startX; previousSourceY = startY;
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
            if (guardian != null) guardian.setShieldPosition(x, y);
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
    public void setWaveRadius(double radius) {
        if (!Double.isFinite(radius)) return;
        waveRadius = Math.max(0, radius);
        if (guardian != null) guardian.setWaveRadius(waveRadius);
    }
    public void setProjectilePosition(double x, double y, double angle) {
        projectileDriven = true; worldX = x; worldY = y; sourceAngle = angle;
    }
    public void setProjectilePosition(double x, double y, double angle, double elapsed) {
        setProjectilePosition(x, y, angle);
        life = Math.max(0, maxLife - elapsed);
    }

    /** Brief contact sparks on the live shield barrier when it blocks damage. */
    public void flashBarrier(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        if (definition.getId().equals("holy_shield") || definition.getId().equals("shield_fortress")) {
            blockAge = 0; blockX = x; blockY = y;
        }
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
        if (isExpired() || definition.isPassive()) return;
        boolean reference = AbilityAnimationTiming.isReferenceSkill(definition.getId());
        boolean reviewed = projectileDriven
                ? SkillEffectAtlas.getAnimation(definition.getId(), "travel") != null
                : SkillEffectAtlas.getAnimation(definition.getId(),
                        progress() < AbilityAnimationTiming.releaseProgress(definition) ? "buildup" : "action") != null;
        if (reference && reviewed) {
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
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.translate(centerX + cameraX, centerY + cameraY);
        if (reference) {
            drawReferenceAbility(g, p);
            g.dispose();
            return;
        }
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

    /** Reviewed source tracks take precedence over the built-in continuous effects. */
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

    /** Every phase uses the combat clock, including live projectile and wave positions. */
    private void drawReferenceAbility(Graphics2D g, double p) {
        String id = definition.getId();
        Color tint = switch (id) {
            case "heavy_slash", "knights_wrath", "flame_burst" -> FIRE;
            case "shadow_strike", "shadow_step", "twin_fang", "death_mark" -> SHADOW;
            case "heal" -> new Color(128, 244, 186);
            case "holy_bolt", "holy_shield", "divine_light" -> HOLY;
            case "ice_shard", "lightning_strike", "elemental_storm" -> ICE;
            default -> EMBER;
        };
        if (projectileDriven) { drawLiveProjectile(g, tint); return; }
        double release = AbilityAnimationTiming.releaseProgress(definition);
        double hit = hits[0];
        double hitAge = hit * actionDuration;
        double elapsed = age(), after = elapsed - hitAge;
        double endFade = 1 - smooth((elapsed - maxLife + Math.min(0.28, maxLife * 0.22))
                / Math.min(0.28, maxLife * 0.22));
        double activation = smooth(after / 0.10) * endFade;
        double targetX = impactFrozen ? impactX : worldX;
        double targetY = impactFrozen ? impactY : worldY;
        double preparation = envelope(p, 0, 0.12, Math.max(0.12, release - 0.06), release + 0.08);
        drawCharge(g, tint, preparation, smooth(p / Math.max(0.01, release)));
        switch (id) {
            case "heavy_slash", "twin_fang", "knights_wrath" -> {
                for (int i = 0; i < hits.length; i++) {
                    double angle = aimAngle + (id.equals("knights_wrath") ? i * Math.PI * 0.65
                            : id.equals("twin_fang") ? (i == 0 ? -0.32 : 0.65) : 0);
                    slash(g, casterX, casterY - 4, angle, Math.min(102, radius * 0.86),
                            i % 2 == 0 ? tint : id.equals("twin_fang") ? new Color(246, 116, 211) : EMBER,
                            p, hits[i]);
                    double flash = envelope(p, hits[i] - 0.045, hits[i], hits[i] + 0.025, hits[i] + 0.17);
                    glow(g, startX, startY, 23, tint, flash * 0.5);
                    if (!id.equals("twin_fang")) {
                        flames(g, startX, startY + 8, 25,
                                phase(p, hits[i], hits[i] + 0.22), flash * 0.8, 7);
                    }
                    sparks(g, startX, startY, id.equals("twin_fang") ? tint : EMBER, 9, 38,
                            phase(p, hits[i], hits[i] + 0.25), flash);
                }
                if (id.equals("knights_wrath")) {
                    double spin = envelope(p, release, release + 0.06, 0.73, 0.96);
                    orbitArc(g, casterX, casterY + 12, Math.min(100, radius * 0.85),
                            elapsed * 9, 2.4, FIRE, spin * 0.55, 0.62);
                    groundSeal(g, casterX, casterY + 20, 48, FIRE, -elapsed * 2, spin * 0.42);
                }
            }
            case "shield_bash" -> {
                double thrust = smooth(phase(p, release, hit));
                double alpha = envelope(p, release - 0.04, release + 0.07, hit, hit + 0.20);
                double bx = casterX + Math.cos(aimAngle) * (16 + thrust * 15);
                double by = casterY + Math.sin(aimAngle) * (16 + thrust * 15) - 4;
                barrier(g, bx, by, 36, STEEL, p, alpha);
                SkillVfxShapes.crescent(g, bx, by, aimAngle, 32 + thrust * 12,
                        8, STEEL, alpha * 0.8);
                if (after >= 0) {
                    double t = Math.min(1, after / 0.30);
                    orbitArc(g, targetX, targetY, 12 + easeOut(t) * 48,
                            aimAngle - 0.85, 1.7, EMBER, (1 - t) * 0.85, 0.8);
                    dust(g, targetX, targetY + 16, 48, t, (1 - t) * 0.6);
                    SkillVfxShapes.shockwave(g, targetX, targetY + 10, 12 + easeOut(t) * 48,
                            STEEL, aimAngle, (1 - t) * 0.64, 0.75);
                }
            }
            case "earth_shatter" -> {
                // Ground contact remains at the sampled blade event, never at a distant aim target.
                groundSlam(g, impactFrozen ? impactX : startX,
                        (impactFrozen ? impactY : startY) + 14, Math.min(125, radius), p, endFade, hit);
                if (after >= 0) {
                    double t = Math.min(1, after / 0.38);
                    for (int i = 0; i < 5; i++) {
                        double px = targetX + (i - 2) * 15;
                        ribbon(g, px, targetY + 12, px + (i - 2) * 4,
                                targetY - Math.sin(t * Math.PI) * (56 - Math.abs(i - 2) * 9),
                                EMBER, 5, (1 - t) * 0.72, 5);
                        shard(g, px, targetY - Math.sin(t * Math.PI) * (62 - Math.abs(i - 2) * 9),
                                -Math.PI * 0.5, 10 - Math.abs(i - 2), EMBER, (1 - t) * 0.8);
                    }
                }
            }
            case "shadow_strike", "shadow_step" -> {
                double travel = envelope(p, release, release + 0.045, hit, hit + 0.16);
                for (int i = 0; i < trailCount; i++) {
                    double old = elapsed - trailBirth[i];
                    if (old < 0 || old > 0.23) continue;
                    double fade = Math.pow(1 - old / 0.23, 2) * 0.6;
                    ribbon(g, trailX[i] - Math.cos(aimAngle) * 21, trailY[i] - 5,
                            trailX[i] + Math.cos(aimAngle) * 11, trailY[i] - 5,
                            SHADOW, 8, fade, Math.sin(i * 1.7) * 7);
                    glow(g, trailX[i], trailY[i] - 7, 24, SHADOW, fade * 0.3);
                    SkillVfxShapes.crescent(g, trailX[i], trailY[i] - 5,
                            aimAngle + Math.PI + Math.sin(i * 1.4) * 0.35, 19, 5,
                            i % 2 == 0 ? SHADOW : new Color(233, 128, 240), fade * 0.72);
                }
                orbitArc(g, casterX, casterY - 7, 23, elapsed * 12, 2.6, SHADOW, travel * 0.7, 1);
                if (id.equals("shadow_strike"))
                    slash(g, casterX, casterY - 4, aimAngle, 70, SHADOW, p, hit);
                else {
                    double flash = envelope(p, hit - 0.035, hit + 0.015, hit + 0.07, hit + 0.24);
                    sparks(g, casterX, casterY - 4, new Color(223, 180, 255), 14, 46,
                            phase(p, hit, 1), flash);
                    SkillVfxShapes.shockwave(g, casterX, casterY + 12,
                            10 + easeOut(phase(p, hit, hit + 0.22)) * 39,
                            SHADOW, -elapsed * 3, flash * 0.8, 0.55);
                }
            }
            case "death_mark" -> {
                double flight = envelope(p, release, release + 0.04, hit - 0.01, hit + 0.03);
                double t = smooth(phase(p, release, hit));
                ribbon(g, startX, startY, worldX, worldY, SHADOW, 2.5, flight * 0.5, 18 * Math.sin(t * Math.PI));
                glow(g, startX + (worldX - startX) * t, startY + (worldY - startY) * t - Math.sin(t * Math.PI) * 18,
                        16, SHADOW, flight * 0.7);
                eye(g, worldX, worldY - 37 + Math.sin(elapsed * 3) * 3, 15, p, activation);
                SkillVfxShapes.castSigil(g, worldX, worldY - 37 + Math.sin(elapsed * 3) * 3,
                        24, SHADOW, -elapsed * 1.3, activation * 0.7);
                orbitArc(g, worldX, worldY - 37, 23, elapsed * 1.8, 2.3, SHADOW, activation * 0.65, 0.7);
                ring(g, worldX, worldY + 15, 24 + Math.sin(elapsed * 3) * 1.5, SHADOW, 1.3, activation * 0.45, 0.5);
            }
            case "flame_burst", "ice_shard" -> {
                // Shots are rendered by their live projectile effect after their launch event.
                double casting = envelope(p, 0.08, 0.24, hits[hits.length - 1], 0.85);
                double size = 9 + smooth(p / hit) * 7;
                glow(g, startX, startY, size * 2, tint, casting * 0.7);
                SkillVfxShapes.castSigil(g, startX, startY, size + 9,
                        tint, -elapsed * 3, casting * 0.62);
                if (id.equals("ice_shard")) shard(g, startX, startY, sourceAngle, size, ICE, casting * 0.9);
                else {
                    orbitArc(g, startX, startY, size, elapsed * 7, 3.7, EMBER, casting * 0.8, 0.85);
                    glow(g, startX, startY, 5, EMBER, casting);
                }
            }
            case "holy_bolt" -> {
                double t = smooth(phase(p, release, hit));
                double flight = envelope(p, release, release + 0.045, hit, hit + 0.035);
                double originX = launchFrozen ? launchX : startX, originY = launchFrozen ? launchY : startY;
                double px = originX + (worldX - originX) * t;
                double py = originY + (worldY - originY) * t - Math.sin(t * Math.PI) * 16;
                ribbon(g, px - Math.cos(aimAngle) * 32, py - Math.sin(aimAngle) * 32,
                        px, py, HOLY, 6, flight * 0.8, 4);
                glow(g, px, py, 25, HOLY, flight * 0.7);
                star(g, px, py, 8, Color.WHITE, flight);
                SkillVfxShapes.crescent(g, px, py, aimAngle, 15, 4, HOLY, flight * 0.8);
                for (int i = -1; i <= 1; i += 2) {
                    double sideX = -Math.sin(aimAngle) * i;
                    double sideY = Math.cos(aimAngle) * i;
                    ribbon(g, px - Math.cos(aimAngle) * 35 + sideX * 13,
                            py - Math.sin(aimAngle) * 35 + sideY * 13,
                            px + sideX * 4, py + sideY * 4, HOLY, 3, flight * 0.56, i * 5);
                }
                impact(g, targetX, targetY, 65, HOLY, p, hit);
            }
            case "heal" -> {
                sigil(g, casterX, casterY + 21, 47, tint, p, activation * 0.75, true);
                rising(g, casterX, casterY + 18, 37, tint, p, activation, 16);
                glow(g, casterX, casterY - 9, 47, tint, activation * 0.22);
                if (after >= 0) {
                    double t = Math.min(1, after / 0.34);
                    ring(g, casterX, casterY + 20, 16 + easeOut(t) * 51,
                            tint, 2.4, (1 - t) * 0.7, 0.48);
                    SkillVfxShapes.shockwave(g, casterX, casterY + 20,
                            16 + easeOut(t) * 51, tint, elapsed, (1 - t) * 0.58, 0.48);
                    double lift = smooth(t) * 30;
                    stroke(g, new Line2D.Double(casterX - 7, casterY - 20 - lift,
                            casterX + 7, casterY - 20 - lift), tint, 3, (1 - t) * 0.9);
                    stroke(g, new Line2D.Double(casterX, casterY - 27 - lift,
                            casterX, casterY - 13 - lift), Color.WHITE, 2.5, (1 - t) * 0.9);
                }
            }
            case "holy_shield" -> {
                barrier(g, casterX, casterY - 9, 57, HOLY, p, activation);
                orbitArc(g, casterX, casterY - 8, 44, elapsed * 0.8, 1.9, Color.WHITE, activation * 0.55, 1.12);
                sigil(g, casterX, casterY + 22, 39, HOLY, p, activation * 0.5, true);
                rising(g, casterX, casterY + 16, 36, HOLY, p, activation * 0.6, 8);
                if (blockAge < 0.24) {
                    double t = blockAge / 0.24;
                    glow(g, blockX, blockY, 30, Color.WHITE, (1 - t) * 0.65);
                    sparks(g, blockX, blockY, HOLY, 10, 32, t, 1 - t);
                }
            }
            case "divine_light" -> {
                double charge = envelope(p, release - 0.05, release + 0.10, hit, hit + 0.05);
                sigil(g, targetX, targetY + 16, Math.min(78, radius * 0.65), HOLY, p, charge * 0.85, true);
                if (after >= 0) {
                    double beam = smooth(after / 0.045) * (1 - smooth((after - 0.13) / 0.33));
                    pillar(g, targetX, targetY + 16, 106, HOLY, p, beam);
                    glow(g, targetX, targetY, 80, HOLY, beam * 0.45);
                    ring(g, targetX, targetY + 15, 12 + easeOut(after / 0.4) * 77,
                            HOLY, 3, beam * 0.8, 0.56);
                    rising(g, targetX, targetY, 68, HOLY, phase(after, 0, 0.55), beam, 18);
                    SkillVfxShapes.shockwave(g, targetX, targetY + 15,
                            12 + easeOut(after / 0.4) * 77, HOLY, elapsed,
                            beam * 0.82, 0.56);
                }
            }
            case "lightning_strike" -> {
                lightningStrike(g, targetX, targetY, Math.min(radius, 150), p, endFade, hit);
                double charge = envelope(p, release - 0.05, release + 0.05, hit - 0.03, hit + 0.03);
                orbitArc(g, targetX, targetY + 14, 32, -elapsed * 4, 4.5, ICE, charge * 0.7, 0.48);
                groundSeal(g, targetX, targetY + 14, 38, ICE, -elapsed * 3, charge * 0.82);
                if (after >= 0 && after < 0.30) {
                    double alpha = (1 - smooth(after / 0.30)) * 0.58;
                    for (int i = 0; i < 3; i++) {
                        double angle = i * Math.PI * 2 / 3 + 0.4;
                        lightning(g, targetX, targetY, targetX + Math.cos(angle) * 60,
                                targetY + Math.sin(angle) * 38, elapsed + i, alpha);
                    }
                    SkillVfxShapes.shockwave(g, targetX, targetY + 12,
                            12 + easeOut(after / 0.30) * 65, ICE, elapsed,
                            alpha * 1.35, 0.62);
                }
            }
            case "elemental_storm" -> drawNova(g, elapsed, hitAge, endFade);
            default -> drawAbility(g, p, endFade);
        }
    }

    private void drawCharge(Graphics2D g, Color tint, double alpha, double progress) {
        if (alpha < 0.002) return;
        boolean spell = definition.getAbilityClass() == AbilityClass.PRIEST
                || definition.getAbilityClass() == AbilityClass.ELEMENTALIST;
        if (spell) {
            groundSeal(g, casterX, casterY + 21, 27 + progress * 17,
                    tint, age() * 1.5, alpha * 0.5);
            SkillVfxShapes.castSigil(g, startX, startY, 13 + progress * 12,
                    tint, -age() * 2.8, alpha * 0.76);
        } else {
            SkillVfxShapes.crescent(g, startX, startY, sourceAngle,
                    12 + progress * 10, 4, tint, alpha * 0.5);
        }
        glow(g, startX, startY, 16 + progress * 12, tint, alpha * 0.5);
        orbitArc(g, startX, startY, 6 + progress * 10, -age() * 5, 3.9, tint, alpha * 0.75, 0.85);
        for (int i = 0; i < 6; i++) {
            double angle = unit(i, 3) * Math.PI * 2 + age() * 0.8;
            double distance = 10 + (1 - progress) * (14 + unit(i, 2) * 13);
            double px = startX + Math.cos(angle) * distance;
            double py = startY + Math.sin(angle) * distance * 0.85;
            stroke(g, new Line2D.Double(px, py, px + Math.cos(angle) * 4,
                    py + Math.sin(angle) * 4), tint, 1.2, alpha * 0.65);
            if (i % 2 == 0) SkillVfxShapes.sparkle(g, px, py, 2.6,
                    tint, angle, alpha * 0.72);
        }
        glow(g, startX, startY, 4, Color.WHITE, alpha * progress * 0.7);
    }

    private void drawLiveProjectile(Graphics2D g, Color tint) {
        boolean ice = definition.getId().equals("ice_shard");
        double elapsed = age(), fade = 1 - smooth((elapsed / maxLife - 0.85) / 0.15);
        double dx = Math.cos(sourceAngle), dy = Math.sin(sourceAngle);
        double speed = ice ? 440 : 360;
        // Integrate the same acceleration curve as gameplay for a stable, continuous trail.
        double now = projectileDistance(elapsed, speed);
        for (int i = 11; i >= 1; i--) {
            double past = Math.max(0, elapsed - i * 0.012);
            double distance = now - projectileDistance(past, speed);
            double next = now - projectileDistance(Math.max(0, elapsed - (i - 1) * 0.012), speed);
            double alpha = fade * Math.pow(1 - i / 12.0, 1.5);
            double sway = Math.sin(elapsed * 11 - i * 0.55) * (ice ? 1.8 : 3.2) * i / 12.0;
            ribbon(g, worldX - dx * distance - dy * sway, worldY - dy * distance + dx * sway,
                    worldX - dx * next, worldY - dy * next, tint,
                    (ice ? 7 : 13) * (1 - i / 13.0), alpha * 0.8, 0);
        }
        glow(g, worldX, worldY, ice ? 30 : 38, tint, fade * 0.84);
        if (ice) {
            shard(g, worldX, worldY, sourceAngle, 28, ICE, fade);
            shard(g, worldX - dx * 16 - dy * 7, worldY - dy * 16 + dx * 7, sourceAngle, 10, ICE, fade * 0.5);
            shard(g, worldX - dx * 16 + dy * 7, worldY - dy * 16 - dx * 7, sourceAngle, 10, ICE, fade * 0.5);
            stroke(g, new Line2D.Double(worldX - dx * 19, worldY - dy * 19,
                    worldX + dx * 26, worldY + dy * 26), Color.WHITE, 1.8, fade * 0.95);
        } else {
            drawFireComet(g, elapsed, fade);
            orbitArc(g, worldX, worldY, 12, elapsed * 10, 3.6, FIRE, fade * 0.9, 0.9);
            glow(g, worldX + dx * 4, worldY + dy * 4, 8, EMBER, fade);
        }
        for (int i = 0; i < 5; i++) {
            double pastAge = Math.max(0, elapsed - (i + 1) * 0.035);
            double distance = now - projectileDistance(pastAge, speed);
            double side = Math.sin(elapsed * 12 + i * 2.4) * (7 + i * 2.3);
            double px = worldX - dx * distance - dy * side;
            double py = worldY - dy * distance + dx * side;
            double alpha = fade * (1 - i / 5.0) * smooth(elapsed / 0.10);
            if (ice) shard(g, px, py, sourceAngle + i * 0.37, 4 + i * 0.5, ICE, alpha * 0.72);
            else SkillVfxShapes.sparkle(g, px, py, 2.2 + i * 0.2, EMBER, elapsed + i, alpha * 0.72);
        }
        glow(g, worldX, worldY, 4, Color.WHITE, fade * 0.85);
    }

    private void drawFireComet(Graphics2D g, double elapsed, double fade) {
        Graphics2D comet = (Graphics2D) g.create();
        try {
            comet.translate(worldX, worldY);
            comet.rotate(sourceAngle);
            double flicker = Math.sin(elapsed * 19) * 2;
            Path2D flame = new Path2D.Double();
            flame.moveTo(-37 - flicker, 0);
            flame.curveTo(-19, -4, -13, -12 - flicker, 5, -9);
            flame.curveTo(25, -6, 25, 6, 5, 9);
            flame.curveTo(-13, 12 + flicker, -19, 4, -37 - flicker, 0);
            flame.closePath();
            comet.setPaint(new GradientPaint(-37, 0, withAlpha(FIRE, 0),
                    15, 0, withAlpha(FIRE, fade * 215)));
            comet.fill(flame);
            SkillVfxShapes.crescent(comet, 3, 0, 0, 15, 7, EMBER, fade * 0.92);
            ribbon(comet, -28, 0, 16, 0, EMBER, 4.5, fade * 0.92, flicker * 0.5);
            glow(comet, 8, 0, 9, Color.WHITE, fade * 0.82);
        } finally { comet.dispose(); }
    }

    private static void groundSeal(Graphics2D g, double x, double y, double radius,
            Color tint, double rotation, double alpha) {
        if (alpha < 0.002) return;
        Graphics2D ground = (Graphics2D) g.create();
        try {
            ground.translate(x, y);
            ground.scale(1.0, 0.48);
            SkillVfxShapes.castSigil(ground, 0, 0, radius, tint, rotation, alpha);
        } finally { ground.dispose(); }
    }

    private static double projectileDistance(double elapsed, double speed) {
        return speed * (elapsed - 0.07 * (1 - Math.exp(-elapsed / 0.14)));
    }

    private void drawNova(Graphics2D g, double elapsed, double hitAge, double fade) {
        double x = impactFrozen ? impactX : casterX, y = impactFrozen ? impactY : casterY;
        double time = elapsed - hitAge;
        Color[] elements = {FIRE, ICE, SHADOW};
        double charge = smooth(elapsed / 0.12) * (1 - smooth((time + 0.03) / 0.12));
        sigil(g, casterX, casterY + 21, 43, ICE, progress(), charge * 0.6, false);
        for (int i = 0; i < 3; i++) {
            double angle = elapsed * 4 + i * Math.PI * 2 / 3;
            glow(g, casterX + Math.cos(angle) * (37 - charge * 14),
                    casterY + Math.sin(angle) * 17, 11, elements[i], charge * 0.8);
        }
        if (time < 0) return;
        double r = waveRadius >= 0 ? waveRadius : radius * smooth(time / 0.60);
        double alpha = smooth(time / 0.045) * fade;
        SkillVfxShapes.shockwave(g, x, y, r, ICE, elapsed * 0.45, alpha * 0.6, 1.0);
        glow(g, x, y, 30 + Math.min(r, 100) * 0.3, ICE, alpha * Math.exp(-time * 8) * 0.6);
        for (int i = 0; i < 3; i++) {
            double from = i * Math.PI * 2 / 3 + elapsed * 0.45;
            orbitArc(g, x, y, r, from, 1.93, elements[i], alpha * 0.85, 1);
            orbitArc(g, x, y, r * 0.86, from + 0.05, 1.72, elements[i], alpha * 0.30, 1);
            SkillVfxShapes.crescent(g, x, y, from + 0.96, r,
                    Math.max(3, Math.min(14, r * 0.07)), elements[i], alpha * 0.67);
            SkillVfxShapes.crescent(g, x, y, from + 0.96, r * 0.86,
                    4, elements[i], alpha * 0.22);
            for (int j = 0; j < 6; j++) {
                double angle = from + j * 1.93 / 6;
                double px = x + Math.cos(angle) * r, py = y + Math.sin(angle) * r;
                if (i == 1) shard(g, px, py, angle, 6 + (1 - time) * 3, ICE, alpha * 0.8);
                else star(g, px, py, 2.8, elements[i], alpha * 0.8);
            }
        }
        sparks(g, x, y, ICE, 18, Math.min(radius, 150), Math.min(1, time / 0.72), alpha * 0.65);
    }

    private static void orbitArc(Graphics2D g, double x, double y, double radius,
            double angle, double sweep, Color tint, double alpha, double verticalScale) {
        if (radius <= 0 || alpha < 0.002) return;
        Path2D arc = new Path2D.Double();
        for (int i = 0; i <= 32; i++) {
            double at = angle + sweep * i / 32;
            double px = x + Math.cos(at) * radius, py = y + Math.sin(at) * radius * verticalScale;
            if (i == 0) arc.moveTo(px, py); else arc.lineTo(px, py);
        }
        stroke(g, arc, tint, 7, alpha * 0.12);
        stroke(g, arc, tint, 2.2, alpha * 0.8);
        stroke(g, arc, Color.WHITE, 0.7, alpha * 0.6);
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
        Graphics2D sweep = (Graphics2D) g.create();
        try {
            sweep.translate(x, y);
            sweep.scale(1.0, 0.7);
            SkillVfxShapes.crescent(sweep, 0, 0, (head + tail) * 0.5,
                    reach * 0.94, reach * 0.14, c, alpha * 0.72);
        } finally { sweep.dispose(); }
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
        g.setColor(withAlpha(c, 145 * alpha)); g.fill(ribbon);
        stroke(g, edge, c, 8, alpha * 0.16);
        stroke(g, edge, c, 3.8, alpha * 0.65);
        stroke(g, edge, new Color(255, 244, 226), 1.3, alpha * 0.8);
        glow(g, x + Math.cos(head) * reach, y + Math.sin(head) * reach * 0.7, 14, c, alpha * 0.45);
        SkillVfxShapes.sparkle(g, x + Math.cos(head) * reach,
                y + Math.sin(head) * reach * 0.7, 5, c, head, alpha * 0.82);
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
        SkillVfxShapes.shockwave(g, x, y, size * (0.08 + easeOut(t) * 0.88),
                EMBER, age() * 0.13, a * release * 0.54, 0.55);
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
            double bx = sx + (x - sx) * t + ox * jitter, by = sy + (y - sy) * t + oy * jitter;
            bolt.lineTo(bx, by);
            if (length > 90 && i % 2 == 0) {
                double side = i == 4 ? -1 : 1;
                double branchLength = Math.min(54, length * 0.19) * (0.8 + unit(i, 3) * 0.2);
                Path2D branch = new Path2D.Double();
                branch.moveTo(bx, by);
                for (int j = 1; j <= 3; j++) {
                    double f = j / 3.0, wobble = (unit(i + j, 9) - 0.5) * 12;
                    branch.lineTo(bx + ox * (branchLength * f * side + wobble) + (x - sx) / length * f * 20,
                            by + oy * (branchLength * f * side + wobble) + (y - sy) / length * f * 20);
                }
                stroke(g, branch, ICE, 7, a * 0.09);
                stroke(g, branch, ICE, 2.2, a * 0.45);
                stroke(g, branch, Color.WHITE, 0.8, a * 0.62);
            }
        }
        bolt.lineTo(x, y);
        stroke(g, bolt, ICE, 14, a * 0.12); stroke(g, bolt, ICE, 6, a * 0.7);
        stroke(g, bolt, new Color(237, 252, 255), 2.1, a * 0.95); glow(g, x, y, 28, ICE, a * 0.45);
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
        SkillVfxShapes.shockwave(g, x, y + 8, size * (0.13 + easeOut(t) * 0.66),
                c, age() * 0.45, a * (1 - t) * 0.55, 0.68);
        SkillVfxShapes.sparkle(g, x, y, Math.max(5, size * 0.18), c,
                age() * 0.3, a * Math.exp(-t * 9));
        sparks(g, x, y, c, 12, size, t, a);
    }

    private void barrier(Graphics2D g, double x, double y, double size, Color c, double p, double a) {
        double r = size * (0.78 + 0.22 * smooth(p / hits[0]) + Math.sin(age() * 4) * 0.025);
        glow(g, x, y, r * 0.85, c, a * 0.07);
        Path2D path = new Path2D.Double(); path.moveTo(x, y - r * 0.75);
        path.curveTo(x + r * 0.7, y - r * 0.54, x + r * 0.76, y + r * 0.17, x, y + r * 0.85);
        path.curveTo(x - r * 0.76, y + r * 0.17, x - r * 0.7, y - r * 0.54, x, y - r * 0.75);
        g.setColor(withAlpha(c, a * 16));
        g.fill(path);
        Path2D facets = new Path2D.Double();
        facets.moveTo(x, y - r * 0.75); facets.lineTo(x, y + r * 0.85);
        facets.moveTo(x - r * 0.47, y - r * 0.2); facets.lineTo(x, y + r * 0.12);
        facets.lineTo(x + r * 0.47, y - r * 0.2);
        stroke(g, facets, c, 1, a * 0.32);
        Graphics2D sheen = (Graphics2D) g.create();
        try {
            sheen.clip(path);
            double at = (age() * 0.6) % 1;
            double bandY = y - r + at * r * 2.2;
            sheen.setPaint(new GradientPaint((float) x, (float) bandY, withAlpha(Color.WHITE, 0),
                    (float) x, (float) (bandY + r * 0.17), withAlpha(Color.WHITE, a * 35)));
            sheen.fill(new java.awt.geom.Rectangle2D.Double(x - r, bandY, r * 2, r * 0.17));
        } finally { sheen.dispose(); }
        stroke(g, path, c, 9, a * 0.1); stroke(g, path, c, 2.4, a * 0.7);
        SkillVfxShapes.sparkle(g, x, y - r * 0.19, r * 0.12, c, 0, a * 0.64);
        stroke(g, new Arc2D.Double(x - r * 0.55, y - r * 0.7, r * 1.1, r * 1.3, 40 + age() * 80, 80, Arc2D.OPEN),
                Color.WHITE, 1, a * 0.6);
    }
    private void sigil(Graphics2D g, double x, double y, double size, Color c, double p, double a, boolean holy) {
        double r = size * (0.75 + smooth(p / 0.24) * 0.25);
        groundSeal(g, x, y, r, c, age() * (holy ? 0.35 : -0.5), a * 0.82);
        ring(g, x, y, r * 0.75, c, 1, a * 0.32, 0.48);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + age() * 0.6;
            double px = x + Math.cos(angle) * r * 0.87, py = y + Math.sin(angle) * r * 0.42;
            if (holy) star(g, px, py, 4.5, c, a * 0.75); else shard(g, px, py, angle, 4, c, a * 0.65);
        }
    }
    private void pillar(Graphics2D g, double x, double y, double size, Color c, double p, double a) {
        double strength = a * smooth(p / hits[0]), height = Math.min(310, size * 2.2), width = Math.max(8, size * 0.17);
        if (strength < 0.002) return;
        Graphics2D column = (Graphics2D) g.create();
        try {
            Path2D light = new Path2D.Double();
            light.moveTo(x - width * 1.3, y);
            light.lineTo(x - width * 0.5, y - height);
            light.lineTo(x + width * 0.5, y - height);
            light.lineTo(x + width * 1.3, y);
            light.closePath();
            column.setPaint(new GradientPaint((float) x, (float) (y - height), withAlpha(c, strength * 18),
                    (float) x, (float) y, withAlpha(c, strength * 85)));
            column.fill(light);
            SkillVfxShapes.castSigil(column, x, y - height * 0.78, width * 1.6,
                    c, -age() * 0.6, strength * 0.45);
        } finally { column.dispose(); }
        for (int i = -2; i <= 2; i++) {
            Path2D beam = new Path2D.Double(); double offset = i * width * 0.55;
            beam.moveTo(x + offset, y);
            beam.curveTo(x + offset + Math.sin(age() * 6 + i) * 7, y - height * 0.45,
                    x + offset * 0.7, y - height * 0.8, x + offset * 0.35, y - height);
            stroke(g, beam, c, i == 0 ? width * 1.7 : width * 0.6, strength * 0.15);
            stroke(g, beam, Color.WHITE, i == 0 ? 3 : 0.8, strength * (i == 0 ? 0.72 : 0.22));
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
        SkillVfxShapes.sparkle(g, x, y, size * 1.3, c, 0, a * 0.9);
    }
    private static void ring(Graphics2D g, double x, double y, double r, Color c, double width, double a, double verticalScale) {
        if (r <= 0 || a < 0.002) return;
        Shape ring = new Ellipse2D.Double(x - r, y - r * verticalScale, r * 2, r * 2 * verticalScale);
        stroke(g, ring, c, width * 3, a * 0.08); stroke(g, ring, c, width, a * 0.68);
    }
    private static void glow(Graphics2D g, double x, double y, double r, Color c, double a) {
        SkillVfxGlow.draw(g, x, y, r, c, a);
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
