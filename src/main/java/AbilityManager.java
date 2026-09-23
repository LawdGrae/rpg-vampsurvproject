import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AbilityManager {
    public static final int EQUIPPED_SLOT_COUNT = 4;

    private static final double MAX_MANA = 100.0;
    private static final double MANA_REGEN_PER_SECOND = 7.5;

    private final List<RpgAbility> abilities = new ArrayList<>();
    private final RpgAbility[] equipped = new RpgAbility[EQUIPPED_SLOT_COUNT];
    private double mana = MAX_MANA;
    private double manaRegenMultiplier = 1.0;
    private double manaRegenBoostTime;
    private double manaLockTimer;
    private int selectedEquipSlot;

    public AbilityManager() {
        for (AbilityDefinition definition : AbilityDefinition.createAll()) {
            abilities.add(new RpgAbility(definition));
        }
        equipDefaults();
    }

    public void update(double deltaTime) {
        if (manaLockTimer > 0.0) {
            manaLockTimer = Math.max(0.0, manaLockTimer - deltaTime);
        }
        if (manaLockTimer <= 0.0) {
            if (manaRegenBoostTime > 0.0) {
                manaRegenBoostTime = Math.max(0.0, manaRegenBoostTime - deltaTime);
                if (manaRegenBoostTime <= 0.0) {
                    manaRegenMultiplier = 1.0;
                }
            }
            mana = Math.min(MAX_MANA,
                    mana + MANA_REGEN_PER_SECOND * manaRegenMultiplier * deltaTime);
        }
        for (RpgAbility ability : abilities) {
            ability.update(deltaTime);
        }
    }

    public boolean triggerSlot(int slotIndex, GameLogic gameLogic, int playerLevel) {
        if (slotIndex < 0 || slotIndex >= equipped.length || equipped[slotIndex] == null) {
            return false;
        }
        return equipped[slotIndex].trigger(gameLogic, this, playerLevel);
    }

    public void equip(RpgAbility ability) {
        if (ability == null) {
            return;
        }
        for (RpgAbility equippedAbility : equipped) {
            if (equippedAbility == ability) {
                return;
            }
        }
        if (equipped[selectedEquipSlot] != null) {
            equipped[selectedEquipSlot].setEquipped(false);
        }
        equipped[selectedEquipSlot] = ability;
        ability.setEquipped(true);
        selectedEquipSlot = (selectedEquipSlot + 1) % equipped.length;
    }

    public void selectEquipSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < equipped.length) {
            selectedEquipSlot = slotIndex;
        }
    }

    public int getSelectedEquipSlot() {
        return selectedEquipSlot;
    }

    public List<RpgAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    public List<RpgAbility> getAbilities(AbilityClass abilityClass) {
        List<RpgAbility> matching = new ArrayList<>();
        for (RpgAbility ability : abilities) {
            if (ability.getDefinition().getAbilityClass() == abilityClass) {
                matching.add(ability);
            }
        }
        return matching;
    }

    public Map<AbilityClass, List<RpgAbility>> getAbilitiesByClass() {
        Map<AbilityClass, List<RpgAbility>> grouped = new EnumMap<>(AbilityClass.class);
        for (AbilityClass abilityClass : AbilityClass.values()) {
            grouped.put(abilityClass, getAbilities(abilityClass));
        }
        return grouped;
    }

    public RpgAbility[] getEquippedAbilities() {
        return equipped.clone();
    }

    public double getMana() {
        return mana;
    }

    public double getMaxMana() {
        return MAX_MANA;
    }

    public boolean isManaLocked() {
        return manaLockTimer > 0.0;
    }

    public void lockMana(double duration) {
        manaLockTimer = Math.max(manaLockTimer, duration);
    }

    public void spendMana(double amount) {
        if (manaLockTimer > 0.0) {
            return;
        }
        mana = Math.max(0.0, mana - amount);
    }

    public void restoreMana(double amount) {
        if (manaLockTimer > 0.0) {
            return;
        }
        mana = Math.min(MAX_MANA, mana + amount);
    }

    public void boostManaRegen(double duration, double multiplier) {
        manaRegenBoostTime = Math.max(manaRegenBoostTime, duration);
        manaRegenMultiplier = Math.max(manaRegenMultiplier, multiplier);
    }

    public void resetRunState() {
        mana = MAX_MANA;
        manaRegenMultiplier = 1.0;
        manaRegenBoostTime = 0.0;
        for (RpgAbility ability : abilities) {
            ability.reset();
            ability.cooldownRemaining = 0.0;
        }
    }

    private void equipDefaults() {
        int slot = 0;
        for (RpgAbility ability : abilities) {
            if (ability.getDefinition().getUnlockLevel() == 1 && slot < equipped.length) {
                equipped[slot] = ability;
                ability.setEquipped(true);
                slot++;
            }
        }
    }
}
