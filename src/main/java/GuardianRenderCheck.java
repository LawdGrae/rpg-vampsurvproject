import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Headless Guardian attachment, timeline and draw-state checks with visual evidence. */
public class GuardianRenderCheck {
    private static final String[] DIRECTIONS = {"right", "left", "up", "down"};
    private static final String[] POSES = {"Idle", "Walk 0", "Walk 1", "Walk 2",
            "Bash windup", "Bash impact", "Bash recovery", "Fortress hold"};
    private static final String[] SKILLS = {"shield_fortress", "iron_charge", "earthbreaker", "guardians_roar"};
    private static final Color BACKGROUND = new Color(18, 23, 30);
    private static final Color GROUND = new Color(31, 38, 34);
    private static final Color TEXT = new Color(236, 221, 183);
    private static final int POSE_WIDTH = 218, POSE_HEIGHT = 230;
    private static final int SCENE_WIDTH = 430, SCENE_HEIGHT = 335;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File directory = new File(args.length > 0 ? args[0] : "out/guardian-check");
        Files.createDirectories(directory.toPath());
        verifyAssetAndLifecycle();
        renderPoseSheet(directory, false);
        renderPoseSheet(directory, true);
        renderSkillSheet(directory);
        System.out.println("Guardian PNG attachment, 32 directional poses, four skill timelines, "
                + "draw isolation and effect lifecycles passed: " + directory.getAbsolutePath());
    }

    private static void verifyAssetAndLifecycle() {
        Player first = new Character_Sir_Rakki();
        Player second = new Character_Sir_Rakki();
        require("greatshield".equals(first.getDefaultWeaponStyle()), "Guardian must equip the greatshield");
        require(first.getPrimaryWeaponSprite() != null
                && first.getPrimaryWeaponSprite() == second.getPrimaryWeaponSprite(),
                "Guardian must use a cached PNG instead of loading it for each render");
        BufferedImage asset = ResourceLoader.loadImage("/main/resources/weapons/aegis_greatshield.png");
        boolean transparent = false, opaque = false;
        for (int y = 0; y < asset.getHeight(); y++) for (int x = 0; x < asset.getWidth(); x++) {
            int alpha = asset.getRGB(x, y) >>> 24;
            transparent |= alpha == 0;
            opaque |= alpha > 220;
        }
        require(transparent && opaque, "Aegis asset must have an opaque shield and transparent background");
        for (String id : SKILLS) {
            AbilityDefinition definition = definition(id);
            double life = lifetime(definition);
            AbilityVisualEffect effect = new AbilityVisualEffect(definition, 15, 5,
                    150, 0, Math.max(100, definition.getRadius()), Color.ORANGE, life);
            effect.setCasterPosition(0, 0);
            effect.setCastOrigin(15, 5);
            effect.update(AbilityAnimationTiming.hitTimes(definition)[0] + 0.13);
            BufferedImage image = renderEffect(effect);
            require(nontransparentPixels(image) > 0, id + " must produce visible action effects");
            require(Arrays.equals(pixels(image), pixels(renderEffect(effect))), id + " draw must be deterministic");
            effect.update(life + 1);
            require(effect.isExpired() && nontransparentPixels(renderEffect(effect)) == 0,
                    id + " must cleanly expire");
        }
    }

    private static void renderPoseSheet(File directory, boolean anchors) throws Exception {
        BufferedImage sheet = new BufferedImage(POSE_WIDTH * POSES.length,
                POSE_HEIGHT * DIRECTIONS.length + 42, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(BACKGROUND); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(TEXT); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString(anchors ? "Measured shield anchors: cyan grip, gold physical rim"
                : "Sir Rakki / Guardian: actual PNG shield in all four facings", 12, 29);
        for (int row = 0; row < DIRECTIONS.length; row++) for (int col = 0; col < POSES.length; col++) {
            Player player = posePlayer(DIRECTIONS[row], col);
            BufferedImage playerImage = renderPlayer(player);
            verifyRenderState(player, playerImage);
            Point2D grip = grip(player);
            double rimX = player.getWeaponCastWorldX() - player.getWorldX();
            double rimY = player.getWeaponCastWorldY() - player.getWorldY();
            require(Double.isFinite(rimX) && Double.isFinite(rimY)
                    && Math.hypot(rimX - grip.getX(), rimY - grip.getY()) < 45,
                    DIRECTIONS[row] + " " + POSES[col] + " must keep the rim near the connected grip");
            require(hasPixelNear(playerImage, 90 + rimX, 80 + rimY, 7),
                    DIRECTIONS[row] + " " + POSES[col] + " actual cast point must meet visible equipment");
            int left = col * POSE_WIDTH, top = 42 + row * POSE_HEIGHT;
            g.setColor(GROUND); g.fillRect(left + 2, top + 2, POSE_WIDTH - 4, POSE_HEIGHT - 4);
            g.setColor(TEXT); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString(DIRECTIONS[row] + " / " + POSES[col], left + 9, top + 22);
            Graphics2D scene = (Graphics2D) g.create();
            scene.translate(left + POSE_WIDTH / 2.0, top + 125);
            scene.scale(2.65, 2.65);
            scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            player.draw(scene, 0, 0);
            if (anchors) {
                scene.setColor(new Color(95, 230, 234));
                scene.fill(new java.awt.geom.Ellipse2D.Double(grip.getX() - 1, grip.getY() - 1, 2, 2));
                scene.setColor(new Color(255, 226, 113));
                scene.fill(new java.awt.geom.Ellipse2D.Double(rimX - 1, rimY - 1, 2, 2));
            }
            scene.dispose();
            if (anchors) {
                g.setColor(TEXT); g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                g.drawString(String.format("grip %.1f,%.1f", grip.getX(), grip.getY()), left + 9, top + 203);
                g.drawString(String.format("rim  %.1f,%.1f", rimX, rimY), left + 9, top + 218);
            }
        }
        g.dispose();
        write(sheet, new File(directory, anchors ? "guardian-attachment-anchors.png" : "guardian-poses.png"));
    }

    private static Player posePlayer(String direction, int pose) {
        Player player = facing(direction);
        if (pose >= 1 && pose <= 3) {
            player.setKeyPressed(direction, true);
            advance(player, walkTime(player.animationSpeed, pose - 1));
        } else if (pose >= 4 && pose <= 6) {
            player.playAttackAnimation(null, 0.65);
            advance(player, 0.65 * new double[] {0.2, 0.52, 0.88}[pose - 4]);
        } else if (pose == 7) {
            player.setGuardianFortressRemaining(5);
            player.update(0.12);
        } else player.update(0.12);
        return player;
    }

    private static void renderSkillSheet(File directory) throws Exception {
        int samples = 6;
        BufferedImage sheet = new BufferedImage(SCENE_WIDTH * samples, SCENE_HEIGHT * SKILLS.length,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int row = 0; row < SKILLS.length; row++) {
            AbilityDefinition definition = definition(SKILLS[row]);
            double duration = AbilityAnimationTiming.duration(definition);
            double hit = AbilityAnimationTiming.hitTimes(definition)[0];
            double life = lifetime(definition);
            double[] times = {duration * 0.18, Math.max(0, hit - 0.02), hit + 0.03,
                    hit + 0.18, duration * 0.92, life - 0.12};
            for (int col = 0; col < samples; col++) {
                Player player = facing("right");
                player.playAttackAnimation(definition, duration);
                double startX = player.getWeaponCastWorldX(), startY = player.getWeaponCastWorldY();
                AbilityVisualEffect effect = new AbilityVisualEffect(definition, startX, startY,
                        startX + 160, startY, Math.max(100, definition.getRadius()), Color.ORANGE, life);
                double elapsed = 0;
                boolean impact = false;
                while (elapsed < times[col] - 1e-10) {
                    double dt = Math.min(1.0 / 120, times[col] - elapsed);
                    if (!impact && elapsed < hit && elapsed + dt > hit) dt = hit - elapsed;
                    player.update(dt);
                    elapsed += dt;
                    if (SKILLS[row].equals("iron_charge")) {
                        double motion = Math.max(0, Math.min(0.5, elapsed - hit));
                        double previous = Math.max(0, Math.min(0.5, elapsed - dt - hit));
                        player.applyGuardianChargeMovement((motion - previous) * 320, 0, dt);
                    }
                    if (!impact && elapsed >= hit - 1e-9) {
                        if (SKILLS[row].equals("earthbreaker")) {
                            effect.freezeImpactOrigin(player.getWeaponCastWorldX(), player.getWeaponCastWorldY());
                        } else if (SKILLS[row].equals("guardians_roar")) {
                            effect.freezeImpactOrigin(player.getWorldX(), player.getWorldY());
                        } else if (SKILLS[row].equals("shield_fortress")) {
                            player.setGuardianFortressRemaining(6);
                        }
                        impact = true;
                    }
                    effect.setCasterPosition(player.getWorldX(), player.getWorldY());
                    effect.setCastOrigin(player.getWeaponCastWorldX(), player.getWeaponCastWorldY());
                    effect.update(dt);
                }
                if (SKILLS[row].equals("shield_fortress") && col == 3) {
                    player.playGuardianBlock();
                    effect.flashBarrier(player.getWeaponCastWorldX(), player.getWeaponCastWorldY());
                }
                int left = col * SCENE_WIDTH, top = row * SCENE_HEIGHT;
                BufferedImage frame = skillFrame(player, effect, definition.getName(), times[col], col);
                verifyRenderState(player, renderPlayer(player));
                g.drawImage(frame, left, top, null);
                write(frame, new File(directory, SKILLS[row] + "-" + col + ".png"));
            }
        }
        g.dispose();
        write(sheet, new File(directory, "guardian-skills.png"));
    }

    private static BufferedImage skillFrame(Player player, AbilityVisualEffect effect,
            String name, double time, int sample) {
        BufferedImage result = new BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(GROUND); g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);
        g.setColor(new Color(57, 63, 45));
        for (int i = 0; i < 55; i++) g.fillRect((i * 67) % SCENE_WIDTH, 55 + (i * 43) % 260, 3, 2);
        g.setColor(TEXT); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        g.drawString(name + "  " + String.format("%.3fs", time), 12, 25);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.drawString(new String[] {"Prepare", "Windup / before hit", "Action / hit event",
                "Effect / contact", "Recovery", "End / fade"}[sample], 12, 46);
        Graphics2D scene = (Graphics2D) g.create();
        scene.clipRect(2, 53, SCENE_WIDTH - 4, SCENE_HEIGHT - 55);
        scene.translate(SCENE_WIDTH * 0.44, SCENE_HEIGHT * 0.6);
        double zoom = name.equals("Guardian's Roar") ? 0.58
                : name.equals("Earthbreaker") ? 0.73 : name.equals("Iron Charge") ? 1.05 : 1.6;
        scene.scale(zoom, zoom);
        if (effect.getLayer() == AbilityVisualEffect.Layer.GROUND) {
            effect.draw(scene, 0, 0, player.getWorldOffsetX(), player.getWorldOffsetY());
        }
        player.draw(scene, 0, 0);
        if (effect.getLayer() == AbilityVisualEffect.Layer.FRONT) {
            effect.draw(scene, 0, 0, player.getWorldOffsetX(), player.getWorldOffsetY());
        }
        scene.dispose();
        g.dispose();
        return result;
    }

    private static void verifyRenderState(Player player, BufferedImage image) throws Exception {
        double worldX = player.getWorldX(), worldY = player.getWorldY();
        double x = player.getWeaponCastWorldX(), y = player.getWeaponCastWorldY();
        double animation = number(player, "animationTime"), visual = number(player, "visualTime");
        double attack = number(player, "attackAnimationTime");
        require(Arrays.equals(pixels(image), pixels(renderPlayer(player))), "Rendering must not change a Guardian pose");
        near(player.getWorldX(), worldX, "Draw changed position x");
        near(player.getWorldY(), worldY, "Draw changed position y");
        near(player.getWeaponCastWorldX(), x, "Draw changed shield rim x");
        near(player.getWeaponCastWorldY(), y, "Draw changed shield rim y");
        near(number(player, "animationTime"), animation, "Draw changed gait clock");
        near(number(player, "visualTime"), visual, "Draw changed visual clock");
        near(number(player, "attackAnimationTime"), attack, "Draw changed attack clock");
    }

    private static Point2D grip(Player player) throws Exception {
        Method poseMethod = Player.class.getDeclaredMethod("animatedWeaponPose", boolean.class, int.class, int.class);
        poseMethod.setAccessible(true);
        Object pose = poseMethod.invoke(player, false, 64, 64);
        Point2D point = new Point2D.Double(number(pose, "weaponX"), number(pose, "weaponY"));
        Method transform = Player.class.getDeclaredMethod("characterTransform", int.class, int.class);
        transform.setAccessible(true);
        return ((AffineTransform) transform.invoke(player, 0, 0)).transform(point, null);
    }

    private static Player facing(String direction) {
        Player player = new Character_Sir_Rakki();
        player.faceToward(direction.equals("right") ? 100 : direction.equals("left") ? -100 : 0,
                direction.equals("down") ? 100 : direction.equals("up") ? -100 : 0);
        return player;
    }

    private static AbilityDefinition definition(String id) {
        return AbilityDefinition.createAll().stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    private static double lifetime(AbilityDefinition definition) {
        double duration = AbilityAnimationTiming.duration(definition);
        return switch (definition.getId()) {
            case "shield_fortress" -> AbilityAnimationTiming.hitTimes(definition)[0] + definition.getDuration();
            case "earthbreaker" -> duration + 0.65;
            case "guardians_roar" -> duration + 0.35;
            default -> duration;
        };
    }

    private static double walkTime(double speed, int column) {
        double target = (4.01 + column) / speed, low = 0, high = 3;
        for (int step = 0; step < 48; step++) {
            double time = (low + high) / 2;
            double gait = 1 / speed + 1.55 * (time - (1 - Math.exp(-12 * time)) / 12);
            if (gait < target) low = time; else high = time;
        }
        return (low + high) / 2;
    }

    private static void advance(Player player, double time) {
        while (time > 1e-10) {
            double dt = Math.min(time, 1.0 / 120);
            player.update(dt);
            time -= dt;
        }
    }

    private static BufferedImage renderPlayer(Player player) {
        BufferedImage image = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        player.draw(g, 90, 80);
        g.dispose();
        return image;
    }

    private static BufferedImage renderEffect(AbilityVisualEffect effect) {
        BufferedImage image = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); effect.draw(g, 320, 320, 0, 0); g.dispose();
        return image;
    }

    private static boolean hasPixelNear(BufferedImage image, double x, double y, int radius) {
        for (int py = (int) Math.round(y) - radius; py <= y + radius; py++) {
            for (int px = (int) Math.round(x) - radius; px <= x + radius; px++) {
                if (px >= 0 && py >= 0 && px < image.getWidth() && py < image.getHeight()
                        && (image.getRGB(px, py) >>> 24) > 80) return true;
            }
        }
        return false;
    }

    private static double number(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name); field.setAccessible(true);
                return ((Number) field.get(target)).doubleValue();
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static int[] pixels(BufferedImage image) { return ((DataBufferInt) image.getRaster().getDataBuffer()).getData(); }
    private static int nontransparentPixels(BufferedImage image) {
        int count = 0; for (int pixel : pixels(image)) if ((pixel >>> 24) != 0) count++; return count;
    }
    private static void near(double actual, double expected, String message) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-7, message);
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static void write(BufferedImage image, File output) throws Exception {
        require(ImageIO.write(image, "png", output), "PNG writer unavailable");
    }
}
