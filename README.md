# Grocery Shop Project using Java

A Java console application for browsing daily groceries and medicines, maintaining a cart, and checking out against a MySQL inventory.

## Features

- User signup and login with a Bangladeshi mobile number
- Browse products by category
- Add and view cart items
- Checkout with transactional stock updates
- PIN hashing and automatic upgrade of legacy plaintext PINs at successful login

## Repository structure

```text
groceryshop/
  CartManager.java
  ConsoleInput.java
  DatabaseManager.java
  GroceryShop.java
  PinSecurity.java
  ProductManager.java
  UserManager.java
database/
  migration_secure_pin.sql
README.md
```

## Requirements

- JDK 14 or newer (the source uses switch expressions introduced in Java 14)
- MySQL
- MySQL Connector/J available on the Java classpath

## Database setup

Create the database and tables if you have not already done so:

```sql
CREATE DATABASE grocery_shop;
USE grocery_shop;

CREATE TABLE users (
    phone VARCHAR(15) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    address VARCHAR(100) NOT NULL,
    pin VARCHAR(255) NOT NULL
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category ENUM('Daily Grocery', 'Medicine') NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    CONSTRAINT chk_products_stock_nonnegative CHECK (stock >= 0)
);

CREATE TABLE cart (
    phone VARCHAR(15) NOT NULL,
    product_id INT NOT NULL,
    category ENUM('Daily Grocery', 'Medicine') NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (phone, product_id, category),
    CONSTRAINT chk_cart_quantity_positive CHECK (quantity > 0),
    FOREIGN KEY (phone) REFERENCES users(phone),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

Add sample products to the `products` table as needed. If you already have a database, do not recreate these tables.

### Existing database: one-time PIN column migration

Before running the updated code against an existing database, run the script in `database/migration_secure_pin.sql` once. This widens `users.pin` to store salted PBKDF2 hashes. Existing plaintext PINs remain usable: on a successful login, the application upgrades that account's PIN to a hash. Encourage all users to log in after the update.

## Database configuration

The app reads these environment variables, with local development defaults for URL and username:

- `GROCERY_DB_URL` (default: `jdbc:mysql://localhost:3306/grocery_shop`)
- `GROCERY_DB_USER` (default: `root`)
- `GROCERY_DB_PASSWORD` (default: empty)

For PowerShell, for example:

```powershell
$env:GROCERY_DB_URL = "jdbc:mysql://localhost:3306/grocery_shop"
$env:GROCERY_DB_USER = "root"
$env:GROCERY_DB_PASSWORD = "your-local-mysql-password"
```

Do not commit real database credentials to Git.

## Compile and run

From the repository root, assuming the MySQL Connector/J JAR is at `lib/mysql-connector-j.jar`:

**Windows PowerShell**
```powershell
javac -cp ".;lib/mysql-connector-j.jar" groceryshop/*.java
java -cp ".;lib/mysql-connector-j.jar" groceryshop.GroceryShop
```

**Linux/macOS**
```bash
javac -cp ".:lib/mysql-connector-j.jar" groceryshop/*.java
java -cp ".:lib/mysql-connector-j.jar" groceryshop.GroceryShop
```

Adjust the JAR path to match the Connector/J file you downloaded.

## Important behavior

- Cart quantities must be positive and the requested total quantity cannot exceed current stock.
- A product must belong to the category selected in the menu.
- Checkout is transactional. If any item cannot be fulfilled, all stock changes are rolled back and the cart remains.
- Prepared statements and try-with-resources are used for database operations.
- Checkout protects against negative stock with a guarded SQL update and checks the affected-row count.

## Limitations

This is an educational console application, not a production e-commerce service. It uses one shared JDBC connection and does not yet include payment processing, order history, account recovery, or automated integration tests.
