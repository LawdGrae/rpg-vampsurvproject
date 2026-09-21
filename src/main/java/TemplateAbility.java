import java.util.ArrayList;
import java.util.List;

public class TemplateAbility extends Ability {
    private static final double COOLDOWN_SECONDS = 60.0;
    private static final double DAMAGE_RADIUS = 170.0;
    private static final double DAMAGE = 30.0;

    public TemplateAbility() {
        super("placeholder ability", COOLDOWN_SECONDS);
    }

    @Override
    protected void applyEffect(double originX, double originY, List<Enemy> enemies) {
        if (enemies == null) {
            return;
        }

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            double distanceSquared = enemy.distanceSquaredTo(originX, originY);
            double radiusSquared = DAMAGE_RADIUS * DAMAGE_RADIUS;
            if (distanceSquared <= radiusSquared) {
                enemy.takeDamage(DAMAGE);
            }
        }
    }

    public static void main(String[] args) {
        TemplateAbility ability = new TemplateAbility();
        ability.trigger(0.0, 0.0, new ArrayList<>());
    }
}
