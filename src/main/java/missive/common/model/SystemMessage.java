package missive.common.model;

public class SystemMessage extends Message {

    private String text;

    public SystemMessage(int roomId, String text, long timestamp) {
        super(0, roomId, timestamp);
        this.text = text;
    }

    @Override
    public String getContent() { return text; }

    @Override
    public String getType() { return "system"; }

    @Override
    public String getFormattedDisplay() {
        return "── " + text + " ──";
    }
}
