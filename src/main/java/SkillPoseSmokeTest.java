import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Regression checks for skill pose continuity, source movement and left/right grips. */
public final class SkillPoseSmokeTest {
    private static final String[][] SKILLS = {
        {"heavy_slash", "shield_bash", "earth_shatter", "knights_wrath"},
        {"shadow_strike", "shadow_step", "death_mark", "twin_fang"},
        {"heal", "holy_bolt", "holy_shield", "divine_light"},
        {"flame_burst", "ice_shard", "lightning_strike", "elemental_storm"}
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        for (int character = 0; character < SKILLS.length; character++) {
            for (int side : new int[] {-1, 1}) {
                for (String id : SKILLS[character]) {
                    verifySkill(character, side, ability(id));
                }
            }
        }
        verifyRisingStrike();
        verifyShieldBash();
        System.out.println("Skill poses passed: all 16 skills, both facings, grips, movement and interruption continuity");
    }

    private static void verifySkill(int character, int side, AbilityDefinition skill) throws Exception {
        Player player = player(character);
        player.faceToward(side * 100.0, 0.0);
        double duration = AbilityAnimationTiming.duration(skill);
        double[] before = pose(player);
        player.playAttackAnimation(skill, duration);
        samePose(before, pose(player), skill.getId() + " initial preparation");
        BufferedImage idle = render(player);
        boolean visiblyMoved = false;
        double previousX = player.getSkillSourceWorldX();
        double previousY = player.getSkillSourceWorldY();
        for (int frame = 1; frame <= 240; frame++) {
            player.update(duration / 240.0);
            double x = player.getSkillSourceWorldX();
            double y = player.getSkillSourceWorldY();
            require(Double.isFinite(x) && Double.isFinite(y)
                    && Double.isFinite(player.getSkillSourceRotation()), skill.getId() + " finite source");
            require(Math.hypot(x - previousX, y - previousY) < 9.0,
                    skill.getId() + " source snapped at frame " + frame + " facing " + side);
            previousX = x;
            previousY = y;
            if (frame == 96) {
                visiblyMoved = !Arrays.equals(pixels(idle), pixels(render(player)));
            }
        }
        player.update(1e-8);
        require(!player.isSkillAnimationActive(), skill.getId() + " must recover");
        require(visiblyMoved, skill.getId() + " needs actual body or weapon motion");

        player.playAttackAnimation(skill, duration);
        player.update(duration * 0.57);
        before = pose(player);
        player.playAttackAnimation(skill, duration);
        samePose(before, pose(player), skill.getId() + " interrupted recovery");

        Player coarse = player(character);
        Player fine = player(character);
        for (Player moving : List.of(coarse, fine)) {
            moving.faceToward(side * 100.0, 0.0);
            moving.setKeyPressed(side == 1 ? "right" : "left", true);
            moving.playAttackAnimation(skill, duration);
        }
        double elapsed = duration * 0.417;
        coarse.update(elapsed);
        for (int part = 0; part < 73; part++) fine.update(elapsed / 73.0);
        near(coarse.getWorldX(), side * 200.0 * elapsed, skill.getId() + " walking must continue");
        samePose(pose(coarse), pose(fine), skill.getId() + " frame-rate independent pose");
        near(coarse.getSkillSourceWorldX(), fine.getSkillSourceWorldX(), skill.getId() + " source x timing");
        near(coarse.getSkillSourceWorldY(), fine.getSkillSourceWorldY(), skill.getId() + " source y timing");
        require(Arrays.equals(pixels(render(coarse)), pixels(render(fine))),
                skill.getId() + " render changed with update partitioning");
    }

    private static void verifyRisingStrike() {
        AbilityDefinition skill = ability("earth_shatter");
        for (int side : new int[] {-1, 1}) {
            Player player = new Character_Eumann();
            player.faceToward(side * 100.0, 0.0);
            double duration = AbilityAnimationTiming.duration(skill);
            double release = AbilityAnimationTiming.hitProgress(skill)[0];
            player.playAttackAnimation(skill, duration);
            player.update(duration * release * 0.46);
            double windupY = player.getSkillSourceWorldY();
            player.update(duration * release * 0.54);
            require(player.getSkillSourceWorldY() < windupY - 12.0,
                    "Rising Strike must move the physical sword upward facing " + side);
        }
    }

    private static void verifyShieldBash() throws Exception {
        AbilityDefinition skill = ability("shield_bash");
        for (int side : new int[] {-1, 1}) {
            Player player = new Character_Eumann();
            player.faceToward(side * 100.0, 0.0);
            double[] idle = weaponPose(player, true);
            player.playAttackAnimation(skill, AbilityAnimationTiming.duration(skill));
            player.update(AbilityAnimationTiming.duration(skill) * AbilityAnimationTiming.hitProgress(skill)[0]);
            double[] impact = weaponPose(player, true);
            require((impact[0] - idle[0]) * side > 8.0,
                    "Shield Bash must thrust the actual offhand shield facing " + side);
        }
    }

    private static double[] pose(Player player) throws Exception {
        double[] primary = weaponPose(player, false);
        double[] offhand = weaponPose(player, true);
        Method transform = Player.class.getDeclaredMethod("characterTransform", int.class, int.class);
        transform.setAccessible(true);
        double[] matrix = new double[6];
        ((AffineTransform) transform.invoke(player, 0, 0)).getMatrix(matrix);
        double[] result = new double[12];
        System.arraycopy(primary, 0, result, 0, 3);
        System.arraycopy(offhand, 0, result, 3, 3);
        System.arraycopy(matrix, 0, result, 6, 6);
        return result;
    }

    private static double[] weaponPose(Player player, boolean offhand) throws Exception {
        Method method = Player.class.getDeclaredMethod("animatedWeaponPose", boolean.class, int.class, int.class);
        method.setAccessible(true);
        Object pose = method.invoke(player, offhand, 64, 64);
        String[] fields = {"weaponX", "weaponY", "weaponRotation"};
        double[] result = new double[3];
        for (int i = 0; i < fields.length; i++) {
            Field field = pose.getClass().getDeclaredField(fields[i]);
            field.setAccessible(true);
            result[i] = field.getDouble(pose);
        }
        return result;
    }

    private static void samePose(double[] expected, double[] actual, String message) {
        for (int i = 0; i < expected.length; i++) {
            double difference = i == 2 || i == 5
                    ? Math.IEEEremainder(actual[i] - expected[i], Math.PI * 2.0) : actual[i] - expected[i];
            near(difference, 0.0, message + " component " + i);
        }
    }

    private static BufferedImage render(Player player) {
        BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        player.draw(graphics, 80, 80);
        graphics.dispose();
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private static AbilityDefinition ability(String id) {
        return AbilityDefinition.createAll().stream().filter(skill -> skill.getId().equals(id))
                .findFirst().orElseThrow();
    }

    private static Player player(int index) {
        return switch (index) {
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            default -> new Character_Eumann();
        };
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1e-7, message + ": " + actual + " vs " + expected);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
