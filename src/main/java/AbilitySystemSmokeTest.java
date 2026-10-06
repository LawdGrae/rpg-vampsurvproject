import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbilitySystemSmokeTest {
    public static void main(String[] args) {
        AbilityManager manager = new AbilityManager();
        if (manager.getAbilities().size() != 65) {
            throw new IllegalStateException("Expected 65 RPG ability definitions and icons");
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
        Set<String> abilityIds = new HashSet<>();
        for (RpgAbility ability : manager.getAbilities()) {
            if (!abilityIds.add(ability.getDefinition().getId())) {
                throw new IllegalStateException("Ability definitions must have unique IDs: "
                        + ability.getDefinition().getId());
            }
            if (ability.getIcon().getWidth() <= 0
                    || ability.getIcon().getWidth() != ability.getIcon().getHeight()) {
                throw new IllegalStateException("Icon must be a valid square PNG: "
                        + ability.getDefinition().getId());
            }
        }
        verifyGuardian(manager);
    }

    private static void verifyGuardian(AbilityManager manager) {
        List<RpgAbility> guardian = manager.getAbilities(AbilityClass.GUARDIAN);
        long activeCount = guardian.stream()
                .filter(ability -> !ability.getDefinition().isPassive()).count();
        long passiveCount = guardian.stream()
                .filter(ability -> ability.getDefinition().isPassive()).count();
        if (guardian.size() != 5 || activeCount != 4 || passiveCount != 1) {
            throw new IllegalStateException("Guardian must have four active skills and one passive");
        }
        RpgAbility passive = manager.getAbilityById("unbreakable");
        if (passive == null || !passive.getDefinition().isPassive()
                || passive.getDefinition().getManaCost() != 0.0) {
            throw new IllegalStateException("Unbreakable must be a mana-free passive trait");
        }
        RpgAbility[] before = manager.getEquippedAbilities();
        int selectedSlot = manager.getSelectedEquipSlot();
        manager.equip(passive);
        if (!Arrays.equals(before, manager.getEquippedAbilities()) || passive.isEquipped()
                || selectedSlot != manager.getSelectedEquipSlot()) {
            throw new IllegalStateException("Equipping a passive must leave active slots unchanged");
        }
        manager.equipLoadout(List.of("unbreakable", "shield_fortress", "iron_charge",
                "earthbreaker", "guardians_roar"));
        List<String> expected = List.of("shield_fortress", "iron_charge",
                "earthbreaker", "guardians_roar");
        RpgAbility[] equipped = manager.getEquippedAbilities();
        for (int slot = 0; slot < equipped.length; slot++) {
            if (equipped[slot] == null
                    || !expected.get(slot).equals(equipped[slot].getDefinition().getId())) {
                throw new IllegalStateException("Guardian loadout must skip the passive at slot " + slot);
            }
        }
        double manaBefore = manager.getMana();
        double cooldownBefore = passive.getCooldownRemaining();
        GameLogic gameLogic = new GameLogic();
        gameLogic.selectCharacter(4);
        gameLogic.startGame();
        if (passive.canTrigger(manager, 1) || passive.trigger(gameLogic, manager, 1)
                || manager.getMana() != manaBefore
                || passive.getCooldownRemaining() != cooldownBefore) {
            throw new IllegalStateException("Unbreakable cannot be triggered or consume resources");
        }
    }
}
