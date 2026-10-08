import javax.sound.sampled.AudioFormat;

/** Small deterministic PCM effects, generated once while audio is preloaded. */
final class EntranceSoundEffects {
    static final AudioFormat FORMAT = new AudioFormat(44_100.0f, 16, 1, true, false);
    static final double FALL_SECONDS = 0.72;
    static final double LANDING_SECONDS = 0.60;

    private EntranceSoundEffects() {
    }

    static byte[] fallPcm() {
        int frames = (int) Math.round(FALL_SECONDS * FORMAT.getSampleRate());
        byte[] pcm = new byte[frames * FORMAT.getFrameSize()];
        Noise noise = new Noise(0x71F411L);
        double lowNoise = 0.0;
        double phase = 0.0;
        for (int frame = 0; frame < frames; frame++) {
            double t = frame / (double) FORMAT.getSampleRate();
            double progress = t / FALL_SECONDS;
            double n = noise.next();
            lowNoise += (n - lowNoise) * (0.035 + 0.20 * progress * progress);
            // Air gains weight and speed as a descending resonant sweep approaches contact.
            double frequency = 630.0 * Math.pow(0.17, progress);
            phase += 2.0 * Math.PI * frequency / FORMAT.getSampleRate();
            double air = lowNoise * 2.8 + (n - lowNoise) * (0.045 + 0.08 * progress);
            double tone = Math.sin(phase) * 0.09 + Math.sin(phase * 1.97) * 0.035;
            double envelope = smoothStep(t / 0.045) * smoothStep((FALL_SECONDS - t) / 0.08);
            double weight = 0.23 + 0.72 * Math.pow(progress, 1.35);
            putSample(pcm, frame, (air + tone) * envelope * weight * 0.65);
        }
        return pcm;
    }

    static byte[] landingPcm() {
        int frames = (int) Math.round(LANDING_SECONDS * FORMAT.getSampleRate());
        byte[] pcm = new byte[frames * FORMAT.getFrameSize()];
        Noise noise = new Noise(0x1A4D17L);
        double lowNoise = 0.0;
        double phase = 0.0;
        for (int frame = 0; frame < frames; frame++) {
            double t = frame / (double) FORMAT.getSampleRate();
            double n = noise.next();
            lowNoise += (n - lowNoise) * 0.055;
            // A fast pitch drop supplies impact weight, followed by dust and debris texture.
            double frequency = 47.0 + 100.0 * Math.exp(-t * 35.0);
            phase += 2.0 * Math.PI * frequency / FORMAT.getSampleRate();
            double thump = Math.sin(phase) * Math.exp(-t * 10.0) * 0.62;
            double body = Math.sin(phase * 1.51) * Math.exp(-t * 17.0) * 0.16;
            double grit = (n - lowNoise) * Math.exp(-t * 28.0) * 0.12;
            double dust = lowNoise * Math.exp(-t * 8.5) * 0.85;
            double envelope = smoothStep(t / 0.003) * smoothStep((LANDING_SECONDS - t) / 0.08);
            putSample(pcm, frame, (thump + body + grit + dust) * envelope * 0.78);
        }
        return pcm;
    }

    private static double smoothStep(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3.0 - 2.0 * x);
    }

    private static void putSample(byte[] pcm, int frame, double value) {
        // Soft saturation preserves headroom, including rare coincident noise peaks.
        short sample = (short) Math.round(Math.tanh(value) * 27_850.0);
        int offset = frame * FORMAT.getFrameSize();
        pcm[offset] = (byte) sample;
        pcm[offset + 1] = (byte) (sample >> 8);
    }

    private static final class Noise {
        private long state;

        Noise(long seed) {
            state = seed;
        }

        double next() {
            state ^= state << 13;
            state ^= state >>> 7;
            state ^= state << 17;
            return ((state >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
        }
    }
}
