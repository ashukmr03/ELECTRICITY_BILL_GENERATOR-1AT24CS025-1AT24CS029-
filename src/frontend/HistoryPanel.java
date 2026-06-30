package frontend;

import backend.dao.BillDAO;
import backend.dao.CustomerDAO;
import backend.dao.SettingsDAO;
import backend.model.Bill;
import backend.model.Customer;
import backend.model.CompanyProfile;
import backend.model.SlabRate;
import frontend.components.InvoicePrintTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 * HistoryPanel lists, searches, filters, and prints past invoices.
 * Restricts updates based on multi-role authorization levels.
 */
public class HistoryPanel extends JPanel {
    private final MainFrame mainFrame;
    private final BillDAO billDAO;
    private final CustomerDAO customerDAO;
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // UI elements
    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private JButton btnToggleStatus;
    private JButton btnPrint;
    private JButton btnDownload;

    // Cache of current bills shown in the table
    private final List<Bill> activeBillsList = new ArrayList<>();

    public HistoryPanel(MainFrame mainFrame, CustomerDAO customerDAO, BillDAO billDAO) {
        this.mainFrame = mainFrame;
        this.customerDAO = customerDAO;
        this.billDAO = billDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create UI components
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createActionsPanel(), BorderLayout.SOUTH);

        // Load data
        refreshData();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 5));
        
        JLabel lblTitle = new JLabel("Invoice History & Ledger");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        panel.add(lblTitle, BorderLayout.WEST);

        // Search and filter panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        searchPanel.add(new JLabel("Search (Name/Meter):"));
        txtSearch = new JTextField(15);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterData(); }
            public void removeUpdate(DocumentEvent e) { filterData(); }
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });
        searchPanel.add(txtSearch);

        searchPanel.add(new JLabel("Status:"));
        cbFilterStatus = new JComboBox<>(new String[]{"ALL", "PAID", "UNPAID"});
        cbFilterStatus.addActionListener(e -> filterData());
        searchPanel.add(cbFilterStatus);

        panel.add(searchPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Invoices"));

        String[] columns = {"Invoice ID", "Customer Name", "Meter Number", "Billing Date", "Units Consumed", "Total Amount (₹)", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setRowHeight(25);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        btnToggleStatus = new JButton("Toggle Payment Status");
        btnToggleStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnToggleStatus.setPreferredSize(new Dimension(180, 36));
        btnToggleStatus.setBackground(new Color(230, 126, 34)); // Orange accent
        btnToggleStatus.setForeground(Color.WHITE);
        btnToggleStatus.setFocusPainted(false);
        btnToggleStatus.addActionListener(e -> toggleBillStatus());
        panel.add(btnToggleStatus);

        btnPrint = new JButton("🖨️ Print Invoice PDF");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setPreferredSize(new Dimension(160, 36));
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printInvoice());
        panel.add(btnPrint);

        btnDownload = new JButton("📥 Download HTML");
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDownload.setPreferredSize(new Dimension(150, 36));
        btnDownload.setFocusPainted(false);
        btnDownload.addActionListener(e -> downloadHTMLInvoice());
        panel.add(btnDownload);

        JButton btnRefresh = new JButton("Refresh Table");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.setPreferredSize(new Dimension(120, 36));
        btnRefresh.addActionListener(e -> refreshData());
        panel.add(btnRefresh);

        return panel;
    }

    private void toggleBillStatus() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the table first.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the active bill reference from cache
        Bill selectedBill = activeBillsList.get(selectedRow);
        String currentStatus = selectedBill.getStatus();
        String newStatus = "PAID".equalsIgnoreCase(currentStatus) ? "UNPAID" : "PAID";

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to mark Invoice INV-" + String.format("%05d", selectedBill.getBillId()) + " as " + newStatus + "?",
                "Confirm Status Change", 
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                billDAO.updateBillStatus(selectedBill.getBillId(), newStatus);
                selectedBill.setStatus(newStatus); // Update cache
                refreshData(); // Reload table
                mainFrame.notifyDataChanged();
                JOptionPane.showMessageDialog(this, "Status updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printInvoice() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the table first.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Bill selectedBill = activeBillsList.get(selectedRow);
        try {
            Customer customer = customerDAO.getCustomerById(selectedBill.getCustomerId());
            CompanyProfile profile = settingsDAO.getCompanyProfile();
            List<SlabRate> slabs = settingsDAO.getSlabRates(customer.getCustomerType());

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new InvoicePrintTemplate(customer, selectedBill, profile, slabs));

            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "Print request sent successfully!", "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadHTMLInvoice() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the table first.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Bill selectedBill = activeBillsList.get(selectedRow);
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("INV_" + String.format("%05d", selectedBill.getBillId()) + ".html"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                Customer customer = customerDAO.getCustomerById(selectedBill.getCustomerId());
                CompanyProfile profile = settingsDAO.getCompanyProfile();
                List<SlabRate> slabs = settingsDAO.getSlabRates(customer.getCustomerType());

                String html = generateInvoiceHTML(customer, selectedBill, profile, slabs);

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

    /**
     * Filters the cached list of bills based on the search keyword and selected status,
     * then populates the table.
     */
    private void filterData() {
        try {
            tableModel.setRowCount(0);
            activeBillsList.clear();

            String keyword = txtSearch.getText().trim().toLowerCase();
            String statusFilter = (String) cbFilterStatus.getSelectedItem();

            List<Bill> allBills = billDAO.getAllBills();

            for (Bill bill : allBills) {
                // Fetch customer details to filter
                Customer customer = customerDAO.getCustomerById(bill.getCustomerId());
                if (customer == null) continue;

                // Match Keyword (Name or Meter Number)
                boolean matchesKeyword = keyword.isEmpty() || 
                        customer.getName().toLowerCase().contains(keyword) || 
                        customer.getMeterNumber().toLowerCase().contains(keyword);

                // Match Status
                boolean matchesStatus = "ALL".equalsIgnoreCase(statusFilter) || 
                        bill.getStatus().equalsIgnoreCase(statusFilter);

                if (matchesKeyword && matchesStatus) {
                    activeBillsList.add(bill);
                    tableModel.addRow(new Object[]{
                            "INV-" + String.format("%05d", bill.getBillId()),
                            customer.getName(),
                            customer.getMeterNumber(),
                            bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                            String.format("%.2f", bill.getUnitsConsumed()),
                            String.format("%.2f", bill.getAmount()),
                            bill.getStatus()
                    });
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error filtering data: " + ex.getMessage());
        }
    }

    /**
     * Refreshes the data list and filters the table.
     */
    public void refreshData() {
        if (mainFrame != null) {
            String role = mainFrame.getUserRole();
            if ("VIEWER".equals(role)) {
                btnToggleStatus.setVisible(false); // Hide status toggle for read-only Viewers
            } else {
                btnToggleStatus.setVisible(true);
            }
        }
        filterData();
    }
}
