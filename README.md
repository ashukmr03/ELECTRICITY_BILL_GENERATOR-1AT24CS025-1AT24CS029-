# ⚡ ElectriFlow — Advanced Electricity Bill Generator

A modular, role-based desktop application for generating and managing electricity bills, built with **Java Swing**, **JDBC**, and **MySQL**. ElectriFlow provides separate dashboards for utility operators (Admin / Employee / Viewer) and consumers, with slab-based billing, analytics charts, and PDF/HTML invoice exports.

---

## Table of Contents

- [Purpose](#purpose)
- [Features](#features)
- [Tech Stack & Dependencies](#tech-stack--dependencies)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [Usage](#usage)
- [Default Login Credentials](#default-login-credentials)
- [Demo Mode (No Database)](#demo-mode-no-database)
- [Contributors](#contributors)

---

## Purpose

ElectriFlow digitizes the end-to-end workflow of an electricity utility provider: registering customers, recording meter readings, calculating slab-based bills, collecting payments, and generating printable invoices. It was built as an academic project to demonstrate layered application design (GUI / business logic / DAO / database), role-based access control, and Java desktop UI development.

## Features

- **Multi-role authentication**: Admin, Employee, and Viewer operator portals, plus a separate Customer portal accessed via meter number.
- **Customer dashboard**: outstanding dues, total paid, cumulative consumption, profile editing, and a simulated payment gateway (Card / UPI / Net Banking).
- **Usage analytics**: custom-drawn line, bar, and donut charts for bill trends, kWh consumption, and slab-wise usage split.
- **Admin control room**: KPI stats (customers, revenue, overdue accounts), configurable slab rates and tax rates, JSON backup/restore, and CSV export.
- **Slab-based billing engine**: separate calculation logic for Domestic and Commercial customers.
- **Invoice generation**: native print-to-PDF support and a standalone HTML invoice exporter.
- **Demo Mode**: run the full app without a MySQL connection using in-memory mock data.

## Tech Stack & Dependencies

| Component | Technology |
|---|---|
| Language | Java (JDK 17+ recommended) |
| GUI | Java Swing + AWT |
| Look & Feel | [FlatLaf](https://www.formdev.com/flatlaf/) `3.4.1` |
| Database | MySQL 8.x |
| DB Connectivity | MySQL Connector/J `8.3.0` |
| Build/Run scripts | PowerShell |

Required JARs are expected in the `lib/` folder:
- `flatlaf-3.4.1.jar`
- `mysql-connector-j-8.3.0.jar`

If `lib/` is empty, run the included `download_libs.ps1` script (PowerShell) to fetch these dependencies automatically.

## Project Structure

```
ElectricityBillGenerator/
│
├── src/
│   ├── database/
│   │   ├── DBConnection.java          # Auto-verifies and seeds MySQL tables
│   │   └── schema.sql                 # SQL schema reference
│   │
│   ├── backend/
│   │   ├── model/
│   │   │   ├── Customer.java          # Abstract base customer class
│   │   │   ├── DomesticCustomer.java  # Slab-based bill calculation
│   │   │   ├── CommercialCustomer.java# Slab-based bill calculation
│   │   │   ├── Bill.java
│   │   │   ├── User.java
│   │   │   ├── SlabRate.java
│   │   │   └── CompanyProfile.java
│   │   │
│   │   └── dao/
│   │       ├── CustomerDAO.java
│   │       ├── BillDAO.java
│   │       ├── UserDAO.java
│   │       └── SettingsDAO.java
│   │
│   ├── frontend/
│   │   ├── components/
│   │   │   ├── CustomChart.java
│   │   │   └── InvoicePrintTemplate.java
│   │   │
│   │   ├── MainFrame.java
│   │   ├── LoginPanel.java
│   │   ├── AdminDashboardPanel.java
│   │   ├── CustomerDashboardPanel.java
│   │   ├── CustomerPanel.java
│   │   ├── ReadingPanel.java
│   │   └── HistoryPanel.java
│   │
│   └── Main.java                      # Entry point
│
├── lib/                                # Third-party JARs
├── bin/                                # Compiled output
├── download_libs.ps1                   # Fetches required JARs
└── README.md
```

## Setup & Installation

### Prerequisites

- **JDK 17+** installed and on your `PATH` (`javac -version` to verify)
- **MySQL Server 8.x** running locally (optional — skip if using Demo Mode)
- **PowerShell** (Windows) — or adapt the equivalent commands for `bash`/macOS/Linux
- Git

### 1. Clone the repository

```powershell
git clone https://github.com/Arghaneel/ELECTRICITY_BILL_GENERATOR-1AT24CS025-1AT24CS029-.git
cd ELECTRICITY_BILL_GENERATOR-1AT24CS025-1AT24CS029-
```

### 2. Fetch dependencies

If the `lib/` folder doesn't already contain the required JARs, run:

```powershell
./download_libs.ps1
```

This downloads `flatlaf-3.4.1.jar` and `mysql-connector-j-8.3.0.jar` into `lib/`.

### 3. Set up the database (skip if using Demo Mode)

- Start your local MySQL server.
- Update the connection credentials (host, username, password) in `src/database/DBConnection.java` to match your local MySQL setup.
- No manual schema creation needed — `DBConnection.java` automatically creates the `electricity_billing` database and tables on first run.

### 4. Compile

```powershell
$files = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -cp "lib/*" -d bin $files
```

### 5. Run

```powershell
java -cp "bin;lib/*" Main
```

> On macOS/Linux, replace the `;` classpath separator with `:`:
> `java -cp "bin:lib/*" Main`

## Usage

1. Launch the app (see [Run](#5-run) above). A login screen with two portals appears: **Operator Access** and **Customer Portal**.
2. **Operators** log in with one of the role-based credentials below to reach the Admin, Employee, or Viewer dashboard.
3. **Customers** enter their unique meter number (e.g. `MTR1001`) to open their personal dashboard.
4. From the Employee/Admin dashboard you can register customers, enter meter readings, generate slab-based invoices, and view billing history.
5. From the Customer dashboard, consumers can view dues, pay bills via the simulated payment gateway, edit their profile, and view consumption analytics.
6. Invoices can be printed (Print-to-PDF) or exported as a standalone HTML file from the History/Invoice screens.

## Default Login Credentials

| Role | Username | Password | Access |
|---|---|---|---|
| Admin | `admin` | `admin123` | Full access: slab rates, company settings, reports, backups |
| Employee | `employee` | `emp123` | Register customers, enter readings, generate invoices |
| Viewer | `viewer` | `view123` | Read-only: search, view invoices and analytics |

> ⚠️ These are default demo credentials intended for local testing only. Change them before any real-world or production use.

## Demo Mode (No Database)

If MySQL is not available or not configured, select **"Run in Demo Mode"** on the startup screen. The app will run entirely on in-memory mock data, letting you explore all features without a database connection. Data will not persist between runs in this mode.

## Contributors

- 1AT24CS025
- 1AT24CS029
