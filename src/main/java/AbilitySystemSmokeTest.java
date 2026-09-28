public class AbilitySystemSmokeTest {
    public static void main(String[] args) {
        AbilityManager manager = new AbilityManager();
        if (manager.getAbilities().size() != 60) {
            throw new IllegalStateException("Expected 60 RPG abilities");
        }
        int equippedCount = 0;
        for (RpgAbility ability : manager.getEquippedAbilities()) {
            if (ability != null) {
                equippedCount++;
            }
        }
        if (equippedCount != AbilityManager.EQUIPPED_SLOT_COUNT) {
            throw new IllegalStateException("Expected every ability slot to start equipped");
        }
        for (RpgAbility ability : manager.getAbilities()) {
            if (ability.getIcon().getWidth() <= 0
                    || ability.getIcon().getWidth() != ability.getIcon().getHeight()) {
                throw new IllegalStateException("Icon must be a valid square PNG: "
                        + ability.getDefinition().getId());
            }
        }
    }
}
