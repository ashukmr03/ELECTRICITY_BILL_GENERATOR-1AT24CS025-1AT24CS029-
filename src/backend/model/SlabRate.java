package backend.model;

/**
 * SlabRate represents a billing tier rate setting.
 */
public class SlabRate {
    private int rateId;
    private String customerType; // "DOMESTIC" or "COMMERCIAL"
    private int slabNum;          // 1, 2, or 3
    private double limitVal;      // Maximum units for this slab
    private double rateVal;       // Cost per unit in INR

    public SlabRate(int rateId, String customerType, int slabNum, double limitVal, double rateVal) {
        this.rateId = rateId;
        this.customerType = customerType.toUpperCase();
        this.slabNum = slabNum;
        this.limitVal = limitVal;
        this.rateVal = rateVal;
    }

    public SlabRate(String customerType, int slabNum, double limitVal, double rateVal) {
        this(0, customerType, slabNum, limitVal, rateVal);
    }

    public int getRateId() {
        return rateId;
    }

    public void setRateId(int rateId) {
        this.rateId = rateId;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType.toUpperCase();
    }

    public int getSlabNum() {
        return slabNum;
    }

    public void setSlabNum(int slabNum) {
        this.slabNum = slabNum;
    }

    public double getLimitVal() {
        return limitVal;
    }

    public void setLimitVal(double limitVal) {
        this.limitVal = limitVal;
    }

    public double getRateVal() {
        return rateVal;
    }

    public void setRateVal(double rateVal) {
        this.rateVal = rateVal;
    }
}
