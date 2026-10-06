import java.lang.reflect.Field;

public class RuntimeStabilitySmokeTest {
    public static void main(String[] args) throws ReflectiveOperationException {
        Field playerField = GameLogic.class.getDeclaredField("player");
        playerField.setAccessible(true);
        int characterCount = new GameLogic().getCharacterCount();
        for (int characterIndex = 0; characterIndex < characterCount; characterIndex++) {
            GameLogic gameLogic = new GameLogic();
            gameLogic.selectCharacter(characterIndex);
            if (gameLogic.getSelectedCharacterIndex() != characterIndex) {
                throw new IllegalStateException("Character selection failed for index " + characterIndex);
            }
            gameLogic.startGame();
            if (characterIndex == 4 && !(playerField.get(gameLogic) instanceof Character_Sir_Rakki)) {
                throw new IllegalStateException("Sire Rakki must use his own character class");
            }

            for (int frame = 0; frame < 900; frame++) {
                if (frame % 90 == 5) {
                    gameLogic.triggerAbility((frame / 90) % AbilityManager.EQUIPPED_SLOT_COUNT);
                }
                gameLogic.update(1.0 / 60.0);
            }
        }
    }
}
