import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

/** Headless checks for the entrance timeline and its gameplay/input boundaries. */
public class RunEntranceSmokeTest {
    private static final double EPSILON = 1.0e-8;
    private static int assertions;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        checkFallAndLanding();
        checkTimePartitions();
        checkInvalidTimes();
        checkGraphicsState();
        for (int hero = 0; hero < 5; hero++) {
            checkGameplayFreeze(hero);
            checkPauseAndRestart(hero);
            checkGameplayRemainder(hero);
            checkPlayerDrawing(hero);
        }
        checkSpawnClock();
        System.out.println("Run entrance smoke checks passed (" + assertions + " assertions).");
    }

    private static RunEntranceAnimation animation() {
        RunEntranceAnimation animation = new RunEntranceAnimation();
        animation.start(460.0, new Color(255, 212, 132));
        return animation;
    }

    private static void checkFallAndLanding() {
        RunEntranceAnimation animation = animation();
        require(animation.isPlaying() && animation.isVisible(), "a fresh entrance is active");
        near(animation.getHeight(), 460.0, "initial drop height");
        near(animation.getAge(), 0.0, "initial entrance age");
        near(animation.getRemainingDuration(), RunEntranceAnimation.DURATION, "initial lock duration");
        double previousHeight = animation.getHeight();
        int landings = 0;
        for (int step = 1; step <= 72; step++) {
            if (animation.update(0.01)) landings++;
            double height = animation.getHeight();
            require(Double.isFinite(height) && height >= 0.0 && height <= previousHeight + EPSILON,
                    "height descends monotonically at step " + step);
            require(animation.getCompression() >= 0.0 && animation.getCompression() <= 1.0,
                    "compression remains normalized");
            if (step < 72) near(animation.getCompression(), 0.0, "no compression before contact");
            previousHeight = height;
        }
        near(animation.getHeight(), 0.0, "contact reaches the ground");
        require(landings == 1, "the fall emits exactly one landing event");
        require(!animation.update(0.0), "an update at contact cannot repeat the landing");
        require(!animation.update(0.08), "the landing recoil cannot repeat the landing");
        require(animation.getCompression() > 0.0, "contact produces a squash pose");
        require(animation.isPlaying(), "the landing pose keeps gameplay locked");
        animation.update(RunEntranceAnimation.DURATION - animation.getAge());
        require(!animation.isPlaying(), "gameplay unlocks at the end of the entrance");
        near(animation.getHeight(), 0.0, "finished height is neutral");
        near(animation.getCompression(), 0.0, "finished compression is neutral");
        near(animation.getRemainingDuration(), 0.0, "finished lock has no remaining duration");
        require(animation.isVisible(), "the ground effect can finish after gameplay unlocks");
        require(!animation.update(2.0), "finished updates cannot repeat the landing");
        require(!animation.isVisible(), "the ground effect eventually expires");

        RunEntranceAnimation accelerated = animation();
        accelerated.update(0.10);
        double firstDrop = 460.0 - accelerated.getHeight();
        double firstHeight = accelerated.getHeight();
        accelerated.update(0.10);
        require(firstHeight - accelerated.getHeight() > firstDrop,
                "equal time slices fall farther as the hero accelerates");

        RunEntranceAnimation coarse = animation();
        require(coarse.update(10.0), "a large step still emits its crossed landing");
        require(!coarse.update(10.0), "a large completed step emits no second landing");
        coarse.start(460.0, Color.CYAN);
        require(coarse.update(RunEntranceAnimation.FALL_DURATION), "restart rearms the landing event");
        coarse.reset();
        require(!coarse.isPlaying() && !coarse.isVisible(), "reset clears the entrance and effect");
        near(coarse.getHeight(), 0.0, "reset height");
        near(coarse.getCompression(), 0.0, "reset compression");
        require(!coarse.update(10.0), "reset cannot emit an old landing");
    }

    private static void checkTimePartitions() {
        double[] times = {0.0, 0.10, 0.28, 0.52, RunEntranceAnimation.FALL_DURATION,
                0.80, RunEntranceAnimation.DURATION, 1.10, RunEntranceAnimation.EFFECT_DURATION, 2.0};
        for (double time : times) {
            RunEntranceAnimation coarse = animation();
            RunEntranceAnimation fine = animation();
            int coarseLandings = coarse.update(time) ? 1 : 0;
            int fineLandings = 0;
            double elapsed = 0.0;
            while (elapsed + 0.01 < time - EPSILON) {
                if (fine.update(0.01)) fineLandings++;
                elapsed += 0.01;
            }
            if (fine.update(Math.max(0.0, time - elapsed))) fineLandings++;
            near(coarse.getAge(), fine.getAge(), "partition-independent age at " + time);
            near(coarse.getHeight(), fine.getHeight(), "partition-independent height at " + time);
            near(coarse.getCompression(), fine.getCompression(), "partition-independent compression at " + time);
            near(coarse.getRemainingDuration(), fine.getRemainingDuration(), "partition-independent lock at " + time);
            require(coarse.isPlaying() == fine.isPlaying(), "partition-independent gameplay state at " + time);
            require(coarse.isVisible() == fine.isVisible(), "partition-independent effect state at " + time);
            require(coarseLandings == fineLandings, "partition-independent landing count at " + time);
        }
    }

    private static void checkInvalidTimes() {
        RunEntranceAnimation animation = animation();
        animation.update(0.20);
        double age = animation.getAge();
        double height = animation.getHeight();
        double[] invalidTimes = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double dt : invalidTimes) {
            require(!animation.update(dt), "invalid time cannot emit a landing: " + dt);
            near(animation.getAge(), age, "invalid time does not change age");
            near(animation.getHeight(), height, "invalid time does not change height");
        }
        require(animation.update(Double.MAX_VALUE), "a huge finite time still crosses contact");
        require(Double.isFinite(animation.getAge()) && Double.isFinite(animation.getHeight())
                && Double.isFinite(animation.getCompression()), "huge time leaves finite drawing state");
        require(!animation.isPlaying() && !animation.isVisible(), "huge time completes the animation");
        for (double invalidHeight : invalidTimes) {
            animation.start(invalidHeight, Color.WHITE);
            require(Double.isFinite(animation.getHeight()) && animation.getHeight() >= 0.0,
                    "invalid starting height is sanitized: " + invalidHeight);
        }
    }

    private static void checkGraphicsState() {
        BufferedImage image = new BufferedImage(1000, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            configureSentinelGraphics(graphics);
            GraphicsState expected = new GraphicsState(graphics);
            RunEntranceAnimation animation = animation();
            double previous = 0.0;
            for (double age : new double[] {0.0, 0.28, 0.52, 0.72, 0.80, 1.04, 1.38, 2.0}) {
                animation.update(age - previous);
                animation.drawGround(graphics, 500, 600.0);
                expected.check(graphics, "ground effect at " + age);
                animation.drawFront(graphics, 500, 570, 28.16);
                expected.check(graphics, "front effect at " + age);
                previous = age;
            }
        } finally {
            graphics.dispose();
        }
        require(hasOpaquePixel(image), "the active entrance draws its effects");
    }

    private static void checkGameplayFreeze(int hero) throws Exception {
        GameLogic logic = game(hero);
        Player player = player(logic);
        player.setWorldCollision(null);
        AbilityManager manager = logic.getAbilityManager();
        manager.spendMana(25.0);
        RpgAbility ability = firstEquipped(manager);
        ability.cooldownRemaining = 3.0;
        double mana = manager.getMana();
        double health = player.getHealth();
        double x = logic.getPlayerWorldX(), y = logic.getPlayerWorldY();
        TemplateEnemy nearby = new TemplateEnemy(x + 75.0, y);
        enemies(logic).add(nearby);
        double enemyHealth = nearby.getHealth();
        logic.setKeyPressed("right", true);
        logic.setKeyPressed("down", true);
        for (int step = 0; step < 40; step++) {
            for (int slot = 0; slot < AbilityManager.EQUIPPED_SLOT_COUNT; slot++) logic.triggerAbility(slot);
            logic.update((RunEntranceAnimation.DURATION - 0.04) / 40.0);
            require(logic.isRunEntrancePlaying() && logic.isSkillActionLocked(), "hero " + hero + " is locked during entrance");
            near(logic.getGameTimer(), 0.0, "hero " + hero + " survival clock is frozen");
            near(logic.getPlayerWorldX(), x, "hero " + hero + " horizontal movement is frozen");
            near(logic.getPlayerWorldY(), y, "hero " + hero + " vertical movement is frozen");
            near(manager.getMana(), mana, "hero " + hero + " mana cannot spend or regenerate");
            near(ability.getCooldownRemaining(), 3.0, "hero " + hero + " cooldown cannot advance");
            near(player.getHealth(), health, "hero " + hero + " does not take contact damage");
            near(nearby.getWorldX(), x + 75.0, "hero " + hero + " enemy movement is frozen");
            near(nearby.getWorldY(), y, "hero " + hero + " enemy vertical movement is frozen");
            near(nearby.getHealth(), enemyHealth, "hero " + hero + " auto attacks cannot damage enemies");
            require(logic.getEnemyCount() == 1, "hero " + hero + " spawns remain frozen");
            require(listSize(logic, "projectiles") == 0 && field(logic, "pendingMeleeAttack") == null,
                    "hero " + hero + " cannot begin an auto attack");
            require(listSize(logic, "floatingTexts") == 0 && listSize(logic, "abilityVisualEffects") == 0,
                    "hero " + hero + " early skill input is ignored without feedback");
        }
        logic.update(logic.getRunEntranceAnimation().getRemainingDuration());
        require(!logic.isRunEntrancePlaying(), "hero " + hero + " unlocks after landing settles");
        near(logic.getGameTimer(), 0.0, "an exact entrance boundary has no gameplay time");
        logic.update(0.12);
        near(logic.getGameTimer(), 0.12, "gameplay time starts after unlock");
        require(logic.getPlayerWorldX() > x && logic.getPlayerWorldY() > y,
                "hero " + hero + " honors the movement keys held through entrance");
        require(manager.getMana() > mana, "hero " + hero + " mana regeneration resumes");
        near(ability.getCooldownRemaining(), 2.88, "hero " + hero + " cooldown resumes");
        logic.setKeyPressed("right", false);
        logic.setKeyPressed("down", false);
    }

    private static void checkPauseAndRestart(int hero) {
        GameLogic logic = game(hero);
        double startingHeight = logic.getRunEntranceAnimation().getHeight();
        logic.update(0.20);
        double age = logic.getRunEntranceAnimation().getAge();
        double height = logic.getRunEntranceAnimation().getHeight();
        logic.togglePause();
        require(logic.isPaused(), "pause is available during entrance");
        logic.update(5.0);
        near(logic.getRunEntranceAnimation().getAge(), age, "pause freezes entrance age");
        near(logic.getRunEntranceAnimation().getHeight(), height, "pause freezes entrance pose");
        near(logic.getGameTimer(), 0.0, "pause keeps survival time frozen");
        logic.resume();
        logic.update(0.15);
        near(logic.getRunEntranceAnimation().getAge(), age + 0.15, "resume continues entrance age");
        for (double dt : new double[] {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            logic.update(dt);
            near(logic.getRunEntranceAnimation().getAge(), age + 0.15, "invalid game time leaves entrance unchanged");
        }
        logic.startGame();
        require(logic.isRunEntrancePlaying(), "restart replays the entrance for hero " + hero);
        near(logic.getRunEntranceAnimation().getAge(), 0.0, "restart clears the old timeline");
        near(logic.getRunEntranceAnimation().getHeight(), startingHeight, "restart restores the drop height");
        near(logic.getGameTimer(), 0.0, "restart resets survival time");
        logic.showMainMenu();
        require(!logic.isRunEntrancePlaying() && !logic.getRunEntranceAnimation().isVisible(), "main menu cancels entrance");
        logic.update(5.0);
        require(!logic.getRunEntranceAnimation().isVisible(), "main menu cannot revive an old effect");
        logic.startGame();
        require(logic.isRunEntrancePlaying(), "starting from the menu replays entrance");
        logic.showCharacterSelection();
        require(!logic.isRunEntrancePlaying() && !logic.getRunEntranceAnimation().isVisible(), "character selection cancels entrance");
        logic.startGame();
        require(logic.isRunEntrancePlaying(), "starting from selection replays entrance");
    }

    private static void checkGameplayRemainder(int hero) throws Exception {
        GameLogic coarse = game(hero), fine = game(hero);
        player(coarse).setWorldCollision(null);
        player(fine).setWorldCollision(null);
        for (GameLogic logic : new GameLogic[] {coarse, fine}) {
            logic.setKeyPressed("right", true);
            logic.getAbilityManager().spendMana(25.0);
            firstEquipped(logic.getAbilityManager()).cooldownRemaining = 2.0;
        }
        double total = RunEntranceAnimation.DURATION + 0.35;
        coarse.update(total);
        double elapsed = 0.0;
        while (elapsed + 0.01 < total - EPSILON) {
            fine.update(0.01);
            elapsed += 0.01;
        }
        fine.update(total - elapsed);
        require(!coarse.isRunEntrancePlaying() && !fine.isRunEntrancePlaying(), "both step sizes finish entrance");
        near(coarse.getGameTimer(), 0.35, "a coarse step applies only its leftover gameplay time");
        near(coarse.getGameTimer(), fine.getGameTimer(), "partition-independent gameplay clock for hero " + hero);
        near(coarse.getPlayerWorldX(), fine.getPlayerWorldX(), "partition-independent resumed movement for hero " + hero);
        near(coarse.getPlayerWorldY(), fine.getPlayerWorldY(), "partition-independent resumed vertical position");
        near(coarse.getAbilityManager().getMana(), fine.getAbilityManager().getMana(), "partition-independent resumed mana");
        near(firstEquipped(coarse.getAbilityManager()).getCooldownRemaining(), 1.65,
                "a coarse step applies only leftover cooldown time");
        near(firstEquipped(coarse.getAbilityManager()).getCooldownRemaining(),
                firstEquipped(fine.getAbilityManager()).getCooldownRemaining(), "partition-independent resumed cooldown");
        require(coarse.getEnemyCount() == 0 && fine.getEnemyCount() == 0,
                "entrance time does not bring the first spawn forward");
    }

    private static void checkSpawnClock() {
        GameLogic logic = game(0);
        logic.update(RunEntranceAnimation.DURATION + 1.19);
        near(logic.getGameTimer(), 1.19, "spawn clock excludes the entrance");
        require(logic.getEnemyCount() == 0, "first spawn waits for its survival clock delay");
        logic.update(0.02);
        require(logic.getEnemyCount() > 0, "spawning starts after the survival clock reaches its delay");
    }

    private static void checkPlayerDrawing(int hero) throws Exception {
        Player player = player(game(hero));
        BufferedImage grounded = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        BufferedImage airborne = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D groundGraphics = grounded.createGraphics();
        Graphics2D airGraphics = airborne.createGraphics();
        try {
            player.drawEntrance(groundGraphics, 300, 350, 0.0, 0.0);
            player.drawEntrance(airGraphics, 300, 350, 120.0, 0.0);
        } finally {
            groundGraphics.dispose();
            airGraphics.dispose();
        }
        require(hasOpaquePixel(grounded), "hero " + hero + " entrance body and equipment draw");
        boolean translated = true;
        for (int y = 120; y < 600 && translated; y++) {
            for (int x = 0; x < 600; x++) {
                if (grounded.getRGB(x, y) != airborne.getRGB(x, y - 120)) {
                    translated = false;
                    break;
                }
            }
        }
        require(translated, "hero " + hero + " body and held gear rise together");
        boolean groundClear = true;
        for (int y = 365; y <= 395 && groundClear; y++) {
            for (int x = 250; x <= 350; x++) {
                if ((airborne.getRGB(x, y) >>> 24) != 0) {
                    groundClear = false;
                    break;
                }
            }
        }
        require(groundClear, "hero " + hero + " airborne drawing leaves ground shadows to the entrance effect");

        Graphics2D graphics = grounded.createGraphics();
        try {
            configureSentinelGraphics(graphics);
            GraphicsState state = new GraphicsState(graphics);
            player.drawEntrance(graphics, 300, 350, 120.0, 0.65);
            state.check(graphics, "hero " + hero + " entrance pose");
            player.drawEntrance(graphics, 300, 350, Double.NaN, Double.POSITIVE_INFINITY);
            state.check(graphics, "hero " + hero + " sanitized entrance pose");
        } finally {
            graphics.dispose();
        }
    }

    private static GameLogic game(int hero) {
        GameLogic logic = new GameLogic();
        logic.selectCharacter(hero);
        require(logic.getSelectedCharacterIndex() == hero, "hero " + hero + " is selectable");
        logic.startGame();
        require(logic.isRunEntrancePlaying(), "a new run begins with entrance");
        return logic;
    }

    private static RpgAbility firstEquipped(AbilityManager manager) {
        for (RpgAbility ability : manager.getEquippedAbilities()) if (ability != null) return ability;
        throw new AssertionError("A selectable hero must have an active ability.");
    }

    private static Player player(GameLogic logic) throws Exception {
        return (Player) field(logic, "player");
    }

    @SuppressWarnings("unchecked")
    private static List<Enemy> enemies(GameLogic logic) throws Exception {
        return (List<Enemy>) field(logic, "enemies");
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static int listSize(Object object, String name) throws Exception {
        return ((List<?>) field(object, name)).size();
    }

    private static boolean hasOpaquePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }

    private static void configureSentinelGraphics(Graphics2D graphics) {
        graphics.translate(4.5, 3.25);
        graphics.setClip(new Rectangle(1, 2, 950, 850));
        graphics.setColor(Color.MAGENTA);
        graphics.setPaint(new GradientPaint(0, 0, Color.MAGENTA, 15, 20, Color.CYAN));
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.73f));
        graphics.setStroke(new BasicStroke(3.0f));
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        graphics.setBackground(Color.ORANGE);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private static final class GraphicsState {
        private final AffineTransform transform;
        private final Shape clip;
        private final Paint paint;
        private final Color color;
        private final java.awt.Composite composite;
        private final java.awt.Stroke stroke;
        private final Font font;
        private final Color background;
        private final RenderingHints hints;

        private GraphicsState(Graphics2D graphics) {
            transform = graphics.getTransform();
            clip = graphics.getClip();
            paint = graphics.getPaint();
            color = graphics.getColor();
            composite = graphics.getComposite();
            stroke = graphics.getStroke();
            font = graphics.getFont();
            background = graphics.getBackground();
            hints = graphics.getRenderingHints();
        }

        private void check(Graphics2D graphics, String context) {
            require(transform.equals(graphics.getTransform()), context + " preserves transform");
            Area difference = new Area(clip);
            difference.exclusiveOr(new Area(graphics.getClip()));
            require(difference.isEmpty(), context + " preserves clip");
            require(paint.equals(graphics.getPaint()) && color.equals(graphics.getColor()), context + " preserves paint");
            require(composite.equals(graphics.getComposite()), context + " preserves composite");
            require(stroke.equals(graphics.getStroke()), context + " preserves stroke");
            require(font.equals(graphics.getFont()), context + " preserves font");
            require(background.equals(graphics.getBackground()), context + " preserves background");
            require(hints.equals(graphics.getRenderingHints()), context + " preserves rendering hints");
        }
    }

    private static void near(double actual, double expected, String message) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) <= EPSILON,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
