public class TemplateCharacter extends Player {
    private static final String SPRITE_PATH = "/main/resources/character/temp_sheet.png";
    private static final double SPEED = 200.0;
    private static final double ANIMATION_SPEED = 5.0;
    private static final int SPRITE_SCALE = 2;
    private static final int SPRITE_WIDTH = 16;
    private static final int SPRITE_HEIGHT = 18;
    private static final double MAX_HEALTH = 100.0;

    public TemplateCharacter() {
        super(SPRITE_PATH, SPEED, ANIMATION_SPEED,
            SPRITE_SCALE, SPRITE_WIDTH, SPRITE_HEIGHT, MAX_HEALTH);
    }
}