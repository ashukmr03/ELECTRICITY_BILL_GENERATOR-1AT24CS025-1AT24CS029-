package frontend;

import backend.dao.BillDAO;
import backend.dao.SettingsDAO;
import backend.model.Customer;
import backend.model.Bill;
import backend.model.CompanyProfile;
import backend.model.SlabRate;
import frontend.components.InvoicePrintTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BillPanel displays the invoice calculations, detailing the slab breakdown,
 * and allows saving the bill, marking payment status, printing PDFs, and exporting invoices.
 */
public class BillPanel extends JPanel {
    private final MainFrame mainFrame;
    private final BillDAO billDAO;
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // Active state
    private Customer activeCustomer;
    private double prevReading;
    private double currReading;
    private double unitsConsumed;
    private double totalAmount;
    private Bill activeBill; // Cached after saving

    // UI elements
    private JLabel lblInvoiceNo;
    private JLabel lblDateVal;
    private JLabel lblNameVal;
    private JLabel lblMeterVal;
    private JLabel lblTypeVal;
    private JLabel lblAddressVal;
    private JLabel lblPrevVal;
    private JLabel lblCurrVal;
    private JLabel lblUnitsVal;
    private JEditorPane epBreakdown;
    private JToggleButton btnStatusToggle;
    private JButton btnSave;
    private JButton btnPrint;
    private JButton btnDownload;

    public BillPanel(MainFrame mainFrame, BillDAO billDAO) {
        this.mainFrame = mainFrame;
        this.billDAO = billDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create UI components
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createInvoiceContainer(), BorderLayout.CENTER);
        add(createActionsPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblTitle = new JLabel("Bill Review & Invoice Generation");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerPanel.add(lblTitle);
        return headerPanel;
    }

    private JComponent createInvoiceContainer() {
        JPanel paper = new JPanel(new BorderLayout(10, 10));
        paper.setBackground(UIManager.getColor("Panel.background"));
        paper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));

        // Invoice Header
        JPanel invHeader = new JPanel(new BorderLayout());
        invHeader.setOpaque(false);
        
        JLabel lblTitle = new JLabel("ELECTRICITY BILL STATEMENT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(41, 128, 185));
        invHeader.add(lblTitle, BorderLayout.WEST);

        JPanel detailsRight = new JPanel(new GridLayout(2, 1, 2, 2));
        detailsRight.setOpaque(false);
        lblInvoiceNo = new JLabel("Invoice: [Pending Save]");
        lblInvoiceNo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblInvoiceNo.setHorizontalAlignment(SwingConstants.RIGHT);
        lblDateVal = new JLabel("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        lblDateVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDateVal.setHorizontalAlignment(SwingConstants.RIGHT);
        detailsRight.add(lblInvoiceNo);
        detailsRight.add(lblDateVal);
        invHeader.add(detailsRight, BorderLayout.EAST);

        paper.add(invHeader, BorderLayout.NORTH);

        // Invoice Body (Split into Left: Customer Info, Right: Reading & Breakdown)
        JPanel invBody = new JPanel(new GridBagLayout());
        invBody.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 0, 10, 0);

        // Divider Line
        JSeparator sep = new JSeparator();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        invBody.add(sep, gbc);

        // Section 1: Billing & Customer Information
        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(false);
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.anchor = GridBagConstraints.WEST;
        cGbc.insets = new Insets(4, 0, 4, 8);
        cGbc.gridx = 0;

        cGbc.gridy = 0;
        leftCol.add(new JLabel("Customer Name:"), cGbc);
        lblNameVal = new JLabel("-");
        lblNameVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cGbc.gridx = 1;
        leftCol.add(lblNameVal, cGbc);

        cGbc.gridx = 0;
        cGbc.gridy = 1;
        leftCol.add(new JLabel("Meter Number:"), cGbc);
        lblMeterVal = new JLabel("-");
        lblMeterVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cGbc.gridx = 1;
        leftCol.add(lblMeterVal, cGbc);

        cGbc.gridx = 0;
        cGbc.gridy = 2;
        leftCol.add(new JLabel("Customer Type:"), cGbc);
        lblTypeVal = new JLabel("-");
        lblTypeVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cGbc.gridx = 1;
        leftCol.add(lblTypeVal, cGbc);

        cGbc.gridx = 0;
        cGbc.gridy = 3;
        leftCol.add(new JLabel("Service Address:"), cGbc);
        lblAddressVal = new JLabel("-");
        lblAddressVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cGbc.gridx = 1;
        leftCol.add(lblAddressVal, cGbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.4;
        gbc.weighty = 1.0;
        invBody.add(leftCol, gbc);

        // Section 2: Readings & Consumption Info
        JPanel rightCol = new JPanel(new GridBagLayout());
        rightCol.setOpaque(false);
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.anchor = GridBagConstraints.WEST;
        rGbc.insets = new Insets(4, 8, 4, 0);
        rGbc.gridx = 0;

        rGbc.gridy = 0;
        rightCol.add(new JLabel("Previous Reading:"), rGbc);
        lblPrevVal = new JLabel("-");
        lblPrevVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rGbc.gridx = 1;
        rightCol.add(lblPrevVal, rGbc);

        rGbc.gridx = 0;
        rGbc.gridy = 1;
        rightCol.add(new JLabel("Current Reading:"), rGbc);
        lblCurrVal = new JLabel("-");
        lblCurrVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rGbc.gridx = 1;
        rightCol.add(lblCurrVal, rGbc);

        rGbc.gridx = 0;
        rGbc.gridy = 2;
        rightCol.add(new JLabel("Units Consumed:"), rGbc);
        lblUnitsVal = new JLabel("-");
        lblUnitsVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUnitsVal.setForeground(new Color(230, 126, 34));
        rGbc.gridx = 1;
        rightCol.add(lblUnitsVal, rGbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        invBody.add(rightCol, gbc);

        // Divider 2
        JSeparator sep2 = new JSeparator();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        invBody.add(sep2, gbc);

        // Section 3: Detailed slab calculations
        epBreakdown = new JEditorPane();
        epBreakdown.setEditable(false);
        epBreakdown.setContentType("text/html");
        epBreakdown.setOpaque(false);
        epBreakdown.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane scrollBreakdown = new JScrollPane(epBreakdown);
        scrollBreakdown.setBorder(BorderFactory.createTitledBorder("Slab Consumption & Pricing Breakdown"));
        scrollBreakdown.setOpaque(false);
        scrollBreakdown.getViewport().setOpaque(false);

        gbc.gridy = 3;
        gbc.weighty = 2.0;
        invBody.add(scrollBreakdown, gbc);

        paper.add(invBody, BorderLayout.CENTER);
        return paper;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Payment status toggle button
        btnStatusToggle = new JToggleButton("Status: UNPAID");
        btnStatusToggle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStatusToggle.setPreferredSize(new Dimension(140, 38));
        btnStatusToggle.setBackground(new Color(192, 57, 43)); // Red for unpaid
        btnStatusToggle.setForeground(Color.WHITE);
        btnStatusToggle.setFocusPainted(false);
        btnStatusToggle.addActionListener(e -> {
            if (btnStatusToggle.isSelected()) {
                btnStatusToggle.setText("Status: PAID");
                btnStatusToggle.setBackground(new Color(39, 174, 96)); // Green for paid
            } else {
                btnStatusToggle.setText("Status: UNPAID");
                btnStatusToggle.setBackground(new Color(192, 57, 43)); // Red for unpaid
            }
        });
        panel.add(btnStatusToggle);

        // Save button
        btnSave = new JButton("Save Bill");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(110, 38));
        btnSave.setBackground(new Color(41, 128, 185)); // Blue
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> saveBill());
        panel.add(btnSave);

        // Print Invoice button
        btnPrint = new JButton("🖨️ Print PDF");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setPreferredSize(new Dimension(120, 38));
        btnPrint.setFocusPainted(false);
        btnPrint.setEnabled(false); // Enable after save
        btnPrint.addActionListener(e -> printInvoice());
        panel.add(btnPrint);

        // Download HTML button
        btnDownload = new JButton("📥 HTML Export");
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDownload.setPreferredSize(new Dimension(140, 38));
        btnDownload.setFocusPainted(false);
        btnDownload.setEnabled(false); // Enable after save
        btnDownload.addActionListener(e -> downloadHTML());
        panel.add(btnDownload);

        // Cancel/Back button
        JButton btnBack = new JButton("Cancel");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(80, 38));
        btnBack.addActionListener(e -> mainFrame.showReadingPanel());
        panel.add(btnBack);

        return panel;
    }

    /**
     * Initializes the panel with a newly calculated bill setup.
     */
    public void setupBill(Customer customer, double prev, double curr) {
        this.activeCustomer = customer;
        this.prevReading = prev;
        this.currReading = curr;
        this.unitsConsumed = curr - prev;
        this.totalAmount = customer.calculateBill(unitsConsumed);
        this.activeBill = null;

        // Update display text
        lblInvoiceNo.setText("Invoice: [Pending Save]");
        lblDateVal.setText("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        lblNameVal.setText(customer.getName());
        lblMeterVal.setText(customer.getMeterNumber());
        lblTypeVal.setText(customer.getCustomerType());
        lblAddressVal.setText(customer.getAddress());
        lblPrevVal.setText(String.format("%.2f units", prev));
        lblCurrVal.setText(String.format("%.2f units", curr));
        lblUnitsVal.setText(String.format("%.2f units", unitsConsumed));

        // Generate HTML slab breakdown display
        epBreakdown.setText(getBreakdownHTML());

        // Reset toggle to Unpaid
        btnStatusToggle.setSelected(false);
        btnStatusToggle.setText("Status: UNPAID");
        btnStatusToggle.setBackground(new Color(192, 57, 43));
        btnStatusToggle.setEnabled(true);
        
        btnSave.setEnabled(true);
        btnPrint.setEnabled(false);
        btnDownload.setEnabled(false);
    }

    private String getBreakdownHTML() {
        if (activeCustomer == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Segoe UI; font-size: 13px; margin: 5px; color: #CCCCCC;'>");
        sb.append("<table width='100%' border='0' cellspacing='5' cellpadding='2'>");
        sb.append("<tr style='font-weight: bold; border-bottom: 1px solid #444444;'>");
        sb.append("<td>Slab Rate Bracket</td><td>Usage (Units)</td><td>Rate</td><td align='right'>Amount (INR)</td>");
        sb.append("</tr>");

        double remaining = unitsConsumed;
        double subtotal = 0.0;

        try {
            List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
            for (SlabRate slab : slabs) {
                double rangeStart = slab.getSlabNum() == 1 ? 0 : slabs.get(slab.getSlabNum() - 2).getLimitVal();
                double rangeEnd = slab.getLimitVal();
                double slabLimit = rangeEnd - rangeStart;
                double u = Math.min(remaining, slabLimit);

                if (u > 0) {
                    double amt = u * slab.getRateVal();
                    subtotal += amt;
                    remaining -= u;

                    String rEndStr = rangeEnd >= 999999 ? "Infinity" : String.format("%.0f", rangeEnd);
                    sb.append("<tr><td>Slab ").append(slab.getSlabNum())
                      .append(" (").append(String.format("%.0f", rangeStart)).append(" - ").append(rEndStr).append(")</td>")
                      .append("<td>").append(String.format("%.2f", u)).append("</td>")
                      .append("<td>₹").append(String.format("%.2f", slab.getRateVal())).append("</td>")
                      .append("<td align='right'>₹").append(String.format("%.2f", amt)).append("</td></tr>");
                }
            }
        } catch (SQLException ex) {
            sb.append("<tr><td colspan='4' style='color: red;'>Error reading dynamic slab config: ").append(ex.getMessage()).append("</td></tr>");
        }

        // Fetch company profile for tax
        double taxRate = 5.0;
        try {
            CompanyProfile profile = settingsDAO.getCompanyProfile();
            if (profile != null) {
                taxRate = profile.getTaxRate();
            }
        } catch (Exception ex) {
            // Use fallback
        }

        double taxAmt = subtotal * (taxRate / 100.0);
        double grandTotal = subtotal + taxAmt;

        sb.append("<tr><td colspan='4'><hr style='border: 0.5px solid #444444;'/></td></tr>");
        sb.append("<tr><td colspan='3'>Slab Subtotal:</td><td align='right'>₹").append(String.format("%.2f", subtotal)).append("</td></tr>");
        sb.append("<tr><td colspan='3'>Govt Tax (").append(String.format("%.1f", taxRate)).append("%):</td><td align='right'>₹").append(String.format("%.2f", taxAmt)).append("</td></tr>");
        sb.append("<tr style='font-weight: bold; font-size: 14px;'>");
        sb.append("<td colspan='3'>Total Invoice Value:</td>");
        sb.append("<td align='right' style='color: #2ecc71;'>₹").append(String.format("%.2f", grandTotal)).append("</td>");
        sb.append("</tr>");
        sb.append("</table></body></html>");

        return sb.toString();
    }

    private void saveBill() {
        if (activeCustomer == null) return;

        String status = btnStatusToggle.isSelected() ? "PAID" : "UNPAID";
        
        // Calculate the exact amount including tax dynamically
        double subtotal = 0.0;
        double remaining = unitsConsumed;
        try {
            List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
            for (SlabRate slab : slabs) {
                double rangeStart = slab.getSlabNum() == 1 ? 0 : slabs.get(slab.getSlabNum() - 2).getLimitVal();
                double rangeEnd = slab.getLimitVal();
                double slabLimit = rangeEnd - rangeStart;
                double u = Math.min(remaining, slabLimit);
                if (u > 0) {
                    subtotal += u * slab.getRateVal();
                    remaining -= u;
                }
            }
        } catch (SQLException ex) {
            subtotal = totalAmount; // Fallback to customer's override
        }

        double taxRate = 5.0;
        try {
            CompanyProfile profile = settingsDAO.getCompanyProfile();
            if (profile != null) taxRate = profile.getTaxRate();
        } catch (Exception ex) {}

        double amtWithTax = subtotal + (subtotal * (taxRate / 100.0));

        activeBill = new Bill(
                activeCustomer.getCustomerId(),
                prevReading,
                currReading,
                unitsConsumed,
                LocalDate.now(),
                amtWithTax,
                status
        );

        try {
            billDAO.generateBill(activeBill);
            lblInvoiceNo.setText("Invoice: INV-" + String.format("%05d", activeBill.getBillId()));
            
            btnSave.setEnabled(false); 
            btnStatusToggle.setEnabled(false); // Lock status modification on this screen once saved
            btnPrint.setEnabled(true);
            btnDownload.setEnabled(true);
            
            JOptionPane.showMessageDialog(this, 
                    "Bill generated and saved successfully!\nInvoice ID: INV-" + String.format("%05d", activeBill.getBillId()), 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            
            mainFrame.notifyDataChanged(); // Notify MainFrame to update charts
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error generating bill: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printInvoice() {
        if (activeBill == null) return;
        try {
            CompanyProfile profile = settingsDAO.getCompanyProfile();
            List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
            
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new InvoicePrintTemplate(activeCustomer, activeBill, profile, slabs));
            
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "Print invoice request sent successfully!", "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadHTML() {
        if (activeBill == null) return;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("INV_" + String.format("%05d", activeBill.getBillId()) + ".html"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                CompanyProfile profile = settingsDAO.getCompanyProfile();
                List<SlabRate> slabs = settingsDAO.getSlabRates(activeCustomer.getCustomerType());
                
                // Construct HTML contents
                String html = generateInvoiceHTML(activeCustomer, activeBill, profile, slabs);
                
                try (PrintWriter writer = new PrintWriter(new FileWriter(dest))) {
                    writer.print(html);
                }
                
                JOptionPane.showMessageDialog(this, "Invoice statement exported successfully to:\n" + dest.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export invoice: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
}
