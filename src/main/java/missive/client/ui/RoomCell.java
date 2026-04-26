package missive.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import missive.common.model.Room;

public class RoomCell extends ListCell<Room> {

    private final Label nameLabel;
    private final Label badgeLabel;
    private final HBox layout;

    public RoomCell() {
        nameLabel = new Label();
        nameLabel.getStyleClass().add("room-name");

        badgeLabel = new Label();
        badgeLabel.getStyleClass().add("unread-badge");
        badgeLabel.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        layout = new HBox(4, nameLabel, spacer, badgeLabel);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(6, 10, 6, 10));

        setBackground(javafx.scene.layout.Background.EMPTY);
        setPadding(new Insets(0));
    }

    @Override
    protected void updateItem(Room room, boolean empty) {
        super.updateItem(room, empty);
        if (empty || room == null) {
            setGraphic(null);
            return;
        }

        String prefix = room.isDm() ? "@ " : "# ";
        nameLabel.setText(prefix + room.getName());

        if (room.getUnreadCount() > 0) {
            badgeLabel.setText(String.valueOf(room.getUnreadCount()));
            badgeLabel.setVisible(true);
        } else {
            badgeLabel.setVisible(false);
        }

        setGraphic(layout);
    }
}
