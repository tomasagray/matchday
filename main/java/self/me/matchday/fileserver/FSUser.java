package self.me.matchday.fileserver;

import org.jetbrains.annotations.NotNull;

public abstract class FSUser
{
    // Fields
    // ---------------------------------
    private final String userName;
    private String password;
    private boolean keepLoggedIn;

    // Constructors
    // --------------------------------------------------------
    public FSUser(String name) {
        this.userName = name;
    }
    public FSUser(String name, String password) {
        this(name);
        this.password = password;
    }
    public FSUser(String name, String password, boolean keepLoggedIn) {
        this(name, password);
        this.keepLoggedIn = keepLoggedIn;
    }


    // Getters & Setters
    // ---------------------------------------------------
    public String getUserName() {
        return userName;
    }

    String getPassword() {
        return password;
    }
    public boolean isKeepLoggedIn() {
        return keepLoggedIn;
    }

    /**
     * Retrieves login data as a pure array of bytes, for easy writing to
     * streams.
     *
     * @return The login data as a raw array of bytes
     */
    @NotNull
    abstract byte[] getLoginDataByteArray();


    public void setPassword(String password) {
        this.password = password;
    }
    public void setKeepLoggedIn(boolean keepLoggedIn) {
        this.keepLoggedIn = keepLoggedIn;
    }


    // Overrides
    // ------------------------------------------------------------------
    @Override
    public String toString() {
        return
                "UserName: " + userName + "\n" +
                "Password: " + password.replaceAll("[A-z,0-9]", "*") + "\n" +
                "Keep Logged In: " + keepLoggedIn;
    }
}
