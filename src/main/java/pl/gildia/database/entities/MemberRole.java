package pl.gildia.database.entities;

/**
 * Enum określający role członków gildii
 */
public enum MemberRole {
    LEADER("Lider"),
    DEPUTY("Zastępca"),
    MEMBER("Członek");

    private final String displayName;

    MemberRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MemberRole fromString(String str) {
        if (str == null) {
            return MEMBER;
        }

        try {
            return MemberRole.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
