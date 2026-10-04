public class RegionalEnemySmokeTest {
    public static void main(String[] args) {
        int bossCount = 0;
        int normalCount = 0;

        for (EnemyRegion region : EnemyRegion.values()) {
            if (EnemyCatalog.normalEnemies(region).isEmpty()) {
                throw new IllegalStateException(region.getDisplayName() + " has no enemy pool");
            }
            EnemyDefinition boss = EnemyCatalog.bossFor(region);
            if (boss == null || !boss.isBoss()) {
                throw new IllegalStateException(region.getDisplayName() + " has no boss");
            }
            bossCount++;
            normalCount += EnemyCatalog.normalEnemies(region).size();

            RegionalEnemy bossEnemy = new RegionalEnemy(boss, 0.0, 0.0);
            if (!bossEnemy.isBoss() || bossEnemy.getBossPhase() != 1) {
                throw new IllegalStateException("Boss metadata failed for " + boss.getDisplayName());
            }
            bossEnemy.takeDamage(bossEnemy.getHealth() * 0.72, boss.getElement(), -20.0, 0.0);
            if (bossEnemy.getBossPhase() < 2) {
                throw new IllegalStateException("Boss phase did not advance for " + boss.getDisplayName());
            }
        }

        EnemyDefinition ranged = EnemyCatalog.normalEnemies(EnemyRegion.RIVENDALE_TOWN)
                .stream()
                .filter(EnemyDefinition::isRanged)
                .findFirst()
                .orElseThrow();
        RegionalEnemy archer = new RegionalEnemy(ranged, 0.0, 0.0);
        archer.update(1.0, 120.0, 0.0, 20.0);
        Projectile projectile = archer.fireAt(120.0, 0.0);
        if (projectile == null || projectile.getOwner() != archer) {
            throw new IllegalStateException("Regional ranged enemy did not fire");
        }

        if (bossCount != 8 || normalCount < 50) {
            throw new IllegalStateException("Unexpected catalog size");
        }
    }
}
