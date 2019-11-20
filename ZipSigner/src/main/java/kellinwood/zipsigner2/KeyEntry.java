package kellinwood.zipsigner2;

/**
 */
public class KeyEntry {
    private long id;
    private String displayName;
    private boolean hasPassword = false;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public String toString() {
        return displayName;
    }
}
