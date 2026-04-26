package missive.server.db;

import missive.common.protocol.Packet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO implements DAO<Packet.MessageRecord> {

    private final Database db = Database.getInstance();

    @Override
    public Packet.MessageRecord findById(int id) throws Exception {
        String sql = """
            SELECT m.id, m.room_id, m.sender_id, m.sender_name, m.content,
                   m.encrypted, m.iv, m.created_at, u.avatar_color
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.id = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Packet.MessageRecord> findByRoom(int roomId, int limit, int offset) throws Exception {
        List<Packet.MessageRecord> list = new ArrayList<>();
        String sql = """
            SELECT m.id, m.room_id, m.sender_id, m.sender_name, m.content,
                   m.encrypted, m.iv, m.created_at, u.avatar_color
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.room_id = ?
            ORDER BY m.created_at DESC
            LIMIT ? OFFSET ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(0, mapRow(rs)); // reverse so oldest first
        }
        return list;
    }

    // search messages in a room by content (only non-encrypted ones — encrypted lookup needs client decrypt)
    public List<Packet.MessageRecord> search(int roomId, String query, int limit) throws Exception {
        List<Packet.MessageRecord> list = new ArrayList<>();
        String sql = """
            SELECT m.id, m.room_id, m.sender_id, m.sender_name, m.content,
                   m.encrypted, m.iv, m.created_at, u.avatar_color
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.room_id = ? AND m.encrypted = FALSE AND m.content LIKE ?
            ORDER BY m.created_at DESC
            LIMIT ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, "%" + query + "%");
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // returns generated message id
    public int saveMessage(int roomId, int senderId, String senderName,
                           String content, boolean encrypted, String iv) throws Exception {
        String sql = "INSERT INTO messages (room_id, sender_id, sender_name, content, encrypted, iv) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, roomId);
            ps.setInt(2, senderId);
            ps.setString(3, senderName);
            ps.setString(4, content);
            ps.setBoolean(5, encrypted);
            ps.setString(6, iv);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new Exception("Failed to save message");
    }

    @Override
    public List<Packet.MessageRecord> findAll() throws Exception {
        throw new UnsupportedOperationException("use findByRoom()");
    }

    @Override
    public int save(Packet.MessageRecord record) throws Exception {
        return saveMessage(record.roomId, record.senderId, record.senderName,
                record.content, record.encrypted, record.iv);
    }

    @Override
    public void update(Packet.MessageRecord record) throws Exception {
        throw new UnsupportedOperationException("messages are immutable");
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Packet.MessageRecord mapRow(ResultSet rs) throws SQLException {
        Packet.MessageRecord r = new Packet.MessageRecord();
        r.id = rs.getInt("id");
        r.roomId = rs.getInt("room_id");
        r.senderId = rs.getInt("sender_id");
        r.senderName = rs.getString("sender_name");
        r.avatarColor = rs.getString("avatar_color") != null ? rs.getString("avatar_color") : "#00cc44";
        r.content = rs.getString("content");
        r.encrypted = rs.getBoolean("encrypted");
        r.iv = rs.getString("iv");
        r.type = "chat";
        r.timestamp = rs.getTimestamp("created_at").getTime();
        return r;
    }
}
