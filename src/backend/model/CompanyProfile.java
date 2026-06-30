package backend.model;

/**
 * CompanyProfile represents billing information settings.
 */
public class CompanyProfile {
    private int profileId;
    private String companyName;
    private String address;
    private String contactNumber;
    private double taxRate; // In percentage (e.g. 5.0 for 5%)

    public CompanyProfile(int profileId, String companyName, String address, String contactNumber, double taxRate) {
        this.profileId = profileId;
        this.companyName = companyName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.taxRate = taxRate;
    }

    public CompanyProfile(String companyName, String address, String contactNumber, double taxRate) {
        this(0, companyName, address, contactNumber, taxRate);
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }
}
