package frontend;

import backend.dao.CustomerDAO;
import backend.dao.BillDAO;
import backend.model.Customer;
import database.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ReadingPanel allows operators to select a customer, auto-fetch their previous meter reading,
 * input the current reading, and calculate/generate the new bill.
 */
public class ReadingPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CustomerDAO customerDAO;
    private final BillDAO billDAO;

    private JComboBox<CustomerWrapper> cbCustomer;
    
    // Customer Info Displays (Read-only)
    private JLabel lblNameVal;
    private JLabel lblMeterVal;
    private JLabel lblTypeVal;
    private JLabel lblAddressVal;

    // Reading inputs
    private JTextField txtPrevReading;
    private JTextField txtCurrReading;

    // List of active customers to map selection
    private List<Customer> customersList = new ArrayList<>();

    public ReadingPanel(MainFrame mainFrame, CustomerDAO customerDAO, BillDAO billDAO) {
        this.mainFrame = mainFrame;
        this.customerDAO = customerDAO;
        this.billDAO = billDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create UI components
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainContentPanel(), BorderLayout.CENTER);

        // Load initial customers
        refreshData();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblTitle = new JLabel("Enter Meter Readings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerPanel.add(lblTitle);
        return headerPanel;
    }

    private JPanel createMainContentPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(8, 8, 8, 8);

        // --- ROW 0: Customer Dropdown ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        JPanel selectPanel = new JPanel(new BorderLayout(10, 10));
        selectPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Select Customer"));
        
        cbCustomer = new JComboBox<>();
        cbCustomer.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbCustomer.addActionListener(e -> onCustomerSelected());
        selectPanel.add(cbCustomer, BorderLayout.CENTER);
        
        mainPanel.add(selectPanel, gbc);

        // --- ROW 1: Details and Input ---
        // Left Column: Customer Info Card
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;

        JPanel infoCard = new JPanel(new GridBagLayout());
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Customer Profile Details"),
                new EmptyBorder(10, 15, 10, 15)
        ));

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.anchor = GridBagConstraints.WEST;
        cardGbc.insets = new Insets(6, 0, 6, 10);
        cardGbc.gridx = 0;

        JLabel lblName = new JLabel("Name:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardGbc.gridy = 0;
        infoCard.add(lblName, cardGbc);

        lblNameVal = new JLabel("-");
        lblNameVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardGbc.gridx = 1;
        infoCard.add(lblNameVal, cardGbc);

        JLabel lblMeter = new JLabel("Meter No:");
        lblMeter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardGbc.gridx = 0;
        cardGbc.gridy = 1;
        infoCard.add(lblMeter, cardGbc);

        lblMeterVal = new JLabel("-");
        lblMeterVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardGbc.gridx = 1;
        infoCard.add(lblMeterVal, cardGbc);

        JLabel lblType = new JLabel("Slab Type:");
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardGbc.gridx = 0;
        cardGbc.gridy = 2;
        infoCard.add(lblType, cardGbc);

        lblTypeVal = new JLabel("-");
        lblTypeVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardGbc.gridx = 1;
        infoCard.add(lblTypeVal, cardGbc);

        JLabel lblAddress = new JLabel("Address:");
        lblAddress.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardGbc.gridx = 0;
        cardGbc.gridy = 3;
        infoCard.add(lblAddress, cardGbc);

        lblAddressVal = new JLabel("-");
        lblAddressVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardGbc.gridx = 1;
        infoCard.add(lblAddressVal, cardGbc);

        mainPanel.add(infoCard, gbc);

        // Right Column: Reading Inputs
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;

        JPanel inputsPanel = new JPanel(new GridLayout(0, 1, 5, 8));
        inputsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Step 2: Input Meter Readings"),
                new EmptyBorder(10, 15, 10, 15)
        ));

        inputsPanel.add(new JLabel("Previous Meter Reading (Units) *"));
        txtPrevReading = new JTextField();
        txtPrevReading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtPrevReading.setEnabled(false); // Read-only
        txtPrevReading.setDisabledTextColor(UIManager.getColor("Label.foreground"));
        inputsPanel.add(txtPrevReading);

        inputsPanel.add(new JLabel("Current Meter Reading (Units) *"));
        txtCurrReading = new JTextField();
        txtCurrReading.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputsPanel.add(txtCurrReading);

        inputsPanel.add(Box.createVerticalStrut(10));
        
        JButton btnGenerate = new JButton("Calculate & Review Bill");
        btnGenerate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGenerate.setBackground(new Color(39, 174, 96)); // Sleek green
        btnGenerate.setForeground(Color.WHITE);
        btnGenerate.setFocusPainted(false);
        btnGenerate.addActionListener(e -> generateBillReview());
        inputsPanel.add(btnGenerate);

        mainPanel.add(inputsPanel, gbc);

        return mainPanel;
    }

    private void onCustomerSelected() {
        CustomerWrapper selectedWrapper = (CustomerWrapper) cbCustomer.getSelectedItem();
        if (selectedWrapper == null || selectedWrapper.customer == null) {
            clearCustomerDetails();
            return;
        }

        Customer customer = selectedWrapper.customer;
        lblNameVal.setText(customer.getName());
        lblMeterVal.setText(customer.getMeterNumber());
        lblTypeVal.setText(customer.getCustomerType());
        lblAddressVal.setText(customer.getAddress());

        try {
            // Auto-fetch the previous reading from database
            double prevReading = billDAO.getLastReading(customer.getCustomerId());
            txtPrevReading.setText(String.format("%.2f", prevReading));
            txtCurrReading.setText("");
            txtCurrReading.requestFocus();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching last reading: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearCustomerDetails() {
        lblNameVal.setText("-");
        lblMeterVal.setText("-");
        lblTypeVal.setText("-");
        lblAddressVal.setText("-");
        txtPrevReading.setText("");
        txtCurrReading.setText("");
    }

    private void generateBillReview() {
        CustomerWrapper selectedWrapper = (CustomerWrapper) cbCustomer.getSelectedItem();
        if (selectedWrapper == null || selectedWrapper.customer == null) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String currText = txtCurrReading.getText().trim();
        if (currText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the current meter reading.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double prevReading;
        double currReading;

        try {
            prevReading = Double.parseDouble(txtPrevReading.getText().trim());
        } catch (NumberFormatException e) {
            prevReading = 0.0;
        }

        try {
            currReading = Double.parseDouble(currText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Current reading must be a valid numeric decimal number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currReading < 0) {
            JOptionPane.showMessageDialog(this, "Meter readings cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currReading < prevReading) {
            JOptionPane.showMessageDialog(this, 
                    "Current reading (" + currReading + ") cannot be less than the previous reading (" + prevReading + ").", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Proceed to BillPanel via MainFrame coordinator
        mainFrame.showBillPanel(selectedWrapper.customer, prevReading, currReading);
    }

    /**
     * Refreshes the customer selection dropdown menu.
     */
    public void refreshData() {
        try {
            cbCustomer.removeAllItems();
            customersList = customerDAO.getAllCustomers();
            
            if (customersList.isEmpty()) {
                cbCustomer.addItem(new CustomerWrapper(null, "No registered customers. Add one first."));
                clearCustomerDetails();
            } else {
                for (Customer c : customersList) {
                    cbCustomer.addItem(new CustomerWrapper(c, c.getName() + " (" + c.getMeterNumber() + ")"));
                }
                // Trigger action for the first selected item
                onCustomerSelected();
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching customer dropdown data: " + ex.getMessage());
        }
    }

    /**
     * Wrapper class to show custom label in JComboBox while keeping object reference.
     */
    private static class CustomerWrapper {
        final Customer customer;
        final String displayName;

        CustomerWrapper(Customer customer, String displayName) {
            this.customer = customer;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
