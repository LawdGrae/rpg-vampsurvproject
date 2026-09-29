public class WeaponDirectionSmokeTest {
    public static void main(String[] args) {
        TestPlayer player = new TestPlayer();
        player.setKeyPressed("right", true);
        player.update(0.1);

        if (Math.abs(player.getRecentMoveX() - 1.0) > 0.001) {
            throw new IllegalStateException("Recent movement x should follow the last movement direction");
        }
        if (Math.abs(player.getRecentMoveY()) > 0.001) {
            throw new IllegalStateException("Recent movement y should remain aligned with the last movement direction");
        }

        AutoFireWeapon weapon = new AutoFireWeapon();
        Enemy enemy = new TemplateEnemy(64.0, 0.0);
        Enemy hitEnemy = weapon.updateMeleeAttack(0.1, 0.0, 0.0, enemy);
        if (hitEnemy != enemy) {
            throw new IllegalStateException("Weapon should resolve a melee hit inside swing range");
        }
    }

    private static class TestPlayer extends Player {
        private TestPlayer() {
            super("/main/resources/character/temp_sheet.png", 200.0, 5.0, 2, 16, 18, 100.0);
        }
    }
}
