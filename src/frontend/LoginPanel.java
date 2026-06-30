package frontend;

import backend.dao.CustomerDAO;
import backend.dao.UserDAO;
import backend.model.Customer;
import backend.model.User;
import database.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * LoginPanel provides a sleek authentication interface with tabs for:
 * 1. Operators (Admin, Employee, Viewer) using standard credentials.
 * 2. Customers using their unique Meter Number.
 */
public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final UserDAO userDAO = new UserDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    // UI components - Operator Login
    private JTextField txtOperatorUsername;
    private JPasswordField txtOperatorPassword;
    private JButton btnOperatorLogin;

    // UI components - Customer Login
    private JTextField txtCustomerMeter;
    private JButton btnCustomerLogin;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(new EmptyBorder(40, 40, 40, 40));

        // Create main login card container (Glassmorphic look)
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(750, 450));
        card.setBackground(UIManager.getColor("Menu.background"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Split into Left (Banner) and Right (Login Forms)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createBannerPanel(), createFormsPanel());
        splitPane.setDividerLocation(320);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        splitPane.setEnabled(false);

        card.add(splitPane, BorderLayout.CENTER);
        add(card, new GridBagConstraints());
    }

    private JPanel createBannerPanel() {
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw a beautiful blue-to-cyan gradient background
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(41, 128, 185), 0, getHeight(), new Color(26, 188, 156));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        banner.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Emoji Icon
        JLabel lblIcon = new JLabel("⚡", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 64));
        lblIcon.setForeground(Color.WHITE);
        gbc.gridy = 0;
        banner.add(lblIcon, gbc);

        // Logo text
        JLabel lblLogo = new JLabel("ElectriFlow", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblLogo.setForeground(Color.WHITE);
        gbc.gridy = 1;
        banner.add(lblLogo, gbc);

        // Subtitle
        JLabel lblSubtitle = new JLabel("<html><center>Smart Electricity Billing &amp;<br>Management Suite</center></html>", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(new Color(236, 240, 241));
        gbc.gridy = 2;
        banner.add(lblSubtitle, gbc);

        // System status
        String status = DBConnection.isDemoMode() 
                ? "🟠 Running in Offline Demo Mode" 
                : "🟢 Connected to Database";
        JLabel lblStatus = new JLabel(status, SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(Color.WHITE);
        gbc.gridy = 3;
        gbc.insets = new Insets(30, 20, 10, 20);
        banner.add(lblStatus, gbc);

        return banner;
    }

    private JTabbedPane createFormsPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Operator login
        tabbedPane.addTab("👤 Operator Login", createOperatorTab());
        
        // Tab 2: Customer Portal login
        tabbedPane.addTab("🔌 Customer Portal", createCustomerTab());

        return tabbedPane;
    }

    private JPanel createOperatorTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        JLabel lblTitle = new JLabel("Operator Access Gateway");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridy = 0;
        panel.add(lblTitle, gbc);

        JLabel lblSubtitle = new JLabel("Log in to manage customers, readings, and bills.");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(lblSubtitle, gbc);

        gbc.insets = new Insets(4, 0, 4, 0);

        // Username
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 2;
        panel.add(lblUser, gbc);

        txtOperatorUsername = new JTextField("admin"); // Auto-fill default admin for ease of testing
        txtOperatorUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 3;
        panel.add(txtOperatorUsername, gbc);

        // Password
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 4;
        panel.add(lblPass, gbc);

        txtOperatorPassword = new JPasswordField("admin123"); // Auto-fill default password
        txtOperatorPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 5;
        panel.add(txtOperatorPassword, gbc);

        gbc.insets = new Insets(18, 0, 5, 0);

        // Login Button
        btnOperatorLogin = new JButton("Authenticate");
        btnOperatorLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnOperatorLogin.setBackground(new Color(41, 128, 185));
        btnOperatorLogin.setForeground(Color.WHITE);
        btnOperatorLogin.setPreferredSize(new Dimension(0, 38));
        btnOperatorLogin.setFocusPainted(false);
        btnOperatorLogin.addActionListener(e -> performOperatorLogin());
        gbc.gridy = 6;
        panel.add(btnOperatorLogin, gbc);

        return panel;
    }

    private JPanel createCustomerTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        JLabel lblTitle = new JLabel("Customer Portal Access");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridy = 0;
        panel.add(lblTitle, gbc);

        JLabel lblSubtitle = new JLabel("Enter your unique Meter Number to view usage & bills.");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        panel.add(lblSubtitle, gbc);

        gbc.insets = new Insets(4, 0, 4, 0);

        // Meter Number
        JLabel lblMeter = new JLabel("Meter Number (e.g. MTR1001)");
        lblMeter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 2;
        panel.add(lblMeter, gbc);

        txtCustomerMeter = new JTextField("MTR1001");
        txtCustomerMeter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 3;
        panel.add(txtCustomerMeter, gbc);

        gbc.insets = new Insets(30, 0, 5, 0);

        // Login Button
        btnCustomerLogin = new JButton("View Personal Dashboard");
        btnCustomerLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCustomerLogin.setBackground(new Color(39, 174, 96)); // Green
        btnCustomerLogin.setForeground(Color.WHITE);
        btnCustomerLogin.setPreferredSize(new Dimension(0, 38));
        btnCustomerLogin.setFocusPainted(false);
        btnCustomerLogin.addActionListener(e -> performCustomerLogin());
        gbc.gridy = 4;
        panel.add(btnCustomerLogin, gbc);

        return panel;
    }

    private void performOperatorLogin() {
        String username = txtOperatorUsername.getText().trim();
        String password = new String(txtOperatorPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnOperatorLogin.setEnabled(false);
        btnOperatorLogin.setText("Authenticating...");

        // Run authentication in background thread to prevent GUI lockup
        new Thread(() -> {
            try {
                User user = userDAO.authenticate(username, password);
                SwingUtilities.invokeLater(() -> {
                    btnOperatorLogin.setEnabled(true);
                    btnOperatorLogin.setText("Authenticate");
                    if (user != null) {
                        mainFrame.loginOperator(user);
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid credentials. Try again.\n\nHint:\nAdmin: admin / admin123\nEmployee: employee / emp123\nViewer: viewer / view123", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    btnOperatorLogin.setEnabled(true);
                    btnOperatorLogin.setText("Authenticate");
                    JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void performCustomerLogin() {
        String meter = txtCustomerMeter.getText().trim();

        if (meter.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your meter number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnCustomerLogin.setEnabled(false);
        btnCustomerLogin.setText("Searching meter profile...");

        new Thread(() -> {
            try {
                Customer customer = customerDAO.getCustomerByMeter(meter);
                SwingUtilities.invokeLater(() -> {
                    btnCustomerLogin.setEnabled(true);
                    btnCustomerLogin.setText("View Personal Dashboard");
                    if (customer != null) {
                        mainFrame.loginCustomer(customer);
                    } else {
                        JOptionPane.showMessageDialog(this, "Meter Number not found in record.\n\nIf you are a new customer, please register with an operator first.", "Access Denied", JOptionPane.WARNING_MESSAGE);
                    }
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    btnCustomerLogin.setEnabled(true);
                    btnCustomerLogin.setText("View Personal Dashboard");
                    JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
