public class UpgradeStatSmokeTest {
    public static void main(String[] args) {
        TemplateCharacter player = new TemplateCharacter();
        TemplateWeapon weapon = new TemplateWeapon();

        player.increaseMaxHealth(10.0);
        player.increaseSpeed(12.0);
        player.increasePickupRadius(18.0);

        weapon.addDamage(1.5);
        weapon.addCritChance(0.08);
        weapon.addFireSpeed(0.15);

        if (player.getMaxHealth() <= 100.0) {
            throw new IllegalStateException("Health upgrade did not apply");
        }
        if (weapon.getProjectileDamage() <= 1.0) {
            throw new IllegalStateException("Damage upgrade did not apply");
        }
        if (weapon.getCritChance() <= 0.0) {
            throw new IllegalStateException("Crit chance upgrade did not apply");
        }
        if (weapon.getFireInterval() >= 0.5) {
            throw new IllegalStateException("Fire speed upgrade did not apply");
        }
    }
}
