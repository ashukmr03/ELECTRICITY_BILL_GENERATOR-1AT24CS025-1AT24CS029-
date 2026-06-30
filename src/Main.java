import database.DBConnection;
import frontend.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Main is the entry point for the application.
 * It initializes FlatLaf, tests the database connection, handles offline fallbacks,
 * and launches the GUI.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Initialize FlatLaf Dark Look and Feel
        try {
            FlatDarkLaf.setup();
            // Customize some FlatLaf look and feel options for a premium feel
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception ex) {
            System.err.println("Warning: FlatLaf Dark Look and Feel failed to initialize. Using default.");
        }

        // 2. Manage Database Connection & Offline Demo Mode
        boolean isConnected = false;
        
        while (!isConnected) {
            // Attempt to connect to local MySQL database
            if (DBConnection.testConnection()) {
                isConnected = true;
                DBConnection.setDemoMode(false);
                System.out.println("MySQL database connection established successfully.");
            } else {
                // Connection failed - offer choices to the operator
                String[] options = {"Run in Demo Mode (Offline / In-Memory)", "Retry Connection", "Exit Application"};
                int choice = JOptionPane.showOptionDialog(
                        null,
                        "Could not connect to the MySQL database at localhost:3306.\n\n" +
                        "Please verify:\n" +
                        "1. MySQL Server is running.\n" +
                        "2. Database 'electricity_billing' has been created.\n" +
                        "3. Credentials in DBConnection.java are correct.\n\n" +
                        "Would you like to run in Demo Mode using in-memory mock storage?",
                        "Database Connection Offline",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                if (choice == 0) { // Run in Demo Mode
                    DBConnection.setDemoMode(true);
                    isConnected = true;
                    System.out.println("Running in Offline Demo Mode. Slabs will calculate in-memory.");
                } else if (choice == 1) { // Retry
                    System.out.println("Retrying database connection...");
                } else { // Exit
                    System.out.println("Exiting application.");
                    System.exit(0);
                }
            }
        }

        // 3. Launch Main Frame on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
