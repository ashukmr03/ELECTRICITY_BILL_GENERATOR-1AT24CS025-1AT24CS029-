-- MySQL Schema for Electricity Billing System
CREATE DATABASE IF NOT EXISTS electricity_billing;
USE electricity_billing;

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    meter_number    VARCHAR(20)  NOT NULL UNIQUE,
    customer_type   VARCHAR(20)  NOT NULL,   -- 'DOMESTIC' or 'COMMERCIAL'
    contact_number  VARCHAR(15)
);

-- Create bills table
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

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    role          VARCHAR(20) NOT NULL -- 'ADMIN', 'EMPLOYEE', 'VIEWER'
);

-- Create slab_rates table
CREATE TABLE IF NOT EXISTS slab_rates (
    rate_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_type VARCHAR(20) NOT NULL, -- 'DOMESTIC', 'COMMERCIAL'
    slab_num      INT NOT NULL,          -- 1, 2, 3
    limit_val     DOUBLE NOT NULL,
    rate_val      DOUBLE NOT NULL
);

-- Create company_profile table
CREATE TABLE IF NOT EXISTS company_profile (
    profile_id    INT AUTO_INCREMENT PRIMARY KEY,
    company_name  VARCHAR(100) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    contact_number VARCHAR(15),
    tax_rate      DOUBLE NOT NULL DEFAULT 5.0
);

-- Insert sample data if tables are empty
INSERT INTO customers (customer_id, name, address, meter_number, customer_type, contact_number)
SELECT 1, 'Ravi Kumar', '12 MG Road, Mumbai', 'MTR1001', 'DOMESTIC', '9876543210'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE customer_id = 1);

INSERT INTO customers (customer_id, name, address, meter_number, customer_type, contact_number)
SELECT 2, 'Sharma Textiles', 'Plot 4, Andheri MIDC', 'MTR1002', 'COMMERCIAL', '9123456780'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE customer_id = 2);

INSERT INTO bills (bill_id, customer_id, previous_reading, current_reading, units_consumed, bill_date, amount, status)
SELECT 1, 1, 1200.0, 1350.0, 150.0, '2026-06-01', 825.00, 'UNPAID'
WHERE NOT EXISTS (SELECT 1 FROM bills WHERE bill_id = 1);

INSERT INTO bills (bill_id, customer_id, previous_reading, current_reading, units_consumed, bill_date, amount, status)
SELECT 2, 2, 5400.0, 5700.0, 300.0, '2026-06-01', 3300.00, 'PAID'
WHERE NOT EXISTS (SELECT 1 FROM bills WHERE bill_id = 2);

-- Insert default users
INSERT INTO users (username, password, role)
SELECT 'admin', 'admin123', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, role)
SELECT 'employee', 'emp123', 'EMPLOYEE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'employee');

INSERT INTO users (username, password, role)
SELECT 'viewer', 'view123', 'VIEWER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'viewer');

-- Insert default slab rates for Domestic
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'DOMESTIC', 1, 100.0, 14.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'DOMESTIC' AND slab_num = 1);
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'DOMESTIC', 2, 200.0, 16.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'DOMESTIC' AND slab_num = 2);
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'DOMESTIC', 3, 999999.0, 18.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'DOMESTIC' AND slab_num = 3);

-- Insert default slab rates for Commercial
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'COMMERCIAL', 1, 100.0, 17.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'COMMERCIAL' AND slab_num = 1);
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'COMMERCIAL', 2, 300.0, 19.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'COMMERCIAL' AND slab_num = 2);
INSERT INTO slab_rates (customer_type, slab_num, limit_val, rate_val)
SELECT 'COMMERCIAL', 3, 999999.0, 111.0 WHERE NOT EXISTS (SELECT 1 FROM slab_rates WHERE customer_type = 'COMMERCIAL' AND slab_num = 3);

-- Insert default company profile
INSERT INTO company_profile (company_name, address, contact_number, tax_rate)
SELECT 'ElectriFlow Corp', 'Power Plaza, Sector 62, Noida', '+91 99999 88888', 5.0
WHERE NOT EXISTS (SELECT 1 FROM company_profile WHERE profile_id = 1);
