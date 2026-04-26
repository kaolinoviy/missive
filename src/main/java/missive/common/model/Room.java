package missive.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private boolean isPrivate;
    private boolean isDm;
    private int unreadCount;
    private List<User> members = new ArrayList<>();

    public Room() {}

    public Room(int id, String name, String description, boolean isPrivate, boolean isDm) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.isDm = isDm;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public boolean isDm() { return isDm; }
    public void setDm(boolean dm) { isDm = dm; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }

    public void incrementUnread() { unreadCount++; }
    public void clearUnread() { unreadCount = 0; }

    @Override
    public String toString() { return name; }
}
