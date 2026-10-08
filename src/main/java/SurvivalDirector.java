/** Tracks combat streaks and alternates short assaults with breathing room. */
public final class SurvivalDirector {
    public enum Assault {
        PINCER("Pincer assault", "Enemies approach from two sides"),
        RUSH("Horde rush", "Keep moving and use your area skills"),
        ENCIRCLEMENT("Encirclement", "Break through before the ring closes");

        private final String title;
        private final String hint;
        Assault(String title, String hint) { this.title = title; this.hint = hint; }
        public String title() { return title; }
        public String hint() { return hint; }
    }

    public static final double STREAK_WINDOW = 6.0;
    private static final double ASSAULT_INTERVAL = 34.0;
    private static final double WARNING_TIME = 3.0;
    private static final double ASSAULT_TIME = 8.0;
    private double timeUntilAssault = 28.0;
    private double assaultRemaining;
    private double recoveryRemaining;
    private double streakRemaining;
    private double rewardRemaining;
    private int assaultNumber;
    private int kills;
    private int streak;
    private int bestStreak;
    private long score;
    private Assault assault = Assault.PINCER;

    /** True once on assault launch; boss encounters suspend this schedule. */
    public boolean update(double dt, boolean bossActive) {
        if (!Double.isFinite(dt) || dt <= 0.0) return false;
        streakRemaining = Math.max(0.0, streakRemaining - dt);
        rewardRemaining = Math.max(0.0, rewardRemaining - dt);
        if (streakRemaining == 0.0) streak = 0;
        if (bossActive) {
            assaultRemaining = 0.0;
            recoveryRemaining = 0.0;
            timeUntilAssault = Math.max(timeUntilAssault, 12.0);
            return false;
        }
        double previousAssault = assaultRemaining;
        assaultRemaining = Math.max(0.0, assaultRemaining - dt);
        recoveryRemaining = Math.max(0.0, recoveryRemaining - dt);
        if (previousAssault > 0.0 && assaultRemaining == 0.0) {
            recoveryRemaining = Math.max(0.0, 5.0 - Math.max(0.0, dt - previousAssault));
        }
        timeUntilAssault -= dt;
        if (timeUntilAssault > 0.0) return false;
        double overshoot = -timeUntilAssault;
        assault = Assault.values()[assaultNumber % Assault.values().length];
        assaultNumber++;
        assaultRemaining = Math.max(0.0, ASSAULT_TIME - overshoot);
        // A delayed frame can start one bounded wave, never a backlog of waves.
        timeUntilAssault = ASSAULT_INTERVAL - (overshoot % ASSAULT_INTERVAL);
        return true;
    }

    /** Returns true only for each fifth kill in a live streak. */
    public boolean registerKill(boolean boss) {
        if (streakRemaining <= 0.0) streak = 0;
        kills++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        streakRemaining = STREAK_WINDOW;
        score += (boss ? 1000 : 100) * (4L + Math.min(4, streak / 5)) / 4L;
        boolean reward = streak % 5 == 0;
        if (reward) rewardRemaining = 1.8;
        return reward;
    }

    public void reset() {
        timeUntilAssault = 28.0;
        assaultRemaining = recoveryRemaining = streakRemaining = rewardRemaining = 0.0;
        assaultNumber = kills = streak = bestStreak = 0;
        score = 0;
        assault = Assault.PINCER;
    }

    public Assault getAssault() {
        return isWarning() ? Assault.values()[assaultNumber % Assault.values().length] : assault;
    }
    public boolean isWarning() { return timeUntilAssault <= WARNING_TIME && assaultRemaining == 0.0; }
    public boolean isAssaultActive() { return assaultRemaining > 0.0; }
    public boolean isRecovering() { return recoveryRemaining > 0.0; }
    public double getAssaultCountdown() { return Math.max(0.0, timeUntilAssault); }
    public double getAssaultRemaining() { return assaultRemaining; }
    public double getStreakRatio() { return streakRemaining / STREAK_WINDOW; }
    public double getRewardFlash() { return rewardRemaining / 1.8; }
    public int getAssaultNumber() { return assaultNumber; }
    public int getKills() { return kills; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }
    public long getScore() { return score; }
}
