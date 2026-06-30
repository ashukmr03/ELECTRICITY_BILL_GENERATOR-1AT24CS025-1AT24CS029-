package backend.model;

import java.time.LocalDate;

/**
 * Bill represents a billing record associated with a Customer.
 * Encapsulates read/write properties for invoice details.
 */
public class Bill {
    private int billId;
    private int customerId;
    private double previousReading;
    private double currentReading;
    private double unitsConsumed;
    private LocalDate billDate;
    private double amount;
    private String status; // "PAID" or "UNPAID"

    public Bill(int billId, int customerId, double previousReading, double currentReading, double unitsConsumed, LocalDate billDate, double amount, String status) {
        this.billId = billId;
        this.customerId = customerId;
        this.previousReading = previousReading;
        this.currentReading = currentReading;
        this.unitsConsumed = unitsConsumed;
        this.billDate = billDate;
        this.amount = amount;
        this.status = status;
    }

    public Bill(int customerId, double previousReading, double currentReading, double unitsConsumed, LocalDate billDate, double amount, String status) {
        this(0, customerId, previousReading, currentReading, unitsConsumed, billDate, amount, status);
    }

    // Getters and Setters
    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public double getPreviousReading() {
        return previousReading;
    }

    public void setPreviousReading(double previousReading) {
        this.previousReading = previousReading;
    }

    public double getCurrentReading() {
        return currentReading;
    }

    public void setCurrentReading(double currentReading) {
        this.currentReading = currentReading;
    }

    public double getUnitsConsumed() {
        return unitsConsumed;
    }

    public void setUnitsConsumed(double unitsConsumed) {
        this.unitsConsumed = unitsConsumed;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
