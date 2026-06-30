package backend.model;

/**
 * Customer is an abstract base class representing a consumer.
 * It encapsulates basic details like ID, name, address, meter number, and contact info,
 * and defines an abstract method for billing slab calculations.
 */
public abstract class Customer {
    private int customerId;
    private String name;
    private String address;
    private String meterNumber;
    private String customerType; // "DOMESTIC" or "COMMERCIAL"
    private String contactNumber;

    public Customer(int customerId, String name, String address, String meterNumber, String customerType, String contactNumber) {
        this.customerId = customerId;
        this.name = name;
        this.address = address;
        this.meterNumber = meterNumber;
        this.customerType = customerType;
        this.contactNumber = contactNumber;
    }

    public Customer(String name, String address, String meterNumber, String customerType, String contactNumber) {
        this(0, name, address, meterNumber, customerType, contactNumber);
    }

    // Getters and Setters
    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    /**
     * Calculates the bill amount for the given units consumed based on slab rates.
     * This is implemented by subclasses to demonstrate polymorphism.
     */
    public abstract double calculateBill(double units);
}
