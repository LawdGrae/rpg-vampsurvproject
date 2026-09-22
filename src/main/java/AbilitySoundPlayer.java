import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class AbilitySoundPlayer {
    private static final int SAMPLE_RATE = 22050;

    private AbilitySoundPlayer() {
    }

    public static void play(AbilityDefinition definition) {
        Thread soundThread = new Thread(() -> playTone(definition), "ability-sound");
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private static void playTone(AbilityDefinition definition) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, SAMPLE_RATE / 8);
                line.start();
                byte[] data = createSound(definition);
                line.write(data, 0, data.length);
                line.drain();
            }
        } catch (Exception ignored) {
            // Some machines have no audio line available; gameplay should continue silently.
        }
    }

    private static byte[] createSound(AbilityDefinition definition) {
        double duration = definition.getEffectType() == AbilityEffectType.ULTIMATE ? 0.22 : 0.11;
        int length = (int) (SAMPLE_RATE * duration);
        byte[] data = new byte[length];
        double baseFrequency = baseFrequency(definition);
        double secondFrequency = baseFrequency * effectMultiplier(definition.getEffectType());

        for (int index = 0; index < data.length; index++) {
            double time = index / (double) SAMPLE_RATE;
            double progress = index / (double) data.length;
            double envelope = Math.sin(Math.PI * progress);
            double wave = Math.sin(Math.PI * 2.0 * baseFrequency * time)
                    + 0.45 * Math.sin(Math.PI * 2.0 * secondFrequency * time);
            if (definition.getAbilityClass() == AbilityClass.WARLOCK
                    || definition.getAbilityClass() == AbilityClass.ASSASSIN) {
                wave += 0.2 * Math.sin(Math.PI * 2.0 * baseFrequency * 0.5 * time);
            }
            data[index] = (byte) Math.max(-120, Math.min(120, wave * envelope * 58.0));
        }
        return data;
    }

    private static double baseFrequency(AbilityDefinition definition) {
        return switch (definition.getAbilityClass()) {
            case ASSASSIN -> 196.0;
            case BLACK_KNIGHT -> 110.0;
            case PRIEST -> 440.0;
            case RANGER -> 330.0;
            case WARLOCK -> 146.8;
            case ELEMENTALIST -> 523.3;
        } + Math.floorMod(definition.getId().hashCode(), 40);
    }

    private static double effectMultiplier(AbilityEffectType effectType) {
        return switch (effectType) {
            case DASH -> 1.8;
            case HEAL, SHIELD, BUFF -> 1.5;
            case POISON, SLOW -> 1.25;
            case STUN -> 2.0;
            case ULTIMATE -> 0.75;
            default -> 1.33;
        };
    }
}
