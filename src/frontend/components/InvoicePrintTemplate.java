package frontend.components;

import backend.model.Bill;
import backend.model.Customer;
import backend.model.CompanyProfile;
import backend.model.SlabRate;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * InvoicePrintTemplate renders a professional, formatted bill statement
 * onto a Java AWT Printable page. Used for generating print invoices and PDFs.
 */
public class InvoicePrintTemplate implements Printable {
    private final Customer customer;
    private final Bill bill;
    private final CompanyProfile profile;
    private final List<SlabRate> slabs;

    public InvoicePrintTemplate(Customer customer, Bill bill, CompanyProfile profile, List<SlabRate> slabs) {
        this.customer = customer;
        this.bill = bill;
        this.profile = profile;
        this.slabs = slabs;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Translate to printable margin origin
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        int width = (int) pageFormat.getImageableWidth();
        int height = (int) pageFormat.getImageableHeight();

        // Standard margin coordinates
        int x = 40;
        int y = 50;
        int lineSpacing = 18;

        // Draw company header
        g2d.setColor(new Color(41, 128, 185)); // Header blue
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2d.drawString(profile.getCompanyName(), x, y);
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        y += 18;
        g2d.drawString(profile.getAddress(), x, y);
        y += 13;
        g2d.drawString("Contact: " + profile.getContactNumber(), x, y);

        // Draw Invoice Details (Right-aligned)
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2d.drawString("INVOICE STATEMENT", width - 180, 50);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2d.drawString("Invoice No: INV-" + String.format("%05d", bill.getBillId()), width - 180, 68);
        String formattedDate = bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        g2d.drawString("Date: " + formattedDate, width - 180, 81);
        g2d.drawString("Status: " + bill.getStatus().toUpperCase(), width - 180, 94);

        // Divider
        y += 25;
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawLine(x, y, width - x, y);

        // Customer details
        y += 20;
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2d.drawString("BILL TO:", x, y);
        
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        y += lineSpacing;
        g2d.drawString("Name:       " + customer.getName(), x, y);
        y += lineSpacing;
        g2d.drawString("Meter No:   " + customer.getMeterNumber(), x, y);
        y += lineSpacing;
        g2d.drawString("Address:    " + customer.getAddress(), x, y);
        y += lineSpacing;
        g2d.drawString("Slab Type:  " + customer.getCustomerType(), x, y);

        // Meter Readings info
        int leftColX = x;
        int rightColX = width / 2 + 20;
        int startReadingY = y + 25;
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(x, startReadingY, width - x, startReadingY);

        y = startReadingY + 20;
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.drawString("METER READING HISTORY", leftColX, y);
        
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        y += lineSpacing;
        g2d.drawString("Previous Reading:  " + String.format("%.2f", bill.getPreviousReading()) + " units", leftColX, y);
        y += lineSpacing;
        g2d.drawString("Current Reading:   " + String.format("%.2f", bill.getCurrentReading()) + " units", leftColX, y);
        y += lineSpacing;
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.drawString("Units Consumed:    " + String.format("%.2f", bill.getUnitsConsumed()) + " units", leftColX, y);

        // Slab breakdown table headers
        y += 35;
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(x, y, width - (2 * x), 22);
        
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width - (2 * x), 22);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        
        int rowY = y + 15;
        g2d.drawString("Slab Category", x + 10, rowY);
        g2d.drawString("Slab Range", x + 120, rowY);
        g2d.drawString("Units Consumed", x + 230, rowY);
        g2d.drawString("Rate (INR)", x + 340, rowY);
        g2d.drawString("Amount (INR)", width - x - 80, rowY);

        // Calculate and draw slab rows
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        double remaining = bill.getUnitsConsumed();
        double runningSum = 0.0;
        
        y += 22; // Align to bottom of table header
        
        for (SlabRate slab : slabs) {
            double rangeStart = slab.getSlabNum() == 1 ? 0 : slabs.get(slab.getSlabNum() - 2).getLimitVal();
            double rangeEnd = slab.getLimitVal();
            double slabLimit = rangeEnd - rangeStart;
            double u = Math.min(remaining, slabLimit);

            if (u > 0) {
                double amt = u * slab.getRateVal();
                runningSum += amt;
                remaining -= u;

                g2d.drawRect(x, y, width - (2 * x), 20);
                rowY = y + 14;
                g2d.drawString("Slab " + slab.getSlabNum(), x + 10, rowY);
                String rangeStr = String.format("%.0f - %.0f", rangeStart, rangeEnd >= 999999 ? Double.POSITIVE_INFINITY : rangeEnd);
                g2d.drawString(rangeStr, x + 120, rowY);
                g2d.drawString(String.format("%.2f", u), x + 230, rowY);
                g2d.drawString("₹" + String.format("%.2f", slab.getRateVal()), x + 340, rowY);
                g2d.drawString("₹" + String.format("%.2f", amt), width - x - 80, rowY);
                
                y += 20;
            }
        }

        // Summary details (Subtotal, Tax, Net Total)
        y += 15;
        int summaryX = width - x - 180;
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        g2d.drawString("Slab Charges Subtotal:", summaryX, y);
        g2d.drawString("₹" + String.format("%.2f", runningSum), width - x - 80, y);
        
        y += lineSpacing;
        double taxAmount = runningSum * (profile.getTaxRate() / 100.0);
        g2d.drawString("Government Tax (" + String.format("%.1f", profile.getTaxRate()) + "%):", summaryX, y);
        g2d.drawString("₹" + String.format("%.2f", taxAmount), width - x - 80, y);
        
        y += lineSpacing;
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(summaryX, y - 10, width - x, y - 10);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2d.drawString("TOTAL AMOUNT DUE:", summaryX, y);
        g2d.setColor(new Color(39, 174, 96)); // Green for total amount
        g2d.drawString("₹" + String.format("%.2f", runningSum + taxAmount), width - x - 80, y);

        // Footer lines
        y += 50;
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        String footerText = "Thank you for your business. Please pay within 15 days of the billing date to avoid disconnection.";
        int ftX = (width - g2d.getFontMetrics().stringWidth(footerText)) / 2;
        g2d.drawString(footerText, ftX, y);
        
        y += 12;
        String stampText = "Generated securely via ElectriFlow Systems. Official Copy.";
        int stX = (width - g2d.getFontMetrics().stringWidth(stampText)) / 2;
        g2d.drawString(stampText, stX, y);

        return Printable.PAGE_EXISTS;
    }
}
