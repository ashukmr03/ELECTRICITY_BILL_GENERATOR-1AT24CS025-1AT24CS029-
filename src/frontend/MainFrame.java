package frontend;

import backend.dao.BillDAO;
import backend.dao.CustomerDAO;
import backend.model.Customer;
import backend.model.User;
import database.DBConnection;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * MainFrame is the main container of the application.
 * Manages side bar navigation, theme toggles, role-based authorization rules, 
 * and routing using CardLayout.
 */
public class MainFrame extends JFrame {
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BillDAO billDAO = new BillDAO();

    private CardLayout cardLayout;
    private JPanel cardPanel;

    // View Panels
    private LoginPanel loginPanel;
    private AdminDashboardPanel adminPanel;
    private CustomerDashboardPanel customerDashboardPanel;
    private CustomerPanel customerPanel;
    private ReadingPanel readingPanel;
    private BillPanel billPanel;
    private HistoryPanel historyPanel;

    // Sidebar navigation and buttons
    private JPanel sidebarPanel;
    private JButton btnAdminDashboard;
    private JButton btnCustomers;
    private JButton btnReadings;
    private JButton btnHistory;
    private JButton btnThemeToggle;
    private JLabel lblUserRole;

    // State
    private User currentUser;
    private Customer currentCustomer;
    private boolean isDarkTheme = true;

    public MainFrame() {
        setTitle("ElectriFlow - Advanced Electricity Billing Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialize child panels
        loginPanel = new LoginPanel(this);
        customerPanel = new CustomerPanel(this, customerDAO);
        readingPanel = new ReadingPanel(this, customerDAO, billDAO);
        billPanel = new BillPanel(this, billDAO);
        historyPanel = new HistoryPanel(this, customerDAO, billDAO);
        adminPanel = new AdminDashboardPanel(this, customerDAO, billDAO);
        customerDashboardPanel = new CustomerDashboardPanel(this, billDAO);

        // Sidebar Navigation
        sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.WEST);
        sidebarPanel.setVisible(false); // Hidden during login

        // Main Card Panel Container
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        
        cardPanel.add(loginPanel, "Login");
        cardPanel.add(adminPanel, "Admin");
        cardPanel.add(customerDashboardPanel, "Customer");
        cardPanel.add(customerPanel, "Customers");
        cardPanel.add(readingPanel, "Readings");
        cardPanel.add(billPanel, "Bill");
        cardPanel.add(historyPanel, "History");

        add(cardPanel, BorderLayout.CENTER);

        // Default view
        showLoginPanel();
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBackground(UIManager.getColor("Menu.background"));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")));

        // Title Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("⚡ ElectriFlow");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(41, 128, 185));
        header.add(lblTitle);
        sidebar.add(header);

        sidebar.add(Box.createVerticalStrut(10));

        // Logged-in role indicator
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        rolePanel.setOpaque(false);
        lblUserRole = new JLabel("Role: Unknown");
        lblUserRole.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUserRole.setForeground(UIManager.getColor("Label.disabledForeground"));
        rolePanel.add(lblUserRole);
        sidebar.add(rolePanel);

        sidebar.add(Box.createVerticalStrut(15));

        // Navigation Buttons
        btnAdminDashboard = createNavButton("📈  Admin Panel", "Admin");
        btnCustomers = createNavButton("👥  Customers", "Customers");
        btnReadings = createNavButton("🔌  New Meter Reading", "Readings");
        btnHistory = createNavButton("📜  Invoice History", "History");

        sidebar.add(btnAdminDashboard);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnCustomers);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnReadings);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnHistory);

        sidebar.add(Box.createVerticalGlue());

        // Dark/Light Theme Switcher & Logout
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(10, 15, 15, 15));

        btnThemeToggle = new JButton("🌙 Dark Mode");
        btnThemeToggle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnThemeToggle.setMaximumSize(new Dimension(210, 36));
        btnThemeToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnThemeToggle.setFocusPainted(false);
        btnThemeToggle.addActionListener(e -> toggleTheme());
        footerPanel.add(btnThemeToggle);

        footerPanel.add(Box.createVerticalStrut(8));

        JButton btnLogout = new JButton("🔓  Log Out");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setBackground(new Color(192, 57, 43));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setMaximumSize(new Dimension(210, 36));
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> logout());
        footerPanel.add(btnLogout);

        sidebar.add(footerPanel);

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setPreferredSize(new Dimension(210, 42));
        btn.setMaximumSize(new Dimension(210, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(0, 15, 0, 0));
        btn.setOpaque(true);

        btn.addActionListener(e -> {
            cardLayout.show(cardPanel, cardName);
            highlightNavButton(btn);
            if ("Customers".equals(cardName)) {
                customerPanel.refreshData();
            } else if ("Readings".equals(cardName)) {
                readingPanel.refreshData();
            } else if ("History".equals(cardName)) {
                historyPanel.refreshData();
            } else if ("Admin".equals(cardName)) {
                adminPanel.refreshData();
            }
        });

        return btn;
    }

    private void highlightNavButton(JButton activeBtn) {
        JButton[] buttons = {btnAdminDashboard, btnCustomers, btnReadings, btnHistory};
        for (JButton btn : buttons) {
            if (btn == activeBtn) {
                btn.setBackground(UIManager.getColor("List.selectionBackground"));
                btn.setForeground(UIManager.getColor("List.selectionForeground"));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            } else {
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(UIManager.getColor("Button.foreground"));
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            }
        }
    }

    public void loginOperator(User user) {
        this.currentUser = user;
        this.currentCustomer = null;
        lblUserRole.setText("Access: " + user.getRole());
        sidebarPanel.setVisible(true);

        // Apply role permissions
        applyRoleAuthorizations();

        // Redirect based on role
        if ("ADMIN".equals(user.getRole())) {
            showAdminDashboard();
        } else if ("EMPLOYEE".equals(user.getRole())) {
            showCustomerPanel();
        } else { // VIEWER
            showHistoryPanel();
        }
    }

    public void loginCustomer(Customer customer) {
        this.currentCustomer = customer;
        this.currentUser = null;
        sidebarPanel.setVisible(false); // Hide sidebar entirely for clean portal

        customerDashboardPanel.setupCustomer(customer);
        cardLayout.show(cardPanel, "Customer");
    }

    public void logout() {
        this.currentUser = null;
        this.currentCustomer = null;
        sidebarPanel.setVisible(false);
        showLoginPanel();
    }

    private void applyRoleAuthorizations() {
        if (currentUser == null) return;

        String role = currentUser.getRole();
        if ("ADMIN".equals(role)) {
            btnAdminDashboard.setVisible(true);
            btnCustomers.setVisible(true);
            btnReadings.setVisible(true);
            btnHistory.setVisible(true);
        } else if ("EMPLOYEE".equals(role)) {
            btnAdminDashboard.setVisible(false); // Hide admin settings
            btnCustomers.setVisible(true);
            btnReadings.setVisible(true);
            btnHistory.setVisible(true);
        } else { // VIEWER
            btnAdminDashboard.setVisible(false);
            btnCustomers.setVisible(true);
            btnReadings.setVisible(false); // Hide meters reading inputs
            btnHistory.setVisible(true);
        }
    }

    private void toggleTheme() {
        try {
            isDarkTheme = !isDarkTheme;
            if (isDarkTheme) {
                FlatDarkLaf.setup();
                btnThemeToggle.setText("🌙 Dark Mode");
            } else {
                FlatLightLaf.setup();
                btnThemeToggle.setText("☀️ Light Mode");
            }
            FlatLaf.updateUI();
            
            // Refresh custom charts components
            adminPanel.refreshData();
            if (currentCustomer != null) {
                customerDashboardPanel.refreshData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getUserRole() {
        return currentUser != null ? currentUser.getRole() : "CUSTOMER";
    }

    public void showLoginPanel() {
        cardLayout.show(cardPanel, "Login");
    }

    public void showAdminDashboard() {
        cardLayout.show(cardPanel, "Admin");
        highlightNavButton(btnAdminDashboard);
        adminPanel.refreshData();
    }

    public void showCustomerPanel() {
        cardLayout.show(cardPanel, "Customers");
        highlightNavButton(btnCustomers);
        customerPanel.refreshData();
    }

    public void showReadingPanel() {
        cardLayout.show(cardPanel, "Readings");
        highlightNavButton(btnReadings);
        readingPanel.refreshData();
    }

    public void showBillPanel(Customer customer, double prev, double curr) {
        billPanel.setupBill(customer, prev, curr);
        cardLayout.show(cardPanel, "Bill");
        highlightNavButton(null); // Clear sidebar highlights
    }

    public void showHistoryPanel() {
        cardLayout.show(cardPanel, "History");
        highlightNavButton(btnHistory);
        historyPanel.refreshData();
    }

    public void notifyDataChanged() {
        customerPanel.refreshData();
        readingPanel.refreshData();
        historyPanel.refreshData();
        adminPanel.refreshData();
        if (currentCustomer != null) {
            customerDashboardPanel.refreshData();
        }
    }
}
