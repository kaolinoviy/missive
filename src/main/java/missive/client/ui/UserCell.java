package missive.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import missive.common.protocol.Packet.UserRecord;

public class UserCell extends ListCell<UserRecord> {

    private final Circle dot;
    private final Label nameLabel;
    private final HBox layout;

    public UserCell() {
        dot = new Circle(4);
        nameLabel = new Label();
        nameLabel.getStyleClass().add("user-name");

        layout = new HBox(8, dot, nameLabel);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(4, 12, 4, 12));

        setBackground(Background.EMPTY);
        setPadding(new Insets(0));
    }

    @Override
    protected void updateItem(UserRecord u, boolean empty) {
        super.updateItem(u, empty);
        if (empty || u == null) {
            setGraphic(null);
            setTooltip(null);
            return;
        }

        nameLabel.setText(u.username);
        try {
            dot.setFill(Color.web(u.avatarColor != null ? u.avatarColor : "#5fb878"));
        } catch (Exception e) {
            dot.setFill(Color.web("#5fb878"));
        }
        dot.setOpacity(u.online ? 1.0 : 0.35);
        nameLabel.setOpacity(u.online ? 1.0 : 0.55);

        setGraphic(layout);
        setTooltip(new javafx.scene.control.Tooltip(
                u.online ? "online · double-click to DM" : "offline · double-click to DM"));
    }
}
