package backend.dao;

import backend.model.*;
import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CustomerDAO handles all CRUD actions for the Customers table.
 * Integrates directly with MySQL, and provides an in-memory fallback in Demo Mode.
 */
public class CustomerDAO {

    // In-memory data store for Demo Mode
    private static final List<Customer> mockCustomers = new ArrayList<>();

    static {
        // Initialize with default sample data as defined in the README
        mockCustomers.add(new DomesticCustomer(1, "Ravi Kumar", "12 MG Road, Mumbai", "MTR1001", "9876543210"));
        mockCustomers.add(new CommercialCustomer(2, "Sharma Textiles", "Plot 4, Andheri MIDC", "MTR1002", "9123456780"));
    }

    /**
     * Adds a new customer.
     */
    public void addCustomer(Customer customer) throws SQLException {
        if (DBConnection.isDemoMode()) {
            // Check for unique meter number
            for (Customer c : mockCustomers) {
                if (c.getMeterNumber().equalsIgnoreCase(customer.getMeterNumber())) {
                    throw new SQLException("Duplicate entry '" + customer.getMeterNumber() + "' for key 'meter_number'");
                }
            }
            int nextId = mockCustomers.stream().mapToInt(Customer::getCustomerId).max().orElse(0) + 1;
            customer.setCustomerId(nextId);
            mockCustomers.add(customer);
            return;
        }

        String sql = "INSERT INTO customers (name, address, meter_number, customer_type, contact_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getAddress());
            stmt.setString(3, customer.getMeterNumber());
            stmt.setString(4, customer.getCustomerType());
            stmt.setString(5, customer.getContactNumber());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    customer.setCustomerId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Retrieves a customer by ID.
     */
    public Customer getCustomerById(int customerId) throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockCustomers.stream()
                    .filter(c -> c.getCustomerId() == customerId)
                    .findFirst()
                    .orElse(null);
        }

        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCustomer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a customer by meter number.
     */
    public Customer getCustomerByMeter(String meterNumber) throws SQLException {
        if (DBConnection.isDemoMode()) {
            return mockCustomers.stream()
                    .filter(c -> c.getMeterNumber().equalsIgnoreCase(meterNumber.trim()))
                    .findFirst()
                    .orElse(null);
        }

        String sql = "SELECT * FROM customers WHERE meter_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, meterNumber.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCustomer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all registered customers.
     */
    public List<Customer> getAllCustomers() throws SQLException {
        if (DBConnection.isDemoMode()) {
            return new ArrayList<>(mockCustomers);
        }

        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToCustomer(rs));
            }
        }
        return list;
    }

    /**
     * Searches customers by keyword (matching name or meter number).
     */
    public List<Customer> searchCustomers(String keyword) throws SQLException {
        if (DBConnection.isDemoMode()) {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getAllCustomers();
            }
            String kw = keyword.toLowerCase().trim();
            return mockCustomers.stream()
                    .filter(c -> c.getName().toLowerCase().contains(kw) || c.getMeterNumber().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR meter_number LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String val = "%" + keyword + "%";
            stmt.setString(1, val);
            stmt.setString(2, val);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToCustomer(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates an existing customer's contact details (address, contact number).
     */
    public void updateCustomer(Customer customer) throws SQLException {
        if (DBConnection.isDemoMode()) {
            for (Customer c : mockCustomers) {
                if (c.getCustomerId() == customer.getCustomerId()) {
                    c.setAddress(customer.getAddress());
                    c.setContactNumber(customer.getContactNumber());
                    return;
                }
            }
            throw new SQLException("Customer not found in mock store with ID: " + customer.getCustomerId());
        }

        String sql = "UPDATE customers SET address = ?, contact_number = ? WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getAddress());
            stmt.setString(2, customer.getContactNumber());
            stmt.setInt(3, customer.getCustomerId());
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Customer not found with ID: " + customer.getCustomerId());
            }
        }
    }

    /**
     * Utility method to construct the polymorphic Customer objects from DB ResultSets.
     */
    private Customer mapRowToCustomer(ResultSet rs) throws SQLException {
        int id = rs.getInt("customer_id");
        String name = rs.getString("name");
        String address = rs.getString("address");
        String meter = rs.getString("meter_number");
        String type = rs.getString("customer_type");
        String contact = rs.getString("contact_number");

        if ("COMMERCIAL".equalsIgnoreCase(type)) {
            return new CommercialCustomer(id, name, address, meter, contact);
        } else {
            return new DomesticCustomer(id, name, address, meter, contact);
        }
    }
}
