package frontend;

import backend.dao.CustomerDAO;
import backend.model.Customer;
import backend.model.DomesticCustomer;
import backend.model.CommercialCustomer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * CustomerPanel allows registering new customers and viewing existing ones.
 * Limits access permissions based on multi-role authorization levels.
 */
public class CustomerPanel extends JPanel {
    private final CustomerDAO customerDAO;
    private final MainFrame mainFrame;

    // Form fields
    private JTextField txtName;
    private JTextField txtAddress;
    private JTextField txtMeter;
    private JComboBox<String> cbType;
    private JTextField txtContact;
    private JButton btnSave;

    // Table elements
    private JTable customerTable;
    private DefaultTableModel tableModel;

    public CustomerPanel(MainFrame mainFrame, CustomerDAO customerDAO) {
        this.mainFrame = mainFrame;
        this.customerDAO = customerDAO;
        
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create UI components
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createFormPanel(), createTablePanel());
        splitPane.setDividerLocation(340);
        splitPane.setResizeWeight(0.1);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // Load initial data
        refreshData();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblTitle = new JLabel("Customer Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerPanel.add(lblTitle);
        return headerPanel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Register New Customer"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Form Fields Grid
        JPanel grid = new JPanel(new GridLayout(0, 1, 5, 8));
        
        grid.add(new JLabel("Full Name / Business Name *"));
        txtName = new JTextField();
        grid.add(txtName);

        grid.add(new JLabel("Address *"));
        txtAddress = new JTextField();
        grid.add(txtAddress);

        grid.add(new JLabel("Meter Number (Unique) *"));
        txtMeter = new JTextField();
        grid.add(txtMeter);

        grid.add(new JLabel("Customer Type *"));
        cbType = new JComboBox<>(new String[]{"DOMESTIC", "COMMERCIAL"});
        grid.add(cbType);

        grid.add(new JLabel("Contact Number"));
        txtContact = new JTextField();
        grid.add(txtContact);

        panel.add(grid);
        panel.add(Box.createVerticalStrut(15));

        // Save Button
        btnSave = new JButton("Save Customer");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnSave.setBackground(new Color(41, 128, 185)); // Sleek blue
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        
        btnSave.addActionListener(e -> saveCustomer());
        panel.add(btnSave);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Registered Customers"));

        // Define columns
        String[] columns = {"ID", "Name", "Meter Number", "Type", "Contact", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(25);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(customerTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void saveCustomer() {
        String name = txtName.getText().trim();
        String address = txtAddress.getText().trim();
        String meter = txtMeter.getText().trim();
        String type = (String) cbType.getSelectedItem();
        String contact = txtContact.getText().trim();

        // Validation
        if (name.isEmpty() || address.isEmpty() || meter.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (*).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!contact.isEmpty() && !contact.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Contact number must be exactly 10 digits.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Customer customer;
        if ("COMMERCIAL".equalsIgnoreCase(type)) {
            customer = new CommercialCustomer(name, address, meter, contact);
        } else {
            customer = new DomesticCustomer(name, address, meter, contact);
        }

        try {
            customerDAO.addCustomer(customer);
            JOptionPane.showMessageDialog(this, "Customer registered successfully!\nGenerated ID: " + customer.getCustomerId(), "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            refreshData();
            mainFrame.notifyDataChanged(); // Notify MainFrame to update ReadingPanel's dropdown
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error saving customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        txtName.setText("");
        txtAddress.setText("");
        txtMeter.setText("");
        cbType.setSelectedIndex(0);
        txtContact.setText("");
    }

    /**
     * Refreshes the customer table list.
     */
    public void refreshData() {
        // Enforce VIEWER permissions
        if (mainFrame != null) {
            String role = mainFrame.getUserRole();
            boolean canEdit = !"VIEWER".equals(role);
            txtName.setEnabled(canEdit);
            txtAddress.setEnabled(canEdit);
            txtMeter.setEnabled(canEdit);
            cbType.setEnabled(canEdit);
            txtContact.setEnabled(canEdit);
            if (btnSave != null) {
                btnSave.setEnabled(canEdit);
            }
        }

        try {
            tableModel.setRowCount(0);
            List<Customer> list = customerDAO.getAllCustomers();
            for (Customer c : list) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getName(),
                        c.getMeterNumber(),
                        c.getCustomerType(),
                        c.getContactNumber() != null ? c.getContactNumber() : "-",
                        c.getAddress()
                });
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching customers: " + ex.getMessage());
        }
    }
}
