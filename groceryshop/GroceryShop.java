package groceryshop;

public class GroceryShop {

    public static void main(String[] args) {
        if (!DatabaseManager.connectDatabase()) {
            return;
        }

        try {
            while (true) {
                System.out.println("\nWelcome to Grocery Shop");
                System.out.println("1. Signup");
                System.out.println("2. Login");
                System.out.println("3. Exit");

                int choice = ConsoleInput.readInt("Select an option: ");
                if (choice == -1 || choice == 3) {
                    System.out.println("Exiting the application. Goodbye!");
                    break;
                }

                switch (choice) {
                    case 1 -> UserManager.signup();
                    case 2 -> UserManager.login();
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            }
        } finally {
            DatabaseManager.closeDatabase();
        }
    }
}
