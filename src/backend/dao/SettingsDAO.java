package backend.dao;

import backend.model.CompanyProfile;
import backend.model.SlabRate;
import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SettingsDAO manages configuration settings including Slab Rates and Company Profile details.
 * Integrates directly with MySQL, and provides in-memory fallback in Demo Mode.
 */
public class SettingsDAO {

    // Mock stores for Demo Mode
    private static final List<SlabRate> mockSlabRates = new ArrayList<>();
    private static CompanyProfile mockCompanyProfile;
    
    // Shared logs list
    private static final List<String> systemLogs = new ArrayList<>();

    static {
        systemLogs.add("System standing by for operational routing.");
    }

    public static void addLog(String msg) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        systemLogs.add(0, "[" + timestamp + "] " + msg);
    }

    public static List<String> getLogs() {
        return new ArrayList<>(systemLogs);
    }

    static {
        // Initialize default slab rates
        mockSlabRates.add(new SlabRate(1, "DOMESTIC", 1, 100.0, 14.0));
        mockSlabRates.add(new SlabRate(2, "DOMESTIC", 2, 200.0, 16.0));
        mockSlabRates.add(new SlabRate(3, "DOMESTIC", 3, 999999.0, 18.0));

        mockSlabRates.add(new SlabRate(4, "COMMERCIAL", 1, 100.0, 17.0));
        mockSlabRates.add(new SlabRate(5, "COMMERCIAL", 2, 300.0, 19.0));
        mockSlabRates.add(new SlabRate(6, "COMMERCIAL", 3, 999999.0, 111.0));

        // Initialize default company profile
        mockCompanyProfile = new CompanyProfile(1, "ElectriFlow Corp", "Power Plaza, Sector 62, Noida", "+91 99999 88888", 5.0);
    }

    /**
     * Gets all slab rates for a given customer type.
     */
    public List<SlabRate> getSlabRates(String customerType) throws SQLException {
        if (DBConnection.isDemoMode()) {
            List<SlabRate> rates = new ArrayList<>();
            for (SlabRate rate : mockSlabRates) {
                if (rate.getCustomerType().equalsIgnoreCase(customerType)) {
                    rates.add(rate);
                }
            }
            return rates;
        }

        List<SlabRate> rates = new ArrayList<>();
        String sql = "SELECT * FROM slab_rates WHERE customer_type = ? ORDER BY slab_num ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerType.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rates.add(new SlabRate(
                            rs.getInt("rate_id"),
                            rs.getString("customer_type"),
                            rs.getInt("slab_num"),
                            rs.getDouble("limit_val"),
                            rs.getDouble("rate_val")
                    ));
                }
            }
        }
        return rates;
    }

    /**
     * Updates limits and rates for a particular slab configuration.
     */
    public void updateSlabRate(SlabRate rate) throws SQLException {
        if (DBConnection.isDemoMode()) {
            for (SlabRate r : mockSlabRates) {
                if (r.getCustomerType().equalsIgnoreCase(rate.getCustomerType()) && r.getSlabNum() == rate.getSlabNum()) {
                    r.setLimitVal(rate.getLimitVal());
                    r.setRateVal(rate.getRateVal());
                    return;
                }
            }
            throw new SQLException("Slab rate config not found in mock store.");
        }

        String sql = "UPDATE slab_rates SET limit_val = ?, rate_val = ? WHERE customer_type = ? AND slab_num = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, rate.getLimitVal());
            stmt.setDouble(2, rate.getRateVal());
            stmt.setString(3, rate.getCustomerType().toUpperCase());
            stmt.setInt(4, rate.getSlabNum());
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                // Insert if not exists
                String insertSql = "INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, rate.getCustomerType().toUpperCase());
                    insertStmt.setInt(2, rate.getSlabNum());
                    insertStmt.setDouble(3, rate.getLimitVal());
                    insertStmt.setDouble(4, rate.getRateVal());
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    /**
     * Gets the company profile information.
     */
    public CompanyProfile getCompanyProfile() throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockCompanyProfile;
        }

        String sql = "SELECT * FROM company_profile LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new CompanyProfile(
                        rs.getInt("profile_id"),
                        rs.getString("company_name"),
                        rs.getString("address"),
                        rs.getString("contact_number"),
                        rs.getDouble("tax_rate")
                );
            }
        }

        // Return mock defaults if the DB is empty
        return mockCompanyProfile;
    }

    /**
     * Updates the company profile information.
     */
    public void updateCompanyProfile(CompanyProfile profile) throws SQLException {
        if (DBConnection.isDemoMode()) {
            mockCompanyProfile = profile;
            return;
        }

        String sql = "UPDATE company_profile SET company_name = ?, address = ?, contact_number = ?, tax_rate = ? WHERE profile_id = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getCompanyName());
            stmt.setString(2, profile.getAddress());
            stmt.setString(3, profile.getContactNumber());
            stmt.setDouble(4, profile.getTaxRate());
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                // Insert if empty
                String insertSql = "INSERT INTO company_profile (profile_id, company_name, address, contact_number, tax_rate) VALUES (1, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, profile.getCompanyName());
                    insertStmt.setString(2, profile.getAddress());
                    insertStmt.setString(3, profile.getContactNumber());
                    insertStmt.setDouble(4, profile.getTaxRate());
                    insertStmt.executeUpdate();
                }
            }
        }
    }
}
