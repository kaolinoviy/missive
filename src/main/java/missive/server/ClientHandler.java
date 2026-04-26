package missive.server;

import missive.common.protocol.Packet;
import missive.common.protocol.PacketType;
import missive.server.db.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// one thread per connected client
public class ClientHandler extends Thread {

    private final Socket socket;
    private final RoomManager roomManager;
    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    private final RoomDAO roomDAO;

    private PrintWriter out;
    private BufferedReader in;

    private int userId = -1;
    private String username;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
        this.roomDAO = new RoomDAO();
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String line;
            while ((line = in.readLine()) != null) {
                Packet packet = Packet.fromJson(line);
                handle(packet);
            }
        } catch (IOException e) {
            // client disconnected
        } finally {
            disconnect();
        }
    }

    private void handle(Packet p) {
        try {
            switch (p.getType()) {
                case AUTH_REQUEST    -> handleAuth(p);
                case REGISTER_REQUEST -> handleRegister(p);
                case MESSAGE_SEND    -> handleMessage(p);
                case HISTORY_REQUEST -> handleHistory(p);
                case CREATE_ROOM     -> handleCreateRoom(p);
                case JOIN_ROOM       -> handleJoinRoom(p);
                case ROOM_MEMBERS    -> handleRoomMembers(p);
                case START_DM        -> handleStartDm(p);
                case SEARCH_REQUEST  -> handleSearch(p);
                case KEY_UPLOAD      -> handleKeyUpload(p);
                case PUBLIC_KEY_REQUEST -> handlePublicKeyRequest(p);
                case PING            -> send(Packet.of(PacketType.PONG).toJson());
                case LOGOUT          -> disconnect();
                default              -> {}
            }
        } catch (Exception e) {
            send(Packet.error(e.getMessage()).toJson());
        }
    }

    private void handleAuth(Packet p) throws Exception {
        int uid = userDAO.authenticate(p.getUsername(), p.getPassword());
        if (uid < 0) {
            Packet resp = Packet.of(PacketType.AUTH_RESPONSE);
            resp.setSuccess(false);
            resp.setError("Invalid username or password");
            send(resp.toJson());
            return;
        }

        this.userId = uid;
        this.username = p.getUsername();
        roomManager.registerUser(userId, this);

        // auto-join public rooms
        List<missive.common.model.Room> publicRooms = roomDAO.findAll();
        for (missive.common.model.Room r : publicRooms) {
            if (!roomDAO.isMember(r.getId(), userId)) {
                roomDAO.addMember(r.getId(), userId);
            }
            roomManager.joinRoom(r.getId(), this);
        }

        Packet resp = Packet.of(PacketType.AUTH_RESPONSE);
        resp.setSuccess(true);
        resp.setUserId(userId);
        resp.setSenderName(username);

        // send room list (public + user's existing DMs)
        List<Packet.RoomRecord> roomRecords = new ArrayList<>();
        for (missive.common.model.Room r : publicRooms) {
            roomRecords.add(new Packet.RoomRecord(r.getId(), r.getName(), r.getDescription(), false, false));
        }
        for (missive.common.model.Room r : roomDAO.findByUser(userId)) {
            if (r.isDm()) {
                String dmName = dmDisplayName(r.getId());
                Packet.RoomRecord rec = new Packet.RoomRecord(r.getId(), dmName, "", false, true);
                roomManager.joinRoom(r.getId(), this);
                roomRecords.add(rec);
            }
        }
        resp.setRooms(roomRecords);
        send(resp.toJson());

        // notify others
        broadcastUserJoined();
    }

    private void handleRegister(Packet p) throws Exception {
        int uid = userDAO.register(p.getUsername(), p.getPassword());
        Packet resp = Packet.of(PacketType.REGISTER_RESPONSE);
        resp.setSuccess(true);
        resp.setUserId(uid);
        send(resp.toJson());
    }

    private void handleMessage(Packet p) throws Exception {
        if (userId < 0) return;
        int roomId = p.getRoomId();

        // persist — store encrypted content if present, otherwise plain
        String stored = p.isEncrypted() ? p.getEncryptedContent() : p.getContent();
        String iv = p.getIv();
        int msgId = messageDAO.saveMessage(roomId, userId, username, stored, p.isEncrypted(), iv);

        // build broadcast packet
        Packet broadcast = Packet.of(PacketType.MESSAGE_RECEIVE);
        broadcast.setMessageId(msgId);
        broadcast.setRoomId(roomId);
        broadcast.setUserId(userId);
        broadcast.setSenderName(username);
        broadcast.setContent(p.getContent());
        broadcast.setEncrypted(p.isEncrypted());
        broadcast.setEncryptedContent(p.getEncryptedContent());
        broadcast.setIv(iv);
        broadcast.setEncryptedKeys(p.getEncryptedKeys());

        // echo back to sender too
        send(broadcast.toJson());
        roomManager.broadcast(roomId, broadcast, userId);
    }

    private void handleHistory(Packet p) throws Exception {
        int roomId = p.getRoomId();
        int limit  = p.getLimit() > 0 ? p.getLimit() : 50;
        int offset = p.getOffset();

        List<Packet.MessageRecord> messages = messageDAO.findByRoom(roomId, limit, offset);

        Packet resp = Packet.of(PacketType.HISTORY_RESPONSE);
        resp.setRoomId(roomId);
        resp.setMessages(messages);
        send(resp.toJson());
    }

    private void handleCreateRoom(Packet p) throws Exception {
        if (userId < 0) return;
        missive.common.model.Room room = new missive.common.model.Room(
                0, p.getRoomName(), "", p.isPrivate(), false
        );
        int roomId = roomDAO.save(room);
        roomDAO.addMember(roomId, userId);
        roomManager.joinRoom(roomId, this);

        Packet resp = Packet.of(PacketType.ROOM_CREATED);
        resp.setSuccess(true);
        resp.setRoomId(roomId);
        resp.setRoomName(p.getRoomName());
        send(resp.toJson());
    }

    private void handleJoinRoom(Packet p) throws Exception {
        if (userId < 0) return;
        int roomId = p.getRoomId();
        if (!roomDAO.isMember(roomId, userId)) {
            roomDAO.addMember(roomId, userId);
        }
        roomManager.joinRoom(roomId, this);

        Packet resp = Packet.of(PacketType.JOIN_ROOM);
        resp.setSuccess(true);
        resp.setRoomId(roomId);
        send(resp.toJson());
    }

    private void handleRoomMembers(Packet p) throws Exception {
        List<missive.common.model.User> members = roomDAO.getMembers(p.getRoomId());
        List<Packet.UserRecord> records = new ArrayList<>();
        for (missive.common.model.User u : members) {
            records.add(new Packet.UserRecord(u.getId(), u.getUsername(), u.getAvatarColor(),
                    roomManager.isOnline(u.getId())));
        }
        Packet resp = Packet.of(PacketType.ROOM_MEMBERS);
        resp.setRoomId(p.getRoomId());
        resp.setUsers(records);
        send(resp.toJson());
    }

    // start a 1-on-1 DM with another user (creates room if not present)
    private void handleStartDm(Packet p) throws Exception {
        if (userId < 0) return;
        int target = p.getTargetUserId();
        if (target == userId || target <= 0) return;

        missive.common.model.User other = userDAO.findById(target);
        if (other == null) {
            send(Packet.error("user not found").toJson());
            return;
        }

        missive.common.model.Room dm = roomDAO.findDmRoom(userId, target);
        boolean created = false;
        if (dm == null) {
            String name = "dm_" + Math.min(userId, target) + "_" + Math.max(userId, target);
            dm = new missive.common.model.Room(0, name, "", true, true);
            int rid = roomDAO.save(dm);
            roomDAO.addMember(rid, userId);
            roomDAO.addMember(rid, target);
            dm.setId(rid);
            created = true;
        }

        roomManager.joinRoom(dm.getId(), this);
        // also register the other handler to the room if they are online
        ClientHandler otherHandler = roomManager.getHandler(target);
        if (otherHandler != null) roomManager.joinRoom(dm.getId(), otherHandler);

        // notify requester
        Packet resp = Packet.of(PacketType.ROOM_CREATED);
        resp.setSuccess(true);
        resp.setRoomId(dm.getId());
        resp.setRoomName(other.getUsername());
        resp.setDm(true);
        send(resp.toJson());

        // notify the other party only if newly created and online
        if (created && otherHandler != null) {
            Packet notify = Packet.of(PacketType.ROOM_CREATED);
            notify.setSuccess(true);
            notify.setRoomId(dm.getId());
            notify.setRoomName(username); // show requester's name to them
            notify.setDm(true);
            otherHandler.send(notify.toJson());
        }
    }

    private void handleSearch(Packet p) throws Exception {
        if (userId < 0) return;
        String q = p.getQuery();
        if (q == null || q.isBlank()) return;
        List<Packet.MessageRecord> hits = messageDAO.search(p.getRoomId(), q.trim(), 100);
        Packet resp = Packet.of(PacketType.SEARCH_RESPONSE);
        resp.setRoomId(p.getRoomId());
        resp.setQuery(q);
        resp.setMessages(hits);
        send(resp.toJson());
    }

    // for a DM room, return the *other* user's username for display
    private String dmDisplayName(int roomId) throws Exception {
        for (missive.common.model.User u : roomDAO.getMembers(roomId)) {
            if (u.getId() != userId) return u.getUsername();
        }
        return "dm";
    }

    private void handleKeyUpload(Packet p) throws Exception {
        if (userId < 0) return;
        userDAO.savePublicKey(userId, p.getPublicKey());
        Packet resp = Packet.of(PacketType.KEY_UPLOAD);
        resp.setSuccess(true);
        send(resp.toJson());
    }

    private void handlePublicKeyRequest(Packet p) throws Exception {
        String key = userDAO.getPublicKey(p.getUserId());
        Packet resp = Packet.of(PacketType.PUBLIC_KEY_RESPONSE);
        resp.setUserId(p.getUserId());
        resp.setPublicKey(key);
        send(resp.toJson());
    }

    private void broadcastUserJoined() {
        Packet p = Packet.of(PacketType.USER_JOINED);
        p.setUserId(userId);
        p.setSenderName(username);
        String json = p.toJson();
        for (missive.common.model.Room r : getRoomsForUser()) {
            roomManager.broadcast(r.getId(), p, userId);
        }
    }

    private List<missive.common.model.Room> getRoomsForUser() {
        try {
            return roomDAO.findByUser(userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void send(String json) {
        if (out != null) out.println(json);
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }

    private void disconnect() {
        if (userId >= 0) {
            roomManager.unregisterUser(userId);
            // broadcast left
            Packet p = Packet.of(PacketType.USER_LEFT);
            p.setUserId(userId);
            p.setSenderName(username);
            for (missive.common.model.Room r : getRoomsForUser()) {
                roomManager.broadcast(r.getId(), p, userId);
            }
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}
