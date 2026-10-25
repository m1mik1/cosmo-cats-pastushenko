-- Use sequence defaults so subsequent inserts cannot collide with seed IDs.
INSERT INTO categories (name) VALUES
    ('Space Food'),
    ('Cosmic Toys'),
    ('Space Accessories');

INSERT INTO products (name, description, price, category_id, stock_quantity)
SELECT 'Cosmic Milk', 'Milk for space cats', 12.50, id, 20
FROM categories WHERE name = 'Space Food';

INSERT INTO products (name, description, price, category_id, stock_quantity)
SELECT 'Galaxy Yarn', 'An antigravity ball of yarn', 8.99, id, 15
FROM categories WHERE name = 'Cosmic Toys';

INSERT INTO products (name, description, price, category_id, stock_quantity)
SELECT 'Comet Collar', 'A glowing collar for space travel', 25.00, id, 10
FROM categories WHERE name = 'Space Accessories';
