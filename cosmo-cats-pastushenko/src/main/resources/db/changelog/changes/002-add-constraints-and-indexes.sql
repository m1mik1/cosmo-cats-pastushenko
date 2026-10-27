ALTER TABLE categories ADD CONSTRAINT uq_categories_name UNIQUE (name);
ALTER TABLE products ADD CONSTRAINT uq_products_category_name UNIQUE (category_id, name);

-- The composite unique index above also supports lookups by category_id.
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
