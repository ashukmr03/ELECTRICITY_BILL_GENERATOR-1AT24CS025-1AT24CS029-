package backend.model;

/**
 * User represents a system user account for authentication and role-based access control.
 */
public class User {
    private int userId;
    private String username;
    private String password;
    private String role; // "ADMIN", "EMPLOYEE", "VIEWER"

    public User(int userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role.toUpperCase();
    }

    public User(String username, String password, String role) {
        this(0, username, password, role);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role.toUpperCase();
    }
}
