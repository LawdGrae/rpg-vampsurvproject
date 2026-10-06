import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Verifies the visual grip and actual attack origin stay attached to the player. */
public class WeaponAttachmentSmokeTest {
    private static final String[] DIRECTIONS = {"right", "left", "up", "down"};
    private static final int[][] VECTORS = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    public static void main(String[] args) throws Exception {
        verifyHeldEquipment();
        verifyAttackPivots();
        verifyPreviewMatchesGameplay();
        verifyFirstStaffShotOrigin();
        verifySkillCastOrigin();
        verifyMeleeWeaponsStayHeld();
        System.out.println("Weapon attachment, cast-origin and melee smoke checks passed");
    }

    private static void verifyHeldEquipment() throws Exception {
        for (int characterIndex = 0; characterIndex < 4; characterIndex++) {
            Player player = createPlayer(characterIndex);
            BufferedImage primary = (BufferedImage) field(player, "heldPrimarySprite");
            BufferedImage offhand = (BufferedImage) field(player, "heldOffhandSprite");
            if (primary == null || (characterIndex <= 1 && offhand == null)) {
                throw new IllegalStateException("Missing held equipment for character " + characterIndex);
            }
            if (characterIndex == 1) {
                int pairWidth = ResourceLoader.loadImage(
                        "/main/resources/weapons/twin_daggers.png").getWidth();
                if (primary == offhand || primary.getWidth() >= pairWidth
                        || offhand.getWidth() >= pairWidth) {
                    throw new IllegalStateException("Twin daggers must be separate images held by two hands");
                }
            }
        }
    }

    private static void verifyAttackPivots() throws Exception {
        Method poseMethod = Player.class.getDeclaredMethod("getWeaponPose",
                boolean.class, int.class, int.class);
        poseMethod.setAccessible(true);
        for (int characterIndex = 0; characterIndex < 4; characterIndex++) {
            for (String direction : DIRECTIONS) {
                Player player = createPlayer(characterIndex);
                player.setKeyPressed(direction, true);
                player.update(0.0);
                player.setKeyPressed(direction, false);
                int weaponCount = characterIndex <= 1 ? 2 : 1;
                for (int weaponIndex = 0; weaponIndex < weaponCount; weaponIndex++) {
                    for (double progress : new double[]{0.0, 0.18, 0.48, 0.72, 0.88, 1.0}) {
                        Object pose = poseMethod.invoke(player, weaponIndex == 1, 64, 64);
                        Method animate = pose.getClass().getDeclaredMethod("applyAttackProgress",
                                double.class);
                        animate.setAccessible(true);
                        animate.invoke(pose, progress);
                        String context = "Character " + characterIndex + " facing " + direction
                                + ", weapon " + weaponIndex + ", attack " + progress;
                        assertNear(numberField(pose, "weaponX"), numberField(pose, "handAnchorX"),
                                context + ": horizontal grip must remain on the hand");
                        assertNear(numberField(pose, "weaponY"), numberField(pose, "handAnchorY"),
                                context + ": vertical grip must remain on the hand");
                    }
                }
                double castX = player.getWeaponCastWorldX();
                double castY = player.getWeaponCastWorldY();
                player.moveWorld(123.5, -84.75);
                assertNear(player.getWeaponCastWorldX() - castX, 123.5,
                        "Cast origin must follow horizontal world movement");
                assertNear(player.getWeaponCastWorldY() - castY, -84.75,
                        "Cast origin must follow vertical world movement");
            }
        }
    }

    private static void verifyPreviewMatchesGameplay() {
        for (int characterIndex = 0; characterIndex < 4; characterIndex++) {
            for (String direction : DIRECTIONS) {
                for (double visualPhase : new double[]{0.0, 0.19, 0.51}) {
                    Player player = createPlayer(characterIndex);
                    player.setKeyPressed(direction, true);
                    player.update(0.0);
                    player.setKeyPressed(direction, false);
                    player.update(visualPhase);
                    BufferedImage gameplay = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gameplayGraphics = gameplay.createGraphics();
                    gameplayGraphics.scale(2.0, 2.0);
                    player.draw(gameplayGraphics, 64, 64);
                    gameplayGraphics.dispose();
                    BufferedImage preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D previewGraphics = preview.createGraphics();
                    player.drawPreview(previewGraphics, new Rectangle(64, 64, 128, 128));
                    previewGraphics.dispose();
                    // Compare the character and held equipment, above the gameplay health bar.
                    for (int y = 0; y < 192; y++) {
                        for (int x = 0; x < 256; x++) {
                            if (gameplay.getRGB(x, y) != preview.getRGB(x, y)) {
                                throw new IllegalStateException("Selection equipment differs from gameplay: "
                                        + "character " + characterIndex + " facing " + direction
                                        + " at animation time " + visualPhase
                                        + " at pixel " + x + "," + y);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void verifyFirstStaffShotOrigin() throws Exception {
        for (int characterIndex = 2; characterIndex < 4; characterIndex++) {
            for (int[] direction : VECTORS) {
                GameLogic logic = new GameLogic();
                logic.selectCharacter(characterIndex);
                logic.startGame();
                Player player = (Player) field(logic, "player");
                // Force an aim change on the very first shot, with no projectile travel.
                player.faceToward(-direction[0] * 200.0, -direction[1] * 200.0);
                enemies(logic).add(new TemplateEnemy(direction[0] * 200.0,
                        direction[1] * 200.0));
                logic.update(0.0);
                List<Projectile> shots = projectiles(logic);
                if (shots.size() != 1) {
                    throw new IllegalStateException("Staff must emit one first-frame shot");
                }
                assertNear(shots.get(0).getWorldX(), player.getWeaponCastWorldX(),
                        "First staff shot must start at the newly aimed staff head (x)");
                assertNear(shots.get(0).getWorldY(), player.getWeaponCastWorldY(),
                        "First staff shot must start at the newly aimed staff head (y)");
            }
        }
    }

    private static void verifyMeleeWeaponsStayHeld() throws Exception {
        for (int characterIndex = 0; characterIndex < 2; characterIndex++) {
            for (int[] direction : VECTORS) {
                GameLogic logic = new GameLogic();
                logic.selectCharacter(characterIndex);
                logic.startGame();
                enemies(logic).add(new TemplateEnemy(direction[0] * 64.0,
                        direction[1] * 64.0));
                logic.update(0.0);
                if (!projectiles(logic).isEmpty()) {
                    throw new IllegalStateException("Sword and dagger users must keep their weapons held");
                }
                if (field(logic, "pendingMeleeAttack") == null) {
                    throw new IllegalStateException("A nearby target must start a held melee swing");
                }
            }
        }
    }

    private static void verifySkillCastOrigin() throws Exception {
        String[] skills = {"holy_bolt", "ice_shard"};
        for (int characterIndex = 2; characterIndex < 4; characterIndex++) {
            for (int[] direction : VECTORS) {
                GameLogic logic = new GameLogic();
                logic.selectCharacter(characterIndex);
                logic.startGame();
                Player player = (Player) field(logic, "player");
                player.faceToward(-direction[0] * 200.0, -direction[1] * 200.0);
                player.playAttackAnimation(null, 0.4);
                player.update(0.4 * 0.48);
                enemies(logic).add(new TemplateEnemy(direction[0] * 200.0,
                        direction[1] * 200.0));
                AbilityDefinition definition = logic.getAbilityManager()
                        .getAbilityById(skills[characterIndex - 2]).getDefinition();
                logic.applyAbilityEffect(definition);
                List<?> visuals = (List<?>) field(logic, "abilityVisualEffects");
                if (visuals.size() != 1) {
                    throw new IllegalStateException("Staff skill must create its cast visual");
                }
                assertNear(numberField(visuals.get(0), "startX"), player.getWeaponCastWorldX(),
                        "A skill interrupting a swing must start at the new staff pose (x)");
                assertNear(numberField(visuals.get(0), "startY"), player.getWeaponCastWorldY(),
                        "A skill interrupting a swing must start at the new staff pose (y)");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Enemy> enemies(GameLogic logic) throws Exception {
        return (List<Enemy>) field(logic, "enemies");
    }

    @SuppressWarnings("unchecked")
    private static List<Projectile> projectiles(GameLogic logic) throws Exception {
        return (List<Projectile>) field(logic, "projectiles");
    }

    private static Object field(Object instance, String name) throws Exception {
        Class<?> owner = instance.getClass();
        while (owner != null) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                owner = owner.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static double numberField(Object instance, String name) throws Exception {
        return ((Number) field(instance, name)).doubleValue();
    }

    private static void assertNear(double actual, double expected, String message) {
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 0.000001) {
            throw new IllegalStateException(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static Player createPlayer(int characterIndex) {
        return switch (characterIndex) {
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            default -> new Character_Eumann();
        };
    }
}
