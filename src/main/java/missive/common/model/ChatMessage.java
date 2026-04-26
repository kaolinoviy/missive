package missive.common.model;

public class ChatMessage extends Message {

    private int senderId;
    private String senderName;
    private String avatarColor;
    private String content;
    private boolean encrypted;

    public ChatMessage(int id, int roomId, int senderId, String senderName,
                       String avatarColor, String content, long timestamp, boolean encrypted) {
        super(id, roomId, timestamp);
        this.senderId = senderId;
        this.senderName = senderName;
        this.avatarColor = avatarColor;
        this.content = content;
        this.encrypted = encrypted;
    }

    @Override
    public String getContent() { return content; }

    @Override
    public String getType() { return "chat"; }

    @Override
    public String getFormattedDisplay() {
        return "[" + getFormattedTime() + "] " + senderName + ": " + content;
    }

    public void setContent(String content) { this.content = content; }

    public int getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getAvatarColor() { return avatarColor; }
    public boolean isEncrypted() { return encrypted; }
}
