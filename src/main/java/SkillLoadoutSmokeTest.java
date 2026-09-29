import java.util.Arrays;
import java.util.List;

public class SkillLoadoutSmokeTest {
    public static void main(String[] args) {
        GameLogic gameLogic = new GameLogic();
        List<List<String>> expectedLoadouts = List.of(
                List.of("heavy_slash", "shield_bash", "earth_shatter", "knights_wrath"),
                List.of("shadow_strike", "twin_fang", "shadow_step", "silent_execution"),
                List.of("holy_bolt", "heal", "holy_shield", "divine_light"),
                List.of("flame_burst", "ice_shard", "lightning_strike", "elemental_storm")
        );
        List<String> expectedPassives = List.of(
                "Iron Guard",
                "Assassin's Instinct",
                "Divine Blessing",
                "Elemental Mastery"
        );

        for (int characterIndex = 0; characterIndex < expectedLoadouts.size(); characterIndex++) {
            gameLogic.selectCharacter(characterIndex);
            gameLogic.startGame();

            RpgAbility[] equipped = gameLogic.getAbilityManager().getEquippedAbilities();
            List<String> expected = expectedLoadouts.get(characterIndex);
            if (equipped.length != AbilityManager.EQUIPPED_SLOT_COUNT) {
                throw new IllegalStateException("Expected four equipped skill slots");
            }
            for (int slot = 0; slot < equipped.length; slot++) {
                if (equipped[slot] == null) {
                    throw new IllegalStateException("Missing equipped skill in slot " + slot);
                }
                AbilityDefinition definition = equipped[slot].getDefinition();
                if (!expected.get(slot).equals(definition.getId())) {
                    throw new IllegalStateException("Wrong skill in slot " + slot
                            + " for " + gameLogic.getSelectedCharacterName()
                            + ": expected " + expected.get(slot)
                            + " but got " + definition.getId());
                }
                if (!definition.isUnlockedAt(gameLogic.getLevel())) {
                    throw new IllegalStateException("Equipped skill should be usable at level 1: "
                            + definition.getId());
                }
                if (definition.getManaCost() < 5.0 || definition.getManaCost() > 30.0) {
                    throw new IllegalStateException("Mana cost is outside the balanced range: "
                            + definition.getId() + " costs " + definition.getManaCost());
                }
                if (SkillEffectAtlas.getAnimation(definition.getId()) == null) {
                    throw new IllegalStateException("Missing per-skill VFX animation for "
                            + definition.getId());
                }
            }

            if (!expectedPassives.get(characterIndex)
                    .equals(gameLogic.getCharacterPassiveName(characterIndex))) {
                throw new IllegalStateException("Wrong passive label for "
                        + gameLogic.getSelectedCharacterName());
            }
        }

        gameLogic.selectCharacter(0);
        gameLogic.startGame();
        RpgAbility[] before = gameLogic.getAbilityManager().getEquippedAbilities();
        List<String> beforeIds = Arrays.stream(before)
                .map(ability -> ability.getDefinition().getId())
                .toList();
        gameLogic.equipAbility(gameLogic.getAbilityManager().getAbilityById("shadow_strike"));
        List<String> afterIds = Arrays.stream(gameLogic.getAbilityManager().getEquippedAbilities())
                .map(ability -> ability.getDefinition().getId())
                .toList();
        if (!beforeIds.equals(afterIds)) {
            throw new IllegalStateException("A character should not equip another class loadout skill");
        }
    }
}
