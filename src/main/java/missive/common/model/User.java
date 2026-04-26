package missive.common.model;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String avatarColor;
    private boolean online;

    public User() {}

    public User(int id, String username, String avatarColor) {
        this.id = id;
        this.username = username;
        this.avatarColor = avatarColor;
        this.online = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    @Override
    public String toString() {
        return username;
    }
}
