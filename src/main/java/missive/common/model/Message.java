package missive.common.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// abstract base for all message types shown in the UI
public abstract class Message implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    protected int id;
    protected int roomId;
    protected long timestamp;

    public Message(int id, int roomId, long timestamp) {
        this.id = id;
        this.roomId = roomId;
        this.timestamp = timestamp;
    }

    public abstract String getContent();
    public abstract String getType(); // "chat" or "system"
    public abstract String getFormattedDisplay();

    public String getFormattedTime() {
        return TIME_FMT.format(Instant.ofEpochMilli(timestamp));
    }

    public int getId() { return id; }
    public int getRoomId() { return roomId; }
    public long getTimestamp() { return timestamp; }
}
