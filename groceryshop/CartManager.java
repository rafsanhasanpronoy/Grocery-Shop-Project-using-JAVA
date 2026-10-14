package groceryshop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartManager {

    private static boolean isValidCategory(String category) {
        return "Daily Grocery".equals(category) || "Medicine".equals(category);
    }

    public static void addToCart(String category, String phone, int productId, int quantity) {
        if (!isValidCategory(category)) {
            System.out.println("Invalid product category.");
            return;
        }
        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return;
        }

        Connection conn = DatabaseManager.getConnection();
        boolean oldAutoCommit = true;

        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            int stock;
            String actualCategory;
            String productQuery = "SELECT stock, category FROM products WHERE id = ? FOR UPDATE";
            try (PreparedStatement stmt = conn.prepareStatement(productQuery)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        System.out.println("Product not found.");
                        return;
                    }
                    stock = rs.getInt("stock");
                    actualCategory = rs.getString("category");
                }
            }

            if (!category.equals(actualCategory)) {
                conn.rollback();
                System.out.println("That product does not belong to the selected category.");
                return;
            }

            int existingQuantity = 0;
            String existingQuery = "SELECT quantity FROM cart WHERE phone = ? AND product_id = ? AND category = ? FOR UPDATE";
            try (PreparedStatement stmt = conn.prepareStatement(existingQuery)) {
                stmt.setString(1, phone);
                stmt.setInt(2, productId);
                stmt.setString(3, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        existingQuantity = rs.getInt("quantity");
                    }
                }
            }

            if (existingQuantity < 0 || existingQuantity > stock || quantity > stock - existingQuantity) {
                conn.rollback();
                System.out.println("Insufficient stock. Available to add: " + Math.max(0, stock - existingQuantity));
                return;
            }

            String insertQuery = "INSERT INTO cart (phone, product_id, category, quantity) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE quantity = quantity + ?";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, phone);
                stmt.setInt(2, productId);
                stmt.setString(3, category);
                stmt.setInt(4, quantity);
                stmt.setInt(5, quantity);
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Product added to cart.");
        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.out.println("Error adding to cart: " + e.getMessage());
        } finally {
            restoreAutoCommit(conn, oldAutoCommit);
        }
    }

    public static void viewCartByCategory(String category, String phone) {
        if (!isValidCategory(category)) {
            System.out.println("Invalid product category.");
            return;
        }

        String query = "SELECT p.name, c.quantity, p.price, (c.quantity * p.price) AS total "
                + "FROM cart c JOIN products p ON c.product_id = p.id "
                + "WHERE c.phone = ? AND c.category = ? ORDER BY p.name";

        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, phone);
            stmt.setString(2, category);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nYour Cart:");
                double grandTotal = 0;
                boolean hasItems = false;

                while (rs.next()) {
                    hasItems = true;
                    double total = rs.getBigDecimal("total").doubleValue();
                    System.out.printf("Product: %s | Quantity: %d | Price: %.2f | Total: %.2f%n",
                            rs.getString("name"), rs.getInt("quantity"),
                            rs.getBigDecimal("price").doubleValue(), total);
                    grandTotal += total;
                }

                if (!hasItems) {
                    System.out.println("Your cart is empty for this category.");
                }
                System.out.printf("%nGrand Total: %.2f%n", grandTotal);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching cart: " + e.getMessage());
        }
    }

    public static void checkoutByCategory(String category, String phone) {
        if (!isValidCategory(category)) {
            System.out.println("Invalid product category.");
            return;
        }

        Connection conn = DatabaseManager.getConnection();
        boolean oldAutoCommit = true;

        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            List<CartItem> items = new ArrayList<>();
            String cartQuery = "SELECT c.product_id, c.quantity, p.category "
                    + "FROM cart c JOIN products p ON p.id = c.product_id "
                    + "WHERE c.phone = ? AND c.category = ? ORDER BY c.product_id FOR UPDATE";

            try (PreparedStatement stmt = conn.prepareStatement(cartQuery)) {
                stmt.setString(1, phone);
                stmt.setString(2, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(new CartItem(rs.getInt("product_id"),
                                rs.getInt("quantity"), rs.getString("category")));
                    }
                }
            }

            if (items.isEmpty()) {
                conn.rollback();
                System.out.println("Your cart is empty for this category.");
                return;
            }

            String stockUpdate = "UPDATE products SET stock = stock - ? "
                    + "WHERE id = ? AND category = ? AND stock >= ?";
            try (PreparedStatement stmt = conn.prepareStatement(stockUpdate)) {
                for (CartItem item : items) {
                    if (item.quantity <= 0 || !category.equals(item.category)) {
                        throw new SQLException("Invalid cart quantity or product category. Checkout cancelled.");
                    }

                    stmt.setInt(1, item.quantity);
                    stmt.setInt(2, item.productId);
                    stmt.setString(3, category);
                    stmt.setInt(4, item.quantity);

                    if (stmt.executeUpdate() != 1) {
                        throw new SQLException("Insufficient stock for product ID " + item.productId
                                + ". No items were purchased.");
                    }
                }
            }

            String clearCartQuery = "DELETE FROM cart WHERE phone = ? AND category = ?";
            try (PreparedStatement stmt = conn.prepareStatement(clearCartQuery)) {
                stmt.setString(1, phone);
                stmt.setString(2, category);
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Checkout successful. Thank you for shopping!");
        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.out.println("Checkout failed. No changes were saved: " + e.getMessage());
        } finally {
            restoreAutoCommit(conn, oldAutoCommit);
        }
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.out.println("Rollback failed: " + e.getMessage());
        }
    }

    private static void restoreAutoCommit(Connection conn, boolean oldAutoCommit) {
        try {
            if (!conn.isClosed()) {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            System.out.println("Could not restore database transaction settings: " + e.getMessage());
        }
    }

    private static class CartItem {
        private final int productId;
        private final int quantity;
        private final String category;

        private CartItem(int productId, int quantity, String category) {
            this.productId = productId;
            this.quantity = quantity;
            this.category = category;
        }
    }
}
