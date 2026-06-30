package backend.dao;

import backend.model.User;
import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO handles database authentication and password actions.
 * Integrates directly with MySQL, and provides in-memory fallback in Demo Mode.
 */
public class UserDAO {
    private static final List<User> mockUsers = new ArrayList<>();

    static {
        // Initialize default user accounts for Demo Mode
        mockUsers.add(new User(1, "admin", "admin123", "ADMIN"));
        mockUsers.add(new User(2, "employee", "emp123", "EMPLOYEE"));
        mockUsers.add(new User(3, "viewer", "view123", "VIEWER"));
    }

    /**
     * Validates credentials and returns the User object if successful, null otherwise.
     */
    public User authenticate(String username, String password) throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockUsers.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username) && u.getPassword().equals(password))
                    .findFirst()
                    .orElse(null);
        }

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Changes a user's password.
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) throws SQLException {
        if (DBConnection.isDemoMode()) {
            User user = mockUsers.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username) && u.getPassword().equals(oldPassword))
                    .findFirst()
                    .orElse(null);
            if (user != null) {
                user.setPassword(newPassword);
                return true;
            }
            return false;
        }

        // Verify old password first
        String verifySql = "SELECT user_id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(verifySql)) {
            stmt.setString(1, username);
            stmt.setString(2, oldPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Old password doesn't match
                }
            }
        }

        // Update to new password
        String updateSql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Adds a new system user.
     */
    public void addUser(User user) throws SQLException {
        if (DBConnection.isDemoMode()) {
            for (User u : mockUsers) {
                if (u.getUsername().equalsIgnoreCase(user.getUsername())) {
                    throw new SQLException("Username already exists in demo storage.");
                }
            }
            int nextId = mockUsers.stream().mapToInt(User::getUserId).max().orElse(0) + 1;
            user.setUserId(nextId);
            mockUsers.add(user);
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Retrieves all users.
     */
    public List<User> getAllUsers() throws SQLException {
        if (DBConnection.isDemoMode()) {
            return new ArrayList<>(mockUsers);
        }

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                ));
            }
        }
        return users;
    }
}
