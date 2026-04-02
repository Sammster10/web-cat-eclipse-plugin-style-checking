package webcat.stylechecking;

public enum StyleViolationSource {
    PMD("PMD"),
    CHECKSTYLE("CheckStyle");

    private final String displayName;

    StyleViolationSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
