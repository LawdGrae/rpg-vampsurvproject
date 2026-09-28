public class Character_Ziea extends Player {
    private static final String SPRITE_PATH = "/main/resources/character/CharZiea.png";
    private static final double SPEED = 200.0;
    private static final double ANIMATION_SPEED = 5.0;
    private static final int SPRITE_SCALE = 1;
    private static final int SPRITE_WIDTH = 64;
    private static final int SPRITE_HEIGHT = 64;
    private static final double MAX_HEALTH = 100.0;

    public Character_Ziea() {
        super(SPRITE_PATH, SPEED, ANIMATION_SPEED,
            SPRITE_SCALE, SPRITE_WIDTH, SPRITE_HEIGHT, MAX_HEALTH);
    }
} 