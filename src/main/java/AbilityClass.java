public enum AbilityClass {
    ASSASSIN("Assassin"),
    BLACK_KNIGHT("Black Knight"),
    PRIEST("Priest"),
    RANGER("Ranger"),
    WARLOCK("Warlock"),
    ELEMENTALIST("Elementalist");

    private final String displayName;

    AbilityClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
