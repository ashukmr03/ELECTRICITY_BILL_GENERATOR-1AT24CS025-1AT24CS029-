# ElectriFlow — Advanced Electricity Bill Generator

ElectriFlow is a modular, high-fidelity desktop application built using **Java Swing** for the user interface, **JDBC** for persistence, and **MySQL** or **In-Memory Mock Stores** for storage. It provides a secure, role-based, dynamic dashboard interface for both utility operators and consumers.

---

## 1. Key Features & Workflows

### 🛡️ Secure Multi-Role Authentication Gate
The application boots into a secure login gate offering two portals:
1. **Operator Access**: Log in with credentials to access administrative and billing tools:
   - **Admin** (`admin` / `admin123`): Full access, including Slab Rate adjustments, Company settings, dynamic reports, password changes, and database backup/restore operations.
   - **Employee** (`employee` / `emp123`): Can register new customers, enter readings, calculate slab-based invoices, view history, and export files.
   - **Viewer** (`viewer` / `view123`): Read-only administrative access. Can search customer files, examine invoices, and view analytical graphs, but cannot execute edits or record billing entries.
2. **Customer Portal**: Direct access using a consumer's unique **Meter Number** (e.g. `MTR1001`), loading their personalized account console.

### 👤 Customer Dashboard
Consumers can manage their utility profile directly:
- **Financial Cards**: Tracks **Outstanding Dues**, **Total Paid Amounts**, and **Cumulative Consumption** in real time.
- **✏️ Edit Profile**: Customers can modify their contact mobile number and billing address. Edits update the system registry instantly and log a notification in the Admin console.
- **💳 Simulated Payment Gateway**: Customers can select unpaid bills and pay them instantly using Card, UPI, or Net Banking simulators, marking the bill as `PAID`.
- **📊 visual Usage Analytics**: Displays three custom vector charts:
  1. **Bill Amount Variations** (Line Chart showing monthly dues).
  2. **kWh Consumption Trend** (Bar Chart showing units consumed).
  3. **Slab Consumption Split** (Donut Chart analyzing usage divisions).

### ⚡ Admin Control Room & KPI Stats
- **Stat Panels**: Monitors aggregate metrics (Customers count, total bills, total revenue paid/unpaid, monthly earnings, overdue accounts).
- **Control Settings**: Admin can modify domestic/commercial slab levels and tax rates, updating invoice generation calculations dynamically.
- **Data backups**: Backup database contents to a self-contained JSON snapshot and restore previous states seamlessly. Also supports CSV exports.

### 🖨️ HD PDF Printing & HTML Exports
- Built-in **AWT Printing Template** maps to the native printer selection (supporting direct "Print to PDF").
- Interactive **HTML Exporter** formats a professional utility bill receipt saved directly to disk.

---

## 2. Updated Database Schema (MySQL)

ElectriFlow manages 5 normalized, related tables. On connection, the system automatically verifies and runs database DDLs to construct missing tables:

```sql
CREATE DATABASE IF NOT EXISTS electricity_billing;
USE electricity_billing;

-- 1. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    meter_number    VARCHAR(20)  NOT NULL UNIQUE,
    customer_type   VARCHAR(20)  NOT NULL,   -- 'DOMESTIC' or 'COMMERCIAL'
    contact_number  VARCHAR(15)
);

-- 2. Bills Ledger Table
CREATE TABLE IF NOT EXISTS bills (
    bill_id           INT AUTO_INCREMENT PRIMARY KEY,
    customer_id       INT NOT NULL,
    previous_reading  DOUBLE NOT NULL,
    current_reading   DOUBLE NOT NULL,
    units_consumed    DOUBLE NOT NULL,
    bill_date         DATE NOT NULL,
    amount            DOUBLE NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'UNPAID',  -- 'PAID' or 'UNPAID'
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- 3. Users Authentication Table
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    role          VARCHAR(20) NOT NULL -- 'ADMIN', 'EMPLOYEE', 'VIEWER'
);

-- 4. Dynamic Slab Rates Table
CREATE TABLE IF NOT EXISTS slab_rates (
    rate_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_type VARCHAR(20) NOT NULL, -- 'DOMESTIC', 'COMMERCIAL'
    slab_num      INT NOT NULL,          -- 1, 2, 3
    limit_val     DOUBLE NOT NULL,
    rate_val      DOUBLE NOT NULL
);

-- 5. Company Invoice Profile Table
CREATE TABLE IF NOT EXISTS company_profile (
    profile_id    INT AUTO_INCREMENT PRIMARY KEY,
    company_name  VARCHAR(100) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    contact_number VARCHAR(15),
    tax_rate      DOUBLE NOT NULL DEFAULT 5.0
);
```

---

## 3. Class Structure & Modularity

The codebase separates GUI forms from backend business rules and persistence DAOs:

```
ElectricityBillGenerator/
│
├── src/
│   ├── database/
│   │   ├── DBConnection.java          # Auto-verifies and seeds MySQL tables
│   │   └── schema.sql                 # SQL schema statements reference
│   │
│   ├── backend/
│   │   ├── model/
│   │   │   ├── Customer.java          # abstract base customer class
│   │   │   ├── DomesticCustomer.java  # overrides calculateBill (Slabs)
│   │   │   ├── CommercialCustomer.java# overrides calculateBill (Slabs)
│   │   │   ├── Bill.java              # Bill ledger model
│   │   │   ├── User.java              # Authentication profile model
│   │   │   ├── SlabRate.java          # Rate model
│   │   │   └── CompanyProfile.java    # Invoice header info model
│   │   │
│   │   └── dao/
│   │       ├── CustomerDAO.java       # CRUD operations for customer table
│   │       ├── BillDAO.java           # CRUD operations for bills table
│   │       ├── UserDAO.java           # Security logins & credentials
│   │       └── SettingsDAO.java       # Dynamic rates & profiles registry
│   │
│   ├── frontend/
│   │   ├── components/
│   │   │   ├── CustomChart.java       # Anti-aliased 2D charts (Line, Bar, Donut)
│   │   │   └── InvoicePrintTemplate.java# Printable AWT template for PDF outputs
│   │   │
│   │   ├── MainFrame.java             # Coordinator holding CardLayout & themes
│   │   ├── LoginPanel.java            # Multi-portal login card
│   │   ├── AdminDashboardPanel.java   # Admin console, stats, settings, logs
│   │   ├── CustomerDashboardPanel.java# Customer portal, payment, edits, charts
│   │   ├── CustomerPanel.java         # Registering & listing customers
│   │   ├── ReadingPanel.java          # Inputting meter metrics
│   │   └── HistoryPanel.java          # Full invoice records & ledgers
│   │
│   └── Main.java                      # Launches Look & Feel, checks connection
│
├── lib/
│   ├── flatlaf-3.4.1.jar              # Look & Feel themes
│   └── mysql-connector-j-8.3.0.jar    # MySQL JDBC Driver
│
└── README.md
```

---

## 4. Compilation & Execution

### Compile:
Run in PowerShell (compiles all classes to `bin/`):
```powershell
$files = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -cp "lib/*" -d bin $files
```

### Run:
```powershell
java -cp "bin;lib/*" Main
```

*(If the local MySQL database is offline, select "Run in Demo Mode" at startup to launch the app using mock collections).*
