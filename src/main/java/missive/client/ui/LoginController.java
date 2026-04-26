package missive.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import missive.client.ChatClient;
import missive.client.MessageListener;
import missive.common.protocol.Packet;
import missive.common.protocol.PacketType;

public class LoginController implements MessageListener {

    // default public server. override with env var MISSIVE_SERVER=host:port for dev/self-hosted.
    private static final String DEFAULT_SERVER = "45.150.32.198:9090";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Label statusLabel;
    @FXML private Label serverLabel;

    private ChatClient client;

    @FXML
    public void initialize() {
        statusLabel.setText("");
        loginBtn.setDefaultButton(true);
        if (serverLabel != null) serverLabel.setText("server: " + resolveServer());
    }

    private static String resolveServer() {
        String env = System.getenv("MISSIVE_SERVER");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_SERVER;
    }

    @FXML
    private void handleLogin() {
        doConnect(false);
    }

    @FXML
    private void handleRegister() {
        doConnect(true);
    }

    private void doConnect(boolean register) {
        String serverAddr = resolveServer();
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("username and password required", true);
            return;
        }

        String host;
        int port;
        try {
            String[] parts = serverAddr.split(":");
            host = parts[0];
            port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;
        } catch (Exception e) {
            setStatus("invalid server address", true);
            return;
        }

        setStatus("connecting...", false);
        loginBtn.setDisable(true);
        registerBtn.setDisable(true);

        final String h = host;
        final int p = port;
        Thread t = new Thread(() -> {
            try {
                client = new ChatClient(h, p);
                client.initCrypto();
                client.addListener(this);
                client.connect();

                if (register) {
                    client.sendRegister(user, pass);
                } else {
                    client.sendAuth(user, pass);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setStatus("connection failed: " + ex.getMessage(), true);
                    loginBtn.setDisable(false);
                    registerBtn.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            if (packet.getType() == PacketType.AUTH_RESPONSE) {
                handleAuthResponse(packet);
            } else if (packet.getType() == PacketType.REGISTER_RESPONSE) {
                handleRegisterResponse(packet);
            } else if (packet.getType() == PacketType.ERROR) {
                setStatus(packet.getError(), true);
                loginBtn.setDisable(false);
                registerBtn.setDisable(false);
            }
        });
    }

    private void handleAuthResponse(Packet p) {
        if (!p.isSuccess()) {
            setStatus(p.getError() != null ? p.getError() : "login failed", true);
            loginBtn.setDisable(false);
            registerBtn.setDisable(false);
            return;
        }
        client.setUserId(p.getUserId());
        client.setUsername(p.getSenderName());
        if (p.getRooms() != null) client.setCachedRooms(p.getRooms());
        client.uploadPublicKey();
        client.removeListener(this);
        try {
            App.showChat(client);
        } catch (Exception e) {
            setStatus("failed to open chat: " + e.getMessage(), true);
        }
    }

    private void handleRegisterResponse(Packet p) {
        if (p.isSuccess()) {
            setStatus("registered! logging in...", false);
            // now log in
            client.sendAuth(usernameField.getText().trim(), passwordField.getText());
        } else {
            setStatus(p.getError() != null ? p.getError() : "registration failed", true);
            loginBtn.setDisable(false);
            registerBtn.setDisable(false);
        }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            setStatus("connection lost", true);
            loginBtn.setDisable(false);
            registerBtn.setDisable(false);
        });
    }
}
