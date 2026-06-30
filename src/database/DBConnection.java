package database;

import java.sql.*;

/**
 * DBConnection provides JDBC connection creation for the MySQL database.
 * If the MySQL database is offline, it supports a mock fallback mode
 * so the application remains fully functional for demonstration.
 */
public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/electricity_billing";
    private static final String USER = "root";
    private static final String PASSWORD = "#arghaneel29"; // Default empty, user can change as needed

    private static boolean demoMode = false;

    /**
     * Sets whether the database should run in in-memory demo mode.
     */
    public static void setDemoMode(boolean enabled) {
        demoMode = enabled;
    }

    /**
     * Checks if demo mode is active.
     */
    public static boolean isDemoMode() {
        return demoMode;
    }

    /**
     * Gets a connection to the MySQL database.
     * Returns null if in demo mode.
     */
    public static Connection getConnection() throws SQLException {
        if (demoMode) {
            return null;
        }
        try {
            // Load driver explicitly to ensure compatibility
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in classpath.", e);
        }
    }

    /**
     * Tests if a connection to the MySQL database can be established.
     * If successful, it also runs table verification.
     */
    public static boolean testConnection() {
        if (demoMode) {
            return false;
        }
        try (Connection conn = getConnection()) {
            if (conn != null) {
                initializeDatabase(conn);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if tables exist and creates/seeds them automatically.
     */
    private static void initializeDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Create tables
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customer_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "address VARCHAR(255) NOT NULL, " +
                    "meter_number VARCHAR(20) NOT NULL UNIQUE, " +
                    "customer_type VARCHAR(20) NOT NULL, " +
                    "contact_number VARCHAR(15)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS bills (" +
                    "bill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_id INT NOT NULL, " +
                    "previous_reading DOUBLE NOT NULL, " +
                    "current_reading DOUBLE NOT NULL, " +
                    "units_consumed DOUBLE NOT NULL, " +
                    "bill_date DATE NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(customer_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS slab_rates (" +
                    "rate_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_type VARCHAR(20) NOT NULL, " +
                    "slab_num INT NOT NULL, " +
                    "limit_val DOUBLE NOT NULL, " +
                    "rate_val DOUBLE NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS company_profile (" +
                    "profile_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "company_name VARCHAR(100) NOT NULL, " +
                    "address VARCHAR(255) NOT NULL, " +
                    "contact_number VARCHAR(15), " +
                    "tax_rate DOUBLE NOT NULL DEFAULT 5.0" +
                    ")");

            // Seed default users if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (username, password, role) VALUES " +
                            "('admin', 'admin123', 'ADMIN'), " +
                            "('employee', 'emp123', 'EMPLOYEE'), " +
                            "('viewer', 'view123', 'VIEWER')");
                }
            }

            // Seed default slab rates if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM slab_rates")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val) VALUES " +
                            "('DOMESTIC', 1, 100.0, 14.0), " +
                            "('DOMESTIC', 2, 200.0, 16.0), " +
                            "('DOMESTIC', 3, 999999.0, 18.0), " +
                            "('COMMERCIAL', 1, 100.0, 17.0), " +
                            "('COMMERCIAL', 2, 300.0, 19.0), " +
                            "('COMMERCIAL', 3, 999999.0, 111.0)");
                }
            }

            // Seed default company profile if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM company_profile")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO company_profile (profile_id, company_name, address, contact_number, tax_rate) VALUES " +
                            "(1, 'ElectriFlow Corp', 'Power Plaza, Sector 62, Noida', '+91 99999 88888', 5.0)");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database auto-initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
