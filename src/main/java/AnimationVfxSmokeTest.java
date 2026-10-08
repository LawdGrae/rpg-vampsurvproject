import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Headless regression checks for coherent character animation and live skill rendering. */
public class AnimationVfxSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        verifyCharacterTiming();
        verifyVisualStateAndSettling();
        verifySkillRendering();
        verifySkillCannotBeInterruptedByAutoAttack();
        verifySkillTakeoverCancelsPendingMelee();
        verifyIntegratedRendering();
        long missing = SkillEffectAtlas.getMetadata().stream().filter(skill ->
                SkillEffectAtlas.getAnimation(skill.id(), skill.passive() ? "passive" : "action") == null
                || ((skill.id().equals("flame_burst") || skill.id().equals("ice_shard"))
                && SkillEffectAtlas.getAnimation(skill.id(), "travel") == null)).count();
        System.out.println("Animation timing, grips, draw isolation and ability VFX lifecycle checks passed");
        if (missing > 0) System.out.println("PNG visual QA pending: " + missing
                + " unresolved reference skill mappings; active skills use procedural fallback VFX");
    }

    private static void verifyCharacterTiming() throws Exception {
        Method transform = Player.class.getDeclaredMethod("characterTransform", int.class, int.class);
        transform.setAccessible(true);
        for (int index = 0; index < 4; index++) {
            for (String direction : List.of("right", "left", "up", "down")) {
                for (String mode : List.of("idle", "walk", "attack")) {
                    Player coarse = player(index);
                    Player fine = player(index);
                    for (Player current : List.of(coarse, fine)) {
                        current.setKeyPressed(direction, true);
                        current.update(0.0);
                        if (!mode.equals("walk")) current.setKeyPressed(direction, false);
                        if (mode.equals("attack")) current.playAttackAnimation(null, 0.49);
                    }
                    double elapsed = mode.equals("attack") ? 0.207 : 0.733;
                    coarse.update(elapsed);
                    advance(fine, elapsed, 73);
                    String context = index + " " + direction + " " + mode;
                    near(coarse.getWorldX(), fine.getWorldX(), context + " world x");
                    near(coarse.getWorldY(), fine.getWorldY(), context + " world y");
                    near(coarse.getWeaponCastWorldX(), fine.getWeaponCastWorldX(), context + " grip x");
                    near(coarse.getWeaponCastWorldY(), fine.getWeaponCastWorldY(), context + " grip y");
                    double[] coarseMatrix = new double[6];
                    double[] fineMatrix = new double[6];
                    ((AffineTransform) transform.invoke(coarse, 128, 128)).getMatrix(coarseMatrix);
                    ((AffineTransform) transform.invoke(fine, 128, 128)).getMatrix(fineMatrix);
                    for (int axis = 0; axis < coarseMatrix.length; axis++) {
                        near(coarseMatrix[axis], fineMatrix[axis], context + " body transform " + axis);
                    }
                    identical(render(coarse), render(fine), context + " must be frame-rate independent");
                    if (mode.equals("attack")) {
                        double beforeX = coarse.getWeaponCastWorldX();
                        double beforeY = coarse.getWeaponCastWorldY();
                        coarse.playAttackAnimation(null, 0.64);
                        near(coarse.getWeaponCastWorldX(), beforeX, context + " interrupted cast x");
                        near(coarse.getWeaponCastWorldY(), beforeY, context + " interrupted cast y");
                        ((AffineTransform) transform.invoke(coarse, 128, 128)).getMatrix(fineMatrix);
                        for (int axis = 0; axis < coarseMatrix.length; axis++) {
                            near(coarseMatrix[axis], fineMatrix[axis], context + " interrupted body " + axis);
                        }
                    }
                }
            }
        }
    }

    private static void verifyVisualStateAndSettling() throws Exception {
        for (int index = 0; index < 4; index++) {
            Player current = player(index);
            BufferedImage idle = render(current);
            current.update(0.37);
            require(!Arrays.equals(pixels(idle), pixels(render(current))),
                    "Character " + index + " needs visible idle breathing");
            current.setKeyPressed("right", true);
            advance(current, 0.42, 25);
            require(number(current, "locomotionBlend") > 0.5, "Walk animation must blend in");
            current.setKeyPressed("right", false);
            current.update(0.0);
            require(number(current, "locomotionBlend") > 0.5,
                    "Releasing movement must preserve the current pose before recovery");
            advance(current, 2.0, 120);
            require(number(current, "locomotionBlend") < 0.001,
                    "Walk animation must settle into idle after movement stops");
            current.playAttackAnimation(null, 0.49);
            current.update(0.23);
            BufferedImage attack = render(current);
            require(!Arrays.equals(pixels(attack), pixels(idle)), "Attack needs visible body motion");
            double visualTime = number(current, "visualTime");
            double phase = number(current, "animationTime");
            double castX = current.getWeaponCastWorldX();
            double castY = current.getWeaponCastWorldY();
            identical(attack, render(current), "Rendering must not advance the animation");
            near(number(current, "visualTime"), visualTime, "Draw mutated idle clock");
            near(number(current, "animationTime"), phase, "Draw mutated gait clock");
            near(current.getWeaponCastWorldX(), castX, "Draw mutated cast x");
            near(current.getWeaponCastWorldY(), castY, "Draw mutated cast y");
            assertGraphicsIsolation((graphics) -> current.draw(graphics, 100, 100),
                    "Character " + index + " gameplay draw");
            assertGraphicsIsolation((graphics) -> current.drawPreview(graphics,
                    new Rectangle(50, 50, 128, 128)), "Character " + index + " preview draw");
            current.update(3.0);
            require(number(current, "attackAnimationTime") == 0.0,
                    "Attack timer must finish instead of looping forever");
            current.update(1000000.125);
            for (String name : List.of("visualTime", "animationTime", "locomotionBlend")) {
                double value = number(current, name);
                require(Double.isFinite(value) && value >= 0.0 && value < 100000,
                        "Long-running animation state must remain finite and bounded: " + name);
            }
            render(current);
        }
    }

    private static void verifySkillRendering() throws Exception {
        for (AbilityDefinition definition : AbilityDefinition.createAll()) {
            AbilityVisualEffect effect = effect(definition);
            double duration = AbilityAnimationTiming.duration(definition);
            BufferedImage first = render(effect);
            effect.update(duration * 0.18);
            BufferedImage growing = render(effect);
            effect.update(duration * 0.47);
            BufferedImage peak = render(effect);
            if (SkillEffectAtlas.getMetadata(definition.getId()) == null) {
                require(nontransparentPixels(growing) > 0 && nontransparentPixels(peak) > 0,
                        definition.getId() + " needs visible legacy VFX");
                require(!Arrays.equals(pixels(first), pixels(growing))
                                && !Arrays.equals(pixels(growing), pixels(peak)),
                        definition.getId() + " legacy VFX must evolve between frames");
            } else {
                verifyReferenceTracks(definition);
                if (!definition.isPassive() && !hasBodyTrack(definition)) {
                    require(nontransparentPixels(growing) + nontransparentPixels(peak) > 0,
                            definition.getId() + " needs visible procedural VFX when PNG tracks are missing");
                    require(!Arrays.equals(pixels(growing), pixels(peak)),
                            definition.getId() + " procedural VFX must evolve through the action");
                }
            }
            identical(peak, render(effect), definition.getId() + " draw must be deterministic");
            assertGraphicsIsolation((graphics) -> effect.draw(graphics, 128, 128, 0, 0),
                    definition.getId() + " effect draw");
            AbilityVisualEffect coarse = effect(definition);
            AbilityVisualEffect fine = effect(definition);
            coarse.update(duration * 0.413);
            for (int part = 0; part < 37; part++) fine.update(duration * 0.413 / 37.0);
            identical(render(coarse), render(fine), definition.getId() + " VFX timing partitions");
            AbilityVisualEffect coincident = new AbilityVisualEffect(definition,
                    10.25, -3.75, 10.25, -3.75, 72, new Color(180, 160, 255), duration);
            coincident.setCasterPosition(10.25, -3.75);
            coincident.update(duration * 0.65);
            BufferedImage coincidentImage = render(coincident);
            if (SkillEffectAtlas.getMetadata(definition.getId()) == null) {
                require(nontransparentPixels(coincidentImage) > 0,
                        definition.getId() + " must render safely at a coincident cast and target point");
            } else if (!definition.isPassive() && !hasBodyTrack(definition)) {
                require(nontransparentPixels(coincidentImage) > 0,
                        definition.getId() + " procedural VFX must safely render at a coincident anchor");
            }
            effect.update(2.0);
            require(effect.isExpired(), definition.getId() + " must expire");
            require(nontransparentPixels(render(effect)) == 0,
                    definition.getId() + " expired VFX must render no pixels");
        }
        AbilityDefinition shield = AbilityDefinition.createAll().stream()
                .filter(definition -> definition.getId().equals("holy_shield"))
                .findFirst().orElseThrow();
        AbilityVisualEffect persistent = new AbilityVisualEffect(shield,
                0, 0, 0, 0, 90, new Color(255, 224, 143), 6.0);
        persistent.update(2.0);
        BufferedImage heldShield = render(persistent);
        persistent.update(0.2);
        SkillEffectAtlas.SkillAnimation barrier = SkillEffectAtlas.getAnimation("holy_shield", "action");
        if (barrier == null) {
            require(nontransparentPixels(heldShield) > 0 && nontransparentPixels(render(persistent)) > 0,
                    "Procedural Divine Barrier must stay visible through its defense duration");
        } else if (barrier.isLooping()) {
            require(nontransparentPixels(heldShield) > 0 && nontransparentPixels(render(persistent)) > 0,
                    "Reviewed looping barrier remains attached through its defense duration");
        }
        persistent.update(5.0);
        require(persistent.isExpired() && nontransparentPixels(render(persistent)) == 0,
                "Sustained shields must disappear when their full lifetime ends");
    }

    private static boolean hasBodyTrack(AbilityDefinition definition) {
        if (definition.isPassive()) return false;
        if (SkillEffectAtlas.getAnimation(definition.getId(), "buildup") != null) return true;
        return !definition.getId().equals("flame_burst") && !definition.getId().equals("ice_shard")
                && SkillEffectAtlas.getAnimation(definition.getId(), "action") != null;
    }

    /** Check actual authored frame times; a partially imported sheet need not draw in every phase. */
    private static void verifyReferenceTracks(AbilityDefinition definition) {
        if (definition.isPassive()) return; // GameLogic draws passive tracks separately.
        double duration = AbilityAnimationTiming.duration(definition);
        double release = AbilityAnimationTiming.releaseProgress(definition) * duration;
        for (String track : List.of("buildup", "action", "travel")) {
            SkillEffectAtlas.SkillAnimation animation = SkillEffectAtlas.getAnimation(definition.getId(), track);
            if (animation == null) continue;
            boolean projectile = definition.getId().equals("flame_burst") || definition.getId().equals("ice_shard");
            if (track.equals("travel") && !projectile) continue;
            if (track.equals("action") && projectile) continue;
            double elapsed = animation.getStartDelaySeconds() + (track.equals("action") ? release : 0);
            int visibleSamples = 0;
            for (SkillEffectAtlas.FrameMetadata frame : animation.getFrames()) {
                double sample = elapsed + frame.durationSeconds() * 0.5;
                double lifetime = Math.max(duration, sample + 0.5);
                AbilityVisualEffect effect = new AbilityVisualEffect(definition, -60.25, 0.75, 55.5, -20.25,
                        72, Color.ORANGE, lifetime);
                if (track.equals("travel")) effect.setProjectilePosition(30, 0, 0);
                effect.freezeImpactOrigin(55.5, -20.25);
                effect.update(sample);
                visibleSamples += nontransparentPixels(render(effect)) > 0 ? 1 : 0;
                elapsed += frame.durationSeconds();
            }
            require(visibleSamples > 0, definition.getId() + " / " + track
                    + " reviewed source sequence must render at an authored frame time");
        }
    }

    private static void verifyIntegratedRendering() throws Exception {
        BufferedImage canvas = new BufferedImage(384, 240, BufferedImage.TYPE_INT_ARGB);
        for (int index = 0; index < 4; index++) {
            GameLogic logic = new GameLogic();
            logic.selectCharacter(index);
            logic.startGame();
            logic.setSoundEnabled(false);
            logic.update(RunEntranceAnimation.DURATION);
            RpgAbility[] loadout = logic.getAbilityManager().getEquippedAbilities();
            int casts = 0;
            for (int frame = 0; frame < 360; frame++) {
                if (frame >= 5 && frame <= 185 && (frame - 5) % 60 == 0) {
                    logic.applyAbilityEffect(loadout[casts++].getDefinition());
                    require(!visuals(logic).isEmpty(), "Equipped cast must create live VFX");
                }
                logic.setKeyPressed("right", frame % 120 < 80);
                logic.setKeyPressed("down", frame % 120 >= 40 && frame % 120 < 80);
                logic.update(1.0 / 60.0);
                Graphics2D graphics = canvas.createGraphics();
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                logic.drawAbilityGroundEffects(graphics, 192, 120);
                logic.drawEntities(graphics, 192, 120);
                logic.drawAbilityBursts(graphics, 192, 120);
                graphics.dispose();
                require(Double.isFinite(logic.getPlayerWorldX())
                        && Double.isFinite(logic.getPlayerWorldY()), "Live movement must stay finite");
                for (AbilityVisualEffect live : visuals(logic)) {
                    require(!live.isExpired(), "Expired VFX must be removed from gameplay");
                }
            }
            require(casts == 4, "Render smoke must cover every equipped skill");
            require(nontransparentPixels(canvas) > 0, "Integrated rendering must produce pixels");
        }
    }

    private static void verifySkillCannotBeInterruptedByAutoAttack() throws Exception {
        GameLogic logic = new GameLogic();
        logic.selectCharacter(3);
        logic.startGame();
        logic.setSoundEnabled(false);
        logic.update(RunEntranceAnimation.DURATION);
        Player current = (Player) field(logic, "player");
        AbilityDefinition storm = logic.getAbilityManager().getAbilityById("elemental_storm")
                .getDefinition();
        logic.applyAbilityEffect(storm);
        for (int frame = 0; frame < 42; frame++) logic.update(1.0 / 60.0);
        require(current.isSkillAnimationActive(),
                "A ready automatic staff shot must preserve the ongoing storm cast");
        require(number(current, "attackAnimationTime") > 0.2,
                "Long skill animation must reach its own recovery instead of being reset by autofire");
        for (int frame = 0; frame < 90; frame++) logic.update(1.0 / 60.0);
        require(!current.isSkillAnimationActive(), "Skill animation must eventually release autofire");
    }

    @SuppressWarnings("unchecked")
    private static void verifySkillTakeoverCancelsPendingMelee() throws Exception {
        for (int index = 0; index < 2; index++) {
            GameLogic logic = new GameLogic();
            logic.selectCharacter(index);
            logic.startGame();
            logic.setSoundEnabled(false);
            logic.update(RunEntranceAnimation.DURATION);
            Enemy target = new TemplateEnemy(80, 0);
            ((List<Enemy>) field(logic, "enemies")).add(target);
            logic.update(0.0);
            require(field(logic, "pendingMeleeAttack") != null,
                    "Fixture must start an automatic held melee swing");
            double healthBefore = target.getHealth();
            AbilityDefinition skill = logic.getAbilityManager().getAbilityById(
                    index == 0 ? "heavy_slash" : "twin_fang").getDefinition();
            logic.applyAbilityEffect(skill);
            require(field(logic, "pendingMeleeAttack") == null,
                    "Skill takeover must cancel the old automatic melee hit");
            for (int frame = 0; frame < 12; frame++) logic.update(1.0 / 60.0);
            near(target.getHealth(), healthBefore,
                    "Interrupted automatic swing must not damage a target during skill anticipation");
        }
    }

    private interface DrawOperation { void draw(Graphics2D graphics); }

    private static void assertGraphicsIsolation(DrawOperation operation, String context) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.translate(2.25, 4.5);
        graphics.scale(0.8, 0.9);
        graphics.setClip(new Rectangle(7, 9, 225, 210));
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.63f));
        graphics.setStroke(new BasicStroke(4.25f));
        graphics.setColor(new Color(83, 71, 115));
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        AffineTransform transform = graphics.getTransform();
        Rectangle clip = graphics.getClipBounds();
        var composite = graphics.getComposite();
        var stroke = graphics.getStroke();
        Color color = graphics.getColor();
        RenderingHints hints = graphics.getRenderingHints();
        operation.draw(graphics);
        require(transform.equals(graphics.getTransform()) && clip.equals(graphics.getClipBounds())
                && composite.equals(graphics.getComposite()) && stroke.equals(graphics.getStroke())
                && color.equals(graphics.getColor()) && hints.equals(graphics.getRenderingHints()),
                context + " leaked graphics state into later draws");
        graphics.dispose();
    }

    private static BufferedImage render(Player player) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(2, 2);
        player.draw(graphics, 64, 56);
        graphics.dispose();
        return image;
    }

    private static BufferedImage render(AbilityVisualEffect effect) {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        effect.draw(graphics, 256, 256, 0, 0);
        graphics.dispose();
        return image;
    }

    private static AbilityVisualEffect effect(AbilityDefinition definition) {
        return new AbilityVisualEffect(definition, -60.25, 0.75, 55.5, -20.25,
                72, new Color(180, 160, 255), AbilityAnimationTiming.duration(definition));
    }

    private static void advance(Player player, double elapsed, int steps) {
        for (int step = 0; step < steps; step++) player.update(elapsed / steps);
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private static int nontransparentPixels(BufferedImage image) {
        int count = 0;
        for (int pixel : pixels(image)) if ((pixel >>> 24) != 0) count++;
        return count;
    }

    private static void identical(BufferedImage a, BufferedImage b, String context) {
        require(Arrays.equals(pixels(a), pixels(b)), context);
    }

    private static void near(double actual, double expected, String context) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) < 0.000001,
                context + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static double number(Object instance, String name) throws Exception {
        return ((Number) field(instance, name)).doubleValue();
    }

    private static Object field(Object instance, String name) throws Exception {
        for (Class<?> owner = instance.getClass(); owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    @SuppressWarnings("unchecked")
    private static List<AbilityVisualEffect> visuals(GameLogic logic) throws Exception {
        return (List<AbilityVisualEffect>) field(logic, "abilityVisualEffects");
    }

    private static Player player(int index) {
        return switch (index) {
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            default -> new Character_Eumann();
        };
    }
}
