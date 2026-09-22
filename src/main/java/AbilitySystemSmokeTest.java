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
            if (ability.getIcon().getWidth() != 1024 || ability.getIcon().getHeight() != 1024) {
                throw new IllegalStateException("Icon must be 1024x1024: "
                        + ability.getDefinition().getId());
            }
        }
    }
}
