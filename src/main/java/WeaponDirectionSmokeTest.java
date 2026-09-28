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
        Projectile projectile = weapon.update(0.1, 0.0, 0.0,
                player.getRecentMoveX(), player.getRecentMoveY());
        if (projectile == null) {
            throw new IllegalStateException("Weapon should continue firing in the recent movement direction");
        }
    }

    private static class TestPlayer extends Player {
        private TestPlayer() {
            super("/main/resources/character/temp_sheet.png", 200.0, 5.0, 2, 16, 18, 100.0);
        }
    }
}
