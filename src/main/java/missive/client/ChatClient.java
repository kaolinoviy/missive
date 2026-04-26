package missive.client;

import missive.common.crypto.CryptoManager;
import missive.common.protocol.Packet;
import missive.common.protocol.PacketType;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// background thread that maintains the socket connection and dispatches incoming packets
public class ChatClient extends Thread {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final String host;
    private final int port;

    private int userId = -1;
    private String username;

    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final CryptoManager cryptoManager;

    // userId -> base64 public key cache
    private final Map<Integer, String> publicKeyCache = new HashMap<>();

    // rooms received in AUTH_RESPONSE, cached for ChatController
    private List<Packet.RoomRecord> cachedRooms = new ArrayList<>();

    private volatile boolean connected = false;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.cryptoManager = new CryptoManager();
        setDaemon(true);
    }

    public void initCrypto() throws Exception {
        cryptoManager.init();
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        start();
    }

    @Override
    public void run() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                Packet packet = Packet.fromJson(line);
                dispatch(packet);
            }
        } catch (IOException e) {
            if (connected) {
                connected = false;
                for (MessageListener l : listeners) l.onConnectionLost();
            }
        }
    }

    private void dispatch(Packet p) {
        for (MessageListener l : listeners) {
            l.onPacketReceived(p);
        }
    }

    public void send(Packet packet) {
        if (out != null) out.println(packet.toJson());
    }

    public void sendAuth(String username, String password) {
        Packet p = Packet.of(PacketType.AUTH_REQUEST);
        p.setUsername(username);
        p.setPassword(password);
        send(p);
    }

    public void sendRegister(String username, String password) {
        Packet p = Packet.of(PacketType.REGISTER_REQUEST);
        p.setUsername(username);
        p.setPassword(password);
        send(p);
    }

    public void requestHistory(int roomId, int offset) {
        Packet p = Packet.of(PacketType.HISTORY_REQUEST);
        p.setRoomId(roomId);
        p.setOffset(offset);
        p.setLimit(50);
        send(p);
    }

    public void requestRoomMembers(int roomId) {
        Packet p = Packet.of(PacketType.ROOM_MEMBERS);
        p.setRoomId(roomId);
        send(p);
    }

    // send a plain (unencrypted) message
    public void sendMessage(int roomId, String content) {
        Packet p = Packet.of(PacketType.MESSAGE_SEND);
        p.setRoomId(roomId);
        p.setContent(content);
        p.setEncrypted(false);
        send(p);
    }

    // send an E2E-encrypted message (hybrid RSA+AES per recipient)
    public void sendEncryptedMessage(int roomId, String content, List<Packet.UserRecord> members) {
        try {
            Map<String, String> encKeys = new HashMap<>();
            CryptoManager.EncryptedMessage enc = null;

            for (Packet.UserRecord member : members) {
                String pubKeyB64 = publicKeyCache.get(member.id);
                if (pubKeyB64 == null) continue;
                var recipientKey = cryptoManager.publicKeyFromBase64(pubKeyB64);
                enc = cryptoManager.encrypt(content, recipientKey);
                encKeys.put(String.valueOf(member.id), enc.encryptedKey);
            }

            if (enc == null) {
                // fallback to plain if no keys available
                sendMessage(roomId, content);
                return;
            }

            Packet p = Packet.of(PacketType.MESSAGE_SEND);
            p.setRoomId(roomId);
            p.setContent(content); // also send plain for self-display
            p.setEncrypted(true);
            p.setEncryptedContent(enc.ciphertext);
            p.setIv(enc.iv);
            p.setEncryptedKeys(encKeys);
            send(p);

        } catch (Exception e) {
            // fallback to plain
            sendMessage(roomId, content);
        }
    }

    public void uploadPublicKey() {
        Packet p = Packet.of(PacketType.KEY_UPLOAD);
        p.setPublicKey(cryptoManager.getPublicKeyBase64());
        send(p);
    }

    public void requestPublicKey(int targetUserId) {
        Packet p = Packet.of(PacketType.PUBLIC_KEY_REQUEST);
        p.setUserId(targetUserId);
        send(p);
    }

    public void createRoom(String name, boolean isPrivate) {
        Packet p = Packet.of(PacketType.CREATE_ROOM);
        p.setRoomName(name);
        p.setPrivate(isPrivate);
        send(p);
    }

    public void startDm(int targetUserId) {
        Packet p = Packet.of(PacketType.START_DM);
        p.setTargetUserId(targetUserId);
        send(p);
    }

    public void searchMessages(int roomId, String query) {
        Packet p = Packet.of(PacketType.SEARCH_REQUEST);
        p.setRoomId(roomId);
        p.setQuery(query);
        send(p);
    }

    public void sendTyping(int roomId) {
        Packet p = Packet.of(PacketType.TYPING);
        p.setRoomId(roomId);
        send(p);
    }

    public void logout() {
        send(Packet.of(PacketType.LOGOUT));
        disconnect();
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public void cachePublicKey(int uid, String key) {
        publicKeyCache.put(uid, key);
    }

    public String getCachedPublicKey(int uid) {
        return publicKeyCache.get(uid);
    }

    public void addListener(MessageListener l) { listeners.add(l); }
    public void removeListener(MessageListener l) { listeners.remove(l); }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isConnected() { return connected; }
    public CryptoManager getCryptoManager() { return cryptoManager; }

    public List<Packet.RoomRecord> getCachedRooms() { return cachedRooms; }
    public void setCachedRooms(List<Packet.RoomRecord> rooms) { this.cachedRooms = rooms; }
}
