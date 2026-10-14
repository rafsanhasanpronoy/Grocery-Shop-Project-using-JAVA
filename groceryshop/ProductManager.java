package groceryshop;

import java.sql.*;

public class ProductManager {

    public static void mainMenu(String phone) {
        while (true) {
            System.out.println("\nMain Menu");
            System.out.println("1. Daily Grocery");
            System.out.println("2. Medicine");
            System.out.println("3. Logout");

            int choice = ConsoleInput.readInt("Select an option: ");
            if (choice == -1) {
                return;
            }

            switch (choice) {
                case 1 -> categoryMenu("Daily Grocery", phone);
                case 2 -> categoryMenu("Medicine", phone);
                case 3 -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public static void categoryMenu(String category, String phone) {
        while (true) {
            System.out.printf("%n%s Menu%n", category);
            System.out.println("1. View Products");
            System.out.println("2. Add to Cart");
            System.out.println("3. View Cart");
            System.out.println("4. Checkout");
            System.out.println("5. Back to Main Menu");

            int choice = ConsoleInput.readInt("Select an option: ");
            if (choice == -1) {
                return;
            }

            switch (choice) {
                case 1 -> viewProductsByCategory(category);
                case 2 -> {
                    int productId = ConsoleInput.readInt("Enter the product ID to add to your cart: ");
                    if (productId == -1) {
                        return;
                    }
                    int quantity = ConsoleInput.readInt("Enter the quantity: ");
                    if (quantity == -1) {
                        return;
                    }
                    CartManager.addToCart(category, phone, productId, quantity);
                }
                case 3 -> CartManager.viewCartByCategory(category, phone);
                case 4 -> CartManager.checkoutByCategory(category, phone);
                case 5 -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public static void viewProductsByCategory(String category) {
        String query = "SELECT id, name, price, stock FROM products WHERE category = ? ORDER BY name";

        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, category);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nAvailable Products:");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("ID: %d | Name: %s | Price: %.2f | Stock: %d%n",
                            rs.getInt("id"), rs.getString("name"),
                            rs.getBigDecimal("price").doubleValue(), rs.getInt("stock"));
                }
                if (!found) {
                    System.out.println("No products found in this category.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching products: " + e.getMessage());
        }
    }
}
