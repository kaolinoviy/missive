package missive.client.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import missive.client.ChatClient;
import missive.client.MessageListener;
import missive.common.model.*;
import missive.common.protocol.Packet;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatController implements MessageListener {

    @FXML private ListView<Room> roomList;
    @FXML private ListView<Message> messageList;
    @FXML private ListView<Packet.UserRecord> userList;
    @FXML private TextField messageInput;
    @FXML private TextField searchField;
    @FXML private Button sendBtn;
    @FXML private Button clearSearchBtn;
    @FXML private Label roomHeaderLabel;
    @FXML private Label roomSubLabel;
    @FXML private Label usernameLabel;
    @FXML private Label typingLabel;
    @FXML private VBox sidebar;

    private ChatClient client;
    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final ObservableList<Packet.UserRecord> onlineUsers = FXCollections.observableArrayList();
    private final Map<Integer, ObservableList<Message>> roomMessages = new HashMap<>();

    // dedup incoming messages — server is broadcast-based and may rarely double-send
    private final Set<Integer> seenMessageIds = new HashSet<>();
    // sliding-window of recent notification timestamps (ms) — throttles notify sound
    private final LinkedList<Long> recentNotifyTimes = new LinkedList<>();
    // notification sound clip (loaded lazily once)
    private AudioClip notifySound;

    private Room currentRoom;
    private List<Packet.UserRecord> currentMembers = new ArrayList<>();
    private boolean inSearchMode = false;

    @FXML
    public void initialize() {
        roomList.setItems(rooms);
        roomList.setCellFactory(lv -> new RoomCell());

        messageList.setItems(messages);
        messageList.setCellFactory(lv -> new MessageCell());

        userList.setItems(onlineUsers);
        userList.setCellFactory(lv -> new UserCell());

        roomList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) switchRoom(selected);
        });

        // double-click on a user → start DM
        userList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Packet.UserRecord u = userList.getSelectionModel().getSelectedItem();
                if (u != null && client != null && u.id != client.getUserId()) {
                    client.startDm(u.id);
                }
            }
        });

        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) handleSend();
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleSearch();
            if (e.getCode() == KeyCode.ESCAPE) clearSearch();
        });

        if (clearSearchBtn != null) clearSearchBtn.setVisible(false);
        typingLabel.setText("");
        loadNotifySound();
    }

    private void loadNotifySound() {
        try {
            var url = getClass().getResource("/sounds/notify.wav");
            if (url != null) notifySound = new AudioClip(url.toExternalForm());
        } catch (Exception ignored) {}
    }

    // ring at most 3 times per 5-second window
    private void maybePlayNotify() {
        if (notifySound == null) return;
        long now = System.currentTimeMillis();
        // drop entries older than 5s from the head — LinkedList shines as a deque here
        while (!recentNotifyTimes.isEmpty() && now - recentNotifyTimes.peekFirst() > 5000) {
            recentNotifyTimes.pollFirst();
        }
        if (recentNotifyTimes.size() >= 3) return;
        recentNotifyTimes.addLast(now);
        notifySound.play(0.45);
    }

    public void init(ChatClient client) {
        this.client = client;
        client.addListener(this);
        usernameLabel.setText("@" + client.getUsername());

        List<Packet.RoomRecord> initial = client.getCachedRooms();
        if (initial != null && !initial.isEmpty()) {
            for (Packet.RoomRecord r : initial) {
                rooms.add(new Room(r.id, r.name, r.description, r.isPrivate, r.isDm));
            }
            roomList.getSelectionModel().select(0);
        }
    }

    // ── input / sending ──

    @FXML
    private void handleSend() {
        if (currentRoom == null) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        messageInput.clear();

        if (currentRoom.isDm() && allHaveKeys()) {
            client.sendEncryptedMessage(currentRoom.getId(), text, currentMembers);
        } else {
            client.sendMessage(currentRoom.getId(), text);
        }
    }

    private boolean allHaveKeys() {
        for (Packet.UserRecord m : currentMembers) {
            if (m.id == client.getUserId()) continue;
            if (client.getCachedPublicKey(m.id) == null) return false;
        }
        return currentMembers.size() > 1;
    }

    // ── search ──

    @FXML
    private void handleSearch() {
        if (currentRoom == null) return;
        String q = searchField.getText().trim();
        if (q.isEmpty()) { clearSearch(); return; }
        client.searchMessages(currentRoom.getId(), q);
        inSearchMode = true;
        if (clearSearchBtn != null) clearSearchBtn.setVisible(true);
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        inSearchMode = false;
        if (clearSearchBtn != null) clearSearchBtn.setVisible(false);
        if (currentRoom != null) {
            ObservableList<Message> cached = roomMessages.get(currentRoom.getId());
            if (cached != null) {
                messageList.setItems(cached);
                scrollToBottom();
            }
        }
    }

    // ── toolbar ──

    @FXML
    private void handleNewRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("new channel");
        dialog.setHeaderText(null);
        dialog.setContentText("channel name:");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/terminal.css").toExternalForm());
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) client.createRoom(name.trim().toLowerCase(), false);
        });
    }

    @FXML
    private void handleExportHistory() {
        if (currentRoom == null || messages.isEmpty()) return;
        try {
            Path exportDir = Paths.get(System.getProperty("user.home"), ".missive", "exports");
            Files.createDirectories(exportDir);
            String filename = currentRoom.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt";
            Path file = exportDir.resolve(filename);

            // tally messages per sender — TreeMap keeps senders alphabetically sorted
            TreeMap<String, Integer> stats = new TreeMap<>();
            for (Message m : messages) {
                if (m instanceof ChatMessage cm) {
                    stats.merge(cm.getSenderName(), 1, Integer::sum);
                }
            }

            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                w.write("# " + currentRoom.getName() + " — exported " + LocalDateTime.now());
                w.newLine();
                w.write("# total messages: " + messages.size());
                w.newLine();
                w.write("# breakdown by sender:");
                w.newLine();
                for (var e : stats.entrySet()) {
                    w.write("#   " + e.getKey() + ": " + e.getValue());
                    w.newLine();
                }
                w.write("# ---");
                w.newLine();
                for (Message m : messages) {
                    w.write(m.getFormattedDisplay());
                    w.newLine();
                }
            }
            showAlert("exported to " + file);
        } catch (IOException e) {
            showAlert("export failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        client.logout();
        try { App.showLogin(); } catch (Exception ignored) {}
    }

    // ── room switching ──

    private void switchRoom(Room room) {
        currentRoom = room;
        room.clearUnread();

        if (room.isDm()) {
            roomHeaderLabel.setText("@ " + room.getName());
            roomSubLabel.setText("direct message · end-to-end encrypted");
        } else {
            roomHeaderLabel.setText("# " + room.getName());
            roomSubLabel.setText(room.getDescription() != null && !room.getDescription().isEmpty()
                    ? room.getDescription() : "channel");
        }

        typingLabel.setText("");
        clearSearch();

        ObservableList<Message> cached = roomMessages.computeIfAbsent(room.getId(),
                k -> FXCollections.observableArrayList());
        messageList.setItems(cached);
        messages.setAll(cached);

        if (cached.isEmpty()) client.requestHistory(room.getId(), 0);
        client.requestRoomMembers(room.getId());

        roomList.refresh();
    }

    // ── packet handling ──

    @Override
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> handlePacket(packet));
    }

    private void handlePacket(Packet p) {
        switch (p.getType()) {
            case ROOM_CREATED        -> handleRoomCreated(p);
            case MESSAGE_RECEIVE     -> handleIncomingMessage(p);
            case HISTORY_RESPONSE    -> handleHistory(p);
            case ROOM_MEMBERS        -> handleRoomMembers(p);
            case PUBLIC_KEY_RESPONSE -> handlePublicKey(p);
            case USER_JOINED         -> handleUserJoined(p);
            case USER_LEFT           -> handleUserLeft(p);
            case TYPING              -> handleTyping(p);
            case SEARCH_RESPONSE     -> handleSearchResponse(p);
            case ERROR               -> showAlert("server: " + p.getError());
            default                  -> {}
        }
    }

    private void handleRoomCreated(Packet p) {
        if (!p.isSuccess()) return;
        // skip if already in list (happens for DMs created by other party)
        for (Room existing : rooms) {
            if (existing.getId() == p.getRoomId()) {
                roomList.getSelectionModel().select(existing);
                return;
            }
        }
        Room newRoom = new Room(p.getRoomId(), p.getRoomName(), "", false, p.isDm());
        rooms.add(newRoom);
        roomList.getSelectionModel().select(newRoom);
    }

    private void handleIncomingMessage(Packet p) {
        // skip duplicates (defensive — HashSet O(1) lookup)
        if (p.getMessageId() > 0 && !seenMessageIds.add(p.getMessageId())) return;

        ChatMessage msg = new ChatMessage(
                p.getMessageId(), p.getRoomId(),
                p.getUserId(), p.getSenderName(),
                p.getAvatarColor() != null ? p.getAvatarColor() : "#5fb878",
                p.getContent(),
                p.getTimestamp(),
                p.isEncrypted()
        );

        ObservableList<Message> roomMsgs = roomMessages.computeIfAbsent(
                p.getRoomId(), k -> FXCollections.observableArrayList());
        roomMsgs.add(msg);

        boolean fromMe = client != null && p.getUserId() == client.getUserId();
        boolean activeRoom = currentRoom != null && currentRoom.getId() == p.getRoomId();

        if (activeRoom && !inSearchMode) {
            messageList.setItems(roomMsgs);
            scrollToBottom();
        }
        if (!activeRoom) {
            rooms.stream().filter(r -> r.getId() == p.getRoomId())
                    .findFirst().ifPresent(r -> {
                        r.incrementUnread();
                        roomList.refresh();
                    });
        }
        // play sound on incoming message that's not mine and not in active foreground room
        if (!fromMe && !activeRoom) maybePlayNotify();
    }

    private void handleHistory(Packet p) {
        if (p.getMessages() == null || currentRoom == null) return;
        if (currentRoom.getId() != p.getRoomId()) return;

        ObservableList<Message> roomMsgs = roomMessages.computeIfAbsent(
                p.getRoomId(), k -> FXCollections.observableArrayList());
        roomMsgs.clear();

        for (Packet.MessageRecord r : p.getMessages()) {
            ChatMessage m = new ChatMessage(r.id, r.roomId, r.senderId, r.senderName,
                    r.avatarColor != null ? r.avatarColor : "#5fb878",
                    r.content, r.timestamp, r.encrypted);
            roomMsgs.add(m);
        }

        if (!inSearchMode) {
            messageList.setItems(roomMsgs);
            scrollToBottom();
        }
    }

    private void handleSearchResponse(Packet p) {
        if (p.getMessages() == null) return;
        ObservableList<Message> result = FXCollections.observableArrayList();
        for (Packet.MessageRecord r : p.getMessages()) {
            result.add(new ChatMessage(r.id, r.roomId, r.senderId, r.senderName,
                    r.avatarColor != null ? r.avatarColor : "#5fb878",
                    r.content, r.timestamp, r.encrypted));
        }
        if (result.isEmpty()) {
            result.add(new SystemMessage(p.getRoomId(),
                    "no messages found for \"" + p.getQuery() + "\"",
                    System.currentTimeMillis()));
        }
        messageList.setItems(result);
    }

    private void handleRoomMembers(Packet p) {
        if (p.getUsers() == null) return;
        currentMembers = new ArrayList<>(p.getUsers());
        onlineUsers.setAll(currentMembers);
        for (Packet.UserRecord u : currentMembers) {
            if (u.id != client.getUserId() && client.getCachedPublicKey(u.id) == null) {
                client.requestPublicKey(u.id);
            }
        }
    }

    private void handlePublicKey(Packet p) {
        if (p.getPublicKey() != null) client.cachePublicKey(p.getUserId(), p.getPublicKey());
    }

    private void handleUserJoined(Packet p) {
        if (currentRoom != null) {
            SystemMessage sys = new SystemMessage(currentRoom.getId(),
                    p.getSenderName() + " joined", System.currentTimeMillis());
            appendSystemMessage(currentRoom.getId(), sys);
            client.requestRoomMembers(currentRoom.getId());
        }
    }

    private void handleUserLeft(Packet p) {
        if (currentRoom != null) {
            SystemMessage sys = new SystemMessage(currentRoom.getId(),
                    p.getSenderName() + " left", System.currentTimeMillis());
            appendSystemMessage(currentRoom.getId(), sys);
            onlineUsers.removeIf(u -> u.id == p.getUserId());
        }
    }

    private void handleTyping(Packet p) {
        if (currentRoom != null && currentRoom.getId() == p.getRoomId()) {
            typingLabel.setText(p.getSenderName() + " is typing…");
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> typingLabel.setText(""));
            }).start();
        }
    }

    private void appendSystemMessage(int roomId, SystemMessage msg) {
        ObservableList<Message> roomMsgs = roomMessages.computeIfAbsent(
                roomId, k -> FXCollections.observableArrayList());
        roomMsgs.add(msg);
        if (currentRoom != null && currentRoom.getId() == roomId && !inSearchMode) {
            messageList.setItems(roomMsgs);
            scrollToBottom();
        }
    }

    private void scrollToBottom() {
        if (!messageList.getItems().isEmpty()) {
            messageList.scrollTo(messageList.getItems().size() - 1);
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("missive");
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/terminal.css").toExternalForm());
        a.showAndWait();
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            showAlert("connection lost");
            try { App.showLogin(); } catch (Exception ignored) {}
        });
    }
}
