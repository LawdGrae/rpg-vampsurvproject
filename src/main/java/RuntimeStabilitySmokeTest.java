public class RuntimeStabilitySmokeTest {
    public static void main(String[] args) {
        for (int characterIndex = 0; characterIndex < 4; characterIndex++) {
            GameLogic gameLogic = new GameLogic();
            gameLogic.selectCharacter(characterIndex);
            gameLogic.startGame();

            for (int frame = 0; frame < 900; frame++) {
                if (frame % 90 == 5) {
                    gameLogic.triggerAbility((frame / 90) % AbilityManager.EQUIPPED_SLOT_COUNT);
                }
                gameLogic.update(1.0 / 60.0);
            }
        }
    }
}
