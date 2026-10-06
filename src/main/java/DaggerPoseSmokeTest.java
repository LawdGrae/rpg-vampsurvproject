import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Checks the real blade direction and temporal continuity of Haze's held daggers. */
public class DaggerPoseSmokeTest {
    private static final List<String> DIRECTIONS = List.of("down", "right", "up", "left");
    private static final double STEP = 1.0 / 240.0;
    private static final Method POSE;
    private static final Method BOUNDS;
    private static final Method GRIP_HAND;

    static {
        try {
            POSE = Player.class.getDeclaredMethod("animatedWeaponPose", boolean.class, int.class, int.class);
            BOUNDS = Player.class.getDeclaredMethod("visibleWeaponBounds", BufferedImage.class);
            GRIP_HAND = Player.class.getDeclaredMethod("gripHandPosition", boolean.class, int.class, int.class);
            POSE.setAccessible(true);
            BOUNDS.setAccessible(true);
            GRIP_HAND.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        verifyIdleAndWalkingBladeDirection();
        verifyTurnsAndAttackContinuity();
        verifyAttackTakeover();
        System.out.println("Dagger blade orientation, walking grips, turns and combo continuity passed");
    }

    private static void verifyIdleAndWalkingBladeDirection() throws Exception {
        for (String direction : DIRECTIONS) {
            Player player = facing(direction);
            for (int frame = 0; frame < 360; frame++) {
                if (frame == 120) player.setKeyPressed(direction, true);
                player.update(STEP);
                for (boolean offhand : new boolean[]{false, true}) {
                    Object pose = pose(player, offhand);
                    assertSourceWrist(pose, direction + " " + frame);
                    assertAttached(player, offhand, direction + " " + frame);
                    BufferedImage blade = (BufferedImage) field(player,
                            offhand ? "heldOffhandSprite" : "heldPrimarySprite");
                    Rectangle bounds = (Rectangle) BOUNDS.invoke(null, blade);
                    // Sample the point end of the actual source texture, not an assumed
                    // angle. The lower fifth contains the tips in these dagger assets.
                    double weightedX = 0.0;
                    double weightedY = 0.0;
                    double weight = 0.0;
                    for (int y = bounds.y + bounds.height * 4 / 5; y < bounds.y + bounds.height; y++) {
                        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                            int alpha = blade.getRGB(x, y) >>> 24;
                            weightedX += x * alpha;
                            weightedY += y * alpha;
                            weight += alpha;
                        }
                    }
                    require(weight > 0.0, "Dagger source must contain visible blade tips");
                    double tipX = weightedX / weight - bounds.x - bounds.width * number(pose, "weaponPivotX");
                    double tipY = weightedY / weight - bounds.y - bounds.height * number(pose, "weaponPivotY");
                    if ((boolean) field(pose, "flipX")) tipX = -tipX;
                    double angle = number(pose, "weaponRotation");
                    double screenTipY = (Math.sin(angle) * tipX + Math.cos(angle) * tipY)
                            * number(pose, "drawHeight") / bounds.height;
                    require(screenTipY < -number(pose, "drawHeight") * 0.35,
                            direction + " " + (offhand ? "offhand" : "main hand")
                                    + " blade must point above its grip, including walking: " + screenTipY);
                }
            }
        }
    }

    private static void verifyTurnsAndAttackContinuity() throws Exception {
        for (String direction : DIRECTIONS) {
            Player player = facing(direction);
            for (String turn : DIRECTIONS) {
                double[] before = angles(player);
                face(player, turn);
                assertAngleStep(before, angles(player), 0.08, "Turning must retain the current wrist angle");
                for (int frame = 0; frame < 72; frame++) {
                    before = angles(player);
                    player.update(STEP);
                    assertAngleStep(before, angles(player), 0.20, "A direction turn must blend smoothly");
                }
            }
            for (String id : List.of("auto", "twin_fang", "silent_execution")) {
                AbilityDefinition definition = id.equals("auto") ? null : ability(id);
                AutoFireWeapon weapon = new AutoFireWeapon();
                weapon.setAttackSprite(null, "daggers");
                double duration = definition == null ? weapon.getSwingDuration()
                        : AbilityAnimationTiming.duration(definition);
                double[] before = angles(player);
                player.playAttackAnimation(definition, duration);
                assertAngleStep(before, angles(player), 0.08, "Starting " + id + " must preserve the wrist pose");
                int count = (int) Math.ceil((duration + 0.12) / STEP);
                for (int frame = 0; frame < count; frame++) {
                    before = angles(player);
                    player.update(STEP);
                    double[] after = angles(player);
                    assertAngleStep(before, after, 0.20, direction + " " + id + " frame " + frame);
                    for (boolean offhand : new boolean[]{false, true}) assertAttached(player, offhand, id);
                }
            }
        }
    }

    private static void verifyAttackTakeover() throws Exception {
        for (String direction : DIRECTIONS) {
            Player player = facing(direction);
            AbilityDefinition twin = ability("twin_fang");
            player.playAttackAnimation(twin, AbilityAnimationTiming.duration(twin));
            player.update(0.29);
            double[] before = angles(player);
            player.playAttackAnimation(ability("silent_execution"), 0.90);
            assertAngleStep(before, angles(player), 0.000001,
                    "A skill interrupting a dagger swing must retain both wrist angles");
            BufferedImage first = render(player);
            require(Arrays.equals(first.getRGB(0, 0, 256, 256, null, 0, 256),
                    render(player).getRGB(0, 0, 256, 256, null, 0, 256)),
                    "Drawing a dagger pose must not advance its animation");
        }
    }

    private static BufferedImage render(Player player) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(2.0, 2.0);
        player.draw(graphics, 64, 64);
        graphics.dispose();
        return image;
    }

    private static AbilityDefinition ability(String id) {
        return AbilityDefinition.createAll().stream().filter(definition -> definition.getId().equals(id))
                .findFirst().orElseThrow();
    }

    private static Player facing(String direction) {
        Player player = new Character_Haze();
        face(player, direction);
        // Let the first turn settle before testing held blade orientation.
        player.update(0.6);
        return player;
    }

    private static void face(Player player, String direction) {
        player.setKeyPressed(direction, true);
        player.update(0.0);
        player.setKeyPressed(direction, false);
    }

    private static Object pose(Player player, boolean offhand) throws Exception {
        return POSE.invoke(player, offhand, 64, 64);
    }

    private static double[] angles(Player player) throws Exception {
        return new double[]{number(pose(player, false), "weaponRotation"),
                number(pose(player, true), "weaponRotation")};
    }

    private static void assertSourceWrist(Object pose, String context) throws Exception {
        require(Math.abs(number(pose, "weaponX") - number(pose, "handAnchorX")) < 0.000001
                        && Math.abs(number(pose, "weaponY") - number(pose, "handAnchorY")) < 0.000001,
                context + ": animated dagger handle must stay at the hand");
    }

    private static void assertAttached(Player player, boolean offhand, String context) throws Exception {
        Object pose = pose(player, offhand);
        // Skill arms move the source-frame wrist. Compare with the hand patch position
        // used by drawGripHands; its pixel snapping can differ by at most half a pixel.
        Point2D grip = (Point2D) GRIP_HAND.invoke(player, offhand, 64, 64);
        require(Math.abs(number(pose, "weaponX") - grip.getX()) <= 0.500001
                        && Math.abs(number(pose, "weaponY") - grip.getY()) <= 0.500001,
                context + ": dagger pivot must remain inside the actually rendered grip hand pixel");
    }

    private static void assertAngleStep(double[] before, double[] after, double maximum, String context) {
        for (int hand = 0; hand < before.length; hand++) {
            double delta = Math.atan2(Math.sin(after[hand] - before[hand]), Math.cos(after[hand] - before[hand]));
            require(Double.isFinite(delta) && Math.abs(delta) <= maximum,
                    context + ": hand " + hand + " jumped " + delta + " radians");
        }
    }

    private static Object field(Object object, String name) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static double number(Object object, String name) throws Exception {
        return ((Number) field(object, name)).doubleValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
