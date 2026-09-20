import java.lang.reflect.Field;

public class TestEnemyProjectile {
    public static void main(String[] args) throws Exception {
        TemplateEnemy2 enemy = new TemplateEnemy2(0, 0);
        enemy.updateFireCooldown(1.0);
        Projectile first = enemy.fireAt(100, 0);
        Projectile second = enemy.fireAt(100, 0);

        if (first == null) {
            throw new AssertionError("LVL2 enemy should fire a projectile when able");
        }
        if (second != null) {
            throw new AssertionError("LVL2 enemy should only fire one projectile at a time");
        }

        first.update(0.1);
        double dx = first.getWorldX() - enemy.getWorldX();
        double dy = first.getWorldY() - enemy.getWorldY();
        if (dx <= 0 || Math.abs(dy) > 0.001) {
            throw new AssertionError("Projectile should travel in the target direction");
        }

        TemplateCharacter player = new TemplateCharacter();
        Field healthField = Player.class.getDeclaredField("health");
        healthField.setAccessible(true);
        double before = (double) healthField.get(player);

        player.takeDamage(20.0);
        double afterFirstHit = (double) healthField.get(player);
        if (afterFirstHit >= before) {
            throw new AssertionError("Player should lose health on the first hit");
        }

        player.takeDamage(20.0);
        double afterSecondHit = (double) healthField.get(player);
        if (afterSecondHit < afterFirstHit) {
            throw new AssertionError("Player should be invulnerable for a short time after the first hit");
        }
    }
}
