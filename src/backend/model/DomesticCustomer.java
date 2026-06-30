package backend.model;

/**
 * DomesticCustomer implements billing calculations for domestic users.
 * Slab rates:
 * - 0 to 100 units @ 14.0 / unit
 * - 101 to 200 units @ 16.0 / unit
 * - More than 200 units @ 18.0 / unit
 */
public class DomesticCustomer extends Customer {

    public DomesticCustomer(int customerId, String name, String address, String meterNumber, String contactNumber) {
        super(customerId, name, address, meterNumber, "DOMESTIC", contactNumber);
    }

    public DomesticCustomer(String name, String address, String meterNumber, String contactNumber) {
        super(name, address, meterNumber, "DOMESTIC", contactNumber);
    }

    @Override
    public double calculateBill(double units) {
        if (units <= 0) {
            return 0.0;
        }
        
        double s1Limit = 100.0, s1Rate = 14.0;
        double s2Limit = 200.0, s2Rate = 16.0;
        double s3Rate = 18.0;

        try {
            backend.dao.SettingsDAO settingsDAO = new backend.dao.SettingsDAO();
            java.util.List<backend.model.SlabRate> rates = settingsDAO.getSlabRates("DOMESTIC");
            for (backend.model.SlabRate r : rates) {
                if (r.getSlabNum() == 1) {
                    s1Limit = r.getLimitVal();
                    s1Rate = r.getRateVal();
                } else if (r.getSlabNum() == 2) {
                    s2Limit = r.getLimitVal();
                    s2Rate = r.getRateVal();
                } else if (r.getSlabNum() == 3) {
                    s3Rate = r.getRateVal();
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching domestic slab rates, using defaults: " + e.getMessage());
        }

        double billAmount = 0.0;
        if (units <= s1Limit) {
            billAmount = units * s1Rate;
        } else if (units <= s2Limit) {
            billAmount = (s1Limit * s1Rate) + ((units - s1Limit) * s2Rate);
        } else {
            billAmount = (s1Limit * s1Rate) + ((s2Limit - s1Limit) * s2Rate) + ((units - s2Limit) * s3Rate);
        }
        return billAmount;
    }
}
