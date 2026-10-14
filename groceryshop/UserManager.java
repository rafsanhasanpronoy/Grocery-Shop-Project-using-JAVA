package groceryshop;

import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.Scanner;

public class UserManager {

    private static final Scanner input = ConsoleInput.scanner();

    private static boolean isValidBangladeshiPhone(String phone) {
        return phone.matches("01[3-9]\\d{8}");
    }

    public static void signup() {
        System.out.print("Enter your phone number: ");
        String phone = input.nextLine().trim();

        if (!isValidBangladeshiPhone(phone)) {
            System.out.println("Invalid phone number. Please enter a valid Bangladeshi phone number.");
            return;
        }

        System.out.print("Enter your name: ");
        String name = input.nextLine().trim();
        System.out.print("Enter your address: ");
        String address = input.nextLine().trim();
        System.out.print("Enter your PIN (4-10 digits): ");
        String pin = input.nextLine().trim();

        if (name.isEmpty() || address.isEmpty()) {
            System.out.println("Name and address cannot be empty.");
            return;
        }
        if (!pin.matches("\\d{4,10}")) {
            System.out.println("PIN must contain 4 to 10 digits.");
            return;
        }

        String query = "INSERT INTO users (phone, name, address, pin) VALUES (?, ?, ?, ?)";
        try {
            String pinHash = PinSecurity.hash(pin);
            try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, phone);
                stmt.setString(2, name);
                stmt.setString(3, address);
                stmt.setString(4, pinHash);
                stmt.executeUpdate();
            }
            System.out.println("Signup successful!");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Signup failed: an account with this phone number may already exist.");
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Signup failed: " + e.getMessage());
        }
    }

    public static void login() {
        System.out.print("Enter your phone number: ");
        String phone = input.nextLine().trim();

        if (!isValidBangladeshiPhone(phone)) {
            System.out.println("Invalid phone number. Please enter a valid Bangladeshi phone number.");
            return;
        }

        System.out.print("Enter your PIN: ");
        String suppliedPin = input.nextLine().trim();

        String query = "SELECT name, pin FROM users WHERE phone = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, phone);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Invalid credentials.");
                    return;
                }

                String storedPin = rs.getString("pin");
                boolean valid;
                if (PinSecurity.isHashed(storedPin)) {
                    valid = PinSecurity.verify(suppliedPin, storedPin);
                } else {
                    // Backward compatibility: verify an old plaintext PIN once, then upgrade it.
                    valid = storedPin != null && storedPin.equals(suppliedPin);
                    if (valid) {
                        String upgradedHash = PinSecurity.hash(suppliedPin);
                        try (PreparedStatement update = DatabaseManager.getConnection()
                                .prepareStatement("UPDATE users SET pin = ? WHERE phone = ?")) {
                            update.setString(1, upgradedHash);
                            update.setString(2, phone);
                            update.executeUpdate();
                        }
                    }
                }

                if (!valid) {
                    System.out.println("Invalid credentials.");
                    return;
                }

                System.out.println("Login successful! Welcome, " + rs.getString("name") + ".");
            }
            ProductManager.mainMenu(phone);
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }
}
