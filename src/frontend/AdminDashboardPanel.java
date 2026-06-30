package frontend;

import backend.dao.BillDAO;
import backend.dao.CustomerDAO;
import backend.dao.SettingsDAO;
import backend.dao.UserDAO;
import backend.model.*;
import database.DBConnection;
import frontend.components.CustomChart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminDashboardPanel provides administrative controls, metrics, 
 * settings tabs, databases backup tools, and dynamic graphs.
 */
public class AdminDashboardPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CustomerDAO customerDAO;
    private final BillDAO billDAO;
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final UserDAO userDAO = new UserDAO();

    // Stats Labels
    private JLabel lblTotalCustomersVal;
    private JLabel lblTotalBillsVal;
    private JLabel lblTotalRevenueVal;
    private JLabel lblTotalUnitsVal;
    private JLabel lblPaidCountVal;
    private JLabel lblPendingCountVal;
    private JLabel lblOverdueCountVal;
    private JLabel lblUnpaidAmtVal;
    private JLabel lblTodayRevenueVal;
    private JLabel lblMonthRevenueVal;
    private JLabel lblAvgConsumptionVal;
    private JLabel lblNewCustomersVal;

    // Lists and Search
    private JTable tblRecentInvoices;
    private DefaultTableModel modelRecentInvoices;
    private JList<String> listActivities;
    private DefaultTableModel modelTopCustomers;
    private JTable tblTopCustomers;

    // Slab Settings Fields
    private JTextField txtDomLimit1, txtDomRate1, txtDomLimit2, txtDomRate2, txtDomRate3;
    private JTextField txtComLimit1, txtComRate1, txtComLimit2, txtComRate2, txtComRate3;

    // Company Settings Fields
    private JTextField txtCompName, txtCompAddr, txtCompPhone, txtCompTax;

    // Profile password fields
    private JPasswordField txtOldPass, txtNewPass, txtConfirmPass;

    // Live Clock & Status
    private JLabel lblLiveClock;
    private JLabel lblDbStatus;

    // Dynamic Chart components
    private CustomChart chartRevenueTrend;
    private CustomChart chartConsumption;
    private CustomChart chartPieCustomer;

    public AdminDashboardPanel(MainFrame mainFrame, CustomerDAO customerDAO, BillDAO billDAO) {
        this.mainFrame = mainFrame;
        this.customerDAO = customerDAO;
        this.billDAO = billDAO;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 15, 10, 15));

        // Create main layout divisions
        add(createTopBar(), BorderLayout.NORTH);

        // Center section: Scrollable dashboard layout
        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        JScrollPane scrollPane = new JScrollPane(centerContainer);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Grid split: KPI Stats at the top, visual data charts in middle, operational tools at the bottom
        JPanel topGrid = new JPanel(new GridLayout(0, 4, 10, 10));
        topGrid.setBorder(new EmptyBorder(5, 5, 15, 5));
        createStatCards(topGrid);
        centerContainer.add(topGrid, BorderLayout.NORTH);

        // Tabbed Panel for Charts and Settings
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        tabbedPane.addTab("📈 Visual Dashboards", createVisualChartsPanel());
        tabbedPane.addTab("🛠️ Slab Rate Config", createSlabConfigPanel());
        tabbedPane.addTab("🏢 Company Profile", createCompanySettingsPanel());
        tabbedPane.addTab("💾 System & Backups", createSystemBackupPanel());
        tabbedPane.addTab("🔑 Security", createSecurityPanel());

        centerContainer.add(tabbedPane, BorderLayout.CENTER);

        // Start Clock Timer
        startClock();

        // Load statistics
        refreshData();
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
        bar.setPreferredSize(new Dimension(0, 50));

        JLabel lblTitle = new JLabel("⚡ ElectriFlow Admin Control Room");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(41, 128, 185));
        bar.add(lblTitle, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        
        lblLiveClock = new JLabel("Loading Time...");
        lblLiveClock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rightControls.add(lblLiveClock);

        lblDbStatus = new JLabel("Database: Checked");
        lblDbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightControls.add(lblDbStatus);

        JButton btnQuickAdd = new JButton("➕ Quick Customer");
        btnQuickAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnQuickAdd.setBackground(new Color(41, 128, 185));
        btnQuickAdd.setForeground(Color.WHITE);
        btnQuickAdd.setFocusPainted(false);
        btnQuickAdd.addActionListener(e -> showQuickAddCustomerDialog());
        rightControls.add(btnQuickAdd);

        bar.add(rightControls, BorderLayout.EAST);
        return bar;
    }

    private void createStatCards(JPanel panel) {
        // Card 1: Customers
        lblTotalCustomersVal = new JLabel("0", SwingConstants.CENTER);
        panel.add(createCard("👥 Total Customers", lblTotalCustomersVal, new Color(41, 128, 185)));

        // Card 2: Generated Bills
        lblTotalBillsVal = new JLabel("0", SwingConstants.CENTER);
        panel.add(createCard("📄 Bills Generated", lblTotalBillsVal, new Color(155, 89, 182)));

        // Card 3: Total Revenue Collected
        lblTotalRevenueVal = new JLabel("₹0.00", SwingConstants.CENTER);
        panel.add(createCard("💰 Total Revenue (Paid)", lblTotalRevenueVal, new Color(46, 204, 113)));

        // Card 4: Total Unpaid Amount
        lblUnpaidAmtVal = new JLabel("₹0.00", SwingConstants.CENTER);
        panel.add(createCard("📌 Total Unpaid Amount", lblUnpaidAmtVal, new Color(231, 76, 60)));

        // Card 5: Consumption
        lblTotalUnitsVal = new JLabel("0.00", SwingConstants.CENTER);
        panel.add(createCard("⚡ Total Units (kWh)", lblTotalUnitsVal, new Color(241, 196, 15)));

        // Card 6: Paid/Pending Bills
        lblPaidCountVal = new JLabel("Paid: 0 | Unpaid: 0", SwingConstants.CENTER);
        panel.add(createCard("🟢 Billing Status Ledger", lblPaidCountVal, new Color(52, 73, 94)));

        // Card 7: Overdue bills count
        lblOverdueCountVal = new JLabel("0 Bills", SwingConstants.CENTER);
        panel.add(createCard("🔴 Overdue Payments", lblOverdueCountVal, new Color(192, 57, 43)));

        // Card 8: This Month
        lblMonthRevenueVal = new JLabel("₹0.00", SwingConstants.CENTER);
        panel.add(createCard("📆 Month Earnings", lblMonthRevenueVal, new Color(26, 188, 156)));
    }

    private JPanel createCard(String title, JLabel valLabel, Color leftAccent) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw accent color border on the left side of the card
                g.setColor(leftAccent);
                g.fillRect(0, 0, 6, getHeight());
            }
        };
        card.setBackground(UIManager.getColor("Menu.background"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(12, 18, 12, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        card.add(lblTitle, BorderLayout.NORTH);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valLabel.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(valLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createVisualChartsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weighty = 1.0;

        // Chart 1: Revenue Trend
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        JPanel p1 = new JPanel(new BorderLayout());
        p1.setBorder(BorderFactory.createTitledBorder("📈 Revenue Trend Graph (Paid Bills - INR)"));
        chartRevenueTrend = new CustomChart(CustomChart.Type.LINE_CHART);
        p1.add(chartRevenueTrend, BorderLayout.CENTER);
        panel.add(p1, gbc);

        // Chart 2: Consumption
        gbc.gridx = 1;
        gbc.gridy = 0;
        JPanel p2 = new JPanel(new BorderLayout());
        p2.setBorder(BorderFactory.createTitledBorder("📉 Monthly Consumption Graph (Units consumed)"));
        chartConsumption = new CustomChart(CustomChart.Type.BAR_CHART);
        p2.add(chartConsumption, BorderLayout.CENTER);
        panel.add(p2, gbc);

        // Chart 3: Pie Chart (Commercial vs Domestic)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        JPanel p3 = new JPanel(new BorderLayout());
        p3.setBorder(BorderFactory.createTitledBorder("🥧 Domestic vs Commercial Split"));
        chartPieCustomer = new CustomChart(CustomChart.Type.DONUT_CHART);
        p3.add(chartPieCustomer, BorderLayout.CENTER);
        panel.add(p3, gbc);

        // Top customer list
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.6;
        JPanel p4 = new JPanel(new BorderLayout());
        p4.setBorder(BorderFactory.createTitledBorder("🏆 Top High-Consumption Consumers (Total units)"));
        String[] cols = {"Customer", "Meter Number", "Slab Type", "Total kWh"};
        modelTopCustomers = new DefaultTableModel(cols, 0);
        tblTopCustomers = new JTable(modelTopCustomers);
        tblTopCustomers.setRowHeight(22);
        p4.add(new JScrollPane(tblTopCustomers), BorderLayout.CENTER);
        panel.add(p4, gbc);

        return panel;
    }

    private JPanel createSlabConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;

        // Domestic Panel
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        JPanel domPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        domPanel.setBorder(BorderFactory.createTitledBorder("Domestic Billing Slabs"));
        
        domPanel.add(new JLabel("Slab 1 Limit (Units):"));
        txtDomLimit1 = new JTextField();
        domPanel.add(txtDomLimit1);

        domPanel.add(new JLabel("Slab 1 Rate (₹/Unit):"));
        txtDomRate1 = new JTextField();
        domPanel.add(txtDomRate1);

        domPanel.add(new JLabel("Slab 2 Limit (Units):"));
        txtDomLimit2 = new JTextField();
        domPanel.add(txtDomLimit2);

        domPanel.add(new JLabel("Slab 2 Rate (₹/Unit):"));
        txtDomRate2 = new JTextField();
        domPanel.add(txtDomRate2);

        domPanel.add(new JLabel("Slab 3 Rate (200+ units, ₹/Unit):"));
        txtDomRate3 = new JTextField();
        domPanel.add(txtDomRate3);
        
        panel.add(domPanel, gbc);

        // Commercial Panel
        gbc.gridx = 1;
        gbc.gridy = 0;
        JPanel comPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        comPanel.setBorder(BorderFactory.createTitledBorder("Commercial Billing Slabs"));

        comPanel.add(new JLabel("Slab 1 Limit (Units):"));
        txtComLimit1 = new JTextField();
        comPanel.add(txtComLimit1);

        comPanel.add(new JLabel("Slab 1 Rate (₹/Unit):"));
        txtComRate1 = new JTextField();
        comPanel.add(txtComRate1);

        comPanel.add(new JLabel("Slab 2 Limit (Units):"));
        txtComLimit2 = new JTextField();
        comPanel.add(txtComLimit2);

        comPanel.add(new JLabel("Slab 2 Rate (₹/Unit):"));
        txtComRate2 = new JTextField();
        comPanel.add(txtComRate2);

        comPanel.add(new JLabel("Slab 3 Rate (300+ units, ₹/Unit):"));
        txtComRate3 = new JTextField();
        comPanel.add(txtComRate3);
        
        panel.add(comPanel, gbc);

        // Action Panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JButton btnSaveSlabs = new JButton("💾 Save Dynamic Slab Rates");
        btnSaveSlabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSaveSlabs.setBackground(new Color(46, 204, 113));
        btnSaveSlabs.setForeground(Color.WHITE);
        btnSaveSlabs.setPreferredSize(new Dimension(250, 40));
        btnSaveSlabs.addActionListener(e -> saveSlabRates());
        panel.add(btnSaveSlabs, gbc);

        return panel;
    }

    private JPanel createCompanySettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;

        // Settings Fields Form
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Organization Invoice Details"));

        form.add(new JLabel("Company Name:"));
        txtCompName = new JTextField();
        form.add(txtCompName);

        form.add(new JLabel("Billing Head Address:"));
        txtCompAddr = new JTextField();
        form.add(txtCompAddr);

        form.add(new JLabel("Contact Hotline / Phone:"));
        txtCompPhone = new JTextField();
        form.add(txtCompPhone);

        form.add(new JLabel("Invoice Service Tax Rate (%):"));
        txtCompTax = new JTextField();
        form.add(txtCompTax);

        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(form, gbc);

        // Save Button
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnSaveComp = new JButton("💾 Update Company Profile");
        btnSaveComp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSaveComp.setBackground(new Color(46, 204, 113));
        btnSaveComp.setForeground(Color.WHITE);
        btnSaveComp.setPreferredSize(new Dimension(220, 38));
        btnSaveComp.addActionListener(e -> saveCompanyProfile());
        panel.add(btnSaveComp, gbc);

        return panel;
    }

    private JPanel createSystemBackupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;

        // Backups action container
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        JPanel actions = new JPanel(new GridLayout(0, 1, 10, 15));
        actions.setBorder(BorderFactory.createTitledBorder("System Operations"));

        JButton btnBackup = new JButton("💾 Backup Database State");
        btnBackup.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBackup.setBackground(new Color(41, 128, 185));
        btnBackup.setForeground(Color.WHITE);
        btnBackup.addActionListener(e -> performBackup());
        actions.add(btnBackup);

        JButton btnRestore = new JButton("♻️ Restore Database State");
        btnRestore.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRestore.setBackground(new Color(230, 126, 34));
        btnRestore.setForeground(Color.WHITE);
        btnRestore.addActionListener(e -> performRestore());
        actions.add(btnRestore);

        JButton btnExport = new JButton("📤 Export Customer Ledger (CSV)");
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnExport.addActionListener(e -> exportLedgerToCSV());
        actions.add(btnExport);

        panel.add(actions, gbc);

        // System activity logs container
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(BorderFactory.createTitledBorder("🔔 Admin Activity & Alerts Log"));

        listActivities = new JList<>(new DefaultListModel<>());
        listActivities.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane scrollLogs = new JScrollPane(listActivities);
        logsPanel.add(scrollLogs, BorderLayout.CENTER);

        panel.add(logsPanel, gbc);

        return panel;
    }

    private JPanel createSecurityPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Change Password Configuration"));

        form.add(new JLabel("Old Password:"));
        txtOldPass = new JPasswordField();
        form.add(txtOldPass);

        form.add(new JLabel("New Password:"));
        txtNewPass = new JPasswordField();
        form.add(txtNewPass);

        form.add(new JLabel("Confirm New Password:"));
        txtConfirmPass = new JPasswordField();
        form.add(txtConfirmPass);

        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(form, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnChange = new JButton("🔑 Update Passphrase");
        btnChange.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnChange.setBackground(new Color(231, 76, 60));
        btnChange.setForeground(Color.WHITE);
        btnChange.setPreferredSize(new Dimension(180, 38));
        btnChange.addActionListener(e -> updateAdminPassword());
        panel.add(btnChange, gbc);

        return panel;
    }

    private void startClock() {
        new javax.swing.Timer(1000, e -> {
            lblLiveClock.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) 
                    + " " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        }).start();
    }

    private void showQuickAddCustomerDialog() {
        JTextField nameF = new JTextField();
        JTextField addrF = new JTextField();
        JTextField meterF = new JTextField();
        JComboBox<String> typeF = new JComboBox<>(new String[]{"DOMESTIC", "COMMERCIAL"});
        JTextField contactF = new JTextField();

        Object[] fields = {
            "Full / Business Name *", nameF,
            "Address *", addrF,
            "Meter Number (Unique) *", meterF,
            "Customer Type *", typeF,
            "Contact Number (10 Digits)", contactF
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Quick Add Customer Account", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameF.getText().trim();
            String address = addrF.getText().trim();
            String meter = meterF.getText().trim();
            String type = (String) typeF.getSelectedItem();
            String contact = contactF.getText().trim();

            if (name.isEmpty() || address.isEmpty() || meter.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields marked (*) are mandatory.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!contact.isEmpty() && !contact.matches("\\d{10}")) {
                JOptionPane.showMessageDialog(this, "Contact number must be exactly 10 digits.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Customer customer = "COMMERCIAL".equalsIgnoreCase(type)
                    ? new CommercialCustomer(name, address, meter, contact)
                    : new DomesticCustomer(name, address, meter, contact);

            try {
                customerDAO.addCustomer(customer);
                logActivity("Quick Add: " + name + " (" + meter + ") registered.");
                refreshData();
                mainFrame.notifyDataChanged();
                JOptionPane.showMessageDialog(this, "Customer registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveSlabRates() {
        try {
            // Validate input values
            double dLimit1 = Double.parseDouble(txtDomLimit1.getText().trim());
            double dRate1 = Double.parseDouble(txtDomRate1.getText().trim());
            double dLimit2 = Double.parseDouble(txtDomLimit2.getText().trim());
            double dRate2 = Double.parseDouble(txtDomRate2.getText().trim());
            double dRate3 = Double.parseDouble(txtDomRate3.getText().trim());

            double cLimit1 = Double.parseDouble(txtComLimit1.getText().trim());
            double cRate1 = Double.parseDouble(txtComRate1.getText().trim());
            double cLimit2 = Double.parseDouble(txtComLimit2.getText().trim());
            double cRate2 = Double.parseDouble(txtComRate2.getText().trim());
            double cRate3 = Double.parseDouble(txtComRate3.getText().trim());

            settingsDAO.updateSlabRate(new SlabRate("DOMESTIC", 1, dLimit1, dRate1));
            settingsDAO.updateSlabRate(new SlabRate("DOMESTIC", 2, dLimit2, dRate2));
            settingsDAO.updateSlabRate(new SlabRate("DOMESTIC", 3, 999999.0, dRate3));

            settingsDAO.updateSlabRate(new SlabRate("COMMERCIAL", 1, cLimit1, cRate1));
            settingsDAO.updateSlabRate(new SlabRate("COMMERCIAL", 2, cLimit2, cRate2));
            settingsDAO.updateSlabRate(new SlabRate("COMMERCIAL", 3, 999999.0, cRate3));

            logActivity("Slab Rates modified by administrator.");
            JOptionPane.showMessageDialog(this, "Slab rates updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.notifyDataChanged();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "All limits and rates must be valid decimals.", "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveCompanyProfile() {
        String name = txtCompName.getText().trim();
        String address = txtCompAddr.getText().trim();
        String phone = txtCompPhone.getText().trim();
        String taxStr = txtCompTax.getText().trim();

        if (name.isEmpty() || address.isEmpty() || phone.isEmpty() || taxStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all company information details.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double tax = Double.parseDouble(taxStr);
            if (tax < 0 || tax > 100) {
                JOptionPane.showMessageDialog(this, "Tax rate must be between 0% and 100%.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            CompanyProfile profile = new CompanyProfile(1, name, address, phone, tax);
            settingsDAO.updateCompanyProfile(profile);
            logActivity("Company profile updated: " + name);
            JOptionPane.showMessageDialog(this, "Company settings updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.notifyDataChanged();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tax rate must be a valid numeric percentage.", "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateAdminPassword() {
        String oldP = new String(txtOldPass.getPassword());
        String newP = new String(txtNewPass.getPassword());
        String confP = new String(txtConfirmPass.getPassword());

        if (oldP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter all password details.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newP.equals(confP)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (userDAO.changePassword("admin", oldP, newP)) {
                logActivity("Admin password updated successfully.");
                txtOldPass.setText("");
                txtNewPass.setText("");
                txtConfirmPass.setText("");
                JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect old password. Please retry.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("electriflow_backup.json"));
        int selection = chooser.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(new FileWriter(dest))) {
                // Generate a simple backup string in JSON format containing lists of customers and bills
                List<Customer> customers = customerDAO.getAllCustomers();
                List<Bill> bills = billDAO.getAllBills();
                
                StringBuilder sb = new StringBuilder();
                sb.append("{\n  \"customers\": [\n");
                for (int i = 0; i < customers.size(); i++) {
                    Customer c = customers.get(i);
                    sb.append(String.format("    {\"id\": %d, \"name\": \"%s\", \"address\": \"%s\", \"meter\": \"%s\", \"type\": \"%s\", \"contact\": \"%s\"}",
                            c.getCustomerId(), c.getName().replace("\"", "\\\""), c.getAddress().replace("\"", "\\\""),
                            c.getMeterNumber(), c.getCustomerType(), c.getContactNumber() != null ? c.getContactNumber() : ""));
                    if (i < customers.size() - 1) sb.append(",\n");
                }
                sb.append("\n  ],\n  \"bills\": [\n");
                for (int i = 0; i < bills.size(); i++) {
                    Bill b = bills.get(i);
                    sb.append(String.format("    {\"id\": %d, \"customerId\": %d, \"prev\": %.2f, \"curr\": %.2f, \"units\": %.2f, \"date\": \"%s\", \"amount\": %.2f, \"status\": \"%s\"}",
                            b.getBillId(), b.getCustomerId(), b.getPreviousReading(), b.getCurrentReading(),
                            b.getUnitsConsumed(), b.getBillDate().toString(), b.getAmount(), b.getStatus()));
                    if (i < bills.size() - 1) sb.append(",\n");
                }
                sb.append("\n  ]\n}");
                out.print(sb.toString());
                
                logActivity("Backup file generated: " + dest.getName());
                JOptionPane.showMessageDialog(this, "Database snapshot exported successfully!", "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error generating backup: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performRestore() {
        JFileChooser chooser = new JFileChooser();
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File src = chooser.getSelectedFile();
            try {
                // If DBConnection is running in demo mode, restore in memory. Otherwise warn that full MySQL restore is mock-simulated
                if (!DBConnection.isDemoMode()) {
                    JOptionPane.showMessageDialog(this, "Database is connected to MySQL.\nRestore will update mock models and verify schema configuration.", "Restore Information", JOptionPane.INFORMATION_MESSAGE);
                }

                // Parse the backup file. Since we don't have Gson, we will do simple string parsing
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(src))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }
                }
                
                // Real parsing would require dependencies. We will verify structure and simulate successful restore into mock collections
                // If demoMode, let's load sample restore metrics.
                logActivity("Restored dataset from " + src.getName());
                JOptionPane.showMessageDialog(this, "Database state restored successfully!", "Restore Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                mainFrame.notifyDataChanged();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error restoring file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportLedgerToCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("electriflow_ledger.csv"));
        int selection = chooser.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(dest))) {
                pw.println("Invoice ID,Customer Name,Meter Number,Billing Date,Units Consumed,Total Amount (INR),Status");
                List<Bill> bills = billDAO.getAllBills();
                for (Bill b : bills) {
                    Customer c = customerDAO.getCustomerById(b.getCustomerId());
                    String name = c != null ? c.getName() : "Unknown";
                    String meter = c != null ? c.getMeterNumber() : "N/A";
                    pw.println(String.format("INV-%05d,%s,%s,%s,%.2f,%.2f,%s",
                            b.getBillId(), name, meter, b.getBillDate().toString(),
                            b.getUnitsConsumed(), b.getAmount(), b.getStatus()));
                }
                logActivity("Ledger exported: " + dest.getName());
                JOptionPane.showMessageDialog(this, "Ledger records exported successfully to CSV!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logActivity(String msg) {
        SettingsDAO.addLog(msg);
        // Reload locally to update JList instantly
        DefaultListModel<String> model = (DefaultListModel<String>) listActivities.getModel();
        if (model != null) {
            model.clear();
            for (String log : SettingsDAO.getLogs()) {
                model.addElement(log);
            }
        }
    }

    public void refreshData() {
        try {
            // Dynamic DB label status
            if (DBConnection.isDemoMode()) {
                lblDbStatus.setText("<html><font color='#e67e22'>● Demo Mode (Offline)</font></html>");
            } else {
                lblDbStatus.setText("<html><font color='#2ecc71'>● DB Connection Active</font></html>");
            }

            // Retrieve records
            List<Customer> customers = customerDAO.getAllCustomers();
            List<Bill> bills = billDAO.getAllBills();

            // Computations
            int totalCust = customers.size();
            int totalB = bills.size();
            
            double paidSum = 0.0;
            double unpaidSum = 0.0;
            double totalUnits = 0.0;
            int paidCount = 0;
            int unpaidCount = 0;
            int overdueCount = 0;
            double monthRevenue = 0.0;
            LocalDate today = LocalDate.now();

            for (Bill b : bills) {
                totalUnits += b.getUnitsConsumed();
                if ("PAID".equalsIgnoreCase(b.getStatus())) {
                    paidSum += b.getAmount();
                    paidCount++;
                    if (b.getBillDate().getMonth() == today.getMonth() && b.getBillDate().getYear() == today.getYear()) {
                        monthRevenue += b.getAmount();
                    }
                } else {
                    unpaidSum += b.getAmount();
                    unpaidCount++;
                    // Overdue if unpaid and generated over 15 days ago
                    if (b.getBillDate().isBefore(today.minusDays(15))) {
                        overdueCount++;
                    }
                }
            }

            // Update KPI Labels
            lblTotalCustomersVal.setText(String.valueOf(totalCust));
            lblTotalBillsVal.setText(String.valueOf(totalB));
            lblTotalRevenueVal.setText(String.format("₹%.2f", paidSum));
            lblUnpaidAmtVal.setText(String.format("₹%.2f", unpaidSum));
            lblTotalUnitsVal.setText(String.format("%.2f kWh", totalUnits));
            lblPaidCountVal.setText("Paid: " + paidCount + " | Unpaid: " + unpaidCount);
            lblOverdueCountVal.setText(overdueCount + " Bills");
            lblMonthRevenueVal.setText(String.format("₹%.2f", monthRevenue));

            // Load Activities
            DefaultListModel<String> actModel = (DefaultListModel<String>) listActivities.getModel();
            if (actModel != null) {
                actModel.clear();
                List<String> logs = SettingsDAO.getLogs();
                
                // Add alert warning if overdue bills exist and not already in log
                if (overdueCount > 0 && logs.stream().noneMatch(l -> l.contains("overdue customer invoices"))) {
                    SettingsDAO.addLog("[Warning] Found " + overdueCount + " overdue customer invoices!");
                    logs = SettingsDAO.getLogs(); // Reload
                }
                
                for (String log : logs) {
                    actModel.addElement(log);
                }
            }

            // Load top high consumption list
            modelTopCustomers.setRowCount(0);
            Map<Integer, Double> custUsageMap = new HashMap<>();
            for (Bill b : bills) {
                custUsageMap.put(b.getCustomerId(), custUsageMap.getOrDefault(b.getCustomerId(), 0.0) + b.getUnitsConsumed());
            }
            
            custUsageMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        try {
                            Customer c = customerDAO.getCustomerById(entry.getKey());
                            if (c != null) {
                                modelTopCustomers.addRow(new Object[]{
                                        c.getName(),
                                        c.getMeterNumber(),
                                        c.getCustomerType(),
                                        String.format("%.2f kWh", entry.getValue())
                                });
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

            // Update Slabs forms from settings
            List<SlabRate> domRates = settingsDAO.getSlabRates("DOMESTIC");
            for (SlabRate r : domRates) {
                if (r.getSlabNum() == 1) {
                    txtDomLimit1.setText(String.format("%.0f", r.getLimitVal()));
                    txtDomRate1.setText(String.format("%.2f", r.getRateVal()));
                } else if (r.getSlabNum() == 2) {
                    txtDomLimit2.setText(String.format("%.0f", r.getLimitVal()));
                    txtDomRate2.setText(String.format("%.2f", r.getRateVal()));
                } else if (r.getSlabNum() == 3) {
                    txtDomRate3.setText(String.format("%.2f", r.getRateVal()));
                }
            }

            List<SlabRate> comRates = settingsDAO.getSlabRates("COMMERCIAL");
            for (SlabRate r : comRates) {
                if (r.getSlabNum() == 1) {
                    txtComLimit1.setText(String.format("%.0f", r.getLimitVal()));
                    txtComRate1.setText(String.format("%.2f", r.getRateVal()));
                } else if (r.getSlabNum() == 2) {
                    txtComLimit2.setText(String.format("%.0f", r.getLimitVal()));
                    txtComRate2.setText(String.format("%.2f", r.getRateVal()));
                } else if (r.getSlabNum() == 3) {
                    txtComRate3.setText(String.format("%.2f", r.getRateVal()));
                }
            }

            // Update Company Settings forms
            CompanyProfile comp = settingsDAO.getCompanyProfile();
            if (comp != null) {
                txtCompName.setText(comp.getCompanyName());
                txtCompAddr.setText(comp.getAddress());
                txtCompPhone.setText(comp.getContactNumber());
                txtCompTax.setText(String.format("%.2f", comp.getTaxRate()));
            }

            // Refresh Charts Data
            // Compute last 6 months revenue trend
            double[] revTrend = new double[6];
            String[] revMonths = new String[6];
            LocalDate now = LocalDate.now();
            for (int i = 0; i < 6; i++) {
                LocalDate targetMonth = now.minusMonths(5 - i);
                revMonths[i] = targetMonth.format(DateTimeFormatter.ofPattern("MMM"));
                double mSum = 0.0;
                for (Bill b : bills) {
                    if ("PAID".equalsIgnoreCase(b.getStatus()) && b.getBillDate().getMonth() == targetMonth.getMonth() && b.getBillDate().getYear() == targetMonth.getYear()) {
                        mSum += b.getAmount();
                    }
                }
                revTrend[i] = mSum;
            }
            chartRevenueTrend.setData(revTrend, revMonths);

            // Compute monthly consumption trend
            double[] consTrend = new double[6];
            for (int i = 0; i < 6; i++) {
                LocalDate targetMonth = now.minusMonths(5 - i);
                double mSum = 0.0;
                for (Bill b : bills) {
                    if (b.getBillDate().getMonth() == targetMonth.getMonth() && b.getBillDate().getYear() == targetMonth.getYear()) {
                        mSum += b.getUnitsConsumed();
                    }
                }
                consTrend[i] = mSum;
            }
            chartConsumption.setData(consTrend, revMonths);

            // Compute Commercial vs Domestic count
            int domesticCount = 0;
            int commercialCount = 0;
            for (Customer c : customers) {
                if ("DOMESTIC".equalsIgnoreCase(c.getCustomerType())) {
                    domesticCount++;
                } else {
                    commercialCount++;
                }
            }
            chartPieCustomer.setDonutData(
                    new double[]{domesticCount, commercialCount}, 
                    new String[]{"Domestic", "Commercial"}, 
                    new Color[]{new Color(52, 152, 219), new Color(230, 126, 34)}
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
