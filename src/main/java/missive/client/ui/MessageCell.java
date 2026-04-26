package missive.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import missive.common.model.ChatMessage;
import missive.common.model.Message;
import missive.common.model.SystemMessage;

// renders Message polymorphically — ChatMessage vs SystemMessage get different layouts
public class MessageCell extends ListCell<Message> {

    private final HBox chatLayout;
    private final VBox contentBox;
    private final Label senderLabel;
    private final Label bodyLabel;
    private final Label timeLabel;
    private final Circle avatar;

    private final HBox systemLayout;
    private final Label systemLabel;

    public MessageCell() {
        setBackground(Background.EMPTY);
        setPadding(new Insets(0));

        avatar = new Circle(14);
        avatar.setStrokeWidth(0);

        senderLabel = new Label();
        senderLabel.getStyleClass().add("msg-sender");

        timeLabel = new Label();
        timeLabel.getStyleClass().add("msg-time");

        bodyLabel = new Label();
        bodyLabel.getStyleClass().add("msg-body");
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(6, senderLabel, timeLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        contentBox = new VBox(2, header, bodyLabel);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        chatLayout = new HBox(10, avatar, contentBox);
        chatLayout.setAlignment(Pos.TOP_LEFT);
        chatLayout.setPadding(new Insets(6, 14, 6, 14));

        // system message layout — centered line
        systemLabel = new Label();
        systemLabel.getStyleClass().add("msg-system");
        systemLabel.setMaxWidth(Double.MAX_VALUE);
        systemLabel.setAlignment(Pos.CENTER);

        systemLayout = new HBox(systemLabel);
        systemLayout.setPadding(new Insets(4, 14, 4, 14));
        systemLayout.setAlignment(Pos.CENTER);
        HBox.setHgrow(systemLabel, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(Message msg, boolean empty) {
        super.updateItem(msg, empty);
        if (empty || msg == null) {
            setGraphic(null);
            return;
        }

        if (msg instanceof ChatMessage cm) {
            senderLabel.setText(cm.getSenderName());
            timeLabel.setText(cm.getFormattedTime());
            bodyLabel.setText(cm.getContent());

            Color c = parseColor(cm.getAvatarColor());
            avatar.setFill(c);
            avatar.setVisible(true);

            setGraphic(chatLayout);
        } else if (msg instanceof SystemMessage) {
            systemLabel.setText(msg.getFormattedDisplay());
            setGraphic(systemLayout);
        }
    }

    private Color parseColor(String hex) {
        try {
            return Color.web(hex != null ? hex : "#5fb878");
        } catch (Exception e) {
            return Color.web("#5fb878");
        }
    }
}
