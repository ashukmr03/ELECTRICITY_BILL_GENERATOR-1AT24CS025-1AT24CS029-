package frontend;

import backend.dao.BillDAO;
import backend.dao.CustomerDAO;
import backend.dao.SettingsDAO;
import backend.model.*;
import frontend.components.CustomChart;
import frontend.components.InvoicePrintTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerDashboardPanel displays a private customer portal showing 
 * profile details, payment gateway simulation, editable contacts, and visual charts.
 */
public class CustomerDashboardPanel extends JPanel {
    private final MainFrame mainFrame;
    private final BillDAO billDAO;
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    private Customer activeCustomer;
    private List<Bill> billsList = new ArrayList<>();

    // UI elements
    private JLabel lblWelcome;
    private JLabel lblNameVal;
    private JLabel lblMeterVal;
    private JLabel lblPhoneVal;
    private JLabel lblAddressVal;
    private JLabel lblTypeVal;

    private JLabel lblOutstandingVal;
    private JLabel lblTotalPaidVal;
    private JLabel lblTotalConsumedVal;

    private JTable tblBills;
    private DefaultTableModel modelBills;

    // Charts
    private CustomChart chartPriceTrend;
    private CustomChart chartConsumption;
    private CustomChart chartSlabSplit;

    public CustomerDashboardPanel(MainFrame mainFrame, BillDAO billDAO) {
        this.mainFrame = mainFrame;
        this.billDAO = billDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Center split layout: Profile & Stats (Left) and Graphs & Bills (Right)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createProfilePanel(), createMainDashboardPanel());
        mainSplit.setDividerLocation(320);
        mainSplit.setDividerSize(5);
        mainSplit.setBorder(null);
        add(mainSplit, BorderLayout.CENTER);
    }

    public void setupCustomer(Customer customer) {
        this.activeCustomer = customer;
        lblWelcome.setText("👤 Welcome back, " + customer.getName());
        lblNameVal.setText(customer.getName());
        lblMeterVal.setText(customer.getMeterNumber());
        lblPhoneVal.setText(customer.getContactNumber() != null && !customer.getContactNumber().isEmpty() ? customer.getContactNumber() : "-");
        lblAddressVal.setText(customer.getAddress());
        lblTypeVal.setText(customer.getCustomerType());

        refreshData();
    }

    private JPanel createHeaderPanel() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
        bar.setPreferredSize(new Dimension(0, 50));

        lblWelcome = new JLabel("👤 Welcome back!");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblWelcome.setForeground(new Color(41, 128, 185));
        bar.add(lblWelcome, BorderLayout.WEST);

        JButton btnLogout = new JButton("🔓 Log Out");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setBackground(new Color(231, 76, 60));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> mainFrame.logout());
        bar.add(btnLogout, BorderLayout.EAST);

        return bar;
    }

    private JPanel createProfilePanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Customer Account Profile"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.gridx = 0;

        // Form Fields
        gbc.gridy = 0;
        form.add(new JLabel("Full Registered Name:"), gbc);
        lblNameVal = new JLabel("-");
        lblNameVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 1;
        form.add(lblNameVal, gbc);

        gbc.gridy = 2;
        form.add(new JLabel("Unique Meter ID:"), gbc);
        lblMeterVal = new JLabel("-");
        lblMeterVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 3;
        form.add(lblMeterVal, gbc);

        gbc.gridy = 4;
        form.add(new JLabel("Contact Mobile No:"), gbc);
        lblPhoneVal = new JLabel("-");
        lblPhoneVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 5;
        form.add(lblPhoneVal, gbc);

        gbc.gridy = 6;
        form.add(new JLabel("Registered Slab Type:"), gbc);
        lblTypeVal = new JLabel("-");
        lblTypeVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTypeVal.setForeground(new Color(41, 128, 185));
        gbc.gridy = 7;
        form.add(lblTypeVal, gbc);

        gbc.gridy = 8;
        form.add(new JLabel("Physical Billing Address:"), gbc);
        lblAddressVal = new JLabel("-");
        lblAddressVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 9;
        form.add(lblAddressVal, gbc);

        // Edit Profile Button
        JButton btnEditProfile = new JButton("✏️ Edit Profile Contacts");
        btnEditProfile.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnEditProfile.setFocusPainted(false);
        btnEditProfile.addActionListener(e -> showEditProfileDialog());
        gbc.gridy = 10;
        gbc.insets = new Insets(15, 0, 5, 0);
        form.add(btnEditProfile, gbc);

        left.add(form);
        left.add(Box.createVerticalStrut(15));

        // Mini KPI cards
        JPanel miniKpi = new JPanel(new GridLayout(3, 1, 5, 8));
        miniKpi.setBorder(BorderFactory.createTitledBorder("Financial Summary"));

        lblOutstandingVal = new JLabel("₹0.00", SwingConstants.CENTER);
        lblOutstandingVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblOutstandingVal.setForeground(new Color(231, 76, 60));
        JPanel cardO = new JPanel(new BorderLayout());
        cardO.add(new JLabel(" Outstanding Dues:"), BorderLayout.NORTH);
        cardO.add(lblOutstandingVal, BorderLayout.CENTER);
        miniKpi.add(cardO);

        lblTotalPaidVal = new JLabel("₹0.00", SwingConstants.CENTER);
        lblTotalPaidVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotalPaidVal.setForeground(new Color(46, 204, 113));
        JPanel cardP = new JPanel(new BorderLayout());
        cardP.add(new JLabel(" Total Paid:"), BorderLayout.NORTH);
        cardP.add(lblTotalPaidVal, BorderLayout.CENTER);
        miniKpi.add(cardP);

        lblTotalConsumedVal = new JLabel("0.00 kWh", SwingConstants.CENTER);
        lblTotalConsumedVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JPanel cardC = new JPanel(new BorderLayout());
        cardC.add(new JLabel(" Historical Usage:"), BorderLayout.NORTH);
        cardC.add(lblTotalConsumedVal, BorderLayout.CENTER);
        miniKpi.add(cardC);

        left.add(miniKpi);
        left.add(Box.createVerticalGlue());

        return left;
    }

    private JPanel createMainDashboardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        // Visual Graphs Tabbed Panel
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.50;

        JTabbedPane graphTabs = new JTabbedPane();
        graphTabs.setFont(new Font("Segoe UI", Font.BOLD, 11));

        chartPriceTrend = new CustomChart(CustomChart.Type.LINE_CHART);
        chartConsumption = new CustomChart(CustomChart.Type.BAR_CHART);
        chartSlabSplit = new CustomChart(CustomChart.Type.DONUT_CHART);

        graphTabs.addTab("📈 Bill Amount Variations", chartPriceTrend);
        graphTabs.addTab("⚡ kWh Consumption Trend", chartConsumption);
        graphTabs.addTab("🥧 Slab Consumption Distribution", chartSlabSplit);

        JPanel tabWrapper = new JPanel(new BorderLayout());
        tabWrapper.setBorder(BorderFactory.createTitledBorder("📊 Visual Usage Analytics"));
        tabWrapper.add(graphTabs, BorderLayout.CENTER);
        panel.add(tabWrapper, gbc);

        // Bill statements table Row
        gbc.gridy = 1;
        gbc.weighty = 0.40;
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("📜 Historical Billing Invoices & Ledger"));

        String[] cols = {"Invoice ID", "Billing Date", "Units Consumed", "Total Amount (₹)", "Status"};
        modelBills = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBills = new JTable(modelBills);
        tblBills.setRowHeight(22);
        tablePanel.add(new JScrollPane(tblBills), BorderLayout.CENTER);
        panel.add(tablePanel, gbc);

        // Operation Buttons Row
        gbc.gridy = 2;
        gbc.weighty = 0.10;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));

        JButton btnPayBill = new JButton("💳 Pay Selected Bill");
        btnPayBill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPayBill.setBackground(new Color(46, 204, 113)); // Green
        btnPayBill.setForeground(Color.WHITE);
        btnPayBill.addActionListener(e -> paySelectedBill());
        btnPanel.add(btnPayBill);

        JButton btnPrint = new JButton("🖨️ Print Selected Invoice");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setBackground(new Color(41, 128, 185)); // Blue
        btnPrint.setForeground(Color.WHITE);
        btnPrint.addActionListener(e -> printInvoice());
        btnPanel.add(btnPrint);

        JButton btnDownload = new JButton("📥 Download Invoice HTML");
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDownload.addActionListener(e -> downloadInvoiceHTML());
        btnPanel.add(btnDownload);

        panel.add(btnPanel, gbc);

        return panel;
    }

    private void showEditProfileDialog() {
        if (activeCustomer == null) return;

        JTextField txtAddr = new JTextField(activeCustomer.getAddress(), 20);
        JTextField txtPhone = new JTextField(activeCustomer.getContactNumber() != null ? activeCustomer.getContactNumber() : "", 20);

        JPanel editForm = new JPanel(new GridLayout(0, 1, 5, 5));
        editForm.add(new JLabel("Full Name (Read-Only Verification):"));
        JLabel nameLbl = new JLabel(activeCustomer.getName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        editForm.add(nameLbl);
        editForm.add(new JLabel("Address *"));
        editForm.add(txtAddr);
        editForm.add(new JLabel("Contact Number (10 Digits)"));
        editForm.add(txtPhone);

        int option = JOptionPane.showConfirmDialog(this, editForm, "✏️ Edit Profile Details", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String addr = txtAddr.getText().trim();
            String phone = txtPhone.getText().trim();

            if (addr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Address is mandatory.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
                JOptionPane.showMessageDialog(this, "Contact number must be exactly 10 digits.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            activeCustomer.setAddress(addr);
            activeCustomer.setContactNumber(phone);

            try {
                customerDAO.updateCustomer(activeCustomer);
                
                // Add system log activity for admin portal
                SettingsDAO.addLog("Customer " + activeCustomer.getName() + " (" + activeCustomer.getMeterNumber() + ") updated contact/address.");

                // Refresh panel text
                lblAddressVal.setText(addr);
                lblPhoneVal.setText(phone.isEmpty() ? "-" : phone);

                JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.notifyDataChanged();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database update error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void paySelectedBill() {
        int selected = tblBills.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the ledger table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Bill bill = billsList.get(selected);
        if ("PAID".equalsIgnoreCase(bill.getStatus())) {
            JOptionPane.showMessageDialog(this, "This bill has already been paid successfully.", "Transaction Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show Simulated Payment Gateway
        JComboBox<String> cbMethods = new JComboBox<>(new String[]{"UPI (GPay / PhonePe / Paytm)", "Debit/Credit Card", "Net Banking"});
        JTextField txtCardDetails = new JTextField();
        txtCardDetails.setToolTipText("Enter Card Number / UPI ID");

        JPanel gateway = new JPanel(new GridLayout(0, 1, 5, 8));
        gateway.add(new JLabel("Invoice ID: INV-" + String.format("%05d", bill.getBillId())));
        gateway.add(new JLabel("Amount Due: ₹" + String.format("%.2f", bill.getAmount())));
        gateway.add(new JLabel("Select Payment Method:"));
        gateway.add(cbMethods);
        gateway.add(new JLabel("Enter payment details / Card No / VPA address:"));
        gateway.add(txtCardDetails);

        int option = JOptionPane.showConfirmDialog(this, gateway, "💳 Simulated Secure Payment Gateway", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String details = txtCardDetails.getText().trim();
            if (details.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in payment details to authorize transaction.", "Authorization Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Simulate loading process
            try {
                billDAO.updateBillStatus(bill.getBillId(), "PAID");
                bill.setStatus("PAID");

                // Log system activity
                SettingsDAO.addLog("Received simulated payment of ₹" + String.format("%.2f", bill.getAmount()) + " from customer " + activeCustomer.getName() + " for INV-" + String.format("%05d", bill.getBillId()));

                JOptionPane.showMessageDialog(this, "Payment approved!\nReceipt INV-" + String.format("%05d", bill.getBillId()) + " generated successfully.", "Transaction Approved", JOptionPane.INFORMATION_MESSAGE);
                
                mainFrame.notifyDataChanged();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database update error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printInvoice() {
        int selected = tblBills.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the ledger table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Bill bill = billsList.get(selected);
        try {
            CompanyProfile profile = settingsDAO.getCompanyProfile();
            List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
            
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new InvoicePrintTemplate(activeCustomer, bill, profile, slabs));
            
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "Print request sent successfully!", "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadInvoiceHTML() {
        int selected = tblBills.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the ledger table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Bill bill = billsList.get(selected);
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("INV_" + String.format("%05d", bill.getBillId()) + ".html"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                CompanyProfile profile = settingsDAO.getCompanyProfile();
                List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
                
                String html = generateInvoiceHTML(activeCustomer, bill, profile, slabs);
                
                try (PrintWriter writer = new PrintWriter(new FileWriter(dest))) {
                    writer.print(html);
                }
                
                JOptionPane.showMessageDialog(this, "Invoice statement saved to:\n" + dest.getAbsolutePath(), "Download Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to download invoice: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String generateInvoiceHTML(Customer c, Bill b, CompanyProfile cp, List<SlabRate> slabs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Invoice INV-").append(String.format("%05d", b.getBillId())).append("</title>");
        sb.append("<style>");
        sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: #2c3e50; line-height: 1.5; margin: 40px; }");
        sb.append(".invoice-box { padding: 30px; border: 1px solid #eee; box-shadow: 0 0 10px rgba(0, 0, 0, 0.15); max-width: 800px; margin: auto; }");
        sb.append(".header { border-bottom: 2px solid #3498db; padding-bottom: 15px; margin-bottom: 25px; display: flex; justify-content: space-between; }");
        sb.append(".company { font-size: 24px; font-weight: bold; color: #2980b9; }");
        sb.append(".inv-title { font-size: 18px; font-weight: bold; text-align: right; color: #7f8c8d; }");
        sb.append(".section { margin-bottom: 20px; }");
        sb.append(".section-title { font-size: 14px; font-weight: bold; text-transform: uppercase; color: #34495e; border-bottom: 1px solid #ecf0f1; padding-bottom: 5px; }");
        sb.append(".meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 10px; }");
        sb.append(".grid { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        sb.append(".grid th, .grid td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; }");
        sb.append(".grid th { background-color: #f8fafc; font-weight: bold; }");
        sb.append(".total-box { float: right; width: 300px; margin-top: 20px; }");
        sb.append(".total-row { display: flex; justify-content: space-between; padding: 6px 0; }");
        sb.append(".grand-total { font-weight: bold; font-size: 16px; border-top: 1.5px solid #2ecc71; padding-top: 8px; color: #27ae60; }");
        sb.append(".footer { margin-top: 100px; text-align: center; font-size: 11px; color: #bdc3c7; }");
        sb.append("</style></head><body>");
        
        sb.append("<div class='invoice-box'>");
        
        // Header
        sb.append("<div class='header'>");
        sb.append("<div><div class='company'>").append(cp.getCompanyName()).append("</div>");
        sb.append("<div>").append(cp.getAddress()).append("</div>");
        sb.append("<div>Hotline: ").append(cp.getContactNumber()).append("</div></div>");
        sb.append("<div><div class='inv-title'>INVOICE STATEMENT</div>");
        sb.append("<div><b>Invoice:</b> INV-").append(String.format("%05d", b.getBillId())).append("</div>");
        sb.append("<div><b>Date:</b> ").append(b.getBillDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))).append("</div>");
        sb.append("<div><b>Status:</b> ").append(b.getStatus()).append("</div></div>");
        sb.append("</div>");

        // Billing Meta Details
        sb.append("<div class='meta-grid'>");
        sb.append("<div class='section'><div class='section-title'>BILL TO:</div>");
        sb.append("<div style='margin-top: 5px;'><b>Name:</b> ").append(c.getName()).append("</div>");
        sb.append("<div><b>Meter ID:</b> ").append(c.getMeterNumber()).append("</div>");
        sb.append("<div><b>Slab Account:</b> ").append(c.getCustomerType()).append("</div>");
        sb.append("<div><b>Address:</b> ").append(c.getAddress()).append("</div></div>");

        sb.append("<div class='section'><div class='section-title'>METER READINGS:</div>");
        sb.append("<div style='margin-top: 5px;'><b>Previous Reading:</b> ").append(String.format("%.2f", b.getPreviousReading())).append(" units</div>");
        sb.append("<div><b>Current Reading:</b> ").append(String.format("%.2f", b.getCurrentReading())).append(" units</div>");
        sb.append("<div><b>Units Consumed:</b> ").append(String.format("%.2f", b.getUnitsConsumed())).append(" kWh</div></div>");
        sb.append("</div>");

        // Billing slabs table
        sb.append("<div class='section'><div class='section-title'>SLAB CALCULATION DETAILS</div>");
        sb.append("<table class='grid'><thead><tr><th>Slab Level</th><th>Usage Range</th><th>Consumed Units</th><th>Rate (INR)</th><th>Amount (INR)</th></tr></thead><tbody>");
        
        double remaining = b.getUnitsConsumed();
        double subtotal = 0.0;
        
        for (SlabRate slab : slabs) {
            double rangeStart = slab.getSlabNum() == 1 ? 0 : slabs.get(slab.getSlabNum() - 2).getLimitVal();
            double rangeEnd = slab.getLimitVal();
            double slabLimit = rangeEnd - rangeStart;
            double u = Math.min(remaining, slabLimit);

            if (u > 0) {
                double amt = u * slab.getRateVal();
                subtotal += amt;
                remaining -= u;

                sb.append("<tr>");
                sb.append("<td>Slab ").append(slab.getSlabNum()).append("</td>");
                String rEndStr = rangeEnd >= 999999 ? "Infinity" : String.format("%.0f", rangeEnd);
                sb.append("<td>").append(String.format("%.0f", rangeStart)).append(" - ").append(rEndStr).append("</td>");
                sb.append("<td>").append(String.format("%.2f", u)).append("</td>");
                sb.append("<td>₹").append(String.format("%.2f", slab.getRateVal())).append("</td>");
                sb.append("<td>₹").append(String.format("%.2f", amt)).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div>");

        // Summary Totals
        double taxAmt = subtotal * (cp.getTaxRate() / 100.0);
        double grand = subtotal + taxAmt;
        
        sb.append("<div class='total-box'>");
        sb.append("<div class='total-row'><span>Subtotal:</span><span>₹").append(String.format("%.2f", subtotal)).append("</span></div>");
        sb.append("<div class='total-row'><span>Tax (").append(String.format("%.1f", cp.getTaxRate())).append("%):</span><span>₹").append(String.format("%.2f", taxAmt)).append("</span></div>");
        sb.append("<div class='total-row grand-total'><span>Net Dues:</span><span>₹").append(String.format("%.2f", grand)).append("</span></div>");
        sb.append("</div>");
        sb.append("<div style='clear: both;'></div>");

        // Footer
        sb.append("<div class='footer'>");
        sb.append("Thank you for your business. Generated securely via ElectriFlow Systems.");
        sb.append("</div>");
        
        sb.append("</div></body></html>");
        return sb.toString();
    }

    public void refreshData() {
        if (activeCustomer == null) return;

        try {
            // Load bills
            billsList = billDAO.getBillsByCustomer(activeCustomer.getCustomerId());
            modelBills.setRowCount(0);

            double outstanding = 0.0;
            double paidTotal = 0.0;
            double totalConsumed = 0.0;

            // Populate table and calculate dues
            for (Bill b : billsList) {
                totalConsumed += b.getUnitsConsumed();
                if ("UNPAID".equalsIgnoreCase(b.getStatus())) {
                    outstanding += b.getAmount();
                } else {
                    paidTotal += b.getAmount();
                }

                modelBills.addRow(new Object[]{
                        "INV-" + String.format("%05d", b.getBillId()),
                        b.getBillDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                        String.format("%.2f units", b.getUnitsConsumed()),
                        String.format("₹%.2f", b.getAmount()),
                        b.getStatus()
                });
            }

            // Set statistics displays
            lblOutstandingVal.setText(String.format("₹%.2f", outstanding));
            lblTotalPaidVal.setText(String.format("₹%.2f", paidTotal));
            lblTotalConsumedVal.setText(String.format("%.2f kWh", totalConsumed));

            // Visual Charts Generation
            int count = Math.min(6, billsList.size());
            double[] priceTrendVals = new double[count];
            double[] consTrendVals = new double[count];
            String[] trendLabels = new String[count];

            // Reconstruct lists in chronological order (left to right)
            for (int i = 0; i < count; i++) {
                Bill b = billsList.get(count - 1 - i);
                priceTrendVals[i] = b.getAmount();
                consTrendVals[i] = b.getUnitsConsumed();
                trendLabels[i] = b.getBillDate().format(DateTimeFormatter.ofPattern("MMM-yy"));
            }

            // Set Price Trend & Consumption Bar Chart data
            chartPriceTrend.setData(priceTrendVals, trendLabels);
            chartConsumption.setData(consTrendVals, trendLabels);

            // Compute Slab Split for Donut Chart
            double s1Total = 0.0;
            double s2Total = 0.0;
            double s3Total = 0.0;

            List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
            if (slabs.size() >= 3) {
                double limit1 = slabs.get(0).getLimitVal();
                double limit2 = slabs.get(1).getLimitVal();

                for (Bill b : billsList) {
                    double remaining = b.getUnitsConsumed();
                    
                    // Slab 1
                    double u1 = Math.min(remaining, limit1);
                    s1Total += u1;
                    remaining -= u1;

                    // Slab 2
                    double u2 = Math.min(remaining, limit2 - limit1);
                    s2Total += u2;
                    remaining -= u2;

                    // Slab 3
                    if (remaining > 0) {
                        s3Total += remaining;
                    }
                }
            }

            chartSlabSplit.setDonutData(
                    new double[]{s1Total, s2Total, s3Total},
                    new String[]{"Slab 1 (Basic)", "Slab 2 (Standard)", "Slab 3 (Peak)"},
                    new Color[]{new Color(46, 204, 113), new Color(241, 196, 15), new Color(231, 76, 60)}
            );

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
