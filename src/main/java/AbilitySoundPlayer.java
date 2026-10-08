import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class AbilitySoundPlayer {
    private static final Map<String, String> ABILITY_SOUNDS = Map.ofEntries(
            Map.entry("heavy_slash", "skill_sword_fire.wav"),
            Map.entry("shield_bash", "skill_shield_upgrade.wav"),
            Map.entry("earth_shatter", "skill_earth_slam.wav"),
            Map.entry("knights_wrath", "skill_spin_blade.wav"),
            Map.entry("shadow_strike", "skill_blink.wav"),
            Map.entry("shadow_step", "skill_dagger_dash.wav"),
            Map.entry("death_mark", "skill_mark.wav"),
            Map.entry("twin_fang", "skill_dagger_spiral.wav"),
            Map.entry("heal", "skill_heal.wav"),
            Map.entry("holy_bolt", "skill_holy_light.wav"),
            Map.entry("holy_shield", "holy_barrier.wav"),
            Map.entry("divine_light", "skill_judgement.wav"),
            Map.entry("fire_bolt", "skill_firebolt.wav"),
            Map.entry("flame_burst", "skill_firebolt.wav"),
            Map.entry("lightning_strike", "skill_lightning.wav"),
            Map.entry("elemental_storm", "skill_elemental_nova.wav"),
            Map.entry("shield_fortress", "skill_shield_upgrade.wav"),
            Map.entry("iron_charge", "skill_charge.wav"),
            Map.entry("earthbreaker", "skill_earth_slam.wav"),
            Map.entry("guardians_roar", "skill_roar.wav"),
            Map.entry("backstep", "skill_dagger_dash.wav"),
            Map.entry("hunters_mark", "skill_mark.wav"),
            Map.entry("curse", "skill_mark.wav"),
            Map.entry("thunder_chain", "skill_lightning.wav"),
            Map.entry("divine_judgment", "skill_judgement.wav"),
            Map.entry("resurrection", "skill_heal.wav"),
            Map.entry("life_drain", "skill_heal.wav"),
            Map.entry("shield_charge", "skill_charge.wav"),
            Map.entry("dark_fortress", "skill_shield_upgrade.wav"),
            Map.entry("arrow_storm", "skill_spin_blade.wav"),
            Map.entry("apocalypse", "skill_elemental_nova.wav"));
    private static final Map<String, Clip> LOADED_CLIPS = new HashMap<>();
    private static final String RUN_FALL_SOUND = "run_fall.wav";
    private static final String RUN_LANDING_SOUND = "run_landing.wav";
    private static final Map<String, Integer> PAUSED_ENTRANCE_FRAMES = new HashMap<>();
    private static volatile boolean preloaded;

    private AbilitySoundPlayer() {
    }

    public static synchronized void preload() {
        if (preloaded) {
            return;
        }

        for (String filename : ABILITY_SOUNDS.values().stream().distinct().toList()) {
            try {
                LOADED_CLIPS.put(filename, loadClip(filename));
            } catch (IOException | UnsupportedAudioFileException
                    | LineUnavailableException | IllegalArgumentException exception) {
                System.err.println("Unable to preload sound " + filename
                        + "; that sound will be unavailable: " + exception.getMessage());
            }
        }
        try {
            LOADED_CLIPS.put("lvl_up.wav", loadClip("lvl_up.wav"));
        } catch (IOException | UnsupportedAudioFileException
                | LineUnavailableException | IllegalArgumentException exception) {
            System.err.println("Unable to preload sound lvl_up.wav"
                    + "; that sound will be unavailable: " + exception.getMessage());
        }
        preloadEntranceClip(RUN_FALL_SOUND, EntranceSoundEffects.fallPcm());
        preloadEntranceClip(RUN_LANDING_SOUND, EntranceSoundEffects.landingPcm());
        preloaded = true;
    }

    public static void play(AbilityDefinition definition) {
        String filename = ABILITY_SOUNDS.get(definition.getId());
        if (filename != null) {
            play(filename);
        }
    }

    public static void playLevelUp() {
        play("lvl_up.wav");
    }

    public static void playRunFall() {
        playRunFall(0.0);
    }

    public static synchronized void playRunFall(double elapsedSeconds) {
        playEntrance(RUN_FALL_SOUND, elapsedSeconds);
    }

    public static void playRunLanding() {
        playRunLanding(0.0);
    }

    public static synchronized void playRunLanding(double elapsedSeconds) {
        playEntrance(RUN_LANDING_SOUND, elapsedSeconds);
    }

    /** Stop only entrance effects; this is also safe before audio is preloaded. */
    public static synchronized void stopRunEntrance() {
        PAUSED_ENTRANCE_FRAMES.clear();
        stopEntranceClip(RUN_FALL_SOUND);
        stopEntranceClip(RUN_LANDING_SOUND);
    }

    /** Preserve the exact playback frame while gameplay and its entrance are frozen. */
    public static synchronized void pauseRunEntrance() {
        pauseEntranceClip(RUN_FALL_SOUND);
        pauseEntranceClip(RUN_LANDING_SOUND);
    }

    public static synchronized void resumeRunEntrance() {
        resumeEntranceClip(RUN_FALL_SOUND);
        resumeEntranceClip(RUN_LANDING_SOUND);
    }

    private static void playEntrance(String filename, double elapsedSeconds) {
        if (!preloaded) {
            preload();
        }
        stopRunEntrance();
        Clip clip = LOADED_CLIPS.get(filename);
        if (clip == null || !Double.isFinite(elapsedSeconds)) {
            return;
        }
        synchronized (clip) {
            try {
                double frameOffset = Math.max(0.0, elapsedSeconds) * clip.getFormat().getFrameRate();
                // A delayed update must not replay a sound whose visual event has already passed.
                if (frameOffset >= clip.getFrameLength()) {
                    return;
                }
                clip.setFramePosition((int) frameOffset);
                clip.start();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                // A removed audio device must not interrupt the game.
            }
        }
    }

    private static void stopEntranceClip(String filename) {
        Clip clip = LOADED_CLIPS.get(filename);
        if (clip == null) {
            return;
        }
        synchronized (clip) {
            try {
                clip.stop();
                clip.setFramePosition(0);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                // The clip may have become unavailable after preloading.
            }
        }
    }

    private static void pauseEntranceClip(String filename) {
        Clip clip = LOADED_CLIPS.get(filename);
        if (clip == null) {
            return;
        }
        synchronized (clip) {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                    int frame = clip.getFramePosition();
                    if (frame < clip.getFrameLength()) {
                        PAUSED_ENTRANCE_FRAMES.put(filename, frame);
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                PAUSED_ENTRANCE_FRAMES.remove(filename);
            }
        }
    }

    private static void resumeEntranceClip(String filename) {
        Integer frame = PAUSED_ENTRANCE_FRAMES.remove(filename);
        Clip clip = LOADED_CLIPS.get(filename);
        if (clip == null || frame == null) {
            return;
        }
        synchronized (clip) {
            try {
                if (frame < clip.getFrameLength()) {
                    clip.setFramePosition(frame);
                    clip.start();
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                // Resuming gameplay remains safe if its audio device disappeared.
            }
        }
    }

    private static void preloadEntranceClip(String filename, byte[] pcm) {
        try (AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(pcm),
                EntranceSoundEffects.FORMAT, pcm.length / EntranceSoundEffects.FORMAT.getFrameSize())) {
            LOADED_CLIPS.put(filename, openClip(audioStream));
        } catch (IOException | LineUnavailableException | IllegalArgumentException
                | SecurityException exception) {
            System.err.println("Unable to preload sound " + filename
                    + "; that sound will be unavailable: " + exception.getMessage());
        }
    }

    private static void play(String filename) {
        if (!preloaded) {
            preload();
        }
        Clip clip = LOADED_CLIPS.get(filename);
        if (clip != null) {
            synchronized (clip) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            }
        }
    }

    private static Clip loadClip(String filename)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        try (InputStream rawStream = openSound(filename);
                BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                AudioInputStream audioStream =
                        AudioSystem.getAudioInputStream(bufferedStream)) {
            AudioFormat sourceFormat = audioStream.getFormat();
            AudioFormat playbackFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
            try (AudioInputStream playbackStream =
                    AudioSystem.getAudioInputStream(playbackFormat, audioStream)) {
                return openClip(playbackStream);
            }
        }
    }

    private static Clip openClip(AudioInputStream audioStream)
            throws IOException, LineUnavailableException {
        Clip clip = AudioSystem.getClip();
        try {
            clip.open(audioStream);
            return clip;
        } catch (IOException | LineUnavailableException | IllegalArgumentException exception) {
            clip.close();
            throw exception;
        }
    }

    private static InputStream openSound(String filename) throws IOException {
        String[] classpathResources = {
            "sounds/" + filename,
            "main/resources/sounds/" + filename
        };
        for (String classpathResource : classpathResources) {
            InputStream classpathStream = AbilitySoundPlayer.class.getClassLoader()
                    .getResourceAsStream(classpathResource);
            if (classpathStream != null) {
                return classpathStream;
            }
        }
        Path sourceResource = Path.of("src", "main", "resources", "sounds", filename);
        if (Files.isRegularFile(sourceResource)) {
            return Files.newInputStream(sourceResource);
        }
        throw new IOException("Resource not found: " + sourceResource);
    }
}
