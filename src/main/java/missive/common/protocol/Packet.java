package missive.common.protocol;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;

// single protocol unit, sent as one JSON line over TCP
public class Packet {

    private static final Gson GSON = new Gson();

    private PacketType type;

    // auth fields
    private String username;
    private String password;
    private String token;
    private boolean success;
    private String error;

    // user fields
    private int userId;
    private String senderName;
    private String avatarColor;
    private List<UserRecord> users;

    // room fields
    private int roomId;
    private String roomName;
    private boolean isPrivate;
    private boolean isDm;
    private List<RoomRecord> rooms;

    // message fields
    private int messageId;
    private String content;
    private boolean encrypted;
    private String encryptedContent;
    private String iv;
    private Map<String, String> encryptedKeys; // userId -> base64(RSA(AES_key))
    private long timestamp;
    private List<MessageRecord> messages;

    // key exchange
    private String publicKey;

    // pagination
    private int offset;
    private int limit;

    // search / dm
    private String query;
    private int targetUserId;

    // ---

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Packet fromJson(String json) {
        return GSON.fromJson(json, Packet.class);
    }

    // factory helpers
    public static Packet of(PacketType type) {
        Packet p = new Packet();
        p.type = type;
        p.timestamp = System.currentTimeMillis();
        return p;
    }

    public static Packet error(String message) {
        Packet p = of(PacketType.ERROR);
        p.error = message;
        return p;
    }

    // inner record types (simple DTOs for serialization)
    public static class UserRecord {
        public int id;
        public String username;
        public String avatarColor;
        public boolean online;

        public UserRecord() {}
        public UserRecord(int id, String username, String avatarColor, boolean online) {
            this.id = id;
            this.username = username;
            this.avatarColor = avatarColor;
            this.online = online;
        }
    }

    public static class RoomRecord {
        public int id;
        public String name;
        public String description;
        public boolean isPrivate;
        public boolean isDm;
        public int unread;

        public RoomRecord() {}
        public RoomRecord(int id, String name, String description, boolean isPrivate, boolean isDm) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.isPrivate = isPrivate;
            this.isDm = isDm;
        }
    }

    public static class MessageRecord {
        public int id;
        public int roomId;
        public int senderId;
        public String senderName;
        public String avatarColor;
        public String content;
        public String type; // "chat" or "system"
        public boolean encrypted;
        public String encryptedContent;
        public String iv;
        public Map<String, String> encryptedKeys;
        public long timestamp;

        public MessageRecord() {}
    }

    // getters and setters

    public PacketType getType() { return type; }
    public void setType(PacketType type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public List<UserRecord> getUsers() { return users; }
    public void setUsers(List<UserRecord> users) { this.users = users; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public boolean isDm() { return isDm; }
    public void setDm(boolean dm) { isDm = dm; }

    public List<RoomRecord> getRooms() { return rooms; }
    public void setRooms(List<RoomRecord> rooms) { this.rooms = rooms; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public Map<String, String> getEncryptedKeys() { return encryptedKeys; }
    public void setEncryptedKeys(Map<String, String> encryptedKeys) { this.encryptedKeys = encryptedKeys; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<MessageRecord> getMessages() { return messages; }
    public void setMessages(List<MessageRecord> messages) { this.messages = messages; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getTargetUserId() { return targetUserId; }
    public void setTargetUserId(int targetUserId) { this.targetUserId = targetUserId; }
}
