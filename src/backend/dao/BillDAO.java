package backend.dao;

import backend.model.Bill;
import database.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BillDAO handles CRUD actions for the Bills table.
 * Integrates directly with MySQL and falls back to a static list in Demo Mode.
 */
public class BillDAO {

    // In-memory data store for Demo Mode
    private static final List<Bill> mockBills = new ArrayList<>();

    static {
        // Initialize with default sample data from the README
        mockBills.add(new Bill(1, 1, 1200.0, 1350.0, 150.0, LocalDate.of(2026, 6, 1), 825.00, "UNPAID"));
        mockBills.add(new Bill(2, 2, 5400.0, 5700.0, 300.0, LocalDate.of(2026, 6, 1), 3300.00, "PAID"));
    }

    /**
     * Saves a newly generated bill to the repository.
     */
    public void generateBill(Bill bill) throws SQLException {
        if (DBConnection.isDemoMode()) {
            int nextId = mockBills.stream().mapToInt(Bill::getBillId).max().orElse(0) + 1;
            bill.setBillId(nextId);
            mockBills.add(bill);
            return;
        }

        String sql = "INSERT INTO bills (customer_id, previous_reading, current_reading, units_consumed, bill_date, amount, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, bill.getCustomerId());
            stmt.setDouble(2, bill.getPreviousReading());
            stmt.setDouble(3, bill.getCurrentReading());
            stmt.setDouble(4, bill.getUnitsConsumed());
            stmt.setDate(5, Date.valueOf(bill.getBillDate()));
            stmt.setDouble(6, bill.getAmount());
            stmt.setString(7, bill.getStatus());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    bill.setBillId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Fetches the latest meter reading for a customer to serve as their next previous reading.
     */
    public double getLastReading(int customerId) throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockBills.stream()
                    .filter(b -> b.getCustomerId() == customerId)
                    .max((b1, b2) -> Integer.compare(b1.getBillId(), b2.getBillId()))
                    .map(Bill::getCurrentReading)
                    .orElse(0.0);
        }

        String sql = "SELECT current_reading FROM bills WHERE customer_id = ? ORDER BY bill_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("current_reading");
                }
            }
        }
        return 0.0;
    }

    /**
     * Retrieves all bills for a specific customer.
     */
    public List<Bill> getBillsByCustomer(int customerId) throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockBills.stream()
                    .filter(b -> b.getCustomerId() == customerId)
                    .collect(Collectors.toList());
        }

        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills WHERE customer_id = ? ORDER BY bill_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBill(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all bills from the system.
     */
    public List<Bill> getAllBills() throws SQLException {
        if (DBConnection.isDemoMode()) {
            return new ArrayList<>(mockBills);
        }

        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY bill_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToBill(rs));
            }
        }
        return list;
    }

    /**
     * Updates the status (PAID/UNPAID) of an existing bill.
     */
    public void updateBillStatus(int billId, String status) throws SQLException {
        if (DBConnection.isDemoMode()) {
            for (Bill b : mockBills) {
                if (b.getBillId() == billId) {
                    b.setStatus(status.toUpperCase());
                    return;
                }
            }
            throw new SQLException("Bill not found with ID: " + billId);
        }

        String sql = "UPDATE bills SET status = ? WHERE bill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toUpperCase());
            stmt.setInt(2, billId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Bill not found with ID: " + billId);
            }
        }
    }

    /**
     * Utility method to map a database row to a Bill object.
     */
    private Bill mapRowToBill(ResultSet rs) throws SQLException {
        int id = rs.getInt("bill_id");
        int custId = rs.getInt("customer_id");
        double prev = rs.getDouble("previous_reading");
        double curr = rs.getDouble("current_reading");
        double units = rs.getDouble("units_consumed");
        Date dbDate = rs.getDate("bill_date");
        LocalDate date = dbDate != null ? dbDate.toLocalDate() : LocalDate.now();
        double amount = rs.getDouble("amount");
        String status = rs.getString("status");

        return new Bill(id, custId, prev, curr, units, date, amount, status);
    }
}
