package missive.server.db;

import missive.common.model.Room;
import missive.common.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO implements DAO<Room> {

    private final Database db = Database.getInstance();

    @Override
    public Room findById(int id) throws Exception {
        String sql = "SELECT id, name, description, is_private, is_dm FROM rooms WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT id, name, description, is_private, is_dm FROM rooms WHERE is_private = FALSE AND is_dm = FALSE ORDER BY name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rooms.add(mapRow(rs));
        }
        return rooms;
    }

    // rooms the user is a member of
    public List<Room> findByUser(int userId) throws Exception {
        List<Room> rooms = new ArrayList<>();
        String sql = """
            SELECT r.id, r.name, r.description, r.is_private, r.is_dm
            FROM rooms r
            JOIN room_members rm ON r.id = rm.room_id
            WHERE rm.user_id = ?
            ORDER BY r.name
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rooms.add(mapRow(rs));
        }
        return rooms;
    }

    public List<User> getMembers(int roomId) throws Exception {
        List<User> members = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, u.avatar_color
            FROM users u
            JOIN room_members rm ON u.id = rm.user_id
            WHERE rm.room_id = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("avatar_color") != null ? rs.getString("avatar_color") : "#00cc44"
                ));
            }
        }
        return members;
    }

    // find existing 1-on-1 DM between two users (returns null if none)
    public Room findDmRoom(int userA, int userB) throws Exception {
        String sql = """
            SELECT r.id, r.name, r.description, r.is_private, r.is_dm
            FROM rooms r
            JOIN room_members m1 ON m1.room_id = r.id AND m1.user_id = ?
            JOIN room_members m2 ON m2.room_id = r.id AND m2.user_id = ?
            WHERE r.is_dm = TRUE
            LIMIT 1
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public boolean isMember(int roomId, int userId) throws Exception {
        String sql = "SELECT 1 FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        }
    }

    public void addMember(int roomId, int userId) throws Exception {
        String sql = "INSERT IGNORE INTO room_members (room_id, user_id) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public int save(Room room) throws Exception {
        String sql = "INSERT INTO rooms (name, description, is_private, is_dm, created_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getName());
            ps.setString(2, room.getDescription());
            ps.setBoolean(3, room.isPrivate());
            ps.setBoolean(4, room.isDm());
            ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new Exception("Failed to save room");
    }

    @Override
    public void update(Room room) throws Exception {
        String sql = "UPDATE rooms SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getName());
            ps.setString(2, room.getDescription());
            ps.setInt(3, room.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_private"),
                rs.getBoolean("is_dm")
        );
    }
}
