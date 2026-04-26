package missive.server;

import missive.common.protocol.Packet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// manages room membership and broadcasting for active connections
public class RoomManager {

    // roomId -> list of connected handlers
    private final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();
    // userId -> handler
    private final Map<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public void registerUser(int userId, ClientHandler handler) {
        onlineUsers.put(userId, handler);
    }

    public void unregisterUser(int userId) {
        ClientHandler h = onlineUsers.remove(userId);
        if (h == null) return;
        for (List<ClientHandler> members : roomClients.values()) {
            members.remove(h);
        }
    }

    public void joinRoom(int roomId, ClientHandler handler) {
        List<ClientHandler> list = roomClients.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        if (!list.contains(handler)) list.add(handler);
    }

    public ClientHandler getHandler(int userId) {
        return onlineUsers.get(userId);
    }

    public void leaveRoom(int roomId, int userId) {
        List<ClientHandler> members = roomClients.get(roomId);
        if (members != null) {
            members.removeIf(h -> h.getUserId() == userId);
        }
    }

    // broadcast to all room members except sender
    public void broadcast(int roomId, Packet packet, int excludeUserId) {
        List<ClientHandler> members = roomClients.get(roomId);
        if (members == null) return;
        String json = packet.toJson();
        for (ClientHandler h : members) {
            if (h.getUserId() != excludeUserId) {
                h.send(json);
            }
        }
    }

    // send to a specific user
    public void sendTo(int userId, Packet packet) {
        ClientHandler h = onlineUsers.get(userId);
        if (h != null) h.send(packet.toJson());
    }

    public boolean isOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }

    public Set<Integer> getOnlineUserIds() {
        return Collections.unmodifiableSet(onlineUsers.keySet());
    }

    public List<ClientHandler> getRoomMembers(int roomId) {
        return roomClients.getOrDefault(roomId, Collections.emptyList());
    }
}
