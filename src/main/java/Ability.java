import java.util.List;

public abstract class Ability {
    protected final String name;
    protected final double cooldownDuration;
    protected double cooldownRemaining;

    protected Ability(String name, double cooldownDuration) {
        this.name = name;
        this.cooldownDuration = cooldownDuration;
        this.cooldownRemaining = cooldownDuration;
    }

    public void update(double deltaTime) {
        if (cooldownRemaining > 0.0) {
            cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaTime);
        }
    }

    public boolean isReady() {
        return cooldownRemaining <= 0.0;
    }

    public double getCooldownRemaining() {
        return cooldownRemaining;
    }

    public String getName() {
        return name;
    }

    public void trigger(double originX, double originY, List<Enemy> enemies) {
        if (!isReady()) {
            return;
        }

        applyEffect(originX, originY, enemies);
        cooldownRemaining = cooldownDuration;
    }

    public void reset() {
        cooldownRemaining = cooldownDuration;
    }

    protected abstract void applyEffect(double originX, double originY, List<Enemy> enemies);
}
