import java.lang.reflect.Field;

public class RestartSmokeTest {
    public static void main(String[] args) throws Exception {
        GameLogic gameLogic = new GameLogic();
        gameLogic.startGame();

        Player firstPlayer = getPlayer(gameLogic);
        firstPlayer.takeDamage(firstPlayer.getMaxHealth() + 100.0);
        gameLogic.update(0.016);
        if (!gameLogic.isGameOver()) {
            throw new IllegalStateException("Expected the run to enter game over");
        }

        gameLogic.startGame();
        Player restartedPlayer = getPlayer(gameLogic);
        if (!gameLogic.isGameStarted() || gameLogic.isGameOver()) {
            throw new IllegalStateException("Try Again should start a fresh active run");
        }
        if (restartedPlayer == firstPlayer) {
            throw new IllegalStateException("Restart should create a fresh player");
        }
        if (restartedPlayer.getHealth() != restartedPlayer.getMaxHealth()) {
            throw new IllegalStateException("Restarted player should have full health");
        }
    }

    private static Player getPlayer(GameLogic gameLogic) throws Exception {
        Field playerField = GameLogic.class.getDeclaredField("player");
        playerField.setAccessible(true);
        return (Player) playerField.get(gameLogic);
    }
}
