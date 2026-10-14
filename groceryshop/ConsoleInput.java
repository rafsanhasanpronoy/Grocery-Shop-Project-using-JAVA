package groceryshop;

import java.util.Scanner;

public final class ConsoleInput {

    private static final Scanner INPUT = new Scanner(System.in);

    private ConsoleInput() {
    }

    public static Scanner scanner() {
        return INPUT;
    }

    /**
     * Reads an integer without crashing when the user enters text.
     * Returns -1 when standard input reaches EOF, allowing menus to exit cleanly.
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!INPUT.hasNextLine()) {
                return -1;
            }

            String line = INPUT.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }
}
