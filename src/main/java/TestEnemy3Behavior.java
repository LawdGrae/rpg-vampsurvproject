public class TestEnemy3Behavior {
    public static void main(String[] args) {
        TemplateEnemy3 boss = new TemplateEnemy3(100, 100);
        if (boss.getMaxHealth() <= 0) {
            throw new AssertionError("Boss should have health");
        }
        if (boss.getReflectEveryHits() != 4) {
            throw new AssertionError("Boss reflection interval should be four hits");
        }

        TemplateEnemyMinion minion = new TemplateEnemyMinion(90, 100);
        if (minion.getExplosionDamage() <= 0) {
            throw new AssertionError("Summon explosion should deal damage");
        }
        if (!minion.shouldExplodeOnContact(100, 100, 30)) {
            throw new AssertionError("Summon should explode when it touches the player");
        }

        System.out.println("Enemy3 behavior checks passed");
    }
}
