
public class AbilitySmokeTest {
    public static void main(String[] args) {
        TemplateAbility ability = new TemplateAbility();

        if (ability.isReady()) {
            throw new IllegalStateException("Ability should start on cooldown");
        }
        if (!"placeholder ability".equals(ability.getName())) {
            throw new IllegalStateException("Ability name should be placeholder ability");
        }

        ability.update(61.0);
        if (!ability.isReady()) {
            throw new IllegalStateException("Ability should become ready after cooldown expires");
        }

        Enemy enemy = new TemplateEnemy(0.0, 0.0);
        ability.trigger(0.0, 0.0, java.util.List.of(enemy));
        if (!enemy.isDead()) {
            throw new IllegalStateException("Ability should damage nearby enemies when triggered");
        }
        if (ability.isReady()) {
            throw new IllegalStateException("Ability should go on cooldown after trigger");
        }
    }
}
