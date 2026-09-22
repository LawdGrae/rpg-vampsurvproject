import java.awt.image.BufferedImage;
import java.util.List;

public class RpgAbility extends Ability {
    private final AbilityDefinition definition;
    private final BufferedImage icon;
    private boolean equipped;

    public RpgAbility(AbilityDefinition definition) {
        super(definition.getName(), definition.getCooldownSeconds());
        this.definition = definition;
        this.icon = ResourceLoader.loadImage(definition.getIconPath());
        this.cooldownRemaining = 0.0;
    }

    public AbilityDefinition getDefinition() {
        return definition;
    }

    public BufferedImage getIcon() {
        return icon;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    public double getCooldownRatio() {
        if (cooldownDuration <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, cooldownRemaining / cooldownDuration));
    }

    public boolean canTrigger(AbilityManager manager, int playerLevel) {
        return isReady()
                && definition.isUnlockedAt(playerLevel)
                && manager.getMana() >= definition.getManaCost();
    }

    public boolean trigger(GameLogic gameLogic, AbilityManager manager, int playerLevel) {
        if (!canTrigger(manager, playerLevel)) {
            gameLogic.showAbilityDenied(definition);
            return false;
        }
        manager.spendMana(definition.getManaCost());
        gameLogic.showManaSpent(definition);
        gameLogic.applyAbilityEffect(definition);
        cooldownRemaining = cooldownDuration;
        return true;
    }

    @Override
    protected void applyEffect(double originX, double originY, List<Enemy> enemies) {
        if (enemies == null) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.distanceSquaredTo(originX, originY)
                    <= definition.getRadius() * definition.getRadius()) {
                enemy.takeDamage(definition.getDamage());
            }
        }
    }
}
