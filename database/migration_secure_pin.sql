-- Run this once against an existing grocery_shop database before using
-- the updated application. It preserves current PIN values; successful
-- logins will upgrade legacy plaintext PINs to salted PBKDF2 hashes.
USE grocery_shop;

ALTER TABLE users
    MODIFY COLUMN pin VARCHAR(255) NOT NULL;
