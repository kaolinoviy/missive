package missive.server.db;

import missive.common.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements DAO<User> {

    private final Database db = Database.getInstance();

    @Override
    public User findById(int id) throws Exception {
        String sql = "SELECT id, username, avatar_color FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public User findByUsername(String username) throws Exception {
        String sql = "SELECT id, username, avatar_color FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // returns user id on success, -1 on invalid credentials
    public int authenticate(String username, String password) throws Exception {
        String sql = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, hash)) {
                    touchLastSeen(rs.getInt("id"));
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    // returns new user id, throws on duplicate
    public int register(String username, String password) throws Exception {
        if (findByUsername(username) != null) throw new Exception("Username already taken");

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new Exception("Registration failed");
    }

    public void savePublicKey(int userId, String publicKey) throws Exception {
        String sql = "UPDATE users SET public_key = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, publicKey);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public String getPublicKey(int userId) throws Exception {
        String sql = "SELECT public_key FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("public_key");
        }
        return null;
    }

    private void touchLastSeen(int userId) throws Exception {
        String sql = "UPDATE users SET last_seen = NOW() WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> findAll() throws Exception {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, avatar_color FROM users ORDER BY username";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public int save(User user) throws Exception {
        throw new UnsupportedOperationException("use register()");
    }

    @Override
    public void update(User user) throws Exception {
        String sql = "UPDATE users SET avatar_color = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getAvatarColor());
            ps.setInt(2, user.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"),
                rs.getString("avatar_color") != null ? rs.getString("avatar_color") : "#00cc44");
    }
}
