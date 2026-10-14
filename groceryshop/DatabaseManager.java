package groceryshop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static Connection connection;

    private DatabaseManager() {
    }

    public static boolean connectDatabase() {
        String url = System.getenv().getOrDefault(
                "GROCERY_DB_URL", "jdbc:mysql://localhost:3306/grocery_shop");
        String user = System.getenv().getOrDefault("GROCERY_DB_USER", "root");
        String password = System.getenv().getOrDefault("GROCERY_DB_PASSWORD", "");

        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection successful.");
            return true;
        } catch (SQLException e) {
            System.out.println("Database connection failed. Check MySQL is running and database settings are correct.");
            System.err.println("Details: " + e.getMessage());
            return false;
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database is not connected. Call connectDatabase() first.");
        }
        try {
            if (connection.isClosed()) {
                throw new IllegalStateException("Database connection is closed.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify database connection.", e);
        }
        return connection;
    }

    public static void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Failed to close database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
