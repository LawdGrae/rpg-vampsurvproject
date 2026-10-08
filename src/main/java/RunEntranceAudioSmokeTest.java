import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;

/** Exercises the actual game/audio boundaries without opening a sound device. */
public final class RunEntranceAudioSmokeTest {
    private static int assertions;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        checkSoundData();
        Field loadedField = field("LOADED_CLIPS");
        Field preloadedField = field("preloaded");
        Map<String, Clip> loaded = (Map<String, Clip>) loadedField.get(null);
        Map<String, Clip> original = new HashMap<>(loaded);
        boolean originalPreloaded = preloadedField.getBoolean(null);
        FakeClip fall = new FakeClip(0.72);
        FakeClip landing = new FakeClip(0.64);
        try {
            loaded.clear();
            loaded.put("run_fall.wav", fall.clip());
            loaded.put("run_landing.wav", landing.clip());
            preloadedField.setBoolean(null, true);
            checkTimeline(fall, landing);
            checkPauseAndOverlays(fall);
            checkMuteAndSeek(fall, landing);
            checkCancellation(fall, landing);
            System.out.println("Run entrance audio checks passed (" + assertions + " assertions).");
        } finally {
            AbilitySoundPlayer.stopRunEntrance();
            loaded.clear();
            loaded.putAll(original);
            preloadedField.setBoolean(null, originalPreloaded);
        }
    }

    private static void checkSoundData() {
        byte[] fall = EntranceSoundEffects.fallPcm();
        byte[] landing = EntranceSoundEffects.landingPcm();
        require(java.util.Arrays.equals(fall, EntranceSoundEffects.fallPcm())
                && java.util.Arrays.equals(landing, EntranceSoundEffects.landingPcm()),
                "preloaded effects are deterministic");
        require(fall.length == Math.round(RunEntranceAnimation.FALL_DURATION
                * EntranceSoundEffects.FORMAT.getFrameRate()) * EntranceSoundEffects.FORMAT.getFrameSize(),
                "the whoosh lasts exactly as long as the fall");
        for (byte[] pcm : new byte[][] {fall, landing}) {
            int peak = 0;
            for (int offset = 0; offset < pcm.length; offset += 2) {
                int sample = (short) ((pcm[offset] & 0xff) | (pcm[offset + 1] << 8));
                peak = Math.max(peak, Math.abs(sample));
            }
            require(peak > 512 && peak < Short.MAX_VALUE, "the sound is audible and has clipping headroom");
            require(pcm[0] == 0 && pcm[1] == 0 && pcm[pcm.length - 2] == 0 && pcm[pcm.length - 1] == 0,
                    "the sound fades to zero at both ends to prevent clicks");
        }
    }

    private static void checkTimeline(FakeClip fall, FakeClip landing) {
        int falls = fall.starts, landings = landing.starts;
        GameLogic logic = new GameLogic();
        logic.startGame();
        require(fall.starts == falls + 1 && fall.running, "a run starts one fall whoosh");
        logic.update(0.40);
        require(landing.starts == landings, "the impact is silent while airborne");
        logic.update(0.35);
        require(!fall.running && landing.starts == landings + 1,
                "contact replaces the whoosh with one impact");
        near(landing.position / FakeClip.FORMAT.getFrameRate(), 0.03,
                "a delayed contact seeks into its sound");
        logic.update(0.05);
        require(landing.starts == landings + 1, "later frames do not repeat impact audio");
        landing.position = 7000;
        logic.togglePause();
        require(!landing.running, "pause also freezes the impact tail");
        logic.resume();
        require(landing.running && landing.position == 7000, "the impact tail resumes at its paused sample");
        logic.showMainMenu();

        logic.startGame();
        landings = landing.starts;
        logic.update(2.0);
        require(landing.starts == landings && !fall.running,
                "a tick past the entire entrance does not replay an expired impact");
        logic.showMainMenu();
    }

    private static void checkPauseAndOverlays(FakeClip fall) {
        GameLogic logic = new GameLogic();
        logic.startGame();
        logic.update(0.20);
        fall.position = 8800;
        logic.togglePause();
        require(!fall.running, "pause stops the fall clip");
        logic.update(3.0);
        near(logic.getRunEntranceAnimation().getAge(), 0.20, "pause freezes the visual timeline");
        logic.toggleSettings();
        logic.toggleSettings();
        require(!fall.running, "closing settings while paused keeps the sound paused");
        logic.resume();
        require(fall.running && fall.position == 8800, "resume preserves the fall sample position");
        int starts = fall.starts;
        logic.resume();
        require(fall.starts == starts, "repeated resume does not restart an active sound");
        logic.toggleSkillMenu();
        require(!fall.running, "the skill menu pauses entrance audio");
        logic.toggleSettings();
        logic.toggleSkillMenu();
        require(!fall.running, "another open overlay keeps entrance audio paused");
        logic.toggleSettings();
        require(fall.running, "closing the final overlay resumes entrance audio");
        logic.showMainMenu();
    }

    private static void checkMuteAndSeek(FakeClip fall, FakeClip landing) {
        GameLogic logic = new GameLogic();
        logic.setSoundEnabled(false);
        int falls = fall.starts, landings = landing.starts;
        logic.startGame();
        require(fall.starts == falls, "muted runs do not play a whoosh");
        logic.update(0.31);
        logic.setSoundEnabled(true);
        require(fall.starts == falls + 1, "unmuting during the fall starts its remaining sound");
        near(fall.position / FakeClip.FORMAT.getFrameRate(), 0.31, "unmute seeks to the current fall age");
        logic.setSoundEnabled(false);
        require(!fall.running, "mute stops the current whoosh immediately");
        logic.update(0.45);
        require(landing.starts == landings, "muted contact produces no impact sound");
        logic.setSoundEnabled(true);
        near(landing.position / FakeClip.FORMAT.getFrameRate(), 0.04,
                "unmuting after contact starts the remaining impact");
        logic.setSoundEnabled(false);
        logic.update(0.80);
        landings = landing.starts;
        logic.setSoundEnabled(true);
        require(landing.starts == landings, "unmute cannot revive an expired impact");
        logic.showMainMenu();
    }

    private static void checkCancellation(FakeClip fall, FakeClip landing) throws Exception {
        GameLogic logic = new GameLogic();
        logic.startGame();
        logic.togglePause();
        logic.showCharacterSelection();
        logic.resume();
        require(!fall.running && !landing.running, "selection cancels paused entrance audio");
        logic.startGame();
        int starts = fall.starts;
        logic.startGame();
        require(fall.starts == starts + 1 && fall.position == 0,
                "restart begins exactly one fresh whoosh");
        logic.showMainMenu();
        logic.resume();
        require(!fall.running && !landing.running, "the main menu cannot resume a cancelled entrance");
        logic.startGame();
        Field playerField = GameLogic.class.getDeclaredField("player");
        playerField.setAccessible(true);
        Player player = (Player) playerField.get(logic);
        player.takeDamage(player.getMaxHealth());
        logic.update(0.01);
        require(logic.isGameOver() && !fall.running && !landing.running,
                "an interrupted entrance cancels its sound on game over");
    }

    private static Field field(String name) throws Exception {
        Field field = AbilitySoundPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) <= 1.0 / FakeClip.FORMAT.getFrameRate(), message);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeClip implements InvocationHandler {
        private static final AudioFormat FORMAT = new AudioFormat(44100.0f, 16, 1, true, false);
        private final int frames;
        private int position;
        private int starts;
        private boolean running;

        private FakeClip(double duration) {
            frames = (int) Math.round(duration * FORMAT.getFrameRate());
        }

        private Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                    new Class<?>[] {Clip.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "start" -> { starts++; running = true; yield null; }
                case "stop", "close" -> { running = false; yield null; }
                case "setFramePosition" -> { position = (int) args[0]; yield null; }
                case "getFramePosition" -> position;
                case "getLongFramePosition" -> (long) position;
                case "getFrameLength" -> frames;
                case "getFormat" -> FORMAT;
                case "isRunning", "isActive" -> running;
                case "isOpen" -> true;
                case "getMicrosecondPosition" -> (long) (position * 1_000_000.0 / FORMAT.getFrameRate());
                case "getMicrosecondLength" -> (long) (frames * 1_000_000.0 / FORMAT.getFrameRate());
                case "toString" -> "Entrance audio test clip";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unexpected clip call: " + method.getName());
            };
        }
    }
}
