CREATE TABLE IF NOT EXISTS orders (
    id             UUID PRIMARY KEY,
    customer_email VARCHAR(255) NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL
);